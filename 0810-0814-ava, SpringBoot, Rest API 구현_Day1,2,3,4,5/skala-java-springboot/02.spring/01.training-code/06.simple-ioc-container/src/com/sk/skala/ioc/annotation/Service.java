package com.sk.skala.ioc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IoC 컨테이너가 관리할 서비스 컴포넌트를 표시하는 어노테이션.
 * 이 어노테이션이 붙은 클래스는 컨테이너가 자동으로 인스턴스를 생성하고 의존성을 주입한다.
 */
@Target(ElementType.TYPE)          // 클래스에만 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME) // 런타임에 리플렉션으로 읽을 수 있음
public @interface Service {
    String value() default ""; // 빈 이름 (비어 있으면 클래스명 소문자 첫글자로 자동 결정)
}
