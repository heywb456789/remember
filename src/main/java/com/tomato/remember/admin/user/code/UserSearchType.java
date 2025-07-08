package com.tomato.remember.admin.user.code;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.tomato.remember.application.member.entity.QMember;
import com.tomato.remember.common.interfaces.SearchTypeEnum;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.code
 * @fileName : UserSearchType
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Getter
@AllArgsConstructor
public enum UserSearchType implements SearchTypeEnum {

    USER_NAME("이름", s-> {
        String keyword = "%" + s.trim() + "%";
        return QMember.member.name.likeIgnoreCase(keyword);
    }),
    USER_EMAIL("이메일", s-> {
        String keyword = "%" + s.trim() + "%";
        return QMember.member.email.likeIgnoreCase(keyword);
    }),
    USER_PHONE("전화번호", s-> {
        String keyword = "%" + s.trim() + "%";
        return QMember.member.phoneNumber.likeIgnoreCase(keyword);
    }),
    ;

    private final String key;
    private final Function<String, BooleanExpression> expression;
}
