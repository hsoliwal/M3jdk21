#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Focused Java21/JNI verification for static language + affect geometry."""
from pathlib import Path
import os
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[3]
AST = ROOT / "m3/ast"
LEX = ROOT / "m3/lexicon/receiver"


def run(args, *, cwd=ROOT, env=None):
    result = subprocess.run(args, cwd=cwd, text=True, capture_output=True, env=env)
    if result.returncode:
        print(result.stdout[-5000:], file=sys.stderr)
        print(result.stderr[-5000:], file=sys.stderr)
        raise SystemExit("FAIL: " + " ".join(str(x) for x in args))
    return result.stdout.strip()


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
    run(["cmake", "-S", str(LEX / "native"), "-B", str(native_build),
         "-DCMAKE_BUILD_TYPE=Release"])
    run(["cmake", "--build", str(native_build), "--config", "Release"])

    library_dirs = [native_build, native_build / "Release"]
    library_path = os.pathsep.join(str(path) for path in library_dirs)
    native = run([
        "java", "-Xmx256m", f"-Djava.library.path={library_path}",
        "-cp", str(lex_out), "com.m3.lexicon.M3StaticLanguageGeometryProof",
        "--native-required"
    ])
    if "native=true" not in native:
        raise SystemExit("FAIL JNI kernel not used: " + native)

    print(affect)
    print(pure)
    print(native)
    print("PASS Java21=strict JNI=C11 parity=required")
    print("FULL_JDK_IMAGE_JTREG_HOTSPOT_GC=NOT_CLAIMED")
