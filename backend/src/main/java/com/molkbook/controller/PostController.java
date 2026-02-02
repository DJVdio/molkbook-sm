package com.molkbook.controller;

import com.molkbook.config.AuthHelper;
import com.molkbook.dto.PostDTO;
import com.molkbook.entity.Post;
import com.molkbook.entity.User;
import com.molkbook.service.AIGenerationService;
import com.molkbook.service.PostService;
import com.molkbook.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PostService postService;
    private final UserService userService;
    private final AuthHelper authHelper;
    private final AIGenerationService aiGenerationService;

    /**
     * 获取帖子列表
     * @param sortBy: newest(默认), likes, comments, hot
     */
    @GetMapping
    public ResponseEntity<Page<PostDTO>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 限制分页大小，防止请求过大
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        Long currentUserId = authHelper.extractUserId(authHeader);
        return ResponseEntity.ok(postService.getPosts(safePage, safeSize, sortBy, currentUserId));
    }

    /**
     * 获取用户的帖子
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostDTO>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        return ResponseEntity.ok(postService.getUserPosts(userId, safePage, safeSize));
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long currentUserId = authHelper.extractUserId(authHeader);
        User currentUser = currentUserId != null ? userService.findById(currentUserId).orElse(null) : null;

        return postService.findById(id)
                .map(post -> ResponseEntity.ok(postService.toDTOWithComments(post, currentUser)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 点赞帖子
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> likePost(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        boolean success = postService.likePost(id, userOpt.get());
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (!success) {
            response.put("message", "Already liked or post not found");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> unlikePost(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        boolean success = postService.unlikePost(id, userOpt.get());
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        return ResponseEntity.ok(response);
    }

    /**
     * AI 生成帖子
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generatePost(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        try {
            Post post = postService.generatePost(userOpt.get());
            if (post != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("post", postService.toDTO(post));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", "Failed to generate post content"
                ));
            }
        } catch (Exception e) {
            log.error("Error generating post", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to generate post"
            ));
        }
    }

    /**
     * AI 流式生成帖子内容
     * 返回 SSE 流，前端可以实时显示生成内容
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generatePostStream(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = authHelper.extractUserId(authHeader);
        if (userId == null) {
            return Flux.just("event: error\ndata: Unauthorized\n\n");
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return Flux.just("event: error\ndata: User not found\n\n");
        }

        User user = userOpt.get();
        StringBuilder contentBuilder = new StringBuilder();

        return aiGenerationService.generatePostContentStream(user)
                .map(chunk -> {
                    contentBuilder.append(chunk);
                    return "data: " + chunk.replace("\n", "\\n") + "\n\n";
                })
                .concatWith(Flux.defer(() -> {
                    // 流结束后，保存帖子并返回完整信息
                    String content = contentBuilder.toString();
                    if (!content.isEmpty()) {
                        try {
                            Post post = postService.createPost(user, content, null);
                            String postJson = String.format(
                                    "{\"id\":%d,\"content\":\"%s\"}",
                                    post.getId(),
                                    content.replace("\"", "\\\"").replace("\n", "\\n")
                            );
                            return Flux.just("event: done\ndata: " + postJson + "\n\n");
                        } catch (Exception e) {
                            log.error("Error saving post", e);
                            return Flux.just("event: error\ndata: Failed to save post\n\n");
                        }
                    }
                    return Flux.just("event: error\ndata: No content generated\n\n");
                }))
                .onErrorResume(e -> {
                    log.error("Error in streaming post generation", e);
                    return Flux.just("event: error\ndata: " + e.getMessage() + "\n\n");
                });
    }
}
