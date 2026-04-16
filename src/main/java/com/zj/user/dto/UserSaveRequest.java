package com.zj.user.dto;

import jakarta.validation.constraints.NotBlank;

public class UserSaveRequest {
    @NotBlank(message = "name不能为空")
    private String name;
    @NotBlank(message = "password不能为空")
    private String password;
    @NotBlank(message = "mobile不能为空")
    private String mobile;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
