function showMyBag() {
  var myBag = [
    { name: "여권", count: 1 },
    { name: "스마트폰", count: 2 },
    { name: "지갑", count: 1 }
  ];

  var result = "[내 가방 속 물품 목록]\n--------------------\n";

  for (var i = 0; i < myBag.length; i++) {
    result += "- " + myBag[i].name + " : " + myBag[i].count + "개\n";
  }

  result += "--------------------\n총 물품 종류: " + myBag.length + "가지";

  alert(result);
}
