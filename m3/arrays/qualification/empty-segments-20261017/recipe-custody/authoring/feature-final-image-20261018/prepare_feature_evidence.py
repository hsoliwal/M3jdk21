#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# SPDX-License-Identifier: Apache-2.0
"""Prepare immutable feature-image evidence inputs for the original Maven recipe.

No receiving repository, recipe engine, product source or recorded test result
is changed. The subsequent qualified recipe remains the only receiving path.
"""
import argparse
import hashlib
import json
from pathlib import Path


PREFIX = "synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/"
EVIDENCE = "evidence/feature-final-image-20261018/"
BUILT = "86528a90079b89a50b51ef13da08e7e70d468cb4"
RECEIVED = "07ba6e383343d32c83cfdaf641387bbe72bf325d"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--workspace", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    root, output = args.workspace.resolve(), args.output.resolve()
    proof = root / "build-proof"
    repository = root / "M3jdk21-feature-current"
    require(not output.exists(), "evidence input directory already exists")
    read = lambda relative: json.loads((proof / relative).read_text())
    configure, build = read("feature-configure.json"), read("feature-images.json")
    require(configure["exit_code"] == build["exit_code"] == 0, "clean image/configure failed")
    require(configure["baseline"] == build["source_baseline"] == BUILT, "actual built baseline drift")
    require("--enable-warnings-as-errors" in configure["command"], "warning gate was not enabled")
    for stem, receipt in (("feature-configure", configure), ("feature-images", build)):
        for name, suffix in (("log_sha256", ".log"), ("resources_sha256", ".resources.txt")):
            require(sha((proof / (stem + suffix)).read_bytes()) == receipt[name], "build evidence drift")
    for relative, expected in build["product_source_sha256"].items():
        require(sha((repository / relative).read_bytes()) == expected, "received feature source drift")
    identity = read("feature-source-identity-after-restoration.json")
    require(identity["status"] == "PASS" and not identity["differences"], "build source identity failed")
    require(identity["built_baseline"] == BUILT and identity["receiving_baseline"] == RECEIVED,
            "source identity belongs to another pair of baselines")
    require(identity["built_files"] == identity["receiving_files"] == 25341 and
            identity["built_inventory_sha256"] == identity["receiving_inventory_sha256"] and
            identity["byte_comparison_after_matching_hashes"], "source identity coverage changed")
    before = read("feature-source-identity-before-restoration.json")
    require(before["status"] == "DIFFERENT" and len(before["differences"]) == 14,
            "restore the separate original14-file regression observation")
    for name in ("feature-receiving", "feature-current-receiving", "feature-current-restoration"):
        receipt = read(name + "/receipt.json")
        exit_key = "exit" if name == "feature-current-restoration" else "exit_code"
        require(receipt["status"] == "PASS" and all(op[exit_key] == 0 for op in receipt["operations"]),
                "receiving operation was incomplete: " + name)
        for op in receipt["operations"]:
            log = proof / name / (op["mode"] + ".txt") if exit_key == "exit" else Path(op["log"])
            expected = op["output_sha256"] if exit_key == "exit" else op["sha256"]
            require(sha(log.read_bytes()) == expected, "receiving output drift: " + name)
    jtreg = read("feature-jtreg/receipt.json")
    require(jtreg["exit"] == 0 and jtreg["receiving_baseline"] == RECEIVED, "jtreg failed or wrong baseline")
    require(sha((proof / "feature-jtreg/jtreg.log").read_bytes()) == jtreg["log_sha256"], "jtreg log drift")
    summary = (proof / "feature-jtreg/report/text/summary.txt").read_text().splitlines()
    require(len(summary) == 9 and all("Passed. Execution successful" in s for s in summary),
            "nine successful jtreg tests are required")
    execution = read("feature-image-execution/receipt.json")
    require(execution["status"] == "PASS" and not execution["patch_module_used"] and
            execution["source_baseline"] == RECEIVED and len(execution["commands"]) == 20,
            "unpatched image witness is incomplete")
    for command in execution["commands"]:
        require(command["exit"] == 0 and not any("--patch-module" in x for x in command["command"]),
                "failed command or replacement-module execution")
        for stream in ("stdout", "stderr"):
            source = proof / "feature-image-execution" / (command["label"] + "." + stream + ".txt")
            require(sha(source.read_bytes()) == command[stream + "_sha256"], "command output drift")
    for mode in ("default", "interpreter", "c1", "c2"):
        for test, count in (("M3RuntimeProgressFacade", 84), ("M3ProgressJob", 81), ("EclipseStatusIdentity", 37)):
            require("checks=" + str(count) in execution["feature_outputs"][mode + "-" + test],
                    "expected executed feature check count missing")
    require("checks=131403" in execution["primitive_output"] and execution["independent_processes"] == 2 and
            execution["concurrent_processes"] is False and len(set(execution["native_outputs"])) == 1,
            "JNI witness scope changed")
    image = Path(execution["image"])
    for relative, expected in execution["image_files"].items():
        require(sha((image / relative).read_bytes()) == expected, "executed image binary drift")
    for relative, expected in execution["source_files"].items():
        require(sha((repository / relative).read_bytes()) == expected, "executed test source drift")
    output.mkdir(parents=True)
    rows = {}

    def add(source, relative, classification="GENERATED_TEST_EVIDENCE", **metadata):
        data = source.read_bytes()
        require(data.decode("utf-8").encode("utf-8") == data, "non-text evidence source")
        if source.suffix in (".log", ".jtr", ".patch"):
            relative += ".txt"
        path = PREFIX + relative
        require(path not in rows, "duplicate evidence destination: " + path)
        rows[path] = {"path": path, "source": str(source), "sha256": sha(data), "size_bytes": len(data),
                      "classification": classification, **metadata}

    for stem in ("feature-configure", "feature-images"):
        for suffix in (".json", ".log", ".resources.txt"):
            add(proof / (stem + suffix), EVIDENCE + "build/" + stem + suffix)
    for name in ("feature-source-identity-before-restoration.json", "feature-source-identity-after-restoration.json"):
        add(proof / name, EVIDENCE + "source-identity/" + name)
    for group in ("feature-receiving", "feature-current-receiving", "feature-current-restoration", "feature-image-execution"):
        for source in sorted((proof / group).iterdir()):
            if source.is_file() and source.suffix in (".json", ".txt", ".tsv"):
                add(source, EVIDENCE + group + "/" + source.name)
    for name in ("receipt.json", "jtreg.log", "resources.txt", "report/text/summary.txt"):
        add(proof / "feature-jtreg" / name, EVIDENCE + "jtreg/" + name)
    jtrs = sorted((proof / "feature-jtreg/work").rglob("*.jtr"))
    require(len(jtrs) == 9, "jtreg individual result count differs")
    for source in jtrs:
        add(source, EVIDENCE + "jtreg/results/" + source.name)
    witnesses = list((proof / "feature-jtreg/work/scratch").glob("*/m3-compatibility-hold-*"))
    require(len(witnesses) == 1, "exactly one executed Object compatibility witness is required")
    require("universal_no_break\tFAIL_COUNTEREXAMPLES_OBSERVED" in
            (witnesses[0] / "compatibility-receipt.tsv").read_text(), "known Object compatibility HOLD was lost")
    for source in sorted(witnesses[0].iterdir()):
        if source.is_file() and source.suffix in (".log", ".tsv"):
            add(source, EVIDENCE + "compatibility/" + source.name)
    qualification = {
        "schema": "M3_FEATURE_FINAL_IMAGE_QUALIFICATION_V1", "status": "PASS_WITH_EXPLICIT_COMPATIBILITY_HOLD",
        "actual_build_baseline": BUILT, "receiving_baseline": RECEIVED,
        "build_source_identity": identity, "image_build": build,
        "jtreg_tests": 9, "feature_checks_per_vm_mode": {"Runtime": 84, "ProgressJob": 81, "EclipseStatus": 37},
        "vm_modes": ["default", "interpreter", "c1", "c2"], "jni_checks": 131403,
        "independent_native_processes": 2, "concurrent_native_processes": False,
        "object_universal_compatibility": "HOLD: interface default dispatch and source compatibility counterexamples observed",
        "phase": "STRING_UNCHANGED", "hosted_ci_claim": False, "jck_claim": False,
        "whole_sdk_completion_claim": False, "image_files": execution["image_files"],
        "separately_built_m3_modules": "Earlier owning-module receipts remain separate; not rerun by this image witness",
        "runtime_recipe_entrypoint": "m3/runtime-integration/runtime-progress-facade-20261018/recipe-custody/README.md",
        "primitive_fixture_origin": "Existing primitive-address-identity verifier, SHA retained by execution receipt",
    }
    generated = output / "QUALIFICATION.json"
    generated.write_text(json.dumps(qualification, indent=2) + "\n")
    add(generated, EVIDENCE + generated.name)
    readme = output / "README.md"
    readme.write_text("""<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Clean feature image qualification

The Runtime facade and ProgressJob were built into a clean Linux x86_64 fastdebug
JDK image. The images target passed with warnings as errors and the existing
doclint gates, including HotSpot, JNI, all JDK modules, jmods, jlink and both CDS
archive variants. Configure/build commands, timestamps and complete logs remain
in `build/`; actual image hashes are in `QUALIFICATION.json` and the execution
receipt. No image binaries or replacement production modules are stored here.

## Exact source basis

The actual build started from commit 86528a90079b89a50b51ef13da08e7e70d468cb4 plus
the qualified Runtime and ProgressJob recipes. The receiving branch starts from
07ba6e383343d32c83cfdaf641387bbe72bf325d, receives the same feature recipes, and
restores 31 exact current-baseline targets through `m3jdk21-runtime-restore-07ba`.
The restoration preserves the newer catalogue row and does not reverse newer
collection changes. The two source trees match by path, kind, executable mode,
SHA-256 and direct file-byte comparison across all 25,341 files under `src`,
`make`, `configure` and `Makefile` (398,067,787 bytes). The before-restoration
14-file difference remains a distinct historical observation. This is a scoped
OpenJDK build-source identity proof, not an assertion that both commits or their
separately built m3 modules are identical.

## Executed results

Nine jtreg files passed on the new image: six Object tests (including the new
HOLD witness), the Runtime legacy client, the Runtime facade and ProgressJob.
The actual run uses `-Xcheck:jni` and an explicitly pinned stock Java 21 compiler
for legacy clients. The independent feature verifier uses no `--patch-module`:
84 Runtime checks, 81 ProgressJob checks and 37 real Eclipse status checks each
passed under default, interpreter, C1 and C2 modes. Frozen stock-compiled Callable
client classes were hash-verified and executed unchanged. Eclipse jar/source
and license provenance belongs to the accompanying ProgressJob donor record;
these receipts do not claim an Eclipse platform or Synexia runtime launch.

The original primitive compiler, identity test and fixed-arena probe were reused.
JNI passed 131,403 checks. Two independent sequential JVMs mapped the same image
at 0x600000000000; each probe also asserted same-process collision refusal. This
does not claim simultaneous-process stress or support for other native platforms.

The Object compatibility witness intentionally reports HOLD: a new Object
method can preempt an old interface default and reject previously valid source.
A passing witness confirms that these counterexamples remain visible. Runtime
and ProgressJob's finite legacy-client results do not remove that global hold.
The STRING phase remains unchanged. These local receipts do not assert hosted
CI success, JCK certification, automatic console/UI wiring or whole-SDK completion.

## Recipe replay

Use `m3/runtime-integration/runtime-progress-facade-20261018/recipe-custody/README.md`
for the qualified Runtime parser host and source-built OpenRewrite dependency.
That successor entrypoint supersedes the older predecessor-host reference in
the feature introduction. ProgressJob and the current 31-target restoration use
the original public host at
`m3/arrays/qualification/empty-segments-20261017/recipe-custody` with its checked-in
`sdk-owner`. Exact preimages, SDK bindings, compiler checks and fixed-point
receipts remain in their respective datasets. This evidence is received by a
separate Maven custody recipe; the evidence receiving recipe is not recursively
embedded in its own output. No target file is hand-patched after receiving.

The authoring helpers validate and inventory existing receipts outside receiving
trees. Reproduction requires the pinned source/test/donor inputs; replace the
transient workspace and toolchain paths in commands with local equivalents.
Source headers and donor notices retain their original rights. Generated
execution evidence does not relicense the source or third-party artifacts.
""")
    add(readme, EVIDENCE + readme.name, "FIRST_PARTY_EVIDENCE_DOCUMENTATION", source_license="Apache-2.0")
    for name in ("prepare_feature_evidence.py", "compare_build_sources.py", "verify_feature_image.py"):
        add(Path(__file__).resolve().parent / name, "authoring/feature-final-image-20261018/" + name,
            "FIRST_PARTY_PREPARATION_SOURCE", source_license="Apache-2.0")
    refusal = Path(__file__).resolve().parent / "preparation-refusal-v1.json"
    if refusal.is_file():
        add(refusal, EVIDENCE + "preparation-refusal-v1.json")
    inventory = output / "canonical-artifacts.json"
    inventory.write_text(json.dumps({"schema": "M3_CANONICAL_ARTIFACT_INVENTORY_V1",
                                    "files": [rows[p] for p in sorted(rows)]}, indent=2) + "\n")
    print(json.dumps({"status": "PASS", "files": len(rows), "bytes": sum(r["size_bytes"] for r in rows.values()),
                      "inventory": str(inventory), "sha256": sha(inventory.read_bytes())}))


if __name__ == "__main__":
    main()
