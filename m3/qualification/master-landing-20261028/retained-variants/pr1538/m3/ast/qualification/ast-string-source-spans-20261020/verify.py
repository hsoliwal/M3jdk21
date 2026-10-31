#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact receiver and strict Java21 AST/M3String occurrence proof."""
from pathlib import Path
import hashlib
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[4]
AST = ROOT / "m3/ast"
CRATE = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-source-spans-20261020"

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run(cmd):
    result = subprocess.run(cmd, cwd=ROOT, text=True, capture_output=True)
    if result.returncode:
        raise SystemExit("FAIL " + " ".join(map(str, cmd)) + "\n" + result.stdout[-3000:] + "\n" + result.stderr[-3000:])
    return result.stdout.strip()

paths = []
for line in (CRATE / "manifest.tsv").read_text().splitlines():
    if not line or line.startswith("#"):
        continue
    name, before, after, template = line.split("\t")
    if before != "ABSENT":
        raise SystemExit("FAIL expected absent preimage: " + name)
    target = ROOT / name
    source = CRATE / template
    if not target.is_file() or not source.is_file():
        raise SystemExit("FAIL missing target/recipe: " + name)
    if sha(target) != after or sha(source) != after or target.read_bytes() != source.read_bytes():
        raise SystemExit("FAIL target and recipe differ: " + name)
    paths.append(name)
if len(paths) != 2 or paths != sorted(set(paths)):
    raise SystemExit("FAIL expected two ordered unique source targets")

recipe = ROOT / "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-ast-source-spans-20261020.yml"
if "M3HashPinnedJavaSnapshotRecipe" not in recipe.read_text():
    raise SystemExit("FAIL recipe registration absent")
with tempfile.TemporaryDirectory(prefix="m3-ast-source-spans-") as temp:
    out = Path(temp)
    production = sorted(map(str, (AST / "src/main/java/com/m3/ast").glob("M3*.java")))
    test = AST / "src/test/java/com/m3/ast/M3AstSourceSpansSelfTest.java"
    run(["javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
         "-d", str(out), *production, str(test)])
    result = run(["java", "-Xmx256m", "-cp", str(out), "com.m3.ast.M3AstSourceSpansSelfTest"])
    again = run(["java", "-Xmx256m", "-cp", str(out), "com.m3.ast.M3AstSourceSpansSelfTest"])
    if result != again or not result.startswith("M3_AST_STRING_SPANS_PASS checks="):
        raise SystemExit("FAIL source span nondeterminism: " + result + " / " + again)
    print(result)
print("PASS sealed_targets=2 java21=strict recipe=fixed_postimage")
print("PENDING source-built SDK image / jtreg / JNI / JCK / native memory")
