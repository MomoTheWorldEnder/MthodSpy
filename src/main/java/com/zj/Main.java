package com.zj;

import com.zj.demo.AppConfig;
import com.zj.demo.UseCaseRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 项目演示启动入口。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
public class Main {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            UseCaseRunner runner = context.getBean(UseCaseRunner.class);
            runner.run();
        }
    }
}