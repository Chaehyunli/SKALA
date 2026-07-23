"""
6개 하이퍼파라미터 실험 실행 스크립트.
transformer_minimodel_practice_(기본).ipynb 의 cell 6 상수만 바꿔가며
동일한 모델/트레이너 코드로 재실행 → 결과를 results.json 에 누적 저장.
"""
import os, math, time, random, json, copy

import numpy as np
import torch
import torch.nn as nn
from torch.nn import functional as F
from dataclasses import dataclass
from collections import defaultdict

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RESULTS_PATH = os.path.join(SCRIPT_DIR, "results.json")
DATA_PATH = os.path.join(SCRIPT_DIR, "input_kr.txt")

DEVICE = torch.device("mps") if torch.backends.mps.is_available() else \
          (torch.device("cuda") if torch.cuda.is_available() else torch.device("cpu"))


def set_seed(seed):
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)


# ---------------------------------------------------------------- 데이터 ----
with open(DATA_PATH, "r", encoding="utf-8") as f:
    FULL_TEXT = f.read()

VAL_SPLIT = int(len(FULL_TEXT) * 0.9)
TRAIN_RAW = FULL_TEXT[:VAL_SPLIT]     # 앞 90% = train 풀
VAL_RAW = FULL_TEXT[VAL_SPLIT:]       # 뒤 10% = held-out val (모든 실험 공통 고정)

# vocab은 corpus 전체(원문)에서 한 번만 만들어 모든 실험·train/val이 공유
_CHARS = sorted(list(set(FULL_TEXT)))
STOI = {ch: i for i, ch in enumerate(_CHARS)}
ITOS = {i: ch for i, ch in enumerate(_CHARS)}
VOCAB_SIZE = len(_CHARS)

CONTEXT_TEXT = FULL_TEXT[:16]  # 모든 실험 공통 시작 문맥 (text[:16])


class CharDataset(torch.utils.data.Dataset):
    def __init__(self, data, block_size):
        self.data = data
        self.block_size = block_size
        self.stoi = STOI
        self.itos = ITOS
        self.vocab_size = VOCAB_SIZE

    def __len__(self):
        return max(1, len(self.data) - self.block_size)

    def __getitem__(self, idx):
        chunk = self.data[idx: idx + self.block_size + 1]
        dix = [self.stoi[s] for s in chunk]
        x = torch.tensor(dix[:-1], dtype=torch.long)
        y = torch.tensor(dix[1:], dtype=torch.long)
        return x, y


# ------------------------------------------------------------- 모델 정의 ----
@dataclass
class GPTConfig:
    vocab_size: int
    block_size: int = 128
    n_layer: int = 6
    n_head: int = 6
    n_embd: int = 192
    embd_pdrop: float = 0.1
    resid_pdrop: float = 0.1
    attn_pdrop: float = 0.1
    use_causal_mask: bool = True


class NewGELU(nn.Module):
    def forward(self, x):
        return 0.5 * x * (1.0 + torch.tanh(
            math.sqrt(2.0 / math.pi) * (x + 0.044715 * torch.pow(x, 3.0))
        ))


class SelfAttention(nn.Module):
    def __init__(self, config):
        super().__init__()
        assert config.n_embd % config.n_head == 0
        self.c_attn = nn.Linear(config.n_embd, 3 * config.n_embd)
        self.c_proj = nn.Linear(config.n_embd, config.n_embd)
        self.attn_dropout = nn.Dropout(config.attn_pdrop)
        self.resid_dropout = nn.Dropout(config.resid_pdrop)
        self.n_head = config.n_head
        self.n_embd = config.n_embd

    def apply_mask(self, att, T):
        return att

    def forward(self, x):
        B, T, C = x.size()
        q, k, v = self.c_attn(x).split(self.n_embd, dim=2)
        k = k.view(B, T, self.n_head, C // self.n_head).transpose(1, 2)
        q = q.view(B, T, self.n_head, C // self.n_head).transpose(1, 2)
        v = v.view(B, T, self.n_head, C // self.n_head).transpose(1, 2)
        att = (q @ k.transpose(-2, -1)) * (1.0 / math.sqrt(k.size(-1)))
        att = self.apply_mask(att, T)
        att = F.softmax(att, dim=-1)
        att = self.attn_dropout(att)
        y = att @ v
        y = y.transpose(1, 2).contiguous().view(B, T, C)
        y = self.resid_dropout(self.c_proj(y))
        return y


class CausalSelfAttention(SelfAttention):
    def __init__(self, config):
        super().__init__(config)
        self.register_buffer(
            "bias",
            torch.tril(torch.ones(config.block_size, config.block_size))
                 .view(1, 1, config.block_size, config.block_size)
        )

    def apply_mask(self, att, T):
        return att.masked_fill(self.bias[:, :, :T, :T] == 0, float("-inf"))


class Block(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.ln_1 = nn.LayerNorm(config.n_embd)
        self.attn = CausalSelfAttention(config) if config.use_causal_mask else SelfAttention(config)
        self.ln_2 = nn.LayerNorm(config.n_embd)
        self.mlp = nn.ModuleDict(dict(
            c_fc=nn.Linear(config.n_embd, 4 * config.n_embd),
            c_proj=nn.Linear(4 * config.n_embd, config.n_embd),
            act=NewGELU(),
            dropout=nn.Dropout(config.resid_pdrop),
        ))
        m = self.mlp
        self.mlpf = lambda x: m.dropout(m.c_proj(m.act(m.c_fc(x))))

    def forward(self, x):
        x = x + self.attn(self.ln_1(x))
        x = x + self.mlpf(self.ln_2(x))
        return x


class GPT(nn.Module):
    def __init__(self, config):
        super().__init__()
        self.block_size = config.block_size
        self.transformer = nn.ModuleDict(dict(
            wte=nn.Embedding(config.vocab_size, config.n_embd),
            wpe=nn.Embedding(config.block_size, config.n_embd),
            drop=nn.Dropout(config.embd_pdrop),
            h=nn.ModuleList([Block(config) for _ in range(config.n_layer)]),
            ln_f=nn.LayerNorm(config.n_embd),
        ))
        self.lm_head = nn.Linear(config.n_embd, config.vocab_size, bias=False)
        self.apply(self._init_weights)
        for pn, p in self.named_parameters():
            if pn.endswith("c_proj.weight"):
                torch.nn.init.normal_(p, mean=0.0, std=0.02 / math.sqrt(2 * config.n_layer))
        self.n_params = sum(p.numel() for p in self.transformer.parameters())

    def _init_weights(self, module):
        if isinstance(module, nn.Linear):
            torch.nn.init.normal_(module.weight, mean=0.0, std=0.02)
            if module.bias is not None:
                torch.nn.init.zeros_(module.bias)
        elif isinstance(module, nn.Embedding):
            torch.nn.init.normal_(module.weight, mean=0.0, std=0.02)
        elif isinstance(module, nn.LayerNorm):
            torch.nn.init.zeros_(module.bias)
            torch.nn.init.ones_(module.weight)

    def forward(self, idx, targets=None):
        device = idx.device
        b, t = idx.size()
        assert t <= self.block_size
        pos = torch.arange(0, t, dtype=torch.long, device=device).unsqueeze(0)
        tok_emb = self.transformer.wte(idx)
        pos_emb = self.transformer.wpe(pos)
        x = self.transformer.drop(tok_emb + pos_emb)
        for block in self.transformer.h:
            x = block(x)
        x = self.transformer.ln_f(x)
        logits = self.lm_head(x)
        loss = None
        if targets is not None:
            loss = F.cross_entropy(logits.view(-1, logits.size(-1)), targets.view(-1), ignore_index=-1)
        return logits, loss

    @torch.no_grad()
    def generate(self, idx, max_new_tokens, temperature=1.0, do_sample=False, top_k=None):
        for _ in range(max_new_tokens):
            idx_cond = idx if idx.size(1) <= self.block_size else idx[:, -self.block_size:]
            logits, _ = self(idx_cond)
            logits = logits[:, -1, :] / temperature
            if top_k is not None:
                v, _ = torch.topk(logits, min(top_k, logits.size(-1)))
                logits[logits < v[:, [-1]]] = -float("Inf")
            probs = F.softmax(logits, dim=-1)
            if do_sample:
                idx_next = torch.multinomial(probs, num_samples=1)
            else:
                _, idx_next = torch.topk(probs, k=1, dim=-1)
            idx = torch.cat((idx, idx_next), dim=1)
        return idx


# ----------------------------------------------------------- Trainer ----
@dataclass
class TrainerConfig:
    max_iters: int = 500
    batch_size: int = 64
    learning_rate: float = 5e-4
    betas: tuple = (0.9, 0.95)
    weight_decay: float = 0.1
    grad_norm_clip: float = 1.0


def configure_optimizers(model, config):
    decay, no_decay = set(), set()
    whitelist = (torch.nn.Linear,)
    blacklist = (torch.nn.LayerNorm, torch.nn.Embedding)
    for mn, m in model.named_modules():
        for pn, p in m.named_parameters():
            fpn = "%s.%s" % (mn, pn) if mn else pn
            if pn.endswith("bias"):
                no_decay.add(fpn)
            elif pn.endswith("weight") and isinstance(m, whitelist):
                decay.add(fpn)
            elif pn.endswith("weight") and isinstance(m, blacklist):
                no_decay.add(fpn)
    param_dict = {pn: p for pn, p in model.named_parameters()}
    optim_groups = [
        {"params": [param_dict[pn] for pn in sorted(decay)], "weight_decay": config.weight_decay},
        {"params": [param_dict[pn] for pn in sorted(no_decay)], "weight_decay": 0.0},
    ]
    return torch.optim.AdamW(optim_groups, lr=config.learning_rate, betas=config.betas)


def get_batch(dataset, batch_size, device):
    idxs = np.random.randint(0, len(dataset), size=batch_size)
    xs, ys = zip(*(dataset[i] for i in idxs))
    return torch.stack(xs).to(device), torch.stack(ys).to(device)


@torch.no_grad()
def estimate_val_loss(model, val_dataset, device, batch_size=64, n_batches=10):
    model.eval()
    losses = []
    for _ in range(n_batches):
        x, y = get_batch(val_dataset, batch_size, device)
        _, loss = model(x, y)
        losses.append(loss.item())
    model.train()
    return float(np.mean(losses))


def diversity_metrics(s):
    """생성 파라미터 실험(2.4)용 다양성/반복 지표."""
    if len(s) == 0:
        return {"unique_char_ratio": 0.0, "repeat_bigram_ratio": 0.0, "avg_word_len": 0.0}
    unique_char_ratio = len(set(s)) / len(s)
    bigrams = [s[i:i+2] for i in range(len(s) - 1)]
    repeat_bigram_ratio = 1 - (len(set(bigrams)) / len(bigrams)) if bigrams else 0.0
    words = [w for w in s.split() if w]
    avg_word_len = float(np.mean([len(w) for w in words])) if words else 0.0
    return {
        "unique_char_ratio": round(unique_char_ratio, 4),
        "repeat_bigram_ratio": round(repeat_bigram_ratio, 4),
        "avg_word_len": round(avg_word_len, 2),
    }


BASELINE = dict(
    N_LAYER=6, N_HEAD=6, N_EMBD=192,
    MAX_ITERS=500, SAMPLE_EVERY=50, SAMPLE_LENGTH=200,
    BLOCK_SIZE=128,
    TEMPERATURE=1.0, TOP_K=10,
    DATA_FRACTION=1.0,
    USE_CAUSAL_MASK=True,
)

_CACHE = {}


def run_experiment(cond_name, group, **overrides):
    params = {**BASELINE, **overrides}
    key = tuple(sorted(params.items()))
    if key in _CACHE:
        cached = copy.deepcopy(_CACHE[key])
        cached["cond"] = cond_name
        cached["group"] = group
        cached["reused_from_cache"] = True
        print(f"[{group}/{cond_name}] cache hit -> reuse")
        return cached

    set_seed(3407)

    train_text = TRAIN_RAW[:int(len(TRAIN_RAW) * params["DATA_FRACTION"])]
    train_dataset = CharDataset(train_text, block_size=params["BLOCK_SIZE"])
    val_dataset = CharDataset(VAL_RAW, block_size=params["BLOCK_SIZE"])

    model_config = GPTConfig(
        vocab_size=VOCAB_SIZE,
        block_size=params["BLOCK_SIZE"],
        n_layer=params["N_LAYER"],
        n_head=params["N_HEAD"],
        n_embd=params["N_EMBD"],
        use_causal_mask=params["USE_CAUSAL_MASK"],
    )
    model = GPT(model_config).to(DEVICE)
    n_params = model.n_params

    x_context = torch.tensor(
        [STOI[c] for c in CONTEXT_TEXT], dtype=torch.long
    )[None, ...].to(DEVICE)

    model.eval()
    with torch.no_grad():
        gen = model.generate(x_context, 300, temperature=params["TEMPERATURE"],
                              do_sample=True, top_k=params["TOP_K"])[0]
    before_training = "".join(ITOS[int(i)] for i in gen)

    trainer_config = TrainerConfig(max_iters=params["MAX_ITERS"])
    optimizer = configure_optimizers(model, trainer_config)

    loss_history = []
    val_history = {}
    generation_history = {}

    model.train()
    t0 = time.time()
    for it in range(1, params["MAX_ITERS"] + 1):
        x, y = get_batch(train_dataset, trainer_config.batch_size, DEVICE)
        _, loss = model(x, y)
        model.zero_grad(set_to_none=True)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), trainer_config.grad_norm_clip)
        optimizer.step()
        loss_history.append(float(loss.item()))

        if it % params["SAMPLE_EVERY"] == 0 or it == params["MAX_ITERS"]:
            vloss = estimate_val_loss(model, val_dataset, DEVICE)
            val_history[it] = vloss
            model.eval()
            with torch.no_grad():
                y_gen = model.generate(x_context, params["SAMPLE_LENGTH"],
                                        temperature=params["TEMPERATURE"],
                                        do_sample=True, top_k=params["TOP_K"])[0]
            generation_history[it] = "".join(ITOS[int(i)] for i in y_gen)
            model.train()
            print(f"[{group}/{cond_name}] iter {it}/{params['MAX_ITERS']} "
                  f"train_loss={loss.item():.4f} val_loss={vloss:.4f}")

    elapsed = time.time() - t0

    model.eval()
    with torch.no_grad():
        gen = model.generate(x_context, 500, temperature=params["TEMPERATURE"],
                              do_sample=True, top_k=params["TOP_K"])[0]
    after_training = "".join(ITOS[int(i)] for i in gen)

    final_train_loss = float(np.mean(loss_history[-20:]))
    final_val_loss = val_history[max(val_history.keys())]

    result = {
        "cond": cond_name,
        "group": group,
        "params": {k: params[k] for k in
                   ["N_LAYER", "N_HEAD", "N_EMBD", "MAX_ITERS", "BLOCK_SIZE",
                    "TEMPERATURE", "TOP_K", "DATA_FRACTION", "USE_CAUSAL_MASK"]},
        "n_params": int(n_params),
        "train_loss": round(final_train_loss, 4),
        "val_loss": round(final_val_loss, 4),
        "time_s": round(elapsed, 2),
        "loss_history": loss_history,
        "val_history": val_history,
        "generation_history": generation_history,
        "before": before_training,
        "after": after_training,
        "reused_from_cache": False,
    }
    _CACHE[key] = copy.deepcopy(result)
    return result


def save_results(all_results):
    with open(RESULTS_PATH, "w", encoding="utf-8") as f:
        json.dump(all_results, f, ensure_ascii=False, indent=2)
    print(f"saved -> {RESULTS_PATH}")


def relabel(result, new_cond):
    """캐시에서 재사용한 baseline 결과를 그 실험 고유의 조건명으로 다시 라벨링(재학습 없음)."""
    r = copy.deepcopy(result)
    r["cond"] = new_cond
    return r


def main():
    print("device:", DEVICE)
    print(f"전체 corpus: {len(FULL_TEXT):,}자 / train_raw: {len(TRAIN_RAW):,}자 / val_raw(고정): {len(VAL_RAW):,}자")
    print(f"vocab size: {VOCAB_SIZE}")

    all_results = {"env": {"device": str(DEVICE), "torch_version": torch.__version__,
                            "vocab_size": VOCAB_SIZE,
                            "full_text_len": len(FULL_TEXT),
                            "train_raw_len": len(TRAIN_RAW),
                            "val_raw_len": len(VAL_RAW)},
                   "exp_2_1_model_size": {"N_LAYER": [], "N_HEAD": [], "N_EMBD": []},
                   "exp_2_2_max_iters": [],
                   "exp_2_3_block_size": [],
                   "exp_2_4_generation_params": [],
                   "exp_2_5_data_fraction": [],
                   "exp_2_6_causal_mask": []}

    # baseline (N_LAYER=6, N_HEAD=6, N_EMBD=192, iters=500, block=128, data=1.0, causal=True) 한 번만 실행
    # 이 결과는 각 실험 안에서 "baseline(변화 없음)" 지점으로만 재사용되고,
    # 조건명은 그 실험이 바꾸는 변수 기준으로 다시 라벨링한다 (실험끼리 서로 비교하지 않음)
    base = run_experiment("baseline", "shared_base", N_LAYER=6, N_HEAD=6, N_EMBD=192)
    save_partial(all_results)

    # 2.1 모델 크기 — N_LAYER/N_HEAD/N_EMBD를 baseline에서 "하나씩만" 올리고 내림 (3개 파라미터 × 2방향 = 6회 학습)
    n_layer_low = run_experiment("N_LAYER=3", "2.1-n_layer", N_LAYER=3, N_HEAD=6, N_EMBD=192)
    n_layer_high = run_experiment("N_LAYER=12", "2.1-n_layer", N_LAYER=12, N_HEAD=6, N_EMBD=192)
    all_results["exp_2_1_model_size"]["N_LAYER"] = [
        n_layer_low, relabel(base, "N_LAYER=6 (baseline)"), n_layer_high
    ]
    save_partial(all_results)

    n_head_low = run_experiment("N_HEAD=3", "2.1-n_head", N_LAYER=6, N_HEAD=3, N_EMBD=192)
    n_head_high = run_experiment("N_HEAD=12", "2.1-n_head", N_LAYER=6, N_HEAD=12, N_EMBD=192)
    all_results["exp_2_1_model_size"]["N_HEAD"] = [
        n_head_low, relabel(base, "N_HEAD=6 (baseline)"), n_head_high
    ]
    save_partial(all_results)

    n_embd_low = run_experiment("N_EMBD=96", "2.1-n_embd", N_LAYER=6, N_HEAD=6, N_EMBD=96)
    n_embd_high = run_experiment("N_EMBD=384", "2.1-n_embd", N_LAYER=6, N_HEAD=6, N_EMBD=384)
    all_results["exp_2_1_model_size"]["N_EMBD"] = [
        n_embd_low, relabel(base, "N_EMBD=192 (baseline)"), n_embd_high
    ]
    save_partial(all_results)

    # 2.2 학습 반복 수
    it100 = run_experiment("MAX_ITERS=100", "2.2", MAX_ITERS=100)
    all_results["exp_2_2_max_iters"].append(it100); save_partial(all_results)
    all_results["exp_2_2_max_iters"].append(relabel(base, "MAX_ITERS=500 (baseline)"))
    it2000 = run_experiment("MAX_ITERS=2000", "2.2", MAX_ITERS=2000)
    all_results["exp_2_2_max_iters"].append(it2000); save_partial(all_results)

    # 2.3 문맥 길이
    bs32 = run_experiment("BLOCK_SIZE=32", "2.3", BLOCK_SIZE=32)
    all_results["exp_2_3_block_size"].append(bs32); save_partial(all_results)
    bs64 = run_experiment("BLOCK_SIZE=64", "2.3", BLOCK_SIZE=64)
    all_results["exp_2_3_block_size"].append(bs64); save_partial(all_results)
    all_results["exp_2_3_block_size"].append(relabel(base, "BLOCK_SIZE=128 (baseline)"))
    save_partial(all_results)

    # 2.5 데이터 양
    df025 = run_experiment("DATA_FRACTION=0.25", "2.5", DATA_FRACTION=0.25)
    all_results["exp_2_5_data_fraction"].append(df025); save_partial(all_results)
    df05 = run_experiment("DATA_FRACTION=0.5", "2.5", DATA_FRACTION=0.5)
    all_results["exp_2_5_data_fraction"].append(df05); save_partial(all_results)
    all_results["exp_2_5_data_fraction"].append(relabel(base, "DATA_FRACTION=1.0 (baseline)"))
    save_partial(all_results)

    # 2.6 causal mask
    all_results["exp_2_6_causal_mask"].append(relabel(base, "USE_CAUSAL_MASK=True (baseline)"))
    nomask = run_experiment("USE_CAUSAL_MASK=False", "2.6", USE_CAUSAL_MASK=False)
    all_results["exp_2_6_causal_mask"].append(nomask); save_partial(all_results)

    # 2.4 생성 파라미터 (재학습 없이 baseline 모델을 다시 만들어 생성만 반복)
    gen_results = run_generation_param_experiment()
    all_results["exp_2_4_generation_params"] = gen_results
    save_partial(all_results)

    save_results(all_results)
    print("ALL DONE")


def save_partial(all_results):
    with open(RESULTS_PATH, "w", encoding="utf-8") as f:
        json.dump(all_results, f, ensure_ascii=False, indent=2)


def run_generation_param_experiment():
    """baseline 설정으로 학습한 모델 하나를 재사용해 생성 파라미터만 바꿔 비교."""
    set_seed(3407)
    params = BASELINE
    train_text = TRAIN_RAW[:int(len(TRAIN_RAW) * params["DATA_FRACTION"])]
    train_dataset = CharDataset(train_text, block_size=params["BLOCK_SIZE"])

    model_config = GPTConfig(
        vocab_size=VOCAB_SIZE, block_size=params["BLOCK_SIZE"],
        n_layer=params["N_LAYER"], n_head=params["N_HEAD"], n_embd=params["N_EMBD"],
        use_causal_mask=params["USE_CAUSAL_MASK"],
    )
    model = GPT(model_config).to(DEVICE)
    trainer_config = TrainerConfig(max_iters=params["MAX_ITERS"])
    optimizer = configure_optimizers(model, trainer_config)
    model.train()
    for it in range(1, params["MAX_ITERS"] + 1):
        x, y = get_batch(train_dataset, trainer_config.batch_size, DEVICE)
        _, loss = model(x, y)
        model.zero_grad(set_to_none=True)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), trainer_config.grad_norm_clip)
        optimizer.step()
    model.eval()

    x_context = torch.tensor([STOI[c] for c in CONTEXT_TEXT], dtype=torch.long)[None, ...].to(DEVICE)

    conditions = []
    for T in [0.5, 1.0, 1.5]:
        for K in [5, 10, None]:  # None = top_k 없음(전체 후보)
            conditions.append((T, K))

    results = []
    for T, K in conditions:
        set_seed(3407)
        with torch.no_grad():
            gen = model.generate(x_context, 400, temperature=T, do_sample=True, top_k=K)[0]
        text_out = "".join(ITOS[int(i)] for i in gen)
        metrics = diversity_metrics(text_out)
        results.append({
            "cond": f"T={T}, TopK={'전체' if K is None else K}",
            "temperature": T,
            "top_k": K,
            "generated": text_out,
            **metrics,
        })
        print(f"[2.4] T={T} K={K} -> unique_char_ratio={metrics['unique_char_ratio']}")
    return results


def run_and_save_final(overrides, cond_name="최종 권장 설정", out_key="final_recommended"):
    """2~6장 분석에서 도출한 최종 권장 하이퍼파라미터로 실제 학습 1회를 수행하고 results.json에 추가."""
    result = run_experiment(cond_name, "final", **overrides)
    with open(RESULTS_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    data[out_key] = result
    with open(RESULTS_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"[final] saved -> {RESULTS_PATH} ({out_key})")
    return result


if __name__ == "__main__":
    main()
