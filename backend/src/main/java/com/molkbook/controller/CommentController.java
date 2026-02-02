package com.molkbook.controller;

import com.molkbook.config.AuthHelper;
import com.molkbook.dto.CommentDTO;
import com.molkbook.entity.Comment;
import com.molkbook.entity.Post;
import com.molkbook.entity.User;
import com.molkbook.repository.PostRepository;
import com.molkbook.service.AIGenerationService;
import com.molkbook.service.CommentService;
import com.molkbook.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CommentService commentService;
    private final UserService userService;
    private final PostRepository postRepository;
    private final AuthHelper authHelper;
    private final AIGenerationService aiGenerationService;

    /**
     * 获取帖子的评论
     */
    @GetMapping
    public ResponseEntity<Page<CommentDTO>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId, safePage, safeSize));
    }

    /**
     * AI 生成评论
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateComment(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Post not found"));
        }

        try {
            Comment comment = commentService.generateComment(postOpt.get(), userOpt.get());
            if (comment != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("comment", commentService.toDTO(comment));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", "Failed to generate comment content"
                ));
            }
        } catch (Exception e) {
            log.error("Error generating comment", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to generate comment"
            ));
        }
    }

    /**
     * 为帖子生成随机用户的评论（用于自动评论功能）
     * 需要登录认证
     */
    @PostMapping("/generate-random")
    public ResponseEntity<Map<String, Object>> generateRandomComment(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 验证用户登录状态
        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized - please login first"));
        }

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Post not found"));
        }

        Post post = postOpt.get();

        // 获取随机的其他用户（限制返回数量为1）
        List<User> randomUsers = userService.findRandomUsersExcluding(post.getUser().getId(), 1);
        if (randomUsers.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "No other users available to comment"
            ));
        }

        User commenter = randomUsers.get(0);

        try {
            Comment comment = commentService.generateComment(post, commenter);
            if (comment != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("comment", commentService.toDTO(comment));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", "Failed to generate comment content"
                ));
            }
        } catch (Exception e) {
            log.error("Error generating random comment", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to generate comment"
            ));
        }
    }

    /**
     * AI 流式生成评论
     * 返回 SSE 流，前端可以实时显示生成内容
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateCommentStream(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return Flux.just("event: error\ndata: Unauthorized\n\n");
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return Flux.just("event: error\ndata: User not found\n\n");
        }

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return Flux.just("event: error\ndata: Post not found\n\n");
        }

        User user = userOpt.get();
        Post post = postOpt.get();
        StringBuilder contentBuilder = new StringBuilder();

        return aiGenerationService.generateCommentContentStream(user, post)
                .map(chunk -> {
                    contentBuilder.append(chunk);
                    return "data: " + chunk.replace("\n", "\\n") + "\n\n";
                })
                .concatWith(Flux.defer(() -> {
                    // 流结束后，保存评论并返回完整信息
                    String content = contentBuilder.toString();
                    if (!content.isEmpty()) {
                        try {
                            Comment comment = commentService.createComment(post, user, content);
                            String commentJson = String.format(
                                    "{\"id\":%d,\"content\":\"%s\"}",
                                    comment.getId(),
                                    content.replace("\"", "\\\"").replace("\n", "\\n")
                            );
                            return Flux.just("event: done\ndata: " + commentJson + "\n\n");
                        } catch (Exception e) {
                            log.error("Error saving comment", e);
                            return Flux.just("event: error\ndata: Failed to save comment\n\n");
                        }
                    }
                    return Flux.just("event: error\ndata: No content generated\n\n");
                }))
                .onErrorResume(e -> {
                    log.error("Error in streaming comment generation", e);
                    return Flux.just("event: error\ndata: " + e.getMessage() + "\n\n");
                });
    }

    /**
     * AI 生成回复评论
     */
    @PostMapping("/{commentId}/reply/generate")
    public ResponseEntity<Map<String, Object>> generateReply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Post not found"));
        }

        Optional<Comment> commentOpt = commentService.findById(commentId);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Comment not found"));
        }

        try {
            Comment reply = commentService.generateReply(postOpt.get(), userOpt.get(), commentOpt.get());
            if (reply != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("comment", commentService.toDTO(reply));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", "Failed to generate reply content"
                ));
            }
        } catch (Exception e) {
            log.error("Error generating reply", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to generate reply"
            ));
        }
    }

    /**
     * 邀请随机AI用户回复评论
     */
    @PostMapping("/{commentId}/reply/generate-random")
    public ResponseEntity<Map<String, Object>> generateRandomReply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized - please login first"));
        }

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Post not found"));
        }

        Optional<Comment> commentOpt = commentService.findById(commentId);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Comment not found"));
        }

        Post post = postOpt.get();
        Comment parentComment = commentOpt.get();

        // 获取随机的其他用户（排除帖子作者和评论作者）
        List<User> randomUsers = userService.findRandomUsersExcludingMultiple(
                List.of(post.getUser().getId(), parentComment.getUser().getId()), 1);
        if (randomUsers.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "No other users available to reply"
            ));
        }

        User replier = randomUsers.get(0);

        try {
            Comment reply = commentService.generateReply(post, replier, parentComment);
            if (reply != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("comment", commentService.toDTO(reply));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", "Failed to generate reply content"
                ));
            }
        } catch (Exception e) {
            log.error("Error generating random reply", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to generate reply"
            ));
        }
    }

    /**
     * AI 流式生成回复
     */
    @PostMapping(value = "/{commentId}/reply/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateReplyStream(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return Flux.just("event: error\ndata: Unauthorized\n\n");
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return Flux.just("event: error\ndata: User not found\n\n");
        }

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return Flux.just("event: error\ndata: Post not found\n\n");
        }

        Optional<Comment> commentOpt = commentService.findById(commentId);
        if (commentOpt.isEmpty()) {
            return Flux.just("event: error\ndata: Comment not found\n\n");
        }

        User user = userOpt.get();
        Post post = postOpt.get();
        Comment parentComment = commentOpt.get();
        StringBuilder contentBuilder = new StringBuilder();

        return aiGenerationService.generateReplyContentStream(user, post, parentComment)
                .map(chunk -> {
                    contentBuilder.append(chunk);
                    return "data: " + chunk.replace("\n", "\\n") + "\n\n";
                })
                .concatWith(Flux.defer(() -> {
                    String content = contentBuilder.toString();
                    if (!content.isEmpty()) {
                        try {
                            Comment reply = commentService.createReply(post, user, content, parentComment);
                            String replyJson = String.format(
                                    "{\"id\":%d,\"content\":\"%s\",\"parentId\":%d}",
                                    reply.getId(),
                                    content.replace("\"", "\\\"").replace("\n", "\\n"),
                                    parentComment.getId()
                            );
                            return Flux.just("event: done\ndata: " + replyJson + "\n\n");
                        } catch (Exception e) {
                            log.error("Error saving reply", e);
                            return Flux.just("event: error\ndata: Failed to save reply\n\n");
                        }
                    }
                    return Flux.just("event: error\ndata: No content generated\n\n");
                }))
                .onErrorResume(e -> {
                    log.error("Error in streaming reply generation", e);
                    return Flux.just("event: error\ndata: " + e.getMessage() + "\n\n");
                });
    }
}
