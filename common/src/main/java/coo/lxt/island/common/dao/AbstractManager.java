package coo.lxt.island.common.dao;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This manager provide redis loading cache for DB interaction.
 */
public abstract class AbstractManager {

    @Value("${common.dao.lock.prefix:lock.}")
    private String LOCK_PREFIX;

    @Value("${common.dao.lock.timeout:3}")
    private int LOCK_TIMEOUT;

    @Value("${common.dao.cache.timeout:30}")
    private int CACHE_TIMEOUT;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * Load from db when miss in redis.
     * Use distributed lock to avoid cache breakdown.
     * Write null to redis when miss in db to avoid cache penetration.
     * Set a very long timeout (1 year) for redis key to enable eviction policy volatile-lru.
     */
    protected <T> Optional<T> read(String key, Supplier<Optional<T>> dbReader) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        if (bucket.isExists()) {
            return Optional.ofNullable(bucket.get());
        } else {
            RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
            try {
                lock.lock(LOCK_TIMEOUT, TimeUnit.SECONDS);
                Optional<T> value = Optional.ofNullable(bucket.get());
                if (!value.isPresent()) {
                    value = dbReader.get();
                    bucket.set(value.orElse(null), CACHE_TIMEOUT, TimeUnit.MINUTES);
                }
                return value;
            } finally {
                lock.unlock();
            }
        }
    }

    protected <T> Optional<T> read(String key, String field, Supplier<List<T>> dbReader, Function<T, String> fieldMapper) {
        RMap<String, T> bucket = redissonClient.getMap(key);
        if (bucket.isExists()) {
            return Optional.ofNullable(bucket.get(field));
        } else {
            RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
            try {
                lock.lock(LOCK_TIMEOUT, TimeUnit.SECONDS);
                Map<String, T> map = bucket.readAllMap();
                if (map.containsKey(field)) {
                    map = dbReader.get().stream().collect(Collectors.toMap(fieldMapper, (o) -> o));
                    if (!map.containsKey(field)) {
                        map.put(field, null);
                    }
                    bucket.putAll(map);
                    bucket.expire(CACHE_TIMEOUT, TimeUnit.MINUTES);
                }
                return Optional.ofNullable(map.get(field));
            } finally {
                lock.unlock();
            }
        }
    }

    protected <T> List<T> readAll(String key, Supplier<List<T>> dbReader, Function<T, String> fieldMapper) {
        RMap<String, T> bucket = redissonClient.getMap(key);
        if (bucket.isExists()) {
            return new ArrayList<>(bucket.values());
        } else {
            RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
            try {
                lock.lock(LOCK_TIMEOUT, TimeUnit.SECONDS);
                List<T> values = new ArrayList<>(bucket.values());
                if (CollectionUtils.isEmpty(values)) {
                    values = dbReader.get();
                    bucket.putAll(values.stream().collect(Collectors.toMap(fieldMapper, (o) -> o)));
                    bucket.expire(CACHE_TIMEOUT, TimeUnit.MINUTES);
                }
                return values;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Write db and flush redis key.
     * Use distributed lock to ensure consistency.
     */
    protected <T> T writeLock(String key, Supplier<T> dbWriter) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        T result;
        try {
            lock.lock(LOCK_TIMEOUT, TimeUnit.SECONDS);
            result = dbWriter.get();
        } finally {
            lock.unlock();
        }
        redissonClient.getBucket(key).deleteAsync();
        return result;
    }

    /**
     * Write db and flush redis key.
     * Cache aside.
     */
    protected <T> T writeCacheAside(String key, Supplier<T> dbWriter) {
        T result = dbWriter.get();
        redissonClient.getBucket(key).deleteAsync();
        return result;
    }

    /**
     * Write db and flush redis key.
     * Cache aside.
     */
    protected <T> T writeCacheAside(String key, String field, Supplier<T> dbWriter) {
        T result = dbWriter.get();
        redissonClient.getMap(key).removeAsync(field);
        return result;
    }

    protected <T> T writeLock(String key, String field, Supplier<T> dbWriter) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        T result;
        try {
            lock.lock(LOCK_TIMEOUT, TimeUnit.SECONDS);
            result = dbWriter.get();
        } finally {
            lock.unlock();
        }
        redissonClient.getMap(key).removeAsync(field);
        return result;
    }
}
