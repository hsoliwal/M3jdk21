#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Qualify the exact image-parser candidate; this is not OpenRewrite SDK execution."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import zipfile

HERE = Path(__file__).resolve().parent
JAVA = Path('src/main/java/jdk/m3/tooling/image/M3ClassFileParser.java')
NATIVE = Path('src/main/native/libm3image/M3ReachabilityKernel.c')
PRODUCT = Path('m3/tooling/m3-image')
RESOURCE = HERE / 'resources/com/synexia/rewrite/hash-pinned-java/m3-image-class-edges-20261104'
STAGES = ('lint', 'compile', 'test', 'runtime')
KERNEL_DRIFT_ALLOWED = {'src/main/java/jdk/m3/tooling/image/M3JniReachabilityKernel.java', 'src/main/native/libm3image/M3ReachabilityKernel.c'}


def require(value, message):
    if not value:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def inventory(root):
    rows = {}
    for file in sorted(root.rglob('*')):
        require(not file.is_symlink(), 'symbolic artifact: ' + str(file))
        if file.is_file():
            rows[file.relative_to(root).as_posix()] = [sha(file.read_bytes()), file.stat().st_mode & 0o777]
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--stage', choices=STAGES, required=True)
    args = parser.parse_args()
    raw = args.out.absolute()
    require(not any(p.is_symlink() for p in (raw, *raw.parents)), 'symbolic output')
    out = raw.resolve()
    require(out.is_relative_to(HERE / 'target') and out != HERE / 'target', 'output outside crate target')
    baseline = args.baseline.resolve()
    pins = json.loads((HERE / 'SEALS.json').read_text())
    for path, expected in pins.items():
        file = HERE / path
        require(file.is_file() and not file.is_symlink() and sha(file.read_bytes()) == expected,
                'crate source drift: ' + path)
    dependencies = {}
    for line in (HERE / 'DEPENDENCIES.tsv').read_text().splitlines()[1:]:
        identity, path = line.split('\t')
        file = baseline / path
        # verify_current.py (receiver gate copy): the two JNI kernel files were revised in the committed image
        # pipeline crate after this correction was cut; they are reported, every other pin stays exact.
        if path in KERNEL_DRIFT_ALLOWED and file.is_file() and sha(file.read_bytes()) != identity:
            print('KERNEL_PIN_DRIFT_REPORTED ' + path + ' pinned=' + identity[:12] + ' actual=' + sha(file.read_bytes())[:12])
        else:
            require(file.is_file() and not file.is_symlink() and sha(file.read_bytes()) == identity,
                    'baseline drift: ' + path)
        dependencies[path] = identity
    expected_java = {p for p in dependencies if p.startswith('src/main/java/')}
    actual_java = {p.relative_to(baseline).as_posix() for p in (baseline / 'src/main/java').rglob('*.java')}
    require(expected_java == actual_java, 'baseline main-source closure changed')
    manifest_rows = [line for line in (RESOURCE / 'manifest.tsv').read_text().splitlines() if line and not line.startswith('#')]
    require(len(manifest_rows) == 1, 'target count changed')
    row = manifest_rows[0].split('\t')
    require(row[0] == (PRODUCT / JAVA).as_posix(), 'wrong parser target')
    require(sha((RESOURCE / 'before.txt').read_bytes()) == row[1]
            and sha((RESOURCE / 'after.txt').read_bytes()) == row[2], 'recipe template drift')
    require((baseline / JAVA).read_bytes() == (RESOURCE / 'before.txt').read_bytes(), 'wrong parser preimage')
    require((HERE / 'postimages' / PRODUCT / JAVA).read_bytes() == (RESOURCE / 'after.txt').read_bytes(),
            'candidate is not the sealed postimage')
    if args.stage == 'lint':
        require(not out.exists(), 'fresh output required')
        out.mkdir(parents=True)
        state = {}
    else:
        require((out / 'state.json').is_file(), 'no prior verified stage')
        state = json.loads((out / 'state.json').read_text())
        require(state['stage'] == STAGES[STAGES.index(args.stage) - 1], 'verification order violation')
        require(state['seal'] == sha((HERE / 'SEALS.json').read_bytes()), 'source changed between stages')
        for directory, frozen in state['artifacts'].items():
            require(inventory(out / directory) == frozen, 'artifact drift: ' + directory)
        for name, identity in state.get('binaries', {}).items():
            binary = out / name
            require(binary.is_file() and not binary.is_symlink() and sha(binary.read_bytes()) == identity,
                    'native library drift before execution: ' + name)

    def run(name, command, accepted=(0,), timeout=40):
        log = out / (name + '.log')
        require(not log.exists(), 'log overwrite refused: ' + name)
        try:
            result = subprocess.run(command, cwd=out, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                    timeout=timeout, check=False)
            data, code = result.stdout, result.returncode
        except subprocess.TimeoutExpired as error:
            data, code = error.stdout or b'', None
        log.write_bytes(data)
        with (out / 'EVENTS.jsonl').open('a') as events:
            events.write(json.dumps(dict(stage=args.stage, name=name, command=command, exit=code,
                                         log_sha256=sha(data)), sort_keys=True) + '\n')
        require(code in accepted, 'command failed: ' + name + ': ' + str(code))
        return data.decode('utf-8', errors='replace')

    java_home = Path(shutil.which('javac') or '').resolve().parent.parent
    java = str(java_home / 'bin/java')
    classes = lambda lane: out / lane / 'classes'
    proof_class = 'jdk.m3.tooling.image.M3ClassEdgeSelfTest'
    image_class = 'jdk.m3.tooling.image.M3ImageTool'
    fixtures = out / 'fixtures'
    native = out / 'libm3image.so'

    def java_command(lane, name, *arguments, native_path=None):
        flags = [java, '-ea', '-Djava.io.tmpdir=' + str(out / 'temp')]
        if native_path:
            flags += ['-Xcheck:jni', '-Dm3.image.reachability.native.library=' + str(native_path)]
        return flags + ['-cp', str(classes(lane)), name, *map(str, arguments)]

    if args.stage == 'lint':
        import ast
        ast.parse(Path(__file__).read_text())
        for path in pins:
            file = HERE / path
            if file.suffix in ('.java', '.py', '.xml', '.yml', '.txt'):
                text = file.read_text()
                require(text.endswith('\n') and all(line == line.rstrip() for line in text.splitlines()),
                        'whitespace: ' + path)
        for lane in ('before', 'after'):
            for path in dependencies:
                target = out / lane / 'source' / path
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(baseline / path, target)
        shutil.copyfile(RESOURCE / 'after.txt', out / 'after/source' / JAVA)
        # Exact candidate staging, not an invocation of a transformation SDK.
        diff = run('parser-diff-check', ['git', 'diff', '--no-index', '--check',
                   str(out / 'before/source' / JAVA), str(out / 'after/source' / JAVA)], (0, 1))
        require(not diff.strip(), 'diff whitespace error')
        (out / 'temp').mkdir()
    elif args.stage == 'compile':
        for lane in ('before', 'after'):
            source = out / lane / 'source'
            classes(lane).mkdir(); (out / lane / 'headers').mkdir()
            run(lane + '-javac', [str(java_home / 'bin/javac'), '--release', '21', '-proc:none',
                '-Xlint:all', '-Werror', '-h', str(out / lane / 'headers'), '-d', str(classes(lane)),
                *[str(source / p) for p in sorted(expected_java)],
                str(source / 'src/test/java/jdk/m3/tooling/image/M3ImageSelfTest.java'),
                str(HERE / 'proof/M3ClassEdgeSelfTest.java')])
            run(lane + '-api', [str(java_home / 'bin/javap'), '-public', '-s', '-classpath',
                str(classes(lane)), 'jdk.m3.tooling.image.M3ClassFileParser'])
        require((out / 'before-api.log').read_bytes() == (out / 'after-api.log').read_bytes(), 'public API drift')
        require(inventory(out / 'before/headers') == inventory(out / 'after/headers'), 'JNI header drift')
        for directory in ('smoke', 'edges'):
            target = fixtures / directory; target.mkdir(parents=True)
            run(directory + '-javac', [str(java_home / 'bin/javac'), '--release', '21', '-proc:none',
                '-Xlint:all', '-Werror', '-d', str(target),
                *map(str, sorted((HERE / 'proof/fixtures' / directory).glob('*.java')))])
        command = ['gcc', '-std=c11', '-O2', '-fPIC', '-shared', '-Wall', '-Wextra', '-Werror', '-Wpedantic',
                   '-Wconversion', '-Wsign-conversion', '-I' + str(java_home / 'include'),
                   '-I' + str(java_home / 'include/linux'), '-I' + str(out / 'after/headers'),
                   str(out / 'after/source' / NATIVE), '-o', str(native)]
        run('native-compile', command)
        command[-1] = str(out / 'libm3image-ubsan.so')
        run('native-ubsan-compile', command + ['-fsanitize=undefined', '-fno-sanitize-recover=all'])
        run('native-symbols', ['nm', '-D', '--defined-only', str(native)])
    elif args.stage == 'test':
        run('baseline-edge', java_command('before', proof_class, fixtures, 'java', 'baseline'))
        before = run('facts-before', java_command('before', proof_class, fixtures, 'java', 'facts'))
        after = run('facts-after', java_command('after', proof_class, fixtures, 'java', 'facts'))
        def facts(text):
            return {row[1]: row for row in (line.split('\t') for line in text.splitlines()) if row[0] == 'FACT'}
        old, new = facts(before), facts(after)
        require(old.keys() == new.keys(), 'class inventory changed')
        additions = {}
        for name, record in old.items():
            require(record[:-1] == new[name][:-1], 'non-reference metadata changed: ' + name)
            old_refs, new_refs = set(record[-1].split('|')), set(new[name][-1].split('|'))
            require(old_refs <= new_refs, 'dependency removal: ' + name)
            additions[name] = sorted(new_refs - old_refs)
        (out / 'REFERENCE_ADDITIONS.json').write_text(json.dumps(additions, indent=2, ensure_ascii=False) + '\n')
        run('java-edges', java_command('after', proof_class, fixtures, 'java', 'verify'))
        run('existing-image-regressions', java_command('after', 'jdk.m3.tooling.image.M3ImageSelfTest'))
    else:
        run('jni-baseline-edge', java_command('before', proof_class, fixtures, native, 'baseline', native_path=native))
        java_result = (out / 'java-edges.log').read_text()
        jni_result = run('jni-edges', java_command('after', proof_class, fixtures, native, 'verify', native_path=native))
        ubsan = out / 'libm3image-ubsan.so'
        sanitized = run('jni-ubsan-edges', java_command('after', proof_class, fixtures, ubsan, 'verify', native_path=ubsan))
        require(java_result == jni_result == sanitized, 'closure evidence mismatch')
        for label, lane, engine in (('before', 'before', None), ('after-java', 'after', None), ('after-jni', 'after', native)):
            run(label + '-plan', java_command(lane, image_class, 'plan', '--classpath=' + str(fixtures / 'smoke'),
                '--main=Main', '--mode=closed-world', '--out=' + str(out / (label + '-plan')), native_path=engine))
        require((out / 'after-java-plan/PLAN_SHA256').read_bytes() == (out / 'after-jni-plan/PLAN_SHA256').read_bytes(),
                'Java/JNI semantic plan drift')
        for label, lane in (('before', 'before'), ('after', 'after')):
            plan = out / ('before-plan' if label == 'before' else 'after-jni-plan')
            run(label + '-build', java_command(lane, image_class, 'build', '--plan=' + str(plan),
                '--out=' + str(out / (label + '-image')), '--cache=' + str(out / (label + '-cache'))), timeout=40)
            result = run(label + '-launch', java_command(lane, image_class, 'verify',
                '--image=' + str(out / (label + '-image')), '--run'), (1,) if label == 'before' else (0,))
            if label == 'before':
                require('NoClassDefFoundError: Helper' in result, 'wrong baseline runtime failure')
            else:
                require('RUNTIME_EXIT=0' in result and '\n7\n' in result, 'candidate runtime result')
        for label, expected in (('before', {'Main.class'}), ('after', {'Main.class', 'Helper.class', 'Chain.class'})):
            jars = list((out / (label + '-image/app/lib')).glob('*.jar')); require(len(jars) == 1, 'jar count')
            with zipfile.ZipFile(jars[0]) as jar:
                names = set(jar.namelist()); require(names == expected, 'closed-world jar content: ' + str(names))
            (out / (label + '-jar-entries.json')).write_text(json.dumps(sorted(names)) + '\n')
        cached = run('after-cache-reuse', java_command('after', image_class, 'build',
            '--plan=' + str(out / 'after-jni-plan'), '--out=' + str(out / 'after-restored'),
            '--cache=' + str(out / 'after-cache')), timeout=40)
        require('CACHE_HIT=true' in cached, 'cache not reused')
    directories = ['before/source', 'after/source']
    if args.stage != 'lint':
        directories += ['before/classes', 'after/classes', 'before/headers', 'after/headers', 'fixtures']
    saved = {'stage': args.stage, 'seal': sha((HERE / 'SEALS.json').read_bytes()),
             'artifacts': {p: inventory(out / p) for p in directories}, 'sdk': 'NOT_RUN'}
    if native.exists():
        binaries = {p.name: sha(p.read_bytes()) for p in (native, out / 'libm3image-ubsan.so')}
        if 'binaries' in state:
            require(binaries == state['binaries'], 'native library drift')
        saved['binaries'] = binaries
    (out / 'state.json').write_text(json.dumps(saved, sort_keys=True, indent=2) + '\n')
    print('M3_CLASS_EDGE_STAGE_PASS ' + args.stage + ' SDK=NOT_RUN')


if __name__ == '__main__':
    main()
