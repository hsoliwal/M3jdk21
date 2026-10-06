#!/usr/bin/env python3
"""V3: direct body reads plus explicit custody of two malformed UTF-8 Git objects."""
import base64
import hashlib
import json
import sys
from pathlib import Path

def normalized(entries):
    answer = []
    names = set()
    for entry in entries:
        assert entry['path'] not in names
        names.add(entry['path'])
        assert '/' not in entry['path'] and entry['path'] not in ('', '.', '..')
        row = {key: entry[key] for key in ('path', 'mode', 'type', 'sha')}
        if entry['type'] == 'blob':
            row['size'] = entry['size']
        answer.append(row)
    return sorted(answer, key=lambda item: item['path'])


def main():
    if not __debug__:
        raise RuntimeError('Optimized Python disables required publication verification assertions')
    helpers = Path(__file__).resolve().parent
    assert hashlib.sha256((helpers / 'audit_base.py').read_bytes()).hexdigest() == '839c35c611de2dc02f2a3f1233131b24c79d77e109aaf010df9689dc8e04893b'
    assert hashlib.sha256((helpers / 'prepare_publication.py').read_bytes()).hexdigest() == '3cd42b4b5c4ba1e0eb01cace40bff7cb2fe967d65ac4e9a646b70d7daf4404fd'
    from audit_base import BASE_COMMIT, BASE_TREE, OUT, TASK, digest, exact_write, git_blob, git_tree, json_bytes, resolve
    from prepare_publication import BRANCH, PRODUCTION_UPDATES, expected_tree
    assert digest((helpers / 'verify_publication.py').read_bytes()) == '8cc5fd5c5df99ae8f48d0fbbf4618ada99be429af9cb39839511940192bd8639'
    assert digest((helpers / 'raw_custody_v3.py').read_bytes()) == '45fe2a39cc936f94e20cf0051f3844e6d6b5ba59b87b2d3ca175bb60917603be'
    from raw_custody_v3 import IDENTITY_ONLY, PROTOCOL, audit, bound

    preflight_path = Path(sys.argv[1]).resolve(strict=True)
    readback_path = Path(sys.argv[2]).resolve(strict=True)
    output = Path(sys.argv[3]).resolve()
    assert output.is_relative_to(OUT)
    assert len(sys.argv) in (4, 5)
    before_branch = len(sys.argv) == 5
    if before_branch:
        assert sys.argv[4] == '--before-branch'
    publisher = OUT / 'prepare_publication.py'
    assert digest(publisher.read_bytes()) == '3cd42b4b5c4ba1e0eb01cace40bff7cb2fe967d65ac4e9a646b70d7daf4404fd'
    model_raw = (OUT / 'BASE_TREE_MODEL.json').read_bytes()
    assert digest(model_raw) == '877a61dc6d9b4f4fa280faeadbd67db9ee3d56a68768fa06c917d85ab8fbaa80'
    model = json.loads(model_raw)
    assert model['commit'] == BASE_COMMIT and model['root'] == BASE_TREE
    base_trees = model['trees']
    for sha, entries in base_trees.items():
        assert git_tree(entries) == sha

    preflight_raw = preflight_path.read_bytes()
    preflight = json.loads(preflight_raw)
    assert preflight['schema'] == 'jcc-source-merge-recovery-publication-preflight/1'
    assert preflight['status'] == 'PASS'
    assert preflight['repository'] == 'hsoliwal/com.synexia' and preflight['branch'] == BRANCH
    assert preflight['expected_parent'] == BASE_COMMIT and preflight['base_tree'] == BASE_TREE
    assert preflight['base_tree_model_sha256'] == digest(model_raw)
    manifest_raw = Path(preflight['manifest_path']).read_bytes()
    assert digest(manifest_raw) == preflight['manifest_sha256']
    manifest = json.loads(manifest_raw)
    original_rows = manifest.get('rows', manifest.get('files'))
    originals = {row['path']: row for row in original_rows}
    rows = preflight['files']
    assert len(rows) == len({row['path'] for row in rows}) == len(original_rows) == len(originals)
    assert set(originals) == {row['path'] for row in rows}
    required_payload_bytes = {}
    for row in rows:
        original = originals[row['path']]
        assert all(row[key] == original[key] for key in ('local_path', 'mode', 'bytes', 'sha256'))
        assert row['git_blob'] == original.get('git_blob', original.get('git_blob_sha1'))
        path = Path(row['local_path'])
        assert path.is_file() and not path.is_symlink() and path.resolve().is_relative_to(TASK)
        raw = path.read_bytes()
        assert (len(raw), digest(raw), git_blob(raw)) == (row['bytes'], row['sha256'], row['git_blob'])
        assert ('100755' if path.stat().st_mode & 0o111 else '100644') == row['mode']
        if row['path'] in PRODUCTION_UPDATES or row['transport_encoding'] == 'base64':
            prior = required_payload_bytes.setdefault(row['git_blob'], raw)
            assert prior == raw
    updates = {row['path']: row['sha256'] for row in rows if row['preimage']['kind'] == 'existing'}
    assert updates == PRODUCTION_UPDATES
    assert len(rows) == preflight['file_count']
    assert sum(row['bytes'] for row in rows) == preflight['bytes']
    assert sum(row['restored_exact_d1_artifact'] for row in rows) == preflight['exact_restorations'] == 1039
    expected_root, expected_changes = expected_tree(base_trees, rows)
    assert expected_root == preflight['expected_tree']
    assert expected_changes == preflight['changed_trees']

    readback_raw = readback_path.read_bytes()
    readback = json.loads(readback_raw)
    assert readback['schema'] == 'jcc-source-recovery-remote-readback/1'
    assert readback['repository'] == 'hsoliwal/com.synexia'
    assert readback['preflight_sha256'] == digest(preflight_raw)
    remote_trees = readback['trees']
    expected_shas = {item['after'] for item in expected_changes}
    assert expected_shas <= set(remote_trees)
    combined = dict(base_trees)
    for sha, body in remote_trees.items():
        assert body['sha'] == sha and body['truncated'] is False
        actual = normalized(body['tree'])
        assert git_tree(actual) == sha
        combined[sha] = actual
    for change in expected_changes:
        assert normalized(remote_trees[change['after']]['tree']) == normalized(change['entries'])
    entries_verified = []
    for row in rows:
        actual = resolve(combined, expected_root, row['path'])
        assert actual['state'] == 'PRESENT'
        entry = actual['entry']
        assert (entry['type'], entry['mode'], entry['sha'], entry['size']) == (
            'blob', row['mode'], row['git_blob'], row['bytes'])
        entries_verified.append({key: row[key] for key in ('path', 'mode', 'git_blob', 'sha256', 'bytes')})

    assert readback['custody_protocol'] == PROTOCOL
    custody = bound(readback['raw_fixture_custody_input'])
    raw_custody = audit(custody, rows, base_trees, digest(preflight_raw), final={
        'commit': readback['commit'], 'combined_trees': combined,
        'file_calls': readback['raw_fixture_final_reads'],
    })
    assert len(raw_custody['blob_identity_custody']) == 2
    direct_required_payload_bytes = {
        sha: raw for sha, raw in required_payload_bytes.items() if sha not in IDENTITY_ONLY
    }
    assert len(direct_required_payload_bytes) == 6
    blob_results = {}
    for body in readback['blobs']:
        assert body['tool'] == 'mcp__codex_apps__github_fetch_file'
        request = body['request']
        assert set(request) == {'repository_full_name', 'path', 'ref', 'encoding'}
        assert request['repository_full_name'] == 'hsoliwal/com.synexia'
        assert request['ref'] == readback['commit']['sha'] and request['encoding'] == 'base64'
        response = body['response']
        assert response['isError'] is False
        actual_response = response['structuredContent']
        assert all(body[key] == actual_response[key] for key in ('sha', 'content', 'encoding'))
        assert actual_response['display_url'] == (
            'https://github.com/hsoliwal/com.synexia/blob/' + request['ref'] + '/' + request['path'])
        resolved = resolve(combined, expected_root, request['path'])
        assert resolved['state'] == 'PRESENT'
        assert resolved['entry']['type'] == 'blob'
        assert resolved['entry']['sha'] == body['sha']
        assert resolved['entry']['size'] == body['size']
        assert body['size_provenance'] == 'decoded actual response; separately matched remote tree size'
        assert body['sha'] not in IDENTITY_ONLY, 'Identity-only fixtures must not be fabricated as downloaded bodies'
        assert body['encoding'] == 'base64'
        assert body['sha'] not in blob_results
        encoded = ''.join(body['content'].split())
        raw = base64.b64decode(encoded, validate=True)
        assert len(raw) == body['size'] and git_blob(raw) == body['sha']
        if body['sha'] in required_payload_bytes:
            assert raw == required_payload_bytes[body['sha']], 'Remote body differs from exact local bytes'
        blob_results[body['sha']] = {'git_blob': body['sha'], 'bytes': len(raw), 'sha256': digest(raw)}
    required_bodies = [row for row in rows if (
        row['path'] in PRODUCTION_UPDATES or row['transport_encoding'] == 'base64')
        and row['git_blob'] not in IDENTITY_ONLY]
    for row in required_bodies:
        body = blob_results[row['git_blob']]
        assert (body['bytes'], body['sha256']) == (row['bytes'], row['sha256'])
    assert set(direct_required_payload_bytes) <= set(blob_results)

    commit = readback['commit']
    assert commit['tree']['sha'] == expected_root
    assert [parent['sha'] for parent in commit['parents']] == [BASE_COMMIT]
    if before_branch:
        assert 'ref' not in readback and 'pull_request' not in readback
        pre_branch_receipt = {
            'schema': 'jcc-source-recovery-before-branch-custody/1',
            'status': 'FINAL_COMMIT_CUSTODY_VERIFIED_BEFORE_BRANCH',
            'repository': 'hsoliwal/com.synexia', 'branch': BRANCH,
            'commit': commit['sha'], 'tree': expected_root, 'parent': BASE_COMMIT,
            'preflight_path': str(preflight_path), 'preflight_sha256': digest(preflight_raw),
            'readback_path': str(readback_path), 'readback_sha256': digest(readback_raw),
            'verification_script_sha256': digest(Path(__file__).read_bytes()),
            'custody_protocol': PROTOCOL, 'raw_fixture_custody': raw_custody,
            'required_blob_bodies_compared_byte_for_byte': 6,
            'required_blob_identities_verified_without_body_readback': 2,
            'distinct_changed_trees_verified': len(expected_shas),
            'payload_files': len(rows), 'payload_bytes': sum(row['bytes'] for row in rows),
            'branch_or_pull_request_verified': False,
        }
        exact_write(output, json_bytes(pre_branch_receipt))
        print(json.dumps({'status': pre_branch_receipt['status'],
                          'commit': commit['sha'], 'tree': expected_root,
                          'receipt_sha256': digest(json_bytes(pre_branch_receipt))}))
        return
    ref = readback['ref']
    assert ref['ref'] == 'refs/heads/' + BRANCH
    assert ref['object']['type'] == 'commit' and ref['object']['sha'] == commit['sha']
    pr = readback['pull_request']
    assert pr['state'] == 'open' and pr['draft'] is True and pr['merged'] is False
    assert pr['head']['sha'] == commit['sha'] and pr['head']['ref'] == BRANCH
    assert pr['head']['repo']['full_name'] == pr['base']['repo']['full_name'] == 'hsoliwal/com.synexia'
    assert pr['base']['ref'] == 'develop'
    assert pr['html_url'] == 'https://github.com/hsoliwal/com.synexia/pull/' + str(pr['number'])
    assert readback['ref_update_forced'] is False

    receipt = {
        'schema': 'jcc-source-recovery-publication/1',
        'status': 'PUBLISHED_CUSTODY_VERIFIED',
        'repository': 'hsoliwal/com.synexia', 'branch': BRANCH,
        'source_input_revision': BASE_COMMIT, 'source_input_root': BASE_TREE,
        'parent': BASE_COMMIT, 'commit': commit['sha'], 'tree': expected_root,
        'pull_request': {'number': pr['number'], 'url': pr['html_url'], 'draft': True, 'base': 'develop'},
        'manifest_path': preflight['manifest_path'], 'manifest_sha256': digest(manifest_raw),
        'preflight_path': str(preflight_path), 'preflight_sha256': digest(preflight_raw),
        'readback_path': str(readback_path), 'readback_sha256': digest(readback_raw),
        'verification_script_sha256': digest(Path(__file__).read_bytes()),
        'base_audit_script_sha256': digest((helpers / 'audit_base.py').read_bytes()),
        'publisher_script_sha256': digest(publisher.read_bytes()),
        'payload_files': len(rows), 'payload_bytes': sum(row['bytes'] for row in rows),
        'updates': 4, 'additions': len(rows) - 4, 'exact_restorations': 1039,
        'changed_ancestor_paths_verified': len(expected_changes),
        'distinct_changed_trees_verified': len(expected_shas),
        'unchanged_sibling_entries_preserved_over_changed_paths': sum(
            change['unchanged_sibling_entries'] for change in expected_changes),
        'tree_entries_verified': entries_verified,
        'blob_body_readbacks': list(blob_results.values()),
        'required_blob_bodies_compared_byte_for_byte': len(direct_required_payload_bytes),
        'required_blob_identities_verified_without_body_readback': 2,
        'production_blob_bodies_compared_byte_for_byte': 4,
        'nul_fixture_bodies_compared_byte_for_byte': 2,
        'malformed_utf8_body_readback': False,
        'custody_protocol': PROTOCOL,
        'raw_fixture_custody': raw_custody,
        'raw_fixture_custody_input': readback['raw_fixture_custody_input'],
        'all_required_blob_bodies_read_back': False,
        'branch_commit_parent_tree_pr_verified': True,
        'commit_parent_tree_and_ref_verified': True,
        'ref_update_forced': False,
        'qualification': 'Publication custody only; source execution and receiving acceptance require their separately scoped receipts.',
    }
    exact_write(output, json_bytes(receipt))
    print(json.dumps({'status': receipt['status'], 'commit': commit['sha'], 'tree': expected_root,
                      'pr': pr['html_url'], 'files': len(rows), 'receipt_sha256': digest(json_bytes(receipt))}))


if __name__ == '__main__':
    main()
