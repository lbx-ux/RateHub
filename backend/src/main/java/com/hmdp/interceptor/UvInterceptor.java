package com.hmdp.interceptor;

import com.hmdp.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
public class UvInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // 1. 获取客户端真实IP
            String ip = IpUtils.getIpAddr(request);
            if ("unknown".equals(ip)) {
                return true;
            }

            // 2. 获取当前时间
            LocalDate now = LocalDate.now();
            String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String monthStr = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
            
            // 获取周数
            int year = now.getYear();
            int week = now.get(WeekFields.of(Locale.getDefault()).weekOfYear());
            String weekStr = year + "-" + String.format("%02d", week);

            // 3. 构建 Redis HyperLogLog Key
            String dailyKey = "uv:daily:" + dateStr;
            String weeklyKey = "uv:weekly:" + weekStr;
            String monthlyKey = "uv:monthly:" + monthStr;

            // 4. 存入 HyperLogLog 统计，PFADD 命令
            stringRedisTemplate.opsForHyperLogLog().add(dailyKey, ip);
            stringRedisTemplate.opsForHyperLogLog().add(weeklyKey, ip);
            stringRedisTemplate.opsForHyperLogLog().add(monthlyKey, ip);
            
        } catch (Exception e) {
            // 异常兜底，防止统计逻辑影响正常主业务
            log.error("UV tracking failed", e);
        }
        return true;
    }
}
