package com.molkbook.controller;

import com.molkbook.config.JwtUtil;
import com.molkbook.entity.User;
import com.molkbook.service.SecondMeApiService;
import com.molkbook.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final SecondMeApiService secondMeApiService;
    private final JwtUtil jwtUtil;

    @Value("${secondme.api.oauth.client-id}")
    private String clientId;

    @Value("${secondme.api.oauth.redirect-uri}")
    private String redirectUri;

    /**
     * 获取 OAuth 授权 URL
     */
    @GetMapping("/oauth/url")
    public ResponseEntity<Map<String, String>> getOAuthUrl() {
        log.info("OAuth config - clientId: {}, redirectUri: {}", clientId, redirectUri);

        // 使用 SecondMe 的通用链接
        String oauthUrl = "https://go.second.me/oauth/" +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode("user.info user.info.shades chat", StandardCharsets.UTF_8);

        log.info("Generated OAuth URL: {}", oauthUrl);

        Map<String, String> response = new HashMap<>();
        response.put("url", oauthUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * OAuth 回调处理
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<Map<String, Object>> handleOAuthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {

        if (error != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", error);
            return ResponseEntity.badRequest().body(response);
        }

        if (code == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "No authorization code provided");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 用授权码换取 access token
            String accessToken = secondMeApiService.exchangeCodeForToken(code);
            if (accessToken == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Failed to exchange code for token");
                return ResponseEntity.badRequest().body(response);
            }

            // 创建或更新用户
            User user = userService.createOrUpdateUser(accessToken);

            // 生成 JWT
            String jwt = jwtUtil.generateToken(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", jwt);
            response.put("user", userService.toDTO(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OAuth callback error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 验证 JWT Token
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            return ResponseEntity.ok(response);
        }

        String token = authHeader.substring(7);
        boolean valid = jwtUtil.isTokenValid(token);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);

        if (valid) {
            Long userId = jwtUtil.extractUserId(token);
            userService.findById(userId).ifPresent(user -> {
                response.put("user", userService.toDTO(user));
            });
        }

        return ResponseEntity.ok(response);
    }
}
