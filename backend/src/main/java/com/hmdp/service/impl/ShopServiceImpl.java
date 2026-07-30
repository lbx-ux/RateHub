package com.hmdp.service.impl;

import com.github.pagehelper.PageHelper;
import com.hmdp.constant.SystemConstants;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopQueryDTO;
import com.hmdp.dto.ShopVO;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
     * 保存店铺信息，并主动清除该类型的列表缓存（Cache Aside 写操作）
     *
     * @param shop 店铺实体对象
     */
    @Override
    public void save(Shop shop) {
        // 1. 写入数据库
        shopMapper.insert(shop);

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

    @Override
    public Result<?> queryShopByType(ShopQueryDTO queryDTO) {
        Double x = queryDTO.getX();
        Double y = queryDTO.getY();

        String sortBy = queryDTO.getSortBy();
        // 判断是否为默认排序（即按距离排序），sortBy 为空说明没有指定按评分/销量排序
        boolean isDefaultSort = StrUtil.isBlank(sortBy);

        // 计算分页的起止偏移量：from 为跳过条数，end 为需要查询到的总条数
        int from = (queryDTO.getCurrent() - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = from + SystemConstants.DEFAULT_PAGE_SIZE;
        // Redis GEO 的 key 按店铺类型区分，格式为 shop:geo:{typeId}
        String key = "shop:geo:" + queryDTO.getTypeId();
        
        // 前端未传距离时，默认搜索 5000 米（5km）范围内的商铺，0 米会导致查不到任何结果
        int odistance = queryDTO.getDistance() != null ? queryDTO.getDistance() : 5000;
        // 构造圆形搜索区域：以 (x, y) 为圆心，odistance 为半径
        GeoShape shape = GeoShape.byRadius(new Distance(odistance, RedisGeoCommands.DistanceUnit.METERS));
        // 构造搜索的圆心坐标参考点
        GeoReference<String> reference = GeoReference.fromCoordinate(x, y);
        
        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance(); // 返回结果中附带距离值，后续用于展示"距您 xxx 米"
                        
        if (isDefaultSort) {
            // 【按距离排序】：让 Redis 按距离升序排列，并只返回前 end 条（覆盖当前页所需数据即可）
            args.sortAscending().limit(end);
        } else {
            // 【按评分/人气排序】：Redis 无法按评分排序，因此先取出半径内所有商铺 ID（上限 5000），
            // 后续再交给数据库按评分/销量排序并分页
            args.limit(5000);
        }
        
        // 执行 Redis GEO 搜索，返回半径内的商铺 ID 列表及各自距离
        GeoResults<RedisGeoCommands.GeoLocation<String>> searchResults = stringRedisTemplate.opsForGeo()
                .search(key, reference, shape, args);
        if(searchResults == null){
            return Result.success(Collections.emptyList());
        }
        
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = searchResults.getContent();
        if (content.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<Long> ids = new ArrayList<>(content.size());
        // 缓存每个商铺 ID 到其距离的映射，后续用于给 ShopVO 填充距离字段
        Map<String, Distance> distanceMap = new HashMap<>(content.size());
        
        if (isDefaultSort) {
            // 【按距离排序的分页逻辑】：
            // Redis 已按距离升序返回了前 end 条数据，只需跳过前 from 条即可实现分页
            if (content.size() <= from) {
                return Result.success(Collections.emptyList());
            }
            content.stream().skip(from).forEach(result -> {
                String shopIdStr = result.getContent().getName();
                ids.add(Long.valueOf(shopIdStr));
                distanceMap.put(shopIdStr, result.getDistance());
            });
        } else {
            // 【按评分/人气排序】：不需要分页跳过，先收集所有 ID，排序和分页交给数据库处理
            content.forEach(result -> {
                String shopIdStr = result.getContent().getName();
                ids.add(Long.valueOf(shopIdStr));
                distanceMap.put(shopIdStr, result.getDistance());
            });
        }

        if (ids.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<ShopVO> shops;
        if (isDefaultSort) {
            // 【按距离排序】：用 Redis 返回的有序 ID 列表去数据库批量查询完整商铺信息
            // 查询 ShopVO（含 hasVoucher 字段）而非普通 Shop，保证列表页能展示"是否有优惠券"
            List<ShopVO> unSortedShops = shopMapper.queryShopVOByIds(ids);
            Map<Long, ShopVO> shopMap = unSortedShops.stream()
                    .collect(Collectors.toMap(ShopVO::getId, shop -> shop));
            
            // 按 Redis 返回的 ID 顺序组装结果（即距离从近到远）
            shops = ids.stream()
                    .map(shopMap::get)
                    .filter(Objects::nonNull) // 过滤脏数据：若数据库中某 ID 已删除但 Redis 中仍有缓存，map.get 会返回 null
                    .collect(Collectors.toList());
        } else {
            // 【按评分/人气排序】：将 Redis 筛出的商铺 ID 传给数据库，由数据库完成排序和分页
            // PageHelper 会自动拦截 SQL 并追加 ORDER BY 和 LIMIT 子句
            PageHelper.startPage(queryDTO.getCurrent(), SystemConstants.DEFAULT_PAGE_SIZE);
            shops = shopMapper.queryShopVOByIdsWithSort(ids, sortBy);
        }

        // 统一为所有商铺补充距离信息（无论哪种排序方式，前端都可能需要展示距离）
        shops.forEach(shop -> {
            Distance distance = distanceMap.get(shop.getId().toString());
            if (distance != null) {
                shop.setDistance(distance.getValue());
            }
        });
        
        return Result.success(shops);
    }
}
