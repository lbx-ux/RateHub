package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageHelper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.constant.RedisConstants.CACHE_SHOP_KEY;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements IShopService {
    private final ShopMapper shopMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Shop getById(Long id) {
        return shopMapper.getById(id);
    }

    @Override
    public void save(Shop shop) {
        shopMapper.insert(shop);
    }

    @Override
    public void updateById(Shop shop) {
        shopMapper.update(shop);
    }

    @Override
    public List<Shop> queryShopByType(Integer typeId, int current, int pageSize) {
        PageHelper.startPage(current, pageSize);
        return shopMapper.queryShopByType(typeId);
    }

    @Override
    public List<Shop> queryShopByName(String name, int current, int pageSize) {
        PageHelper.startPage(current, pageSize);
        return shopMapper.queryShopByName(name);
    }

    @Override
    public Result<Shop> queryShopById(Long id) {
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        if(StrUtil.isNotBlank(shopJson)){
            return Result.success(JSONUtil.toBean(shopJson,Shop.class));
        }
        Shop shop=shopMapper.getById(id);
        if(shop==null){
            return Result.error("店铺不存在");
        }
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop));
        return Result.success(shop);
    }
}
