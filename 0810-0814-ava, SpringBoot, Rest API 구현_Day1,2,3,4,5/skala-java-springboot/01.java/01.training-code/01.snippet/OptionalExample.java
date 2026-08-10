import java.util.Optional;

public class OptionalExample {

    public static void printName(Optional<String> optName) {
        // 값이 있으면 대문자로 출력, 없으면 "이름 없음"
        String name = optName
                .map(String::toUpperCase)                          // 값이 있으면 대문자로 변환
                .orElse("이름 없음");                                // 값이 없으면 기본값
        System.out.println("결과: " + name);
    }

    public static void emailCheck(Optional<String> email) {
        // 값이 있고 '@' 가 포함된 경우만 소문자로 변환 후 발송
        email.filter(s -> s.contains("@"))                        // 유효한 이메일만
             .map(String::toLowerCase)                            // 소문자로 변환
             .ifPresent(s -> System.out.println("메일 발송: " + s));
    }

    public static void main(String[] args) {
        Optional<String> email1 = Optional.of("hong@gmail.com"); // 값이 있는 경우 (정상 이메일)
        emailCheck(email1);                                      // "메일 발송: hong@gmail.com"

        Optional<String> email2 = Optional.empty();              // 값이 없는 경우
        printName(email2);                                       // "결과: 이름 없음"

        // null 가능성을 가진 변수
        String nullableName = null; // null 가능성을 가진 변수
        Optional<String> name3 = Optional.ofNullable(nullableName);
        emailCheck(name3);                                       // '@' 가 없으므로 발송 안 됨
        printName(name3);                                        // "결과: HONGGILDONG"
    }
}

