#!/usr/bin/env python3
"""Current migration-recipes, A3 and Python consumers after twenty-four actual outputs."""
import argparse
import ast
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import py_compile
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

STAGES = ('lint', 'compile', 'test', 'runtime')
JAVA_MODULE = 'm3/tooling/migration-recipes'
A3_MODULE = 'm3/tooling/a3'
JAVA_MODULES = (JAVA_MODULE, A3_MODULE)
BACKPORTS = 'm3/backports'
HOST = 'm3/migration/intake-20261005/reconcile-8131'
OUTPUT_ROOTS = tuple(name + '/target' for name in (*JAVA_MODULES, BACKPORTS, HOST))
REGEX_CRATE = JAVA_MODULE + '/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab'
TARGETS = {
    JAVA_MODULE + '/src/test/java/com/m3/rewrite/M3A3RegexMemoryLabDeliveryRecipeTest.java',
    JAVA_MODULE + '/src/test/java/com/m3/rewrite/M3A3RegexMemoryWorkflowRecipeTest.java',
    BACKPORTS + '/compatibility_policy.py', BACKPORTS + '/test_compatibility_policy.py',
    '.github/workflows/m3-foundation.yml',
    A3_MODULE + '/src/main/java/com/m3/a3/A3RegexMatrix.java',
    A3_MODULE + '/src/main/java/com/m3/a3/A3MemoryCompiler.java',
    REGEX_CRATE + '/A3RegexMatrix.java.after',
    REGEX_CRATE + '/A3MemoryCompiler.java.after',
    REGEX_CRATE + '/manifest.tsv',
    A3_MODULE + '/src/main/java/com/m3/a3/A3Alg.java',
    A3_MODULE + '/src/test/java/com/m3/a3/A3PlanTest.java',
    JAVA_MODULE + '/src/main/resources/com/m3/rewrite/a3-algorithm-catalogue/after/A3Alg.java',
    JAVA_MODULE + '/src/main/resources/com/m3/rewrite/a3-algorithm-catalogue/after/A3PlanTest.java',
    JAVA_MODULE + '/src/main/java/com/m3/rewrite/a3/M3A3AlgorithmCatalogueManifest.java',
    JAVA_MODULE + '/pom.xml',
    A3_MODULE + '/src/main/java/com/m3/a3/A3Cases.java',
    JAVA_MODULE + '/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-mastery-lab/A3Cases.java.after',
    JAVA_MODULE + '/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-mastery-lab/manifest.tsv',
}
JCC_CRATE = JAVA_MODULE + '/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-handoff-20261005'
ADDED_TARGETS = {
    'm3/migration/test/test_jcc_handoff_current_context.py',
    JCC_CRATE + '/plan-intake-752-ci.json',
    JCC_CRATE + '/pom-before-752.xml',
}
TARGETS |= ADDED_TARGETS | {
    'm3/migration/test/test_jcc_handoff_packet.py',
    'm3/migration/test/test_jcc_handoff_context_profiles.py',
}
SOURCE = {'commit': '9963cc08ff13922b92a0e3db7c30f56fddacba7d',
          'tree': '6c055c584ebb5fa177420d1668db73bcb7e7fef4'}
RECEIVING = {'commit': '752191c9291f6467110fb8a7badfbdc4c2d41af2',
             'tree': '44feeb32f77a3442606ef3b63b923fd43732b20d'}
PUBLICATION_HEAD = '5c1ee9490e76943dfdb3ca27fade0d093d017226'
STAGE_SHA = '349234ce404067947b4bb9ade2d54897b19915524262358a45236b222b823ddd'
RUNTIME_MARKER = 'M3_CURRENT_752_RUNTIME_PASS'


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def digest(content):
    return hashlib.sha256(content).hexdigest()


def row(path, name=None, git=False):
    content = path.read_bytes()
    result = {'path': str(path) if name is None else name,
              'sha256': digest(content), 'bytes': len(content)}
    if git:
        result['gitBlob'] = hashlib.sha1(b'blob ' + str(len(content)).encode() + b'\0' + content).hexdigest()
    return result


def inventory(root, git=False, links=False):
    require(root.is_dir() and not root.is_symlink(), 'regular inventory root required: ' + str(root))
    result = []
    for path in sorted(root.rglob('*')):
        name = path.relative_to(root).as_posix()
        if path.is_symlink():
            require(links, 'source symlink refused: ' + name)
            result.append({'path': name, 'symlink': os.readlink(path)})
        elif path.is_file():
            result.append(row(path, name, git))
        else:
            require(path.is_dir(), 'special inventory entry refused: ' + name)
    return sorted(result, key=lambda entry: entry['path'])


def relative(value):
    require(isinstance(value, str) and value and '\\' not in value
            and not any(ord(c) < 32 for c in value), 'canonical relative path required')
    path = PurePosixPath(value)
    require(not path.is_absolute() and str(path) == value and '..' not in path.parts,
            'unsafe relative path: ' + value)
    return value


def load(path):
    return json.loads(path.read_text(encoding='utf-8'))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8') as stream:
        stream.write(json.dumps(value, indent=2, sort_keys=True) + '\n')


def copy(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    require(not destination.exists(), 'fresh copied file required')
    shutil.copyfile(source, destination)


def tool_roots(manifest):
    tools = manifest['tools']
    return {'java': Path(tools['javaHome']), 'maven': Path(tools['mavenHome']),
            'cache': Path(tools['cache']), 'python': Path(tools['pythonEnv'])}


def check_manifest(manifest):
    required = {'schema', 'source', 'receiving', 'publicationHead', 'files', 'producerOutputs',
                'externalInputs', 'tools', 'toolSets', 'runtime'}
    require(required <= set(manifest) <= required | {'task', 'provenance'}, 'manifest keys')
    require(manifest['schema'] == 'm3.current-consumer-proof/1' and manifest['source'] == SOURCE
            and manifest['receiving'] == RECEIVING, 'exact current source/receiving identities')
    head = manifest['publicationHead']
    require(set(head) == {'commit', 'tree'} and head['commit'] == PUBLICATION_HEAD
            and re.fullmatch('[0-9a-f]{40}', head['tree']), 'publication head identity')
    files = manifest['files']
    require(files and [r['path'] for r in files] == sorted({r['path'] for r in files}), 'complete sorted source set')
    indexed = {r['path']: r for r in files}
    for item in files:
        relative(item['path'])
        require(set(item) == {'path', 'sha256', 'bytes', 'gitBlob'} and type(item['bytes']) is int
                and item['bytes'] >= 0 and re.fullmatch('[0-9a-f]{64}', item['sha256'])
                and re.fullmatch('[0-9a-f]{40}', item['gitBlob']), 'source row shape')
        require(not any(item['path'] == p or item['path'].startswith(p + '/') for p in OUTPUT_ROOTS),
                'source context must not contain prior consumer build outputs')
    outputs = manifest['producerOutputs']
    require(len(outputs) == 24 and {r['path'] for r in outputs} == TARGETS, 'twenty-four exact producer targets')
    require(manifest['provenance']['addedTargetPaths'] == sorted(ADDED_TARGETS), 'three exact ABSENT-before additions')
    require(manifest['provenance']['outputOperations'] == {path: 'ADD' if path in ADDED_TARGETS else 'REPLACE' for path in TARGETS}, 'closed target operation declarations')
    external = manifest['externalInputs']
    require(external and len({r['path'] for r in external}) == len(external), 'distinct external provenance inputs')
    external_by_path = {r['path']: r for r in external}
    for item in external:
        require(set(item) == {'path', 'sha256', 'bytes'} and Path(item['path']).is_absolute(), 'external row shape')
    for item in outputs:
        require(set(item) == {'path', 'sha256', 'bytes', 'gitBlob', 'artifact'}, 'producer output row shape')
        require({k: item[k] for k in ('path', 'sha256', 'bytes', 'gitBlob')} == indexed[item['path']],
                'producer output differs from effective source')
        require(external_by_path.get(item['artifact']) ==
                {'path': item['artifact'], 'sha256': item['sha256'], 'bytes': item['bytes']},
                'each actual producer artifact must be an external sealed input')
    tools = manifest['tools']
    require(set(tools) == {'javaHome', 'mavenHome', 'cache', 'pythonEnv', 'python', 'git'}
            and all(Path(value).is_absolute() for value in tools.values()), 'exact absolute tool paths')
    require(set(manifest['toolSets']) == {'java', 'maven', 'cache', 'python'}, 'complete tool-set roles')
    runtime = manifest['runtime']
    require(set(runtime) == {'script', 'sha256', 'bytes'} and Path(runtime['script']).is_absolute(), 'runtime owner binding')


def prepare(args):
    manifest = load(args.manifest)
    check_manifest(manifest)
    require(inventory(args.context, git=True) == manifest['files'], 'effective context differs from manifest')
    for item in manifest['externalInputs']:
        require(row(Path(item['path'])) == item, 'external provenance drift')
    for role, root in tool_roots(manifest).items():
        require(inventory(root, links=True) == manifest['toolSets'][role], 'tool-set drift: ' + role)
    runtime = manifest['runtime']
    require(row(Path(runtime['script'])) == {'path': runtime['script'], 'sha256': runtime['sha256'], 'bytes': runtime['bytes']},
            'runtime script drift')
    require(digest(args.stage_helper.read_bytes()) == STAGE_SHA, 'retained stage helper identity')
    proof = args.proof.absolute()
    require(not os.path.lexists(proof) and not any(p.is_symlink() for p in proof.parents), 'fresh canonical proof directory')
    proof.mkdir(parents=True)
    shutil.copytree(args.context, proof / 'source')
    shutil.copytree(proof / 'source', proof / 'work')
    copy(args.manifest, proof / 'authority/manifest.json')
    copy(Path(__file__).resolve(), proof / 'harness/proof.py')
    copy(args.stage_helper, proof / 'harness/run_stage.py')
    copy(Path(runtime['script']), proof / 'harness/runtime.py')
    (proof / 'harness/empty-settings.xml').write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"/>\n')
    (proof / 'tmp').mkdir()
    tools = manifest['tools']
    environment = {'JAVA_HOME': tools['javaHome'], 'MAVEN_SKIP_RC': 'true',
        'PATH': os.pathsep.join((tools['javaHome'] + '/bin', tools['mavenHome'] + '/bin',
                                str(Path(tools['git']).parent), '/usr/local/bin', '/usr/bin', '/bin')),
        'PYTHONDONTWRITEBYTECODE': '1', 'PYTHONNOUSERSITE': '1',
        'PYTHONPYCACHEPREFIX': str(proof / 'unused-python-cache'), 'TMPDIR': str(proof / 'tmp'),
        'M3_JCC_CANDIDATE_ROOT': str(proof / 'work'), 'GIT_CONFIG_NOSYSTEM': '1',
        'GIT_CONFIG_GLOBAL': '/dev/null', 'LANG': 'C.UTF-8', 'LC_ALL': 'C.UTF-8', 'TZ': 'UTC'}
    config = {'proof': str(proof), 'source': str(proof / 'source'), 'work': str(proof / 'work'),
              'manifest': str(proof / 'authority/manifest.json'), 'java': tools['javaHome'],
              'maven': tools['mavenHome'], 'm2': tools['cache'], 'pythonEnv': tools['pythonEnv'],
              'python': tools['python'], 'git': tools['git'], 'stagePythonCachePrefix': environment['PYTHONPYCACHEPREFIX'],
              'environment': environment}
    write(proof / 'config.json', config)
    commands = {stage: [config['python'], str(proof / 'harness/proof.py'), stage, '--proof', str(proof)] for stage in STAGES}
    write(proof / 'commands.json', commands)
    write(proof / 'owner-commands.json', owner_commands(proof, config))
    sets = {name: inventory(proof / name) for name in ('source', 'authority', 'harness')}
    write(proof / 'sets.json', sets)
    write(proof / 'PATCH.json', {'state': 'SEALED_BEFORE_LINT', 'source': SOURCE, 'receiving': RECEIVING,
          'publicationHead': manifest['publicationHead'], 'actualProducerTargets': 24,
          'manifestSHA256': digest((proof / 'authority/manifest.json').read_bytes()),
          'ownerCommandsSHA256': digest((proof / 'owner-commands.json').read_bytes()), 'gatesRun': False})
    inputs = [row(proof / name / item['path']) for name, entries in sets.items() for item in entries]
    inputs += [row(proof / name) for name in ('config.json', 'commands.json', 'owner-commands.json', 'sets.json', 'PATCH.json')]
    inputs += manifest['externalInputs'] + [row(Path(config['git']))]
    for role, root in tool_roots(manifest).items():
        inputs += [row(root / item['path']) for item in manifest['toolSets'][role] if 'symlink' not in item]
    unique = {}
    for item in inputs:
        require(item['path'] not in unique or unique[item['path']] == item, 'conflicting input identity')
        unique[item['path']] = item
    write(proof / 'inputs.json', [unique[path] for path in sorted(unique)])
    print(json.dumps({'state': 'PREPARED_NOT_EXECUTED', 'sourceFiles': len(manifest['files']),
                      'inputsSHA256': digest((proof / 'inputs.json').read_bytes())}))


def owner_commands(proof, config):
    work = Path(config['work'])
    maven = [config['maven'] + '/bin/mvn', '-o', '-B', '-ntp', '-s', str(proof / 'harness/empty-settings.xml'),
             '-gs', str(proof / 'harness/empty-settings.xml'), '-Dmaven.repo.local=' + config['m2']]
    return {
        'java-version': [config['java'] + '/bin/java', '-version'],
        'javac-version': [config['java'] + '/bin/javac', '-version'],
        'maven-version': [config['maven'] + '/bin/mvn', '-version'],
        'java-test-compile': maven + ['-f', str(work / 'm3/pom.xml'), '-pl', 'tooling/a3', '-am', 'test-compile'],
        'java-clean-verify': maven + ['-f', str(work / 'm3/pom.xml'), '-pl', 'tooling/a3', '-am', 'clean', 'verify'],
        'backports-clean-verify': maven + ['-Dpython.executable=' + config['python'], '-f', str(work / BACKPORTS / 'pom.xml'), 'clean', 'verify'],
        'host-test': maven + ['-Dm3.python=' + config['python'], '-f', str(work / HOST / 'pom.xml'), 'test'],
        'runtime-owner': [config['python'], str(proof / 'harness/runtime.py'), '--proof', str(proof)],
    }


def check_work(proof, manifest):
    actual = inventory(proof / 'work', git=True)
    inputs = [r for r in actual if not any(r['path'].startswith(p + '/') for p in OUTPUT_ROOTS)]
    require(inputs == manifest['files'], 'consumer modified source bytes or membership outside declared target directories')


def verify_inputs(proof):
    config, manifest = load(proof / 'config.json'), load(proof / 'authority/manifest.json')
    check_manifest(manifest)
    require(str(Path(sys.executable).absolute()) == config['python'] and str(proof) == config['proof'], 'exact interpreter/proof path')
    for item in load(proof / 'inputs.json'):
        require(row(Path(item['path'])) == item, 'sealed input drift: ' + item['path'])
    for name, expected in load(proof / 'sets.json').items():
        require(inventory(proof / name) == expected, 'immutable input set drift: ' + name)
    for role, root in tool_roots(manifest).items():
        require(inventory(root, links=True) == manifest['toolSets'][role], 'tool/cache membership drift: ' + role)
    require(not os.path.lexists(config['stagePythonCachePrefix']), 'Python import cache was written')
    require(load(proof / 'owner-commands.json') == owner_commands(proof, config), 'owner command plan differs')
    check_work(proof, manifest)
    return config, manifest


def python_sources(work):
    return sorted(path for path in (work / 'm3').rglob('*.py')
                  if not any(path.relative_to(work).as_posix().startswith(p + '/') for p in OUTPUT_ROOTS))


def method_names(paths):
    names = []
    for path in paths:
        parsed = ast.parse(path.read_bytes(), filename=str(path))
        for cls in parsed.body:
            if isinstance(cls, ast.ClassDef):
                names.extend(path.stem + '.' + cls.name + '.' + node.name for node in cls.body
                             if isinstance(node, ast.FunctionDef) and node.name.startswith('test_'))
    require(len(names) == len(set(names)), 'duplicate declared Python test identity')
    return sorted(names)


def lint(proof, config, manifest):
    work = proof / 'work'
    paths = python_sources(work)
    for path in paths + [proof / 'harness/proof.py', proof / 'harness/runtime.py']:
        ast.parse(path.read_bytes(), filename=str(path))
    for module in ('m3', *JAVA_MODULES, BACKPORTS, HOST):
        ET.parse(work / module / 'pom.xml')
    backports = method_names(sorted((work / BACKPORTS).glob('test_*.py')))
    migration = method_names(sorted((work / 'm3/migration/test').glob('test_*.py')))
    union = method_names(sorted((work / HOST / 'test').glob('test_*.py')))
    require(len(migration) == 109 and len(union) == 6, 'retained host discovery owner changed')
    write(proof / 'lint-report.json', {'pythonSources': [p.relative_to(work).as_posix() for p in paths],
          'backportsMethods': backports, 'hostMethods': sorted(migration + union), 'hostCounts': [109, 6],
          'javaTestSourceFiles': {module: [p.relative_to(work).as_posix()
                                         for p in sorted((work / module / 'src/test/java').rglob('*.java'))]
                                  for module in JAVA_MODULES}})


def run(proof, config, name):
    command = load(proof / 'owner-commands.json')[name]
    directory = proof / 'commands'
    directory.mkdir(exist_ok=True)
    stdout, stderr = directory / (name + '.stdout.log'), directory / (name + '.stderr.log')
    started = datetime.now(timezone.utc).isoformat()
    with stdout.open('xb') as out, stderr.open('xb') as err:
        result = subprocess.run(command, cwd=config['work'], env=config['environment'], stdout=out, stderr=err, check=False)
    write(directory / (name + '.json'), {'command': command, 'cwd': config['work'], 'exitCode': result.returncode,
          'startedAt': started, 'endedAt': datetime.now(timezone.utc).isoformat(),
          'stdoutSHA256': digest(stdout.read_bytes()), 'stderrSHA256': digest(stderr.read_bytes())})
    require(result.returncode == 0, 'owner command failed: ' + name)
    return stdout.read_bytes(), stderr.read_bytes()


def compile_stage(proof, config, manifest):
    work = proof / 'work'
    versions = {}
    for name in ('java-version', 'javac-version', 'maven-version'):
        out, err = run(proof, config, name)
        versions[name] = (out + err).decode('utf-8')
    require('version "21"' in versions['java-version'] and '21+35' in versions['java-version']
            and 'javac 21' in versions['javac-version'] and 'Apache Maven 3.9.9' in versions['maven-version']
            and config['java'] in versions['maven-version'], 'Maven must select exact acquired Temurin21+35')
    output = proof / 'compiled-python'
    output.mkdir()
    for index, path in enumerate(python_sources(work)):
        py_compile.compile(str(path), cfile=str(output / (str(index) + '.pyc')), doraise=True)
    run(proof, config, 'java-test-compile')
    for module in JAVA_MODULES:
        for name in ('classes', 'test-classes'):
            source = work / module / 'target' / name
            require(source.is_dir() and list(source.rglob('*.class')), 'actual Java compilation output absent: ' + module)
            shutil.copytree(source, proof / 'compiled-java' / Path(module).name / name)
    write(proof / 'compile-report.json', {'versions': versions, 'python': inventory(output),
          'java': inventory(proof / 'compiled-java'),
          'note': 'Archived before clean verify deletes/recompiles the module target.'})


def observed_tests(out, err):
    text = (out + b'\n' + err).decode('utf-8')
    names = []
    for name, owner, state in re.findall(r'^(test_\w+) \(([^)]+)\) \.\.\. (\S+)\s*$', text, re.M):
        require(state == 'ok', 'Python test not passed: ' + owner)
        names.append(owner if owner.endswith('.' + name) else owner + '.' + name)
    summaries = [int(value) for value in re.findall(r'^Ran ([0-9]+) tests? in ', text, re.M)]
    return sorted(names), summaries


def test_stage(proof, config, manifest):
    run(proof, config, 'java-clean-verify')
    suites, names = [], set()
    for module in JAVA_MODULES:
        reports = sorted((proof / 'work' / module / 'target/surefire-reports').glob('TEST-*.xml'))
        require(reports, 'full Java consumer produced no Surefire XML: ' + module)
        for path in reports:
            suite = ET.parse(path).getroot()
            identity = (module, suite.attrib['name'])
            require(suite.tag == 'testsuite' and identity not in names, 'unique Surefire suite required')
            names.add(identity)
            counts = {key: int(suite.attrib.get(key, '0')) for key in ('tests', 'failures', 'errors', 'skipped')}
            cases = suite.findall('testcase')
            require(counts['tests'] == len(cases) and counts['tests'] > 0
                    and counts['failures'] == counts['errors'] == counts['skipped'] == 0
                    and not any(case.find(tag) is not None for case in cases for tag in ('failure', 'error', 'skipped')),
                    'Java suite had failures/errors/skips or inconsistent test inventory')
            suites.append({'module': module, 'name': suite.attrib['name'], **counts, 'report': row(path),
                           'methods': [{'class': c.attrib.get('classname'), 'name': c.attrib['name']} for c in cases]})
    lint_report = load(proof / 'lint-report.json')
    backports, backport_counts = observed_tests(*run(proof, config, 'backports-clean-verify'))
    require(backports == lint_report['backportsMethods'] and backport_counts == [len(backports)], 'full backports discovery mismatch')
    host, host_counts = observed_tests(*run(proof, config, 'host-test'))
    require(host == lint_report['hostMethods'] and host_counts == [109, 6], 'full retained host discovery mismatch')
    for path in ('target/jacoco.exec', 'target/site/jacoco/jacoco.xml', 'target/site/jacoco/index.html'):
        require((proof / 'work' / JAVA_MODULE / path).is_file(), 'coverage output absent: ' + path)
    write(proof / 'test-report.json', {'javaTests': sum(s['tests'] for s in suites), 'javaSuites': suites,
          'javaCountSource': 'complete actual Surefire XML; no invented expected count',
          'javaModules': {module: {'suites': sum(s['module'] == module for s in suites),
                                   'tests': sum(s['tests'] for s in suites if s['module'] == module)}
                          for module in JAVA_MODULES},
          'backportsTests': len(backports), 'backportsMethods': backports, 'hostTests': len(host),
          'hostMethods': host, 'hostCounts': host_counts, 'failures': 0, 'errors': 0, 'skipped': 0,
          'fullCleanVerify': [*JAVA_MODULES, BACKPORTS], 'originalCoverageThresholdsRetained': True})


def runtime_stage(proof, config, manifest):
    out, _ = run(proof, config, 'runtime-owner')
    require(out.decode('utf-8').splitlines().count(RUNTIME_MARKER) == 1, 'actual delegated runtime completion marker absent')
    report = load(proof / 'runtime-report.json')
    require(report['productWrites'] == 0 and report['runtimeAccepted'] is False,
            'runtime owner changed product acceptance boundary')


def verify(proof):
    config, _ = verify_inputs(proof)
    require(not (proof / 'chain').exists() and not (proof / 'RESULT.json').exists(), 'fresh ordered chain required')
    input_sha = digest((proof / 'inputs.json').read_bytes())
    frozen, frozen_reports, failure = {}, {}, None
    try:
        for stage in STAGES:
            verify_inputs(proof)
            require(digest((proof / 'inputs.json').read_bytes()) == input_sha, 'input manifest drift')
            for name, entries in frozen.items():
                require(inventory(proof / name) == entries, 'prior evidence set drift: ' + name)
            for name, expected in frozen_reports.items():
                require(row(proof / name) == expected, 'prior report drift: ' + name)
            command = [config['python'], str(proof / 'harness/run_stage.py'), '--chain', str(proof / 'chain'),
                       '--stage', stage, '--cwd', str(proof), '--source-manifest', str(proof / 'inputs.json'), '--']
            command += load(proof / 'commands.json')[stage]
            completed = subprocess.run(command, env=config['environment'], check=False)
            verify_inputs(proof)
            require(digest((proof / 'inputs.json').read_bytes()) == input_sha, 'input manifest drift after stage')
            for name, entries in frozen.items():
                if name != 'chain' and name != 'commands':
                    require(inventory(proof / name) == entries, 'prior evidence set drift after stage: ' + name)
                else:
                    indexed = {r['path']: r for r in inventory(proof / name)}
                    require(all(indexed.get(r['path']) == r for r in entries), 'prior command evidence drift')
            for name, expected in frozen_reports.items():
                require(row(proof / name) == expected, 'prior report drift after stage: ' + name)
            require(completed.returncode == 0, 'stage failed: ' + stage)
            frozen = {name: inventory(proof / name) for name in ('chain', 'commands', 'compiled-python', 'compiled-java')
                      if (proof / name).is_dir()}
            if stage in ('test', 'runtime'):
                frozen.update({'work/' + name: inventory(proof / 'work' / name) for name in OUTPUT_ROOTS
                               if (proof / 'work' / name).is_dir()})
            frozen_reports = {name: row(proof / name) for name in
                              ('lint-report.json', 'compile-report.json', 'test-report.json', 'runtime-report.json')
                              if (proof / name).is_file()}
        final = {'state': 'PASS_BOUNDED_CURRENT_CONSUMERS', 'failed': None}
    except Exception as error:
        failure = str(error)
        final = {'state': 'STOPPED', 'failed': failure}
    evidence = {path.relative_to(proof).as_posix(): digest(path.read_bytes()) for root in ('chain', 'commands', 'runtime-commands')
                for path in sorted((proof / root).rglob('*')) if path.is_file()}
    for name in ('lint-report.json', 'compile-report.json', 'test-report.json', 'runtime-report.json'):
        if (proof / name).is_file():
            evidence[name] = digest((proof / name).read_bytes())
    final.update(inputsSHA256=input_sha, source=SOURCE, receiving=RECEIVING, evidence=evidence,
                 runtimeAccepted=False, canonicalProductionApplied=False, wholeSourceTreeCovered=False,
                 dependencyClosureComplete=False, originalToolchainCustodyRevalidated=False,
                 hostSystemLibrariesSealed=False, productWrites=0)
    write(proof / 'RESULT.json', final)
    print(json.dumps({'state': final['state'], 'failed': failure}))
    return 1 if failure else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=('prepare', 'verify') + STAGES)
    parser.add_argument('--proof', required=True, type=Path)
    parser.add_argument('--context', type=Path)
    parser.add_argument('--manifest', type=Path)
    parser.add_argument('--stage-helper', type=Path)
    args = parser.parse_args()
    if args.mode == 'prepare':
        require(args.context and args.manifest and args.stage_helper, 'complete source, manifest and retained stage helper required')
        prepare(args)
        return 0
    proof = args.proof.resolve()
    if args.mode == 'verify':
        return verify(proof)
    config, manifest = verify_inputs(proof)
    {'lint': lint, 'compile': compile_stage, 'test': test_stage, 'runtime': runtime_stage}[args.mode](proof, config, manifest)
    verify_inputs(proof)
    print('M3_CURRENT_752_' + args.mode.upper() + '_PASS')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
