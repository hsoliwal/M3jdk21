#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Thin M3JDK21 product receiver: exact source seal, javac21, API and contracts.

Not a second recipe implementation. Canonical source generation belongs to Synexia.
Requires a full checkout or the exact seven-file selected source closure.
"""
import argparse
import hashlib
from pathlib import Path
import subprocess
import sys
import tempfile

NAMES = ('M3IntHashSet', 'M3IntIntHashMap', 'M3IntLongHashMap')
DEPENDENCIES = ('M3IntCollection', 'M3IntIterator', 'M3IntIntConsumer', 'M3IntLongConsumer')


def sha(data): return hashlib.sha256(data).hexdigest()


def run():
    cli=argparse.ArgumentParser(description=__doc__)
    cli.add_argument('--root',required=True,type=Path)
    args=cli.parse_args()
    root=args.root.resolve(strict=True)
    here=Path(__file__).resolve().parent
    source='m3/collections/src/main/java/com/m3/collections/'
    test='m3/collections/src/test/java/com/m3/collections/M3ZeroSlotCapacityContract.java'
    manifest=(here/'SOURCE_MANIFEST.tsv').read_text().splitlines()
    rows=[line.split('\t') for line in manifest if line and not line.startswith('#')]
    if len(rows)!=3:raise AssertionError('expected three exact target sources')
    # Additive chronological admission. Never overwrite the original SOURCE_MANIFEST.
    # A successor must identify the exact original postimage and a single reviewed
    # original-owner source; arbitrary code drift remains a hard failure.
    successors={}
    successor_file=here/'SOURCE_SUCCESSORS.tsv'
    if successor_file.exists():
        additions=[line.split('\t') for line in successor_file.read_text().splitlines()
                   if line and not line.startswith('#')]
        if len(additions)!=1 or len(additions[0])!=5:
            raise AssertionError('expected exactly one source-qualified successor')
        current,previous,approved,recipe_ref,git_blob=additions[0]
        source_target=source+'M3IntIntHashMap.java'
        historical={entry[0]:entry[2] for entry in rows}
        if (current!=source_target or historical.get(current)!=previous
                or recipe_ref!='hsoliwal/com.synexia#12402'
                or len(git_blob)!=40 or any(c not in '0123456789abcdef' for c in git_blob)
                or len(approved)!=64 or any(c not in '0123456789abcdef' for c in approved)):
            raise AssertionError('invalid successor source authority')
        successors[current]=(previous,approved)
    found={}
    for path,old,post,template in rows:
        if path!=source+template.replace('.java.txt','.java'):
            raise AssertionError('unexpected target path')
        f=root/path
        if f.is_symlink():raise AssertionError('symlink not an admitted source')
        data=f.read_bytes()
        actual=sha(data)
        if actual!=post and successors.get(path)!=(post,actual):
            raise AssertionError('receiver source changed outside canonical recipe: '+path)
        found[path.split('/')[-1].removesuffix('.java')]=f
    if tuple(found)!=NAMES:raise AssertionError('manifest order changed')
    for name in DEPENDENCIES:
        if not (root/source/(name+'.java')).is_file():
            raise AssertionError('missing actual product dependency '+name)
    with tempfile.TemporaryDirectory(prefix='m3-zero-slot-') as tmp:
        classes=Path(tmp)/'classes';classes.mkdir()
        cmd=['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',str(classes),
             *(str(root/source/(n+'.java')) for n in DEPENDENCIES),
             *(str(found[n]) for n in NAMES),str(root/test)]
        r=subprocess.run(cmd,text=True,capture_output=True,timeout=75)
        if r.returncode:raise AssertionError('Java21 compiler failed: '+r.stderr+r.stdout)
        for name in NAMES:
            actual=subprocess.run(['javap','-public','-s','-classpath',str(classes),
                                    'com.m3.collections.'+name],text=True,capture_output=True,timeout=15)
            if actual.returncode:raise AssertionError(actual.stderr)
            baseline=(here/(name+'-public.javap')).read_text()
            if actual.stdout!=baseline:raise AssertionError('public method ABI changed: '+name)
        test_run=subprocess.run(['java','-ea','-Xmx128m','-cp',str(classes),
                    'com.m3.collections.M3ZeroSlotCapacityContract'],
                    text=True,capture_output=True,timeout=75)
        if test_run.returncode or 'PASS M3ZeroSlotCapacityContract' not in test_run.stdout:
            raise AssertionError('contract failed '+test_run.stdout+test_run.stderr)
        print(test_run.stdout.strip())
    print('PASS M3JDK21 receiver exact postimages=3 public descriptors=3 Java21 selected-source compile')

if __name__=='__main__':
    try:run()
    except (OSError,AssertionError,subprocess.SubprocessError) as failure:
        print('FAIL '+str(failure),file=sys.stderr);sys.exit(1)
