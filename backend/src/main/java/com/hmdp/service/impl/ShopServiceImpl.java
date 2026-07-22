package com.hmdp.service.impl;

import com.github.pagehelper.PageHelper;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopVO;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    /** 店铺列表缓存 Key 前缀，格式: cache:shop:list:{typeId}:{current} */
    private static final String CACHE_SHOP_LIST_KEY = "cache:shop:list:";
    /** 店铺列表缓存 TTL（分钟） */
    private static final long CACHE_SHOP_LIST_TTL = 720L;

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
     * 保存店铺信息，并主动清除该类型的列表缓存（Cache Aside 写操作）
     *
     * @param shop 店铺实体对象
     */
    @Override
    public void save(Shop shop) {
        // 1. 写入数据库
        shopMapper.insert(shop);
        // 2. 清除该 typeId 下所有分页缓存，避免新数据不可见
        clearShopListCache(shop.getTypeId());
    }

    /**
     * 更新店铺信息，并删除单条缓存与列表缓存（Cache Aside 写操作）
     *
     * @param shop 店铺实体对象
     */
    @Override
    public void updateById(Shop shop) {
        // 1. 更新数据库
        shopMapper.update(shop);
        // 2. 删除单条店铺缓存（保证详情页一致性）
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        // 3. 清除该 typeId 下所有分页缓存（保证列表页一致性）
        clearShopListCache(shop.getTypeId());
    }

    /**
     * 通过 Redis SCAN 命令删除指定 typeId 下所有列表分页缓存。
     * 使用 execute(RedisCallback) 访问底层连接，避免 KEYS 命令在大数据集下阻塞 Redis。
     *
     * @param typeId 店铺类型ID
     */
    private void clearShopListCache(Long typeId) {
        String pattern = CACHE_SHOP_LIST_KEY + typeId + ":*";
        try {
            // 通过 RedisCallback 访问底层连接执行 SCAN
            List<String> keys = stringRedisTemplate.execute((RedisCallback<List<String>>) connection -> {
                List<String> result = new ArrayList<>();
                Cursor<byte[]> cursor = connection.scan(
                        ScanOptions.scanOptions().match(pattern).count(100).build());
                while (cursor.hasNext()) {
                    result.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
                return result;
            });
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 降级兜底：至少删除第 1 页，保证首页数据不陈旧
            stringRedisTemplate.delete(CACHE_SHOP_LIST_KEY + typeId + ":1");
        }
    }

    /**
     * 根据店铺类型分页查询店铺列表（带 Redis 缓存，TTL 5 分钟）
     *
     * @param typeId 店铺类型ID
     * @param current 当前页码
     * @param pageSize 每页条数
     * @return 店铺列表
     */
    @Override
    public List<ShopVO> queryShopByType(Integer typeId, int current, int pageSize) {
        // 1. 拼接缓存 Key，区分不同类型和页码
        String cacheKey = CACHE_SHOP_LIST_KEY + typeId + ":" + current;

        // 2. 优先从 Redis 读取
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (json != null && !json.isEmpty()) {
            return JSONUtil.toList(json, ShopVO.class);
        }

        // 3. 缓存未命中，查询数据库
        PageHelper.startPage(current, pageSize);
        List<ShopVO> list = shopMapper.queryShopByType(typeId);

        // 4. 将结果写入 Redis，设置 5 分钟 TTL
        stringRedisTemplate.opsForValue().set(
                cacheKey, JSONUtil.toJsonStr(list), CACHE_SHOP_LIST_TTL, TimeUnit.MINUTES);

        return list;
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
    public List<ShopVO> queryShopByName(String name, int current, int pageSize) {
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
        Shop shop = cacheClient.queryWithPassThrough(
                CACHE_SHOP_KEY, id, Shop.class, shopMapper::getById, 30L, TimeUnit.MINUTES);

        // 方案二：采用通用互斥锁防击穿与防穿透查询
        // Shop shop = cacheClient.queryWithMutex(
        //         CACHE_SHOP_KEY, LOCK_SHOP_KEY, id, Shop.class, shopMapper::getById, 30L, TimeUnit.MINUTES);

         //方案三：采用通用逻辑过期防击穿查询（适用于热点Key提前缓存预热）
         //Shop shop = cacheClient.queryWithLogicalExpire(
         //   CACHE_SHOP_KEY, LOCK_SHOP_KEY, id, Shop.class, shopMapper::getById, 20L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.error("店铺不存在");
        }
        return Result.success(shop);
    }
}
