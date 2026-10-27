package sisso.logicsim.classroom.persistence;

import sisso.logicsim.classroom.domain.ClassroomViews.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import static sisso.logicsim.classroom.persistence.SqliteAccess.*;

/** Indexed, role-scoped metadata shared by administrative and teaching screens. */
public final class LearningProgress {
    private LearningProgress() { }

    public static final String LATEST = "s.revision=(SELECT max(s2.revision) FROM learning_submissions s2 WHERE s2.activity_id=s.activity_id AND s2.student_id=s.student_id)";
    public static final String JOINS = """
        LEFT JOIN learning_submissions s ON s.activity_id=a.id AND s.student_id=? AND
        """ + LATEST + "\n" + """
        LEFT JOIN learning_versions sv ON sv.activity_id=s.activity_id AND sv.number=s.publication
        LEFT JOIN learning_grades g ON g.submission_id=s.id AND g.revision=(SELECT max(g2.revision) FROM learning_grades g2 WHERE g2.submission_id=s.id)
        """;
    public static final String COLUMNS = """
        a.id AS progress_activity,v.title AS progress_title,v.deadline AS progress_deadline,v.value AS progress_value,
        s.id AS progress_submission,s.revision AS progress_revision,s.late AS progress_late,
        g.score AS progress_score,sv.value AS progress_submitted_value
        """;
    public static final String DELIVERED = """
        (SELECT count(*) FROM learning_activities pa WHERE pa.classroom_id=m.classroom_id AND pa.archived=0
          AND pa.current_version IS NOT NULL AND EXISTS(SELECT 1 FROM learning_submissions ps
            WHERE ps.activity_id=pa.id AND ps.student_id=m.player_uuid))
        """;

    public record Counts(int students,int activities,Overview overview) { }

    public static Counts counts(Connection c,UUID classroom,UUID actor,boolean teacher) throws SQLException {
        return one(c,"""
            SELECT count(*) AS students,
              (SELECT count(*) FROM learning_activities a WHERE a.classroom_id=? AND a.archived=0
                AND (?=1 OR a.current_version IS NOT NULL)) AS activities,
              (SELECT count(*) FROM learning_activities a WHERE a.classroom_id=? AND a.archived=0 AND a.current_version IS NOT NULL) AS published,
              (SELECT count(*) FROM learning_activities a WHERE a.classroom_id=? AND a.archived=0 AND a.current_version IS NULL AND ?=1) AS drafts,
              (SELECT count(*) FROM learning_submissions s JOIN learning_activities a ON a.id=s.activity_id
                JOIN classroom_memberships am ON am.classroom_id=a.classroom_id AND am.player_uuid=s.student_id AND am.active=1 AND am.role='STUDENT'
                WHERE a.classroom_id=? AND a.archived=0 AND a.current_version IS NOT NULL AND ?=1 AND
            """ + LATEST + "\n" + """
                AND NOT EXISTS(SELECT 1 FROM learning_grades g WHERE g.submission_id=s.id)) AS waiting,
              (SELECT count(*) FROM learning_activities a WHERE a.classroom_id=? AND a.archived=0 AND a.current_version IS NOT NULL
                AND EXISTS(SELECT 1 FROM learning_submissions s WHERE s.activity_id=a.id AND s.student_id=?)) AS delivered
            FROM classroom_memberships WHERE classroom_id=? AND active=1 AND role='STUDENT' AND ?=1
            """,r->new Counts(r.getInt("students"),r.getInt("activities"),
                new Overview(r.getInt("published"),r.getInt("drafts"),r.getInt("waiting"),r.getInt("delivered"))),
                classroom,teacher?1:0,classroom,classroom,teacher?1:0,classroom,teacher?1:0,classroom,actor,classroom,teacher?1:0).orElseThrow();
    }

    public static ActivityProgress activity(ResultSet r) throws SQLException {
        if(r.getObject("progress_deadline")==null) return null;
        String submitted=r.getString("progress_submission");
        return new ActivityProgress(UUID.fromString(r.getString("progress_activity")),r.getString("progress_title"),
                r.getLong("progress_deadline"),r.getLong("progress_value"),submitted==null?null:UUID.fromString(submitted),
                r.getInt("progress_revision"),r.getInt("progress_late")==1,
                r.getObject("progress_score")==null?null:r.getLong("progress_score"),r.getLong("progress_submitted_value"));
    }
}
