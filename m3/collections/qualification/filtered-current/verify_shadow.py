#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Replay frozen filtered-window SDK Results in isolation, then bind evolved current owners.

Never apply a historical source-changing recipe to a newer product checkout.
The historical nine-target batch remains authoritative for its original input state.
Later product changes require explicit current Git-blob identities and runtime tests.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import subprocess
import sys
import tempfile

RECIPE_NAME = "filtered-navigation-20261018"
PREFIX = "m3/collections/qualification/m3-filtered-navigation-20261018"
WORKFLOW = ".github/workflows/m3-filtered-bounded-current-20261018.yml"
OLD_RECEIVE = '          python3 "$Q/recipe/receive.py" --dataset filtered-navigation-20261018 --target "$PWD" --check'
NEW_RECEIVE = '          python3 m3/collections/qualification/filtered-current/verify_shadow.py --crate "$Q/recipe" --dataset filtered-navigation-20261018 --target "$PWD"'
# Postimages of newer product work, NOT invented replacements for historical SDK results.
EVOLVED_GIT_BLOBS = {
    "m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java":
        "25e1ba23e0269cd6226e9f0b76fd9a43c6bb1d59",
    "m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java":
        "8c7c19eac9338bbc1a6f82a9e339cfb4952ad0c6",
    PREFIX + "/runtime/pom.xml":
        "ea72015355e7fdc7534775192af94c22ea468c64",
}
TARGET_COUNT = 9


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def checked(root: Path, relative: str) -> Path:
    p = PurePosixPath(relative)
    if (not relative or str(p) != relative or p.is_absolute()
            or ".." in p.parts or ".git" in p.parts or "\\" in relative
            or any(c in relative for c in "\t\r\n\0")):
        raise ValueError("invalid receiver source path: " + repr(relative))
    target = root / relative
    path = target
    while path != root:
        if path.is_symlink():
            raise ValueError("symlink source refused: " + relative)
        path = path.parent
    if not target.resolve().is_relative_to(root.resolve()):
        raise ValueError("path escapes root: " + relative)
    return target


def manifests(crate: Path, dataset: str) -> list[tuple[str, str, str, bytes, bytes | None]]:
    lane_root = crate / "datasets" / dataset / "resources/com/synexia/rewrite"
    meta = json.loads((crate / "datasets" / dataset / "DATASET.json").read_text())
    if (dataset != RECIPE_NAME or meta["dataset"] != RECIPE_NAME
            or meta["targets"] != TARGET_COUNT or meta["java_targets"] != 5
            or meta["text_targets"] != 4):
        raise ValueError("historical source batch schema/count drift")
    records: list[tuple[str, str, str, bytes, bytes | None]] = []
    seen: set[str] = set()
    for kind in ("java", "text"):
        lane = lane_root / ("hash-pinned-" + kind) / meta["crate_name"]
        manifest = lane / "manifest.tsv"
        for line in manifest.read_text().splitlines():
            if not line or line.startswith("#"):
                continue
            cells = line.split("\t")
            if len(cells) != 4:
                raise ValueError("invalid historical manifest")
            relative, before, after, resource = cells
            if relative in seen or relative.endswith(".java") != (kind == "java"):
                raise ValueError("duplicate or invalid manifest target")
            seen.add(relative)
            output = checked(lane, resource).read_bytes()
            if digest(output) != after:
                raise ValueError("historical template source drift: " + relative)
            original: bytes | None = None
            if before != "ABSENT":
                original = checked(lane, "pre/" + resource).read_bytes()
                if digest(original) != before:
                    raise ValueError("historical preimage drift: " + relative)
            records.append((relative, before, after, output, original))
    if len(records) != TARGET_COUNT or {item[0] for item in records} != (
            set(EVOLVED_GIT_BLOBS) | {WORKFLOW} | {
                "m3/collections/src/test/java/com/m3/collections/M3FilteredNavigationReconciliationContract.java",
                "m3/collections/src/test/java/com/m3/collections/M3FilteredNavigationReconciliationTest.java",
                "m3/collections/src/test/java/com/m3/collections/M3IndexPathMasksAliasContract.java",
                PREFIX + "/README.md",
                PREFIX + "/runtime/verify.py",
            }):
        raise ValueError("historical nine-target identity drift")
    return records


def sdk_receive(crate: Path, dataset: str, target: Path, check: bool) -> dict:
    command = [sys.executable, str(crate / "receive.py"), "--dataset", dataset,
               "--target", str(target)]
    if check:
        command.append("--check")
    result = subprocess.run(command, text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, check=False, timeout=90)
    if result.returncode:
        raise ValueError("historical SDK receiver failed: " + result.stderr.strip()[-600:])
    prefix, sep, payload = result.stdout.strip().partition(" ")
    if prefix != "M3_AST_BUILD_RECEIVER_PASS" or not sep:
        raise ValueError("receiver did not emit canonical SDK proof")
    value = json.loads(payload)
    if not value["actual_sdk_results"] or value["targets"] != TARGET_COUNT:
        raise ValueError("receiver did not use complete real SDK batch")
    return value


def verify(crate: Path, dataset: str, target: Path) -> dict:
    if not crate.is_dir() or not target.is_dir() or crate.is_symlink() or target.is_symlink():
        raise ValueError("source/recipient roots must be real directories")
    rows = manifests(crate, dataset)
    with tempfile.TemporaryDirectory(prefix="m3-filtered-historical-") as temp:
        shadow = Path(temp)
        for relative, before, _, _, original in rows:
            if before == "ABSENT":
                continue
            dest = checked(shadow, relative)
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(original)
            dest.chmod(0o644)
        receipts = [
            sdk_receive(crate, dataset, shadow, True),
            sdk_receive(crate, dataset, shadow, False),
            sdk_receive(crate, dataset, shadow, False),
        ]
        if [(r["mode"], r["changes"]) for r in receipts] != [
                ("check", TARGET_COUNT), ("apply", TARGET_COUNT), ("apply", 0)]:
            raise ValueError("historical source receiving/fixed-point contradiction")
        for relative, _, after, _, _ in rows:
            if digest(checked(shadow, relative).read_bytes()) != after:
                raise ValueError("historical receiving changed source bytes: " + relative)
    changed = 0
    exact = 0
    for relative, _, after, output, _ in rows:
        current_path = checked(target, relative)
        if not current_path.is_file():
            raise ValueError("current owner missing: " + relative)
        data = current_path.read_bytes()
        if relative == WORKFLOW:
            expected = output.replace(
                (OLD_RECEIVE + "\n").encode(), (NEW_RECEIVE + "\n").encode())
            if expected == output or data != expected:
                raise ValueError("workflow successor mutation not exactly admitted")
            changed += 1
        elif relative in EVOLVED_GIT_BLOBS:
            if git_blob(data) != EVOLVED_GIT_BLOBS[relative] or digest(data) == after:
                raise ValueError("newer product owner identity drift: " + relative)
            changed += 1
        elif digest(data) == after:
            exact += 1
        else:
            raise ValueError("historical product owner differs: " + relative)
    return {"schema": "M3_FILTERED_CURRENT_SHADOW_V1", "historical_results": TARGET_COUNT,
            "sdk_check_changes": TARGET_COUNT, "sdk_apply_changes": TARGET_COUNT,
            "sdk_fixed_point_changes": 0, "current_evolved": changed,
            "current_exact_historical": exact, "product_mutated": False,
            "source_identity_fail_closed": True}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--crate", type=Path, required=True)
    parser.add_argument("--dataset", default=RECIPE_NAME)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = verify(args.crate.absolute(), args.dataset, args.target.absolute())
    message = "M3_FILTERED_CURRENT_SHADOW_PASS " + json.dumps(result, sort_keys=True)
    print(message)
    if args.output:
        if args.output.exists() or args.output.is_symlink():
            raise ValueError("output file must be fresh")
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(message + "\n")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, KeyError, TimeoutError) as exc:
        print("M3_FILTERED_CURRENT_SHADOW_REFUSED " + str(exc), file=sys.stderr)
        raise SystemExit(2)
