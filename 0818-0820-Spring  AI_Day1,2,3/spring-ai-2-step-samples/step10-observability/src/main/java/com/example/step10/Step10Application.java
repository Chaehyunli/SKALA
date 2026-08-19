package com.example.step10;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 10 - 애플리케이션 진입점 (포트 8110)
 *
 * <p>{@code spring-boot-starter-actuator}와 {@code micrometer-registry-prometheus}가
 * classpath에 있으므로, 기동과 동시에 아래 관측용 엔드포인트가 함께 열린다.
 * <ul>
 *   <li>http://localhost:8110/actuator/health     — 상태 확인</li>
 *   <li>http://localhost:8110/actuator/metrics    — 수집된 지표 목록</li>
 *   <li>http://localhost:8110/actuator/prometheus — Prometheus 스크랩용 지표</li>
 * </ul>
 */
@SpringBootApplication
public class Step10Application {
    public static void main(String[] args) {
        SpringApplication.run(Step10Application.class, args);
    }
}
