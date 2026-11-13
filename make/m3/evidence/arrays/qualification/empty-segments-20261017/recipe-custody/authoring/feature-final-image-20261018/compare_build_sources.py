#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# SPDX-License-Identifier: Apache-2.0
"""Compare actual OpenJDK build source bytes, paths, kinds and execute modes.

The specified scopes are all src/make inputs plus the root build launchers.
This is not a claim about separately built m3 modules or other unlisted roots.
Neither repository is changed. Outputs are inputs to existing recipe custody.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import stat
import subprocess


SCOPES = ("src", "make", "configure", "Makefile")


def snapshot(root):
    rows = {}
    for scope in SCOPES:
        base = root / scope
        paths = sorted(base.rglob("*")) if base.is_dir() else [base]
        for path in paths:
            info = path.lstat()
            if stat.S_ISDIR(info.st_mode):
                continue
            relative = path.relative_to(root).as_posix()
            if stat.S_ISLNK(info.st_mode):
                kind, data = "symlink", os.readlink(path).encode("utf-8")
            elif stat.S_ISREG(info.st_mode):
                kind, data = "file", path.read_bytes()
            else:
                raise ValueError("unsupported build source kind: " + relative)
            rows[relative] = {"kind": kind, "executable": bool(info.st_mode & 0o111),
                "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
    return rows


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--built-repository", required=True, type=Path)
    p.add_argument("--receiving-repository", required=True, type=Path)
    p.add_argument("--output", required=True, type=Path)
    args = p.parse_args()
    left, right = args.built_repository.resolve(), args.receiving_repository.resolve()
    before, after = snapshot(left), snapshot(right)
    differences = []
    for path in sorted(before.keys() | after.keys()):
        if before.get(path) != after.get(path):
            differences.append({"path": path, "built": before.get(path), "receiving": after.get(path)})
        elif before[path]["kind"] == "file" and (left / path).read_bytes() != (right / path).read_bytes():
            raise AssertionError("equal hashes but unequal file bytes: " + path)
    canonical = lambda rows: json.dumps(rows, sort_keys=True, separators=(",", ":")).encode("utf-8")
    head = lambda repo: subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=repo, text=True).strip()
    result = {"schema": "M3_OPENJDK_BUILD_SOURCE_IDENTITY_V1", "scopes": list(SCOPES),
        "built_repository": str(left), "built_baseline": head(left),
        "receiving_repository": str(right), "receiving_baseline": head(right),
        "built_files": len(before), "receiving_files": len(after),
        "built_bytes": sum(x["bytes"] for x in before.values()),
        "receiving_bytes": sum(x["bytes"] for x in after.values()),
        "built_inventory_sha256": hashlib.sha256(canonical(before)).hexdigest(),
        "receiving_inventory_sha256": hashlib.sha256(canonical(after)).hexdigest(),
        "byte_comparison_after_matching_hashes": True,
        "differences": differences, "status": "PASS" if not differences else "DIFFERENT",
        "limits": "Source identity for the named OpenJDK build scopes. The build configuration, external tools, original build workspace, native image identity and executed tests retain their separate receipts. No separately built m3 module, hosted CI or phase-promotion claim."}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"status": result["status"], "files": len(before),
                      "differences": len(differences), "receipt": str(args.output)}))
    raise SystemExit(bool(differences))


if __name__ == "__main__":
    main()
