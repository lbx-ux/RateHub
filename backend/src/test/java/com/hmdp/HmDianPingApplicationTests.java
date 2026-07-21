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

    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private RedisIdWorker redisIdWorker;

    @Test
    void testSaveShopToRedis() {
        // 预热 ID 为 1 的商铺，逻辑过期时间设为 10秒
        Shop shop = shopMapper.getById(1L);
        if (shop != null) {
            cacheClient.setWithLogicalExpire(
                    RedisConstants.CACHE_SHOP_KEY + 1L,
                    shop,
                    10L,
                    TimeUnit.SECONDS
            );
        }
    }

    @Test
    void testIdWorker() {
        for (int i = 0; i < 100; i++) {
            long id = redisIdWorker.nextId("order");
            System.out.println("id = " + id);
        }
    }



}
