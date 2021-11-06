package coo.lxt.island.common.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This manager provide redis loading cache for DB interaction.
 */
@Slf4j
public abstract class AbstractManager {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Nullable<T> {
        private T value;
    }

    @Value("${common.dao.cache.lock.prefix:lock.}")
    private String LOCK_PREFIX;

    @Value("${common.dao.cache.lock.timeout:3}")
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
        RBucket<Nullable<T>> bucket = redissonClient.getBucket(key);
        Optional<Nullable<T>> value = Optional.ofNullable(bucket.get());
        if (value.isPresent()) {
            return value.map(Nullable::getValue);
        } else {
            log.warn("Miss in cache for key: {}", key);
            RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
            try {
                boolean locked = lock.tryLock(LOCK_TIMEOUT, LOCK_TIMEOUT, TimeUnit.SECONDS);
                value = Optional.ofNullable(bucket.get());
                if (locked && !value.isPresent()) {
                    Optional<T> res = dbReader.get();
                    value = Optional.of(new Nullable<>(res.orElse(null)));
                    bucket.set(value.get(), CACHE_TIMEOUT, TimeUnit.MINUTES);
                }
                return value.map(Nullable::getValue);
            } catch (InterruptedException e) {
                log.error("Acquiring lock interrupted: {}.", key, e);
                Thread.currentThread().interrupt();
                return value.map(Nullable::getValue);
            } finally {
                lock.unlock();
            }
        }
    }

    protected <T> Optional<T> read(String key, String field, Supplier<List<T>> dbReader, Function<T, String> fieldMapper) {
        RMap<String, Nullable<T>> bucket = redissonClient.getMap(key);
        Optional<Nullable<T>> value = Optional.ofNullable(bucket.get(field));
        if (value.isPresent()) {
            return value.map(Nullable::getValue);
        } else {
            log.warn("Miss in cache for key: {}, field: {}", key, field);
            RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
            try {
                boolean locked = lock.tryLock(LOCK_TIMEOUT, LOCK_TIMEOUT, TimeUnit.SECONDS);
                value = Optional.ofNullable(bucket.get(field));
                if (locked && !value.isPresent()) {
                    Map<String, Nullable<T>> map = dbReader.get().stream().collect(Collectors.toMap(fieldMapper, Nullable::new));
                    if (locked && !map.containsKey(field)) {
                        map.put(field, new Nullable<>());
                    }
                    value = Optional.ofNullable(map.get(field));
                    bucket.putAll(map);
                    bucket.expire(CACHE_TIMEOUT, TimeUnit.MINUTES);
                }
                return value.map(Nullable::getValue);
            } catch (InterruptedException e) {
                log.error("Acquiring lock interrupted: {}.", key, e);
                Thread.currentThread().interrupt();
                return value.map(Nullable::getValue);
            } finally {
                lock.unlock();
            }
        }
    }

    protected <T> List<T> readAll(String key, Supplier<List<T>> dbReader, Function<T, String> fieldMapper) {
        RMap<String, Nullable<T>> bucket = redissonClient.getMap(key);
        Collection<Nullable<T>> values = bucket.readAllValues();
        if (!CollectionUtils.isEmpty(values)) {
            return values.stream().map(Nullable::getValue).filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            log.warn("Miss in cache for key: {}", key);
            RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
            try {
                boolean locked = lock.tryLock(LOCK_TIMEOUT, LOCK_TIMEOUT, TimeUnit.SECONDS);
                values = bucket.readAllValues();
                if (locked && CollectionUtils.isEmpty(values)) {
                    Map<String, Nullable<T>> map = dbReader.get().stream().collect(Collectors.toMap(fieldMapper, Nullable::new));
                    values = map.values();
                    bucket.putAll(map);
                    bucket.expire(CACHE_TIMEOUT, TimeUnit.MINUTES);
                }
                return values.stream().map(Nullable::getValue).filter(Objects::nonNull).collect(Collectors.toList());
            } catch (InterruptedException e) {
                log.error("Acquiring lock interrupted: {}.", key, e);
                Thread.currentThread().interrupt();
                return values.stream().map(Nullable::getValue).filter(Objects::nonNull).collect(Collectors.toList());
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
