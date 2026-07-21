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
        stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());
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
        //Shop shop=queryWithPassThrough(id);
        Shop shop=queryWithLogicalExpire(id);
        if(shop==null){
            return Result.error("店铺不存在");
        }
        return Result.success(shop);
    }

    //解决缓存穿透
    public Shop queryWithPassThrough(Long id){
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson,Shop.class);
        }
        if(shopJson!=null){
            return null;
        }
        Shop shop=shopMapper.getById(id);
        if(shop==null){
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",2L,TimeUnit.MINUTES);
            return null;
        }
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),30L, TimeUnit.MINUTES);
        return shop;
    }

    //设置互斥锁
    private boolean tryLock(String key){
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10L, TimeUnit.SECONDS));
    }

    //释放锁
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    //使用互斥锁解决缓存击穿
    public Shop queryWithMutex(Long id) {
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson,Shop.class);
        }
        if(shopJson!=null){
            return null;
        }
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            if(!isLock){
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            shop=shopMapper.getById(id);
            if(shop == null){
                stringRedisTemplate.opsForValue().set("cache:shop:" + id, "", 2, TimeUnit.MINUTES);
                return null;
            }
            stringRedisTemplate.opsForValue().set("cache:shop:" + id, JSONUtil.toJsonStr(shop),30,TimeUnit.MINUTES);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
        finally {
            //7.释放互斥锁
            unLock(lockKey);
        }
        return shop;
    }

    public void saveShopRedis(Long id, Long expireSeconds){
        Shop shop=shopMapper.getById(id);
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    //使用逻辑过期解决缓存击穿
    public Shop queryWithLogicalExpire(Long id){
        String shopJson=stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        if(StrUtil.isBlank(shopJson)){
            return null;
        }
        RedisData redisData=JSONUtil.toBean(shopJson,RedisData.class);
        Shop shop=JSONUtil.toBean((JSONObject) redisData.getData(),Shop.class);
        LocalDateTime expireTime=redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            return shop;
        }
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if(isLock){
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShopRedis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        return shop;
    }

}
