package com.molkbook.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 认证辅助工具类
 * 统一处理 JWT Token 提取逻辑，避免代码重复
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final JwtUtil jwtUtil;

    /**
     * 从 Authorization header 提取用户 ID
     * @param authHeader Authorization header (格式: "Bearer {token}")
     * @return 用户 ID，如果 token 无效则返回 null
     */
    public Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            return null;
        }
        return jwtUtil.extractUserId(token);
    }

    /**
     * 检查 Authorization header 是否有效
     * @param authHeader Authorization header
     * @return true 如果 token 有效
     */
    public boolean isAuthenticated(String authHeader) {
        return extractUserId(authHeader) != null;
    }
}
