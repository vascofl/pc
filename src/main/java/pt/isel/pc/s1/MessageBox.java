package pt.isel.pc.s1;

import pt.isel.pc.utils.NodeLinkedList;
import pt.isel.pc.utils.Timeouts;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageBox<T>{

    private static class Request<T> {
        public T message = null;
        public final Condition condition;

        public Request(Lock lock) {
            condition = lock.newCondition();
        }
    }
    private final Lock monitor = new ReentrantLock();
    private final NodeLinkedList<Request<T>> requests = new NodeLinkedList<>();

    public Optional<T> waitForMessage(long timeout) throws InterruptedException {
        monitor.lock();
        try {
            if (Timeouts.noWait(timeout)) {
                return Optional.empty();
            }
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            var myrequest = requests.enqueue(new Request<>(monitor));
            while (true) {
                // 4. wait
                try {
                    myrequest.value.condition.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if(myrequest.value.message != null) {
                        Thread.currentThread().interrupt();
                        return Optional.of(myrequest.value.message);
                    }
                    requests.remove(myrequest);
                    throw e;
                }
                if(myrequest.value.message != null) {
                    return Optional.of(myrequest.value.message);
                }
                // 6. compute new remaining time
                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    // 7. if already at or after deadline, complete with a failure
                    requests.remove(myrequest);
                    return Optional.empty();
                }
            }
            } finally {
            monitor.unlock();
        }
    }

    public int sendToAll(T message){
        int threadCounter = 0;
        monitor.lock();
        try{
            while(requests.isNotEmpty()) {
                threadCounter++;
                var request = requests.pull();
                request.value.message = message;
                request.value.condition.signal();
            }
        }finally{
            monitor.unlock();
        }
        return threadCounter;
    }

}
