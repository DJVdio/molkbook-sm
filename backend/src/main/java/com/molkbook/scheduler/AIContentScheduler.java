package com.molkbook.scheduler;

import com.molkbook.entity.Post;
import com.molkbook.entity.User;
import com.molkbook.service.CommentService;
import com.molkbook.service.PostService;
import com.molkbook.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class AIContentScheduler {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final Random random = new Random();

    // 使用 ScheduledExecutorService 替代 Thread.sleep
    private final java.util.concurrent.ScheduledExecutorService scheduler =
            java.util.concurrent.Executors.newScheduledThreadPool(2);

    @Value("${scheduler.post-generation.enabled:true}")
    private boolean postGenerationEnabled;

    @Value("${scheduler.comment-generation.enabled:true}")
    private boolean commentGenerationEnabled;

    @Value("${scheduler.like-generation.enabled:true}")
    private boolean likeGenerationEnabled;

    /**
     * 定时为活跃用户生成帖子
     * 每小时执行一次，随机 20% 的用户会发帖
     */
    @Scheduled(cron = "${scheduler.post-generation.cron:0 0 * * * *}")
    public void generatePostsForActiveUsers() {
        if (!postGenerationEnabled) {
            log.debug("Post generation is disabled");
            return;
        }

        log.info("Starting scheduled post generation...");

        List<User> activeUsers = userService.findActiveUsers();
        if (activeUsers.isEmpty()) {
            log.info("No active users found for post generation");
            return;
        }

        // 随机 20% 的用户会发帖（至少 1 个）
        int numPosters = Math.max(1, (int) Math.ceil(activeUsers.size() * 0.2));
        log.info("Will generate posts for {} users out of {}", numPosters, activeUsers.size());

        // 打乱用户列表，取前 numPosters 个
        java.util.Collections.shuffle(activeUsers);
        List<User> selectedUsers = activeUsers.subList(0, Math.min(numPosters, activeUsers.size()));

        for (int i = 0; i < selectedUsers.size(); i++) {
            final User user = selectedUsers.get(i);
            final int delaySeconds = i * 10; // 每个用户间隔 10 秒，避免同时请求 API

            scheduler.schedule(() -> {
                try {
                    Post post = postService.generatePost(user);
                    if (post != null) {
                        log.info("Generated post {} for user {}", post.getId(), user.getId());
                    }
                } catch (Exception e) {
                    log.error("Error generating post for user {}", user.getId(), e);
                }
            }, delaySeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * 定时为最近的帖子生成点赞
     * 每小时的第15分钟执行
     */
    @Scheduled(cron = "${scheduler.like-generation.cron:0 15 * * * *}")
    public void generateLikesForRecentPosts() {
        if (!likeGenerationEnabled) {
            log.debug("Like generation is disabled");
            return;
        }

        log.info("Starting scheduled like generation...");

        // 获取最近的帖子
        List<Post> recentPosts = postService.getRecentPosts(10);
        if (recentPosts.isEmpty()) {
            log.info("No recent posts found for like generation");
            return;
        }

        // 为每个帖子触发自动点赞
        for (Post post : recentPosts) {
            triggerAutoLikes(post);
        }
    }

    /**
     * 定时为最近的帖子生成评论
     * 每小时的第30分钟执行
     */
    @Scheduled(cron = "${scheduler.comment-generation.cron:0 30 * * * *}")
    public void generateCommentsForRecentPosts() {
        if (!commentGenerationEnabled) {
            log.debug("Comment generation is disabled");
            return;
        }

        log.info("Starting scheduled comment generation...");

        // 获取最近的帖子
        List<Post> recentPosts = postService.getRecentPosts(10);
        if (recentPosts.isEmpty()) {
            log.info("No recent posts found for comment generation");
            return;
        }

        // 随机选择一个帖子
        Post selectedPost = recentPosts.get(random.nextInt(recentPosts.size()));

        // 为该帖子生成评论
        triggerAutoComments(selectedPost);
    }

    /**
     * 为帖子触发自动点赞（AI 分身自动点赞）
     */
    private void triggerAutoLikes(Post post) {
        if (!likeGenerationEnabled) {
            return;
        }

        // 获取除帖子作者外的其他用户
        List<User> otherUsers = userService.findRandomUsersExcluding(post.getUser().getId());
        if (otherUsers.isEmpty()) {
            log.debug("No other users available to like post {}", post.getId());
            return;
        }

        // 随机选择 1-5 个用户来点赞（概率性，不是每个人都会点赞）
        int numLikers = Math.min(random.nextInt(5) + 1, otherUsers.size());

        for (int i = 0; i < numLikers; i++) {
            // 50% 概率点赞，模拟真实行为
            if (random.nextBoolean()) {
                final User liker = otherUsers.get(i);
                final int delaySeconds = (i + 1) * (random.nextInt(3) + 1);

                scheduler.schedule(() -> {
                    try {
                        boolean success = postService.likePost(post.getId(), liker);
                        if (success) {
                            log.info("AI auto-liked post {} by user {}", post.getId(), liker.getId());
                        }
                    } catch (Exception e) {
                        log.error("Error auto-liking post {} by user {}", post.getId(), liker.getId(), e);
                    }
                }, delaySeconds, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 为帖子触发自动评论（异步执行，不阻塞调度线程）
     */
    private void triggerAutoComments(Post post) {
        // 获取除帖子作者外的其他用户
        List<User> otherUsers = userService.findRandomUsersExcluding(post.getUser().getId());
        if (otherUsers.isEmpty()) {
            log.debug("No other users available to comment on post {}", post.getId());
            return;
        }

        // 随机选择 1-3 个用户来评论
        int numCommenters = Math.min(random.nextInt(3) + 1, otherUsers.size());

        for (int i = 0; i < numCommenters; i++) {
            final User commenter = otherUsers.get(i);
            final int delaySeconds = (i + 1) * (random.nextInt(5) + 1);

            // 使用 ScheduledExecutorService 异步延迟执行，不阻塞调度线程
            scheduler.schedule(() -> {
                try {
                    commentService.generateComment(post, commenter);
                    log.info("Generated comment for post {} by user {}", post.getId(), commenter.getId());
                } catch (Exception e) {
                    log.error("Error generating comment for post {} by user {}", post.getId(), commenter.getId(), e);
                }
            }, delaySeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * 手动触发为指定用户生成帖子
     */
    public Post triggerPostGeneration(Long userId) {
        return userService.findById(userId)
                .map(user -> {
                    try {
                        Post post = postService.generatePost(user);
                        if (post != null) {
                            log.info("Manually triggered post {} for user {}", post.getId(), user.getId());
                        }
                        return post;
                    } catch (Exception e) {
                        log.error("Error in manual post generation for user {}", user.getId(), e);
                        return null;
                    }
                })
                .orElse(null);
    }
}
