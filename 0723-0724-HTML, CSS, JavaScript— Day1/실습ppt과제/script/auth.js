// [과제] 회원가입 시 입력한 이름을 sessionStorage에 저장하고, 헤더 배너에 환영 메시지로 표시
document.addEventListener('DOMContentLoaded', () => {
  // signUpResult.html로 넘어온 userName 쿼리스트링을 세션에 저장
  const userName = new URLSearchParams(location.search).get('userName');
  if (userName) sessionStorage.setItem('userName', userName);

  const banner = document.getElementById('authBanner');
  if (!banner) return;

  const savedName = sessionStorage.getItem('userName');
  banner.innerHTML = savedName
    ? `<span class="welcome-text">${savedName}님! 환영합니다 🎉</span>`
    : '<a href="signUp.html"><button type="button">📝 회원가입</button></a>';
});
