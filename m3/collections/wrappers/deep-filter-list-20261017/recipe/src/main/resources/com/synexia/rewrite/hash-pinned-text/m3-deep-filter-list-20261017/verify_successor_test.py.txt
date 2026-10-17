#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Finite acceptance, drift-refusal and no-write checks for the emitted successor."""
import argparse
import hashlib
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


TASK = "m3/collections/wrappers/deep-filter-list-20261017"
CURRENT = TASK + "/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-deep-filter-list-20261017"
HISTORY = "m3/collections/wrappers/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/"
CRATES = ("m3jdk21-filtered-mat-receiver-20261017", "m3jdk21-writable-map-avl-20261017")
LIST = "m3/collections/src/main/java/com/m3/collections/M3FilteredLists.java"
MAP = "m3/collections/src/main/java/com/m3/collections/M3FilteredMaps.java"
COLLECTION = "m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java"
COUNTED = "m3/collections/wrappers/index-counted-receiver-20261017/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-index-counted-20261017"
MULTISET = "m3/collections/src/main/java/com/m3/collections/M3FilteredSortedMultisets.java"


def inventory(root):
    return {
        str(path.relative_to(root)): (hashlib.sha256(path.read_bytes()).hexdigest(), path.stat().st_mtime_ns)
        for path in root.rglob("*") if path.is_file()
    }


def fixture(root, target):
    shutil.copytree(root / CURRENT, target / CURRENT)
    for crate in CRATES:
        shutil.copytree(root / HISTORY / crate, target / HISTORY / crate)
    shutil.copytree(root / COUNTED, target / COUNTED)
    helper = target / TASK / "verify_successor.py"
    helper.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(root / TASK / "verify_successor.py", helper)
    for line in (target / CURRENT / "manifest.tsv").read_text().splitlines():
        path, _, _, filename = line.split("\t")
        output = target / path
        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(target / CURRENT / filename, output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve(strict=True)
    cases = []
    with tempfile.TemporaryDirectory(prefix="m3-deep-filter-successor-proof-") as temporary:
        base = Path(temporary) / "base"
        fixture(root, base)

        def run(label, mutate, expected, edge="filtered-list"):
            target = Path(temporary) / label
            shutil.copytree(base, target)
            mutate(target)
            before = inventory(target)
            result = subprocess.run(
                [sys.executable, str(target / TASK / "verify_successor.py"),
                 "--root", str(target), "--edge", edge],
                text=True, capture_output=True, timeout=10)
            if (result.returncode == 0) != expected:
                raise AssertionError(label + "\n" + result.stdout + result.stderr)
            if expected and "M3_DEEP_FILTER_SUCCESSOR_PASS edge=" + edge not in result.stdout:
                raise AssertionError("missing acceptance receipt: " + label)
            if inventory(target) != before:
                raise AssertionError("validator wrote input: " + label)
            cases.append({"label": label, "accepted": expected})

        def alter(target, path):
            file = target / path
            file.write_bytes(file.read_bytes() + b"\n# drift\n")

        run("list-exact-successor", lambda target: None, True)
        run("map-exact-successor", lambda target: None, True, "writable-map")
        run("missing-list", lambda target: (target / LIST).unlink(), False)
        run("missing-map", lambda target: (target / MAP).unlink(), False, "writable-map")
        run("missing-companion", lambda target: (target / COLLECTION).unlink(), False)
        run("changed-list", lambda target: alter(target, LIST), False)
        run("changed-map", lambda target: alter(target, MAP), False, "writable-map")
        run("changed-companion", lambda target: alter(target, COLLECTION), False)
        run("changed-current-manifest", lambda target: alter(target, CURRENT + "/manifest.tsv"), False)
        run("changed-current-payload", lambda target: alter(target, CURRENT + "/M3FilteredLists.java.txt"), False)
        run("changed-current-preimage", lambda target: alter(target, CURRENT + "/pre/M3FilteredLists.java.txt"), False)
        run("changed-history-manifest",
            lambda target: alter(target, HISTORY + CRATES[0] + "/manifest.tsv"), False)
        run("changed-history-payload",
            lambda target: alter(target, HISTORY + CRATES[1] + "/M3FilteredMaps.java.txt"),
            False, "writable-map")
        run("historical-output-is-not-current",
            lambda target: shutil.copyfile(target / CURRENT / "pre/M3FilteredLists.java.txt", target / LIST),
            False)
        run("unknown-edge", lambda target: None, False, "unqualified-owner")
        proof = "m3/collections/wrappers/src/test/java/com/m3/collections/M3DeepFilteredListsProof.java"
        run("missing-proof-output", lambda target: (target / proof).unlink(), False)
        run("changed-proof-output", lambda target: alter(target, proof), False)
        run("sorted-multiset-exact-successor", lambda target: None, True, "sorted-multiset")
        run("missing-sorted-multiset", lambda target: (target / MULTISET).unlink(), False, "sorted-multiset")
        run("changed-sorted-multiset", lambda target: alter(target, MULTISET), False, "sorted-multiset")
        run("changed-counted-history-manifest", lambda target: alter(target, COUNTED + "/manifest.tsv"), False, "sorted-multiset")
        run("changed-counted-history-payload", lambda target: alter(target, COUNTED + "/M3FilteredSortedMultisets.java.txt"), False, "sorted-multiset")
        run("changed-counted-preimage", lambda target: alter(target, CURRENT + "/pre/M3FilteredSortedMultisets.java.txt"), False, "sorted-multiset")
        run("historical-counted-output-is-not-current",
            lambda target: shutil.copyfile(target / CURRENT / "pre/M3FilteredSortedMultisets.java.txt", target / MULTISET),
            False, "sorted-multiset")
        multiset_proof = "m3/collections/wrappers/src/test/java/com/m3/collections/M3DeepFilteredSortedMultisetsProof.java"
        run("missing-counted-proof-output", lambda target: (target / multiset_proof).unlink(), False, "sorted-multiset")
        run("changed-counted-proof-output", lambda target: alter(target, multiset_proof), False, "sorted-multiset")
    accepted = sum(case["accepted"] for case in cases)
    print("M3_DEEP_FILTER_SUCCESSOR_PROOF_PASS cases=" + str(len(cases))
          + " accepted=" + str(accepted) + " refused=" + str(len(cases) - accepted)
          + " no_write=" + str(len(cases)))


if __name__ == "__main__":
    main()
