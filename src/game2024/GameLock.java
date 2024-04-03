package game2024;

import java.util.concurrent.locks.Lock;

public class GameLock {

    private boolean locked;

    public GameLock() {
        this.locked = false;
    }

    public synchronized void lock() throws InterruptedException {
        while(locked) {
            wait();
        }
        locked = true;
    }

    public synchronized void unlock() {
        locked = false;
        notify();
    }

}
