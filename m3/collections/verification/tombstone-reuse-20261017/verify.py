#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed M3JDK21 selected-source receiving proof for Synexia tombstone reuse.

Run inside a full M3JDK21 git checkout with JDK21. No checkout writes.
Source templates and OpenRewrite recipe are canonically owned by Synexia.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile

FILES = [
    'm3/collections/src/main/java/com/m3/collections/M3LongHashSet.java',
    'm3/collections/src/main/java/com/m3/collections/M3LongLongHashMap.java',
    'm3/collections/src/main/java/com/m3/collections/M3LongCollection.java',
    'm3/collections/src/main/java/com/m3/collections/M3LongIterator.java',
    'm3/collections/src/main/java/com/m3/collections/M3LongLongConsumer.java',
]
ROOT = Path(__file__).resolve().parents[4]
CONFIG = Path(__file__).with_name('receiver.json')
TEST = ROOT / 'm3/collections/src/test/java/com/m3/collections/M3TombstoneReuseSelfTest.java'
TARGETS = FILES[:2]


def digest(data):
    return hashlib.sha256(data).hexdigest()


def command(args, cwd, receipt, expected=0, contains=None):
    task = subprocess.run(args, cwd=cwd, capture_output=True, text=True, timeout=180)
    receipt.append({'args':args, 'exit':task.returncode, 'expected_exit':expected,
                    'stdout':task.stdout, 'stderr':task.stderr})
    if (task.returncode == 0) != (expected == 0):
        raise AssertionError(f'unexpected command exit: {args}: {task.stdout}\n{task.stderr}')
    if contains and contains not in (task.stdout + task.stderr):
        raise AssertionError(f'missing expected witness {contains}: {args}')
    return task


def compare(source_dir, label, root, out, receipts, test):
    destination = out / f'classes-{label}'
    destination.mkdir()
    inputs = [str(source_dir / relative) for relative in FILES] + [str(test)]
    command(['javac','--release','21','-proc:none','-Xlint:all','-Werror',
             '-d',str(destination),*inputs],root,receipts)
    for owner in ['M3LongHashSet','M3LongLongHashMap']:
        args=['javap','-public','-s','-classpath',str(destination),
              'com.m3.collections.'+owner]
        public = command(args,root,receipts).stdout
        (out/f'api-{label}-{owner}.txt').write_text(public)
    return destination


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=ROOT)
    parser.add_argument('--output', type=Path)
    args=parser.parse_args()
    root=args.root.resolve()
    cfg=json.loads(CONFIG.read_text())
    if root.resolve() != ROOT.resolve():
        raise AssertionError('proof script not checked out at supplied root')
    if not TEST.is_file():
        raise AssertionError('missing receiver contract')
    if cfg['target_files'] != TARGETS:
        raise AssertionError('changed source surface drift')
    original = {}
    for path in FILES:
        body = subprocess.run(['git','show',cfg['base_commit']+':'+path],cwd=root,
                              capture_output=True,check=True).stdout
        original[path]=body
        after=(root/path).read_bytes()
        if path in TARGETS:
            expected=cfg['sources'][path]['before']
            admitted=cfg['sources'][path]['after']
            if digest(body)!=expected:
                raise AssertionError('frozen preimage mismatch: '+path)
            if digest(after)!=admitted:
                raise AssertionError('installed postimage mismatch: '+path)
        elif body!=after:
            raise AssertionError('unexpected dependency change: '+path)
    if digest(TEST.read_bytes()) != cfg['test_sha256']:
        raise AssertionError('test drift')
    receipts=[]
    with tempfile.TemporaryDirectory(prefix='m3-tombstone-proof-') as temp:
        out=Path(temp)
        baseline=out/'baseline'
        baseline.mkdir()
        for path,source in original.items():
            file=baseline/path
            file.parent.mkdir(parents=True,exist_ok=True)
            file.write_bytes(source)
        old=compare(baseline,'old',root,out,receipts,TEST)
        command(['java','-Xmx64m','-cp',str(old),'com.m3.collections.M3TombstoneReuseSelfTest'],
                root,receipts,expected=1,contains='allocated array despite reachable deleted slot')
        new=compare(root,'new',root,out,receipts,TEST)
        command(['java','-Xmx64m','-cp',str(new),'com.m3.collections.M3TombstoneReuseSelfTest'],
                root,receipts,contains='PASS M3TombstoneReuseSelfTest')
        again=compare(root,'repeat',root,out,receipts,TEST)
        for owner in ['M3LongHashSet','M3LongLongHashMap']:
            if (out/f'api-old-{owner}.txt').read_bytes() != (out/f'api-new-{owner}.txt').read_bytes():
                raise AssertionError('public descriptor drift: '+owner)
        cls=lambda folder:{p.name:digest(p.read_bytes()) for p in folder.rglob('*.class')}
        if cls(new)!=cls(again):
            raise AssertionError('repeat compilation differs')
        summary={'status':'PASS_SELECTED_SOURCE_JAVA21','base_commit':cfg['base_commit'],
                 'synexia_recipe_commit':cfg['synexia_recipe_commit'],
                 'compiled_class_files':len(cls(new)), 'command_count':len(receipts),
                 'api_descriptors':'IDENTICAL','repeat_classes':'IDENTICAL',
                 'jtreg':'NOT_RUN','full_maven_reactor':'NOT_RUN','native_jni':'UNCHANGED_NOT_RUN',
                 'hotspot_image':'NOT_RUN','performance':'PAYLOAD_ONLY_NO_BENCHMARK'}
        dest=(args.output or root/'m3/collections/verification/tombstone-reuse-20261017/receipts').resolve()
        dest.mkdir(parents=True,exist_ok=True)
        (dest/'SUMMARY.json').write_text(json.dumps(summary,indent=2)+'\n')
        (dest/'COMMANDS.json').write_text(json.dumps(receipts,indent=2)+'\n')
        print(json.dumps(summary,indent=2))

if __name__=='__main__':
    try: main()
    except (OSError, ValueError, AssertionError, subprocess.SubprocessError) as error:
        print('FAIL: '+str(error),file=sys.stderr)
        sys.exit(1)
