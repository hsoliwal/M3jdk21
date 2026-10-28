#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Apply qualified D and freshly test its selected-source M3JDK21 receiver.

Reuses the qualified copier, real Java owners, existing JUnit launcher and
sealed preservation oracles. The canonical D packet remains unchanged. Internal
stack-frame inspection and allocation/OOME timing are outside equivalence.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys
import time

BASE = '6d9bad615851f346e3062e3034ccd74cbe2776af'
PROOF = '688e24e3c80dfa2ea64bc6cfbb952291b3eeb5898a3cd8a6e20d2565513cecfe'
COPIER = 'ffbe6545ed75a96a534814ab6c6705a79be555aec28bf97f9db5dbf038c88720'
QUALIFICATION = 'f97d250bc8a2eda0e46808935ce56ee200344a175ac44d0e1532e7c88b9a0112'
PRIOR_INPUTS = '2f0ebb7adf0f22979fedff62190cb28f412da5ed87837a2c9c142b1191554cb1'
LAUNCHER = '5a13137e385cb6a04ed2c7f130b5ff6436268d2973a93ae1349d091af2efbe5c'
OWNER = 'm3/collections/src/main/java/com/m3/collections/M3FilteredViews.java'
TARGETS = {
    OWNER: '4209587c4cc43537e1ce0d7b316d1824a4de0f25154fef1a87cbe7e9ae9871ea',
    'm3/collections/src/test/java/com/m3/collections/M3FirstVisibleAtomContract.java':
        'cea097e4b1cf67a3cd2d8fdd4df5f29751db7d767daf785732c350003acede29',
    'm3/collections/src/test/java/com/m3/collections/M3FirstVisibleAtomTest.java':
        'c16fa40bc6768618df372d8d43936f5761d3375c35dfeeb9da15b84dc4fcb44d',
}


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def blob(data):
    return hashlib.sha1(f'blob {len(data)}\0'.encode() + data).hexdigest()


def snapshot(root):
    result = {}
    for path in sorted(root.rglob('*')):
        require(not path.is_symlink(), 'symbolic product entry refused')
        info = path.stat()
        item = {'kind': 'file' if path.is_file() else 'directory',
                'mode': stat.S_IMODE(info.st_mode), 'mtime_ns': info.st_mtime_ns,
                'inode': info.st_ino}
        if path.is_file():
            raw = path.read_bytes()
            item.update(bytes=len(raw), sha256=sha(raw), git_blob=blob(raw))
        result[path.relative_to(root).as_posix()] = item
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repository', type=Path, required=True)
    parser.add_argument('--crate', type=Path, required=True)
    parser.add_argument('--preimage-custody', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--maven-cache', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    repo = args.repository.resolve(strict=True)
    crate = args.crate.resolve(strict=True)
    jdk = args.java_home.resolve(strict=True)
    cache = args.maven_cache.resolve(strict=True)
    out = args.output.resolve()
    require(not out.is_relative_to(crate) and not out.is_relative_to(repo),
            'fresh verification output must be outside product and canonical crate')
    out.mkdir(parents=True, exist_ok=False)
    build = out / 'build'
    build.mkdir()
    observed = {}

    def retain(path, expected=None):
        raw = path.read_bytes()
        if expected is not None:
            require(sha(raw) == expected, 'input custody drift: ' + str(path))
        observed[path] = raw
        return raw

    def write_json(name, value):
        (out / name).write_text(json.dumps(value, indent=2) + '\n')

    preimage_raw = retain(args.preimage_custody.resolve(strict=True))
    preimage = json.loads(preimage_raw)
    require(preimage['base_revision'] == BASE, 'expected native PR #1642 stacking head')
    before = snapshot(repo)
    for row in preimage['files']:
        item = before[row['path']]
        require(item['bytes'] == row['bytes'] and item['sha256'] == row['sha256']
                and item['git_blob'] == row['git_blob'], 'selected-source preimage drift')
    require({path for path, item in before.items() if item['kind'] == 'file'}
            == {row['path'] for row in preimage['files']}, 'selected source closure differs')
    require(all(path not in before for path in preimage['absent_targets']), 'new focused tests must be absent')
    excluded = [row['path'] for row in preimage['files']
                if row['role'] == 'excluded_existing_native_lane_source']
    require(len(excluded) == 5, 'expected five retained native-lane source owners')
    write_json('product-before.json', before)

    frozen = crate / 'qualification/collections'
    manifest_raw = retain(frozen / 'FILES.json', QUALIFICATION)
    manifest = json.loads(manifest_raw)
    for item in manifest['files']:
        path = frozen / item['path']
        require(path.resolve().is_relative_to(frozen) and not path.is_symlink(),
                'qualification fixture escaped')
        raw = retain(path, item['sha256'])
        require(len(raw) == item['bytes'], 'qualification fixture length changed')
    require(len(manifest['files']) == 76, 'frozen qualification closure changed')
    historical = json.loads(observed[frozen / 'evidence/results.json'])
    require(historical['status'] == 'PASS_FIRST_VISIBLE_CALLBACK_AND_RESULT_PRESERVATION'
            and historical['command_count'] == 44 and historical['exact_output_comparisons'] == 38,
            'historical qualification claim changed')
    historical_commands = json.loads(observed[frozen / 'evidence/commands.json'])
    for command in historical_commands:
        require(command['actual_exit'] == command['expected_exit'], 'historical command outcome changed')
        require(sha(observed[frozen / 'evidence' / command['log']]) == command['sha256'],
                'historical command output changed')
    historical_comparisons = json.loads(observed[frozen / 'evidence/comparisons.json'])
    require(len(historical_comparisons) == 38 and all(row['equal'] for row in historical_comparisons),
            'historical comparison changed')
    prior_inputs = json.loads(retain(crate / 'verification/collections/evidence/inputs.json', PRIOR_INPUTS))
    require(prior_inputs['recipe_proof_sha256'] == PROOF and prior_inputs['emitted_files'] == TARGETS,
            'prior emitted identity changed')
    launcher = crate / 'verification/collections/ReceivingJUnitRunner.java'
    retain(launcher, LAUNCHER)
    jars = []
    for row in prior_inputs['dependencies']:
        path = cache / row['path']
        retain(path, row['sha256'])
        jars.append(path)
    require(len(jars) == 7, 'unexpected JUnit dependency closure')

    proof_raw = retain(crate / 'target/recipe-proof.tsv', PROOF)
    proof = dict(csv.reader(proof_raw.decode().splitlines()[1:], delimiter='\t'))
    status_raw = retain(crate / 'target/proof-status.tsv')
    require(list(csv.DictReader(status_raw.decode().splitlines(), delimiter='\t'))
            == [{'status': 'PASS', 'proof_sha256': PROOF}], 'incomplete source recipe proof')
    require(proof['scope'] == 'source-replay-only' and proof['targets'] == '3'
            and proof['phases'] == '3' and proof['atom_phases'] == '2', 'unexpected source composition')
    targets = json.loads(retain(crate / 'TARGETS.json'))
    require({row['path']: row['after_sha256'] for row in targets['targets']} == TARGETS,
            'qualified target allowlist differs')

    source_module = crate.parent.parent
    runner_module = build / 'qualified-copier/synexia-openrewrite-recipes'
    runner_crate = runner_module / 'recipe-crates' / crate.name

    def copy_qualified(path):
        raw = retain(path)
        dest = runner_module / path.relative_to(source_module)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(raw)

    for name in ['apply_receiver.py', 'phases.tsv', 'atom-chains.tsv', 'pom.xml']:
        copy_qualified(crate / name)
    require(sha(observed[crate / 'apply_receiver.py']) == COPIER, 'qualified copier changed')
    copy_qualified(crate / 'src/test/java/com/synexia/rewrite/M3FilteredFirstVisibleRuntimeRecipeTest.java')
    for name in ['M3EditScope.java', 'M3ScopeInference.java', 'M3HashPinnedJavaSnapshotRecipe.java',
                 'M3HashPinnedTextSnapshotRecipe.java']:
        owner = source_module / 'src/main/java/com/synexia/rewrite' / name
        retain(owner, proof[name])
        copy_qualified(owner)
    for name in ['recipe-proof.tsv', 'proof-status.tsv', 'named-recipe.yml']:
        copy_qualified(crate / 'target' / name)
    for directory in ['preimages', 'target/phase-receipts', 'target/receiver']:
        for path in sorted((crate / directory).rglob('*')):
            if path.is_file():
                copy_qualified(path)
    resources = source_module / 'src/main/resources/com/synexia/rewrite'
    phases = list(csv.DictReader(observed[crate / 'phases.tsv'].decode().splitlines(), delimiter='\t'))
    atoms = list(csv.DictReader(observed[crate / 'atom-chains.tsv'].decode().splitlines(), delimiter='\t'))
    for kind, phase_id in [(row['kind'], row['id']) for row in phases] + [
            (row['kind'], row['atom_phase']) for row in atoms]:
        for path in sorted((resources / ('hash-pinned-' + kind) / phase_id).rglob('*')):
            if path.is_file():
                copy_qualified(path)

    env = dict(os.environ)
    removed = [key for key in ['JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS',
                               'CLASSPATH', 'LD_PRELOAD', 'LD_LIBRARY_PATH'] if key in env]
    for key in removed:
        env.pop(key)
    commands = []
    comparisons = []

    def run(label, argv, expected=0, reference=None):
        argv = [str(value) for value in argv]
        started = time.monotonic()
        result = subprocess.run(argv, cwd=build, env=env, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, timeout=90, check=False)
        raw = result.stdout
        (out / (label + '.log')).write_bytes(raw)
        commands.append({'label': label, 'argv': argv, 'cwd': str(build),
                         'expected_exit': expected, 'actual_exit': result.returncode,
                         'elapsed_seconds': time.monotonic() - started,
                         'removed_ambient_environment_keys': removed,
                         'output_sha256': sha(raw)})
        write_json('commands.json', commands)
        require(result.returncode == expected, label + ': unexpected exit/output: '
                + raw.decode(errors='replace')[-1600:])
        if reference is not None:
            reference_raw = retain(reference)
            equal = raw == reference_raw
            comparisons.append({'label': label, 'reference': str(reference), 'equal': equal,
                                'actual_sha256': sha(raw), 'reference_sha256': sha(reference_raw)})
            write_json('comparisons.json', comparisons)
            require(equal, label + ': exact preserved output differs')
        print(label + ': expected exit ' + str(expected) + ' matched', flush=True)
        return raw

    receipts = {}
    for label, apply in [('copier-check', False), ('copier-apply', True), ('copier-replay', True)]:
        run(label, [sys.executable, runner_crate / 'apply_receiver.py', repo] + (['--apply'] if apply else []))
        receipt_path = runner_crate / 'target' / ('application-receipt.json' if apply else 'application-check.json')
        raw = receipt_path.read_bytes()
        (out / (label + '.receipt.json')).write_bytes(raw)
        receipt = json.loads(raw)
        require(receipt['status'] == 'PASS' and receipt['recipe_proof_sha256'] == PROOF,
                'copier did not consume qualified D proof')
        require({row['path'] for row in receipt['files']} == set(TARGETS), 'copier scope changed')
        receipts[label] = sha(raw)
        current = snapshot(repo)
        if label == 'copier-check':
            require(current == before, 'check modified receiving repository')
        elif label == 'copier-apply':
            after = current
            for row in preimage['files']:
                if row['path'] != OWNER:
                    require(current[row['path']] == before[row['path']], 'excluded source was modified')
            require({path for path, item in current.items() if item['kind'] == 'file'}
                    == {row['path'] for row in preimage['files']} | set(TARGETS), 'unlisted product file')
        else:
            require(current == after, 'replay changed product snapshot')
            require(all(row['action'] == 'ALREADY_APPLIED' for row in receipt['files']), 'replay not no-op')
    write_json('product-after-apply.json', after)
    print('D application complete; all five native-lane source files preserved.', flush=True)

    for path, expected in TARGETS.items():
        retain(repo / path, expected)
    for row in preimage['files']:
        if row['path'] != OWNER:
            retain(repo / row['path'], row['sha256'])
    classes = build / 'classes'
    classes.mkdir()
    classpath = os.pathsep.join(str(path) for path in [classes, *jars])
    toolchain = {}
    for name in ['java', 'javac', 'javap']:
        path = jdk / 'bin' / name
        raw = path.read_bytes()
        toolchain[str(path)] = {'bytes': len(raw), 'sha256': sha(raw)}
    write_json('toolchain-custody.json', toolchain)
    java_version = run('java-version', [jdk / 'bin/java', '-XX:-UsePerfData', '-version'])
    javac_version = run('javac-version', [jdk / 'bin/javac', '-J-XX:-UsePerfData', '-version'])
    require(re.search(rb'version "21(?:\.|"|\+)', java_version) is not None, 'actual Java21 runtime required')
    require(re.search(rb'javac 21(?:\.|\s|\+)', javac_version) is not None, 'actual Java21 compiler required')
    real_source_paths = [row['path'] for row in preimage['files'] if row['path'] not in excluded]
    sources = [repo / path for path in real_source_paths]
    sources.extend(repo / path for path in TARGETS if path != OWNER)
    external_probe = frozen / 'tests/M3FilteredRangeFailureContract.java'
    sources.extend([external_probe, launcher])
    compiled = run('target-compile', [jdk / 'bin/javac', '-J-XX:-UsePerfData', '--release', '21',
        '-encoding', 'UTF-8', '-Xlint:all', '-Werror', '-proc:none', '-implicit:none',
        '-classpath', classpath, '-sourcepath', '', '-d', classes, *sources])
    require(not compiled, 'strict Java compilation emitted diagnostics')
    launch = [jdk / 'bin/java', '-XX:-UsePerfData', '-cp', classpath]
    first_visible = frozen / 'evidence/logs/baseline-first-visible.log'
    run('target-direct-probe', [*launch, 'com.m3.collections.M3FirstVisibleAtomContract'], reference=first_visible)
    junit = run('target-junit', [*launch, 'ReceivingJUnitRunner'])
    require(junit == observed[first_visible]
            + b'PASS receiving-junit found=1 succeeded=1 skipped=0 aborted=0 failed=0\n',
            'focused JUnit did not execute exactly one successful receiving test')
    comparisons.append({'label': 'target-junit-callback-tape', 'reference': str(first_visible),
                        'equal': True, 'actual_sha256': sha(junit.splitlines(keepends=True)[0]),
                        'reference_sha256': sha(observed[first_visible])})
    write_json('comparisons.json', comparisons)
    run('target-navigation-tape', [*launch, 'com.m3.collections.M3FilteredNavigationReconciliationContract', 'tape'],
        reference=frozen / 'receipts/baseline-navigation-tape.log')
    for mode in ['controls', 'late', 'real']:
        run('target-range-' + mode, [*launch, 'com.m3.collections.M3FilteredRangeFailureContract', mode],
            expected=0 if mode == 'controls' else 1,
            reference=frozen / ('receipts/baseline-range-' + mode + '.log'))
    run('target-open-comparator-origin', [*launch, 'com.m3.collections.M3WrapperConvergenceContract', 'comparator-failure'],
        expected=1, reference=frozen / 'receipts/baseline-open-comparator-origin.log')
    run('target-api', [jdk / 'bin/javap', '-J-XX:-UsePerfData', '-protected', '-classpath', classes,
                      'com.m3.collections.M3FilteredViews'], reference=frozen / 'receipts/baseline-api.log')
    for path, raw in observed.items():
        require(path.read_bytes() == raw, 'qualification input changed during target execution: ' + str(path))
    final = snapshot(repo)
    for path, item in after.items():
        if item['kind'] == 'file':
            require(final[path] == item, 'product source changed during target execution')
    write_json('input-custody.json', {
        'scope': 'selected-source M3JDK21 receiving subset stacked on native PR #1642',
        'repository': str(repo), 'base_revision': BASE, 'source_ancestor_revision': preimage['source_ancestor_revision'],
        'recipe_proof_sha256': PROOF, 'receiver_root': proof['receiver_root'],
        'driver_sha256': sha(Path(__file__).read_bytes()), 'copier_sha256': COPIER,
        'preimage_custody_sha256': sha(preimage_raw), 'qualification_manifest_sha256': QUALIFICATION,
        'external_fixture': str(external_probe), 'launcher': str(launcher),
        'compiled_product_source_paths': real_source_paths + [path for path in TARGETS if path != OWNER],
        'excluded_native_source_paths': excluded,
        'inputs': [{'path': str(path), 'bytes': len(raw), 'sha256': sha(raw)}
                   for path, raw in sorted(observed.items())],
    })
    write_json('generated-build-artifacts.json', {
        str(path.relative_to(out)): {'bytes': path.stat().st_size, 'sha256': sha(path.read_bytes())}
        for path in sorted(classes.rglob('*.class'))})
    result = {
        'status': 'PASS', 'scope': 'fresh selected-source M3JDK21 wrapper application and target Java execution',
        'base_pull_request': 1642, 'base_revision': BASE, 'whole_repository_checkout': False,
        'commands': len(commands), 'copier_commands': 3, 'fresh_exact_output_comparisons': len(comparisons),
        'recipe_execution': 'CONSUMED_EXISTING_QUALIFIED_D_OUTPUT', 'recipe_proof_sha256': PROOF,
        'receiver_root': proof['receiver_root'], 'target_files': [{'path': path, 'sha256': pin} for path, pin in TARGETS.items()],
        'application_receipt_sha256': receipts, 'check_product_snapshot_unchanged': True,
        'replay_product_snapshot_unchanged': True, 'unchanged_native_source_files': len(excluded),
        'all_excluded_native_bytes_modes_mtimes_inodes_preserved': True,
        'focused_junit': {'found': 1, 'succeeded': 1, 'skipped': 0, 'aborted': 0, 'failed': 0},
        'focused_probe_invocations': 2, 'normal_cases_per_probe_invocation': 64,
        'caller_failure_cases_per_probe_invocation': 1104,
        'focused_tape_digest': '53a3ac80b4ba43ad3df0c2397041513fb02346c2ba2cb2ddebc12b7540329b99',
        'public_protected_api_unchanged': True, 'stable_read_tape_unchanged': True,
        'exception_controls': {'cases': 120, 'checks': 240, 'failures': 0},
        'preserved_known_defects': {'late_range_failures': 100, 'real_comparator_failures': 3,
                                    'initial_comparator_origin': 'exact expected failure preserved'},
        'historical_three_stage_commands_reused': 44, 'historical_comparisons_reused': 38,
        'historical_qualification_files_verified': 76, 'broad_three_stage_matrix_rerun': False,
        'semantic_scope_exclusions': ['internal stack-frame inspection including StackWalker',
                                      'exhaustive allocation-failure/OOME equivalence', 'allocation timing'],
        'inputs_unchanged': True, 'native_execution_this_run': 'NONE_EXCLUDED_SOURCES_ONLY',
        'full_jdk_build': 'NOT_RUN', 'whole_module': 'NOT_RUN', 'jtreg': 'NOT_RUN',
        'performance_claim': 'NONE', 'target_metadata': 'PARENT_OWNED_NOT_EDITED', 'target_promotion': 'NOT_PERFORMED',
    }
    write_json('results.json', result)
    print(json.dumps(result, indent=2), flush=True)


if __name__ == '__main__':
    main()
