package com.sk.skala.myapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sk.skala.myapp.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
