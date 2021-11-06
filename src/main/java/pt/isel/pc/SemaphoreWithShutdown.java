package pt.isel.pc;

import pt.isel.pc.utils.NodeLinkedList;
import pt.isel.pc.utils.Timeouts;
import java.util.concurrent.CancellationException;

public class SemaphoreWithShutdown {

    private final Object monitor = new Object();
    //mudar para o quê? Lista de objects? T? Objeto Request? Não preciso do value do node
    private final NodeLinkedList<Integer> requests = new NodeLinkedList<>();
    private int units;
    private final int initialUnits;
    private boolean shutdown = false;

    public SemaphoreWithShutdown(int initialUnits) {
        this.initialUnits = units = initialUnits;
    }

    public boolean acquireSingle(long timeout) throws InterruptedException, CancellationException {
        synchronized (monitor) {
            if (shutdown) {
                if (requests.isNotEmpty())
                    monitor.notifyAll();
                throw new CancellationException();
            }
            // 1. fast-path
            if (requests.isEmpty() && units > 0) {
                units -= 1;
                return true;
            }
            // 2. should wait or complete immediately with a failure
            if (Timeouts.noWait(timeout)) {
                return false;
            }

            // 3. wait-path
            // - compute wait deadline and current remaining
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            NodeLinkedList.Node<Integer> node = requests.enqueue(1);
            while (true) {
                // 4. wait
                try {
                    monitor.wait(remaining);
                } catch (InterruptedException e) {
                    requests.remove(node);
                    notifyAllIfNeeded();
                    throw e;
                }
                if (shutdown) {
                    requests.remove(node);
                    throw new CancellationException();
                }
                // 5. is the condition true?
                if (requests.isHeadNode(node) && units > 0) {
                    units -= 1;
                    requests.remove(node);
                    notifyAllIfNeeded();
                    return true;
                }
                // 6. compute new remaining time
                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    // 7. if already at or after deadline, complete with a failure
                    requests.remove(node);
                    notifyAllIfNeeded();
                    return false;
                }
            }
        }
    }

    public void releaseSingle() {
        synchronized (monitor) {
            units += 1;
            notifyAllIfNeeded();
        }
    }

    public void startShutdown() {
        synchronized (monitor) {
            shutdown = true;
            if(requests.isNotEmpty())
                monitor.notifyAll();
        }
    }

    public boolean waitShutdownCompleted(long timeout) throws InterruptedException {
        synchronized (monitor) {
            // fast-path
            if(units == initialUnits) {
                return true;
            }
            if(Timeouts.noWait(timeout)) {
                return false;
            }
            // wait-path
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            while(true) {
                try {
                    monitor.wait(remaining);
                } catch (InterruptedException e) {
                    notifyAllIfNeeded();
                    throw e;
                }
                if(units == initialUnits) {
                    return true;
                }
                remaining = Timeouts.remainingUntil(deadline);
                if(Timeouts.isTimeout(remaining)) {
                    return false;
                }
            }
        }
    }


    private void notifyAllIfNeeded() {
        if(requests.isNotEmpty() && units > 0) {
            monitor.notifyAll();
        }
    }

}
