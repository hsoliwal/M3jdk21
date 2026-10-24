#!/usr/bin/env python3
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Replay bounded negative CLI cases of the v4 verifier in an explicit, isolated source fixture.

Adapted from the v3 probe: every pinned source, history record, receipt and the predecessor verifier gets an
independent byte drift; the SDK gets BOM, CRLF and optimized-mode drift; each D1 body is reverted to its v2
preimage (the recipe crate's .before.txt); every case must be refused without touching the argument output.
"""
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
PREFIX = "m3/collections/qualification/owned-apache-multivalue-20261024/"
VERIFIER = PREFIX + "verify_source_binding_successor_v4.py"
TEMPLATES = "m3/collections/recipe/owned-apache-multivalue-20261024/src/main/resources/com/synexia/rewrite/hash-pinned-java/apache-multivalue-v2-to-v3/"
SEALED = "3360d65da5075cee73e8c74a7aa863ce6dffa5faf0e7e8f5741526a301825646"
RESERVE = 256 * 1024**2

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
    # The fixture is about 1 MB; v3's fixed 8 GB reserve stays with the workflow job's own disk assertion.
    if shutil.disk_usage(work.parent).free < RESERVE:
        raise ValueError("insufficient reserve for bounded fixture")
    receipt_raw = (HERE / "SDK_SOURCE_BINDING_SUCCESSOR_V4.json").read_bytes()
    if hashlib.sha256(receipt_raw).hexdigest() != SEALED:
        raise ValueError("unexpected sealed receipt")
    receipt = json.loads(receipt_raw)
    v3 = json.loads((root / receipt["predecessor"]["path"]).read_bytes())
    old = json.loads((root / v3["history"][0]["path"]).read_bytes())
    paths = {row["path"] for row in old["sources"]}
    paths.update(row["path"] for row in v3["history"] + v3["arrayAndNativeOwners"] + receipt["added"])
    paths.update([v3["before"]["path"], v3["sdkPath"], receipt["predecessor"]["path"],
                  receipt["predecessorVerifier"]["path"], receipt["recipe"]["path"],
                  PREFIX + "SDK_SOURCE_BINDING_SUCCESSOR_V4.json", VERIFIER])
    d1 = [row["path"] for row in receipt["successors"] if row["reason"].startswith("D1:")]
    fixture = work / "fixture"
    saved = {}
    for path in sorted(paths):
        source = root / path
        if source.is_symlink() or not source.resolve().is_relative_to(root):
            raise ValueError("unapproved source path: " + path)
        saved[path] = source.read_bytes()
    reverted = {path: (root / (TEMPLATES + path.rsplit("/", 1)[1] + ".before.txt")).read_bytes() for path in d1}
    work.mkdir()
    for path, data in saved.items():
        target = fixture / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    helper = fixture / VERIFIER
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
                ok = (report["source_custody"] == "PASS" and report["sources"] == 33
                      and output.read_text(encoding="utf-8").splitlines() == ['"' + path + '"' for path in expected])
        results.append({"case": label, "expectedRefusal": negative, "optimized": optimized, "returncode": process.returncode, "passed": ok, "stdout": process.stdout, "stderr": process.stderr})
        if not ok:
            raise AssertionError("CLI contract failed: " + label)
    def replaced(label: str, path: str, data: bytes, optimized: bool = False) -> None:
        target = fixture / path
        try:
            target.write_bytes(data)
            invoke(label, True, optimized)
        finally:
            target.write_bytes(saved[path])
    completed = False
    try:
        invoke("positive-before-negatives")
        for path in sorted(paths - {VERIFIER}):
            original = saved[path]
            replaced("byte-drift:" + path, path, original[:-1] + bytes([original[-1] ^ 1]))
        sdk = saved[v3["sdkPath"]]
        replaced("bom", v3["sdkPath"], b"\xef\xbb\xbf" + sdk)
        replaced("crlf", v3["sdkPath"], sdk.replace(b"\n", b"\r\n"))
        replaced("optimized-sdk-drift", v3["sdkPath"], sdk[:-1] + b"x", True)
        for path, data in reverted.items():
            replaced("v2-revert:" + path, path, data)
        invoke("positive-after-negatives")
        completed = True
    finally:
        restored = all((fixture / path).read_bytes() == data for path, data in saved.items())
        report = {"status": "PASS" if completed and all(row["passed"] for row in results) and restored else "FAIL", "negativeCases": sum(row["expectedRefusal"] for row in results), "positiveCases": sum(not row["expectedRefusal"] for row in results), "fixtureFiles": len(saved), "fixtureBytes": sum(map(len, saved.values())), "restored": restored, "scope": "Independent byte drift over every pinned source/history/receipt, SDK BOM/CRLF/optimized-mode drift and a v2 revert of each D1 body. Portable subset; no Windows source-blind claim.", "cases": results}
        (work / "PROBE_RESULT.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: report[key] for key in ["status", "negativeCases", "positiveCases", "fixtureFiles", "fixtureBytes", "restored"]}))

if __name__ == "__main__":
    main()
