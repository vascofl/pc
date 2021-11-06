package pt.isel.pc;

import pt.isel.pc.utils.NodeLinkedList;
import pt.isel.pc.utils.Timeouts;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingMessageQueue<E> {

    public static class EnqueueRequest<E> {
        final E message;
        boolean isDone;
        Condition condition;
        public EnqueueRequest(E message, Condition condition) {
            this.message = message;
            this.condition = condition;
        }
    }

    private final NodeLinkedList<EnqueueRequest<E>> enqueueRequests = new NodeLinkedList<>();
    private final LinkedList<E> messages;
    private final Lock lock = new ReentrantLock();
    private int capacity;
    private Condition full;
    private Condition empty;

    public BlockingMessageQueue(int capacity) {
        this.capacity = capacity;
        messages = new LinkedList<>();
        empty = lock.newCondition();
        full = lock.newCondition();
    }

    public boolean enqueue(E message, long timeout) throws InterruptedException {
        lock.lock();
        try{
            if(Timeouts.noWait(timeout)){
                return false;
            }
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            // fast-path
            while (true){
                try{
                    while(messages.size() == capacity){
                        empty.await(remaining, TimeUnit.MILLISECONDS);
                    }
                    remaining = Timeouts.remainingUntil(deadline);
                    if(Timeouts.isTimeout(remaining)) {
                        return false;
                    }
                    messages.add(message);
                    full.signal();
                    return true;
                } catch (InterruptedException e) {
                    if (messages.size() < capacity)
                        empty.signal();
                    throw e;
                }
            }
        }finally {
            lock.unlock();
        }
    }

    public Future<E> dequeue() {
        lock.lock();
        try {
            if (messages.size() > 0) {
                E message = messages.remove();
                empty.signal();
                return new CompletedFuture(message);
            }

            do {
                try {
                    full.await();
                    if (messages.size() > 0) {
                        E message = messages.remove();
                        empty.signal();
                        return new CompletedFuture(message);
                    }
                }
                catch(InterruptedException e) {
                    if (messages.size() > 0)
                        empty.signal();
                }
            }
            while (true);
        }
        finally {
            lock.unlock();
        }    }


    /**
     * A Future that is already completed when created.
     */
    public class CompletedFuture implements Future<E> {

        private final E e;

        CompletedFuture(E e) {
            this.e = e;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public E get() {
            return e;
        }

        @Override
        public E get(long timeout, TimeUnit unit) {
            return e;
        }
    }
}
