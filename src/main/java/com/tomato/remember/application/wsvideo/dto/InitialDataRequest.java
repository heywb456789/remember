package com.tomato.remember.application.wsvideo.dto;

import com.tomato.remember.application.wsvideo.code.DeviceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitialDataRequest {

    private Long memberId;
    private Long memorialId;
    private String contactName;
    private DeviceType deviceType;


}