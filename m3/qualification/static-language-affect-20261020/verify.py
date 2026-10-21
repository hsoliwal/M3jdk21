#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Focused source-seal, Java21 and JNI proof for static language + affect geometry."""
from pathlib import Path
import hashlib
import os
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[3]
AST = ROOT / "m3/ast"
LEX = ROOT / "m3/lexicon/receiver"
RESOURCES = ROOT / "m3/tooling/migration-recipes/src/main/resources"
JAVA_CRATE = (
    RESOURCES
    / "com/synexia/rewrite/hash-pinned-java/m3-static-language-affect-20261020"
)
TEXT_CRATE = (
    RESOURCES
    / "com/m3/rewrite/backport/jdk21-hash-pinned-text"
    / "m3-static-language-affect-text-20261020"
)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify_manifest(crate, expected_before, minimum_rows):
    manifest = crate / "manifest.tsv"
    if not manifest.is_file():
        raise SystemExit("FAIL missing recipe manifest: " + str(manifest))
    rows = []
    for line in manifest.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        target_name, before, after, template_name = line.split("\t")
        target = ROOT / target_name
        template = crate / template_name
        if before != expected_before:
            raise SystemExit("FAIL unexpected recipe preimage for " + target_name)
        if not target.is_file() or not template.is_file():
            raise SystemExit("FAIL missing target/template " + target_name)
        if sha(target) != after or sha(template) != after:
            raise SystemExit("FAIL recipe SHA mismatch " + target_name)
        if target.read_bytes() != template.read_bytes():
            raise SystemExit("FAIL recipe byte mismatch " + target_name)
        rows.append(target_name)
    if len(rows) < minimum_rows or rows != sorted(rows) or len(rows) != len(set(rows)):
        raise SystemExit("FAIL recipe manifest order/count/uniqueness " + str(manifest))
    return rows


def run(args, *, cwd=ROOT, env=None):
    result = subprocess.run(args, cwd=cwd, text=True, capture_output=True, env=env)
    if result.returncode:
        print(result.stdout[-5000:], file=sys.stderr)
        print(result.stderr[-5000:], file=sys.stderr)
        raise SystemExit("FAIL: " + " ".join(str(x) for x in args))
    return result.stdout.strip()


java_rows = verify_manifest(JAVA_CRATE, "ABSENT", 10)
text_rows = verify_manifest(TEXT_CRATE, "ABSENT", 7)

java_yaml = RESOURCES / "META-INF/rewrite/m3-static-language-affect-20261020.yml"
text_yaml = RESOURCES / "META-INF/rewrite/m3-static-language-affect-text-20261020.yml"
if "crateName: m3-static-language-affect-20261020" not in java_yaml.read_text(encoding="utf-8"):
    raise SystemExit("FAIL Java recipe registration")
if "crateName: m3-static-language-affect-text-20261020" not in text_yaml.read_text(encoding="utf-8"):
    raise SystemExit("FAIL text recipe registration")

ast_sources = sorted((AST / "src/main/java/com/m3/ast").glob("M3*.java"))
ast_test = AST / "src/test/java/com/m3/ast/M3AffectGeometrySelfTest.java"
lex_sources = sorted((LEX / "src/main/java/com/m3/lexicon").glob("M3*.java"))
lex_test = LEX / "src/test/java/com/m3/lexicon/M3StaticLanguageGeometryProof.java"

with tempfile.TemporaryDirectory(prefix="m3-language-affect-") as directory:
    out = Path(directory)
    ast_out = out / "ast"
    lex_out = out / "lex"
    ast_out.mkdir()
    lex_out.mkdir()

    run([
        "javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
        "-d", str(ast_out), *map(str, ast_sources), str(ast_test)
    ])
    affect = run([
        "java", "-Xmx256m", "-cp", str(ast_out),
        "com.m3.ast.M3AffectGeometrySelfTest"
    ])
    if not affect.startswith("M3_AFFECT_GEOMETRY_PASS"):
        raise SystemExit("FAIL affect proof: " + affect)

    run([
        "javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
        "-d", str(lex_out), *map(str, lex_sources), str(lex_test)
    ])
    pure = run([
        "java", "-Xmx256m", "-cp", str(lex_out),
        "com.m3.lexicon.M3StaticLanguageGeometryProof"
    ])
    if not pure.startswith("M3_STATIC_LANGUAGE_PASS"):
        raise SystemExit("FAIL Java language proof: " + pure)

    native_build = out / "native"
    run([
        "cmake", "-S", str(LEX / "native"), "-B", str(native_build),
        "-DCMAKE_BUILD_TYPE=Release"
    ])
    run(["cmake", "--build", str(native_build), "--config", "Release"])

    library_dirs = [native_build, native_build / "Release"]
    library_path = os.pathsep.join(str(value) for value in library_dirs)
    native = run([
        "java", "-Xcheck:jni", "-Xmx256m", f"-Djava.library.path={library_path}",
        "-cp", str(lex_out), "com.m3.lexicon.M3StaticLanguageGeometryProof",
        "--native-required"
    ])
    if "native=true" not in native:
        raise SystemExit("FAIL JNI kernel not used: " + native)

    print("SOURCE_SEAL_PASS java=" + str(len(java_rows)) + " text=" + str(len(text_rows)))
    print(affect)
    print(pure)
    print(native)
    print("PASS Java21=strict JNI=C11 Xcheck:jni parity=required")
    print("FULL_JDK_IMAGE_JTREG_HOTSPOT_GC=NOT_CLAIMED")
