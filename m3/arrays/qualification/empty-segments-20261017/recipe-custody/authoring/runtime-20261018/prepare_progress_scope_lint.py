#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Seal method-local try warnings for intentionally borrowed progress scopes."""
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import sys

sys.dont_write_bytecode = True
WORK = Path(__file__).resolve().parents[2]
HERE = WORK / "runtime-refinement"
BASE = "4b24824c3aa09d802121447f8970d442a855f064"
OWNER = HERE / "canonical/synexia-openrewrite-recipes"
CRATE = OWNER / "recipe-crates/m3-collections-contract-refinement-20261017"
BASELINE = HERE / "scope-lint-baseline"
CANDIDATE = HERE / "candidates/progress-scope-lint"
MANIFESTS = (("m3jdk21-progress-doclint", "m3/compatibility/m3-progress-doclint-20261018.tsv", "7246c16a0e8ec742eba98dac97e24b7fc3947250ad5d2a22f72ac32dcddf0219"),
             ("m3jdk21-functional-doclint", "m3/compatibility/m3-functional-progress-doclint-20261018.tsv", "3b62981be6a617b903c559cc31904eca2bffb2b200380f1be57e07a8f634d969"))
SIGNATURES = {
"src/java.base/share/classes/java/util/concurrent/ProgressMonitors.java": ["    public static void run(Runnable task, ProgressMonitor monitor) {", "    public static <T> T call(Callable<T> task, ProgressMonitor monitor) throws Exception {"],
"src/java.base/share/classes/java/util/function/Consumer.java": ["    default void accept(T value, java.util.concurrent.ProgressMonitor monitor) {"],
"src/java.base/share/classes/java/util/function/Function.java": ["    default R apply(T value, java.util.concurrent.ProgressMonitor monitor) {"],
"src/java.base/share/classes/java/util/function/Supplier.java": ["    default T get(java.util.concurrent.ProgressMonitor monitor) {"],
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    dependencies = {}
    for dataset, manifest, expected in MANIFESTS:
        source_root = CRATE / "target" / dataset / "candidate"
        data = (source_root / manifest).read_bytes()
        if digest(data) != expected:
            raise ValueError("qualified documentation manifest drift")
        for raw in data.decode().splitlines():
            if raw and not raw.startswith("#"):
                path, _, after, _ = raw.split("\t")
                content = (source_root / path).read_bytes()
                if digest(content) != after or path in dependencies:
                    raise ValueError("documentation successor source drift")
                destination = BASELINE / path
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(content)
                dependencies[path] = after
    rows = []
    for path, signatures in sorted(SIGNATURES.items()):
        before = (BASELINE / path).read_text()
        after = before
        for signature in signatures:
            if after.count(signature) != 1:
                raise ValueError("borrowed-scope method drift: " + path)
            after = after.replace(signature, '    @SuppressWarnings("try")\n' + signature)
        if after.replace('    @SuppressWarnings("try")\n', '') != before:
            raise ValueError("non-annotation change")
        destination = CANDIDATE / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(after)
        rows.append((path, digest(before.encode()), digest(after.encode()), Path(path).name + ".txt"))
    manifest = CANDIDATE / "m3/compatibility/m3-progress-scope-lint-20261018.tsv"
    manifest.parent.mkdir(parents=True, exist_ok=True)
    manifest.write_text("# Method-local try-resource annotations after the exact progress/functional Javadoc successors.\n" +
                        "".join("\t".join(row) + "\n" for row in rows))
    spec = importlib.util.spec_from_file_location("scope_lint_prepare", CRATE / "prepare.py")
    prepare = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prepare)
    metadata = prepare.seal(CRATE, "m3jdk21-progress-scope-lint", "hsoliwal/M3jdk21", BASELINE,
                            [CANDIDATE], BASE, OWNER)
    receipt = {"schema": "M3_PROGRESS_SCOPE_LINT_CANDIDATE_V1", "baseline": BASE,
               "manifest_sha256": digest(manifest.read_bytes()), "sources": rows,
               "source_predecessors": [{"dataset": d, "manifest": m, "sha256": s} for d, m, s in MANIFESTS],
               "compiler_dependency_sources": dependencies, "receiving_source_written": False,
               "metadata": metadata, "scope": "Five SOURCE-retained method annotations in four existing owners; no bytecode change expected"}
    (HERE / "authoring/progress-scope-lint-candidate-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
