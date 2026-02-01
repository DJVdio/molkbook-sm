package com.molkbook.controller;

import com.molkbook.config.JwtUtil;
import com.molkbook.dto.UserDTO;
import com.molkbook.entity.User;
import com.molkbook.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> user = userService.findById(userId);
        return user.map(u -> ResponseEntity.ok(userService.toDTO(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        return user.map(u -> ResponseEntity.ok(userService.toDTO(u)))
                .orElse(ResponseEntity.notFound().build());
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
