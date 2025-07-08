package com.tomato.naraclub.common.interfaces;

import com.querydsl.core.types.dsl.BooleanExpression;
import java.util.function.Function;

/**
 * 검색 조건 동적 구성
 */
public interface SearchTypeEnum extends SearchEnum {

  //enum 각각이 Function<String, BooleanExpression>을 구현하도록 강제함
  //예: title::eq, author.name::like 등
  Function<String , BooleanExpression> getExpression();
}
