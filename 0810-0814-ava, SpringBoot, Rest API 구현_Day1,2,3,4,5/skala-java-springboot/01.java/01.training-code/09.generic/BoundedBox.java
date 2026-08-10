// -------------------------------------------------------
// Upper Bound 적용 클래스: <T extends Number>
//   Number 및 그 하위 타입(Integer, Double, Long ...)만 허용
// -------------------------------------------------------
public class BoundedBox<T extends Number> {
    private T item;

    public BoundedBox(T item) {
        this.item = item;
    }

    public T getItem() {
        return item;
    }
}
