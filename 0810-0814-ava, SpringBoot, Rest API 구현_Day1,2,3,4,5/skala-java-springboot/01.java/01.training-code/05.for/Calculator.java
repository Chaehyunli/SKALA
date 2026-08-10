import java.util.Scanner;

public class Calculator {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String[] history = new String[100]; // 최대 100개의 계산 기록 저장
        int historyCount = 0; // 저장된 기록 개수

        boolean isRun = true;
        while (isRun) {
            // 첫 번째 숫자 입력
            System.out.print("첫 번째 숫자: ");
            int firstNumber = scanner.nextInt();

            // 연산자 입력
            System.out.print("연산자(+ - * /): ");
            String operator = scanner.next().trim();

            // 두 번째 숫자 입력
            System.out.print("두 번째 숫자: ");
            int secondNumber = scanner.nextInt();

            // 0으로 나누기 예외 처리
            if (operator.equals("/") && secondNumber == 0) {
                System.out.println("0으로 나눌 수 없습니다.");
                continue;
            }

            // switch문으로 계산 (잘못된 연산자는 default에서 처리)
            double result;
            switch (operator) {
                case "+" -> result = firstNumber + secondNumber;
                case "-" -> result = firstNumber - secondNumber;
                case "*" -> result = firstNumber * secondNumber;
                case "/" -> result = (double) firstNumber / secondNumber;
                default -> {
                    System.out.println("잘못된 연산자입니다.");
                    continue;
                }
            }

            System.out.println("결과: " + result);
            
            String record = firstNumber + " " + operator + " " + secondNumber + " = " + result;
            if (historyCount <history.length) {
                history[historyCount] = record;
                historyCount++;
            }

            // 계속 여부 입력
            System.out.print("계속하려면 c(continue) / 종료하려면 q(quit) 입력: ");
            String choice = scanner.next().trim().toLowerCase();

            if (choice.equals("q") || choice.equals("quit")) {
                isRun = false;
            }
        }

        // for-each로 기록 출력
        System.out.println("\n=== 계산 기록 ===");
        for (String rec : history) {
            if (rec == null) break; // 저장된 만큼만 출력
            System.out.println(rec);
        }
        



        
        scanner.close();
    }
}
