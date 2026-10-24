package sisso.logicsim.integration;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.Message;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.classroom.ClassroomRuntime;
import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.classroom.domain.ClassroomViews.State;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.nio.file.Files;
import java.nio.file.Path;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

/** Uses the production plugin's I/O queue, packaged JDBC and native permission provider. */
final class ClassroomServerChecks {
    private static final UUID TEACHER = UUID.fromString("c1a55000-0000-4000-8000-000000000001");
    private static final UUID OTHER = UUID.fromString("c1a55000-0000-4000-8000-000000000002");
    private static final UUID STUDENT = UUID.fromString("c1a55000-0000-4000-8000-000000000003");
    private static final UUID INVITED = UUID.fromString("c1a55000-0000-4000-8000-000000000004");
    private final List<String> passed;
    private final Path expectedId;
    ClassroomServerChecks(List<String> passed, Path expectedId) { this.passed = passed; this.expectedId = expectedId; }

    CompletableFuture<Void> run(boolean create) {
        var permissions = PermissionsModule.get();
        var permission = Set.of(ClassroomRuntime.TEACH_PERMISSION);
        permissions.addUserPermission(OTHER, permission);
        var runtime = LogicSimPlugin.getClassroom();
        authorizationEvents(runtime);
        return CommandManager.get().handleCommand(new Console(), "perm user add " + TEACHER + " " + ClassroomRuntime.TEACH_PERMISSION)
                .thenCompose(ignored -> {
                    check(permissions.hasPermission(TEACHER, ClassroomRuntime.TEACH_PERMISSION, false), "native teacher permission command");
                    passed.add("native perm user add command grants the dedicated Classroom teacher permission");
                    return authorized(runtime, create);
                }).whenComplete((value, failure) -> {
                    permissions.removeUserPermission(TEACHER, permission);
                    permissions.removeUserPermission(OTHER, permission);
                });
    }

    private void authorizationEvents(ClassroomRuntime runtime) {
        UUID subject=UUID.fromString("c1a55000-0000-4000-8000-000000000005");
        String group="LogicGatesClassroomIntegration";
        var permissions=PermissionsModule.get(); var teacherPermission=Set.of(ClassroomRuntime.TEACH_PERMISSION);
        var values=new java.util.concurrent.CopyOnWriteArrayList<Boolean>();
        var data=new java.util.concurrent.atomic.AtomicInteger();
        try(var subscription=runtime.listenAuthorization(subject,values::add);
            var unrelated=runtime.listen(null,change->data.incrementAndGet())) {
            permissions.addUserPermission(subject,teacherPermission);
            permissions.removeUserPermission(subject,teacherPermission);
            permissions.addUserToGroup(subject,"hytale:Admin");
            permissions.removeUserFromGroup(subject,"hytale:Admin");
            permissions.addUserToGroup(subject,group);
            permissions.addGroupPermission(group,teacherPermission);
            permissions.removeGroupPermission(group,teacherPermission);
            check(values.equals(List.of(true,false,true,false,true,false)),"native permission/OP/group events refresh only effective authorization transitions");
            check(data.get()==0,"permission invalidations do not publish data/MOTD changes");
            passed.add("native individual permission, OP and group permission events reach production authorization listeners without data or MOTD notifications");
        } catch(Exception failure) { throw new CompletionException(failure); }
        finally {
            permissions.removeUserPermission(subject,teacherPermission);
            permissions.removeUserFromGroup(subject,"hytale:Admin");
            permissions.removeUserFromGroup(subject,group);
            permissions.removeGroupPermission(group,teacherPermission);
        }
    }

    private CompletableFuture<Void> authorized(ClassroomRuntime runtime, boolean create) {
        CompletableFuture<Classroom> preparation;
        if (create) {
            preparation = runtime.create(TEACHER, "Turma nativa", "Descrição literal %server.fake")
                    .thenCompose(value -> runtime.update(TEACHER, value.id(), value.revision(), "Turma persistida", "Descrição\ncom acento"));
        } else {
            preparation = runtime.own(TEACHER).thenApply(value -> value.orElseThrow());
        }
        return preparation.thenCompose(classroom -> {
            check(classroom.name().equals("Turma persistida") && classroom.description().equals("Descrição\ncom acento")
                    && classroom.revision() == (create ? 1 : 2) && classroom.teacherId().equals(TEACHER), "classroom content/revision preserved");
            try {
                if (create) {
                    Files.createDirectories(expectedId.getParent());
                    Files.writeString(expectedId, classroom.id().toString());
                } else check(Files.readString(expectedId).equals(classroom.id().toString()), "classroom UUID survives restart");
            } catch (java.io.IOException failure) { throw new CompletionException(failure); }
            passed.add(create ? "production Classroom queue creates and edits a class using packaged native SQLite"
                    : "real server restart preserves Classroom UUID, teacher link, name, description and revision");
            return admissions(runtime,classroom,create)
                    .thenCompose(ignored -> expect(ALREADY_MEMBER, runtime.create(TEACHER, "Segunda turma", "")))
                    .thenCompose(ignored -> expect(FORBIDDEN, runtime.update(OTHER, classroom.id(), classroom.revision(), "Intrusão", "")))
                    .thenCompose(ignored -> {
                        PermissionsModule.get().removeUserPermission(TEACHER, Set.of(ClassroomRuntime.TEACH_PERMISSION));
                        return expect(FORBIDDEN, runtime.update(TEACHER, classroom.id(), classroom.revision(), "Revogado", ""));
                    }).thenRun(() -> passed.add("native teacher permission revocation, classroom ownership and one-active-link policy reject unauthorized mutations"));
        });
    }

    private CompletableFuture<Void> admissions(ClassroomRuntime runtime,Classroom classroom,boolean create) {
        if (create) {
            return runtime.remember(STUDENT,"Aluno integração")
                .thenCompose(ignored->runtime.remember(INVITED,"Aluno offline"))
                .thenCompose(ignored->runtime.request(STUDENT,classroom.id(),"00123"))
                .thenCompose(request->runtime.home(STUDENT).thenCompose(home->{
                    check(home.member()==null,"request must await teacher approval");
                    return runtime.approve(TEACHER,request.id(),request.revision(),"00123");
                }))
                .thenCompose(member->{
                    check(member.enrollment().equals("00123"),"text enrollment preserves leading zeroes");
                    return runtime.invite(TEACHER,INVITED);
                })
                .thenCompose(invite->{
                    check(invite.state()==State.INVITED,"offline invitation remains pending");
                    return runtime.message(TEACHER,classroom.id(),classroom.revision(),"Bem-vindos %server.fake");
                }).thenAccept(value->passed.add("production Classroom request requires approval, preserves 00123 and persists an offline invitation and literal message"));
        }
        return runtime.home(STUDENT).thenCompose(home->{
            check(home.member()!=null && home.member().enrollment().equals("00123"),"student link survives restart");
            check(home.classroom().message().equals("Bem-vindos %server.fake"),"literal message survives restart");
            return runtime.intakes(INVITED,false,0);
        }).thenCompose(page->{
            var invite=page.items().getFirst(); check(invite.state()==State.INVITED,"invitation survives full restart");
            return runtime.accept(INVITED,invite.id(),invite.revision(),"00007");
        }).thenCompose(request->runtime.home(INVITED).thenCompose(home->{
            check(home.member()==null,"accepting an invitation must still await teacher approval");
            return runtime.approve(TEACHER,request.id(),request.revision(),"00007");
        })).thenCompose(member->runtime.correct(TEACHER,member.id(),member.revision(),"00008"))
          .thenCompose(member->runtime.end(INVITED,member.id(),member.revision(),false))
          .thenCompose(ended->{
              check(!ended.active() && ended.endedAt()!=null,"leaving retains historical membership");
              return runtime.request(INVITED,classroom.id(),"00008").thenCompose(request->runtime.approve(TEACHER,request.id(),request.revision(),"00008"))
                  .thenCompose(joined->{
                      check(!joined.id().equals(ended.id()),"reentry creates a new membership identity");
                      return runtime.end(TEACHER,joined.id(),joined.revision(),true);
                  });
          }).thenCompose(removed->runtime.home(INVITED)).thenAccept(home->{
              check(home.member()==null,"teacher removal ends active membership");
              passed.add("real restart preserves student enrollment, invitation and message; invite acceptance awaits approval, correction/leave/reentry/removal retain history");
          });
    }

    private CompletableFuture<Void> expect(ClassroomException.Reason reason, CompletableFuture<?> operation) {
        return operation.handle((value, failure) -> {
            while (failure instanceof CompletionException && failure.getCause() != null) failure = failure.getCause();
            check(failure instanceof ClassroomException error && error.reason() == reason, "expected Classroom error " + reason);
            return null;
        });
    }
    private void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static final class Console implements CommandSender {
        public String getUsername() { return "ClassroomIntegration"; }
        public UUID getUuid() { return new UUID(0, 0); }
        public boolean hasPermission(String permission) { return true; }
        public boolean hasPermission(String permission, boolean fallback) { return true; }
        public void sendMessage(Message message) { }
    }
}
