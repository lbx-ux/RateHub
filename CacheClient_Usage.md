# Redis 通用缓存客户端 CacheClient 使用指南

`CacheClient` 是一个面向企业级、高并发场景的通用缓存辅助工具。它整合了泛型与函数式编程，将复杂的缓存穿透防护、互斥锁重建、逻辑过期异步刷新等底层逻辑与具体的数据库查询逻辑解耦，实现了**一次编写，处处复用**的目标。

---

## 一、核心痛点与解决方案

在开发基于 Redis 的缓存系统时，有两大经典高并发问题需要防范，本工具类已在内部完成了无感封装：

| 场景 | 概念及危害 | CacheClient 解决方案 |
| :--- | :--- | :--- |
| **缓存穿透** | 查询一个**不存在**的数据，缓存和数据库都未命中，导致每次请求都直达数据库，容易被攻击。 | **缓存空对象**：数据库无此数据时，向缓存写入一个空对象（`""`），并限定 2 分钟内物理过期。再次请求会直接在 Redis 被拦截并返回 `null`。 |
| **缓存击穿** | 一个**热门 key** 突然失效，或者高并发访问瞬间该 key 不存在，导致大量并发请求直接冲垮数据库。 | 1. **互斥锁方案**：通过 Redis 的 `SETNX` 加锁，同一时刻只允许一个线程查库重建缓存，其他线程退避自旋等待。<br>2. **逻辑过期方案**：热点 key 不过期物理时限，只设置“逻辑过期时间”。过期时仅由一个线程异步获取锁并在后台线程池中重建缓存，而其他请求直接读取旧缓存数据，**零等待，高吞吐**。 |

---

## 二、核心 API 详解

```java
@Component
public class CacheClient {
    // 写入物理缓存
    public void set(String key, Object value, Long time, TimeUnit unit);

    // 写入逻辑过期缓存
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit);

    // 1. 通用缓存穿透防护查询
    public <R, ID> R queryWithPassThrough(
        String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit
    );

    // 2. 通用缓存击穿防护查询（互斥锁模式）
    public <R, ID> R queryWithMutex(
        String keyPrefix, String lockKeyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit
    );

    // 3. 通用缓存击穿防护查询（逻辑过期模式）
    public <R, ID> R queryWithLogicalExpire(
        String keyPrefix, String lockKeyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit
    );
}
```

### 关键参数说明
- `keyPrefix`: 缓存的 Key 前缀。例如：`"cache:shop:"`。
- `lockKeyPrefix` (仅击穿方案): 分布式互斥锁的 Key 前缀。例如：`"lock:shop:"`。
- `id`: 查询对象的唯一业务 ID（支持 `Long`、`String` 等任何泛型 ID 类型）。
- `type`: 期望反序列化回的实体类 Class（例如 `Shop.class`）。
- `dbFallback`: 当缓存未命中或逻辑过期时，用于**调用数据库查询**的回调逻辑。可通过方法引用（`shopMapper::getById`）或 Lambda（`id -> selectFromDb(id)`）传入。
- `time` & `unit`: 过期时间时长及其时间单位。

---

## 三、开发使用范例

下面以在其他服务类中（例如 **VoucherServiceImpl（优惠券服务）**）集成缓存为例：

### 1. 注入缓存客户端
```java
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements IVoucherService {

    private final VoucherMapper voucherMapper;
    
    // 1. 引入我们的通用 CacheClient
    private final CacheClient cacheClient;
}
```

### 2. 使用“防缓存穿透”模式查询数据
若你希望以**防穿透**为核心防护，并允许数据在缓存中到期物理自动清除：
```java
public Voucher queryVoucherById(Long id) {
    // 执行查询，自动完成 Redis 与数据库的联动
    return cacheClient.queryWithPassThrough(
        "cache:voucher:",        // 缓存前缀
        id,                     // 查询 ID
        Voucher.class,          // 反序列化的目标类型
        voucherMapper::getById, // 数据库查询逻辑 (函数式回调)
        30L,                    // 缓存有效期数值
        TimeUnit.MINUTES        // 有效期单位
    );
}
```

### 3. 使用“分布式互斥锁”防击穿模式
适合于对**数据强一致性**要求高，且即使高并发也仅允许极少数请求回打数据库的业务：
```java
public Voucher queryVoucherWithMutex(Long id) {
    return cacheClient.queryWithMutex(
        "cache:voucher:", 
        "lock:voucher:", 
        id, 
        Voucher.class, 
        voucherMapper::getById, 
        30L, 
        TimeUnit.MINUTES
    );
}
```

### 4. 使用“逻辑过期”异步更新防击穿模式
适合于**超高并发、超大访问量**的热点数据（如秒杀优惠券、爆款店铺），接受读取几秒钟之前的旧数据，但不允许请求发生任何响应卡顿：
> **注意**：逻辑过期机制依赖“缓存预热”，即必须有其他组件提前将数据以 `setWithLogicalExpire` 形式持久化写入 Redis 之后，该机制才能正常运行。
```java
public Voucher queryVoucherWithLogical(Long id) {
    return cacheClient.queryWithLogicalExpire(
        "cache:voucher:", 
        "lock:voucher:", 
        id, 
        Voucher.class, 
        voucherMapper::getById, 
        20L, 
        TimeUnit.SECONDS // 逻辑过期时间通常较短，可以根据重建频率设定
    );
}
```

---

## 四、如何进行缓存预热（针对逻辑过期方案）

逻辑过期机制假设数据**在缓存中必须存在**。因为如果未命中，它直接返回 `null` 而不走同步查库。因此，在使用逻辑过期之前，**必须提前进行缓存预热**。

通常有以下三种企业级预热方式：

### 1. 临时/单次预热：编写 JUnit 单元测试类
适合在**大促上线前**，开发或运维手动运行测试类，一次性将特定爆款/热点数据写入 Redis。

```java
@SpringBootTest
class CachePreheatTest {

    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ShopMapper shopMapper;

    @Test
    void testSaveShopToRedis() {
        // 预热 ID 为 1 的商铺，逻辑过期时间设为 30 分钟
        Shop shop = shopMapper.getById(1L);
        if (shop != null) {
            cacheClient.setWithLogicalExpire(
                RedisConstants.CACHE_SHOP_KEY + 1L, 
                shop, 
                30L, 
                TimeUnit.MINUTES
            );
        }
    }
}
```

### 2. 启动时自动预热：实现 CommandLineRunner 接口
适合项目启动时需要加载的**全局热点数据**（如分类、少量固定的热点配置或大促专场商品）。项目启动成功后，会自动查库并刷入 Redis。

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class CachePreheatRunner implements CommandLineRunner {

    private final ShopMapper shopMapper;
    private final CacheClient cacheClient;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始进行商铺热点数据缓存预热...");
        // 模拟查出前 100 个高频访问的商铺进行预热
        List<Shop> hotShops = shopMapper.queryHotShops(100);
        for (Shop shop : hotShops) {
            cacheClient.setWithLogicalExpire(
                RedisConstants.CACHE_SHOP_KEY + shop.getId(),
                shop,
                30L,
                TimeUnit.MINUTES
            );
        }
        log.info("商铺数据缓存预热完成！");
    }
}
```

### 3. 动态/周期性预热：定时任务（如 Spring Scheduled 或 XXL-JOB）
适合**动态变化**的热点数据（如根据访问量统计出每天变化的热点商品列表）。每天深夜或每隔数小时重新计算并刷新逻辑过期时间。

```java
@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class PreheatTask {

    private final ShopMapper shopMapper;
    private final CacheClient cacheClient;

    // 每 20 分钟执行一次，更新缓存中的逻辑过期时间以确保数据常新
    @Scheduled(cron = "0 0/20 * * * ?")
    public void preheatHotShops() {
        log.info("定时执行热点商铺缓存预热重建任务...");
        // 根据业务规则筛选热点数据（例如点击量前 50 的店铺）
        List<Shop> hotShops = shopMapper.getTop50ActiveShops();
        for (Shop shop : hotShops) {
            cacheClient.setWithLogicalExpire(
                RedisConstants.CACHE_SHOP_KEY + shop.getId(),
                shop,
                30L,
                TimeUnit.MINUTES
            );
        }
    }
}
```
