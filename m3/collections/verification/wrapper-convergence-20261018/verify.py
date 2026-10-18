#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Verify one sealed collections successor using the existing module and test owners.

Source admission/command orchestration follows m3/collections/verify.py and
verification/zero-slot-growth-20261017/verify.py. This is a receipt-producing
build runner, not a transformation engine. Historical donation guards are not
changed or replaced. No full JDK image, jtreg, or performance claim is made.

The adjacent SOURCE_MANIFEST.json is sealed together with this TEXT candidate
by the canonical recipe. See README.md for its schema and invocation. A fresh
--out is required. Maven builds an exact hash-checked copy of the received
module inputs there; neither the target nor the optional preimage is edited.
"""

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


SCHEMA = "m3-wrapper-verification/v1"
MODULE = "m3/collections/"
JAVA = MODULE + "src/main/java/"
TEST = MODULE + "src/test/java/com/m3/collections/"
NATIVE = MODULE + "src/main/native/collections/"
HERE = MODULE + "verification/wrapper-convergence-20261018/"
PACKAGE = "com.m3.collections."
RESOURCE_FILES = ("pom.xml", "LICENSE", "NOTICE", "provenance.json",
                  "synexia-donation.json", "name-mapping.json",
                  "SYNEXIA-DONATION-NOTICE.txt")
RUNTIME_OWNERS = ("M3FilteredNavigableSet", "M3FilteredViews",
                  "M3MutableFilteredViews", "M3LongIndexProjection", "M3IndexPathMasks")
JNI_OWNERS = ("M3IndexPathMasks", "M3IndexMaskCompactor", "M3IndexMaskWindow")
FROZEN_TESTS = (
    "M3DeepReadOnlyCollectionSelfTest", "M3DeepReadOnlyCollectionTest",
    "M3FilteredNavigationBoundarySelfTest", "M3FilteredNavigationBoundaryTest",
    "M3IndexPathImpossibleStageSelfTest", "M3IndexPathImpossibleStageTest",
    "M3IndexPathMaskAliasSelfTest", "M3IndexPathMasksLongSelfTest", "M3IndexPathMasksLongTest",
    "M3IndexPathZeroStagesSelfTest", "M3IndexPathZeroStagesTest",
    "M3IndexPathZeroWindowSelfTest", "M3IndexPathZeroWindowTest",
    "M3LongIndexProjectionPathMasksTest", "M3OrderWindowProof", "M3OrderWindowTest",
    "M3ReadOnlyViewBoundarySelfTest", "M3ReadOnlyViewBoundaryTest",
    "M3WrapperConvergenceSelfTest", "M3WrapperConvergenceTest")
MASK_PROOFS = (
    ("M3IndexPathMasksSelfTest", "PASS M3IndexPathMasksSelfTest", "JNI="),
    ("M3IndexPathMasksLongSelfTest", "PASS M3IndexPathMasksLongSelfTest", "JNI="),
    ("M3IndexPathMaskAliasSelfTest", "PATH_MASK_ALIAS_PASS", "backend="),
    ("M3IndexPathImpossibleStageSelfTest", "M3_IMPOSSIBLE_STAGE_MASK_PASS", "JNI="),
    ("M3IndexPathZeroStagesSelfTest", "M3_ZERO_STAGES_PATH_MASK_PASS", "JNI="),
    ("M3IndexPathZeroWindowSelfTest", "M3_ZERO_INDEX_WINDOW_PASS", "backend="),
    ("M3IndexPathMaskWindowSelfTest", "M3_PATH_MASK_WINDOW_PASS", "backend="),
    ("M3IndexMaskWindowSelfTest", "PASS M3_INDEX_MASK_WINDOW", "native="))
WRAPPER_PROOFS = (
    ("M3FilteredNavigationBoundarySelfTest", "PASS M3FilteredNavigationBoundarySelfTest"),
    ("M3ReadOnlyViewBoundarySelfTest", "PASS M3ReadOnlyViewBoundarySelfTest"),
    ("M3WrapperConvergenceSelfTest", "PASS M3WrapperConvergenceSelfTest"),
    ("M3DeepReadOnlyCollectionSelfTest", "PASS M3DeepReadOnlyCollectionSelfTest"),
    ("M3NavigationFailureContract", "PASS M3NavigationFailureContract checks="),
    ("M3BoundedFilteredNavigationContract", "PASS M3_BOUNDED_FILTERED_NAVIGATION checks="))
HASH = re.compile(r"[0-9a-f]{64}\Z")
JNI_WARNING = re.compile(r"WARNING in native method|WARNING[^\n]*JNI|"
                         r"FATAL ERROR in native method|runtime error:", re.I)


class Refusal(RuntimeError):
    """A missing, changed or unsuccessful qualification input/gate."""


def require(condition, message):
    if not condition:
        raise Refusal(message)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def utc():
    return datetime.now(timezone.utc).isoformat()


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def relative_path(value):
    require(isinstance(value, str), "manifest path must be text")
    path = PurePosixPath(value)
    require(not path.is_absolute() and path.as_posix() == value and
            ".." not in path.parts and value.startswith(MODULE) and
            not any(c in value for c in "\x00\r\n\\"), "unsafe source path: " + value)
    return path


def regular_file(root, relative, absent=False):
    current = root
    for part in relative_path(relative).parts:
        current = current / part
        require(not current.is_symlink(), "source alias refused: " + relative)
    if absent:
        require(not current.exists(), "expected absent preimage: " + relative)
    else:
        require(current.is_file(), "missing regular source: " + relative)
    return current


def module_inputs(root):
    """Exact closure copied for the existing standalone module POM."""
    paths = {MODULE + name for name in RESOURCE_FILES}
    source = root / MODULE / "src"
    require(source.is_dir() and not source.is_symlink(), "missing/aliased module src")
    for path in sorted(source.rglob("*")):
        require(not path.is_symlink(), "module input alias refused: " + str(path))
        if path.is_file():
            paths.add(path.relative_to(root).as_posix())
    return {path: {"sha256": sha(regular_file(root, path)),
                   "bytes": regular_file(root, path).stat().st_size}
            for path in sorted(paths)}


def read_manifest(path, target, before):
    require(path.is_file() and not path.is_symlink(), "missing/aliased source manifest")
    manifest = json.loads(path.read_text(encoding="utf-8"))
    require(manifest.get("schema") == SCHEMA, "unsupported source manifest schema")
    require(isinstance(manifest.get("provenance"), dict) and manifest["provenance"],
            "source provenance must be a nonempty object")
    rows = manifest.get("files")
    require(isinstance(rows, list) and rows, "empty source manifest")
    found = {}
    for row in rows:
        require(isinstance(row, dict), "source row must be an object")
        path = row.get("path")
        relative_path(path)
        require(path not in found, "duplicate source: " + path)
        old, post = row.get("before_sha256"), row.get("after_sha256")
        require(isinstance(old, str) and (old == "ABSENT" or HASH.fullmatch(old)),
                "invalid before hash: " + path)
        require(isinstance(post, str) and HASH.fullmatch(post), "invalid after hash: " + path)
        require(isinstance(row.get("role"), str) and row["role"], "missing source role: " + path)
        require(sha(regular_file(target, path)) == post, "postimage source drift: " + path)
        if before:
            preimage = regular_file(before, path, absent=old == "ABSENT")
            if old != "ABSENT":
                require(sha(preimage) == old, "preimage source drift: " + path)
        found[path] = row
    required = {JAVA + "com/m3/collections/" + n + ".java" for n in RUNTIME_OWNERS}
    required |= {NATIVE + n for n in ("m3_index_path_masks.c", "m3_index_mask_compactor.c",
                                       "m3_index_mask_window.c")}
    names = set(FROZEN_TESTS) | {n for n, _, _ in MASK_PROOFS} | {n for n, _ in WRAPPER_PROOFS}
    names |= {"M3NavigationFailureTest", "M3BoundedFilteredNavigationTest"}
    required |= {TEST + name + ".java" for name in names}
    required.add(HERE + "verify.py")
    require(required <= found.keys(), "required source omitted: " + repr(sorted(required - found.keys())))
    require(sha(Path(__file__).resolve()) == found[HERE + "verify.py"]["after_sha256"],
            "executing runner differs from the sealed received runner")
    return manifest, found


class Evidence:
    def __init__(self, out, env, arguments):
        self.out, self.env = out, env
        self.receipt = {"schema": SCHEMA, "status": "RUNNING", "started_utc": utc(),
                        "arguments": arguments, "commands": [], "gates": {},
                        "scope": "sealed collections module and named Java/JNI proofs",
                        "limitations": ["No full JDK image or jtreg qualification",
                                        "No speed or allocation improvement claim",
                                        "Historical verify_donation guards remain independent"]}
        self.save()

    def save(self):
        write_json(self.out / "RECEIPT.json", self.receipt)

    def run(self, label, argv, cwd, expected=(), jni=False, timeout=900):
        argv = [str(arg) for arg in argv]
        stem = f"{len(self.receipt['commands']) + 1:03d}-{label}"
        stdout, stderr = self.out / (stem + ".stdout.log"), self.out / (stem + ".stderr.log")
        command = {"label": label, "argv": argv, "cwd": str(cwd), "started_utc": utc(),
                   "timeout_seconds": timeout, "expected_output": list(expected),
                   "stdout": stdout.name, "stderr": stderr.name}
        self.receipt["commands"].append(command)
        self.save()
        print("RUN " + label, flush=True)
        began = time.monotonic()
        error = None
        with stdout.open("wb") as output, stderr.open("wb") as errors:
            try:
                result = subprocess.run(argv, cwd=cwd, env=self.env, stdout=output,
                                        stderr=errors, timeout=timeout, check=False)
                command["returncode"] = result.returncode
            except (OSError, subprocess.TimeoutExpired) as failure:
                error = str(failure)
                command["returncode"] = None
                command["execution_error"] = error
        command.update(ended_utc=utc(), elapsed_seconds=round(time.monotonic() - began, 6),
                       stdout_sha256=sha(stdout), stderr_sha256=sha(stderr))
        text = stdout.read_text(encoding="utf-8", errors="replace")
        diagnostics = stderr.read_text(encoding="utf-8", errors="replace")
        missing = [marker for marker in expected if marker not in text]
        warning = bool(jni and JNI_WARNING.search(text + "\n" + diagnostics))
        command["status"] = "PASS" if not error and command["returncode"] == 0 and not missing and not warning else "FAIL"
        command["missing_output"] = missing
        command["jni_or_ubsan_diagnostic"] = warning
        command["observed_check_counts"] = [int(value) for value in re.findall(r"\bchecks=(\d+)", text)]
        self.save()
        require(command["status"] == "PASS", label + " failed; see " + str(stdout) + " and " + str(stderr))
        return text, diagnostics


def snapshot(root, inventory, destination):
    for relative, record in inventory.items():
        source = regular_file(root, relative)
        require(sha(source) == record["sha256"], "source changed before snapshot: " + relative)
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        require(sha(target) == record["sha256"], "snapshot source mismatch: " + relative)


def compile_main(evidence, java_home, root, label):
    classes, headers = evidence.out / (label + "-classes"), evidence.out / (label + "-jni-headers")
    classes.mkdir()
    headers.mkdir()
    sources = sorted((root / JAVA).rglob("*.java"))
    require(sources, "no actual main Java sources")
    arguments = ["--release", "21", "-proc:none", "-Xlint:all,-module", "-Werror",
                 "-h", str(headers), "-d", str(classes), *map(str, sources)]
    argfile = evidence.out / (label + "-javac.args")
    # javac argument-file quoting, never shell evaluation.
    argfile.write_text("\n".join('"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'
                                 for value in arguments) + "\n", encoding="utf-8")
    evidence.run(label + "-strict-java21", [java_home / "bin/javac", "@" + str(argfile)], root)
    return classes, headers


def public_api(evidence, java_home, classes, label):
    names = sorted(path.relative_to(classes).with_suffix("").as_posix().replace("/", ".")
                   for path in classes.rglob("*.class") if path.name != "module-info.class")
    output, _ = evidence.run(label + "-javap", [java_home / "bin/javap", "-protected", "-s",
                                               "-constants", "-classpath", classes, *names], classes)
    blocks = re.split(r'(?m)^Compiled from "[^\n]*"\n', output)[1:]
    require(len(blocks) == len(names), "incomplete javap output for " + label)
    all_types = {}
    for name, block in zip(names, blocks):
        lines = [line.strip() for line in block.splitlines() if line.strip()]
        require(lines and lines[-1] == "}", "malformed javap type: " + name)
        if not lines[0].startswith(("public ", "protected ")):
            continue
        members = []
        for index, line in enumerate(lines[1:], 1):
            if line.startswith(("public ", "protected ")):
                descriptor = lines[index + 1] if index + 1 < len(lines) else ""
                require(descriptor.startswith("descriptor:"), "missing JVM descriptor: " + line)
                members.append(line + " " + descriptor)
        all_types[name] = {"declaration": lines[0], "members": sorted(members)}
    # A public nested declaration inside an inaccessible implementation class is not exported API.
    visible = {name: value for name, value in all_types.items()
               if all(name.rsplit("$", depth)[0] in all_types for depth in range(1, name.count("$") + 1))}
    require(visible, "no public/protected types inspected")
    write_json(evidence.out / (label + "-public-api.json"), visible)
    return visible


def compare_api(before, after):
    changes = {"removed_or_changed": [], "added": [], "added_types": sorted(after.keys() - before.keys())}
    for name, old in before.items():
        new = after.get(name)
        if new is None or new["declaration"] != old["declaration"]:
            changes["removed_or_changed"].append({"class": name, "declaration": old["declaration"]})
            continue
        for member in sorted(set(old["members"]) - set(new["members"])):
            changes["removed_or_changed"].append({"class": name, "member": member})
    for name, new in after.items():
        old = before.get(name)
        for member in sorted(set(new["members"]) - set(old["members"] if old else [])):
            changes["added"].append({"class": name, "member": member})
            if old and (" abstract " in member or (" interface " in old["declaration"] and
                                                    "(" in member and " static " not in member and
                                                    " default " not in member)):
                changes["removed_or_changed"].append({"class": name, "new_abstract_member": member})
    return changes


def jni_prototypes(headers, owner):
    path = headers / (PACKAGE + owner).replace(".", "_")
    path = path.with_suffix(".h")
    require(path.is_file(), "missing generated JNI header: " + str(path))
    declarations = re.findall(r"JNIEXPORT\s+(.+?)\s+JNICALL\s+(Java_\w+)\s*(\([^;]+\));",
                              path.read_text(encoding="utf-8"), re.S)
    require(declarations, "empty JNI prototypes: " + owner)
    return path, {symbol: " ".join((result + " " + arguments).split())
                  for result, symbol, arguments in declarations}


def native_build(evidence, java_home, root, headers, before_headers, native_dir, gcc, nm):
    native_dir.mkdir()
    descriptions = {}
    for owners, sources, library in (
            (("M3IndexPathMasks",), ("m3_index_path_masks.c",), "libm3_collections_pathmask.so"),
            (("M3IndexMaskCompactor", "M3IndexMaskWindow"),
             ("m3_index_mask_compactor.c", "m3_index_mask_window.c"), "libm3_collections_mask.so")):
        includes, expected, retained = [], {}, {}
        for owner in owners:
            header, prototypes = jni_prototypes(headers, owner)
            includes.extend(["-include", str(header)])
            expected.update(prototypes)
            if before_headers:
                _, old = jni_prototypes(before_headers, owner)
                retained.update(old)
                require(all(expected.get(symbol) == signature for symbol, signature in old.items()),
                        "existing JNI declaration changed: " + owner)
        if "M3IndexPathMasks" in owners:
            require({"Java_com_m3_collections_M3IndexPathMasks_selectNative",
                     "Java_com_m3_collections_M3IndexPathMasks_selectLongNative"} <= expected.keys(),
                    "old int/new long path-mask JNI declarations missing")
        binary = native_dir / library
        evidence.run(library + "-gcc-c17-ubsan", [gcc, "-std=c17", "-O2", "-Wall", "-Wextra",
                     "-Werror", "-fPIC", "-shared", "-fsanitize=undefined", "-fno-sanitize-recover=all",
                     "-fno-omit-frame-pointer", "-Wl,-z,defs", "-I" + str(java_home / "include"),
                     "-I" + str(java_home / "include/linux"), *includes,
                     *(root / NATIVE / source for source in sources), "-o", binary], root, jni=True)
        symbols, _ = evidence.run(library + "-nm", [nm, "-D", "--defined-only", binary], root)
        exported = {line.split()[-1] for line in symbols.splitlines()
                    if line.split() and line.split()[-1].startswith("Java_")}
        require(exported == expected.keys(), "JNI export/header mismatch: " + library)
        descriptions[library] = {"sha256": sha(binary), "prototypes": expected,
                                 "retained_preimage_prototypes": retained, "exports": sorted(exported)}
    write_json(evidence.out / "JNI_ABI.json", descriptions)


def surefire_results(module, expected_sources):
    reports = sorted((module / "target/surefire-reports").glob("TEST-*.xml"))
    require(reports, "Maven produced no Surefire test reports")
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    executed, artifacts = set(), {}
    for report in reports:
        suite = ET.parse(report).getroot()
        require(suite.tag == "testsuite", "unexpected Surefire report: " + str(report))
        for key in totals:
            totals[key] += int(suite.attrib.get(key, 0))
        for case in suite.findall("testcase"):
            if case.find("skipped") is None:
                executed.add(case.attrib.get("classname", "").split("$")[0])
        for item in suite.findall("properties/property"):
            if item.attrib.get("name") in {"java.class.path", "surefire.test.class.path"}:
                for part in item.attrib.get("value", "").split(os.pathsep):
                    artifact = Path(part)
                    if artifact.suffix == ".jar" and artifact.is_file():
                        artifacts[str(artifact.resolve())] = sha(artifact)
    require(totals["tests"] > 0 and totals["failures"] == totals["errors"] == totals["skipped"] == 0,
            "Maven tests failed or were skipped: " + repr(totals))
    expected = set()
    for source in expected_sources:
        text = source.read_text(encoding="utf-8")
        if re.search(r"@(Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b", text):
            relative = source.relative_to(module / "src/test/java").with_suffix("")
            expected.add(relative.as_posix().replace("/", "."))
    require(expected <= executed, "test owner absent from successful Surefire cases: " + repr(sorted(expected - executed)))
    return {"totals": totals, "executed_owners": sorted(executed),
            "reports": {report.name: sha(report) for report in reports}, "test_jars": artifacts}


def qualify(args, evidence):
    target, before = args.target.resolve(strict=True), args.preimage_tree
    before = before.resolve(strict=True) if before else None
    java_home = args.java_home.resolve(strict=True)
    require(sys.platform.startswith("linux"), "Linux GCC/JNI qualification required; no partial native PASS")
    manifest_path = (args.manifest or target / HERE / "SOURCE_MANIFEST.json").absolute()
    manifest, rows = read_manifest(manifest_path, target, before)
    evidence.receipt.update(source_manifest_sha256=sha(manifest_path), provenance=manifest["provenance"],
                            runner_sha256=sha(Path(__file__).resolve()), python=sys.version,
                            target=str(target), preimage_tree=str(before) if before else None)
    shutil.copyfile(manifest_path, evidence.out / "SOURCE_MANIFEST.json")
    initial = module_inputs(target)
    require(initial.keys() <= rows.keys(), "module build input omitted from manifest: " + repr(sorted(initial.keys() - rows.keys())))
    previous = module_inputs(before) if before else None
    if previous is not None:
        require(previous.keys() <= rows.keys(), "preimage module input omitted from manifest")
    write_json(evidence.out / "RECEIVED_INPUTS.json", initial)
    write_json(evidence.out / "PREIMAGE_INPUTS.json", previous)
    evidence.receipt["gates"]["source_admission"] = "PASS"
    tools = {name: java_home / "bin" / name for name in ("java", "javac", "javap")}
    tools.update(maven=Path(shutil.which(str(args.maven)) or str(args.maven)).resolve(strict=True),
                 gcc=Path(shutil.which("gcc") or "/unavailable/gcc"),
                 nm=Path(shutil.which("nm") or "/unavailable/nm"))
    for name in ("java", "javac", "javap"):
        stdout, stderr = evidence.run(name + "-version", [tools[name], "-version"], target)
        require(re.search(r"(?:version\s+\"|javac\s+|\A)21(?:\.|\"|\s|$)", stdout + stderr),
                name + " is not Java 21")
    evidence.run("maven-version", [tools["maven"], "--version"], target, expected=("Java version: 21",))
    evidence.run("gcc-version", [tools["gcc"], "--version"], target)
    evidence.run("nm-version", [tools["nm"], "--version"], target)
    toolchain = {name: {"path": str(path.resolve()), "sha256": sha(path.resolve())}
                 for name, path in tools.items()}
    toolchain["jdk_inputs"] = {name: sha(java_home / name) for name in
                                ("release", "include/jni.h", "include/linux/jni_md.h")}
    write_json(evidence.out / "TOOLCHAIN.json", toolchain)
    build_root = evidence.out / "module"
    snapshot(target, initial, build_root)
    after_classes, after_headers = compile_main(evidence, java_home, build_root, "after")
    after_api = public_api(evidence, java_home, after_classes, "after")
    before_headers = None
    if before:
        old_classes, before_headers = compile_main(evidence, java_home, before, "before")
        comparison = compare_api(public_api(evidence, java_home, old_classes, "before"), after_api)
        write_json(evidence.out / "API_COMPARISON.json", comparison)
        require(not comparison["removed_or_changed"], "public/protected API contract changed; see API_COMPARISON.json")
        evidence.receipt["gates"]["preimage_api"] = "EXISTING_API_RETAINED" if comparison["added"] or comparison["added_types"] else "UNCHANGED"
    else:
        evidence.receipt["gates"]["preimage_api"] = "NOT_RUN_NO_PREIMAGE_TREE"
    native_dir, missing_dir = evidence.out / "native", evidence.out / "native-absent"
    missing_dir.mkdir()
    native_build(evidence, java_home, build_root, after_headers, before_headers,
                 native_dir, tools["gcc"], tools["nm"])
    evidence.receipt["gates"]["jni_headers_exports_c17_ubsan_build"] = "PASS"
    module = build_root / MODULE
    maven = [tools["maven"], "-B", "-ntp", "-f", module / "pom.xml",
             "-Dmaven.repo.local=" + str(args.maven_repository.resolve()), "-DskipTests=false",
             "-Dmaven.test.skip=false", "-Dmaven.main.skip=false", "-DfailIfNoTests=true",
             '-DargLine=-ea -Xcheck:jni "-Djava.library.path=' + str(missing_dir) + '"']
    if args.offline:
        maven.append("--offline")
    evidence.run("collections-full-module-tests", [*maven, "test"], module, jni=True, timeout=1800)
    result = surefire_results(module, sorted((module / "src/test/java").rglob("*.java")))
    write_json(evidence.out / "MAVEN_TESTS.json", result)
    evidence.receipt["gates"]["maven_module_tests"] = result["totals"]
    classpath = os.pathsep.join(str(module / "target" / name) for name in ("test-classes", "classes"))

    def proof(name, arguments, markers, native=False, vm=()):
        evidence.run(name + ("-native" if native else "-java") + ("-" + "-".join(arguments) if arguments else ""),
                     [tools["java"], "-ea", "-Xcheck:jni", "-Xmx1g", *vm,
                      "-Djava.library.path=" + str(native_dir if native else missing_dir),
                      "-cp", classpath, PACKAGE + name, *arguments], module,
                     expected=markers, jni=True)

    for native in (False, True):
        for name, marker, tag in MASK_PROOFS:
            value = ("NATIVE_REQUIRED" if native else "JAVA") if tag == "backend=" else str(native).lower()
            proof(name, ["--native"] if native else [], (marker, tag + value), native)
        proof("M3OrderWindowProof", ["common", "jni"] if native else ["common"],
              ("ORDER_WINDOW checks=", "native=" + str(native).lower(), " PASS"), native)
    proof("M3IndexPathMaskAliasSelfTest", ["--missing"], ("PATH_MASK_ALIAS_PASS", "backend=NATIVE_REQUIRED"))
    for name, marker in WRAPPER_PROOFS:
        proof(name, [], (marker,), vm=("-Xss256k",) if name == "M3DeepReadOnlyCollectionSelfTest" else ())
    proof("M3OrderWindowProof", ["nullable"], ("NULL_REPRESENTATIVE checks=", " PASS"))
    evidence.receipt["gates"]["named_java_jni_contracts"] = "PASS"
    # Build/test tools may create output, but the admitted input bytes must stay exact.
    read_manifest(manifest_path, target, before)
    require(sha(manifest_path) == evidence.receipt["source_manifest_sha256"], "manifest changed during verification")
    require(module_inputs(target) == initial, "received module input set/bytes changed during verification")
    require(module_inputs(build_root) == initial, "Maven changed its copied module inputs")
    if before:
        require(module_inputs(before) == previous, "preimage inputs changed during verification")
    write_json(evidence.out / "FINAL_INPUTS.json", module_inputs(target))
    evidence.receipt["gates"]["final_source_recheck"] = "PASS"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", required=True, type=Path, help="received repository root")
    parser.add_argument("--java-home", required=True, type=Path)
    parser.add_argument("--maven", required=True, type=Path, help="Maven executable or executable name")
    parser.add_argument("--maven-repository", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path, help="new evidence directory")
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--preimage-tree", type=Path, help="exact repository-shaped baseline; enables API comparison")
    parser.add_argument("--manifest", type=Path, help="defaults to target verification/SOURCE_MANIFEST.json")
    args = parser.parse_args()
    require(not args.out.exists() and not args.out.is_symlink(), "--out must be a new directory")
    out = args.out.resolve()
    target = args.target.resolve(strict=True)
    require(not out.is_relative_to(target / MODULE / "src"), "--out may not be inside product sources")
    if args.preimage_tree:
        require(not out.is_relative_to(args.preimage_tree.resolve(strict=True)), "--out may not edit the preimage tree")
    out.mkdir(parents=True)
    args.out = out.resolve()
    env = dict(os.environ)
    removed = ("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "CLASSPATH",
               "MAVEN_ARGS", "MAVEN_OPTS", "LD_LIBRARY_PATH", "LD_PRELOAD")
    for name in removed:
        env.pop(name, None)
    env.update(JAVA_HOME=str(args.java_home.resolve(strict=True)), LC_ALL="C", LANG="C", TZ="UTC",
               UBSAN_OPTIONS="halt_on_error=1:print_stacktrace=1")
    env["PATH"] = str(args.java_home.resolve(strict=True) / "bin") + os.pathsep + env.get("PATH", "")
    evidence = Evidence(args.out, env, {key: str(value) if isinstance(value, Path) else value
                                      for key, value in vars(args).items()})
    evidence.receipt["controlled_environment"] = {key: env[key] for key in
                                                  ("JAVA_HOME", "LC_ALL", "LANG", "TZ", "UBSAN_OPTIONS")}
    evidence.receipt["removed_environment_keys"] = list(removed)
    evidence.receipt["gates"]["historical_donation_guard"] = "NOT_RUN_IN_THIS_SCOPED_RUNNER"
    try:
        qualify(args, evidence)
        evidence.receipt["status"] = "PASS_SCOPED" if args.preimage_tree else "PASS_SCOPED_API_NOT_RUN"
    except (Exception, KeyboardInterrupt) as failure:
        evidence.receipt.update(status="FAIL", failure_type=type(failure).__name__, failure=str(failure))
        print("FAIL " + str(failure), file=sys.stderr)
    finally:
        evidence.receipt["ended_utc"] = utc()
        try:
            evidence.receipt["artifacts"] = {path.relative_to(args.out).as_posix(): sha(path)
                                             for path in sorted(args.out.rglob("*"))
                                             if path.is_file() and path != args.out / "RECEIPT.json"}
        except OSError as failure:
            evidence.receipt.update(status="FAIL", artifact_receipt_error=str(failure))
        evidence.save()
    print(evidence.receipt["status"] + " " + str(args.out / "RECEIPT.json"), flush=True)
    return 1 if evidence.receipt["status"] == "FAIL" else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (Refusal, OSError, ValueError) as failure:
        print("FAIL " + str(failure), file=sys.stderr)
        sys.exit(1)
