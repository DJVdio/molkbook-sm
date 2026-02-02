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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserService userService;
    private final AIGenerationService aiGenerationService;

    /**
     * 获取帖子的评论（分页）- 只返回顶级评论，带嵌套回复
     */
    public Page<CommentDTO> getCommentsByPostId(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtAsc(postId, pageable);
        return comments.map(this::toDTOWithReplies);
    }

    /**
     * 获取帖子的所有顶级评论（带嵌套回复）
     */
    public List<CommentDTO> getTopLevelCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtAsc(postId);
        return comments.stream().map(this::toDTOWithReplies).collect(Collectors.toList());
    }

    /**
     * 获取帖子的所有评论（扁平结构）
     */
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    /**
     * 根据ID获取评论
     */
    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    /**
     * 创建顶级评论
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
     * 创建回复评论
     */
    @Transactional
    public Comment createReply(Post post, User user, String content, Comment parentComment) {
        // 验证父评论属于同一帖子
        if (!parentComment.getPost().getId().equals(post.getId())) {
            throw new IllegalArgumentException("Parent comment does not belong to the specified post");
        }

        Comment reply = Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .parent(parentComment)
                .aiGenerated(true)
                .build();
        Comment savedReply = commentRepository.save(reply);

        // 更新帖子的评论数
        post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
        postRepository.save(post);

        return savedReply;
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
     * AI 生成回复
     */
    @Transactional
    public Comment generateReply(Post post, User replier, Comment parentComment) {
        String content = aiGenerationService.generateReplyContent(replier, post, parentComment);
        if (content != null && !content.isEmpty()) {
            return createReply(post, replier, content, parentComment);
        }
        return null;
    }

    /**
     * 转换为 DTO（不含子评论）
     */
    public CommentDTO toDTO(Comment comment) {
        CommentDTO.CommentDTOBuilder builder = CommentDTO.builder()
                .id(comment.getId())
                .user(userService.toDTO(comment.getUser()))
                .content(comment.getContent())
                .aiGenerated(comment.getAiGenerated())
                .createdAt(comment.getCreatedAt());

        // 如果有父评论，设置父评论ID和被回复的用户
        if (comment.getParent() != null) {
            builder.parentId(comment.getParent().getId());
            builder.replyToUser(userService.toDTO(comment.getParent().getUser()));
        }

        return builder.build();
    }

    /**
     * 转换为 DTO（包含子评论）
     */
    public CommentDTO toDTOWithReplies(Comment comment) {
        return toDTOWithReplies(comment, 0);
    }

    private static final int MAX_REPLY_DEPTH = 5;  // 最大嵌套深度

    /**
     * 转换为 DTO（包含子评论，带深度限制）
     */
    private CommentDTO toDTOWithReplies(Comment comment, int depth) {
        CommentDTO dto = toDTO(comment);

        // 超过最大深度则不再获取子评论
        if (depth >= MAX_REPLY_DEPTH) {
            return dto;
        }

        // 获取并转换子评论
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId());
        if (!replies.isEmpty()) {
            dto.setReplies(replies.stream()
                    .map(reply -> toDTOWithReplies(reply, depth + 1))
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
