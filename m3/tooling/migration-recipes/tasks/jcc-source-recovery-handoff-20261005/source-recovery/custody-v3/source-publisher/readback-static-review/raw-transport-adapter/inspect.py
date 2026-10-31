#!/usr/bin/env python3
"""Read-only raw transport plan arithmetic; no project or transport execution."""
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


def git_object(kind, raw):
    return hashlib.sha1(kind.encode() + b' ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()


def read(path):
    path = Path(path).resolve()
    raw = path.read_bytes()
    row = {'path': str(path), 'bytes': len(raw), 'sha256': sha(raw)}
    assert path not in READS or READS[path] == row, str(path)
    READS[path] = row
    return raw


def obj(path):
    return json.loads(read(path))


def tree_id(entries):
    ordered = sorted(entries, key=lambda row: row['path'].encode() + (b'/' if row['type'] == 'tree' else b''))
    raw = b''.join(row['mode'].lstrip('0').encode() + b' ' + row['path'].encode() + b'\0'
                   + bytes.fromhex(row['sha']) for row in ordered)
    return git_object('tree', raw)


def main():
    pins = {
        'RAW_TRANSPORT_ADAPTER.md': 'c12b3bb6415f89a94e946d8937b50e9f974e6bcf79f4f6ecdb388dc1cdfa16c6',
        'verify_publication.py': '8cc5fd5c5df99ae8f48d0fbbf4618ada99be429af9cb39839511940192bd8639',
        'prepare_publication.py': '3cd42b4b5c4ba1e0eb01cace40bff7cb2fe967d65ac4e9a646b70d7daf4404fd',
        'audit_base.py': '839c35c611de2dc02f2a3f1233131b24c79d77e109aaf010df9689dc8e04893b',
        'BASE_TREE_MODEL.json': '877a61dc6d9b4f4fa280faeadbd67db9ee3d56a68768fa06c917d85ab8fbaa80',
        'RAW_VERIFICATION_TREE_PLAN.json': '00fa5b9e4e99621ed6b5aac3c0da4c6506f82e921a3da42fdd4645285653e319',
        'RAW_TOOL_CAPABILITY_OBSERVATION.json': 'fba19b92e8b20eb4ef2ba2c6dbc6d48d5bce01eec5763362f62324fe27ab410a',
        'RAW_BLOB_CREATIONS.json': '1213f1a8c1da0a7032f1dc9507c5aef152eaf94ea39db0212f3684be06778a5a'
    }
    for name, digest in pins.items():
        assert sha(read(PUB / name)) == digest, name
    capability = obj(OUT / 'FETCH_FILE_CAPABILITY.json')
    assert 'encoding?: "utf-8" | "base64"' in capability['description']
    observation = obj(PUB / 'RAW_TOOL_CAPABILITY_OBSERVATION.json')
    for key in ['fetch_git_blob_response', 'fetch_blob_response']:
        response = observation[key]
        assert response['isError'] is False
        assert isinstance(response['structuredContent']['content'], str)
        assert 'encoding' not in response['structuredContent']
    assert observation['initial_creation']['create_response_captured'] is False
    assert observation['initial_creation']['tree_ref_or_pr_created'] is False
    probe = observation['base64_file_response']['structuredContent']
    assert probe['encoding'] == 'base64' and 'size' not in probe
    encoded = ''.join(probe['content'].split())
    probe_bytes = base64.b64decode(encoded, validate=True)
    assert git_object('blob', probe_bytes) == probe['sha']
    expected_probe = ROOT / 'recovery-baseline-be92/synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3MasteryClassLoader.java'
    assert probe_bytes == read(expected_probe)
    assert '/be92c62ece9023b5c33676716a1076d00e26120a/' in probe['display_url']

    index = obj(PUB / 'epoch-01/transport/INDEX.json')
    manifest_path = ROOT / 'work/source-merge-review/publication/FINAL_MANIFEST.json'
    manifest_raw = read(manifest_path)
    assert sha(manifest_raw) == index['manifest_sha256'] == '9270a6b4098960e2b7c7bd87b17325b648c4de363957f36028bacc9c1955e328'
    manifest = json.loads(manifest_raw)
    raw_rows = [row for row in manifest['rows'] if row['transport_encoding'] == 'base64']
    by_blob = {row['git_blob']: row for row in raw_rows}
    assert len(raw_rows) == len(by_blob) == len(index['binary_blobs']) == 4
    sizes = {}
    for row in index['binary_blobs']:
        expected = by_blob[row['git_blob']]
        raw = base64.b64decode(row['content'], validate=True)
        assert raw == read(expected['local_path'])
        assert len(raw) == row['bytes'] == expected['bytes']
        assert sha(raw) == row['sha256'] == expected['sha256']
        assert git_object('blob', raw) == row['git_blob']
        sizes[row['git_blob']] = len(raw)
    creations = obj(PUB / 'RAW_BLOB_CREATIONS.json')
    assert creations['branch_or_ref_created'] is False and len(creations['records']) == 4
    assert {row['git_blob'] for row in creations['records']} == set(by_blob)
    for row in creations['records']:
        expected = by_blob[row['git_blob']]
        response = row['response']
        assert response['isError'] is False
        assert response['structuredContent']['sha'] == row['returned_sha'] == row['git_blob']
        assert row['bytes'] == expected['bytes'] and row['sha256'] == expected['sha256']

    plan = obj(PUB / 'RAW_VERIFICATION_TREE_PLAN.json')
    model = obj(PUB / 'BASE_TREE_MODEL.json')
    assert plan['branch_or_ref'] is None
    assert plan['base_commit'] == model['commit'] == index['expected_parent'] == 'be92c62ece9023b5c33676716a1076d00e26120a'
    assert plan['base_tree'] == model['root'] == index['base_tree'] == '3c4f32b66633a251ba2c117090830252a7e2da03'
    expected_entries = [{'path': row['path'], 'mode': row['mode'], 'type': 'blob', 'sha': row['git_blob']} for row in raw_rows]
    assert plan['entries'] == expected_entries
    trie = {}
    for entry in plan['entries']:
        node = trie
        parts = entry['path'].split('/')
        for part in parts[:-1]:
            node = node.setdefault(part, {})
        node[parts[-1]] = {'leaf': {**entry, 'path': parts[-1], 'size': sizes[entry['sha']]}}
    changes = []
    def build(before, branch, prefix):
        original = model['trees'][before] if before is not None else []
        if before is not None:
            assert tree_id(original) == before
        entries = {entry['path']: dict(entry) for entry in original}
        for name, node in sorted(branch.items()):
            if 'leaf' in node:
                assert name not in entries
                entries[name] = node['leaf']
            else:
                old = entries.get(name)
                assert old is None or old['type'] == 'tree'
                child = build(old['sha'] if old else None, node, prefix + '/' + name if prefix else name)
                entries[name] = {'path': name, 'mode': '040000', 'type': 'tree', 'sha': child}
        final = sorted(entries.values(), key=lambda entry: entry['path'])
        after = tree_id(final)
        changes.append({'path': prefix, 'before': before, 'after': after, 'entries': final,
                        'unchanged_sibling_entries': sum(entry['path'] not in branch for entry in original)})
        return after
    tree = build(model['root'], trie, '')
    assert tree == plan['expected_tree'] == 'bf2b8fe43cda47f0c6363c8d376f61419e647e4d'
    assert len(changes) == len(plan['changed_trees']) == 17
    assert {row['path']: row for row in changes} == {row['path']: row for row in plan['changed_trees']}
    assert index['expected_tree'] == '0361e4a07b77a01df170523033fef01c4052ddc5' and tree != index['expected_tree']

    companion = ROOT / 'work/source-merge-review/execution-static-review/current-owner-upstream-review/raw-transport-normalization-review'
    seal = obj(companion / 'FILE_SEALS.json')
    for row in seal['files']:
        raw = read(row['path'])
        assert len(raw) == row['bytes'] and sha(raw) == row['sha256']
    inputs = sorted(READS.values(), key=lambda row: row['path'])
    for row in inputs:
        raw = Path(row['path']).read_bytes()
        assert len(raw) == row['bytes'] and sha(raw) == row['sha256']
    facts = {
        'schema': 'independent-raw-transport-adapter-admission/v1',
        'status': 'ADMITTED_FOR_EXACT_CONTENTS_VERIFICATION',
        'adapter_sha256': pins['RAW_TRANSPORT_ADAPTER.md'],
        'manifest_sha256': index['manifest_sha256'],
        'final_expected_tree': index['expected_tree'],
        'verification_expected_tree': tree,
        'verification_changed_trees': len(changes),
        'verification_entries': expected_entries,
        'only_four_raw_additions_to_exact_base': True,
        'all_other_base_entries_preserved': True,
        'actual_create_responses_match_four_expected_blobs': True,
        'historic_first_creation_response_uncaptured': True,
        'current_all_four_creation_responses_retained': True,
        'base64_capability_probe': {'sha': probe['sha'], 'bytes': len(probe_bytes), 'sha256': sha(probe_bytes),
                                    'exact_immutable_baseline_body_equal': True,
                                    'raw_fixture_verification': False},
        'raw_fixture_contents_readbacks_completed_in_this_review': False,
        'final_commit_or_branch_readback_completed_in_this_review': False,
        'v2_verifier_unchanged': True,
        'root_responsibilities': [
            'Use explicit exact verification/final commit refs with full-file base64 reads, without line slicing.',
            'Preserve actual wrapper/request; normalized sha/encoding/content must come from that response.',
            'Label size as decoded-response-derived; never claim independently observed remote size.',
            'Verify all four raw bodies before any final-payload batch; verify all four raw and four canonical final bodies before branch creation.',
            'Use only be92 as the final commit parent; verification commit must not enter final ancestry.',
            'Retain original refused-envelope episode without promoting it to a successful raw readback.'
        ],
        'limits': [
            'This explicitly changes the old before-any-tree sequence by adding an unreferenced four-fixture verification tree/commit, while retaining before-final-payload and before-branch byte gates.',
            'V2 verifies normalized bytes/identities, not the provenance of their wrapper-to-entry copying or caller sequencing.',
            'Derived size is a consistency check; expected manifest/tree lengths, Git identity, SHA-256 and direct byte equality remain separate checks.',
            'No source/payload/manifest/script change, project execution, remote mutation or qualification promotion is performed by this reviewer.'
        ],
        'stable_read_inputs': len(inputs)
    }
    (OUT / 'REVIEW.json').write_text(json.dumps(facts, indent=2) + '\n')
    (OUT / 'INPUT_SEALS.json').write_text(json.dumps({'schema': 'independent-read-input-seals/v1', 'count': len(inputs),
                                                    'all_inputs_reread_unchanged': True, 'files': inputs}, indent=2) + '\n')
    print(json.dumps({'status': facts['status'], 'verification_tree': tree, 'four_raw_entries': len(expected_entries),
                      'changed_trees': len(changes), 'stable_inputs': len(inputs),
                      'review_sha256': sha((OUT / 'REVIEW.json').read_bytes())}))


if __name__ == '__main__':
    main()
