#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Replay the existing FS136 scope against exact recorded receiving sources."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository-root", type=Path, required=True)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--dependency-cache", type=Path, required=True)
    parser.add_argument("--recorded-receipt", type=Path, default=Path(__file__).resolve().parent / "fresh/RESULTS.json")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    repository = args.repository_root.resolve(strict=True)
    module = repository / "m3/fs"
    jdk = args.jdk_home.resolve(strict=True)
    cache = args.dependency_cache.resolve(strict=True)
    recorded = json.loads(args.recorded_receipt.read_text())
    if recorded.get("status") != "PASS":
        raise RuntimeError("An exact recorded passing scope is required")
    if re.search(r'(?m)^JAVA_VERSION="21(?:\.|\+|\")', (jdk / "release").read_text()) is None:
        raise RuntimeError("An actual JDK21 is required")
    pins = dict(recorded["prior_qualified_source_pins"])
    pins.update(recorded["test_sources"])
    for name, expected in pins.items():
        if digest(repository / name) != expected:
            raise RuntimeError("Receiving source drift: " + name)
    prior_path = module / "verification/parallel-20261020/authored/parallel/verification/generated/RESULTS.json"
    if digest(prior_path) != recorded["prior_receipt_sha256"]:
        raise RuntimeError("Original harness/custody receipt drift")
    prior = json.loads(prior_path.read_text())
    sys.path.insert(0, str(module / "tools"))
    from vfs_pack_support import load_dependencies, selected_vfs_source
    dependencies = load_dependencies(module / "tools/vfs-dependencies.lock.json", cache, module)
    jars = []
    for row in recorded["dependencies"]:
        jar = cache / row["repository_path"]
        if digest(jar) != row["sha256"]:
            raise RuntimeError("Dependency SHA256 drift: " + row["repository_path"])
        jars.append(jar)
    out = args.out.absolute()
    if os.path.lexists(out):
        raise RuntimeError("Fresh output required: " + str(out))
    out.mkdir(parents=True)
    environment = dict(os.environ)
    for name in list(environment):
        if name in {"LD_LIBRARY_PATH", "LD_PRELOAD", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH"} or name.startswith("DYLD_"):
            environment.pop(name)
    environment["JAVA_HOME"] = str(jdk)
    environment["LC_ALL"] = "C.UTF-8"
    receipt = {"status": "RUNNING", "scope": recorded["scope"], "recorded_receipt_sha256": digest(args.recorded_receipt), "source_pins": pins, "jdk_release": (jdk / "release").read_text(), "locale": "C.UTF-8", "commands": []}
    def persist():
        (out / "RESULTS.json").write_text(json.dumps(receipt, indent=2) + "\n")
    def run(name, command):
        argv = list(map(str, command))
        started = time.monotonic()
        result = subprocess.run(argv, cwd=out, env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=120)
        log = out / (name + ".log")
        log.write_bytes(result.stdout)
        receipt["commands"].append({"name":name, "argv":argv, "exit_code":result.returncode, "elapsed_seconds":time.monotonic()-started, "log":log.name, "log_sha256":digest(log)})
        persist()
        print(name + ": exit=" + str(result.returncode), flush=True)
        if result.returncode:
            receipt["status"] = "FAIL"
            persist()
            raise RuntimeError("Command failed: " + str(log))
        return result.stdout.decode("utf-8", errors="replace")
    classes = out / "classes"
    proof = out / "test-classes"
    classes.mkdir()
    proof.mkdir()
    sources = sorted((module / "src/main/java").rglob("*.java"))
    vfs = [p for p in sources if selected_vfs_source(p.relative_to(module).as_posix())]
    owned = [p for p in sources if "/com/m3/fs/vfs/" not in str(p)]
    if {str(p.relative_to(repository)):digest(p) for p in vfs+owned} != recorded["production_sources"]:
        raise RuntimeError("Compiled production inventory differs from recorded scope")
    runtime_cp = os.pathsep.join(str(row["jar"]) for row in dependencies)
    run("java-version", [jdk / "bin/java", "-version"])
    run("provider-compile", [jdk / "bin/javac", "--release", "21", "-Xlint:all", "-implicit:none", "-sourcepath", "", "-cp", runtime_cp, "-d", classes, *vfs])
    run("m3fs-strict-compile", [jdk / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-implicit:none", "-sourcepath", "", "-cp", str(classes)+os.pathsep+runtime_cp, "-d", classes, *owned])
    harness_source = repository / prior["receiving_harness"]["receiving_relative_path"]
    if digest(harness_source) != prior["receiving_harness"]["sha256"]:
        raise RuntimeError("Existing JUnit harness drift")
    harness = out / "FacadeTestLauncher.java"
    shutil.copyfile(harness_source, harness)
    names = ["MFilesParallelTest", "MFilesBehaviorTest", "MFilesProviderTest", "MFileCaptureTest", "MFileStreamsTest"]
    tests = [module / ("src/test/java/com/m3/fs/" + name + ".java") for name in names]
    cp = os.pathsep.join(map(str, [classes, *jars, module / "src/main/resources"]))
    run("compile-tests", [jdk / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-implicit:none", "-sourcepath", "", "-cp", cp, "-d", proof, *tests, harness])
    result = run("junit", [jdk / "bin/java", "-Xcheck:jni", "-cp", str(proof)+os.pathsep+cp, "FacadeTestLauncher", *("com.m3.fs."+name for name in names)])
    counts = {m.group(2):int(m.group(1)) for m in re.finditer(r"\[\s*(\d+)\s+tests\s+(found|skipped|started|aborted|successful|failed)\s*\]", result)}
    if counts != {"found":136,"skipped":0,"started":136,"aborted":0,"successful":136,"failed":0}:
        raise RuntimeError("Unexpected test inventory/results: " + repr(counts))
    for name, expected in pins.items():
        if digest(repository / name) != expected:
            raise RuntimeError("Receiving source changed during replay: " + name)
    if {p.relative_to(classes) for p in classes.rglob("*.class")} & {p.relative_to(proof) for p in proof.rglob("*.class")}:
        raise RuntimeError("Proof classes overlap production")
    receipt.update(status="PASS", junit=counts, source_pins_unchanged=True, compiled_sources={"vfs":len(vfs),"m3fs":len(owned)}, full_jdk="NOT_RUN", maven_lifecycle="NOT_RUN", native_runtime="NOT_RUN", hosted_ci="NOT_RUN")
    persist()
    print(json.dumps({"status":"PASS","junit":counts,"receipt":str(out / "RESULTS.json")}), flush=True)

if __name__ == "__main__":
    main()
