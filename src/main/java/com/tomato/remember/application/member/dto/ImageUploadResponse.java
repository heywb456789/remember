package com.tomato.remember.application.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    private String imageUrl;           // 업로드된 이미지 URL
    private Integer sortOrder;         // 정렬 순서
    private Long fileSize;            // 파일 크기
    private String originalFilename;   // 원본 파일명
    private boolean aiProcessed;       // AI 처리 여부
    private int totalImages;          // 총 업로드된 이미지 수
    private int validImages;          // AI 처리 완료된 이미지 수
    private boolean canStartVideoCall; // 영상통화 가능 여부
}