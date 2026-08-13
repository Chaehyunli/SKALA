package com.sk.skala.myapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.sk.skala.myapp.domain.User;
import com.sk.skala.myapp.dto.UserRequest;
import com.sk.skala.myapp.repository.UserRepository;

import jakarta.validation.Valid;

@Service
@Validated
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 모든 사용자 조회
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 특정 사용자 조회
    public Optional<User> getUserById(long id) {
        return userRepository.findById(id);
    }

    // 사용자 추가
    public User createUser(@Valid UserRequest request) {
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        return userRepository.save(user);
    }

    // 사용자 삭제
    public void deleteUser(long id) {
        userRepository.deleteById(id);
    }

    // 사용자 정보 수정
    public Optional<User> updateUser(long id, @Valid UserRequest request) {
        return userRepository.findById(id).map(user -> {
            user.setName(request.name());
            user.setEmail(request.email());
            return userRepository.save(user);
        });
    }
}
