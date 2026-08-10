public class CardPay implements Payment {
    @Override
    public void pay(int amount) {
        System.out.println("[결제 성공] 신용카드로 " + amount + "원이 결제되었습니다.");
    }
}