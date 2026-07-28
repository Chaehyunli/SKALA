function calculateGrade() {
  var subjects = ["HTML", "CSS", "JavaScript"];
  var total = 0;

  for (var i = 0; i < subjects.length; i++) {
    var score = Number(prompt(subjects[i] + " 점수를 입력하세요."));
    total += score;
  }

  var average = total / subjects.length;
  var result = average >= 60 ? "합격입니다!" : "불합격입니다.";

  alert("총점: " + total + "점, 평균: " + average.toFixed(1) + ", 결과: " + result);
}
