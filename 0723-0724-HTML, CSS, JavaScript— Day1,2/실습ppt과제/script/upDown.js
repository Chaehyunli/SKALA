function startUpDownGame() {
  var computerNum = Math.floor(Math.random() * 50) + 1;
  var tryCount = 0;
  var guess;

  while (true) {
    guess = Number(prompt("1부터 50 사이의 숫자 중 컴퓨터가 생각한 숫자는 무엇일까요?"));
    tryCount++;

    if (guess > computerNum) {
      alert("Down!");
    } else if (guess < computerNum) {
      alert("Up!");
    } else {
      alert("축하합니다! " + tryCount + "번 만에 맞추셨습니다.");
      break;
    }
  }
}
