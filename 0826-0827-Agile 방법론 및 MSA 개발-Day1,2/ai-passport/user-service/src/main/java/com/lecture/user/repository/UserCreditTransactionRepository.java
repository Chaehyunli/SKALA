package com.lecture.user.repository;

import com.lecture.user.entity.UserCreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCreditTransactionRepository extends JpaRepository<UserCreditTransaction, Long> {
}
