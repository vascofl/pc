package pt.isel.pc.s2;

import java.util.concurrent.atomic.AtomicInteger;

public class Ex2<M> {

    private class MsgHolder {
        final M msg;
        AtomicInteger lives;

        public MsgHolder(M msg, AtomicInteger lives) {
            this.msg = msg;
            this.lives = lives;
        }
    }

    private volatile MsgHolder msgHolder = null;

    public void Publish(M m, AtomicInteger lvs) {
        msgHolder = new MsgHolder(m, lvs);
    }

    public M TryConsume() {
        while(true) {
            var observedHolder = msgHolder;
            if (observedHolder != null) {
                var observedLives = observedHolder.lives.get();
                if (observedLives > 0) {
                    if (msgHolder.lives.compareAndSet(observedLives, observedLives - 1))
                        return msgHolder.msg;
                }
            }
        }
    }
}


