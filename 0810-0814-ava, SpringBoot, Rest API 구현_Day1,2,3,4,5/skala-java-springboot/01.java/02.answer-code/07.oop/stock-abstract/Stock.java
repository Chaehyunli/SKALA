public abstract class Stock {
    private String name;
    private double price;

    protected Stock(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public abstract void printInfo();
}
