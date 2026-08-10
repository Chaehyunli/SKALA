// DIP의 장점 시연: Computer 코드 수정 없이 새로운 출력 장치 추가 가능
class WirelessDisplay implements OutputDevice {
    @Override
    public void display() {
        System.out.println("무선 디스플레이에 화면을 출력");
    }
}
