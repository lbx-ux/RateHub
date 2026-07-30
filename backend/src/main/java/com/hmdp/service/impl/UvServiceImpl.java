package com.hmdp.service.impl;

import com.hmdp.dto.UvVo;
import com.hmdp.service.UvService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UvServiceImpl implements UvService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public UvVo stats() {
        LocalDate now = LocalDate.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String monthStr = now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        int year = now.getYear();
        int week = now.get(WeekFields.of(Locale.getDefault()).weekOfYear());
        String weekStr = year + "-" + String.format("%02d", week);

        String dailyKey = "uv:daily:" + dateStr;
        String weeklyKey = "uv:weekly:" + weekStr;
        String monthlyKey = "uv:monthly:" + monthStr;

        Long dailyUv = stringRedisTemplate.opsForHyperLogLog().size(dailyKey);
        Long weeklyUv = stringRedisTemplate.opsForHyperLogLog().size(weeklyKey);
        Long monthlyUv = stringRedisTemplate.opsForHyperLogLog().size(monthlyKey);

        UvVo uvVo = new UvVo();
        uvVo.setDaily(dailyUv == null ? 0L : dailyUv);
        uvVo.setWeekly(weeklyUv == null ? 0L : weeklyUv);
        uvVo.setMonthly(monthlyUv == null ? 0L : monthlyUv);

        return uvVo;
    }
}
