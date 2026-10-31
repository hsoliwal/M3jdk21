#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Execute the frozen four-file mapping recipe after actual source publication.

Each run creates an exclusive workspace without generated class files. Exact
beforeimages, immutable historical oracles, the unchanged text owner, and the
source-bound Java tests form its finite input seal. GitHub publication is external.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import traceback
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
AUDIT = HERE.parents[1]
ROOT = AUDIT.parent
TEMPLATE_PACKET_SHA256 = "bdb03b20006830eb767595c9d3b38f222e5df44e1b66cc345a25c0f5ded0344d"
BINDER_SHA256 = "a0d3c129789ae96f014513d6671da7cb9b3e0c1f8b3e011d8a951b4591ebc432"
INSTALLER_TEST_SHA256 = "0399feddf23a4686589528e9cf61d49cc530f34f04e946e4d23cbb158bfb50ce"
POM_PATH = "m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/mapping-verification-v03/pom.xml"
TEST_CLASS = "com.m3.rewrite.backport.JccMergeMappingRecoveryV03Test"
CACHE_RECEIPTS = {
    "OFFLINE_DIAGNOSTIC_CACHE_V02.json": "034d226e546825818559e5517ce62bb00dd855f9ce59760fa607049388280da9",
    "RECEIVER_ASM93_ADDON.json": "c49c23fd5ec3fc02ad0c3332e16fe4b0f8c9ea4bfab296ad013c726bc226823d",
}


def digest(body):
    return hashlib.sha256(body).hexdigest()


def json_file(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def load_binder():
    path = HERE / "bind_mapping.py"
    assert digest(path.read_bytes()) == BINDER_SHA256
    spec = importlib.util.spec_from_file_location("frozen_mapping_binder", path)
    binder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(binder)
    return binder


def verify_cache(cache):
    """Verify the selected offline cache, including metadata and exact file set."""
    receipts, expected = {}, {}
    for name, identity in CACHE_RECEIPTS.items():
        body = (ROOT / "runtime-resolution/actual-acquisition" / name).read_bytes()
        assert digest(body) == identity, "cache origin receipt drift: " + name
        receipts[name] = body
        for row in json.loads(body)["files"]:
            relative = Path(row["relative"])
            assert not relative.is_absolute() and ".." not in relative.parts
            expected[row["relative"]] = row
    paths = {path.relative_to(cache).as_posix(): path for path in cache.rglob("*") if path.is_file()}
    assert set(paths) == set(expected), "offline cache file set differs from selected snapshot plus exact addition"
    for relative, path in paths.items():
        assert not path.is_symlink(), "offline cache symlink: " + relative
        body = path.read_bytes()
        assert len(body) == expected[relative]["bytes"] and digest(body) == expected[relative]["sha256"], "offline cache drift: " + relative
    return receipts, [{"path": path, "bytes": row["bytes"], "sha256": row["sha256"]}
                      for path, row in sorted(expected.items())]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--maven-cache", required=True, type=Path)
    args = parser.parse_args()
    source_input = args.source_input.resolve(strict=True)
    cache = args.maven_cache.resolve(strict=True)
    output = args.output.resolve()
    assert not output.exists(), "each execution requires a fresh output directory"
    binder = load_binder()
    template = HERE / "java-template"
    packet_file = template / "TEMPLATE_PACKET.json"
    assert digest(packet_file.read_bytes()) == TEMPLATE_PACKET_SHA256
    template_packet = json.loads(packet_file.read_bytes())
    assert template_packet["junit_methods_authored"] == 13 and template_packet["junit_methods_executed"] == 0
    for row in template_packet["files"]:
        body = (template / row["path"]).read_bytes()
        assert len(body) == row["bytes"] and digest(body) == row["sha256"], row["path"]
    assert digest((HERE / "verify_installer.py").read_bytes()) == INSTALLER_TEST_SHA256
    epoch_root = HERE / "input-epoch"
    epoch_file = epoch_root / "INPUT_EPOCH.json"
    assert digest(epoch_file.read_bytes()) == binder.INPUT_EPOCH_SHA256
    epoch = json.loads(epoch_file.read_bytes())
    current = {path: (epoch_root / "current" / path).read_bytes() for path in binder.PATHS}
    prior = {path: (AUDIT / "files/prior" / path).read_bytes() for path in binder.PATHS}
    source = json.loads(source_input.read_bytes())
    assert source_input.read_bytes() == binder.pretty(source), "source packet must use binder.pretty serialization for exact byte custody"
    # Validate actual publication and current receiving pins before creating outputs.
    binder.compose(current, prior, source)
    cache_receipts, cache_rows = verify_cache(cache)
    output.mkdir(parents=True)
    (output / "cache-proof").mkdir()
    for name, body in cache_receipts.items():
        (output / "cache-proof" / name).write_bytes(body)
    json_file(output / "cache-proof/VERIFIED_CACHE.json", {
        "schema": "m3-jcc-mapping-exact-offline-cache/1", "status": "EXACT_FILE_SET_AND_BYTES_VERIFIED",
        "cache": str(cache), "origin_receipts": CACHE_RECEIPTS, "files": cache_rows,
        "scope": "Finite diagnostic snapshot plus exact ASM 9.3 addition; source/native custody is separate."})
    started = datetime.now(timezone.utc).isoformat()
    status, commands, checks = "FAIL", [], []
    workspace = output / "repository"
    materialized = output / "materialized"
    seal_rows = []

    def command(label, argv, cwd, env=None):
        folder = output / label
        folder.mkdir(exist_ok=False)
        with (folder / "stdout.log").open("wb") as stdout, (folder / "stderr.log").open("wb") as stderr:
            result = subprocess.run(argv, cwd=cwd, env=env, stdout=stdout, stderr=stderr, check=False)
        entry = {"name": label, "command": argv, "cwd": str(cwd), "exit_code": result.returncode,
                 "stdout_sha256": digest((folder / "stdout.log").read_bytes()),
                 "stderr_sha256": digest((folder / "stderr.log").read_bytes())}
        commands.append(entry)
        json_file(folder / "receipt.json", entry)
        return result.returncode

    try:
        bound = output / "bound-inputs"
        assert command("01-source-binding", [sys.executable, "-B", str(HERE / "bind_mapping.py"),
            "--audit-root", str(AUDIT), "--source-input", str(source_input), "--output", str(bound)], HERE) == 0
        rendered = output / "rendered-tests"
        assert command("02-render-tests", [sys.executable, "-B", str(template / "render_template.py"),
            "--audit-root", str(AUDIT), "--source-input", str(source_input), "--mapping-resources", str(bound),
            "--output", str(rendered)], HERE) == 0
        rendered_receipt = json.loads((rendered / "RENDERED_TEMPLATE_RECEIPT.json").read_bytes())
        assert rendered_receipt["status"] == "ACTUAL_SOURCE_BOUND_TEST_INPUTS_NOT_EXECUTED"
        bodies = dict(current)
        for row in epoch["guards"]:
            body = (epoch_root / row["file"]).read_bytes()
            assert digest(body) == row["sha256"]
            assert row["path"] not in bodies
            bodies[row["path"]] = body
        prefix = binder.RESOURCE_ROOT + "/" + binder.CRATE
        for path in sorted((bound / "resources").iterdir()):
            assert path.is_file()
            bodies[prefix + "/" + path.name] = path.read_bytes()
        for row in rendered_receipt["files"]:
            body = (rendered / row["path"]).read_bytes()
            assert digest(body) == row["sha256"] and len(body) == row["bytes"]
            assert row["path"] not in bodies
            bodies[row["path"]] = body
        historical = binder.RESOURCE_ROOT + "/jcc-merge-recovery-20261006-history-01/"
        for name, expected in [("history-0014.txt", "eb42442866c73c38027b481a84c0473bea422e4628d0794fa31fc61d9f1a818e"),
                               ("history-0016.txt", "f8b27e3582eb494af2c8dd47225976f974be6edc2f6c26313975064d48739497")]:
            body = (AUDIT / "candidate/overlay" / (historical + name)).read_bytes()
            assert digest(body) == expected
            bodies[historical + name] = body
        task = binder.TASK_ROOT
        bodies[task + "/source-publication-input.json"] = (bound / "source-publication-input.json").read_bytes()
        bodies[task + "/mapping-input-epoch.json"] = (bound / "mapping-input-epoch.json").read_bytes()
        bodies[task + "/mapping-verification-v03/MAPPING_RESOURCES.json"] = (bound / "MAPPING_RESOURCES.json").read_bytes()
        bodies[task + "/mapping-verification-v03/verify_installer.py"] = (HERE / "verify_installer.py").read_bytes()
        assert not any("target" in Path(path).parts for path in bodies)
        workspace.mkdir()
        for path, body in sorted(bodies.items()):
            target = workspace / path
            target.parent.mkdir(parents=True, exist_ok=True)
            with target.open("xb") as stream:
                stream.write(body)
            seal_rows.append({"path": path, "bytes": len(body), "sha256": digest(body),
                              "git_blob": binder.git_object("blob", body)})
        json_file(output / "INPUT_SEAL.json", {"schema": "m3-jcc-mapping-execution-inputs/2", "files": seal_rows,
            "source_commit": source["commit"], "source_root_tree": source["root_tree"],
            "source_input_sha256": digest(source_input.read_bytes()), "receiving_input_epoch_sha256": binder.INPUT_EPOCH_SHA256,
            "template_packet_sha256": TEMPLATE_PACKET_SHA256, "generated_class_files_present_at_start": False})
        checks.append({"case": "fresh finite staged inputs", "files": len(seal_rows), "generated_classes": 0})
        jdk = ROOT / "toolchain/jdk-21+35"
        maven = ROOT / "toolchain/apache-maven-3.9.9/bin/mvn"
        env = dict(os.environ, JAVA_HOME=str(jdk), PATH=str(jdk / "bin") + os.pathsep + os.environ["PATH"])
        code = command("03-maven", [str(maven), "-o", "-B", "-f", str(workspace / POM_PATH),
            "-Dmaven.repo.local=" + str(cache), "-Dm3.mapping.materialized=" + str(materialized), "verify"], workspace, env)
        _, cache_after = verify_cache(cache)
        assert cache_after == cache_rows, "offline cache changed during execution"
        checks.append({"case": "exact offline cache unchanged", "files": len(cache_rows), "passed": True})
        unchanged = all(digest((workspace / row["path"]).read_bytes()) == row["sha256"] for row in seal_rows)
        checks.append({"case": "frozen input bodies unchanged after Maven", "passed": unchanged})
        reports = workspace / Path(POM_PATH).parent / "target/surefire-reports"
        if reports.is_dir():
            shutil.copytree(reports, output / "03-maven/surefire-reports")
        assert code == 0 and unchanged, "Maven or immutable-input verification failed"
        report = reports / ("TEST-" + TEST_CLASS + ".xml")
        suite = ET.parse(report).getroot()
        assert suite.tag == "testsuite"
        assert {name: int(suite.attrib[name]) for name in ["tests", "failures", "errors", "skipped"]} == {
            "tests": 13, "failures": 0, "errors": 0, "skipped": 0}
        assert len(suite.findall("testcase")) == 13
        assert {case.attrib["classname"] for case in suite.findall("testcase")} == {TEST_CLASS}
        checks.append({"case": "current actual-source mapping JUnit", "tests": 13, "failures": 0, "errors": 0, "skipped": 0})
        after_rows = []
        expected = json.loads((bound / "MAPPING_RESOURCES.json").read_bytes())["rows"]
        assert {str(path.relative_to(materialized)) for path in materialized.rglob("*") if path.is_file()} == set(binder.PATHS)
        for row in expected:
            body = (materialized / row["path"]).read_bytes()
            assert digest(body) == row["after_sha256"] and len(body) == row["after_bytes"]
            assert binder.git_object("blob", body) == row["after_git_blob"]
            after_rows.append({"path": row["path"], "bytes": len(body), "sha256": digest(body),
                               "git_blob": row["after_git_blob"], "origin": "actual OpenRewrite Result afterimage"})
        json_file(output / "MATERIALIZED_OUTPUTS.json", {"schema": "m3-jcc-materialized-mapping/2", "status": "PASS",
                  "source_commit": source["commit"], "destination_input": binder.DESTINATION, "files": after_rows})
        code = command("04-installer", [sys.executable, "-B", str(HERE / "verify_installer.py"),
            "--repository", str(workspace), "--materialized", str(materialized), "--output", str(output / "installer")], workspace)
        assert code == 0, "sealed installer verification failed"
        installer_receipt = json.loads((output / "installer/receipt.json").read_bytes())
        assert installer_receipt["status"] == "PASS" and len(installer_receipt["refusals"]) == 28
        checks.append({"case": "sealed installer agrees with actual recipe", "outputs": 4, "refusals": 28})
        status = "PASS"
    except BaseException:
        (output / "failure.txt").write_text(traceback.format_exc())
        raise
    finally:
        receipt = {"schema": "m3-jcc-current-mapping-execution/2", "status": status, "started": started,
                   "finished": datetime.now(timezone.utc).isoformat(), "commands": commands, "checks": checks,
                   "source_input": str(source_input), "source_input_sha256": digest(source_input.read_bytes()),
                   "source_commit": source["commit"], "source_root_tree": source["root_tree"],
                   "destination_input": binder.DESTINATION, "destination_root": binder.DESTINATION_ROOT,
                   "workspace": str(workspace), "materialized": str(materialized), "maven_cache": str(cache),
                   "cache_origin_sha256": "034d226e546825818559e5517ce62bb00dd855f9ce59760fa607049388280da9",
                   "asm93_addon_receipt_sha256": "c49c23fd5ec3fc02ad0c3332e16fe4b0f8c9ea4bfab296ad013c726bc226823d",
                   "verified_cache_sha256": digest((output / "cache-proof/VERIFIED_CACHE.json").read_bytes()),
                   "binder_sha256": BINDER_SHA256, "template_packet_sha256": TEMPLATE_PACKET_SHA256,
                   "scope": "Finite offline metadata recipe/JUnit/installer execution using exact artifact identities. Complete source/native/plugin custody, source export and JDK runtime gates remain separate.",
                   "source_export_admitted": False, "destination_gates_passed": False}
        json_file(output / "receipt.json", receipt)
    print(json.dumps({"status": status, "source_commit": source["commit"], "junit_methods": 13,
                      "materialized_outputs": 4, "installer_refusals": 28, "receipt": str(output / "receipt.json")}))


if __name__ == "__main__":
    main()
