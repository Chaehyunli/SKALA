import java.text.DecimalFormat;
import java.util.Scanner;

public class FormatterExample {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n===== Formatter 선택 =====");
            System.out.println("1) string-format");
            System.out.println("2) message-format");
            System.out.println("3) decimal-format");
            System.out.println("4) exit");
            System.out.print("선택: ");

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    stringFormat();
                    break;
                case "2":
                    messageFormat();
                    break;
                case "3":
                    decimalFormat();
                    break;
                case "4":
                    System.out.println("종료합니다.");
                    scanner.close();
                    return;
                default:
                    System.out.println("잘못된 입력입니다. 1~4 중에서 선택하세요.");
            }
        }
    }


    private static void stringFormat() {
        System.out.println("--- [예시 1] 문자열 + 정수 포맷팅 ---");
        String name = "스칼라";
        int age = 30;
        String formatted = String.format("이름: %s, 나이: %d", name, age);
        System.out.println(formatted);  // 이름: 스칼라, 나이: 30

        System.out.println("--- [예시 2] 소수점 2자리 포맷팅 ---");
        double pi = 3.141592;
        System.out.println(String.format("원주율: %.2f", pi));  // 원주율: 3.14

        System.out.println("--- [예시 3] 문자열 정렬 ---");
        System.out.printf("|%10s|\n", "Java");   // 오른쪽 정렬 (총 10자리)
        System.out.printf("|%-10s|\n", "Java");  // 왼쪽 정렬

        System.out.println("--- [예시 4] 숫자 앞자리 0 채우기 ---");
        System.out.printf("%05d\n", 42);  // 00042
    }

    private static void messageFormat() {
        System.out.println("--- [예시 1] 문자열 + 정수 포맷팅 ---");
        String pattern = "이름: {0}, 나이: {1}";
        String result = java.text.MessageFormat.format(pattern, "스칼라", 30);
        System.out.println(result);  // 이름: 스칼라, 나이: 30

        System.out.println("--- [예시 2] 날짜 포맷팅 ---");
        java.util.Date today = new java.util.Date();
        String msg = java.text.MessageFormat.format("오늘은 {0,date,yyyy-MM-dd}입니다.", today);
        System.out.println(msg);  // 출력 예: 오늘은 2025-07-23입니다.

        System.out.println("--- [예시 3] 숫자 포맷팅 ---");
        String msg2 = java.text.MessageFormat.format("총 금액: {0,number,#,##0.00}원", 1234567.8);
        System.out.println(msg2);  // 출력: 총 금액: 1,234,567.80원
    }

    private static void decimalFormat() {
        System.out.println("--- [예시 1] 천단위 구분 + 소수점 2자리 ---");
        double value = 1234.567;
        DecimalFormat df = new DecimalFormat("#,###.00");
        System.out.println(df.format(value));  // 출력: 1,234.57

        System.out.println("--- [예시 2] 앞자리 0 채우기 ---");
        DecimalFormat df2 = new DecimalFormat("00000");
        System.out.println(df2.format(42));    // 출력: 00042

        System.out.println("--- [예시 3] 소수점 2자리 (반올림) ---");
        DecimalFormat df3 = new DecimalFormat("#.00");
        System.out.println(df3.format(3.1));   // 출력: 3.10
        System.out.println(df3.format(33.126)); // 출력: 33.13 (반올림)
        System.out.println(df3.format(0.5));   // 출력: .50

        System.out.println("--- [예시 4] 천단위 구분 (정수) ---");
        DecimalFormat df4 = new DecimalFormat("#,###");
        System.out.println(df4.format(12345678)); // 출력: 12,345,678
    }

}
