package com.zj.spy;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 MethodSpy 的 Spring 自动代理能力。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MethodSpyAutoConfiguration.class)
public @interface EnableMethodSpy {
}
