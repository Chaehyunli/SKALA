public class Main {

    public static void main(String[] args) {
        // 구현체를 외부에서 생성하여 Coffee에 주입 (의존성 주입)
        Coffee hotCoffee     = new Coffee(new Hot());
        Coffee iceCoffee     = new Coffee(new Ice());

        hotCoffee.coffeeType();      // 나는 따뜻한 아메리카노
        iceCoffee.coffeeType();      // 나는 차가운 아메리카노
    }
}
