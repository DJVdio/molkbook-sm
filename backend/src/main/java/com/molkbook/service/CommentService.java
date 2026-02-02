package com.molkbook.service;

import com.molkbook.dto.CommentDTO;
import com.molkbook.entity.Comment;
import com.molkbook.entity.Post;
import com.molkbook.entity.User;
import com.molkbook.repository.CommentRepository;
import com.molkbook.repository.PostRepository;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserService userService;
    private final AIGenerationService aiGenerationService;

    /**
     * 获取帖子的评论（分页）
     */
    public Page<CommentDTO> getCommentsByPostId(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
        return comments.map(this::toDTO);
    }

    /**
     * 获取帖子的所有评论
     */
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    /**
     * 创建评论
     */
    @Transactional
    public Comment createComment(Post post, User user, String content) {
        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .aiGenerated(true)
                .build();
        Comment savedComment = commentRepository.save(comment);

        // 更新帖子的评论数
        post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
        postRepository.save(post);

        return savedComment;
    }

    /**
     * AI 生成评论
     */
    @Transactional
    public Comment generateComment(Post post, User commenter) {
        String content = aiGenerationService.generateCommentContent(commenter, post);
        if (content != null && !content.isEmpty()) {
            return createComment(post, commenter, content);
        }
        return null;
    }

    /**
     * 转换为 DTO
     */
    public CommentDTO toDTO(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .user(userService.toDTO(comment.getUser()))
                .content(comment.getContent())
                .aiGenerated(comment.getAiGenerated())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
