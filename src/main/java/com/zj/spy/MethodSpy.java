package com.zj.spy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MethodSpy 的“触发开关”。
 * <p>
 * 当 {@link Spy#on(Object)} 进入注解模式后，只有标注了该注解的方法才会输出详细运行日志。
 * 这样可以避免在大型项目里“全量刷屏”，把监控火力精准集中到关键链路。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodSpy {
    /**
     * 需要脱敏的参数字段关键词（忽略大小写，按包含关系匹配）。
     * 默认覆盖 password/pwd/secret/token 等常见敏感词。
     */
    String[] maskFields() default {"password", "pwd", "secret", "token"};
}
