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
"""Run existing feature and JNI witnesses against an unpatched built image.

This read-only verifier changes neither receiving sources nor the image. Its
receipts become inputs to the existing Maven snapshot/custody recipe.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", required=True, type=Path)
    parser.add_argument("--repository", required=True, type=Path)
    parser.add_argument("--image", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    root, repo, image, out = (p.resolve() for p in (
        args.workspace, args.repository, args.image, args.output))
    out.mkdir(parents=True, exist_ok=False)
    receipt = {
        "schema": "M3_FEATURE_UNPATCHED_IMAGE_EXECUTION_V1",
        "status": "RUNNING", "image": str(image), "repository": str(repo),
        "source_baseline": subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=repo, text=True).strip(),
        "patch_module_used": False, "commands": [],
        "scope": "Existing Runtime, ProgressJob, real Eclipse status and JNI witnesses; finite qualification, no JCK or phase promotion",
    }

    def save():
        (out / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")

    def run(label, command):
        command = [str(x) for x in command]
        start = time.time()
        proc = subprocess.run(command, cwd=repo, capture_output=True)
        stdout, stderr = out / (label + ".stdout.txt"), out / (label + ".stderr.txt")
        stdout.write_bytes(proc.stdout)
        stderr.write_bytes(proc.stderr)
        receipt["commands"].append({
            "label": label, "command": command, "exit": proc.returncode,
            "seconds": time.time() - start,
            "stdout_sha256": sha(proc.stdout), "stderr_sha256": sha(proc.stderr),
        })
        save()
        if proc.returncode:
            raise RuntimeError(label + " failed; exact output retained")
        return proc.stdout.decode("utf-8")

    java, javac = image / "bin/java", image / "bin/javac"
    receipt["image_files"] = {str(p.relative_to(image)): sha(p.read_bytes())
        for p in (java, javac, image / "release", image / "lib/libjava.so",
                  image / "lib/server/libjvm.so", image / "lib/modules")}
    run("java-version", [java, "-version"])
    run("javac-version", [javac, "-version"])
    donors = root / "runtime-refinement/progress-job/donors/eclipse-3.35.0"
    jars = [donors / "org.eclipse.equinox.common-3.21.0.jar",
            donors / "org.eclipse.osgi-3.24.300.jar"]
    known = ["2545ed27d408f19882695703097e5998bf25652bd0dac76a8e6d1391ec228ab7",
             "4a770aa4a78df653af871073c8b4ddd56884895f5fc71d10c40b4289ac7c8b9f"]
    if [sha(p.read_bytes()) for p in jars] != known:
        raise RuntimeError("Eclipse artifact drift")
    receipt["eclipse_jars"] = [{"name": p.name, "sha256": s} for p, s in zip(jars, known)]
    classes = out / "classes"
    classes.mkdir()
    sources = [
        repo / "test/jdk/java/lang/Runtime/M3RuntimeProgressFacade.java",
        repo / "test/jdk/java/util/concurrent/ProgressJob/M3ProgressJob.java",
        repo / "m3/runtime-integration/progress-job-20261018/src/test/java/EclipseStatusIdentity.java",
        repo / "test/jdk/jdk/internal/mindex/M3PrimitiveIdentityTest.java",
        repo / "test/jdk/jdk/internal/mindex/M3PrimitiveFixedArenaProbe.java",
    ]
    receipt["source_files"] = {str(p.relative_to(repo)): sha(p.read_bytes()) for p in sources}
    run("compile-existing-tests", [javac, "-g:none", "-proc:none", "-Xlint:all", "-Werror",
        "--add-exports", "java.base/jdk.internal.mindex=ALL-UNNAMED",
        "-cp", os.pathsep.join(str(p) for p in jars), "-d", classes, *sources])
    classpath = os.pathsep.join(str(p) for p in [classes, *jars])
    receipt["feature_outputs"] = {}
    for mode, flags in [("default", []), ("interpreter", ["-Xint"]),
                        ("c1", ["-XX:TieredStopAtLevel=1"]),
                        ("c2", ["-XX:-TieredCompilation", "-Xbatch"])]:
        for name in ("M3RuntimeProgressFacade", "M3ProgressJob", "EclipseStatusIdentity"):
            label = mode + "-" + name
            receipt["feature_outputs"][label] = run(label,
                [java, "-Xmx256m", "-ea", "-Xcheck:jni", *flags, "-cp", classpath, name])
    old_receipt = json.loads((root / "runtime-refinement/progress-job/proof/behavior-v2/receipt.json").read_text())
    frozen = root / "runtime-refinement/progress-job/proof/behavior-v2/frozen-stock-client"
    if {p.relative_to(frozen).as_posix(): sha(p.read_bytes())
            for p in frozen.rglob("*.class")} != old_receipt["frozen_stock_client_classes"]:
        raise RuntimeError("Frozen client drift")
    receipt["frozen_stock_client_sha256"] = old_receipt["frozen_stock_client_classes"]
    receipt["old_client_output"] = run("frozen-stock-callable-client",
        [java, "-Xmx256m", "-ea", "-Xcheck:jni", "-cp", frozen, "OldCallableClient"])
    fixture = root / "build-proof/native-image-execution/PrimitiveFixture.java"
    primitive = out / "canonical.m3prim"
    receipt["primitive_fixture_sha256"] = sha(fixture.read_bytes())
    run("compile-primitive-image", [java, "-Xcheck:jni", "-m",
        "jdk.compiler/com.sun.tools.javac.m3.M3PrimitiveImageCompiler", "--out", primitive,
        "--slots", out / "canonical.tsv", fixture])
    jni_flags = [java, "-Xmx256m", "-Xcheck:jni", "--add-exports",
                 "java.base/jdk.internal.mindex=ALL-UNNAMED", "-cp", classes]
    receipt["primitive_output"] = run("primitive-identity", [*jni_flags, "M3PrimitiveIdentityTest", primitive])
    receipt["native_outputs"] = [run("native-mapping-" + str(i),
        [*jni_flags, "M3PrimitiveFixedArenaProbe", primitive, image / "lib/libjava.so"])
        for i in range(2)]
    receipt["independent_processes"] = 2
    receipt["concurrent_processes"] = False
    receipt["collision_refusal"] = "Asserted by existing probe in each process"
    receipt["primitive_image_sha256"] = sha(primitive.read_bytes())
    receipt["status"] = "PASS"
    save()
    print(json.dumps({"status": "PASS", "commands": len(receipt["commands"]),
                      "receipt": str(out / "receipt.json")}))


if __name__ == "__main__":
    main()
