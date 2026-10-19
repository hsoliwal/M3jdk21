# SPDX-License-Identifier: Apache-2.0
# Portable composition of unchanged frozen Spring fixtures and established strict commands.
import argparse
import ctypes
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

HERE = Path(__file__).resolve().parent
OWNER = 'com.m3.collections.spring.M3LinkedCaseInsensitiveMap'
PROVIDER = 'org.springframework.util.LinkedCaseInsensitiveMap'
CAP = 512 * 1024**2
RESERVE = 8 * 1024**3


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path('.'))
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--temp', type=Path)
    parser.add_argument('--java-home', type=Path)
    parser.add_argument('--native-library-dir', type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    jar = args.jar.resolve()
    output = args.out.resolve()
    if output.exists():
        raise SystemExit('preserve prior output; choose a new --out')
    if not output.parent.is_dir() or shutil.disk_usage(output.parent).free < RESERVE + CAP:
        raise SystemExit('output destination absent or reserve/task headroom refused')
    # Source/provider custody is checked before any compilation or fixture execution.
    saved = sys.argv
    try:
        sys.argv = ['verify.py', '--root', str(root), '--jar', str(jar)]
        load('spring_packet_custody', HERE / 'verify.py').main()
    finally:
        sys.argv = saved
    output.mkdir()
    temporary = args.temp.resolve() if args.temp else output / 'tmp'
    temporary.mkdir(parents=True, exist_ok=True)
    os.environ.update(TEMP=str(temporary), TMP=str(temporary), TMPDIR=str(temporary))
    if os.name == 'nt':
        buffer = ctypes.create_unicode_buffer(32768)
        if not ctypes.windll.kernel32.GetTempPathW(len(buffer), buffer):
            raise ctypes.WinError()
        if buffer.value.rstrip('\\').casefold() != str(temporary).rstrip('\\').casefold():
            raise SystemExit('native child temporary directory differs; no fallback')
    suffix = '.exe' if os.name == 'nt' else ''
    java_home = args.java_home or (Path(os.environ['JAVA_HOME']) if os.environ.get('JAVA_HOME') else None)
    java = str(java_home / ('bin/java' + suffix)) if java_home else shutil.which('java')
    javac = str(java_home / ('bin/javac' + suffix)) if java_home else shutil.which('javac')
    if not java or not javac:
        raise SystemExit('explicit/provisioned Java21 tools absent')
    commands = []

    def budget():
        used = sum(path.stat().st_size for path in output.rglob('*') if path.is_file())
        if used > CAP or shutil.disk_usage(output).free < RESERVE:
            raise SystemExit('output budget or reserve refused')

    def run(label, argv, stdout_file=None):
        budget()
        completed = subprocess.run(list(map(str, argv)), cwd=root, env=os.environ.copy(),
            stdin=subprocess.DEVNULL, capture_output=True, timeout=180)
        (output / (label + '.stderr')).write_bytes(completed.stderr)
        (output / (stdout_file or label + '.stdout')).write_bytes(completed.stdout)
        record = {'label': label, 'argv': list(map(str, argv)), 'returncode': completed.returncode,
            'stdoutFile': stdout_file or label + '.stdout', 'stderrFile': label + '.stderr'}
        commands.append(record)
        (output / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n', encoding='utf-8')
        budget()
        if completed.returncode:
            raise SystemExit('command failed; actual output retained: ' + label)

    runtime = ['-Xmx256m', '-Xss512k', '-XX:ActiveProcessorCount=2', '-XX:-UsePerfData',
        '-Djava.io.tmpdir=' + str(temporary), '-Duser.home=' + str(temporary),
        '-XX:ErrorFile=' + str(output / 'hs_err_pid%p.log')]
    compiler_vm = ['-J' + option for option in runtime]
    base = output / 'core'
    owned = output / 'owned'
    provider_fixtures = output / 'provider-fixtures'
    owned_fixtures = output / 'owned-fixtures'
    for directory in (base, owned, provider_fixtures, owned_fixtures):
        directory.mkdir()
    spec = json.loads((HERE / 'source-bindings.json').read_text(encoding='utf-8'))
    core = [root / row['path'] for row in spec['sources'] if row['path'].startswith('m3/collections/src/main/java/')]
    expected_core = spec['selectedBaseJavaSources']
    if not isinstance(expected_core, int) or expected_core < 1 or len(core) != expected_core:
        raise SystemExit('selected bound core cardinality differs')
    response = output / 'core.args'
    response.write_text(''.join('"' + str(path).replace('\\', '/') + '"\n' for path in core), encoding='utf-8')
    strict = ['--release', '21', '-g', '-encoding', 'UTF-8', '-proc:none']
    run('strict-core-compile', [javac, *compiler_vm, *strict, '-Xlint:all,-module', '-Werror', '-d', base, '@' + str(response)])
    owner_source = root / 'm3/collections/implementations/spring-case-insensitive-map/src/main/java/com/m3/collections/spring/M3LinkedCaseInsensitiveMap.java'
    run('strict-jdk-only-owner-compile', [javac, *compiler_vm, *strict, '-Xlint:all', '-Werror', '-d', owned, owner_source])
    common = [HERE / name for name in [
        'SpringCaseInsensitiveMapProtocolProof.java', 'SpringCaseInsensitiveMapEntryCompatibilityObserver.java',
        'SpringCaseInsensitiveMapFullSurfaceProof.java', 'SpringEmptyLargeConstructorProbe.java',
        'SpringEntryGetterProtocol.java', 'SpringMapEqualityProtocol.java']]
    run('strict-provider-fixture-compile', [javac, *compiler_vm, *strict, '-Xlint:all', '-Werror',
        '-cp', jar, '-d', provider_fixtures, *common,
        HERE / 'provider-hook/SpringCaseInsensitiveMapHookCases.java', HERE / 'ProviderBaselineLauncher.java'])
    run('strict-owned-fixture-compile', [javac, *compiler_vm, *strict, '-Xlint:all', '-Werror',
        '-cp', owned, '-d', owned_fixtures, *common,
        HERE / 'owned-hook/SpringCaseInsensitiveMapHookCases.java', HERE / 'OwnedBaselineLauncher.java'])
    run('strict-projection-fixture-compile', [javac, *compiler_vm, *strict, '-Xlint:all,-module', '-Werror',
        '-cp', base, '-d', owned_fixtures, HERE / 'SpringCaseInsensitiveMapMaskProjectionProof.java'])
    provider_cp = os.pathsep.join(map(str, [provider_fixtures, base, jar]))
    owned_cp = os.pathsep.join(map(str, [owned_fixtures, base, owned]))
    projection_provider_cp = os.pathsep.join(map(str, [owned_fixtures, base, jar]))
    absent = '-Djava.library.path=' + str(output / 'absent-native')
    run('provider-protocol', [java, *runtime, absent, '-cp', provider_cp, 'ProviderBaselineLauncher', PROVIDER], 'provider-protocol.jsonl')
    run('owned-protocol', [java, *runtime, absent, '-cp', owned_cp, 'OwnedBaselineLauncher', OWNER], 'owned-protocol.jsonl')
    surface_cp = os.pathsep.join(map(str, [owned_fixtures, owned, jar]))
    run('full-surface', [java, *runtime, '-cp', surface_cp, 'SpringCaseInsensitiveMapFullSurfaceProof', PROVIDER, OWNER], 'surface.json')
    constructor_vm = ['-Xms64m', '-Xmx64m', *runtime[1:]]
    for label, owner, cp in [('provider', PROVIDER, provider_cp), ('owned', OWNER, owned_cp)]:
        run(label + '-constructors', [java, *constructor_vm, absent, '-cp', cp,
            'SpringEmptyLargeConstructorProbe', owner], label + '-constructors.jsonl')
        for fixture, suffix_name in [('SpringEntryGetterProtocol', 'getter'), ('SpringMapEqualityProtocol', 'equality')]:
            run(label + '-' + suffix_name, [java, *runtime, absent, '-cp', cp, fixture, owner], label + '-' + suffix_name + '.tsv')
    run('provider-projection', [java, *runtime, absent, '-cp', projection_provider_cp,
        'SpringCaseInsensitiveMapMaskProjectionProof', PROVIDER, 'java'], 'provider-projection.json')
    run('owned-projection-java', [java, *runtime, absent, '-cp', owned_cp,
        'SpringCaseInsensitiveMapMaskProjectionProof', OWNER, 'java'], 'owned-projection-java.json')
    if args.native_library_dir:
        native = args.native_library_dir.resolve()
        if not native.is_dir():
            raise SystemExit('explicit native library directory absent')
        run('owned-projection-native', [java, *runtime, '-Xcheck:jni', '-Djava.library.path=' + str(native),
            '-cp', owned_cp, 'SpringCaseInsensitiveMapMaskProjectionProof', OWNER, 'native'], 'owned-projection-native.json')
    run('compare-actual-output', [sys.executable, '-I', '-S', '-B', HERE / 'compare.py', output])
    try:
        sys.argv = ['verify.py', '--root', str(root), '--jar', str(jar)]
        load('spring_packet_custody_after', HERE / 'verify.py').main()
    finally:
        sys.argv = saved
    budget()
    print(json.dumps({'status': 'PASS', 'commands': len(commands), 'output': str(output),
        'nativeProjectionExecuted': bool(args.native_library_dir), 'scope': 'Actual source-bound fresh execution; no hosted status, map-native store/ABI, benchmark or full-five completion inferred.'}))


if __name__ == '__main__':
    main()
