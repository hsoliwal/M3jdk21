#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify the optional persistent collection modules with existing JNI/JMOD owners.

Source materialization belongs to the canonical Synexia recipes. This bounded
verifier never edits source and never generates a module descriptor. A fresh lint
generation invalidates downstream receipts while retaining all earlier evidence.
Use the same toolchain/dependency arguments for each separate --stage invocation.
"""
import argparse
import ast
import csv
import hashlib
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

STAGES = ("lint", "compile", "test", "runtime")
MODULES = ("org.pcollections", "com.m3.collections", "com.m3.pcollections")
DONOR_COMMIT = "1f2212e7bf935451ea421ca3d499ef3c96c45e5c"
DONOR_TEST_COUNT = 264
CONTRACTS = ("M3PVectorContract", "M3PHashContract", "M3PDerivedContract")
VIEW_CONTRACT = "M3PViewsContract"
RESOURCE_PROBE = "M3PResourceProbe"
JNI_PREFIX = "Java_com_m3_collections_M3IndexMaskCompactor_"
JNI_METHODS = {"selectNative", "rangeAbiNative", "selectRangeNative", "positionsAbiNative",
               "selectPositionsNative", "selectWindowNative", "selectLongWindowNative"}
DEPENDENCIES = {
    "junit": ("org/junit/platform/junit-platform-console-standalone/1.12.2/"
              "junit-platform-console-standalone-1.12.2.jar",
              "329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a"),
    "assertj": ("org/assertj/assertj-core/3.27.7/assertj-core-3.27.7.jar",
                "c4a445426c3c2861666863b842cc4ec7bbb1c4226fefd370b6d2fe83d6c4ff0f"),
}


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def fingerprint(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(",", ":"))
                          .encode("utf-8")).hexdigest()


def dependency(given, key):
    relative, expected = DEPENDENCIES[key]
    path = given if given is not None else Path.home() / ".m2/repository" / relative
    path = path.resolve(strict=True)
    if sha256(path) != expected:
        raise ValueError("Dependency byte pin mismatch: " + str(path))
    return path


def executable(name):
    found = shutil.which(name)
    if found is None:
        raise FileNotFoundError("Required executable unavailable: " + name)
    return Path(found).resolve(strict=True)


def donor_files(root):
    manifest = root / "DONOR_PROVENANCE.tsv"
    with manifest.open(encoding="utf-8", newline="") as source:
        rows = list(csv.DictReader((line for line in source if not line.startswith("#")), delimiter="\t"))
    if len(rows) != 53:
        raise ValueError("Pinned donor manifest must contain exactly 53 selected source/test/legal rows")
    seen = set()
    for row in rows:
        relative = row["target_path"]
        path = root / relative
        if (relative in seen or path.is_symlink()
                or not path.resolve(strict=True).is_relative_to(root)):
            raise ValueError("Duplicate, symlinked, or escaping donor path: " + relative)
        seen.add(relative)
        data = path.read_bytes()
        git_blob = hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()
        if (row["source_commit"] != DONOR_COMMIT or row["license_id"] != "MIT"
                or len(data) != int(row["bytes"]) or sha256(path) != row["sha256"]
                or git_blob != row["git_blob_sha1"]):
            raise ValueError("Donor provenance mismatch: " + relative)
    actual_java = {str(path.relative_to(root)) for directory in ("src/main/java", "src/test/java")
                   for path in (root / directory).rglob("*.java")}
    if actual_java != {path for path in seen if path.endswith(".java")}:
        raise ValueError("Donor Java inventory differs from the pinned selected tree")
    if (len(list((root / "src/main/java").rglob("*.java"))) != 34
            or len(list((root / "src/test/java").rglob("*.java"))) != 15):
        raise ValueError("Expected 34 donor production and 15 original test sources")
    return [manifest, *(root / path for path in sorted(seen))]


def artifact_files(directory):
    if directory.is_symlink() or not directory.is_dir():
        raise ValueError("Missing or symlinked artifact directory: " + str(directory))
    result = {}
    for path in sorted(directory.rglob("*")):
        if path.is_symlink():
            raise ValueError("Symlink in compiled artifact tree: " + str(path))
        if path.is_file():
            result[str(path.relative_to(directory))] = sha256(path)
    return result


def sealed_file(root, relative):
    path = root / relative
    if (path.is_symlink() or not path.is_file()
            or not path.resolve(strict=True).is_relative_to(root.resolve(strict=True))):
        raise ValueError("Missing, symlinked, or escaping artifact file: " + str(path))
    return sha256(path)


class Verifier:
    def __init__(self, arguments):
        self.repo = arguments.repo_root.resolve(strict=True)
        self.home = arguments.jdk_home.resolve(strict=True)
        self.out = arguments.out.resolve()
        self.pack = self.repo / "m3/collections/pcollections"
        self.donor = self.repo / "m3/vendor/third_party/pcollections"
        self.base = self.repo / "m3/collections"
        self.packager = self.repo / "m3/tooling/jmod-recipes/tools/jmod_pack.py"
        self.packager_helper = self.packager.with_name("jmod_image.py")
        self.old_build = self.repo / "m3/tooling/module-packs/collections/build.py"
        self.old_legal = self.old_build.parent / "legal"
        self.native_source = self.base / "src/main/native/collections/m3_index_mask_compactor.c"
        self.junit = dependency(arguments.junit_console, "junit")
        self.assertj = dependency(arguments.assertj, "assertj")
        self.cc = executable(os.environ.get("CC", "cc"))
        self.nm = executable("nm")
        self.objcopy = executable("objcopy")
        self.python = Path(sys.executable).resolve(strict=True)
        if not re.search(r'^JAVA_VERSION="21(?:[.+"-])',
                         (self.home / "release").read_text(encoding="utf-8"), re.MULTILINE):
            raise ValueError("Java 21 development toolchain required")
        if platform.system() != "Linux":
            raise ValueError("Existing JNI/JMOD qualification is currently scoped to Linux")
        protected = [self.home, self.donor, self.base / "src", self.pack / "src",
                     self.pack / "tools", self.old_build.parent, self.packager.parent]
        if (self.repo.is_relative_to(self.out)
                or any(self.out.is_relative_to(path) or path.is_relative_to(self.out) for path in protected)):
            raise ValueError("Output must be disjoint from source owners and the supplied JDK")
        self.sources = {
            "org.pcollections": sorted((self.donor / "src/main/java").rglob("*.java")),
            "com.m3.collections": sorted((self.base / "src/main/java").rglob("*.java")),
            "com.m3.pcollections": sorted((self.pack / "src/main/java").rglob("*.java")),
        }
        self.donor_tests = sorted((self.donor / "src/test/java").rglob("*.java"))
        self.m3_tests = sorted((self.pack / "src/test/java").rglob("*.java"))
        required_tests = {*CONTRACTS, VIEW_CONTRACT, RESOURCE_PROBE, "M3PCollectionsTest"}
        if not required_tests.issubset({path.stem for path in self.m3_tests}):
            raise ValueError("Complete contract, resource probe, and JUnit bridge sources required")
        for module, sources in self.sources.items():
            descriptors = [path for path in sources if path.name == "module-info.java"]
            if len(descriptors) != 1 or not re.search(r"\bmodule\s+" + re.escape(module) + r"\s*\{",
                                                     descriptors[0].read_text(encoding="utf-8")):
                raise ValueError("Missing exact named-module source owner: " + module)
        self.legal = sorted(self.old_legal.iterdir()) + [self.donor / "LICENSE.txt"]
        self.legal += [self.pack / name for name in ("LICENSE.txt", "NOTICE.txt", "THIRD_PARTY_NOTICES.md")]
        self.poms = [self.donor / "pom.xml", self.base / "pom.xml", self.pack / "pom.xml",
                     self.pack / "reactor/pom.xml"]
        self.metadata = [self.pack / name for name in
                         ("provenance.json", "name-mapping.json", "synexia-donation.json")]
        self.environment = os.environ.copy()
        for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH",
                    "LD_LIBRARY_PATH", "LD_PRELOAD"):
            self.environment.pop(key, None)
        self.environment["LC_ALL"] = "C.UTF-8"
        self.environment["PYTHONDONTWRITEBYTECODE"] = "1"
        self.generation = None
        self.stage_dir = None
        self.sequence = 0

    def inputs(self):
        for module, sources in self.sources.items():
            root = next(path.parent for path in sources if path.name == "module-info.java")
            if sorted(root.rglob("*.java")) != sources:
                raise ValueError("Production source inventory changed during invocation: " + module)
        if (sorted((self.donor / "src/test/java").rglob("*.java")) != self.donor_tests
                or sorted((self.pack / "src/test/java").rglob("*.java")) != self.m3_tests):
            raise ValueError("Test source inventory changed during invocation")
        paths = donor_files(self.donor)
        paths += [path for sources in self.sources.values() for path in sources]
        paths += self.m3_tests + self.donor_tests + self.legal + self.poms + self.metadata
        paths += [Path(__file__).resolve(), self.native_source, self.packager, self.packager_helper,
                  self.old_build, self.junit, self.assertj, self.cc, self.nm, self.objcopy, self.python,
                  self.home / "release", self.home / "lib/modules", self.home / "include/jni.h",
                  self.home / "include/linux/jni_md.h"]
        paths += [self.home / "bin" / tool for tool in ("java", "javac", "jar", "jmod", "jlink", "javap")]
        paths += sorted((self.home / "jmods").glob("*.jmod"))
        locked = {str(path.resolve(strict=True)): sha256(path) for path in sorted(set(paths))}
        return {"files": locked, "repo": str(self.repo), "jdk": str(self.home),
                "python": str(self.python), "platform": platform.platform(),
                "java_source_counts": {name: len(sources) for name, sources in self.sources.items()},
                "donor_test_sources": len(self.donor_tests), "m3_test_sources": len(self.m3_tests),
                "environment_policy": "remove Java option/classpath injection and LD library overrides; LC_ALL=C.UTF-8"}

    def command(self, label, *arguments, expected=None):
        self.sequence += 1
        argv = [str(argument) for argument in arguments]
        prefix = self.stage_dir / (f"{self.sequence:03d}-" + label)
        output_log = Path(str(prefix) + ".log")
        command_log = Path(str(prefix) + ".command.json")
        print("[" + self.stage_dir.name + "] " + label, flush=True)
        record = {"argv": argv, "cwd": str(self.repo), "timeout_seconds": 600}
        try:
            result = subprocess.run(argv, cwd=self.repo, env=self.environment,
                                    stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                    text=True, encoding="utf-8", errors="replace", timeout=600)
            record["returncode"] = result.returncode
            output = result.stdout
        except subprocess.TimeoutExpired as failure:
            output = failure.stdout or ""
            if isinstance(output, bytes):
                output = output.decode("utf-8", errors="replace")
            record.update({"returncode": None, "timeout": True})
            output_log.write_text(output, encoding="utf-8")
            write_json(command_log, record)
            raise RuntimeError("Command timed out: " + str(prefix)) from failure
        except OSError as failure:
            record.update({"returncode": None, "launch_error": str(failure)})
            output_log.write_text(str(failure) + "\n", encoding="utf-8")
            write_json(command_log, record)
            raise RuntimeError("Command could not start: " + str(prefix)) from failure
        output_log.write_text(output, encoding="utf-8")
        write_json(command_log, record)
        if result.returncode != 0:
            raise RuntimeError("Command failed (" + str(result.returncode) + "): " + str(prefix))
        if expected is not None and expected not in output:
            raise RuntimeError("Missing proof marker " + repr(expected) + ": " + str(prefix))
        return output

    def guard(self, stage):
        state = json.loads((self.out / "ACTIVE.json").read_text(encoding="utf-8"))
        self.generation = self.out / state["generation"]
        if not self.generation.resolve(strict=True).is_relative_to(self.out):
            raise ValueError("Invalid active generation path")
        current = self.inputs()
        if fingerprint(current) != state["inputs_sha256"]:
            raise ValueError("Inputs changed after lint; restart --stage lint (earlier evidence is retained)")
        for predecessor in STAGES[:STAGES.index(stage)]:
            path = self.generation / predecessor / "RECEIPT.json"
            receipt = json.loads(path.read_text(encoding="utf-8"))
            if receipt.get("status") != "PASS" or receipt.get("inputs_sha256") != state["inputs_sha256"]:
                raise ValueError("Required predecessor did not pass: " + predecessor)
        if stage in ("test", "runtime"):
            compile_root = self.generation / "compile"
            if (compile_root.is_symlink() or not compile_root.is_dir()
                    or not compile_root.resolve(strict=True).is_relative_to(self.generation.resolve(strict=True))):
                raise ValueError("Missing, symlinked, or escaping compile artifact root")
            manifest = compile_root / "ARTIFACTS.json"
            receipt = json.loads((compile_root / "RECEIPT.json").read_text(encoding="utf-8"))
            if sha256(manifest) != receipt.get("artifacts_manifest_sha256"):
                raise ValueError("Compiled artifact manifest changed; restart --stage lint")
            locked = json.loads(manifest.read_text(encoding="utf-8"))
            actual = {name: ({path: sealed_file(compile_root, path) for path in files}
                             if name == "." else artifact_files(compile_root / name))
                      for name, files in locked.items()}
            if actual != locked:
                raise ValueError("Compiled artifacts changed; restart --stage lint")
        return current

    def run_stage(self, stage):
        self.out.mkdir(parents=True, exist_ok=True)
        if stage == "lint":
            count = 1
            while (self.out / f"generation-{count:04d}").exists():
                count += 1
            self.generation = self.out / f"generation-{count:04d}"
            self.generation.mkdir()
            # Invalidate older successes before reading potentially changed input data.
            write_json(self.out / "ACTIVE.json", {"generation": self.generation.name, "inputs_sha256": None})
            try:
                current = self.inputs()
            except Exception as failure:
                refused = self.generation / "lint"
                refused.mkdir()
                write_json(refused / "RECEIPT.json", {"stage": "lint", "status": "FAIL",
                                                      "commands": 0, "reason": str(failure)})
                raise
            write_json(self.generation / "INPUTS.json", current)
            write_json(self.out / "ACTIVE.json", {"generation": self.generation.name,
                                                  "inputs_sha256": fingerprint(current)})
        else:
            try:
                current = self.guard(stage)
            except Exception as failure:
                with (self.out / "GUARD_EVENTS.jsonl").open("a", encoding="utf-8") as log:
                    log.write(json.dumps({"stage": stage, "status": "REFUSED", "reason": str(failure)}) + "\n")
                raise
        self.stage_dir = self.generation / stage
        if self.stage_dir.exists():
            raise FileExistsError("Stage already attempted; restart lint to retain the earlier receipt")
        self.stage_dir.mkdir()
        self.sequence = 0
        receipt = {"stage": stage, "status": "RUNNING", "inputs_sha256": fingerprint(current)}
        write_json(self.stage_dir / "RECEIPT.json", receipt)
        try:
            getattr(self, "stage_" + stage)()
            if self.inputs() != current:
                raise ValueError("Inputs changed during " + stage)
            if stage in ("test", "runtime"):
                self.guard(stage)
            if stage == "compile":
                receipt["artifacts_manifest_sha256"] = sha256(self.stage_dir / "ARTIFACTS.json")
            receipt.update({"status": "PASS", "commands": self.sequence})
        except Exception as failure:
            receipt.update({"status": "FAIL", "commands": self.sequence, "reason": str(failure)})
            write_json(self.stage_dir / "RECEIPT.json", receipt)
            raise
        write_json(self.stage_dir / "RECEIPT.json", receipt)
        print("M3_PCOLLECTIONS_" + stage.upper() + "_PASS generation=" + self.generation.name, flush=True)

    def stage_lint(self):
        for script in (Path(__file__), self.packager, self.packager_helper, self.old_build):
            ast.parse(script.read_text(encoding="utf-8"), filename=str(script))
        for pom in self.poms:
            document = ElementTree.parse(pom)
            if document.getroot().tag != "{http://maven.apache.org/POM/4.0.0}project":
                raise ValueError("Maven project XML namespace/root mismatch: " + str(pom))
        for metadata in self.metadata:
            if not isinstance(json.loads(metadata.read_text(encoding="utf-8")), dict):
                raise ValueError("Scoped metadata must be a JSON object: " + str(metadata))
        new_sources = self.sources["com.m3.pcollections"] + self.m3_tests
        for source in new_sources:
            data = source.read_bytes()
            text = data.decode("utf-8")
            if b"\0" in data or not data.endswith(b"\n") or "SPDX-License-Identifier: Apache-2.0" not in text:
                raise ValueError("New Java source hygiene failure: " + str(source))
        owner = self.base / "src/main/java/com/m3/collections/M3IndexMaskCompactor.java"
        declarations = set(re.findall(r"\bnative\s+[\w\[\]]+\s+(\w+)\s*\(", owner.read_text(encoding="utf-8")))
        exports = set(re.findall(r"\bJava_\w+(?=\s*\()", self.native_source.read_text(encoding="utf-8")))
        if declarations != JNI_METHODS or exports != {JNI_PREFIX + name for name in JNI_METHODS}:
            raise ValueError("Existing Java/C native declaration set changed")
        self.command("native-syntax", self.cc, "-std=c17", "-Wall", "-Wextra", "-Werror",
                     "-fsyntax-only", "-I" + str(self.home / "include"),
                     "-I" + str(self.home / "include/linux"), self.native_source)
        write_json(self.stage_dir / "SOURCE_LINT.json", {
            "donor_revision": DONOR_COMMIT, "donor_selected_rows": 53,
            "donor_bytes_and_git_blob_ids": "PASS", "new_java_sources": len(new_sources),
            "native_exports": sorted(exports), "python_ast": "PASS",
            "maven_xml_files": [str(path.relative_to(self.repo)) for path in self.poms],
            "scoped_json_files": [str(path.relative_to(self.repo)) for path in self.metadata],
            "java_language_lint": "javac -Xlint is executed in the following compile phase"})

    def stage_compile(self):
        modules = self.stage_dir / "modules"
        headers = self.stage_dir / "headers"
        jars = self.stage_dir / "jars"
        native = self.stage_dir / "native"
        legal = self.stage_dir / "legal"
        for directory in (modules, headers, jars, native, legal, self.stage_dir / "empty-native"):
            directory.mkdir()
        for module in MODULES:
            output = modules / module
            options = ["--release", "21", "-proc:none", "-Xlint:all,-module"]
            # Exact third-party bytes retain their original warning surface.
            if module != "org.pcollections":
                options.append("-Werror")
            self.command(module + "-javac", self.home / "bin/javac", *options,
                         "--module-path", modules, "-h", headers, "-d", output, *self.sources[module])
        classpath = os.pathsep.join(str(modules / name) for name in MODULES)
        donor_classpath = os.pathsep.join((classpath, str(self.junit), str(self.assertj)))
        self.command("donor-tests-javac", self.home / "bin/javac", "--release", "21",
                     "-proc:none", "-Xlint:all,-module", "-cp", donor_classpath,
                     "-d", self.stage_dir / "donor-tests", *self.donor_tests)
        self.command("m3-tests-and-probe-javac", self.home / "bin/javac", "--release", "21",
                     "--add-modules", "java.management,jdk.management", "-proc:none",
                     "-Xlint:all,-module", "-Werror", "-cp", donor_classpath,
                     "-d", self.stage_dir / "m3-tests", *self.m3_tests)
        header = headers / "com_m3_collections_M3IndexMaskCompactor.h"
        header_exports = set(re.findall(r"\bJava_\w+(?=\s*\()", header.read_text(encoding="utf-8")))
        expected = {JNI_PREFIX + name for name in JNI_METHODS}
        if header_exports != expected:
            raise ValueError("Generated JNI header differs from the frozen existing export set")
        library = native / "libm3_collections_mask.so"
        self.command("native-shared-library", self.cc, "-std=c17", "-O2", "-fPIC", "-shared",
                     "-Wall", "-Wextra", "-Werror", "-I" + str(self.home / "include"),
                     "-I" + str(self.home / "include/linux"), "-include", header,
                     "-o", library, self.native_source)
        symbols = self.command("native-exported-symbols", self.nm, "-D", "--defined-only", library)
        exported = {line.split()[-1] for line in symbols.splitlines()
                    if line.split() and line.split()[-1].startswith("Java_")}
        if exported != expected:
            raise ValueError("Compiled native export set differs from javac-generated declarations")
        self.command("native-strip-debug-reference", self.objcopy, "--strip-debug", library,
                     self.stage_dir / "native-strip-debug-reference.so")
        for module in MODULES:
            jar = jars / (module + ".jar")
            self.command(module + "-jar", self.home / "bin/jar", "--create", "--file", jar,
                         "--date", "2026-01-01T00:00:00Z", "-C", modules / module, ".")
            self.command(module + "-descriptor", self.home / "bin/jar", "--describe-module",
                         "--file", jar, expected=module)
            output = modules / module
            classfiles = sorted(output.rglob("*.class"))
            types = sorted(".".join(path.relative_to(output).with_suffix("").parts)
                           for path in classfiles
                           if path.name not in ("module-info.class", "package-info.class"))
            if not types:
                raise ValueError("Compiled module has no inspectable class files: " + module)
            source_root = next(path.parent for path in self.sources[module]
                               if path.name == "module-info.java")
            package_sources = [path.relative_to(source_root)
                               for path in self.sources[module] if path.name == "package-info.java"]
            write_json(self.stage_dir / (module + "-class-inventory.json"), {
                "module": module, "source_file_count": len(self.sources[module]),
                "classfile_count": len(classfiles),
                "classfiles": {str(path.relative_to(output)): sha256(path) for path in classfiles},
                "descriptor_classfiles": [str(path.relative_to(output)) for path in classfiles
                                          if path.name in ("module-info.class", "package-info.class")],
                "package_info_sources": [str(path) for path in package_sources],
                "unemitted_package_info_sources": [str(path) for path in package_sources
                                                  if not (output / path.with_suffix(".class")).is_file()],
                "javap_type_count": len(types), "javap_binary_names": types})
            self.command(module + "-public-surface", self.home / "bin/javap", "--module-path", modules,
                         "--module", module, "-public", *types)
        for source in self.legal:
            if source.parent == self.donor:
                name = "PCOLLECTIONS-LICENSE.txt"
            elif source.parent == self.pack:
                name = "M3_PCOLLECTIONS_" + source.name
            else:
                name = source.name
            shutil.copyfile(source, legal / name)
        rows = ["artifact\tversion\tjar\tmodule\trequires\tdisposition"]
        for module in MODULES:
            requires = "java.base"
            if module == "com.m3.pcollections":
                requires += ",org.pcollections,com.m3.collections"
            rows.append("\t".join((module, "1", str(jars / (module + ".jar")), module, requires, "JMOD")))
        (self.stage_dir / "INPUT_JMOD_IMAGE.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8")
        (self.stage_dir / "EMPTY_DESCRIPTOR_MANIFEST.tsv").write_text(
            "module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n", encoding="utf-8")
        self.verify_pack_inputs(self.stage_dir)
        write_json(self.stage_dir / "JNI_ABI.json", {"generated_header_sha256": sha256(header),
                                                    "native_sha256": sha256(library),
                                                    "exports": sorted(exported)})
        locked = {name: artifact_files(self.stage_dir / name)
                  for name in ("modules", "headers", "jars", "native", "legal", "donor-tests", "m3-tests", "empty-native")}
        locked["."] = {name: sha256(self.stage_dir / name) for name in
                       ("INPUT_JMOD_IMAGE.tsv", "EMPTY_DESCRIPTOR_MANIFEST.tsv", "native-strip-debug-reference.so",
                        *(module + "-class-inventory.json" for module in MODULES))}
        write_json(self.stage_dir / "ARTIFACTS.json", locked)

    def verify_pack_inputs(self, compiled):
        for name in ("jars", "native", "legal", "empty-native"):
            artifact_files(compiled / name)
        if any((compiled / "empty-native").iterdir()):
            raise ValueError("Native-absence search directory must remain empty")
        for name in ("INPUT_JMOD_IMAGE.tsv", "EMPTY_DESCRIPTOR_MANIFEST.tsv"):
            sealed_file(compiled, name)
        with (compiled / "INPUT_JMOD_IMAGE.tsv").open(encoding="utf-8", newline="") as source:
            rows = list(csv.DictReader(source, delimiter="\t"))
        if len(rows) != len(MODULES) or {row["module"] for row in rows} != set(MODULES):
            raise ValueError("JMOD inputs must contain exactly the three compiled collection modules")
        for row in rows:
            expected = compiled / "jars" / (row["module"] + ".jar")
            if (row["jar"] != str(expected) or row["artifact"] != row["module"]
                    or row["version"] != "1" or row["disposition"] != "JMOD"):
                raise ValueError("JMOD input path or descriptor disposition escaped its compiled owner")
            sealed_file(compiled, str(expected.relative_to(compiled)))
        with (compiled / "EMPTY_DESCRIPTOR_MANIFEST.tsv").open(encoding="utf-8", newline="") as source:
            if list(csv.DictReader(source, delimiter="\t")):
                raise ValueError("Published modules require an empty descriptor-generation manifest")

    def classpath(self):
        compiled = self.generation / "compile"
        return os.pathsep.join([str(compiled / "modules" / module) for module in MODULES]
                              + [str(compiled / "m3-tests")])

    def donor_suite(self, java, label, linked=False):
        compiled = self.generation / "compile"
        paths = [self.junit, self.assertj, compiled / "donor-tests"]
        options = ["--add-modules", "org.pcollections"] if linked else []
        if not linked:
            paths.append(compiled / "modules/org.pcollections")
        output = self.command(label + "-original-donor-suite", java, *options, "-cp",
                              os.pathsep.join(map(str, paths)), "org.junit.platform.console.ConsoleLauncher",
                              "execute", "--select-package", "org.pcollections.tests",
                              "--include-engine", "junit-vintage", "--include-engine", "junit-jupiter",
                              "--disable-ansi-colors", "--details", "summary", "--fail-if-no-tests",
                              "--reports-dir", self.stage_dir / (label + "-junit"))
        counts = {}
        for kind in ("found", "started", "successful", "failed", "skipped", "aborted"):
            match = re.search(r"\[\s*(\d+) tests " + kind + r"\s*\]", output)
            if match is None:
                raise ValueError("Missing donor test count: " + kind)
            counts[kind] = int(match.group(1))
        if (any(counts[kind] != DONOR_TEST_COUNT for kind in ("found", "started", "successful"))
                or any(counts[kind] != 0 for kind in ("failed", "skipped", "aborted"))):
            raise ValueError("Original donor suite count/outcome drift: " + repr(counts))
        write_json(self.stage_dir / (label + "-donor-counts.json"), counts)

    def main_test(self, java, name, label, mode=None, linked=False, native=False):
        compiled = self.generation / "compile"
        options = ["-Xcheck:jni"]
        if not linked:
            options += ["-Djava.library.path=" + str(compiled / ("native" if native else "empty-native"))]
        if linked:
            options += ["--add-modules", "com.m3.pcollections,java.management,jdk.management",
                        "--add-reads", "com.m3.pcollections=java.management",
                        "--add-reads", "com.m3.pcollections=jdk.management",
                        "--patch-module", "com.m3.pcollections=" + str(compiled / "m3-tests"),
                        "-m", "com.m3.pcollections/com.m3.pcollections." + name]
        else:
            options += ["-cp", self.classpath(), "com.m3.pcollections." + name]
        marker = name + (" PASS mode=" + mode if name == VIEW_CONTRACT else " PASS")
        return self.command(label + "-" + name + ("-" + mode.lstrip("-") if mode else ""),
                            java, *options, *([mode] if mode else []), expected=marker)

    def stage_test(self):
        java = self.home / "bin/java"
        self.donor_suite(java, "host")
        for name in CONTRACTS:
            self.main_test(java, name, "host")
        self.main_test(java, VIEW_CONTRACT, "host", "--java")
        self.main_test(java, VIEW_CONTRACT, "host", "--absent")

    def resource_probe(self, java, label, mode, linked):
        compiled = self.generation / "compile"
        options = ["-Xcheck:jni"]
        if not linked:
            options += ["--module-path", str(compiled / "modules"), "-Djava.library.path="
                        + str(compiled / ("native" if mode == "--native" else "empty-native"))]
        options += ["--add-modules", "com.m3.pcollections,java.management,jdk.management",
                    "--add-reads", "com.m3.pcollections=java.management",
                    "--add-reads", "com.m3.pcollections=jdk.management",
                    "--patch-module", "com.m3.pcollections=" + str(compiled / "m3-tests"),
                    "-m", "com.m3.pcollections/com.m3.pcollections." + RESOURCE_PROBE, mode]
        output = self.command(label + "-resource-" + mode.lstrip("-"), java, *options)
        records = [json.loads(line) for line in output.splitlines() if line.startswith("{")]
        environments = [record for record in records if record.get("environment")]
        if len(environments) != 1 or any(environments[0].get(key) != module for key, module in
                                       (("m3Module", "com.m3.pcollections"),
                                        ("donorModule", "org.pcollections"),
                                        ("jniOwnerModule", "com.m3.collections"))):
            raise ValueError("Resource probe did not verify all three named module identities")
        scenarios = {}
        for record in records:
            if "scenario" in record:
                name = re.sub(r"-(?:JAVA|NATIVE_REQUIRED)$", "", record["scenario"])
                if name in scenarios:
                    raise ValueError("Duplicate resource scenario: " + name)
                scenarios[name] = record["digest"]
        expected_names = {"m3-vector-branching", "donor-vector-branching", "jdk-copy-branching",
                          "index-mask-selection"}
        if set(scenarios) != expected_names:
            raise ValueError("Resource scenario inventory changed")
        write_json(self.stage_dir / (label + "-resource-" + mode.lstrip("-") + ".json"), records)
        return scenarios

    def stage_runtime(self):
        compiled = self.generation / "compile"
        self.verify_pack_inputs(compiled)
        java = self.home / "bin/java"
        self.main_test(java, VIEW_CONTRACT, "host", "--native", native=True)
        observed = [self.resource_probe(java, "host", mode, False) for mode in ("--java", "--native")]
        self.command("existing-jmod-packager", self.python, self.packager,
                     "--jdk-home", self.home, "--image-tsv", compiled / "INPUT_JMOD_IMAGE.tsv",
                     "--manifest", compiled / "EMPTY_DESCRIPTOR_MANIFEST.tsv", "--layer-root", self.repo,
                     "--out", self.stage_dir / "pack", "--legal-notices", compiled / "legal",
                     "--native-library", "com.m3.collections=" + str(compiled / "native"),
                     "--add-modules", "java.se,jdk.management,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps")
        image = self.stage_dir / "pack/image"
        linked_library = image / "lib/libm3_collections_mask.so"
        if sha256(linked_library) != sha256(compiled / "native-strip-debug-reference.so"):
            raise ValueError("Linked JNI bytes differ from the exact objcopy --strip-debug reference")
        image_java = image / "bin/java"
        self.command("linked-validate-modules", image_java, "--validate-modules")
        listed = self.command("linked-list-modules", image_java, "--list-modules")
        names = {line.split("@")[0] for line in listed.splitlines()}
        if not set(MODULES).issubset(names):
            raise ValueError("Linked image is missing a collection module")
        self.donor_suite(image_java, "image", linked=True)
        for name in CONTRACTS:
            self.main_test(image_java, name, "image", linked=True)
        for mode in ("--java", "--native"):
            self.main_test(image_java, VIEW_CONTRACT, "image", mode, linked=True)
            observed.append(self.resource_probe(image_java, "image", mode, True))
        if any(value != observed[0] for value in observed[1:]):
            raise ValueError("Host/image or Java/native logical resource digests differ")
        write_json(self.stage_dir / "QUALIFICATION.json", {
            "status": "PASS", "modules": list(MODULES), "donor_revision": DONOR_COMMIT,
            "original_donor_tests_per_environment": DONOR_TEST_COUNT,
            "native_exports": sorted(JNI_PREFIX + name for name in JNI_METHODS),
            "native_sha256": sha256(compiled / "native/libm3_collections_mask.so"),
            "linked_native_sha256": sha256(linked_library), "linked_image": str(image),
            "source_and_compile_artifacts_guarded": True, "host_and_linked_digests": observed[0],
            "metrics": "Descriptive wall/CPU/current-thread allocation; no speed threshold or universal saving claim",
            "scope": "Linux standalone source modules and JNI/JMOD image; not a full M3JDK/HotSpot or jtreg qualification"})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[4])
    parser.add_argument("--stage", choices=(*STAGES, "all"), default="all")
    parser.add_argument("--junit-console", type=Path, help="Pinned JUnit Console 1.12.2; defaults to its Maven cache path")
    parser.add_argument("--assertj", type=Path, help="Pinned AssertJ 3.27.7; defaults to its Maven cache path")
    arguments = parser.parse_args()
    verifier = Verifier(arguments)
    for stage in STAGES if arguments.stage == "all" else (arguments.stage,):
        verifier.run_stage(stage)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print("M3_PCOLLECTIONS_REFUSED: " + str(error), file=sys.stderr)
        sys.exit(1)
