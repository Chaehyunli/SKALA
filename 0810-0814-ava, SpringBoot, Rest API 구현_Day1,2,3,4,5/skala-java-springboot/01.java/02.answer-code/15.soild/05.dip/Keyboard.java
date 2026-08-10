// 저수준 모듈: InputDevice 인터페이스를 구현하는 유선 키보드
class Keyboard implements InputDevice {
    @Override
    public void type() {
        System.out.println("키보드를 사용하여 입력");
    }
}
