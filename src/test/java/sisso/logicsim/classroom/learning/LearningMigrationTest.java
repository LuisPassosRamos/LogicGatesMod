package sisso.logicsim.classroom.learning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sisso.logicsim.classroom.domain.*;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LearningMigrationTest {
    @TempDir Path dir;
    UUID teacher=UUID.randomUUID(),student=UUID.randomUUID(),classroom=UUID.randomUUID(),activity=UUID.randomUUID(),submission=UUID.randomUUID();
    Path database(){return dir.resolve("classroom.db");}
    void releasedFive() throws Exception{
        try(var input=getClass().getResourceAsStream("/classroom/released-v5.sql");var c=DriverManager.getConnection("jdbc:sqlite:"+database());var s=c.createStatement()){
            for(var sql:new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split(";"))if(!sql.isBlank())s.execute(sql);
            UUID membership=UUID.randomUUID();
            s.execute("INSERT INTO classrooms VALUES('"+classroom+"','"+teacher+"','Persisted','Description',7,'persisted','Message',NULL)");
            s.execute("INSERT INTO classroom_memberships VALUES('"+UUID.randomUUID()+"','"+classroom+"','"+teacher+"','TEACHER',1,NULL,10,NULL,0)");
            s.execute("INSERT INTO classroom_memberships VALUES('"+membership+"','"+classroom+"','"+student+"','STUDENT',1,'00123',20,NULL,2)");
            s.execute("INSERT INTO learning_activities(id,classroom_id,revision,has_draft,draft_title,draft_description,draft_value,current_version) VALUES('"+activity+"','"+classroom+"',4,1,'Private','Draft',2000,1)");
            s.execute("INSERT INTO learning_versions VALUES('"+activity+"',1,'Published','Statement',9999999999999,1000,30)");
            s.execute("INSERT INTO learning_submissions VALUES('"+submission+"','"+activity+"',1,'"+student+"','"+membership+"',1,40,0,'"+UUID.randomUUID()+"','"+"a".repeat(64)+"',123,2,1,1)");
            s.execute("INSERT INTO learning_grades VALUES('"+submission+"',1,700,'Feedback',50,'"+teacher+"')");
        }
    }
    @Test void upgradePreservesVersionsPrivateDraftsEnrollmentSubmissionsAndGradesAndBacksUpFive() throws Exception{
        releasedFive();new SqliteClassroomRepository(database());
        var store=new LearningStore(database(),id->true,LearningLimits.DEFAULT);
        var saved=store.activity(teacher,activity);assertEquals("Private",saved.draft().title());assertEquals("Published",saved.published().title());
        assertEquals(VerificationRules.EMPTY,saved.draft().rules());assertEquals(VerificationRules.EMPTY,saved.published().rules());
        assertEquals(700,store.submission(student,submission).assessment().score());assertEquals(submission,store.verification(teacher,submission).submission().id());
        assertEquals("00123",store.students(teacher,activity,0).items().getFirst().enrollment());
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+database());var s=c.createStatement();var rows=s.executeQuery("PRAGMA user_version")){assertEquals(6,rows.getInt(1));}
        Path backup;try(var entries=Files.list(dir.resolve("backups"))){backup=entries.findFirst().orElseThrow();}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+backup);var s=c.createStatement()){
            try(var rows=s.executeQuery("PRAGMA user_version")){assertEquals(5,rows.getInt(1));}
            try(var rows=s.executeQuery("SELECT score FROM learning_grades")){assertEquals(700,rows.getInt(1));}
            assertThrows(SQLException.class,()->s.executeQuery("SELECT rules FROM learning_versions"));
        }
        new SqliteClassroomRepository(database());try(var entries=Files.list(dir.resolve("backups"))){assertEquals(1,entries.count());}
    }
    @Test void failedUpgradeRollsBackAndLeavesTheOriginalVersionAndData() throws Exception{
        releasedFive();
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+database());var s=c.createStatement()){s.execute("ALTER TABLE learning_versions ADD COLUMN rules TEXT");}
        assertThrows(ClassroomException.class,()->new SqliteClassroomRepository(database()));
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+database());var s=c.createStatement()){
            try(var rows=s.executeQuery("PRAGMA user_version")){assertEquals(5,rows.getInt(1));}
            assertThrows(SQLException.class,()->s.executeQuery("SELECT draft_rules FROM learning_activities"));
            try(var rows=s.executeQuery("SELECT feedback FROM learning_grades")){assertEquals("Feedback",rows.getString(1));}
        }
    }
}
