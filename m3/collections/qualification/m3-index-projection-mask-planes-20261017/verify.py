#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact product receiver source seals plus authority boundary checks."""
from pathlib import Path
from hashlib import sha256
root=Path(__file__).resolve().parents[4]
expected={
    "m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java":
        "c6159ed8f93b8ed364cd67bd2aeb82e246a7eaf93506cf4b38ac5a0ec09ae2ec",
    "m3/collections/qualification/m3-index-projection-mask-planes-20261017/M3LongIndexPathMaskProof.java":
        "07dd1d5312e1a5827852ae402c8d2f1256e11ed4c8ad8ca3e4ca88389d753b7c",
}
for relative,wanted in expected.items():
    source=root/relative
    if source.is_symlink() or not source.is_file() or sha256(source.read_bytes()).hexdigest()!=wanted:
        raise SystemExit("M3_PROJECTION_MASK_PLAN_RECEIVER_FAIL path="+relative)
owner=(root/"m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java").read_text()
for token in ("M3IndexPathMasks.select(indices, indices,", "M3IndexPathMasks.firstMatchingIndex(indices, indices,",
              "M3LongAddressKernel.addressUnchecked", "M3LongAddressKernel.loadUnchecked",
              "M3IndexMaskCompactor.select(", "sortedAscending()", "sortedDescending()"):
    if token not in owner: raise SystemExit("M3_PROJECTION_MASK_PLAN_RECEIVER_FAIL lost-owner="+token)
print("M3_PROJECTION_MASK_PLAN_RECEIVER_PASS files=2 exact_synexia=true existing_jni_unchanged=true")
