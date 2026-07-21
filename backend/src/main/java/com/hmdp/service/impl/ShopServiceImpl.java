package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageHelper;
import com.hmdp.dto.RedisData;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.constant.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.constant.RedisConstants.LOCK_SHOP_KEY;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements IShopService {
    private final ShopMapper shopMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

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
        stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());
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
     * 根据店铺ID查询店铺信息，使用缓存机制（支持缓存穿透、缓存击穿等解决方案）
     *
     * @param id 店铺ID
     * @return 包含店铺信息的Result对象
     */
    @Override
    public Result<Shop> queryShopById(Long id) {
        // 解决缓存穿透方案
        // Shop shop = queryWithPassThrough(id);

        // 逻辑过期方案解决缓存击穿
        Shop shop = queryWithLogicalExpire(id);
        if(shop == null){
            return Result.error("店铺不存在");
        }
        return Result.success(shop);
    }

    /**
     * 查询店铺信息，采用缓存空对象的方式解决缓存穿透问题
     *
     * @param id 店铺ID
     * @return 店铺实体对象，不存在则返回null
     */
    public Shop queryWithPassThrough(Long id){
        // 1. 从Redis中查询缓存
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        // 2. 判断缓存是否命中（非空字符串）
        if(StrUtil.isNotBlank(shopJson)){
            // 缓存命中且不为空值，反序列化并直接返回
            return JSONUtil.toBean(shopJson,Shop.class);
        }
        // 3. 缓存未命中的情况：判断是否命中了防缓存穿透写入的空对象（如 ""）
        if(shopJson != null){
            // 命中了缓存的空对象，直接返回null，阻止穿透到数据库
            return null;
        }
        // 4. 缓存未命中且不是空值，前往数据库查询
        Shop shop=shopMapper.getById(id);
        // 5. 数据库中不存在
        if(shop==null){
            // 将空对象写入Redis缓存，设置较短的过期时间（防缓存穿透）
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",2L,TimeUnit.MINUTES);
            return null;
        }
        // 6. 数据库中存在，写入Redis缓存并设置过期时间
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),30L, TimeUnit.MINUTES);
        return shop;
    }

    /**
     * 尝试获取互斥锁（基于Redis setnx实现）
     *
     * @param key 锁的键值
     * @return 是否成功获取锁
     */
    private boolean tryLock(String key){
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10L, TimeUnit.SECONDS));
    }

    /**
     * 释放锁
     *
     * @param key 锁的键值
     */
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    /**
     * 查询店铺信息，采用互斥锁机制解决缓存击穿问题
     *
     * @param id 店铺ID
     * @return 店铺实体对象
     */
    public Shop queryWithMutex(Long id) {
        // 1. 从Redis中查询缓存
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        // 2. 判断是否命中缓存
        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson,Shop.class);
        }
        // 判断命中是否为空白缓存防穿透值
        if(shopJson!=null){
            return null;
        }
        
        // 3. 未命中缓存，开始尝试缓存重建
        // 3.1 定义互斥锁的Key
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try {
            // 3.2 尝试获取锁
            boolean isLock = tryLock(lockKey);
            // 3.3 判断是否获取成功
            if(!isLock){
                // 3.4 失败则休眠片刻并重试自旋
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 3.5 成功，从数据库中查询数据
            shop=shopMapper.getById(id);
            // 4. 如果数据库中数据不存在
            if(shop == null){
                // 写入空值进缓存以防缓存穿透
                stringRedisTemplate.opsForValue().set("cache:shop:" + id, "", 2, TimeUnit.MINUTES);
                return null;
            }
            // 5. 写入Redis缓存
            stringRedisTemplate.opsForValue().set("cache:shop:" + id, JSONUtil.toJsonStr(shop),30,TimeUnit.MINUTES);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
        finally {
            // 6. 释放互斥锁
            unLock(lockKey);
        }
        return shop;
    }

    /**
     * 将店铺数据保存到Redis中，并设置逻辑过期时间，用于解决缓存击穿
     *
     * @param id 店铺ID
     * @param expireSeconds 逻辑过期时间（秒）
     */
    public void saveShopRedis(Long id, Long expireSeconds){
        // 1. 查询数据库最新店铺数据
        Shop shop=shopMapper.getById(id);
        // 2. 封装RedisData（将数据与逻辑过期时间组合）
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3. 写入缓存，由于是逻辑过期，这里不显式在Redis上设过期时间，避免物理过期导致丢失
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    /**
     * 查询店铺信息，采用逻辑过期机制解决缓存击穿问题
     *
     * @param id 店铺ID
     * @return 店铺实体对象
     */
    public Shop queryWithLogicalExpire(Long id){
        // 1. 从Redis查询缓存
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        // 2. 判断是否命中缓存
        if(StrUtil.isBlank(shopJson)){
            // 逻辑过期方案默认缓存已预热，未命中则直接返回null
            return null;
        }
        // 3. 命中缓存，反序列化对象
        RedisData redisData=JSONUtil.toBean(shopJson,RedisData.class);
        Shop shop=JSONUtil.toBean((JSONObject) redisData.getData(),Shop.class);
        LocalDateTime expireTime=redisData.getExpireTime();
        // 4. 判断缓存是否已过期
        if(expireTime.isAfter(LocalDateTime.now())){
            // 4.1 未过期，直接返回商铺数据
            return shop;
        }
        
        // 4.2 已过期，开始重建缓存
        // 5. 尝试获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6. 判断锁是否获取成功
        if(isLock){
            // 6.1 获取成功，提交异步重建缓存任务给线程池
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 执行缓存重建，设定逻辑过期时长为20秒
                    this.saveShopRedis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 重建完毕后释放互斥锁
                    unLock(lockKey);
                }
            });
        }
        // 7. 哪怕锁获取失败或成功（在后台异步重建时），都先返回过期数据
        return shop;
    }

}
