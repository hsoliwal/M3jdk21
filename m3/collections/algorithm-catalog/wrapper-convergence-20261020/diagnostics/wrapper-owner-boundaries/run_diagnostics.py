#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Reproduce explicit defects and keep the frozen behavioral oracles RED.

Historical .java.txt snapshots become .java only in a fresh external output
directory. Nothing is written to the product or this diagnostic source tree.
An exit of zero means the listed observations were reproduced, not repaired.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile


SNAPSHOTS = {
    "M3FilteredLists.baseline.java.txt": "032bffd456e67c7568b028489ceec85f24510469b3a0e00571ab36cd17ae3b75",
    "M3FilteredLists.rejected.java.txt": "a8bfd9e5d38aa378e7dab73fdbc59ef1512ff2782670100712178bd3c7261c88",
    "M3WrapperConvergenceContract.baseline.java.txt": "2913aeae2f20b80c669398e5727ad9bc000eccf3bb225d85bdd672c17d4fb847",
}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def argfile_path(path):
    return '"' + str(path).replace("\\", "\\\\").replace('"', '\\"') + '"'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product-root", required=True, type=Path)
    parser.add_argument("--java-home", required=True, type=Path)
    parser.add_argument("--output", type=Path,
                        help="new or empty directory outside both product and catalogue trees; default: temporary directory")
    args = parser.parse_args()
    product = args.product_root.resolve()
    fixtures = Path(__file__).resolve().parent
    source_tree = next((parent for parent in fixtures.parents
                        if (parent / "m3/collections").is_dir()), fixtures)
    output = args.output.resolve() if args.output else Path(tempfile.mkdtemp(prefix="m3-wrapper-diagnostics-"))
    if output.is_relative_to(product) or output.is_relative_to(source_tree):
        parser.error("--output must be outside the product and catalogue trees; historical Java files are temporary")
    if output.exists() and any(output.iterdir()):
        parser.error("--output must be new or empty, to exclude stale class files")
    output.mkdir(parents=True, exist_ok=True)
    source_dir = product / "m3/collections/src/main/java"
    if not source_dir.is_dir():
        parser.error("--product-root does not contain m3/collections/src/main/java")
    java = str(args.java_home.resolve() / "bin/java")
    compiler = str(args.java_home.resolve() / "bin/javac")
    for name, expected in SNAPSHOTS.items():
        if digest(fixtures / name) != expected:
            parser.error("historical snapshot hash drift: " + name)

    package_path = Path("com/m3/collections")
    baseline_source = output / "baseline-src" / package_path
    rejected_source = output / "rejected-src" / package_path
    baseline_source.mkdir(parents=True)
    rejected_source.mkdir(parents=True)
    for version, destination in [("baseline", baseline_source), ("rejected", rejected_source)]:
        (destination / "M3FilteredLists.java").write_bytes(
            (fixtures / ("M3FilteredLists." + version + ".java.txt")).read_bytes())
    (baseline_source / "M3WrapperConvergenceContract.java").write_bytes(
        (fixtures / "M3WrapperConvergenceContract.baseline.java.txt").read_bytes())
    baseline_classes = output / "baseline-classes"
    rejected_classes = output / "rejected-classes"
    baseline_classes.mkdir()
    rejected_classes.mkdir()
    sources = [path for path in sorted(source_dir.rglob("*.java"))
               if path.name not in {"module-info.java", "M3FilteredLists.java"}]
    sources += sorted(baseline_source.glob("*.java"))
    sources += [fixtures / "src/test/java" / package_path / name for name in ["M3ComparatorBoundaryProof.java", "M3ReadonlyBoundaryProof.java",
                "M3CandidateListBoundaryProof.java", "M3ListMutationGuardCounterexamples.java"]]
    manifest = output / "javac-inputs.txt"
    manifest.write_text("\n".join(argfile_path(path) for path in sources) + "\n", encoding="utf-8")
    seals = {str(path): digest(path) for path in sources}
    seals.update({str(fixtures / name): digest(fixtures / name) for name in SNAPSHOTS})
    seals[str(rejected_source / "M3FilteredLists.java")] = digest(rejected_source / "M3FilteredLists.java")
    (output / "source-seals.json").write_text(json.dumps(seals, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    observations = []

    def execute(name, command, expected_exit, disposition, source_version, required_text=()):
        completed = subprocess.run(command, cwd=product, text=True, capture_output=True,
                                   check=False, timeout=180)
        combined = completed.stdout + completed.stderr
        log = output / (name + ".log")
        log.write_text(combined, encoding="utf-8")
        reproduced = completed.returncode == expected_exit and all(fragment in combined for fragment in required_text)
        observations.append({"name": name, "source_version": source_version,
                             "command": command, "actual_exit_code": completed.returncode,
                             "expected_diagnostic_exit_code_for_this_version": expected_exit,
                             "required_log_fragments": list(required_text),
                             "observation_reproduced": reproduced, "disposition": disposition,
                             "log": str(log), "log_sha256": digest(log)})
        print(f"{name}: actual_exit={completed.returncode} expected_for_{source_version}={expected_exit} "
              f"observation_reproduced={reproduced} disposition={disposition}", flush=True)
        return reproduced

    compiled = execute("compile-baseline", [compiler, "--release", "21", "-Xlint:all,-module", "-Werror",
                       "-d", str(baseline_classes), "@" + str(manifest)],
                       0, "DIAGNOSTIC_COMPILER_GATE", "baseline")
    rejected_compiled = compiled and execute("compile-rejected", [compiler, "--release", "21",
                       "-Xlint:all,-module", "-Werror", "-cp", str(baseline_classes),
                       "-d", str(rejected_classes), str(rejected_source / "M3FilteredLists.java")],
                       0, "DIAGNOSTIC_COMPILER_GATE", "rejected")
    if compiled:
        modes = [
            ("comparator-failure", 1, "PENDING_ORIGINAL_PRODUCT_CONTRACT", ["read-only map comparator failure: missing IllegalArgumentException"]),
            ("readonly-failfast", 1, "PENDING_ORIGINAL_PRODUCT_CONTRACT", ["ordinary read-only iterator retains backing fail-fast detection: missing ConcurrentModificationException"]),
            ("bounded-map", 0, "EXISTING_ORACLE_PRESERVED", ["PASS"]),
            ("bounded-set", 0, "EXISTING_ORACLE_PRESERVED", ["PASS"]),
            ("interior-null", 0, "EXISTING_ORACLE_PRESERVED", ["PASS"]),
            ("readonly-effects", 0, "EXISTING_ORACLE_PRESERVED", ["PASS"]),
        ]
        for version in ["baseline", "rejected"]:
            if version == "rejected" and not rejected_compiled:
                continue
            classpath = str(baseline_classes) if version == "baseline" else os.pathsep.join(
                [str(rejected_classes), str(baseline_classes)])
            command = [java, "-XX:-UsePerfData", "-ea", "-cp", classpath]
            for mode, expected, disposition, fragments in modes:
                execute(version + "-original-" + mode,
                        command + ["com.m3.collections.M3WrapperConvergenceContract", mode],
                        expected, disposition, version, fragments)
            for proof in ["M3ComparatorBoundaryProof", "M3ReadonlyBoundaryProof"]:
                execute(version + "-" + proof, command + ["com.m3.collections." + proof],
                        0, "POSITIVE_DEFECT_OBSERVATION_ONLY", version, ["DIAGNOSTIC OBSERVATIONS REPRODUCED"])
            execute(version + "-M3ListMutationGuardCounterexamples",
                    command + ["com.m3.collections.M3ListMutationGuardCounterexamples", version],
                    0, "POSITIVE_DEFECT_OBSERVATION_ONLY", version, ["DIAGNOSTIC OBSERVATIONS REPRODUCED"])
            for mode in ["transport", "empty-add", "exhausted-add", "exhausted-set", "exhausted-remove"]:
                expected = 0 if version == "baseline" and mode == "transport" else 1
                fragments = ["WRITE-READ-BOUNDARY"] if mode == "transport" else [
                    "filtered mutation must preserve the original subList iterator's generation check"]
                if version == "rejected" and mode == "transport":
                    fragments += ["same caller marker tape=[get:2] stored=2",
                                  "writing delivered occurrence must not load unrelated next element"]
                execute(version + "-required-list-" + mode,
                        command + ["com.m3.collections.M3CandidateListBoundaryProof", mode], expected,
                        "BASELINE_VALID_BEHAVIOR" if expected == 0 else "REQUIRED_BEHAVIOR_ORACLE_REMAINS_RED",
                        version, fragments)

    revision = subprocess.run(["git", "rev-parse", "HEAD"], cwd=product, text=True,
                              capture_output=True, check=True).stdout.strip()
    version = subprocess.run([java, "-version"], text=True, capture_output=True, check=True)
    summary = {"actual_dependency_checkout_revision": revision,
               "frozen_owner_revision": "2779aba3f98f31c324be43f731be7914e1a05935",
               "scope": "isolated diagnostic replay; original failures stay RED; no product acceptance or integration",
               "java_version": version.stdout + version.stderr, "snapshot_sha256": SNAPSHOTS,
               "baseline_compile_source_count": len(sources), "observations": observations,
               "all_documented_observations_reproduced": bool(rejected_compiled) and
                   all(row["observation_reproduced"] for row in observations),
               "pending_original_contracts": ["comparator-failure", "readonly-failfast"],
               "rejected_change": "next/previous raw-cursor mutation guard",
               "pending_list_contracts": ["empty-sublist-add", "exhausted-sublist-add",
                                          "exhausted-sublist-set", "exhausted-sublist-remove"]}
    receipt = output / "observations.json"
    receipt.write_text(json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("Diagnostic receipt: " + str(receipt))
    print("A reproduced defect is not an accepted implementation.")
    return 0 if summary["all_documented_observations_reproduced"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
