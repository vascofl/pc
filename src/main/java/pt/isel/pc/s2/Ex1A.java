package pt.isel.pc.s2;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ex1A<V> extends Ex1<V>{

    private Holder ex = null;
    private final Lock lock = new ReentrantLock();
    Condition condition = lock.newCondition();

    public V exchange(V data) throws InterruptedException{
        lock.lock();
        Holder holder = null;
        try {
            if (ex == null) {
                holder = new Holder(data);
                ex = holder;
                while (!holder.done)
                    condition.await();
                return holder.data2;
            } else {
                holder = ex;
                ex = null;
                holder.data2 = data;
                holder.done = true;
                condition.signal();
                return holder.data1;
            }
        } catch (InterruptedException e){
            if (holder.done){
                Thread.currentThread().interrupt();
                return holder.data2;
            }
            ex = null;
            throw e;
        }
        finally {
            lock.unlock();
        }
    }

    //public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException { }

    public static void main(String[] args) {
        Ex1A<String> exchanger = new Ex1A<>();

        new MakeString(exchanger);
        new UseString(exchanger);
    }
}