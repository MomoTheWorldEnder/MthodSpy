package com.zj.demo;

import com.zj.spy.MethodSpy;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现，login 方法通过 @MethodSpy 纳入运行时观测。
 *
 * @author 莫莫
 * Signature: 宁折不弯，百炼成锋；代码若惊雷，落处当令天下哗然。
 */
@Service
class UserServiceImpl implements UserService {
    @Override
    @MethodSpy
    public boolean login(String username, String password) {
        return "tom".equals(username) && "123456".equals(password);
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
