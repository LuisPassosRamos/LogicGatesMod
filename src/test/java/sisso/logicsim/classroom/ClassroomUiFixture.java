package sisso.logicsim.classroom;

import sisso.logicsim.classroom.domain.ClassroomService;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Test-only fixture, never packaged in the mod. Refuses an existing destination; uses production policies. */
public final class ClassroomUiFixture {
    private ClassroomUiFixture() { }
    public static void main(String[] args) throws Exception {
        if (args.length!=3) throw new IllegalArgumentException("Expected new output directory, teacher UUID and student UUID");
        Path output=Path.of(args[0]); UUID teacher=UUID.fromString(args[1]), student=UUID.fromString(args[2]);
        create(output,teacher,student);
        System.out.println("Isolated Classroom UI fixture: "+output.toAbsolutePath());
        System.out.println("25 members, 25 pending requests, 25 known eligible profiles and student history. Never copy into your current server.");
    }
    public static void create(Path output,UUID teacher,UUID student) throws Exception {
        if (teacher.equals(student)) throw new IllegalArgumentException("Teacher and student must be different accounts");
        // Validate all identities before creating anything. Never modify an existing directory or database.
        for(int i=0;i<74;i++) if (synthetic(i).equals(teacher) || synthetic(i).equals(student)) throw new IllegalArgumentException("Account overlaps a synthetic fixture identity");
        Files.createDirectories(output.toAbsolutePath().getParent()); Files.createDirectory(output);
        var service=new ClassroomService(new SqliteClassroomRepository(output.resolve("classroom.db")),teacher::equals);
        service.remember(teacher,"Professor de teste"); service.remember(student,"Aluno de teste");
        var classroom=service.create(teacher,"Turma de teste: nomes longos e paginação da interface Classroom",longText());
        service.settings(teacher,classroom.id(),classroom.revision(),classroom.name(),classroom.description(),longText());
        for(int i=0;i<25;i++) {
            var request=service.request(student,classroom.id(),"00123");
            service.closeIntake(student,request.id(),request.revision(),false);
        }
        for(int i=0;i<25;i++) {
            UUID player=i==0?student:synthetic(i-1);
            service.remember(player,"Aluno %02d com nome para conferir a ficha".formatted(i+1));
            var request=service.request(player,classroom.id(),i==0?"00123":"%05d".formatted(i+1));
            service.approve(teacher,request.id(),request.revision(),request.enrollment());
        }
        for(int i=24;i<49;i++) {
            UUID player=synthetic(i); service.remember(player,"Pendente %02d".formatted(i-23));
            service.request(player,classroom.id(),"P%04d".formatted(i));
        }
        for(int i=49;i<74;i++) service.remember(synthetic(i),"Disponível %02d".formatted(i-48));
        Files.writeString(output.resolve("README.txt"),"""
                Synthetic UI fixture. Use ONLY in a separate test server/save with an independent plugin data directory.
                Stop that test server before copying classroom.db to the data directory printed by its log.
                Do not replace a database belonging to your real world. No licensed assets or worlds are included.
                Teacher UUID: %s
                Student UUID: %s
                Grant the teacher in that test server: perm user add %s sisso.logicgates.classroom.teach
                Opening Classroom as the student shows the linked class; leave it to inspect paginated history.
                """.formatted(teacher,student,teacher),StandardCharsets.UTF_8);
    }
    private static UUID synthetic(int number) { return UUID.nameUUIDFromBytes(("LogicGates Classroom UI fixture "+number).getBytes(StandardCharsets.UTF_8)); }
    private static String longText() {
        return "Texto literal com acentos, %server.fake e <b>sem markup</b>. Confira a rolagem e os botões fixos.\n".repeat(24).substring(0,2048);
    }
}
