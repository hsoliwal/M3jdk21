#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Refuse a parser dependency that differs from the source-qualified artifact."""
from pathlib import Path
import argparse
import hashlib

EXPECTED_SHA256 = "bc5fef92a854c72e8706fc58f545a8ac3e9e4395a052b50810366b94869aef52"

def verify(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("source-built parser must be a regular artifact file")
    if hashlib.sha256(path.read_bytes()).hexdigest() != EXPECTED_SHA256:
        raise ValueError("source-built parser artifact identity drift")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    args = parser.parse_args()
    verify(args.jar)
    print("M3_SOURCE_BUILT_PARSER_IDENTITY_PASS " + EXPECTED_SHA256)
