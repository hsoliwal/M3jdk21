#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed static audit of standalone M3 core test *wiring*.

The result is not code coverage, test success, JNI parity, or product release
acceptance. It only proves that every top-level executable *Test.java file
is named in both the javac inputs and a Java invocation in m3/build.sh.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

COMPILED = re.compile(r"\bcore/test/([A-Za-z][A-Za-z0-9_]*Test)\.java\b")
EXECUTED = re.compile(
    r'(?m)^\s*"\$M3JDK/bin/java"[^\n]*-cp\s+build/tests(?:[:][^\s]+)?\s+'
    r'([A-Za-z][A-Za-z0-9_]*Test)\b'
)


def audit(root: Path) -> dict:
    root = root.resolve()
    directory = root / "m3/core/test"
    script = root / "m3/build.sh"
    if not directory.is_dir() or not script.is_file():
        raise FileNotFoundError("m3/core/test or m3/build.sh is missing")
    source = script.read_text(encoding="utf-8")
    discovered = {p.stem for p in directory.glob("*Test.java") if p.is_file()}
    compiled = set(COMPILED.findall(source))
    executed = set(EXECUTED.findall(source))
    return {
        "schema": "M3_CORE_TEST_WIRING_V1",
        "scope": "top-level m3/core/test/*Test.java (static compile/launch references)",
        "discovered": len(discovered),
        "compiled": len(discovered & compiled),
        "executed": len(discovered & executed),
        "missing_compile": sorted(discovered - compiled),
        "missing_execution": sorted(discovered - executed),
        "unknown_compile": sorted(compiled - discovered),
        "unknown_execution": sorted(executed - discovered),
        "passed": bool(discovered) and discovered <= compiled and discovered <= executed
                  and compiled <= discovered and executed <= discovered,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()
    result = audit(args.root)
    result["source_scope"] = str(args.root.resolve())
    rendered = json.dumps(result, indent=2, sort_keys=True) + "\n"
    if args.json_out:
        args.json_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_out.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0 if result["passed"] or not args.strict else 1


if __name__ == "__main__":
    raise SystemExit(main())
