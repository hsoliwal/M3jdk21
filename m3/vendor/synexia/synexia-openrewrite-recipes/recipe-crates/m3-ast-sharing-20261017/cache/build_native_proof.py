# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Build the exact recipe-generated SQLite JNI donor and verify its Java ABI on Linux."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shlex
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parent
SQLITE_C_SHA3_256 = "32d5424f97e0a7fc5ed2f6335afbb58be4e0298bd7117a34e39d345ff13d859e"
SQLITE_SOURCE_ID = "2026-03-13 10:38:09 737ae4a34738ffa0c3ff7f9bb18df914dd1cad163f28fd6b6e114a344fe6d618"
SQLITE_H_SHA256 = "19585c8b5230e9d4f223bf31b709ece7b6a0bb3faf00d8310625d8e58cda1b1d"
# The unchanged shared native unit includes this sibling API. These declarations are only ABI
# proof inputs; they do not enlarge the compiler cache's Java runtime dependency closure.
ABI_DONORS = {
    "JniSwapPageBinaryOps.java": "90d592a4db43c0d4f0d6b04e32b9ded4ae12a36147a6caae94964b12dfb5246f",
    "SwapPageBinaryOps.java": "37bf0ef4bf586dd1a305ee9ae45396f7a856a22eb8b14640d1ebbbcc1a9d06e1",
    "JavaSwapPageBinaryOps.java": "16dbc821c896e4cba3ce289c741fd4ab3df35a0577e6461deea7a296db03fbfe",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--sqlite-source", type=Path, required=True)
    parser.add_argument("--cc", default="cc")
    parser.add_argument("--nm", default="nm")
    args = parser.parse_args()
    if sys.platform != "linux":
        parser.error("this finite JNI proof currently qualifies Linux only")
    sqlite = args.sqlite_source.resolve()
    sqlite_bytes = (sqlite / "sqlite3.c").read_bytes()
    if hashlib.sha3_256(sqlite_bytes).hexdigest() != SQLITE_C_SHA3_256:
        parser.error("SQLite source differs from the official 3.51.3 release SHA3-256")
    if SQLITE_SOURCE_ID.encode("ascii") not in sqlite_bytes:
        parser.error("SQLite source ID mismatch")
    if hashlib.sha256((sqlite / "sqlite3.h").read_bytes()).hexdigest() != SQLITE_H_SHA256:
        parser.error("SQLite header differs from the reviewed official release archive")
    jdk = args.jdk_home.resolve()
    publication = ROOT / "target/native"
    publication.mkdir(parents=True, exist_ok=True)
    generation = tempfile.TemporaryDirectory(prefix="m3-ast-native-proof-")
    output = Path(generation.name)
    headers = output / "headers"
    classes = output / "javac-classes"
    headers.mkdir(parents=True, exist_ok=True)
    classes.mkdir(parents=True, exist_ok=True)
    java_sources = sorted((ROOT / "target/runtime-sources").rglob("*.java"))
    if len(java_sources) != 9:
        parser.error("run the real SDK recipe qualification first; expected 9 generated runtime sources")
    abi_inputs = []
    for name, expected in sorted(ABI_DONORS.items()):
        path = "synexia-mindex/runtime/src/main/java/com/synexia/mindex/swap/" + name
        custody = ROOT / "native-abi-sources" / (path + ".txt")
        data = custody.read_bytes()
        if hashlib.sha256(data).hexdigest() != expected:
            parser.error("shared native ABI declaration differs from the pinned canonical donor: " + path)
        source = output / "abi-sources" / path
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_bytes(data)
        java_sources.append(source)
        abi_inputs.append({"source": path, "sourceSha256": expected,
                           "custody": custody.relative_to(ROOT).as_posix(),
                           "mode": "EXACT_ABI_PROOF_ONLY"})
    native_path = "synexia-mindex/runtime/native/swap/mindex_swap.c"
    native = ROOT / "target/candidate/m3-ast-cache-native-20261017/m3/vendor/synexia" / native_path
    lineage = json.loads((ROOT / "SOURCE_LINEAGE.json").read_text())
    expected_native = next(row["sourceSha256"] for row in lineage["files"] if row["source"] == native_path)
    native_hash = hashlib.sha256(native.read_bytes()).hexdigest()
    if native_hash != expected_native:
        parser.error("generated native donor differs from the sealed source lineage")
    log = []

    def run(command):
        command = [str(part) for part in command]
        result = subprocess.run(command, capture_output=True, text=True)
        log.append("$ " + shlex.join(command) + "\n" + result.stdout + result.stderr)
        (output / "native-build.log").write_text("\n".join(log))
        (publication / "native-build.log").write_text("\n".join(log))
        if result.returncode:
            raise RuntimeError(f"native proof command failed ({result.returncode}); see {output / 'native-build.log'}")
        return result.stdout

    java_version = run([jdk / "bin/javac", "-version"]).strip()
    if not java_version.startswith("javac 21"):
        parser.error("the admitted Java ABI is JDK 21")
    cc_version = run([args.cc, "--version"]).splitlines()[0]
    run([jdk / "bin/javac", "-J-Xmx256m", "--release", "21", "-Xlint:all", "-Werror",
         "-h", headers, "-d", classes, *java_sources])
    generated_headers = [headers / ("com_synexia_mindex_swap_" + name + ".h")
                         for name in ("SqliteSwapStore", "JniSwapPageBinaryOps")]
    expected_symbols = sorted(set(symbol for header in generated_headers
                                  for symbol in re.findall(r"JNICALL\s+(Java_\w+)", header.read_text())))
    if not expected_symbols:
        parser.error("javac emitted no SQLite JNI declarations")
    run([args.cc, "-std=c17", "-O2", "-fPIC", "-DSQLITE_THREADSAFE=1", "-c",
         sqlite / "sqlite3.c", "-o", output / "sqlite3.o"])
    run([args.cc, "-std=c17", "-O2", "-fPIC", "-Wall", "-Wextra", "-Werror",
         "-Wconversion", "-Wshadow", "-I", jdk / "include", "-I", jdk / "include/linux",
         "-I", sqlite, *[part for header in generated_headers for part in ("-include", header)],
         "-c", native, "-o", output / "mindex_swap.o"])
    library = output / "libsynexia_mindex_swap.so"
    run([args.cc, "-shared", "-Wl,--no-undefined", "-o", library,
         output / "mindex_swap.o", output / "sqlite3.o", "-ldl", "-lpthread", "-lm"])
    symbols = run([args.nm, "-D", "--defined-only", library])
    actual_symbols = sorted(set(re.findall(r"\b(Java_\w+)\s*$", symbols, re.MULTILINE)))
    if actual_symbols != expected_symbols:
        raise RuntimeError("javac-generated JNI declarations differ from native exports")
    receipt = {
        "status": "PASS",
        "scope": "Linux C17 / Java 21 exact-donor ABI and native build",
        "sourceRevision": lineage["sourceRevision"],
        "nativeSourceSha256": native_hash,
        "javaCompiler": java_version,
        "cCompiler": cc_version,
        "sqliteSource": "https://www.sqlite.org/2026/sqlite-amalgamation-3510300.zip",
        "sqliteRelease": "https://www.sqlite.org/releaselog/3_51_3.html",
        "sqliteSourceId": SQLITE_SOURCE_ID,
        "sqliteSourceSha3_256": SQLITE_C_SHA3_256,
        "sqliteHeaderSha256": SQLITE_H_SHA256,
        "abiProofInputs": abi_inputs,
        "jniHeaders": [{"name": header.name, "sha256": hashlib.sha256(header.read_bytes()).hexdigest()}
                       for header in generated_headers],
        "jniSymbols": expected_symbols,
        "librarySha256": hashlib.sha256(library.read_bytes()).hexdigest(),
        "freshCompilationGeneration": True,
        "behaviorGate": "pom-runtime.xml executes 2 real JNI tests and refuses missing native library",
    }
    (output / "native-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    for artifact in [library, output / "native-receipt.json", *generated_headers]:
        destination = publication / artifact.relative_to(output)
        destination.parent.mkdir(parents=True, exist_ok=True)
        pending = destination.with_name(destination.name + ".new")
        pending.write_bytes(artifact.read_bytes())
        pending.replace(destination)
    print(f"PASS: C17 strict donor compilation and {len(expected_symbols)} JNI symbols match javac -h")
    generation.cleanup()


if __name__ == "__main__":
    main()
