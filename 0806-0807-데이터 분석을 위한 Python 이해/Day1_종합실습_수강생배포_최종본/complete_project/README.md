# Day 1 비동기 데이터 파이프라인

## 실행

```bash
python -m pip install -r requirements.txt
python -m app.main
pytest -v
ruff check .
```

## 제출 전 자동 검사

```bash
python scripts/preflight_check.py
```

## 제출 ZIP 생성

```bash
python scripts/package_submission.py 서울_1반_홍길동_day1종합실습.zip
```

ZIP 생성 스크립트는 `.git`을 포함하고 `.venv`, 캐시, `.pyc`를 제외합니다.
