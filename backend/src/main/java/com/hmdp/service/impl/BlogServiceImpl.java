package com.hmdp.service.impl;

import com.github.pagehelper.PageHelper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import cn.hutool.core.bean.BeanUtil;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements IBlogService {
    private final BlogMapper blogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final IUserService userService;

    @Override
    public List<Blog> queryMyBlog(Long userId, int current, int pageSize) {
        PageHelper.startPage(current, pageSize);
        List<Blog> records = blogMapper.queryMyBlog(userId);
        records.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return records;
    }

    @Override
    public List<Blog> queryHotBlog(int current, int pageSize) {
        PageHelper.startPage(current, pageSize);
        List<Blog> records = blogMapper.queryHotBlog();
        records.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return records;
    }

    @Override
    public Result<?> save(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        blog.setCreateTime(LocalDateTime.now());
        blog.setUpdateTime(LocalDateTime.now());
        blogMapper.insert(blog);
        return Result.success(blog.getId());
    }

    @Override
    public Result<?> queryBlogById(Long id) {
        Blog blog = blogMapper.getById(id);
        if (blog == null) {
            return Result.error("博文不存在！");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.success(blog);
    }

    @Override
    public void updateLiked(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked:" + id;
        // 1. 判断当前用户是否已经点赞（从 ZSet 中获取 score，若不为 null 说明已赞过）
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 2. 如果未点赞，可以点赞
            // 2.1 数据库点赞数 +1
            int row = blogMapper.addLiked(id);
            if (row > 0) {
                // 2.2 保存用户到 Redis 的 SortedSet 集合
                // 【难点解析 - 为什么选用 ZSet 与 score 设定】：
                // 原有的 Set 集合无序，无法做到“最早点赞的 Top5 倒序排行”。
                // 改用 ZSet 后，使用当前系统毫秒时间戳（System.currentTimeMillis()）作为 score，
                // 这样在 Redis 中内部就会按时间从早到晚自动正序排列，为后续求 Top N 奠定基础。
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 3. 如果已经点赞，则取消点赞
            // 3.1 数据库点赞数 -1
            int row = blogMapper.subLiked(id);
            if (row > 0) {
                // 3.2 把用户从 Redis 的 SortedSet 集合移除
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
    }

    @Override
    public Result<?> queryBlogLikes(Long id) {
        String key = "blog:liked:" + id;
        // 1. 查询 top5 的点赞用户 zrange key 0 4 (按照 score 从小到大排序，即时间最早的前 5 个)
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        // 2. 解析出其中的用户 id 集合 (此时 ids 列表中顺序已经是时间倒序排行：如 [1010, 1001, 1005])
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());

        // 【难点解析 - MySQL IN 查询乱序问题与解决策略】：
        // 如果直接用 MyBatis-Plus 的 listByIds(ids) 构建 SQL: SELECT * FROM tb_user WHERE id IN (1010, 1001, 1005)，
        // MySQL 默认会按照主键 B+ 树的物理大小排列返回（即 1001, 1005, 1010），彻底打乱 Redis 中原本的“时间先后排行”！
        // 解决办法：
        // 方案A (SQL原生)：拼装 ORDER BY FIELD(id, 1010, 1001, 1005)。
        // 方案B (Java内存/单体优选，本项目采用)：因为仅查询 Top 5 个用户，数据量极小且存在单实体查询缓存，
        // 我们直接利用 Java Stream 顺着有序的 ids 遍历调用 userService.getById(userId)，
        // 既完全杜绝了 MySQL 乱序问题，保证了点赞排行榜的时间顺序，又最大限度利用了单体查询的高效缓存。
        List<UserDTO> userDTOList = ids.stream()
                .map(userId -> userService.getById(userId))
                .filter(Objects::nonNull)
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.success(userDTOList);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        if (user != null) {
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        } else {
            blog.setName("已注销用户");
            blog.setIcon("");
        }
    }

    private void isBlogLiked(Blog blog) {
        String key = "blog:liked:" + blog.getId();
        // 【核心校验与对齐】：以 Redis ZSet 中真实的成员数量校准准对齐点赞数，解决历史测试脏数据导致点赞显示 0 的问题
        Long count = stringRedisTemplate.opsForZSet().zCard(key);
        if (count != null && count > 0) {
            blog.setLiked(count.intValue());
        } else if (blog.getLiked() == null || blog.getLiked() < 0) {
            blog.setLiked(0);
        }

        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 判断登录用户的 userId 是否在 ZSet 的成员中（通过 score 是否为 null 快速判定）
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result<?> deleteBlog(Long id) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.error("请先登录！");
        }
        Blog blog = blogMapper.getById(id);
        if (blog == null) {
            return Result.error("探店笔记不存在！");
        }
        if (!blog.getUserId().equals(user.getId())) {
            return Result.error("您无权删除他人的探店笔记！");
        }
        int row = blogMapper.deleteById(id);
        if (row > 0) {
            // 清理该笔记的 Redis 点赞排行榜与缓存记录
            stringRedisTemplate.delete("blog:liked:" + id);
            return Result.success();
        }
        return Result.error("删除失败！");
    }
}
