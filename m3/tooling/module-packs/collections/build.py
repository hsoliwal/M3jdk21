#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build the optional M3 collection modules with the existing JMOD/image packager."""
import argparse
import hashlib
import json
import os
import platform
import re
import subprocess
import sys
from pathlib import Path

PORT_CLASSES = ("M3PrimitiveIndexView", "M3PrimitiveSoaStore", "M3PrimitiveKind",
                "M3DenseHash", "M3SortableTopK", "M3RuntimeHeap")
CONTRACTS = ("jdk.m3.collections.M3FilteredViewsContract",
             "jdk.m3.collections.M3MutableFilteredViewsContract",
             "com.m3.util.M3PrimitiveIndexViewFilterContract",
             "com.m3.util.M3PrimitiveSelectionWordContract")
OWNER_CONTRACTS = (
    "M3StrictFilteredSequencedContract", "M3FilteredNavigationReconciliationContract",
    "M3FilteredViewsContract", "M3MutableFilteredViewsContract",
    "M3FilteredSetIdentityContract", "M3FilteredNavigableSetRangeContract",
    "M3BoundedFilteredNavigationContract")
NATIVE_CONTRACTS = (
    ("M3IndexMaskCompactorSelfTest", ("--native",)),
    ("M3IndexMaskWindowRecoverySelfTest", ("--native",)),
    ("M3RequiredExcludedMaskSelectorNativeContract", ()),
    ("M3MaskNativeParityProof", ()))
NATIVE_SYMBOL = "Java_com_m3_collections_M3IndexMaskCompactor_selectNative"


def command(log, *args, expected=None):
    argv = [str(value) for value in args]
    result = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, encoding="utf-8", timeout=240)
    log.write_text("$ " + json.dumps(argv) + "\n" + result.stdout
                   + "\nEXIT " + str(result.returncode) + "\n", encoding="utf-8")
    if result.returncode:
        raise RuntimeError("command failed: " + str(log))
    if expected is not None and expected not in result.stdout:
        raise RuntimeError("missing required proof marker: " + str(log))
    return result.stdout


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def qualify(java, javac, modules, pack, repo, output, native, linked):
    label = "image" if linked else "host"
    patch = output / (label + "-contracts")
    patch.mkdir()
    tests = [pack / "src/test/java" / (name.replace(".", "/") + ".java")
             for name in CONTRACTS]
    common = ["--add-modules", "jdk.m3.collections,com.m3.collections"]
    common += ["--add-reads", "jdk.m3.collections=com.m3.collections"]
    if not linked:
        common += ["--module-path", str(modules)]
    # Linked javac uses the image's actual modules; --release would hide nonstandard APIs.
    command(output / (label + "-contracts-javac.log"), javac, "-source", "21", "-target", "21",
            "-Xlint:all,-module", "-Werror", "-proc:none", *common,
            "--patch-module", "jdk.m3.collections=" + str(pack / "src/test/java"),
            "-d", patch, *tests)
    for mode in ((), ("-Xint",)):
        suffix = "-int" if mode else ""
        for name in CONTRACTS:
            command(output / (label + "-" + name.rsplit(".", 1)[1] + suffix + ".log"), java,
                    *mode, "-Xcheck:jni", *common,
                    "--patch-module", "jdk.m3.collections=" + str(patch),
                    "-m", "jdk.m3.collections/" + name, expected="PASS")
    owner_tests = repo / "m3/collections/src/test/java/com/m3/collections"
    owner_patch = output / (label + "-owner-contracts")
    owner_patch.mkdir()
    names = [*OWNER_CONTRACTS, *(name for name, _ in NATIVE_CONTRACTS)]
    command(output / (label + "-owner-oracles-javac.log"), javac,
            "-source", "21", "-target", "21", "-Xlint:all,-module", "-Werror", "-proc:none",
            *common, "--patch-module", "com.m3.collections=" + str(owner_tests),
            "-d", owner_patch, *(owner_tests / (name + ".java") for name in names))
    library_path = [] if linked else ["-Djava.library.path=" + str(native)]
    for mode in ((), ("-Xint",)):
        suffix = "-int" if mode else ""
        for name in OWNER_CONTRACTS:
            command(output / (label + "-owner-" + name + suffix + ".log"), java,
                    *mode, "-Xcheck:jni", *common,
                    "--patch-module", "com.m3.collections=" + str(owner_patch),
                    "-m", "com.m3.collections/com.m3.collections." + name)
        for name, arguments in NATIVE_CONTRACTS:
            command(output / (label + "-native-" + name + suffix + ".log"), java,
                    *mode, "-Xcheck:jni", *library_path, *common,
                    "--patch-module", "com.m3.collections=" + str(owner_patch),
                    "-m", "com.m3.collections/com.m3.collections." + name,
                    *arguments, expected="PASS")


def verify_exports(header, symbols):
    """The generated Java declarations own the complete ABI, including additive capabilities."""
    declared = set(re.findall(r"JNIEXPORT\s+\w+\s+JNICALL\s+(Java_\w+)\s*\(", header))
    exported = {line.split()[-1] for line in symbols.splitlines()
                if line.split() and line.split()[-1].startswith("Java_")}
    if NATIVE_SYMBOL not in declared:
        raise ValueError("existing JNI header contract is missing")
    if exported != declared:
        raise ValueError("JNI exports differ from generated declarations: missing="
                         + repr(sorted(declared - exported)) + " extra="
                         + repr(sorted(exported - declared)))
    return sorted(declared)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    home = args.jdk_home.resolve(strict=True)
    repo = args.repo_root.resolve(strict=True)
    pack = repo / "m3/tooling/module-packs/collections"
    if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', (home / "release").read_text(), re.MULTILINE):
        raise ValueError("Java 21 development toolchain required")
    if platform.system() != "Linux":
        raise ValueError("the existing POSIX JNI peer is currently qualified on Linux")
    output = args.out.resolve()
    if output.is_relative_to(home) or home.is_relative_to(output):
        raise ValueError("output must be separate from the supplied JDK")
    if output.exists():
        raise FileExistsError("fresh output directory required: " + str(output))
    collection_sources = sorted((repo / "m3/collections/src/main/java").rglob("*.java"))
    view_sources = sorted((pack / "src/main/java").rglob("*.java"))
    port = repo / "m3/ports/collections/src/main/java/com/m3/util"
    view_sources.extend(port / (name + ".java") for name in PORT_CLASSES)
    native_source = repo / "m3/collections/src/main/native/collections/m3_index_mask_compactor.c"
    packager = repo / "m3/tooling/jmod-recipes/tools/jmod_pack.py"
    for source in [*collection_sources, *view_sources, native_source, packager]:
        source.resolve(strict=True)
    if not collection_sources or not (pack / "src/main/java/module-info.java").is_file():
        raise ValueError("complete collection source modules required")
    output.mkdir(parents=True)
    modules, jars, native, headers = (output / name for name in ("modules", "jars", "native", "headers"))
    for directory in (modules, jars, native, headers):
        directory.mkdir()
    contract_sources = [pack / "src/test/java" / (name.replace(".", "/") + ".java")
                        for name in CONTRACTS]
    contract_sources.extend(repo / "m3/collections/src/test/java/com/m3/collections" / (name + ".java")
                            for name in [*OWNER_CONTRACTS, *(n for n, _ in NATIVE_CONTRACTS)])
    inputs = sorted([*collection_sources, *view_sources, *contract_sources,
                     native_source, packager, Path(__file__).resolve()])
    input_hashes = {str(path.relative_to(repo)): digest(path) for path in inputs}
    source_receipt = ["path\tsha256"]
    source_receipt.extend(str(path.relative_to(repo)) + "\t" + digest(path)
                          for path in inputs)
    (output / "COMPILED_INPUTS.tsv").write_text("\n".join(source_receipt) + "\n", encoding="utf-8")
    for name, sources in (("com.m3.collections", collection_sources), ("jdk.m3.collections", view_sources)):
        command(output / (name + "-javac.log"), home / "bin/javac", "--release", "21",
                "-Xlint:all,-module", "-Werror", "-proc:none", "-h", headers,
                "-d", modules / name, *sources)
    header = headers / "com_m3_collections_M3IndexMaskCompactor.h"
    if NATIVE_SYMBOL not in header.read_text():
        raise ValueError("existing JNI header contract is missing")
    library = native / "libm3_collections_mask.so"
    command(output / "native-compile.log", os.environ.get("CC", "cc"), "-std=c17", "-O2",
            "-fPIC", "-shared", "-Wall", "-Wextra", "-Werror",
            "-I" + str(home / "include"), "-I" + str(home / "include/linux"),
            "-include", header, "-o", library, native_source)
    symbols = command(output / "native-symbols.log", "nm", "-D", "--defined-only", library)
    native_symbols = verify_exports(header.read_text(), symbols)
    command(output / "native-header-cxx20.log", os.environ.get("CXX", "c++"),
            "-std=c++20", "-Wall", "-Wextra", "-Werror", "-x", "c++", "-fsyntax-only",
            "-I" + str(home / "include"), "-I" + str(home / "include/linux"), header)
    qualify(home / "bin/java", home / "bin/javac", modules, pack, repo, output, native, False)
    rows = ["artifact\tversion\tjar\tmodule\trequires\tdisposition"]
    for module in ("com.m3.collections", "jdk.m3.collections"):
        jar = jars / (module + ".jar")
        command(output / (module + "-jar.log"), home / "bin/jar", "--create", "--file", jar,
                "--date", "2026-01-01T00:00:00Z", "-C", modules / module, ".")
        command(output / (module + "-descriptor.log"), home / "bin/jar", "--describe-module",
                "--file", jar, expected=module)
        rows.append("\t".join((module, "1", str(jar), module, "java.base", "JMOD")))
    image_tsv, manifest = output / "INPUT_JMOD_IMAGE.tsv", output / "EMPTY_DESCRIPTOR_MANIFEST.tsv"
    image_tsv.write_text("\n".join(rows) + "\n", encoding="utf-8")
    manifest.write_text("module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n", encoding="utf-8")
    command(output / "jmod-pack.log", sys.executable, packager, "--jdk-home", home,
            "--image-tsv", image_tsv, "--manifest", manifest, "--layer-root", repo,
            "--out", output / "pack", "--legal-notices", pack / "legal",
            "--native-library", "com.m3.collections=" + str(native),
            "--add-modules", "java.se,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps")
    image = output / "pack/image"
    for tool in ("java", "javac", "jar", "jmod", "jlink", "jdeps"):
        if not os.access(image / "bin" / tool, os.X_OK):
            raise ValueError("missing developer tool: " + tool)
    linked_library = image / "lib" / library.name
    # The existing packager passes --strip-debug to jlink. Compare against that
    # exact packaging transform; a raw object-file hash would reject valid images.
    normalized_library = output / "reference-strip-debug.so"
    command(output / "native-strip-debug-reference.log", "objcopy", "--strip-debug",
            library, normalized_library)
    if not linked_library.is_file() or digest(linked_library) != digest(normalized_library):
        raise ValueError("linked native library differs from the exact strip-debug reference")
    qualify(image / "bin/java", image / "bin/javac", modules, pack, repo, output, native, True)
    if input_hashes != {str(path.relative_to(repo)): digest(path) for path in inputs}:
        raise ValueError("source or contract changed during qualification")
    receipt = {"status": "PASS", "modules": ["com.m3.collections", "jdk.m3.collections"],
               "existing_collection_source_files": len(collection_sources),
               "optional_projection_source_files": len(view_sources),
               "native_symbol": NATIVE_SYMBOL, "native_symbols": native_symbols,
               "generated_header_sha256": digest(header),
               "execution_modes": ["mixed", "interpreted"],
               "contract_mains_per_mode_and_runtime": len(CONTRACTS) + len(OWNER_CONTRACTS) + len(NATIVE_CONTRACTS), "native_sha256": digest(library),
               "linked_native_sha256": digest(linked_library),
               "native_packaging_transform": "objcopy --strip-debug reference; exact linked byte parity",
               "native_required_on_host_and_image": True, "jdk_home": str(home),
               "toolchain_release_sha256": digest(home / "release"),
               "host_platform": platform.platform(),
               "scope": "Linux optional source modules and actual JNI/JMOD linked-image proof; toolchain provenance is identified separately; no java.util family receiving promotion"}
    (output / "M3_COLLECTIONS_PACK_PROOF.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print("M3_COLLECTIONS_PACK_PASS modules=2 native_required=true host_and_image=true")


if __name__ == "__main__":
    main()
