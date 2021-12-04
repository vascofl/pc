package pt.isel.pc.s2;

public abstract class Ex1 <V>{

    public class Holder {
        V data1, data2;
        boolean done;

        Holder(V first) {
            data1 = first;
        }
    }

    public V exchange(V data) throws InterruptedException{
        return null;
    }
}

class MakeString implements Runnable {
    Ex1<String> exchanger;
    String str;

    MakeString(Ex1<String> exchanger)
    {
        this.exchanger = exchanger;
        str = new String();

        new Thread(this).start();
    }

    public void run()
    {
        char ch = 'A';
        try {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 5; j++) {
                    str += ch++;
                }
                // Exchange a full buffer for an empty one
                str = exchanger.exchange(str);
            }
        }
        catch (InterruptedException e) {
            System.out.println(e);
        }
    }
}

// A thread that uses a string
class UseString implements Runnable {

    Ex1<String> exchanger;
    String str;

    UseString(Ex1<String> exchanger) {
        this.exchanger = exchanger;

        new Thread(this).start();
    }

    public void run() {
        try {
            for (int i = 0; i < 3; i++) {

                // Exchange an empty buffer for a full one
                str = exchanger.exchange(new String());
                System.out.println("Got: " + str);
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }
}

