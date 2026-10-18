#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only default-template custody composed with the sealed documentation successor."""
import importlib.util
from pathlib import Path
import sys

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "m3/compatibility/m3-default-template-source-seal.tsv"
SPEC = importlib.util.spec_from_file_location("m3_progress_receiver_custody",
        Path(__file__).with_name("check_object_adapter_progress_receiver_20261017.py"))
CHECKER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CHECKER)


def run(root=None):
    """Retain the five template owners within the exact 14-file current closure."""
    receiver = ROOT if root is None else Path(root)
    CHECKER.verify(receiver)
    # The shared verifier pins the original five-row manifest and proves each
    # before-to-after transition before checking the current source bytes.
    print("PASS 5 exact current M3 Java/template receiver digests")
    return 5


if __name__ == "__main__":
    run()
