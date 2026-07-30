package com.hmdp.service.impl;

import com.github.pagehelper.PageHelper;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements IBlogService {
    private final BlogMapper blogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final IUserService userService;
    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final BlogCommentsMapper blogCommentsMapper;

    @Override
    @SuppressWarnings("resource")
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
    @SuppressWarnings("resource")
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

        //Feed
        List<Long> ids=followMapper.selectFanUserIds(user.getId());
        if(ids==null||ids.isEmpty()){
            return Result.success(blog.getId());
        }
        for(Long id:ids){
            //用户的收信箱
            String key="feed:"+id;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(),System.currentTimeMillis());
        }
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

        List<User> unsortedUsers = userMapper.listByIds(ids);
        Map<Long,User> userMap=unsortedUsers.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<UserDTO> userDTOList = ids.stream()
                .map(userMap::get)
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
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteBlog(Long id) {
        UserDTO user = UserHolder.getUser();
        Blog blog = blogMapper.getById(id);
        if (blog == null) {
            return Result.error("探店笔记不存在！");
        }
        if (!blog.getUserId().equals(user.getId())) {
            return Result.error("您无权删除他人的探店笔记！");
        }
        int row = blogMapper.deleteById(id);
        if (row ==0) {
            return Result.error("删除失败！");
        }
        // 级联删除该笔记下的所有评论，防止产生孤儿脏数据
        blogCommentsMapper.deleteByBlogId(id);

        // 清理该笔记的 Redis 点赞排行榜与缓存记录
        stringRedisTemplate.delete("blog:liked:" + id);
        //清理用户的收信箱
        List<Long> ids=followMapper.selectFanUserIds(user.getId());
        if(ids!=null){
            for(Long userId:ids){
                //用户的收信箱
                String key="feed:"+userId;
                stringRedisTemplate.opsForZSet().remove(key, blog.getId().toString());
            }
        }
        return Result.success();
    }

    @Override
    public Result<?> queryBlogByfollow(Long max, Integer offset) {
        Long userId=UserHolder.getUser().getId();
        String key="feed:"+userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples=stringRedisTemplate.opsForZSet().
                reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        if(typedTuples==null||typedTuples.isEmpty()){
            return Result.success();
        }
        List<Long> ids=new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            String blogId = tuple.getValue();
            ids.add(Long.valueOf(blogId));
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }

        List<Blog> unsortedBlogs=blogMapper.selectByIds(ids);
        Map<Long,Blog> blogMap=unsortedBlogs.stream()
                .collect(Collectors.toMap(Blog::getId, blog -> blog));
        List<Blog> blogs=ids.stream()
                .map(blogMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (Blog blog : blogs) {
            queryBlogUser(blog);
            isBlogLiked(blog);
        }

        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);
        return Result.success(r);
    }
}
