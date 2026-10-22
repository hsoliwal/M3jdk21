#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Strict, source-sealed one-owner Apache Bag 4.5.0 receiving gate.

Do not change any older 36/7/40/three-test source receipt. Prove the
preceding product generation using its original verifier and Git checkouts,
then admit ONLY one production Java behavioral atom at exact SHA+blob identity.
"""
import argparse
import hashlib
from pathlib import Path
import subprocess
import sys

SOURCE_ROOT = "m3/collections/interop/src/"
TARGET = SOURCE_ROOT + "main/java/com/m3/collections/interop/M3ApacheBag.java"
PREVIOUS_HEAD = "378da3e595c0279faea6e2c3ea504364886f89bd"
PREVIOUS_GATE = ("m3/collections/interop/qualification/"
                 "compiler-three-test-successor-20261022/verify_compile_successor.py")
RECIPE = "com.synexia.m3.M3Jdk21ApacheBag450Retain20261022"
BEFORE_HASH = "5ff51272c4fd48ef11a09b9138ac2329392bb2a70e2a4fe43fab2889909d224f"
BEFORE_BLOB = "86473e61bf886a28ca3a40010b6bd25f5f34cdb0"
AFTER_HASH = "2044c8d98a76056270092b01d95c0784e97752105b6bc3be4698048764b56f7e"
AFTER_BLOB = "39dba81e4c34b54eaeca309d1b1740868853128d"
BEFORE_ATOM = (
    "        for (E key : new ArrayList<>(counts.distinctElements())) {\n"
    "            int excess = counts.count(key) - wanted.count(key);\n"
    "            if (excess > 0) changed |= remove(key, excess);\n"
    "        }")
AFTER_ATOM = (
    "        for (E key : new ArrayList<>(counts.distinctElements())) {\n"
    "            int owned = counts.count(key);\n"
    "            int requested = wanted.count(key);\n"
    "            // Apache Commons Collections 4.5.0 AbstractMapBag.retainAll:\n"
    "            // if requested > owned, its else branch removes *all* owned copies.\n"
    "            // Preserve that version-specific behavior, not the newer API variant.\n"
    "            int excess = requested > owned ? owned : owned - requested;\n"
    "            if (excess > 0) changed |= remove(key, excess);\n"
    "        }")


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii")
                        + b"\0" + data).hexdigest()


def receipt_rows(receipt: Path) -> list[str]:
    if receipt.is_symlink() or not receipt.is_file():
        raise ValueError("missing/symlinked one-owner M3 Apache source receipt")
    rows = [r.split("\t") for r in receipt.read_text(encoding="utf-8").splitlines()
            if r and not r.startswith("#")]
    expected = [TARGET, BEFORE_HASH, BEFORE_BLOB, AFTER_HASH, AFTER_BLOB,
                RECIPE, "PENDING_FULL_PROVIDER_JNI_CI"]
    if len(rows) != 1 or rows[0] != expected:
        raise ValueError("unapproved M3 Bag source revision/extra receipt row")
    return rows[0]


def source_paths(root: Path) -> set[str]:
    paths = subprocess.check_output(
        ["git", "ls-files", "--", SOURCE_ROOT + "main", SOURCE_ROOT + "test"],
        cwd=root, text=True).splitlines()
    if len(paths) != 76 or len(set(paths)) != 76:
        raise ValueError("exact 76 source owners not retained")
    return set(paths)


def check_revision(current: Path, previous: Path, receipt: Path) -> int:
    current = current.resolve(strict=True)
    previous = previous.resolve(strict=True)
    receipt_rows(receipt)
    current_paths = source_paths(current)
    previous_paths = source_paths(previous)
    if current_paths != previous_paths or TARGET not in current_paths:
        raise ValueError("unadmitted M3 interop source path deletion/addition")
    for relative in sorted(current_paths):
        old_path, new_path = previous / relative, current / relative
        if (old_path.is_symlink() or new_path.is_symlink()
                or not old_path.is_file() or not new_path.is_file()
                or not old_path.resolve().is_relative_to(previous)
                or not new_path.resolve().is_relative_to(current)):
            raise ValueError("missing/symlinked M3 owned array source: " + relative)
        old, new = old_path.read_bytes(), new_path.read_bytes()
        if relative != TARGET:
            if old != new:
                raise ValueError("unreviewed edit outside one Apache Bag semantic atom: "
                                 + relative)
            continue
        if (sha(old) != BEFORE_HASH or blob(old) != BEFORE_BLOB
                or sha(new) != AFTER_HASH or blob(new) != AFTER_BLOB
                or old.count(BEFORE_ATOM.encode("utf-8")) != 1
                or new != old.replace(BEFORE_ATOM.encode("utf-8"),
                                      AFTER_ATOM.encode("utf-8"), 1)
                or b"SPDX-License-Identifier: Apache-2.0" not in new[:1800]):
            raise ValueError("M3 Apache 4.5.0 atom source/image drift")
    return len(current_paths)


def verify(current: Path, previous: Path, predecessor: Path,
           historical: Path, receipt: Path) -> int:
    previous = previous.resolve(strict=True)
    if subprocess.check_output(["git", "rev-parse", "HEAD"],
                               cwd=previous, text=True).strip() != PREVIOUS_HEAD:
        raise ValueError("previous product source generation not pinned")
    older = previous / PREVIOUS_GATE
    if older.is_symlink() or not older.is_file():
        raise ValueError("previous M3 36+7+40+3 verifier missing")
    subprocess.run([sys.executable, str(older), "--product", str(previous),
                    "--predecessor", str(predecessor),
                    "--historical-product", str(historical)],
                   cwd=previous, check=True)
    return check_revision(current, previous, receipt)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", type=Path, required=True)
    parser.add_argument("--previous-product", type=Path, required=True)
    parser.add_argument("--predecessor", type=Path, required=True)
    parser.add_argument("--historical-product", type=Path, required=True)
    parser.add_argument("--receipt", type=Path,
                        default=Path(__file__).parent / "receipts" / "changed-bag-01.tsv")
    args = parser.parse_args()
    try:
        count = verify(args.product, args.previous_product, args.predecessor,
                       args.historical_product, args.receipt)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print("M3_APACHE_BAG_SOURCE_REFUSAL", error, file=sys.stderr)
        raise SystemExit(1) from error
    print(f"M3_APACHE_BAG_450_SOURCE_PASS retainedSources={count} revised=1")


if __name__ == "__main__":
    main()
