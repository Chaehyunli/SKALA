# Day 1 종합실습 실행 결과 보고서

## 1. 프로젝트 개요

- 프로젝트명: 데이터 수집 미니 파이프라인
- 작성자: 임채현
- 캠퍼스 / 반: 판교 / 7반
- 구현 목적: Open-Meteo/Countries.dev/ip-api 3개 API를 비동기로 동시 수집하고, Pydantic v2로 검증한 뒤 CSV·Parquet로 저장·비교

## 2. 실행 환경

| 항목 | 내용 |
|---|---|
| 운영체제 | |
| Python | |
| 주요 패키지 | httpx 0.28.1, pydantic 2.13.4, pandas 3.0.5, pyarrow 25.0.0 |

## 3. 프로그램 실행 결과

다음 화면을 캡처하여 삽입합니다.

- API 3개 수집 결과
- Pydantic 검증 결과
- CSV와 Parquet 저장 결과
- 저장 결과 재로딩 검증
- 성능 측정 결과

## 4. 테스트와 코드 품질

다음 화면을 캡처하여 삽입합니다.

- `pytest -v`
- `ruff check .`
- `git log --oneline`

## 5. CSV와 Parquet 비교

| 데이터 | 형식 | 저장 시간 | 파일 크기 | 의견 |
|---|---|---:|---:|---|
| weather | CSV | | | |
| weather | Parquet | | | |
| country | CSV | | | |
| country | Parquet | | | |
| ip | CSV | | | |
| ip | Parquet | | | |

## 6. 본인 의견

- 구현하며 어려웠던 점:
- 비동기 수집의 장점:
- CSV와 Parquet 비교 결과:
- 개선할 사항:
- 코드 품질을 높이기 위한 제안:
