package com.tomato.remember.application.wsvideo.dto;

import com.tomato.remember.application.wsvideo.code.DeviceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSessionRequest {

    private String contactName;
    private String contactKey;
    private Long memorialId;
    private Long callerId;
    private DeviceType deviceType;
    private String deviceId;
}