#!/usr/bin/env python3
"""Replay the existing five-class FS contract suite on exact staged source bytes."""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parent
RECEIVER = ROOT / "current-receiver"
MODULE = RECEIVER / "m3/fs"
JDK = ROOT.parent / "toolchain/jdk21"
OUT = ROOT / "fs-rerun"
sys.path.insert(0, str(MODULE / "tools"))
from vfs_pack_support import load_dependencies, selected_vfs_source

def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def main():
    if OUT.exists():
        raise RuntimeError("Fresh output required")
    OUT.mkdir()
    prior_path = MODULE / "verification/parallel-20261020/authored/parallel/verification/generated/RESULTS.json"
    prior = json.loads(prior_path.read_text())
    pins = prior["receiving_inputs"]
    for name, expected in pins.items():
        if digest(RECEIVER / name) != expected:
            raise RuntimeError("Prior qualified source drift: " + name)
    dependencies = load_dependencies(MODULE / "tools/vfs-dependencies.lock.json", ROOT / "dependencies", MODULE)
    all_dependencies = json.loads((ROOT / "rerun-dependencies.json").read_text())
    for row in all_dependencies:
        if digest(ROOT / "dependencies" / row["repository_path"]) != row["sha256"]:
            raise RuntimeError("Pinned test dependency drift")
    environment = dict(os.environ)
    removed = []
    for name in list(environment):
        if name in {"LD_LIBRARY_PATH", "LD_PRELOAD", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH"} or name.startswith("DYLD_"):
            environment.pop(name)
            removed.append(name)
    environment["JAVA_HOME"] = str(JDK)
    environment["LC_ALL"] = "C"
    receipt = {"status": "RUNNING", "target_base": "2779aba3f98f31c324be43f731be7914e1a05935", "prepared_base": "065e3626aac2de62d2f531aec3bed2fca533c9f9", "scope": "Existing FS five-class 136-test suite on exact prepared Java sources plus unchanged current owner closure; real JUnit; no full JDK/HotSpot or new native-runtime qualification", "prior_receipt_sha256": digest(prior_path), "prior_qualified_source_pins": pins, "dependencies": all_dependencies, "removed_environment_names": sorted(removed), "commands": []}
    def persist():
        (OUT / "RESULTS.json").write_text(json.dumps(receipt, indent=2) + "\n")
    def run(name, argv):
        argv = list(map(str, argv))
        start = time.monotonic()
        result = subprocess.run(argv, env=environment, cwd=OUT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=120)
        log = OUT / (name + ".log")
        log.write_bytes(result.stdout)
        receipt["commands"].append({"name": name, "argv": argv, "exit_code": result.returncode, "elapsed_seconds": time.monotonic()-start, "log": log.name, "log_sha256": digest(log)})
        persist()
        print(name + ": exit=" + str(result.returncode), flush=True)
        if result.returncode:
            receipt["status"] = "FAIL"
            persist()
            raise RuntimeError("Command failed; see " + str(log))
        return result.stdout.decode("utf-8", errors="replace")
    classes = OUT / "classes"
    proof = OUT / "test-classes"
    classes.mkdir()
    proof.mkdir()
    jars = [ROOT / "dependencies" / row["repository_path"] for row in all_dependencies]
    runtime_jars = [row["jar"] for row in dependencies]
    dependency_cp = os.pathsep.join(map(str, runtime_jars))
    sources = sorted((MODULE / "src/main/java").rglob("*.java"))
    vfs = [p for p in sources if selected_vfs_source(p.relative_to(MODULE).as_posix())]
    owned = [p for p in sources if "/com/m3/fs/vfs/" not in str(p)]
    receipt["production_sources"] = {str(p.relative_to(RECEIVER)): digest(p) for p in vfs + owned}
    receipt["compiled_sources"] = {"vfs": len(vfs), "m3fs": len(owned)}
    run("java-version", [JDK / "bin/java", "-version"])
    run("provider-compile", [JDK / "bin/javac", "--release", "21", "-Xlint:all", "-implicit:none", "-sourcepath", "", "-cp", dependency_cp, "-d", classes, *vfs])
    run("m3fs-strict-compile", [JDK / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-implicit:none", "-sourcepath", "", "-cp", str(classes)+os.pathsep+dependency_cp, "-d", classes, *owned])
    resources = MODULE / "src/main/resources"
    consumer_cp = os.pathsep.join(map(str, [classes, *runtime_jars, resources]))
    (OUT / "consumer-classpath.txt").write_text(consumer_cp)
    harness_source = MODULE / prior["receiving_harness"]["receiving_relative_path"].removeprefix("m3/fs/")
    if digest(harness_source) != prior["receiving_harness"]["sha256"]:
        raise RuntimeError("Existing harness source drift")
    harness = OUT / "FacadeTestLauncher.java"
    shutil.copyfile(harness_source, harness)
    names = ["MFilesParallelTest", "MFilesBehaviorTest", "MFilesProviderTest", "MFileCaptureTest", "MFileStreamsTest"]
    tests = [MODULE / ("src/test/java/com/m3/fs/" + n + ".java") for n in names]
    receipt["test_sources"] = {str(p.relative_to(RECEIVER)): digest(p) for p in tests}
    compile_cp = os.pathsep.join(map(str, [classes, *jars, resources]))
    run("compile-tests", [JDK / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-implicit:none", "-sourcepath", "", "-cp", compile_cp, "-d", proof, *tests, harness])
    result = run("junit", [JDK / "bin/java", "-Xcheck:jni", "-cp", str(proof)+os.pathsep+compile_cp, "FacadeTestLauncher", *("com.m3.fs."+n for n in names)])
    counts = {m.group(2): int(m.group(1)) for m in re.finditer(r"\[\s*(\d+)\s+tests\s+(found|skipped|started|aborted|successful|failed)\s*\]", result)}
    if counts != {"found":136,"skipped":0,"started":136,"aborted":0,"successful":136,"failed":0}:
        raise RuntimeError("Unexpected test inventory/results: " + repr(counts))
    for name, expected in pins.items():
        if digest(RECEIVER / name) != expected:
            raise RuntimeError("Qualified source changed during verification: " + name)
    production_classes = {p.relative_to(classes).as_posix():digest(p) for p in classes.rglob("*.class")}
    proof_classes = {p.relative_to(proof).as_posix():digest(p) for p in proof.rglob("*.class")}
    overlap = sorted(set(production_classes) & set(proof_classes))
    if overlap:
        raise RuntimeError("Test classes shadow production: " + repr(overlap))
    receipt.update(status="PASS", junit=counts, qualified_source_pins_unchanged=True, test_production_class_overlap=overlap, production_class_hashes=production_classes, test_class_hashes=proof_classes, legal_gate="All five pinned dependency original LICENSE/NOTICE texts and module descriptor kinds checked by existing load_dependencies")
    persist()
    print(json.dumps({"status":"PASS", "junit":counts, "sources":receipt["compiled_sources"], "receipt":str(OUT / "RESULTS.json")}), flush=True)

if __name__ == "__main__":
    main()
