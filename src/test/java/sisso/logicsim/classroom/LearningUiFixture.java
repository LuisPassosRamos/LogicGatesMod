package sisso.logicsim.classroom;

import sisso.logicsim.classroom.domain.ClassroomService;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Extends the frozen administrative fixture without changing the rc.9/rc.10 comparison harness. */
public final class LearningUiFixture {
    private LearningUiFixture() { }
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("Expected new output directory, teacher UUID and student UUID");
        create(Path.of(args[0]),UUID.fromString(args[1]),UUID.fromString(args[2]));
        System.out.println("Isolated administrative + learning fixture: "+Path.of(args[0]).toAbsolutePath());
    }
    public static void create(Path output,UUID teacher,UUID student) throws Exception {
        ClassroomUiFixture.create(output,teacher,student);
        var service=new ClassroomService(new SqliteClassroomRepository(output.resolve("classroom.db")),teacher::equals);
        learning(output,teacher,student,service);
        var instructions=output.resolve("README.txt");
        String text=Files.readString(instructions).replace("copying classroom.db","copying classroom.db AND submissions");
        Files.writeString(instructions,text+"\n25 publications (one archived), 3 private drafts, 25 students with submissions.\nActivity 01: 25 revisions for your student; revision 1 is graded, the latest is ungraded.\n");
    }
    private static void learning(Path output,UUID teacher,UUID student,ClassroomService service) {
        var limits=sisso.logicsim.classroom.learning.LearningLimits.DEFAULT;
        var store=new sisso.logicsim.classroom.learning.LearningStore(output.resolve("classroom.db"),teacher::equals,limits);
        var files=new sisso.logicsim.classroom.learning.SubmissionFiles(output.resolve("submissions"),limits);
        var area=new sisso.logicsim.classroom.learning.AreaSnapshot(1,3,2,1,java.util.List.of(
            new sisso.logicsim.classroom.learning.AreaSnapshot.Block(0,0,0,"Rock_Stone",0,0,null),
            new sisso.logicsim.classroom.learning.AreaSnapshot.Block(1,0,0,"Rock_Stone",0,0,null),
            new sisso.logicsim.classroom.learning.AreaSnapshot.Block(2,0,0,"Rock_Stone",0,0,null),
            new sisso.logicsim.classroom.learning.AreaSnapshot.Block(0,1,0,"Logic_Lever",0,0,true),
            new sisso.logicsim.classroom.learning.AreaSnapshot.Block(1,1,0,"Logic_Cable",0,0,null),
            new sisso.logicsim.classroom.learning.AreaSnapshot.Block(2,1,0,"Logic_Lamp",0,0,null)));
        UUID first=null;
        for(int i=1;i<=25;i++) {
            var a=store.create(teacher);a=store.save(teacher,a.id(),a.revision(),new sisso.logicsim.classroom.learning.LearningViews.Draft(
                    "Atividade %02d: título longo para conferir seleção e paginação".formatted(i),longText().repeat(4),System.currentTimeMillis()+7*86400000L,1000L));
            a=store.publish(teacher,a.id(),a.revision());if(i==1)first=a.id();if(i==25)store.archive(teacher,a.id(),a.revision(),true);
        }
        for(int i=0;i<3;i++)store.create(teacher);
        for(int i=0;i<25;i++) {
            UUID actor=i==0?student:synthetic(i-1);UUID membership=service.home(actor).member().id();
            var submitted=store.submit(actor,first,1,membership,UUID.randomUUID(),files.write(area));
            if(i==0)store.grade(teacher,submitted.id(),0,700,"Feedback da revisão 1, preservado no histórico.");
        }
        for(int i=0;i<24;i++)store.submit(student,first,1,service.home(student).member().id(),UUID.randomUUID(),files.write(area));
    }
    private static UUID synthetic(int number) { return UUID.nameUUIDFromBytes(("LogicGates Classroom UI fixture "+number).getBytes(StandardCharsets.UTF_8)); }
    private static String longText() { return "Enunciado literal com acentos, %server.fake e <b>sem markup</b>. Confira rolagem e ações.\n".repeat(32).substring(0,2048); }
}
