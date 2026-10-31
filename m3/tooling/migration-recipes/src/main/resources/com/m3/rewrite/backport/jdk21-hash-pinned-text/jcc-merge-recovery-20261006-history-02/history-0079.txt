#!/usr/bin/env python3
"""Inspect additive actual raw transport failure; no remote or owner execution."""
from pathlib import Path
import base64
import hashlib
import json

OUT = Path(__file__).resolve().parent
PUB = OUT.parents[1]
ROOT = PUB.parents[2]
READS = {}


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def git_blob(raw):
    return hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()


def read(path):
    path = Path(path).resolve()
    raw = path.read_bytes()
    row = {'path': str(path), 'bytes': len(raw), 'sha256': sha(raw)}
    assert path not in READS or READS[path] == row, str(path)
    READS[path] = row
    return raw


def obj(path):
    return json.loads(read(path))


def main():
    original = PUB / 'readback-static-review/raw-transport-adapter'
    old_seal = obj(original / 'FILE_SEALS.json')
    assert sha(read(original / 'FILE_SEALS.json')) == '2102f85bd93143fd60431fd26a8069bdff2a5e4a2e6616deb6d6f934dc5191f8'
    for row in old_seal['files']:
        raw = read(row['path'])
        assert len(raw) == row['bytes'] and sha(raw) == row['sha256']
    failure = obj(PUB / 'RAW_READBACK_FAILURE_OBSERVATION.json')
    index = obj(PUB / 'epoch-01/transport/INDEX.json')
    manifest = obj(ROOT / 'work/source-merge-review/publication/FINAL_MANIFEST.json')
    assert sha(read(ROOT / 'work/source-merge-review/publication/FINAL_MANIFEST.json')) == index['manifest_sha256'] == '9270a6b4098960e2b7c7bd87b17325b648c4de363957f36028bacc9c1955e328'
    expected_rows = [row for row in manifest['rows'] if row['transport_encoding'] == 'base64']
    assert len(expected_rows) == len(index['binary_blobs']) == 4
    commit_id = '9dab34bd9e34e4d661004482248b5b02d94e0816'
    tree_id = 'bf2b8fe43cda47f0c6363c8d376f61419e647e4d'
    assert failure['verification_commit'] == commit_id
    assert failure['verification_tree_response']['structuredContent']['sha'] == tree_id
    assert failure['verification_commit_creation']['structuredContent']['sha'] == commit_id
    commit_response = failure['verification_commit_readback']
    assert commit_response['isError'] is False
    commit = json.loads(commit_response['structuredContent']['content'])
    assert commit['sha'] == commit_id and commit['tree']['sha'] == tree_id
    assert [row['sha'] for row in commit['parents']] == ['be92c62ece9023b5c33676716a1076d00e26120a']
    outcomes = []
    for request in failure['requests']:
        i = request['row_index']
        assert i in (0, 1)
        expected = expected_rows[i]
        prepared = index['binary_blobs'][i]
        local = read(expected['local_path'])
        assert local == base64.b64decode(prepared['content'], validate=True)
        assert len(local) == expected['bytes'] and sha(local) == expected['sha256']
        assert git_blob(local) == expected['git_blob'] == prepared['git_blob']
        assert request['ref'] == commit_id and request['repository_full_name'] == 'hsoliwal/com.synexia'
        assert request['encoding'] == 'base64'
        response = request['response']
        assert response['isError'] is False
        body = response['structuredContent']
        assert body['encoding'] == 'base64' and body['sha'] == expected['git_blob']
        assert body['display_url'] == 'https://github.com/hsoliwal/com.synexia/blob/' + commit_id + '/' + expected['path']
        returned = base64.b64decode(''.join(body['content'].split()), validate=True)
        outcomes.append({'row_index': i, 'path': expected['path'],
                         'reported_git_blob': body['sha'], 'expected_git_blob': git_blob(local),
                         'returned_bytes_git_blob': git_blob(returned),
                         'expected_bytes': len(local), 'returned_bytes': len(returned),
                         'expected_sha256': sha(local), 'returned_sha256': sha(returned),
                         'body_bytes_equal': returned == local, 'reported_sha_matches_expected': True,
                         'returned_bytes_match_reported_sha': git_blob(returned) == body['sha'],
                         'expected_base64': base64.b64encode(local).decode(),
                         'returned_base64_normalized': base64.b64encode(returned).decode()})
        if i == 1:
            assert local == bytes.fromhex('23c3280a')
            assert returned == bytes.fromhex('23efbfbd280a')
            assert returned != local and git_blob(returned) != body['sha']
        else:
            assert returned == local
    assert [row['row_index'] for row in outcomes] == [0, 1]
    read(OUT / 'READ_CAPABILITIES.json')
    facts = {
        'schema': 'independent-actual-raw-readback-failure/v1',
        'status': 'ACTUAL_REQUIRED_RAW_READBACK_FAILED',
        'failure_observation_sha256': sha(read(PUB / 'RAW_READBACK_FAILURE_OBSERVATION.json')),
        'conditional_prior_admission_preserved': True,
        'actual_raw_gate_passed': False,
        'verification_commit': commit_id,
        'verification_tree': tree_id,
        'verification_parent': commit['parents'][0]['sha'],
        'retained_response_outcomes': outcomes,
        'awaited_response_indexes_not_retained': [2, 3],
        'unretained_responses_are_not_claimed_unrun_or_successful': True,
        'publication_activity_limit_from_root_observation': failure['limit'],
        'synthesized_successful_raw_body_entries': 0,
        'repo_source_fixture_manifest_verifier_changes': 0,
        'reviewer_remote_reads_or_writes': 0,
        'alternative_custody': {
            'status': 'CONCEPT_JUDGMENT_ONLY_PENDING_FROZEN_V3_REVIEW',
            'proposed_identity_only_git_blobs': ['59b4ec83f687499a54f15695b9b246599f9579d7', '24cdc62e634934527d551324c2449bcf951b5589'],
            'mandatory_direct_readbacks': 6,
            'basis': 'Captured exact base64 create request bytes and returned Git object identities plus complete immutable verification/final tree membership; ordinary Git/GitHub object identity assumptions.',
            'limits': 'Does not recover the two remote byte arrays or establish a remotely observed SHA-256; must remain separately labeled and cannot satisfy the original V2 body gate by fabricated normalization.'
        },
        'primary_documents_consulted': [
            'https://git-scm.com/docs/git-hash-object',
            'https://docs.github.com/en/rest/git/blobs'
        ]
    }
    inputs = sorted(READS.values(), key=lambda row: row['path'])
    for row in inputs:
        data = Path(row['path']).read_bytes()
        assert len(data) == row['bytes'] and sha(data) == row['sha256']
    (OUT / 'REVIEW.json').write_text(json.dumps(facts, indent=2) + '\n')
    (OUT / 'INPUT_SEALS.json').write_text(json.dumps({'schema': 'independent-read-input-seals/v1', 'count': len(inputs),
                                                    'all_inputs_reread_unchanged': True, 'files': inputs}, indent=2) + '\n')
    print(json.dumps({'status': facts['status'], 'retained_responses': len(outcomes),
                      'byte_passes': sum(row['body_bytes_equal'] for row in outcomes),
                      'byte_failures': sum(not row['body_bytes_equal'] for row in outcomes),
                      'mutated_blob_identity': outcomes[1]['returned_bytes_git_blob'],
                      'stable_inputs': len(inputs), 'review_sha256': sha((OUT / 'REVIEW.json').read_bytes())}))


if __name__ == '__main__':
    main()
