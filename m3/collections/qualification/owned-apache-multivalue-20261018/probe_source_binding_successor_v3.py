#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Replay bounded negative CLI cases in an explicit, isolated source fixture."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

HERE = Path(__file__).resolve().parent
PREFIX = "m3/collections/qualification/owned-apache-multivalue-20261018/"
SEALED = "18a5bbbe10e3a97c01d7b3d55277a3e3dabc7ed4666ee00f1d7fb094ae8653d7"

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path)
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--jar", type=Path, required=True)
    args = parser.parse_args()
    root = (args.root if args.root is not None else HERE.parents[3]).resolve()
    work = args.work.resolve()
    if args.work.is_symlink() or work.is_relative_to(root) or work.exists():
        raise ValueError("use a new explicit work directory outside the source checkout")
    if shutil.disk_usage(work.parent).free < 8 * 1024**3 + 4 * 1024**2:
        raise ValueError("insufficient reserve for bounded fixture")
    receipt_raw = (HERE / "SDK_SOURCE_BINDING_SUCCESSOR_V3.json").read_bytes()
    if hashlib.sha256(receipt_raw).hexdigest() != SEALED:
        raise ValueError("unexpected sealed receipt")
    receipt = json.loads(receipt_raw)
    old = json.loads((root / receipt["history"][0]["path"]).read_bytes())
    paths = {row["path"] for row in old["sources"]}
    paths.update(row["path"] for row in receipt["history"])
    paths.update(row["path"] for row in receipt["arrayAndNativeOwners"])
    paths.update([receipt["before"]["path"], PREFIX + "SDK_SOURCE_BINDING_SUCCESSOR_V3.json", PREFIX + "verify_source_binding_successor_v3.py"])
    fixture = work / "fixture"
    saved = {}
    for path in sorted(paths):
        source = root / path
        if source.is_symlink() or not source.resolve().is_relative_to(root):
            raise ValueError("unapproved source path: " + path)
        saved[path] = source.read_bytes()
    work.mkdir()
    for path, data in saved.items():
        target = fixture / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    helper = fixture / (PREFIX + "verify_source_binding_successor_v3.py")
    output = work / "owned.args"
    env = dict(os.environ)
    results = []
    def invoke(label: str, negative: bool = False, optimized: bool = False) -> None:
        if negative:
            output.write_bytes(b"PRESERVE ARGUMENT OUTPUT\n")
        command = [sys.executable, "-I", "-S", "-B"]
        if optimized:
            command.append("-O")
        command.extend([str(helper), "--root", str(fixture), "--jar", str(args.jar.resolve()), "--args", str(output)])
        process = subprocess.run(command, env=env, capture_output=True, text=True, encoding="utf-8", timeout=60)
        if negative:
            ok = process.returncode != 0 and process.stdout == "" and output.read_bytes() == b"PRESERVE ARGUMENT OUTPUT\n"
        else:
            ok = process.returncode == 0
            if ok:
                report = json.loads(process.stdout)
                expected = [str((fixture / row["path"]).resolve()).replace("\\", "/") for row in old["sources"] if "/implementations/" in row["path"]]
                ok = report["source_custody"] == "PASS" and report["sources"] == 33 and output.read_text(encoding="utf-8").splitlines() == ['"' + path + '"' for path in expected]
        results.append({"case": label, "expectedRefusal": negative, "optimized": optimized, "returncode": process.returncode, "passed": ok, "stdout": process.stdout, "stderr": process.stderr})
        if not ok:
            raise AssertionError("CLI contract failed: " + label)
    completed = False
    try:
        invoke("positive-before-negatives")
        # Every pinned source or historical record gets an independent byte drift.
        for path in sorted(paths - {PREFIX + "verify_source_binding_successor_v3.py"}):
            target = fixture / path
            original = saved[path]
            try:
                target.write_bytes(original[:-1] + bytes([original[-1] ^ 1]))
                invoke("byte-drift:" + path, True)
            finally:
                target.write_bytes(original)
        sdk = fixture / receipt["sdkPath"]
        for label, data in [("historical7020", saved[receipt["before"]["path"]]), ("bom", b"\xef\xbb\xbf" + saved[receipt["sdkPath"]]), ("crlf", saved[receipt["sdkPath"]].replace(b"\n", b"\r\n"))]:
            try:
                sdk.write_bytes(data)
                invoke(label, True)
            finally:
                sdk.write_bytes(saved[receipt["sdkPath"]])
        try:
            sdk.write_bytes(saved[receipt["sdkPath"]][:-1] + b"x")
            invoke("optimized-sdk-drift", True, True)
        finally:
            sdk.write_bytes(saved[receipt["sdkPath"]])
        invoke("positive-after-negatives")
        completed = True
    finally:
        restored = all((fixture / path).read_bytes() == data for path, data in saved.items())
        report = {"status": "PASS" if completed and all(row["passed"] for row in results) and restored else "FAIL", "negativeCases": sum(row["expectedRefusal"] for row in results), "positiveCases": sum(not row["expectedRefusal"] for row in results), "fixtureFiles": len(saved), "fixtureBytes": sum(map(len, saved.values())), "restored": restored, "scope": "Independent byte-drift over every pinned source/history/receipt plus SDK historical/BOM/CRLF and optimized-mode checks. Earlier Windows source-blind98-case proof remains separate; this portable subset does not claim identical coverage.", "cases": results}
        (work / "PROBE_RESULT.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: report[key] for key in ["status", "negativeCases", "positiveCases", "fixtureFiles", "fixtureBytes", "restored"]}))

if __name__ == "__main__":
    main()
