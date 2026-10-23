#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors
"""Fail closed on the exact M3JDK21 native java.base sparse-mask source postimage."""
from pathlib import Path
import hashlib, sys
ROOT = Path(__file__).resolve().parents[3]
TASK = Path(__file__).resolve().parent
OWNER = "src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java"
TEMPLATE = TASK / "recipe/src/main/resources/com/synexia/rewrite/m3-array-mask-sparse-20261018/M3CollectionMaskCompactor.java.txt"
PREIMAGE = TASK / "recipe/src/test/resources/preimage/M3CollectionMaskCompactor.java"
BEFORE = "6d26934bab87d6bc68a0c38f0c8ceeec72dc5817e5fca12cfafbc276bafb133d"
AFTER = "050f051ebf615ed27a8089fa0874a398029fbfeabc25d4a310af1e1b2a0261b8"
def digest(path):
    if path.is_symlink() or not path.is_file(): raise ValueError("refuse missing/aliased source: " + str(path))
    return hashlib.sha256(path.read_bytes()).hexdigest()
def verify():
    if digest(PREIMAGE) != BEFORE: raise ValueError("preimage drift")
    if digest(TEMPLATE) != AFTER: raise ValueError("generated template drift")
    output = ROOT / OWNER
    if digest(output) != AFTER or output.read_bytes() != TEMPLATE.read_bytes():
        raise ValueError("JDK direct product differs from canonical emitted source")
    print("PASS native M3JDK21 java.base compactor source identity", AFTER)
if __name__ == "__main__":
    verify()
