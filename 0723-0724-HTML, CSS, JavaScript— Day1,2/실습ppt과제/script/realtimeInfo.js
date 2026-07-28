// [과제3] 모듈분리: 화면 담당(realtimeInfo.js) — weatherAPI.js에서 함수만 import해서 사용
import { fetchWeather } from './weatherAPI.js';

const citySelect = document.getElementById('citySelect');
const weatherBox = document.getElementById('weather-box');

// [과제1] DOM/이벤트: select가 바뀔 때마다(change) 선택된 도시 이름/좌표를 innerHTML로 표시
async function showWeather() {
  const cityName = citySelect.options[citySelect.selectedIndex].text;
  const [lat, lon] = citySelect.value.split(',');

  weatherBox.innerHTML = `<p>${cityName} 실시간 날씨 로딩 중... ⏳</p>`;

  // [과제2] 비동기 호출: 응답을 기다리는 동안 로딩 메시지 → 완료되면 실제 기온/습도 표시
  const { temperature_2m, relative_humidity_2m } = await fetchWeather(lat, lon);

  weatherBox.innerHTML = `
    <p>${cityName} 실시간 날씨</p>
    <p>🌡️ 현재 기온: ${temperature_2m}°C</p>
    <p>💧 현재 습도: ${relative_humidity_2m}%</p>
  `;
}

citySelect.addEventListener('change', showWeather);
document.addEventListener('DOMContentLoaded', showWeather);
