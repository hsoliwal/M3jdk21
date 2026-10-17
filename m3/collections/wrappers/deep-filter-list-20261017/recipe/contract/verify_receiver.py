#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify the receiving adapter using actual SDK outputs and isolated source overlays."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import stat
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

CRATE_NAME = "m3-deep-filter-list-20261017"
REPORT = "target/surefire-reports/TEST-com.synexia.rewrite.M3TaskRecipeTest.xml"
OWNERS = ("M3EditScope.java", "M3HashPinnedJavaSnapshotRecipe.java",
          "M3HashPinnedTextSnapshotRecipe.java", "M3ScopeInference.java")
TARGET_COUNT = 13
SENTINEL = "unrelated/receiver-sentinel.txt"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def rows(data, width):
    result = [line.split("\t") for line in data.decode("utf-8").splitlines()
              if line and not line.startswith("#")]
    if not result or any(len(row) != width for row in result):
        raise AssertionError("malformed qualification ledger")
    if len({row[0] for row in result}) != len(result):
        raise AssertionError("duplicate qualification ledger paths")
    return result


def safe_path(root, relative):
    part = Path(relative)
    if not relative or part.is_absolute() or ".." in part.parts:
        raise AssertionError("unsafe qualification source path: " + relative)
    path = root / part
    cursor = path
    while cursor != root:
        if cursor.is_symlink():
            raise AssertionError("symlinked qualification source: " + relative)
        cursor = cursor.parent
    if not path.is_file():
        raise AssertionError("missing qualification source: " + relative)
    return path


def file_state(path):
    value = path.lstat()
    return {"sha256": sha(path.read_bytes()), "mode": stat.S_IMODE(value.st_mode),
            "mtime_ns": value.st_mtime_ns}


def tree_state(root):
    result = {}

    def visit(path):
        value = path.lstat()
        common = {"mode": stat.S_IMODE(value.st_mode), "mtime_ns": value.st_mtime_ns}
        relative = path.relative_to(root).as_posix()
        if stat.S_ISLNK(value.st_mode):
            result[relative] = dict(common, kind="symlink", link=os.readlink(path))
        elif stat.S_ISDIR(value.st_mode):
            result[relative] = dict(common, kind="directory")
            for child in sorted(path.iterdir()):
                visit(child)
        elif stat.S_ISREG(value.st_mode):
            result[relative] = dict(common, kind="file", sha256=sha(path.read_bytes()))
        else:
            raise AssertionError("unexpected file kind in isolated proof")
    visit(root)
    return result


def actual_qualification(crate, owner_override):
    """Read actual SDK custody evidence; candidate bytes never come from templates."""
    captured = {}

    def read(root, relative):
        path = safe_path(root, relative)
        data = path.read_bytes()
        captured[path] = file_state(path)
        return data

    report_bytes = read(crate, REPORT)
    report = ET.fromstring(report_bytes)
    totals = {key: int(report.attrib.get(key, "-1"))
              for key in ("tests", "failures", "errors", "skipped")}
    if totals != {"tests": 5, "failures": 0, "errors": 0, "skipped": 0}:
        raise AssertionError("all five actual SDK qualification tests are required")
    properties = {item.attrib["name"]: item.attrib.get("value", "")
                  for item in report.findall("./properties/property")}
    if properties.get("m3.crate.name") != CRATE_NAME:
        raise AssertionError("this receiver proof requires the named wrapper crate")
    owner_value = owner_override or properties.get("m3.recipe.owner.root")
    if not owner_value:
        raise AssertionError("SDK report does not identify the recipe source owner")
    owner = Path(owner_value).resolve(strict=True)
    input_bytes = read(crate, "target/candidate/SDK_INPUTS.tsv")
    inputs = dict(rows(input_bytes, 2))
    expected_inputs = {"crate/pom.xml", "crate/receive.py"}
    for directory in ("src", "contract"):
        expected_inputs.update("crate/" + path.relative_to(crate).as_posix()
                               for path in (crate / directory).rglob("*") if path.is_file())
    expected_inputs.update("owner/src/main/java/com/synexia/rewrite/" + name for name in OWNERS)
    if set(inputs) != expected_inputs:
        raise AssertionError("actual SDK source binding is incomplete or has extra rows")
    sources = {}
    for path, expected in inputs.items():
        domain, relative = path.split("/", 1)
        data = read(crate if domain == "crate" else owner, relative)
        if sha(data) != expected:
            raise AssertionError("stale actual SDK input binding: " + path)
        sources[path] = data

    expected_targets = {}
    preimages = {}
    for kind in ("java", "text"):
        directory = "src/main/resources/com/synexia/rewrite/hash-pinned-" + kind + "/" + CRATE_NAME
        manifest = sources["crate/" + directory + "/manifest.tsv"]
        lane = rows(manifest, 4)
        if len(lane) != {"java": 7, "text": 6}[kind]:
            raise AssertionError("wrapper receiver proof requires seven Java and six text targets")
        for path, before, after, resource in lane:
            if path in expected_targets:
                raise AssertionError("duplicate cross-lane target")
            if sha(sources["crate/" + directory + "/" + resource]) != after:
                raise AssertionError("qualified manifest postimage drift")
            expected_targets[path] = (before, after)
            if before != "ABSENT":
                data = sources["crate/" + directory + "/pre/" + resource]
                if sha(data) != before:
                    raise AssertionError("qualified manifest preimage drift")
                preimages[path] = data

    result_bytes = read(crate, "target/candidate/SDK_RESULTS.tsv")
    results = rows(result_bytes, 3)
    if len(results) != TARGET_COUNT or {row[0] for row in results} != set(expected_targets):
        raise AssertionError("actual SDK Results do not contain the thirteen qualified targets")
    emitted = {}
    for path, before, after in results:
        if (before, after) != expected_targets[path]:
            raise AssertionError("actual SDK Results have a different manifest state")
        data = read(crate, "target/candidate/" + path)
        if sha(data) != after:
            raise AssertionError("actual SDK Result drift before receiver test")
        emitted[path] = data
    return {"report": report_bytes, "inputs": input_bytes, "results": result_bytes,
            "sources": sources, "preimages": preimages, "emitted": emitted,
            "targets": expected_targets, "captured": captured, "owner": owner}


def execute(crate, output, owner_override):
    qualified = actual_qualification(crate, owner_override)
    output.mkdir(parents=True, exist_ok=False)
    evidence = output / "evidence"
    evidence.mkdir()
    (evidence / "SDK_INPUTS.tsv").write_bytes(qualified["inputs"])
    (evidence / "SDK_RESULTS.tsv").write_bytes(qualified["results"])
    (evidence / "SDK_TESTS.xml").write_bytes(qualified["report"])
    driver_hash = sha(Path(__file__).read_bytes())
    binding = dict(rows(qualified["inputs"], 2))
    cases = []
    status = "RUNNING"

    def receipt():
        data = {"schema": "M3_WRAPPER_RECEIVER_CONTRACT_V1", "status": status,
                "crate": str(crate), "recipe_owner": str(qualified["owner"]),
                "driver_sha256": driver_hash,
                "driver_in_sdk_source_binding": binding.get("crate/contract/verify_receiver.py") == driver_hash,
                "receiver_sha256": sha(qualified["sources"]["crate/receive.py"]),
                "sdk_input_binding_sha256": sha(qualified["inputs"]),
                "sdk_results_sha256": sha(qualified["results"]),
                "qualified_sdk_targets": {path: sha(data) for path, data in qualified["emitted"].items()},
                "real_target_files_written": False, "canonical_files_written": False, "cases": cases}
        (evidence / "RECEIVER_RECEIPT.json").write_text(json.dumps(data, indent=2) + "\n")
    receipt()

    def put(root, relative, data):
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        return path

    def preimage_target(root):
        root.mkdir(parents=True)
        for path, data in qualified["preimages"].items():
            created = put(root, path, data)
            if path.endswith("/verify.sh"):
                created.chmod(0o751)
            os.utime(created, ns=(1_600_000_000_000_000_000, 1_600_000_000_000_000_000))
        sentinel = put(root, SENTINEL, b"unrelated bytes must remain unchanged\n")
        os.utime(sentinel, ns=(1_600_000_000_000_000_001, 1_600_000_000_000_000_001))
        return root

    def fixture(root):
        receiving_crate = root / "crate"
        copied_owner = root / "owner"
        for path, data in qualified["sources"].items():
            domain, relative = path.split("/", 1)
            put(receiving_crate if domain == "crate" else copied_owner, relative, data)
        put(receiving_crate, REPORT, qualified["report"])
        put(receiving_crate, "target/candidate/SDK_INPUTS.tsv", qualified["inputs"])
        put(receiving_crate, "target/candidate/SDK_RESULTS.tsv", qualified["results"])
        for path, data in qualified["emitted"].items():
            put(receiving_crate, "target/candidate/" + path, data)
        return receiving_crate, copied_owner

    def invoke(label, fixture_crate, fixture_owner, target, *, check=False, changes=None, refuses=None):
        arguments = [sys.executable, str(fixture_crate / "receive.py"), "--crate", str(fixture_crate),
                     "--recipe-owner", str(fixture_owner), "--target", str(target)]
        if check:
            arguments.append("--check")
        env = dict(os.environ, PYTHONDONTWRITEBYTECODE="1")
        result = subprocess.run(arguments, capture_output=True, text=True, timeout=30, env=env)
        (evidence / (label + ".stdout.log")).write_text(result.stdout)
        (evidence / (label + ".stderr.log")).write_text(result.stderr)
        if refuses is not None:
            if result.returncode == 0 or refuses not in result.stderr:
                raise AssertionError(label + " did not refuse for the expected reason: "
                                     + result.stdout + result.stderr)
            if "M3_SDK_RESULTS_RECEIVER_PASS" in result.stdout:
                raise AssertionError("refused invocation reported success")
        else:
            expected = ("M3_SDK_RESULTS_RECEIVER_PASS mode=" + ("check" if check else "apply")
                        + " targets=" + str(TARGET_COUNT) + " changes=" + str(changes) + "\n")
            if result.returncode != 0 or result.stderr or result.stdout != expected:
                raise AssertionError(label + " failed: " + result.stdout + result.stderr)
        return {"case": label, "exit_code": result.returncode, "expected_refusal": refuses}

    try:
        with tempfile.TemporaryDirectory(prefix="receiver-cases-", dir=output) as temporary:
            root = Path(temporary)
            check_crate, check_owner = fixture(root / "check-apply")
            target = preimage_target(root / "check-apply/target")
            initial = tree_state(target)
            item = invoke("check-no-write", check_crate, check_owner, target, check=True, changes=TARGET_COUNT)
            assert tree_state(target) == initial, "--check wrote bytes, modes, paths, or mtimes"
            cases.append(dict(item, all_target_bytes_and_mtimes_unchanged=True))
            receipt()

            item = invoke("apply-all-thirteen", check_crate, check_owner, target, changes=TARGET_COUNT)
            for path, data in qualified["emitted"].items():
                actual = target / path
                assert actual.read_bytes() == data, "applied output mismatch: " + path
                expected_mode = initial[path]["mode"] if path in initial else 0o644
                assert stat.S_IMODE(actual.stat().st_mode) == expected_mode, "mode mismatch: " + path
            assert file_state(target / SENTINEL) == {
                key: initial[SENTINEL][key] for key in ("sha256", "mode", "mtime_ns")}
            cases.append(dict(item, exact_sdk_outputs=TARGET_COUNT, existing_modes_preserved=True,
                              unrelated_file_unchanged=True))
            receipt()

            applied = tree_state(target)
            item = invoke("second-apply-fixed-point", check_crate, check_owner, target, changes=0)
            assert tree_state(target) == applied, "fixed-point receiving changed a path or mtime"
            cases.append(dict(item, all_target_bytes_and_mtimes_unchanged=True))
            receipt()

            late_path = sorted(qualified["targets"])[-1]
            assert late_path in qualified["preimages"], "drift control should exercise an existing late target"
            drift_crate, drift_owner = fixture(root / "source-drift")
            drift_target = preimage_target(root / "source-drift/target")
            with (drift_target / late_path).open("ab") as stream:
                stream.write(b"\nreceiver source drift\n")
            initial = tree_state(drift_target)
            item = invoke("late-target-drift-refusal", drift_crate, drift_owner, drift_target,
                          refuses="target source drift: " + late_path)
            assert tree_state(drift_target) == initial, "a late target drift caused earlier writes"
            cases.append(dict(item, drift_target=late_path, all_target_bytes_and_mtimes_unchanged=True))
            receipt()

            stale_crate, stale_owner = fixture(root / "stale-inputs")
            stale_target = preimage_target(root / "stale-inputs/target")
            descriptor = "src/main/resources/META-INF/rewrite/" + CRATE_NAME + ".yml"
            with (stale_crate / descriptor).open("ab") as stream:
                stream.write(b"\n# changed after SDK qualification\n")
            initial = tree_state(stale_target)
            item = invoke("stale-sdk-inputs-refusal", stale_crate, stale_owner, stale_target,
                          refuses="recipe changed after SDK qualification: crate/" + descriptor)
            assert tree_state(stale_target) == initial, "stale source binding wrote targets"
            cases.append(dict(item, all_target_bytes_and_mtimes_unchanged=True))
            receipt()

            altered_crate, altered_owner = fixture(root / "altered-result")
            altered_target = preimage_target(root / "altered-result/target")
            with (altered_crate / "target/candidate" / late_path).open("ab") as stream:
                stream.write(b"\nreceiver emitted result drift\n")
            initial = tree_state(altered_target)
            item = invoke("altered-sdk-result-refusal", altered_crate, altered_owner, altered_target,
                          refuses="SDK result changed after qualification: " + late_path)
            assert tree_state(altered_target) == initial, "altered SDK result wrote targets"
            cases.append(dict(item, all_target_bytes_and_mtimes_unchanged=True))
            receipt()

            link_crate, link_owner = fixture(root / "symlink-parent")
            link_target = root / "symlink-parent/target"
            backing = preimage_target(link_target / "backing")
            (link_target / "m3").symlink_to(backing / "m3", target_is_directory=True)
            initial = tree_state(link_target)
            item = invoke("symlink-parent-refusal", link_crate, link_owner, link_target,
                          refuses="symlink target or parent is not admitted:")
            assert tree_state(link_target) == initial, "symlink refusal changed the backing graph"
            cases.append(dict(item, symlink_within_target_root=True,
                              all_target_bytes_and_mtimes_unchanged=True))
            receipt()

        if len(cases) != 7:
            raise AssertionError("incomplete seven-case receiver proof")
        for path, initial in qualified["captured"].items():
            if file_state(path) != initial:
                raise AssertionError("actual qualification input changed during isolated proof: " + str(path))
        status = "PASS"
        receipt()
        print("M3_WRAPPER_RECEIVER_CONTRACT_PASS cases=7 targets=13 actual_sdk_results=true")
    except BaseException:
        status = "FAIL"
        receipt()
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--crate", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path, help="Fresh directory for retained evidence")
    parser.add_argument("--recipe-owner", type=Path, help="Defaults to the actual SDK Surefire property")
    args = parser.parse_args()
    if sys.flags.optimize:
        parser.error("receiver contract requires Python assertions; optimized execution is refused")
    crate = args.crate.resolve(strict=True)
    output = args.output.resolve()
    if (output.exists() or output == crate or output in set(crate.parents)
            or output.is_relative_to(crate / "src") or output.is_relative_to(crate / "contract")
            or output.is_relative_to(crate / "target/candidate")):
        parser.error("--output must be a fresh proof directory outside recipe sources and SDK Results")
    execute(crate, output, args.recipe_owner)


if __name__ == "__main__":
    main()

