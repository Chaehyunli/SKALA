import java.lang.reflect.Method;

/**
 * 리플렉션 어노테이션 읽기 예제용 대상 클래스
 */
@MyAnnotation(value = "UserService", author = "admin")
class AnnotatedService {

    @MyAnnotation(value = "login", author = "security-team")
    public void login(String username) {
        System.out.println(username + " 로그인");
    }

    public void logout() {
        System.out.println("로그아웃");
    }

    @MyAnnotation(value = "getUser")
    public String getUser() {
        return "user";
    }
}

/**
 * Reflection - 어노테이션(Annotation) 읽기 예제
 *  - 클래스에 부착된 어노테이션 읽기
 *  - 메서드에 부착된 어노테이션 읽기 + @MyAnnotation이 있는 메서드만 선별 실행
 */
public class ReflectionAnnotation {
    public static void main(String[] args) throws Exception {

        Class<?> clazz = AnnotatedService.class;

        // 1. 클래스 레벨 어노테이션 읽기
        System.out.println("=== 클래스 어노테이션 ===");
        if (clazz.isAnnotationPresent(MyAnnotation.class)) {
            MyAnnotation ann = clazz.getAnnotation(MyAnnotation.class);
            System.out.println("value  : " + ann.value());
            System.out.println("author : " + ann.author());
        }

        // 2. 메서드 레벨 어노테이션 읽기
        System.out.println("\n=== 메서드 어노테이션 ===");
        AnnotatedService service = new AnnotatedService();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MyAnnotation.class)) {
                MyAnnotation ann = method.getAnnotation(MyAnnotation.class);
                System.out.printf("메서드: %-10s | value: %-12s | author: %s%n",
                        method.getName(), ann.value(), ann.author());
            } else {
                System.out.printf("메서드: %-10s | @MyAnnotation 없음%n", method.getName());
            }
        }

        // 3. @MyAnnotation이 붙은 메서드만 선별 실행 (파라미터 없는 것)
        System.out.println("\n=== @MyAnnotation 메서드 선별 실행 ===");
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MyAnnotation.class)
                    && method.getParameterCount() == 0) {
                Object result = method.invoke(service);
                System.out.println(method.getName() + "() 결과: " + result);
            }
        }
    }
}
