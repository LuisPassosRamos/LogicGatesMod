package sisso.logicsim.classroom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sisso.logicsim.classroom.domain.*;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClassroomMigrationTest {
    @TempDir Path dir;
    final UUID teacher=UUID.randomUUID(), classroom=UUID.randomUUID(), membership=UUID.randomUUID();
    Path db() { return dir.resolve("classroom.db"); }
    // Frozen released v1 schema, independent of the current migration implementation.
    void releasedV1() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db());var s=c.createStatement()) {
            s.execute("CREATE TABLE classrooms(id TEXT PRIMARY KEY NOT NULL,teacher_uuid TEXT NOT NULL UNIQUE,name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 64),description TEXT NOT NULL CHECK(length(description)<=2048),revision INTEGER NOT NULL CHECK(revision>=0))");
            s.execute("CREATE TABLE classroom_memberships(id TEXT PRIMARY KEY NOT NULL,classroom_id TEXT NOT NULL REFERENCES classrooms(id),player_uuid TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN('TEACHER','STUDENT')),active INTEGER NOT NULL CHECK(active IN(0,1)))");
            s.execute("CREATE UNIQUE INDEX one_active_classroom_per_player ON classroom_memberships(player_uuid) WHERE active=1");
            s.execute("CREATE UNIQUE INDEX one_active_teacher_per_classroom ON classroom_memberships(classroom_id) WHERE active=1 AND role='TEACHER'");
            s.execute("PRAGMA application_id=1279738700");
            s.execute("PRAGMA user_version=1");
            s.execute("INSERT INTO classrooms VALUES('"+classroom+"','"+teacher+"','Turma teste','Descrição já salva',7)");
            s.execute("INSERT INTO classroom_memberships VALUES('"+membership+"','"+classroom+"','"+teacher+"','TEACHER',1)");
        }
    }
    @Test void releasedV1PreservesIdsContentRevisionAndCreatesRestorableBackup() throws Exception {
        releasedV1();
        var s=new ClassroomService(new SqliteClassroomRepository(db()),id->true);
        var c=s.own(teacher).orElseThrow();
        assertEquals(classroom,c.id()); assertEquals(7,c.revision()); assertEquals("Descrição já salva",c.description());
        assertEquals(membership,s.home(teacher).member().id()); assertEquals("",c.message());
        Path backup;
        try(var files=Files.list(dir.resolve("backups"))) { backup=files.findFirst().orElseThrow(); }
        try(var conn=DriverManager.getConnection("jdbc:sqlite:"+backup);var q=conn.createStatement();var r=q.executeQuery("PRAGMA user_version")) { assertEquals(1,r.getInt(1)); }
        var changed=s.update(teacher,c.id(),7,"Nova","Literal");
        assertEquals(8,changed.revision());
        Path restoreDir=dir.resolve("restored"); Files.createDirectories(restoreDir); Path restored=restoreDir.resolve("classroom.db"); Files.copy(backup,restored);
        assertEquals(c,new SqliteClassroomRepository(restored).findByMember(teacher).orElseThrow());
        new SqliteClassroomRepository(db());
        try(var files=Files.list(dir.resolve("backups"))) { assertEquals(1,files.count()); }
    }
    @Test void migrationFailureRollsBackEntireUpgradeAndKeepsOriginalFile() throws Exception {
        releasedV1();
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db());var s=c.createStatement()) {
            s.execute("INSERT INTO classroom_memberships VALUES('"+UUID.randomUUID()+"','"+classroom+"','"+UUID.randomUUID()+"','STUDENT',1)");
        }
        byte[] before=Files.readAllBytes(db());
        assertThrows(ClassroomException.class,()->new SqliteClassroomRepository(db()));
        assertArrayEquals(before,Files.readAllBytes(db()));
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db());var s=c.createStatement();var r=s.executeQuery("PRAGMA user_version")) { assertEquals(1,r.getInt(1)); }
    }
    @Test void schemaChecksRejectCounterfeitIndexWithCorrectName() throws Exception {
        new SqliteClassroomRepository(db());
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db());var s=c.createStatement()) {
            s.execute("DROP INDEX one_active_classroom_per_player");
            s.execute("CREATE INDEX one_active_classroom_per_player ON classroom_memberships(player_uuid)");
        }
        byte[] before=Files.readAllBytes(db());
        assertThrows(ClassroomException.class,()->new SqliteClassroomRepository(db()));
        assertArrayEquals(before,Files.readAllBytes(db()));
    }
    @Test void versionTwoAddsMessageWithoutLosingAdmissions() throws Exception {
        var s=new ClassroomService(new SqliteClassroomRepository(db()),id->true);
        var c=s.create(teacher,"A",""); UUID student=UUID.randomUUID();
        s.remember(student,"Known offline"); var invitation=s.invite(teacher,student);
        try(var conn=DriverManager.getConnection("jdbc:sqlite:"+db());var q=conn.createStatement()) {
            q.execute("ALTER TABLE classrooms DROP COLUMN message"); q.execute("PRAGMA user_version=2");
        }
        var reopened=new ClassroomService(new SqliteClassroomRepository(db()),id->true);
        assertEquals(c,reopened.own(teacher).orElseThrow());
        assertEquals(invitation,reopened.intakes(student,false,0).items().getFirst());
    }
    @Test void releasedV3MigrationKeepsForeignKeysAdmissionsAndBackupWhileAllowingTeacherReuse() throws Exception {
        Class.forName("org.sqlite.JDBC"); UUID student=UUID.randomUUID(), link=UUID.randomUUID(), intake=UUID.randomUUID();
        try(var input=getClass().getResourceAsStream("/classroom/released-v3.sql");
            var c=DriverManager.getConnection("jdbc:sqlite:"+db()); var q=c.createStatement()) {
            assertNotNull(input);
            for(String sql:new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split(";")) if(!sql.isBlank()) q.execute(sql);
            q.execute("INSERT INTO classrooms VALUES('"+classroom+"','"+teacher+"','Saved','Description',7,'saved','Message')");
            q.execute("INSERT INTO classroom_memberships VALUES('"+membership+"','"+classroom+"','"+teacher+"','TEACHER',1,NULL,123,NULL,0)");
            q.execute("INSERT INTO classroom_memberships VALUES('"+link+"','"+classroom+"','"+student+"','STUDENT',1,'00123',456,NULL,2)");
            q.execute("INSERT INTO classroom_intakes VALUES('"+intake+"','"+classroom+"','"+student+"','REQUEST','APPROVED','00123',1,234,'"+link+"')");
        }
        var service=new ClassroomService(new SqliteClassroomRepository(db()),id->true);
        var saved=service.own(teacher).orElseThrow(); assertEquals(7,saved.revision()); assertEquals("Message",saved.message());
        assertEquals(link,service.home(student).member().id()); assertEquals("00123",service.home(student).member().enrollment());
        assertEquals(intake,service.intakes(student,false,0).items().getFirst().id());
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db());var q=c.createStatement();var r=q.executeQuery("PRAGMA foreign_key_check")) { assertFalse(r.next()); }
        service.disband(teacher,classroom,7); assertNotEquals(classroom,service.create(teacher,"Next","").id());
        Path backup;
        try(var files=Files.list(dir.resolve("backups"))) { var list=files.toList(); assertEquals(1,list.size()); backup=list.getFirst(); }
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+backup);var q=c.createStatement();var r=q.executeQuery("PRAGMA user_version")) { assertEquals(3,r.getInt(1)); }
        Path restored=dir.resolve("restore/classroom.db"); Files.createDirectories(restored.getParent()); Files.copy(backup,restored);
        var original=new ClassroomService(new SqliteClassroomRepository(restored),id->true);
        assertEquals(saved,original.own(teacher).orElseThrow()); assertEquals(link,original.home(student).member().id());
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db());var q=c.createStatement()) {
            q.execute("PRAGMA foreign_keys=ON");
            assertThrows(SQLException.class,()->q.execute("INSERT INTO classroom_memberships VALUES('"+UUID.randomUUID()+"','missing','"+UUID.randomUUID()+"','TEACHER',1,NULL,0,NULL,0)"));
        }
    }

}
