#!/usr/bin/env python3
"""Executable source regression for the n-gram reference-only handoff."""

from __future__ import annotations

import importlib.util
from pathlib import Path


def _load_checker():
    path = Path(__file__).with_name("check-synexia-ngram-reference-pending-receipt.py")
    spec = importlib.util.spec_from_file_location("m3_ngram_reference_checker", path)
    if spec is None or spec.loader is None:
        raise AssertionError("checker module could not be loaded")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main() -> int:
    checker = _load_checker()
    root = Path(__file__).resolve().parents[2]
    checks = checker.inspect_source(root)
    passed = sum(item.passed for item in checks)
    assert passed == len(checks) == 26, checker.receipt_line(checks)
    assert all(item.passed for item in checks), checker.receipt_line(checks)
    print(f"M3_NGRAM_REFERENCE_PENDING_TEST_PASS checks={passed}/{len(checks)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
