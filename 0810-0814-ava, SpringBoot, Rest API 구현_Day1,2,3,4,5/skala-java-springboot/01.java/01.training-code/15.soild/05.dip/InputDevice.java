// 고수준 모듈(Computer)이 의존할 추상화(인터페이스)
// 구체적인 입력 장치(Keyboard, Mouse 등)는 이 인터페이스를 구현
interface InputDevice {
    void type();
}
