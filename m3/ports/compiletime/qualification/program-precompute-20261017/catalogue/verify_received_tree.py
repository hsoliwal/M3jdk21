#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify actual SDK POST images with the unchanged sources of a receiving Git HEAD."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

FAMILIES = {
    'collections': ('m3-bitlane-precomputed-navigation-20261017',
                    'm3-mask-program-specialization-20261017'),
    'compiletime': ('m3-program-escape-precompute-20261017',),
}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git_blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()


def contained(root, relative, *, file=True):
    """Use the same no-parent-symlink boundary as the qualified SDK receiver."""
    if (not relative or Path(relative).is_absolute() or '..' in Path(relative).parts
            or Path(relative).as_posix() != relative or relative == '.'):
        raise ValueError('unsafe source path: ' + relative)
    path = root / relative
    cursor = path
    while cursor != root:
        if cursor.is_symlink():
            raise ValueError('symlink source or parent: ' + relative)
        cursor = cursor.parent
    if not path.resolve().is_relative_to(root):
        raise ValueError('source path escape: ' + relative)
    if file and not path.is_file():
        raise ValueError('missing regular source file: ' + relative)
    return path


def rows(path, width):
    result = []
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        row = line.split('\t')
        if len(row) != width:
            raise ValueError('invalid receipt row: ' + str(path))
        result.append(row)
    if not result or len({row[0] for row in result}) != len(result):
        raise ValueError('empty or duplicate receipt: ' + str(path))
    return result


def checked_process(command):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True,
                            timeout=120)
    if result.returncode:
        raise ValueError('SDK receiver refused: ' + result.stdout[-2000:] + result.stderr[-2000:])
    return result.stdout + result.stderr


def require_fresh_sdk_report(report, inputs, results):
    suite = ET.parse(report).getroot()
    if int(suite.attrib.get('tests', '0')) != 5 or any(
            int(suite.attrib.get(key, '0')) for key in ('failures', 'errors', 'skipped')):
        raise ValueError('exact successful five-test SDK report required')
    if report.stat().st_mtime_ns < max(inputs.stat().st_mtime_ns, results.stat().st_mtime_ns):
        raise ValueError('SDK success report predates emitted inputs or Results')


def require_post_images(actual, candidate, target):
    for name, (_, after) in actual.items():
        if digest(contained(candidate, name)) != after:
            raise ValueError('SDK Result output drift: ' + name)
        if digest(contained(target, name)) != after:
            raise ValueError('required received POST image is absent: ' + name)


def admit_recipe(recipe, target, run=None):
    """Delegate source/Result qualification to the existing receiver; additionally require POST."""
    recipe, target = recipe.resolve(strict=True), target.resolve(strict=True)
    candidate = contained(recipe, 'target/candidate', file=False)
    report = contained(recipe, 'target/surefire-reports/TEST-com.synexia.rewrite.M3TaskRecipeTest.xml')
    inputs = contained(candidate, 'SDK_INPUTS.tsv')
    results = contained(candidate, 'SDK_RESULTS.tsv')
    require_fresh_sdk_report(report, inputs, results)
    command = [sys.executable, contained(recipe, 'receive.py'), '--crate', recipe,
               '--target', target, '--check']
    message = (run or checked_process)(command)
    seals = list((recipe/'src/main/resources/com/synexia/rewrite').glob('hash-pinned-*/*/manifest.tsv'))
    identities = {manifest.parent.name for manifest in seals}
    if len(identities) != 1:
        raise ValueError('ambiguous or missing recipe identity')
    manifests = {}
    for manifest in sorted(seals):
        contained(recipe, manifest.relative_to(recipe).as_posix())
        for name, before, after, resource in rows(manifest, 4):
            if name in manifests or not re.fullmatch('[0-9a-f]{64}', after):
                raise ValueError('duplicate target or invalid postimage digest')
            if digest(contained(manifest.parent, resource)) != after:
                raise ValueError('resource postimage drift: ' + name)
            manifests[name] = (before, after)
    actual = {name: (before, after) for name, before, after in rows(results, 3)}
    if actual != manifests:
        raise ValueError('SDK Results differ from qualified manifests')
    require_post_images(actual, candidate, target)
    expected = ('M3_SDK_RESULTS_RECEIVER_PASS mode=check targets=' + str(len(actual)) + ' changes=0')
    if expected not in message.splitlines():
        raise ValueError('SDK receiver did not confirm zero pending POST images')
    return {
        'name': next(iter(identities)),
        'targets': {name: value[1] for name, value in sorted(actual.items())},
        'sdk_inputs_sha256': digest(inputs), 'sdk_results_sha256': digest(results),
        'sdk_tests_sha256': digest(report), 'sdk_inputs': dict(rows(inputs, 2)),
    }


def java_sources(root, relative):
    base = contained(root, relative, file=False)
    if not base.is_dir():
        raise ValueError('required source directory missing: ' + relative)
    found = []
    for path in sorted(base.rglob('*')):
        if path.is_symlink():
            raise ValueError('symlink in compiled source tree: ' + str(path))
        if path.is_file() and path.suffix == '.java':
            contained(root, path.relative_to(root).as_posix())
            found.append(path)
    if not found:
        raise ValueError('empty source cohort: ' + relative)
    return found


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--target', type=Path, required=True)
    group = p.add_mutually_exclusive_group(required=True)
    group.add_argument('--recipe', type=Path, action='append')
    group.add_argument('--recipes-root', type=Path)
    p.add_argument('--family', choices=tuple(FAMILIES), required=True)
    p.add_argument('--expected-head', help='Optional exact receiving commit admission')
    p.add_argument('--jdk', type=Path, required=True)
    p.add_argument('--junit', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    root, out = args.target.resolve(strict=True), args.output.resolve()
    if out.exists() or out.is_relative_to(root) or root.is_relative_to(out):
        raise ValueError('fresh isolated proof output required')
    if args.expected_head and not re.fullmatch('[0-9a-f]{40}', args.expected_head):
        raise ValueError('expected receiving HEAD must be an exact Git commit')
    out.mkdir(parents=True)
    commands, sources, targets, recipe_receipts = [], {}, {}, {}
    jdk, junit = args.jdk.resolve(strict=True), args.junit.resolve(strict=True)
    environment = dict(os.environ)
    environment['JAVA_TOOL_OPTIONS'] = '-XX:ActiveProcessorCount=2'

    def run(name, command, timeout=300):
        command = list(map(str, command))
        started = time.monotonic()
        try:
            result = subprocess.run(command, cwd=root, env=environment,
                                    capture_output=True, text=True, timeout=timeout)
            stdout, stderr, code = result.stdout, result.stderr, result.returncode
            classification = 'COMPLETED'
        except subprocess.TimeoutExpired as failure:
            def decoded(value):
                return value.decode('utf-8', errors='backslashreplace') if isinstance(value, bytes) else value or ''
            stdout, stderr, code = decoded(failure.stdout), decoded(failure.stderr), None
            classification = 'INCOMPLETE_TIMEOUT'
        if '\0' in stdout:
            # Keep NUL-delimited Git output losslessly in a text JSON envelope.
            (out/(name+'.stdout.json')).write_text(json.dumps({'stdout': stdout}, indent=2)+'\n')
        else:
            (out/(name+'.stdout.log')).write_text(stdout)
        (out/(name+'.stderr.log')).write_text(stderr)
        commands.append({'name': name, 'argv': command, 'exit': code,
                         'classification': classification, 'seconds': time.monotonic()-started})
        (out/'COMMANDS.json').write_text(json.dumps(commands, indent=2)+'\n')
        if classification == 'INCOMPLETE_TIMEOUT':
            raise RuntimeError(name+' timed out; partial evidence retained, no product failure inferred')
        if code or 'WARNING in native method' in stdout+stderr or 'runtime error:' in stderr:
            raise RuntimeError(name+' failed: '+stdout[-3000:]+stderr[-3000:])
        print(name+' PASS', flush=True)
        return stdout+stderr

    recipes = ([args.recipes_root/name for name in FAMILIES[args.family]]
               if args.recipes_root else args.recipe)
    for index, recipe in enumerate(recipes):
        receipt = admit_recipe(recipe, root, lambda command: run('sdk-post-admission-'+str(index), command, 120))
        name = receipt['name']
        if name in recipe_receipts or set(targets) & set(receipt['targets']):
            raise ValueError('duplicate recipe or target')
        recipe_receipts[name] = {key: value for key, value in receipt.items() if key != 'sdk_inputs'}
        targets.update(receipt['targets'])
    if set(recipe_receipts) != set(FAMILIES[args.family]) or len(targets) != (11 if args.family == 'collections' else 5):
        raise ValueError('wrong exact recipe family or target inventory')

    version = run('java-version', [jdk/'bin/java', '-version'])
    if 'version "21.' not in version and 'version "21"' not in version:
        raise ValueError('Java 21 required')
    head = run('receiving-head', ['git', 'rev-parse', 'HEAD']).strip()
    if args.expected_head and head != args.expected_head:
        raise ValueError('receiving HEAD differs from explicit admission')
    tree_roots = (['m3/collections/src/main/java', 'm3/collections/src/test/java',
                   'm3/collections/src/main/native/collections'] if args.family == 'collections' else
                  ['m3/vendor/third_party/asm-9.6/src/main/java', 'm3/vendor/third_party/asm-tree/src/main/java',
                   'm3/vendor/third_party/asm-analysis/src/main/java', 'm3/ports/precompute/src/main/java',
                   'm3/ports/compiletime/src/main/java', 'm3/ports/compiletime/src/test/java'])
    tree_text = run('receiving-source-tree', ['git', 'ls-tree', '-r', '-z', '--full-tree', head, '--', *tree_roots])
    git_sources = {}
    for entry in tree_text.split('\0'):
        if not entry:
            continue
        info, relative = entry.split('\t', 1)
        mode, kind, object_id = info.split(' ')
        git_sources[relative] = (mode, kind, object_id)

    def remember(paths):
        for path in paths:
            relative = path.relative_to(root).as_posix()
            data = contained(root, relative).read_bytes()
            actual = hashlib.sha256(data).hexdigest()
            if relative in targets:
                if actual != targets[relative]:
                    raise ValueError('qualified POST changed: ' + relative)
            else:
                entry = git_sources.get(relative)
                if not entry or entry[0] not in ('100644', '100755') or entry[1] != 'blob' or git_blob(data) != entry[2]:
                    raise ValueError('unqualified change outside SDK targets: ' + relative)
            sources[relative] = actual
        return paths

    cohorts = {}
    def cohort(relative):
        paths = java_sources(root, relative)
        names = {path.relative_to(root).as_posix() for path in paths}
        prefix = relative+'/'
        expected = {name for name in git_sources if name.startswith(prefix) and name.endswith('.java')}
        expected.update(name for name in targets if name.startswith(prefix) and name.endswith('.java'))
        if names != expected:
            raise ValueError('compiled source inventory differs from HEAD plus SDK targets: ' + relative)
        cohorts[relative] = names
        return remember(paths)

    def argfile(name, paths):
        path = out/(name+'.args')
        path.write_text(''.join(json.dumps(str(x), ensure_ascii=False)+'\n' for x in paths))
        return '@'+str(path)

    common = [jdk/'bin/javac', '--release', '21', '-proc:none', '-Xlint:all,-module', '-Werror']
    classes, tests, headers = [out/name for name in ('classes', 'test-classes', 'headers')]
    if args.family == 'collections':
        mains = cohort('m3/collections/src/main/java')
        run('strict-current-named-module', common+['-h', headers, '-d', classes, argfile('mains', mains)])
        test_names = ['M3BitLane28FenwickSelfTest', 'M3BitLane28PreparedTraversalSelfTest',
                      'M3ObjectStateSlots28SelfTest', 'M3IndexMaskWindowSelfTest', 'M3MaskProgramSelfTest',
                      'M3LongBitmapFilteredSnapshotSelfTest', 'M3LongIndexProjectionMaskOffsetProof']
        test_sources = remember([root/'m3/collections/src/test/java/com/m3/collections'/(name+'.java')
                                 for name in test_names])
        run('current-test-compile', common+['-cp', classes, '-d', tests, argfile('tests', test_sources)])
        cp = str(classes)+os.pathsep+str(tests)
        for name in test_names:
            extra = ['java'] if name == 'M3MaskProgramSelfTest' else []
            run('java-'+name, [jdk/'bin/java', '-Xmx512m', '-ea', '-cp', cp, 'com.m3.collections.'+name, *extra])
        native_root = root/'m3/collections/src/main/native/collections'
        bitlane, window, compactor = remember([native_root/name for name in
              ('segmented_bit_lane.c', 'm3_index_mask_window.c', 'm3_index_mask_compactor.c')])
        cflags = ['cc', '-std=c17', '-Wall', '-Wextra', '-Werror', '-O2', '-fPIC', '-shared',
                  '-fsanitize=undefined', '-fno-sanitize-recover=all',
                  '-I'+str(jdk/'include'), '-I'+str(jdk/'include/linux')]
        bitlib = out/'libbitlane.so'
        run('current-bitlane-native', cflags+['-include', headers/'com_m3_collections_M3BitLane28.h', bitlane, '-o', bitlib])
        run('current-mask-native', cflags+[window, compactor, '-o', out/'libm3_collections_mask.so'])
        run('current-bitlane-checked-jni', [jdk/'bin/java', '-Xcheck:jni', '-Xmx512m', '-ea', '-cp', cp,
            'com.m3.collections.M3BitLane28PreparedTraversalSelfTest', 'native', bitlib])
        run('current-mask-checked-jni', [jdk/'bin/java', '-Xcheck:jni', '-Xmx512m',
            '-Djava.library.path='+str(out), '-cp', cp, 'com.m3.collections.M3MaskProgramSelfTest', 'native'])
    else:
        asm_sources = []
        for name in ('asm-9.6', 'asm-tree', 'asm-analysis'):
            asm_sources += cohort('m3/vendor/third_party/'+name+'/src/main/java')
        dependencies = out/'asm-classes'
        run('current-vendored-asm', [jdk/'bin/javac', '--release', '8', '-proc:none', '-d', dependencies,
                                    argfile('asm', asm_sources)])
        mains = []
        for name in ('precompute', 'compiletime'):
            mains += cohort('m3/ports/'+name+'/src/main/java')
        test_sources = cohort('m3/ports/compiletime/src/test/java')
        cp = str(dependencies)+os.pathsep+str(junit)
        run('strict-current-port-and-tests', common+['-cp', cp, '-d', classes,
                                                   argfile('ports', mains+test_sources)])
        cp = str(classes)+os.pathsep+str(dependencies)
        run('current-port-junit', [jdk/'bin/java', '-Xmx512m', '--add-opens',
            'java.base/java.util.regex=ALL-UNNAMED', '-jar', junit, 'execute', '--disable-banner',
            '--class-path', cp, '--scan-class-path', '--include-package', 'com.m3.compiletime',
            '--fail-if-no-tests', '--reports-dir', out/'junit-reports'])
        run('current-ast-leaf-selftest', [jdk/'bin/java', '-Xmx512m', '-ea', '-cp', cp,
                                         'com.m3.compiletime.M3ASTLeafIndexSelfTest'])
    for relative, expected in {**sources, **targets}.items():
        if digest(contained(root, relative)) != expected:
            raise ValueError('source changed during integration proof: '+relative)
    for relative, expected in cohorts.items():
        if {path.relative_to(root).as_posix() for path in java_sources(root, relative)} != expected:
            raise ValueError('source cohort changed during integration proof: '+relative)
    if run('receiving-head-after', ['git', 'rev-parse', 'HEAD']).strip() != head:
        raise ValueError('receiving HEAD changed during integration proof')
    report = {'schema': 'M3_RECEIVED_CURRENT_TREE_PROOF_V1', 'status': 'PASS', 'family': args.family,
              'receiving_head': head, 'recipe_targets': targets, 'recipe_receipts': recipe_receipts,
              'compiled_sources': sources, 'compiled_source_count': len(sources), 'main_java_sources': len(mains),
              'non_target_sources': 'EXACT_RECEIVING_HEAD_GIT_BLOBS',
              'jdk_version': version, 'junit_sha256': digest(junit),
              'java_tool_options': environment['JAVA_TOOL_OPTIONS'], 'commands': len(commands),
              'sources_unchanged': True, 'source_gate_sha256': digest(Path(__file__)),
              'whole_jdk_image_claimed': False, 'global_phase_advanced': False}
    (out/'RESULT.json').write_text(json.dumps(report, indent=2)+'\n')
    print('CURRENT_RECEIVING_TREE_PASS family='+args.family+' sources='+str(len(sources)), flush=True)


if __name__ == '__main__':
    sys.dont_write_bytecode = True
    main()
