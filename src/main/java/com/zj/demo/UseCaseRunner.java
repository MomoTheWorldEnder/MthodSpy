package com.zj.demo;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 业务调用示例：展示 @Resource 注入后的直接调用形态。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
@Component
public class UseCaseRunner {
    @Resource
    private UserService userService;

    public void run() {
        boolean loginOk = userService.login("tom", "123456");
        int sum = userService.add(10, 20);
    }
}
