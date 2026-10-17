#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Source-sealed focused proof, not matched JDK/JNI/jtreg authority."""
from pathlib import Path
import hashlib, subprocess, tempfile

ROOT=Path(__file__).resolve().parents[2]
HERE=ROOT/'m3/ast'
SOURCE=HERE/'src/main/java/com/m3/ast/M3ASTHierarchyMaskSelection.java'
PARENT=HERE/'src/main/java/com/m3/ast/M3ASTHierarchy.java'
TEST=HERE/'src/test/java/com/m3/ast/M3ASTHierarchyMaskSelectionContract.java'
PIN=HERE/'SOURCE_BORROW_MASK_SELECTION.tsv'

def hash(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def run(*args):
    result=subprocess.run(list(args),capture_output=True,text=True)
    if result.returncode: raise RuntimeError(f'failed {args} stdout={result.stdout[-650:]} stderr={result.stderr[-900:]}')
    return result.stdout.strip()

def main():
    fields=dict(line.split('\t',1) for line in PIN.read_text().splitlines()[1:])
    if fields['status'] != 'CANDIDATE_ONLY_HOLD': raise AssertionError('unexpected promotion state')
    if fields['source_commit'].startswith('PENDING_'): raise AssertionError('donor Git pin not yet published')
    if hash(SOURCE) != fields['target_sha256']: raise AssertionError('source drift')
    observed=[]; classes=[]
    for _ in range(2):
        with tempfile.TemporaryDirectory(prefix='m3ast-mask-') as directory:
            out=Path(directory)
            run('javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',str(out),str(PARENT),str(SOURCE),str(TEST))
            result=run('java','-Xmx256m','-cp',str(out),'com.m3.ast.M3ASTHierarchyMaskSelectionContract')
            if 'checks=189905' not in result: raise AssertionError(result)
            observed.append(result)
            classes.append({p.relative_to(out).as_posix():hash(p) for p in out.rglob('*.class')})
    if classes[0] != classes[1] or observed[0]!=observed[1]: raise AssertionError('unstable bytecode or receipts')
    print('PASS M3AST mask selection source sealed; Java21 differential',observed[0])
    print('PASS byte-identical class files',len(classes[0]))
    print('FULL_MAVEN_OPENREWRITE_JTREG_JNI=NOT_RUN')

if __name__=='__main__': main()
