#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify custody and original public-owner-only restoration in an isolated pinned tree."""

import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

from prepare_public_recovery import HOST, INNER, OUTER, PIN, TASK, git_snapshot, sha


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
    receipt = {"schema": "M3_RUNTIME_RECOVERY_PUBLIC_CUSTODY_REPLAY_V1", "result": "RUNNING",
               "baseline": PIN, "outer_dataset": OUTER, "inner_dataset": INNER, "commands": observations,
               "parent_receiving_repository_modified": False, "full_image": "NOT_RUN_BY_THIS_DRIVER",
               "Object_no_break": "HOLD_UNCHANGED", "phase": "STRING_UNCHANGED"}

    def run(label, command, env=None, timeout=180):
        command = [str(value) for value in command]
        print("RUN " + label, flush=True)
        try:
            result = subprocess.run(command, capture_output=True, env=env, timeout=timeout)
            stdout, stderr, code = result.stdout, result.stderr, result.returncode
        except subprocess.TimeoutExpired as failure:
            stdout, stderr, code = failure.stdout or b"", failure.stderr or b"", "TIMEOUT"
        (output / (label + ".stdout.log")).write_bytes(stdout)
        (output / (label + ".stderr.log")).write_bytes(stderr)
        observations.append({"label": label, "command": command, "exit": code,
                             "stdout_sha256": sha(stdout), "stderr_sha256": sha(stderr)})
        if code != 0:
            raise ValueError("command failed: " + label)
        return stdout.decode("utf-8"), stderr.decode("utf-8")

    def receive(label, crate, dataset, owner, target, check=False):
        command = [sys.executable, "-B", crate / "receive.py", "--dataset", dataset,
                   "--recipe-owner", owner, "--target", target]
        if check:
            command.append("--check")
        stdout, _ = run(label, command)
        return json.loads(stdout.split("M3_COLLECTIONS_RECEIVER_PASS ", 1)[1])

    try:
        original = git_snapshot(args.repository.resolve(), PIN, [HOST], public)
        receipt["baseline_public_host_files"] = original
        outer = [receive("custody-check", canonical_crate, OUTER, canonical_owner, public, True),
                 receive("custody-apply", canonical_crate, OUTER, canonical_owner, public),
                 receive("custody-fixed-point", canonical_crate, OUTER, canonical_owner, public)]
        if outer[0]["changes"] != outer[1]["changes"] or outer[2]["changes"] != 0:
            raise ValueError("custody fixed-point mismatch")
        receipt["custody_receiving"] = outer
        host = public / HOST
        owner = host / "sdk-owner"
        dataset = host / "datasets" / INNER
        manifest = {}
        for path in sorted((dataset / "resources").rglob("manifest.tsv")):
            for line in path.read_text().splitlines():
                if not line or line.startswith("#"):
                    continue
                target, before, after, template = line.split("\t")
                if target in manifest:
                    raise ValueError("duplicate public inner target")
                manifest[target] = {"before_sha256": before, "after_sha256": after}
        if len(manifest) != 31:
            raise ValueError("public restoration target set changed")
        emitted = output / "public-inner-receiving"
        emitted.mkdir()
        preimages = git_snapshot(args.repository.resolve(), PIN, sorted(manifest), emitted)
        for target, value in manifest.items():
            observed = preimages.get(target, {}).get("sha256", "ABSENT")
            if observed != value["before_sha256"]:
                raise ValueError("public inner baseline is not the exact current source preimage")
        receipt["baseline_product_preimages"] = preimages
        env = os.environ.copy()
        env["JAVA_HOME"] = str(args.stock_jdk.resolve())
        env["PATH"] = str(args.stock_jdk.resolve() / "bin") + os.pathsep + env.get("PATH", "")
        env["MAVEN_OPTS"] = "-Xmx256m -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
        command = [args.maven, "-B", "-ntp", "-o", "-s", args.maven_settings,
                   "-Dmaven.repo.local=" + str(args.maven_repository.resolve()), "-f", host / "pom.xml",
                   "-Drecipe.owner.root=" + str(owner), "-Dm3.dataset=" + INNER,
                   "-Dm3.contract.output=" + str(output / "public-sdk-receiver-contract"), "-Pcontract", "verify"]
        stdout, stderr = run("public-host-maven", command, env=env, timeout=900)
        log = stdout + stderr
        if ("Tests run: 5, Failures: 0, Errors: 0, Skipped: 0" not in log or "Ran 8 tests" not in log
                or "M3_AST_BUILD_RECEIVER_CONTRACT_PASS cases=9 targets=31" not in log or "BUILD SUCCESS" not in log):
            raise ValueError("original public SDK/sealing/receiver gates missing")
        inner = [receive("public-inner-check", host, INNER, owner, emitted, True),
                 receive("public-inner-apply", host, INNER, owner, emitted),
                 receive("public-inner-fixed-point", host, INNER, owner, emitted)]
        if [entry["changes"] for entry in inner] != [31, 31, 0]:
            raise ValueError("public inner receiving effects differ")
        receipt["public_inner_receiving"] = inner
        canonical_candidate = canonical_crate / "target" / INNER / "candidate"
        public_candidate = host / "target" / INNER / "candidate"
        products = []
        for target, value in sorted(manifest.items()):
            data = (emitted / target).read_bytes()
            if (sha(data) != value["after_sha256"] or data != (canonical_candidate / target).read_bytes()
                    or data != (public_candidate / target).read_bytes()):
                raise ValueError("public postimage differs from the admitted canonical product")
            products.append({"path": target, "sha256": sha(data), "size_bytes": len(data)})
        for target, identity in original.items():
            source = public / target
            if sha(source.read_bytes()) != identity["sha256"] or source.stat().st_mode & 0o777 != identity["mode"]:
                raise ValueError("existing public host source or mode changed")
        for name in ("SDK_INPUTS.tsv", "SDK_RESULTS.tsv", "java.patch", "text.patch"):
            shutil.copyfile(public_candidate / name, output / ("public-" + name + (".txt" if name.endswith(".patch") else "")))
        shutil.copytree(host / "target" / INNER / "surefire-reports", output / "public-surefire-reports")
        receipt.update(result="PASS", fresh_public_sdk_gates={"sdk": 5, "sealer": 8, "receiver": 9},
                       public_source_owner=str(owner), private_owner_used_as_maven_source=False,
                       public_inner_postimages=products, existing_public_baseline_files_unchanged=len(original),
                       exact_current_product_preimages=len(preimages))
    except Exception as failure:
        receipt["result"] = "FAIL"
        receipt["failure"] = str(failure)
        raise
    finally:
        (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS public custody; original public-owner-only 5/8/9; inner31/31/0; pinned owners unchanged", flush=True)


if __name__ == "__main__":
    main()
