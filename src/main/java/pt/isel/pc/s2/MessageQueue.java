package pt.isel.pc.s2;

import pt.isel.pc.utils.Timeouts;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MessageQueue<E> {

    private static class Node<E> {
        //volatile?
        final E message;
        final AtomicReference<Node<E>> next = new AtomicReference<>();

        Node(E value) {
            this.message = value;
        }
    }

    private final AtomicReference<Node<E>> head;
    private final AtomicReference<Node<E>> tail;

    public MessageQueue() {
        Node<E> node = new Node<E>(null);
        head = new AtomicReference<>(node);
        tail = new AtomicReference<>(node);
    }

    public void enqueue(E message) {
        Node<E> node = new Node<>(message);
        while (true) {
            Node<E> currTail = tail.get();
            Node<E> nextTail = currTail.next.get();
            if (nextTail == null) {
                if (currTail.next.compareAndSet(null, node)) {
                    tail.compareAndSet(currTail, node);
                    return;
                }
            } else {
                tail.compareAndSet(currTail, nextTail);
            }
        }
    }

    public Optional<E> dequeue(long timeout) throws InterruptedException {
        if (Timeouts.noWait(timeout)) {
            return Optional.empty();
        }
        long deadline = Timeouts.deadlineFor(timeout);
        long remaining = Timeouts.remainingUntil(deadline);
        while (true) {
            Node<E> currentHead = head.get();
            Node<E> currentTail = tail.get();
            Node<E> next = currentHead.next.get();
            if (currentHead == head.get()) {
                if (currentHead == currentTail) {
                    while (currentHead.next.get() == null && !Timeouts.isTimeout(remaining)) {
                        Thread.yield();
                    }
                    if (Timeouts.isTimeout(remaining))
                        return Optional.empty();
                    next = currentHead.next.get();
                    tail.compareAndSet(currentTail, next);
                } else {
                    if (head.compareAndSet(currentHead, next)) {
                        return Optional.of(next.message);
                    }
                }
            }
        }
    }
}




