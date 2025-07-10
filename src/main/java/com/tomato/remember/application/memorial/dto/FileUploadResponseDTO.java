package com.tomato.remember.application.memorial.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadResponseDTO {
    
    private String uploadId;
    private String originalFileName;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
    private String fileType;
    
    public static FileUploadResponseDTO of(String uploadId, String originalFileName, 
                                         String fileName, String fileUrl, 
                                         Long fileSize, String contentType, String fileType) {
        return FileUploadResponseDTO.builder()
                .uploadId(uploadId)
                .originalFileName(originalFileName)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .contentType(contentType)
                .fileType(fileType)
                .build();
    }
}