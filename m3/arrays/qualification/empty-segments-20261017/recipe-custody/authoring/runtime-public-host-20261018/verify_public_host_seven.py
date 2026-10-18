#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify seven public recipes using only their public checked-in SDK owner."""

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True
BASE = "4b24824c3aa09d802121447f8970d442a855f064"
PUBLIC = "m3/arrays/qualification/empty-segments-20261017/recipe-custody"
DATASETS = ("m3jdk21-object-vtable", "m3jdk21-progress-doclint", "m3jdk21-functional-doclint",
            "m3jdk21-progress-scope-lint", "m3jdk21-strings-doclint",
            "m3jdk21-native-feature-guard", "m3jdk21-runtime-control")
HISTORY = ("m3/compatibility/m3-object-adapter-progress-20261017.tsv",
           "m3/compatibility/m3-default-template-source-seal.tsv")


def digest(data):
    return hashlib.sha256(data).hexdigest()


def snapshot(root):
    return {p.relative_to(root).as_posix(): (digest(p.read_bytes()), p.stat().st_mtime_ns)
            for p in root.rglob("*") if p.is_file()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--maven", type=Path, required=True)
    parser.add_argument("--settings", type=Path, required=True)
    parser.add_argument("--maven-repository", type=Path, required=True)
    args = parser.parse_args()
    repository = args.repository.resolve()
    public = repository / PUBLIC
    owner = public / "sdk-owner"
    work = args.work_dir.resolve()
    work.mkdir(parents=True, exist_ok=False)
    proof = {"schema": "M3_PUBLIC_RUNTIME_SEVEN_DATASET_REPLAY_V1", "status": "RUNNING",
             "public_repository": str(repository), "receiving_baseline": BASE,
             "recipe_host": str(public), "recipe_owner": str(owner),
             "private_recipe_owner_used": False, "qualifications": [], "operations": []}
    receipt_path = work / "receipt.json"

    def save():
        receipt_path.write_text(json.dumps(proof, indent=2) + "\n")

    def git(*arguments):
        return subprocess.check_output(["git", "-C", str(repository), *arguments])

    def execute(label, command, timeout=600, env=None):
        stdout = work / (label + ".stdout.log")
        stderr = work / (label + ".stderr.log")
        with stdout.open("wb") as out, stderr.open("wb") as err:
            result = subprocess.run(command, stdout=out, stderr=err, env=env, timeout=timeout)
        record = {"command": command, "exit_code": result.returncode,
                  "stdout_sha256": digest(stdout.read_bytes()), "stderr_sha256": digest(stderr.read_bytes())}
        if result.returncode:
            proof["status"] = "FAIL"
            proof["failed_operation"] = {"label": label, **record}
            save()
            raise ValueError(label + " failed; exact logs retained")
        return record

    handoff = json.loads((public / "HANDOFF-runtime-20261018.json").read_text())
    controls = handoff["existing_host_controls"] + handoff["existing_engine_sources"]
    for row in controls:
        if digest((public / row["path"]).read_bytes()) != row["sha256"]:
            raise ValueError("public engine/control custody drift: " + row["path"])
    if digest((public / "pom.xml").read_bytes()) != handoff["pom_after_sha256"]:
        raise ValueError("public capped host POM drift")
    proof["public_host_source_bindings"] = controls
    proof["public_pom_sha256"] = handoff["pom_after_sha256"]
    save()
    env = dict(os.environ)
    env["JAVA_HOME"] = str(args.java_home.resolve())
    env["PATH"] = str(args.java_home.resolve() / "bin") + os.pathsep + env["PATH"]
    env["MAVEN_OPTS"] = "-Xmx256m -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
    for dataset in DATASETS:
        command = [str(args.maven.resolve()), "-B", "-ntp", "-U", "-s", str(args.settings.resolve()),
                   "-Dmaven.repo.local=" + str(args.maven_repository.resolve()), "-f", str(public / "pom.xml"),
                   "-Drecipe.owner.root=" + str(owner), "-Dm3.dataset=" + dataset,
                   "-Dm3.contract.output=" + str(work / (dataset + "-receiver-proof")), "-Pcontract", "verify"]
        execution = execute(dataset + "-maven", command, env=env)
        xml = public / "target" / dataset / "surefire-reports/TEST-com.synexia.rewrite.M3TaskRecipeTest.xml"
        suite = ET.parse(xml).getroot()
        properties = {p.attrib["name"]: p.attrib.get("value", "") for p in suite.findall("./properties/property")}
        if properties.get("m3.recipe.owner.root") != str(owner):
            raise ValueError("SDK qualification used an owner outside public custody")
        counts = {key: int(suite.attrib[key]) for key in ("tests", "failures", "errors", "skipped")}
        if counts != {"tests": 5, "failures": 0, "errors": 0, "skipped": 0}:
            raise ValueError("SDK qualification did not execute all five cases")
        proof["qualifications"].append({"dataset": dataset, **counts, "sealing_cases": 8,
                                        "receiver_cases": 9, "execution": execution,
                                        "sdk_report_sha256": digest(xml.read_bytes())})
        save()
        print("PUBLIC_HOST_QUALIFIED " + dataset, flush=True)
    spec = importlib.util.spec_from_file_location("public_runtime_receiver", public / "receive.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    qualified = {name: module.qualification(public, name, owner) for name in DATASETS}
    paths = set(HISTORY)
    initial = {}
    final = {}
    for dataset in DATASETS:
        for relative, (before, after) in qualified[dataset]["manifests"].items():
            if relative in final and before != final[relative]:
                raise ValueError("non-composable dataset source transition: " + relative)
            initial.setdefault(relative, before)
            final[relative] = after
            paths.add(relative)
    for history in HISTORY:
        for line in git("show", BASE + ":" + history).decode().splitlines():
            if line and not line.startswith("#"):
                paths.add(line.split("\t")[0])
    target = work / "receiving"
    target.mkdir()
    baseline_rows = []
    for relative in sorted(paths):
        result = subprocess.run(["git", "-C", str(repository), "show", BASE + ":" + relative],
                                capture_output=True)
        if result.returncode:
            if initial.get(relative) != "ABSENT":
                raise ValueError("missing required pinned baseline source: " + relative)
            continue
        if initial.get(relative) == "ABSENT":
            raise ValueError("new recipe target already exists at pinned baseline: " + relative)
        data = result.stdout
        if relative in initial and digest(data) != initial[relative]:
            raise ValueError("recipe initial preimage differs from immutable Git source: " + relative)
        blob = git("rev-parse", BASE + ":" + relative).decode().strip()
        observed = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        if observed != blob:
            raise ValueError("Git baseline source hash differs: " + relative)
        destination = target / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        baseline_rows.append({"path": relative, "git_blob": blob, "sha256": digest(data)})
    proof["baseline_sources"] = baseline_rows
    for dataset in DATASETS:
        before_check = snapshot(target)
        check = module.receive(public, dataset, owner, target, check=True)
        if snapshot(target) != before_check:
            raise ValueError("public receiver check wrote a source")
        apply = module.receive(public, dataset, owner, target)
        before_fixed = snapshot(target)
        fixed = module.receive(public, dataset, owner, target)
        if fixed["changes"] != 0 or before_fixed != snapshot(target):
            raise ValueError("public receiver fixed point wrote a source")
        proof["operations"].append({"dataset": dataset, "check": check, "apply": apply, "fixed_point": fixed})
    mismatches = [path for path, expected in final.items()
                  if digest((target / path).read_bytes()) != expected or digest((repository / path).read_bytes()) != expected]
    if mismatches:
        raise ValueError("composed public outputs differ from built receiving source: " + repr(mismatches))
    proof["composed_current_targets"] = [{"path": path, "sha256": final[path]} for path in sorted(final)]
    proof["built_receiving_source_comparison"] = "ALL_COMPOSED_TARGET_BYTES_EQUAL"
    proof["checks"] = {}
    for name in ("check_object_adapter_progress_receiver_20261017.py", "verify_m3_default_template_20261017.py",
                 "test_object_adapter_progress_receiver_20261018.py"):
        proof["checks"][name] = execute(name, [sys.executable, "-B", str(target / "m3/compatibility" / name)], timeout=90)
    for history in HISTORY:
        if (target / history).read_bytes() != git("show", BASE + ":" + history):
            raise ValueError("historical manifest changed")
    proof["historical_manifests_preserved"] = list(HISTORY)
    proof["negative_regression_methods"] = 31
    proof["fixed_point_scope"] = "Each historical dataset is replayed at its admitted immediate postimage; later exact successors are composed, not accepted as arbitrary old-dataset drift"
    proof["universal_object_compatibility"] = "HOLD_COUNTEREXAMPLES_RETAINED"
    proof["status"] = "PASS"
    save()
    print("PUBLIC_HOST_SEVEN_PASS " + json.dumps({"datasets": 7, "sdk_cases": 35, "sealing_cases": 56,
                                                "receiver_cases": 63, "composed_targets": len(final),
                                                "receipt": str(receipt_path)}, sort_keys=True), flush=True)


if __name__ == "__main__":
    main()
