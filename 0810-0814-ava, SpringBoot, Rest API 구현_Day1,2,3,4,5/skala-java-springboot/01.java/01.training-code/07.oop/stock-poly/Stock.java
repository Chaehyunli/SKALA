public class Stock {
    private String name;
    private double price;

    public Stock(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public void printInfo() {
        System.out.println("[일반주] 종목: " + name + ", 가격: " + price + "원");
    }
}
