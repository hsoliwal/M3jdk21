#!/usr/bin/env python3
"""Validate a sealed source recovery manifest and prepare exact additive Git transport."""
import base64
import hashlib
import json
import re
import sys
from pathlib import Path

from audit_base import (BASE_COMMIT, BASE_TREE, OUT, REVIEW, TASK, digest,
                        exact_write, git_blob, git_tree, json_bytes, resolve, safe_path)

BRANCH = 'aix/jcc-source-merge-recovery-20261005'
PRODUCTION_UPDATES = {
    'm3-fast-search/src/main/java/com/synexia/m3/search/CompetitiveProblemReview.java':
        '83dad41fa4ce50b70f1e2e6800241b69b5136807902fd2fc6a6d024024894ac4',
    'synexia-algo/src/main/java/com/synexia/algorithms/corpus/ChallengeDonorPassLedger.java':
        'a089808e8563bac1f2d57aa1cc9f131e609c1d01ffcb1af8b302579adb00c346',
    'synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java':
        'adee46f08cd93edb1f97c30be904d0b686cc6426f56c1fae0ee6d670693526df',
    'synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3MasteryContractSurface.java':
        'd825d59df9ca7839ad06af2fd5c6ae386ce373f24eecc9184d23d1fe3261d8b1',
}


def private_sentinels():
    records = json.loads((TASK / 'work/current-integration-0b8dc/publication-redactions/REDACTION_MAP.json').read_bytes())['records']
    values = set()
    raw_paths = set()
    for row in records:
        raw_path = Path(row['original_local_path'])
        raw, derivative = raw_path.read_bytes(), Path(row['derivative_local_path']).read_bytes()
        assert digest(raw) == row['original_sha256'] and digest(derivative) == row['derivative_sha256']
        a, b = raw.splitlines(keepends=True), derivative.splitlines(keepends=True)
        assert len(a) == len(b)
        assert {i + 1 for i, (x, y) in enumerate(zip(a, b)) if x != y} == {r['line'] for r in row['redactions']}
        raw_paths.add(raw_path.resolve())
        for match in re.finditer(rb'env\.RSYNC_PASSWORD\s*[:=]\s*([^\r\n]+)', raw):
            value = match.group(1).strip()
            if value and b'REDACT' not in value:
                values.add(value)
    assert values, 'Historical local sentinel admission missing'
    return values, raw_paths


def admitted_non_text_resources():
    raw = (REVIEW / 'parser-tests/NON_TEXT_RESOURCES.json').read_bytes()
    assert digest(raw) == 'b8d1e91ac63146131cbb73aac667eb303a70180a8eba5238ea8b5680a8dc55d2'
    manifest = json.loads(raw)
    assert manifest['invalid_utf8_count'] == 2 and manifest['nul_count'] == 2
    assert len(manifest['files']) == 4
    admitted = {}
    for row in manifest['files']:
        assert row['transport_encoding'] == 'base64'
        payload = base64.b64decode(row['transport_content'], validate=True)
        assert len(payload) == row['bytes'] and digest(payload) == row['sha256']
        assert (b'\0' in payload) == row['contains_nul']
        try:
            payload.decode('utf-8')
            utf8 = True
        except UnicodeError:
            utf8 = False
        assert utf8 == row['strict_utf8']
        assert row['path'].startswith('resources/')
        admitted[row['sha256']] = {**row, 'raw': payload,
                                   'path_suffix': row['path'][len('resources/'):],
                                   'git_blob': git_blob(payload)}
    return admitted, digest(raw)


def expected_tree(trees, checked):
    changes = {}
    for row in checked:
        node = changes
        parts = safe_path(row['path'])
        for part in parts[:-1]:
            node = node.setdefault(part, {})
            assert '_entry' not in node
        assert parts[-1] not in node
        node[parts[-1]] = {'_entry': {'path': parts[-1], 'type': 'blob', 'mode': row['mode'],
                                    'sha': row['git_blob'], 'size': row['bytes']}}
    changed_trees = []

    def change(before_sha, requested, path):
        assert before_sha is None or before_sha in trees, f'Unobserved existing parent tree: {path}'
        entries = {e['path']: dict(e) for e in trees[before_sha]} if before_sha else {}
        before_entries = {name: dict(e) for name, e in entries.items()}
        for name, child in requested.items():
            old = entries.get(name)
            if '_entry' in child:
                assert len(child) == 1 and (old is None or old['type'] == 'blob')
                entries[name] = child['_entry']
            else:
                assert old is None or old['type'] == 'tree'
                child_path = path + '/' + name if path else name
                sha = change(old['sha'] if old else None, child, child_path)
                entries[name] = {'path': name, 'mode': '040000', 'type': 'tree', 'sha': sha}
        after = list(entries.values())
        sha = git_tree(after)
        preserved = [e for name, e in before_entries.items() if name not in requested]
        assert all(entries[e['path']] == e for e in preserved)
        changed_trees.append({'path': path, 'before': before_sha, 'after': sha,
                              'entries': sorted(after, key=lambda e: e['path']),
                              'unchanged_sibling_entries': len(preserved)})
        return sha

    return change(BASE_TREE, changes, ''), changed_trees


def main():
    if not __debug__:
        raise RuntimeError('Optimized Python disables required publication admission assertions')
    manifest_path = Path(sys.argv[1]).resolve(strict=True)
    destination = Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else OUT / 'epoch-01'
    assert destination.is_relative_to(OUT)
    model_raw = (OUT / 'BASE_TREE_MODEL.json').read_bytes()
    assert digest(model_raw) == '877a61dc6d9b4f4fa280faeadbd67db9ee3d56a68768fa06c917d85ab8fbaa80'
    model = json.loads(model_raw)
    assert model['commit'] == BASE_COMMIT and model['root'] == BASE_TREE
    trees = model['trees']
    manifest_raw = manifest_path.read_bytes()
    manifest = json.loads(manifest_raw)
    assert manifest['repository'] == 'hsoliwal/com.synexia'
    assert manifest['branch'] == BRANCH
    rows = manifest.get('rows', manifest.get('files'))
    assert isinstance(rows, list) and rows
    assert len(rows) == len({r['path'] for r in rows})
    safe_raw = (REVIEW / 'SAFE_ADDITIONS_STAGED.json').read_bytes()
    safe_sha = digest(safe_raw)
    assert safe_sha == 'a3c4741cad08018ff12f21288fa1483ffd55b2c878ab65f5c3c0ce82a10e3e91'
    assert safe_sha == model['inputs']['safe_additions_sha256']
    safe = json.loads(safe_raw)
    assert safe['merged_revision'] == BASE_COMMIT and safe['merged_root'] == BASE_TREE
    assert len(safe['rows']) == 1039
    restored = {r['path']: r for r in safe['rows']}
    assert len(restored) == 1039 and sum(r['bytes'] for r in restored.values()) == 24321144
    private_values, raw_paths = private_sentinels()
    non_text_resources, non_text_manifest_sha = admitted_non_text_resources()
    assert len(non_text_resources) == 4
    non_text_seen = set()
    patterns = {
        'github-token': re.compile(rb'\b(?:gh[pousr]_[A-Za-z0-9]{24,}|github_pat_[A-Za-z0-9_]{40,})\b'),
        'slack-token': re.compile(rb'\bxox[baprs]-[A-Za-z0-9-]{20,}\b'),
        'private-key': re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
        'aws-access-key': re.compile(rb'\b(?:AKIA|ASIA)[A-Z0-9]{16}\b'),
    }
    known = {e['sha'] for entries in trees.values() for e in entries if e['type'] == 'blob'}
    new_contents, references, checked, updates, scan_matches = {}, [], [], set(), []
    binary_blobs = {}
    for row in rows:
        path = row['path']
        safe_path(path)
        assert row['mode'] in ('100644', '100755')
        local = Path(row['local_path'])
        assert local.is_file() and not local.is_symlink() and local.resolve().is_relative_to(TASK)
        assert local.resolve() not in raw_paths
        raw = local.read_bytes()
        sha, blob = digest(raw), git_blob(raw)
        named_non_text = [r for r in non_text_resources.values()
                          if path.endswith('/' + r['path_suffix'])]
        assert len(named_non_text) <= 1
        if named_non_text:
            admitted = named_non_text[0]
            assert raw == admitted['raw'] and sha == admitted['sha256'], \
                f'Raw regression fixture identity changed: {path}'
            non_text_seen.add(sha)
        try:
            text = raw.decode('utf-8')
            non_text = '\0' in text
        except UnicodeError:
            text = None
            non_text = True
        if non_text:
            admitted = non_text_resources.get(sha)
            assert admitted is not None and raw == admitted['raw']
            assert path.endswith('/' + admitted['path_suffix'])
            assert row['mode'] == '100644'
        else:
            assert text.encode('utf-8') == raw
        assert len(raw) == row['bytes'] and sha == row['sha256']
        assert blob == row.get('git_blob', row.get('git_blob_sha1'))
        assert ('100755' if local.stat().st_mode & 0o111 else '100644') == row['mode']
        if any(v in raw for v in private_values):
            scan_matches.append({'path': path, 'kind': 'known-private-value-present'})
        for key, pattern in patterns.items():
            for match in pattern.finditer(raw):
                scan_matches.append({'path': path, 'kind': key, 'line': raw[:match.start()].count(b'\n') + 1})
        before = resolve(trees, BASE_TREE, path)
        if before['state'] == 'PRESENT':
            assert path in PRODUCTION_UPDATES, f'Unexpected existing path update: {path}'
            assert sha == PRODUCTION_UPDATES[path], f'Unreviewed production postimage: {path}'
            entry = before['entry']
            assert entry['type'] == 'blob' and entry['mode'] == row['mode']
            current_file = TASK / 'recovery-baseline-be92' / path
            current_raw = current_file.read_bytes()
            assert git_blob(current_raw) == entry['sha'] and len(current_raw) == entry['size']
            assert current_raw != raw, f'No-change path selected for publication: {path}'
            preimage = {'kind': 'existing', 'git_blob': entry['sha'], 'sha256': digest(current_raw),
                        'bytes': len(current_raw), 'mode': entry['mode']}
            updates.add(path)
        else:
            preimage = {'kind': 'absent', 'first_missing_path': before['first_missing_path']}
        if non_text:
            assert preimage['kind'] == 'absent'
        if path in restored:
            old = restored[path]
            assert preimage['kind'] == 'absent'
            assert (blob, sha, row['mode'], len(raw)) == (old['git_blob'], old['sha256'], old['mode'], old['bytes'])
            assert raw == Path(old['local_path']).read_bytes()
        checked.append({'path': path, 'local_path': str(local), 'bytes': len(raw), 'sha256': sha,
                        'git_blob': blob, 'mode': row['mode'], 'preimage': preimage,
                        'restored_exact_d1_artifact': path in restored,
                        'transport_encoding': 'base64' if non_text else 'utf-8'})
        element = {'path': path, 'type': 'blob', 'mode': row['mode']}
        if non_text:
            if blob not in known:
                binary_blobs.setdefault(blob, {'git_blob': blob, 'bytes': len(raw), 'sha256': sha,
                                               'encoding': 'base64',
                                               'content': base64.b64encode(raw).decode('ascii')})
            references.append({**element, 'sha': blob})
        elif blob in known or blob in new_contents:
            references.append({**element, 'sha': blob})
        else:
            new_contents[blob] = {**element, 'content': text}
    assert updates == set(PRODUCTION_UPDATES)
    assert restored.keys() <= {r['path'] for r in checked}
    assert non_text_seen == set(non_text_resources), 'Incomplete four-fixture raw resource coverage'
    assert not scan_matches, f'Credential scan requires review: {scan_matches}'
    expected_root, changed_trees = expected_tree(trees, checked)
    preflight = {'schema': 'jcc-source-merge-recovery-publication-preflight/1', 'status': 'PASS',
                 'repository': 'hsoliwal/com.synexia', 'branch': BRANCH,
                 'expected_parent': BASE_COMMIT, 'base_tree': BASE_TREE, 'expected_tree': expected_root,
                 'manifest_path': str(manifest_path), 'manifest_sha256': digest(manifest_raw),
                 'safe_additions_sha256': safe_sha,
                 'base_tree_model_sha256': digest(model_raw), 'file_count': len(checked),
                 'bytes': sum(r['bytes'] for r in checked), 'updates': len(updates),
                 'additions': len(checked) - len(updates), 'exact_restorations': len(restored),
                 'non_text_resource_manifest_sha256': non_text_manifest_sha,
                 'non_text_files': sum(r['transport_encoding'] == 'base64' for r in checked),
                 'new_binary_blobs': len(binary_blobs),
                 'credential_scan_matches': scan_matches, 'changed_trees': changed_trees, 'files': checked,
                 'scope': 'Exact payload bytes, source preimages, all historical restorations, and additive Git tree preservation. Publication validation does not establish recipe/compiler/JUnit/JNI acceptance.'}
    exact_write(destination / 'PREFLIGHT.json', json_bytes(preflight))
    batches, pending, size = [], [], 0
    for element in new_contents.values():
        count = len(element['content'].encode('utf-8'))
        if pending and (size + count > 900000 or len(pending) >= 100):
            batches.append(pending)
            pending, size = [], 0
        pending.append(element)
        size += count
    if pending:
        batches.append(pending)
    for i in range(0, len(references), 100):
        batches.append(references[i:i + 100])
    index = []
    for i, entries in enumerate(batches):
        raw = json.dumps(entries, ensure_ascii=True, separators=(',', ':')).encode('ascii')
        path = destination / 'transport' / f'batch-{i:03d}.json'
        exact_write(path, raw)
        index.append({'index': i, 'path': str(path), 'bytes': len(raw), 'sha256': digest(raw),
                      'entries': len(entries), 'content_entries': sum('content' in e for e in entries),
                      'reference_entries': sum('sha' in e for e in entries)})
    assert sum(e['entries'] for e in index) == len(checked)
    transport = {'schema': 'jcc-source-merge-recovery-git-transport/1',
                 'repository': 'hsoliwal/com.synexia', 'branch': BRANCH,
                 'expected_parent': BASE_COMMIT, 'base_tree': BASE_TREE, 'expected_tree': expected_root,
                 'manifest_path': str(manifest_path), 'manifest_sha256': digest(manifest_raw),
                 'files': len(checked), 'distinct_new_blobs': len(new_contents),
                 'binary_blobs': list(binary_blobs.values()),
                 'binary_transport_precondition': 'Create and losslessly verify these exact base64 Git blobs before any tree references them.',
                 'reused_reference_entries': len(references),
                 'payload_bytes': sum(e['bytes'] for e in index), 'batches': index,
                 'status': 'PREPARED_ONLY_NO_REMOTE_MUTATION'}
    exact_write(destination / 'transport' / 'INDEX.json', json_bytes(transport))
    print(json.dumps({'status': 'PASS', 'files': len(checked), 'updates': len(updates),
                      'bytes': preflight['bytes'], 'expected_tree': expected_root,
                      'batches': len(index), 'preflight_sha256': digest(json_bytes(preflight)),
                      'index': str(destination / 'transport' / 'INDEX.json')}))


if __name__ == '__main__':
    main()
