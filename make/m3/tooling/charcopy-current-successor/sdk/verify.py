#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""The unchanged SDK POM's validate hook checks sealed inputs, not execution."""
import os
from pathlib import Path
import subprocess
import sys

if __name__ == "__main__":
    if sys.argv[1:] != ["--self-test"]:
        raise SystemExit("expected --self-test")
    output = os.environ.get("M3_QUALIFICATION_ROOT")
    if not output:
        raise SystemExit("explicit qualification root required")
    subprocess.run([sys.executable, "-I", "-S", "-B",
                    str(Path(__file__).resolve().parent.parent / "qualify.py"),
                    "--inputs-only", "--output-root", output], check=True)
