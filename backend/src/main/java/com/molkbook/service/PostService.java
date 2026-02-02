package com.molkbook.service;

import com.molkbook.dto.PostDTO;
import com.molkbook.entity.Post;
import com.molkbook.entity.PostLike;
import com.molkbook.entity.User;
import com.molkbook.repository.CommentRepository;
import com.molkbook.repository.PostLikeRepository;
import com.molkbook.repository.PostRepository;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserService userService;
    private final AIGenerationService aiGenerationService;

    /**
     * 获取帖子列表（分页）
     */
    public Page<PostDTO> getPosts(int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        return mapPostsToDTO(posts, currentUserId);
    }

    /**
     * 获取帖子列表（按排序方式）
     * @param sortBy: newest, likes, comments, hot
     */
    public Page<PostDTO> getPosts(int page, int size, String sortBy, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts;

        switch (sortBy) {
            case "likes":
                posts = postRepository.findAllByOrderByLikeCountDesc(pageable);
                break;
            case "comments":
                posts = postRepository.findAllByOrderByCommentCountDesc(pageable);
                break;
            case "hot":
                posts = postRepository.findAllByHotness(pageable);
                break;
            case "newest":
            default:
                posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
                break;
        }

        return mapPostsToDTO(posts, currentUserId);
    }

    private Page<PostDTO> mapPostsToDTO(Page<Post> posts, Long currentUserId) {
        // 批量查询当前用户是否点赞了这些帖子
        List<Long> postIds = posts.getContent().stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        Set<Long> likedPostIds = currentUserId != null
                ? new HashSet<>(postLikeRepository.findLikedPostIdsByUserAndPostIds(currentUserId, postIds))
                : Collections.emptySet();

        return posts.map(post -> toDTO(post, likedPostIds.contains(post.getId())));
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
     * 根据 ID 查找帖子
     */
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
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
     * 点赞帖子
     */
    @Transactional
    public boolean likePost(Long postId, User user) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return false;
        }

        // 检查是否已经点赞
        if (postLikeRepository.existsByPostAndUser(post, user)) {
            return false;
        }

        // 创建点赞记录
        PostLike like = PostLike.builder()
                .post(post)
                .user(user)
                .build();
        postLikeRepository.save(like);

        // 更新帖子的点赞数
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);

        return true;
    }

    /**
     * 取消点赞
     */
    @Transactional
    public boolean unlikePost(Long postId, User user) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return false;
        }

        // 检查是否已经点赞
        if (!postLikeRepository.existsByPostAndUser(post, user)) {
            return false;
        }

        // 删除点赞记录
        postLikeRepository.deleteByPostAndUser(post, user);

        // 更新帖子的点赞数
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postRepository.save(post);

        return true;
    }

    /**
     * 检查用户是否点赞了帖子
     */
    public boolean hasUserLiked(Long postId, User user) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || user == null) {
            return false;
        }
        return postLikeRepository.existsByPostAndUser(post, user);
    }

    /**
     * 更新评论数
     */
    @Transactional
    public void updateCommentCount(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            long count = commentRepository.countByPostId(postId);
            post.setCommentCount((int) count);
            postRepository.save(post);
        }
    }

    /**
     * 转换为 DTO
     */
    public PostDTO toDTO(Post post) {
        return toDTO(post, false);
    }

    public PostDTO toDTO(Post post, boolean liked) {
        return PostDTO.builder()
                .id(post.getId())
                .user(userService.toDTO(post.getUser()))
                .content(post.getContent())
                .topic(post.getTopic())
                .aiGenerated(post.getAiGenerated())
                .createdAt(post.getCreatedAt())
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : (int) commentRepository.countByPostId(post.getId()))
                .liked(liked)
                .build();
    }

    /**
     * 转换为包含评论的 DTO
     */
    public PostDTO toDTOWithComments(Post post, User currentUser) {
        CommentService commentService = new CommentService(commentRepository, userService, null, null);
        boolean liked = currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser);

        return PostDTO.builder()
                .id(post.getId())
                .user(userService.toDTO(post.getUser()))
                .content(post.getContent())
                .topic(post.getTopic())
                .aiGenerated(post.getAiGenerated())
                .createdAt(post.getCreatedAt())
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : (int) commentRepository.countByPostId(post.getId()))
                .liked(liked)
                .comments(commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId())
                        .stream()
                        .map(comment -> commentService.toDTO(comment))
                        .collect(Collectors.toList()))
                .build();
    }

    public PostDTO toDTOWithComments(Post post) {
        return toDTOWithComments(post, null);
    }
}
