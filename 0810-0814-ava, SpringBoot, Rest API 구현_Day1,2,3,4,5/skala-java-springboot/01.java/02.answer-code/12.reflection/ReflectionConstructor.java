import java.lang.reflect.Constructor;

/**
 * Reflection - 생성자(Constructor) 접근 예제
 *  - public 기본 생성자로 인스턴스 생성
 *  - private 생성자에 setAccessible(true)로 강제 접근
 */
public class ReflectionConstructor {
    public static void main(String[] args) throws Exception {

        Class<?> clazz = Class.forName("Person");

        // 1. public 기본 생성자로 인스턴스 생성
        Constructor<?> publicCtor = clazz.getDeclaredConstructor();
        Person person1 = (Person) publicCtor.newInstance();
        System.out.println("1. public 기본 생성자 생성: " + person1.getClass().getSimpleName());

        // 2. private 생성자 목록 출력
        System.out.println("\n2. 선언된 모든 생성자:");
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            System.out.println("   " + ctor);
        }

        // 3. private 생성자 강제 접근 → 인스턴스 생성
        Constructor<?> privateCtor = clazz.getDeclaredConstructor(String.class, int.class);
        privateCtor.setAccessible(true);
        Person person2 = (Person) privateCtor.newInstance("Alice", 30);
        System.out.println("\n3. private 생성자로 생성된 Person: " + person2);
    }
}
