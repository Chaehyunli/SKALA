public class Main {
    public static void main(String[] args) {
        // 빌더 패턴을 이용한 User 객체 생성 (메서드 체이닝 방식)
        User user = new User.Builder()
                .setName("홍길동")
                .setAge(25)
                .setEmail("hong@example.com")
                .setPhoneNumber("010-1234-5678")
                .build();

        // 생성된 객체 정보 출력 (toString() 호출)
        System.out.println("--- 생성된 유저 정보 ---");
        System.out.println(user);
    }
}
