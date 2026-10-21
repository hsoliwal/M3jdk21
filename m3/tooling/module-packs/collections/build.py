#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build the optional M3 collection modules with the existing JMOD/image packager."""
# Modified 2026: package the complete existing mask/window and path-mask JNI owners.
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
             "com.m3.util.M3PrimitiveIndexViewFilterContract")
# These are the existing javac-generated ABI names, not a count-only allowlist.
NATIVE_OWNERS = {
    "M3IndexMaskCompactor": ("m3_index_mask_compactor.c", (
        "selectWindowNative", "selectLongWindowNative", "positionsAbiNative",
        "selectPositionsNative", "positionsMasksAbiNative", "selectPositionsMasksNative",
        "rangeAbiNative", "selectRangeNative", "selectNative")),
    "M3IndexMaskWindow": ("m3_index_mask_window.c", ("scanNative", "scanBoundedNative")),
    "M3IndexPathMasks": ("m3_index_path_masks.c", ("selectNative",)),
}
NATIVE_LIBRARIES = {
    "m3_collections_mask": ("M3IndexMaskCompactor", "M3IndexMaskWindow"),
    "m3_collections_pathmask": ("M3IndexPathMasks",),
}
NATIVE_CONTRACTS = (
    ("M3IndexMaskCompactorSelfTest", "PASS M3IndexMaskCompactorSelfTest"),
    ("M3IndexMaskWindowSelfTest", "PASS M3_INDEX_MASK_WINDOW"),
    ("M3BudgetedMaskWindowContract", "BUDGETED_MASK_WINDOW_PASS"),
    ("M3IndexPathMasksSelfTest", "PASS M3IndexPathMasksSelfTest"),
    ("M3PhysicalStateSnapshotProof", "PASS M3PhysicalStateSnapshotProof"),
    ("M3MaskedSortFusionContract", "PASS M3MaskedSortFusionContract"),
)


def expected_symbols(owner):
    return {"Java_com_m3_collections_" + owner + "_" + method
            for method in NATIVE_OWNERS[owner][1]}


def require_symbols(actual, expected, label):
    """Refuse missing, unknown or duplicate JNI entries before packaging."""
    actual = list(actual)
    if len(actual) != len(set(actual)):
        raise ValueError("duplicate JNI symbol in " + label)
    missing, unexpected = set(expected) - set(actual), set(actual) - set(expected)
    if missing or unexpected:
        raise ValueError(label + " JNI ABI differs: missing=" + repr(sorted(missing))
                         + " unexpected=" + repr(sorted(unexpected)))


def header_symbols(text):
    return re.findall(r"\bJNICALL\s+(Java_[A-Za-z0-9_]+)\s*\(", text)


def export_symbols(text):
    return [line.split()[-1] for line in text.splitlines()
            if line.split() and line.split()[-1].startswith("Java_")]


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
    for name in CONTRACTS:
        command(output / (label + "-" + name.rsplit(".", 1)[1] + ".log"), java,
                "-Xcheck:jni", *common, "--patch-module", "jdk.m3.collections=" + str(patch),
                "-m", "jdk.m3.collections/" + name, expected="PASS")
    test_root = repo / "m3/collections/src/test/java/com/m3/collections"
    native_tests = [test_root / (name + ".java") for name, _ in NATIVE_CONTRACTS]
    native_patch = output / (label + "-native-contracts")
    native_patch.mkdir()
    command(output / (label + "-native-oracle-javac.log"), javac,
            "-source", "21", "-target", "21", "-Xlint:all,-module", "-Werror", "-proc:none",
            *common, "--patch-module", "com.m3.collections=" + str(test_root),
            "-d", native_patch, *native_tests)
    library_path = [] if linked else ["-Djava.library.path=" + str(native)]
    for name, marker in NATIVE_CONTRACTS:
        for mode, arguments in (("java", ()), ("native", ("--native",))):
            command(output / (label + "-" + mode + "-" + name + ".log"), java,
                    "-Xcheck:jni", *library_path, *common,
                    "--patch-module", "com.m3.collections=" + str(native_patch),
                    "-m", "com.m3.collections/com.m3.collections." + name,
                    *arguments, expected=marker)


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
    native_root = repo / "m3/collections/src/main/native/collections"
    native_sources = [native_root / source for source, _ in NATIVE_OWNERS.values()]
    oracle_sources = [repo / "m3/collections/src/test/java/com/m3/collections" / (name + ".java")
                      for name, _ in NATIVE_CONTRACTS]
    oracle_sources += [pack / "src/test/java" / (name.replace(".", "/") + ".java")
                       for name in CONTRACTS]
    packager = repo / "m3/tooling/jmod-recipes/tools/jmod_pack.py"
    inputs = [*collection_sources, *view_sources, *native_sources, *oracle_sources,
              packager, Path(__file__).resolve()]
    for source in inputs:
        source.resolve(strict=True)
    if not collection_sources or not (pack / "src/main/java/module-info.java").is_file():
        raise ValueError("complete collection source modules required")
    output.mkdir(parents=True)
    modules, jars, native, headers = (output / name for name in ("modules", "jars", "native", "headers"))
    for directory in (modules, jars, native, headers):
        directory.mkdir()
    source_receipt = ["path\tsha256"]
    source_receipt.extend(str(path.relative_to(repo)) + "\t" + digest(path)
                          for path in sorted(set(inputs)))
    (output / "COMPILED_INPUTS.tsv").write_text("\n".join(source_receipt) + "\n", encoding="utf-8")
    for name, sources in (("com.m3.collections", collection_sources), ("jdk.m3.collections", view_sources)):
        command(output / (name + "-javac.log"), home / "bin/javac", "--release", "21",
                "-Xlint:all,-module", "-Werror", "-proc:none", "-h", headers,
                "-d", modules / name, *sources)
    objects = output / "native-objects"
    objects.mkdir()
    native_receipts = {}
    for library_name, owners in NATIVE_LIBRARIES.items():
        library = native / ("lib" + library_name + ".so")
        compiled = []
        expected = set()
        for owner in owners:
            header = headers / ("com_m3_collections_" + owner + ".h")
            required = expected_symbols(owner)
            require_symbols(header_symbols(header.read_text()), required, owner + " generated header")
            if expected.intersection(required):
                raise ValueError("duplicate JNI owner in " + library_name)
            expected.update(required)
            source = native_root / NATIVE_OWNERS[owner][0]
            obj = objects / (owner + ".o")
            command(output / (owner + "-native-compile.log"), os.environ.get("CC", "cc"),
                    "-std=c17", "-O2", "-fPIC", "-Wall", "-Wextra", "-Werror",
                    "-I" + str(home / "include"), "-I" + str(home / "include/linux"),
                    "-include", header, "-c", source, "-o", obj)
            compiled.append(obj)
        command(output / (library_name + "-native-link.log"), os.environ.get("CC", "cc"),
                "-shared", "-o", library, *compiled)
        symbols = command(output / (library_name + "-native-symbols.log"),
                          "nm", "-D", "--defined-only", library)
        require_symbols(export_symbols(symbols), expected, library_name + " exports")
        native_receipts[library_name] = {"symbols": sorted(expected),
            "source_owners": list(owners), "native_sha256": digest(library),
            "generated_headers": {owner: digest(headers / ("com_m3_collections_" + owner + ".h"))
                                  for owner in owners}}
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
    # The existing packager passes --strip-debug to jlink. Compare against that
    # exact packaging transform; a raw object-file hash would reject valid images.
    for library_name, proof in native_receipts.items():
        library = native / ("lib" + library_name + ".so")
        linked_library = image / "lib" / library.name
        normalized_library = output / (library_name + "-reference-strip-debug.so")
        command(output / (library_name + "-strip-debug-reference.log"), "objcopy", "--strip-debug",
                library, normalized_library)
        if not linked_library.is_file() or digest(linked_library) != digest(normalized_library):
            raise ValueError("linked native library differs from exact strip-debug reference: " + library_name)
        symbols = command(output / (library_name + "-linked-symbols.log"),
                          "nm", "-D", "--defined-only", linked_library)
        require_symbols(export_symbols(symbols), proof["symbols"], library_name + " linked exports")
        proof["linked_native_sha256"] = digest(linked_library)
    qualify(image / "bin/java", image / "bin/javac", modules, pack, repo, output, native, True)
    receipt = {"status": "PASS", "modules": ["com.m3.collections", "jdk.m3.collections"],
               "existing_collection_source_files": len(collection_sources),
               "optional_projection_source_files": len(view_sources),
               "native_libraries": native_receipts,
               "native_contracts": [name for name, _ in NATIVE_CONTRACTS],
               "native_packaging_transform": "objcopy --strip-debug reference; exact linked byte parity",
               "native_required_on_host_and_image": True, "jdk_home": str(home),
               "toolchain_release_sha256": digest(home / "release"),
               "host_platform": platform.platform(),
               "scope": "Linux optional source modules and actual JNI/JMOD linked-image proof; not a full custom HotSpot build or java.util receiving promotion"}
    (output / "M3_COLLECTIONS_PACK_PROOF.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print("M3_COLLECTIONS_PACK_PASS modules=2 native_required=true host_and_image=true")


if __name__ == "__main__":
    main()
