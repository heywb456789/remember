package com.tomato.naraclub.admin.user.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.code
 * @fileName : ActivityType
 * @date : 2025-05-14
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Getter
@RequiredArgsConstructor
public enum ActivityType {
    BOARD_WRITE,
    BOARD_LIKE,
    BOARD_SHARE,
    BOARD_COMMENT,
    BOARD_READ,
    NEWS_READ,
    NEWS_COMMENT,
    NEWS_SHARE,
    VIDEO_READ,
    VIDEO_COMMENT,
    VIDEO_SHARE,
    VOTE_READ,
    VOTE_JOIN,
    VOTE_COMMENT,

}
