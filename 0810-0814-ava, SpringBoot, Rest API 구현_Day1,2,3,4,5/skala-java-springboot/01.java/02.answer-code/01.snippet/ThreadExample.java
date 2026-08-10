public class ThreadExample {
    public static void main(String[] args) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("1초 후에 실행되는 작업");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        long startTime = System.currentTimeMillis();
        long beforeMemory = Runtime.getRuntime().freeMemory();
        System.out.println("스레드 시작 전 가용 메모리: " + beforeMemory + " bytes");

        t.start();
        System.out.println("join() 호출 전");
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long afterMemory = Runtime.getRuntime().freeMemory();
        System.out.println("스레드 종료 후 가용 메모리: " + afterMemory + " bytes");
        System.out.println("스레드 실행 소요 시간: " + (endTime - startTime) + " ms");
        System.out.println("메인 스레드 종료");
    }
}

 