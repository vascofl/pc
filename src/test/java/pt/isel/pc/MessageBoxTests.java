package pt.isel.pc;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isel.pc.s1.MessageBox;
import pt.isel.pc.utils.TestHelper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class MessageBoxTests {

    private static final int N_OF_THREADS = 15;
    private static final Duration TEST_DURATION = Duration.ofSeconds(20);
    private static final Logger log = LoggerFactory.getLogger(MessageBox.class);

    @Test
    public void firstTest() throws InterruptedException{
        MessageBox box = new MessageBox();
        TestHelper helper = new TestHelper(TEST_DURATION);
        AtomicInteger numOfThreads = new AtomicInteger();
        helper.createAndStart(1, (ignore, isDone) -> {
                log.info("first");
                box.waitForMessage(Long.MAX_VALUE);
        });
        helper.createAndStart(2, (ignore, isDone) -> {
                log.info("second");
                box.waitForMessage(Long.MAX_VALUE);
        });
        helper.createAndStart(2, (ignore, isDone) -> {
                log.info("third");
                box.waitForMessage(Long.MAX_VALUE);
        });
        Thread.sleep(1000);
        helper.createAndStart(3,(ignore, isDone) -> {
            log.info("final");
            numOfThreads.set(box.sendToAll("MENSAGEM"));
        });
        helper.join();
        log.info("Number of Threads:" + numOfThreads);
        assertEquals(3,numOfThreads.get());
    }

    @Test
    public void secondTest() throws InterruptedException{
        MessageBox box = new MessageBox();
        TestHelper helper = new TestHelper(TEST_DURATION);
        AtomicInteger numOfThreads = new AtomicInteger();
        helper.createAndStart(1, (ignore, isDone) -> {
                log.info("first");
                box.waitForMessage(Long.MAX_VALUE);
        });
        helper.createAndStart(2, (ignore, isDone) -> {
                log.info("second");
                box.waitForMessage(Long.MAX_VALUE);
        });
        Thread.sleep(1000);
        helper.createAndStart(2,(ignore, isDone) -> {
            log.info("final");
            numOfThreads.set(box.sendToAll("MENSAGEM"));
        });
        log.info("Number of Threads:" + numOfThreads);
        try {
            helper.interruptAndJoin();
        } catch (InterruptedException e){
            assertNotNull(e);
        }
        //assertEquals(3,numOfThreads.get());
    }


}
