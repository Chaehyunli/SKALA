package com.sk.skala.myapp.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.skala.myapp.domain.User;


@RestController
@RequestMapping("/api")
public class UserController {
    private List<User> users = new ArrayList<>(List.of(
        new User(1, "alice", "alice@example.com"),
        new User(2, "bob", "bob@example.com"),
        new User(3, "charlie", "charlie@example.com")
    ));
    private long userIdCounter = 4;

    // 모든 사용자 조회
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return users;
    }

    // GET: 특정 사용자 가져오기
    @GetMapping("/users/{id}")
    public User getUserById(@PathVariable long id) {
        return users.stream()
                .filter(user -> user.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // GET: @RequestParam을 사용하여 특정 사용자 검색
    @GetMapping("/users")
    public User getUserByIdParam(@RequestParam long id) {
        return users.stream()
                .filter(user -> user.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // POST: 사용자 추가
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        user.setId(userIdCounter++);
        users.add(user);
        return user;
    }

    // DELETE: 사용자 삭제
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable long id) {
        User userToRemove = users.stream()
                .filter(user -> user.getId() == id)
                .findFirst()
                .orElse(null);
        if (userToRemove != null) {
            users.remove(userToRemove);
        }
    }
    // PUT: 사용자 정보 수정
    @PutMapping("/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        // 기존 사용자 찾기
        Optional<User> existingUser = users.stream()
            .filter(user -> user.getId() == id)
            .findFirst();
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // 새로운 정보로 업데이트 (id는 유지)
            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            // 필요한 다른 필드들도 업데이트
            return user;
        }
        return null;
    }        
        

}