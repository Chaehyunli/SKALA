public class Main {
    public static void main(String[] args) {
        // DIP 준수: 구체적인 구현체는 Main(외부)에서 결정하고 주입
        InputDevice keyboard = new Keyboard();
        OutputDevice monitor = new Monitor();
        Computer computer = new Computer(keyboard, monitor);
        computer.operate();

        System.out.println("---");

        // 확장성 시연: Computer 코드를 전혀 수정하지 않고 무선 키보드로 교체
        InputDevice wirelessKeyboard = new WirelessKeyboard();
        Computer computer2 = new Computer(wirelessKeyboard, monitor);
        computer2.operate();

        System.out.println("---");

        // 확장성 시연: Computer 코드를 전혀 수정하지 않고 무선 디스플레이로 교체
        OutputDevice wirelessDisplay = new WirelessDisplay();
        Computer computer3 = new Computer(wirelessKeyboard, wirelessDisplay);
        computer3.operate();
    }
}
