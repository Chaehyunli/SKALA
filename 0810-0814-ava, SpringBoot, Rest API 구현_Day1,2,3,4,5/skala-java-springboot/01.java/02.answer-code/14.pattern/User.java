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

    // 정적 내부 클래스(Static Inner Class) Builder
    public static class Builder {
        private String name;
        private int age;
        private String email;
        private String phoneNumber;

        // 빌더 생성자 (필수 값이 있다면 여기에 인자로 추가 가능, 현재는 기본 생성자)
        public Builder() {}

        public Builder setName(String name) {
            this.name = name;
            return this; // 메서드 체이닝을 위해 자기 자신 반환
        }

        public Builder setAge(int age) {
            this.age = age;
            return this;
        }

        // 추가 필드에 대한 빌더 메서드
        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        // 최종 객체를 조립하여 반환하는 build() 메서드
        public User build() {
            return new User(this);
        }
    }
}