package com.molkbook.repository;

import com.molkbook.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.topic = :topic ORDER BY p.createdAt DESC")
    Page<Post> findByTopic(String topic, Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.comments WHERE p.id = :postId")
    Post findByIdWithComments(Long postId);

    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findRecentPosts(Pageable pageable);

    long countByUserId(Long userId);

    // 按点赞数排序
    Page<Post> findAllByOrderByLikeCountDesc(Pageable pageable);

    // 按评论数排序
    Page<Post> findAllByOrderByCommentCountDesc(Pageable pageable);

    // 综合热度排序（点赞数 + 评论数）
    @Query("SELECT p FROM Post p ORDER BY (p.likeCount + p.commentCount) DESC, p.createdAt DESC")
    Page<Post> findAllByHotness(Pageable pageable);
}
