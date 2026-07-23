"""build_report.py의 section_3()에서 계산한 최종 권장 설정으로 실제 학습 1회를 수행."""
import run_experiments as R

FINAL_CONFIG = {
    "N_LAYER": 12, "N_HEAD": 3, "N_EMBD": 192,
    "MAX_ITERS": 450, "SAMPLE_EVERY": 50, "SAMPLE_LENGTH": 200,
    "BLOCK_SIZE": 64,
    "TEMPERATURE": 1.0, "TOP_K": 10,
    "DATA_FRACTION": 1.0,
    "USE_CAUSAL_MASK": True,
}

if __name__ == "__main__":
    R.run_and_save_final(FINAL_CONFIG)
