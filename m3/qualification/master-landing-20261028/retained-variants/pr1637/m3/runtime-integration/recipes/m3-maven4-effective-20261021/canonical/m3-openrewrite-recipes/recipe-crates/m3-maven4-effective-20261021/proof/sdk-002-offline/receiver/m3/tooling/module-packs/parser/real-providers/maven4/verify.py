#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
"""Qualify the existing M3AST owner against the exact source-built Maven 4 fork.

The optional online pass provisions build dependencies. Acceptance always uses
an offline source-reactor pass with the real 41-test ModelBuilder suite, followed
by actual-provider baseline/candidate probes. No Maven distribution replacement,
plugin execution, full-fork, full-JDK or current JDT-fork acceptance is claimed.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

FORK_COMMIT = '2993571eedbe96e06a737eaa8ae47cf112b255de'
BASE_COMMIT = '2779aba3f98f31c324be43f731be7914e1a05935'
PACK = Path('m3/tooling/module-packs/parser')
JAVA = PACK / 'src/main/java/jdk/m3/parser'
TEST = PACK / 'real-providers/src/test/java/jdk/m3/parser/tests/M3Maven4ProviderContract.java'
PREIMAGES = {
    'M3File.java': '5851f9fc09cb079818fe2224249129688e7637bd562f77cb016cdc8c32c868ad',
    'M3ASTIndex.java': '4563fcc9ea0a1572f9e0d8040f4980adf40feec4d85b3d0e30d14906b4672fc2',
    'M3ASTIngress.java': 'c84e5c826d306f14be105c28a459a8a056023e44b828791f984b9a627c4166ec',
    'M3ASTAuthoritativeModels.java': '8fe4828ed449335d3ec4975b16f708ff26fa6739dad82523379cd2f7bfb6bef3',
}
POSTIMAGE = '2814f6f292978b421cec2914cc48a96384cc3e926377b138559e287a005854c3'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def git(fork, *arguments):
    return subprocess.check_output(['git', '-C', str(fork), *arguments], text=True).strip()


def public_methods(text):
    lines = text.splitlines()
    return {line.strip(): lines[i + 1].strip()
            for i, line in enumerate(lines) if line.strip().startswith('public static ')}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo-root', type=Path, required=True)
    parser.add_argument('--baseline-root', type=Path, required=True)
    parser.add_argument('--fork-root', type=Path, required=True)
    parser.add_argument('--jdk-home', type=Path, required=True)
    parser.add_argument('--maven', type=Path, required=True)
    parser.add_argument('--cache', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--settings', type=Path)
    parser.add_argument('--online-bootstrap', action='store_true')
    args = parser.parse_args()
    repo, baseline, fork = (p.resolve(strict=True) for p in (args.repo_root, args.baseline_root, args.fork_root))
    jdk = args.jdk_home.resolve(strict=True)
    output = args.out.absolute()
    if output.exists():
        raise FileExistsError('fresh output required: ' + str(output))
    if git(fork, 'rev-parse', 'HEAD') != FORK_COMMIT:
        raise ValueError('Maven fork commit differs from the qualified API owner')
    if git(fork, 'status', '--porcelain', '--untracked-files=all'):
        raise ValueError('Maven fork must have unchanged tracked sources and no untracked input files')
    if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', (jdk / 'release').read_text(), re.MULTILINE):
        raise ValueError('Java 21 development toolchain required')
    for name, expected in PREIMAGES.items():
        if sha(baseline / JAVA / name) != expected:
            raise ValueError('baseline source differs: ' + name)
        wanted = POSTIMAGE if name == 'M3ASTAuthoritativeModels.java' else expected
        if sha(repo / JAVA / name) != wanted:
            raise ValueError('current source differs: ' + name)
    tracked = git(fork, 'ls-files', '-z').split('\0')
    fork_inputs = {name: sha(fork / name) for name in tracked if name}
    m3_files = [repo / JAVA / name for name in PREIMAGES] + [repo / TEST,
            repo / PACK / 'real-providers/maven4/pom.xml', Path(__file__).resolve()]
    m3_inputs = {str(path): sha(path) for path in m3_files}
    output.mkdir(parents=True)
    save(output / 'INPUTS.json', {'fork_commit': FORK_COMMIT, 'baseline_commit': BASE_COMMIT,
            'fork_inputs': fork_inputs, 'm3_inputs': m3_inputs, 'jdk_release_sha256': sha(jdk / 'release')})
    env = dict(os.environ, JAVA_HOME=str(jdk))
    env['PATH'] = str(jdk / 'bin') + os.pathsep + env.get('PATH', '')
    commands = []

    def run(label, arguments, cwd=repo):
        command = list(map(str, arguments))
        log = output / (label + '.log')
        with log.open('w', encoding='utf-8') as stream:
            stream.write(json.dumps(command) + '\n')
            stream.flush()
            result = subprocess.run(command, cwd=cwd, env=env, stdout=stream,
                    stderr=subprocess.STDOUT, timeout=600)
            stream.write('\nEXIT ' + str(result.returncode) + '\n')
        commands.append({'label': label, 'command': command, 'cwd': str(cwd),
                'exit': result.returncode, 'log': log.name, 'sha256': sha(log)})
        save(output / 'COMMANDS.json', commands)
        if result.returncode:
            raise RuntimeError('qualification failed: ' + str(log))
        return log.read_text(encoding='utf-8')

    mvn = [args.maven.resolve(strict=True), '-B', '-ntp', '-Dmaven.repo.local=' + str(args.cache.absolute())]
    if args.settings:
        mvn += ['-s', args.settings.resolve(strict=True)]
    selected = ['-pl', 'impl/maven-impl', '-am']
    if args.online_bootstrap:
        run('source-bootstrap', [*mvn, *selected, '-DskipTests', 'package'], fork)
    # Never allow a prior XML report to stand in for this source-reactor run.
    for reports in fork.glob('*/**/target/surefire-reports'):
        if reports.is_dir():
            shutil.rmtree(reports)
    run('source-offline', [*mvn, '-o', *selected, '-Dtest=DefaultModelBuilderTest',
            '-Dsurefire.failIfNoSpecifiedTests=false', 'package'], fork)
    report = fork / 'impl/maven-impl/target/surefire-reports/TEST-org.apache.maven.impl.model.DefaultModelBuilderTest.xml'
    suite = ET.parse(report).getroot()
    totals = {key: int(suite.get(key, '0')) for key in ('tests', 'failures', 'errors', 'skipped')}
    if totals != {'tests': 41, 'failures': 0, 'errors': 0, 'skipped': 0}:
        raise ValueError('actual pinned ModelBuilder suite did not pass exactly: ' + str(totals))
    shutil.copy2(report, output / report.name)
    properties = {p.get('name'): p.get('value') for p in suite.find('properties')}
    provider_paths = []
    for entry in properties['java.class.path'].split(os.pathsep):
        path = Path(entry)
        if path.name == 'test-classes' and path.parent.name == 'target':
            continue
        if path.is_relative_to(fork) and path.name == 'classes' and path.parent.name == 'target':
            jars = [p for p in path.parent.glob('*.jar')
                    if not p.name.endswith(('-tests.jar', '-sources.jar', '-javadoc.jar'))]
            if len(jars) != 1:
                raise ValueError('ambiguous source-built provider artifact: ' + str(path))
            path = jars[0]
        if not path.is_file():
            raise FileNotFoundError(path)
        if path not in provider_paths:
            provider_paths.append(path)
    source_jars = sum(path.is_relative_to(fork) for path in provider_paths)
    if source_jars != 16:
        raise ValueError('pinned Maven source-built artifact closure changed')
    pom = repo / PACK / 'real-providers/maven4/pom.xml'
    classpath_file = output / 'incumbent-classpath.txt'
    goal = 'org.apache.maven.plugins:maven-dependency-plugin:3.11.0:build-classpath'
    if args.online_bootstrap:
        run('incumbent-bootstrap', [*mvn, '-f', pom, goal, '-Dmdep.outputFile=' + str(classpath_file)])
        classpath_file.unlink()
    run('incumbent-offline', [*mvn, '-o', '-f', pom, goal, '-Dmdep.outputFile=' + str(classpath_file)])
    for entry in classpath_file.read_text().strip().split(os.pathsep):
        path = Path(entry)
        if not path.is_file():
            raise FileNotFoundError(path)
        if path not in provider_paths:
            provider_paths.append(path)
    provider_cp = os.pathsep.join(map(str, provider_paths))
    artifacts = [{'path': str(path), 'bytes': path.stat().st_size, 'sha256': sha(path),
            'origin': 'pinned Maven fork source build' if path.is_relative_to(fork) else 'cached external dependency'}
            for path in provider_paths]
    save(output / 'PROVIDER_ARTIFACTS.json', artifacts)
    (output / 'provider-classpath.txt').write_text(provider_cp + '\n')
    for mode, source in [('baseline', baseline), ('candidate', repo)]:
        classes = output / (mode + '-classes')
        classes.mkdir()
        run(mode + '-compile', [jdk / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror',
                '-proc:none', '-d', classes, *[source / JAVA / name for name in PREIMAGES]])
    fixture_classes = output / 'fixture-classes'
    fixture_classes.mkdir()
    run('fixture-compile', [jdk / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror',
            '-proc:none', '-cp', str(output / 'baseline-classes') + os.pathsep + provider_cp,
            '-d', fixture_classes, repo / TEST])
    results = {}
    shapes = {}
    methods = {}
    for mode in ('baseline', 'candidate'):
        home = output / (mode + '-home')
        home.mkdir()
        classes = output / (mode + '-classes')
        cp = os.pathsep.join((str(classes), str(fixture_classes), provider_cp))
        result = run(mode + '-provider', [jdk / 'bin/java', '-Xmx512m', '-XX:ActiveProcessorCount=2',
                '-Duser.home=' + str(home / 'user'), '-Dmaven.home=' + str(home / 'maven-home'),
                '-cp', cp, 'jdk.m3.parser.tests.M3Maven4ProviderContract', mode, home / 'fixture'])
        marker = 'M3_MAVEN4_BASELINE_GAP checks=13' if mode == 'baseline' else 'M3_MAVEN4_REAL_PROVIDER_PASS checks=102'
        if marker not in result.splitlines():
            raise ValueError('actual provider contract marker missing: ' + mode)
        results[mode] = marker
        shapes[mode] = [line for line in result.splitlines() if line.startswith('INCUMBENT_')]
        methods[mode] = public_methods(run(mode + '-api', [jdk / 'bin/javap', '-public', '-s',
                '-classpath', classes, 'jdk.m3.parser.M3ASTAuthoritativeModels']))
    if shapes['baseline'] != shapes['candidate'] or len(shapes['baseline']) != 2:
        raise ValueError('incumbent Maven 3 or JDT index shape changed')
    added = set(methods['candidate']) - set(methods['baseline'])
    removed = set(methods['baseline']) - set(methods['candidate'])
    expected = 'public static jdk.m3.parser.M3ASTIndex maven4Effective(jdk.m3.parser.M3File, java.lang.Object, jdk.m3.parser.M3ASTIngress$Limits);'
    if added != {expected} or removed:
        raise ValueError('unexpected public API change')
    if any(methods['baseline'][key] != methods['candidate'][key] for key in methods['baseline']):
        raise ValueError('incumbent descriptor changed')
    for name, expected_hash in fork_inputs.items():
        if sha(fork / name) != expected_hash:
            raise ValueError('fork source changed during qualification: ' + name)
    if git(fork, 'status', '--porcelain', '--untracked-files=all'):
        raise ValueError('fork source or input inventory changed during qualification')
    for path, expected_hash in m3_inputs.items():
        if sha(Path(path)) != expected_hash:
            raise ValueError('M3 input changed during qualification: ' + path)
    for row in artifacts:
        if sha(Path(row['path'])) != row['sha256']:
            raise ValueError('provider artifact changed during qualification')
    proof = {'status': 'PASS', 'repo_root': str(repo), 'fork_commit': FORK_COMMIT,
            'baseline_commit': BASE_COMMIT, 'source_builder_tests': totals,
            'source_built_fork_jars': source_jars, 'provider_artifact_count': len(artifacts),
            'results': results, 'incumbent_maven3_jdt_shapes_equal': True,
            'added_public_method': expected, 'source_postimage_sha256': POSTIMAGE,
            'input_sha256': sha(output / 'INPUTS.json'), 'commands_sha256': sha(output / 'COMMANDS.json'),
            'artifacts_sha256': sha(output / 'PROVIDER_ARTIFACTS.json'),
            'limits': ['Host Java 21 provider qualification; full parser/JMOD owner gate is separate.',
                'Maven4 source API/implementation closure and 41 model-builder tests, not full fork/distribution/plugin execution.',
                'Maven3.9.11 and JDT3.40.0 incumbent libraries are cached artifacts; current custom JDT fork is not source-qualified here.',
                'External dependencies and Maven build plugins are cached artifacts, not claimed source-built.',
                'No full custom JDK, HotSpot, cross-platform, Maven Resolver fork, m2e, Tycho or mvnd promotion.']}
    save(output / 'PROOF.json', proof)
    print(json.dumps({key: proof[key] for key in ('status', 'source_builder_tests', 'source_built_fork_jars', 'results')}, indent=2))


if __name__ == '__main__':
    main()
