package com.molkbook.repository;

import com.molkbook.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // 只获取顶级评论（没有父评论的）
    List<Comment> findByPostIdAndParentIsNullOrderByCreatedAtAsc(Long postId);

    Page<Comment> findByPostIdAndParentIsNullOrderByCreatedAtAsc(Long postId, Pageable pageable);

    // 获取某个评论的回复
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    long countByPostId(Long postId);

    long countByUserId(Long userId);
}
