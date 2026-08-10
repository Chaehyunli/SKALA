public class StaticTest {
    static class Example {
        public static final int INITIAL_COUNT = 10;    //Metaspace 의 클래스 메타데이터에 상수 풀에 존재
        private static int count = 0; // Metaspace 클래스 메타데이터에는 Heap 내 java.lang.Class의 Index 정보
        private int instanceId;

        Example() {
            count++;
            instanceId = count + INITIAL_COUNT; 
        }

        public static int getLastInstanceId() {
            return count + INITIAL_COUNT;
        }

        public static int getCount() {
            return count;
        }

        public int getInstanceId() {
            return instanceId;
        }
    }

    public static void main(String[] args) {
        Example e1 = new Example();
        Example e2 = new Example();
        Example e3 = new Example();

        System.out.println("총 생성된 객체 수(count): " + Example.getCount());
        System.out.println("마지막 생성된 instanceId: " + Example.getLastInstanceId());

        System.out.println("e1의 instanceId: " + e1.getInstanceId());
        System.out.println("e2의 instanceId: " + e2.getInstanceId());
        System.out.println("e3의 instanceId: " + e3.getInstanceId());
    }
    
}
