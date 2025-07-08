package com.tomato.remember.admin.user.code;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.tomato.remember.application.member.entity.QMember;
import lombok.Getter;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.code
 * @fileName : AppUserSortType
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Getter
public enum AppUserSortType {
    LATEST(QMember.member.createdAt),
//    POPULAR(new OrderSpecifier<>(Order.DESC, QMember.member.createdAt)),
//    SHARED(new OrderSpecifier<>(Order.DESC, QMember.member.createdAt)),
    ;

    private final Path<? extends Comparable<?>> path;

    AppUserSortType(Path<? extends Comparable<?>> path) {
        this.path = path;
    }

    /**
     * 주어진 방향으로 OrderSpecifier를 만들어 주는 헬퍼
     */
    public OrderSpecifier<?> order(Order direction) {
        return new OrderSpecifier<>(direction, path);
    }
}
