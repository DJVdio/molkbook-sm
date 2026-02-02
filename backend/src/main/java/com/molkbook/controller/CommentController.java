package com.molkbook.controller;

import com.molkbook.config.AuthHelper;
import com.molkbook.dto.CommentDTO;
import com.molkbook.entity.Comment;
import com.molkbook.entity.Post;
import com.molkbook.entity.User;
import com.molkbook.repository.PostRepository;
import com.molkbook.service.CommentService;
import com.molkbook.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
