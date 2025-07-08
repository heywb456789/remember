package com.tomato.naraclub.application.oneld.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.oneld.dto
 * @fileName : OneIdImageResponse
 * @date : 2025-05-20
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneIdImageResponse {
    private boolean result;
    private int code;
    private String message;
    private List<ImageValue> value;

}
