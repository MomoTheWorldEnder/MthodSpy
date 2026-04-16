package com.zj.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zj.user.dto.UserSaveRequest;
import com.zj.user.dto.UserUpdateRequest;
import com.zj.user.mapper.UserMapper;
import com.zj.user.model.User;
import com.zj.user.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public Integer create(UserSaveRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setPassword(request.getPassword());
        user.setMobile(request.getMobile());
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        save(user);
        return user.getId();
    }

    @Override
    public boolean update(UserUpdateRequest request) {
        User user = new User();
        user.setId(request.getId());
        user.setName(request.getName());
        user.setPassword(request.getPassword());
        user.setMobile(request.getMobile());
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    public boolean delete(Integer id) {
        return removeById(id);
    }

    @Override
    public User getById(Integer id) {
        User user = super.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        return user;
    }

    @Override
    public List<User> list() {
        return list(new QueryWrapper<User>().orderByDesc("id"));
    }
}
