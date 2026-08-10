import java.util.ArrayList;
import java.util.List;

public class BoundedGenericsExample {

    // -------------------------------------------------------
    // Upper Bound Wildcard 메서드: <? extends Number>
    //   Number 하위 타입 리스트의 합계를 반환 (BoundedBox에서 이동)
    // -------------------------------------------------------
    public static double sumBox(List<? extends Number> list) {
        double sum = 0;
        for (Number n : list) {
            sum += n.doubleValue();
        }
        return sum;
    }

    // -------------------------------------------------------
    // Lower Bound Wildcard 메서드: <? super Integer>
    //   Integer 및 그 상위 타입(Number, Object)만 허용
    //   → 값을 하나씩 받아서 리스트에 추가
    // -------------------------------------------------------
    public static void addBox(List<? super Integer> list, int value) {
        list.add(value);
    }

    public static void main(String[] args) {

        BoundedBox<Integer> intBox = new BoundedBox<>(100);
        System.out.println("Integer Box: " + intBox.getItem());

        BoundedBox<Double> doubleBox = new BoundedBox<>(3.14);
        System.out.println("Double  Box: " + doubleBox.getItem());

        // BoundedBox<String> stringBox = new BoundedBox<>("hi"); // 컴파일 에러: String은 Number의 하위 타입이 아님

        // ---- Upper Bound Wildcard: sumBox(List<? extends Number>) ----
        List<Integer> intList = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Double>  dblList = new ArrayList<>(List.of(1.1, 2.2, 3.3));

        System.out.println("Integer 리스트 합계: " + sumBox(intList));
        System.out.println("Double  리스트 합계: " + sumBox(dblList));

        // ---- Lower Bound Wildcard: addBox(List<? super Integer>, int) ----
        List<Number> numberList = new ArrayList<>();  // Number는 Integer의 상위 타입 → 허용
        List<Object> objectList = new ArrayList<>();  // Object는 Integer의 상위 타입 → 허용
        // List<Double> doubleList2 = new ArrayList<>();  // 컴파일 에러: Double은 Integer의 상위 타입이 아님

        // addBox를 값 하나씩 여러 번 호출
        addBox(numberList, 10);
        addBox(numberList, 20);
        addBox(numberList, 30);

        addBox(objectList, 100);
        addBox(objectList, 200);
        addBox(objectList, 300);

        System.out.println("Number 리스트 (addBox): " + numberList);
        System.out.println("Object 리스트 (addBox): " + objectList);
    }
}
