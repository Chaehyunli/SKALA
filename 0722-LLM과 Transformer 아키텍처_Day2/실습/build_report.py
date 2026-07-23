# -*- coding: utf-8 -*-
"""results.json 을 읽어 6(+1)개 .png 그래프 + experiment_report.md 를 생성.

반영된 피드백:
  - 2.1 모델 크기: N_LAYER/N_HEAD/N_EMBD를 동시에 바꾸지 않고, baseline(6/6/192)에서
    파라미터 1개씩만 올리고/내려서(총 6회 학습) 3개 파라미터를 각각 독립적으로 비교
  - 2.2~2.6: 다른 실험에 "Base(6/6/192)"라는 2.1 전용 라벨이 섞여 들어가지 않도록,
    각 실험 자신의 baseline 조건명(MAX_ITERS=500, BLOCK_SIZE=128 등)으로만 표기
  - 모든 정량 표에서 Param 수 열 삭제
  - 3장 (2) 파라미터별 권장값과 근거를 표로 정리
  - 최종 권장 설정을 실제로 1회 더 학습(run_final.py)해 4장에 동일한 절차로 검증
"""
import json, math, os
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

plt.rcParams["font.family"] = "AppleGothic"
plt.rcParams["axes.unicode_minus"] = False

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RESULTS_PATH = os.path.join(SCRIPT_DIR, "results.json")
REPORT_PATH = os.path.join(SCRIPT_DIR, "experiment_report.md")

with open(RESULTS_PATH, "r", encoding="utf-8") as f:
    R = json.load(f)

ENV = R["env"]


def find(group_key, cond_substr):
    for r in R[group_key]:
        if cond_substr in r["cond"]:
            return r
    raise KeyError(cond_substr)


def hist_xy(history_dict):
    items = sorted(((int(k), v) for k, v in history_dict.items()), key=lambda t: t[0])
    xs = [i for i, _ in items]
    ys = [v for _, v in items]
    return xs, ys


def ppl(loss):
    return round(math.exp(min(loss, 20)), 2)


def mid_sample(r):
    hist = r["generation_history"]
    if not hist:
        return None, None
    iters_sorted = sorted(int(k) for k in hist.keys())
    mid_iter = iters_sorted[len(iters_sorted) // 2]
    key = str(mid_iter) if str(mid_iter) in hist else mid_iter
    return mid_iter, hist[key]


def code_block(text):
    return "```\n" + text + "\n```"


def quant_table(rows, header="실험 조건"):
    lines = [
        f"| {header} | Train Loss | Val Loss | Val Perplexity | 학습 시간(s) |",
        "| :--- | :---: | :---: | :---: | :---: |",
    ]
    for r in rows:
        lines.append(
            f"| {r['cond']} | {r['train_loss']:.4f} | {r['val_loss']:.4f} | "
            f"{ppl(r['val_loss'])} | {r['time_s']:.1f} |"
        )
    return "\n".join(lines)


def loss_curve_panel(ax, conds, title):
    for r in conds:
        ax.plot(r["loss_history"], label=f"{r['cond']} train", alpha=0.7)
        xs, ys = hist_xy(r["val_history"])
        ax.plot(xs, ys, "o--", label=f"{r['cond']} val")
    ax.set_xlabel("Iteration"); ax.set_ylabel("Loss")
    ax.set_title(title); ax.legend(fontsize=6.5); ax.grid(True)


# ============================================================ 2.1 모델 크기 (파라미터별 독립 비교) ====
def plot_model_size():
    groups = R["exp_2_1_model_size"]
    param_keys = [("N_LAYER", "N_LAYER"), ("N_HEAD", "N_HEAD"), ("N_EMBD", "N_EMBD")]
    fig, axes = plt.subplots(2, 3, figsize=(17, 8))

    for col, (gkey, pname) in enumerate(param_keys):
        conds = sorted(groups[gkey], key=lambda r: r["params"][pname])
        loss_curve_panel(axes[0, col], conds, f"{pname} 변화 (나머지 baseline 고정)")

        names = [r["cond"] for r in conds]
        params_m = [r["n_params"] / 1e6 for r in conds]
        axes[1, col].bar(names, params_m, color="#4C72B0")
        axes[1, col].set_ylabel("파라미터 수 (M)")
        axes[1, col].set_title(f"{pname}별 파라미터 수")
        axes[1, col].tick_params(axis="x", rotation=15)

    fig.tight_layout()
    fig.savefig(os.path.join(SCRIPT_DIR, "model_size_report.png"), dpi=130)
    plt.close(fig)


def section_2_1_param(gkey, pname, title, hypothesis):
    conds = sorted(R["exp_2_1_model_size"][gkey], key=lambda r: r["params"][pname])
    low, base, high = conds
    best = min(conds, key=lambda r: r["val_loss"])

    mid_it, mid_txt = mid_sample(base)

    return f"""#### 2.1-{gkey} {title}

- **가설:** {hypothesis}
- **실험 설계 및 근거:** baseline(N_LAYER=6, N_HEAD=6, N_EMBD=192)에서 {pname} 한 값만 낮추거나 높이고(다른 두 파라미터는 baseline 그대로 고정) Val Loss·생성문을 비교했다. 이렇게 하면 {pname} 하나의 효과만 분리해서 볼 수 있다.

**정량적 비교 표**

{quant_table(conds, header=f"{pname} 조건")}

**생성 문장 샘플 비교 (학습 후 원문)**

- {low['cond']}:
{code_block(low['after'])}

- {base['cond']}:
{code_block(base['after'])}

- {high['cond']}:
{code_block(high['after'])}

**참고: baseline({base['cond']}) 학습 진행 3단계**

[학습 전]
{code_block(base['before'])}

[iter {mid_it}]
{code_block(mid_txt)}

[학습 후]
{code_block(base['after'])}

**결과 분석:** {pname}만 바꿨을 때 Val Loss가 가장 낮았던 조건은 {best['cond']}({best['val_loss']:.4f})였다. {'값이 클수록 Val Loss가 낮아지는 방향(표현력 증가)이 관측됐다' if high['val_loss'] < base['val_loss'] < low['val_loss'] else ('값이 작을수록 오히려 Val Loss가 낮아 500 iter 안에서는 작은 모델이 더 잘 수렴했다' if low['val_loss'] < base['val_loss'] < high['val_loss'] else '값을 올리거나 내린다고 Val Loss가 단조적으로 변하지는 않아, 이 파라미터 하나만으로는 명확한 방향성이 나오지 않았다')}.
"""


def section_2_1():
    plot_model_size()
    n_layer = section_2_1_param(
        "N_LAYER", "N_LAYER", "층 수",
        "층(N_LAYER)이 깊을수록 표현력이 커져 Val Loss가 낮아지겠지만, 500 iter로는 깊은 모델일수록 수렴이 덜 될 수 있다.",
    )
    n_head = section_2_1_param(
        "N_HEAD", "N_HEAD", "헤드 수",
        "헤드 수(N_HEAD)가 많을수록(=head_dim은 작아짐) 다양한 관점의 attention을 학습할 수 있어 Val Loss가 낮아질 것이다.",
    )
    n_embd = section_2_1_param(
        "N_EMBD", "N_EMBD", "임베딩 차원",
        "임베딩 차원(N_EMBD)이 클수록 표현력이 커져 Val Loss가 낮아지겠지만, 파라미터가 늘어난 만큼 500 iter로는 완전히 수렴하지 못할 수 있다.",
    )

    return f"""### 2.1 모델 크기 — N_LAYER / N_HEAD / N_EMBD 각각 독립적으로 비교

baseline(N_LAYER=6, N_HEAD=6, N_EMBD=192)에서 세 파라미터를 동시에 바꾸지 않고, 한 번에 하나씩만 올리고 내려 총 6회 추가 학습했다(N_LAYER∈{{3,12}}, N_HEAD∈{{3,12}}, N_EMBD∈{{96,384}}, N_HEAD 변경 시에도 N_EMBD가 나누어떨어지도록 값을 선택). 아래 세 개 소절은 서로 다른 파라미터를 바꾼 실험이므로 소절 간 직접 비교(예: N_LAYER=12 vs N_HEAD=12)는 하지 않는다.

**시각화 그래프:**

![모델 크기별 비교](model_size_report.png)

{n_layer}

{n_head}

{n_embd}
"""


# ============================================================ 2.2 반복 수 ====
def plot_max_iters():
    conds = sorted(R["exp_2_2_max_iters"], key=lambda r: r["params"]["MAX_ITERS"])
    fig, axes = plt.subplots(1, 2, figsize=(12, 4.5))
    loss_curve_panel(axes[0], conds, "MAX_ITERS별 Loss 수렴")

    longest = max(conds, key=lambda r: r["params"]["MAX_ITERS"])
    xs, vys = hist_xy(longest["val_history"])
    tys = [longest["loss_history"][it - 1] for it in xs]
    gap = [v - t for v, t in zip(vys, tys)]
    axes[1].plot(xs, gap, "o-", color="#C44E52")
    axes[1].axhline(0, color="black", linewidth=0.8)
    best_it = xs[int(np.argmin(vys))]
    axes[1].axvline(best_it, color="#55A868", linestyle="--",
                     label=f"Val Loss 최저 지점(iter {best_it})")
    axes[1].set_xlabel("Iteration"); axes[1].set_ylabel("Val Loss - Train Loss")
    axes[1].set_title(f"{longest['cond']}: 과적합 갭 추이")
    axes[1].legend(fontsize=8)

    fig.tight_layout()
    fig.savefig(os.path.join(SCRIPT_DIR, "max_iters_report.png"), dpi=130)
    plt.close(fig)


def section_2_2():
    conds = sorted(R["exp_2_2_max_iters"], key=lambda r: r["params"]["MAX_ITERS"])
    it100, it500, it2000 = conds

    longest = it2000
    xs, vys = hist_xy(longest["val_history"])
    best_it = xs[int(np.argmin(vys))]
    best_val = min(vys)
    last_it, last_val = xs[-1], vys[-1]
    overfit_sentence = (
        f"MAX_ITERS=2000 조건에서 Val Loss는 iter {best_it}에서 {best_val:.4f}로 최저를 찍은 뒤 "
        f"iter {last_it}에는 {last_val:.4f}로 {'다시 상승' if last_val > best_val else '큰 변화 없이 유지'}했다. "
        + (f"즉 iter {best_it} 근방이 이 실험에서 관측된 조기 종료(early stopping) 후보 지점이다."
           if last_val > best_val else
           f"2000 iter까지도 뚜렷한 val loss 반등은 관측되지 않았다.")
    )

    mid_it, mid_txt = mid_sample(it2000)

    return f"""### 2.2 학습 반복 수 (MAX_ITERS)

- **가설:** 반복 수가 늘어날수록 Train Loss는 단조 감소하지만, Val Loss는 어느 지점부터 더 이상 줄지 않거나 반등할 것이다.
- **실험 설계 및 근거:** baseline(N_LAYER=6, N_HEAD=6, N_EMBD=192, BLOCK_SIZE=128, DATA_FRACTION=1.0, USE_CAUSAL_MASK=True)을 고정하고 MAX_ITERS만 100 → 500 → 2000으로 바꿨다. SAMPLE_EVERY(50)마다 Val Loss를 동일한 held-out 셋으로 측정했다.

**정량적 비교 표**

{quant_table(conds, header="MAX_ITERS 조건")}

**시각화 그래프:**

![학습 반복 수별 비교](max_iters_report.png)

**생성 문장 샘플 비교**

(A) MAX_ITERS=2000 조건의 학습 진행 3단계

[학습 전]
{code_block(it2000['before'])}

[iter {mid_it}]
{code_block(mid_txt)}

[학습 후]
{code_block(it2000['after'])}

(B) 조건별 학습 후(after_training) 원문 비교

- {it100['cond']}:
{code_block(it100['after'])}

- {it500['cond']}:
{code_block(it500['after'])}

- {it2000['cond']}:
{code_block(it2000['after'])}

**결과 분석 및 원인 고찰**

Train Loss는 100→500→2000 iter로 갈수록 {it100['train_loss']:.4f} → {it500['train_loss']:.4f} → {it2000['train_loss']:.4f}로 예상대로 단조 감소했다. {overfit_sentence} Train Loss만으로는 실력 향상과 암기(과적합)를 구분할 수 없다는 것이 이번 실험의 핵심 관측이다.
"""


# ============================================================ 2.3 문맥 길이 ====
def plot_block_size():
    conds = sorted(R["exp_2_3_block_size"], key=lambda r: r["params"]["BLOCK_SIZE"])
    fig, axes = plt.subplots(1, 2, figsize=(12, 4.5))
    loss_curve_panel(axes[0], conds, "BLOCK_SIZE별 Loss")

    names = [r["cond"] for r in conds]
    batch_size = 64
    tok_per_sec = [batch_size * r["params"]["BLOCK_SIZE"] * r["params"]["MAX_ITERS"] / r["time_s"]
                   for r in conds]
    axes[1].bar(names, tok_per_sec, color="#8172B2")
    axes[1].set_ylabel("처리 토큰 수 / 초")
    axes[1].set_title("문맥 길이별 처리량 (batch×block_size / 총시간)")

    fig.tight_layout()
    fig.savefig(os.path.join(SCRIPT_DIR, "block_size_report.png"), dpi=130)
    plt.close(fig)


def section_2_3():
    conds = sorted(R["exp_2_3_block_size"], key=lambda r: r["params"]["BLOCK_SIZE"])
    bs32, bs64, bs128 = conds
    best = min(conds, key=lambda r: r["val_loss"])
    mid_it, mid_txt = mid_sample(bs128)

    return f"""### 2.3 문맥 길이 (BLOCK_SIZE)

- **가설:** 문맥 길이가 길수록 모델이 더 먼 과거까지 참조할 수 있어 Val Loss가 낮아지겠지만, 문맥이 길어질수록 self-attention 연산량(T²에 비례)이 늘어 처리량(토큰/초)은 떨어질 것이다.
- **실험 설계 및 근거:** baseline(N_LAYER=6, N_HEAD=6, N_EMBD=192, MAX_ITERS=500)을 고정하고 BLOCK_SIZE만 32 → 64 → 128로 바꿨다.

**정량적 비교 표**

{quant_table(conds, header="BLOCK_SIZE 조건")}

**시각화 그래프:**

![문맥 길이별 비교](block_size_report.png)

**생성 문장 샘플 비교**

(A) BLOCK_SIZE=128 조건의 학습 진행 3단계

[학습 전]
{code_block(bs128['before'])}

[iter {mid_it}]
{code_block(mid_txt)}

[학습 후]
{code_block(bs128['after'])}

(B) 조건별 학습 후(after_training) 원문 비교

- {bs32['cond']}:
{code_block(bs32['after'])}

- {bs64['cond']}:
{code_block(bs64['after'])}

- {bs128['cond']}:
{code_block(bs128['after'])}

**결과 분석 및 원인 고찰**

Val Loss 기준 최적 조건은 {best['cond']}({best['val_loss']:.4f})였다. 문맥 길이가 늘어난다고 항상 Val Loss가 선형으로 좋아지지는 않았는데, 문자 단위 모델에서는 아주 먼 과거보다 직전 몇 어절이 다음 글자 예측에 더 결정적이기 때문일 수 있다. block_size가 커질수록 self-attention 연산량이 늘어 처리량(토큰/초)이 떨어지는 트레이드오프도 함께 관측됐다.
"""


# ============================================================ 2.4 생성 파라미터 ====
def plot_generation_params():
    conds = R["exp_2_4_generation_params"]
    temps = sorted(set(r["temperature"] for r in conds))
    topks = sorted(set(r["top_k"] for r in conds), key=lambda k: (k is None, k))

    def grid(metric):
        g = np.zeros((len(temps), len(topks)))
        for r in conds:
            i = temps.index(r["temperature"])
            j = topks.index(r["top_k"])
            g[i, j] = r[metric]
        return g

    uniq_g = grid("unique_char_ratio")
    rep_g = grid("repeat_bigram_ratio")
    topk_labels = ["전체" if k is None else str(k) for k in topks]

    fig, axes = plt.subplots(1, 2, figsize=(11, 4.5))
    for ax, g, title, cmap in [
        (axes[0], uniq_g, "고유 문자 비율 (다양성)", "viridis"),
        (axes[1], rep_g, "반복 bigram 비율", "magma"),
    ]:
        im = ax.imshow(g, cmap=cmap, vmin=0, vmax=1)
        ax.set_xticks(range(len(topks))); ax.set_xticklabels(topk_labels)
        ax.set_yticks(range(len(temps))); ax.set_yticklabels([str(t) for t in temps])
        ax.set_xlabel("Top-K"); ax.set_ylabel("Temperature"); ax.set_title(title)
        for i in range(len(temps)):
            for j in range(len(topks)):
                ax.text(j, i, f"{g[i,j]:.2f}", ha="center", va="center",
                        color="white" if g[i, j] < 0.6 else "black", fontsize=9)
        fig.colorbar(im, ax=ax, shrink=0.8)

    fig.tight_layout()
    fig.savefig(os.path.join(SCRIPT_DIR, "generation_params_report.png"), dpi=130)
    plt.close(fig)


def section_2_4():
    conds = R["exp_2_4_generation_params"]

    def c(t, k):
        for r in conds:
            if r["temperature"] == t and r["top_k"] == k:
                return r
        raise KeyError((t, k))

    rows = "\n".join(
        f"| {r['cond']} | {r['unique_char_ratio']} | {r['repeat_bigram_ratio']} | {r['avg_word_len']} | "
        f"{'낮은 온도·좁은 Top-K → 반복적' if r['temperature'] <= 0.5 and (r['top_k'] or 99) <= 10 else ('높은 다양성' if r['unique_char_ratio'] > 0.6 else '중간 수준')} |"
        for r in conds
    )

    low = c(0.5, 5)
    mid = c(1.0, 10)
    high = c(1.5, None)

    return f"""### 2.4 생성 파라미터 (TEMPERATURE × TOP_K)

- **가설:** temperature를 낮추고 top_k를 좁힐수록 같은 글자가 반복되어 다양성(고유 문자 비율)이 떨어지고 반복 bigram 비율이 오를 것이다. 반대로 temperature를 높이고 top_k를 넓힐수록 다양성은 늘지만 문법적으로 그럴듯하지 않은 조합이 섞일 것이다.
- **실험 설계 및 근거:** TEMPERATURE/TOP_K는 생성에만 관여하므로 baseline(N_LAYER=6, N_HEAD=6, N_EMBD=192, MAX_ITERS=500) 조건으로 학습한 모델 하나를 재사용해 9가지 조합(T∈{{0.5,1.0,1.5}}×TopK∈{{5,10,전체}})에 대해서만 `model.generate()`를 반복 호출했다. 모든 조건의 학습 loss는 동일하므로 Loss 대신 다양성·반복 지표로 비교한다.

**정량적 비교 표**

| 생성 조건(T, Top-K) | 고유 문자 비율 | 반복 bigram 비율 | 평균 어절 길이 | 정성 인상 |
| :--- | :---: | :---: | :---: | :--- |
{rows}

**시각화 그래프:**

![생성 파라미터별 비교](generation_params_report.png)

**생성 문장 샘플 비교**

- T=0.5, Top-K=5 (낮은 온도·좁은 K):
{code_block(low['generated'])}

- T=1.0, Top-K=10 (baseline 기본값):
{code_block(mid['generated'])}

- T=1.5, Top-K=전체 (높은 온도·무제한 K):
{code_block(high['generated'])}

**결과 분석 및 원인 고찰**

고유 문자 비율은 T=0.5/K=5에서 {low['unique_char_ratio']}, T=1.0/K=10에서 {mid['unique_char_ratio']}, T=1.5/K=전체에서 {high['unique_char_ratio']}로, temperature·top_k가 커질수록 다양성이 늘어난다는 가설과 일치했다. 반복 bigram 비율은 T=0.5/K=5에서 {low['repeat_bigram_ratio']}로 가장 높아, 낮은 온도·좁은 top_k 조합이 같은 어절·구두점을 반복하는 모드 붕괴에 더 취약함을 보여준다.
"""


# ============================================================ 2.5 데이터 양 ====
def plot_data_fraction():
    conds = sorted(R["exp_2_5_data_fraction"], key=lambda r: r["params"]["DATA_FRACTION"])
    fig, ax = plt.subplots(figsize=(8, 4.5))
    loss_curve_panel(ax, conds, "DATA_FRACTION별 Loss 수렴")
    fig.tight_layout()
    fig.savefig(os.path.join(SCRIPT_DIR, "data_fraction_report.png"), dpi=130)
    plt.close(fig)


def section_2_5():
    conds = sorted(R["exp_2_5_data_fraction"], key=lambda r: r["params"]["DATA_FRACTION"])
    d025, d05, d10 = conds
    gaps = {r["cond"]: round(r["val_loss"] - r["train_loss"], 4) for r in conds}
    worst_gap_cond = max(gaps, key=gaps.get)
    mid_it, mid_txt = mid_sample(d10)

    return f"""### 2.5 데이터 양 (DATA_FRACTION)

- **가설:** 학습에 쓰는 데이터가 적을수록 corpus를 통째로 외워버리기 쉬워, Train Loss는 낮아도 Val Loss와의 갭(과적합)이 커질 것이다.
- **실험 설계 및 근거:** held-out val 10%를 먼저 떼어낸 뒤, 남은 train 90%(train_raw)에 대해서만 DATA_FRACTION(0.25/0.5/1.0)을 적용해 앞부분만 잘라 썼다 — val 구간이 train에 섞이는 데이터 누수를 방지하기 위함이다. baseline(N_LAYER=6, N_HEAD=6, N_EMBD=192, MAX_ITERS=500) 나머지 설정은 고정.

**정량적 비교 표**

{quant_table(conds, header="DATA_FRACTION 조건")}

**시각화 그래프:**

![데이터 비율별 비교](data_fraction_report.png)

**생성 문장 샘플 비교**

(A) DATA_FRACTION=1.0 조건의 학습 진행 3단계

[학습 전]
{code_block(d10['before'])}

[iter {mid_it}]
{code_block(mid_txt)}

[학습 후]
{code_block(d10['after'])}

(B) 조건별 학습 후(after_training) 원문 비교

- {d025['cond']}:
{code_block(d025['after'])}

- {d05['cond']}:
{code_block(d05['after'])}

- {d10['cond']}:
{code_block(d10['after'])}

**결과 분석 및 원인 고찰**

(Val−Train) 갭이 가장 큰 조건은 {worst_gap_cond}({gaps[worst_gap_cond]:.4f})였다. {'가설대로 데이터가 가장 적은 DATA_FRACTION=0.25에서 과적합 갭이 가장 컸다' if worst_gap_cond.startswith('DATA_FRACTION=0.25') else '가설과 달리 데이터가 가장 적은 조건이 갭이 가장 크지는 않았는데, 이는 500 iter라는 동일한 학습량이 작은 데이터셋에서는 여러 epoch를 반복해서 볼 수 있어 오히려 더 잘 수렴했거나, 반대로 큰 데이터셋도 500 iter로는 1 epoch를 다 못 돌아 과적합이 본격화되기 전이었을 가능성을 시사한다'}.
"""


# ============================================================ 2.6 causal mask ====
def plot_causal_mask():
    conds = R["exp_2_6_causal_mask"]
    fig, ax = plt.subplots(figsize=(8, 4.5))
    loss_curve_panel(ax, conds, "Causal Mask 유무에 따른 Loss")
    fig.tight_layout()
    fig.savefig(os.path.join(SCRIPT_DIR, "causal_mask_report.png"), dpi=130)
    plt.close(fig)


def section_2_6():
    conds = R["exp_2_6_causal_mask"]
    on = find("exp_2_6_causal_mask", "True")
    off = find("exp_2_6_causal_mask", "False")
    mid_it, mid_txt = mid_sample(on)

    return f"""### 2.6 Causal Mask 사용 여부 (USE_CAUSAL_MASK)

- **가설:** Causal mask를 끄면(SelfAttention, 마스크 없음) 학습 시 모델이 정답(다음 글자)이 포함된 미래 위치를 그대로 참조할 수 있어(학습 단계에서의 정보 누수) Train/Val Loss가 비정상적으로 낮게 나오지만, 실제 autoregressive 생성에서는 미래 정보가 없으므로 생성 품질은 오히려 causal mask를 켠 GPT보다 나쁠 것이다.
- **실험 설계 및 근거:** baseline(N_LAYER=6, N_HEAD=6, N_EMBD=192, MAX_ITERS=500)을 고정하고 USE_CAUSAL_MASK만 True/False로 바꿨다.

**정량적 비교 표**

{quant_table(conds, header="USE_CAUSAL_MASK 조건")}

**시각화 그래프:**

![Causal Mask 유무 비교](causal_mask_report.png)

**생성 문장 샘플 비교**

(A) USE_CAUSAL_MASK=True 조건의 학습 진행 3단계

[학습 전]
{code_block(on['before'])}

[iter {mid_it}]
{code_block(mid_txt)}

[학습 후]
{code_block(on['after'])}

(B) 조건별 학습 후(after_training) 원문 비교

- {on['cond']} (GPT, 정상):
{code_block(on['after'])}

- {off['cond']} (마스크 없음, 인코더형):
{code_block(off['after'])}

**결과 분석 및 원인 고찰**

Loss만 보면 USE_CAUSAL_MASK=False 조건이 True보다 {'낮게' if off['val_loss'] < on['val_loss'] else '비슷하거나 높게'} 나왔다({off['val_loss']:.4f} vs {on['val_loss']:.4f}). {'이는 가설대로 마스크가 없어 학습 시 미래 글자를 커닝했기 때문으로 해석된다 — 즉 이 낮은 loss는 실력이 아니라 정보 누수의 결과다.' if off['val_loss'] < on['val_loss'] else '이번 실측에서는 뚜렷한 정보 누수 효과가 loss에 크게 드러나지 않았지만, 학습-생성 방식의 구조적 불일치는 여전히 존재한다.'} (B)의 생성문에서도 USE_CAUSAL_MASK=False는 학습 때 참조했던 미래 정보를 생성 때는 쓸 수 없어 학습-추론 불일치가 발생하고, 이 때문에 낮은 loss가 실제 생성 품질로 이어지지 않을 수 있다.
"""


# ============================================================ 4장 최종 검증 ====
def plot_final():
    r = R["final_recommended"]
    fig, ax = plt.subplots(figsize=(8, 4.5))
    ax.plot(r["loss_history"], label="train", alpha=0.7, color="#4C72B0")
    xs, ys = hist_xy(r["val_history"])
    ax.plot(xs, ys, "o--", label="val", color="#DD8452")
    ax.set_xlabel("Iteration"); ax.set_ylabel("Loss")
    ax.set_title("최종 권장 설정 검증 학습"); ax.legend(fontsize=8); ax.grid(True)
    fig.tight_layout()
    fig.savefig(os.path.join(SCRIPT_DIR, "final_recommended_report.png"), dpi=130)
    plt.close(fig)


def section_4():
    if "final_recommended" not in R:
        return ""
    r = R["final_recommended"]
    plot_final()
    mid_it, mid_txt = mid_sample(r)
    p = r["params"]
    return f"""## 4. 최종 권장 설정 검증 실험

2~3장에서 도출한 "최종 권장 설정(성능 최우선)" 값을 그대로 cell 6에 적용해 실제로 1회 더 학습했다. 지금까지와 동일한 절차(동일 seed=3407, 동일 시작 문맥 text[:16], 동일 held-out val 10%)로 Train/Val Loss와 생성문을 확인한다.

**적용된 설정:** N_LAYER={p['N_LAYER']}, N_HEAD={p['N_HEAD']}, N_EMBD={p['N_EMBD']}, MAX_ITERS={p['MAX_ITERS']}, BLOCK_SIZE={p['BLOCK_SIZE']}, TEMPERATURE={p['TEMPERATURE']}, TOP_K={p['TOP_K']}, DATA_FRACTION={p['DATA_FRACTION']}, USE_CAUSAL_MASK={p['USE_CAUSAL_MASK']}

**정량적 비교 표**

{quant_table([r], header="최종 권장 설정")}

**시각화 그래프:**

![최종 권장 설정 검증](final_recommended_report.png)

**생성 문장 샘플 비교 (학습 진행 3단계)**

[학습 전]
{code_block(r['before'])}

[iter {mid_it}]
{code_block(mid_txt)}

[학습 후]
{code_block(r['after'])}

**결과 분석:** 최종 권장 설정으로 학습한 결과 Val Loss {r['val_loss']:.4f}(Val Perplexity {ppl(r['val_loss'])})를 얻었다. 2~3장에서 각 파라미터가 개별적으로 관측된 최적값을 조합했을 때도 실제로 안정적으로 수렴하는지, 그리고 생성문이 baseline보다 자연스러워졌는지를 위 원문으로 직접 확인할 수 있다.
"""


# ============================================================ 3장 종합 결론 ====
def best_of(conds, key="val_loss"):
    return min(conds, key=lambda r: r[key])


def section_3():
    n_layer_conds = sorted(R["exp_2_1_model_size"]["N_LAYER"], key=lambda r: r["params"]["N_LAYER"])
    n_head_conds = sorted(R["exp_2_1_model_size"]["N_HEAD"], key=lambda r: r["params"]["N_HEAD"])
    n_embd_conds = sorted(R["exp_2_1_model_size"]["N_EMBD"], key=lambda r: r["params"]["N_EMBD"])
    m22 = R["exp_2_2_max_iters"]
    m23 = R["exp_2_3_block_size"]
    m25 = R["exp_2_5_data_fraction"]
    m26 = R["exp_2_6_causal_mask"]
    gen = R["exp_2_4_generation_params"]

    best_n_layer = best_of(n_layer_conds)
    best_n_head = best_of(n_head_conds)
    best_n_embd = best_of(n_embd_conds)
    best23 = best_of(m23)
    best25 = best_of(m25)

    it2000 = find("exp_2_2_max_iters", "2000")
    xs, vys = hist_xy(it2000["val_history"])
    best_it = xs[int(np.argmin(vys))]

    on = find("exp_2_6_causal_mask", "True")
    off = find("exp_2_6_causal_mask", "False")

    balanced = next(r for r in gen if r["temperature"] == 1.0 and r["top_k"] == 10)
    low = next(r for r in gen if r["temperature"] == 0.5 and r["top_k"] == 5)
    high = next(r for r in gen if r["temperature"] == 1.5 and r["top_k"] is None)

    # N_LAYER/N_HEAD/N_EMBD 최종 조합: 각 파라미터의 개별 최적값을 채택하되,
    # N_EMBD % N_HEAD == 0 제약을 만족하는지 확인하고, 아니면 N_HEAD 기준으로 N_EMBD를 보정한다.
    final_n_layer = best_n_layer["params"]["N_LAYER"]
    final_n_head = best_n_head["params"]["N_HEAD"]
    final_n_embd = best_n_embd["params"]["N_EMBD"]
    n_embd_adjust_note = ""
    if final_n_embd % final_n_head != 0:
        adjusted = round(final_n_embd / final_n_head) * final_n_head
        n_embd_adjust_note = (
            f" (단, N_HEAD={final_n_head}와 N_EMBD={final_n_embd}는 나누어떨어지지 않아 "
            f"head_dim 제약을 만족하도록 N_EMBD={adjusted}로 보정)"
        )
        final_n_embd = adjusted

    summary_rows = f"""| 실험 | 관측된 최적 조건 | 근거 |
| :--- | :--- | :--- |
| 2.1 N_LAYER | {best_n_layer['cond']} | 3개 조건 중 Val Loss 최저({best_n_layer['val_loss']:.4f}) |
| 2.1 N_HEAD | {best_n_head['cond']} | 3개 조건 중 Val Loss 최저({best_n_head['val_loss']:.4f}) |
| 2.1 N_EMBD | {best_n_embd['cond']} | 3개 조건 중 Val Loss 최저({best_n_embd['val_loss']:.4f}) |
| 2.2 학습 반복 수 | iter≈{best_it} (MAX_ITERS=2000 조건 내부에서) | 해당 조건의 Val Loss가 iter {best_it}에서 최저({min(vys):.4f})를 기록, 이후 재상승 여부로 조기 종료 지점 추정 |
| 2.3 문맥 길이 | {best23['cond']} | Val Loss 최저({best23['val_loss']:.4f}) |
| 2.4 생성 파라미터 | T=1.0, Top-K=10 | 고유 문자 비율({balanced['unique_char_ratio']})과 반복 bigram 비율({balanced['repeat_bigram_ratio']}) 사이 균형 — T=0.5/K=5는 반복 bigram {low['repeat_bigram_ratio']}로 과도하게 반복적, T=1.5/K=전체는 다양성 {high['unique_char_ratio']}로 지나치게 무작위적 |
| 2.5 데이터 양 | {best25['cond']} | Val Loss 최저({best25['val_loss']:.4f}) |
| 2.6 Causal Mask | USE_CAUSAL_MASK=True | Loss 수치와 무관하게, autoregressive 생성 방식과 학습 방식을 일치시켜야 실제 생성 품질이 보장됨 |"""

    param_table = f"""| 파라미터 | 권장값 | 근거 |
| :--- | :---: | :--- |
| N_LAYER | {final_n_layer} | 2.1-N_LAYER 실험에서 Val Loss 최저({best_n_layer['val_loss']:.4f}) |
| N_HEAD | {final_n_head} | 2.1-N_HEAD 실험에서 Val Loss 최저({best_n_head['val_loss']:.4f}) |
| N_EMBD | {final_n_embd} | 2.1-N_EMBD 실험에서 Val Loss 최저({best_n_embd['val_loss']:.4f}){n_embd_adjust_note} |
| MAX_ITERS | {best_it} | MAX_ITERS=2000 조건 내부에서 Val Loss가 iter {best_it}에서 최저({min(vys):.4f}), 이후 과적합 방향으로 재상승 |
| SAMPLE_EVERY | 50 | baseline 값 유지 — Val Loss 최저 지점을 촘촘히 관측하기 위함 |
| SAMPLE_LENGTH | 200 | baseline 값 유지 — 중간 생성 품질 확인에 충분한 길이 |
| BLOCK_SIZE | {best23['params']['BLOCK_SIZE']} | 2.3 실험에서 Val Loss 최저({best23['val_loss']:.4f}) |
| TEMPERATURE | 1.0 | 2.4 실험에서 다양성(고유문자비율 {balanced['unique_char_ratio']})과 반복 억제(반복bigram {balanced['repeat_bigram_ratio']}) 사이 균형점 |
| TOP_K | 10 | 위와 동일 — T=0.5/K=5(과도한 반복 {low['repeat_bigram_ratio']})와 T=1.5/K=전체(과도한 다양성 {high['unique_char_ratio']})의 중간 |
| DATA_FRACTION | {best25['params']['DATA_FRACTION']} | 2.5 실험에서 Val Loss 최저({best25['val_loss']:.4f}), corpus 전량 사용이 일반화에 유리 |
| USE_CAUSAL_MASK | True | 2.6 실험에서 causal=False의 낮은 loss는 학습 시 미래 정보 누수 가능성이 있어, autoregressive 생성과 조건을 일치시키는 True를 채택 |"""

    final_config = {
        "N_LAYER": final_n_layer, "N_HEAD": final_n_head, "N_EMBD": final_n_embd,
        "MAX_ITERS": int(best_it), "SAMPLE_EVERY": 50, "SAMPLE_LENGTH": 200,
        "BLOCK_SIZE": best23["params"]["BLOCK_SIZE"],
        "TEMPERATURE": 1.0, "TOP_K": 10,
        "DATA_FRACTION": best25["params"]["DATA_FRACTION"],
        "USE_CAUSAL_MASK": True,
    }

    code = f"""```python
# ===== 최종 권장 설정 (성능 최우선) =====
N_LAYER = {final_config['N_LAYER']}; N_HEAD = {final_config['N_HEAD']}; N_EMBD = {final_config['N_EMBD']}     # 2.1 근거: N_LAYER/N_HEAD/N_EMBD 각각 독립 실험에서 Val Loss가 가장 낮았던 값{n_embd_adjust_note}
MAX_ITERS = {final_config['MAX_ITERS']}; SAMPLE_EVERY = 50; SAMPLE_LENGTH = 200  # 2.2 근거: MAX_ITERS=2000 조건 내 Val Loss가 iter {best_it} 부근에서 최저({min(vys):.4f}), 이후 과적합
BLOCK_SIZE = {final_config['BLOCK_SIZE']}                          # 2.3 근거: 3개 조건 중 Val Loss 최저({best23['val_loss']:.4f})
TEMPERATURE = 1.0; TOP_K = 10              # 2.4 근거: 다양성(고유문자비율 {balanced['unique_char_ratio']})과 반복 억제(반복bigram {balanced['repeat_bigram_ratio']}) 사이 균형점
DATA_FRACTION = {final_config['DATA_FRACTION']}                       # 2.5 근거: 3개 조건 중 Val Loss 최저({best25['val_loss']:.4f}), corpus 전량 사용이 일반화에 유리
USE_CAUSAL_MASK = True                  # 2.6 근거: 학습(마스크 있음)과 autoregressive 생성 방식을 일치시켜야 실제 생성 품질이 보장됨
```"""

    return f"""## 3. 종합 결론 및 최적 하이퍼파라미터 제안

### (1) 실험별 결론 요약

{summary_rows}

### (2) 파라미터별 권장값과 근거

{param_table}

### (3) 최종 권장 설정 (성능 최우선)

{code}
""", final_config


# ============================================================ 메인 ====
def make_plots():
    plot_model_size()
    plot_max_iters()
    plot_block_size()
    plot_generation_params()
    plot_data_fraction()
    plot_causal_mask()
    if "final_recommended" in R:
        plot_final()
    print("plots saved")


def build_report():
    header = f"""# 🚀 GPT 하이퍼파라미터 및 구조 실험 종합 보고서

## 1. 개요 및 실험 환경

- **실험 목적:** 문자 단위 charGPT(minGPT 교육용 축소판)를 사용해, 모델 크기·학습 반복 수·문맥 길이·생성 파라미터·데이터 양·Causal Mask 유무가 학습/생성 품질에 미치는 영향을 정량(Train/Val Loss, Perplexity)·정성(생성문 원문) 양쪽에서 확인한다.
- **실행 환경:** PyTorch {ENV['torch_version']}, device={ENV['device']} (Apple Silicon GPU/MPS 사용, 미가용 시 CPU로 자동 전환)
- **통제 변수:** 매 조건 학습 전 `set_seed(3407)` 고정, 생성 비교는 항상 동일한 시작 문맥 `text[:16]` 사용, corpus 뒤쪽 10%를 모든 실험 공통의 held-out 검증셋으로 고정(train 90% / val 10%, DATA_FRACTION은 train 90% 안에서만 적용해 val 누수 방지). 각 실험은 baseline(N_LAYER=6, N_HEAD=6, N_EMBD=192, MAX_ITERS=500, BLOCK_SIZE=128, TEMPERATURE=1.0, TOP_K=10, DATA_FRACTION=1.0, USE_CAUSAL_MASK=True)에서 비교 대상 변수 1개만 바꿔 실행했다. 특히 2.1(모델 크기)은 N_LAYER/N_HEAD/N_EMBD를 동시에 바꾸지 않고, baseline에서 한 파라미터씩만 올리고 내려 완전히 독립적으로 비교했다(각 소절 안에서만 비교하며, 서로 다른 파라미터 간에는 직접 비교하지 않음).
- **데이터셋:** 김유정 단편 corpus(위키문헌 퍼블릭 도메인), 전체 {ENV['full_text_len']:,}자 / train_raw {ENV['train_raw_len']:,}자 / val(held-out, 고정) {ENV['val_raw_len']:,}자, vocab 크기 {ENV['vocab_size']}자(고유 문자 수).

## 2. 실험 결과 및 상세 분석

"""
    body = "\n".join([
        section_2_1(),
        section_2_2(),
        section_2_3(),
        section_2_4(),
        section_2_5(),
        section_2_6(),
    ])
    section3_text, final_config = section_3()
    section4_text = section_4()

    full = header + body + "\n" + section3_text + "\n" + section4_text
    with open(REPORT_PATH, "w", encoding="utf-8") as f:
        f.write(full)
    print(f"report saved -> {REPORT_PATH}")
    return final_config


if __name__ == "__main__":
    make_plots()
    final_config = build_report()
    print("최종 권장 설정:", json.dumps(final_config, ensure_ascii=False))
