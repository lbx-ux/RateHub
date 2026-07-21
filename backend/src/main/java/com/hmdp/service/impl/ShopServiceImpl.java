package com.hmdp.service.impl;

import com.github.pagehelper.PageHelper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.constant.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.constant.RedisConstants.LOCK_SHOP_KEY;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements IShopService {
    private final ShopMapper shopMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;

    /**
     * 根据店铺ID查询店铺信息
     *
     * @param id 店铺ID
     * @return 店铺实体对象
     */
    @Override
    public Shop getById(Long id) {
        // 直接从数据库Mapper层根据ID查询店铺
        return shopMapper.getById(id);
    }

    /**
     * 保存店铺信息
     *
     * @param shop 店铺实体对象
     */
    @Override
    public void save(Shop shop) {
        // 保存店铺信息到数据库
        shopMapper.insert(shop);
    }

    /**
     * 更新店铺信息，并删除对应的Redis缓存以保证缓存与数据库的一致性
     *
     * @param shop 店铺实体对象
     */
    @Override
    public void updateById(Shop shop) {
        // 1. 更新数据库中的店铺数据
        shopMapper.update(shop);
        // 2. 删除Redis中的缓存，保证下次查询时获取最新数据（Cache Aside 模式）
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
    }

    /**
     * 根据店铺类型分页查询店铺列表
     *
     * @param typeId 店铺类型ID
     * @param current 当前页码
     * @param pageSize 每页条数
     * @return 店铺列表
     */
    @Override
    public List<Shop> queryShopByType(Integer typeId, int current, int pageSize) {
        // 1. 设置PageHelper分页参数
        PageHelper.startPage(current, pageSize);
        // 2. 根据类别查询店铺数据
        return shopMapper.queryShopByType(typeId);
    }

    /**
     * 根据店铺名称模糊/分页查询店铺列表
     *
     * @param name 店铺名称
     * @param current 当前页码
     * @param pageSize 每页条数
     * @return 店铺列表
     */
    @Override
    public List<Shop> queryShopByName(String name, int current, int pageSize) {
        // 1. 设置PageHelper分页参数
        PageHelper.startPage(current, pageSize);
        // 2. 根据店铺名进行模糊查询
        return shopMapper.queryShopByName(name);
    }

    /**
     * 根据店铺ID查询店铺信息，使用缓存客户端机制（封装了缓存穿透与击穿防护）
     *
     * @param id 店铺ID
     * @return 包含店铺信息的Result对象
     */
    @Override
    public Result<Shop> queryShopById(Long id) {
        // 方案一：采用通用缓存穿透防护查询
        //Shop shop = cacheClient.queryWithPassThrough(
         //       CACHE_SHOP_KEY, id, Shop.class, shopMapper::getById, 30L, TimeUnit.MINUTES);

        // 方案二：采用通用互斥锁防击穿与防穿透查询
        // Shop shop = cacheClient.queryWithMutex(
        //         CACHE_SHOP_KEY, LOCK_SHOP_KEY, id, Shop.class, shopMapper::getById, 30L, TimeUnit.MINUTES);

         //方案三：采用通用逻辑过期防击穿查询（适用于热点Key提前缓存预热）
         Shop shop = cacheClient.queryWithLogicalExpire(
                CACHE_SHOP_KEY, LOCK_SHOP_KEY, id, Shop.class, shopMapper::getById, 20L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.error("店铺不存在");
        }
        return Result.success(shop);
    }
}
