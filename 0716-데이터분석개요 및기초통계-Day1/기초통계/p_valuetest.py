import numpy as np
from scipy.stats import norm

# 1. 시나리오 데이터
mu_0 = 100 # 귀무가설의 모평균 (기준 무게)
sigma = 5 # 모집단 표준편차 (알고 있다고 가정)
n = 50 # 표본 크기
x_bar = 101.5  # 표본 평균
alpha = 0.05 # 유의수준

# 2. 검정 통계량 Z 계산 (Z-score calculation)
std_error = sigma / np.sqrt(n)
z_score = (x_bar - mu_0) / std_error

# --- P-값 계산 부분 ---
# 3. P-값 계산
# 양측 검정 P-값: P = 2 * P(Z > |Z_score|)
# norm.cdf(Z_score)는 Z_score보다 작거나 같은 확률을 반환합니다.
# 1 - norm.cdf(|Z_score|)는 |Z_score|보다 크거나 같은 (꼬리) 확률을 반환합니다.
p_value = 2 * (1 - norm.cdf(abs(z_score)))
# --------------------

print(f"표준 오차 (Standard Error): {std_error:.3f} g")
print(f"계산된 검정 통계량 Z-score: {z_score:.3f}")
print(f"계산된 **P-값 (P-value)**: **{p_value:.4f}**")

# 양측 검정 임계 Z값 (Z-critical for Two-Tailed Test)
z_critical_two = norm.ppf(1 - alpha/2)

print("\n##  2. P-값 기반 최종 결론")
print(f"P-값: {p_value:.4f}")
print(f"유의수준 (alpha): {alpha:.4f}")

if p_value < alpha:
    print(f" {p_value:.4f} < {alpha:.4f} 이므로, 귀무가설 **기각**")
    print("해석: 과자 무게는 통계적으로 유의미하게 100g과 **다르다**고 결론 내릴 수 있습니다.")
else:
    print(" 귀무가설 기각 **실패**")
    print("해석: 과자 무게가 100g과 다르다고 볼 통계적 증거가 충분하지 않습니다.")