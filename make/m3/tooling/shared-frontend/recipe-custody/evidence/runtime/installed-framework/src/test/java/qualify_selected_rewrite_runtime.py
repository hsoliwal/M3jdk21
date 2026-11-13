#!/usr/bin/env python3
"""Run unchanged SDK framework tests using only the selected image's runtime jars."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET


def pin(path):
    path = Path(path).resolve(strict=True)
    checksum = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            checksum.update(block)
    return {'path': str(path), 'bytes': path.stat().st_size, 'sha256': checksum.hexdigest()}


parser = argparse.ArgumentParser()
parser.add_argument('--image', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
parser.add_argument('--proof', type=Path, required=True)
parser.add_argument('--tool-pom', type=Path, required=True)
parser.add_argument('--maven-repository', type=Path, default=Path('/tmp/m3-maven-repository'))
args = parser.parse_args()
image, output, proof, tool_pom, repository = [value.resolve() for value in
    (args.image, args.output, args.proof, args.tool_pom, args.maven_repository)]
if output.exists():
    raise SystemExit('Fresh output required; prior qualification receipts are preserved.')
lock_path = proof / 'openrewrite-lock.json'
lock = json.loads(lock_path.read_text())
pack, = [pack for pack in lock['packs'] if pack['name'] == 'openrewrite']
original_receipt = json.loads((proof / 'QUALIFICATION_RECEIPT.json').read_text())
assert original_receipt['status'] == 'PASS'
assert original_receipt['runtime_junit_tests'] == 11
assert original_receipt['runtime_dependency_jars'] == 57
installed = image / 'lib/m3-tools/openrewrite'


def verify_assets():
    result = []
    for asset in pack['assets']:
        destination = image / asset['target']
        assert destination.resolve(strict=True).is_relative_to(image)
        current = pin(destination)
        assert current['sha256'] == asset['sha256'], asset['target']
        assert bool(destination.stat().st_mode & 0o111) == asset['executable'], asset['target']
        result.append(current)
    return result


assets = verify_assets()
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
exports = ET.parse(tool_pom).getroot().findtext('m:properties/m:m3.compiler.exports', namespaces=ns).split()
assert len(exports) == 12 and all(value.startswith('--add-exports=jdk.compiler/') for value in exports)
coordinates = [
    ('org.junit.platform', 'junit-platform-launcher', '1.10.2'),
    ('org.junit.platform', 'junit-platform-engine', '1.10.2'),
    ('org.junit.platform', 'junit-platform-commons', '1.10.2'),
    ('org.junit.jupiter', 'junit-jupiter-api', '5.10.2'),
    ('org.junit.jupiter', 'junit-jupiter-engine', '5.10.2'),
    ('org.junit.jupiter', 'junit-jupiter-params', '5.10.2'),
    ('org.opentest4j', 'opentest4j', '1.3.0'),
    ('org.apiguardian', 'apiguardian-api', '1.1.2'),
]
junit_jars = [repository.joinpath(*group.split('.'), artifact, version,
                                 artifact + '-' + version + '.jar')
              for group, artifact, version in coordinates]
test_classes = proof / 'runtime/test-classes'
test_class, = test_classes.rglob('M3RewriteRuntimeTest.class')
source = Path(__file__).with_name('SelectedFrameworkTests.java')
input_pins = [pin(path) for path in [source, Path(__file__), lock_path,
    proof / 'QUALIFICATION_RECEIPT.json', proof / 'RUNTIME_QUALIFICATION_RECEIPT.json',
    tool_pom, test_class, image / 'bin/java', image / 'bin/javac',
    image / 'lib/modules', image / 'release', *junit_jars]]
output.mkdir(parents=True)
classes = output / 'harness-classes'
classes.mkdir()
home = output / 'test-user-home'
home.mkdir()
environment = os.environ.copy()
for name in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH'):
    environment.pop(name, None)
environment['JAVA_HOME'] = str(image)
commands = []


def run(name, command):
    command = [str(value) for value in command]
    commands.append({'name': name, 'arguments': command})
    with (output / (name + '.log')).open('w') as stream:
        result = subprocess.run(command, env=environment, stdout=stream,
                                stderr=subprocess.STDOUT, check=False)
    if result.returncode:
        raise SystemExit(f'{name} failed with {result.returncode}; inspect its proof log')


run('harness-compile', [image / 'bin/javac', '-Xlint:all', '-Werror',
    '-cp', os.pathsep.join(map(str, junit_jars)), '-d', classes, source])
runtime_classpath = [classes, test_classes, installed / 'launch.jar', *junit_jars]
assert not any(str(path).startswith(str(repository)) for path in runtime_classpath[:3])
run('installed-framework-tests', [image / 'bin/java', *exports,
    '-Duser.home=' + str(home), '-cp', os.pathsep.join(map(str, runtime_classpath)),
    'SelectedFrameworkTests', installed])
text = (output / 'installed-framework-tests.log').read_text()
assert 'M3_SELECTED_RUNTIME_TESTS found=11 succeeded=11 failed=0 aborted=0 skipped=0' in text
assert text.count('INSTALLED_CLASS ') == 14
assert text.count('FRAMEWORK_TEST ') == 11
assert verify_assets() == assets
for expected in input_pins:
    assert pin(expected['path']) == expected, expected['path']
(output / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
receipt = {
    'status': 'PASS', 'dataset': 'm3jdk21-openrewrite-pack',
    'scope': 'Existing build_pack linked image with selected optional OpenRewrite runtime',
    'framework_tests': 11, 'failures': 0, 'skipped': 0,
    'production_framework_artifacts': 15, 'runtime_dependency_jars': 57,
    'runtime_source': 'Installed image launch.jar manifest; no Maven-repository framework jars on execution classpath',
    'external_test_support': 'Unmodified JUnit Platform 1.10.2 and Jupiter 5.10.2 test-only jars',
    'installed_class_origins_verified': 14,
    'installed_asset_count': len(assets), 'installed_assets': assets,
    'input_pins': input_pins,
    'files': [pin(path) for path in [output / 'commands.json', output / 'harness-compile.log',
              output / 'installed-framework-tests.log']],
}
receipt_path = output / 'QUALIFICATION_RECEIPT.json'
receipt_path.write_text(json.dumps(receipt, indent=2) + '\n')
print(json.dumps({'status': 'PASS', 'framework_tests': 11, 'receipt': str(receipt_path)}))
