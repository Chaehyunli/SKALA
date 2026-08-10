// 저수준 모듈: OutputDevice 인터페이스를 구현하는 모니터
class Monitor implements OutputDevice {
    @Override
    public void display() {
        System.out.println("모니터에 화면을 출력");
    }
}
