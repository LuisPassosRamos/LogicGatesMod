package sisso.logicsim.classroom.learning;

import sisso.logicsim.classroom.domain.ClassroomViews.Page;

import java.time.*;
import java.time.format.*;
import java.math.BigDecimal;
import java.util.*;
import static sisso.logicsim.classroom.learning.LearningException.Code.*;

public final class LearningViews {
    private LearningViews() { }
    public static final ZoneId BRASILIA=ZoneId.of("America/Sao_Paulo");
    public static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public record Draft(String title,String description,Long deadline,Long value,VerificationRules rules) {
        public Draft(String title,String description,Long deadline,Long value){this(title,description,deadline,value,VerificationRules.EMPTY);}
        public Draft {
            rules=rules==null?VerificationRules.EMPTY:rules;
            title=title==null?"":title.strip(); description=description==null?"":description.strip();
            if(title.length()>128 || description.length()>8192 || title.codePoints().anyMatch(Character::isISOControl)
                    || description.codePoints().anyMatch(c->Character.isISOControl(c) && c!='\n' && c!='\t')
                    || value!=null && value<1) throw new LearningException(INVALID_DRAFT);
        }
        public static Draft empty() { return new Draft("","",null,1000L); }
        public void publishable(long now) {
            if(title.isBlank() || description.isBlank() || deadline==null || value==null) throw new LearningException(INVALID_DRAFT);
            if(deadline<=now) throw new LearningException(FUTURE_DEADLINE);
            rules.cases();
        }
    }
    public record Publication(UUID activity,int number,String title,String description,long deadline,long value,long publishedAt,VerificationRules rules) {
        public Publication(UUID activity,int number,String title,String description,long deadline,long value,long publishedAt){this(activity,number,title,description,deadline,value,publishedAt,VerificationRules.EMPTY);}
        public Publication{rules=rules==null?VerificationRules.EMPTY:rules;}
        public Draft draft() { return new Draft(title,description,deadline,value,rules); }
    }
    public record Activity(UUID id,UUID classroom,long revision,boolean archived,Draft draft,Publication published) {
        public Draft editable() { return draft!=null?draft:published!=null?published.draft():Draft.empty(); }
        public String title() { return draft!=null && !draft.title().isBlank()?draft.title():published!=null?published.title():""; }
    }
    public record Assessment(int revision,long score,String feedback,long at) { }
    public record Submission(UUID id,UUID activity,int publication,UUID student,UUID membership,int revision,long at,boolean late,
                             UUID content,String checksum,long bytes,int width,int height,int depth,long value,Assessment assessment) { }
    public record Student(UUID id,String name,String enrollment,boolean active,Submission latest) { }
    public record Context(UUID classroom,UUID membership,boolean teacher,String name) { }
    public record Catalog(Page<Activity> rows,Map<UUID,sisso.logicsim.classroom.domain.ClassroomViews.ActivityProgress> progress,
                          sisso.logicsim.classroom.domain.ClassroomViews.Overview overview,int students) {
        public Catalog { progress=Map.copyOf(progress); }
    }
    public record VerificationContext(Submission submission,Publication publication) { }
    public record ReviewInfo(String activity,String student,int revision) { }
    public record Review(UUID id,UUID teacher,UUID submission,UUID classroom,UUID world,int x,int y,int z,int width,int height,int depth) {
        public boolean contains(UUID world,int x,int y,int z) {
            return this.world.equals(world) && x>=this.x && y>=this.y && z>=this.z
                    && (long)x<this.x+(long)width && (long)y<this.y+(long)height && (long)z<this.z+(long)depth;
        }
        public boolean overlaps(Review other) {
            return world.equals(other.world) && (long)x<other.x+(long)other.width && (long)other.x<x+(long)width
                    && (long)y<other.y+(long)other.height && (long)other.y<y+(long)height
                    && (long)z<other.z+(long)other.depth && (long)other.z<z+(long)depth;
        }
    }
    public static Long points(String input) {
        if(input==null || input.isBlank()) return null;
        if(input.length()>24 || !input.strip().matches("[+-]?\\d+(?:[.,]\\d{1,2})?")) throw new LearningException(INVALID_DRAFT);
        try { return new BigDecimal(input.strip().replace(',','.')).movePointRight(2).longValueExact(); }
        catch(RuntimeException e) { throw new LearningException(INVALID_DRAFT); }
    }
    public static String points(Long cents) { return cents==null?"":BigDecimal.valueOf(cents,2).stripTrailingZeros().toPlainString(); }
    public static Long deadline(String input) {
        if(input==null || input.isBlank()) return null;
        try {
            var local=LocalDateTime.parse(input.strip(),DATE); var offsets=BRASILIA.getRules().getValidOffsets(local);
            if(offsets.size()!=1) throw new LearningException(INVALID_DRAFT);
            return local.toInstant(offsets.getFirst()).toEpochMilli();
        } catch(DateTimeException e) { throw new LearningException(INVALID_DRAFT); }
    }
    public static String date(Long at) { return at==null?"":DATE.format(Instant.ofEpochMilli(at).atZone(BRASILIA)); }
}
