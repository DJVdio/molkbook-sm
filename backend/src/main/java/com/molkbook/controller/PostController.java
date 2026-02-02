package com.molkbook.controller;

import com.molkbook.config.JwtUtil;
import com.molkbook.dto.PostDTO;
import com.molkbook.entity.Post;
import com.molkbook.entity.User;
import com.molkbook.service.PostService;
import com.molkbook.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

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
        Long currentUserId = extractUserId(authHeader);
        return ResponseEntity.ok(postService.getPosts(page, size, sortBy, currentUserId));
    }

    /**
     * 获取用户的帖子
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostDTO>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getUserPosts(userId, page, size));
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long currentUserId = extractUserId(authHeader);
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

        Long userId = extractUserId(authHeader);
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

        Long userId = extractUserId(authHeader);
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

        Long userId = extractUserId(authHeader);
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
                    "error", e.getMessage()
            ));
        }
    }

    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            return null;
        }
        return jwtUtil.extractUserId(token);
    }
}
