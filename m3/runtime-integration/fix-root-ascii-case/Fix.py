#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Bind the retained runtime patch engine to the ROOT-ASCII case repair packet."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def manifest():
    return json.loads((HERE / "manifest.json").read_text())


def engine():
    path = HERE.parent / "recipe/apply.py"
    expected = manifest()["engine_sha256"]
    if hashlib.sha256(path.read_bytes()).hexdigest() != expected:
        raise ValueError("runtime patch engine drift")
    spec = importlib.util.spec_from_file_location("m3_root_ascii_case_engine", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.HERE = HERE
    return module


def verify_proofs(target):
    target = target.resolve()
    for name, expected in manifest().get("verification_files", {}).items():
        path = target / name
        if not path.exists() or path.is_symlink():
            raise ValueError("verification drift: " + name)
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != expected:
            raise ValueError("verification drift: " + name)


def apply(target, reverse=False, check=False):
    verify_proofs(target)
    return engine().apply(target, reverse=reverse, check=check)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", type=Path, default=ROOT)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--reverse", action="store_true")
    args = parser.parse_args()
    state = apply(args.target, reverse=args.reverse, check=args.check)
    print("M3_ROOT_ASCII_CASE_FIX state=" + state)


if __name__ == "__main__":
    main()
