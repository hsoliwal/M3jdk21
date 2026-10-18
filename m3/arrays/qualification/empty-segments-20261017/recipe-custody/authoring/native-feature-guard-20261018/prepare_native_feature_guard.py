#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Seal a native feature-macro guard with the existing Maven/OpenRewrite host.

Only candidate inputs are written. The emitted native payload keeps its
original GPL-2.0-only WITH Classpath-exception-2.0 license and attribution.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
TARGET = "src/java.base/linux/native/libjava/M3PrimitiveFixedArena.c"
BEFORE = "04c5281944444e9a9c4bee49748b2a80f7661f44897003f87619403e709e2ddb"
DATASET = "m3jdk21-native-feature-guard"
BASE_REF = "4b24824c3aa09d802121447f8970d442a855f064"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline-root", type=Path, required=True)
    parser.add_argument("--recipe-owner", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    args = parser.parse_args()
    base = args.baseline_root.resolve()
    owner = args.recipe_owner.resolve()
    work = args.work_dir.resolve()
    if work.is_relative_to(base):
        raise ValueError("candidate inputs must be outside receiving sources")
    original = (base / TARGET).read_bytes()
    if hashlib.sha256(original).hexdigest() != BEFORE:
        raise ValueError("native source preimage drift")
    token = b"#define _GNU_SOURCE\n"
    if original.count(token) != 1:
        raise ValueError("ambiguous feature macro")
    replacement = b"#ifndef _GNU_SOURCE\n#define _GNU_SOURCE\n#endif\n"
    candidate = original.replace(token, replacement, 1)
    output = work / "candidate" / TARGET
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(candidate)
    crate = owner / "recipe-crates/m3-collections-contract-refinement-20261017"
    spec = importlib.util.spec_from_file_location("native_guard_prepare", crate / "prepare.py")
    prepare = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prepare)
    metadata = prepare.seal(crate, DATASET, "hsoliwal/M3jdk21", base,
                            [work / "candidate"], BASE_REF, owner)
    receipt = {
        "schema": "M3_NATIVE_FEATURE_GUARD_CANDIDATE_V1",
        "dataset": DATASET,
        "target": TARGET,
        "before_sha256": BEFORE,
        "after_sha256": hashlib.sha256(candidate).hexdigest(),
        "candidate_only": True,
        "metadata": metadata,
        "category": "JNI / NATIVE_FEATURE_MACRO_ADMISSION",
        "selection": "Reuse the existing conditional feature-macro idiom; preserve a build-supplied definition and supply the original definition otherwise.",
        "existing_pattern": "src/hotspot/os/linux/os_linux.cpp at " + BASE_REF,
        "rejected": ["Disable warnings-as-errors", "Remove the feature macro from standalone source", "Change JNI mapping or ownership logic"],
        "contract": "No JNI signature, C function, mapping flag, file validation, exception, or cleanup path changes.",
        "license": "GPL-2.0-only WITH Classpath-exception-2.0",
        "required_gates": ["Original Maven/OpenRewrite5+sealer8+receiver9", "Preprocessed C equality", "Object-code equality with debug information disabled", "Original full JDK images build with warnings-as-errors"]
    }
    (work / "authoring-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
