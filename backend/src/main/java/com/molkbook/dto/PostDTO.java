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
public class PostDTO {
    private Long id;
    private UserDTO user;
    private String content;
    private String topic;
    private Boolean aiGenerated;
    private LocalDateTime createdAt;
    private Long commentCount;
    private List<CommentDTO> comments;
}
