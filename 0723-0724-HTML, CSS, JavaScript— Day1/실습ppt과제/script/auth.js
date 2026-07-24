// [과제] 회원가입 시 입력한 이름을 sessionStorage에 저장하고, 헤더 배너에 환영 메시지로 표시
document.addEventListener('DOMContentLoaded', () => {
  // signUpResult.html로 넘어온 userName 쿼리스트링을 세션에 저장
  const userName = new URLSearchParams(location.search).get('userName');
  if (userName) sessionStorage.setItem('userName', userName);

  const savedName = sessionStorage.getItem('userName');

  const banner = document.getElementById('authBanner');
  if (banner) {
    banner.innerHTML = savedName
      ? `<span class="welcome-text">${savedName}님! 환영합니다 🎉</span>`
      : '<a href="signUp.html"><button type="button">📝 회원가입</button></a>';
  }

  // 로그인 상태(세션에 이름이 있음)면 각 페이지 nav의 "회원가입" 링크는 숨김
  if (savedName) {
    document.querySelectorAll('.signup-nav-link').forEach(el => el.style.display = 'none');
  }
});
