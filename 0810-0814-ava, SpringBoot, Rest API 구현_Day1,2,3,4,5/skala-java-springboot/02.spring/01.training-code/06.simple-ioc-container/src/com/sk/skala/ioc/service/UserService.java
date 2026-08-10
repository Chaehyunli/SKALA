package com.sk.skala.ioc.service;

import com.sk.skala.ioc.annotation.Service;

/**
 * UserService - DatabaseService 에 의존하는 서비스.
 * IoC 컨테이너가 생성자 파라미터 타입을 분석해서 DatabaseService 를 주입한다.
 */
@Service
public class UserService {

    private final DatabaseService databaseService; // 생성자 주입 필드

    // 생성자 주입 - IoC 컨테이너가 이 생성자를 통해 DatabaseService 를 자동 주입
    public UserService(DatabaseService databaseService) {
        this.databaseService = databaseService;
        System.out.println("  UserService 생성자 호출 (DatabaseService 주입됨)");
    }

    public String getUser(String name) {
        return databaseService.findUser(name);
    }

    @Override
    public String toString() {
        return "UserService@" + Integer.toHexString(hashCode())
                + " -> (databaseService=" + databaseService + ")";
    }
}
