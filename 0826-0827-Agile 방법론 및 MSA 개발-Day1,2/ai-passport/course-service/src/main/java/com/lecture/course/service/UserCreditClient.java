package com.lecture.course.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

/** Calls the User Service to debit credits before an LLM analysis starts. */
@Component
public class UserCreditClient {

    private final RestClient restClient;

    public UserCreditClient(
            @Value("${service.user-service.url:http://localhost:8081}") String userServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
    }

    public void deductAnalysisCredit(Long userId, BigDecimal amount) {
        try {
            restClient.post()
                    .uri("/api/users/internal/{id}/credits/deduct", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreditDeductRequest(amount))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException("크레딧이 부족합니다. 크레딧을 충전한 뒤 다시 시도해주세요.");
            }
            throw new IllegalStateException("크레딧 차감에 실패했습니다. 잠시 후 다시 시도해주세요.", exception);
        }
    }

    private record CreditDeductRequest(BigDecimal amount) {
    }
}
