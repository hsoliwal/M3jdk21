#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed five-test source successor without rewriting any historical census.

The predecessor product checkout is pinned to a real source-commit, rather than
constructing a fictitious merge or relaxing original 36+40+8 receipts.
"""
import argparse
import hashlib
from pathlib import Path
import re
import subprocess
import sys

PREDECESSOR_HEAD = "55473093e8ef3aba832fa1a00a7e6e8ef8a54221"
SOURCE_PREFIX = "m3/collections/interop/src/"
RECEIPT_REL = ("m3/collections/interop/qualification/"
               "current-source-successors-20261026/CURRENT_TEST_SUCCESSIONS.tsv")
V2_REL = ("m3/collections/interop/qualification/"
          "current-census-superset-20261024/verify_superset_v2.py")
JAVA_BASE = SOURCE_PREFIX + "test/java/com/m3/collections/interop/"
STATUS = "SOURCE_ONLY_PENDING_FULL_JUNIT"
MERGE_RECIPE = "com.synexia.m3.InteropStableMergeAndFactoryCensus"
SPRING_RECIPE = "com.synexia.rewrite.M3SpringArrayOracle20261026"
EXPECTED = (
    (JAVA_BASE + "M3FiveLibraryArrayificationTest.java",
     "6d31a53eb94af7c3cfe08ce126ce1be1f8d7da1b09150bd72a12c602d3ab3565",
     "e220622405432368d2c46b32f9f1c197913e7c0a",
     "76944bcd035774a87852940ad1d314db1013ae3a189bbda8f847356a62449f1d",
     "d163cdc9ebbf7c023df16c423cc094f01987e4bd", MERGE_RECIPE, STATUS),
    (JAVA_BASE + "M3LibraryClosureTest.java",
     "5ece9a369bcf5a223cb163ed0857534600fcd53bcc881f239956be15a4200f40",
     "141661a24e962506a2f1f87106e7d7eab15e5d7c",
     "29691e36f212ae79c31a06a597a58e76c7e51280787c46db6d7264b30fd567e0",
     "879c1560e47f028195bdada4f8b4cca2382cfd52", SPRING_RECIPE, STATUS),
    (JAVA_BASE + "M3SdkFamiliesTest.java",
     "9b84b5d1c5d8b6f40e125cd458f5fa737e0cb84d742359105e332643929701fc",
     "dedf938dc8a292ae43b89d11666219a4e3f8487d",
     "d41be8f32b9ad6eca1a5e5b30ed8f556a28aaf837e1d01b8e61e5763bd376720",
     "ff4754059d05d2c9a40b93c05867024639b1bf6d", MERGE_RECIPE, STATUS),
    (JAVA_BASE + "M3SpringArrayContractTest.java",
     "a8b537381b64899e263eb481a95a83ae40a9b7ed8109fe401f2329c750419267",
     "978dc2e1b8468ae3178a35adc478267d34723b4e",
     "3282d28c0bf4272e8a426baccfbca383a89a05fae7b624e1a2340d1864135dd1",
     "dc2090392322557e021d7b27508dc95680f6e601", SPRING_RECIPE, STATUS),
    (JAVA_BASE + "M3SpringCaseInsensitiveSdkFactoryTest.java",
     "4ae299915a2d3a0d0b93ed74943d03f9298134f5b54878c6533ab8f4f3ad1d30",
     "f2927f1a920cc3628ac09d1fe3202628912adff1",
     "42530ed135329f1160693b9d998bbb86f9be8b1cca29ccb2af07d9eaa25d1fba",
     "530bbe2f3def29336a71c0d9030c8d541055641e", MERGE_RECIPE, STATUS),
)
HEX40 = re.compile(r"[0-9a-f]{40}\Z")
HEX64 = re.compile(r"[0-9a-f]{64}\Z")


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def receipt_rows(path: Path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("missing or aliased five-source successor receipt")
    rows = tuple(tuple(row.split("\t")) for row in path.read_text(encoding="utf-8").splitlines()
                 if row and not row.startswith("#"))
    if (len(rows) != 5 or any(len(row) != 7 for row in rows)
            or list(row[0] for row in rows) != sorted(set(row[0] for row in rows))):
        raise ValueError("five-source successor receipt shape or sorting drift")
    if rows != EXPECTED:
        raise ValueError("five-source successor receipt content drift")
    for path_str, before_sha, before_git, after_sha, after_git, recipe, status in rows:
        if (not path_str.startswith(JAVA_BASE) or not path_str.endswith(".java")
                or not HEX40.fullmatch(before_git) or not HEX40.fullmatch(after_git)
                or not HEX64.fullmatch(before_sha) or not HEX64.fullmatch(after_sha)
                or recipe not in (MERGE_RECIPE, SPRING_RECIPE) or status != STATUS):
            raise ValueError("invalid five-source successor row")
    return rows


def tracked(root: Path):
    data = subprocess.check_output(
        ["git", "-C", str(root), "ls-files", "--stage", "-z", "--", SOURCE_PREFIX])
    found = {}
    for item in data.split(b"\0"):
        if not item:
            continue
        try:
            header, source = item.split(b"\t", 1)
            mode, blob, stage = header.decode("ascii").split(" ")
            path = source.decode("utf-8")
        except (ValueError, UnicodeError) as exc:
            raise ValueError("malformed source index record") from exc
        if stage != "0" or path in found or ".." in Path(path).parts:
            raise ValueError("aliased or multi-stage source index")
        found[path] = (mode, blob)
    return found


def checked_bytes(root: Path, relative: str):
    path = root / relative
    if (path.is_symlink() or not path.is_file()
            or not path.resolve().is_relative_to(root)):
        raise ValueError("missing or aliased source: " + relative)
    return path.read_bytes()


def prove_current(product: Path, before: Path, manifest: Path):
    product = product.resolve(strict=True)
    before = before.resolve(strict=True)
    if product == before:
        raise ValueError("before and current source roots must differ")
    sha = subprocess.check_output(
        ["git", "-C", str(before), "rev-parse", "HEAD"], text=True).strip()
    if sha != PREDECESSOR_HEAD:
        raise ValueError("wrong exact predecessor commit")
    rows = receipt_rows(manifest)
    old_files, now_files = tracked(before), tracked(product)
    if old_files.keys() != now_files.keys():
        raise ValueError("unadmitted interop source addition/deletion")
    required = {row[0] for row in rows}
    if not required.issubset(now_files):
        raise ValueError("one or more reviewed tests untracked")
    for path in sorted(now_files):
        prev = checked_bytes(before, path)
        now = checked_bytes(product, path)
        old_mode, old_blob = old_files[path]
        new_mode, new_blob = now_files[path]
        if old_mode != new_mode or old_mode != "100644":
            raise ValueError("source mode drift: " + path)
        if git_blob(prev) != old_blob or git_blob(now) != new_blob:
            raise ValueError("source index/worktree disagreement: " + path)
        if path not in required and prev != now:
            raise ValueError("unadmitted Java owner changed: " + path)
    for path, prior_sha, prior_blob, after_sha, after_blob, _recipe, _status in rows:
        old = checked_bytes(before, path)
        now = checked_bytes(product, path)
        if hashlib.sha256(old).hexdigest() != prior_sha or git_blob(old) != prior_blob:
            raise ValueError("historical test image differs from pinned source: " + path)
        if hashlib.sha256(now).hexdigest() != after_sha or git_blob(now) != after_blob:
            raise ValueError("reviewed current test image drift: " + path)
        if old == now:
            raise ValueError("phantom successor with no source change: " + path)
    return len(now_files)


def replay_original(before: Path, historical: Path):
    original = before / V2_REL
    if original.is_symlink() or not original.is_file():
        raise ValueError("missing original V2 executable source verifier")
    subprocess.run([sys.executable, str(original), "--product", str(before),
                    "--historical-product", str(historical)], check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", type=Path, required=True)
    parser.add_argument("--before-product", type=Path, required=True)
    parser.add_argument("--historical-product", type=Path, required=True)
    args = parser.parse_args()
    now = args.product.resolve(strict=True)
    before = args.before_product.resolve(strict=True)
    count = prove_current(now, before, now / RECEIPT_REL)
    replay_original(before, args.historical_product.resolve(strict=True))
    print(f"M3_INTEROP_SOURCE_SUCCESSION_V3_PASS tests=5 unchanged={count - 5} "
          f"predecessor={PREDECESSOR_HEAD}")


if __name__ == "__main__":
    main()
