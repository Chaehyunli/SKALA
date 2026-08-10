// 2. 직사각형은 고유의 규칙대로 구현
class Rectangle implements Shape {
    //-- 여기에 Shape 인터페이스를 구현하는 코드를 작성하세요.

}

// 3. 정사각형도 고유의 규칙대로 구현 (더 이상 직사각형의 눈치를 보지 않음)
class Square implements Shape {
    //-- 여기에 Shape 인터페이스를 구현하는 코드를 작성하세요.
}

class Main {
    public static void renderArea(Shape shape) {
        // 어떤 도형(Shape)이 들어오든, 개발자는 뒤통수 맞을 걱정 없이 getArea() 규칙만 믿고 쓰면 됨
        System.out.println("도형의 넓이: " + shape.getArea());
    }

    public static void main(String[] args) {
        //--- 여기에 Rectangle과 Square 객체를 생성하고 renderArea() 메서드를 호출하는 코드를 작성하세요.
    }
}
