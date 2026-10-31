#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Qualify emitted actual owners against a separately frozen, source-sealed baseline."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import subprocess
import sys


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for option in ('repo-root', 'baseline-root', 'jdk-home', 'out'):
        parser.add_argument('--' + option, type=Path, required=True)
    args = parser.parse_args()
    repo, baseline, home = (getattr(args, name).resolve(strict=True)
                            for name in ('repo_root', 'baseline_root', 'jdk_home'))
    out = args.out.resolve()
    if out.exists():
        raise FileExistsError('fresh qualification output required: ' + str(out))
    if baseline == repo:
        raise ValueError('baseline must be an independent frozen input')
    out.mkdir(parents=True)
    packet = repo / 'm3/collections/qualification/masked-sort-fusion-20261021'
    identity = json.loads((packet / 'SOURCE_IDENTITY.json').read_text())
    commands = []

    def run(label, argv, expected_exit=0, marker=None):
        argv = list(map(str, argv))
        completed = subprocess.run(argv, cwd=repo, stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT, text=True, timeout=600)
        log = out / (label + '.log')
        log.write_text('$ ' + json.dumps(argv) + '\n' + completed.stdout
                       + '\nEXIT ' + str(completed.returncode) + '\n')
        commands.append({'label': label, 'argv': argv, 'exit': completed.returncode,
                         'log': log.name, 'sha256': sha(log)})
        (out / 'COMMANDS.json').write_text(json.dumps(commands, indent=2) + '\n')
        if completed.returncode != expected_exit:
            raise RuntimeError('unexpected command exit: ' + label)
        if marker is not None and marker not in completed.stdout:
            raise RuntimeError('missing command evidence marker: ' + label)
        return completed.stdout

    for row in identity['files']:
        path = row['path']
        before, after = baseline / path, repo / path
        if row['before_sha256'] == 'ABSENT':
            if before.exists(): raise ValueError('unexpected baseline path: ' + path)
        elif sha(before) != row['before_sha256']:
            raise ValueError('baseline source drift: ' + path)
        if sha(after) != row['after_sha256']:
            raise ValueError('emitted source drift: ' + path)
    for prefix in ('m3/collections/src/main/java', 'm3/collections/src/main/native/collections',
                   'm3/collections/src/test/java'):
        suffix = '*.c' if '/native/' in prefix else '*.java'
        expected = {row['path'] for row in identity['files'] if row['path'].startswith(prefix + '/')}
        actual = {str(path.relative_to(repo)) for path in (repo / prefix).rglob(suffix)}
        if expected != actual:
            raise ValueError('exact source closure changed: ' + prefix)

    java, javac = home / 'bin/java', home / 'bin/javac'
    source = baseline / 'm3/collections/src/main/java'
    base_classes, base_headers = out / 'baseline-classes', out / 'baseline-headers'
    base_headers.mkdir()
    inputs = sorted(source.rglob('*.java'))
    argfile = out / 'baseline-main.args'
    argfile.write_text('\n'.join(map(str, inputs)) + '\n')
    run('baseline-main-compile', [javac, '--release', '21', '-Xlint:all,-module', '-Werror',
        '-proc:none', '-h', base_headers, '-d', base_classes, '@' + str(argfile)])
    run('abi-refusal-tests', [sys.executable, '-m', 'unittest', 'discover', '-s',
        repo / 'm3/tooling/module-packs/collections/tests', '-p', 'test_*.py', '-v'], marker='OK')
    pack_out = out / 'installed-pack'
    run('actual-owner-pack', [sys.executable, repo / 'm3/tooling/module-packs/collections/build.py',
        '--repo-root', repo, '--jdk-home', home, '--out', pack_out], marker='M3_COLLECTIONS_PACK_PASS')
    pack_proof = json.loads((pack_out / 'M3_COLLECTIONS_PACK_PROOF.json').read_text())
    if set(pack_proof['native_libraries']) != {'m3_collections_mask', 'm3_collections_pathmask'}:
        raise ValueError('unexpected packaged library closure')
    if [len(pack_proof['native_libraries'][name]['symbols']) for name in
        ('m3_collections_mask', 'm3_collections_pathmask')] != [11, 1]:
        raise ValueError('existing ABI count differs; named equality is also enforced by builder')
    headers = {}
    for name in ('M3IndexMaskCompactor', 'M3IndexMaskWindow', 'M3IndexPathMasks'):
        header = 'com_m3_collections_' + name + '.h'
        before, after = base_headers / header, pack_out / 'headers' / header
        if before.read_bytes() != after.read_bytes():
            raise ValueError('Java native declaration/header changed: ' + header)
        headers[name] = sha(after)

    sources = [repo / 'm3/collections/src/test/java/com/m3/collections' / (name + '.java')
               for name in ('M3FilteredEndpointRestorationContract', 'M3MaskedSortFusionContract')]
    sources += sorted((packet / 'src/test/java').rglob('*.java'))
    tests = out / 'probe-classes'
    run('oracle-compile-against-baseline', [javac, '--release', '21', '-Xlint:all,-module', '-Werror',
        '-cp', base_classes, '-d', tests, *sources])
    target_classes = pack_out / 'modules/com.m3.collections'
    installed_native = pack_out / 'pack/image/lib'
    missing = out / 'missing-native'
    missing.mkdir()
    surfaces = {}
    allocation = {}
    for label, classes in (('baseline', base_classes), ('target', target_classes)):
        cp = str(classes) + os.pathsep + str(tests)
        surfaces[label] = run(label + '-public-surface', [java, '-cp', cp,
            'com.m3.collections.M3CollectionSurfaceProbe'])
        for mode in ('private', 'facade'):
            run(label + '-strict-endpoint-' + mode, [java, '-cp', cp,
                'com.m3.collections.M3FilteredEndpointRestorationContract', mode],
                expected_exit=1 if label == 'baseline' else 0,
                marker='NoSuchElementException' if label == 'baseline' else 'PASS')
        for mode, arguments in (('java', []), ('native', ['--native'])):
            run(label + '-masked-sort-' + mode, [java, '-Xcheck:jni',
                '-Djava.library.path=' + str(installed_native), '-cp', cp,
                'com.m3.collections.M3MaskedSortFusionContract', *arguments], marker='PASS')
            output = run(label + '-allocation-' + mode, [java, '-Xint', '-Xcheck:jni',
                '-Djava.library.path=' + str(installed_native), '-cp', cp,
                'com.m3.collections.M3MaskedSortAllocationProbe', *arguments])
            rows = [json.loads(line) for line in output.splitlines() if line.startswith('{')]
            if [row['selected'] for row in rows] != [0, 1, 64, 1024]:
                raise ValueError('allocation workload inventory differs')
            allocation[label + '-' + mode] = rows
        run(label + '-missing-native', [java, '-Xcheck:jni', '-Djava.library.path=' + str(missing),
            '-cp', cp, 'com.m3.collections.M3MaskedSortFusionContract', '--missing'], marker='PASS')
    calls = {label: [line for line in text.splitlines() if line.startswith('CALL\t')]
             for label, text in surfaces.items()}
    if calls['baseline'] != calls['target']:
        raise ValueError('effective callable descriptors changed')
    expected_reflection = {
        'baseline': ['ENDPOINT\t' + name + '\tdeclaring=java.util.NavigableSet\tdefault=true'
                     for name in ('removeFirst', 'removeLast')],
        'target': ['ENDPOINT\t' + name + '\tdeclaring=com.m3.collections.M3FilteredNavigableSet\tdefault=false'
                   for name in ('removeFirst', 'removeLast')]}
    for label, expected in expected_reflection.items():
        if [line for line in surfaces[label].splitlines() if line.startswith('ENDPOINT\t')] != expected:
            raise ValueError('unreviewed reflection observation: ' + label)
    deltas = []
    for mode in ('java', 'native'):
        for before, after in zip(allocation['baseline-' + mode], allocation['target-' + mode], strict=True):
            if (before['rows'], before['selected'], before['samples']) != (after['rows'], after['selected'], after['samples']):
                raise ValueError('allocation workloads are not comparable')
            reduction = before['allocated_bytes'] - after['allocated_bytes']
            if reduction <= 0: raise ValueError('allocation reduction not observed')
            deltas.append({'mode': mode, 'rows': before['rows'], 'selected': before['selected'],
                'samples': before['samples'], 'total_allocated_byte_reduction': reduction,
                'bytes_per_operation_fraction': str(reduction) + '/' + str(before['samples'])})
    # Recheck every sealed input after all compiler/runtime activity.
    for row in identity['files']:
        if sha(repo / row['path']) != row['after_sha256']:
            raise ValueError('source changed during qualification: ' + row['path'])
    proof = {'status': 'PASS', 'baseline_commit': identity['baseline_commit'],
        'main_java_sources': len(inputs), 'source_identity_sha256': sha(packet / 'SOURCE_IDENTITY.json'),
        'jdk_release_sha256': sha(home / 'release'), 'platform': platform.platform(),
        'effective_public_callable_descriptors': len(calls['target']),
        'declared_endpoint_reflection_change': expected_reflection,
        'unchanged_native_headers': headers, 'installed_pack_receipt': 'installed-pack/M3_COLLECTIONS_PACK_PROOF.json',
        'installed_pack_receipt_sha256': sha(pack_out / 'M3_COLLECTIONS_PACK_PROOF.json'),
        'allocation_observations': allocation, 'allocation_deltas': deltas,
        'commands': commands,
        'limits': ['Allocation observations use -Xint and 80 samples; elapsed time is not a speedup claim.',
                   'Owning Maven and full all-library UBSan/JNI suite are separate required receipts.',
                   'No full custom JDK/HotSpot, all-platform, provider, JIT, or runtime family promotion.']}
    (out / 'PROOF.json').write_text(json.dumps(proof, indent=2) + '\n')
    print('M3_MASKED_SORT_FUSION_QUALIFICATION_PASS')


if __name__ == '__main__':
    main()
