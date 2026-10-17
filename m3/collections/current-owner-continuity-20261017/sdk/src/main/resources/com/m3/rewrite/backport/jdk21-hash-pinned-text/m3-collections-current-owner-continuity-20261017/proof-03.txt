#!/usr/bin/env python3
# Modified 2026: current-owner continuity ADAPT; immutable donor blob 5c813e8aa766ae9bd90a8c3a7e54412a8b5281c8.
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Qualify actual recipe outputs; reuse predecessor Java/JNI gates. No runtime transformation."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parent))
from custody import Context, add_arguments, junit_inventory


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    for name in ('root', 'deps', 'junit'):
        ap.add_argument('--' + name, type=Path, required=True)
    ap.add_argument('--phase', choices=('all', 'java', 'native', 'finish'), default='all')
    add_arguments(ap)
    args = ap.parse_args()
    root, deps, junit = args.root.resolve(), args.deps.resolve(), args.junit.resolve()
    owner = root / 'm3/collections'
    packet = owner / 'wrappers'
    ctx = Context(root, args.run_id, args.expected_source_sha, 'wrappers', deps, junit)
    target = ctx.target
    if target.is_symlink() or target.parent.is_symlink():
        raise ValueError('symlinked proof output')
    target.mkdir(parents=True, exist_ok=True)
    evidence = target / 'evidence'
    evidence.mkdir(exist_ok=True)
    java = Path(os.environ.get('JAVA_HOME', str(Path(shutil.which('javac')).resolve().parent.parent)))
    digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    if not sys.platform.startswith('linux') or digest(junit) != '329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a':
        raise RuntimeError('Linux and exact JUnit console required')

    def run(name, command, expect_success=True):
        command = list(map(str, command))
        print(name + ": START", flush=True)
        result = subprocess.run(command, cwd=root, text=True, capture_output=True, timeout=90)
        (evidence / (name + '.log')).write_text('$ ' + repr(command) + '\n' + result.stdout + '\nEXIT=' + str(result.returncode) + '\n')
        (evidence / (name + '.stderr.log')).write_text(result.stderr)
        if (result.returncode == 0) != expect_success:
            raise RuntimeError(name + ':\n' + (result.stdout + result.stderr)[-14000:])
        if expect_success and ('WARNING in native method' in result.stdout + result.stderr or 'runtime error:' in result.stderr):
            raise RuntimeError(name + ': JNI/sanitizer diagnostic')
        print(name + ': ' + ('PASS' if expect_success else 'EXPECTED_FAILURE'), flush=True)
        return result.stdout

    expected = {}
    for kind in ('java', 'text'):
        resources = packet / ('recipe/src/main/resources/com/synexia/rewrite/hash-pinned-' + kind + '/m3-filtered-sorted-receiver')
        for line in (resources / 'manifest.tsv').read_text().splitlines():
            if not line or line.startswith('#'):
                continue
            path, before, after, filename = line.split('\t')
            full = path if kind == 'java' else 'm3/collections/' + path
            if before != 'ABSENT' or full in expected or digest(resources / filename) != after or digest(root / full) != after:
                raise ValueError('manifest/output drift: ' + full)
            expected[full] = after
    receipt = dict(line.split('\t') for line in (packet / 'recipe-receipt.tsv').read_text().splitlines() if line and not line.startswith('#'))
    if receipt != expected or len(expected) != 12:
        raise ValueError('incomplete actual recipe receipt')

    core = ctx.base / 'counts/core'
    interop = ctx.base / 'counts/interop'
    tests, headers = target / 'tests', target / 'headers'
    jars = sorted(p for p in deps.glob('*.jar') if not p.name.startswith('m3-'))
    cp = os.pathsep.join(map(str, [core, interop, tests] + jars))
    prepared = evidence / 'prepared.json'
    native_receipt = evidence / 'native.json'
    def compiled_inventory():
        return {str(p.resolve()): digest(p) for base in (core, interop, tests, headers)
                for p in base.rglob('*') if p.is_file() and (p.suffix in ('.class', '.h'))}
    if args.phase in ('all', 'java'):
        prepared.unlink(missing_ok=True)
        native_receipt.unlink(missing_ok=True)
        run('predecessor', [sys.executable, '-I', '-S', '-B', ctx.verification / 'bimap.py', '--root', root, '--deps', deps, '--junit', junit, *ctx.forward])
        prior = ctx.read_prior('bimap')
        retained = [prior['runtime'], prior['retained_scalar_runtime'], *prior['retained_runtime'].values()]
        if sum(x['tests'] for x in retained) != 112 or any(x[k] for x in retained for k in ('failures', 'errors', 'skipped')):
            raise RuntimeError('incomplete retained runtime proof')
        for directory in (tests, headers):
            shutil.rmtree(directory, ignore_errors=True)
            directory.mkdir()
        run('test-compile', [java / 'bin/javac', '--release', '21', '-proc:none', '-Xlint:all', '-Werror', '-h', headers,
                            '-cp', cp + os.pathsep + str(junit), '-d', tests] + sorted((packet / 'src/test/java').rglob('*.java')))
        reports = evidence / 'runtime-reports'
        shutil.rmtree(reports, ignore_errors=True)
        output = run('runtime', [java / 'bin/java', '-jar', junit, 'execute', '--class-path', cp, '--fail-if-no-tests',
                                '--select-class', 'com.m3.collections.M3WrapperContractsTest',
                                '--select-class', 'com.m3.collections.M3FilteredMapsTest', '--reports-dir', reports])
        xmls = list(reports.glob('TEST-*.xml'))
        totals = {k: sum(int(ET.parse(p).getroot().attrib.get(k, 0)) for p in xmls) for k in ('tests', 'failures', 'errors', 'skipped')}
        if totals != {'tests': 25, 'failures': 0, 'errors': 0, 'skipped': 0} or 'M3_WRAPPER_NAVIGATION_QUERIES=25088' not in output or 'M3_FILTERED_MAPS_PASS checks=862420' not in output:
            raise RuntimeError('incomplete wrapper runtime proof: ' + repr(totals))

        if junit_inventory(reports, ctx.pins['suites']['wrappers']) != totals:
            raise ValueError('wrapper named method drift')
        prepared.write_text(json.dumps({'outputs': expected, 'prior': prior, 'retained': retained,
                                        'totals': totals, 'compiled': compiled_inventory(),
                                        'custody': ctx.stamp([core, interop, tests, headers])}, sort_keys=True) + '\n')
    else:
        state = json.loads(prepared.read_text())
        if state['outputs'] != expected or state['compiled'] != compiled_inventory():
            raise RuntimeError('prepared source/compiled proof drift')
        ctx.check_stamp(state['custody'])
        prior, retained, totals = state['prior'], state['retained'], state['totals']
    if args.phase == 'java':
        print('JAVA_PHASE_COMPLETE', flush=True)
        return
    prepared_sha = digest(prepared)

    if args.phase in ('all', 'native'):
        library = target / 'librun_search.so'
        run('native-compile', [os.environ.get('CC', 'gcc'), '-std=c17', '-O2', '-Wall', '-Wextra', '-Werror', '-shared', '-fPIC',
                              '-fsanitize=undefined', '-fno-sanitize-recover=all', '-I' + str(java / 'include'),
                              '-I' + str(java / 'include/linux'), '-I' + str(headers),
                              owner / 'src/test/native/collections/run_search_jni.c', '-o', library])
        if 'Java_RunSearchJni_first' not in run('native-symbols', ['nm', '-D', '--defined-only', library]):
            raise RuntimeError('missing native export')
        for mode, flags in [('tiered', []), ('interpreter', ['-Xint']), ('c1', ['-XX:TieredStopAtLevel=1'])]:
            native = run('native-' + mode, [java / 'bin/java', *flags, '-Xmx32m', '-Xcheck:jni', '-cp', cp,
                         'com.m3.collections.M3FilteredMapsNativeSelfTest', library])
            direct = run('java-' + mode, [java / 'bin/java', *flags, '-Xmx32m', '-cp', cp, 'com.m3.collections.M3FilteredMapsSelfTest'])
            if 'M3_FILTERED_MAPS_JNI_PASS checks=166656 calls=41478 refusals=6' not in native or 'M3_FILTERED_MAPS_PASS checks=862420' not in direct:
                raise RuntimeError('incomplete mode corpus: ' + mode)
            for kind in ('java', 'native'):
                if (evidence / (kind + '-' + mode + '.stderr.log')).read_text():
                    raise RuntimeError('unexpected corpus stderr: ' + mode)

        native_receipt.write_text(json.dumps({'prepared_sha256': prepared_sha, 'modes': 3,
                                             'compiled': compiled_inventory(),
                                             'custody': ctx.stamp([core, interop, tests, headers, library])}) + '\n')
    else:
        native_state = json.loads(native_receipt.read_text())
        if native_state['prepared_sha256'] != prepared_sha or native_state['modes'] != 3 or native_state['compiled'] != compiled_inventory():
            raise RuntimeError('missing or mismatched native proof')
        ctx.check_stamp(native_state['custody'])
    if args.phase == 'native':
        print('NATIVE_PHASE_COMPLETE', flush=True)
        return

    # Negative-control fixture only: source is immutable donor behavior, namespace adapted for the same probe.
    original = packet / 'proof/M3CollectionViews.original.java.txt'
    if digest(original) != 'c3ff418b51cd1b471ded6da0376e0e17eb429151d7e1383da0b23c587c9a3f53':
        raise ValueError('original fixture drift')
    old_source = target / 'original-source/com/m3/collections/M3CollectionViews.java'
    old_source.parent.mkdir(parents=True, exist_ok=True)
    old_source.write_text(original.read_text().replace('package com.synexia.primitives;', 'package com.m3.collections;')
                          .replace('{@link PrimitiveIndexView}', 'primitive indexed views'))
    old = target / 'original'
    old.mkdir(exist_ok=True)
    compiler = [java / 'bin/javac', '--release', '21', '-proc:none', '-Xlint:all', '-Werror']
    run('original-compile', compiler + ['-d', old, old_source])
    run('probe-compile', compiler + ['-cp', core, '-d', tests, packet / 'proof/M3WrapperRefinementProbe.java'])
    for mode in ('erased', 'snapshot', 'remove', 'identity'):
        original_output = run('original-' + mode, [java / 'bin/java', '-cp', str(old) + os.pathsep + str(tests), 'M3WrapperRefinementProbe', mode], False)
        diagnostic = {'erased': 'ERASED_SORTED_CAPABILITY_LOST', 'snapshot': 'SNAPSHOT_PREDICATE_REPEATED=',
                      'remove': 'UnsupportedOperationException', 'identity': 'OWNER_IDENTITY_NOT_PRESERVED'}[mode]
        if diagnostic not in original_output + (evidence / ('original-' + mode + '.stderr.log')).read_text():
            raise ValueError('unrelated original negative-control failure: ' + mode)
        if 'WRAPPER_REFINEMENT_PASS=' + mode not in run('candidate-' + mode, [java / 'bin/java', '-cp', str(core) + os.pathsep + str(tests), 'M3WrapperRefinementProbe', mode]):
            raise RuntimeError('probe incomplete: ' + mode)
    old_api = run('original-public-api', [java / 'bin/javap', '-public', '-cp', old, 'com.m3.collections.M3CollectionViews'])
    new_api = run('candidate-public-api', [java / 'bin/javap', '-public', '-cp', core, 'com.m3.collections.M3CollectionViews'])
    if old_api != new_api:
        raise RuntimeError('public API drift')
    run('catalogue', [java / 'bin/java', '-cp', ctx.base / 'counts/tests',
                     'com.m3.collections.tooling.M3CountedCatalogue', packet / 'catalogue.tsv', 'filtered'])
    meta = core / 'META-INF/m3-wrappers'
    meta.mkdir(parents=True, exist_ok=True)
    for name in ('README.md', 'provenance.json', 'catalogue.tsv', 'name-mapping.tsv', 'recipe-receipt.tsv'):
        shutil.copyfile(packet / name, meta / name)
    for name in ('LICENSE', 'NOTICE'):
        shutil.copyfile(owner / name, meta / name)
    jar = target / 'm3-collections-with-wrappers.jar'
    run('package', [java / 'bin/jar', '--create', '--file', jar, '-C', core, '.'])
    modules = run('module-deps', [java / 'bin/jdeps', '--print-module-deps', jar]).strip()
    if modules != 'java.base':
        raise RuntimeError('core dependency boundary changed: ' + modules)
    result = {'schema': 'M3_FILTERED_SORTED_CURRENT_VERIFIED_V1', 'new_runtime': totals, 'retained_runtime': retained,
              'distinct_runtime_tests': 137, 'core_main_units': prior['core_main_units'], 'core_requires': ['java.base'],
              'java_checks_per_mode': 862420, 'additional_m3_navigation_queries': 25088,
              'jni_checks_per_mode': 166656, 'jni_calls_per_mode': 41478, 'jni_refusals_per_mode': 6,
              'modes': ['tiered', 'interpreter', 'c1'], 'mode_counts_overlap': True,
              'wrapper_original_negative_controls': 4, 'wrapper_candidate_passes': 4,
              'retained_allocation_negative_controls': prior['allocation_negative_controls'],
              'retained_allocation_candidate_passes': prior['allocation_candidate_passes'],
              'existing_owners_changed': False, 'output_sha256': expected, 'jar_sha256': digest(jar),
              'new_jni': 'UNCHANGED TEST-ONLY navigation peer; no production generic native filter/map',
              'full_library_coverage': False, 'full_reactor': 'NOT_RUN', 'installed_jdk': 'NOT_RUN', 'phase_advance': False}
    regressions = prior['current_regressions']
    if sum(value['tests'] for value in regressions.values()) != 5:
        raise ValueError('missing separate current mask/borrow regressions')
    result['current_regressions'] = regressions
    result['targeted_distinct_tests_with_current_regressions'] = 142
    result['current_mask_jni'] = prior['current_mask_jni']
    result['predecessor_receipt_sha256'] = digest(ctx.base / 'bimap/evidence/qualification.json')
    ctx.write_report(result, [core, interop, tests, headers, old, target / 'librun_search.so', jar])
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
