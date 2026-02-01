package com.molkbook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecondMeShade {
    private Long id;
    private String shadeName;
    private String shadeIcon;
    private String confidenceLevel;
    private String shadeDescription;
    private String shadeDescriptionThirdView;
    private String shadeContent;
    private String shadeContentThirdView;
    private List<String> sourceTopics;
    private String shadeNamePublic;
    private String shadeIconPublic;
    private String confidenceLevelPublic;
    private String shadeDescriptionPublic;
    private String shadeDescriptionThirdViewPublic;
    private String shadeContentPublic;
    private String shadeContentThirdViewPublic;
    private List<String> sourceTopicsPublic;
    private Boolean hasPublicContent;
}
