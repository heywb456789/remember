package com.tomato.naraclub.common.interfaces;

import com.querydsl.core.types.dsl.BooleanExpression;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * 검색 조건 동적 구성
 */
public interface SearchTypeRequest {

  SearchTypeEnum getSearchType();

  String getSearchText();

  //조건의 널인 경우 치환
  @Hidden
  default boolean nullCondition() {
    return Objects.isNull(getSearchType()) || StringUtils.isBlank(getSearchText());
  }

  /**
   * 이 메서드는 searchType에 맞는 필드 조건을 가져와서 searchText를 동적으로 적용해주는 핵심 부분
   * 실질적인 QueryDSL where 절을 반환함
   */
  @Hidden
  default BooleanExpression isSearchTypeOk() {
    if (nullCondition()) {
      return null;
    }
    return getSearchType().getExpression().apply(getSearchText());
  }
}
