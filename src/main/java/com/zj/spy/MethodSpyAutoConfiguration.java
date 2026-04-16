package com.zj.spy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MethodSpy 的自动装配配置类，向容器注册核心后处理器。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
@Configuration
class MethodSpyAutoConfiguration {

    @Bean
    static MethodSpyBeanPostProcessor methodSpyBeanPostProcessor() {
        return new MethodSpyBeanPostProcessor();
    }
}
