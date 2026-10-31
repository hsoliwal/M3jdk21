#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify public custody and fresh public-owner-only replay in an isolated pinned tree."""

import argparse
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import tarfile


PIN = "86528a90079b89a50b51ef13da08e7e70d468cb4"
HOST = "m3/arrays/qualification/empty-segments-20261017/recipe-custody"
TASK = "m3-collections-contract-refinement-20261017"
DATASET = "m3jdk21-progress-job"
CUSTODY = "m3jdk21-progress-job-custody"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("repository", "recipe-owner", "stock-jdk", "maven", "maven-settings", "maven-repository", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    public = output / "public-tree"
    public.mkdir()
    canonical_owner = args.recipe_owner.resolve()
    canonical_crate = canonical_owner / "recipe-crates" / TASK
    observations = []
    receipt = {"schema": "M3_PROGRESS_JOB_PUBLIC_CUSTODY_REPLAY_V1", "result": "RUNNING", "base": PIN,
               "outer_dataset": CUSTODY, "inner_dataset": DATASET, "commands": observations,
               "original_receiving_repository_modified": False, "full_image": "NOT_RUN",
               "phase": "STRING_UNCHANGED", "public_api": "PENDING", "Object_no_break": "HOLD_UNCHANGED"}

    def run(label, command, env=None, timeout=180):
        result = subprocess.run([str(value) for value in command], capture_output=True, text=True, env=env, timeout=timeout)
        stdout, stderr = output / (label + ".stdout.log"), output / (label + ".stderr.log")
        stdout.write_text(result.stdout)
        stderr.write_text(result.stderr)
        observations.append({"label": label, "command": [str(value) for value in command], "exit": result.returncode,
                             "stdout_sha256": sha(stdout), "stderr_sha256": sha(stderr)})
        if result.returncode != 0:
            raise ValueError("failed " + label + ": " + result.stdout + result.stderr)
        return result.stdout

    def receive(label, crate, dataset, owner, target, check=False):
        command = ["python", "-B", crate / "receive.py", "--dataset", dataset,
                   "--recipe-owner", owner, "--target", target]
        if check:
            command.append("--check")
        text = run(label, command)
        return json.loads(text.split("M3_COLLECTIONS_RECEIVER_PASS ", 1)[1])

    try:
        archive = subprocess.check_output(["git", "-C", str(args.repository), "archive", PIN, HOST])
        baseline_files = {}
        with tarfile.open(fileobj=io.BytesIO(archive), mode="r:") as bundle:
            for member in bundle:
                relative = PurePosixPath(member.name)
                if relative.is_absolute() or ".." in relative.parts or not (
                        str(relative).startswith("m3/") or (str(relative) == "m3" and member.isdir())):
                    raise ValueError("unsafe baseline archive path")
                target = public / str(relative)
                if member.isdir():
                    target.mkdir(parents=True, exist_ok=True)
                elif member.isfile():
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(bundle.extractfile(member).read())
                    target.chmod(member.mode & 0o777)
                    baseline_files[str(relative)] = sha(target)
                else:
                    raise ValueError("unexpected baseline archive member")
        receipt["baseline_public_host_files"] = baseline_files
        outer = [receive("custody-check", canonical_crate, CUSTODY, canonical_owner, public, True),
                 receive("custody-apply", canonical_crate, CUSTODY, canonical_owner, public),
                 receive("custody-fixed-point", canonical_crate, CUSTODY, canonical_owner, public)]
        if outer[0]["changes"] != outer[1]["changes"] or outer[2]["changes"] != 0:
            raise ValueError("custody fixed-point mismatch")
        receipt["custody_receiving"] = outer
        host = public / HOST
        public_owner = host / "sdk-owner"
        env = os.environ.copy()
        env["JAVA_HOME"] = str(args.stock_jdk.resolve())
        env["PATH"] = str(args.stock_jdk.resolve() / "bin") + os.pathsep + env.get("PATH", "")
        env["MAVEN_OPTS"] = "-Xmx256m -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
        command = [args.maven, "-B", "-ntp", "-o", "-s", args.maven_settings,
                   "-Dmaven.repo.local=" + str(args.maven_repository.resolve()), "-f", host / "pom.xml",
                   "-Drecipe.owner.root=" + str(public_owner), "-Dm3.dataset=" + DATASET,
                   "-Dm3.contract.output=" + str(output / "public-sdk-receiver-contract"), "-Pcontract", "verify"]
        text = run("public-host-maven", command, env=env, timeout=240)
        if "Tests run: 5, Failures: 0, Errors: 0, Skipped: 0" not in text or "M3_AST_BUILD_RECEIVER_CONTRACT_PASS cases=9 targets=10" not in text:
            raise ValueError("fresh public SDK/receiver success evidence missing")
        emitted = output / "public-inner-receiving"
        emitted.mkdir()
        inner = [receive("public-inner-check", host, DATASET, public_owner, emitted, True),
                 receive("public-inner-apply", host, DATASET, public_owner, emitted),
                 receive("public-inner-fixed-point", host, DATASET, public_owner, emitted)]
        if [value["changes"] for value in inner] != [10, 10, 0]:
            raise ValueError("public inner replay mismatch")
        receipt["public_inner_receiving"] = inner
        canonical_candidate = canonical_crate / "target" / DATASET / "candidate"
        products = []
        for source in sorted(emitted.rglob("*")):
            if source.is_file():
                relative = source.relative_to(emitted)
                if sha(source) != sha(canonical_candidate / relative):
                    raise ValueError("public receiving differs from behavior-qualified target: " + str(relative))
                products.append({"path": str(relative), "sha256": sha(source), "bytes": source.stat().st_size})
        receipt["public_sources_equal_to_qualified_behavior_inputs"] = products
        receipt["fresh_public_sdk_gates"] = {"sdk": 5, "sealer": 8, "receiver": 9}
        receipt["public_source_owner"] = str(public_owner)
        receipt["public_runtime_private_dependency"] = False
        receipt["behavior_evidence"] = "Exact ten-source equality to qualified v2 inputs; this run does not claim a fresh full-image build or new behavior run."
        for relative, expected in baseline_files.items():
            if sha(public / relative) != expected:
                raise ValueError("existing public baseline owner changed: " + relative)
        receipt["existing_public_baseline_files_unchanged"] = len(baseline_files)
        shutil.copytree(host / "target" / DATASET / "surefire-reports", output / "public-surefire-reports")
        for name in ("SDK_INPUTS.tsv", "SDK_RESULTS.tsv", "java.patch", "text.patch"):
            source = host / "target" / DATASET / "candidate" / name
            shutil.copyfile(source, output / ("public-" + name + (".txt" if name.endswith(".patch") else "")))
        receipt["result"] = "PASS"
    except Exception as error:
        receipt["result"] = "FAIL"
        receipt["failure"] = str(error)
        raise
    finally:
        (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS public custody", outer[1]["changes"], "; fresh sdk-owner-only 5/8/9; inner 10/10/0; baseline owners unchanged")


if __name__ == "__main__":
    main()
