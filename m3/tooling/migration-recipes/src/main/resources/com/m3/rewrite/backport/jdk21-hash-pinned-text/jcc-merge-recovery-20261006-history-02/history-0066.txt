#!/usr/bin/env python3
"""Narrow Git-object custody for two malformed UTF-8 fixtures; never a byte readback."""
import base64
import hashlib
import json
import sys
from pathlib import Path

REPOSITORY = 'hsoliwal/com.synexia'
PROTOCOL = 'EXACT_CREATE_AND_TREE_IDENTITY_FOR_TWO_INVALID_UTF8_FIXTURES'
MANIFEST_SHA = '9270a6b4098960e2b7c7bd87b17325b648c4de363957f36028bacc9c1955e328'
INDEX_SHA = 'dce1e97079aeb3ad9edbdf9f814b3ccaeebfd0053c9d2b4b79ff841632c0736d'
FAILURE_SHA = '224b3baa99e71c846cc96319c0a829eec3521d75f68731332ac2fa59fa214112'
PLAN_SHA = '00fa5b9e4e99621ed6b5aac3c0da4c6506f82e921a3da42fdd4645285653e319'
PROBE_COMMIT = '9dab34bd9e34e4d661004482248b5b02d94e0816'
PROBE_TREE = 'bf2b8fe43cda47f0c6363c8d376f61419e647e4d'
IDENTITY_ONLY = {
    '59b4ec83f687499a54f15695b9b246599f9579d7': (
        'parser-bad-manifest-utf8/parser-dependencies.tsv', 4,
        '823a8864668594f33fd21b51f42c08643d90be2f0d903653175047f5d0298e6c'),
    '24cdc62e634934527d551324c2449bcf951b5589': (
        'parser-bad-utf8/Dependency.java.txt', 22,
        '7eb80c376a9681170665f5c0d8b86214e9b44aa6f1d08690a83b26aeba536f3c'),
}
RAW_SHAS = set(IDENTITY_ONLY) | {
    '9e48fa497f59343f91fc5e2c36b9aba9cbc8b63d',
    'fb1c29bba17633d2de5d53d924ec210965374c37',
}


def bound(spec, expected_sha=None):
    from audit_base import OUT, digest
    path = Path(spec['local_path']).resolve(strict=True)
    assert path.is_file() and not path.is_symlink() and path.is_relative_to(OUT)
    raw = path.read_bytes()
    assert digest(raw) == spec['sha256']
    if expected_sha is not None:
        assert spec['sha256'] == expected_sha
    return json.loads(raw)


def actual_file_reads(calls, rows, revision):
    from audit_base import digest, git_blob
    expected = {row['path']: row for row in rows}
    assert len(calls) == len(expected) == 4
    observed = {}
    for call in calls:
        request = call['request']
        assert set(request) == {'repository_full_name', 'path', 'ref', 'encoding'}
        path = request['path']
        assert path in expected and path not in observed
        assert request == {'repository_full_name': REPOSITORY, 'path': path,
                           'ref': revision, 'encoding': 'base64'}
        result = call['result']
        assert result['status'] == 'fulfilled'
        response = result['value']
        assert response['isError'] is False
        body = response['structuredContent']
        row = expected[path]
        assert body['sha'] == row['git_blob'] and body['encoding'] == 'base64'
        assert body['display_url'] == 'https://github.com/' + REPOSITORY + '/blob/' + revision + '/' + path
        downloaded = base64.b64decode(''.join(body['content'].split()), validate=True)
        local = Path(row['local_path']).read_bytes()
        assert (len(local), digest(local), git_blob(local)) == (
            row['bytes'], row['sha256'], row['git_blob'])
        if row['git_blob'] in IDENTITY_ONLY:
            suffix, size, local_sha = IDENTITY_ONLY[row['git_blob']]
            assert path.endswith('/' + suffix)
            assert (len(local), digest(local)) == (size, local_sha)
            try:
                local.decode('utf-8', errors='strict')
            except UnicodeDecodeError:
                pass
            else:
                raise AssertionError('Allowlisted fixture is no longer malformed UTF-8')
            assert downloaded != local
            assert downloaded == local.decode('utf-8', errors='replace').encode('utf-8')
            assert git_blob(downloaded) != row['git_blob']
            observed[path] = {
                'path': path, 'git_blob': row['git_blob'],
                'raw_body_readback': False, 'remote_sha256_observed': False,
                'read_attempt': 'FAILED_UTF8_REENCODING',
                'returned_encoded_body_bytes': len(downloaded),
                'returned_encoded_body_sha256': digest(downloaded),
            }
        else:
            assert downloaded == local and git_blob(downloaded) == row['git_blob']
            observed[path] = {
                'path': path, 'git_blob': row['git_blob'],
                'raw_body_readback': True, 'remote_sha256_observed': True,
                'bytes': len(downloaded), 'sha256': digest(downloaded),
            }
    return observed


def audit(custody, rows, base_trees, preflight_sha, *, final=None):
    from audit_base import BASE_COMMIT, BASE_TREE, digest, git_blob, git_tree, resolve
    from prepare_publication import expected_tree
    from verify_publication import normalized
    assert custody['schema'] == 'jcc-two-utf8-fixture-custody-input/1'
    assert custody['repository'] == REPOSITORY and custody['protocol'] == PROTOCOL
    assert custody['preflight_sha256'] == preflight_sha
    assert custody['manifest_sha256'] == MANIFEST_SHA
    index = bound(custody['transport_index'], INDEX_SHA)
    assert index['manifest_sha256'] == MANIFEST_SHA
    assert index['repository'] == REPOSITORY
    failure = bound(custody['failed_readback_observation'], FAILURE_SHA)
    assert failure['invalid_utf8_example']['result'] == 'FAIL_INVALID_UTF8_REENCODED'
    plan = bound(custody['verification_plan'], PLAN_SHA)
    assert plan['expected_tree'] == PROBE_TREE and plan['base_commit'] == BASE_COMMIT
    raw_rows = [row for row in rows if row['transport_encoding'] == 'base64']
    by_sha = {row['git_blob']: row for row in raw_rows}
    assert len(raw_rows) == len(by_sha) == 4 and set(by_sha) == RAW_SHAS
    payloads = {sha: Path(row['local_path']).read_bytes() for sha, row in by_sha.items()}
    index_rows = {row['git_blob']: row for row in index['binary_blobs']}
    assert set(index_rows) == RAW_SHAS
    calls = custody['creation_calls']
    assert len(calls) == 4
    created = set()
    for call in calls:
        assert call['tool'] == 'mcp__codex_apps__github_create_blob'
        request = call['request']
        assert set(request) == {'repository_full_name', 'content', 'encoding'}
        assert request['repository_full_name'] == REPOSITORY and request['encoding'] == 'base64'
        uploaded = base64.b64decode(request['content'], validate=True)
        sha = git_blob(uploaded)
        assert sha in by_sha and sha not in created
        row, indexed = by_sha[sha], index_rows[sha]
        assert uploaded == payloads[sha]
        assert (len(uploaded), digest(uploaded)) == (row['bytes'], row['sha256'])
        assert request['content'] == indexed['content'] and indexed['encoding'] == 'base64'
        assert call['expected'] == {'git_blob': sha, 'bytes': row['bytes'], 'sha256': row['sha256']}
        response = call['response']
        assert response['isError'] is False and response['structuredContent']['sha'] == sha
        created.add(sha)
    assert created == RAW_SHAS

    commit = custody['verification_commit']
    assert commit['request_url'] == 'https://api.github.com/repos/' + REPOSITORY + '/git/commits/' + PROBE_COMMIT
    body = commit['body']
    assert body['sha'] == PROBE_COMMIT and body['tree']['sha'] == PROBE_TREE
    assert [parent['sha'] for parent in body['parents']] == [BASE_COMMIT]
    for sha, entries in base_trees.items():
        assert git_tree(entries) == sha
    probe_root, changes = expected_tree(base_trees, raw_rows)
    assert probe_root == PROBE_TREE and changes == plan['changed_trees']
    assert plan['base_tree'] == BASE_TREE and plan['branch_or_ref'] is None
    expected_shas = {change['after'] for change in changes}
    trees = {}
    for spec in custody['verification_tree_calls']:
        call = bound(spec)
        result = call['result']
        assert result['status'] == 'fulfilled' and result['value']['isError'] is False
        actual = json.loads(result['value']['structuredContent']['content'])
        sha = actual['sha']
        assert sha in expected_shas and sha not in trees
        assert call['url'] == 'https://api.github.com/repos/' + REPOSITORY + '/git/trees/' + sha
        assert actual['truncated'] is False
        entries = normalized(actual['tree'])
        assert git_tree(entries) == sha
        trees[sha] = entries
    assert set(trees) == expected_shas
    for change in changes:
        assert trees[change['after']] == normalized(change['entries'])
    combined = dict(base_trees)
    combined.update(trees)
    for row in raw_rows:
        item = resolve(combined, PROBE_TREE, row['path'])
        assert item['state'] == 'PRESENT'
        entry = item['entry']
        assert (entry['type'], entry['sha'], entry['mode'], entry['size']) == (
            'blob', row['git_blob'], row['mode'], row['bytes'])
    probe_reads = bound(custody['verification_file_reads'])
    assert probe_reads['schema'] == 'jcc-actual-file-read-calls/1'
    probe_observed = actual_file_reads(probe_reads['calls'], raw_rows, PROBE_COMMIT)
    result = {
        'protocol': PROTOCOL,
        'raw_create_requests_and_responses_verified': 4,
        'verification_commit': PROBE_COMMIT, 'verification_tree': PROBE_TREE,
        'verification_trees_verified': len(trees),
        'probe_direct_body_reads': 2, 'probe_failed_utf8_body_reads': 2,
        'failed_readback_observation': custody['failed_readback_observation'],
        'identity_only_allowlist': sorted(IDENTITY_ONLY),
        'assumption': 'Authentic GitHub object identifiers and ordinary Git content addressing bind the exact uploaded bytes; the two malformed UTF-8 remote byte arrays and their SHA-256 values were not recovered.',
        'publication_qualification': 'Custody only; no source test or receiving acceptance promotion.',
    }
    if final is not None:
        assert final['commit']['sha'] != PROBE_COMMIT
        assert [parent['sha'] for parent in final['commit']['parents']] == [BASE_COMMIT]
        final_root = final['commit']['tree']['sha']
        assert final_root == index['expected_tree']
        final_observed = actual_file_reads(final['file_calls'], raw_rows, final['commit']['sha'])
        identities = []
        for row in raw_rows:
            item = resolve(final['combined_trees'], final_root, row['path'])
            assert item['state'] == 'PRESENT'
            entry = item['entry']
            assert (entry['type'], entry['sha'], entry['mode'], entry['size']) == (
                'blob', row['git_blob'], row['mode'], row['bytes'])
            if row['git_blob'] in IDENTITY_ONLY:
                identities.append({
                    'path': row['path'], 'git_blob': row['git_blob'], 'bytes': row['bytes'],
                    'sha256_local_uploaded_bytes': row['sha256'],
                    'method': 'BASE64_CREATE_AND_REMOTE_GIT_TREE_IDENTITY',
                    'raw_body_readback': False, 'remote_sha256_observed': False,
                    'creation_response_git_sha': row['git_blob'],
                    'verification_tree_size': row['bytes'], 'final_tree_size': entry['size'],
                    'failed_probe_read': probe_observed[row['path']],
                    'failed_final_read': final_observed[row['path']],
                })
        assert len(identities) == 2
        result['blob_identity_custody'] = identities
        result['final_direct_raw_body_reads'] = 2
        result['final_failed_utf8_body_reads'] = 2
    return result


def main():
    if not __debug__:
        raise RuntimeError('Optimized Python disables required assertions')
    from audit_base import OUT, digest, exact_write, json_bytes
    preflight_path, custody_path, output = map(Path, sys.argv[1:])
    preflight_raw = preflight_path.read_bytes()
    preflight = json.loads(preflight_raw)
    assert preflight['status'] == 'PASS' and preflight['manifest_sha256'] == MANIFEST_SHA
    model = json.loads((OUT / 'BASE_TREE_MODEL.json').read_bytes())
    custody = json.loads(custody_path.read_bytes())
    result = audit(custody, preflight['files'], model['trees'], digest(preflight_raw))
    result.update({'schema': 'jcc-two-utf8-fixture-probe-custody/1',
                   'status': 'GIT_OBJECT_CUSTODY_VERIFIED_BODY_READBACK_FAILED_FOR_TWO',
                   'custody_input_path': str(custody_path.resolve()),
                   'custody_input_sha256': digest(custody_path.read_bytes()),
                   'final_payload_batches_executed_by_this_script': 0})
    assert output.resolve().is_relative_to(OUT)
    exact_write(output, json_bytes(result))
    print(json.dumps({'status': result['status'], 'raw_creation_calls': 4,
                      'verification_trees': result['verification_trees_verified'],
                      'direct_body_reads': 2, 'failed_utf8_body_reads': 2,
                      'receipt_sha256': digest(json_bytes(result))}))


if __name__ == '__main__':
    main()
