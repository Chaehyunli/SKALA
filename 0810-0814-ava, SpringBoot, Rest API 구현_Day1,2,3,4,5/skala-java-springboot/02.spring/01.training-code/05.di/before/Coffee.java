// DI 적용 전: Coffee가 구체 타입을 내부에서 직접 생성하고 문자열로 종류를 구분
public class Coffee {

    private String kind; // 커피 종류를 문자열로 구분
    private Ame ame; // 구체 타입에 직접 의존

    public Coffee(Ice kind) {
        this.kind = kind;
    }

    // ★ 문제점: ThinIce 같은 새 종류가 추가될 때마다
    //   이 비즈니스 로직(else if 블록)을 직접 수정해야 함!
    public void coffeeType() {
        if (kind.equals("hot")) {
            ame = new Hot();         // 구체 클래스를 내부에서 직접 생성
        } else if (kind.equals("ice")) {
            ame = new Ice();         // 구체 클래스를 내부에서 직접 생성
        } 
        ame.get();
    }
}
