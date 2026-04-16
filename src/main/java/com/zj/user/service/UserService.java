package com.zj.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zj.user.dto.UserSaveRequest;
import com.zj.user.dto.UserUpdateRequest;
import com.zj.user.model.User;

import java.util.List;

public interface UserService extends IService<User> {
    Integer create(UserSaveRequest request);

    boolean update(UserUpdateRequest request);

    boolean delete(Integer id);

    User getById(Integer id);

    List<User> list();
}
