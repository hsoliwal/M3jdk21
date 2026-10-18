#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Seal Javadoc-only functional progress overloads after the eight-owner progress pass."""
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import sys

sys.dont_write_bytecode = True
WORK = Path(__file__).resolve().parents[2]
HERE = WORK / "runtime-refinement"
BASE = "4b24824c3aa09d802121447f8970d442a855f064"
BASELINE = WORK / "M3jdk21"
OWNER = HERE / "canonical/synexia-openrewrite-recipes"
CRATE = OWNER / "recipe-crates/m3-collections-contract-refinement-20261017"
CANDIDATE = HERE / "candidates/functional-doclint"
COMMENTS = {
"Consumer": """    /**
     * Performs this operation with a borrowed monitor scoped to the call.
     * The previous binding is restored on exit and the monitor is not completed.
     *
     * @param value the input argument
     * @param monitor the borrowed monitor
     * @throws NullPointerException if {@code monitor} is {@code null}
     * @throws java.util.concurrent.CancellationException if cancellation is
     *         observed before or after this operation
     */""",
"Function": """    /**
     * Applies this function with a borrowed monitor scoped to the call.
     * The previous binding is restored on exit and the monitor is not completed.
     *
     * @param value the input argument
     * @param monitor the borrowed monitor
     * @return the function result
     * @throws NullPointerException if {@code monitor} is {@code null}
     * @throws java.util.concurrent.CancellationException if cancellation is
     *         observed before or after this operation
     */""",
"Supplier": """    /**
     * Gets a result with a borrowed monitor scoped to the call.
     * The previous binding is restored on exit and the monitor is not completed.
     *
     * @param monitor the borrowed monitor
     * @return the supplied result
     * @throws NullPointerException if {@code monitor} is {@code null}
     * @throws java.util.concurrent.CancellationException if cancellation is
     *         observed before or after this operation
     */""",
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=BASELINE, text=True).strip() != BASE:
        raise ValueError("baseline drift")
    rows = []
    for name, comment in sorted(COMMENTS.items()):
        relative = "src/java.base/share/classes/java/util/function/" + name + ".java"
        before = (BASELINE / relative).read_text()
        needle = "    /** Execute with an explicit borrowed monitor, retaining the original SAM. */"
        if before.count(needle) != 1:
            raise ValueError("documentation anchor drift: " + name)
        after = before.replace(needle, comment)
        if re.sub(r"/\*\*.*?\*/", "", before, flags=re.S) != re.sub(r"/\*\*.*?\*/", "", after, flags=re.S):
            raise ValueError("non-documentation mutation: " + name)
        target = CANDIDATE / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(after)
        rows.append((relative, digest(before.encode()), digest(after.encode()), name + ".java.txt"))
    manifest = CANDIDATE / "m3/compatibility/m3-functional-progress-doclint-20261018.tsv"
    manifest.parent.mkdir(parents=True, exist_ok=True)
    manifest.write_text("# Exact functional progress Javadoc successor after M3jdk21 #1082.\n" +
                        "".join("\t".join(row) + "\n" for row in rows))
    spec = importlib.util.spec_from_file_location("functional_doclint_prepare", CRATE / "prepare.py")
    prepare = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prepare)
    metadata = prepare.seal(CRATE, "m3jdk21-functional-doclint", "hsoliwal/M3jdk21", BASELINE,
                            [CANDIDATE], BASE, OWNER)
    receipt = {"schema": "M3_FUNCTIONAL_DOCLINT_CANDIDATE_V1", "baseline": BASE,
               "manifest_sha256": digest(manifest.read_bytes()), "sources": rows,
               "receiving_source_written": False, "metadata": metadata,
               "scope": "Javadoc only; seven remaining functional progress warnings"}
    (HERE / "authoring/functional-doclint-candidate-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
