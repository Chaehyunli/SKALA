import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Reflection - 필드(Field) 접근 예제
 *  - private 필드 읽기 / 쓰기
 *  - 모든 필드 메타 정보 출력
 */
public class ReflectionField {
    public static void main(String[] args) throws Exception {

        Class<?> clazz = Class.forName("Person");

        // private 생성자로 인스턴스 생성
        Constructor<?> ctor = clazz.getDeclaredConstructor(String.class, int.class);
        ctor.setAccessible(true);
        Person person = (Person) ctor.newInstance("Bob", 25);
        System.out.println("초기 상태: " + person);

        // 1. private 필드 읽기
        Field nameField = clazz.getDeclaredField("name");
        nameField.setAccessible(true);
        String currentName = (String) nameField.get(person);
        System.out.println("\n1. name 필드 읽기: " + currentName);

        // 2. private 필드 쓰기
        nameField.set(person, "Charlie");
        Field ageField = clazz.getDeclaredField("age");
        ageField.setAccessible(true);
        ageField.set(person, 99);
        System.out.println("2. 필드 수정 후: " + person);

        // 3. 모든 필드 메타 정보 출력
        System.out.println("\n3. 선언된 모든 필드 정보:");
        for (Field field : clazz.getDeclaredFields()) {
            System.out.printf("   %-10s | 타입: %-10s | 접근제어자: %s%n",
                    field.getName(),
                    field.getType().getSimpleName(),
                    Modifier.toString(field.getModifiers()));
        }
    }
}
