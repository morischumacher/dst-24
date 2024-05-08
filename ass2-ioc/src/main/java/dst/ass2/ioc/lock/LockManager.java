package dst.ass2.ioc.lock;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {

    private static LockManager instance;
    private final HashMap<String, Lock> locks;

    private LockManager() {
        this.locks = new HashMap<>();
    }

    public static synchronized LockManager getInstance() {
        if (instance == null) {
            instance = new LockManager();
        }
        return instance;
    }

    public synchronized Lock getLock(String name) {
        if (!locks.containsKey(name)) {
            locks.put(name, createLock());
        }
        return locks.get(name);
    }

    private Lock createLock() {
        return new ReentrantLock();
    }
}