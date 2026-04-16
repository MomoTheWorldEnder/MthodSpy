package com.zj.demo;

/**
 * 用户服务契约，供 Spring/JDK 动态代理按接口暴露能力。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
public interface UserService {
    boolean login(String username, String password);

    int add(int a, int b);
}
