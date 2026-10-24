#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only original M3JDK21 collection-source inventory, never an admission gate.

The historical installed-source manifest is not replaced or quietly promoted.
Unlisted files may have separately reviewed successor receipts; this tool makes
no claim about their qualification without reviewing those receipts individually.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess


def fail(message):
    raise ValueError(message)


def checksum(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def pairs_unique(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            fail('duplicate JSON field: ' + key)
        result[key] = value
    return result


def checked_path(root, relative):
    if (not isinstance(relative, str) or '\\' in relative or '\x00' in relative
            or not relative or relative.startswith('/') or '..' in relative.split('/')):
        fail('unsafe relative source path: ' + repr(relative))
    path = root / relative
    if path.is_symlink():
        fail('symlinked source/receipt refused: ' + relative)
    return path


def read_historical(root):
    packet_path = checked_path(root, 'm3/collections/synexia-donation.json')
    packet = json.loads(packet_path.read_text('utf-8'), object_pairs_hook=pairs_unique)
    manifest_rel = 'm3/collections/' + packet['installed_source_manifest']
    manifest = checked_path(root, manifest_rel)
    if not manifest.is_file() or checksum(manifest) != packet['installed_source_manifest_sha256']:
        fail('historical installed-source manifest is missing or changed')
    rows = {}
    for line in manifest.read_text('utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        cells = line.split('\t')
        if len(cells) != 2 or not re.fullmatch('[0-9a-f]{64}', cells[1]):
            fail('invalid historical source row')
        path = cells[0]
        checked_path(root, path)
        if path in rows:
            fail('duplicate historical source path')
        rows[path] = cells[1]
    if not rows:
        fail('empty historical source census')
    return manifest_rel, checksum(manifest), rows


def git_head(root):
    return subprocess.check_output(
        ['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()


def exact_git_java_sources(root, manifest_rel, current_paths):
    # Match the original index, not merely whatever happened to exist on disk.
    # Untracked Java files, deleted tracked files and staged changes are all
    # refused before any report can claim exact-head source evidence.
    paths = subprocess.check_output([
        'git', '-C', str(root), 'ls-files', '-z', '--', 'm3/collections/src'])
    tracked = {item.decode('utf-8') for item in paths.split(b'\0') if item}
    tracked_java = {path for path in tracked if path.endswith('.java')}
    if tracked_java != current_paths:
        fail('current Java source set differs from the checked-out Git index')
    dirty = subprocess.check_output([
        'git', '-C', str(root), 'status', '--porcelain', '--untracked-files=no', '--',
        'm3/collections/src', 'm3/collections/synexia-donation.json', manifest_rel])
    if dirty:
        fail('tracked collection source or provenance modified after checkout')
    return len(tracked_java)


def inspect(root, expected_head=None):
    root = Path(root).resolve(strict=True)
    if expected_head and git_head(root) != expected_head:
        fail('checked-out head does not match requested PR head')
    manifest_rel, manifest_sha, history = read_historical(root)
    source = root / 'm3/collections/src'
    if not source.is_dir():
        fail('missing original M3 collection source tree')
    current = {}
    for file in sorted(source.rglob('*.java')):
        if file.is_symlink():
            fail('symlinked current Java owner: ' + str(file))
        relative = file.relative_to(root).as_posix()
        current[relative] = checksum(file)
    if not current:
        fail('empty current Java owner inventory')
    tracked_count = None
    if expected_head:
        tracked_count = exact_git_java_sources(root, manifest_rel, set(current))
        if git_head(root) != expected_head:
            fail('checked-out head changed during source census')
    old_java = {p: digest for p, digest in history.items()
                if p.startswith('m3/collections/src/') and p.endswith('.java')}
    mismatch = sorted(p for p in old_java if p in current and current[p] != old_java[p])
    missing = sorted(p for p in old_java if p not in current)
    not_in_history = sorted(p for p in current if p not in old_java)
    matching = sorted(p for p in old_java if p in current and current[p] == old_java[p])
    return {
        'schema': 'M3_COLLECTION_CURRENT_SOURCE_CENSUS_V1',
        'status': 'INVENTORY_ONLY_NOT_QUALIFIED',
        'promotion_allowed': False,
        'checked_head_sha': expected_head,
        'exact_git_java_index_count': tracked_count,
        'historical_manifest': manifest_rel,
        'historical_manifest_sha256': manifest_sha,
        'historical_total_rows': len(history),
        'historical_java_rows': len(old_java),
        'current_java_files': len(current),
        'current_main_java_files': sum('/src/main/' in p for p in current),
        'current_test_java_files': sum('/src/test/' in p for p in current),
        'historical_exact_match': len(matching),
        'historical_digest_mismatch': mismatch,
        'historical_missing': missing,
        'absent_from_this_historical_manifest': not_in_history,
        'current_java_sha256': current,
        'qualification_warning': 'Other successor receipts may exist; this census does not interpret or supersede them.'
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('root', type=Path)
    parser.add_argument('--expected-head')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = inspect(args.root, args.expected_head)
    data = json.dumps(result, sort_keys=True, indent=2) + '\n'
    if args.output:
        # Output is only a derived report; authoritative collection sources never change.
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(data, encoding='utf-8')
    print(json.dumps({key: result[key] for key in (
        'status', 'historical_java_rows', 'current_java_files',
        'historical_exact_match', 'current_main_java_files',
        'current_test_java_files', 'promotion_allowed')}, sort_keys=True))


if __name__ == '__main__':
    main()
