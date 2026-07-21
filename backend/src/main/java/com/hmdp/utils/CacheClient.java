package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    // 缓存重建使用的固定大小为 10 的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将对象写入 Redis，并设置物理过期时间
     *
     * @param key 缓存Key
     * @param value 缓存数据
     * @param time 过期时间数值
     * @param unit 时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将对象写入 Redis，并封装逻辑过期时间
     *
     * @param key 缓存Key
     * @param value 缓存数据
     * @param time 逻辑过期时间数值
     * @param unit 时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 封装逻辑过期数据对象
        RedisData redisData = new RedisData();
        redisData.setData(value);
        // 将指定单位的时间转换为秒计算
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入 Redis，这里不设置物理过期时间，使其永久保存（物理不过期）
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 根据指定的 Key 查询缓存，若未命中则通过 dbFallback 查询数据库并写入缓存（防止缓存穿透方案）
     *
     * @param keyPrefix 缓存前缀
     * @param id 业务ID
     * @param type 返回实体类型Class
     * @param dbFallback 数据库查询函数式回调
     * @param time 缓存物理过期时间
     * @param unit 时间单位
     * @return 实体对象
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1. 从 Redis 中获取缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        
        // 2. 判断缓存是否命中（非空且非特殊空白字符）
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        
        // 3. 判断是否命中空对象（防穿透，例如缓存中存的值为 ""）
        if (json != null) {
            // 说明命中为了防止缓存穿透而缓存的空对象，直接返回 null
            return null;
        }

        // 4. 缓存未命中，调用 fallback 查询数据库
        R r = dbFallback.apply(id);
        
        // 5. 数据库也不存在该数据，向 Redis 中写入一个空值（有效期较短），防止缓存穿透
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
            return null;
        }
        
        // 6. 数据库存在，写入缓存并设置物理过期时间
        this.set(key, r, time, unit);
        return r;
    }

    /**
     * 根据指定的 Key 查询缓存，若未命中直接返回 null；若命中但逻辑过期，则获取互斥锁异步重建缓存（逻辑过期防击穿方案）
     *
     * @param keyPrefix 缓存前缀
     * @param lockKeyPrefix 锁的前缀
     * @param id 业务ID
     * @param type 返回实体类型Class
     * @param dbFallback 数据库查询函数式回调
     * @param time 缓存逻辑过期时间
     * @param unit 时间单位
     * @return 实体对象
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, String lockKeyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1. 从 Redis 中查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        
        // 2. 逻辑过期要求先热数据，如果缓存根本不存在，直接返回 null
        if (StrUtil.isBlank(json)) {
            return null;
        }
        
        // 3. 命中缓存，反序列化为封装了过期时间的 RedisData 结构
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        
        // 4. 判断缓存是否已逻辑过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 4.1. 未过期，直接返回现有实体数据
            return r;
        }
        
        // 4.2. 已逻辑过期，需要重建缓存
        // 5. 尝试获取互斥锁重建缓存
        String lockKey = lockKeyPrefix + id;
        boolean isLock = tryLock(lockKey);
        
        // 6. 如果成功获取到锁，开启异步独立线程重建缓存
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库最新数据
                    R newR = dbFallback.apply(id);
                    // 异步写入缓存并指定逻辑过期时间
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 最终释放锁
                    unlock(lockKey);
                }
            });
        }
        
        // 7. 若未获取到锁，或异步在后台执行时，均先返回过期的旧数据
        return r;
    }

    /**
     * 根据指定的 Key 查询缓存，若未命中，则使用互斥锁防缓存击穿并二次校验缓存（互斥锁模式防击穿方案）
     *
     * @param keyPrefix 缓存前缀
     * @param lockKeyPrefix 锁的前缀
     * @param id 业务ID
     * @param type 返回实体类型Class
     * @param dbFallback 数据库查询函数式回调
     * @param time 缓存物理过期时间
     * @param unit 时间单位
     * @return 实体对象
     */
    public <R, ID> R queryWithMutex(
            String keyPrefix, String lockKeyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1. 查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        if (json != null) {
            return null; // 防穿透的空值
        }

        // 2. 尝试获取互斥锁
        String lockKey = lockKeyPrefix + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                // 没拿到锁，休眠并重试（自旋）
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, lockKeyPrefix, id, type, dbFallback, time, unit);
            }
            
            // 3. Double Check：获取锁成功后再次进行缓存的双重校验
            json = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)) {
                return JSONUtil.toBean(json, type);
            }
            if (json != null) {
                return null;
            }
            
            // 4. 双重检查依然未命中，执行 fallback 查询数据库
            r = dbFallback.apply(id);
            if (r == null) {
                // 将空值写入 Redis 防缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
                return null;
            }
            // 5. 将查询出的最新结果存入 Redis 物理缓存
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 6. 释放互斥锁
            unlock(lockKey);
        }
        return r;
    }

    /**
     * 尝试获取锁
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10L, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
