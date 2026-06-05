package com.minitiktok.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "请输入用户名")
    @Size(min = 3, max = 32, message = "用户名长度需为 3-32 位")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 64, message = "密码长度需为 6-64 位")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
