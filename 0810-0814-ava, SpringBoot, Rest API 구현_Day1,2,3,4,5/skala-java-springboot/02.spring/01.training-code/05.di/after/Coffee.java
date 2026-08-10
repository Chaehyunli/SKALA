// DI 적용 후: Coffee는 Ame 인터페이스에만 의존
public class Coffee {

    private Ame ame; // 구체 타입이 아닌 인터페이스에 의존

    // 외부에서 구현체를 주입받음 (생성자 주입)
    public Coffee(Ame ame) {
        this.ame = ame;
    }

    // ★ 새로운 커피 종류가 추가되어도 이 메서드는 변경 없음!
    public void coffeeType() {
        ame.get();
    }
}
