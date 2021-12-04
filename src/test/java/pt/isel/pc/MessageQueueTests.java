package pt.isel.pc;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isel.pc.s1.MessageBox;
import pt.isel.pc.s2.MessageQueue;
import pt.isel.pc.utils.TestHelper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class MessageQueueTests {

    private static final int N_OF_THREADS = 15;
    private static final Duration TEST_DURATION = Duration.ofSeconds(1000);
    private static final Logger log = LoggerFactory.getLogger(MessageBox.class);

    @Test
    public void firstTest() throws InterruptedException{
        MessageQueue queue = new MessageQueue();
        TestHelper helper = new TestHelper(TEST_DURATION);
        AtomicInteger numOfThreads = new AtomicInteger();
        helper.createAndStart(1, (ignore, isDone) -> {
            log.info("first");
            queue.enqueue("Primeiro");
        });
        Thread.sleep(100);
        helper.createAndStart(2, (ignore, isDone) -> {
            log.info("second");
            queue.enqueue("Segundo");
        });
        Thread.sleep(100);
        helper.createAndStart(3, (ignore, isDone) -> {
            log.info("third");
            System.out.println(queue.dequeue(10));
        });
        Thread.sleep(100);
        helper.createAndStart(4, (ignore, isDone) -> {
            log.info("forth");
            System.out.println(queue.dequeue(10));
        });
        Thread.sleep(100);
        helper.createAndStart(5, (ignore, isDone) -> {
            log.info("fifth");
            var message = queue.dequeue(1);
            log.info("Did not timeout: " + message);
        });
        Thread.sleep(1000);
        helper.createAndStart(6, (ignore, isDone) -> {
            log.info("break");
            queue.enqueue("testString");
        });
        helper.join();
        log.info("Number of Threads:" + numOfThreads);
        assertEquals(0,numOfThreads.get());
    }
}
