package com.hmdp;

import com.hmdp.constant.RedisConstants;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private com.hmdp.mapper.UserMapper userMapper;

    @Resource
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Test
    void testMultiLogin() throws java.io.IOException {
        java.util.List<com.hmdp.entity.User> userList = userMapper.selectAll();
        java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter("tokens.txt"));
        for (com.hmdp.entity.User user : userList) {
            String token = cn.hutool.core.lang.UUID.randomUUID().toString(true);
            com.hmdp.dto.UserDTO userDTO = cn.hutool.core.bean.BeanUtil.copyProperties(user, com.hmdp.dto.UserDTO.class);
            java.util.Map<String, Object> userMap = cn.hutool.core.bean.BeanUtil.beanToMap(userDTO, new java.util.HashMap<>(),
                    cn.hutool.core.bean.copier.CopyOptions.create()
                            .setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? "" : fieldValue.toString()));
            String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
            stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
            stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);
            out.println(token);
        }
        out.flush();
        out.close();
    }
}
