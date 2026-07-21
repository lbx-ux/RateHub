package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    // 假设自定义的起始基准时间为 2026-01-01 00:00:00 的时间戳
    private static final long BEGIN_TIMESTAMP = 1767225600L;

    // 序列号的位数
    private static final int COUNT_BITS = 32;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号
        // 获取当前日期，精确到天，例如：2026:07:21
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 利用 Redis 的 INCR 获取自增序号 (如果 key 不存在会自动创建并从 1 开始)
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        // 3. 拼接并返回
        // 时间戳向左移动 32 位，空出低 32 位，然后与序列号进行按位或运算
        return timestamp << COUNT_BITS | count;
    }
}
