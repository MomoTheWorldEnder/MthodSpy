package com.zj.user.controller;

import com.zj.spy.MethodSpy;
import com.zj.user.dto.ApiResponse;
import com.zj.user.dto.UserSaveRequest;
import com.zj.user.dto.UserUpdateRequest;
import com.zj.user.model.User;
import com.zj.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @MethodSpy
    @PostMapping
    public ApiResponse<Integer> create(@Valid @RequestBody UserSaveRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @MethodSpy
    @PutMapping
    public ApiResponse<Boolean> update(@Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(userService.update(request));
    }

    @MethodSpy
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Integer id) {
        return ApiResponse.ok(userService.delete(id));
    }

    @MethodSpy
    @GetMapping("/{id}")
    public ApiResponse<User> getById(@PathVariable Integer id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @MethodSpy
    @GetMapping
    public ApiResponse<List<User>> list() {
        return ApiResponse.ok(userService.list());
    }
}
