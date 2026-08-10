// DI 적용 전: 문자열로 커피 종류를 지정 → Coffee 내부 if-else에 의존
public class Main {

    public static void main(String[] args) {
        Coffee hotCoffee     = new Coffee("hot");
        Coffee iceCoffee     = new Coffee("ice");

        hotCoffee.coffeeType();      // 나는 따뜻한 아메리카노
        iceCoffee.coffeeType();      // 나는 차가운 아메리카노
    }
}
