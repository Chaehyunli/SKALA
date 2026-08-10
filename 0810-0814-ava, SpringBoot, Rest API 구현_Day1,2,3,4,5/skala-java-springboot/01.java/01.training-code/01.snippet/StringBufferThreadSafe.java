public class StringBufferThreadSafe {

    // 멀티 스레드가 공유하는 StringBuffer (스레드 안전)
    private static StringBuffer sharedBuffer = new StringBuffer();

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== StringBuffer 멀티 스레드 예제 ===\n");

        // 3개의 스레드가 공유 StringBuffer에 동시에 데이터를 추가
        Thread t1 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                sharedBuffer.append("[Thread-1: " + i + "] ");
                System.out.println("Thread-1 추가: " + i);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                sharedBuffer.append("[Thread-2: " + i + "] ");
                System.out.println("Thread-2 추가: " + i);
            }
        });

        Thread t3 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                sharedBuffer.append("[Thread-3: " + i + "] ");
                System.out.println("Thread-3 추가: " + i);
            }
        });

        t1.start();
        t2.start();
        t3.start();

        // 모든 스레드가 끝날 때까지 대기
        t1.join();
        t2.join();
        t3.join();

        System.out.println("\n=== 최종 결과 (StringBuffer는 스레드 안전) ===");
        System.out.println(sharedBuffer.toString());
        System.out.println("\n총 길이: " + sharedBuffer.length());
    }
}
