package com.hmdp;

import com.hmdp.constant.RedisConstants;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ShopMapper shopMapper;

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

    @Test
    void loadShopData() {
        List<Shop> list = shopMapper.selectAll();
        Map<Long,List<Shop>> map=list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = "shop:geo:" + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }
}
