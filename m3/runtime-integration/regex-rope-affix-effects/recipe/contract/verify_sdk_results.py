#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify String affixes and prepared Effects using source-bound actual SDK Results.

The isolated source/JNI contract preserves explicit baseline holds. It is not a
matched M3JDK image, CUDA device qualification, or full String-stage acceptance.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True

CRATE = "m3-rope-affix-effects-20261017"
RECIPE = "com.synexia.m3jdk.RopeAffixPreparedEffects20261017"
TARGET_BASELINE = "3bbb4712fb20adba1405741db8457164b9f436f5"
RESOURCE = "src/main/resources/com/synexia/rewrite"
MANIFEST_HASHES = {
    "java": "d05609ee1f486274c7e17380fe977b51050a1a3e1c2e5cdebe4f5df019db4903",
    "text": "654f67f4ccfc86811d707c23d84514f08eb3dd4638e76df333816623c79b08cc",
}
MANIFEST_COUNTS = {"java": 6, "text": 9}
PARENT_INPUTS_HASH = "cfd27d7d001eda4da6053b07890ba1bfd7d3056790b325dea4e1a100c77cff5d"
PARENT_INPUT_COUNT = 77
SDK_REPORT = "target/surefire-reports/TEST-com.synexia.rewrite.M3TaskRecipeTest.xml"
SDK_TESTS = {
    "exactReplayFixedPointAndInputOrder", "mixedPreimagesAndPostimages",
    "driftMissingDuplicateAndWrongRepresentationRefuse", "postScanChangesRefuse",
    "qualifiedSdkResultsAreRetained",
}
HISTORICAL_COMMIT = "970a34350ebdf6a3f1c5ff5acc8445d0f3515f55"
HISTORICAL_SOURCE = "src/java.base/share/classes/java/lang/M3StringRegexDp.java"
HISTORICAL_SHA = "9a5d9511b996d780f26672e2f5dc6ccc0a2747fe33f0c5ef1a9c62629564a41e"
BASELINE_DIAGNOSTIC = (
    "M3_STRING_INVARIANT_FAIL: M3 adaptive BMH/KMP convergence missing: "
    "skip256[pattern.charAt(index) & 255] = length - 1 - index;"
)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def terminate_process_group(process):
    """Retire the private command session, including descendants of Python/shell gates."""
    signals = []
    try:
        os.killpg(process.pid, signal.SIGTERM)
        signals.append("SIGTERM")
    except ProcessLookupError:
        pass
    output = None
    try:
        output = process.communicate(timeout=5)
    except subprocess.TimeoutExpired:
        pass
    # A descendant may ignore TERM or close its inherited pipes. Retire any remaining
    # members even when communicate() has already reaped the direct child.
    try:
        os.killpg(process.pid, signal.SIGKILL)
        signals.append("SIGKILL")
    except ProcessLookupError:
        pass
    if output is None:
        output = process.communicate(timeout=5)
    return output[0], output[1], signals


def safe_file(root, relative, absent=False):
    rel = Path(relative)
    if rel.is_absolute() or not rel.parts or ".." in rel.parts:
        raise ValueError("invalid relative source path: " + relative)
    path = root / rel
    cursor = path
    while cursor != root:
        if cursor.is_symlink():
            raise ValueError("symlinked source input: " + relative)
        cursor = cursor.parent
    if absent and not path.exists():
        return None
    if not path.is_file():
        raise ValueError("missing or non-file source input: " + relative)
    return path.read_bytes()


def parent_inventory():
    data = Path(__file__).with_name("parent-inputs.tsv").read_bytes()
    if sha(data) != PARENT_INPUTS_HASH:
        raise ValueError("sealed parent input inventory drift")
    rows = [line.split("\t") for line in data.decode("utf-8").splitlines()]
    if len(rows) != PARENT_INPUT_COUNT or any(len(row) != 2 for row in rows):
        raise ValueError("incomplete parent input inventory")
    if [row[0] for row in rows] != sorted({row[0] for row in rows}):
        raise ValueError("unordered or duplicate parent input paths")
    return dict(rows), data


def sdk_input_binding(crate, report):
    """Require the current executable recipe, receiver, contract and four SDK owners."""
    data = safe_file(crate, "target/candidate/SDK_INPUTS.tsv")
    if not data.startswith(b"# Recipe inputs used by this SDK execution\n"):
        raise ValueError("missing source-bound SDK input receipt header")
    rows = [line.split("\t") for line in data.decode("utf-8").splitlines()
            if line and not line.startswith("#")]
    if any(len(row) != 2 for row in rows) or len({row[0] for row in rows}) != len(rows):
        raise ValueError("malformed or duplicate SDK input binding")
    properties = {node.attrib["name"]: node.attrib.get("value", "")
                  for node in report.findall("./properties/property")}
    if properties.get("m3.crate.name") != CRATE or properties.get("m3.recipe.name") != RECIPE:
        raise ValueError("SDK report identifies a different task recipe")
    if properties.get("java.specification.version") != "21":
        raise ValueError("SDK qualification did not execute on Java21")
    owner_value = properties.get("m3.recipe.owner.root")
    if not owner_value:
        raise ValueError("SDK report does not identify the source recipe owner")
    owner = Path(owner_value).resolve(strict=True)
    expected = {"crate/" + path: sha(safe_file(crate, path))
                for path in ("pom.xml", "receive.py")}
    for domain in ("src", "contract"):
        directory = crate / domain
        if not directory.is_dir() or directory.is_symlink():
            raise ValueError("missing or symlinked SDK input domain: " + domain)
        for source in sorted(directory.rglob("*")):
            if source.is_symlink():
                raise ValueError("symlinked SDK input: " + str(source))
            if source.is_file():
                relative = source.relative_to(crate).as_posix()
                expected["crate/" + relative] = sha(safe_file(crate, relative))
    for name in ("M3EditScope.java", "M3HashPinnedJavaSnapshotRecipe.java",
                 "M3HashPinnedTextSnapshotRecipe.java", "M3ScopeInference.java"):
        relative = "src/main/java/com/synexia/rewrite/" + name
        expected["owner/" + relative] = sha(safe_file(owner, relative))
    if dict(rows) != expected:
        raise ValueError("SDK inputs changed since qualification; rerun Maven tests")
    if safe_file(crate, "contract/verify_sdk_results.py") != Path(__file__).read_bytes():
        raise ValueError("executing contract is not the SDK-bound contract")
    return data


def historical_input(target):
    """Resolve the retained rolling proof's read-only Git baseline without copying a repo."""
    environment = {key: value for key, value in os.environ.items() if not key.startswith("GIT_")}
    environment.update({"GIT_OPTIONAL_LOCKS": "0", "GIT_NO_REPLACE_OBJECTS": "1",
                        "GIT_NO_LAZY_FETCH": "1"})
    command = ["git", "-C", str(target), "show", HISTORICAL_COMMIT + ":" + HISTORICAL_SOURCE]
    result = subprocess.run(command, env=environment, capture_output=True, timeout=30)
    if result.returncode != 0 or result.stderr or sha(result.stdout) != HISTORICAL_SHA:
        raise ValueError("retained rolling proof needs the sealed historical Git blob "
                         + HISTORICAL_COMMIT + ":" + HISTORICAL_SOURCE)
    git = subprocess.run(["git", "-C", str(target), "rev-parse", "--absolute-git-dir"],
                         env=environment, text=True, capture_output=True, timeout=10, check=True)
    if git.stderr:
        raise ValueError("cannot resolve clean read-only target Git metadata")
    return result.stdout, Path(git.stdout.strip()).resolve(strict=True)


def admit_inputs(target, crate):
    """Complete read-only admission before creating any output directory."""
    dependencies, inventory_bytes = parent_inventory()
    if safe_file(crate, "contract/parent-inputs.tsv") != inventory_bytes:
        raise ValueError("executing parent inventory is not the SDK-bound inventory")
    dependency_bytes = {}
    target_state = {}
    for path, expected in dependencies.items():
        data = safe_file(target, path, absent=True)
        actual = "ABSENT" if data is None else sha(data)
        if actual != expected:
            raise ValueError("required parent input drift: " + path)
        if data is not None:
            dependency_bytes[path] = data
        target_state[path] = actual

    outputs = {}
    preimages = {}
    receipt_rows = []
    for kind, manifest_hash in MANIFEST_HASHES.items():
        directory = RESOURCE + "/hash-pinned-" + kind + "/" + CRATE
        manifest = safe_file(crate, directory + "/manifest.tsv")
        if sha(manifest) != manifest_hash:
            raise ValueError("exact recipe manifest drift: " + kind)
        rows = [line.split("\t") for line in manifest.decode("utf-8").splitlines()]
        if len(rows) != MANIFEST_COUNTS[kind] or any(len(row) != 4 for row in rows):
            raise ValueError("incomplete recipe target inventory: " + kind)
        if [row[0] for row in rows] != sorted({row[0] for row in rows}):
            raise ValueError("duplicate or unordered recipe paths")
        for path, before, after, name in rows:
            payload = safe_file(crate, directory + "/" + name)
            if sha(payload) != after:
                raise ValueError("recipe postimage drift: " + path)
            if before != "ABSENT":
                original = safe_file(crate, directory + "/pre/" + name)
                if sha(original) != before:
                    raise ValueError("recipe preimage drift: " + path)
                preimages[path] = original
            present = safe_file(target, path, absent=True)
            actual = "ABSENT" if present is None else sha(present)
            if actual not in (before, after):
                raise ValueError("target is neither sealed preimage nor postimage: " + path)
            target_state[path] = actual
            emitted = safe_file(crate, "target/candidate/" + path)
            if emitted != payload:
                raise ValueError("actual SDK Result differs from sealed postimage: " + path)
            if path in outputs or path in dependencies:
                raise ValueError("duplicate cross-lane or parent target: " + path)
            outputs[path] = emitted
            receipt_rows.append(path + "\t" + before + "\t" + after + "\n")

    sdk_receipt = safe_file(crate, "target/candidate/SDK_RESULTS.tsv")
    expected = "# OpenRewrite SDK Results; no template-copy materialization\n" + "".join(receipt_rows)
    if sdk_receipt != expected.encode("utf-8"):
        raise ValueError("SDK Results receipt does not match exact recipe lanes")
    sdk_report = safe_file(crate, SDK_REPORT)
    report = ET.fromstring(sdk_report)
    totals = {name: int(report.attrib.get(name, "-1"))
              for name in ("tests", "failures", "errors", "skipped")}
    tests = report.findall("./testcase")
    if (totals != {"tests": 5, "failures": 0, "errors": 0, "skipped": 0}
            or len(tests) != 5 or {test.attrib.get("name") for test in tests} != SDK_TESTS
            or any(list(test.findall(tag)) for test in tests for tag in ("failure", "error", "skipped"))):
        raise ValueError("actual five-test SDK qualification is incomplete")
    binding = sdk_input_binding(crate, report)
    history, git_dir = historical_input(target)
    return {
        "dependencies": dependencies, "dependency_bytes": dependency_bytes,
        "inventory_bytes": inventory_bytes, "target_state": target_state,
        "outputs": outputs, "preimages": preimages, "sdk_receipt": sdk_receipt,
        "sdk_report": sdk_report, "sdk_input_binding": binding,
        "historical_bytes": history, "git_dir": git_dir,
    }


def execute(target, crate, output, java_home):
    admitted = admit_inputs(target, crate)
    if not sys.platform.startswith("linux"):
        raise ValueError("retained prepared JNI and allocation proof requires Linux")
    java = java_home / "bin"
    version = subprocess.run([str(java / "javac"), "-version"], text=True,
                             capture_output=True, timeout=10, check=True)
    if version.stderr or not re.match(r"javac 21(?:[.\s]|$)", version.stdout):
        raise ValueError("JAVA_HOME must select JDK21: " + version.stdout + version.stderr)
    output.mkdir(parents=True, exist_ok=False)
    overlay = output / "overlay"
    baseline = output / "baseline"
    evidence = output / "evidence"
    build = output / "build"
    evidence.mkdir()
    build.mkdir()
    environment = dict(os.environ)
    environment.update({"JAVA_HOME": str(java_home), "PYTHONDONTWRITEBYTECODE": "1",
                        "PATH": str(java) + os.pathsep + environment.get("PATH", "")})
    commands = []
    holds = []
    status = "RUNNING"

    def write_receipt():
        (evidence / "SDK_RUNTIME_RECEIPT.json").write_text(json.dumps({
            "schema": "M3_ROPE_AFFIX_EFFECTS_SDK_CONTRACT_V1", "status": status,
            "target": str(target), "crate": str(crate), "required_parent_baseline": TARGET_BASELINE,
            "java_home": str(java_home), "javac_version": version.stdout.strip(),
            "manifest_sha256": MANIFEST_HASHES, "parent_inputs_sha256": PARENT_INPUTS_HASH,
            "contract_script_sha256": sha(Path(__file__).read_bytes()),
            "sdk_input_binding_verified": True,
            "required_parent_files": len(admitted["dependency_bytes"]),
            "required_parent_absences": len(admitted["dependencies"]) - len(admitted["dependency_bytes"]),
            "historical_rolling_source": {"commit": HISTORICAL_COMMIT, "path": HISTORICAL_SOURCE,
                                           "sha256": HISTORICAL_SHA},
            "target_input_states": admitted["target_state"],
            "actual_sdk_generated_files": {path: sha(data) for path, data in admitted["outputs"].items()},
            "commands": commands, "baseline_holds": holds, "target_files_written": False,
            "scope": {"source_extracted_affixes": True, "stock_jdk21_jni": True,
                      "matched_m3jdk_image": False, "cuda_kernel_device": "NOT_RUN",
                      "product_throughput": "NOT_RUN", "full_string_stage_accepted": False},
        }, indent=2) + "\n", encoding="utf-8")

    def put(root, relative, data):
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    for path, data in admitted["dependency_bytes"].items():
        put(overlay, path, data)
        put(baseline, path, data)
    for path, data in admitted["preimages"].items():
        put(baseline, path, data)
    for path, data in admitted["outputs"].items():
        put(overlay, path, data)
    (evidence / "SDK_RESULTS.tsv").write_bytes(admitted["sdk_receipt"])
    (evidence / "SDK_TESTS.xml").write_bytes(admitted["sdk_report"])
    (evidence / "SDK_INPUTS.tsv").write_bytes(admitted["sdk_input_binding"])
    (evidence / "parent-inputs.tsv").write_bytes(admitted["inventory_bytes"])
    (evidence / "historical-rolling-before.java.txt").write_bytes(admitted["historical_bytes"])
    write_receipt()

    def run(label, arguments, marker=None, *, cwd=overlay, extra_env=None,
            expected_failure=None, unittest_progress=False):
        argv = [str(argument) for argument in arguments]
        env = dict(environment)
        if extra_env:
            env.update(extra_env)
        started = time.monotonic()
        process = subprocess.Popen(argv, cwd=cwd, env=env, text=True,
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   start_new_session=True)
        try:
            stdout, stderr = process.communicate(timeout=240)
        except subprocess.TimeoutExpired as failure:
            stdout, stderr, signals = terminate_process_group(process)
            for stream, value in (("stdout", stdout), ("stderr", stderr)):
                (evidence / (label + "." + stream + ".log")).write_text(value, encoding="utf-8")
            commands.append({"label": label, "command": argv, "exit_code": process.returncode,
                             "timeout_seconds": 240, "process_group": process.pid,
                             "termination_signals": signals, "expectation": "PASS", "accepted": False})
            write_receipt()
            raise RuntimeError("contract command timed out: " + label) from failure
        except BaseException:
            terminate_process_group(process)
            raise
        result = subprocess.CompletedProcess(argv, process.returncode, stdout, stderr)
        (evidence / (label + ".stdout.log")).write_text(result.stdout, encoding="utf-8")
        (evidence / (label + ".stderr.log")).write_text(result.stderr, encoding="utf-8")
        row = {"label": label, "command": argv, "exit_code": result.returncode,
               "seconds": round(time.monotonic() - started, 4), "expectation": "PASS"}
        commands.append(row)
        if expected_failure is not None:
            row["expectation"] = "EXACT_BASELINE_HOLD"
            accepted = (result.returncode == 1 and not result.stdout
                        and result.stderr.strip() == expected_failure)
        else:
            accepted_stderr = not result.stderr or (unittest_progress
                and re.search(r"\nRan 9 tests in [^\n]+\n\nOK\n$", result.stderr) is not None)
            accepted = result.returncode == 0 and accepted_stderr
            if marker and marker not in result.stdout:
                accepted = False
        if any(diagnostic in result.stdout + result.stderr
               for diagnostic in ("WARNING in native method", "runtime error:")):
            accepted = False
        row["accepted"] = accepted
        write_receipt()
        print(label + " exit=" + str(result.returncode) + " accepted=" + str(accepted).lower(),
              (result.stdout or result.stderr)[-1500:].strip(), flush=True)
        if not accepted:
            raise RuntimeError(label + " failed its exact contract:\n" + result.stdout + result.stderr)
        return result.stdout

    try:
        integration = overlay / "m3/runtime-integration"
        run("affix-complete-source-before-after", [
            sys.executable, integration / "regex-mandatory-affixes/run_source_extracted_java21.py",
            "--source-root", overlay, "--candidate-root", overlay, "--java-home", java_home,
        ], "M3_REGEX_AFFIX_EXTRACTED_JAVA_PASS")
        for label, path, marker in (
            ("retained-amalgamation-source-seal", "regex-plan-amalgamation/verify_receiver.py",
             "M3_REGEX_AMALGAMATION_RECEIVER_PASS"),
            ("retained-required-grams-source-seal", "regex-required-ngrams/verify.py",
             "M3_REQUIRED_NGRAMS_PRODUCT_SOURCE_PASS"),
            ("retained-rolling-source-seal", "regex-rolling-cursor/verify_exact_source.py",
             "M3_REGEX_ROLLING_RECEIVER_PASS"),
            ("retained-amalgamation-before-after", "regex-plan-amalgamation/source_extracted_java21.py",
             "M3_REGEX_AMALGAMATION_EXTRACTED_JAVA_PASS"),
            ("retained-row-precompute", "regex-row-precompute/verify.py",
             "M3JDK21_REGEX_ROW_RECEIVER_PASS"),
            ("retained-literal-matches", "check-m3string-literal-matches.py",
             "M3_STRING_LITERAL_MATCHES_SOURCE_PASS"),
            ("retained-regex-split", "check-m3string-regex-split-current.py",
             "M3_STRING_REGEX_SPLIT_CURRENT_SOURCE_PASS"),
            ("retained-codepoint-geometry", "check-m3string-codepoint-geometry.py",
             "M3_STRING_CODEPOINT_GEOMETRY_SOURCE_PASS"),
        ):
            run(label, [sys.executable, integration / path], marker)
        run("retained-rolling-before-after", [
            sys.executable, integration / "regex-rolling-cursor/run_source_extracted_java21.py",
        ], "M3_REGEX_ROLLING_EXACT_JAVA_PASS", extra_env={
            "GIT_DIR": str(admitted["git_dir"]), "GIT_WORK_TREE": str(overlay),
            "GIT_OPTIONAL_LOCKS": "0", "GIT_NO_REPLACE_OBJECTS": "1", "GIT_NO_LAZY_FETCH": "1",
        })
        run("retained-native-utf16-oracle", [
            "bash", integration / "regex-jni-oracle/run-host-proof.sh",
        ], "M3_STRING_REGEX_NATIVE_BRIDGE_PASS")
        run("retained-ledger-refusals", [
            sys.executable, integration / "test_string_completeness.py",
        ], unittest_progress=True)
        test_root = overlay / "test/jdk/java/lang/String"
        tests = sorted(path for path in test_root.glob("M3StringRegex*.java")
                       if path.name not in ("M3StringRegexPreparedBatchTest.java",
                                            "M3StringRegexReplacementTest.java"))
        tests.append(test_root / "M3StringHierarchicalRegexTest.java")
        if len(tests) != 12:
            raise AssertionError("retained regex jtreg compile inventory changed")
        run("strict-retained-regex-jtreg-compile", [
            java / "javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
            "-d", build / "retained-jtreg", *tests,
        ])
        run("prepared-effects-java-jni-contract", [
            "bash", crate / "contract/batch/qualify.sh", "--target", overlay,
            "--crate", crate, "--output", output / "prepared-effects",
        ], "PREPARED_REGEX_CPU_QUALIFICATION_PASS; CUDA_KERNEL_AND_DEVICE_GATE_NOT_RUN")
        for label, root in (("baseline-full-string-invariant", baseline),
                            ("candidate-full-string-invariant", overlay)):
            run(label, [sys.executable, root / "m3/runtime-integration/check-m3string-invariants.py"],
                cwd=root, expected_failure=BASELINE_DIAGNOSTIC)
        holds.append({"gate": "full-string-source-invariant", "status": "UNCHANGED_BASELINE_FAILURE",
                      "baseline_commit": TARGET_BASELINE, "diagnostic": BASELINE_DIAGNOSTIC,
                      "candidate_failure_identical": True, "full_string_stage_accepted": False})
        if len(commands) != 16:
            raise AssertionError("incomplete sixteen-command String contract")

        for path, data in admitted["outputs"].items():
            if safe_file(crate, "target/candidate/" + path) != data or safe_file(overlay, path) != data:
                raise AssertionError("SDK Result drift during contract: " + path)
        for path, data in admitted["dependency_bytes"].items():
            if safe_file(overlay, path) != data or safe_file(baseline, path) != data:
                raise AssertionError("retained dependency drift during contract: " + path)
        for path, expected in admitted["target_state"].items():
            data = safe_file(target, path, absent=True)
            if ("ABSENT" if data is None else sha(data)) != expected:
                raise AssertionError("target input changed during isolated proof: " + path)
        if (safe_file(crate, SDK_REPORT) != admitted["sdk_report"]
                or safe_file(crate, "target/candidate/SDK_RESULTS.tsv") != admitted["sdk_receipt"]
                or sdk_input_binding(crate, ET.fromstring(admitted["sdk_report"])) != admitted["sdk_input_binding"]
                or historical_input(target)[0] != admitted["historical_bytes"]):
            raise AssertionError("SDK qualification evidence changed during contract")
        status = "PASS_WITH_BASELINE_HOLD"
        write_receipt()
        shutil.rmtree(build)
        print("SDK_STRING_CONTRACT_COMPLETE commands=16 generated_files=15 "
              "baseline_hold=full-string-source-invariant matched_m3jdk=false cuda_device=NOT_RUN", flush=True)
    except BaseException:
        status = "FAIL"
        write_receipt()
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", type=Path, required=True,
                        help="M3JDK21 checkout at the sealed pre/post states, with retained rolling history")
    parser.add_argument("--crate", type=Path, required=True,
                        help="Canonical or mirrored recipe crate with source-bound actual SDK Results")
    parser.add_argument("--output", type=Path, required=True,
                        help="Fresh directory for isolated overlays, native proof and evidence")
    args = parser.parse_args()
    if not os.environ.get("JAVA_HOME"):
        parser.error("JAVA_HOME must point to a JDK21 installation")
    target = args.target.resolve(strict=True)
    crate = args.crate.resolve(strict=True)
    output = args.output.resolve()
    if (output.exists() or output == target or output == crate or output.is_relative_to(target)
            or output in (set(target.parents) | set(crate.parents))
            or any(output.is_relative_to(crate / path) for path in ("src", "contract", "target/candidate"))):
        parser.error("--output must be a fresh proof directory outside product and bound inputs")
    execute(target, crate, output, Path(os.environ["JAVA_HOME"]).resolve(strict=True))


if __name__ == "__main__":
    main()
