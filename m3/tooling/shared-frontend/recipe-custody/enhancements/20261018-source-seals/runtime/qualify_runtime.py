#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Build, then separately validate, a descriptor-aware hybrid M3 Java 21 image.

Read README.md before use. No product source, stock installation or previous
output is changed. Every build and validation directory must be fresh.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import platform
import re
import shutil
import stat
import struct
import subprocess
import sys
from datetime import datetime, timezone
import zipfile


SOURCE_PREFIX = "src/jdk.compiler/share/classes/"
OWNERS = (
    "com/sun/source/util/M3AST",
    "com/sun/tools/javac/m3/M3ASTPC",
    "com/sun/tools/javac/m3/M3PrimitiveImageCompiler",
    "com/sun/tools/javac/main/JavaCompiler",
)
SOURCES = tuple(SOURCE_PREFIX + owner + ".java" for owner in OWNERS) + (
    SOURCE_PREFIX + "module-info.java",
)
TESTS = (
    "test/langtools/tools/javac/m3/M3ASTSharedFrontendTest.java",
    "test/langtools/tools/javac/m3/M3ASTSourceSealTest.java",
)
SECTIONS = {
    "classes": "--class-path", "bin": "--cmds", "conf": "--config",
    "include": "--header-files", "legal": "--legal-notices",
    "man": "--man-pages", "lib": "--libs",
}
ENV_INJECTION = (
    "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "JDK_JAVAC_OPTIONS",
    "_JAVA_OPTIONS", "CLASSPATH",
)
JMOD_DATE = "2026-10-18T00:00:00Z"
CLASSIFICATION = (
    "hybrid qualification image: stock Java 21 modules/native payload with four "
    "source-compiled M3 compiler owners, actual target descriptor and supported "
    "java.base module-hash regeneration; not a full source-built M3 JDK"
)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def now() -> str:
    return datetime.now(timezone.utc).isoformat()


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha_file(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def record(path: Path) -> dict:
    info = path.lstat()
    common = {"mode": oct(stat.S_IMODE(info.st_mode))}
    if stat.S_ISLNK(info.st_mode):
        target = os.readlink(path)
        return dict(common, type="symlink", target=target,
                    sha256=digest(os.fsencode(target)))
    require(stat.S_ISREG(info.st_mode), f"Not a regular file or symlink: {path}")
    return dict(common, type="file", size=info.st_size, sha256=sha_file(path))


def tree_records(root: Path, exclude: set[str] | None = None) -> dict:
    excluded = exclude or set()
    result = {}
    for current, directories, files in os.walk(root, followlinks=False):
        parent = Path(current)
        for name in sorted(directories[:]):
            item = parent / name
            if item.is_symlink():
                files.append(name)
                directories.remove(name)
        directories.sort()
        for name in sorted(files):
            item = parent / name
            relative = item.relative_to(root).as_posix()
            if relative not in excluded:
                result[relative] = record(item)
    return result


def write_json(path: Path, value: dict) -> None:
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(value, stream, indent=2, sort_keys=True)
        stream.write("\n")


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def within(path: Path, root: Path) -> bool:
    return path == root or root in path.parents


def hygiene(path: Path) -> None:
    data = path.read_bytes()
    text = data.decode("utf-8", errors="strict")
    require(bool(data) and data.endswith(b"\n"), f"Missing final newline: {path}")
    require(b"\r" not in data and b"\0" not in data, f"CR/NUL in source: {path}")
    require(all(line == line.rstrip(" \t") for line in text.splitlines()),
            f"Trailing whitespace in source: {path}")


class Commands:
    def __init__(self, root: Path):
        self.root = root
        self.logs = root / "logs"
        self.logs.mkdir()
        self.tmp = root / "tmp"
        self.tmp.mkdir()
        self.commands = []
        self.env = os.environ.copy()
        self.removed = sorted(name for name in ENV_INJECTION if name in self.env)
        for name in ENV_INJECTION:
            self.env.pop(name, None)
        self.env.update({"LC_ALL": "C", "TZ": "UTC", "TMPDIR": str(self.tmp)})

    def run(self, label: str, argv: list[str | Path], *, expect_failure: bool = False) -> str:
        command = [str(part) for part in argv]
        log = self.logs / f"{len(self.commands) + 1:02d}-{label}.log"
        event = {"label": label, "argv": command, "cwd": str(self.root),
                 "started": now(), "log": str(log),
                 "expect_failure": expect_failure}
        print(f"{label}: starting", flush=True)
        with log.open("xb") as stream:
            result = subprocess.run(command, cwd=self.root, env=self.env,
                                    stdout=stream, stderr=subprocess.STDOUT, check=False)
        event.update(finished=now(), exit_code=result.returncode, log_record=record(log))
        self.commands.append(event)
        with (self.root / "commands.jsonl").open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(event, sort_keys=True) + "\n")
        require((result.returncode != 0) if expect_failure else (result.returncode == 0),
                f"Unexpected exit {result.returncode}: {label}; see {log}")
        print(f"{label}: exit {result.returncode}", flush=True)
        return log.read_text(encoding="utf-8", errors="replace")


class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def take(self, length: int) -> bytes:
        result = self.data[self.pos:self.pos + length]
        require(len(result) == length, "Truncated class file")
        self.pos += length
        return result

    def u1(self) -> int:
        return self.take(1)[0]

    def u2(self) -> int:
        return struct.unpack(">H", self.take(2))[0]

    def u4(self) -> int:
        return struct.unpack(">I", self.take(4))[0]


def descriptor(data: bytes) -> dict:
    """Read module metadata without running a patched JVM or editing class files."""
    reader = Reader(data)
    require(reader.u4() == 0xCAFEBABE, "Invalid module class file")
    minor, major = reader.u2(), reader.u2()
    pool: list[str | int | None] = [None] * reader.u2()
    index = 1
    while index < len(pool):
        tag = reader.u1()
        if tag == 1:
            # Names and versions in these JDK module descriptors are ASCII.
            pool[index] = reader.take(reader.u2()).decode("utf-8", errors="strict")
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = reader.u2()
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            reader.take(4)
        elif tag in (5, 6):
            reader.take(8)
            index += 1
        elif tag == 15:
            reader.take(3)
        else:
            raise RuntimeError(f"Unsupported constant-pool tag: {tag}")
        index += 1

    def name(item: int) -> str | None:
        if item == 0:
            return None
        value = pool[item]
        if isinstance(value, int):
            value = pool[value]
        require(isinstance(value, str), "Invalid module constant-pool reference")
        return value

    flags, this_class, super_class = reader.u2(), reader.u2(), reader.u2()
    require(name(this_class) == "module-info" and super_class == 0,
            "Expected module-info class")
    require(reader.u2() == 0 and reader.u2() == 0 and reader.u2() == 0,
            "Unexpected module interfaces, fields or methods")
    result = {"minor": minor, "major": major, "class_flags": flags}
    for _ in range(reader.u2()):
        attribute = name(reader.u2())
        raw = reader.take(reader.u4())
        part = Reader(raw)
        if attribute == "Module":
            module = {"name": name(part.u2()), "flags": part.u2(),
                      "version": name(part.u2())}
            module["requires"] = sorted(
                (name(part.u2()), part.u2(), name(part.u2()))
                for _ in range(part.u2()))
            for kind in ("exports", "opens"):
                entries = []
                for _ in range(part.u2()):
                    package, entry_flags = name(part.u2()), part.u2()
                    targets = sorted(name(part.u2()) for _ in range(part.u2()))
                    entries.append((package, entry_flags, targets))
                module[kind] = sorted(entries)
            module["uses"] = sorted(name(part.u2()) for _ in range(part.u2()))
            services = []
            for _ in range(part.u2()):
                service = name(part.u2())
                providers = sorted(name(part.u2()) for _ in range(part.u2()))
                services.append((service, providers))
            module["provides"] = sorted(services)
            result[attribute] = module
        elif attribute == "ModuleHashes":
            algorithm = name(part.u2())
            hashes = {}
            for _ in range(part.u2()):
                module = name(part.u2())
                require(module not in hashes, "Duplicate recorded module hash")
                hashes[module] = part.take(part.u2()).hex()
            result[attribute] = {"algorithm": algorithm, "hashes": hashes}
        elif attribute == "ModulePackages":
            result[attribute] = sorted(name(part.u2()) for _ in range(part.u2()))
        elif attribute in ("ModuleTarget", "ModuleMainClass", "SourceFile"):
            result[attribute] = name(part.u2())
        elif attribute == "ModuleResolution":
            result[attribute] = part.u2()
        else:
            result[attribute] = {"raw_sha256": digest(raw)}
            part.take(len(raw))
        require(part.pos == len(raw), f"Unread module attribute bytes: {attribute}")
    require(reader.pos == len(data) and "Module" in result, "Invalid module descriptor")
    return result


def jmod_entries(path: Path) -> dict:
    result = {}
    with zipfile.ZipFile(path) as archive:
        for item in archive.infolist():
            relative = PurePosixPath(item.filename)
            require(not relative.is_absolute() and ".." not in relative.parts,
                    f"Unsafe JMOD entry in {path}: {item.filename}")
            require(relative.parts and relative.parts[0] in SECTIONS,
                    f"Unrecognized JMOD section: {item.filename}")
            if not item.is_dir():
                require(item.filename not in result, f"Duplicate JMOD entry: {item.filename}")
                data = archive.read(item)
                result[item.filename] = {"size": len(data), "sha256": digest(data)}
    return result


def jmod_descriptor(path: Path) -> dict:
    with zipfile.ZipFile(path) as archive:
        return descriptor(archive.read("classes/module-info.class"))


def module_hash(path: Path) -> str:
    """Match JDK ModuleHashes: sorted module resource names, then resource bytes."""
    value = hashlib.sha256()
    with zipfile.ZipFile(path) as archive:
        names = sorted(item.filename for item in archive.infolist()
                       if item.filename.startswith("classes/") and not item.is_dir())
        for name in names:
            value.update(name.removeprefix("classes/").encode("utf-8"))
            with archive.open(name) as stream:
                for block in iter(lambda: stream.read(32768), b""):
                    value.update(block)
    return value.hexdigest()


def owned_class(relative: str) -> bool:
    if relative == "module-info.class":
        return True
    return relative.endswith(".class") and any(
        relative == owner + ".class" or relative.startswith(owner + "$")
        for owner in OWNERS)


def preservation(before: dict, after: dict, compiler: bool) -> dict:
    changed = sorted(name for name in before.keys() | after.keys()
                     if before.get(name) != after.get(name))
    for name in changed:
        allowed = (name == "classes/module-info.class" or
                   (compiler and name.startswith("classes/") and
                    owned_class(name.removeprefix("classes/"))))
        require(allowed, f"Unintended JMOD payload change: {name}")
    return {"original_entry_count": len(before), "new_entry_count": len(after),
            "unchanged_entry_count": sum(before.get(name) == after.get(name)
                                         for name in before.keys() & after.keys()),
            "changed_entries": changed,
            "all_changes_confined_to_owned_classes_and_descriptor": True}


def create_jmod(runner: Commands, stock: Path, extracted: Path, target: Path,
                metadata: dict, label: str, extra: list[str | Path] | None = None) -> None:
    args: list[str | Path] = [stock / "bin/jmod", "create", "--date", JMOD_DATE]
    for section, flag in SECTIONS.items():
        directory = extracted / section
        if directory.is_dir():
            args.extend([flag, directory])
    version = metadata["Module"]["version"]
    if version:
        args.extend(["--module-version", version])
    if metadata.get("ModuleTarget"):
        args.extend(["--target-platform", metadata["ModuleTarget"]])
    if metadata.get("ModuleMainClass"):
        args.extend(["--main-class", metadata["ModuleMainClass"].replace("/", ".")])
    args.extend(extra or [])
    args.append(target)
    runner.run(label, args)


def build_lane(lane: Path, source_root: Path, stock: Path, runner: Commands,
               stock_metadata: dict, stock_entries: dict) -> dict:
    lane.mkdir()
    isolated = lane / "sources"
    isolated.mkdir()
    source_files = []
    for relative in SOURCES:
        source = source_root / relative
        hygiene(source)
        target = isolated / relative.removeprefix(SOURCE_PREFIX)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        require(record(source) == record(target), f"Source copy mismatch: {source}")
        source_files.append(target)
    write_json(lane / "SOURCE_LINT.json", {
        "scope": list(SOURCES), "utf8_lf_final_newline_no_trailing_whitespace": "PASS",
        "isolated_source_records": tree_records(isolated),
        "javac_lint": "-Xlint:all -Werror; no warning categories disabled",
    })

    extracts = lane / "extract"
    extracts.mkdir()
    for module in ("jdk.compiler", "java.base"):
        runner.run(f"{lane.name}-extract-{module}", [
            stock / "bin/jmod", "extract", "--dir", extracts / module,
            stock / "jmods" / (module + ".jmod")])
    compiler_tree = extracts / "jdk.compiler"
    classes = lane / "compiled-classes"
    classes.mkdir()
    targets = sorted({target for entry in stock_metadata["jdk.compiler"]["Module"]["exports"]
                      for target in entry[2]})
    compile_args: list[str | Path] = [
        stock / "bin/javac", "-g", "-Xlint:all", "-Werror", "-proc:none",
        "-implicit:none", "-source", "21", "-target", "21", "-XDstringConcat=inline",
        "--system", stock, "--source-path", isolated,
        "--patch-module", f"jdk.compiler={isolated}{os.pathsep}{compiler_tree / 'classes'}",
        "-d", classes,
    ]
    if targets:
        compile_args.extend(["--add-modules", ",".join(targets)])
    compile_args.extend(source_files)
    runner.run(f"{lane.name}-compile-five-explicit-sources", compile_args)
    outputs = tree_records(classes)
    require("module-info.class" in outputs, "Compiler did not emit the actual module descriptor")
    require(all(value["type"] == "file" and owned_class(name)
                for name, value in outputs.items()), "Compiler emitted an unlisted source owner")
    require(all(owner + ".class" in outputs for owner in OWNERS), "Missing compiler owner output")

    removed = {}
    for name in sorted(tree_records(compiler_tree / "classes")):
        if owned_class(name):
            path = compiler_tree / "classes" / name
            require(path.is_file() and not path.is_symlink(), f"Unexpected owned staging path: {path}")
            removed[name] = record(path)
            path.unlink()
    write_json(lane / "CLASS_OVERLAY.json", {"removed_stale_family_entries": removed,
                                             "compiled_entries": outputs})
    for name in sorted(outputs):
        target = compiler_tree / "classes" / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(classes / name, target)

    module_path = lane / "jmods"
    module_path.mkdir()
    copied = {}
    for original in sorted((stock / "jmods").glob("*.jmod")):
        if original.stem not in ("jdk.compiler", "java.base"):
            destination = module_path / original.name
            shutil.copy2(original, destination)
            require(record(destination) == record(original), f"JMOD copy mismatch: {original}")
            copied[original.name] = record(destination)

    compiler_jmod = module_path / "jdk.compiler.jmod"
    create_jmod(runner, stock, compiler_tree, compiler_jmod,
                stock_metadata["jdk.compiler"], f"{lane.name}-create-compiler-jmod")
    base_jmod = module_path / "java.base.jmod"
    require(not base_jmod.exists(), "Stale java.base must be absent during hash creation")
    old_hashes = stock_metadata["java.base"]["ModuleHashes"]
    require(old_hashes["algorithm"] == "SHA-256" and "jdk.compiler" in old_hashes["hashes"],
            "Unsupported or missing original compiler hash coverage")
    pattern = "^(?:" + "|".join(re.escape(name) for name in sorted(old_hashes["hashes"])) + ")$"
    create_jmod(runner, stock, extracts / "java.base", base_jmod,
                stock_metadata["java.base"], f"{lane.name}-create-base-and-hashes", [
                    "--hash-modules", pattern, "--module-path", module_path])
    runner.run(f"{lane.name}-supported-jmod-hash", [
        stock / "bin/jmod", "hash", "--date", JMOD_DATE, "--hash-modules", pattern,
        "--module-path", module_path, base_jmod])

    updated = {module: jmod_descriptor(module_path / (module + ".jmod"))
               for module in ("java.base", "jdk.compiler")}
    old_base = {key: value for key, value in stock_metadata["java.base"].items()
                if key != "ModuleHashes"}
    new_base = {key: value for key, value in updated["java.base"].items()
                if key != "ModuleHashes"}
    require(old_base == new_base, "Non-hash java.base descriptor metadata changed")
    new_hashes = updated["java.base"]["ModuleHashes"]
    require(new_hashes["algorithm"] == old_hashes["algorithm"] and
            new_hashes["hashes"].keys() == old_hashes["hashes"].keys(),
            "Original module-hash algorithm or coverage changed")
    witnesses = {}
    for module, recorded in sorted(new_hashes["hashes"].items()):
        observed = module_hash(module_path / (module + ".jmod"))
        require(observed == recorded, f"Module hash mismatch: {module}")
        if module != "jdk.compiler":
            require(recorded == old_hashes["hashes"][module],
                    f"Unchanged module's recorded hash changed: {module}")
        witnesses[module] = {"recorded_sha256": recorded, "computed_sha256": observed}

    compiler_module = updated["jdk.compiler"]["Module"]
    require("com/sun/source/util/M3AST$Consumer" in compiler_module["uses"],
            "Actual compiler descriptor lacks the M3 consumer uses declaration")
    providers = dict(compiler_module["provides"])
    require("com/sun/tools/javac/m3/M3ASTPC" in providers.get("com/sun/source/util/Plugin", []),
            "Actual compiler descriptor lacks the M3ASTPC plugin provider")
    for name, original in copied.items():
        require(record(module_path / name) == original, f"jmod hash changed another JMOD: {name}")
    evidence = {
        "classification": CLASSIFICATION, "source_root": str(source_root),
        "descriptor": updated, "module_hash_witnesses": witnesses,
        "base_preservation": preservation(stock_entries["java.base"],
                                           jmod_entries(base_jmod), False),
        "compiler_preservation": preservation(stock_entries["jdk.compiler"],
                                               jmod_entries(compiler_jmod), True),
        "all_other_jmods_preserved_byte_for_byte": copied,
        "hash_gate": "supported jmod create/hash; exact original coverage; no bypass",
    }
    write_json(lane / "JMOD_PROOF.json", evidence)
    runner.run(f"{lane.name}-link-descriptor-image", [
        stock / "bin/jlink", "--module-path", module_path,
        "--add-modules", "ALL-MODULE-PATH", "--output", lane / "image"])
    require((lane / "image/bin/java").is_file(), "jlink did not produce a Java launcher")
    return {"name": lane.name, "image": str(lane / "image"),
            "proof": record(lane / "JMOD_PROOF.json")}


def inputs_manifest(stock: Path, source: Path, baseline: Path | None) -> dict:
    script = Path(__file__).resolve()
    files = {str(script): record(script), str(script.with_name("README.md")):
             record(script.with_name("README.md"))}
    executable = Path(sys.executable).resolve()
    files[str(executable)] = record(executable)
    for root, relatives in ((source, SOURCES + TESTS), (baseline, SOURCES)):
        if root is not None:
            for relative in relatives:
                path = root / relative
                require(path.is_file() and not path.is_symlink(), f"Missing regular input: {path}")
                files[str(path)] = record(path)
    return {"created": now(), "stock_jdk": str(stock), "stock_tree": tree_records(stock),
            "files": files, "python_version": sys.version,
            "host_platform": platform.platform(),
            "python_library_scope": "host standard library; executable pinned, library not vendored"}


def verify_inputs(manifest: dict) -> None:
    require(tree_records(Path(manifest["stock_jdk"])) == manifest["stock_tree"],
            "Stock JDK input tree changed")
    for name, expected in manifest["files"].items():
        require(record(Path(name)) == expected, f"Input changed: {name}")


def build(args: argparse.Namespace) -> None:
    stock, source = args.stock_jdk.resolve(), args.source_root.resolve()
    baseline = args.baseline_source_root.resolve() if args.baseline_source_root else None
    output = args.output.resolve()
    require(not output.exists() and not output.is_symlink(), f"Output must be fresh: {output}")
    for original in (stock, source, baseline):
        if original is not None:
            require(not within(output, original) and not within(original, output),
                    f"Output overlaps an input tree: {original}")
    release = (stock / "release").read_text(encoding="utf-8")
    require(re.search(r'^JAVA_VERSION="21(?:[.\-"]|$)', release, re.MULTILINE) is not None,
            "This driver requires a stock Java 21 JDK")
    for tool in ("java", "javac", "jmod", "jlink"):
        require(os.access(stock / "bin" / tool, os.X_OK), f"Missing stock tool: {tool}")
    metadata, entries = {}, {}
    for module in ("java.base", "jdk.compiler"):
        path = stock / "jmods" / (module + ".jmod")
        entries[module], metadata[module] = jmod_entries(path), jmod_descriptor(path)
    require(module_hash(stock / "jmods/jdk.compiler.jmod") ==
            metadata["java.base"]["ModuleHashes"]["hashes"]["jdk.compiler"],
            "Original stock compiler hash does not match independent computation")
    inventory = inputs_manifest(stock, source, baseline)
    output.mkdir(parents=True)
    runner = None
    try:
        write_json(output / "INPUTS.json", inventory)
        write_json(output / "STOCK_JMOD_INVENTORY.json", {
            "descriptor": metadata, "entry_records": entries,
            "stock_compiler_module_hash_verified": True})
        runner = Commands(output)
        lanes = [build_lane(output / "candidate", source, stock, runner, metadata, entries)]
        if baseline:
            lanes.append(build_lane(output / "baseline", baseline, stock, runner, metadata, entries))
        test_sources, tests_dir = output / "test-sources", output / "test-classes"
        test_sources.mkdir()
        tests_dir.mkdir()
        empty = output / "empty-source-path"
        empty.mkdir()
        test_files = []
        for relative in TESTS:
            origin, target = source / relative, test_sources / Path(relative).name
            hygiene(origin)
            shutil.copy2(origin, target)
            require(record(origin) == record(target), f"Test source copy mismatch: {origin}")
            test_files.append(target)
        write_json(output / "TEST_SOURCE_LINT.json", {
            "utf8_lf_final_newline_no_trailing_whitespace": "PASS",
            "files": tree_records(test_sources)})
        runner.run("compile-two-explicit-qualification-tests", [
            stock / "bin/javac", "-g", "-Xlint:all", "-Werror", "-proc:none",
            "-implicit:none", "-source", "21", "-target", "21",
            "--system", output / "candidate/image", "--source-path", empty,
            "--add-exports", "jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED",
            "-d", tests_dir, *test_files])
        verify_inputs(inventory)
        write_json(output / "BUILD_OUTPUTS.json", {"files": tree_records(output)})
        write_json(output / "BUILD_RECEIPT.json", {
            "status": "BUILD_PASS_RUNTIME_NOT_EXECUTED", "finished": now(),
            "classification": CLASSIFICATION, "lanes": lanes,
            "inputs_manifest": record(output / "INPUTS.json"),
            "outputs_manifest": record(output / "BUILD_OUTPUTS.json"),
            "removed_environment_variable_names": runner.removed,
            "commands": runner.commands, "test_class_path": str(tests_dir),
        })
        print(f"BUILD_PASS_RUNTIME_NOT_EXECUTED {output}", flush=True)
    except BaseException as failure:
        write_json(output / "BUILD_FAILURE.json", {
            "status": "BUILD_FAILED", "finished": now(), "exception": repr(failure),
            "commands": runner.commands if runner else [],
            "output_files": tree_records(output),
        })
        raise


def validate(args: argparse.Namespace) -> None:
    output = args.output.resolve(strict=True)
    receipt = read_json(output / "BUILD_RECEIPT.json")
    require(receipt["status"] == "BUILD_PASS_RUNTIME_NOT_EXECUTED", "Build did not pass")
    require(record(output / "INPUTS.json") == receipt["inputs_manifest"], "Input manifest changed")
    require(record(output / "BUILD_OUTPUTS.json") == receipt["outputs_manifest"],
            "Build-output manifest changed")
    expected = read_json(output / "BUILD_OUTPUTS.json")["files"]
    require(tree_records(output, {"BUILD_OUTPUTS.json", "BUILD_RECEIPT.json"}) == expected,
            "Build outputs changed or a validation already exists")
    verify_inputs(read_json(output / "INPUTS.json"))
    validation = output / "validation"
    validation.mkdir()
    runner = Commands(validation)
    results = []
    try:
        for lane in receipt["lanes"]:
            name, image = lane["name"], Path(lane["image"])
            description = runner.run(name + "-describe-compiler", [
                image / "bin/java", "--describe-module", "jdk.compiler"])
            require("uses com.sun.source.util.M3AST$Consumer" in description and
                    "provides com.sun.source.util.Plugin with com.sun.tools.javac.m3.M3ASTPC"
                    in description, f"Linked {name} descriptor is missing M3 services")
            lane_tmp = runner.tmp / name
            lane_tmp.mkdir()
            java = [image / "bin/java", f"-Djava.io.tmpdir={lane_tmp}",
                    "--add-exports=jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED",
                    "-cp", receipt["test_class_path"]]
            sharing = runner.run(name + "-shared-frontend", java + ["M3ASTSharedFrontendTest"])
            require("M3_AST_SHARED_FRONTEND checks=" in sharing, "Sharing suite success marker missing")
            seal = runner.run(name + "-source-seal", java + ["M3ASTSourceSealTest"],
                              expect_failure=name == "baseline")
            if name == "baseline":
                require("capture refusal closes before a later COMPILATION event" in seal,
                        "Baseline failure was not the expected missing capture-abort witness")
                partial = sorted(path for path in lane_tmp.rglob("rejected-source.m3prim")
                                 if path.is_file() and path.stat().st_size > 0)
                require(bool(partial), "Expected baseline partial primitive image witness absent")
                results.append({"lane": name, "status": "EXPECTED_NEGATIVE_CAPTURE_ABORT_WITNESS",
                                "partial_images": {str(path): record(path) for path in partial}})
            else:
                require("M3_AST_SOURCE_SEAL checks=" in seal and "codeUnits=65536" in seal,
                        "Source-seal suite success marker missing")
                results.append({"lane": name, "status": "SHARING_AND_SOURCE_SEAL_PASS"})
        # Tests must not mutate the source/stock/build estate; only validation is writable work.
        actual = tree_records(output, {"BUILD_OUTPUTS.json", "BUILD_RECEIPT.json"})
        actual = {key: value for key, value in actual.items() if not key.startswith("validation/")}
        require(actual == expected, "Runtime validation changed a build output")
        verify_inputs(read_json(output / "INPUTS.json"))
        write_json(validation / "VALIDATION_OUTPUTS.json", {"files": tree_records(validation)})
        write_json(validation / "VALIDATION_RECEIPT.json", {
            "status": "VALIDATION_PASS", "finished": now(), "classification": CLASSIFICATION,
            "build_receipt": record(output / "BUILD_RECEIPT.json"), "results": results,
            "outputs_manifest": record(validation / "VALIDATION_OUTPUTS.json"),
            "commands": runner.commands,
            "removed_environment_variable_names": runner.removed,
        })
        print(f"VALIDATION_PASS {validation}", flush=True)
    except BaseException as failure:
        write_json(validation / "VALIDATION_FAILURE.json", {
            "status": "VALIDATION_FAILED", "finished": now(), "exception": repr(failure),
            "commands": runner.commands, "output_files": tree_records(validation),
        })
        raise


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="stage", required=True)
    builder = commands.add_parser("build", help="lint, compile and package; do not execute image")
    builder.add_argument("--stock-jdk", required=True, type=Path)
    builder.add_argument("--source-root", required=True, type=Path)
    builder.add_argument("--output", required=True, type=Path)
    builder.add_argument("--baseline-source-root", type=Path)
    validator = commands.add_parser("validate", help="verify custody, then execute qualification")
    validator.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    if args.stage == "build":
        build(args)
    else:
        validate(args)


if __name__ == "__main__":
    main()
