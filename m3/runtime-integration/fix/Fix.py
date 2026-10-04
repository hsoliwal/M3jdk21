#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Bind the existing runtime patch engine to this exact native/Java repair packet."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def engine():
    """An isolated module instance supplies resource context; no copied patch algorithm."""
    path = HERE.parent / "recipe/apply.py"
    expected = json.loads((HERE / "manifest.json").read_text())["engine_sha256"]
    if hashlib.sha256(path.read_bytes()).hexdigest() != expected:
        raise ValueError("runtime patch engine drift")
    spec = importlib.util.spec_from_file_location("m3_jni_fix_engine", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.HERE = HERE
    return module


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", type=Path, default=ROOT)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--reverse", action="store_true")
    args = parser.parse_args()
    state = engine().apply(args.target, reverse=args.reverse, check=args.check)
    print("M3_JNI_FIX state=" + state)


if __name__ == "__main__":
    main()
