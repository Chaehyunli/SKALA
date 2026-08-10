public class PreferredStock implements Stock {
    private double dividendRate;
    private String name;
    private double price;

    public PreferredStock(String name, double price, double dividendRate) {
        this.name = name;
        this.price = price;
        this.dividendRate = dividendRate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public void printInfo() {
        System.out.println("[우선주] 종목: " + name + ", 가격: " + price + "원, 배당률: " + dividendRate + "%");
    }
}
