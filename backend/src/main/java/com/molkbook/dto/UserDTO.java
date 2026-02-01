package com.molkbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String avatar;
    private String bio;
    private String selfIntroduction;
    private LocalDateTime createdAt;
    private Long postCount;
    private Long commentCount;
}
