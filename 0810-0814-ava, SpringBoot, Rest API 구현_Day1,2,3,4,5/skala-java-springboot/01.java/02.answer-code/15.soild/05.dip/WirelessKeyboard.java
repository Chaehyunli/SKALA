// DIP의 장점 시연: Computer 코드 수정 없이 새로운 입력 장치 추가 가능
class WirelessKeyboard implements InputDevice {
    @Override
    public void type() {
        System.out.println("무선 키보드를 사용하여 입력");
    }
}
