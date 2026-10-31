# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Scratch qualification driver; canonical recipe admission remains a separate gate."""

import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET


BASE = Path(__file__).resolve().parent
PRODUCT = BASE.parent / "M3JDK21/m3/collections/interop"
INPUT = BASE / "isolated/m3/collections/interop"
JAVA = Path("/tmp/m3jdk21-toolchain-20261020/jdk-21.0.12.1+1")
MAVEN = Path("/tmp/m3jdk21-toolchain-20261020/apache-maven-3.9.9")
M2 = Path("/tmp/m3jdk21-toolchain-20261020/m2")
NATIVE = BASE.parent / "M3JDK21/m3/collections/target/full-java-jni/native"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def files(root):
    return {str(path.relative_to(root)): sha(path)
            for path in sorted(root.rglob("*")) if path.is_file()}


def main():
    mode = sys.argv[1]
    if mode not in ("native", "java"):
        raise SystemExit("mode must be native or java")
    original = json.loads((BASE / "evidence/INPUTS.json").read_text())
    for name, expected in original["module_source_before_sha256"].items():
        assert sha(PRODUCT / name) == expected, ("product source changed", name)
    for post in sorted((BASE / "candidate").rglob("*")):
        if post.is_file():
            rel = post.relative_to(BASE / "candidate/m3/collections/interop")
            assert sha(INPUT / rel) == sha(post), ("candidate mismatch", str(rel))
    owner = Path(original["owning_collection_jar"]["path"])
    assert sha(owner) == original["owning_collection_jar"]["sha256"]
    for name, expected in original["native_libraries"].items():
        assert sha(NATIVE / name) == expected, ("native input changed", name)

    work = BASE / ("fresh-" + mode + "-" + uuid.uuid4().hex)
    assert not work.exists(), "fresh directory already exists"
    module = work / "m3/collections/interop"
    module.mkdir(parents=True)
    shutil.copytree(INPUT / "src", module / "src")
    resource_names = ["pom.xml", "README.md", "donor-origin.json",
                      "THIRD_PARTY_NOTICES.md", "recipe-receipt.tsv",
                      "sdk-family-bindings.tsv", "SDK_FAMILIES.md",
                      "SDK_FAMILY_NOTICES.md"]
    for name in resource_names:
        shutil.copy2(INPUT / name, module / name)
    for name in ("LICENSE", "NOTICE"):
        shutil.copy2(INPUT.parent / name, module.parent / name)
    initial = files(work)
    assert not (module / "target").exists(), "fresh receiver contains target"
    assert not any(name.endswith(".class") for name in initial), "fresh receiver contains class files"
    empty_native = work / "empty-native"
    empty_native.mkdir()
    library_path = NATIVE if mode == "native" else empty_native
    arg_line = "-ea -Xcheck:jni -Djava.library.path=" + str(library_path)
    arg_line += " -Dm3.interop.requireNative=" + str(mode == "native").lower()
    command = [str(MAVEN / "bin/mvn"), "-o", "-B", "-ntp",
               "-Dmaven.repo.local=" + str(M2), "-DargLine=" + arg_line,
               "clean", "verify"]
    env = os.environ.copy()
    env.update(JAVA_HOME=str(JAVA),
               PATH=str(JAVA / "bin") + ":" + str(MAVEN / "bin") + ":/usr/local/bin:/usr/bin:/bin",
               MAVEN_OPTS="-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts",
               PYTHONDONTWRITEBYTECODE="1")
    prefix = BASE / "evidence" / ("attempt-09-fresh-" + mode)
    log = prefix.with_suffix(".log")
    assert not log.exists(), "immutable attempt log already exists"
    start = time.time_ns()
    with log.open("w") as output:
        output.write(json.dumps(command) + "\n")
        output.flush()
        result = subprocess.run(command, cwd=module, env=env,
                                stdout=output, stderr=subprocess.STDOUT)
        output.write("\nEXIT=" + str(result.returncode) + "\n")
    end = time.time_ns()
    source_unchanged = all(sha(work / name) == digest for name, digest in initial.items())
    classes = {str(path.relative_to(module)): {
                    "sha256": sha(path), "mtime_ns": path.stat().st_mtime_ns}
               for path in sorted((module / "target").rglob("*.class"))}
    classes_fresh = bool(classes) and all(start <= item["mtime_ns"] <= end
                                         for item in classes.values())
    reports = {}
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    for report in sorted((module / "target/surefire-reports").glob("TEST-*.xml")):
        root = ET.parse(report).getroot()
        counts = {key: int(root.attrib[key]) for key in totals}
        for key in totals:
            totals[key] += counts[key]
        reports[report.name] = {"sha256": sha(report), **counts}
    snapshots = prefix.with_name(prefix.name + "-class-inspection")
    snapshots.mkdir()
    class_inspection = {}
    for name in ("M3FiveLibraryArrayificationTest", "M3GuavaListMultimapContract",
                 "M3SpringArrayContractTest", "M3ApacheBagContractTest"):
        path = module / "target/test-classes/com/m3/collections/interop" / (name + ".class")
        inspected = subprocess.run([str(JAVA / "bin/javap"), "-p", "-c", "-l", str(path)],
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        text = inspected.stdout
        (snapshots / (name + ".javap.log")).write_text(text)
        class_inspection[name] = {"exit": inspected.returncode, "sha256": sha(path)}
        if name == "M3GuavaListMultimapContract":
            class_inspection[name]["contains_native_availability_branch"] = "M3IndexPathMasks.nativeAvailable:()Z" in text
            class_inspection[name]["contains_native_selection_assertion"] = "native full 32-bit selection" in text
        if name == "M3SpringArrayContractTest":
            class_inspection[name]["contains_minimal_failure_helper"] = "assertSameFailure" in text and "failureOf" in text
        if name == "M3ApacheBagContractTest":
            class_inspection[name]["contains_divergence_fixture"] = "oversizedRetentionPreservesContractAndPinsDonorDivergence" in text
    receipt = {"scope": "isolated owning interop module; canonical recipe and receiving admission pending",
               "mode": mode, "source_head": original["source_head"],
               "receiver": str(module), "command": command,
               "start_time_ns": start, "end_time_ns": end,
               "maven_exit": result.returncode,
               "fresh_directory_had_no_target_or_class_files": True,
               "sources_sha256": initial, "source_hashes_unchanged_after_build": source_unchanged,
               "all_class_mtimes_within_build": classes_fresh,
               "classes": classes, "class_inspection": class_inspection,
               "owning_collection_jar": original["owning_collection_jar"],
               "native_libraries": original["native_libraries"],
               "test_totals": totals, "reports": reports,
               "log": str(log.relative_to(BASE)), "log_sha256": sha(log)}
    passed = result.returncode == 0 and source_unchanged and classes_fresh
    passed &= totals == {"tests": 159, "failures": 0, "errors": 0, "skipped": 0}
    passed &= all(item["exit"] == 0 for item in class_inspection.values())
    passed &= all(value for item in class_inspection.values()
                  for key, value in item.items() if key.startswith("contains_"))
    receipt["qualified_isolated_module"] = passed
    prefix.with_suffix(".json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"mode": mode, "receiver": str(module), "maven_exit": result.returncode,
                      "totals": totals, "sources_unchanged": source_unchanged,
                      "classes_fresh": classes_fresh, "qualified_isolated_module": passed,
                      "receipt": str(prefix.with_suffix('.json'))}, indent=2))
    raise SystemExit(0 if passed else 1)


if __name__ == "__main__":
    main()
