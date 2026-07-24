var RANKS = ["A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"];

function drawCard() {
  return RANKS[Math.floor(Math.random() * RANKS.length)];
}

function cardValue(card) {
  if (card === "A") return 11;
  if (card === "J" || card === "Q" || card === "K") return 10;
  return Number(card);
}

function handTotal(cards) {
  var total = cards.reduce(function (sum, card) {
    return sum + cardValue(card);
  }, 0);

  var aceCount = cards.filter(function (card) {
    return card === "A";
  }).length;

  while (total > 21 && aceCount > 0) {
    total -= 10;
    aceCount--;
  }

  return total;
}

function playBlackjack() {
  alert(
    "🃏 블랙잭 게임 규칙\n\n" +
    "나와 딜러가 카드 2장씩 나눠 갖고 시작합니다. 합계가 21을 넘지 않는 선에서 최대한 21에 가깝게 만들면 이깁니다.\n" +
    "J, Q, K는 10으로 계산하고, A(에이스)는 상황에 따라 11 또는 1로 계산됩니다.\n" +
    "\"카드를 더 뽑으시겠습니까?\"에서 확인을 누르면 카드를 한 장 더 받고(Hit), 취소를 누르면 그만 받습니다(Stand).\n" +
    "21을 넘기면(Bust) 즉시 패배하며, 내가 멈추면 딜러가 합계 17 이상이 될 때까지 자동으로 카드를 뽑습니다."
  );

  var player = [drawCard(), drawCard()];
  var dealer = [drawCard(), drawCard()];

  while (handTotal(player) < 21 && confirm(
    "내 카드: " + player.join(", ") + " (합계: " + handTotal(player) + ")\n" +
    "딜러 공개 카드: " + dealer[0] + "\n" +
    "카드를 더 뽑으시겠습니까?"
  )) {
    player.push(drawCard());
  }

  var playerTotal = handTotal(player);
  var result;

  if (playerTotal > 21) {
    result = "버스트! 딜러 승리입니다.";
  } else {
    while (handTotal(dealer) < 17) {
      dealer.push(drawCard());
    }
    var dealerTotal = handTotal(dealer);

    if (dealerTotal > 21 || playerTotal > dealerTotal) {
      result = "플레이어 승리입니다!";
    } else if (playerTotal < dealerTotal) {
      result = "딜러 승리입니다.";
    } else {
      result = "무승부입니다.";
    }
  }

  alert(
    "내 카드: " + player.join(", ") + " (합계: " + playerTotal + ")\n" +
    "딜러 카드: " + dealer.join(", ") + " (합계: " + handTotal(dealer) + ")\n" +
    "결과: " + result
  );
}
