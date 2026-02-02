package com.molkbook.config;

import com.molkbook.entity.Post;
import com.molkbook.repository.CommentRepository;
import com.molkbook.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用启动时同步数据
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        syncCommentCounts();
    }

    /**
     * 同步所有帖子的评论数
     */
    private void syncCommentCounts() {
        log.info("Syncing comment counts for all posts...");
        List<Post> posts = postRepository.findAll();
        int updated = 0;

        for (Post post : posts) {
            long actualCount = commentRepository.countByPostId(post.getId());
            int currentCount = post.getCommentCount() != null ? post.getCommentCount() : 0;

            if (actualCount != currentCount) {
                post.setCommentCount((int) actualCount);
                postRepository.save(post);
                updated++;
                log.debug("Updated post {} comment count: {} -> {}", post.getId(), currentCount, actualCount);
            }
        }

        if (updated > 0) {
            log.info("Synced comment counts for {} posts", updated);
        } else {
            log.info("All post comment counts are in sync");
        }
    }
}
