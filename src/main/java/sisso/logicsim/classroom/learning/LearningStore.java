package sisso.logicsim.classroom.learning;

import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.classroom.domain.ClassroomReadPolicy;
import sisso.logicsim.classroom.domain.ClassroomViews.Page;
import sisso.logicsim.classroom.persistence.SqliteAccess;
import sisso.logicsim.classroom.persistence.LearningProgress;
import static sisso.logicsim.classroom.persistence.SqliteAccess.*;
import sisso.logicsim.classroom.learning.LearningViews.*;
import java.nio.file.Path;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Predicate;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;
import static sisso.logicsim.classroom.learning.LearningException.Code.*;

/** Pure Java learning application/persistence boundary. Native permission is a trusted server predicate. */
public final class LearningStore {
    public enum Filter { ALL, PUBLISHED, DRAFT, ARCHIVED }
    private final SqliteAccess database;
    private final Predicate<UUID> canTeach;
    private final LearningLimits limits;
    private final Clock clock;
    private static final String ACTIVITY="""
        SELECT a.*,v.number AS published_number,v.title AS published_title,v.description AS published_description,
        v.deadline AS published_deadline,v.value AS published_value,v.published_at,v.rules AS published_rules
        FROM learning_activities a LEFT JOIN learning_versions v ON v.activity_id=a.id AND v.number=a.current_version
        """;
    private static final String REVIEWS="SELECT r.*,a.classroom_id FROM learning_reviews r JOIN learning_submissions s ON s.id=r.submission_id JOIN learning_activities a ON a.id=s.activity_id";
    private static final String SUBMISSION="""
        SELECT s.*,v.value AS version_value,g.revision AS grade_revision,g.score,g.feedback,g.graded_at
        FROM learning_submissions s JOIN learning_versions v ON v.activity_id=s.activity_id AND v.number=s.publication
        LEFT JOIN learning_grades g ON g.submission_id=s.id AND g.revision=(SELECT max(g2.revision) FROM learning_grades g2 WHERE g2.submission_id=s.id)
        """;
    public LearningStore(Path database,Predicate<UUID> canTeach,LearningLimits limits) { this(database,canTeach,limits,Clock.systemUTC()); }
    public LearningStore(Path database,Predicate<UUID> canTeach,LearningLimits limits,Clock clock) {
        this.database=new SqliteAccess(database); this.canTeach=canTeach; this.limits=limits; this.clock=clock;
    }
    public Context context(UUID actor) {
        return read(c -> context(c, actor));
    }

    private Context context(Connection c, UUID actor) throws SQLException {
        if (actor == null) throw new ClassroomException(FORBIDDEN);
        var context = one(c, """
            SELECT m.id,m.classroom_id,m.role,c.teacher_uuid,c.name FROM classroom_memberships m
            JOIN classrooms c ON c.id=m.classroom_id WHERE m.player_uuid=? AND m.active=1 AND c.closed_at IS NULL
            """, r -> {
                boolean teacher = r.getString("role").equals("TEACHER");
                if (teacher && (!canTeach.test(actor) || !actor.toString().equals(r.getString("teacher_uuid"))))
                    throw new ClassroomException(FORBIDDEN);
                return new Context(id(r, "classroom_id"), id(r, "id"), teacher, r.getString("name"));
            }, actor);
        return context.orElseThrow(() -> new ClassroomException(FORBIDDEN));
    }
    private static void teacher(Context context) { if(!context.teacher()) throw new ClassroomException(FORBIDDEN); }
    public Page<Activity> activities(UUID actor,Filter filter,int page) {
        ClassroomReadPolicy.page(page);
        return read(c->{
            var ctx=context(c,actor);
            String state=!ctx.teacher()?" AND a.archived=0 AND a.current_version IS NOT NULL":switch(filter) {
                case ALL->""; case PUBLISHED->" AND a.archived=0 AND a.current_version IS NOT NULL";
                case DRAFT->" AND a.archived=0 AND a.has_draft=1"; case ARCHIVED->" AND a.archived=1";
            };
            return page(c,ACTIVITY+" WHERE a.classroom_id=?"+state+" ORDER BY coalesce(v.title,a.draft_title),a.id LIMIT 21 OFFSET ?",
                    r->activity(r,ctx.teacher()),page,ctx.classroom(),page*PAGE_SIZE);
        });
    }
    public Activity activity(UUID actor, UUID activity) {
        return read(c -> activity(c, context(c, actor), activity));
    }

    private Activity activity(Connection c, Context ctx, UUID activity) throws SQLException {
        var result = one(c, ACTIVITY + " WHERE a.id=? AND a.classroom_id=?",
                r -> activity(r, ctx.teacher()), activity, ctx.classroom())
                .orElseThrow(() -> new ClassroomException(NOT_FOUND));
        if (!ctx.teacher() && (result.archived() || result.published() == null)) throw new ClassroomException(FORBIDDEN);
        return result;
    }

    public Activity create(UUID actor) {
        return write(c -> {
            var ctx = context(c, actor);
            return create(c,ctx);
        });
    }
    private Activity create(Connection c,Context ctx) throws SQLException {
        teacher(ctx);
        UUID id=UUID.randomUUID();
        execute(c,"INSERT INTO learning_activities(id,classroom_id,draft_value) VALUES(?,?,1000)",id,ctx.classroom());
        return activity(c,ctx,id);
    }
    /** First explicit save/publication is atomic: failed publication cannot leave a blank activity. */
    public Activity create(UUID actor,Draft draft,boolean publish) {
        Objects.requireNonNull(draft);
        return write(c->{var ctx=context(c,actor);var created=create(c,ctx);
            var saved=save(c,ctx,created.id(),created.revision(),draft);
            return publish?publish(c,ctx,saved.id(),saved.revision()):saved;});
    }
    public Catalog catalog(UUID actor,Filter filter,int number) {
        ClassroomReadPolicy.page(number);
        return read(c->{
            var ctx=context(c,actor);
            String state=!ctx.teacher()?" AND a.archived=0 AND a.current_version IS NOT NULL":switch(filter){
                case ALL->"";case PUBLISHED->" AND a.archived=0 AND a.current_version IS NOT NULL";
                case DRAFT->" AND a.archived=0 AND a.has_draft=1";case ARCHIVED->" AND a.archived=1";};
            var progress=new HashMap<UUID,sisso.logicsim.classroom.domain.ClassroomViews.ActivityProgress>();
            String select=ACTIVITY.replace("FROM learning_activities",", "+LearningProgress.COLUMNS+" FROM learning_activities");
            var rows=page(c,select+LearningProgress.JOINS+" WHERE a.classroom_id=?"+state+" ORDER BY coalesce(v.title,a.draft_title),a.id LIMIT 21 OFFSET ?",
                r->{var item=activity(r,ctx.teacher());var status=LearningProgress.activity(r);if(status!=null)progress.put(item.id(),status);return item;},
                number,actor,ctx.classroom(),number*PAGE_SIZE);
            var counts=LearningProgress.counts(c,ctx.classroom(),actor,ctx.teacher());
            return new Catalog(rows,progress,counts.overview(),counts.students());
        });
    }

    public Activity save(UUID actor, UUID id, long revision, Draft draft) {
        Objects.requireNonNull(draft);
        return write(c -> save(c, context(c, actor), id, revision, draft));
    }

    private Activity save(Connection c, Context ctx, UUID id, long revision, Draft draft) throws SQLException {
        teacher(ctx);
        var current = activity(c, ctx, id);
        checkRevision(current, revision);
        if (current.archived()) throw new LearningException(ARCHIVED);
        execute(c, "UPDATE learning_activities SET has_draft=1,draft_title=?,draft_description=?,draft_deadline=?,draft_value=?,draft_rules=?,revision=revision+1 WHERE id=?",
                draft.title(), draft.description(), draft.deadline(), draft.value(), draft.rules().encode(), id);
        return activity(c, ctx, id);
    }

    public Activity saveAndPublish(UUID actor, UUID id, long revision, Draft draft) {
        Objects.requireNonNull(draft);
        return write(c -> {
            var ctx = context(c, actor);
            var saved = save(c, ctx, id, revision, draft);
            return publish(c, ctx, id, saved.revision());
        });
    }

    public Activity publish(UUID actor, UUID id, long revision) {
        return write(c -> publish(c, context(c, actor), id, revision));
    }

    private Activity publish(Connection c, Context ctx, UUID id, long revision) throws SQLException {
        teacher(ctx);
        var current = activity(c, ctx, id);
        checkRevision(current, revision);
        if (current.archived()) throw new LearningException(ARCHIVED);
        if (current.draft() == null) throw new LearningException(NO_DRAFT);
        long now = clock.millis();
        var draft = current.draft();
        draft.publishable(now);
        int number = current.published() == null ? 1 : current.published().number() + 1;
        execute(c, "INSERT INTO learning_versions(activity_id,number,title,description,deadline,value,published_at,rules) VALUES(?,?,?,?,?,?,?,?)",
                id, number, draft.title(), draft.description(), draft.deadline(), draft.value(), now, draft.rules().encode());
        execute(c, "UPDATE learning_activities SET current_version=?,has_draft=0,revision=revision+1 WHERE id=?", number, id);
        return activity(c, ctx, id);
    }
    public Activity archive(UUID actor,UUID id,long revision,boolean archived) {
        return write(c->{ var ctx=context(c,actor); teacher(ctx); checkRevision(activity(c,ctx,id),revision);
            execute(c,"UPDATE learning_activities SET archived=?,revision=revision+1 WHERE id=?",archived?1:0,id); return activity(c,ctx,id); });
    }
    /** Only never-published activities can be removed; immutable versions and student history are retained. */
    public void deleteDraft(UUID actor,UUID id,long revision) {
        write(c->{
            var ctx=context(c,actor);teacher(ctx);
            var current=activity(c,ctx,id);checkRevision(current,revision);
            if(current.published()!=null || one(c,"SELECT 1 FROM learning_versions WHERE activity_id=? LIMIT 1",r->r.getInt(1),id).isPresent())
                throw new LearningException(PUBLISHED_ACTIVITY);
            if(current.draft()==null)throw new LearningException(NO_DRAFT);
            execute(c,"DELETE FROM learning_activities WHERE id=? AND classroom_id=?",id,ctx.classroom());
            return null;
        });
    }
    private static void checkRevision(Activity activity,long expected) { if(activity.revision()!=expected) throw new ClassroomException(STALE); }
    public Publication publication(UUID actor, UUID activity, int number) {
        return read(c -> {
            activity(c, context(c, actor), activity);
            return publication(c, activity, number);
        });
    }

    private Publication publication(Connection c, UUID activity, int number) throws SQLException {
        return one(c, "SELECT * FROM learning_versions WHERE activity_id=? AND number=?", LearningStore::publication, activity, number)
                .orElseThrow(() -> new ClassroomException(NOT_FOUND));
    }

    /** One authorized read snapshot binds assistance to the delivered publication, never the latest draft. */
    public VerificationContext verification(UUID actor, UUID id) {
        return read(c -> {
            var ctx = context(c, actor);
            teacher(ctx);
            var submitted = submission(c, id);
            activity(c, ctx, submitted.activity());
            return new VerificationContext(submitted, publication(c, submitted.activity(), submitted.publication()));
        });
    }
    public Page<Student> students(UUID actor,UUID activity,int page) {
        ClassroomReadPolicy.page(page);
        return read(c->{ var ctx=context(c,actor); teacher(ctx); activity(c,ctx,activity);
            // One row per UUID, including historical participants; no per-student content or query load.
            return page(c,"""
                SELECT m.player_uuid,coalesce(p.name,m.player_uuid) AS name,m.enrollment,m.active,s.*,
                  v.value AS version_value,g.revision AS grade_revision,g.score,g.feedback,g.graded_at
                FROM classroom_memberships m LEFT JOIN classroom_players p ON p.uuid=m.player_uuid
                LEFT JOIN learning_submissions s ON s.activity_id=? AND s.student_id=m.player_uuid
                  AND s.revision=(SELECT max(s2.revision) FROM learning_submissions s2 WHERE s2.activity_id=s.activity_id AND s2.student_id=s.student_id)
                LEFT JOIN learning_versions v ON v.activity_id=s.activity_id AND v.number=s.publication
                LEFT JOIN learning_grades g ON g.submission_id=s.id AND g.revision=(SELECT max(g2.revision) FROM learning_grades g2 WHERE g2.submission_id=s.id)
                WHERE m.classroom_id=? AND m.role='STUDENT' AND m.id=(SELECT m2.id FROM classroom_memberships m2
                  WHERE m2.classroom_id=m.classroom_id AND m2.player_uuid=m.player_uuid AND m2.role='STUDENT' ORDER BY m2.joined_at DESC,m2.id DESC LIMIT 1)
                ORDER BY coalesce(p.search_name,m.player_uuid),m.player_uuid LIMIT 21 OFFSET ?
                """,r->new Student(id(r,"player_uuid"),r.getString("name"),r.getString("enrollment"),r.getInt("active")==1,
                    r.getString("id")==null?null:submission(r)),page,activity,ctx.classroom(),page*PAGE_SIZE);
        });
    }
    /** Metadata for a directly opened student outside the visible roster page. */
    public Student student(UUID actor,UUID student) {
        return read(c->{var ctx=context(c,actor);teacher(ctx);
            return one(c,"""
                SELECT m.player_uuid,coalesce(p.name,m.player_uuid) AS name,m.enrollment,m.active
                FROM classroom_memberships m LEFT JOIN classroom_players p ON p.uuid=m.player_uuid
                WHERE m.classroom_id=? AND m.player_uuid=? AND m.role='STUDENT'
                ORDER BY m.joined_at DESC,m.id DESC LIMIT 1
                """,r->new Student(id(r,"player_uuid"),r.getString("name"),r.getString("enrollment"),r.getInt("active")==1,null),ctx.classroom(),student).orElse(null);
        });
    }
    public Page<Submission> submissions(UUID actor,UUID activity,UUID student,int page) {
        ClassroomReadPolicy.page(page);
        return read(c->{ var ctx=context(c,actor); activity(c,ctx,activity);
            if(!ctx.teacher() && !actor.equals(student)) throw new ClassroomException(FORBIDDEN);
            return page(c,SUBMISSION+" WHERE s.activity_id=? AND s.student_id=? ORDER BY s.revision DESC LIMIT 21 OFFSET ?",
                    LearningStore::submission,page,activity,student,page*PAGE_SIZE); });
    }
    public Submission submission(UUID actor,UUID id) {
        return read(c->{ var ctx=context(c,actor); var result=submission(c,id); activity(c,ctx,result.activity());
            if(!ctx.teacher() && !actor.equals(result.student())) throw new ClassroomException(FORBIDDEN); return result; });
    }
    private Submission submission(Connection c,UUID id) throws SQLException {
        return one(c,SUBMISSION+" WHERE s.id=?",LearningStore::submission,id).orElseThrow(()->new ClassroomException(NOT_FOUND));
    }
    public Submission submit(UUID actor,UUID activity,int publication,UUID membership,UUID request,SubmissionFiles.Content content) {
        return submit(actor,activity,publication,membership,request,content,()->true);
    }
    public Submission submit(UUID actor,UUID activity,int publication,UUID membership,UUID request,SubmissionFiles.Content content,java.util.function.BooleanSupplier selectionActive) {
        content.snapshot().validate(limits);
        return write(c->{ var ctx=context(c,actor);
            if(ctx.teacher() || !ctx.membership().equals(membership)) throw new ClassroomException(FORBIDDEN);
            var current=activity(c,ctx,activity);
            if(current.archived()) throw new LearningException(ARCHIVED);
            if(current.published()==null || current.published().number()!=publication) throw new ClassroomException(STALE);
            var existing=one(c,SUBMISSION+" WHERE s.id=?",LearningStore::submission,request);
            if(existing.isPresent()) {
                if(!existing.get().student().equals(actor) || !existing.get().activity().equals(activity)) throw new ClassroomException(FORBIDDEN);
                return existing.get();
            }
            long stored=one(c,"SELECT coalesce(sum(s.bytes),0) FROM learning_submissions s JOIN learning_activities a ON a.id=s.activity_id WHERE a.classroom_id=?",r->r.getLong(1),ctx.classroom()).orElse(0L);
            if(content.bytes()<1 || content.bytes()>limits.contentBytes()+65536L || stored>limits.classroomBytes()-content.bytes()) throw new LearningException(STORAGE_LIMIT);
            int revision=one(c,"SELECT coalesce(max(revision),0)+1 FROM learning_submissions WHERE activity_id=? AND student_id=?",r->r.getInt(1),activity,actor).orElse(1);
            var area=content.snapshot(); long now=clock.millis();
            if(!selectionActive.getAsBoolean())throw new LearningException(NO_SELECTION);
            execute(c,"INSERT INTO learning_submissions VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",request,activity,publication,actor,membership,revision,now,
                    now>current.published().deadline()?1:0,content.id(),content.checksum(),content.bytes(),area.width(),area.height(),area.depth());
            return submission(c,request); });
    }
    public Submission grade(UUID actor,UUID submission,int expected,long score,String feedback) {
        String text=feedback==null?"":feedback.strip();
        if(text.length()>4096 || text.codePoints().anyMatch(c->Character.isISOControl(c) && c!='\n' && c!='\t')) throw new LearningException(INVALID_SCORE);
        return write(c->{ var ctx=context(c,actor); teacher(ctx); var current=submission(c,submission); activity(c,ctx,current.activity());
            int revision=current.assessment()==null?0:current.assessment().revision();
            if(expected!=revision) throw new ClassroomException(STALE);
            if(score<0 || score>current.value()) throw new LearningException(INVALID_SCORE);
            execute(c,"INSERT INTO learning_grades VALUES(?,?,?,?,?,?)",submission,revision+1,score,text,clock.millis(),actor); return submission(c,submission); });
    }
    public Review reserve(UUID actor,UUID submission,UUID world,int x,int y,int z) {
        return write(c->{ var ctx=context(c,actor); teacher(ctx); var s=submission(c,submission); activity(c,ctx,s.activity());
            var review=new Review(UUID.randomUUID(),actor,submission,ctx.classroom(),world,x,y,z,s.width(),s.height(),s.depth());
            for(var other:query(c,REVIEWS,LearningStore::review))
                if(other.teacher().equals(actor) || other.overlaps(review)) throw new LearningException(AREA_PROTECTED);
            execute(c,"INSERT INTO learning_reviews VALUES(?,?,?,?,?,?,?,?,?,?)",review.id(),actor,submission,world,x,y,z,s.width(),s.height(),s.depth()); return review; });
    }
    /** Internal recovery ledger, never exposed directly to a client. */
    public List<Review> reviews() { return read(c->query(c,REVIEWS,LearningStore::review)); }
    /** Only the authorized owner can see the context of a temporary correction copy. */
    public ReviewInfo reviewInfo(UUID actor,UUID review) {
        return read(c->{var ctx=context(c,actor);teacher(ctx);
            return query(c,"""
                    SELECT v.title,coalesce(p.name,s.student_id) student,s.revision
                    FROM learning_reviews r JOIN learning_submissions s ON s.id=r.submission_id
                    JOIN learning_versions v ON v.activity_id=s.activity_id AND v.number=s.publication
                    JOIN learning_activities a ON a.id=s.activity_id
                    LEFT JOIN classroom_players p ON p.uuid=s.student_id
                    WHERE r.id=? AND r.teacher_id=? AND a.classroom_id=?
                    """,r->new ReviewInfo(r.getString("title"),r.getString("student"),r.getInt("revision")),review,actor,ctx.classroom()).stream().findFirst().orElse(null);
        });
    }
    public Set<UUID> contentIds() { return read(c->Set.copyOf(query(c,"SELECT content_id FROM learning_submissions",r->id(r,"content_id")))); }
    public void removedReview(UUID id) { write(c->{execute(c,"DELETE FROM learning_reviews WHERE id=?",id); return null;}); }

    private static Activity activity(ResultSet r, boolean teacher) throws SQLException {
        Publication publication = r.getObject("published_number") == null ? null : new Publication(
                id(r, "id"), r.getInt("published_number"), r.getString("published_title"),
                r.getString("published_description"), r.getLong("published_deadline"),
                r.getLong("published_value"), r.getLong("published_at"), VerificationRules.decode(r.getString("published_rules")));
        Draft draft = teacher && r.getInt("has_draft") == 1 ? new Draft(
                r.getString("draft_title"), r.getString("draft_description"), nullable(r, "draft_deadline"),
                nullable(r, "draft_value"), VerificationRules.decode(r.getString("draft_rules"))) : null;
        return new Activity(id(r, "id"), id(r, "classroom_id"), r.getLong("revision"), r.getInt("archived") == 1, draft, publication);
    }
    private static Publication publication(ResultSet r) throws SQLException {
        return new Publication(id(r,"activity_id"),r.getInt("number"),r.getString("title"),r.getString("description"),r.getLong("deadline"),r.getLong("value"),r.getLong("published_at"),VerificationRules.decode(r.getString("rules")));
    }
    private static Submission submission(ResultSet r) throws SQLException {
        var grade=r.getObject("grade_revision")==null?null:new Assessment(r.getInt("grade_revision"),r.getLong("score"),r.getString("feedback"),r.getLong("graded_at"));
        return new Submission(id(r,"id"),id(r,"activity_id"),r.getInt("publication"),id(r,"student_id"),id(r,"membership_id"),r.getInt("revision"),r.getLong("received_at"),r.getInt("late")==1,id(r,"content_id"),r.getString("checksum"),r.getLong("bytes"),r.getInt("width"),r.getInt("height"),r.getInt("depth"),r.getLong("version_value"),grade);
    }
    private static Review review(ResultSet r) throws SQLException { return new Review(id(r,"id"),id(r,"teacher_id"),id(r,"submission_id"),id(r,"classroom_id"),id(r,"world_id"),r.getInt("x"),r.getInt("y"),r.getInt("z"),r.getInt("width"),r.getInt("height"),r.getInt("depth")); }
    private static UUID id(ResultSet r,String name) throws SQLException { return UUID.fromString(r.getString(name)); }
    private static Long nullable(ResultSet r,String name) throws SQLException { return r.getObject(name)==null?null:r.getLong(name); }
    private <T> T read(Action<T> work) {
        return transaction(Transaction.READ, work);
    }

    private <T> T write(Action<T> work) {
        return transaction(Transaction.WRITE, work);
    }

    private <T> T transaction(Transaction mode, Action<T> work) {
        try (var connection = database.connect()) {
            return SqliteAccess.transaction(connection, mode, work);
        } catch (ClassroomException | LearningException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new ClassroomException(UNAVAILABLE, failure);
        }
    }
}
