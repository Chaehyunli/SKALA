package com.skala.helpdesk.repository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 실습용 사용자 목록. 계좌처럼 서버 메모리에만 존재하며 user1·user2를 기본 제공한다. */
@Repository
public class UserRepository {

    private final Set<String> userIds = ConcurrentHashMap.newKeySet();

    public UserRepository() {
        userIds.add("user1");
        userIds.add("user2");
    }

    public List<String> findAll() {
        return userIds.stream().sorted().toList();
    }

    public boolean exists(String userId) {
        return userIds.contains(userId);
    }

    public boolean add(String userId) {
        return userIds.add(userId);
    }
}
