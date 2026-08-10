package com.sk.skala.ioc.container;

import com.sk.skala.ioc.annotation.Service;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SimpleIoCContainer - 핵심 IoC 컨테이너.
 *
 * 동작 원리:
 *  1. register() 에 클래스 목록을 넘기면
 *  2. @Service 어노테이션을 확인하고
 *  3. 생성자 파라미터 타입을 분석해서 의존 빈을 먼저 생성(재귀)한 뒤
 *  4. 생성자를 통해 주입(Constructor Injection)하여 인스턴스를 만들고
 *  5. ConcurrentHashMap<빈이름, 인스턴스> 에 등록한다.
 */
public class SimpleIoCContainer {

    // 빈 저장소 - 키: 클래스 단순명(예: DatabaseService), 값: 실제 인스턴스
    private final ConcurrentMap<String, Object> beanRegistry = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // public API
    // -----------------------------------------------------------------------

    /**
     * @Service 클래스들을 받아서 의존성을 분석하고 컨테이너에 등록한다.
     */
    public void register(Class<?>... classes) throws Exception {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Service.class)) {
                createBean(clazz);
            }
        }
    }

    /** 타입으로 빈을 꺼낸다. */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> clazz) {
        return (T) beanRegistry.get(clazz.getSimpleName());
    }

    /** 등록된 전체 빈 저장소를 반환한다. */
    public ConcurrentMap<String, Object> getBeanRegistry() {
        return beanRegistry;
    }

    // -----------------------------------------------------------------------
    // 핵심 내부 로직
    // -----------------------------------------------------------------------

    /**
     * 재귀적으로 빈을 생성한다.
     *  - 의존성(생성자 파라미터)이 있으면 먼저 의존 빈을 생성한 뒤 생성자에 주입한다.
     *  - 이미 등록된 빈이면 재생성하지 않고 그대로 반환한다(싱글톤 보장).
     */
    private Object createBean(Class<?> clazz) throws Exception {
        String name = clazz.getSimpleName();

        // 이미 등록되어 있으면 재사용 (싱글톤)
        if (beanRegistry.containsKey(name)) {
            return beanRegistry.get(name);
        }

        // 첫 번째 public 생성자를 사용
        Constructor<?> constructor = clazz.getConstructors()[0];
        Class<?>[] paramTypes = constructor.getParameterTypes();

        Object instance;

        if (paramTypes.length == 0) {
            // ── 의존성 없음: 기본 생성자로 직접 생성 ──────────────────────────
            System.out.println("[컨테이너] '" + name + "' 생성 (의존성 없음)");
            instance = constructor.newInstance();

        } else {
            // ── 의존성 있음: 파라미터 타입별로 빈을 먼저 생성 후 주입 ────────
            System.out.println("[컨테이너] '" + name + "' 생성 준비 - 의존성 " + paramTypes.length + "개 분석 중...");
            Object[] args = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                // 의존 빈이 없으면 재귀 생성
                args[i] = beanRegistry.containsKey(paramTypes[i].getSimpleName())
                        ? beanRegistry.get(paramTypes[i].getSimpleName())
                        : createBean(paramTypes[i]);
                System.out.println("  ↳ 주입 파라미터[" + i + "]: " + paramTypes[i].getSimpleName());
            }
            // 생성자 주입(Constructor Injection)
            instance = constructor.newInstance(args);
        }

        // ConcurrentHashMap 에 등록
        beanRegistry.put(name, instance);
        System.out.println("[컨테이너] '" + name + "' 등록 완료 → beanRegistry 크기: " + beanRegistry.size());
        return instance;
    }

}
