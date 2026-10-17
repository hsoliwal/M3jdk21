#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Execute the wrapper contract against actual SDK Results and sealed parent inputs."""
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
import xml.etree.ElementTree as ET


CRATE = "m3-deep-filter-list-20261017"
TARGET_BASELINE = "37eba37dcbdf0c0222b494d07ba2fca401ce248f"
TASK = "m3/collections/wrappers/deep-filter-list-20261017"
RESOURCE = "src/main/resources/com/synexia/rewrite"
MANIFEST_HASHES = {
    "java": "d4f4b345672f0e3f6d7e71f5dd23f6e011ed7f7e7d9e356caf67730e61573cad",
    "text": "ee69271da7d7596726e31e3d0cdbd7bec2501aaa3ba576be235a48d2f28ca9f9",
}
PREDECESSOR_JAVA_MANIFEST = "c5169cd31a7881cc78c95ad2b7fa63d67ebb46565f97b340876296efa2250442"
ADDED_JAVA_TARGETS = {
    "m3/collections/src/main/java/com/m3/collections/M3FilteredSortedMultisets.java",
    "m3/collections/wrappers/src/test/java/com/m3/collections/M3DeepFilteredSortedMultisetsProof.java",
}
COUNTED_COMPILE_INPUTS = tuple("m3/collections/src/main/java/com/m3/collections/" + name + ".java"
    for name in ("MIndexSpace", "M3IndexHeapAdmission", "M3IndexOrderedStore", "M3IndexRange",
                 "M3IndexCountKernel", "M3IndexNavigableSet", "M3IndexSortedMultiset")) + (
    "m3/collections/src/test/java/com/m3/collections/M3FilteredSortedMultisetContract.java",)
PARENT_INPUTS_HASH = "d31df2309df5e26ef2363d5908818b5a1a743e573dbd0ac0d46eec57a3a9c1d0"
SDK_REPORT = "target/surefire-reports/TEST-com.synexia.rewrite.M3TaskRecipeTest.xml"
MAPPING_TARGET = "m3/docs/name-mapping.json"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def safe_file(root, relative, absent=False):
    rel = Path(relative)
    if rel.is_absolute() or ".." in rel.parts:
        raise ValueError("invalid relative source path: " + relative)
    path = root / rel
    cursor = path
    while cursor != root:
        if cursor.is_symlink():
            raise ValueError("symlinked source input: " + relative)
        cursor = cursor.parent
    if absent and not path.exists():
        return None
    if not path.is_file():
        raise ValueError("missing source input: " + relative)
    return path.read_bytes()


def parent_inventory():
    path = Path(__file__).with_name("parent-inputs.tsv")
    data = path.read_bytes()
    if sha(data) != PARENT_INPUTS_HASH:
        raise ValueError("sealed parent input inventory drift")
    rows = [line.split("\t") for line in data.decode("utf-8").splitlines()]
    if len(rows) != 61 or any(len(row) != 2 for row in rows):
        raise ValueError("incomplete parent input inventory")
    if [row[0] for row in rows] != sorted({row[0] for row in rows}):
        raise ValueError("unordered or duplicate parent inputs")
    return dict(rows)


def manifest_seals(crate):
    data = safe_file(crate, "contract/MANIFEST_SEALS.json")
    seals = json.loads(data)
    if set(seals) != {"schema", "manifests"} or seals["schema"] != "M3_WRAPPER_MANIFEST_SEALS_V1":
        raise ValueError("invalid manifest seal schema")
    lanes = seals["manifests"]
    if set(lanes) != {"java", "text"}:
        raise ValueError("invalid sealed lane inventory")
    for lane in lanes.values():
        if (set(lane) != {"sha256", "count"}
                or not re.fullmatch(r"[0-9a-f]{64}", lane["sha256"])):
            raise ValueError("invalid manifest seal")
    if (lanes["java"] != {"sha256": MANIFEST_HASHES["java"], "count": 7}
            or lanes["text"] != {"sha256": MANIFEST_HASHES["text"], "count": 6}):
        raise ValueError("only the exact thirteen reviewed wrapper targets are admitted")
    return lanes, data


def sdk_input_binding(crate, report):
    """Bind retained SDK evidence to the exact recipe and four reused SDK owners."""
    data = safe_file(crate, "target/candidate/SDK_INPUTS.tsv")
    rows = [line.split("\t") for line in data.decode("utf-8").splitlines()
            if line and not line.startswith("#")]
    if any(len(row) != 2 for row in rows) or len({row[0] for row in rows}) != len(rows):
        raise ValueError("malformed or duplicate SDK input binding")
    properties = {node.attrib["name"]: node.attrib.get("value", "")
                  for node in report.findall("./properties/property")}
    owner_value = properties.get("m3.recipe.owner.root")
    if not owner_value:
        raise ValueError("SDK report does not identify the source recipe owner")
    owner = Path(owner_value).resolve(strict=True)
    expected = {"crate/" + relative: sha(safe_file(crate, relative))
                for relative in ("pom.xml", "receive.py")}
    for directory in ("src", "contract"):
        for source in sorted((crate / directory).rglob("*")):
            if source.is_file():
                relative = source.relative_to(crate).as_posix()
                expected["crate/" + relative] = sha(safe_file(crate, relative))
    for name in ("M3EditScope.java", "M3HashPinnedJavaSnapshotRecipe.java",
                 "M3HashPinnedTextSnapshotRecipe.java", "M3ScopeInference.java"):
        relative = "src/main/java/com/synexia/rewrite/" + name
        expected["owner/" + relative] = sha(safe_file(owner, relative))
    if dict(rows) != expected:
        raise ValueError("SDK inputs changed since qualification; rerun Maven tests")
    return data


def admit_inputs(target, crate):
    """Read-only admission; no output directory exists before this succeeds."""
    lanes, manifest_seals_bytes = manifest_seals(crate)
    dependencies = parent_inventory()
    dependency_bytes = {}
    target_state = {}
    for path, expected in dependencies.items():
        data = safe_file(target, path)
        actual = sha(data)
        if actual != expected:
            raise ValueError("required parent input drift: " + path)
        dependency_bytes[path] = data
        target_state[path] = actual

    resources = {}
    outputs = {}
    preimages = {}
    java_paths = []
    receipt_rows = []
    for kind in ("java", "text"):
        seal = lanes[kind]
        directory = RESOURCE + "/hash-pinned-" + kind + "/" + CRATE
        data = safe_file(crate, directory + "/manifest.tsv")
        if sha(data) != seal["sha256"]:
            raise ValueError("exact recipe manifest drift: " + kind)
        resources[directory + "/manifest.tsv"] = data
        rows = [line.split("\t") for line in data.decode("utf-8").splitlines()]
        if len(rows) != seal["count"] or any(len(row) != 4 for row in rows):
            raise ValueError("incomplete recipe target inventory: " + kind)
        if [row[0] for row in rows] != sorted({row[0] for row in rows}):
            raise ValueError("duplicate or unordered recipe paths")
        if kind == "java":
            core_rows = [row for row in rows if row[0] not in ADDED_JAVA_TARGETS]
            core_data = "".join("\t".join(row) + "\n" for row in core_rows).encode("utf-8")
            if sha(core_data) != PREDECESSOR_JAVA_MANIFEST:
                raise ValueError("the five predecessor Java targets must remain byte-identical")
        if any(row[0] == MAPPING_TARGET and (kind != "text" or row[1] == "ABSENT") for row in rows):
            raise ValueError("name mapping must refine the existing text owner")
        for path, before, after, name in rows:
            payload = safe_file(crate, directory + "/" + name)
            if sha(payload) != after:
                raise ValueError("recipe postimage drift: " + path)
            resources[directory + "/" + name] = payload
            if before != "ABSENT":
                preimage = safe_file(crate, directory + "/pre/" + name)
                if sha(preimage) != before:
                    raise ValueError("recipe preimage drift: " + path)
                resources[directory + "/pre/" + name] = preimage
                preimages[path] = preimage
            present = safe_file(target, path, absent=True)
            actual = "ABSENT" if present is None else sha(present)
            if actual not in (before, after):
                raise ValueError("target is neither sealed preimage nor postimage: " + path)
            target_state[path] = actual
            emitted = safe_file(crate, "target/candidate/" + path)
            if emitted != payload:
                raise ValueError("actual SDK Results differ from sealed postimage: " + path)
            if path in outputs:
                raise ValueError("duplicate cross-lane target")
            outputs[path] = emitted
            receipt_rows.append(path + "\t" + before + "\t" + after + "\n")
            if kind == "java":
                java_paths.append(path)

    sdk_receipt = safe_file(crate, "target/candidate/SDK_RESULTS.tsv")
    expected_receipt = "# OpenRewrite SDK Results; no template-copy materialization\n" + "".join(receipt_rows)
    if sdk_receipt != expected_receipt.encode("utf-8"):
        raise ValueError("SDK Results receipt does not match exact recipe lanes")
    sdk_report = safe_file(crate, SDK_REPORT)
    report = ET.fromstring(sdk_report)
    totals = {name: int(report.attrib.get(name, "-1"))
              for name in ("tests", "failures", "errors", "skipped")}
    if totals != {"tests": 5, "failures": 0, "errors": 0, "skipped": 0}:
        raise ValueError("actual five-test SDK qualification is incomplete")
    input_binding = sdk_input_binding(crate, report)
    return {
        "dependencies": dependencies, "dependency_bytes": dependency_bytes, "target_state": target_state,
        "resources": resources, "outputs": outputs, "preimages": preimages,
        "java_paths": java_paths, "sdk_receipt": sdk_receipt, "sdk_report": sdk_report,
        "sdk_input_binding": input_binding,
        "manifest_seals": lanes, "manifest_seals_bytes": manifest_seals_bytes,
    }


def execute(target, crate, output, java_home):
    admitted = admit_inputs(target, crate)
    if not sys.platform.startswith("linux"):
        raise ValueError("retained JNI/collector proof requires Linux")
    java = java_home / "bin"
    version = subprocess.run([str(java / "javac"), "-version"], text=True,
                             capture_output=True, timeout=10, check=True)
    if not re.match(r"javac 21(?:[.\s]|$)", version.stdout):
        raise ValueError("JAVA_HOME must select JDK21: " + version.stdout + version.stderr)
    output.mkdir(parents=True, exist_ok=False)
    overlay = output / "overlay"
    evidence = output / "evidence"
    evidence.mkdir()
    build = output / "build"
    build.mkdir()
    environment = dict(os.environ)
    environment["JAVA_HOME"] = str(java_home)
    environment["PATH"] = str(java) + os.pathsep + environment.get("PATH", "")
    commands = []
    status = "RUNNING"

    def write_receipt():
        (evidence / "SDK_RUNTIME_RECEIPT.json").write_text(json.dumps({
            "schema": "M3_DEEP_FILTER_LIST_SDK_CONTRACT_V1",
            "status": status, "target": str(target), "crate": str(crate),
            "required_parent_baseline": TARGET_BASELINE,
            "java_home": str(java_home), "javac_version": version.stdout.strip(),
            "manifest_sha256": {kind: seal["sha256"] for kind, seal in admitted["manifest_seals"].items()},
            "manifest_seals_sha256": sha(admitted["manifest_seals_bytes"]),
            "parent_inputs_sha256": PARENT_INPUTS_HASH,
            "contract_script_sha256": sha(Path(__file__).read_bytes()),
            "sdk_input_binding_verified": admitted["sdk_input_binding"] is not None,
            "required_parent_files": len(admitted["dependencies"]),
            "target_input_states": admitted["target_state"],
            "actual_sdk_generated_files": {path: sha(data) for path, data in admitted["outputs"].items()},
            "commands": commands, "target_files_written": False,
        }, indent=2) + "\n")

    def put(relative, data):
        path = overlay / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    for path, data in admitted["dependency_bytes"].items():
        put(path, data)
    for path, data in admitted["resources"].items():
        put(TASK + "/recipe/" + path, data)
    for path, data in admitted["outputs"].items():
        put(path, data)
    (evidence / "SDK_RESULTS.tsv").write_bytes(admitted["sdk_receipt"])
    (evidence / "SDK_TESTS.xml").write_bytes(admitted["sdk_report"])
    (evidence / "MANIFEST_SEALS.json").write_bytes(admitted["manifest_seals_bytes"])
    if admitted["sdk_input_binding"] is not None:
        (evidence / "SDK_INPUTS.tsv").write_bytes(admitted["sdk_input_binding"])
    shutil.copyfile(Path(__file__).with_name("parent-inputs.tsv"), evidence / "parent-inputs.tsv")
    write_receipt()

    def run(label, arguments):
        argv = [str(argument) for argument in arguments]
        started = time.monotonic()
        try:
            result = subprocess.run(argv, cwd=overlay, env=environment,
                                    text=True, capture_output=True, timeout=240)
        except subprocess.TimeoutExpired as failure:
            stdout = failure.stdout or b""
            stderr = failure.stderr or b""
            if isinstance(stdout, bytes):
                stdout = stdout.decode("utf-8", errors="replace")
            if isinstance(stderr, bytes):
                stderr = stderr.decode("utf-8", errors="replace")
            (evidence / (label + ".stdout.log")).write_text(stdout)
            (evidence / (label + ".stderr.log")).write_text(stderr)
            commands.append({"label": label, "command": argv, "exit_code": None,
                             "timeout_seconds": 240})
            write_receipt()
            raise RuntimeError("contract command timed out: " + label) from failure
        (evidence / (label + ".stdout.log")).write_text(result.stdout)
        (evidence / (label + ".stderr.log")).write_text(result.stderr)
        commands.append({"label": label, "command": argv, "exit_code": result.returncode,
                         "seconds": round(time.monotonic() - started, 4)})
        write_receipt()
        print(label + " exit=" + str(result.returncode), result.stdout[-1800:].strip(), flush=True)
        if result.returncode != 0 or result.stderr:
            raise RuntimeError(label + " failed or emitted stderr:\n" + result.stdout + result.stderr)
        if "WARNING in native method" in result.stdout or "runtime error:" in result.stdout:
            raise RuntimeError("JNI/sanitizer diagnostic: " + label)
        return result.stdout

    try:
        classes = build / "current"
        run("strict-sdk-java21", [
            java / "javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
            "-d", classes, *(overlay / path for path in admitted["java_paths"]),
            *(overlay / path for path in COUNTED_COMPILE_INPUTS)
        ])
        run("list-tiered-depth65536", [
            java / "java", "-Xss256k", "-Xmx256m", "-cp", classes,
            "com.m3.collections.M3DeepFilteredListsProof", "65536", "192"
        ])
        run("donor-tiered-depth65536", [
            java / "java", "-Xss256k", "-Xmx256m", "-cp", classes,
            "com.m3.collections.M3DeepFilterCompositionProof"
        ])
        run("list-interpreter-depth8192", [
            java / "java", "-Xint", "-Xss256k", "-Xmx256m", "-cp", classes,
            "com.m3.collections.M3DeepFilteredListsProof", "8192", "192"
        ])
        run("donor-interpreter-depth8192", [
            java / "java", "-Xint", "-Xss256k", "-Xmx256m", "-Dm3.deep.depth=8192",
            "-cp", classes, "com.m3.collections.M3DeepFilterCompositionProof"
        ])
        run("sorted-multiset-tiered-depth65536", [
            java / "java", "-Xss256k", "-Xmx256m", "-cp", classes,
            "com.m3.collections.M3DeepFilteredSortedMultisetsProof", "65536", "64"
        ])
        run("sorted-multiset-interpreter-depth8192", [
            java / "java", "-Xint", "-Xss256k", "-Xmx256m", "-cp", classes,
            "com.m3.collections.M3DeepFilteredSortedMultisetsProof", "8192", "64"
        ])
        originals = output / "original-sources"
        originals.mkdir()
        old_sources = []
        for path, data in admitted["preimages"].items():
            if path.startswith("m3/collections/src/main/java/"):
                source = originals / Path(path).name
                source.write_bytes(data)
                old_sources.append(source)
        old_classes = build / "original"
        proof = overlay / "m3/collections/wrappers/src/test/java/com/m3/collections/M3DeepFilteredListsProof.java"
        run("original-owner-strict-compile", [
            java / "javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
            "-d", old_classes, *old_sources, proof,
            overlay / "m3/collections/wrappers/src/test/java/com/m3/collections/M3DeepFilteredSortedMultisetsProof.java",
            *(overlay / path for path in COUNTED_COMPILE_INPUTS)
        ])
        run("original-list-overflow", [
            java / "java", "-Xss256k", "-Xmx256m", "-cp", old_classes,
            "com.m3.collections.M3DeepFilteredListsProof", "--baseline-overflow", "65536"
        ])
        run("original-sorted-multiset-overflow", [
            java / "java", "-Xss256k", "-Xmx256m", "-cp", old_classes,
            "com.m3.collections.M3DeepFilteredSortedMultisetsProof", "--baseline-overflow", "65536"
        ])
        for name in ("M3CollectionViews", "M3FilteredMaps", "M3FilteredLists",
                     "M3FilteredSortedMultisets", "M3FilteredSortedMultisets$View"):
            owner = "com.m3.collections." + name
            before = run("original-public-api-" + name, [
                java / "javap", "-public", "-s", "-cp", old_classes, owner
            ])
            after = run("sdk-public-api-" + name, [
                java / "javap", "-public", "-s", "-cp", classes, owner
            ])
            if before != after:
                raise AssertionError("public ABI changed: " + owner)
        run("successor-refusal-proof", [
            sys.executable, overlay / TASK / "verify_successor_test.py", "--root", overlay
        ])
        run("retained-list-int-jni-five-collectors", [
            "sh", overlay / "m3/collections/wrappers/filtered-mat-receiver-20261017/verify.sh"
        ])
        run("retained-writable-map-avl", [
            "sh", overlay / "m3/collections/wrappers/writable-map-avl-20261017/verify.sh"
        ])
        run("retained-index-counted-java-jni", [
            "sh", overlay / "m3/collections/wrappers/index-counted-receiver-20261017/verify.sh"
        ])
        if len(commands) != 24:
            raise AssertionError("incomplete twenty-four-command contract")
        for path, data in admitted["outputs"].items():
            if safe_file(crate, "target/candidate/" + path) != data or safe_file(overlay, path) != data:
                raise AssertionError("SDK Result drift during contract: " + path)
        for path, expected in admitted["target_state"].items():
            data = safe_file(target, path, absent=True)
            if ("ABSENT" if data is None else sha(data)) != expected:
                raise AssertionError("target input changed during isolated proof: " + path)
        if (safe_file(crate, SDK_REPORT) != admitted["sdk_report"]
                or safe_file(crate, "target/candidate/SDK_RESULTS.tsv") != admitted["sdk_receipt"]
                or safe_file(crate, "contract/MANIFEST_SEALS.json") != admitted["manifest_seals_bytes"]
                or sdk_input_binding(crate, ET.fromstring(admitted["sdk_report"])) != admitted["sdk_input_binding"]):
            raise AssertionError("SDK qualification evidence changed during contract")
        status = "PASS"
        write_receipt()
        shutil.rmtree(build)
        print("SDK_WRAPPER_RUNTIME_VALIDATION_COMPLETE commands=24 parent_inputs=61", flush=True)
    except BaseException:
        status = "FAIL"
        write_receipt()
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", type=Path, required=True, help="M3JDK21 checkout at sealed pre/post states")
    parser.add_argument("--crate", type=Path, required=True, help="Canonical or mirrored recipe crate with SDK Results")
    parser.add_argument("--output", type=Path, required=True, help="Fresh directory for the minimal overlay and evidence")
    args = parser.parse_args()
    if not os.environ.get("JAVA_HOME"):
        parser.error("JAVA_HOME must point to a JDK21 installation")
    target = args.target.resolve(strict=True)
    crate = args.crate.resolve(strict=True)
    output = args.output.resolve()
    if (output.exists() or output == target or output == crate
            or output in (set(target.parents) | set(crate.parents))
            or output.is_relative_to(crate / "target/candidate")
            or output.is_relative_to(crate / "src")):
        parser.error("--output must be a fresh proof directory")
    execute(target, crate, output, Path(os.environ["JAVA_HOME"]).resolve(strict=True))


if __name__ == "__main__":
    main()
