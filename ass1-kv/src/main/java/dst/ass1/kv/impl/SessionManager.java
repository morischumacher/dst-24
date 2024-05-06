package dst.ass1.kv.impl;

import dst.ass1.kv.ISessionManager;
import dst.ass1.kv.SessionCreationFailedException;
import dst.ass1.kv.SessionNotFoundException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.UUID;
import java.util.function.Function;

public class SessionManager implements ISessionManager {
    private final JedisPool pool;

    public SessionManager(String host, String port) {
        this.pool = new JedisPool(host, Integer.parseInt(port));
    }

    private <T> T executeOperation(Function<Jedis, T> operation) {
        try (Jedis jedis = pool.getResource()) {
            return operation.apply(jedis);
        }
    }

    @Override
    public String createSession(Long userId, int timeToLive) throws SessionCreationFailedException {
        try {
            return executeOperation(jedis -> {
                String sessionId = UUID.randomUUID().toString();
                Transaction transaction = jedis.multi();

                // Add session details
                transaction.hset(sessionId, "userId", String.valueOf(userId));
                transaction.hset(sessionId, "timeToLive", String.valueOf(timeToLive));
                transaction.expire(sessionId, timeToLive);

                // User directory to have mapping between userId and sessionId
                transaction.hset("directory", String.valueOf(userId), sessionId);

                transaction.exec();

                return sessionId;
            });
        } catch (Exception e) {
            throw new SessionCreationFailedException(e.getMessage());
        }
    }

    @Override
    public void setSessionVariable(String sessionId, String key, String value) throws SessionNotFoundException {
        Object result = executeOperation(jedis -> {
            if (!jedis.exists(sessionId)) {
                return new SessionNotFoundException("SessionId does not exist (" + sessionId + ")");
            }
            Transaction transaction = jedis.multi();
            transaction.hset(sessionId, key, value);
            transaction.exec();
            return true;
        });

        if (result instanceof SessionNotFoundException) {
            throw (SessionNotFoundException) result;
        }
    }

    @Override
    public String getSessionVariable(String sessionId, String key) throws SessionNotFoundException {
        Object result = executeOperation(jedis -> {
            if (!jedis.exists(sessionId)) {
                return new SessionNotFoundException("SessionId does not exist (" + sessionId + ")");
            }
            if (!jedis.hexists(sessionId, key)) {
                return null;
            }

            return jedis.hget(sessionId, key);
        });

        if (result instanceof SessionNotFoundException) {
            throw (SessionNotFoundException) result;
        }

        return (String) result;
    }

    @Override
    public Long getUserId(String sessionId) throws SessionNotFoundException {
        return Long.valueOf(getSessionVariable(sessionId, "userId"));
    }

    @Override
    public int getTimeToLive(String sessionId) throws SessionNotFoundException {
        return Integer.parseInt(getSessionVariable(sessionId, "timeToLive"));
    }

    @Override
    public String requireSession(Long userId, int timeToLive) throws SessionCreationFailedException {
        boolean exists = executeOperation(jedis -> jedis.hexists("directory", String.valueOf(userId)));
        if (exists) {
            return executeOperation(jedis -> jedis.hget("directory", String.valueOf(userId)));
        }

        return createSession(userId, timeToLive);
    }

    @Override
    public void close() {
        pool.close();
    }
}
