package com.molkbook.scheduler;

import com.molkbook.entity.Post;
import com.molkbook.entity.User;
import com.molkbook.service.CommentService;
import com.molkbook.service.PostService;
import com.molkbook.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class AIContentScheduler {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final Random random = new Random();

    @Value("${scheduler.post-generation.enabled:true}")
    private boolean postGenerationEnabled;

    @Value("${scheduler.comment-generation.enabled:true}")
    private boolean commentGenerationEnabled;

    /**
     * 定时为活跃用户生成帖子
     * 每小时执行一次
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

        // 随机选择一个用户生成帖子
        User selectedUser = activeUsers.get(random.nextInt(activeUsers.size()));

        try {
            Post post = postService.generatePost(selectedUser);
            if (post != null) {
                log.info("Generated post {} for user {}", post.getId(), selectedUser.getId());

                // 触发自动评论
                triggerAutoComments(post);
            }
        } catch (Exception e) {
            log.error("Error generating post for user {}", selectedUser.getId(), e);
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
     * 为帖子触发自动评论
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
            User commenter = otherUsers.get(i);
            try {
                commentService.generateComment(post, commenter);
                log.info("Generated comment for post {} by user {}", post.getId(), commenter.getId());

                // 添加随机延迟，避免评论时间太接近
                Thread.sleep(random.nextInt(5000) + 1000);
            } catch (Exception e) {
                log.error("Error generating comment for post {} by user {}", post.getId(), commenter.getId(), e);
            }
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
