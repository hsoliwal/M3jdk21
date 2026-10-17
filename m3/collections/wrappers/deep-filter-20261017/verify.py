#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Qualify/reuse the pinned Java/Maven replay adapter; never patch an emitted target.

Run in an exclusively owned workspace. SOURCE checkout is read-only. OUTPUT must
be absent. The OpenRewrite SDK/Maven lifecycle is an independent, explicit gate;
this script executes the incumbent Java replay adapter, not an SDK substitute.
"""
import argparse
import fcntl
import hashlib
import json
import os
import re
from pathlib import Path
import shutil
import subprocess

P = Path('m3/collections/wrappers/deep-filter-20261017')
J = Path('m3/collections/wrappers/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-deep-filter-20261017')
T = Path('m3/collections/wrappers/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-deep-filter-native-20261017')
NATIVE = Path('m3/collections/wrappers/direct-projection-20261017/m3_map_cursor_jni.c')
sha = lambda b: hashlib.sha256(b).hexdigest()
gitblob = lambda b: hashlib.sha1(b'blob ' + str(len(b)).encode() + b'\0' + b).hexdigest()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', type=Path, required=True)
    ap.add_argument('--output', type=Path, required=True)
    args = ap.parse_args()
    root = args.root.resolve(strict=True)
    out = args.output.absolute()
    if out.exists() or out.is_symlink():
        raise ValueError('output must be absent')
    for parent in out.parents:
        if parent.is_symlink():
            raise ValueError('symlinked output ancestor')
    out.mkdir(parents=True)
    evidence = out/'evidence'; evidence.mkdir()
    commands = []

    def run(name, argv, expected=0, signature=None):
        proc = subprocess.run([str(a) for a in argv], cwd=root, capture_output=True,
                              text=True, timeout=180)
        text = proc.stdout + proc.stderr
        (evidence/(name+'.log')).write_text('$ '+repr([str(a) for a in argv])+'\n'+text+'\nEXIT='+str(proc.returncode)+'\n')
        commands.append({'name':name, 'exit':proc.returncode, 'expected_exit':expected,
                         'log_sha256':sha((evidence/(name+'.log')).read_bytes())})
        if (expected == 0 and proc.returncode != 0) or (expected != 0 and proc.returncode == 0):
            raise RuntimeError(name+'\n'+text[-8000:])
        if signature is not None and signature not in text:
            raise AssertionError(name+': expected signature missing: '+signature)
        return text

    def write(path, data):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    def snapshot(directory):
        return {str(p.relative_to(directory)):sha(p.read_bytes()) for p in directory.rglob('*')
                if p.is_file() and not p.is_symlink() and 'target' not in p.relative_to(directory).parts}

    binding = json.loads((root/P/'REPLAY_BINDINGS.json').read_text())
    original = (root/P/'donor/Replay.java.txt').read_bytes()
    assert gitblob(original) == binding['donor_git_blob'] == 'e01bc8c2fac834a6d683dc62836a9528a92db5e1'
    bound = original.decode()
    for before, after in binding['literal_bindings'].items():
        assert bound.count(before) == 1
        bound = bound.replace(before, after)
    assert bound.encode() == (root/P/'replay/Replay.java').read_bytes()
    assert sha(bound.encode()) == binding['bound_sha256']
    manifest = (root/P/'replay-inputs/manifest.tsv').read_bytes()
    assert sha(manifest) == binding['manifest_sha256']
    rows = [line.split('\t') for line in manifest.decode().splitlines()]
    assert len(rows) == 6 and rows == sorted(rows) and len({row[0] for row in rows}) == 6
    canonical = (root/J/'manifest.tsv').read_text()+(root/T/'manifest.tsv').read_text()
    assert sorted(line.split('\t') for line in canonical.splitlines()) == rows
    pins = {'M3CollectionViews.java':'2750647c832d5bec07940b032ad2a2f66f425072',
            'M3FilteredMaps.java':'c3779cb4dd07e71d28fbc547ff1991256919ace5'}
    for path, before, after, template in rows:
        src = root/(T if path.endswith('.c') else J)/template
        assert sha(src.read_bytes()) == after
        assert src.read_bytes() == (root/P/'replay-inputs'/template).read_bytes()
        if before != 'ABSENT':
            pre = root/J/'pre'/template
            assert sha(pre.read_bytes()) == before
            assert gitblob(pre.read_bytes()) == pins[Path(path).name]
    native = (root/NATIVE).read_bytes()
    assert gitblob(native) == '0fa362205377e67d82e1c66479704d94936dbebe'
    classes = out/'replay-classes'; classes.mkdir()
    run('compile-replay', ['javac','--release','21','-proc:none','-Xlint:all','-Werror',
                          '-d',classes,root/P/'replay/Replay.java'])
    adapter = ['java','-cp',classes,'Replay']

    def seed(destination):
        destination.mkdir()
        for path, before, after, template in rows:
            if before != 'ABSENT': write(destination/path, (root/J/'pre'/template).read_bytes())
        write(destination/'untouched.txt', b'preserve me\n')
        return destination

    owner = rows[0][0]
    absent = next(row[0] for row in rows if row[1] == 'ABSENT')
    cases = out/'refusals'; cases.mkdir()
    refused = []
    for label in ['source-drift','missing-original','occupied-new','mixed-state','leaf-symlink','parent-symlink','directory-target','check-pre','invalid-mode','ownership-lock']:
        target = seed(cases/label)
        mode = '--apply'
        if label == 'source-drift': (target/owner).write_bytes((target/owner).read_bytes()+b'\n// drift\n')
        if label == 'missing-original': (target/owner).unlink()
        if label == 'occupied-new': write(target/absent,b'occupied\n')
        if label == 'mixed-state':
            row = next(r for r in rows if r[0] == absent)
            write(target/absent,(root/P/'replay-inputs'/row[3]).read_bytes())
        if label == 'leaf-symlink':
            (target/owner).unlink(); (target/owner).symlink_to(root/J/'pre'/Path(owner).with_suffix('.java.txt').name)
        if label == 'parent-symlink':
            parent = (target/owner).parent
            moved = parent.with_name('actual'); parent.rename(moved); parent.symlink_to(moved, target_is_directory=True)
        if label == 'directory-target': (target/owner).unlink(); (target/owner).mkdir()
        if label == 'check-pre': mode = '--check'
        if label == 'invalid-mode': mode = '--unknown'
        lock = None
        if label == 'ownership-lock':
            lockpath = target/'target/m3-short-flag-replay/ownership.lock'
            lockpath.parent.mkdir(parents=True)
            lock = lockpath.open('wb'); fcntl.lockf(lock, fcntl.LOCK_EX|fcntl.LOCK_NB)
        before = snapshot(target)
        try: run('refuse-'+label,adapter+[mode,root,target],expected=1)
        finally:
            if lock is not None: lock.close()
        assert snapshot(target) == before, label+' mutated source'
        refused.append(label)
    for label in ['manifest-drift','template-drift','template-symlink']:
        source = cases/(label+'-source'); source.mkdir()
        shutil.copytree(root/P/'replay-inputs',source/P/'replay-inputs')
        if label == 'manifest-drift':
            f = source/P/'replay-inputs/manifest.tsv'; f.write_bytes(f.read_bytes()+b'\n')
        else:
            f = source/P/'replay-inputs'/rows[0][3]
            if label == 'template-drift': f.write_bytes(f.read_bytes()+b'\n')
            else: f.unlink(); f.symlink_to(root/P/'replay-inputs'/rows[0][3])
        target = seed(cases/label); before = snapshot(target)
        run('refuse-'+label, adapter+['--apply',source,target],expected=1)
        assert snapshot(target) == before
        refused.append(label)

    # The only installation path: execute the qualified, existing Java recipe adapter.
    generated = seed(out/'generated')
    run('actual-replay-emission',adapter+['--apply',root,generated],signature='changes=6')
    for path, before, after, template in rows:
        assert sha((generated/path).read_bytes()) == after
    times = {path:(generated/path).stat().st_mtime_ns for path, *_ in rows}
    run('fixed-point-replay',adapter+['--apply',root,generated],signature='changes=0')
    run('fixed-point-check',adapter+['--check',root,generated],signature='changes=0')
    assert times == {path:(generated/path).stat().st_mtime_ns for path, *_ in rows}
    assert (generated/'untouched.txt').read_bytes() == b'preserve me\n'

    java = [generated/path for path, *_ in rows if path.endswith('.java') and not path.endswith('CompositionTest.java')]
    product = out/'classes'; product.mkdir()
    headers = out/'headers'; headers.mkdir()
    run('compile-generated-java', ['javac','--release','21','-proc:none','-Xlint:all','-Werror',
                                  '-h',headers,'-d',product]+java)
    cls = 'com.m3.collections.M3DeepFilterCompositionProof'
    java_results = {}
    for label, flags, depth in [('tiered',[],65536),('interpreter',['-Xint'],8192)]:
        text = run('generated-'+label,['java','-ea','-Xss256k','-Xmx256m','-Dm3.deep.depth='+str(depth)]+flags+['-cp',product,cls],
                   signature='M3_DEEP_FILTER_PASS checks=')
        found = re.search(r'M3_DEEP_FILTER_PASS checks=(\d+) depth=(\d+) seeds=(\d+)',text)
        assert found and int(found[2]) == depth and int(found[3]) == 192
        java_results[label] = {'checks':int(found[1]),'depth':depth,'seeds':192}
    baseline = out/'baseline'; baseline.mkdir()
    pre_sources = []
    for name in pins:
        p = baseline/name; p.write_bytes((root/J/'pre'/(name+'.txt')).read_bytes()); pre_sources.append(p)
    proof = generated/'m3/collections/wrappers/src/test/java/com/m3/collections/M3DeepFilterCompositionProof.java'
    run('compile-original-witness', ['javac','--release','21','-proc:none','-Xlint:all','-Werror',
                                   '-d',baseline]+pre_sources+[proof])
    for witness in ['collection-witness','map-witness']:
        run('original-'+witness,['java','-Xss256k','-Xmx256m','-cp',baseline,cls,witness],
            expected=1,signature='StackOverflowError')
    for name in pins:
        owner_name = 'com.m3.collections.'+name[:-5]
        old = run(name+'-old-api',['javap','-public','-s','-cp',baseline,owner_name])
        new = run(name+'-new-api',['javap','-public','-s','-cp',product,owner_name])
        assert old == new, name+' public API changed'
    jhome = Path(shutil.which('javac')).resolve().parent.parent
    library = out/'libm3_deep_filter_jni.so'
    shim = generated/P/'native/m3_deep_filter_jni.c'
    header = (headers/'com_m3_collections_M3DeepFilterNativeProof.h').read_text()
    assert 'Java_com_m3_collections_M3DeepFilterNativeProof_fold' in header
    run('compile-jni-ubsan',['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-pedantic','-fPIC','-shared',
                           '-fsanitize=undefined','-fno-sanitize-recover=undefined',
                           '-I'+str(jhome/'include'),'-I'+str(jhome/'include/linux'),root/NATIVE,shim,'-o',library])
    run('generated-jni',['java','-ea','-Xss256k','-Xmx256m','-Xcheck:jni',
                        '-Dm3.deep.native.library='+str(library),'-cp',product,
                        'com.m3.collections.M3DeepFilterNativeProof'],signature='M3_DEEP_FILTER_JNI_PASS checks=156 depth=65536')
    receipt={'schema':'M3_DEEP_FILTER_REPLAY_V1','targets':rows,'replay_sha256':sha(bound.encode()),
             'refusals':refused,'unchanged_public_owners':2,'jdk_source_baselines':pins,
             'native_original_git_blob':gitblob(native),'java_modes':java_results,
             'jni_checks':156,'maven_lifecycle':'NOT_EXECUTED_BY_THIS_SCRIPT',
             'openrewrite_sdk':'NOT_EXECUTED_BY_THIS_SCRIPT','commands':commands}
    (evidence/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print('M3_DEEP_FILTER_QUALIFIED targets=6 refusals='+str(len(refused))+' commands='+str(len(commands)))

if __name__ == '__main__':
    main()
