#!/usr/bin/env python3
"""Independent reads of frozen V3 source and actual 4/4/17 custody evidence."""
from pathlib import Path
import ast
import base64
import difflib
import hashlib
import json

OUT = Path(__file__).resolve().parent
PUB = OUT.parents[1]
ROOT = PUB.parents[2]
READS = {}


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def git_object(kind, raw):
    return hashlib.sha1(kind.encode() + b' ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()


def read(path):
    path = Path(path)
    assert path.is_file() and not path.is_symlink(), str(path)
    path = path.resolve()
    raw = path.read_bytes()
    row = {'path': str(path), 'bytes': len(raw), 'sha256': sha(raw)}
    assert path not in READS or READS[path] == row, str(path)
    READS[path] = row
    return raw


def obj(path):
    return json.loads(read(path))


def bound(spec):
    path = Path(spec['local_path'])
    assert path.is_file() and not path.is_symlink()
    assert path.resolve().is_relative_to(PUB)
    raw = read(path)
    assert sha(raw) == spec['sha256']
    return json.loads(raw)


def normalized(rows):
    result = []
    names = set()
    for row in rows:
        assert row['path'] not in names and '/' not in row['path'] and row['path'] not in ('', '.', '..')
        names.add(row['path'])
        entry = {k: row[k] for k in ['path', 'mode', 'type', 'sha']}
        if row['type'] == 'blob':
            entry['size'] = row['size']
        result.append(entry)
    return sorted(result, key=lambda row: row['path'])


def tree_id(entries):
    ordered = sorted(entries, key=lambda row: row['path'].encode() + (b'/' if row['type'] == 'tree' else b''))
    body = b''.join(row['mode'].lstrip('0').encode() + b' ' + row['path'].encode() + b'\0'
                    + bytes.fromhex(row['sha']) for row in ordered)
    return git_object('tree', body)


def main():
    pins = {
        'raw_custody_v3.py': '45fe2a39cc936f94e20cf0051f3844e6d6b5ba59b87b2d3ca175bb60917603be',
        'verify_publication_v3.py': '45d1e83ba81dfaed9434fddb7df9f4962b7fbbb8cb1c9b29f90754c8fcf38baa',
        'verify_publication.py': '8cc5fd5c5df99ae8f48d0fbbf4618ada99be429af9cb39839511940192bd8639',
        'audit_base.py': '839c35c611de2dc02f2a3f1233131b24c79d77e109aaf010df9689dc8e04893b',
        'prepare_publication.py': '3cd42b4b5c4ba1e0eb01cace40bff7cb2fe967d65ac4e9a646b70d7daf4404fd',
        'raw-custody-v3/PROTOCOL.md': 'cdf3826e3725acded05b38031b70d9e7a8f8579540b38d65a2a8e38ce64209f5',
        'raw-custody-v3/CUSTODY_INPUT.json': '22b58a9e97fd6bb4ebebc6a89dc13e368107c10429bff59191914a5e639c81df',
        'raw-custody-v3/draft-T01/raw_custody_v3.py': 'a96f1aec811774467092a09cd750d47785fd4dd5482ce792ffac93c38deb9f5b',
        'raw-custody-v3/draft-T01/verify_publication_v3.py': '7138709c360728c17c701391f303a02e21e6e1334b29bc3f67f6c9702f7440f4',
        'epoch-01/PREFLIGHT.json': '13e56b17314e89370fb8ce3f8417b405b89b34db8a1868d968589550975db84f',
        'BASE_TREE_MODEL.json': '877a61dc6d9b4f4fa280faeadbd67db9ee3d56a68768fa06c917d85ab8fbaa80'
    }
    snapshots = []
    for name, expected in pins.items():
        raw = read(PUB / name)
        assert sha(raw) == expected, name
        if name.endswith('.py'):
            ast.parse(raw, filename=name)
        if name.endswith(('.py', '.md')):
            destination = OUT / 'inputs' / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            assert not destination.exists()
            destination.write_bytes(raw)
            snapshots.append({'source': name, 'copy': str(destination), 'sha256': sha(raw), 'bytes': len(raw)})
    for name in ['raw_custody_v3.py', 'verify_publication_v3.py']:
        before = read(PUB / 'raw-custody-v3/draft-T01' / name).decode().splitlines(keepends=True)
        after = read(PUB / name).decode().splitlines(keepends=True)
        (OUT / (name + '.T01-T02.diff')).write_text(''.join(difflib.unified_diff(before, after, fromfile='T01/' + name, tofile='T02/' + name)))

    custody = obj(PUB / 'raw-custody-v3/CUSTODY_INPUT.json')
    assert custody['schema'] == 'jcc-two-utf8-fixture-custody-input/1'
    assert custody['protocol'] == 'EXACT_CREATE_AND_TREE_IDENTITY_FOR_TWO_INVALID_UTF8_FIXTURES'
    assert custody['repository'] == 'hsoliwal/com.synexia'
    preflight = obj(PUB / 'epoch-01/PREFLIGHT.json')
    assert custody['preflight_sha256'] == sha(read(PUB / 'epoch-01/PREFLIGHT.json'))
    manifest = obj(preflight['manifest_path'])
    assert sha(read(preflight['manifest_path'])) == custody['manifest_sha256'] == '9270a6b4098960e2b7c7bd87b17325b648c4de363957f36028bacc9c1955e328'
    original_rows = {row['path']: row for row in manifest['rows']}
    raw_rows = [row for row in preflight['files'] if row['transport_encoding'] == 'base64']
    assert len(raw_rows) == 4
    by_sha = {row['git_blob']: row for row in raw_rows}
    expected_raws = {'9e48fa497f59343f91fc5e2c36b9aba9cbc8b63d', '59b4ec83f687499a54f15695b9b246599f9579d7',
                     'fb1c29bba17633d2de5d53d924ec210965374c37', '24cdc62e634934527d551324c2449bcf951b5589'}
    identity_only = {'59b4ec83f687499a54f15695b9b246599f9579d7', '24cdc62e634934527d551324c2449bcf951b5589'}
    assert set(by_sha) == expected_raws
    index = bound(custody['transport_index'])
    indexed = {row['git_blob']: row for row in index['binary_blobs']}
    assert set(indexed) == expected_raws
    failure = bound(custody['failed_readback_observation'])
    assert failure['invalid_utf8_example']['result'] == 'FAIL_INVALID_UTF8_REENCODED'
    created = []
    for call in custody['creation_calls']:
        request = call['request']
        assert call['tool'] == 'mcp__codex_apps__github_create_blob'
        assert set(request) == {'repository_full_name', 'content', 'encoding'}
        assert request['repository_full_name'] == 'hsoliwal/com.synexia' and request['encoding'] == 'base64'
        raw = base64.b64decode(request['content'], validate=True)
        oid = git_object('blob', raw)
        assert oid in by_sha and oid not in {row['git_blob'] for row in created}
        row = by_sha[oid]
        assert raw == read(row['local_path']) == read(original_rows[row['path']]['local_path'])
        assert (len(raw), sha(raw)) == (row['bytes'], row['sha256'])
        assert request['content'] == indexed[oid]['content']
        assert call['expected'] == {'git_blob': oid, 'bytes': len(raw), 'sha256': sha(raw)}
        assert call['response']['isError'] is False and call['response']['structuredContent']['sha'] == oid
        created.append({'path': row['path'], 'git_blob': oid, 'bytes': len(raw), 'sha256': sha(raw),
                        'actual_request_local_index_bytes_equal': True, 'actual_returned_sha_matches': True})
    assert len(created) == 4

    probe_commit = '9dab34bd9e34e4d661004482248b5b02d94e0816'
    probe_tree = 'bf2b8fe43cda47f0c6363c8d376f61419e647e4d'
    commit_spec = custody['verification_commit']
    assert commit_spec['request_url'] == 'https://api.github.com/repos/hsoliwal/com.synexia/git/commits/' + probe_commit
    response = commit_spec['original_response']
    assert response['isError'] is False
    commit = json.loads(response['structuredContent']['content'])
    assert commit == commit_spec['body']
    assert commit['sha'] == probe_commit and commit['tree']['sha'] == probe_tree
    assert [row['sha'] for row in commit['parents']] == ['be92c62ece9023b5c33676716a1076d00e26120a']
    plan = bound(custody['verification_plan'])
    assert plan['expected_tree'] == probe_tree and plan['branch_or_ref'] is None
    expected_trees = {row['after']: normalized(row['entries']) for row in plan['changed_trees']}
    observed_trees = {}
    tree_receipts = []
    for spec in custody['verification_tree_calls']:
        call = bound(spec)
        result = call['result']
        assert result['status'] == 'fulfilled' and result['value']['isError'] is False
        body = json.loads(result['value']['structuredContent']['content'])
        oid = body['sha']
        assert oid not in observed_trees and oid in expected_trees
        assert call['url'] == 'https://api.github.com/repos/hsoliwal/com.synexia/git/trees/' + oid
        assert body['truncated'] is False
        entries = normalized(body['tree'])
        assert tree_id(entries) == oid and entries == expected_trees[oid]
        observed_trees[oid] = entries
        tree_receipts.append({'git_tree': oid, 'entries': len(entries), 'complete': True, 'independent_tree_hash_equal': True,
                              'actual_entries_equal_planned': True, 'response_file_sha256': spec['sha256']})
    assert len(observed_trees) == len(expected_trees) == len(plan['changed_trees']) == 17
    model = obj(PUB / 'BASE_TREE_MODEL.json')
    all_trees = {**model['trees'], **observed_trees}
    memberships = []
    for row in raw_rows:
        current = probe_tree
        components = row['path'].split('/')
        for i, part in enumerate(components):
            entry = next(entry for entry in all_trees[current] if entry['path'] == part)
            if i < len(components) - 1:
                assert entry['type'] == 'tree'
                current = entry['sha']
        assert (entry['type'], entry['mode'], entry['sha'], entry['size']) == ('blob', row['mode'], row['git_blob'], row['bytes'])
        memberships.append({'path': row['path'], 'mode': entry['mode'], 'git_blob': entry['sha'], 'bytes': entry['size']})

    read_packet = bound(custody['verification_file_reads'])
    assert read_packet['schema'] == 'jcc-actual-file-read-calls/1' and len(read_packet['calls']) == 4
    by_path = {row['path']: row for row in raw_rows}
    file_results = []
    seen = set()
    for call in read_packet['calls']:
        request = call['request']
        path = request['path']
        assert path in by_path and path not in seen
        seen.add(path)
        assert request == {'repository_full_name': 'hsoliwal/com.synexia', 'path': path, 'ref': probe_commit, 'encoding': 'base64'}
        result = call['result']
        assert result['status'] == 'fulfilled' and result['value']['isError'] is False
        response = result['value']['structuredContent']
        row = by_path[path]
        assert response['sha'] == row['git_blob'] and response['encoding'] == 'base64'
        assert response['display_url'] == 'https://github.com/hsoliwal/com.synexia/blob/' + probe_commit + '/' + path
        returned = base64.b64decode(''.join(response['content'].split()), validate=True)
        local = read(row['local_path'])
        equal = returned == local
        if row['git_blob'] in identity_only:
            assert not equal and returned == local.decode('utf-8', errors='replace').encode('utf-8')
            assert git_object('blob', returned) != row['git_blob']
        else:
            assert equal and b'\0' in returned and git_object('blob', returned) == row['git_blob']
        file_results.append({'path': path, 'reported_git_blob': response['sha'], 'expected_bytes': len(local),
                             'returned_bytes': len(returned), 'raw_body_readback': equal,
                             'returned_bytes_git_blob': git_object('blob', returned),
                             'returned_sha256': sha(returned), 'identity_only_exception': row['git_blob'] in identity_only})
    assert sum(row['raw_body_readback'] for row in file_results) == 2

    inputs = sorted(READS.values(), key=lambda row: row['path'])
    for row in inputs:
        raw = Path(row['path']).read_bytes()
        assert len(raw) == row['bytes'] and sha(raw) == row['sha256'], row['path']
    facts = {'schema': 'independent-v3-custody-source-and-actual-input-admission/v1',
             'status': 'ADMITTED_T02_FOR_PROBE_AND_SUBSEQUENT_CONDITIONAL_PUBLICATION',
             'helper_sha256': pins['raw_custody_v3.py'], 'verifier_sha256': pins['verify_publication_v3.py'],
             'custody_input_sha256': pins['raw-custody-v3/CUSTODY_INPUT.json'], 'protocol_sha256': pins['raw-custody-v3/PROTOCOL.md'],
             'manifest_sha256': custody['manifest_sha256'], 'source_snapshots': snapshots,
             'python_files_ast_parsed_without_execution': 7,
             'original_unexecuted_T01_preserved': True,
             'two_review_corrections_present': ['commit original wrapper body equality', 'original leaf path symlink check before resolve'],
             'actual_creation_calls': created, 'verification_tree_reads': tree_receipts,
             'verification_memberships': memberships, 'verification_file_reads': file_results,
             'verification_commit': probe_commit, 'verification_tree': probe_tree,
             'verification_parent': commit['parents'][0]['sha'], 'parsed_commit_equals_actual_wrapper': True,
             'required_direct_final_bodies': 6, 'required_identity_only_final_bodies': 2,
             'helper_or_verifier_executed_by_reviewer': False, 'project_executions': 0, 'remote_actions': 0,
             'current_raw_probe_helper_execution_claimed': False, 'final_payload_or_branch_execution_claimed': False,
             'stable_read_inputs': len(inputs),
             'limits': ['Actual GitHub call authenticity and chronological branch ordering remain root-owned evidence.',
                        'Two raw remote byte arrays and their remote SHA-256 are not recovered; local/request identities are bound through Git object identity and immutable tree membership.',
                        'Standalone probe uses the frozen inspected helper modules; full V3 pins them before imports. No full process attestation is claimed.',
                        'Prior failed raw readback, source BLOCKED status and export/destination limits remain unchanged.']}
    (OUT / 'REVIEW.json').write_text(json.dumps(facts, indent=2) + '\n')
    (OUT / 'INPUT_SEALS.json').write_text(json.dumps({'schema': 'independent-read-input-seals/v1', 'count': len(inputs),
                                                    'all_inputs_reread_unchanged': True, 'files': inputs}, indent=2) + '\n')
    print(json.dumps({'status': facts['status'], 'creation_calls': len(created), 'actual_tree_reads': len(tree_receipts),
                      'file_read_calls': len(file_results), 'direct_raw_reads': 2, 'failed_utf8_reads': 2,
                      'stable_inputs': len(inputs), 'review_sha256': sha((OUT / 'REVIEW.json').read_bytes())}))


if __name__ == '__main__':
    main()
