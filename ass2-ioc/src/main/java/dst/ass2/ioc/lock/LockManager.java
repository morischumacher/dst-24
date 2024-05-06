package dst.ass2.ioc.lock;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
//for recursion
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {

    private static LockManager instance;

    public synchronized static LockManager getInstance() {
        if (instance == null) {
            instance = new LockManager();
        }
        return instance;
    }

    private HashMap<String, Lock> locks = new HashMap<>();

    public synchronized Lock getLock(String name) {
        var lock = locks.get(name);
        if (lock != null) return lock;

        lock = new ReentrantLock();
        locks.put(name, lock);
        return lock;
    }
}
