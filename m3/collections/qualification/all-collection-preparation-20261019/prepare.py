#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Prepare exact Git-backed collection images; never transform or promote input code."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import subprocess
import sys

SCHEMA = 'M3_COLLECTION_PREPARATION/1'
PHASES = ('INVENTORY', 'ATOMIZE', 'PATTERNIZE', 'ARRAYIFY', 'ALGORITHM_COMPARE',
          'TARGET_QUALIFY', 'CONSOLIDATE', 'MERGE')
REQUIRED = ('JDK_M3SDK', 'GUAVA', 'ECLIPSE_COLLECTIONS', 'APACHE_COMMONS_COLLECTIONS',
            'SPRING', 'SWIFT_COLLECTIONS', 'PCOLLECTIONS', 'OTHER_COLLECTION_FORKS')
RECIPE_OWNERS = {
    'INVENTORY': 'com.synexia.rewrite.M3EveryModuleAtomPatternInventoryRecipe',
    'ATOMIZE': 'com.synexia.rewrite.M3AtomizeRecipe',
    'PATTERNIZE': 'com.synexia.rewrite.M3PatternizeRecipe',
    'ALGORITHM_COMPARE': 'com.synexia.rewrite.M3AlgorithmRecipeFirstFullReviewRecipe',
}


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob(data: bytes) -> str:
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()


def canonical_path(value: str) -> PurePosixPath:
    if not value or any(ord(c) < 32 for c in value) or '\\' in value or ':' in value:
        raise ValueError('Nonportable source path')
    path = PurePosixPath(value)
    if path.is_absolute() or str(path) != value or any(p in ('', '.', '..') for p in value.split('/')):
        raise ValueError('Noncanonical source path')
    return path


def git(root: Path, *args: str) -> bytes:
    result = subprocess.run(['git', '-C', str(root), *args], check=False,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=60)
    if result.returncode:
        raise RuntimeError('Git read failed: ' + ' '.join(args[:2]) + ': '
                           + result.stderr.decode('utf-8', 'replace')[:1500])
    return result.stdout


def stable_json(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True, ensure_ascii=True) + '\n').encode('utf-8')


def family(repo: str) -> str:
    name = repo.split('/')[-1].lower()
    return {'m3jdk21': 'JDK_M3SDK', 'guava': 'GUAVA', 'eclipse-collections': 'ECLIPSE_COLLECTIONS',
            'commons-collections': 'APACHE_COMMONS_COLLECTIONS', 'spring-framework': 'SPRING',
            'swift-collections': 'SWIFT_COLLECTIONS', 'pcollections': 'PCOLLECTIONS'}.get(name,
            'OTHER_COLLECTION_FORKS')


def selected(repo: str, path: str) -> bool:
    if repo.lower().endswith('/m3jdk21'):
        return (path.startswith('m3/collections/') or path.startswith('m3/pcollections/')
                or path.startswith('src/java.base/share/classes/java/util/')
                or path in ('AGENTS.md', 'm3/docs/m3jdk21-porting-invariant.md',
                            'm3/docs/name-mapping.json', 'LICENSE', 'ADDITIONAL_LICENSE_INFO'))
    return True


def kind(path: str) -> str:
    suffix = PurePosixPath(path).suffix.lower()
    return {'.java': 'JAVA', '.c': 'JNI_C', '.h': 'NATIVE_HEADER', '.cpp': 'JNI_CPP',
            '.cc': 'JNI_CPP', '.cxx': 'JNI_CPP', '.swift': 'SWIFT', '.xml': 'BUILD_OR_DATA',
            '.gradle': 'BUILD', '.kts': 'BUILD', '.md': 'DOCUMENT', '.py': 'TOOL'}.get(suffix, 'RESOURCE')


def nearest_pom(path: str, available: set[str]) -> str:
    parent = PurePosixPath(path).parent
    while str(parent) != '.':
        candidate = str(parent / 'pom.xml')
        if candidate in available:
            return candidate
        parent = parent.parent
    return 'pom.xml' if 'pom.xml' in available else 'MAVEN_BINDING_REQUIRED_NATIVE_BUILD_RETAINED'


def inspect_checkout(repo: str, root: Path, expected: str | None = None) -> dict:
    if len(repo.split('/')) != 2 or not (repo.startswith('hsoliwal/')
            or repo == 'hrldcpr/pcollections'):
        raise ValueError('Only hsoliwal or the explicit read-only PCollections reference is admitted')
    if root.is_symlink():
        raise ValueError('Refuse symlink checkout root')
    root = root.resolve(strict=True)
    head = git(root, 'rev-parse', '--verify', 'HEAD').decode('ascii').strip()
    if expected is not None and head != expected:
        raise ValueError('Checkout revision does not match required pin')
    if git(root, 'status', '--porcelain', '--untracked-files=all'):
        raise ValueError('Input checkout is dirty; no implicit stash or cleanup')
    tracked = []
    for record in git(root, 'ls-tree', '-rz', '--full-tree', head).split(b'\0'):
        if not record:
            continue
        descriptor, encoded_path = record.split(b'\t', 1)
        mode, object_kind, object_id = descriptor.decode('ascii').split(' ')
        path = encoded_path.decode('utf-8', 'strict')
        canonical_path(path)
        if selected(repo, path):
            tracked.append((path, mode, object_kind, object_id))
    if not tracked:
        raise ValueError('No tracked collection scope: refuse empty-success preparation')
    tracked.sort()
    return {'repository': repo, 'root': root, 'commit': head, 'tracked': tracked}


def prepare(inputs: list[dict], output: Path, required_families=REQUIRED) -> dict:
    if output.exists() or output.is_symlink():
        raise FileExistsError('Preparation output must be absent')
    output = output.absolute()
    identities = set()
    for item in inputs:
        identity = item['repository'].lower()
        if identity in identities:
            raise ValueError('Duplicate repository input; reconcile pins explicitly')
        identities.add(identity)
        if output == item['root'] or item['root'] in output.parents:
            raise ValueError('Output would mutate an input checkout')
    # All admission checks above precede output creation.
    output.mkdir(parents=True)
    rows, unresolved, repositories = [], [], []
    for item in sorted(inputs, key=lambda value: value['repository']):
        repo, root, commit = item['repository'], item['root'], item['commit']
        available = {row[0] for row in item['tracked']}
        repositories.append({'repository': repo, 'commit': commit, 'family': family(repo),
                             'scope_paths': len(available)})
        for path, mode, object_kind, object_id in item['tracked']:
            identity = digest((repo + '\0' + commit + '\0' + path).encode('utf-8'))
            row = {'id': identity, 'repository': repo, 'commit': commit, 'path': path,
                   'git_blob': object_id, 'mode': mode, 'source_kind': kind(path),
                   'maven_owner': nearest_pom(path, available),
                   'phases': {phase: 'NOT_RUN' for phase in PHASES},
                   'transformed': False, 'merge_ready': False}
            if object_kind != 'blob' or mode not in ('100644', '100755'):
                row['source_status'] = 'UNSUPPORTED_GIT_MODE'
                unresolved.append({'id': identity, 'reason': row['source_status']})
            else:
                data = git(root, 'cat-file', 'blob', object_id)
                if blob(data) != object_id:
                    raise ValueError('Git source bytes do not match object identity')
                row['sha256'] = digest(data)
                destination = output / 'sources' / repo.replace('/', '__') / commit / path
                destination.parent.mkdir(parents=True, exist_ok=True)
                with destination.open('xb') as stream:
                    stream.write(data)
                row['source_status'] = 'CAPTURED_EXACT_NOT_TRANSFORMED'
                row['captured_path'] = destination.relative_to(output).as_posix()
            rows.append(row)
        if git(root, 'rev-parse', '--verify', 'HEAD').decode('ascii').strip() != commit:
            raise RuntimeError('Input HEAD moved during capture; packet remains unqualified')
        if git(root, 'status', '--porcelain', '--untracked-files=all'):
            raise RuntimeError('Input worktree changed during capture; packet remains unqualified')
    rows.sort(key=lambda row: (row['repository'], row['path']))
    present = {item['family'] for item in repositories}
    missing = sorted(set(required_families) - present)
    manifest = {'schema': SCHEMA, 'repositories': repositories, 'files': rows,
                'recipe_owners': RECIPE_OWNERS, 'required_families': list(required_families),
                'missing_families': missing, 'unresolved': unresolved,
                'preparation_complete_for_supplied_checkouts': bool(inputs) and not unresolved,
                'entire_estate_complete': False,
                'atomized_all': False, 'patternized_all': False,
                'arrayified_all': False, 'merge_ready': False}
    data = stable_json(manifest)
    (output / 'SOURCE_MANIFEST.json').write_bytes(data)
    (output / 'SOURCE_MANIFEST.sha256').write_text(digest(data) + '  SOURCE_MANIFEST.json\n')
    (output / 'FILES.tsv').write_text('repository\tcommit\tpath\tgit_blob\tsha256\tsource_status\n'
        + ''.join('\t'.join(str(row.get(key, '')) for key in
                           ('repository', 'commit', 'path', 'git_blob', 'sha256', 'source_status')) + '\n'
                  for row in rows), encoding='utf-8')
    return manifest


def verify_packet(output: Path) -> dict:
    data = (output / 'SOURCE_MANIFEST.json').read_bytes()
    expected = (output / 'SOURCE_MANIFEST.sha256').read_text().split()[0]
    if digest(data) != expected:
        raise ValueError('Manifest drift')
    packet = json.loads(data)
    if packet.get('schema') != SCHEMA:
        raise ValueError('Unknown preparation schema')
    if any(packet.get(key) is not False for key in
           ('entire_estate_complete', 'atomized_all', 'patternized_all', 'arrayified_all', 'merge_ready')):
        raise ValueError('A preparation packet cannot assert transformation or merge readiness')
    paths = set()
    expected_captures = set()
    for row in packet['files']:
        identity = row['repository'], row['path']
        if identity in paths:
            raise ValueError('Duplicate source row')
        paths.add(identity)
        canonical_path(row['path'])
        if row['phases'] != {phase: 'NOT_RUN' for phase in PHASES}:
            raise ValueError('Unexecuted phase represented as execution')
        if row.get('transformed') is not False or row.get('merge_ready') is not False:
            raise ValueError('Unqualified file readiness')
        if row['source_status'] == 'CAPTURED_EXACT_NOT_TRANSFORMED':
            rel = str(canonical_path(row['captured_path']))
            p = output / rel
            if p.is_symlink() or not p.is_file():
                raise ValueError('Missing/symlink captured source')
            content = p.read_bytes()
            if digest(content) != row['sha256'] or blob(content) != row['git_blob']:
                raise ValueError('Captured source drift')
            expected_captures.add(rel)
    actual = {p.relative_to(output).as_posix() for p in (output / 'sources').rglob('*') if p.is_file()}
    if actual != expected_captures:
        raise ValueError('Source census is not closed: omitted or foreign file')
    return {'status': 'PASS_EXACT_PREPARATION', 'files': len(packet['files']),
            'captured': len(expected_captures), 'missing_families': packet['missing_families'],
            'manifest_sha256': expected, 'product_ready': False}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--checkout', action='append', default=[], metavar='OWNER/REPO=PATH')
    parser.add_argument('--pin', action='append', default=[], metavar='OWNER/REPO=SHA')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--verify-only', action='store_true')
    args = parser.parse_args()
    if args.verify_only:
        print(json.dumps(verify_packet(args.output), indent=2)); return
    pins = dict(value.split('=', 1) for value in args.pin)
    sources = []
    for value in args.checkout:
        repo, path = value.split('=', 1)
        sources.append(inspect_checkout(repo, Path(path), pins.get(repo)))
    prepare(sources, args.output)
    result = verify_packet(args.output)
    (args.output / 'PREPARATION_RESULT.json').write_bytes(stable_json(result))
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    main()
