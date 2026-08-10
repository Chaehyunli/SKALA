public class KakaoPay implements Payment {
    @Override
    public void pay(int amount) {
        System.out.println("[결제 성공] 카카오페이로 " + amount + "원이 결제되었습니다.");
    }
}