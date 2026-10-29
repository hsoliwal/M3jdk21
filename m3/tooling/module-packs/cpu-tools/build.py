#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Compile and qualify twenty CPU tool modules using the existing JMOD packager.

Requires a clean M3 Java 21 development image without earlier copies of these
optional modules, Python 3.10+, a Linux C compiler, and PATH-selected objcopy.
The output directory must not exist. The source modules, recipes and source JDK
are never modified. JAR timestamps are fixed; JMOD hashes identify this build
and are not a claim of byte-reproducible JMOD packaging.
"""

import argparse
import csv
import hashlib
import json
import os
import platform
import re
import shutil
import shlex
import signal
import subprocess
import sys
import time
import zipfile
from pathlib import Path


TEXT_MODULES = ("jdk.m3.text", "jdk.m3.io", "jdk.m3.structured")
TOOL_SUFFIXES = (
    "text.normalize", "text.tokenize", "text.regex", "text.search", "text.ngram",
    "text.frequency", "text.similarity", "text.diff", "text.index",
    "graph.topology", "graph.paths", "stats.streaming", "constraint.interval",
    "codec.varint", "verify.sha256", "plan.priority",
)
CPU_MODULES = tuple("com.m3.tools." + value for value in TOOL_SUFFIXES) + ("com.m3.tools.cpu",)
MODULES = tuple(sorted(TEXT_MODULES + CPU_MODULES))
FIXTURES = ("M3DonorToolsTest", "M3SourceToolsTest", "M3CpuToolsTest", "M3FileCpuFactsTest",
            "M3StructuredAtomsContract", "M3EvidenceMathContract",
            "M3LogicFlowContract", "M3IndexedDataContract")
FIXED_JAR_DATE = "2026-01-01T00:00:00Z"
COMMAND_SECONDS = 115
CLEANUP_SECONDS = 5


def stream_sha256(stream):
    digest = hashlib.sha256()
    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
        digest.update(chunk)
    return digest.hexdigest()


def sha256(path):
    with path.open("rb") as stream:
        return stream_sha256(stream)


def verify_jmod_legal(jmod, notices):
    """Read the actual legal section, including the standard JMOD ZIP prefix."""
    with zipfile.ZipFile(jmod) as archive:
        names = archive.namelist()
        for relative, expected in notices.items():
            name = "legal/" + relative
            if names.count(name) != 1:
                raise ValueError("missing or duplicate JMOD legal entry: " + name)
            with archive.open(name) as stream:
                if stream_sha256(stream) != expected:
                    raise ValueError("JMOD legal bytes differ: " + name)


def native_tool_identity(requested, search_path, cwd):
    """Resolve as subprocesses in cwd do, retaining both PATH entry and real file."""
    effective_path = os.pathsep.join(str((cwd / item).resolve())
                                    for item in search_path.split(os.pathsep))
    request = str(cwd / requested) if os.path.dirname(requested) else requested
    selected = shutil.which(request, path=effective_path)
    if selected is None:
        raise ValueError("required native tool is unavailable: " + requested)
    resolved = required_file(Path(selected).resolve(strict=True))
    return {"requested": requested, "selected": str(Path(selected).absolute()),
            "resolved": str(resolved), "sha256": sha256(resolved)}


def verify_native_tool_identity(before, search_path, cwd):
    after = native_tool_identity(before["requested"], search_path, cwd)
    if after != before:
        raise ValueError("native tool resolution or bytes changed: " + before["requested"])
    return after


def verify_jmod_native(jmod, compiled_sha256, member="lib/libm3textio.so"):
    """Require one exact original JNI member before any jlink transformation."""
    with zipfile.ZipFile(jmod) as archive:
        if archive.namelist().count(member) != 1:
            raise ValueError("missing or duplicate JMOD native member: " + member)
        with archive.open(member) as stream:
            actual = stream_sha256(stream)
    if actual != compiled_sha256:
        raise ValueError("JMOD native bytes differ from compiled owner")
    return actual


def verify_linked_native(linked, expected_stripped_sha256):
    actual = sha256(required_file(linked))
    if actual != expected_stripped_sha256:
        raise ValueError("linked JNI bytes differ from expected objcopy -g transformation")
    return actual


def verify_image_legal(image, module, notices):
    """Follow jlink's internal legal deduplication links and verify every byte."""
    root = image.resolve(strict=True)
    for relative, expected in notices.items():
        path = image / "legal" / module / relative
        resolved = path.resolve(strict=True)
        if not resolved.is_relative_to(root) or not resolved.is_file():
            raise ValueError("linked legal entry escapes image or is not a file: " + str(path))
        if sha256(resolved) != expected:
            raise ValueError("linked legal bytes differ: " + module + "/" + relative)


def write_json(path, value):
    path.write_text(json.dumps(value, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def required_file(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("regular, non-symlink input required: " + str(path))
    return path.resolve(strict=True)


def fresh_output(path, protected):
    """Refuse replacement, symlink output components and writing inside inputs."""
    selected = Path(os.path.abspath(path.expanduser()))
    if os.path.lexists(selected):
        raise FileExistsError("fresh output directory required: " + str(selected))
    if any(parent.is_symlink() for parent in selected.parents):
        raise ValueError("output parent must not be a symlink: " + str(selected))
    resolved = selected.resolve()
    for root in protected:
        if resolved.is_relative_to(root.resolve()):
            raise ValueError("output overlaps protected input: " + str(root))
    return resolved


def source_jdk_modules(jdk, admitted=MODULES):
    """Refuse a source image/JMOD directory that can shadow this fresh pack.

    The existing packager puts the source JDK's JMOD directory first. An
    earlier optional-module installation is therefore not an admissible
    source toolchain, even when its version and public descriptor look right.
    """
    release = required_file(jdk / "release").read_text(encoding="utf-8")
    values = re.findall(r'^MODULES="([^"\r\n]*)"\s*$', release, re.M)
    if len(values) != 1:
        raise ValueError("source JDK release must declare exactly one MODULES inventory")
    linked = tuple(values[0].split())
    if not linked or len(linked) != len(set(linked)):
        raise ValueError("source JDK release MODULES inventory is empty or duplicated")
    source_jmods = jdk / "jmods"
    if source_jmods.is_symlink() or not source_jmods.is_dir():
        raise ValueError("regular source JDK jmods directory required")
    names = {path.stem for path in source_jmods.glob("*.jmod")}
    collisions = sorted(set(admitted).intersection(set(linked) | names))
    if collisions:
        raise ValueError("source JDK already contains admitted tool modules; use a clean M3 JDK: "
                         + ",".join(collisions))
    return linked


def module_sources(root, expected):
    if root.is_symlink() or not root.is_dir():
        raise ValueError("module source directory required: " + str(root))
    found = tuple(sorted(path.name for path in root.iterdir() if path.is_dir()))
    if found != tuple(sorted(expected)):
        raise ValueError("module source inventory differs: " + str(found))
    files = []
    for module in sorted(expected):
        if (root / module).is_symlink():
            raise ValueError("symlink module source directory refused: " + module)
        descriptor = required_file(root / module / "module-info.java")
        text = re.sub(r"/\*.*?\*/|//[^\n]*", "", descriptor.read_text(encoding="utf-8"), flags=re.S)
        names = re.findall(r"\b(?:open\s+)?module\s+([\w.]+)\s*\{", text)
        if names != [module]:
            raise ValueError("module descriptor/name mismatch: " + str(descriptor))
        for path in sorted((root / module).rglob("*")):
            if path.is_symlink():
                raise ValueError("symlink in module inputs: " + str(path))
            if path.is_file():
                if path.suffix in (".class", ".jar", ".jmod"):
                    raise ValueError("precompiled module input refused: " + str(path))
                files.append(path)
    return tuple(files)


class Commands:
    """Bound each subprocess, retain output, and kill its group on timeout."""

    def __init__(self, output, repo, jdk):
        self.output = output
        self.repo = repo
        self.rows = []
        self.replacements = ((str(output), "${OUT}"), (str(repo), "${REPO}"),
                             (str(jdk), "${JDK}"))
        self.environment = dict(os.environ)
        # Explicit tool arguments own qualification. Avoid inherited Java agents,
        # boot/module options and compiler classpaths changing the tested program.
        for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH"):
            self.environment.pop(name, None)
        self.environment["TZ"] = "UTC"

    def normalized(self, argument):
        value = str(argument)
        for original, replacement in self.replacements:
            value = value.replace(original, replacement)
        return value

    def run(self, label, *arguments, java_tmp_dir=None):
        environment = self.environment
        temporary = None
        if java_tmp_dir is not None:
            temporary = Path(java_tmp_dir).resolve(strict=True)
            if not temporary.is_dir():
                raise ValueError("Java temporary directory required: " + str(temporary))
            environment = dict(self.environment)
            # HotSpot parses concatenated quoted argument segments. Quote this
            # one controlled property; never forward caller-supplied JVM options.
            environment["JAVA_TOOL_OPTIONS"] = shlex.quote("-Djava.io.tmpdir=" + str(temporary))
        log = self.output / "logs" / (label + ".log")
        started = time.monotonic()
        record = {"label": label, "argv": [self.normalized(arg) for arg in arguments],
                  "log": log.relative_to(self.output).as_posix(),
                  "timeout_seconds": COMMAND_SECONDS, "status": "STARTED"}
        if temporary is not None:
            record["java_tmp_dir"] = self.normalized(temporary)
        self.rows.append(record)
        write_json(self.output / "COMMANDS.json", self.rows)
        with log.open("wb") as stream:
            try:
                process = subprocess.Popen([str(arg) for arg in arguments], cwd=self.repo,
                                           env=environment, stdin=subprocess.DEVNULL,
                                           stdout=stream, stderr=subprocess.STDOUT,
                                           start_new_session=True)
                try:
                    code = process.wait(timeout=COMMAND_SECONDS)
                except subprocess.TimeoutExpired:
                    record["status"] = "TIMEOUT"
                    try:
                        os.killpg(process.pid, signal.SIGKILL)
                    except ProcessLookupError:
                        pass
                    process.wait(timeout=CLEANUP_SECONDS)
                    raise RuntimeError("command exceeded time budget: " + label) from None
                record["exit_code"] = code
                record["status"] = "PASS" if code == 0 else "FAILED"
                if code:
                    raise RuntimeError("command failed; see " + str(log))
            except OSError as failure:
                record["status"] = "LAUNCH_FAILED"
                record["error"] = type(failure).__name__
                raise
            finally:
                record["elapsed_seconds"] = round(time.monotonic() - started, 6)
                stream.flush()
                record["log_sha256"] = sha256(log)
                write_json(self.output / "COMMANDS.json", self.rows)
        return log.read_text(encoding="utf-8", errors="replace")


def description_name(description):
    lines = description.splitlines()
    return lines[0].split()[0].split("@", 1)[0] if lines and lines[0].split() else ""


def qualify(args):
    repo = args.repo_root.resolve(strict=True)
    owner = repo / "m3/tooling/module-packs/cpu-tools"
    existing = repo / "m3/tooling/module-packs/text-tools"
    source_roots = (existing / "src", owner / "src")
    native_source = required_file(existing / "native/m3textio.c")
    native_text_source = required_file(existing / "native/m3textops.c")
    legal_roots = (("text-tools", existing / "legal"), ("cpu-tools", owner / "legal"))
    notices = []
    for label, directory in legal_roots:
        for name in ("LICENSE.txt", "NOTICE.txt"):
            required_file(directory / name)
        for path in sorted(directory.rglob("*")):
            if path.is_symlink():
                raise ValueError("symlink legal input refused: " + str(path))
            if path.is_file():
                notices.append(required_file(path))
    notices = tuple(notices)
    packager = required_file(repo / "m3/tooling/jmod-recipes/tools/jmod_pack.py")
    packager_support = required_file(packager.with_name("jmod_image.py"))
    fixture_files = tuple(required_file(owner / "fixtures" / (name + ".java")) for name in FIXTURES)
    existing_smoke = required_file(existing / "fixtures/src/test/java/M3TextSmoke.java")
    files = module_sources(source_roots[0], TEXT_MODULES) + module_sources(source_roots[1], CPU_MODULES)
    jdk = args.jdk_home.resolve(strict=True)
    release = required_file(jdk / "release")
    if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', release.read_text(encoding="utf-8"), re.M):
        raise ValueError("M3 Java 21 development toolchain required")
    source_linked_modules = source_jdk_modules(jdk)
    if platform.system() != "Linux":
        raise ValueError("existing POSIX JNI qualification currently requires Linux")
    executables = []
    for tool in ("java", "javac", "jar", "javap", "jmod", "jlink"):
        executable = jdk / "bin" / tool
        if not executable.is_file() or not os.access(executable, os.X_OK):
            raise ValueError("missing JDK development executable: " + tool)
        executables.append(required_file(executable))
    required_file(jdk / "jmods/java.base.jmod")
    required_file(jdk / "jmods/jdk.compiler.jmod")
    toolchain_inputs = tuple(sorted(set((release, required_file(jdk / "lib/modules"),
                         required_file(jdk / "lib/server/libjvm.so"), *executables,
                         *(required_file(path) for path in sorted((jdk / "jmods").glob("*.jmod")))))))
    native_path = os.environ.get("PATH", os.defpath)
    native_tools = {
        "compiler": {"before": native_tool_identity(args.cc, native_path, repo)},
        "objcopy": {"before": native_tool_identity("objcopy", native_path, repo)},
    }
    compiler = native_tools["compiler"]["before"]["resolved"]
    objcopy = native_tools["objcopy"]["before"]["resolved"]
    output = fresh_output(args.out, (jdk, *source_roots, *(path for _, path in legal_roots), owner / "fixtures",
                                    existing / "native", existing / "fixtures"))
    output.mkdir(parents=True, exist_ok=False)
    for name in ("logs", "modules", "native", "native-expected", "native-text", "native-text-expected",
                 "jars", "legal", "java-tmp", "test-classes", "test-work"):
        (output / name).mkdir()
    command = Commands(output, repo, jdk)
    # jlink invokes the name objcopy. Keep its exact preflight PATH for every
    # subprocess, and verify that name still resolves to the same executable.
    command.environment["PATH"] = native_path
    native_path_sha256 = hashlib.sha256(native_path.encode("utf-8")).hexdigest()
    receipt = {"schema": "M3_CPU_TOOLS_BUILD_V1", "status": "RUNNING", "module_count": len(MODULES),
               "modules": list(MODULES), "jdk_release_sha256": sha256(release),
               "source_jdk_tool_module_collisions": "NONE_IN_RELEASE_OR_JMOD_FILENAMES",
               "source_jdk_linked_modules": list(source_linked_modules),
               "jar_timestamp": FIXED_JAR_DATE, "jmod_byte_reproducibility": "NOT_CLAIMED",
               "native_tools": native_tools, "native_path_sha256_before": native_path_sha256,
               "packager": packager.relative_to(repo).as_posix()}
    write_json(output / "BUILD_RECEIPT.json", receipt)
    try:
        inputs = sorted(set(files + notices + fixture_files + (native_source, native_text_source, packager,
                            packager_support, existing_smoke, required_file(owner / "build.py"),
                            required_file(owner / "pom.xml"), required_file(owner / "fixtures/test_build.py"))))
        source_seals = {path: sha256(path) for path in inputs}
        with (output / "SOURCE_INPUTS.tsv").open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
            writer.writerow(("path", "sha256", "bytes"))
            for path in inputs:
                writer.writerow((path.relative_to(repo).as_posix(), source_seals[path], path.stat().st_size))
        receipt["source_inputs_sha256"] = sha256(output / "SOURCE_INPUTS.tsv")
        toolchain_seals = {path: sha256(path) for path in toolchain_inputs}
        with (output / "TOOLCHAIN_INPUTS.tsv").open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
            writer.writerow(("jdk_relative_path", "sha256", "bytes"))
            for path in toolchain_inputs:
                writer.writerow((path.relative_to(jdk).as_posix(), toolchain_seals[path], path.stat().st_size))
        receipt["toolchain_inputs_sha256"] = sha256(output / "TOOLCHAIN_INPUTS.tsv")
        receipt["runtime_artifacts"] = [
            {"path": relative, "sha256": toolchain_seals[jdk / relative]}
            for relative in ("release", "lib/modules", "lib/server/libjvm.so")]
        legal_seals = {}
        for label, directory in legal_roots:
            for path in notices:
                if path.is_relative_to(directory):
                    destination = output / "legal" / label / path.relative_to(directory)
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(path, destination)
                    legal_seals[destination.relative_to(output / "legal").as_posix()] = source_seals[path]
        command.run("jdk-version", jdk / "bin/java", "--version")
        command.run("m3-ast-api", jdk / "bin/javap", "-public", "com.sun.source.util.M3AST")
        command.run("m3-string-owner", jdk / "bin/javap", "-private", "java.lang.M3String")
        command.run("m3-progress-api", jdk / "bin/javap", "-public", "java.util.concurrent.ProgressMonitor")
        native_tools["compiler"]["version"] = command.run("cc-version", compiler, "--version").strip()
        native_tools["objcopy"]["version"] = command.run("objcopy-version", objcopy, "--version").strip()
        command.run("modules-javac", jdk / "bin/javac", "-source", "21", "-target", "21",
                    "-Xlint:all,-module", "-Werror", "--module-source-path",
                    os.pathsep.join(str(path) for path in source_roots), "-d", output / "modules",
                    *(path for path in files if path.suffix == ".java"))
        for root in source_roots:
            for path in files:
                if path.is_relative_to(root) and path.suffix != ".java":
                    destination = output / "modules" / path.relative_to(root)
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(path, destination)
        command.run("native-compile", compiler, "-std=c11", "-Wall", "-Wextra", "-Werror",
                    "-shared", "-fPIC", "-I" + str(jdk / "include"),
                    "-I" + str(jdk / "include/linux"), "-o", output / "native/libm3textio.so", native_source)
        compiled_native = output / "native/libm3textio.so"
        expected_native = output / "native-expected/libm3textio.so"
        native_receipt = {"source": native_source.relative_to(repo).as_posix(),
                          "compiled": "native/libm3textio.so", "compiled_sha256": sha256(compiled_native),
                          "expected_stripped": "native-expected/libm3textio.so",
                          "jmod": "pack/jmods/jdk.m3.io.jmod", "jmod_member": "lib/libm3textio.so",
                          "linked": "pack/image/lib/libm3textio.so", "transformation": "objcopy -g"}
        receipt["native_provenance"] = native_receipt
        shutil.copyfile(compiled_native, expected_native)
        command.run("native-strip-expected", objcopy, "-g", expected_native)
        native_receipt["expected_stripped_sha256"] = sha256(expected_native)
        # Reuse the existing text-tools runner's original text JNI owner and module binding.
        command.run("native-text-compile", compiler, "-std=c11", "-Wall", "-Wextra", "-Werror",
                    "-shared", "-fPIC", "-I" + str(jdk / "include"),
                    "-I" + str(jdk / "include/linux"), "-o", output / "native-text/libm3textops.so", native_text_source)
        compiled_text_native = output / "native-text/libm3textops.so"
        expected_text_native = output / "native-text-expected/libm3textops.so"
        native_text_receipt = {"source": native_text_source.relative_to(repo).as_posix(),
                               "compiled": "native-text/libm3textops.so", "compiled_sha256": sha256(compiled_text_native),
                               "expected_stripped": "native-text-expected/libm3textops.so",
                               "jmod": "pack/jmods/jdk.m3.text.jmod", "jmod_member": "lib/libm3textops.so",
                               "linked": "pack/image/lib/libm3textops.so", "transformation": "objcopy -g"}
        receipt["native_text_provenance"] = native_text_receipt
        shutil.copyfile(compiled_text_native, expected_text_native)
        command.run("native-text-strip-expected", objcopy, "-g", expected_text_native)
        native_text_receipt["expected_stripped_sha256"] = sha256(expected_text_native)
        rows = [("artifact", "version", "jar", "module", "requires", "disposition")]
        artifact_rows = []
        for module in MODULES:
            jar = output / "jars" / (module + ".jar")
            command.run(module + "-jar", jdk / "bin/jar", "--create", "--file", jar,
                        "--date", FIXED_JAR_DATE, "-C", output / "modules" / module, ".")
            description = command.run(module + "-jar-describe", jdk / "bin/jar",
                                      "--describe-module", "--file", jar)
            if description_name(description) != module:
                raise ValueError("modular JAR readback differs: " + module)
            requires = sorted(line.split()[1] for line in description.splitlines() if line.startswith("requires "))
            rows.append((module, "1", str(jar), module, ",".join(requires), "JMOD"))
            artifact_rows.append({"module": module, "jar": jar.relative_to(output).as_posix(),
                                  "jar_sha256": sha256(jar), "requires": requires})
        image_tsv = output / "INPUT_JMOD_IMAGE.tsv"
        with image_tsv.open("w", encoding="utf-8", newline="") as stream:
            csv.writer(stream, delimiter="\t", lineterminator="\n").writerows(rows)
        manifest = output / "EMPTY_DESCRIPTOR_MANIFEST.tsv"
        manifest.write_text("module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n", encoding="utf-8")
        for tool in native_tools.values():
            verify_native_tool_identity(tool["before"], command.environment["PATH"], repo)
        command.run("jmod-pack", sys.executable, packager, "--jdk-home", jdk,
                    "--image-tsv", image_tsv, "--manifest", manifest, "--layer-root", repo,
                    "--out", output / "pack", "--legal-notices", output / "legal",
                    "--native-library", "jdk.m3.io=" + str(output / "native"),
                    "--native-library", "jdk.m3.text=" + str(output / "native-text"),
                    "--add-modules", "java.se,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps",
                    java_tmp_dir=output / "java-tmp")
        jmods = output / "pack/jmods"
        if tuple(sorted(path.stem for path in jmods.glob("*.jmod"))) != MODULES:
            raise ValueError("packager JMOD inventory differs from the twenty admitted modules")
        for artifact in artifact_rows:
            module = artifact["module"]
            jmod = jmods / (module + ".jmod")
            description = command.run(module + "-jmod-describe", jdk / "bin/jmod", "describe", jmod)
            if description_name(description) != module:
                raise ValueError("JMOD module readback differs: " + module)
            verify_jmod_legal(jmod, legal_seals)
            if module == "jdk.m3.io":
                native_receipt["jmod_member_sha256"] = verify_jmod_native(
                    jmod, native_receipt["compiled_sha256"])
            if module == "jdk.m3.text":
                native_text_receipt["jmod_member_sha256"] = verify_jmod_native(
                    jmod, native_text_receipt["compiled_sha256"], native_text_receipt["jmod_member"])
            artifact.update({"jmod": jmod.relative_to(output).as_posix(), "jmod_sha256": sha256(jmod),
                             "legal_readback": "EXACT_HASH_MATCH"})
        write_json(output / "MODULE_ARTIFACTS.json", artifact_rows)
        image = output / "pack/image"
        for tool in ("java", "javac", "jar", "javap", "jmod", "jlink"):
            if not os.access(image / "bin" / tool, os.X_OK):
                raise ValueError("linked development tool missing: " + tool)
        command.run("image-validate", image / "bin/java", "--validate-modules")
        listed = command.run("image-modules", image / "bin/java", "--list-modules")
        actual = {line.split("@", 1)[0] for line in listed.splitlines()}
        if not set(MODULES).issubset(actual):
            raise ValueError("linked image is missing admitted CPU modules")
        for module in MODULES:
            verify_image_legal(image, module, legal_seals)
        bundled_native = image / "lib/libm3textio.so"
        native_receipt["linked_sha256"] = verify_linked_native(
            bundled_native, native_receipt["expected_stripped_sha256"])
        bundled_text_native = image / "lib/libm3textops.so"
        native_text_receipt["linked_sha256"] = verify_linked_native(
            bundled_text_native, native_text_receipt["expected_stripped_sha256"])
        command.run("image-fixtures-javac", image / "bin/javac", "-source", "21", "-target", "21",
                    "-Xlint:all,-module", "-Werror", "--add-modules", ",".join(MODULES),
                    "-d", output / "test-classes", *fixture_files, existing_smoke)
        java = (image / "bin/java", "-Xcheck:jni", "-Djava.io.tmpdir=" + str(output / "test-work"))
        for test in FIXTURES:
            command.run("image-" + test, *java, "--add-modules", ",".join(MODULES),
                        "-cp", output / "test-classes", test)
        for test in ("M3SourceToolsTest", "M3CpuToolsTest", "M3FileCpuFactsTest"):
            command.run("image-m3-storage-" + test, *java, "-XX:+UnlockExperimentalVMOptions",
                        "-XX:+UseM3StringStorage", "-Dm3.test.requireStorage=true",
                        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                        "--add-modules", ",".join(MODULES), "-cp", output / "test-classes", test)
        smoke = command.run("image-existing-text-jni", *java, "--add-modules", ",".join(MODULES),
                            "-cp", output / "test-classes", "M3TextSmoke", output / "text-jni-fixture")
        if "M3_TEXT_IMAGE_PASS tools=24" not in smoke:
            raise ValueError("missing existing text/JNI runtime proof")
        cli = (*java, "-m", "com.m3.tools.cpu/com.m3.tools.cpu.CpuToolsCli")
        for option, label in (("--list", "catalogue"), ("--self-test", "self-test")):
            first = command.run("image-cli-" + label, *cli, option)
            second = command.run("image-cli-" + label + "-replay", *cli, option)
            if not first.strip() or first != second:
                raise ValueError("linked CLI replay differs: " + option)
            receipt[label + "_output_sha256"] = hashlib.sha256(first.encode("utf-8")).hexdigest()
        if any(sha256(path) != seal for path, seal in source_seals.items()):
            raise ValueError("qualification input changed during build; source receipt refused")
        if any(sha256(path) != seal for path, seal in toolchain_seals.items()):
            raise ValueError("M3 toolchain changed during build; runtime identity receipt refused")
        if source_jdk_modules(jdk) != source_linked_modules:
            raise ValueError("source JDK module inventory changed during qualification")
        if os.environ.get("PATH", os.defpath) != native_path or command.environment["PATH"] != native_path:
            raise ValueError("native tool PATH changed during qualification")
        for tool in native_tools.values():
            tool["after"] = verify_native_tool_identity(tool["before"], command.environment["PATH"], repo)
        if sha256(compiled_native) != native_receipt["compiled_sha256"] or sha256(expected_native) != native_receipt["expected_stripped_sha256"]:
            raise ValueError("native provenance input changed during qualification")
        if verify_jmod_native(jmods / "jdk.m3.io.jmod", native_receipt["compiled_sha256"]) != native_receipt["jmod_member_sha256"]:
            raise ValueError("JMOD native identity changed during qualification")
        verify_linked_native(bundled_native, native_receipt["expected_stripped_sha256"])
        native_receipt["before_after"] = "EXACT_COMPILED_JMOD_EXPECTED_LINKED_HASH_MATCH"
        if (sha256(compiled_text_native) != native_text_receipt["compiled_sha256"]
                or sha256(expected_text_native) != native_text_receipt["expected_stripped_sha256"]):
            raise ValueError("native text provenance input changed during qualification")
        if verify_jmod_native(jmods / "jdk.m3.text.jmod", native_text_receipt["compiled_sha256"],
                              native_text_receipt["jmod_member"]) != native_text_receipt["jmod_member_sha256"]:
            raise ValueError("JMOD native text identity changed during qualification")
        verify_linked_native(bundled_text_native, native_text_receipt["expected_stripped_sha256"])
        native_text_receipt["before_after"] = "EXACT_COMPILED_JMOD_EXPECTED_LINKED_HASH_MATCH"
        receipt["native_tools_before_after"] = "EXACT_PATH_RESOLUTION_AND_EXECUTABLE_HASH_MATCH"
        receipt["native_path_sha256_after"] = hashlib.sha256(command.environment["PATH"].encode("utf-8")).hexdigest()
        receipt.update({"status": "PASS", "fixture_classes": list(FIXTURES) + ["M3TextSmoke"],
                        "toolchain_before_after": "EXACT_HASH_MATCH", "legal_notice_roots": [label for label, _ in legal_roots],
                        "legal_notice_sha256": legal_seals, "legal_jmod_and_image_readback": "EXACT_HASH_MATCH_ALL_20_MODULES",
                        "m3_string_storage_enabled_fixtures": ["M3SourceToolsTest", "M3CpuToolsTest", "M3FileCpuFactsTest"],
                        "jni_owner": native_source.relative_to(repo).as_posix(),
                        "jni_text_owner": native_text_source.relative_to(repo).as_posix(),
                        "jni_text_sha256": sha256(bundled_text_native),
                        "jni_sha256": sha256(bundled_native), "image_module_count": len(actual),
                        "module_artifacts_sha256": sha256(output / "MODULE_ARTIFACTS.json"),
                        "linked_image": "pack/image", "cli_replay": "EXACT_OUTPUT_MATCH"})
    except BaseException as failure:
        receipt.update({"status": "FAILED", "failure": str(failure), "failure_type": type(failure).__name__})
        raise
    finally:
        receipt["commands_sha256"] = sha256(output / "COMMANDS.json") if (output / "COMMANDS.json").exists() else None
        write_json(output / "BUILD_RECEIPT.json", receipt)
    print("M3_CPU_TOOLS_PACK_PASS modules=20 fixtures=" + str(len(FIXTURES)) + " jni=existing_owner replay=exact")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[4])
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--cc", default=os.environ.get("CC", "cc"))
    qualify(parser.parse_args())


if __name__ == "__main__":
    main()
