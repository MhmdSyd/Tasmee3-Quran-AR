#!/usr/bin/env python3
"""Convert a Hugging Face Whisper checkpoint (pytorch_model.bin + config.json) into the openai-whisper
.pt format, which sherpa-onnx's exporter needs. Optionally verifies the result numerically.

Usage: hf_to_openai_whisper.py <hf_model_dir> <out.pt> [--verify]
"""
import json
import pathlib
import sys

import torch

# HF name fragment -> openai-whisper fragment. Order matters: specific patterns first.
RENAMES = [
    ("encoder_attn_layer_norm", "cross_attn_ln"),
    ("self_attn_layer_norm", "attn_ln"),
    ("final_layer_norm", "mlp_ln"),
    ("encoder_attn.q_proj", "cross_attn.query"),
    ("encoder_attn.k_proj", "cross_attn.key"),
    ("encoder_attn.v_proj", "cross_attn.value"),
    ("encoder_attn.out_proj", "cross_attn.out"),
    ("self_attn.q_proj", "attn.query"),
    ("self_attn.k_proj", "attn.key"),
    ("self_attn.v_proj", "attn.value"),
    ("self_attn.out_proj", "attn.out"),
    ("fc1", "mlp.0"),
    ("fc2", "mlp.2"),
    (".layers.", ".blocks."),
    ("decoder.layer_norm.", "decoder.ln."),
    ("encoder.layer_norm.", "encoder.ln_post."),
    ("embed_tokens", "token_embedding"),
    ("encoder.embed_positions.weight", "encoder.positional_embedding"),
    ("decoder.embed_positions.weight", "decoder.positional_embedding"),
]


def to_openai_key(hf_key: str) -> str:
    key = hf_key[len("model."):] if hf_key.startswith("model.") else hf_key
    for old, new in RENAMES:
        key = key.replace(old, new)
    return key


def convert_state_dict(hf_sd: dict) -> dict:
    out = {}
    for k, v in hf_sd.items():
        if k == "proj_out.weight":  # tied to decoder.embed_tokens
            continue
        out[to_openai_key(k)] = v.float()
    return out


def dims_from_config(cfg: dict) -> dict:
    return dict(
        n_mels=cfg["num_mel_bins"],
        n_audio_ctx=cfg["max_source_positions"],
        n_audio_state=cfg["d_model"],
        n_audio_head=cfg["encoder_attention_heads"],
        n_audio_layer=cfg["encoder_layers"],
        n_vocab=cfg["vocab_size"],
        n_text_ctx=cfg["max_target_positions"],
        n_text_state=cfg["d_model"],
        n_text_head=cfg["decoder_attention_heads"],
        n_text_layer=cfg["decoder_layers"],
    )


def verify(hf_dir: pathlib.Path, hf_sd: dict, dims: dict, new_sd: dict) -> None:
    """Strict-load into openai-whisper, then compare logits with the HF implementation."""
    from transformers import WhisperConfig, WhisperForConditionalGeneration
    from whisper.model import ModelDimensions, Whisper

    oa = Whisper(ModelDimensions(**dims))
    oa.load_state_dict(new_sd)  # strict: any wrong key name fails here
    oa.eval()
    hf = WhisperForConditionalGeneration(WhisperConfig.from_pretrained(str(hf_dir)))
    missing, unexpected = hf.load_state_dict(hf_sd, strict=False)
    assert not unexpected, f"unexpected HF keys: {unexpected}"
    assert set(missing) <= {"proj_out.weight"}, f"missing HF keys: {missing}"
    hf.eval()
    torch.manual_seed(0)
    mel = torch.randn(1, dims["n_mels"], 3000)
    tokens = torch.tensor([[50258, 50272, 50359, 50363, 100, 200, 300]])
    with torch.no_grad():
        a = oa(mel, tokens)
        b = hf(input_features=mel, decoder_input_ids=tokens).logits
    diff = (a - b).abs().max().item()
    print(f"verify: max |logit diff| openai vs HF = {diff:.2e}")
    assert diff < 1e-3, "converted model does not match the Hugging Face model"


def main() -> None:
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    hf_dir, out = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
    cfg = json.loads((hf_dir / "config.json").read_text())
    hf_sd = torch.load(hf_dir / "pytorch_model.bin", map_location="cpu", weights_only=True)
    dims = dims_from_config(cfg)
    new_sd = convert_state_dict(hf_sd)
    if "--verify" in sys.argv:
        verify(hf_dir, hf_sd, dims, new_sd)
    torch.save({"dims": dims, "model_state_dict": new_sd}, out)
    print(f"wrote {out} ({len(new_sd)} tensors), dims={dims}")


if __name__ == "__main__":
    main()
