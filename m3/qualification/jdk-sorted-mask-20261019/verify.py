#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Offline direct-java.base proof. --replay additionally executes the pinned Synexia kernel.

Writes only a new scratch directory outside input checkouts. The target mode requires
exact postimages. Replay accepts sealed before/after sources, reconstructs the frozen
baseline, applies one production FILE per stage, and verifies each stage independently.
This is not an OpenRewrite SDK or full source-built-JDK/jtreg execution claim.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import time

BIT_TAPE = 'c64eda10cb2ca6b0180df360621ccc404c4fdfe353da3b5dda0e133a54ee40e7'
BIT_PASS = 'PASS bitmap checks=212597 tape=' + BIT_TAPE
SORT_PASS = 'checks=273297 failures=0'
LANES = ['sorted-view', 'bit-java', 'bit-native']
JAVA_OWNER = 'jdk.internal.mindex.M3BitLane28Checks'
SORT_OWNER = 'java.util.M3SortedViewChecks'
RESOURCE = 'com/synexia/rewrite/jdk-sorted-mask-20261019'
COUNTEREXAMPLES = ['duplicate-add-result', 'null-iterator-remove', 'null-lookahead-cache',
    'null-range-endpoints', 'nested-tail-retains-from', 'exclusive-tail-equality',
    'reject-invalid-top-level-range', 'empty-foreach-null', 'comparator-equivalent-membership',
    'filtered-bulk-removal', 'iterator-lookahead-removal', 'sorted-spliterator',
    'restricted-backing-bounds', 'partial-removal-on-predicate-exception']


def sha(data):
    return hashlib.sha256(data).hexdigest()


def source(root, relative):
    path = root / relative
    if not path.resolve(strict=True).is_relative_to(root):
        raise ValueError('source leaves root: ' + relative)
    return path.read_bytes()


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--target', type=Path, required=True)
    ap.add_argument('--output', type=Path, required=True)
    ap.add_argument('--fixtures', type=Path, help='Root of sealed test/jdk fixtures; defaults to target')
    ap.add_argument('--synexia', type=Path, help='Checkout with canonical recipe/resources and kernel')
    ap.add_argument('--replay', action='store_true')
    ap.add_argument('--bench', action='store_true', help='Three fresh JVM pairs; descriptive only')
    args = ap.parse_args()
    if args.replay and args.synexia is None:
        ap.error('--replay requires --synexia')
    here = Path(__file__).resolve().parent
    target = args.target.resolve(strict=True)
    fixtures = (args.fixtures or args.target).resolve(strict=True)
    synexia = args.synexia.resolve(strict=True) if args.synexia else None
    out = args.output.resolve()
    for root in [target, fixtures, here, synexia]:
        if root and (out == root or root in out.parents):
            raise ValueError('output must be outside every input root')
    seals = json.loads((here / 'source-seals.json').read_text())
    tools = {n: shutil.which(n) for n in ['java', 'javac', 'javap', 'gcc', 'nm']}
    if not all(tools.values()) or platform.system() != 'Linux':
        raise RuntimeError('Requires Linux, a Java 21 JDK, GCC and nm; no downloads')
    home = Path(tools['javac']).resolve().parent.parent
    out.mkdir(parents=True, exist_ok=False)
    evidence = out / 'evidence'; evidence.mkdir()
    records = []
    env = dict(os.environ, UBSAN_OPTIONS='halt_on_error=1')

    def run(label, command, expected=0):
        command = [str(x) for x in command]
        start = time.monotonic()
        p = subprocess.run(command, cwd=out, env=env, text=True,
                           stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=180)
        text = p.stdout
        (evidence / (label + '.txt')).write_text(text)
        records.append({'stage': label, 'command': command, 'exit': p.returncode,
                        'expected_exit': expected, 'seconds': time.monotonic()-start,
                        'output_sha256': sha(text.encode())})
        (evidence / 'commands.json').write_text(json.dumps(records, indent=2)+'\n')
        if p.returncode != expected:
            raise RuntimeError(label + ' failed: ' + text)
        return text

    run('java-version', [tools['java'], '-version'])
    run('gcc-version', [tools['gcc'], '--version'])
    baseline = out / 'baseline'; baseline.mkdir()
    for item in seals['sources']:
        data = source(target, item['path'])
        allowed = {item['after_sha256']}
        if args.replay:
            allowed.add(item['before_sha256'])
        if sha(data) not in allowed:
            raise ValueError('unqualified source drift: '+item['path'])
        if args.replay and item['before_sha256'] != item['after_sha256']:
            lane = next(l for l in LANES if (synexia / 'synexia-openrewrite-recipes/src/main/resources'
                / RESOURCE / l / ('before-'+Path(item['path']).name+'.txt')).is_file())
            data = source(synexia, 'synexia-openrewrite-recipes/src/main/resources/'+RESOURCE
                +'/'+lane+'/before-'+Path(item['path']).name+'.txt')
        expected_hash = item['before_sha256'] if args.replay else item['after_sha256']
        if sha(data) != expected_hash:
            raise ValueError('baseline template drift: '+item['path'])
        p = baseline/item['path'];p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
    for item in seals['tests']:
        data = source(fixtures, item['path'])
        if sha(data) != item['sha256']:
            raise ValueError('frozen test drift: '+item['path'])
        p = baseline/item['path'];p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
    candidate = out/'candidate';shutil.copytree(baseline,candidate)

    def compile_stage(label, tree):
        build = out/'build'/label
        patch=build/'patch';patch.mkdir(parents=True)
        headers=build/'headers';headers.mkdir()
        files=[tree/x['path'] for x in seals['sources']+seals['tests'] if x['path'].endswith('.java')]
        patch_roots=[tree/'src/java.base/share/classes']+[ (tree/x['path']).parent for x in seals['tests']]
        run(label+'-javac',[tools['javac'],'--release','21','-Xlint:all','-Werror',
            '--patch-module','java.base='+os.pathsep.join(map(str,patch_roots)),
            '-h',headers,'-d',patch,*files])
        return patch,headers

    def native(label, tree, headers, sanitize=True):
        build=out/'build'/label;build.mkdir(parents=True,exist_ok=True)
        lib=build/'libm3directtest.so'
        flags=['-fsanitize=undefined','-fno-sanitize-recover=undefined'] if sanitize else []
        run(label+'-gcc',[tools['gcc'],'-std=c17','-O2','-fPIC','-shared','-Wall','-Wextra',
            '-Werror','-Wconversion','-Wsign-conversion','-fstack-usage',*flags,
            '-I'+str(home/'include'),'-I'+str(home/'include/linux'),'-I'+str(headers),
            tree/'src/java.base/share/native/libjava/M3BitLane28.c','-o',lib])
        return lib

    def verify(label, patch, library, flags=(), baseline_failure=False):
        common=[tools['java'],*flags,'-Xcheck:jni','--patch-module','java.base='+str(patch)]
        if baseline_failure:
            text=run(label+'-sorted',common+[SORT_OWNER,'baseline'],expected=1)
            if 'checks=13 failures=14' not in text or any(x+':' not in text for x in COUNTEREXAMPLES):
                raise AssertionError('baseline did not reproduce the same 14 counterexamples')
        else:
            text=run(label+'-sorted',common+[SORT_OWNER])
            if SORT_PASS not in text:
                raise AssertionError('sorted contract result changed')
        text=run(label+'-bitmap',common+[JAVA_OWNER,library])
        if text.strip()!=BIT_PASS:
            raise AssertionError('bitmap oracle observations changed')
        print(label+': '+ ('14 known baseline failures; ' if baseline_failure else SORT_PASS+'; ')+BIT_PASS,flush=True)

    old_patch,old_headers=compile_stage('baseline',baseline)
    old_native=native('baseline-native',baseline,old_headers)
    verify('baseline',old_patch,old_native,baseline_failure=args.replay)
    final_patch,final_headers,final_native=old_patch,old_headers,old_native
    if args.replay:
        module=synexia/'synexia-openrewrite-recipes'
        kernel=module/'src/main/java/com/synexia/rewrite/M3GpuToolJoinManifest.java'
        b=kernel.read_bytes()
        if hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()!=seals['kernel_blob']:
            raise ValueError('canonical manifest kernel drift')
        classes=out/'kernel';classes.mkdir()
        run('kernel-javac',[tools['javac'],'--release','21','-Xlint:all','-Werror','-d',classes,
            kernel,module/'src/test/java/com/synexia/rewrite/M3JdkSortedMaskReplay.java'])
        for lane in LANES:
            command=[tools['java'],'-cp',str(classes)+os.pathsep+str(module/'src/main/resources'),
                'com.synexia.rewrite.M3JdkSortedMaskReplay',lane,candidate]
            if 'writes=1 refusals=5' not in run(lane+'-apply',command):
                raise AssertionError('expected one-file application')
            if lane!='bit-native':
                final_patch,final_headers=compile_stage(lane,candidate)
            else:
                final_native=native(lane,candidate,final_headers)
            verify(lane,final_patch,final_native)
            if 'writes=0 refusals=5' not in run(lane+'-replay',command):
                raise AssertionError('fixed-point changed a file')
        for item in seals['sources']:
            if sha((candidate/item['path']).read_bytes())!=item['after_sha256']:
                raise AssertionError('postimage mismatch')
    for label,flags in [('interpreter',['-Xint']),('c1',['-XX:TieredStopAtLevel=1'])]:
        verify(label,final_patch,final_native,flags)
    if args.replay:
        verify('new-java-old-jni',final_patch,old_native)
        # Exactly these existing declared public/protected surfaces must remain stable.
        for owner in ['java.util.ReverseOrderSortedSetView','jdk.internal.mindex.M3BitLane28',
                'jdk.internal.mindex.M3Address28','jdk.internal.mindex.M3Bits']:
            rel=owner.replace('.','/')+'.class'
            before=run('before-api-'+owner,[tools['javap'],'-protected','-s',old_patch/rel])
            after=run('after-api-'+owner,[tools['javap'],'-protected','-s',final_patch/rel])
            if before!=after:
                raise AssertionError('declared surface drift: '+owner)
        a=run('before-symbols',[tools['nm'],'-D','--defined-only',old_native])
        b=run('after-symbols',[tools['nm'],'-D','--defined-only',final_native])
        names=lambda text: sorted(line.split()[1:] for line in text.splitlines() if 'Java_' in line)
        if names(a)!=names(b) or (old_headers/'jdk_internal_mindex_M3BitLane28.h').read_bytes()!=(final_headers/'jdk_internal_mindex_M3BitLane28.h').read_bytes():
            raise AssertionError('native ABI/header drift')
    measurements=[]
    if args.bench:
        old_opt=native('baseline-opt',baseline,old_headers,sanitize=False)
        final_opt=native('candidate-opt',candidate,final_headers,sanitize=False)
        for pair in range(3):
            versions=[('baseline',old_patch,old_opt),('candidate',final_patch,final_opt)]
            if pair%2:versions.reverse()
            for version,patch,lib in versions:
                text=run('bench-'+version+'-'+str(pair),[tools['java'],'-Xms128m','-Xmx128m',
                    '--patch-module','java.base='+str(patch),JAVA_OWNER,lib,'bench'])
                measurements.append(dict(version=version,pair=pair,**json.loads(text)))
        (evidence/'microprobe.json').write_text(json.dumps(measurements,indent=2)+'\n')
    result={'status':'PASSED_FOCUSED_SOURCE_PROOF','target_base':seals['target_base'],
        'execution':'KERNEL_REPLAY' if args.replay else 'TARGET_POSTIMAGE_RUNTIME',
        'runtime':'Exact source patches loaded by bootstrap java.base on the recorded host JDK',
        'full_jdk_build':'NOT_RUN','jtreg_harness':'NOT_RUN','openrewrite_sdk':'NOT_RUN',
        'maven_junit':'NOT_RUN','source_postimages':[x for x in seals['sources']],
        'commands':len(records),'bitmap_assertions_per_run':212597,'sorted_assertions_per_candidate_run':273297,
        'known_baseline_contract_failures':14 if args.replay else None,
        'microprobe_scope':'one-page warm range query; not JMH or universal performance proof'}
    (evidence/'RESULT.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))


if __name__=='__main__':
    main()
