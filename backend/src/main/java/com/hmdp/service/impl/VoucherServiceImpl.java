package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VoucherServiceImpl implements IVoucherService {

    @Resource
    private VoucherMapper voucherMapper;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getCreateTime() == null) {
            voucher.setCreateTime(now);
        }
        if (voucher.getUpdateTime() == null) {
            voucher.setUpdateTime(now);
        }
        voucherMapper.insert(voucher);

        // 新增优惠券后，主动清除该店铺类型对应的 Redis 列表缓存，防止列表页读到旧缓存 (hasVoucher = false)
        clearShopListCache(voucher.getShopId());
    }

    @Override
    public Result<List<Voucher>> queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = voucherMapper.queryVoucherOfShop(shopId);
        // 返回结果
        return Result.success(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券主体 (包含清除店铺列表缓存)
        save(voucher);
        // 保存秒杀信息
        LocalDateTime now = LocalDateTime.now();
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucher.setCreateTime(now);
        seckillVoucher.setUpdateTime(now);
        seckillVoucherService.save(seckillVoucher);
    }

    /**
     * 清除指定店铺所属类型的 Redis 列表缓存
     */
    private void clearShopListCache(Long shopId) {
        if (shopId == null) return;
        try {
            Shop shop = shopMapper.getById(shopId);
            if (shop != null && shop.getTypeId() != null) {
                String pattern = "cache:shop:list:" + shop.getTypeId() + ":*";
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
            }
        } catch (Exception e) {
            // 异常兜底保护
        }
    }
}
