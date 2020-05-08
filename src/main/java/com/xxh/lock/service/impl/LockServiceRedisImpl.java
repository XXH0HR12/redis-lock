package com.xxh.lock.service.impl;

import com.xxh.lock.service.LockService;
import java.util.Collections;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 分布式锁 redis 实现
 *
 * @author xxh
 * @date 2020/5/7
 */
@Slf4j
@Service
public class LockServiceRedisImpl implements LockService {

    private JedisPool jedisPool;

    private static final String SET_SUCCESS = "OK";

    private static final String KEY_PRE = "REDIS_LOCK_";

    private static final int expiredTime = 20;

    public LockServiceRedisImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public String lock(String key) {
        Jedis jedis = jedisPool.getResource();
        key = KEY_PRE + key;
        String value = fetchLockValue();
        if (SET_SUCCESS.equals(jedis.set(key, value, "NX", "EX", expiredTime))) {
            log.info("拿锁成功 key={},value={}", key, value);
            return SET_SUCCESS;
        }
        log.info("拿锁失败");
        jedis.close();
        return null;
    }

    public String tryLock(String key) {
        Jedis jedis = jedisPool.getResource();
        key = KEY_PRE + key;
        String value = fetchLockValue();
        long firstTryTime = System.currentTimeMillis();
        do {
            if (SET_SUCCESS.equals(jedis.set(key, value, "NX", "EX", expiredTime))) {
                log.info("拿锁成功 key={},value={}", key, value);
                return SET_SUCCESS;
            }
            log.info("Redis拿锁失败，正在等待下次个尝试");
        } while ((System.currentTimeMillis() - expiredTime * 1000) < firstTryTime);
        jedis.close();
        return null;
    }

    public boolean unLock(String key, String value) {
        Long RELEASE_SUCCESS = 1L;
        Jedis jedis = jedisPool.getResource();
        key = KEY_PRE + key;
        String command = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        if (RELEASE_SUCCESS.equals(jedis.eval(command, Collections.singletonList(key), Collections.singletonList(value)))) {
            log.info("锁释放成功 key={},value={}", key, value);
            return true;
        }
        jedis.close();
        return false;
    }

    /**
     * 生成加锁的唯一字符串
     *
     * @return 唯一字符串
     */
    private String fetchLockValue() {
        return UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    }
}
