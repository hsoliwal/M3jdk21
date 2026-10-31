#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Admit exactly two additive JUnit sources after the real, unchanged V3-approved tree."""
import argparse
import hashlib
from pathlib import Path
import re
import subprocess
import sys

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "current-source-successors-20261026"))
import verify_source_v3 as v3

APPROVED = "b123804aff3ac6930e420cfb82b5ce07040049fe"
SRC = "m3/collections/interop/src/test/java/com/m3/collections/interop/"
RECEIPT = ("m3/collections/interop/qualification/"
           "current-source-successors-v4-20261031/ADDITIVE_TEST_SOURCES_V4.tsv")
V3_SCRIPT = ("m3/collections/interop/qualification/"
             "current-source-successors-20261026/verify_source_v3.py")
V3_SCRIPT_BLOB = "9fdc61dcf42e190add3458f2f55dd29203f4a540"
V3_RECEIPT_BLOB = "48226ca99a17b81deb1848a8e80b15f166819e81"
TEST = re.compile(r"@Test\b\s+void\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\(")

EXPECTED = (
    (SRC + "M3Flat3MapProviderTest.java",
     "508f91d8852c460c27cd4b81004496c58af08f5bab14432228fff2ebe33970ba",
     "802c3479c69e22d92ede9bab4213af16e128eafc",
     "nullMappingsAndThreeToFourGrowthPreserveReturnIdentity,"
     "collidingKeysAcrossWordBoundariesSurviveDeletionAndReuse,"
     "seededEightMethodDifferentialMatchesProviderAndJdk,"
     "equalValuesAndObservedHashCallbacksDoNotChangeSharedResults"),
    (SRC + "M3FrozenLongSetProviderTest.java",
     "80e0b77589a102a71ba81cbf7e4556a453601a388129f7525385300e1dca9a7a",
     "95a0cadb931759e68fe893f1451c92583ff3b426",
     "differentialAgainstGuavaImmutableSortedSet,"
     "differentialAgainstEclipseImmutableLongSet"),
)


def receipt_rows(file: Path) -> tuple:
    if file.is_symlink() or not file.is_file():
        raise ValueError("missing or aliased additive source receipt")
    rows = tuple(tuple(line.split("\t")) for line in
                 file.read_text(encoding="utf-8").splitlines()
                 if line and not line.startswith("#"))
    if rows != EXPECTED:
        raise ValueError("additive source manifest drift")
    return rows


def dedicated_methods(path: str, raw: bytes, declared: str) -> set[str]:
    if not path.startswith(SRC) or not path.endswith(".java"):
        raise ValueError("out-of-scope JUnit source")
    if b"SPDX-License-Identifier: Apache-2.0" not in raw[:1800]:
        raise ValueError("unlicensed new JUnit source")
    text = raw.decode("utf-8", "strict")
    if any(x in text for x in ("@ParameterizedTest", "@RepeatedTest", "@TestFactory")):
        raise ValueError("unadmitted JUnit annotation")
    found = TEST.findall(text)
    if (found != declared.split(",") or len(set(found)) != len(found)
            or len(found) != len(re.findall(r"@Test\b", text))):
        raise ValueError("dedicated JUnit method drift")
    return {"com.m3.collections.interop." + Path(path).stem + "#" + name
            for name in found}


def require_additive(previous: dict, current: dict, added: set):
    if set(previous) - set(current):
        raise ValueError("deleted historical interop source")
    if set(current) - set(previous) != added:
        raise ValueError("unadmitted interop source addition")
    for path, data in previous.items():
        if current[path] != data:
            raise ValueError("existing interop source Git index drift: " + path)


def prove(product: Path, approved: Path, before: Path, historical: Path) -> set[str]:
    product, approved, before, historical = (
        value.resolve(strict=True) for value in (product, approved, before, historical))
    if len({product, approved, before, historical}) != 4:
        raise ValueError("source roots must be distinct")
    sha = subprocess.check_output(
        ["git", "-C", str(approved), "rev-parse", "HEAD"], text=True).strip()
    if sha != APPROVED:
        raise ValueError("wrong real V3-approved predecessor commit")
    v3.prove_current(approved, before, approved / v3.RECEIPT_REL)
    v3.replay_original(before, historical)
    for path, blob in ((V3_SCRIPT, V3_SCRIPT_BLOB),
                       (v3.RECEIPT_REL, V3_RECEIPT_BLOB)):
        old = v3.checked_bytes(approved, path)
        new = v3.checked_bytes(product, path)
        if old != new or v3.git_blob(new) != blob:
            raise ValueError("immutable five-source V3 source/receipt changed")

    receipt = receipt_rows(product / RECEIPT)
    prev, current = v3.tracked(approved), v3.tracked(product)
    require_additive(prev, current, {row[0] for row in receipt})
    for path, (mode, blob) in prev.items():
        old = v3.checked_bytes(approved, path)
        new = v3.checked_bytes(product, path)
        if (mode != "100644" or old != new or
                v3.git_blob(old) != blob or v3.git_blob(new) != blob):
            raise ValueError("historical source changed since V3: " + path)

    methods = set()
    for path, expected_sha, expected_blob, names in receipt:
        raw = v3.checked_bytes(product, path)
        if (current[path] != ("100644", expected_blob)
                or v3.git_blob(raw) != expected_blob
                or hashlib.sha256(raw).hexdigest() != expected_sha):
            raise ValueError("source-sealed new JUnit file drift: " + path)
        added = dedicated_methods(path, raw, names)
        if added & methods:
            raise ValueError("duplicate atom JUnit identity")
        methods.update(added)
    if len(prev) != 84 or len(current) != 86 or len(methods) != 6:
        raise ValueError("source/JUnit cardinality drift")
    return methods


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", type=Path, required=True)
    parser.add_argument("--approved-product", type=Path, required=True)
    parser.add_argument("--before-product", type=Path, required=True)
    parser.add_argument("--historical-product", type=Path, required=True)
    args = parser.parse_args()
    methods = prove(args.product, args.approved_product,
                    args.before_product, args.historical_product)
    print("M3_INTEROP_SOURCE_V4_PASS baseline=84 additions=2 junit_atoms="
          + str(len(methods)) + " original_V3=EXECUTED_REAL_HEAD")


if __name__ == "__main__":
    main()
