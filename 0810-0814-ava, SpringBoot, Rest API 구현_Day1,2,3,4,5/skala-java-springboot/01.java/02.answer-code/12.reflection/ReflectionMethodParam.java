import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Reflection - 메서드(Method) 파라미터 포함 호출 예제
 *  - 파라미터 없는 private 메서드 호출
 *  - String 파라미터 포함 private 메서드 호출
 *  - 모든 메서드 메타 정보 출력
 */
public class ReflectionMethodParam {
    public static void main(String[] args) throws Exception {

        Class<?> clazz = Class.forName("Person");

        // 인스턴스 생성 (private 생성자 활용)
        Constructor<?> ctor = clazz.getDeclaredConstructor(String.class, int.class);
        ctor.setAccessible(true);
        Person person = (Person) ctor.newInstance("Dave", 40);

        // 1. 파라미터 없는 private 메서드 호출
        Method sayHello = clazz.getDeclaredMethod("sayHello");
        sayHello.setAccessible(true);
        System.out.print("1. sayHello()        → ");
        sayHello.invoke(person);

        // 2. String 파라미터 포함 private 메서드 호출
        Method sayHelloWithParam = clazz.getDeclaredMethod("sayHello", String.class);
        sayHelloWithParam.setAccessible(true);
        System.out.print("2. sayHello(String)  → ");
        sayHelloWithParam.invoke(person, "Hi");

        // 3. 모든 메서드 메타 정보 출력
        System.out.println("\n3. 선언된 모든 메서드 정보:");
        for (Method method : clazz.getDeclaredMethods()) {
            System.out.printf("   %-20s | 반환타입: %-10s | 파라미터: %s%n",
                    method.getName(),
                    method.getReturnType().getSimpleName(),
                    Arrays.toString(method.getParameterTypes()));
        }
    }
}
