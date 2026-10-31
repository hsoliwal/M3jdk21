#!/usr/bin/env python3
"""Qualify the admitted collections Java against unchanged current JNI owners."""
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0

from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

C = Path(__file__).resolve().parents[1]
B = C.parent / "m3fs-vfs-20261020"
R = B / "receiver"
J = B / "toolchain/jdk21"
G = C / "verification/generated"
Q = G / "classes"
O = C / "verification/native-final"
GIT = Path("/workspace/scratch/53bcd62c244c/M3JDK21")
TREE = "985d672bf250aa090b2399d626f0e3e1b541135d"
OWNERS = ["M3LongIndexProjection", "M3LongAddressKernel", "M3IndexMaskCompactor", "M3IndexMaskWindow"]
CURRENT_TESTS = ["M3IndexMaskCompactorSelfTest", "M3IndexMaskWindowSelfTest",
                 "M3IndexMaskWindowRecoverySelfTest", "M3LongAddressKernelSelfTest"]
NATIVE_NAMES = ["m3_index_mask_compactor.c", "m3_index_mask_window.c", "m3_long_address_kernel.c"]
SUPPORT = ["M3LongArrayList", "M3LongArrayDeque", "M3LongIterator", "M3LongList",
           "M3LongListSpliterator", "M3LongStack", "M3LongDeque", "M3LongSequence",
           "M3LongCollection", "M3LongQueue"]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(name, value):
    (O / name).write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def inventory(path):
    return {str(p.relative_to(path)): sha(p) for p in sorted(path.rglob("*.class"))}


def main():
    if O.exists():
        raise SystemExit("Refusing to overwrite an existing native receipt")
    O.mkdir()
    report = {"status": "RUNNING", "started_utc": datetime.now(timezone.utc).isoformat(),
              "production_classes": str(Q), "production_overlays": False,
              "native_source_changes": False, "commands": [], "runtime_checks": [],
              "source_built_m3jdk21": "NOT_RUN",
              "scope": "Four admitted/current Java owners, three unchanged native C owners, existing finite contract and JNI fixtures. Compactor self-test additionally uses ten exact unchanged target primitive-container dependency sources; not a whole-module or whole-JDK gate"}
    env = os.environ.copy()
    for key in ("CLASSPATH", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
        env.pop(key, None)
    env["GIT_NO_LAZY_FETCH"] = "1"

    def run(name, command, expected=None, timeout=180):
        argv = [str(x) for x in command]
        start = time.monotonic()
        result = subprocess.run(argv, cwd=C, env=env, stdout=subprocess.PIPE,
                                stderr=subprocess.PIPE, timeout=timeout)
        stdout, stderr = O / (name + ".stdout.log"), O / (name + ".stderr.log")
        stdout.write_bytes(result.stdout)
        stderr.write_bytes(result.stderr)
        report["commands"].append({"name": name, "argv": argv, "cwd": str(C),
                                   "exit": result.returncode,
                                   "elapsed_seconds": round(time.monotonic() - start, 3),
                                   "stdout": str(stdout), "stdout_sha256": sha(stdout),
                                   "stderr": str(stderr), "stderr_sha256": sha(stderr)})
        save("commands.json", report["commands"])
        print(name + ": exit=" + str(result.returncode), flush=True)
        output = result.stdout.decode(errors="replace")
        errors = result.stderr.decode(errors="replace")
        if result.returncode:
            raise RuntimeError(name + " failed; see its saved stdout/stderr")
        if re.search(r"WARNING in native method|FATAL ERROR in native method|runtime error:", output + errors):
            raise RuntimeError(name + " emitted a JNI or undefined-behavior diagnostic")
        if expected is not None and expected not in output:
            raise RuntimeError(name + " did not emit required witness " + expected)
        return output

    try:
        prior = C / "verification/native-compile-closure-failure"
        if prior.is_dir():
            target = O / "prior-compile-failure"
            target.mkdir()
            for source in prior.iterdir():
                if source.is_file() and source.suffix in {".json", ".log"}:
                    shutil.copyfile(source, target / source.name)
            report["prior_attempt"] = {"path": str(prior / "RESULTS.json"),
                                       "reason": "Existing compactor self-test requires primitive list/deque dependency closure"}
        sdk_path = C / "verification/sdk/RESULTS.json"
        sdk = json.loads(sdk_path.read_text())
        generated_path = G / "RESULTS.json"
        generated = json.loads(generated_path.read_text())
        if sdk["status"] != "PASS" or generated["status"] != "PASS":
            raise RuntimeError("Canonical admission and generated Java gate must both be PASS")
        if Path(generated["production_classes"]) != Q:
            raise RuntimeError("Generated Java gate identifies different production classes")
        for path, digest in sdk["admitted_files"].items():
            if sha(R / path) != digest:
                raise RuntimeError("Admitted receiver source drift: " + path)
        source_inventory = json.loads((C / "SOURCE_INVENTORY.json").read_text())
        snapshots = {row["target_path"]: row for row in source_inventory["snapshot_records"]}
        retained = json.loads((C / "research/retained-owners.json").read_text())
        native_records = {Path(row["path"]).name: row for row in retained["owners"]
                          if row["path"].endswith(".c")}
        production = [R / "m3/collections/src/main/java/com/m3/collections" / (name + ".java")
                      for name in OWNERS]
        for source in production[1:]:
            rel = str(source.relative_to(R))
            if sha(source) != snapshots[rel]["sha256"]:
                raise RuntimeError("Retained Java dependency owner drift: " + rel)
        report["production_sources"] = {str(p): sha(p) for p in production}
        report["sdk_receipt"] = {"path": str(sdk_path), "sha256": sha(sdk_path)}
        report["generated_java_receipt"] = {"path": str(generated_path), "sha256": sha(generated_path)}
        report["java_release"] = (J / "release").read_text()
        before = inventory(Q)
        if not before:
            raise RuntimeError("Generated production class directory is empty")
        save("production-classes.before.json", before)
        native_dir, proof_dir, header_dir, header_classes, library_dir, missing_dir = [
            O / name for name in ("native-sources", "proof-classes", "headers", "header-classes", "lib", "no-native")]
        for directory in (native_dir, proof_dir, header_dir, header_classes, library_dir, missing_dir):
            directory.mkdir()
        custody = []

        def extract(path, output, digest=None, blob=None):
            git_path = path.removeprefix("m3/collections/")
            actual_blob = subprocess.check_output(["git", "-C", str(GIT), "rev-parse", TREE + ":" + git_path], env=env).decode().strip()
            if blob is not None and actual_blob != blob:
                raise RuntimeError("Immutable source tree/blob mismatch: " + path)
            payload = subprocess.check_output(["git", "-C", str(GIT), "cat-file", "blob", actual_blob], env=env)
            observed_blob = hashlib.sha1(b"blob " + str(len(payload)).encode() + b"\0" + payload).hexdigest()
            observed = hashlib.sha256(payload).hexdigest()
            if observed_blob != actual_blob or (digest is not None and observed != digest):
                raise RuntimeError("Immutable source digest mismatch: " + path)
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes(payload)
            custody.append({"path": path, "tree": TREE, "git_blob": actual_blob,
                            "sha256": observed, "local_path": str(output)})
            return output

        native_sources = {}
        for name in NATIVE_NAMES:
            row = native_records[name]
            native_sources[name] = extract(row["path"], native_dir / name, row["sha256"], row["git_blob"])
        c_fixtures = [extract("m3/collections/src/test/native/" + name, native_dir / name)
                      for name in ("m3_mask_window_regions.c", "m3_mask_window_spans.c")]
        range_source = extract("m3/collections/src/test/java/com/m3/collections/M3NativeIndexRangeSelfTest.java",
                               O / "proof-src/com/m3/collections/M3NativeIndexRangeSelfTest.java")
        proofs = []
        for name in CURRENT_TESTS:
            rel = "m3/collections/src/test/java/com/m3/collections/" + name + ".java"
            row = snapshots[rel]
            source = Path(row["local_path"])
            if sha(source) != row["sha256"]:
                raise RuntimeError("Current native self-test drift: " + rel)
            proofs.append(source)
        contract = R / "m3/collections/src/test/java/com/m3/collections/M3IndexOrderWindowContract.java"
        proofs.extend([range_source, contract])
        support_sources = [extract("m3/collections/src/main/java/com/m3/collections/" + name + ".java",
                           O / "support-src/com/m3/collections" / (name + ".java")) for name in SUPPORT]
        support_classes = O / "support-classes"
        support_classes.mkdir()
        report["unchanged_fixture_dependency_sources"] = {str(p): sha(p) for p in support_sources}
        report["unchanged_fixture_dependency_classes"] = str(support_classes)
        report["unchanged_fixture_dependency_scope"] = "M3IndexMaskCompactorSelfTest only; exact current immutable target tree, disjoint from generated production classes"
        report["proof_sources"] = {str(p): sha(p) for p in proofs}
        report["immutable_git_custody"] = custody
        save("source-custody.json", custody)
        run("java-version", [J / "bin/java", "-version"])
        run("gcc-version", ["/usr/bin/gcc", "--version"])
        run("jni-headers", [J / "bin/javac", "--release", "21", "-Xlint:all", "-Werror",
                             "-sourcepath", "", "-h", header_dir, "-d", header_classes, *production])
        if inventory(header_classes) != before:
            raise RuntimeError("Header derivative compilation differs from generated production bytecode")
        report["header_derivative_classes_match_generated"] = True
        report["header_derivative_classes_on_runtime_classpath"] = False
        run("support-javac", [J / "bin/javac", "--release", "21", "-Xlint:all", "-Werror",
                               "-sourcepath", "", "-cp", Q, "-d", support_classes, *support_sources])
        support_outputs = inventory(support_classes)
        if any(p in before or Path(p).name.removesuffix(".class").split("$", 1)[0] not in SUPPORT for p in support_outputs):
            raise RuntimeError("Current support closure overlaps generated production classes")
        report["unchanged_fixture_dependency_classes_disjoint"] = True
        save("support-classes.json", support_outputs)
        run("proof-javac", [J / "bin/javac", "--release", "21", "-Xlint:all", "-Werror",
                             "-sourcepath", "", "-cp", os.pathsep.join([str(Q), str(support_classes)]),
                             "-d", proof_dir, *proofs])
        outputs = inventory(proof_dir)
        allowed = set(CURRENT_TESTS) | {"M3NativeIndexRangeSelfTest", "M3IndexOrderWindowContract"}
        if any(p in before or Path(p).name.removesuffix(".class").split("$", 1)[0] not in allowed for p in outputs):
            raise RuntimeError("Proof output contains a production or undeclared class")
        save("proof-classes.json", outputs)
        report["fresh_test_fixtures_only"] = True
        api = run("public-protected-api", [J / "bin/javap", "-protected", "-s", "-classpath", Q,
                  *["com.m3.collections." + name for name in OWNERS],
                  "com.m3.collections.M3LongIndexProjection$Order", "com.m3.collections.M3IndexMaskCompactor$Backend"])
        if api != (C / "verification/baseline/public-protected-api.stdout.log").read_text():
            raise RuntimeError("Generated Java public/protected API drift from baseline")
        report["public_protected_api_identical_to_baseline"] = True
        cflags = ["-std=c17", "-O2", "-Wall", "-Wextra", "-Werror", "-Wpedantic",
                  "-fsanitize=undefined", "-fno-sanitize-recover=undefined",
                  "-I" + str(J / "include"), "-I" + str(J / "include/linux")]
        object_files = {}
        for name, owner in zip(NATIVE_NAMES, ["M3IndexMaskCompactor", "M3IndexMaskWindow", "M3LongAddressKernel"]):
            target = O / (Path(name).stem + ".o")
            run("compile-" + Path(name).stem, ["/usr/bin/gcc", *cflags, "-fPIC", "-include",
                header_dir / ("com_m3_collections_" + owner + ".h"), "-c", native_sources[name], "-o", target])
            object_files[name] = target
        libraries = {
            "libm3_collections_mask.so": [object_files[NATIVE_NAMES[0]], object_files[NATIVE_NAMES[1]]],
            "libm3_collections_address.so": [object_files[NATIVE_NAMES[2]]],
        }
        actual_symbols = set()
        for name, objects in libraries.items():
            path = library_dir / name
            run("link-" + name, ["/usr/bin/gcc", "-shared", "-fsanitize=undefined",
                "-fno-sanitize-recover=undefined", "-Wl,-z,defs", "-Wl,-soname," + name, *objects, "-o", path])
            text = run("symbols-" + name, ["/usr/bin/nm", "-D", "--defined-only", path])
            actual_symbols.update(re.findall(r"\bJava_\w+", text))
        expected_symbols = set()
        for header in header_dir.glob("*.h"):
            expected_symbols.update(re.findall(r"\bJava_\w+", header.read_text()))
        if actual_symbols != expected_symbols or len(actual_symbols) != 12:
            raise RuntimeError("Actual JNI symbol set differs from generated Java headers")
        report["jni_abi"] = {"status": "PASS", "symbol_count": len(actual_symbols),
                             "symbols": sorted(actual_symbols), "force_included_header_signature_checks": "PASS"}
        report["libraries"] = {str(library_dir / name): sha(library_dir / name) for name in libraries}
        report["c_flags"] = cflags
        for fixture, marker in zip(c_fixtures, ["PASS JNI region work:", "JNI_SPANS_PASS single=1/128 dense_limit1=1/128 cross_chunk=130/300 faults=3"]):
            executable = O / fixture.stem
            run("compile-" + fixture.stem, ["/usr/bin/gcc", *cflags, fixture,
                object_files["m3_index_mask_window.c"], "-o", executable])
            output = run("run-" + fixture.stem, [executable], expected=marker)
            report["runtime_checks"].append({"name": fixture.stem, "status": "PASS", "witness": output.strip(),
                                             "kind": "instrumented JNI table over exact current native object"})
        cp = os.pathsep.join([str(proof_dir), str(Q)])

        def java_check(label, classname, args, native, witness):
            path = library_dir if native else missing_dir
            runtime_cp = cp + os.pathsep + str(support_classes) if classname == "M3IndexMaskCompactorSelfTest" else cp
            output = run(label, [J / "bin/java", "-ea", "-XX:-UsePerfData", "-Xmx512m", "-Xcheck:jni",
                "-Djava.library.path=" + str(path), "-cp", runtime_cp, "com.m3.collections." + classname, *args], expected=witness)
            report["runtime_checks"].append({"name": label, "status": "PASS", "witness": output.strip(),
                                             "kind": "real JVM", "native_library_mode": "required" if native else "absent"})
            return output

        for native in (False, True):
            prefix = "native-" if native else "java-"
            args = ["--native"] if native else []
            for name in CURRENT_TESTS[:3]:
                java_check(prefix + name, name, args, native, "PASS")
            java_check(prefix + "M3LongAddressKernelSelfTest", "M3LongAddressKernelSelfTest", [], native,
                       "native=" + str(native).lower())
            mode = "range" if native else "none"
            java_check(prefix + "M3NativeIndexRangeSelfTest", "M3NativeIndexRangeSelfTest", [mode, "sharing"],
                       native, "M3NativeIndexRangeSelfTest mode=" + mode)
        java_check("missing-native-contract", "M3IndexOrderWindowContract", ["--missing"], False,
                   "INDEX_ORDER_WINDOW_NATIVE_REFUSAL_PASS oldBinary=false")
        observed = java_check("native-contract", "M3IndexOrderWindowContract", ["--native"], True,
                              "INDEX_ORDER_WINDOW_PASS native=true")
        match = re.search(r"INDEX_ORDER_WINDOW_PASS native=true maskPairs=(\d+) windows=(\d+) checks=(\d+) observations=([0-9a-f]{64})", observed)
        if not match or match.group(4) != generated["finite_observation_sha256"]:
            raise RuntimeError("Native finite observation digest differs from generated Java oracle")
        report["finite_oracle"] = {"mask_pairs": int(match.group(1)), "windows": int(match.group(2)),
                                   "checks": int(match.group(3)), "observation_sha256": match.group(4),
                                   "identical_to_generated_java": True}
        after = inventory(Q)
        save("production-classes.after.json", after)
        report["production_classes_unchanged"] = before == after
        report["production_sources_unchanged"] = all(sha(Path(path)) == digest for path, digest in report["production_sources"].items())
        report["native_sources_unchanged"] = all(sha(Path(row["local_path"])) == row["sha256"] for row in custody)
        report["proof_sources_unchanged"] = all(sha(Path(path)) == digest for path, digest in report["proof_sources"].items())
        report["unchanged_fixture_dependency_class_hashes_stable"] = inventory(support_classes) == support_outputs
        if not all(report[key] for key in ("production_classes_unchanged", "production_sources_unchanged",
                                         "native_sources_unchanged", "proof_sources_unchanged",
                                         "unchanged_fixture_dependency_class_hashes_stable")):
            raise RuntimeError("Source or generated class changed during native qualification")
        report["runtime_checks_passed"] = len(report["runtime_checks"])
        report["jni_diagnostics"] = 0
        report["undefined_behavior_diagnostics"] = 0
        report["status"] = "PASS"
    except Exception as failure:
        report["status"] = "FAIL"
        report["error"] = type(failure).__name__ + ": " + str(failure)
        print(report["error"], file=sys.stderr)
    finally:
        report["finished_utc"] = datetime.now(timezone.utc).isoformat()
        save("RESULTS.json", report)
    print(report["status"] + "; receipt: " + str(O / "RESULTS.json"), flush=True)
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
