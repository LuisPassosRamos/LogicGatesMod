package sisso.logicsim.classroom.learning;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AreaWorkQueueTest {
    @Test void heavyWorkIsSerializedBoundedAndDuplicateActorIsRejectedWithoutBlockingCaller() throws Exception {
        try(var queue=new AreaWorkQueue(8)) {
            var release=new CountDownLatch(1);var started=new CountDownLatch(1);var actor=UUID.randomUUID();var running=new AtomicInteger();var maximum=new AtomicInteger();
            var first=queue.submit(actor,()->{started.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}return 1;});
            assertTrue(started.await(3,TimeUnit.SECONDS));assertBusy(queue.submit(actor,()->2));
            var accepted=new ArrayList<CompletableFuture<Integer>>();
            for(int i=0;i<8;i++)accepted.add(queue.submit(UUID.randomUUID(),()->{maximum.accumulateAndGet(running.incrementAndGet(),Math::max);running.decrementAndGet();return 3;}));
            for(int i=0;i<20;i++)assertBusy(queue.submit(UUID.randomUUID(),()->4));
            release.countDown();assertEquals(1,first.get(3,TimeUnit.SECONDS));for(var future:accepted)assertEquals(3,future.get(3,TimeUnit.SECONDS));assertEquals(1,maximum.get());
            assertEquals(5,queue.submit(actor,()->5).get(3,TimeUnit.SECONDS));
        }
    }
    @Test void shutdownReleasesQueuedWaitersAndRejectsNewWork() throws Exception {
        var queue=new AreaWorkQueue(1);var started=new CountDownLatch(1);
        var first=queue.submit(UUID.randomUUID(),()->{started.countDown();try{new CountDownLatch(1).await();}catch(InterruptedException e){Thread.currentThread().interrupt();}return 1;});
        assertTrue(started.await(3,TimeUnit.SECONDS));var pending=queue.submit(UUID.randomUUID(),()->2);queue.close();
        assertTrue(first.isDone());assertTrue(pending.isCompletedExceptionally());assertBusy(queue.submit(UUID.randomUUID(),()->3));
    }
    private static void assertBusy(CompletableFuture<?> future) {
        var failure=assertThrows(CompletionException.class,future::join);assertInstanceOf(LearningException.class,failure.getCause());assertEquals(LearningException.Code.BUSY,((LearningException)failure.getCause()).code());
    }
}
