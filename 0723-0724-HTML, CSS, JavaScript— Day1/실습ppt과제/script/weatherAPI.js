// [과제3] 모듈분리: 데이터 담당(weatherAPI.js) — API 호출 로직만 export

// [과제2] 비동기 호출: fetch + async/await로 Open-Meteo에서 실시간 날씨를 가져옴
export async function fetchWeather(lat, lon) {
  const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m`;
  const res = await fetch(url);
  const data = await res.json();
  return data.current;
}
