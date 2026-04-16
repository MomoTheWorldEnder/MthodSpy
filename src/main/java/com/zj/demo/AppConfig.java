package com.zj.demo;

import com.zj.spy.EnableMethodSpy;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Demo 场景的 Spring 配置入口，负责组件扫描与 MethodSpy 功能开关。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
@Configuration
@ComponentScan(basePackages = "com.zj.demo")
@EnableMethodSpy
public class AppConfig {
}
