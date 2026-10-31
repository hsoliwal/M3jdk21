#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""One fresh, bounded execution of unchanged retained wrapper contract fixtures."""

from __future__ import annotations

import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys


HERE = Path(__file__).resolve().parent
C2 = HERE.parents[1]
B = C2.parent / "m3fs-vfs-20261020"
REPO = Path("/workspace/scratch/53bcd62c244c/M3JDK21")
TREE = "985d672bf250aa090b2399d626f0e3e1b541135d"
JDK = B / "toolchain/jdk21"
ENV = dict(os.environ, GIT_NO_LAZY_FETCH="1")
OWNER_NAMES = [
    "M3FilteredLists", "M3FilteredViews", "M3MutableFilteredViews", "M3SlotSpliterators"
]
DEPENDENCIES = [
    "org/junit/jupiter/junit-jupiter-api/5.10.2/junit-jupiter-api-5.10.2.jar",
    "org/junit/jupiter/junit-jupiter-engine/5.10.2/junit-jupiter-engine-5.10.2.jar",
    "org/junit/platform/junit-platform-commons/1.10.2/junit-platform-commons-1.10.2.jar",
    "org/junit/platform/junit-platform-engine/1.10.2/junit-platform-engine-1.10.2.jar",
    "org/junit/platform/junit-platform-launcher/1.10.2/junit-platform-launcher-1.10.2.jar",
    "org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar",
    "org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar",
]
STARTED = datetime.datetime.now(datetime.timezone.utc).isoformat()
COMMANDS: list[dict] = []
INPUTS: list[dict] = []


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def stamp(path: Path) -> dict:
    data = path.read_bytes()
    return {"path": str(path), "bytes": len(data), "sha256": sha(data)}


def remember(path: Path, role: str) -> dict:
    record = dict(stamp(path), role=role)
    INPUTS.append(record)
    return record


def git(*args: str) -> bytes:
    command = ["git", "-C", str(REPO), *args]
    completed = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                               env=ENV, check=False)
    if completed.returncode:
        raise RuntimeError(completed.stderr.decode(errors="replace"))
    return completed.stdout


def run(label: str, arguments: list[str]) -> dict:
    before = datetime.datetime.now(datetime.timezone.utc)
    completed = subprocess.run(arguments, cwd=HERE, env=ENV, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, check=False)
    output = HERE / (label + ".log")
    output.write_bytes(completed.stdout)
    record = {
        "label": label, "argv": arguments, "cwd": str(HERE),
        "started_at": before.isoformat(),
        "finished_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "exit_code": completed.returncode,
        "log": output.name, "log_sha256": sha(completed.stdout),
    }
    COMMANDS.append(record)
    write_json(HERE / "COMMANDS.json", COMMANDS)
    print(label + ": exit=" + str(completed.returncode), flush=True)
    return record


def must_pass(record: dict) -> None:
    if record["exit_code"] != 0:
        raise RuntimeError("Required command failed: " + record["label"])


def main() -> int:
    if (HERE / "RESULTS.json").exists() or (HERE / "classes").exists():
        raise RuntimeError("Refusing to overwrite an existing qualification run")
    inventory_path = C2 / "SOURCE_INVENTORY.json"
    inventory = json.loads(inventory_path.read_text())
    assert inventory["collections_tree"] == TREE
    remember(inventory_path, "parent source inventory")
    owners_path = C2 / "research/retained-owners.json"
    retained = json.loads(owners_path.read_text())
    remember(owners_path, "retained-owner pin")
    assert retained["collections_tree"] == TREE
    assert git("cat-file", "-t", TREE).strip() == b"tree"
    source_dir = HERE / "source/com/m3/collections"
    source_dir.mkdir(parents=True)
    sources: list[Path] = []
    owner_records = []
    for name in OWNER_NAMES:
        relative = "src/main/java/com/m3/collections/" + name + ".java"
        data = git("show", TREE + ":" + relative)
        blob = git("rev-parse", TREE + ":" + relative).decode().strip()
        assert git_blob(data) == blob
        destination = source_dir / (name + ".java")
        destination.write_bytes(data)
        sources.append(destination)
        record = dict(remember(destination, "unchanged production owner"),
                      tree=TREE, git_blob=blob, target_path="m3/collections/" + relative)
        owner_records.append(record)
        if name == "M3FilteredLists":
            pin = next(p for p in retained["owners"] if p["path"] == record["target_path"])
            assert record["sha256"] == pin["sha256"] and blob == pin["git_blob"]

    host = C2 / ("custody/synexia-openrewrite-recipes/recipe-crates/"
                 "m3-wrapper-convergence-20261018/recipe-host")
    fixture_records = []
    for pass_name, class_name in [
        ("wrapper-contract-v2", "M3WrapperConvergenceContract"),
        ("wrapper-junit-v2", "M3WrapperConvergenceTest"),
    ]:
        root = host / ("passes/" + pass_name + "/resources/com/synexia/rewrite/"
                       "hash-pinned-java/m3-wrapper-" + pass_name)
        original = root / (class_name + ".java.txt")
        manifest = root / "manifest.tsv"
        data = original.read_bytes()
        inventory_record = next(p for p in inventory["exact_custody_records"]
                                if p["local_path"] == str(original))
        assert sha(data) == inventory_record["sha256"]
        assert git_blob(data) == inventory_record["git_blob"]
        manifest_record = next(p for p in inventory["exact_custody_records"]
                               if p["local_path"] == str(manifest))
        assert sha(manifest.read_bytes()) == manifest_record["sha256"]
        rows = [line.split("\t") for line in manifest.read_text().splitlines()
                if line and not line.startswith("#")]
        assert len(rows) == 1 and rows[0][2] == sha(data) and rows[0][3] == original.name
        destination = source_dir / (class_name + ".java")
        destination.write_bytes(data)
        sources.append(destination)
        remember(original, "historical immutable contract fixture")
        remember(manifest, "historical fixture manifest")
        fixture_records.append(dict(
            remember(destination, "unchanged copied historical fixture"),
            original=str(original), manifest=str(manifest),
            git_blob=inventory_record["git_blob"],
            canonical_publication_commit=inventory["canonical_publication_commit"],
            manifest_target=rows[0][0],
            qualification_kind="historical fixture, executed freshly in this run",
        ))

    old_guard = host / ("passes/readonly-iterator/resources/com/synexia/rewrite/"
                       "hash-pinned-java/m3-wrapper-readonly-iterator/M3FilteredLists.java.txt")
    assert old_guard.read_bytes() == (source_dir / "M3FilteredLists.java").read_bytes()
    remember(old_guard, "historical accepted read-only guard postimage")

    launcher_original = B / "authored/facade/verification/FacadeTestLauncher.java"
    assert sha(launcher_original.read_bytes()) == (
        "69f7bf5cd343daeac5f3d14d919f8d3a12c4d870a6a7d3a05de00e879ba8c647")
    launcher = HERE / "source/FacadeTestLauncher.java"
    launcher.write_bytes(launcher_original.read_bytes())
    sources.append(launcher)
    remember(launcher_original, "existing unmodified generic JUnit launcher")
    remember(launcher, "copied unmodified generic JUnit launcher")
    remember(Path(__file__).resolve(), "qualification driver")

    jars = [B / "m2" / relative for relative in DEPENDENCIES]
    dependencies = [remember(path, "JUnit execution dependency") for path in jars]
    toolchain = [remember(JDK / relative, "Java 21 toolchain")
                 for relative in ["bin/java", "bin/javac", "bin/javap", "release"]]
    source_pin = {
        "target_repository": inventory["target_repository"],
        "target_commit_as_recorded_by_parent_inventory": inventory["target_commit"],
        "executed_collections_tree": TREE,
        "read_mechanism": "git show exact local immutable tree objects; working tree not copied",
        "dependency_closure": "Three contract-referenced public owners plus their M3SlotSpliterators dependency; all other dependencies are java.base.",
        "production_owners": owner_records,
        "historical_fixtures": fixture_records,
        "test_dependencies": dependencies,
        "toolchain": toolchain,
        "production_sources_modified": False,
        "fixture_sources_modified": False,
        "historical_guard_postimage_equals_current_owner": True,
    }
    write_json(HERE / "SOURCES.json", source_pin)
    (HERE / "SOURCE_SHA256.tsv").write_text(
        "role\tsha256\tbytes\tpath\n" + "".join(
            p["role"] + "\t" + p["sha256"] + "\t" + str(p["bytes"]) + "\t" + p["path"] + "\n"
            for p in INPUTS))
    must_pass(run("java-version", [str(JDK / "bin/java"), "-version"]))
    must_pass(run("javac-version", [str(JDK / "bin/javac"), "-version"]))
    classes = HERE / "classes"
    classes.mkdir()
    jar_path = os.pathsep.join(str(p) for p in jars)
    compile_record = run("strict-javac", [
        str(JDK / "bin/javac"), "--release", "21", "-Xlint:all", "-Werror",
        "-implicit:none", "-sourcepath", "", "-classpath", jar_path,
        "-d", str(classes), *[str(p) for p in sources],
    ])
    must_pass(compile_record)
    api_record = run("public-api", [
        str(JDK / "bin/javap"), "-public", "-s", "-classpath", str(classes),
        *["com.m3.collections." + name for name in OWNER_NAMES[:3]],
    ])
    must_pass(api_record)
    junit = run("junit-readonly-policy", [
        str(JDK / "bin/java"), "-ea", "-cp", str(classes) + os.pathsep + jar_path,
        "FacadeTestLauncher", "com.m3.collections.M3WrapperConvergenceTest",
    ])
    must_pass(junit)
    junit_text = (HERE / junit["log"]).read_text()
    junit_counts = {}
    for label in ["found", "skipped", "started", "aborted", "successful", "failed"]:
        match = re.search(r"\[\s*(\d+) tests " + label + r"\s*\]", junit_text)
        assert match, "Missing JUnit counter " + label
        junit_counts[label] = int(match.group(1))
    assert junit_counts == {"found": 1, "skipped": 0, "started": 1, "aborted": 0,
                           "successful": 1, "failed": 0}

    modes = ["readonly-policy", "readonly-effects", "readonly-linear", "readonly-failfast",
             "comparator-failure", "bounded-map", "bounded-set", "interior-null", "readonly"]
    known_failures = {
        "readonly-linear": "ordinary iterator predicate work must be linear, actual=",
        "readonly-failfast": "ordinary read-only iterator retains backing fail-fast detection: missing ConcurrentModificationException",
        "comparator-failure": "read-only map comparator failure: missing IllegalArgumentException",
        "interior-null": "interior-null floor skips hidden null: expected=-2 actual=null",
        "readonly": "read-only map comparator failure: missing IllegalArgumentException",
    }
    outcomes = []
    for mode in modes:
        # Exercise the original no-argument default for the final readonly mode.
        arguments = [str(JDK / "bin/java"), "-ea", "-cp", str(classes),
                     "com.m3.collections.M3WrapperConvergenceContract"]
        if mode != "readonly":
            arguments.append(mode)
        record = run("oracle-" + mode, arguments)
        text = (HERE / record["log"]).read_text()
        match = re.search(r"PASS M3WrapperConvergenceContract mode=" + re.escape(mode)
                          + r" checks=(\d+)", text)
        if record["exit_code"] == 0:
            assert match, "Successful exit without original contract PASS marker"
            outcome = dict(record, mode=mode, outcome="PASS", checks=int(match.group(1)))
        else:
            marker = known_failures.get(mode)
            assert marker and marker in text, "New or unexpected failure in " + mode
            outcome = dict(record, mode=mode, outcome="RETAINED_KNOWN_FAILURE",
                           first_failure=text.splitlines()[0],
                           original_oracle_not_weakened=True)
        outcomes.append(outcome)
    assert next(p for p in outcomes if p["mode"] == "readonly-policy")["outcome"] == "PASS"

    # Verify inputs and fresh compilation products without recompiling or rerunning tests.
    for record in INPUTS:
        assert sha(Path(record["path"]).read_bytes()) == record["sha256"], record["path"]
    for owner in owner_records:
        relative = owner["target_path"].removeprefix("m3/collections/")
        assert sha(git("show", TREE + ":" + relative)) == owner["sha256"]
    class_records = [stamp(path) for path in sorted(classes.rglob("*.class"))]
    write_json(HERE / "CLASSES.json", class_records)
    results = {
        "status": "RETAINED_READONLY_POLICY_PASS_WITH_PRESERVED_KNOWN_FAILURES",
        "started_at": STARTED,
        "finished_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "source_tree": TREE,
        "strict_compilation": dict(compile_record, flags="--release 21 -Xlint:all -Werror -implicit:none -sourcepath empty",
                                   production_owner_count=4, historical_fixture_count=2,
                                   existing_launcher_count=1, classfile_count=len(class_records)),
        "original_junit_gate": dict(junit, counts=junit_counts),
        "original_standalone_contract": outcomes,
        "standalone_counts": {"executed": len(outcomes),
                              "passed": sum(p["outcome"] == "PASS" for p in outcomes),
                              "retained_known_failures": sum(p["outcome"] == "RETAINED_KNOWN_FAILURE" for p in outcomes)},
        "public_api": api_record,
        "input_hashes_unchanged_after_run": True,
        "immutable_git_owner_hashes_rechecked_after_run": True,
        "original_fixture_bytes_and_default_mode_preserved": True,
        "production_changes": 0,
        "qualification_scope": "Fresh bounded Java 21 execution of historical exact fixtures against current four-owner source closure. No complete module, Maven, native, performance, or source-built JDK claim.",
        "known_findings": [
            "Comparator-owned IllegalArgumentException is swallowed in the bounded read-only map witness. The unchanged original mode stops at that first failure; its later mutable-set assertion is not reached.",
            "The ordinary read-only iterator retains the indexed callback path and exceeds the preserved linear-work oracle; there is no traversal speed claim.",
            "The ordinary read-only iterator does not detect the backing mutation in the retained fail-fast witness. Later assertions in that mode are not reached.",
            "Read-only navigable set navigation stops at hidden interior null in the retained lawful-comparator witness. Later assertions in that mode are not reached.",
        ],
        "sources_receipt": "SOURCES.json",
        "input_hashes": "SOURCE_SHA256.tsv",
        "commands": "COMMANDS.json",
        "fresh_classes": "CLASSES.json",
    }
    write_json(HERE / "RESULTS.json", results)
    print(json.dumps({"status": results["status"], "junit": junit_counts,
                      "standalone": results["standalone_counts"]}, sort_keys=True), flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
