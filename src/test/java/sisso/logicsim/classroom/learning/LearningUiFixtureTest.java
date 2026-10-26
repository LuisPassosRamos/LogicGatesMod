package sisso.logicsim.classroom.learning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sisso.logicsim.classroom.LearningUiFixture;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LearningUiFixtureTest {
    @TempDir Path dir;
    @Test void isolatedFixtureSupportsAllThreePaginatedListsAndKeepsUngradedLatestRevision() throws Exception {
        var teacher=UUID.randomUUID();var student=UUID.randomUUID();var output=dir.resolve("new-fixture");LearningUiFixture.create(output,teacher,student);
        var store=new LearningStore(output.resolve("classroom.db"),teacher::equals,LearningLimits.DEFAULT);
        assertEquals(20,store.activities(teacher,LearningStore.Filter.ALL,0).items().size());
        assertEquals(8,store.activities(teacher,LearningStore.Filter.ALL,1).items().size());
        assertEquals(4,store.activities(student,LearningStore.Filter.ALL,1).items().size());
        var activity=store.activities(student,LearningStore.Filter.ALL,0).items().getFirst();
        assertEquals(20,store.students(teacher,activity.id(),0).items().size());assertEquals(5,store.students(teacher,activity.id(),1).items().size());
        var revisions=store.submissions(student,activity.id(),student,0);assertEquals(20,revisions.items().size());assertNull(revisions.items().getFirst().assessment());
        var older=store.submissions(student,activity.id(),student,1);assertEquals(5,older.items().size());assertEquals(700,older.items().getLast().assessment().score());
        var files=new SubmissionFiles(output.resolve("submissions"),LearningLimits.DEFAULT);var latest=revisions.items().getFirst();assertEquals(6,files.read(latest.content(),latest.checksum()).blocks().size());
        assertThrows(FileAlreadyExistsException.class,()->LearningUiFixture.create(output,teacher,student));
    }
}
