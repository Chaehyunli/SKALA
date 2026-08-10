package com.sk.skala.ioc;

import com.sk.skala.ioc.container.SimpleIoCContainer;
import com.sk.skala.ioc.service.DatabaseService;
import com.sk.skala.ioc.service.OrderService;
import com.sk.skala.ioc.service.UserService;

/**
 * Main - IoC 컨테이너 동작을 확인하는 진입점.
 *
 * [의존 관계 구조]
 *   OrderService
 *       └── UserService
 *               └── DatabaseService
 *
 * 개발자가 직접 new 를 호출하지 않고 컨테이너가 알아서 생성·주입한다.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        separator("Simple IoC Container 시작");

        // 1. IoC 컨테이너 생성
        SimpleIoCContainer container = new SimpleIoCContainer();

        // 2. @Service 클래스들을 컨테이너에 등록 (의존성 자동 분석 & 생성자 주입)
        //    등록 순서와 관계없이 컨테이너가 의존성을 재귀적으로 해결한다.
        separator("빈(Bean) 등록 & 의존성 주입");        
        container.register(
                OrderService.class,    // 최상위 - UserService 에 의존
                UserService.class,     // 중간   - DatabaseService 에 의존
                DatabaseService.class  // 최하위 - 의존성 없음
        );

        // 3. 등록된 빈 목록 출력
        separator("등록된 빈 목록 (ConcurrentHashMap)");
        container.getBeanRegistry().forEach((name, bean) ->
                System.out.printf("  %-20s → %s%n", name, bean)
        );

        // 4. 실제 빈을 꺼내서 사용
        separator("빈 사용 예시");
        OrderService orderService = container.getBean(OrderService.class);
        String result = orderService.processOrder("홍길동");
        System.out.println("  " + result);

        // 5. 동일 타입 빈은 싱글톤(같은 인스턴스) 확인
        separator("싱글톤 확인 (같은 인스턴스인지 비교)");
        UserService us1 = container.getBean(UserService.class);
        UserService us2 = container.getBean(UserService.class);
        System.out.println("  getBean(UserService) 1회: " + us1);
        System.out.println("  getBean(UserService) 2회: " + us2);
        System.out.println("  동일 인스턴스? → " + (us1 == us2));

        separator("완료");
    }

    private static void separator(String title) {
        System.out.println("\n========== " + title + " ==========");
    }
}
