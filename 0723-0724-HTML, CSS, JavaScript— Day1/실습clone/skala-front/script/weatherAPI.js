// 외부에서 가져다 쓸 수 있도록 export를 함수 맨 앞에 붙입니다.
export async function getLiveWeather(lat, lon) {
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m`;
    
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error("서버 응답 불안정");
        
        const data = await response.json();
        
        // 필요한 데이터만 깔끔한 객체로 패킹해서 리턴
        return {
            temp: data.current.temperature_2m,
            humidity: data.current.relative_humidity_2m
        };
    } catch (error) {
        console.error("API 모듈 에러:", error);
        return null; // 에러 시 빈 값 던지기
    }
}