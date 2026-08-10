// 고수준 모듈: 구체적인 클래스가 아닌 인터페이스(추상화)에 의존
// DIP 준수: 의존성 주입(Constructor Injection)으로 외부에서 구현체를 주입받음
class Computer {
    private final InputDevice inputDevice;   // Keyboard 직접 참조 → 인터페이스 참조
    private final OutputDevice outputDevice; // Monitor 직접 참조 → 인터페이스 참조

    // 생성자 주입: Computer는 어떤 구현체인지 몰라도 됨
    public Computer(InputDevice inputDevice, OutputDevice outputDevice) {
        this.inputDevice = inputDevice;
        this.outputDevice = outputDevice;
    }

    public void operate() {
        inputDevice.type();
        outputDevice.display();
    }
}
