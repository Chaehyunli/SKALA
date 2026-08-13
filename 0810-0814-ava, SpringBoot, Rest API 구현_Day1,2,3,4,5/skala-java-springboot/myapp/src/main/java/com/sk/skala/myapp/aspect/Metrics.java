package com.sk.skala.myapp.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메소드 실행 시간 측정을 적용하기 위한 커스텀 애노테이션.
 *
 * 이 애노테이션이 붙은 메소드는 {@link MetricsAnnotationAspect}에 의해
 * 실행 전/후 시각과 총 소요 시간이 로그로 출력된다.
 *
 * 사용 예:
 * <pre>
 * {@code
 * @Metrics
 * public List<User> getAllUsers() { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Metrics {
}
