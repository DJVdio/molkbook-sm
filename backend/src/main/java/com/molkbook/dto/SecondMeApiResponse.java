package com.molkbook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecondMeApiResponse<T> {
    private Integer code;
    private T data;
    private String message;
    private String subCode;
}
