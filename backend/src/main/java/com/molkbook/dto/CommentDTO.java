package com.molkbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private UserDTO user;
    private String content;
    private Boolean aiGenerated;
    private LocalDateTime createdAt;
    private Long parentId;           // 父评论ID
    private UserDTO replyToUser;     // 被回复的用户
    private List<CommentDTO> replies; // 子评论列表
}
