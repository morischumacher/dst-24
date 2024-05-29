package dst.ass2.ioc.lock;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {

    private static LockManager instance;

    // Storing the actual locks.
    private final HashMap<String, Lock> locks;

    private LockManager() {
        this.locks = new HashMap<>();
    }

    // Get the instance of the lock manager, synchronized to avoid multiple instances.
    public static synchronized LockManager getInstance() {
        if (instance == null) {
            instance = new LockManager();
        }
        return instance;
    }

    // Get the lock by name, if not present create a new lock.
    public synchronized Lock getLock(String name) {
        if (!locks.containsKey(name)) {
            locks.put(name, createLock());
        }
        return locks.get(name);
    }

    // Create new lock.
    private Lock createLock() {
        // Create a new default lock (lock/unlock).
        return new ReentrantLock();
    }
}