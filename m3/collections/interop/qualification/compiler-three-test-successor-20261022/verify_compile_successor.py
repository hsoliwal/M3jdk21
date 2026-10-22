#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Check a new three-file JDK21 test source epoch without modifying prior receipts.

The original 36+7+40 verifier executes UNCHANGED on its exact previous product
commit and original historical authoring commit. This successor then validates
that ONLY three named original test files changed, each by one approved Java
semantic atom, with exact old/new SHA-256 and Git blob identities.
"""
import argparse
import hashlib
from pathlib import Path
import subprocess
import sys

ORIGINAL_HEAD = "4bd27a6fc78ba3f7ca019125c096c33d8a324cee"
HISTORICAL_HEAD = "a720780b1a5ab41ea27182b9b7ad873efdf533b4"
SOURCE_PREFIX = "m3/collections/interop/src/"
ORIGINAL_VERIFIER = (
    "m3/collections/interop/qualification/current-census-superset-20261022/"
    "verify_superset.py")
RECIPE = "com.synexia.m3.M3Jdk21InteropThreeTestCompile20261022"
TEST_ROOT = SOURCE_PREFIX + "test/java/com/m3/collections/interop/"
APPROVED = {
    TEST_ROOT + "M3ApacheCollectionsTest.java": (
        "ede37d37ca4373ac7888da6542f7447730be822829f789ed82caaaf99dc2ec53",
        "9edf50007e098846f0d677204a445b6bc52b7f0d",
        "a0c4185e885964e56b4131149139fafcfd259b4147ff6a8255811090e55d5317",
        "54753ddbd1f4dbffe7ccd800188fb97acd745744",
        "\nfinal class M3ApacheCollectionsTest {",
        "\n@SuppressWarnings(\"deprecation\")\nfinal class M3ApacheCollectionsTest {"),
    TEST_ROOT + "M3FiveLibraryKeyRangesTest.java": (
        "f2f8adcccfee3366007330f84a630ddfb1f093e99cdac060952dddb59dda486d",
        "9698cccbf6ca77c7e6a48eec23cee7781838b19e",
        "ef93d9a53e0b9935c6e796b55ce2c47e4d94a732c7ac9f07b5a635eb436cc467",
        "bda93a103282d5b8a14a9ad600b054a6484d0d19",
        "import java.util.List;\n",
        "import java.util.List;\nimport java.util.Map;\n"),
    TEST_ROOT + "M3FiveProviderFlagSelectionTest.java": (
        "511c42a8f8cf2119770931307ef4c29f35e0fdbbc1ba438e170aa151905ca76e",
        "86939063fd00e472850add045086c07521ba29c3",
        "52289c1c82f43e18d1c661ffc26ee6d640129a9483838cda3199ee99e333cc31",
        "06769397f474d75b57b2a3309e37231c28f8c353",
        "M3FiveProviderDirectSdk.springMultiValueMap()",
        "M3LibraryCollections.springMultiValueMap()"),
}


def git_blob(data: bytes) -> str:
    return hashlib.sha1(
        b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def one_exact_atom(old: bytes, new: bytes, before: str, after: str) -> bool:
    before_bytes = before.encode("utf-8")
    after_bytes = after.encode("utf-8")
    return (old.count(before_bytes) == 1
            and before_bytes != after_bytes
            and new == old.replace(before_bytes, after_bytes, 1))


def rows(receipt: Path) -> list[list[str]]:
    if receipt.is_symlink() or not receipt.is_file():
        raise ValueError("missing or aliased exact test-source revision receipt")
    decoded = receipt.read_text(encoding="utf-8")
    parsed = [line.split("\t") for line in decoded.splitlines()
              if line and not line.startswith("#")]
    keys = [fields[0] for fields in parsed if len(fields) == 7]
    if (len(parsed) != 3 or any(len(fields) != 7 for fields in parsed)
            or keys != sorted(APPROVED)):
        raise ValueError("expected exactly three sorted source-custody revisions")
    for fields in parsed:
        path, before_sha, before_blob, after_sha, after_blob, recipe, state = fields
        expected = APPROVED[path][:4]
        if ((before_sha, before_blob, after_sha, after_blob) != expected
                or recipe != RECIPE or state != "PENDING_FULL_INTEROP_CI"):
            raise ValueError("unreviewed M3 five-provider Java21 source revision: " + path)
    return parsed


def tracked_files(root: Path) -> set[str]:
    output = subprocess.check_output(
        ["git", "ls-files", "--", SOURCE_PREFIX + "main", SOURCE_PREFIX + "test"],
        cwd=root, text=True)
    files = output.splitlines()
    if len(files) != 76 or len(set(files)) != 76:
        raise ValueError("product must retain all 76 source/test files")
    return set(files)


def check_source_delta(product: Path, predecessor: Path, receipt: Path) -> int:
    product = product.resolve(strict=True)
    predecessor = predecessor.resolve(strict=True)
    revisions = rows(receipt)
    new_paths = tracked_files(product)
    old_paths = tracked_files(predecessor)
    if old_paths != new_paths or not set(APPROVED).issubset(old_paths):
        raise ValueError("source path additions/deletions outside the sealed three-atom delta")
    accepted = {row[0]: row for row in revisions}
    for relative in sorted(old_paths):
        original = predecessor / relative
        current = product / relative
        if (original.is_symlink() or current.is_symlink() or not original.is_file()
                or not current.is_file() or not original.resolve().is_relative_to(predecessor)
                or not current.resolve().is_relative_to(product)):
            raise ValueError("missing/aliased source owner: " + relative)
        old = original.read_bytes()
        new = current.read_bytes()
        if relative not in accepted:
            if old != new:
                raise ValueError("unexpected changed Java owner outside reviewed atoms: " + relative)
            continue
        before_sha, before_blob, after_sha, after_blob, needle, replacement = APPROVED[relative]
        if (digest(old) != before_sha or git_blob(old) != before_blob
                or digest(new) != after_sha or git_blob(new) != after_blob
                or not one_exact_atom(old, new, needle, replacement)
                or b"SPDX-License-Identifier: Apache-2.0" not in new[:1800]):
            raise ValueError("unadmitted M3 Java21 compiler source delta: " + relative)
    return len(accepted)


def verify(product: Path, predecessor: Path, historic: Path, receipt: Path) -> int:
    product = product.resolve(strict=True)
    predecessor = predecessor.resolve(strict=True)
    historic = historic.resolve(strict=True)
    before = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=predecessor, text=True).strip()
    history = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=historic, text=True).strip()
    if before != ORIGINAL_HEAD or history != HISTORICAL_HEAD:
        raise ValueError("wrong pinned product/history provenance generation")
    # The preceding 36+7+40 source gate is run unchanged on its authoring epoch.
    old_verifier = predecessor / ORIGINAL_VERIFIER
    if old_verifier.is_symlink() or not old_verifier.is_file():
        raise ValueError("missing previous M3 interop source-verification authority")
    subprocess.run(
        [sys.executable, str(old_verifier), "--product", str(predecessor),
         "--historical-product", str(historic)], cwd=predecessor, check=True)
    return check_source_delta(product, predecessor, receipt)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", type=Path, required=True)
    parser.add_argument("--predecessor", type=Path, required=True)
    parser.add_argument("--historical-product", type=Path, required=True)
    parser.add_argument("--receipt", type=Path,
                        default=Path(__file__).parent / "receipts" / "changed-compile-03.tsv")
    args = parser.parse_args()
    try:
        admitted = verify(args.product, args.predecessor,
                          args.historical_product, args.receipt)
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print("M3_INTEROP_THREE_TEST_SOURCE_FAIL", str(error), file=sys.stderr)
        raise SystemExit(1) from error
    print(f"M3_INTEROP_THREE_TEST_SOURCE_PASS old36=36 old7=7 added40=40 "
          f"modifiedTests={admitted} totalSources=76")


if __name__ == "__main__":
    main()
