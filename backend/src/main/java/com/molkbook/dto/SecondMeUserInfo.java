package com.molkbook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecondMeUserInfo {
    private String name;
    private String email;
    private String avatar;
    private String bio;
    private String selfIntroduction;
    private String voiceId;
    private Integer profileCompleteness;
}
