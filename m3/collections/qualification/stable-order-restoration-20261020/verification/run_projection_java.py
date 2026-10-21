#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Bounded actual-source Java qualification; no recipe application or product writes."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'baseline/m3/collections'
B = ROOT.parent / 'm3fs-vfs-20261020'
JDK = B / 'toolchain/jdk21/bin'
HOST = ROOT / 'custody/synexia-openrewrite-recipes/recipe-crates/m3-wrapper-convergence-20261018/recipe-host'
BEFORE = '77ef7d6197ef936f5ae9f62c35accf2f5f3ff690dcc55ca3fd4d8f545a007545'
AFTER = '7bf939ea374ce6c0b13c403f5e154c0f8d376f9cddb8e623cf00ef299183a77e'
OWNERS = ['M3LongIndexProjection', 'M3LongAddressKernel', 'M3IndexMaskCompactor', 'M3IndexMaskWindow']
TESTS = ['M3LongAddressKernelTest', 'M3LongIndexProjectionMaskOffsetTest',
         'M3IndexMaskWindowTest', 'M3IndexMaskWindowRecoveryTest', 'M3IndexOrderWindowTest']
CURRENT_PROOFS = ['M3LongIndexProjectionSelfTest', 'M3LongIndexProjectionMaskOffsetProof',
                  'M3LongIndexProjectionMaskOffsetTest', 'M3LongAddressKernelSelfTest',
                  'M3LongAddressKernelTest', 'M3IndexMaskWindowSelfTest', 'M3IndexMaskWindowTest',
                  'M3IndexMaskWindowRecoverySelfTest', 'M3IndexMaskWindowRecoveryTest']
PACKET_PROOFS = [('index-contract', 'M3IndexOrderWindowContract'),
                ('index-resource-probe', 'M3IndexOrderWindowResourceProbe'),
                ('index-junit', 'M3IndexOrderWindowTest')]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def class_hashes(directory):
    return {str(p.relative_to(directory)): digest(p) for p in sorted(directory.rglob('*.class'))}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--label', required=True, choices=['baseline', 'diagnostic', 'generated'])
    parser.add_argument('--projection', type=Path)
    args = parser.parse_args()
    output = ROOT / 'verification' / args.label
    output.mkdir(parents=True, exist_ok=True)
    receipt_path = output / 'RESULTS.json'
    if receipt_path.exists():
        raise SystemExit('Refusing to overwrite existing qualification receipt: ' + str(receipt_path))
    receiver = B / 'receiver'
    sdk_path = ROOT / 'verification/sdk/RESULTS.json'
    sdk = None
    if args.label == 'generated':
        sdk = json.loads(sdk_path.read_text())
        expected_admissions = {}
        for pass_id in ['stable-order-groups', *[p for p, _ in PACKET_PROOFS]]:
            manifest = HOST / ('passes/' + pass_id + '/resources/com/synexia/rewrite/hash-pinned-java/'
                               + 'm3-wrapper-' + pass_id + '/manifest.tsv')
            rows = [line.split('\t') for line in manifest.read_text().splitlines()
                    if line and not line.startswith('#')]
            if len(rows) != 1:
                raise SystemExit('Expected one sealed output for ' + pass_id)
            target, _, after, _ = rows[0]
            expected_admissions[target] = after
        if (sdk.get('status') != 'PASS' or sdk.get('passes') != 4 or sdk.get('sdk_tests') != 20
                or sdk.get('production_changes') != 1 or sdk.get('verification_files') != 3
                or sdk.get('production_overlays') is not False
                or sdk.get('admitted_files') != expected_admissions):
            raise SystemExit('All four exact canonical SDK outputs must be admitted before generated verification')
        admissions = sdk.get('admissions', [])
        if len(admissions) != 4 or {r['path'] for r in admissions} != set(expected_admissions):
            raise SystemExit('SDK admission rows do not match the exact four outputs')
        for row in admissions:
            if (row['sdk_tests'] != {'tests': 5, 'failures': 0, 'errors': 0, 'skipped': 0}
                    or row['receiving_compile'] != 'PASS'
                    or row['after_sha256'] != expected_admissions[row['path']]):
                raise SystemExit('Incomplete per-file SDK or receiving compile proof: ' + row['path'])
            if digest(receiver / row['path']) != row['after_sha256']:
                raise SystemExit('Receiver differs from admitted SDK output: ' + row['path'])
            for name, expected_hash in row['sdk_receipts'].items():
                evidence = sdk_path.parent / row['pass'] / name
                if digest(evidence) != expected_hash:
                    raise SystemExit('SDK receipt drift: ' + str(evidence))
        projection = receiver / 'm3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java'
        if args.projection is not None and args.projection.resolve() != projection.resolve():
            raise SystemExit('Generated mode accepts only the root-admitted receiving projection')
    else:
        projection = args.projection or BASE / 'src/main/java/com/m3/collections/M3LongIndexProjection.java'
    expected = BEFORE if args.label == 'baseline' else AFTER
    if digest(projection) != expected:
        raise SystemExit('Projection source does not match the approved ' + args.label + ' seal')
    if not (ROOT / 'FOSS_REUSE_DECISION.md').is_file():
        raise SystemExit('Source/reuse decision must precede qualification')
    if args.label != 'baseline':
        prior = json.loads((ROOT / 'verification/baseline/RESULTS.json').read_text())
        if prior['status'] != 'PASS':
            raise SystemExit('An executed passing baseline is required first')
    inventory = json.loads((ROOT / 'SOURCE_INVENTORY.json').read_text())
    for row in inventory['snapshot_records'] + inventory['exact_custody_records']:
        if digest(Path(row['local_path'])) != row['sha256']:
            raise SystemExit('Frozen input drift: ' + row['local_path'])
    main_classes = output / 'classes'
    proof_classes = output / 'proof-classes'
    proof_sources = output / 'proof-src/com/m3/collections'
    no_native = output / 'no-native'
    for directory in [main_classes, proof_classes, proof_sources, no_native]:
        directory.mkdir(parents=True, exist_ok=True)
    if args.label == 'generated':
        if class_hashes(main_classes) or class_hashes(proof_classes):
            raise SystemExit('Generated qualification requires fresh class directories')
        production = [receiver / ('m3/collections/src/main/java/com/m3/collections/' + n + '.java')
                      for n in OWNERS]
        for name, path in zip(OWNERS[1:], production[1:]):
            unchanged = BASE / ('src/main/java/com/m3/collections/' + name + '.java')
            if digest(path) != digest(unchanged):
                raise SystemExit('An unchanged production dependency differs from baseline: ' + str(path))
    else:
        production = [projection] + [BASE / ('src/main/java/com/m3/collections/' + n + '.java') for n in OWNERS[1:]]
    proofs = [BASE / ('src/test/java/com/m3/collections/' + n + '.java') for n in CURRENT_PROOFS]
    for pass_id, name in PACKET_PROOFS:
        if args.label == 'generated':
            proofs.append(receiver / ('m3/collections/src/test/java/com/m3/collections/' + name + '.java'))
        else:
            source = HOST / ('passes/' + pass_id + '/resources/com/synexia/rewrite/hash-pinned-java/m3-wrapper-'
                             + pass_id + '/' + name + '.java.txt')
            target = proof_sources / (name + '.java')
            target.write_bytes(source.read_bytes())
            proofs.append(target)
    proofs.append(ROOT / 'verification/ProjectionJUnitRunner.java')
    coordinates = [('org/junit/jupiter', 'junit-jupiter-api', '5.10.2'),
                   ('org/junit/jupiter', 'junit-jupiter-engine', '5.10.2'),
                   ('org/junit/platform', 'junit-platform-launcher', '1.10.2'),
                   ('org/junit/platform', 'junit-platform-engine', '1.10.2'),
                   ('org/junit/platform', 'junit-platform-commons', '1.10.2'),
                   ('org/opentest4j', 'opentest4j', '1.3.0'),
                   ('org/apiguardian', 'apiguardian-api', '1.1.2')]
    jars = [B / ('m2/' + group + '/' + name + '/' + version + '/' + name + '-' + version + '.jar')
            for group, name, version in coordinates]
    for jar in jars:
        if not jar.is_file():
            raise SystemExit('Missing actual JUnit dependency: ' + str(jar))
    jar_cp = os.pathsep.join(str(p) for p in jars)
    proof_cp = os.pathsep.join([str(main_classes), jar_cp])
    runtime_cp = os.pathsep.join([str(proof_classes), str(main_classes), jar_cp])
    receipt = {'format': 'm3-stable-order-java-qualification-1', 'label': args.label,
               'status': 'RUNNING', 'scope': 'Four actual production owners; five existing JUnit entrypoints, projection self-test, missing-native oracle, unchanged sort resource probe',
               'production': [{'path': str(p), 'sha256': digest(p)} for p in production],
               'proof_sources': [{'path': str(p), 'sha256': digest(p)} for p in proofs],
               'junit_jars': [{'path': str(p), 'sha256': digest(p)} for p in jars], 'commands': []}
    if sdk is not None:
        receipt['sdk_admission'] = {'path': str(sdk_path), 'sha256': digest(sdk_path),
                                    'admitted_files': sdk['admitted_files'],
                                    'all_receiver_output_hashes_verified': True,
                                    'unchanged_production_source_hashes_match_baseline': True,
                                    'verification_template_copies_used': False}

    def run(name, argv, timeout=120):
        argv = [str(a) for a in argv]
        started = time.monotonic()
        result = subprocess.run(argv, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        stdout = output / (name + '.stdout.log')
        stderr = output / (name + '.stderr.log')
        stdout.write_bytes(result.stdout)
        stderr.write_bytes(result.stderr)
        receipt['commands'].append({'name': name, 'argv': argv, 'exit': result.returncode,
                                    'elapsed_seconds': time.monotonic() - started,
                                    'stdout': str(stdout), 'stderr': str(stderr),
                                    'stdout_sha256': digest(stdout), 'stderr_sha256': digest(stderr)})
        receipt_path.write_text(json.dumps(receipt, indent=2) + '\n')
        print(name + ': exit=' + str(result.returncode), flush=True)
        if result.returncode:
            receipt['status'] = 'FAIL'
            receipt_path.write_text(json.dumps(receipt, indent=2) + '\n')
            print(result.stdout.decode(errors='replace') + result.stderr.decode(errors='replace'))
            raise SystemExit(result.returncode)
        return result.stdout.decode()

    run('java-version', [JDK / 'java', '-version'])
    run('production-javac', [JDK / 'javac', '--release', '21', '-Xlint:all', '-Werror', '-d', main_classes, *production])
    if sdk is not None:
        production_class_seals = class_hashes(main_classes)
        receipt['production_class_hashes_before_proof'] = production_class_seals
        for relative, expected_hash in production_class_seals.items():
            if Path(relative).name.startswith('M3LongIndexProjection'):
                continue
            if digest(ROOT / 'verification/baseline/classes' / relative) != expected_hash:
                raise SystemExit('An unchanged production class differs from baseline: ' + relative)
        receipt['unchanged_production_class_hashes_match_baseline'] = True
    run('proof-javac', [JDK / 'javac', '--release', '21', '-Xlint:all', '-Werror', '-cp', proof_cp, '-d', proof_classes, *proofs])
    api = run('public-protected-api', [JDK / 'javap', '-protected', '-s', '-classpath', main_classes,
              *['com.m3.collections.' + n for n in OWNERS],
              'com.m3.collections.M3LongIndexProjection$Order', 'com.m3.collections.M3IndexMaskCompactor$Backend'])
    if args.label != 'baseline':
        baseline_api = (ROOT / 'verification/baseline/public-protected-api.stdout.log').read_text()
        if api != baseline_api:
            receipt['status'] = 'FAIL_API_DRIFT'
            receipt_path.write_text(json.dumps(receipt, indent=2) + '\n')
            raise SystemExit('Public/protected API differs from actual compiled baseline')
        receipt['api_identical_to_baseline'] = True
    java = [JDK / 'java', '-XX:-UsePerfData', '-Xmx768m', '-Xcheck:jni',
            '-Djava.library.path=' + str(no_native), '-cp', runtime_cp]
    observed = run('junit', [*java, 'ProjectionJUnitRunner', *['com.m3.collections.' + n for n in TESTS]])
    run('projection-self-test', [*java, 'com.m3.collections.M3LongIndexProjectionSelfTest'])
    run('missing-native', [*java, 'com.m3.collections.M3IndexOrderWindowContract', '--missing'])
    probe = run('resource-sort', [JDK / 'java', '-XX:-UsePerfData', '-Xms128m', '-Xmx768m',
                '-Djava.library.path=' + str(no_native), '-cp', runtime_cp,
                'com.m3.collections.M3IndexOrderWindowResourceProbe', 'sort', '32768', '100'])
    match = re.search(r'INDEX_ORDER_WINDOW_PASS .* observations=([0-9a-f]{64})', observed)
    if not match:
        raise SystemExit('Missing deterministic oracle completion marker')
    receipt['finite_observation_sha256'] = match.group(1)
    if args.label != 'baseline' and match.group(1) != prior['finite_observation_sha256']:
        receipt['status'] = 'FAIL_OBSERVATION_DRIFT'
        receipt_path.write_text(json.dumps(receipt, indent=2) + '\n')
        raise SystemExit('Deterministic behavior differs from baseline')
    receipt['sort_samples'] = [dict(item.split('=', 1) for item in line.split()[1:])
                               for line in probe.splitlines() if line.startswith('INDEX_RESOURCE case=')]
    if sdk is not None:
        after_classes = class_hashes(main_classes)
        overlap = sorted(set(after_classes) & set(class_hashes(proof_classes)))
        source_stable = all(digest(Path(row['path'])) == row['sha256'] for row in receipt['production'])
        proof_stable = all(digest(Path(row['path'])) == row['sha256'] for row in receipt['proof_sources'])
        sdk_stable = digest(sdk_path) == receipt['sdk_admission']['sha256']
        if after_classes != production_class_seals or overlap or not source_stable or not proof_stable or not sdk_stable:
            receipt['status'] = 'FAIL_VERIFICATION_INPUT_OR_CLASS_DRIFT'
            receipt_path.write_text(json.dumps(receipt, indent=2) + '\n')
            raise SystemExit('Verification changed input source, SDK receipt, or production classes, or shadowed a production class')
        receipt['production_class_hashes_after_proof'] = after_classes
        receipt['proof_class_hashes'] = class_hashes(proof_classes)
        receipt['production_source_hashes_unchanged_during_verification'] = source_stable
        receipt['proof_source_hashes_unchanged_during_verification'] = proof_stable
        receipt['production_class_hashes_unchanged_during_verification'] = True
        receipt['test_production_class_overlap'] = overlap
    receipt['status'] = 'PASS'
    receipt['tests_successful'] = len(TESTS)
    receipt['production_classes'] = str(main_classes)
    receipt['proof_classes'] = str(proof_classes)
    receipt['resource_limits'] = ['Three samples from one unchanged finite workload; timing is observational',
                                  'RSS and requested-GC heap are whole-process observations, not retained-storage promises',
                                  'No recipe admission, product-wide compatibility or receiving-phase advancement is claimed']
    receipt_path.write_text(json.dumps(receipt, indent=2) + '\n')
    print('PASS; receipt: ' + str(receipt_path), flush=True)


if __name__ == '__main__':
    main()
