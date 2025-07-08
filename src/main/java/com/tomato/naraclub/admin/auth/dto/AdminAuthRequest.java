package com.tomato.naraclub.admin.auth.dto;

import lombok.Data;


@Data
public class AdminAuthRequest {

    private String username;

    private String password;

    private String email;

    private String phoneNumber;

    private String name;

    private Boolean autoLogin;
}
