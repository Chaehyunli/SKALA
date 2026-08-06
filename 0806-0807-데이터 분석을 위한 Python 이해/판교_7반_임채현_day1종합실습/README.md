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
