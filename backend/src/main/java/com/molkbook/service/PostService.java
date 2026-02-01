package com.molkbook.service;

import com.molkbook.dto.PostDTO;
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
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final AIGenerationService aiGenerationService;

    /**
     * 获取帖子列表（分页）
     */
    public Page<PostDTO> getPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        return posts.map(this::toDTO);
    }

    /**
     * 获取用户的帖子
     */
    public Page<PostDTO> getUserPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return posts.map(this::toDTO);
    }

    /**
     * 获取帖子详情
     */
    public Optional<PostDTO> getPostById(Long id) {
        return postRepository.findById(id).map(this::toDTOWithComments);
    }

    /**
     * 创建帖子
     */
    @Transactional
    public Post createPost(User user, String content, String topic) {
        Post post = Post.builder()
                .user(user)
                .content(content)
                .topic(topic)
                .aiGenerated(true)
                .build();
        return postRepository.save(post);
    }

    /**
     * AI 生成帖子
     */
    @Transactional
    public Post generatePost(User user) {
        String content = aiGenerationService.generatePostContent(user);
        if (content != null && !content.isEmpty()) {
            return createPost(user, content, null);
        }
        return null;
    }

    /**
     * 获取最近的帖子
     */
    public List<Post> getRecentPosts(int limit) {
        return postRepository.findRecentPosts(PageRequest.of(0, limit));
    }

    /**
     * 转换为 DTO
     */
    public PostDTO toDTO(Post post) {
        return PostDTO.builder()
                .id(post.getId())
                .user(userService.toDTO(post.getUser()))
                .content(post.getContent())
                .topic(post.getTopic())
                .aiGenerated(post.getAiGenerated())
                .createdAt(post.getCreatedAt())
                .commentCount(commentRepository.countByPostId(post.getId()))
                .build();
    }

    /**
     * 转换为包含评论的 DTO
     */
    public PostDTO toDTOWithComments(Post post) {
        CommentService commentService = new CommentService(commentRepository, userService, null, null);
        return PostDTO.builder()
                .id(post.getId())
                .user(userService.toDTO(post.getUser()))
                .content(post.getContent())
                .topic(post.getTopic())
                .aiGenerated(post.getAiGenerated())
                .createdAt(post.getCreatedAt())
                .commentCount(commentRepository.countByPostId(post.getId()))
                .comments(commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId())
                        .stream()
                        .map(comment -> commentService.toDTO(comment))
                        .collect(Collectors.toList()))
                .build();
    }
}
