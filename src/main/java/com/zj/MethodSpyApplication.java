package com.zj;

import com.zj.spy.EnableMethodSpy;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableMethodSpy
@SpringBootApplication
@MapperScan("com.zj.user.mapper")
public class MethodSpyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MethodSpyApplication.class, args);
    }
}
