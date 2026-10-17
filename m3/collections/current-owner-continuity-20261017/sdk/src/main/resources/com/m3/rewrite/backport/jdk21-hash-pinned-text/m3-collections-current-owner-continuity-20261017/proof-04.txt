#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Current-run custody adapters for the existing collection qualification scripts.

ADAPT donor hsoliwal/M3jdk21@e81a42da2158801a9358b5371e22563c24f4853f:
m3/collections/wrappers/verify.py, blob 5c813e8aa766ae9bd90a8c3a7e54412a8b5281c8.
compiled_inventory and staged source/compiled checks are extracted below; the
uncovered delta binds current source, exact named tests and a caller-owned run.
This module neither transforms product code nor grants JDK/runtime admission.
"""
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

CONTROL = Path('m3/collections/current-owner-continuity-20261017')
IMPLEMENTATIONS = ('counts.py', 'totals.py', 'bimap.py', 'wrappers.py',
                   'custody.py', 'donation.py', 'current-source-pins.json',
                   'donation-packet.json', 'test_custody.py')


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def reject_native_diagnostics(output):
    """Checked JNI can warn and still exit zero; diagnostics forbid acceptance."""
    if 'WARNING in native method' in output or 'runtime error:' in output:
        raise RuntimeError('JNI/sanitizer diagnostic in qualification output')


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate proof metadata key: ' + key)
        result[key] = value
    return result


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique_object)


def validate_identity(run_id, source_sha):
    if not re.fullmatch(r'[0-9a-f]{40}', source_sha):
        raise ValueError('expected source SHA must be 40 lower-case hex characters')
    if not re.fullmatch(r'[1-9][0-9]{0,19}-[1-9][0-9]{0,5}-[0-9a-f]{12}', run_id):
        raise ValueError('run ID must be <run>-<attempt>-<head12>, at most 40 characters')
    if run_id.rsplit('-', 1)[1] != source_sha[:12]:
        raise ValueError('run ID/source SHA mismatch')


def safe_path(root, relative):
    p = Path(relative)
    if p.is_absolute() or '..' in p.parts or '\\' in str(relative) or not p.parts:
        raise ValueError('unsafe proof path: ' + str(relative))
    current = root
    for part in p.parts:
        current = current / part
        if current.is_symlink():
            raise ValueError('symlinked proof path: ' + str(relative))
    return current


def inventory(root, directories):
    """Extracted donor compiled_inventory; extend to native/package artifacts."""
    result = {}
    for base in directories:
        base = Path(base)
        if not base.is_absolute():
            base = root / base
        if not base.exists():
            raise ValueError('missing compiled proof path: ' + str(base))
        safe_path(root, base.relative_to(root))
        paths = [base] if base.is_file() else sorted(base.rglob('*'))
        for path in paths:
            safe_path(root, path.relative_to(root))
            if path.is_file() and path.suffix in ('.class', '.h', '.so', '.jar'):
                result[path.relative_to(root).as_posix()] = digest(path)
    if not result:
        raise ValueError('empty compiled proof inventory')
    return dict(sorted(result.items()))


def assert_inventory(root, expected, roots):
    if not expected:
        raise ValueError('empty inherited compiled proof inventory')
    for relative, expected_sha in expected.items():
        p = safe_path(root, relative)
        if not p.is_file() or digest(p) != expected_sha:
            raise ValueError('compiled proof drift: ' + relative)
    actual = inventory(root, roots)
    if actual != expected:
        raise ValueError('compiled proof set drift: added=' + repr(sorted(set(actual) - set(expected))))


def junit_inventory(reports, expected):
    files = sorted(reports.glob('TEST-*.xml'))
    if not files:
        raise ValueError('missing actual JUnit XML')
    totals = {k: 0 for k in ('tests', 'failures', 'errors', 'skipped')}
    actual = []
    for path in files:
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.attrib.get(key, 0))
        for case in suite.iter('testcase'):
            name = case.attrib.get('name', '')
            match = re.fullmatch(r'([A-Za-z_$][A-Za-z0-9_$]*)(?:\(\))?', name)
            if match is None or not case.attrib.get('classname'):
                raise ValueError('unsupported or unnamed JUnit case: ' + repr(case.attrib))
            if any(case.find(tag) is not None for tag in ('skipped', 'failure', 'error')):
                raise ValueError('nonpassing actual JUnit case: ' + name)
            actual.append(case.attrib['classname'] + '#' + match.group(1))
    if not expected or len(set(expected)) != len(expected):
        raise ValueError('empty or duplicate expected method inventory')
    if len(set(actual)) != len(actual) or sorted(actual) != sorted(expected):
        raise ValueError('actual JUnit method set mismatch: missing=' +
                         repr(sorted(set(expected) - set(actual))) + ' unexpected=' +
                         repr(sorted(set(actual) - set(expected))))
    if totals != dict(tests=len(expected), failures=0, errors=0, skipped=0):
        raise ValueError('actual JUnit totals mismatch: ' + repr(totals))
    return totals


def add_arguments(parser):
    parser.add_argument('--run-id', required=True)
    parser.add_argument('--expected-source-sha', required=True)


def source_inventory(root, pins):
    sources = {}
    declared = pins['files']
    for relative, expected in declared.items():
        p = safe_path(root, relative)
        if not p.is_file() or digest(p) != expected['sha256']:
            raise ValueError('current source drift: ' + relative)
        data = p.read_bytes()
        git_blob = hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()
        if len(data) != expected['bytes'] or git_blob != expected['gitBlob']:
            raise ValueError('current source Git/blob custody drift: ' + relative)
        sources[relative] = expected['sha256']
    allowed = set(declared) | set(pins['receiptPaths'])
    for directory in pins['sourceRoots'] + pins['packetRoots']:
        base = safe_path(root, directory)
        expected_paths = {p for p in allowed if p.startswith(directory + '/')}
        actual_paths = set()
        for p in base.rglob('*'):
            relative = p.relative_to(root)
            safe_path(root, relative)
            if p.is_file():
                actual_paths.add(relative.as_posix())
        if actual_paths != expected_paths:
            raise ValueError('current source set drift: ' + directory +
                             ' missing=' + repr(sorted(expected_paths - actual_paths)) +
                             ' extra=' + repr(sorted(actual_paths - expected_paths)))
    for relative in pins['receiptPaths']:
        p = safe_path(root, relative)
        if not p.is_file():
            raise ValueError('missing actual recipe delivery receipt: ' + relative)
        sources[relative] = digest(p)
    return dict(sorted(sources.items()))


class Context:
    def __init__(self, root, run_id, source_sha, stage, deps, junit):
        validate_identity(run_id, source_sha)
        if stage not in ('counts', 'totals', 'bimap', 'wrappers', 'donation-admission'):
            raise ValueError('unknown proof stage')
        self.root = root.resolve()
        self.run_id, self.source_sha, self.stage = run_id, source_sha, stage
        self.control = self.root / CONTROL
        self.verification = self.control / 'verification'
        self.pins_path = safe_path(self.root, CONTROL / 'verification/current-source-pins.json')
        self.pins = read_json(self.pins_path)
        if self.pins.get('schema') != 'M3_COLLECTIONS_CURRENT_PROOF_SOURCE_PINS_V1':
            raise ValueError('unknown current proof source schema')
        self.deps, self.junit = deps.resolve(), junit.resolve()
        self.forward = ['--run-id', run_id, '--expected-source-sha', source_sha]
        self.base = safe_path(self.root, CONTROL / 'target' / run_id)
        self.target = safe_path(self.root, self.base.relative_to(self.root) / stage)
        self._head()
        self.binding = self.source_binding()
        self.target.mkdir(parents=True, exist_ok=True)

    def _head(self):
        actual = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=self.root,
                                check=True, capture_output=True, text=True, timeout=30).stdout.strip()
        if actual != self.source_sha:
            raise ValueError('exact checkout commit mismatch')

    def source_binding(self):
        self._head()
        sources = source_inventory(self.root, self.pins)
        implementations = {}
        for name in IMPLEMENTATIONS:
            p = safe_path(self.root, CONTROL / 'verification' / name)
            if not p.is_file():
                raise ValueError('missing current proof implementation: ' + name)
            implementations[name] = digest(p)
        manifest = safe_path(self.root, 'm3/collections/qualification/installed-source-current-owner-continuity-20261017.tsv')
        implementations[manifest.relative_to(self.root).as_posix()] = digest(manifest)
        jars = {p.name: digest(p) for p in sorted(self.deps.glob('*.jar'))}
        if not jars or digest(self.junit) != '329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a':
            raise ValueError('current dependency/JUnit custody unavailable')
        return {'run_id': self.run_id, 'source_commit': self.source_sha,
                'pins_sha256': digest(self.pins_path), 'sources': dict(sorted(sources.items())),
                'implementations': implementations, 'dependencies': jars, 'junit_sha256': digest(self.junit)}

    def recheck(self):
        if self.source_binding() != self.binding:
            raise ValueError('current source/dependency/run proof drift')

    def stamp(self, compiled):
        self.recheck()
        return {'binding': self.binding, 'compiled': inventory(self.root, compiled),
                'compiled_roots': [Path(p).relative_to(self.root).as_posix() for p in compiled]}

    def check_stamp(self, stamp):
        self.recheck()
        if stamp.get('binding') != self.binding:
            raise ValueError('cross-run/source proof receipt')
        assert_inventory(self.root, stamp['compiled'], stamp['compiled_roots'])

    def read_prior(self, stage):
        report = safe_path(self.root, (self.base / stage / 'evidence/qualification.json').relative_to(self.root))
        value = read_json(report)
        self.check_stamp(value['custody'])
        return value

    def write_report(self, report, compiled):
        report['custody'] = self.stamp(compiled)
        report['source_custody_only'] = False
        report['installed_jdk'] = 'NOT_RUN'
        report['full_reactor'] = 'NOT_RUN'
        report['phase_advance'] = False
        report['donor_verifier_blob'] = self.pins['donors'].get(self.stage)
        path = self.target / 'evidence/qualification.json'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(report, indent=2, sort_keys=True) + '\n', encoding='utf-8')
