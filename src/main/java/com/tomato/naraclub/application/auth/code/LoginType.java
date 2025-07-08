package com.tomato.naraclub.application.auth.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.auth.code
 * @fileName : LoginType
 * @date : 2025-05-13
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Getter
@AllArgsConstructor
public enum LoginType {
    LOGIN("로그인"),
    REFRESH("토큰갱신"),
    ;

    private final String displayName;
}
