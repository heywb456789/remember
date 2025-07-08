package com.tomato.naraclub.application.oneld.dto;

import java.util.Map;
import lombok.Data;

@Data
public class OneIdValue {

    private String userKey;
    private String phoneNum;
    private String passwd;
    private String name;
    private String email;
    private String deviceType; //IOS 1 ANDROID 2 DEFAULT 3 ANBAIDU 4 IOSBAIDU 5
    private String userStatus; //login 1 logout 2 default 3
    private boolean register;
    private int online;
    private int longOffline;
    private int logoutUC;
    private String interPhoneNum;
    private String profileImg;
    private String profileImgThume;
    private String nationType;
    private long lastLoginDate;
    private long lastLogoffDate;
    private String decPhoneNum;
    private String wallet_addr;
    private Map<String,Object> coin_info;
    private double supporter_airdrop_amount;
    private String compCode;
    private String registerDate;
    private String timeStamp;
}
