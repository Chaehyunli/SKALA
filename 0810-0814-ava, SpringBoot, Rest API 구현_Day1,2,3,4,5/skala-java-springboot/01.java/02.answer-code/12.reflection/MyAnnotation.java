import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 리플렉션 예제용 커스텀 어노테이션
 *  - @MyAnnotation : 클래스/메서드에 부착 가능
 *  - RetentionPolicy.RUNTIME : 런타임까지 유지 (리플렉션으로 읽기 위해 필수)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MyAnnotation {
    String value() default "default";
    String author() default "unknown";
}
