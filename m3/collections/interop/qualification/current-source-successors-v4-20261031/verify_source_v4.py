#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Replay immutable V3 history, then admit exact reviewed interop source successors.

This source gate does not replace the independent provider/JUnit/native gates.
The six original V4 JUnit atom identities remain the return contract for V5.
"""
import argparse
import hashlib
from pathlib import Path
import re
import subprocess
import sys
import types

HERE = Path(__file__).resolve().parent

APPROVED = "b123804aff3ac6930e420cfb82b5ce07040049fe"
SRC = "m3/collections/interop/src/test/java/jdk/m3/collections/interop/"
RECEIPT = ("m3/collections/interop/qualification/"
           "current-source-successors-v4-20261031/ADDITIVE_TEST_SOURCES_V4.tsv")
V3_SCRIPT = ("m3/collections/interop/qualification/"
             "current-source-successors-20261026/verify_source_v3.py")
V3_SCRIPT_BLOB = "9fdc61dcf42e190add3458f2f55dd29203f4a540"
V3_RECEIPT_BLOB = "48226ca99a17b81deb1848a8e80b15f166819e81"
CURRENT_V3_SCRIPT_BLOB = "5ca5bfa0524f87d47b8aeca99da2ad5a4a0c5115"
CURRENT_V3_RECEIPT_BLOB = "cacfab995ec8746c5d5db65cc5a3df12f03dc41a"
BEFORE_V2_BLOB = "d07a222453c53bff7a5784e05cec8f868c5d3a07"
SUCCESSIONS = ("m3/collections/interop/qualification/"
               "current-source-successors-v4-20261031/SOURCE_SUCCESSIONS_20261113.tsv")
SUCCESSIONS_SHA256 = "4796402ab39dcc91e0902312b1878a53fb6a1048397a59371b543afc0a682c98"
APPROVED_PREFIX = "m3/collections/interop/src/"
DISPOSITIONS = {
    "NAMESPACE": 44,
    "NAMESPACE_AND_TYPE_NAMES": 32,
    "NAMESPACE_TYPE_NAMES_AND_COMMENTS": 4,
    "QUALIFIED_SOURCE_SUCCESSOR": 4,
    "V4_ADDITION": 2,
    "LATER_QUALIFIED_ADDITION": 4,
}
LATER_ADDITIONS = {
    APPROVED_PREFIX + "main/java/jdk/m3/collections/interop/M3GuavaLongMultiset.java",
    APPROVED_PREFIX + "main/java/jdk/m3/collections/interop/M3LibraryBags.java",
    SRC + "M3BagInteropTest.java",
    SRC + "M3FiveProviderIntegrationTest.java",
}
ATTRIBUTIONS = {
    "make/m3/collections/qualification/mergeloss-01-15-20261106/EDITS.tsv":
        "a6949d00ad72a8a631728d1b8116521b40edc2484150398027b216d2e2beb1e2",
    "make/m3/collections/qualification/foldaudit-collections-20261107/EDITS.tsv":
        "92e1957330a6b8c5874a4452bb258b9fe772342c87a27c61f0e5bd34f7cf66da",
    "m3/collections/interop/qualification/bindings-gating-20261107/EDITS.tsv":
        "d39d7503ca53aa2a696e46760bff70d911a69ab909360c60f1090179b99b9b18",
}
HEX40 = re.compile(r"[0-9a-f]{40}\Z")
HEX64 = re.compile(r"[0-9a-f]{64}\Z")
TEST = re.compile(r"@Test\b\s+void\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\(")

EXPECTED = (
    (SRC + "M3Flat3MapProviderTest.java",
     "c9498ec115a48a9c23c884c62f1011af6bb439b2826a12d454a38f0768b17a47",
     "254d85294724b1cdcaa26a2c526da0fa17d623be",
     "nullMappingsAndThreeToFourGrowthPreserveReturnIdentity,"
     "collidingKeysAcrossWordBoundariesSurviveDeletionAndReuse,"
     "seededEightMethodDifferentialMatchesProviderAndJdk,"
     "equalValuesAndObservedHashCallbacksDoNotChangeSharedResults"),
    (SRC + "M3FrozenLongSetProviderTest.java",
     "d089236cb463d8b016c01b9c26216b918e7ec497442d6f01850c50216150291e",
     "4d88d4e3912ab9a5528f8c81a836d501bcf88511",
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
    return {"jdk.m3.collections.interop." + Path(path).stem + "#" + name
            for name in found}


def require_additive(previous: dict, current: dict, added: set):
    if set(previous) - set(current):
        raise ValueError("deleted historical interop source")
    if set(current) - set(previous) != added:
        raise ValueError("unadmitted interop source addition")
    for path, data in previous.items():
        if current[path] != data:
            raise ValueError("existing interop source Git index drift: " + path)


def checked_file(root: Path, relative: str) -> bytes:
    """Reject parent aliases as well as the direct-file aliases rejected by V3."""
    relative_path = Path(relative)
    if (relative_path.is_absolute() or relative_path.as_posix() != relative
            or ".." in relative_path.parts or "\\" in relative):
        raise ValueError("unsafe source path: " + relative)
    cursor = root / relative_path
    while cursor != root:
        if cursor.is_symlink():
            raise ValueError("missing or aliased source: " + relative)
        cursor = cursor.parent
    path = root / relative_path
    if not path.is_file() or not path.resolve().is_relative_to(root):
        raise ValueError("missing or aliased source: " + relative)
    return path.read_bytes()


def source_blob(raw: bytes) -> str:
    """Authenticate executable helpers without trusting code from those helpers."""
    return hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()


def source_module(name: str, file: Path, raw: bytes):
    module = types.ModuleType(name)
    module.__file__ = str(file)
    exec(compile(raw, module.__file__, "exec"), module.__dict__)
    return module


def load_current_v3(product: Path):
    raw = checked_file(product, V3_SCRIPT)
    if source_blob(raw) != CURRENT_V3_SCRIPT_BLOB:
        raise ValueError("current V3 executable authority drift")
    return source_module("_m3_preserved_current_v3", product / V3_SCRIPT, raw)


def load_original_v3(approved: Path):
    """Execute the authenticated historical bytes, never the relocated V3 copy."""
    raw = checked_file(approved, V3_SCRIPT)
    receipt = checked_file(approved, v3.RECEIPT_REL)
    if (source_blob(raw) != V3_SCRIPT_BLOB
            or source_blob(receipt) != V3_RECEIPT_BLOB):
        raise ValueError("historical V3 authority or receipt drift")
    # Compile the exact bytes just authenticated; do not reopen through a loader.
    return source_module("_m3_immutable_v3_source", approved / V3_SCRIPT, raw)


def head_sources(root: Path):
    raw = subprocess.check_output(
        ["git", "-C", str(root), "ls-tree", "-r", "-z", "HEAD", "--", APPROVED_PREFIX])
    result = {}
    for entry in raw.split(b"\0"):
        if not entry:
            continue
        descriptor, name = entry.split(b"\t", 1)
        mode, kind, blob = descriptor.decode().split()
        if kind != "blob":
            raise ValueError("unexpected historical source object")
        result[name.decode()] = (mode, blob)
    return result


def validate_succession_rows(rows):
    if (len(rows) != 90 or any(len(row) != 8 for row in rows)
            or [row[0] for row in rows] != sorted({row[0] for row in rows})):
        raise ValueError("source succession shape, ordering or uniqueness drift")
    before_paths, counts, additions = set(), {}, set()
    scopes = tuple(APPROVED_PREFIX + lane + "/java/jdk/m3/collections/interop/"
                   for lane in ("main", "test"))
    for path, sha, blob, before, before_sha, before_blob, disposition, attribution in rows:
        if (not path.startswith(scopes) or not path.endswith(".java")
                or Path(path).as_posix() != path or ".." in Path(path).parts
                or "\\" in path or not HEX64.fullmatch(sha) or not HEX40.fullmatch(blob)
                or disposition not in DISPOSITIONS):
            raise ValueError("invalid exact source succession row")
        counts[disposition] = counts.get(disposition, 0) + 1
        if before == "ABSENT":
            if (before_sha != "ABSENT" or before_blob != "ABSENT"
                    or disposition not in ("V4_ADDITION", "LATER_QUALIFIED_ADDITION")):
                raise ValueError("invalid additive source predecessor")
            additions.add(path)
        else:
            if (before != path.replace("/java/jdk/m3/", "/java/com/m3/")
                    or before in before_paths or not HEX64.fullmatch(before_sha)
                    or not HEX40.fullmatch(before_blob)
                    or disposition in ("V4_ADDITION", "LATER_QUALIFIED_ADDITION")):
                raise ValueError("invalid historical source succession")
            before_paths.add(before)
        if disposition in ("QUALIFIED_SOURCE_SUCCESSOR", "LATER_QUALIFIED_ADDITION"):
            if attribution not in ATTRIBUTIONS:
                raise ValueError("unadmitted source attribution")
        elif disposition == "V4_ADDITION":
            if attribution != RECEIPT or path not in {row[0] for row in EXPECTED}:
                raise ValueError("original V4 addition identity drift")
        elif attribution not in ("namespace-source-comparison", "eight-existing-type-name-comparison",
                                 "four-exact-comment-only-residuals"):
            raise ValueError("unadmitted naming succession attribution")
    if (counts != DISPOSITIONS or len(before_paths) != 84
            or additions != LATER_ADDITIONS | {row[0] for row in EXPECTED}):
        raise ValueError("source succession cardinality or addition-set drift")
    return rows


def succession_rows(product: Path):
    raw = checked_file(product, SUCCESSIONS)
    if hashlib.sha256(raw).hexdigest() != SUCCESSIONS_SHA256:
        raise ValueError("source succession manifest content drift")
    rows = tuple(tuple(line.split("\t")) for line in raw.decode("utf-8").splitlines()
                 if line and not line.startswith("#"))
    return validate_succession_rows(rows)


def prove_successions(product: Path, approved: Path, rows, previous, current):
    historical = {row[3]: ("100644", row[5]) for row in rows if row[3] != "ABSENT"}
    if previous != historical:
        raise ValueError("historical source index differs from exact succession")
    translated = {row[0]: ("100644", row[2]) for row in rows if row[3] != "ABSENT"}
    additions = {row[0] for row in rows if row[3] == "ABSENT"}
    require_additive(translated, current, additions)
    for path, sha, blob, before, before_sha, before_blob, _kind, _attribution in rows:
        if before != "ABSENT":
            old = checked_file(approved, before)
            if hashlib.sha256(old).hexdigest() != before_sha or v3.git_blob(old) != before_blob:
                raise ValueError("historical source succession drift: " + before)
        raw = checked_file(product, path)
        if (current[path] != ("100644", blob) or v3.git_blob(raw) != blob
                or hashlib.sha256(raw).hexdigest() != sha):
            raise ValueError("source-sealed current succession drift: " + path)


def prove(product: Path, approved: Path, before: Path, historical: Path) -> set[str]:
    product, approved, before, historical = (
        value.resolve(strict=True) for value in (product, approved, before, historical))
    if len({product, approved, before, historical}) != 4:
        raise ValueError("source roots must be distinct")
    sha = subprocess.check_output(
        ["git", "-C", str(approved), "rev-parse", "HEAD"], text=True).strip()
    if sha != APPROVED:
        raise ValueError("wrong real V3-approved predecessor commit")
    original = load_original_v3(approved)
    for root in (approved, before):
        if original.tracked(root) != head_sources(root):
            raise ValueError("historical source index/HEAD disagreement")
    original.prove_current(approved, before, approved / original.RECEIPT_REL)
    if v3.git_blob(checked_file(before, original.V2_REL)) != BEFORE_V2_BLOB:
        raise ValueError("historical V2 authority drift")
    original.replay_original(before, historical)
    for path, blob in ((V3_SCRIPT, CURRENT_V3_SCRIPT_BLOB),
                       (v3.RECEIPT_REL, CURRENT_V3_RECEIPT_BLOB)):
        if v3.git_blob(checked_file(product, path)) != blob:
            raise ValueError("preserved current V3 source/receipt changed")
    for path, expected in ATTRIBUTIONS.items():
        if hashlib.sha256(checked_file(product, path)).hexdigest() != expected:
            raise ValueError("existing source attribution drift: " + path)

    receipt = receipt_rows(product / RECEIPT)
    rows = succession_rows(product)
    prev, current = original.tracked(approved), v3.tracked(product)
    prove_successions(product, approved, rows, prev, current)

    methods = set()
    # Historical additive receipt pins remain intact. Current renamed source
    # identities are proved by the exact succession manifest above.
    for path, _historical_sha, _historical_blob, names in receipt:
        raw = checked_file(product, path)
        added = dedicated_methods(path, raw, names)
        if added & methods:
            raise ValueError("duplicate atom JUnit identity")
        methods.update(added)
    if len(prev) != 84 or len(current) != 90 or len(methods) != 6:
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
    print("M3_INTEROP_SOURCE_V4_PASS baseline=84 additions=6 current=90 v4_junit_atoms="
          + str(len(methods)) + " original_V3=EXECUTED_REAL_HEAD scope=SOURCE_ONLY")


# Preserve the existing V5 helper API, after authenticating its exact source.
v3 = load_current_v3(HERE.parents[4])


if __name__ == "__main__":
    main()
