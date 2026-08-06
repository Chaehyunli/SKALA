# Code 분석 결과에 대한 본인 의견

> 이 문서는 `app/` 파이프라인(Open-Meteo/Countries.dev/ip-api 비동기 수집 → Pydantic v2 strict 검증 → CSV/Parquet 저장·비교)을 실제로 구현하면서 겪은 문제와 그 해결 과정, 그리고 코드를 다시 읽으며 든 개선 아이디어를 정리한 것이다.

## 요약

| 구분 | 핵심 내용 |
|---|---|
| 가장 어려웠던 부분 | Pydantic strict 모드에서 문자열 → datetime 자동 변환이 안 됨 |
| 비동기의 체감 이득 | 3개 API 병렬 호출로 총 대기 시간이 "가장 느린 1개" 수준으로 단축 |
| CSV vs Parquet | 이번 데이터 규모(72건 이하)에서는 **CSV가 더 빠르고 더 작았음** (통념과 반대) |
| 가장 시급한 개선점 | `main.py`의 weather/country/ip 저장·검증 블록 3중 반복을 반복문으로 축약 |

---

## 1. 구현하며 어려웠던 점

### 1-1. strict 모드가 ISO8601 문자열을 datetime으로 바꿔주지 않음

`WeatherHourRecord`에 `model_config = ConfigDict(strict=True)`를 걸고, Open-Meteo가 그대로 주는 문자열(`"2026-08-06T00:00"`)을 `time: datetime` 필드에 넣었더니 다음과 같은 에러가 났다.

```text
pydantic_core._pydantic_core.ValidationError: 1 validation error for WeatherHourRecord
time
  Input should be a valid datetime [type=datetime_type, input_value='2026-08-06T00:00', input_type=str]
```

lenient(기본) 모드였다면 pydantic이 알아서 문자열을 파싱해줬겠지만, strict 모드는 "이미 올바른 타입인 값"만 받아들인다. 처음엔 모델 쪽 문제인 줄 알고 `Field`에 옵션을 이것저것 추가해봤지만 근본 원인은 **"입력 정규화"와 "검증"을 같은 단계에서 하려 했다는 것**이었다. 결국 `pipeline.py`의 `extract_weather_rows()`에서 모델에 넘기기 *전에* `datetime.fromisoformat(time_value)`로 미리 변환하도록 고쳐서 해결했다.

```python
# extract_weather_rows() 안에서
rows.append({
    "time": datetime.fromisoformat(time_value),  # 검증 전에 타입을 먼저 맞춘다
    "temperature_2m": temperature,
    "precipitation_probability": precipitation,
})
```

이 경험으로 "strict 검증 = 입력이 이미 정제되어 있다는 전제 하에 마지막 문지기 역할만 한다"는 걸 체감했다. strict 모드를 쓸 거면 그 앞단에 반드시 명시적인 파싱/정규화 계층이 있어야 한다.

### 1-2. 형태가 다른 3개 소스를 하나의 검증 함수로 억지로 통일

weather는 원본이 리스트(72건)지만, country/ip는 원본이 단일 dict 1건이다. `validate_many(model_class, rows, source_name)`를 세 소스 모두에 재사용하기 위해, 호출부에서 country/ip를 `[raw_data["country"]]`처럼 1건짜리 리스트로 감싸야 했다.

```python
# main.py
country_records, country_errors = validate_many(CountryInfo, [raw_data["country"]], "country")
```

"함수 하나로 재사용 가능"이라는 장점은 얻었지만, 호출부만 보면 country가 원래 리스트 데이터인지 아닌지 알 수 없다는 단점도 같이 생겼다. 재사용성과 원본 데이터 형태를 코드로 드러내는 것 사이의 트레이드오프를 실감했다 (자세한 개선안은 5번 참고).

### 1-3. CSV/Parquet 재로딩 검증에서 datetime 컬럼 비교의 함정

`verify_saved_data()`로 저장 직후 재로딩한 CSV와 Parquet의 특정 컬럼 값이 완전히 같은지 비교하는 로직을 만들었는데, weather의 `time` 컬럼에 이걸 그대로 적용하면 실패했다. CSV로 왕복하면 `time`이 문자열(`"2026-08-06 00:00:00"`)로 돌아오고, Parquet로 왕복하면 `pandas.Timestamp` 객체로 돌아와서 `.tolist()` 비교 시 타입 자체가 달라 항상 불일치로 나온다. 결국 weather는 `key_column`을 넘기지 않고 **행 수만** 비교하도록 타협했다(country/ip는 문자열 컬럼이라 `key_column="name"` / `"query"`로 값까지 비교).

### 1-4. (부록) 코드 밖에서 겪은 환경 문제 — venv 활성화가 실제로는 안 되고 있었던 문제

이건 코드 로직 문제는 아니지만 실습 전체에서 가장 시간을 많이 쓴 부분이라 기록해둔다. `~/.zshrc`에 `python`/`pip`가 홈브류 전역 인터프리터로 고정 `alias`되어 있어서, `source .venv/bin/activate`를 해도 `python`/`pip` 명령이 venv를 무시하고 전역 환경을 사용하고 있었다. 그 상태로 `pip install`을 하면 패키지가 프로젝트 venv가 아니라 전역에 설치돼버려서, "설치했는데 이 프로젝트에서는 없다고 나오는" 혼란스러운 상황이 반복됐다. 원인을 찾은 뒤 alias를 제거하고 나서야 `python`이 정확히 `.venv/bin/python`을 가리키게 됐다. 또 한 번은 프로젝트 폴더를 이동시켰더니 `.venv/bin/activate`가 **폴더 이동 전의 절대경로**를 하드코딩하고 있어서 `python: command not found`가 나기도 했다(venv는 생성 시점의 절대경로를 스크립트에 그대로 박아두기 때문에, 폴더를 옮기면 venv를 새로 만드는 게 가장 확실하다).

---

## 2. 비동기 수집의 장점

`fetch_all_data()`는 `httpx.AsyncClient` 하나를 3개 요청이 공유하면서 `asyncio.gather()`로 Open-Meteo/Countries.dev/ip-api를 동시에 호출한다.

```python
weather, country, ip = await asyncio.gather(
    fetch_json(client, WEATHER_URL),
    fetch_json(client, COUNTRY_URL),
    fetch_json(client, IP_URL),
)
```

- **대기 시간**: 순차 호출이었다면 세 응답 시간의 합만큼 기다려야 하지만, `gather()`는 세 요청을 동시에 던져놓고 모두 끝나길 기다리므로 이론상 총 대기 시간이 "가장 느린 요청 1개"에 수렴한다. 실습에 쓴 3개 API가 모두 1초 내외로 빨라 체감 차이는 크지 않았지만, 외부 API 중 하나라도 느려지면(수 초 지연) 순차 방식과의 격차가 그대로 드러나는 구조라는 걸 코드로 확인할 수 있었다.
- **커넥션 재사용**: `httpx.AsyncClient`를 세 요청이 공유해서, 매 요청마다 새 TCP/TLS 연결을 맺지 않고 하나의 클라이언트 컨텍스트 안에서 처리했다. `requests`로 세 번 따로 호출하는 것보다 연결 오버헤드가 줄어드는 구조다.
- **예외 처리와의 결합**: `fetch_json()` 내부에서 `httpx.HTTPStatusError`/`httpx.RequestError`를 구분해 `ApiFetchError`로 통일하기 때문에, `gather()`로 동시에 실행되는 3개 중 하나가 실패해도 원인(HTTP 상태 오류인지 네트워크 오류인지)을 잃지 않고 상위(`main()`)까지 전달된다.

---

## 3. CSV와 Parquet 비교 결과

실제 실행 결과(`data/output/performance_result.json`)는 다음과 같다.

| 데이터 | 건수 | CSV 쓰기(s) | Parquet 쓰기(s) | CSV 크기(B) | Parquet 크기(B) |
|---|---:|---:|---:|---:|---:|
| weather | 72 | 0.005895 | 0.041950 | 2,011 | 3,527 |
| country | 1 | 0.000633 | 0.002133 | 135 | 4,528 |
| ip | 1 | 0.000265 | 0.000496 | 124 | 4,778 |

**세 소스 모두 CSV가 Parquet보다 빠르고 작았다.** 흔히 "Parquet가 CSV보다 빠르고 작다"고 알려져 있는데, 이번 실습 데이터 규모(최대 72건)에서는 오히려 반대로 나왔다. 이유를 코드/구조 관점에서 정리하면:

- **쓰기 시간**: Parquet는 pyarrow 엔진이 스키마를 분석하고 열 지향 구조로 인코딩하는 고정 오버헤드가 있다. 이 오버헤드는 데이터 건수와 거의 무관하게 발생하는 비용이라, 72건처럼 적은 데이터에서는 오버헤드가 실제 인코딩 시간보다 커진다. CSV는 그냥 값을 텍스트로 순서대로 이어 쓰는 것이라 이런 고정비가 거의 없다.
- **파일 크기**: Parquet 파일에는 스키마 정의, 컬럼 메타데이터, 압축 통계 등 부가 정보가 함께 저장된다. country/ip처럼 1건뿐인 데이터에서는 이 메타데이터가 실제 데이터보다 훨씬 커서(135B → 4,528B) 파일 크기가 30배 넘게 차이 났다.
- **언제 역전되는가**: Parquet의 장점(빠른 쓰기/작은 크기)은 행 수가 수만~수백만 단위로 커지고, 반복되는 값이 많아 열 지향 압축이 효과를 발휘할 때 나타난다. 이번 실습은 "적은 데이터에서는 오히려 CSV가 유리하다"는 반례를 직접 눈으로 확인한 셈이다.

---

## 4. 개선할 사항

1. **weather의 `time` 컬럼도 재로딩 검증에 포함하기**: `verify_saved_data()` 호출 전에 CSV 쪽 문자열을 `pd.to_datetime()`으로 다시 파싱하면, weather도 country/ip처럼 키 컬럼 값까지 완전히 비교할 수 있다. 지금은 건수만 확인해 "정확히 같은 시간대가 저장됐는지"까지는 보장하지 못한다.
2. **1건짜리 데이터(country/ip)에 CSV/Parquet 비교를 적용하는 것 자체를 재검토**: 3번에서 보듯 데이터가 1건일 때는 두 포맷의 차이가 사실상 "메타데이터 오버헤드 비교"에 가깝다. country/ip는 단순 JSON 저장으로 단순화하고, CSV/Parquet 비교는 weather처럼 시계열성으로 계속 쌓이는 데이터에만 적용하는 게 실습 취지(포맷 트레이드오프 체감)에 더 맞을 것 같다.
3. **가짜 데이터가 아닌 실제 실패 케이스로 오류 경로를 검증**: 지금은 `tests/`의 `ValidationError` 테스트가 전부 손으로 만든 잘못된 값(음수 강수확률, 잘못된 위도 등)이다. 실제 운영이라면 "API 스키마가 예고 없이 바뀌는 경우"에 대비해, 존재하지 않는 필드가 왔을 때의 동작도 테스트로 남겨두면 더 견고할 것 같다.

---

## 5. 코드 품질을 높이기 위한 제안

### 5-1. `validate_many()`를 단일 레코드에도 자연스럽게 쓸 수 있게

지금은 country/ip처럼 원본이 dict 1개인 경우도 호출부에서 리스트로 감싸야 한다:

```python
# 지금: 호출부가 "이건 원래 1건짜리다"라는 사실을 알아서 감싸줘야 함
country_records, country_errors = validate_many(CountryInfo, [raw_data["country"]], "country")
```

`validate_many()`를 감싸는 얇은 `validate_one()`을 추가하면 호출부의 의도가 코드에 그대로 드러난다:

```python
def validate_one(model_class, row, source_name):
    valid, errors = validate_many(model_class, [row], source_name)
    return (valid[0] if valid else None), errors

country_record, country_errors = validate_one(CountryInfo, raw_data["country"], "country")
```

### 5-2. `main.py`의 3중 반복 축약

`run_pipeline()`에서 weather/country/ip 각각에 대해 `save_with_performance()` → `verify_saved_data()` → 출력을 구조적으로 동일하게 3번 반복하고 있다. 소스별 설정(레코드, 경로, 키 컬럼)을 리스트로 만들고 `for` 루프 하나로 처리하면, 나중에 API가 하나 더 늘어나도(예: 환율 API 추가) `main.py`의 반복 블록을 늘리는 대신 리스트에 항목 하나만 추가하면 된다:

```python
sources = [
    ("weather", weather_records, WEATHER_CSV, WEATHER_PARQUET, None),
    ("country", country_records, COUNTRY_CSV, COUNTRY_PARQUET, "name"),
    ("ip", ip_records, IP_CSV, IP_PARQUET, "query"),
]

performances = []
for name, records, csv_path, parquet_path, key_column in sources:
    perf = save_with_performance(records, csv_path, parquet_path)
    perf["name"] = name
    performances.append(perf)
    csv_rows, parquet_rows = verify_saved_data(csv_path, parquet_path, key_column)
    print(f"[{name}] CSV={csv_rows}건 Parquet={parquet_rows}건")
```

지금 코드가 틀린 건 아니지만, "소스가 3개"라는 가정이 코드 곳곳(변수명 3벌)에 그대로 박혀 있어서 확장성 면에서는 이 구조가 더 낫다고 생각한다.
