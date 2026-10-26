package sisso.logicsim.classroom.learning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sisso.logicsim.classroom.ClassroomRuntime;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AreaRuntimeTest {
    @TempDir Path dir;
    @Test void storageFailureDisablesAreaOperationsWithoutBlockingUnrelatedWorldEdits() throws Exception {
        Files.writeString(dir.resolve("classroom.db"),"invalid database");var failures=new CopyOnWriteArrayList<Throwable>();
        try(var runtime=new ClassroomRuntime(dir,id->false,failures::add);var areas=new AreaRuntime(runtime)) {
            assertThrows(ExecutionException.class,()->areas.ready().get(5,TimeUnit.SECONDS));assertFalse(failures.isEmpty());
            assertTrue(areas.allows(UUID.randomUUID(),UUID.randomUUID(),0,64,0));
        }
    }
}
