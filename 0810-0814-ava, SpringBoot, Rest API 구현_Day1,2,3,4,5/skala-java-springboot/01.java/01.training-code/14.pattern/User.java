public class User {
    // 기존 필드
    private final String name;
    private final int age;
    
    private final String email;
    private final String phoneNumber;

    // private 생성자 -> 외부에서는 오직 Builder를 통해서만 객체 생성 가능
    private User(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
        this.email = builder.email;
        this.phoneNumber = builder.phoneNumber;
    }

    // Getter 메서드들 (필요 시 사용)
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }

    // StringBuilder를 적용한 toString() 메서드
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("User{")
          .append("name='").append(name).append('\'')
          .append(", age=").append(age)
          .append(", email='").append(email).append('\'')
          .append(", phoneNumber='").append(phoneNumber).append('\'')
          .append('}');
        return sb.toString();
    }

    // -- 여기에 Builder 클래스 정의 --

}