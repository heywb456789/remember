package com.tomato.naraclub.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Pagination {

    @Schema(description = "현재 페이지 (1부터)")
    private int currentPage;
    @Schema(description = "페이지 크기")
    private int pageSize;
    @Schema(description = "전체 페이지 수")
    private int totalPages;
    @Schema(description = "전체 데이터 건수")
    private long totalElements;

}