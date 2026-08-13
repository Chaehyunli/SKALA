package com.sk.skala.myapp.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.skala.myapp.aspect.Metrics;
import com.sk.skala.myapp.domain.User;
import com.sk.skala.myapp.dto.UserRequest;
import com.sk.skala.myapp.dto.UserResponse;
import com.sk.skala.myapp.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 모든 사용자 조회
    @Metrics
    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream().map(UserResponse::from).toList();
    }

    // GET: 특정 사용자 가져오기
    @Metrics
    @GetMapping("/users/{id}")
    public UserResponse getUserById(@PathVariable long id) {
        return userService.getUserById(id).map(UserResponse::from).orElse(null);
    }

    // POST: 사용자 추가
    @PostMapping("/users")
    public UserResponse createUser(@RequestBody UserRequest request) {
        User user = userService.createUser(request);
        return UserResponse.from(user);
    }

    // DELETE: 사용자 삭제
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable long id) {
        userService.deleteUser(id);
    }

    // PUT: 사용자 정보 수정
    @PutMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable long id, @RequestBody UserRequest request) {
        return userService.updateUser(id, request).map(UserResponse::from).orElse(null);
    }
}