public class NaverPay implements Payment {
    @Override
    public void pay(int amount) {
        System.out.println("[결제 성공] 네이버페이로 " + amount + "원이 결제되었습니다.");
    }
}
