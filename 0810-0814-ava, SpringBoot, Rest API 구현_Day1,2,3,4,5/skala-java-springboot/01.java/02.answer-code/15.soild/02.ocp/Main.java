// 결제 처리를 담당하는 서비스 클래스 (인터페이스에만 의존하므로 변경에 닫혀 있음)
class PaymentService {
    public void processPayment(Payment payment, int amount) {
        // 어떤 결제 수단 클래스가 들어오든 이 코드는 절대 변경되지 않습니다.
        payment.pay(amount); 
    }
}

public class Main {
    public static void main(String[] args) {
        PaymentService paymentService = new PaymentService();

        // 1. 카카오페이 결제 시도
        Payment kakao = new KakaoPay();
        paymentService.processPayment(kakao, 15000);

        // 2. 신용카드 결제 시도
        Payment card = new CardPay();
        paymentService.processPayment(card, 53000);

        // 3. OCP를 증명하는 핵심: 기존 코드를 전혀 고치지 않고 새로 추가된 NaverPay를 바로 수용함
        Payment naver = new NaverPay();
        paymentService.processPayment(naver, 21000);
    }
}
