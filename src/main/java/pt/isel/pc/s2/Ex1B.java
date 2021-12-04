package pt.isel.pc.s2;

public class Ex1B<V> extends Ex1<V>{


    private volatile Holder ex = null;

    public V exchange(V data){
        if (ex == null) {
            Holder holder = new Holder(data);
            ex = holder;
            while (!holder.done)
                Thread.yield();
            return holder.data2;
        } else {
            Holder holder = ex;
            ex = null;
            holder.data2 = data;
            holder.done = true;
            return holder.data1;
        }
    }

    public static void main(String[] args) {
        Ex1B<String> exchanger = new Ex1B<>();

        new UseString(exchanger);
        new MakeString(exchanger);
    }
}

