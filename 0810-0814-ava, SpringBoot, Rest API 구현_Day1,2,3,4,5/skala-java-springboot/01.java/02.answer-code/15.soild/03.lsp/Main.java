// 2. 직사각형은 고유의 규칙대로 구현
class Rectangle implements Shape {
    private final int width;
    private final int height;

    public Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getArea() { return width * height; }
}

// 3. 정사각형도 고유의 규칙대로 구현 (더 이상 직사각형의 눈치를 보지 않음)
class Square implements Shape {
    private final int length;

    public Square(int length) {
        this.length = length;
    }

    @Override
    public int getArea() { return length * length; }
}

class Main {
    public static void renderArea(Shape shape) {
        // 어떤 도형(Shape)이 들어오든, 개발자는 뒤통수 맞을 걱정 없이 getArea() 규칙만 믿고 쓰면 됨
        System.out.println("도형의 넓이: " + shape.getArea());
    }

    public static void main(String[] args) {
        Shape rect = new Rectangle(5, 10);
        Shape square = new Square(5);

        renderArea(rect);   // 출력: 50
        renderArea(square); // 출력: 25
    }
}
