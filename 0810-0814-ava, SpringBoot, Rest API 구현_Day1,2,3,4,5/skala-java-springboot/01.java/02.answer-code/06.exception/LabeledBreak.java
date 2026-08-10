public class LabeledBreak {
    public static void main(String[] args) {
        OUTER_LOOP: // 💡 바깥쪽 루프에 라벨을 붙임
        for (int i = 1; i <= 9; i++) {
            for (int j = 1; j <= 9; j++) {
                if (i == 5 && j == 5) {
                    System.out.println("5x5에서 탈출!");
                    break OUTER_LOOP; // 바깥쪽 루프까지 바로 탈출
                }
            }
            System.out.println(i + "단 완료");
        }

        System.out.println("루프 종료 후 실행되는 코드");

    }
}