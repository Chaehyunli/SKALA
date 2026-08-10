package com.sk.skala.ioc.service;

import com.sk.skala.ioc.annotation.Service;

/**
 * DatabaseService - 의존성 없는 최하위 서비스.
 * IoC 컨테이너가 기본 생성자로 직접 생성한다.
 */
@Service
public class DatabaseService {

    // 기본 생성자 - 의존성 없음
    public DatabaseService() {
        System.out.println("  DatabaseService 생성자 호출");
    }

    public String findUser(String name) {
        return "[DB] 사용자 조회 -> " + name;
    }

    @Override
    public String toString() {
        return "DatabaseService@" + Integer.toHexString(hashCode());
    }
}
