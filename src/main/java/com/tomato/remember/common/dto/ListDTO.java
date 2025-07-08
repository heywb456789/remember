package com.tomato.remember.common.dto;

import com.querydsl.jpa.impl.JPAQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Getter
@ToString
@AllArgsConstructor
public class ListDTO<T> {

  @Schema(description = "페이지 정보")
    private Pagination pagination;

  @Schema(description = "검색 탭 카운트")
  private Map<String, Long> counts;

  @Schema(description = "조회 데이터")
  private List<T> data;

  /**
     * JPAQuery<Long> countQuery : 전체 데이터 건수를 조회할 count 쿼리
     * List<T> content        : 요청한 페이지만큼 조회된 데이터
     * Pageable pageable      : 페이지 정보 (page, size)
     */
    public static <T> ListDTO<T> of(JPAQuery<Long> countQuery,
                                    List<T> content,
                                    Pageable pageable) {
        long total = 0;
        Long fetched = countQuery.fetchOne();
        if (fetched != null) {
            total = fetched;
        }

        int pageSize = pageable.getPageSize();
        // Spring Data Pageable pageIndex는 0-based, 화면에서는 1-based
        int currentPage = pageable.isPaged() ? pageable.getPageNumber() + 1 : 1;
        int totalPages = pageSize == 0
            ? 1
            : (int) Math.ceil((double) total / pageSize);

        Pagination pg = new Pagination(currentPage, pageSize, totalPages, total);
        return new ListDTO<>(content, pg);
    }

    /**
     * Spring Data Page<T> 를 바로 변환
     */
    public static <R> ListDTO<R> of(Page<R> page) {
        List<R> content = page.getContent();
        int currentPage = page.getNumber() + 1;
        int pageSize = page.getSize();
        long total = page.getTotalElements();
        int totalPages = page.getTotalPages();

        Pagination pg = new Pagination(currentPage, pageSize, totalPages, total);
        return new ListDTO<>(content, pg);
    }

    /**
     * ResponseDTO 래핑용
     */
    public static <R> ResponseDTO<ListDTO<R>> ok(Page<R> page) {
        return ResponseDTO.ok(ListDTO.of(page));
    }

    // private 생성자
    private ListDTO(List<T> items, Pagination pagination) {
        this.data = items;
        this.pagination = pagination;
    }

    // private 생성자
    public ListDTO(Pagination pagination, List<T> items, Map<String, Long> counts) {
        this.data = items;
        this.pagination = pagination;
        this.counts = counts;
    }
}
