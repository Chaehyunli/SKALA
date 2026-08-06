# Day 1 종합실습 - 데이터 수집 미니 파이프라인

Open-Meteo(서울 3일 시간대별 기온·강수확률), Countries.dev(한국 국가 정보), ip-api(IP 기반 지역 정보) 3개 API를 `asyncio.gather()`로 동시에 수집하고, Pydantic v2(strict) 모델로 검증한 뒤 CSV/Parquet 두 형식으로 저장하며 성능을 비교합니다.

## 실행

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
python -m app.main
```

## 테스트 / 코드 품질

```bash
pytest -v
ruff check .
```

## 제출 전 자동 검사

```bash
python scripts/preflight_check.py
```

## 제출 ZIP 생성

```bash
python scripts/package_submission.py 판교_7반_임채현_day1종합실습.zip
```

ZIP 생성 스크립트는 `.venv`, 캐시, `.pyc`를 제외합니다.

## 프로젝트 구조

```text
app/            파이프라인 코드 (config/api_client/models/pipeline/storage/main)
tests/          pytest 테스트
scripts/        제출 전 검사 / ZIP 생성 스크립트
data/output/    실행 결과 (CSV, Parquet, JSON)
reports/        실행 결과 PDF 보관 위치
```

## 흐름별 함수 상세 설명

### 1) 환경 구성 (`requirements.txt`, `pyproject.toml`)

- `requirements.txt` — httpx/pydantic/pandas/pyarrow/pytest/pytest-asyncio/ruff 버전을 `==`로 고정. 팀/채점 환경 간 결과 재현성을 보장.
- `pyproject.toml` — `asyncio_mode = "auto"`로 `@pytest.mark.asyncio` 데코레이터 없이도 async 테스트 함수가 자동 인식되게 설정. ruff는 `E`(pycodestyle) `F`(pyflakes) `I`(isort) `B`(bugbear) `UP`(pyupgrade) 규칙 세트를 사용.

### 2) 비동기 데이터 수집 — `app/api_client.py`

- **`fetch_json(client, url)`** — API 1개를 호출하는 최소 단위 함수.
  - `client.get(url, timeout=REQUEST_TIMEOUT_SECONDS)` → 응답 획득
  - `response.raise_for_status()` → 4xx/5xx면 `HTTPStatusError` 발생
  - `httpx.HTTPStatusError` / `httpx.RequestError` / `ValueError`(JSON 파싱 실패) 세 갈래를 잡아 전부 자체 예외인 `ApiFetchError`로 통일 → 호출부가 httpx 내부 예외 타입을 몰라도 됨
- **`fetch_all_data(client)`** — `asyncio.gather(fetch_json(...), fetch_json(...), fetch_json(...))`로 weather/country/ip 3개를 **동시에** 요청.
  - `httpx.AsyncClient` 하나를 3개 요청이 공유(`app/main.py`의 `async with` 블록) → TCP/TLS 연결 재사용.

### 3) 스키마 검증 — `app/models.py` + `app/pipeline.py`

- **`WeatherHourRecord` / `CountryInfo` / `IPInfo`** — 전부 `ConfigDict(strict=True)`로 타입 자동 변환(문자열→숫자 등)을 차단. `precipitation_probability: int = Field(ge=0, le=100)`, `lat: float = Field(ge=-90, le=90)`처럼 필드마다 범위 제약을 걸어 "타입은 맞지만 값이 이상한" 데이터도 걸러냄.
- **`extract_weather_rows(raw)`** — Open-Meteo가 주는 열지향(`{"hourly": {"time": [...], ...}}`) 구조를 `zip(times, temperatures, precipitations, strict=True)`로 묶어 행 단위 dict 리스트로 변환. 이때 `datetime.fromisoformat(time_value)`로 문자열을 미리 datetime 객체로 바꿔둠 — strict 모델이 문자열을 거부하기 때문에 검증 *이전*에 타입을 맞추는 단계.
- **`validate_many(model_class, rows, source_name)`** — 제네릭(`TypeVar`)하게 만들어 weather(72건)/country(1건)/ip(1건) 검증에 동일 함수 재사용. `ValidationError` 발생 시 콘솔에 출력하고 `errors` 리스트에 `{source, index, errors}` 형태로 누적.
- **`main.py`의 흐름** — 3개 소스의 오류를 합쳐서 하나라도 있으면 `VALIDATION_ERROR_OUTPUT`에 JSON으로 저장하고 `RuntimeError`로 파이프라인을 중단 (잘못된 데이터가 저장 단계로 넘어가지 않게 하는 안전장치).

### 4) 저장 및 성능 비교 — `app/storage.py`

- **`save_with_performance(records, csv_path, parquet_path)`** — Pydantic 모델 리스트를 `[r.model_dump() for r in records]`로 dict화 → `pd.DataFrame`. `perf_counter()`로 CSV(`to_csv`)와 Parquet(`to_parquet`, engine=pyarrow, compression=snappy) 각각의 쓰기 시간을 측정하고 `Path.stat().st_size`로 파일 크기까지 기록.
- **`verify_saved_data(csv_path, parquet_path, key_column)`** — 저장 직후 CSV/Parquet를 다시 읽어(`pd.read_csv`/`pd.read_parquet`) 행 수 일치 확인, `key_column`이 주어지면 (country="name", ip="query") 해당 컬럼 값까지 `.tolist()`로 완전 비교.
- **`save_json`** — 성능 결과·검증 오류를 JSON으로 저장. `ensure_ascii=False`로 한글 깨짐 방지.

### 5) 실행 진입점 — `app/main.py`

- **`run_pipeline()`** — 1)수집 → 2)검증 → 3)저장+성능측정(weather/country/ip 각각 `save_with_performance` 호출) → 4)재로딩 검증을 순서대로 실행, 단계마다 콘솔 로그 출력.
- **`main()`** — `ApiFetchError`(API 문제) / `ImportError`(패키지 미설치) / `(OSError, RuntimeError, ValueError)`(그 외 실행 오류)로 예외를 분기해서 사용자 메시지를 다르게 출력하고 `SystemExit(1)`로 종료 — `scripts/preflight_check.py`가 이 종료 코드로 성공/실패를 판정함.

### 6) 테스트 — `tests/`

- `tests/test_api_client.py` — `httpx.MockTransport`로 실제 네트워크 없이 3개 호스트 호출 여부, HTTP 오류/비-dict 응답 시 `ApiFetchError` 발생 여부 검증.
- `tests/test_models.py` — strict 모델이 잘못된 타입(문자열 time)·범위 초과 값(강수확률 150, 위도 초과 등)을 정확히 거부하는지 검증.
- `tests/test_pipeline.py` — `extract_weather_rows`의 열→행 변환, `validate_many`의 valid/error 분리 검증.
- `tests/test_storage.py` — 저장 후 재로딩 시 행 수/키 컬럼 일치, 행 수 불일치 시 예외 발생, 한글 텍스트 보존 검증.
