package com.tomato.remember.application.wsvideo.dto;

import com.tomato.remember.application.wsvideo.code.DeviceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRegistrationRequest {

    private String deviceId;
    private DeviceType deviceType;
    private boolean setPrimary = false;
}