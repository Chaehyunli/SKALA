import java.util.function.*;

public class LambdaExternalVariable {
    public static void main(String[] args) {
        int base = 10;
        Function<Integer, Integer> addBase = (x) -> x + base;
        System.out.println(addBase.apply(5)); // 15
    }
}
