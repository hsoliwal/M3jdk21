#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Source-build and qualify original native APIs; existing tool_packs owns installation."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import uuid
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

ROOT = Path(__file__).resolve().parent
API_JAR = "native-api-1.1.0-m3-workset.jar"
FIXTURE = "verification/src/test/java/NativeApiContract.java"
PACK_NAME = "native-api"


def digest(path):
    with Path(path).open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def read_json(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def write_json(path, value):
    path = Path(path)
    temporary = path.with_name(path.name + "." + uuid.uuid4().hex + ".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def now():
    return datetime.now(timezone.utc).isoformat()


def require_hash(path, expected):
    path = Path(path)
    if not path.is_file() or path.is_symlink() or digest(path) != expected:
        raise ValueError("Pinned input drift: " + str(path))
    return path


def source_snapshot():
    donor = read_json(ROOT / "DONOR_PINS.json")
    adaptations = {row["path"]: row for row in read_json(ROOT / "ADAPTATIONS.json")["files"]}
    allowed = {
        "upstream/maven-native-api/src/main/java/org/codehaus/mojo/natives/compiler/AbstractCompiler.java",
        "upstream/maven-native-api/src/main/java/org/codehaus/mojo/natives/compiler/CompilerConfiguration.java",
    }
    if set(adaptations) != allowed:
        raise ValueError("Only the two original compiler owners may be adapted")
    snapshot = {}
    expected_java = set()
    for row in donor["files"]:
        relative = row["target_relative"]
        expected = row["sha256"]
        if relative in adaptations:
            adaptation = adaptations[relative]
            if adaptation["before_sha256"] != expected or adaptation["donor_git_blob"] != row["git_blob"]:
                raise ValueError("Adaptation preimage does not match donor")
            expected = adaptation["after_sha256"]
        require_hash(ROOT / relative, expected)
        snapshot[relative] = expected
        if relative.endswith(".java"):
            expected_java.add(relative)
    actual_java = {str(path.relative_to(ROOT)) for path in (ROOT / "upstream").rglob("*.java")}
    if expected_java != actual_java or len(actual_java) != 51:
        raise ValueError("Expected exactly the pinned 51 original/adapted Java sources")
    for relative in ("DONOR_PINS.json", "ADAPTATIONS.json", "DEPENDENCY_PINS.json",
                     "FOUNDATION_PINS.json", "build.py", "pom.xml", "README.md", "NOTICE.md", FIXTURE):
        snapshot[relative] = digest(ROOT / relative)
    for path in sorted((ROOT / "legal").rglob("*")):
        if path.is_file():
            snapshot[str(path.relative_to(ROOT))] = digest(path)
    for row in read_json(ROOT / "DEPENDENCY_PINS.json")["source_licenses"]:
        require_hash(ROOT / row["path"], row["sha256"])
    return snapshot


def files_below(root):
    return {str(path.relative_to(root)): digest(path)
            for path in sorted(Path(root).rglob("*")) if path.is_file()}


class Run:
    def __init__(self, out, mode):
        self.out = Path(out).absolute()
        if self.out.is_symlink():
            raise ValueError("Output directory must not be a symlink")
        self.out.mkdir(parents=True, exist_ok=True)
        self.path = self.out / "receipt.json"
        self.data = {
            "schema": "M3_NATIVE_API_PROOF_V1", "state": "STARTED", "mode": mode,
            "run_id": str(uuid.uuid4()), "started_utc": now(), "commands": [],
            "full_m3jdk21_bootstrap": "NOT_RUN", "upstream_full_test_suite": "NOT_RUN",
            "general_native_incremental_planner": "NOT_IMPLEMENTED",
            "native_toolchain_lock": "DRIVER_HASHES_AND_VERSIONS; FULL_SYSROOT_NOT_LOCKED",
        }
        self.save()

    def save(self):
        write_json(self.path, self.data)

    def command(self, arguments, name, timeout=90):
        arguments = [str(argument) for argument in arguments]
        log = self.out / (str(len(self.data["commands"]) + 1).zfill(2) + "-" + name + ".log")
        row = {"argv": arguments, "log": log.name, "state": "STARTED"}
        self.data["commands"].append(row)
        self.save()
        try:
            with log.open("w", encoding="utf-8") as stream:
                result = subprocess.run(arguments, stdout=stream, stderr=subprocess.STDOUT,
                                        timeout=timeout, check=False)
            row["exit_code"] = result.returncode
            row["state"] = "PASSED" if result.returncode == 0 else "FAILED"
            row["log_sha256"] = digest(log)
            self.save()
            if result.returncode:
                raise RuntimeError(name + " failed; see " + str(log))
            return log.read_text(encoding="utf-8", errors="replace")
        except BaseException as error:
            row["state"] = "TIMED_OUT" if isinstance(error, subprocess.TimeoutExpired) else "FAILED"
            row["exception"] = type(error).__name__
            if log.exists():
                row["log_sha256"] = digest(log)
            self.save()
            raise

    def finish(self, error=None):
        self.data["finished_utc"] = now()
        self.data["state"] = "PASSED" if error is None else "FAILED"
        if error is not None:
            self.data["exception"] = type(error).__name__
            self.data["message"] = str(error)
        self.save()


def copy_checked(source, destination, expected):
    require_hash(source, expected)
    destination.parent.mkdir(parents=True, exist_ok=True)
    with Path(source).open("rb") as reader, destination.open("xb") as writer:
        shutil.copyfileobj(reader, writer)
    require_hash(source, expected)
    require_hash(destination, expected)


def make_api_jar(classes, jar):
    entries = {str(path.relative_to(classes)): path.read_bytes()
               for path in classes.rglob("*") if path.is_file()}
    resources = ROOT / "upstream/maven-native-api/src/main/resources"
    for path in resources.rglob("*"):
        if path.is_file():
            entries[str(path.relative_to(resources))] = path.read_bytes()
    entries["META-INF/MANIFEST.MF"] = (
        "Manifest-Version: 1.0\r\n"
        "Implementation-Title: M3JDK21 received Maven Native APIs\r\n"
        "Implementation-Version: 1.1.0-m3-workset\r\n\r\n").encode("utf-8")
    entries["META-INF/LICENSE-MIT.txt"] = (ROOT / "upstream/LICENSE.txt").read_bytes()
    entries["META-INF/LICENSE-APACHE-2.0.txt"] = (ROOT / "legal/LICENSE-APACHE-2.0.txt").read_bytes()
    entries["META-INF/NOTICE.md"] = (ROOT / "NOTICE.md").read_bytes()
    with ZipFile(jar, "x", compression=ZIP_DEFLATED) as archive:
        for name, content in sorted(entries.items()):
            entry = ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            entry.compress_type = ZIP_DEFLATED
            entry.external_attr = 0o644 << 16
            archive.writestr(entry, content)


def build(args, run):
    if any(path.name != "receipt.json" for path in run.out.iterdir()):
        raise FileExistsError("Source build requires a fresh output directory")
    inputs = source_snapshot()
    run.data["inputs"] = inputs
    java_home = Path(args.java_home).resolve(strict=True)
    dependency_root = Path(args.dependencies).resolve(strict=True)
    foundation = read_json(ROOT / "FOUNDATION_PINS.json")
    m3_file = require_hash(Path(args.m3_file_jar).resolve(strict=True), foundation["m3_file"]["jar_sha256"])
    with ZipFile(m3_file) as archive:
        for name, expected in foundation["m3_file"]["class_files"].items():
            if hashlib.sha256(archive.read(name)).hexdigest() != expected:
                raise ValueError("M3File class drift: " + name)
    run.data["m3_file_jar"] = {"path": str(m3_file), "sha256": digest(m3_file)}
    run.data["java_home"] = str(java_home)
    run.data["java_tools"] = {name: digest(java_home / "bin" / name) for name in ("java", "javac", "jmod")}
    run.data["java_modules_sha256"] = digest(java_home / "lib/modules")
    run.save()
    run.command([java_home / "bin/java", "-version"], "java-version")

    dependencies = read_json(ROOT / "DEPENDENCY_PINS.json")
    jars = []
    external_inputs = {str(m3_file): digest(m3_file)}
    payload = run.out / "pack"
    libraries = payload / "lib"
    libraries.mkdir(parents=True)
    legal = payload / "legal"
    legal.mkdir()
    for row in dependencies["artifacts"]:
        jar = require_hash(dependency_root / row["path"], row["sha256"])
        pom = require_hash(dependency_root / row["pom_path"], row["pom_sha256"])
        jars.append(jar)
        external_inputs[str(jar)] = row["sha256"]
        external_inputs[str(pom)] = row["pom_sha256"]
        copy_checked(jar, libraries / jar.name, row["sha256"])
        artifact_legal = legal / jar.stem
        copy_checked(pom, artifact_legal / "pom.xml", row["pom_sha256"])
        with ZipFile(jar) as archive:
            for name in archive.namelist():
                if name.upper().startswith("META-INF/") and Path(name).name.upper() in (
                        "LICENSE", "LICENSE.TXT", "NOTICE", "NOTICE.TXT", "COPYING", "COPYRIGHT"):
                    (artifact_legal / Path(name).name).write_bytes(archive.read(name))
    for row in dependencies["parent_poms"]:
        path = require_hash(dependency_root / row["path"], row["sha256"])
        external_inputs[str(path)] = row["sha256"]
        copy_checked(path, legal / "parent-poms" / path.name, row["sha256"])
    for path in sorted((ROOT / "legal").rglob("*")):
        if path.is_file():
            copy_checked(path, legal / path.relative_to(ROOT / "legal"), digest(path))
    for relative in ("NOTICE.md", "DONOR_PINS.json", "ADAPTATIONS.json", "DEPENDENCY_PINS.json", "FOUNDATION_PINS.json"):
        copy_checked(ROOT / relative, legal / relative, inputs[relative])
    copy_checked(ROOT / "upstream/LICENSE.txt", legal / "LICENSE-MIT.txt", inputs["upstream/LICENSE.txt"])

    for relative, expected in foundation["jni_headers"]["files"].items():
        source = require_hash(java_home / relative, expected)
        external_inputs[str(source)] = expected
        if relative.startswith("include/"):
            destination = payload / relative
        else:
            destination = legal / "jni-headers" / Path(relative).name
        copy_checked(source, destination, expected)
    run.data["external_inputs"] = external_inputs
    run.save()

    classes = run.out / "classes"
    classes.mkdir()
    classpath = os.pathsep.join(str(path) for path in jars)
    sources = [ROOT / name for name in sorted(inputs) if name.startswith("upstream/") and name.endswith(".java")]
    run.command([java_home / "bin/javac", "--release", "8", "-encoding", "UTF-8", "-Xlint:deprecation",
                 "-cp", classpath, "-d", classes, *sources], "compile-original-api")
    api_jar = libraries / API_JAR
    make_api_jar(classes, api_jar)
    class_hashes = files_below(classes)
    with ZipFile(api_jar) as archive:
        jar_classes = {name: hashlib.sha256(archive.read(name)).hexdigest()
                       for name in archive.namelist() if name.endswith(".class")}
    if jar_classes != class_hashes:
        raise ValueError("Packaged API classes do not match compiler outputs")
    fixture_classes = run.out / "fixture-classes"
    fixture_classes.mkdir()
    run.command([java_home / "bin/javac", "--release", "21", "-Xlint:all", "-Werror",
                 "--module-path", m3_file, "--add-modules", "com.m3.file", "-cp",
                 str(api_jar) + os.pathsep + classpath, "-d", fixture_classes, ROOT / FIXTURE], "compile-fixture")

    modules = run.out / "modules"
    modules.mkdir()
    file_legal = run.out / "file-module-legal"
    file_legal.mkdir()
    copy_checked(ROOT / "legal/LICENSE-APACHE-2.0.txt", file_legal / "LICENSE", inputs["legal/LICENSE-APACHE-2.0.txt"])
    (file_legal / "NOTICE").write_text("com.m3.file\nCopyright 2026 Hitesh Soliwal and contributors.\n"
                                     "Apache-2.0. Original qualified module; no source changes in this pack.\n", encoding="utf-8")
    file_jmod = modules / "com.m3.file.jmod"
    run.command([java_home / "bin/jmod", "create", "--class-path", m3_file,
                 "--legal-notices", file_legal, file_jmod], "package-existing-file-module")
    fixture_hashes = files_below(fixture_classes)
    source_build = {
        "state": "SOURCE_COMPILED", "donor_commit": "59b9d76471f42265141e1edd3594f9dca156099a",
        "java_sources": 51, "source_release": 8, "fixture_release": 21,
        "inputs": inputs, "api_class_files": class_hashes, "fixture_class_files": fixture_hashes,
        "api_jar_sha256": digest(api_jar), "java_home": str(java_home),
        "java_modules_sha256": run.data["java_modules_sha256"],
        "commands": run.data["commands"], "native_execution": "NOT_RUN_BY_SOURCE_BUILD",
    }
    write_json(legal / "SOURCE_BUILD.json", source_build)
    assets = []
    for path in sorted(payload.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(payload)
        if relative.parts[0] == "legal":
            target = "legal/m3-native-api/" + "/".join(relative.parts[1:])
        elif relative.parts[0] == "lib":
            target = "lib/m3-tools/native-api/" + path.name
        else:
            target = "lib/m3-tools/native-api/" + relative.as_posix()
        assets.append({"file": str(path.relative_to(run.out)), "target": target,
                       "sha256": digest(path), "executable": False})
    lock = {"schema_version": 1, "packs": [{
        "name": PACK_NAME, "version": "1.1.0-m3-workset",
        "source": "https://github.com/mojohaus/maven-native/tree/59b9d76471f42265141e1edd3594f9dca156099a; SOURCE_BUILD.json",
        "license": "MIT AND Apache-2.0; original dependency notices; JNI headers GPL-2.0-with-Classpath-exception",
        "roots": ["com.m3.file", "jdk.compiler", "java.base", "java.desktop", "java.management", "java.naming", "java.sql"],
        "modules": [{"module": "com.m3.file", "file": str(file_jmod.relative_to(run.out)), "sha256": digest(file_jmod)}],
        "assets": assets,
    }]}
    write_json(run.out / "native-api-lock.json", lock)
    if source_snapshot() != inputs:
        raise ValueError("Source/tooling inputs changed during compilation")
    for path, expected in external_inputs.items():
        require_hash(path, expected)
    if digest(java_home / "lib/modules") != run.data["java_modules_sha256"]:
        raise ValueError("Compiler image changed during source build")
    for name, expected in run.data["java_tools"].items():
        require_hash(java_home / "bin" / name, expected)
    if files_below(classes) != class_hashes or files_below(fixture_classes) != fixture_hashes:
        raise ValueError("Compiled classes changed during packaging")
    run.data.update({"api_class_files": class_hashes, "fixture_class_files": fixture_hashes,
                     "api_jar_sha256": digest(api_jar), "pack_lock_sha256": digest(run.out / "native-api-lock.json"),
                     "source_build": "ORIGINAL_51_SOURCES_WITH_TWO_RECORDED_ADAPTATIONS",
                     "native_execution": "NOT_RUN_BY_SOURCE_BUILD", "inputs_unchanged": True})


def verify(args, run):
    build_root = Path(args.build).resolve(strict=True)
    build_receipt = read_json(build_root / "receipt.json")
    if build_receipt.get("state") != "PASSED" or build_receipt.get("mode") != "build":
        raise ValueError("A successful current source-build receipt is required")
    inputs = source_snapshot()
    if inputs != build_receipt["inputs"]:
        raise ValueError("Source build does not correspond to current sources/tooling")
    require_hash(build_root / "native-api-lock.json", build_receipt["pack_lock_sha256"])
    fixture_classes = build_root / "fixture-classes"
    if files_below(fixture_classes) != build_receipt["fixture_class_files"]:
        raise ValueError("Fixture bytecode drift")
    image = Path(args.image).resolve(strict=True)
    pack = read_json(build_root / "native-api-lock.json")["packs"][0]
    installed = {}
    for asset in pack["assets"]:
        target = require_hash(image / asset["target"], asset["sha256"])
        installed[str(target)] = asset["sha256"]
    native_lib = image / "lib/m3-tools/native-api"
    expected_jars = {Path(asset["target"]).name for asset in pack["assets"]
                     if asset["target"].startswith("lib/m3-tools/native-api/") and asset["target"].endswith(".jar")}
    if {path.name for path in native_lib.glob("*.jar")} != expected_jars:
        raise ValueError("Unexpected or missing installed native API JAR")
    tools = {"gcc": Path(args.gcc).resolve(strict=True), "g++": Path(args.cxx).resolve(strict=True),
             "ar": Path(args.ar).resolve(strict=True)}
    run.data.update({"inputs": inputs, "installed_assets": installed, "build_receipt_sha256": digest(build_root / "receipt.json"),
                     "java_home": str(image), "java_modules_sha256": digest(image / "lib/modules"),
                     "fixture_class_files": build_receipt["fixture_class_files"],
                     "native_tools": {name: {"path": str(path), "sha256": digest(path)} for name, path in tools.items()}})
    run.save()
    run.command([image / "bin/java", "-version"], "installed-java-version")
    for name, executable in tools.items():
        run.command([executable, "--version"], "native-" + name.replace("+", "p") + "-version")
    classpath = os.pathsep.join([str(fixture_classes)] + [str(native_lib / name) for name in sorted(expected_jars)])
    output = run.command([image / "bin/java", "--add-modules", "com.m3.file", "-cp", classpath,
                          "NativeApiContract", run.out / "work", native_lib / "include", tools["gcc"],
                          tools["g++"], tools["ar"], image], "installed-native-contract")
    passes = [line for line in output.splitlines() if line.startswith("NativeApiContract: PASS;")]
    if len(passes) != 1:
        raise ValueError("Missing unique native contract completion marker")
    if source_snapshot() != inputs or files_below(fixture_classes) != build_receipt["fixture_class_files"]:
        raise ValueError("Sources or fixture bytecode changed during proof")
    for path, expected in installed.items():
        require_hash(path, expected)
    for row in run.data["native_tools"].values():
        require_hash(row["path"], row["sha256"])
    if digest(image / "lib/modules") != run.data["java_modules_sha256"]:
        raise ValueError("Installed image changed during proof")
    require_hash(build_root / "receipt.json", run.data["build_receipt_sha256"])
    require_hash(build_root / "native-api-lock.json", build_receipt["pack_lock_sha256"])
    run.data.update({"result": passes[0], "native_outputs": files_below(run.out / "work"),
                     "native_execution": "INSTALLED_IMAGE_C_CPP_ARCHIVE_LINK_JNI_PASSED",
                     "finite_known_dependency_delta_workset": "PASSED", "inputs_unchanged": True})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="mode", required=True)
    source = commands.add_parser("build")
    source.add_argument("--java-home", required=True)
    source.add_argument("--dependencies", required=True)
    source.add_argument("--m3-file-jar", required=True)
    source.add_argument("--out", required=True)
    proof = commands.add_parser("verify")
    proof.add_argument("--build", required=True)
    proof.add_argument("--image", required=True)
    proof.add_argument("--gcc", required=True)
    proof.add_argument("--cxx", required=True)
    proof.add_argument("--ar", required=True)
    proof.add_argument("--out", required=True)
    args = parser.parse_args()
    run = Run(args.out, args.mode)
    try:
        (build if args.mode == "build" else verify)(args, run)
        run.finish()
    except BaseException as error:
        run.finish(error)
        print(type(error).__name__ + ": " + str(error), file=sys.stderr)
        return 1
    print(str(run.path))
    return 0


if __name__ == "__main__":
    sys.exit(main())
