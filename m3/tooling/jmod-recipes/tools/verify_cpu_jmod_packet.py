# SPDX-License-Identifier: Apache-2.0
"""Read-only intake of independently packaged Java21 Synexia tool JMODs.

This verifies candidate artifacts and source bytes; it never installs or renames
a Synexia module into the M3JDK21 runtime. Target owners retain promotion authority.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
from pathlib import Path
import shutil
import subprocess
import sys


class Refusal(ValueError):
    """A source artifact or JMOD violates the exact receiver contract."""


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as stream:
        text = (line for line in stream if line.strip() and not line.startswith("#"))
        return list(csv.DictReader(text, delimiter="\t"))


def checked_file(path: Path, base: Path) -> bytes:
    if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(base.resolve()):
        raise Refusal(f"missing, foreign or symbolic file: {path}")
    return path.read_bytes()


def check(
    source: Path,
    catalog: Path,
    mapping: Path,
    receipt: Path,
    jmods: Path,
    jmod_exe: str,
) -> int:
    manifest, plan, results = rows(catalog), rows(mapping), rows(receipt)
    if any(len(items) != 18 for items in (manifest, plan, results)):
        raise Refusal("receiver requires exactly 18 source, map and JMOD receipt rows")
    by_source = {row["module"]: row for row in manifest}
    by_target = {row["source_module"]: row for row in plan}
    by_result = {row["module"]: row for row in results}
    if any(len(d) != 18 for d in (by_source, by_target, by_result)):
        raise Refusal("duplicated CPU module name")
    if by_source.keys() != by_target.keys() or by_source.keys() != by_result.keys():
        raise Refusal("source, target and image module sets differ")
    binary = shutil.which(jmod_exe)
    if binary is None:
        raise Refusal("JDK21 jmod tool unavailable")

    output_names = {p.name for p in jmods.glob("*.jmod") if p.is_file()}
    expected_names = {m + ".jmod" for m in by_source}
    if output_names != expected_names:
        raise Refusal("missing or foreign .jmod artifact")

    for name in sorted(by_source):
        src, mapped, evidence = by_source[name], by_target[name], by_result[name]
        if not name.startswith("com.synexia.m3.") or mapped["intake_mode"] != "INDEPENDENT_APACHE_MODULE":
            raise Refusal(f"unknown CPU tool authority {name}")
        suffix = name.removeprefix("com.synexia.m3.")
        if mapped["candidate_target_module"] != "com.m3.tools." + suffix:
            raise Refusal(f"target naming/role drift {name}")
        if mapped["status"] != "QUALIFICATION_PENDING" or mapped["phase"] != "OPTIONAL_TOOLING":
            raise Refusal(f"unreviewed automatic product promotion {name}")
        if src["status"] not in ("IMPLEMENTED", "ADAPTER_PENDING") or src["license"] != "Apache-2.0":
            raise Refusal(f"source type or license mismatch {name}")
        if evidence["main"] != src["main_class"] or evidence["selftest"] != "PASS":
            raise Refusal(f"missing executed module oracle {name}")
        if not src["main_class"].startswith(name + "."):
            raise Refusal(f"main class outside namespace {name}")
        modroot = source / "modules" / name / "src" / "main" / "java"
        descriptor = checked_file(modroot / "module-info.java", source)
        java_source = checked_file(modroot / (src["main_class"].replace(".", "/") + ".java"), source)
        source_sha = hashlib.sha256(descriptor + java_source).hexdigest()
        if source_sha != evidence["source-sha256"]:
            raise Refusal(f"source SHA-256 drift {name}")
        path = jmods / (name + ".jmod")
        data = checked_file(path, jmods)
        if len(data) != int(evidence["jmod-bytes"]) or data[:4] != b"JM\x01\x00":
            raise Refusal(f"non-JMOD or incorrect image size {name}")
        if hashlib.sha256(data).hexdigest() != evidence["jmod-sha256"]:
            raise Refusal(f"JMOD checksum mismatch {name}")
        run = subprocess.run((binary, "describe", str(path)), check=False, text=True,
                             stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=30)
        parts = run.stdout.splitlines()
        if run.returncode != 0 or not parts or parts[0] != name:
            raise Refusal(f"JMOD named descriptor not proved {name}")
        if "exports " + name not in run.stdout:
            raise Refusal(f"JMOD export missing {name}")
    return 18


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--source-manifest", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--jmods", type=Path, required=True)
    parser.add_argument("--map", type=Path, required=True)
    parser.add_argument("--jmod-exe", default="jmod")
    args = parser.parse_args()
    try:
        total = check(args.source_root, args.source_manifest, args.map,
                      args.receipt, args.jmods, args.jmod_exe)
    except (Refusal, KeyError, OSError, ValueError, subprocess.TimeoutExpired) as ex:
        print(f"M3_CPU_JMOD_RECEIVER_REFUSED: {ex}", file=sys.stderr)
        return 3
    print(f"M3_CPU_JMOD_PACKET_VERIFIED modules={total} CANDIDATE_ONLY")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
