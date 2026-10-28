#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Qualify complete Jackson source JMODs through four explicit ordered stages.

The receiving contract is documented in pom.xml and the pack's README. Invoke
lint, compile, test and runtime separately with identical arguments and a fresh
--out for each source revision. PASS receipts are immutable and chained; source
or compiled-input drift, a failed attempt, and skipped/replayed stages refuse.

Compile copies the exact four vendor trees below --out and uses their unchanged
Maven POMs. Maven JAR entry contents are retained byte for byte; the downstream
archive copies use sorted entries and a fixed ZIP timestamp for reproducibility.
Their original archive hashes and normalized hashes are both retained. Runtime
uses the existing jmod_pack.py release-21 flattening implementation and verifies
the selected donor class/resource bytes in the resulting JMODs. Module-info is
verified by the JDK descriptor tool because jmod may add descriptor attributes.

Only --out and the explicitly supplied --maven-repo may receive build output.
The driver does not set HOME or consult its .m2 directory. Every child command
has separate argument, stdout, stderr and exit receipts. Java fixtures compile
in compile and execute only in test/runtime; runtime never compiles Java.
"""

import argparse
import ast
import csv
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import traceback
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path, PurePosixPath


STAGES = ("lint", "compile", "test", "runtime")
VENDORS = (
    "fastdoubleparser-1.0.90",
    "jackson-annotations-2.19.2",
    "jackson-core-2.19.2",
    "jackson-databind-2.19.2",
)
DONOR_MODULES = (
    ("jackson-annotations", "com.fasterxml.jackson.annotation"),
    ("jackson-core", "com.fasterxml.jackson.core"),
    ("jackson-databind", "com.fasterxml.jackson.databind"),
)
RECEIVER_MODULE = "jdk.m3.jackson"
CONTRACT_MAIN = "jdk.m3.jackson.tests.M3JacksonContract"
CONTRACT_MARKER = "M3_JACKSON_CONTRACT_PASS"
# Test-only retained-cursor assertions; no module-info or consumer runtime opening.
CONTRACT_OPENS = (
    "--add-opens", "jdk.m3.jackson/jdk.m3.jackson=ALL-UNNAMED",
    "--add-opens", "com.fasterxml.jackson.core/com.fasterxml.jackson.core.base=ALL-UNNAMED",
)
DEVELOPER_MODULES = ("java.se", "jdk.compiler", "jdk.jlink", "jdk.jartool", "jdk.jdeps")
ARCHIVE_DATE = "2026-01-01T00:00:00Z"
ZIP_DATE = (2026, 1, 1, 0, 0, 0)
IGNORED_DIRECTORIES = {".git", "__pycache__", "target"}
IGNORED_FILES = {"dependency-reduced-pom.xml"}


def digest(path):
    """Hash file contents without admitting path or timestamp as file identity."""
    result = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            result.update(block)
    return result.hexdigest()


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode("ascii")


def value_digest(value):
    return hashlib.sha256(canonical(value)).hexdigest()


def write_json(path, value):
    """Receipts are append-only files; existing files never become new evidence."""
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        stream.write(json.dumps(value, sort_keys=True, indent=2, ensure_ascii=True) + "\n")


def regular_files(directory, ignore_generated=True):
    """Deterministic complete tree inventory; symlinks are refused, never followed."""
    if not directory.is_dir() or directory.is_symlink():
        raise ValueError("regular source directory required: " + str(directory))
    result = []
    for parent, directories, files in os.walk(directory, followlinks=False):
        base = Path(parent)
        for name in directories + files:
            if (base / name).is_symlink():
                raise ValueError("symlink input refused: " + str(base / name))
        directories[:] = sorted(name for name in directories
                                if not ignore_generated or name not in IGNORED_DIRECTORIES)
        for name in sorted(files):
            if ignore_generated and (name in IGNORED_FILES or name.endswith(".pyc")):
                continue
            path = base / name
            if not path.is_file():
                raise ValueError("non-regular input refused: " + str(path))
            result.append(path)
    return sorted(result)


def tree_manifest(directory, prefix, ignore_generated=True):
    return {prefix + "/" + path.relative_to(directory).as_posix(): digest(path)
            for path in regular_files(directory, ignore_generated)}


def safe_entry(name):
    path = PurePosixPath(name)
    if (not name or "\\" in name or path.is_absolute() or ".." in path.parts
            or any("\x00" in part for part in path.parts)):
        raise ValueError("unsafe ZIP entry refused: " + repr(name))
    return name


def jar_view(path):
    """Hash the same highest-release-at-most-21 view used by the existing packager."""
    selected = {}
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise ValueError("duplicate ZIP entries refused: " + str(path))
        for name in sorted(names):
            safe_entry(name)
            if name.endswith("/"):
                continue
            logical, release = name, 0
            if name.startswith("META-INF/versions/"):
                parts = name.split("/", 3)
                if len(parts) != 4 or not parts[2].isdigit():
                    raise ValueError("malformed multi-release path: " + name)
                release, logical = int(parts[2]), parts[3]
                if release > 21:
                    continue
            prior = selected.get(logical)
            if prior is None or release > prior[0]:
                selected[logical] = (release, hashlib.sha256(archive.read(name)).hexdigest())
    return {name: {"release": release, "sha256": checksum}
            for name, (release, checksum) in sorted(selected.items())}


def normalized_jar(source, destination):
    """Normalize only ZIP metadata; retain every non-directory entry's exact bytes."""
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(destination, "x") as result:
        names = original.namelist()
        if len(names) != len(set(names)):
            raise ValueError("duplicate donor JAR entries refused")
        for name in sorted(names):
            safe_entry(name)
            if not name.endswith("/"):
                info = zipfile.ZipInfo(name, ZIP_DATE)
                info.compress_type = zipfile.ZIP_STORED
                info.create_system = 3
                info.external_attr = 0o100644 << 16
                result.writestr(info, original.read(name))
    if jar_view(source) != jar_view(destination):
        raise ValueError("archive normalization changed the Java 21 content view")


class Run:
    """One immutable source epoch and its ordered qualification receipts."""

    def __init__(self, args):
        self.args = args
        self.pack = Path(__file__).resolve().parent
        self.repo = args.repo_root.resolve(strict=True)
        self.jdk = args.jdk_home.resolve(strict=True)
        self.maven = args.maven.resolve(strict=True)
        self.maven_repo = args.maven_repo.resolve()
        self.maven_settings = (args.maven_settings.resolve(strict=True)
                               if args.maven_settings is not None else None)
        if self.maven_settings is not None and not self.maven_settings.is_file():
            raise ValueError("Maven settings must be a regular file")
        self.java_trust_store = (args.java_trust_store.resolve(strict=True)
                                 if args.java_trust_store is not None else None)
        if self.java_trust_store is not None and not self.java_trust_store.is_file():
            raise ValueError("Java trust store must be a regular file")
        if self.java_trust_store is not None and not re.fullmatch(r"[A-Za-z0-9_./:+-]+", str(self.java_trust_store)):
            raise ValueError("Maven JVM trust-store path must contain only simple path characters")
        self.output = args.out.resolve()
        self.vendor = self.repo / "m3/vendor/third_party"
        self.packager = self.repo / "m3/tooling/jmod-recipes/tools/jmod_pack.py"
        self.flatten_owner = self.packager.with_name("jmod_image.py")
        self.command_number = 0
        self.stage = args.stage
        if args.timeout_seconds <= 0:
            raise ValueError("positive command timeout required")
        release = self.jdk / "release"
        if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', release.read_text(encoding="utf-8"), re.M):
            raise ValueError("a complete Java 21 development toolchain is required")
        self.tools = {}
        for name in ("java", "javac", "jar", "jmod", "jlink", "jdeps"):
            path = self.jdk / "bin" / (name + (".exe" if os.name == "nt" else ""))
            if not path.is_file() or not os.access(path, os.X_OK):
                raise ValueError("missing executable Java 21 tool: " + str(path))
            self.tools[name] = path
        if not self.maven.is_file() or not os.access(self.maven, os.X_OK):
            raise ValueError("explicit Maven executable required")
        for owned in (self.pack, self.jdk, *(self.vendor / name for name in VENDORS)):
            if self.output.is_relative_to(owned) or owned.is_relative_to(self.output):
                raise ValueError("output must be separate from source/toolchain trees")
            if self.maven_repo.is_relative_to(owned) or owned.is_relative_to(self.maven_repo):
                raise ValueError("Maven repository must be separate from source/toolchain trees")
        if self.output == self.maven_repo or self.output.is_relative_to(self.maven_repo):
            raise ValueError("run output cannot equal or be inside the Maven repository")
        for required in (self.packager, self.flatten_owner,
                         self.pack / "source-build/pom.xml",
                         self.pack / "src/main/java/module-info.java"):
            if not required.is_file():
                raise ValueError("required source input missing: " + str(required))
        self.config = {
            "format": "M3_JACKSON_STAGED_RUN_1",
            "pack": str(self.pack), "repo_root": str(self.repo),
            "jdk_home": str(self.jdk), "maven": str(self.maven),
            "maven_repo": str(self.maven_repo), "out": str(self.output),
            "maven_settings": str(self.maven_settings) if self.maven_settings else None,
            "maven_settings_sha256": digest(self.maven_settings) if self.maven_settings else None,
            "java_trust_store": str(self.java_trust_store) if self.java_trust_store else None,
            "java_trust_store_sha256": digest(self.java_trust_store) if self.java_trust_store else None,
            "timeout_seconds": args.timeout_seconds,
            "java_release_sha256": digest(release),
            "java_runtime_sha256": digest(self.jdk / "lib/modules"),
            "java_tool_sha256": {name: digest(path) for name, path in self.tools.items()},
            "maven_executable_sha256": digest(self.maven),
            "python_executable": str(Path(sys.executable).resolve()),
            "python_version": list(sys.version_info[:3]),
        }

    def sources(self):
        result = tree_manifest(self.pack, "pack")
        for name in VENDORS:
            result.update(tree_manifest(self.vendor / name, "vendor/" + name))
        result["packager/jmod_pack.py"] = digest(self.packager)
        result["packager/jmod_image.py"] = digest(self.flatten_owner)
        return dict(sorted(result.items()))

    def command(self, label, argv, allowed=(0,), marker=None, cwd=None):
        if not re.fullmatch(r"[a-z0-9-]+", label):
            raise ValueError("invalid command log label")
        self.command_number += 1
        stem = f"{self.command_number:03d}-{label}"
        directory = self.output / "logs" / self.stage
        arguments = [str(value) for value in argv]
        environment = os.environ.copy()
        unset = ("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "MAVEN_ARGS",
                 "MAVEN_OPTS", "CLASSPATH")
        for name in unset:
            environment.pop(name, None)
        environment["JAVA_HOME"] = str(self.jdk)
        environment["MAVEN_SKIP_RC"] = "1"
        if label == "maven-source-install" and self.java_trust_store is not None:
            if digest(self.java_trust_store) != self.config["java_trust_store_sha256"]:
                raise ValueError("Java trust store changed before Maven execution")
            environment["MAVEN_OPTS"] = "-Djavax.net.ssl.trustStore=" + str(self.java_trust_store)
        environment["PYTHONDONTWRITEBYTECODE"] = "1"
        environment["TMPDIR"] = str(self.output / "tmp")
        environment["TMP"] = str(self.output / "tmp")
        environment["TEMP"] = str(self.output / "tmp")
        working = self.output if cwd is None else cwd
        write_json(directory / (stem + ".command.json"), {
            "argv": arguments, "cwd": str(working), "allowed_exit_codes": list(allowed),
            "required_stdout_marker": marker, "unset_environment": list(unset),
            "JAVA_HOME": str(self.jdk), "MAVEN_SKIP_RC": "1",
            "java_trust_store": self.config["java_trust_store"] if label == "maven-source-install" else None,
            "temporary_directory": str(self.output / "tmp"),
        })
        stdout_path, stderr_path = (directory / (stem + suffix) for suffix in (".stdout.log", ".stderr.log"))
        print("M3_JACKSON_COMMAND", self.stage, label, flush=True)
        code, failure = None, None
        with stdout_path.open("xb") as stdout, stderr_path.open("xb") as stderr:
            try:
                result = subprocess.run(arguments, cwd=working, env=environment,
                                        stdout=stdout, stderr=stderr, check=False,
                                        timeout=self.args.timeout_seconds)
                code = result.returncode
            except (OSError, subprocess.TimeoutExpired) as error:
                failure = str(error)
        text = stdout_path.read_text(encoding="utf-8", errors="replace")
        errors = stderr_path.read_text(encoding="utf-8", errors="replace")
        passed = failure is None and code in allowed and (marker is None or marker in text)
        write_json(directory / (stem + ".exit.json"), {
            "exit_code": code, "status": "PASS" if passed else "FAIL", "error": failure,
            "stdout_sha256": digest(stdout_path), "stderr_sha256": digest(stderr_path),
        })
        if not passed:
            raise RuntimeError("command failed: " + str(directory / stem))
        return text, errors

    def start(self):
        self.current_sources = self.sources()
        self.source_sha256 = value_digest(self.current_sources)
        index = STAGES.index(self.stage)
        self.predecessor_sha256 = None
        if self.stage == "lint":
            if self.output.exists():
                raise FileExistsError("lint requires a fresh output directory")
            self.output.mkdir(parents=True)
            for name in ("receipts", "logs", "tmp", "tool-home"):
                (self.output / name).mkdir()
            write_json(self.output / "RUN_INPUTS.json", {
                "config": self.config, "source_sha256": self.source_sha256,
                "source_files": self.current_sources,
            })
        else:
            inputs = json.loads((self.output / "RUN_INPUTS.json").read_text(encoding="utf-8"))
            if inputs["config"] != self.config:
                raise ValueError("run configuration or toolchain drift; fresh output required")
            if inputs["source_files"] != self.current_sources or inputs["source_sha256"] != self.source_sha256:
                raise ValueError("source drift; fresh lint/compile/test/runtime epoch required")
            for prior in STAGES[:index]:
                receipt = self.output / "receipts" / (prior + ".pass.json")
                entry = json.loads(receipt.read_text(encoding="utf-8"))
                if (entry["status"] != "PASS" or entry["stage"] != prior
                        or entry["source_sha256"] != self.source_sha256
                        or entry["predecessor_sha256"] != self.predecessor_sha256):
                    raise ValueError("invalid predecessor receipt: " + str(receipt))
                self.predecessor_sha256 = digest(receipt)
            for pending in STAGES[index:]:
                if any((self.output / "receipts").glob(pending + ".*.json")):
                    raise ValueError("stage replay or incomplete/failed attempt; fresh output required")
        (self.output / "logs" / self.stage).mkdir()
        write_json(self.output / "receipts" / (self.stage + ".start.json"), {
            "stage": self.stage, "source_sha256": self.source_sha256,
            "predecessor_sha256": self.predecessor_sha256,
        })

    def finish(self, status, evidence):
        current = self.sources()
        if current != self.current_sources:
            status = "FAIL"
            evidence = {"error": "source drift during stage", "observed_sha256": value_digest(current)}
        write_json(self.output / "receipts" / (self.stage + "." + status.lower() + ".json"), {
            "status": status, "stage": self.stage, "source_sha256": self.source_sha256,
            "predecessor_sha256": self.predecessor_sha256, "evidence": evidence,
            "logs": tree_manifest(self.output / "logs" / self.stage, "logs/" + self.stage, False),
        })
        if status != "PASS":
            raise RuntimeError(str(evidence.get("error", "stage failed")))
        print("M3_JACKSON_STAGE_PASS", self.stage, self.source_sha256, flush=True)

    def compiled_inputs(self):
        result = {}
        for name in ("donor-jars", "modules", "test-classes"):
            result.update(tree_manifest(self.output / name, name, False))
        return dict(sorted(result.items()))

    def verify_compiled_inputs(self):
        receipt = json.loads((self.output / "receipts/compile.pass.json").read_text(encoding="utf-8"))
        if receipt["evidence"]["compiled_files"] != self.compiled_inputs():
            raise ValueError("compiled-input drift; fresh qualification epoch required")

    def lint(self):
        python_count, xml_count, java_count = 0, 0, 0
        for path in [*regular_files(self.pack), self.packager, self.flatten_owner]:
            if path.suffix == ".py":
                ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
                python_count += 1
            if path.suffix == ".xml":
                ET.parse(path)
                xml_count += 1
            if path.is_relative_to(self.pack) and path.suffix in {".java", ".py", ".xml"}:
                content = path.read_text(encoding="utf-8")
                if "SPDX-License-Identifier:" not in content[:4096]:
                    raise ValueError("new source license header missing: " + str(path))
                if not content.endswith("\n") or "\r" in content:
                    raise ValueError("new source requires LF lines and final newline: " + str(path))
                if any(line.rstrip(" \t") != line for line in content.splitlines()):
                    raise ValueError("trailing whitespace in new source: " + str(path))
                java_count += path.suffix == ".java"
        for name in VENDORS:
            ET.parse(self.vendor / name / "pom.xml")
            xml_count += 1
        empty = self.output / "empty-diff-preimage"
        empty.mkdir()
        text, errors = self.command("new-source-diff-check", [
            "git", "-c", "core.autocrlf=false", "--no-pager", "diff", "--no-index", "--check",
            "--", empty, self.pack,
        ], allowed=(0, 1))
        if text.strip() or errors.strip():
            raise ValueError("git diff whitespace check reported errors")
        return {"python_ast_parsed": python_count, "xml_parsed": xml_count,
                "new_java_header_whitespace_checks": java_count,
                "java_syntax_authority": "javac in the next compile stage; no substitute parser"}

    def compile(self):
        reactor = self.output / "reactor"
        sources = reactor / "sources"
        sources.mkdir(parents=True)
        shutil.copy2(self.pack / "source-build/pom.xml", reactor / "pom.xml")
        for name in VENDORS:
            donor, destination = self.vendor / name, sources / name
            destination.mkdir()
            for path in regular_files(donor):
                target = destination / path.relative_to(donor)
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(path, target)
            if tree_manifest(donor, name) != tree_manifest(destination, name):
                raise ValueError("vendor copy hash mismatch: " + name)
        settings = self.output / "maven-settings.xml"
        if self.maven_settings is None:
            settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"/>\n', encoding="utf-8")
        else:
            shutil.copy2(self.maven_settings, settings)
            if digest(settings) != self.config["maven_settings_sha256"]:
                raise ValueError("Maven settings changed before compilation")
        self.maven_repo.mkdir(parents=True, exist_ok=True)
        self.command("maven-source-install", [
            self.maven, "--batch-mode", "--no-transfer-progress", "--settings", settings,
            "--global-settings", settings, "--file", reactor / "pom.xml",
            "-Dmaven.repo.local=" + str(self.maven_repo),
            "-Duser.home=" + str(self.output / "tool-home"),
            "-Djava.io.tmpdir=" + str(self.output / "tmp"),
            "-Dproject.build.outputTimestamp=" + ARCHIVE_DATE,
            "-Dmaven.test.skip=true", "-DskipTests=true", "install",
        ], cwd=reactor)
        jars = self.output / "donor-jars"
        jars.mkdir()
        donor_archives = {}
        for artifact, module in DONOR_MODULES:
            filename = artifact + "-2.19.2-m3-source-1.jar"
            original = sources / (artifact + "-2.19.2") / "target" / filename
            jar = jars / filename
            normalized_jar(original, jar)
            view = jar_view(jar)
            if "module-info.class" not in view:
                raise ValueError("published JPMS descriptor missing: " + module)
            descriptor, _ = self.command("describe-" + artifact, [
                self.tools["jar"], "--describe-module", "--file", jar, "--release", "21",
            ], marker=module)
            if not any(line.startswith(module + "@") or line.startswith(module + " ")
                       for line in descriptor.splitlines()):
                raise ValueError("unexpected donor module name: " + module)
            legal_entries = [entry for entry in view if "LICENSE" in entry or "NOTICE" in entry]
            if not legal_entries:
                raise ValueError("donor legal resources missing: " + module)
            donor_archives[module] = {"original_sha256": digest(original),
                                      "normalized_sha256": digest(jar), "java21_view": view}
        core = donor_archives["com.fasterxml.jackson.core"]["java21_view"]
        shaded = "com/fasterxml/jackson/core/internal/shaded/fdp/v2_19_2/"
        if not any(name.startswith(shaded) and name.endswith(".class") for name in core):
            raise ValueError("published shaded FastDoubleParser implementation missing")
        if any(name.startswith("ch/randelshofer/fastdoubleparser/") for name in core):
            raise ValueError("unrelocated FastDoubleParser package in Jackson core")
        if not any(name.startswith(shaded) and entry["release"] == 21 for name, entry in core.items()):
            raise ValueError("FastDoubleParser release-21 override missing")
        java_sources = sorted((self.pack / "src/main/java").rglob("*.java"))
        tests = sorted((self.pack / "src/test/java").rglob("*.java"))
        if not java_sources or not tests:
            raise ValueError("receiver and real contract sources are required")
        if any(path.name == "module-info.java" for path in tests):
            raise ValueError("contract fixture source is currently a classpath test owner")
        modules = self.output / "modules"
        receiver = modules / RECEIVER_MODULE
        receiver.mkdir(parents=True)
        self.command("receiver-javac", [
            self.tools["javac"], "--release", "21", "-Xlint:all,-module", "-Werror", "-proc:none",
            "--module-path", jars, "-d", receiver, *java_sources,
        ])
        resources = self.pack / "src/main/resources"
        if resources.is_dir():
            for path in regular_files(resources):
                target = receiver / path.relative_to(resources)
                if target.exists():
                    raise ValueError("resource collides with compiled class: " + str(target))
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(path, target)
        test_classes = self.output / "test-classes"
        test_classes.mkdir()
        self.command("contract-javac", [
            self.tools["javac"], "--release", "21", "-Xlint:all,-module", "-Werror", "-proc:none",
            "--module-path", os.pathsep.join((str(jars), str(modules))),
            "--add-modules", RECEIVER_MODULE, "-d", test_classes, *tests,
        ])
        return {"donor_archives": donor_archives, "compiled_files": self.compiled_inputs(),
                "receiver_sources": len(java_sources), "contract_sources": len(tests),
                "donor_tests": "published source jars contain no tests; consumer contract is the test gate"}

    def test(self):
        self.verify_compiled_inputs()
        text, _ = self.command("host-contract", [
            self.tools["java"], *CONTRACT_OPENS, "--module-path",
            os.pathsep.join((str(self.output / "donor-jars"), str(self.output / "modules"))),
            "--add-modules", RECEIVER_MODULE, "-cp", self.output / "test-classes",
            CONTRACT_MAIN, self.output / "host-contract-output",
        ], marker=CONTRACT_MARKER)
        self.verify_compiled_inputs()
        return {"contract_main": CONTRACT_MAIN, "required_marker": CONTRACT_MARKER,
                "stdout_sha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
                "runtime": "supplied Java 21; internal M3String execution depends on that runtime"}

    def runtime(self):
        self.verify_compiled_inputs()
        legal = self.pack / "legal"
        if not legal.is_dir() or not regular_files(legal):
            raise ValueError("receiver legal notices are required")
        receiver_jar = self.output / "receiver-jar"
        receiver_jar.mkdir()
        jar = receiver_jar / (RECEIVER_MODULE + ".jar")
        self.command("receiver-jar", [
            self.tools["jar"], "--create", "--file", jar, "--date", ARCHIVE_DATE,
            "--module-version", "0.1.0", "-C", self.output / "modules" / RECEIVER_MODULE, ".",
        ])
        self.command("receiver-descriptor", [
            self.tools["jar"], "--describe-module", "--file", jar,
        ], marker=RECEIVER_MODULE)
        artifacts = [(artifact, module,
                      self.output / "donor-jars" / (artifact + "-2.19.2-m3-source-1.jar"))
                     for artifact, module in DONOR_MODULES]
        artifacts.append((RECEIVER_MODULE, RECEIVER_MODULE, jar))
        table = self.output / "INPUT_JMOD_IMAGE.tsv"
        with table.open("x", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
            writer.writerow(("artifact", "version", "jar", "module", "requires", "disposition"))
            for artifact, module, artifact_path in artifacts:
                writer.writerow((artifact, "0.1.0" if module == RECEIVER_MODULE else "2.19.2",
                                 str(artifact_path), module, "descriptor-authoritative", "JMOD"))
        manifest = self.output / "EMPTY_DESCRIPTOR_MANIFEST.tsv"
        manifest.write_text("module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n", encoding="utf-8")
        self.command("existing-jmod-packager", [
            sys.executable, "-B", self.packager, "--jdk-home", self.jdk,
            "--image-tsv", table, "--manifest", manifest, "--layer-root", self.repo,
            "--out", self.output / "pack", "--legal-notices", legal,
            "--add-modules", ",".join(DEVELOPER_MODULES),
        ])
        jmods = self.output / "pack/jmods"
        module_evidence = {}
        for artifact, module, artifact_path in artifacts:
            jmod = jmods / (module + ".jmod")
            self.command("jmod-describe-" + artifact.replace(".", "-"), [
                self.tools["jmod"], "describe", jmod,
            ], marker=module)
            expected = jar_view(artifact_path)
            checked = 0
            with zipfile.ZipFile(jmod) as archive:
                names = set(archive.namelist())
                for name, item in expected.items():
                    if name in {"module-info.class", "META-INF/MANIFEST.MF"}:
                        continue
                    entry = "classes/" + name
                    if entry not in names or hashlib.sha256(archive.read(entry)).hexdigest() != item["sha256"]:
                        raise ValueError("JMOD release-21 payload mismatch: " + module + "/" + name)
                    checked += 1
                if any(name.startswith("classes/META-INF/versions/") for name in names):
                    raise ValueError("unflattened multi-release entry survived JMOD packaging")
            module_evidence[module] = {"jmod_sha256": digest(jmod),
                                       "verified_class_resource_entries": checked}
        image = self.output / "pack/image"
        java = image / "bin" / ("java.exe" if os.name == "nt" else "java")
        for name in self.tools:
            executable = image / "bin" / (name + (".exe" if os.name == "nt" else ""))
            if not executable.is_file() or not os.access(executable, os.X_OK):
                raise ValueError("linked developer tool missing: " + name)
        self.command("linked-validate-modules", [java, "--validate-modules"])
        listed, _ = self.command("linked-list-modules", [java, "--list-modules"])
        actual = {line.split("@", 1)[0] for line in listed.splitlines() if line.strip()}
        required = {module for _, module, _ in artifacts} | set(DEVELOPER_MODULES)
        if not required.issubset(actual):
            raise ValueError("linked module closure incomplete: " + repr(sorted(required - actual)))
        self.command("linked-contract", [
            java, *CONTRACT_OPENS, "--add-modules", RECEIVER_MODULE, "-cp", self.output / "test-classes",
            CONTRACT_MAIN, self.output / "linked-contract-output",
        ], marker=CONTRACT_MARKER)
        self.verify_compiled_inputs()
        return {"modules": module_evidence, "linked_modules": sorted(actual),
                "compiled_again_at_runtime": False, "contract_main": CONTRACT_MAIN,
                "source_receipt_sha256": self.source_sha256,
                "image_module_file_sha256": digest(image / "lib/modules"),
                "runtime_scope": "supplied Java 21 image; no custom HotSpot/M3String qualification claimed"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage", choices=STAGES, required=True)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--maven", type=Path, required=True)
    parser.add_argument("--maven-repo", type=Path, required=True)
    parser.add_argument("--maven-settings", type=Path)
    parser.add_argument("--java-trust-store", type=Path)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--timeout-seconds", type=int, default=1800)
    args = parser.parse_args()
    run = Run(args)
    run.start()
    try:
        evidence = getattr(run, args.stage)()
    except Exception as error:
        run.finish("FAIL", {"error": str(error), "traceback": traceback.format_exc()})
        return 1
    run.finish("PASS", evidence)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as failure:
        print("M3_JACKSON_STAGE_REFUSED:", failure, file=sys.stderr)
        sys.exit(1)
