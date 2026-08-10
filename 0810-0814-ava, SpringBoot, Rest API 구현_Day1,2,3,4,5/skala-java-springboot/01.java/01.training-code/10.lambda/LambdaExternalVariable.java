import java.util.function.*;

public class LambdaExternalVariable {
    public static void main(String[] args) {
        int base = 10;
        Function<Integer, Integer> addBase = (x) -> x + base;
        System.out.println(addBase.apply(5)); // 15

        // 외부 변수는 람다식 내부에서 final 또는 effectively final이어야 함
        base = 20;
    }
}
