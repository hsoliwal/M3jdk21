#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Finite linked-image proof of the exact upstream shared compiler contract.

Only a new isolated scratch directory receives assembled classes, jmods and images.
The source checkout and original JDK are read-only inputs. Standard jmod hashing
updates the scratch java.base descriptor's compiler hash; no check is disabled.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time
import zipfile

HERE = Path(__file__).resolve().parent
EVIDENCE = HERE / "evidence"
PATCH_SHA = "3f50d59cca1cf7cf2811c0bb1da9314b71240c7f81f37854ed6fc021f03681b4"
PINS = {
    "test/langtools/tools/javac/m3/M3ASTSharedFrontendTest.java":
        ("41eee89134939047e81f93b61bdda8c38ef805ed", "47349ef82633f724f3ca694521250048ba70486b094b069fc51c13b5e64978a5"),
    "src/jdk.compiler/share/classes/module-info.java":
        ("13ebbaf1db2e8ba997e48c317f4b64bf8f0cfb1d", "ae52d1734570fee7404aeb68b65ef86ea23ad322b72ff0b137c9039c71aafb09"),
}
OWNERS = {
    "com/sun/source/util/M3AST",
    "com/sun/tools/javac/m3/M3ASTPC",
    "com/sun/tools/javac/m3/M3PrimitiveImageCompiler",
    "com/sun/tools/javac/main/JavaCompiler",
}
SECTIONS = {"classes": "--class-path", "bin": "--cmds", "conf": "--config",
            "lib": "--libs", "include": "--header-files", "legal": "--legal-notices",
            "man": "--man-pages"}
FAMILIES = ["sharing", "lazyAndFailureMemoization", "editedSource", "separatePhases",
            "callbackFailure", "explicitCallbackClose", "cleanupFailure", "pluginAndImage",
            "providerClassLoader", "generatedSources"]


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def file_sha(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def entries(path: Path) -> dict[str, str]:
    with zipfile.ZipFile(path) as archive:
        require(len(archive.namelist()) == len(set(archive.namelist())), "duplicate archive entries: " + str(path))
        return {name: sha(archive.read(name)) for name in archive.namelist() if not name.endswith("/")}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jdk", type=Path, default=Path(os.environ.get("JAVA_HOME", "")))
    parser.add_argument("--patch", type=Path, default=HERE.parent / "target/m3-javac-patch.jar")
    parser.add_argument("--work-dir", type=Path, help="New, nonexistent isolated directory; default creates a unique /tmp directory")
    args = parser.parse_args()
    jdk = args.jdk.resolve()
    patch = args.patch.resolve()
    require((jdk / "jmods/jdk.compiler.jmod").is_file(), "Java 21 JDK with original jmods is required")
    require(file_sha(patch) == PATCH_SHA, "unqualified compiler patch bytes")
    if args.work_dir is None:
        TARGET = Path(tempfile.mkdtemp(prefix="m3-ast-upstream-contract-"))
    else:
        TARGET = args.work_dir.resolve()
        require(not TARGET.exists(), "proof work directory must be new")
        TARGET.mkdir(parents=True)
    EVIDENCE.mkdir(exist_ok=True)
    logs = TARGET / "logs"
    logs.mkdir()
    commands = []
    receipt = {"schema": "M3_UPSTREAM_SHARED_FRONTEND_LINKED_PROOF_V1", "status": "in-progress",
               "source_commit": "4e29dc9d4141261e8763fbe9ffd6a20e017b1aff",
               "patch_sha256": PATCH_SHA, "upstream_test_families": FAMILIES,
               "source_lineage_sha256": file_sha(HERE / "SOURCE_LINEAGE.json"),
               "proof_work_directory": str(TARGET),
               "no_product_edits": True, "full_jdk_or_jck_claim": False}

    def run(label: str, command: list[str], timeout: int = 300) -> str:
        start = time.monotonic()
        path = logs / (label + ".log")
        with path.open("w") as output:
            result = subprocess.run(command, cwd=TARGET, text=True, stdout=output,
                                    stderr=subprocess.STDOUT, timeout=timeout)
        commands.append({"step": label, "argv": command, "exit_code": result.returncode,
                         "elapsed_seconds": round(time.monotonic() - start, 3), "log": path.name,
                         "log_sha256": file_sha(path)})
        shutil.copyfile(path, EVIDENCE / path.name)
        (EVIDENCE / "COMMANDS.json").write_text(json.dumps(commands, indent=2) + "\n")
        print(json.dumps({"step": label, "exit_code": result.returncode}), flush=True)
        require(result.returncode == 0, "proof command failed: " + label)
        return path.read_text()

    def describe(label: str, path: Path) -> str:
        return run(label, [str(jdk / "bin/jmod"), "describe", str(path)])

    def create_jmod(name: str, expanded: Path, description: str, extra: list[str] | None = None) -> Path:
        version = re.search(r"^" + re.escape(name) + r"@([^\s]+)$", description, re.M)
        platform = re.search(r"^platform ([^\s]+)$", description, re.M)
        require(version is not None and platform is not None, "missing original jmod metadata")
        command = [str(jdk / "bin/jmod"), "-J-Xmx384m", "create", "--date=2026-10-17T00:00:00Z"]
        # javac's new compiler descriptor has no version/platform decoration.
        # The stock base descriptor already has both; JDK21's extender appends
        # ModuleTarget rather than replacing it, so preserve that original field.
        if name == "jdk.compiler":
            command.extend(("--module-version", version[1], "--target-platform", platform[1]))
        actual = {part.name for part in expanded.iterdir() if part.is_dir()}
        require(actual <= SECTIONS.keys(), "unknown jmod section")
        for section, option in SECTIONS.items():
            if (expanded / section).exists():
                command.extend((option, str(expanded / section)))
        if extra:
            command.extend(extra)
        destination = TARGET / "jmods" / (name + ".jmod")
        command.append(str(destination))
        run("create-" + name, command)
        return destination

    try:
        version = run("original-java-version", [str(jdk / "bin/java"), "-version"])
        require(re.search(r'version "21(?:\.|\")', version) is not None, "proof requires Java 21")
        originals = {name: jdk / "jmods" / (name + ".jmod") for name in ("java.base", "jdk.compiler")}
        receipt["original_jmods_sha256"] = {name: file_sha(path) for name, path in originals.items()}
        sources = {}
        for path, (expected_blob, expected_sha) in PINS.items():
            data = (HERE / "custody" / (path + ".txt")).read_bytes()
            require(sha(data) == expected_sha, "upstream custody SHA drift: " + path)
            require(hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest() == expected_blob,
                    "upstream custody blob drift: " + path)
            destination = TARGET / "sources" / path
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(data)
            sources[path] = destination
        test_source = sources["test/langtools/tools/javac/m3/M3ASTSharedFrontendTest.java"]
        test_text = test_source.read_text()
        for family in FAMILIES:
            require(test_text.count("        " + family + "();") == 1, "exact upstream main family drift")
        staged = TARGET / "jmods"
        staged.mkdir()
        for source in sorted((jdk / "jmods").glob("*.jmod")):
            if source.stem not in originals:
                (staged / source.name).symlink_to(source)

        compiler_desc = describe("original-compiler-descriptor", originals["jdk.compiler"])
        expanded_compiler = TARGET / "expanded-compiler"
        run("extract-compiler", [str(jdk / "bin/jmod"), "-J-Xmx256m", "extract", "--dir",
                                 str(expanded_compiler), str(originals["jdk.compiler"])])
        classes = expanded_compiler / "classes"
        patch_entries = {}
        with zipfile.ZipFile(patch) as archive:
            require(len(archive.namelist()) == len(set(archive.namelist())), "duplicate patch entry")
            for name in archive.namelist():
                if name.endswith("/") or name == "META-INF/MANIFEST.MF":
                    continue
                require(name.endswith(".class") and name[:-6].split("$", 1)[0] in OWNERS,
                        "patch outside the four qualified source owners: " + name)
                destination = classes / name
                destination.parent.mkdir(parents=True, exist_ok=True)
                data = archive.read(name)
                destination.write_bytes(data)
                patch_entries["classes/" + name] = sha(data)
        require(all("classes/" + owner + ".class" in patch_entries for owner in OWNERS), "incomplete four-source patch")
        run("compile-exact-descriptor", [str(jdk / "bin/javac"), "-J-Xmx256m", "-proc:none", "-Xlint:all", "-Werror",
            "--patch-module", "jdk.compiler=" + str(classes), "--add-modules", "jdk.javadoc,jdk.jshell,jdk.jdeps",
            "-d", str(classes), str(sources["src/jdk.compiler/share/classes/module-info.java"])])
        compiler = create_jmod("jdk.compiler", expanded_compiler, compiler_desc)
        original_entries = entries(originals["jdk.compiler"])
        compiler_entries = entries(compiler)
        require(compiler_entries.keys() == original_entries.keys() | patch_entries.keys(), "compiler jmod entry set changed unexpectedly")
        for name, digest in original_entries.items():
            if name == "classes/module-info.class":
                continue
            require(compiler_entries[name] == patch_entries.get(name, digest), "non-overlay compiler entry changed: " + name)
        require(all(compiler_entries[name] == digest for name, digest in patch_entries.items()), "patch overlay did not survive jmod assembly")

        base_desc = describe("original-base-descriptor", originals["java.base"])
        hashes = dict(re.findall(r"^hashes (\S+) SHA-256 (\w+)$", base_desc, re.M))
        require("jdk.compiler" in hashes, "expected original compiler hash in java.base descriptor")
        expanded_base = TARGET / "expanded-base"
        run("extract-base", [str(jdk / "bin/jmod"), "-J-Xmx256m", "extract", "--dir", str(expanded_base), str(originals["java.base"])])
        helper_classes = TARGET / "proof-tools"
        helper_classes.mkdir()
        helper = HERE / "proof/JmodDescriptorInputs.java"
        exports = ["--add-exports", "java.base/jdk.internal.classfile=ALL-UNNAMED",
                   "--add-exports", "java.base/jdk.internal.classfile.attribute=ALL-UNNAMED"]
        run("compile-jmod-descriptor-input-helper", [str(jdk / "bin/javac"), "-J-Xmx128m", "-proc:none", "-Xlint:all", "-Werror",
                                                    *exports, "-d", str(helper_classes), str(helper)])
        run("prepare-generated-base-attributes", [str(jdk / "bin/java"), "-Xmx128m", *exports,
                                                   "-cp", str(helper_classes), "JmodDescriptorInputs",
                                                   str(expanded_base / "classes/module-info.class")])
        # The module being created is represented by jmod's standard synthetic
        # descriptor during hashing, so the old recorded hash is replaced by an
        # actual computed hash rather than disabled or deleted.
        pattern = "^(?:" + "|".join(re.escape(name) for name in sorted(hashes)) + ")$"
        base = create_jmod("java.base", expanded_base, base_desc,
                           ["--module-path", str(staged), "--hash-modules", pattern])
        original_base_entries = entries(originals["java.base"])
        base_entries = entries(base)
        require(base_entries.keys() == original_base_entries.keys(), "base jmod entry set changed")
        changed_base = [name for name in original_base_entries if original_base_entries[name] != base_entries[name]]
        require(changed_base == ["classes/module-info.class"], "non-descriptor stock java.base bytes changed")
        updated_desc = describe("rehashed-base-descriptor", base)
        updated_hashes = dict(re.findall(r"^hashes (\S+) SHA-256 (\w+)$", updated_desc, re.M))
        require(updated_hashes.keys() == hashes.keys(), "recorded module hash names changed")
        require([name for name in hashes if hashes[name] != updated_hashes[name]] == ["jdk.compiler"],
                "hash update affected a module besides the composed compiler")
        structural = lambda text: sorted(line for line in text.splitlines() if line and not line.startswith("hashes "))
        require(structural(base_desc) == structural(updated_desc), "java.base module contract changed")
        receipt["assembly"] = {"compiler_patch_class_count": len(patch_entries), "compiler_source_owner_count": 4,
            "compiler_jmod_sha256": file_sha(compiler), "base_jmod_sha256": file_sha(base),
            "compiler_stock_entries_preserved_except_exact_overlay_and_descriptor": True,
            "base_only_changed_entry": "classes/module-info.class", "base_structural_descriptor_unchanged": True,
            "base_prepared_input_attributes_recomputed_by_jmod": ["ModulePackages", "ModuleHashes"],
            "descriptor_input_helper_sha256": file_sha(helper),
            "base_recorded_hash_count": len(hashes), "base_only_updated_hash": "jdk.compiler",
            "old_compiler_module_hash": hashes["jdk.compiler"], "new_compiler_module_hash": updated_hashes["jdk.compiler"],
            "hash_checks_disabled": False, "patch_class_sha256": patch_entries}
        image = TARGET / "image"
        run("jlink", [str(jdk / "bin/jlink"), "-J-Xmx512m", "--module-path", str(staged),
                      "--add-modules", "jdk.compiler", "--output", str(image)])
        modules = run("linked-modules", [str(image / "bin/java"), "--list-modules"])
        receipt["linked_modules"] = modules.splitlines()
        receipt["linked_image_modules_sha256"] = file_sha(image / "lib/modules")
        linked_desc = run("linked-compiler-descriptor", [str(image / "bin/java"), "--describe-module", "jdk.compiler"])
        require("uses com.sun.source.util.M3AST$Consumer" in linked_desc, "linked provider uses clause absent")
        require("provides com.sun.source.util.Plugin with com.sun.tools.javac.m3.M3ASTPC" in linked_desc,
                "linked builtin Plugin provider absent")
        test_classes = TARGET / "test-classes"
        test_classes.mkdir()
        export = "jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED"
        run("compile-exact-upstream-test", [str(image / "bin/javac"), "-J-Xmx256m", "-proc:none", "-Xlint:all", "-Werror",
                                           "--add-exports", export, "-d", str(test_classes), str(test_source)])
        output = run("exact-upstream-contract", [str(image / "bin/java"), "-Xmx512m", "--add-exports", export,
                                                 "-cp", str(test_classes), "M3ASTSharedFrontendTest"])
        checks = re.findall(r"^M3_AST_SHARED_FRONTEND checks=(\d+)$", output, re.M)
        require(len(checks) == 1 and int(checks[0]) > 0, "upstream successful-main receipt absent")
        receipt["status"] = "passed"
        receipt["upstream_checks"] = int(checks[0])
        receipt["exact_test_unmodified"] = True
        receipt["uses_linked_module_descriptor_without_runtime_patch"] = True
        receipt["recheck_final_product_patch_sha256"] = PATCH_SHA
        receipt["commands"] = "COMMANDS.json"
        receipt["evidence_sha256"] = {path.name: file_sha(path) for path in sorted(EVIDENCE.iterdir())
                                      if path.is_file() and path.name != "QUALIFICATION.json"}
    except BaseException as failure:
        receipt["status"] = "failed"
        receipt["failure"] = str(failure)
        raise
    finally:
        (EVIDENCE / "QUALIFICATION.json").write_text(json.dumps(receipt, indent=2) + "\n")
        print(json.dumps({"status": receipt["status"], "upstream_checks": receipt.get("upstream_checks")}), flush=True)


if __name__ == "__main__":
    main()
