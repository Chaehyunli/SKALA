import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Reflection - 클래스 메타 정보(ClassInfo) 조회 예제
 *  - 클래스 이름, 패키지, 슈퍼클래스, 구현 인터페이스
 *  - 생성자 / 필드 / 메서드 전체 목록
 */
public class ReflectionClassInfo {
    public static void main(String[] args) throws Exception {

        Class<?> clazz = Class.forName("Person");

        // 1. 기본 클래스 정보
        System.out.println("=== 클래스 기본 정보 ===");
        System.out.println("클래스명       : " + clazz.getName());
        System.out.println("단순 클래스명  : " + clazz.getSimpleName());
        System.out.println("패키지         : " + clazz.getPackageName());
        System.out.println("슈퍼클래스     : " + clazz.getSuperclass().getSimpleName());
        System.out.println("접근제어자     : " + Modifier.toString(clazz.getModifiers()));

        // 2. 구현 인터페이스
        System.out.println("\n=== 구현 인터페이스 ===");
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length == 0) {
            System.out.println("(없음)");
        } else {
            for (Class<?> iface : interfaces) {
                System.out.println("  " + iface.getSimpleName());
            }
        }

        // 3. 생성자 목록
        System.out.println("\n=== 생성자 목록 ===");
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            System.out.printf("  [%s] %s%n",
                    Modifier.toString(ctor.getModifiers()), ctor.getName());
        }

        // 4. 필드 목록
        System.out.println("\n=== 필드 목록 ===");
        for (Field field : clazz.getDeclaredFields()) {
            System.out.printf("  [%s] %s %s%n",
                    Modifier.toString(field.getModifiers()),
                    field.getType().getSimpleName(),
                    field.getName());
        }

        // 5. 메서드 목록
        System.out.println("\n=== 메서드 목록 ===");
        for (Method method : clazz.getDeclaredMethods()) {
            System.out.printf("  [%s] %s %s()%n",
                    Modifier.toString(method.getModifiers()),
                    method.getReturnType().getSimpleName(),
                    method.getName());
        }
    }
}
