# 실습 프로젝트 정답

이 폴더는 교재의 모든 단계를 완료한 결과입니다. 먼저 메인 교재를 따라 직접 구현하고, 오류가 해결되지 않을 때 코드와 실행 결과를 비교합니다.

## 실행 방법

프로젝트 폴더에서 다음 명령어를 실행합니다.

```bash
python -m json.tool Python_Practice1_Data.json
python -m py_compile practice1.py
python practice1.py
python -m py_compile practice1_check.py
python practice1_check.py
```

macOS 또는 Linux에서 `python` 명령을 사용할 수 없으면 `python3`를 사용합니다.

마지막에 다음 문장이 출력되면 정상입니다.

```text
전체 검사를 통과했습니다.
```
