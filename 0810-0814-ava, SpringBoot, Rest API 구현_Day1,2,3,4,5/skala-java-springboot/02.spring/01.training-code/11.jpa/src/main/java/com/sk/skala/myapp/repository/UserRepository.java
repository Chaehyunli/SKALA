package com.sk.skala.myapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sk.skala.myapp.domain.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
