package com.zj.spy;

import com.zj.demo.AppConfig;
import com.zj.demo.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MethodSpy 的契约测试集。
 * <p>
 * 这组测试不是单纯“跑通代码”，而是用于固定 MethodSpy 的核心行为边界：
 * 1) 推荐路径：注解驱动（@MethodSpy）按预期拦截；
 * 2) 兼容路径：历史 DSL 仍然可用，防止升级破坏旧用户；
 * 3) Spring 路径：@Resource 注入下无需手工包代理也能正常工作。
 * <p>
 * 换句话说，这个类是 MethodSpy 的“行为说明书 + 回归防火墙”。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
class MethodSpyTest {

    /**
     * 用接口定义服务契约，确保测试场景贴近 JDK 动态代理的真实约束。
     */
    interface CalculatorService {
        int add(int a, int b);

        int multiply(int a, int b);
    }

    /**
     * 仅为测试服务的最小实现：
     * - add() 打上 @MethodSpy，用于验证“被标注才拦截”；
     * - multiply() 不打注解，用于验证“未标注不拦截”。
     */
    static class CalculatorServiceImpl implements CalculatorService {
        @Override
        @MethodSpy
        public int add(int a, int b) {
            return a + b;
        }

        @Override
        public int multiply(int a, int b) {
            return a * b;
        }
    }

    @Test
    void shouldSpyAnnotatedMethodsOnlyInRecommendedApi() {
        // 推荐 API：Spy.on(instance) + 方法注解驱动。
        List<String> logs = new ArrayList<>();
        CalculatorService original = new CalculatorServiceImpl();

        CalculatorService calculator = Spy.on(original)
                .before(args -> logs.add("before"))
                .after(result -> logs.add("after"))
                .install();

        calculator.add(1, 2);
        calculator.multiply(3, 4);

        // 只应捕获 add() 的 before/after，multiply() 必须保持“静默”。
        assertEquals(List.of("before", "after"), logs);
    }

    @Test
    void shouldKeepDslApiCompatibleForLegacyUsers() {
        // 兼容 API：老版本用户可能仍使用 on(Class, method).target(instance)。
        List<String> logs = new ArrayList<>();

        CalculatorService calculator = Spy.on(CalculatorService.class, "add")
                .target(new CalculatorServiceImpl())
                .before(args -> logs.add("before:add"))
                .after(result -> logs.add("after:add"))
                .install();

        calculator.add(2, 3);
        calculator.multiply(2, 3);

        // 精确命中 add()，确保升级后旧语法行为不变。
        assertEquals(List.of("before:add", "after:add"), logs);
    }

    @Test
    void shouldWorkWithSpringResourceStyleInjection() {
        // Spring 容器路径：验证 @Resource 注入后可直接调用，不需要手工 Spy.on(...) 包装。
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(AppConfig.class, ResourceClient.class);
            context.refresh();

            ResourceClient client = context.getBean(ResourceClient.class);
            assertEquals(true, client.login());
            assertEquals(3, client.add());
        }
    }

    /**
     * 模拟业务侧真实写法：
     * @Resource private UserService userService;
     * 直接调用 userService.xxx(...)。
     */
    @Component
    static class ResourceClient {
        @Resource
        private UserService userService;

        boolean login() {
            return userService.login("tom", "123456");
        }

        int add() {
            return userService.add(1, 2);
        }
    }
}
