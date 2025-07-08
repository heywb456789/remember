package com.tomato.naraclub.admin.user.code;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.tomato.naraclub.admin.user.entity.QAdmin;
import com.tomato.naraclub.common.interfaces.SearchTypeEnum;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.code
 * @fileName : AdminSearchType
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Getter
@AllArgsConstructor
public enum AdminSearchType implements SearchTypeEnum {

    ADMIN_NAME("이름", QAdmin.admin.name::eq),
    ADMIN_EMAIL("이메일", QAdmin.admin.email::eq),
    ADMIN_PHONE("전화번호", QAdmin.admin.phoneNumber::eq)
    ;

    private final String key;
    private final Function<String, BooleanExpression> expression;

}
