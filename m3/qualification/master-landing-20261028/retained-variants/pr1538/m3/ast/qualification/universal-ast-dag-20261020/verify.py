#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact-source and dependency-free Java21 proof for universal M3 AST/DAG mechanics."""
from pathlib import Path
import hashlib
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[4]
AST = ROOT / "m3/ast"
CRATE = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-universal-ast-dag-20261020"
MANIFEST = CRATE / "manifest.tsv"

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run(args):
    result = subprocess.run(args, cwd=ROOT, text=True, capture_output=True)
    if result.returncode:
        raise SystemExit("FAIL " + " ".join(args) + "\n" + result.stdout[-3000:] + "\n" + result.stderr[-3000:])
    return result.stdout.strip()

rows = []
for line in MANIFEST.read_text(encoding="utf-8").splitlines():
    if not line or line.startswith("#"):
        continue
    path, before, after, template = line.split("\t")
    if before != "ABSENT":
        raise SystemExit("FAIL universal AST/DAG recipe must create from ABSENT preimage: " + path)
    target = ROOT / path
    source = CRATE / template
    if not target.is_file() or not source.is_file():
        raise SystemExit("FAIL missing target/template " + path)
    if sha(target) != after or sha(source) != after or target.read_bytes() != source.read_bytes():
        raise SystemExit("FAIL exact postimage/template mismatch " + path)
    rows.append(path)

if rows != sorted(rows) or len(rows) != len(set(rows)):
    raise SystemExit("FAIL manifest must be sorted and unique")
if len(rows) != 9:
    raise SystemExit("FAIL expected 9 sealed Java targets")

production = sorted(str(p) for p in (AST / "src/main/java/com/m3/ast").glob("M3*.java"))
selftest = AST / "src/test/java/com/m3/ast/M3UniversalAstDagSelfTest.java"
with tempfile.TemporaryDirectory(prefix="m3-universal-ast-dag-") as directory:
    out = Path(directory)
    run(["javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
         "-d", str(out), *production, str(selftest)])
    first = run(["java", "-Xmx256m", "-cp", str(out), "com.m3.ast.M3UniversalAstDagSelfTest"])
    second = run(["java", "-Xmx256m", "-cp", str(out), "com.m3.ast.M3UniversalAstDagSelfTest"])
    if first != second or not first.startswith("M3_UNIVERSAL_AST_DAG_PASS checks="):
        raise SystemExit("FAIL nondeterministic/unexpected proof: " + first + " / " + second)
    print(first)

yaml = ROOT / "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-universal-ast-dag-20261020.yml"
if "M3HashPinnedJavaSnapshotRecipe" not in yaml.read_text(encoding="utf-8"):
    raise SystemExit("FAIL missing reusable hash-pinned recipe registration")

print("PASS exact targets=9 recipe=fixed-postimage Java21=strict repeated=2")
print("FULL_JDK_IMAGE_JTREG_JNI=NOT_CLAIMED")
