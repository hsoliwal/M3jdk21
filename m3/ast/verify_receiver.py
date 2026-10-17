#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Bounded Java21 receiver proof; no Synexia runtime, JDK-image or JNI claims."""
from pathlib import Path
import hashlib
import os
import subprocess
import sys
import tempfile

THIS = Path(__file__).resolve().parent
ROOT = THIS.parents[1]
SOURCE = THIS / 'src/main/java/com/m3/ast/M3ASTHierarchy.java'
TEST = THIS / 'src/test/java/com/m3/ast/M3ASTHierarchyContract.java'
BORROW = THIS / 'SOURCE_BORROW.tsv'

def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run(cmd):
    p = subprocess.run(cmd,capture_output=True,text=True)
    if p.returncode: raise RuntimeError(f'FAIL exit={p.returncode} {cmd} stderr={p.stderr[-900:]}')
    return p.stdout.strip()

fields = dict(line.split('\t',1) for line in BORROW.read_text().splitlines()[1:])
if digest(SOURCE) != fields['target_sha256']:
    raise SystemExit('FAIL: generated receiver source drift')
if fields['source_recipe'] != 'com.synexia.rewrite.M3ASTL0L5Target20261017':
    raise SystemExit('FAIL: missing canonical recipe identity')
if not TEST.is_file() or not SOURCE.is_file():
    raise SystemExit('FAIL: incomplete Java sources')

classes=[]
for iteration in range(2):
    with tempfile.TemporaryDirectory(prefix='m3ast-receiver-') as directory:
        output=Path(directory)
        run(['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',str(output),str(SOURCE),str(TEST)])
        observed=run(['java','-Xmx256m','-cp',str(output),'com.m3.ast.M3ASTHierarchyContract','2000'])
        if 'checks=39550' not in observed or 'atoms=17967' not in observed:
            raise AssertionError('unexpected target corpus result: '+observed)
        classes.append({p.relative_to(output).as_posix():digest(p) for p in sorted(output.rglob('*.class'))})
        print('PASS run',iteration,observed)
if classes[0] != classes[1]:
    raise AssertionError('classfile repeat drift')
print('PASS receiver source pinned; repeat classfiles',len(classes[0]))
if (ROOT/'m3/collections/src/main/java/com/m3/collections').is_dir():
    census=run(['python3',str(THIS/'collection_source_census.py')])
    print('SOURCE_CENSUS_ONLY',len([s for s in census.splitlines() if '\t' in s])-1)
else:
    print('COLLECTION_SOURCE_CENSUS=NOT_RUN; full target checkout unavailable')
print('MAVEN_OPENREWRITE_JTREG_JNI_IMAGE=NOT_RUN')
