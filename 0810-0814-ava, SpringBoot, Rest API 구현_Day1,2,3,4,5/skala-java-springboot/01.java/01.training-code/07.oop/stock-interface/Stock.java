public interface Stock {
    String getName();

    double getPrice();

    default void printInfo();
}
