#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed static audit of all standalone M3 core-test compile/launch wiring.

A legacy build script is historical source-sealed and MUST NOT be changed for
coverage. Additional tests belong to an additive release runner, explicitly
invoked after that build. This audit is NOT Java code coverage or a test pass.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

PRIMARY = "make/m3/build.sh"
SUPPLEMENTAL = "make/m3/release/run_extra_core_contracts.sh"
WORKFLOW = ".github/workflows/m3-first-release-admission.yml"
COMPILED = re.compile(r"\bcore/test/([A-Za-z][A-Za-z0-9_]*Test)\.java\b")
EXECUTED = re.compile(
    r'(?m)^\s*"\$M3JDK/bin/java"[^\n]*-cp\s+build/tests(?::[^\s]+)?\s+'
    r'([A-Za-z][A-Za-z0-9_]*Test)\b'
)


def audit(root: Path) -> dict:
    root = root.resolve()
    directory = root / "m3/core/test"
    if not directory.is_dir():
        raise FileNotFoundError("missing m3/core/test")
    scripts = (PRIMARY, SUPPLEMENTAL)
    source = {}
    missing_scripts = []
    for rel in scripts:
        path = root / rel
        if not path.is_file():
            missing_scripts.append(rel)
            source[rel] = ""
        else:
            source[rel] = path.read_text(encoding="utf-8")
    workflow = root / WORKFLOW
    contents = workflow.read_text(encoding="utf-8") if workflow.is_file() else ""
    invoked = ("bash ./make/m3/build.sh" in contents
               and "bash ./make/m3/release/run_extra_core_contracts.sh" in contents)
    discovered = {p.stem for p in directory.glob("*Test.java") if p.is_file()}
    compiled = set().union(*(set(COMPILED.findall(txt)) for txt in source.values()))
    executed = set().union(*(set(EXECUTED.findall(txt)) for txt in source.values()))
    return {
        "schema": "M3_CORE_TEST_WIRING_V2",
        "scope": "top-level m3/core/test/*Test.java across immutable build plus additive release runner",
        "scripts": list(scripts),
        "missing_scripts": missing_scripts,
        "release_workflow_invokes_both": invoked,
        "discovered": len(discovered),
        "compiled": len(discovered & compiled),
        "executed": len(discovered & executed),
        "missing_compile": sorted(discovered - compiled),
        "missing_execution": sorted(discovered - executed),
        "unknown_compile": sorted(compiled - discovered),
        "unknown_execution": sorted(executed - discovered),
        "passed": bool(discovered) and not missing_scripts and invoked
                  and discovered == compiled == executed,
        "disclaimer": "source wiring only, not code/branch coverage or executed Java/native proof",
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
