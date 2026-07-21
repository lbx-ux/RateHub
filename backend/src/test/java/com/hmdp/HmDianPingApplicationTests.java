package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private  ShopServiceImpl shopService;

    @Test
    public void testsave(){
        shopService.saveShopRedis(1L,20L);
    }



}
