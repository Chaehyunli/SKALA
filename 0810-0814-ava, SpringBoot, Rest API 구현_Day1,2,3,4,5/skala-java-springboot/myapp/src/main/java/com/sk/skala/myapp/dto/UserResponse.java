package com.sk.skala.myapp.dto;

import jakarta.validation.constraints.Positive;

import com.sk.skala.myapp.domain.User;

public record UserResponse(
        @Positive(message = "ID는 양수여야 합니다") long id,
        String name,
        String email
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
