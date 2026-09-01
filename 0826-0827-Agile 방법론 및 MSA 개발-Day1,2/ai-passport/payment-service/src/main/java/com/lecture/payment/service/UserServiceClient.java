package com.lecture.payment.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.user-service-url:http://localhost:8081}") String userServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(userServiceUrl).build();
    }

    public BigDecimal getCredit(Long userId) {
        UserCreditResponse response = restClient.get()
                .uri("/api/users/internal/{id}", userId)
                .retrieve()
                .body(UserCreditResponse.class);

        if (response == null || response.getCredit() == null) {
            throw new IllegalStateException("사용자 크레딧 정보를 불러오지 못했습니다: " + userId);
        }
        return response.getCredit();
    }

    @Getter
    @NoArgsConstructor
    static class UserCreditResponse {
        private BigDecimal credit;
    }
}
