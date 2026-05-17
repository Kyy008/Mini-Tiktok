package com.minitiktok.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@MapperScan("com.minitiktok.auth.mapper")
@SpringBootApplication
public class AuthBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthBackendApplication.class, args);
    }
}
