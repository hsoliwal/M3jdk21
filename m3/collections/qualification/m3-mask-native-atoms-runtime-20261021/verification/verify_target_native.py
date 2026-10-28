#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Apply qualified C and freshly validate its actual selected-source M3 receiver.

This bounded GNU/Linux gate reuses existing qualification commands and oracles.
It creates no production transformation engine or new behavioral model. The
qualified crate stays unchanged: its copier runs from an exact isolated copy.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
import time

NATIVE = 'm3/collections/src/main/native/collections/m3_index_mask_compactor.c'
OWNER = 'm3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java'
TEST_ROOT = 'm3/collections/src/test/java/com/m3/collections/'
TESTS = ['M3MaskPositionAtomsTest', 'M3IndexMaskRangeNativeTest',
         'M3RequiredExcludedMaskSelectorNativeContract']
RECIPE_PROOF = '8b96ff9f844e6c238d2105c55af7b4c37f4628737e8232ca8115a5942dd14bd0'
COPIER = '185b03d4ab0961acf658694bfb654cddfc361a04dad249a0ced6b49ba70687c8'
CONTRACT = 'b51bff459e073cefcac9639a259e5da11faea48b6437bbe807d1706757fee75f'
PRIOR_RESULTS = 'b60da790dc5206f097e42df257445b514a627ca379a46a890667de843212f329'
EXPECTED_STDOUT = {
    TESTS[0]: '00d6715c4893348cb2bc486633632cecea069826d07101e491a68b7b9c7e82c2',
    TESTS[1]: 'f5bcf7f607e507686a3f7981703e1aa5fc0b7b4fa5c482099631b5614917ec87',
    TESTS[2]: 'c1a67ac51dee0798af3359903f125ca0bdf6b71f5e1f73ef666ff06dbb0147e0',
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
            data = path.read_bytes()
            item.update({'bytes': len(data), 'sha256': sha(data), 'git_blob': blob(data)})
        result[path.relative_to(root).as_posix()] = item
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repository', type=Path, required=True)
    parser.add_argument('--crate', type=Path, required=True)
    parser.add_argument('--source-crate', type=Path, required=True)
    parser.add_argument('--preimage-custody', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--cc', default='gcc')
    parser.add_argument('--nm', default='nm')
    args = parser.parse_args()
    require(sys.platform.startswith('linux'), 'bounded GNU/Linux qualification only')
    repo = args.repository.resolve(strict=True)
    crate = args.crate.resolve(strict=True)
    source = args.source_crate.resolve(strict=True)
    jdk = args.java_home.resolve(strict=True)
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    build = output / 'build'
    build.mkdir()
    observed = {}

    def retain(path, expected=None):
        raw = path.read_bytes()
        if expected is not None:
            require(sha(raw) == expected, 'input custody drift: ' + str(path))
        observed[path] = raw
        return raw

    def write_json(name, data):
        (output / name).write_text(json.dumps(data, indent=2) + '\n')

    preimage_raw = retain(args.preimage_custody.resolve(strict=True))
    preimage = json.loads(preimage_raw)
    require(preimage['base_revision'] == '7c0d2493edbf93ac5cb726667ce83046d3519e3e',
            'unexpected runtime base')
    before = snapshot(repo)
    for row in preimage['files']:
        item = before[row['path']]
        require(item['sha256'] == row['sha256'] and item['bytes'] == row['bytes']
                and item['git_blob'] == row['git_blob'], 'selected runtime preimage drift')
    require(preimage['absent_target'] not in before, 'new regression must be absent before application')
    require({p for p, entry in before.items() if entry['kind'] == 'file'}
            == {row['path'] for row in preimage['files']}, 'selected-source subset is not exact')
    write_json('product-before.json', before)

    contract_raw = retain(source / 'verification/native/fixtures/baseline-contract.json', CONTRACT)
    contract = json.loads(contract_raw)
    prior_raw = retain(source / 'verification/native/evidence/results.json', PRIOR_RESULTS)
    prior = json.loads(prior_raw)
    require(prior['status'] == 'PASS' and prior['schedule_ubsan'] == 'PASS', 'prior schedule receipt failed')
    tape_path = source / 'qualification/native/evidence/baseline-schedule.stdout.log'
    tape = retain(tape_path, contract['schedule_sha256'])
    require(len(tape) == contract['schedule_bytes'] and len(tape.splitlines()) == 899,
            'prior schedule tape dimensions differ')
    expected_stdout = {}
    for test in TESTS:
        expected_stdout[test] = retain(
            source / ('qualification/native/evidence/baseline-' + test + '.stdout.log'),
            EXPECTED_STDOUT[test])
    require(prior['schedule_sha256'] == contract['schedule_sha256'], 'prior schedule hash differs')
    require({row['path']: row['sha256'] for row in prior['emitted_files']}
            == contract['emitted_sha256'], 'prior schedule does not bind exact receiving postimages')

    proof_raw = retain(crate / 'target/recipe-proof.tsv', RECIPE_PROOF)
    proof = dict(csv.reader(proof_raw.decode().splitlines()[1:], delimiter='\t'))
    status_raw = retain(crate / 'target/proof-status.tsv')
    require(list(csv.DictReader(status_raw.decode().splitlines(), delimiter='\t'))
            == [{'status': 'PASS', 'proof_sha256': RECIPE_PROOF}], 'incomplete source recipe proof')
    require(proof['targets'] == '2' and proof['phases'] == '2' and proof['atom_phases'] == '2',
            'native-only source scope differs')
    targets = json.loads(retain(crate / 'TARGETS.json'))
    require({row['path']: row['after_sha256'] for row in targets['targets']}
            == contract['emitted_sha256'], 'native-only target allowlist differs')

    # The unchanged copier writes application receipts under its own target/.
    # Reconstruct just its qualified inputs there, leaving the frozen C untouched.
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
    copy_qualified(crate / 'src/test/java/com/synexia/rewrite/M3MaskNativeRuntimeRecipeTest.java')
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

    clean_env = dict(os.environ)
    removed = [key for key in ['JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS',
                               'CLASSPATH', 'LD_PRELOAD', 'LD_LIBRARY_PATH'] if key in clean_env]
    for key in removed:
        clean_env.pop(key)
    commands = []

    def command(label, argv):
        argv = [str(value) for value in argv]
        started = time.monotonic()
        run = subprocess.run(argv, cwd=build, env=clean_env, capture_output=True, timeout=180, check=False)
        (output / (label + '.stdout.log')).write_bytes(run.stdout)
        (output / (label + '.stderr.log')).write_bytes(run.stderr)
        commands.append({'label': label, 'argv': argv, 'cwd': str(build),
                         'exit_code': run.returncode, 'elapsed_seconds': time.monotonic() - started,
                         'removed_ambient_environment_keys': removed,
                         'stdout_sha256': sha(run.stdout), 'stderr_sha256': sha(run.stderr)})
        write_json('commands.json', commands)
        require(run.returncode == 0, label + ' failed: ' + run.stderr.decode(errors='replace')[-2000:])
        print(label + ': PASS', flush=True)
        return run

    receipts = {}
    for label, apply in [('copier-check', False), ('copier-apply', True), ('copier-replay', True)]:
        command(label, [sys.executable, runner_crate / 'apply_receiver.py', repo] + (['--apply'] if apply else []))
        receipt_path = runner_crate / 'target' / ('application-receipt.json' if apply else 'application-check.json')
        raw = receipt_path.read_bytes()
        (output / (label + '.receipt.json')).write_bytes(raw)
        receipt = json.loads(raw)
        require(receipt['status'] == 'PASS' and receipt['recipe_proof_sha256'] == RECIPE_PROOF,
                'copier did not consume qualified C proof')
        require({row['path'] for row in receipt['files']} == set(contract['emitted_sha256']),
                'copier receipt target scope differs')
        receipts[label] = sha(raw)
        current = snapshot(repo)
        if label == 'copier-check':
            require(current == before, 'check modified receiving repository')
        elif label == 'copier-apply':
            post_apply = current
            for row in preimage['files']:
                if row['path'] != NATIVE:
                    require(current[row['path']] == before[row['path']], 'copier modified unchanged owner/regression')
            require({p for p, entry in current.items() if entry['kind'] == 'file'}
                    == {row['path'] for row in preimage['files']} | set(contract['emitted_sha256']),
                    'copier created an unlisted product file')
        else:
            require(current == post_apply, 'fixed-point replay changed product snapshot')
            require(all(row['action'] == 'ALREADY_APPLIED' for row in receipt['files']), 'replay not exact no-op')
    write_json('product-after-apply.json', post_apply)
    print('Receiving check/apply/replay complete; only two allowed source paths changed.', flush=True)

    for relative, expected in contract['emitted_sha256'].items():
        retain(repo / relative, expected)
    for row in preimage['files']:
        if row['path'] != NATIVE:
            retain(repo / row['path'], row['sha256'])
    cc = shutil.which(args.cc)
    nm = shutil.which(args.nm)
    require(cc is not None and nm is not None, 'native toolchain unavailable')
    tools = {}
    for path in [jdk / 'bin/java', jdk / 'bin/javac', jdk / 'include/jni.h',
                 jdk / 'include/linux/jni_md.h', Path(cc).resolve(), Path(nm).resolve()]:
        data = path.read_bytes()
        tools[str(path)] = {'bytes': len(data), 'sha256': sha(data)}
    write_json('toolchain-custody.json', tools)
    write_json('input-custody.json', {
        'scope': 'exact selected-source receiving repository, not a full checkout or installed JDK image',
        'repository': str(repo), 'base_revision': preimage['base_revision'],
        'recipe_proof_sha256': RECIPE_PROOF, 'receiver_root': proof['receiver_root'],
        'driver_sha256': sha(Path(__file__).read_bytes()), 'copier_sha256': COPIER,
        'preimage_custody_sha256': sha(preimage_raw),
        'inputs': [{'path': str(path), 'bytes': len(raw), 'sha256': sha(raw)}
                   for path, raw in sorted(observed.items())],
    })
    classes = build / 'classes'
    headers = build / 'headers'
    classes.mkdir()
    headers.mkdir()
    java_version = command('java-version', [jdk / 'bin/java', '-XX:-UsePerfData', '-version'])
    require(re.search(r'\bversion "21(?:[.\-\"]|$)', java_version.stderr.decode()), 'actual Java21 required')
    javac_version = command('javac-version', [jdk / 'bin/javac', '-J-XX:-UsePerfData', '--version'])
    require(re.match(r'^javac 21(?:\D|$)', javac_version.stdout.decode()), 'actual javac21 required')
    command('cc-version', [cc, '--version'])
    compiled = command('compile-java', [jdk / 'bin/javac', '-J-XX:-UsePerfData', '--release', '21',
        '-encoding', 'UTF-8', '-Xlint:all', '-Werror', '-proc:none', '-implicit:none',
        '-classpath', classes, '-sourcepath', '', '-h', headers, '-d', classes, repo / OWNER,
        *[repo / (TEST_ROOT + test + '.java') for test in TESTS]])
    require(not compiled.stderr, 'strict Java compilation emitted diagnostics')
    library = build / 'libm3_collections_mask.so'
    compiled = command('compile-native', [cc, '-std=c17', '-O3', '-Wall', '-Wextra', '-Werror',
        '-pedantic', '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'),
        '-fPIC', '-shared', '-include', headers / 'com_m3_collections_M3IndexMaskCompactor.h',
        repo / NATIVE, '-o', library])
    require(not compiled.stderr, 'strict native compilation emitted diagnostics')
    exports = command('exports', [nm, '-D', '--defined-only', library])
    symbols = sorted(line.split()[-1] for line in exports.stdout.decode().splitlines() if 'Java_' in line)
    require(len(symbols) == 9 and symbols == contract['jni_exports'], 'nine-export JNI ABI changed')
    for test in TESTS:
        run = command('target-' + test, [jdk / 'bin/java', '-XX:-UsePerfData', '-Xms32m', '-Xmx256m',
            '-Xcheck:jni', '-Djava.library.path=' + str(build), '-cp', classes, 'com.m3.collections.' + test])
        require(not run.stderr, 'actual target JVM emitted JNI/other diagnostics: ' + test)
        require(run.stdout == expected_stdout[test], 'target result differs from sealed prior oracle: ' + test)
    for path, raw in observed.items():
        require(path.read_bytes() == raw, 'qualification input changed: ' + str(path))
    for relative, entry in post_apply.items():
        if entry['kind'] == 'file':
            require(snapshot(repo)[relative] == entry, 'target source changed during validation')
    generated = {}
    for path in [library, *sorted(headers.glob('*.h')), *sorted(classes.rglob('*.class'))]:
        raw = path.read_bytes()
        generated[str(path.relative_to(output))] = {'bytes': len(raw), 'sha256': sha(raw)}
    write_json('generated-build-artifacts.json', generated)
    result = {
        'status': 'PASS', 'scope': 'fresh selected-source target Java/JNI execution after qualified C application',
        'repository': str(repo), 'base_revision': preimage['base_revision'],
        'whole_repository_checkout': False, 'commands': len(commands), 'copier_commands': 3,
        'fresh_target_jvm_test_commands': 3, 'recipe_execution': 'CONSUMED_EXISTING_QUALIFIED_C_OUTPUT',
        'recipe_proof_sha256': RECIPE_PROOF, 'receiver_root': proof['receiver_root'],
        'target_files': [{'path': relative, 'sha256': expected} for relative, expected in contract['emitted_sha256'].items()],
        'application_receipt_sha256': receipts, 'check_product_snapshot_unchanged': True,
        'replay_product_snapshot_unchanged': True, 'unchanged_owner_and_old_regressions_preserved': True,
        'real_jni_required': True, 'jni_check_mode': '-Xcheck:jni', 'jni_exports': symbols,
        'new_oracle': {'truth_cases': contract['truth_cases'], 'vectors': contract['vectors'],
                       'checks': contract['checks'], 'result_digest': contract['oracle_result_digest']},
        'existing_range_regression': {'cases': 7680, 'checks': 53782, 'stdout_sha256': EXPECTED_STDOUT[TESTS[1]]},
        'existing_dual_mask_regression': {'checks': 512, 'stdout_sha256': EXPECTED_STDOUT[TESTS[2]]},
        'all_fresh_oracle_stdout_matches_sealed_baseline': True,
        'prior_schedule_evidence': {'execution': 'REUSED_BY_EXACT_SOURCE_AND_RECEIPT_HASH_NOT_RERUN',
            'source_crate': str(source), 'results_path': 'verification/native/evidence/results.json',
            'results_sha256': PRIOR_RESULTS, 'cases': 899, 'schedule_sha256': contract['schedule_sha256']},
        'schedule_instrumentation_commands_this_run': 0, 'inputs_unchanged': True,
        'native_platform': sys.platform, 'full_jdk_build': 'NOT_RUN', 'whole_module': 'NOT_RUN',
        'jtreg': 'NOT_RUN', 'other_native_platforms': 'NOT_RUN', 'performance_claim': 'NONE',
        'target_metadata': 'PARENT_OWNED_NOT_EDITED', 'target_promotion': 'NOT_PERFORMED',
    }
    write_json('results.json', result)
    print(json.dumps(result, indent=2), flush=True)


if __name__ == '__main__':
    main()
