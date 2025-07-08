package com.tomato.naraclub.application.oneld.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.oneld.dto
 * @fileName : ImageValue
 * @date : 2025-05-20
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Getter
@Setter
public class ImageValue {
    private String fullPath;
    private String sender;
    private String target;
    private String name;
    private String originalname;
    private String extension;
    private String size;
    private String thumbnailPath;
    private int chatType;
    private int fileType;
    private String realSize;
    private String thumbSize;
    private String regDate;
    private String expireDate;
    private int seq;
    private int filecheck;
    private int downloadtype;
}
