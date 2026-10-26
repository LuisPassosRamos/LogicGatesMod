package sisso.logicsim.integration;

import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.classroom.ClassroomRuntime;
import sisso.logicsim.classroom.learning.*;
import sisso.logicsim.classroom.learning.AreaSessions.Bounds;
import sisso.logicsim.classroom.learning.LearningViews.Draft;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.world.World;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Deliberately leaves a durable copy for the runner's separate JVM restart. */
final class AreaRecoveryServerChecks {
    private static final UUID TEACHER=UUID.fromString("c1a55000-0000-4000-8000-000000000071");
    private static final UUID STUDENT=UUID.fromString("c1a55000-0000-4000-8000-000000000072");
    private static final Bounds DESTINATION=new Bounds(-16,128,-16,2,1,1);
    private final Path expected;
    private final List<String> passed;
    AreaRecoveryServerChecks(Path data,List<String> passed){expected=data.resolve("area-recovery-submission.txt");this.passed=passed;}
    CompletableFuture<Void> run(World world,boolean create) {
        var runtime=LogicSimPlugin.getClassroom();var gates=new GateServerChecks(passed);
        if(!create)return checkRecovered(world,10).thenCompose(ignored->runtime.learn(s->s.submission(STUDENT,readExpected())))
                .thenCompose(s->CompletableFuture.runAsync(()->{
                    runtime.submissionFiles().read(s.content(),s.checksum());
                    passed.add("area recovery: real JVM restart clears leased blocks when sections load, removes ledger and retains checked submission");
                }));
        PermissionsModule.get().addUserPermission(TEACHER,Set.of(ClassroomRuntime.TEACH_PERMISSION));
        var snapshot=new AreaSnapshot(1,2,1,1,List.of(new AreaSnapshot.Block(0,0,0,"Rock_Stone",0,0,null),new AreaSnapshot.Block(1,0,0,"Rock_Stone",0,0,null)));
        return runtime.create(TEACHER,"Area restart proof","")
                .thenCompose(c->runtime.remember(STUDENT,"Area student").thenCompose(ignored->runtime.request(STUDENT,c.id(),"AREA001")))
                .thenCompose(i->runtime.approve(TEACHER,i.id(),i.revision(),"AREA001"))
                .thenCompose(m->runtime.learn(s->{var a=s.create(TEACHER);a=s.save(TEACHER,a.id(),a.revision(),new Draft("Recovery","Immutable area",System.currentTimeMillis()+3600000,1000L));return new Object[]{m,s.publish(TEACHER,a.id(),a.revision())};}))
                .thenCompose(data->CompletableFuture.supplyAsync(()->{
                    var m=(sisso.logicsim.classroom.domain.ClassroomViews.Member)data[0];var a=(sisso.logicsim.classroom.learning.LearningViews.Activity)data[1];
                    var content=runtime.submissionFiles().write(snapshot);
                    return runtime.learn(s->s.submit(STUDENT,a.id(),1,m.id(),UUID.randomUUID(),content)).join();
                }))
                .thenCompose(s->runtime.learn(store->{store.reserve(TEACHER,s.id(),world.getWorldConfig().getUuid(),DESTINATION.x(),DESTINATION.y(),DESTINATION.z());return s;}))
                .thenCompose(s->gates.inWorld(world,()->{
                    NativeAreaAdapter.paste(world,snapshot,DESTINATION);
                    try{Files.writeString(expected,s.id().toString());}catch(java.io.IOException e){throw new CompletionException(e);}
                    passed.add("area recovery: content and reservation persisted before a copy retained for real restart");return (Void)null;
                })).whenComplete((ignored,failure)->PermissionsModule.get().removeUserPermission(TEACHER,Set.of(ClassroomRuntime.TEACH_PERMISSION)));
    }
    private CompletableFuture<Void> checkRecovered(World world,int attempts) {
        var runtime=LogicSimPlugin.getClassroom();LogicSimPlugin.getAreas().recoverLoaded(world);
        return new GateServerChecks(passed).afterTicks(world,()->{}).thenCompose(ignored->runtime.learn(s->s.reviews().stream().noneMatch(r->r.teacher().equals(TEACHER))))
                .thenCompose(done->{
                    if(!done && attempts>0)return checkRecovered(world,attempts-1);
                    if(!done)return CompletableFuture.failedFuture(new AssertionError("durable area not recovered"));
                    return new GateServerChecks(passed).inWorld(world,()->{NativeAreaAdapter.requireEmpty(world,DESTINATION);return null;});
                });
    }
    private UUID readExpected(){try{return UUID.fromString(Files.readString(expected).strip());}catch(java.io.IOException e){throw new CompletionException(e);}}
}
