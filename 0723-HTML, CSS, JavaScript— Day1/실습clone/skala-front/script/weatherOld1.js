// DOM 요소 가져오기
const citySelect = document.querySelector('#city-select');
const weatherBox = document.querySelector('#weather-box');

// 선택 변경 이벤트 리스너 연결
citySelect.addEventListener('change', function(event) {
    const selectedValue = event.target.value; // 예: "37.56,126.97"
    
    // 사용자가 '도시를 선택하세요'를 고른 경우 처리
    if (selectedValue === "none") {
        weatherBox.innerHTML = "<p>도시를 선택하면 좌표가 표시됩니다.</p>";
        return;
    }

    // 콤마를 기준으로 위도와 경도 글자 쪼개기
    const coords = selectedValue.split(',');
    const lat = coords[0];
    const lon = coords[1];

    // selectedOptions를 활용해 선택된 <option> 안의 글자(도시 이름) 가져오기
    const cityName = citySelect.options[citySelect.selectedIndex].text;

    // DOM 조작으로 화면 바꾸기
    weatherBox.innerHTML = `
        <div style="border: 1px dashed #3498db; padding: 10px; margin-top: 10px;">
            <h4>📍 ${cityName} 정보</h4>
            <p>• 위도(Latitude): ${lat}</p>
            <p>• 경도(Longitude): ${lon}</p>
        </div>
    `;
});