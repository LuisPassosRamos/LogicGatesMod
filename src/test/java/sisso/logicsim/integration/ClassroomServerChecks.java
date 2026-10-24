package sisso.logicsim.integration;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.Message;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.classroom.ClassroomRuntime;
import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomException;
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
    private final List<String> passed;
    private final Path expectedId;
    ClassroomServerChecks(List<String> passed, Path expectedId) { this.passed = passed; this.expectedId = expectedId; }

    CompletableFuture<Void> run(boolean create) {
        var permissions = PermissionsModule.get();
        var permission = Set.of(ClassroomRuntime.TEACH_PERMISSION);
        permissions.addUserPermission(OTHER, permission);
        var runtime = LogicSimPlugin.getClassroom();
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
                    && classroom.revision() == 1 && classroom.teacherId().equals(TEACHER), "classroom content/revision preserved");
            try {
                if (create) {
                    Files.createDirectories(expectedId.getParent());
                    Files.writeString(expectedId, classroom.id().toString());
                } else check(Files.readString(expectedId).equals(classroom.id().toString()), "classroom UUID survives restart");
            } catch (java.io.IOException failure) { throw new CompletionException(failure); }
            passed.add(create ? "production Classroom queue creates and edits a class using packaged native SQLite"
                    : "real server restart preserves Classroom UUID, teacher link, name, description and revision");
            return expect(ALREADY_MEMBER, runtime.create(TEACHER, "Segunda turma", ""))
                    .thenCompose(ignored -> expect(FORBIDDEN, runtime.update(OTHER, classroom.id(), classroom.revision(), "Intrusão", "")))
                    .thenCompose(ignored -> {
                        PermissionsModule.get().removeUserPermission(TEACHER, Set.of(ClassroomRuntime.TEACH_PERMISSION));
                        return expect(FORBIDDEN, runtime.update(TEACHER, classroom.id(), classroom.revision(), "Revogado", ""));
                    }).thenRun(() -> passed.add("native teacher permission revocation, classroom ownership and one-active-link policy reject unauthorized mutations"));
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
