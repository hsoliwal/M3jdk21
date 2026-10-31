"""Read-only E4 audit of actual materialized files; no recipe, build or remote calls.

Extends the scope of the preserved E3 check_references.py with independently
reconstructed Git trees, current-context admission and explicit V3 custody limits.
Run only after the coordinating owner authorizes completed E4 verification.
"""
from __future__ import annotations

import base64
import copy
import csv
import hashlib
import json
import re
import stat
import xml.etree.ElementTree as ET
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath

HERE = Path(__file__).resolve().parent
BASE = HERE.parent
ROOT = HERE / 'overlay'
OLD = BASE / 'successor-e03/overlay'
E2 = BASE / 'candidate'
SOURCE = '0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8'
SOURCE_ROOT = '0361e4a07b77a01df170523033fef01c4052ddc5'
INPUT = 'be92c62ece9023b5c33676716a1076d00e26120a'
INPUT_ROOT = '3c4f32b66633a251ba2c117090830252a7e2da03'
DEST = '0994ecd65e86600f417f4d8702838d8c2663af61'
DEST_ROOT = '7e8f39ac999174f8d5b6028d638a68d8c317b070'
OWNER = 'da958d00d24154c0db87beca0ec80a7df2b43b73'
REF = 'refs/heads/aix/jcc-source-merge-recovery-20261005'
IDS = ('synexia.jcc-recipe-laboratory', 'synexia.jcc-java-jni-regression')
NAME = 'jcc-source-recovery-handoff-20261005'
TASK = PurePosixPath('m3/tooling/migration-recipes/tasks') / NAME
CRATE = ROOT / 'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text' / NAME
MAP = 'm3/docs/name-mapping.json'
DOC = 'm3/docs/jcc-source-handoff.md'
BIND = 'm3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json'
COV = 'm3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv'
FIELD_HASH = '7b5e197da5e2645efb87345e2abf92dcd8071993ff308c8fa0bca0511d00c52e'
ROOT_HASH = '55610f52b7b1e01ee15999969cc385616ff2e43f734e589ff61c750c5ab3c845'
CONTEXT_HASH = 'fbc460874aeb04a688ad0ed9b47ad8e3f3b37f88af09d0f32ecfeedc2827fbed'
PUBLICATION_HASH = '95f325ce57dadab60031f41514d9a9c5fa3ef6eb29a41e3c9a8c81e2dee996b8'
EXPECTATIONS_HASH = '548b98ef8531b369f4c378bfdebf681e514a5b46da15bcf32db9ab491a7ce8d5'


def load(path):
    return json.loads(path.read_bytes())


def body_identity(body):
    return {'bytes': len(body), 'sha256': hashlib.sha256(body).hexdigest(),
            'git_blob_sha1': hashlib.sha1(f'blob {len(body)}\0'.encode() + body).hexdigest()}


def ident(path):
    assert path.is_absolute() and path.resolve(strict=True) == path, path
    assert stat.S_ISREG(path.lstat().st_mode) and not path.is_symlink(), path
    before = path.stat()
    body = path.read_bytes()
    after = path.stat()
    assert (before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns, before.st_ctime_ns, before.st_mode) == (
        after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns, after.st_ctime_ns, after.st_mode), path
    return {**body_identity(body), 'mode': oct(after.st_mode & 0o777)}


def check(path, ref):
    got = ident(path)
    assert 'sha256' in ref, ('Unbound file reference', path)
    for key in ('bytes', 'sha256', 'git_blob_sha1'):
        if key in ref:
            assert got[key] == ref[key], (path, key, got[key], ref[key])
    if 'git_blob' in ref:
        assert got['git_blob_sha1'] == ref['git_blob'], path
    if 'mode' in ref:
        expected = ref['mode']
        actual = ('100755' if path.stat().st_mode & 0o111 else '100644') if expected in ('100644', '100755') else got['mode']
        assert actual == expected, (path, 'mode', actual, expected)
    return got


def local_ref(ref):
    path = Path(ref['local_path'])
    check(path, ref)
    return path


def relative(name):
    path = PurePosixPath(name)
    assert name and '\\' not in name and not path.is_absolute() and path.as_posix() == name, name
    assert all(part not in ('', '.', '..') for part in path.parts), name
    return path


def unique(rows, key='path'):
    result = {row[key]: row for row in rows}
    assert len(result) == len(rows), ('Duplicate identities', key)
    return result


def git_tree_id(tree):
    assert tree['truncated'] is False
    entries = tree['tree']
    names = [entry['path'] for entry in entries]
    assert len(names) == len(set(names))
    assert all(name and '/' not in name and '\0' not in name and name not in ('.', '..') for name in names)
    body = bytearray()
    for entry in sorted(entries, key=lambda row: (row['path'] + ('/' if row['type'] == 'tree' else '')).encode('utf-8')):
        assert re.fullmatch('[0-9a-f]{40}', entry['sha'])
        mode = entry['mode']
        assert (entry['type'], mode) in {('tree', '040000'), ('commit', '160000'),
                                        ('blob', '100644'), ('blob', '100755'), ('blob', '120000')}
        body.extend((('40000' if mode == '040000' else mode) + ' ' + entry['path']).encode('utf-8'))
        body.extend(b'\0' + bytes.fromhex(entry['sha']))
    return hashlib.sha1(f'tree {len(body)}\0'.encode() + body).hexdigest()


def normalized_entries(tree):
    return sorted(({key: row[key] for key in ('path', 'mode', 'type', 'sha')} for row in tree['tree']), key=lambda row: row['path'])


def main():
    if not __debug__:
        raise RuntimeError('Optimized Python disables required reference assertions')
    out = HERE / 'reference-audit-v3'
    assert not out.exists(), 'Use an additive audit epoch; never overwrite evidence'
    final = load(HERE / 'FINAL_SOURCE_INPUTS_V3.json')
    assert final['state'] == 'PUBLISHED_CUSTODY_VERIFIED'
    assert (final['source_commit'], final['source_root_tree'], final['source_ref']) == (SOURCE, SOURCE_ROOT, REF)
    assert final['source_qualification_reviewed'] is True and final['promote_capability'] is False
    fields_path = local_ref(final['source_inventory'])
    roots_path = local_ref(final['root_accounting'])
    assert check(fields_path, {'sha256': FIELD_HASH}) and check(roots_path, {'sha256': ROOT_HASH})
    fields, roots = load(fields_path), load(roots_path)
    publication_path = local_ref(final['publication_receipt'])
    check(publication_path, {'sha256': PUBLICATION_HASH})
    publication = load(publication_path)
    assert publication['status'] == 'PUBLISHED_CUSTODY_VERIFIED'
    assert publication['repository'] == 'hsoliwal/com.synexia'
    assert (publication['commit'], publication['tree'], publication['parent']) == (SOURCE, SOURCE_ROOT, INPUT)
    assert publication['branch_commit_parent_tree_pr_verified'] is True and publication['ref_update_forced'] is False
    assert final['source_pr'] == publication['pull_request']['url']

    # Reconstruct actual recorded Git objects; a format-valid digest is insufficient.
    assert fields['tree_receipts'] == roots['tree_receipts'] and len(fields['tree_receipts']) == 2
    trees = {}
    for receipt in fields['tree_receipts']:
        document = load(local_ref(receipt))
        if document['schema'] == 'jcc-merge-publication-base-tree-model/1':
            assert (document['commit'], document['root']) == (INPUT, INPUT_ROOT)
            supplied = {oid: {'sha': oid, 'tree': entries, 'truncated': False} for oid, entries in document['trees'].items()}
        else:
            assert document['schema'] == 'jcc-source-recovery-remote-readback/1'
            remote = document
            assert document['commit']['sha'] == SOURCE and document['commit']['tree']['sha'] == SOURCE_ROOT
            assert [p['sha'] for p in document['commit']['parents']] == [INPUT]
            assert document['ref']['ref'] == REF and document['ref']['object']['sha'] == SOURCE
            assert document['pull_request']['head']['sha'] == SOURCE
            assert document['pull_request']['html_url'] == final['source_pr']
            check(Path(publication['readback_path']), {'sha256': publication['readback_sha256']})
            assert Path(publication['readback_path']) == Path(receipt['local_path'])
            supplied = document['trees']
        for oid, tree in supplied.items():
            assert tree['sha'] == oid == git_tree_id(tree)
            if oid in trees:
                assert normalized_entries(trees[oid]) == normalized_entries(tree)
            trees[oid] = tree
    assert SOURCE_ROOT in trees

    def entry(path, expected_chain=None):
        parts = relative(path).parts
        at, chain = SOURCE_ROOT, []
        for index, name in enumerate(parts):
            assert at in trees, ('Missing exact ancestor', path, at)
            found = [row for row in trees[at]['tree'] if row['path'] == name]
            assert len(found) == 1, (path, name)
            row = found[0]
            chain.append({'tree': at, 'name': name, 'entry': row})
            if index + 1 < len(parts):
                assert row['type'] == 'tree'
                at = row['sha']
        if expected_chain is not None:
            assert len(expected_chain) == len(chain), path
            for recorded, actual in zip(expected_chain, chain):
                assert recorded['tree'] == actual['tree'] and recorded['name'] == actual['name']
                assert all(recorded['entry'][key] == actual['entry'][key] for key in ('path', 'mode', 'type', 'sha'))
                if 'size' in recorded['entry']:
                    assert recorded['entry']['size'] == actual['entry']['size']
        return row

    def source_body(row):
        got = check(local_ref(row), row)
        assert row['commit'] == SOURCE
        found = entry(row['path'], row['final_tree_path_chain'])
        assert found['type'] == 'blob' and found['mode'] == row['mode']
        assert found['sha'] == got['git_blob_sha1']
        if 'size' in found:
            assert found['size'] == got['bytes']
        return got

    inventory = unique(fields['sources'])
    proof_inventory = unique(fields['proof_references'])
    assert len(inventory) == 19 and len(proof_inventory) == 55
    assert (fields['source_commit'], fields['source_root_tree'], fields['tracking_ref']) == (SOURCE, SOURCE_ROOT, REF)
    assert (fields['source_input_commit'], fields['source_input_root']) == (INPUT, INPUT_ROOT)
    source_identities = {name: source_body(row) for name, row in inventory.items()}
    proof_identities = {name: source_body(row) for name, row in proof_inventory.items()}
    published_entries = unique(publication['tree_entries_verified'])
    for name, row in proof_inventory.items():
        check(Path(row['local_path']), published_entries[name])
        assert entry(name)['sha'] == published_entries[name]['git_blob']

    # Explicitly retain the narrow transport distinction: 21 direct reads, two exceptions.
    expectations = load(HERE / 'custody-v3-proposal/LOCAL_EXPECTATIONS.json')
    check(HERE / 'custody-v3-proposal/LOCAL_EXPECTATIONS.json', {'sha256': EXPECTATIONS_HASH})
    custody = fields['source_publication']['custody']
    assert custody == final['source_publication_custody']
    assert fields['source_publication']['publication_status'] == custody['status'] == publication['status']
    for key, value in {'required_blob_bodies_compared_byte_for_byte': 6,
                       'required_blob_identities_verified_without_body_readback': 2,
                       'production_blob_bodies_compared_byte_for_byte': 4,
                       'nul_fixture_bodies_compared_byte_for_byte': 2,
                       'malformed_utf8_body_readback': False, 'all_required_blob_bodies_read_back': False}.items():
        assert publication[key] == custody[key] == value, key
    assert custody['observed_direct_body_readbacks'] == len(custody['blob_body_readbacks']) == 21
    assert custody['blob_body_readbacks'] == publication['blob_body_readbacks']
    assert custody['raw_fixture_custody'] == publication['raw_fixture_custody']
    assert custody['raw_fixture_custody_input'] == publication['raw_fixture_custody_input']
    mandatory = expectations['mandatory_direct_production_bodies'] + expectations['mandatory_direct_NUL_fixture_bodies']
    exceptions = expectations['exact_content_addressed_invalid_utf8_exceptions']
    assert len(mandatory) == 6 and len(exceptions) == 2
    allowed_bodies = {row['git_blob_sha1']: row for row in list(inventory.values()) + expectations['mandatory_direct_NUL_fixture_bodies']}
    direct = unique(publication['blob_body_readbacks'], 'git_blob')
    assert set(direct) == set(allowed_bodies) and {row['git_blob_sha1'] for row in mandatory} <= set(direct)
    observed = unique(remote['blobs'], 'sha')
    assert set(observed) == set(direct)
    for oid, row in observed.items():
        assert row['encoding'] == 'base64' and row['request']['ref'] == SOURCE
        expected = allowed_bodies[oid]
        assert row['request']['path'] == expected['path']
        assert row['request']['repository_full_name'] == 'hsoliwal/com.synexia'
        response = row['response']['structuredContent']
        assert row['response'].get('isError') is not True and response['sha'] == oid
        assert response['content'] == row['content'] and response['encoding'] == 'base64'
        body = base64.b64decode(''.join(row['content'].split()), validate=True)
        assert body == Path(expected['local_path']).read_bytes()
        actual = body_identity(body)
        assert actual == {key: direct[oid][key if key != 'git_blob_sha1' else 'git_blob'] for key in actual}
        assert entry(expected['path'])['sha'] == oid
    raw = custody['raw_fixture_custody']
    assert raw['identity_only_allowlist'] == sorted(row['git_blob_sha1'] for row in exceptions)
    assert not set(raw['identity_only_allowlist']) & set(direct)
    raw_rows = unique(raw['blob_identity_custody'])
    assert set(raw_rows) == {row['path'] for row in exceptions}
    for row in exceptions:
        check(local_ref(row), row)
        found, claimed = entry(row['path']), raw_rows[row['path']]
        assert found['sha'] == claimed['git_blob'] == claimed['creation_response_git_sha'] == row['git_blob_sha1']
        assert found['size'] == claimed['bytes'] == claimed['final_tree_size'] == row['bytes']
        assert claimed['sha256_local_uploaded_bytes'] == row['sha256']
        assert claimed['raw_body_readback'] is False and claimed['remote_sha256_observed'] is False
        assert claimed['method'] == 'BASE64_CREATE_AND_REMOTE_GIT_TREE_IDENTITY'
        for key in ('failed_probe_read', 'failed_final_read'):
            assert claimed[key]['raw_body_readback'] is False and claimed[key]['remote_sha256_observed'] is False
            assert claimed[key]['read_attempt'] == 'FAILED_UTF8_REENCODING'

    # E3 publication is the exact preimage authority; its 93 payloads stay intact except four outputs.
    e3_manifest_path = BASE / 'successor-e03/PUBLICATION_MANIFEST.json'
    check(e3_manifest_path, {'sha256': '590f2f8d9c021d8f416e1f8682f1cac83f795967e9f1e0d3980a2bc4144ac815'})
    e3_manifest = load(e3_manifest_path)
    e3_publication_path = BASE.parent / 'publication-current/m3-e3-publish/PUBLICATION_VERIFIED.json'
    check(e3_publication_path, {'sha256': 'cc5b762699bff718d2e3ba120ba475c2d1e5b78e88202adf907a5d2693b742f1'})
    e3_publication = load(e3_publication_path)
    assert (e3_publication['commit'], e3_publication['tree']) == (DEST, DEST_ROOT)
    assert e3_publication['manifest_sha256'] == ident(e3_manifest_path)['sha256']
    e3 = unique(e3_manifest['files'])
    published_e3 = unique(e3_publication['files'])
    assert set(e3) == set(published_e3) and len(e3) == 93
    plan = load(CRATE / 'plan.json')
    canonical = (json.dumps({key: value for key, value in plan.items() if key != 'plan_sha256'}, sort_keys=True, separators=(',', ':'), ensure_ascii=True) + '\n').encode()
    assert hashlib.sha256(canonical).hexdigest() == plan['plan_sha256']
    assert (plan['source_commit'], plan['target_commit']) == (SOURCE, DEST)
    outputs = unique(plan['outputs'])
    assert set(outputs) == {MAP, DOC, BIND, COV}
    preserved_e3, materialized = [], []
    for path, row in e3.items():
        check(OLD / path, row)
        check(OLD / path, published_e3[path])
        if path not in outputs:
            got = check(ROOT / path, row)
            assert (ROOT / path).read_bytes() == (OLD / path).read_bytes()
            preserved_e3.append({'path': path, **got})
    assert len(preserved_e3) == 89
    e2_manifest_path = BASE / 'PUBLICATION_MANIFEST.json'
    check(e2_manifest_path, {'sha256': '7facad2750623bdfb10b16bdcfb9b8b508d94297fd3b9d90f7a37f6a19694a6d'})
    e2 = unique(load(e2_manifest_path)['files'])
    preserved_e2 = []
    for path, row in e2.items():
        check(E2 / path, row)
        if path not in outputs:
            got = check(ROOT / path, row)
            assert (ROOT / path).read_bytes() == (E2 / path).read_bytes()
            preserved_e2.append({'path': path, **got})
    assert len(preserved_e2) == 56
    for path, row in outputs.items():
        assert row['before'] is not None
        before = CRATE / relative(row['before']['resource'])
        after = CRATE / relative(row['after']['resource'])
        assert before.read_bytes() == (OLD / path).read_bytes()
        check(before, row['before'])
        assert after.read_bytes() == (ROOT / path).read_bytes()
        check(after, row['after'])
        materialized.append({'path': path, 'preimage': ident(before), 'postimage': ident(after)})

    previous, current = load(OLD / MAP), load(ROOT / MAP)
    before, rows = unique(previous['migration']['records'], 'id'), unique(current['migration']['records'], 'id')
    assert list(before) == list(rows) and len(rows) == 46
    assert {key: value for key, value in previous.items() if key != 'migration'} == {key: value for key, value in current.items() if key != 'migration'}
    assert {key: value for key, value in previous['migration'].items() if key != 'records'} == {key: value for key, value in current['migration'].items() if key != 'records'}
    assert len(current['migration']['gates']) == 20
    for rid in set(rows) - set(IDS):
        assert rows[rid] == before[rid], rid
    old_binding, binding = load(OLD / BIND), load(ROOT / BIND)
    assert (binding['source_commit'], binding['source_root_tree'], binding['destination_commit'], binding['destination_root_tree']) == (SOURCE, SOURCE_ROOT, DEST, DEST_ROOT)
    for key in ('descriptor_reconciliation', 'independent_receiving_fixture', 'existing_destination_reuse_artifacts', 'acceptance'):
        assert binding[key] == old_binding[key], key
    assert all(value is False for value in binding['acceptance'].values())
    assert binding['source_execution'] == fields['source_execution']
    assert {key: value for key, value in binding['source_publication'].items() if key != 'receipts'} == fields['source_publication']
    assert binding['source_publication']['source_gate_or_export_authority_granted'] is False
    execution = binding['source_execution']
    assert execution['input_remote_revision'] == INPUT and execution['input_remote_root_tree'] == INPUT_ROOT
    assert execution['published_output_revision'] == SOURCE and execution['qualification_status'] == 'BLOCKED'
    assert execution['source_export_admitted'] is False and execution['destination_gates_passed'] is False
    assert execution['independent_native']['tests'] == 19 and execution['independent_native']['aggregate_promoted'] is False
    assert execution['upstream']['classification'] == 'EXECUTED_FAILED' and execution['upstream']['tests'] == 67
    assert (execution['upstream']['passed'], execution['upstream']['errors']) == (66, 1)
    assert execution['selected_input_files'] == 527 and execution['compiled_main_sources'] == 446
    source_occurrences, history = [], []
    for rid in IDS:
        row, old = rows[rid], before[rid]
        assert row['status'] == 'blocked' and row['tests'] == []
        assert row['lineage']['previous_sources'] == old['lineage']['previous_sources'] + old['sources']
        assert row['targets'] == old['targets']
        for key in ('previous_targets', 'supersedes'):
            assert row['lineage'][key] == old['lineage'][key]
        history.extend(row['lineage']['previous_sources'])
        expected = [item for item in fields['sources'] if item['record'] == rid]
        assert [item['path'] for item in row['sources']] == [item['path'] for item in expected]
        for index, ref in enumerate(row['sources']):
            inv = inventory[ref['path']]
            assert ref == {key: inv[key] for key in ('repo', 'commit', 'module', 'path', 'symbol', 'signatures', 'sha256', 'git_blob_sha1', 'fingerprint', 'revision_role', 'tracking_ref')}
            source_occurrences.append({'reference': f'{rid}.sources[{index}]', 'path': ref['path'], **source_identities[ref['path']], 'commit': SOURCE, 'basis': 'REHASHED_FINAL_GIT_ROOT_PATH_AND_EXACT_LOCAL_BODY'})
    assert [len(rows[rid]['sources']) for rid in IDS] == [13, 6]
    assert len(history) == 21 and Counter(row['commit'] for row in history) == {'0b8dc32b9e8a616b7b7141bbdd88722839dc64bc': 4, 'd1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906': 17}
    assert binding['source_artifacts'] == [row for rid in IDS for row in rows[rid]['sources']]
    assert len([row for rid in IDS for row in rows[rid]['targets']]) == 6
    assert binding['prior_handoff']['commit'] == DEST and binding['prior_handoff']['root_tree'] == DEST_ROOT
    prior_binding = ROOT / relative(binding['prior_handoff']['old_binding_resource'])
    check(prior_binding, {'sha256': binding['prior_handoff']['old_binding_sha256']})
    assert prior_binding.read_bytes() == (OLD / BIND).read_bytes()

    # Every new copied receipt has an exact origin, canonical key and matching binding.
    reserved = {'PUBLICATION_CUSTODY.json': final['publication_receipt'],
                'SOURCE_FIELDS_AND_PROOFS.json': final['source_inventory'], 'ROOT_ACCOUNTING.json': final['root_accounting']}
    assert not set(reserved) & set(final['additional_receipts'])
    assert 'PUBLICATION_READBACK.json' not in final['additional_receipts']
    origin_refs = {**reserved, **final['additional_receipts']}
    receipt_refs = binding['source_publication']['receipts']
    assert set(receipt_refs) == set(origin_refs)
    copied_receipts = []
    for name, origin in origin_refs.items():
        relative(name)
        expected_path = str(TASK / 'source-recovery' / name)
        ref = receipt_refs[name]
        assert ref['path'] == expected_path
        got = check(ROOT / expected_path, ref)
        assert (ROOT / expected_path).read_bytes() == local_ref(origin).read_bytes()
        copied_receipts.append({'name': name, 'path': expected_path, 'origin': str(local_ref(origin)), **got})
    declared_paths = {str(ROOT / row['path']) for row in receipt_refs.values()}
    observed_paths = {str(path) for path in (ROOT / TASK / 'source-recovery').rglob('*') if path.is_file()}
    assert declared_paths == observed_paths, 'Undeclared receiving source-recovery copy'

    acquired = unique(load(BASE / 'ACQUISITION_VALIDATION_MANIFEST.json')['files'])
    context = load(HERE / 'current-context/CONTEXT_REFERENCES.json')
    check(HERE / 'current-context/CONTEXT_REFERENCES.json', {'sha256': CONTEXT_HASH})
    assert (context['source_input_commit'], context['source_input_root']) == (INPUT, INPUT_ROOT)
    assert context['source_output_commit'] is None and context['source_export_admitted'] is False
    current_context = unique(context['current_context'])
    assert len(current_context) == context['current_context_count'] == 527
    for row in current_context.values():
        assert row['repo'] == 'hsoliwal/com.synexia' and row['commit'] == INPUT
        check(local_ref(row), row)
    current_receipts = {row['path']: row for row in context['receipts'].values()}
    assert len(current_receipts) == 10
    expected_current = {'input_commit': INPUT, 'input_root_tree': INPUT_ROOT, 'designated_binding_count': 19,
                        'original_source_packet_count': 503, 'current_context_count': 527, 'current_main_explicit_count': 446,
                        'current_parent_count': 109, 'current_parent_main': 88, 'current_parent_release8_vendor': 19,
                        'current_parent_test_classes': 2, 'receipts': context['receipts'],
                        'explicit_context_artifacts': context['explicit_context_artifacts']}
    for key, value in expected_current.items():
        assert binding['source_current_context'][key] == value, key
    references = []

    def dest(ref, location):
        path = str(relative(ref['path']))
        got = check(ROOT / path, ref)
        if path in outputs:
            check(ROOT / path, outputs[path]['after'])
            basis = 'E4_ACTUAL_MATERIALIZED_AFTERIMAGE'
        elif path in e3:
            check(ROOT / path, e3[path])
            basis = 'PRESERVED_PUBLISHED_E3_PAYLOAD'
        elif path in e2:
            check(ROOT / path, e2[path])
            basis = 'PRESERVED_PUBLISHED_E2_PAYLOAD'
        elif path in acquired:
            check(ROOT / path, acquired[path])
            assert (ROOT / path).read_bytes() == Path(acquired[path]['local_path']).read_bytes()
            basis = 'PINNED_DA958_ACQUISITION'
        elif path in current_receipts:
            check(ROOT / path, current_receipts[path])
            basis = 'PINNED_CURRENT_INPUT_RECEIPT_COPY'
        else:
            assert path in {row['path'] for row in receipt_refs.values()}, ('Unclassified receiving reference', location, path)
            basis = 'EXACT_E4_SOURCE_RECOVERY_RECEIPT_COPY'
        references.append({'reference': location, 'path': path, **got, 'basis': basis})

    def nested_dest(value, location):
        if isinstance(value, dict):
            if 'path' in value and 'sha256' in value:
                dest(value, location)
            for key, child in value.items():
                nested_dest(child, location + '.' + key)
        elif isinstance(value, list):
            for index, child in enumerate(value):
                nested_dest(child, f'{location}[{index}]')

    old_plan = load(OLD / 'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-source-final-handoff-20261005/plan.json')
    assert plan['guards'] == old_plan['guards'] and len(plan['guards']) == 7
    for index, guard in enumerate(plan['guards']):
        dest(guard, f'plan.guards[{index}]')
    for rid in IDS:
        for index, ref in enumerate(rows[rid]['targets']):
            dest(ref, f'{rid}.targets[{index}]')
        dest(rows[rid]['recipe'], rid + '.recipe')
    for key in ('existing_destination_reuse_artifacts', 'independent_receiving_fixture'):
        nested_dest(binding[key], 'binding.' + key)
    for name, ref in receipt_refs.items():
        dest(ref, 'binding.source_publication.receipts.' + name)
    for name, ref in context['receipts'].items():
        dest(ref, 'binding.source_current_context.receipts.' + name)
    for path in (DOC, BIND, COV):
        dest({'path': path, 'sha256': outputs[path]['after']['sha256']}, 'blocked_record.observation')
    proofs = []
    assert len(execution['proofs']) == 55
    for index, ref in enumerate(execution['proofs']):
        inv = proof_inventory[ref['path']]
        expected = {key: inv[key] for key in ('path', 'commit', 'bytes', 'sha256', 'git_blob_sha1')}
        expected.update(repo='hsoliwal/com.synexia', url=f'https://github.com/hsoliwal/com.synexia/blob/{SOURCE}/{ref["path"]}')
        assert ref == expected
        proofs.append({'reference': f'binding.source_execution.proofs[{index}]', **ref, 'basis': 'REHASHED_FINAL_GIT_ROOT_PATH_AND_EXACT_LOCAL_BODY'})

    # Fresh POM parse and independently rehashed final root, not an inherited count.
    assert (roots['final_commit'], roots['final_root_tree']) == (SOURCE, SOURCE_ROOT)
    assert sorted(roots['root_entries'], key=lambda row: row['path']) == normalized_entries(trees[SOURCE_ROOT])
    root_entries = unique(roots['root_entries'])
    pom = roots['root_pom']
    pom_path = Path(pom['body_path'])
    check(pom_path, pom)
    pom_entry = entry('pom.xml', pom['final_tree_path_chain'])
    assert pom_entry['sha'] == pom['git_blob_sha1']
    xml, ns = ET.fromstring(pom_path.read_bytes()), {'m': 'http://maven.apache.org/POM/4.0.0'}
    assert xml.tag == '{' + ns['m'] + '}project'
    declarations = [{'path': (node.text or '').strip(), 'context': 'project'} for node in xml.findall('./m:modules/m:module', ns)]
    for profile in xml.findall('./m:profiles/m:profile', ns):
        profile_id = profile.findtext('m:id', default='', namespaces=ns).strip()
        assert profile_id
        declarations.extend({'path': (node.text or '').strip(), 'context': 'profile:' + profile_id} for node in profile.findall('./m:modules/m:module', ns))
    assert declarations == roots['maven_declarations'] and len(declarations) == 201
    assert all(row['path'] and root_entries[row['path'].split('/')[0]]['type'] == 'tree' for row in declarations)
    coverage = binding['source_root_accounting']
    dest({'path': coverage['coverage_receipt_path'], 'sha256': coverage['coverage_receipt_sha256']}, 'binding.source_root_accounting.coverage')
    with (ROOT / COV).open() as stream:
        coverage_rows = list(csv.DictReader(stream, delimiter='\t'))
    assert len(coverage_rows) == len(root_entries) == roots['root_entry_count'] == 343
    assert {row['root_path'] for row in coverage_rows} == set(root_entries)
    total_declarations = 0
    for row in coverage_rows:
        actual = root_entries[row['root_path']]
        assert (row['object_type'], row['mode'], row['object_id']) == (actual['type'], actual['mode'], actual['sha'])
        assert row['source_commit'] == SOURCE and row['source_tree'] == SOURCE_ROOT
        selected = [item for item in declarations if item['path'].split('/')[0] == row['root_path']]
        assert row['root_pom_declarations'] == ';'.join(item['path'] for item in selected)
        assert row['declaration_contexts'] == ';'.join(item['context'] for item in selected)
        for key in ('export_admitted', 'destination_materialized', 'read_back_delivered'):
            assert row[key] == 'false'
        total_declarations += len(selected)
    assert total_declarations == 201
    assert coverage['whole_repository_file_count'] is None and coverage['semantic_dependency_closure_complete'] is False

    # The execution receipt must exist before an audit can be issued; no test is rerun.
    verification = load(HERE / 'verification-v3/receipt.json')
    assert (verification['source_publication_commit'], verification['source_publication_root'], verification['destination_preimage_commit']) == (SOURCE, SOURCE_ROOT, DEST)
    assert verification['source_publication_custody'] == custody and verification['plan_sha256'] == plan['plan_sha256']
    for key in ('source_export_admitted', 'receiver_behavior_rerun', 'jdk_acceptance', 'remote_writes', 'source_qualification_transfer'):
        assert verification[key] is False
    assert verification['all_fixed_inputs_unchanged'] is True and verification['four_outputs_equal_sealed_afterimages'] is True
    assert verification['java_suite']['tests'] == '12' and verification['python_tests'] == 6
    assert all(verification['java_suite'][key] == '0' for key in ('failures', 'errors', 'skipped'))
    check(HERE / 'verification-v3/inputs.json', verification['inputs'])
    freeze = load(HERE / 'verification-v3/inputs.json')
    assert freeze['plan_sha256'] == plan['plan_sha256']
    frozen, stable = {}, []
    for group in ('fixed_inputs', 'external_source_and_proof_inputs'):
        for row in freeze[group]:
            assert row['path'] not in frozen, ('Duplicate frozen input', row['path'])
            got = check(Path(row['path']), row)
            assert got['mode'] == row['mode']
            frozen[row['path']] = row
            stable.append({'path': row['path'], **got, 'group': group})
    external = {row['path'] for row in freeze['external_source_and_proof_inputs']}
    assert {row['local_path'] for row in exceptions} <= external
    assert {row['local_path'] for row in current_context.values()} <= external
    assert {row['local_path'] for row in inventory.values()} <= external
    assert {row['local_path'] for row in proof_inventory.values()} <= external
    for row in exceptions:
        assert all(frozen[row['local_path']][key] == row[key] for key in ('bytes', 'sha256', 'git_blob_sha1'))
    for run in verification['runs']:
        assert run['exit_code'] == run['expected_exit_code']
        check(Path(run['stdout']['path']), run['stdout'])
        check(Path(run['stderr']['path']), run['stderr'])
    check(Path(verification['java_xml']['path']), verification['java_xml'])

    # Check exact carried originals and distinguish every explicitly omitted envelope.
    custody_index_result = check_custody_indexes(final, receipt_refs)
    result = {'schema': 'jcc-e4-explicit-reference-audit/1', 'observed_at_utc': datetime.now(timezone.utc).isoformat(),
              'result': 'PASS', 'scope': 'Read-only exact body, reconstructed Git-tree path, receiving preimage, lineage and frozen-input audit; separate from the ordinary blocked-row validator. No build, behavioral test or remote operation is performed.',
              'source_publication_commit': SOURCE, 'source_publication_root': SOURCE_ROOT,
              'destination_preimage_commit': DEST, 'destination_preimage_root': DEST_ROOT, 'destination_owner_commit': OWNER,
              'plan_sha256': plan['plan_sha256'], 'source_inventory': {'path': str(fields_path), **ident(fields_path)},
              'root_accounting': {'path': str(roots_path), **ident(roots_path)},
              'e3_preserved_payload_files': preserved_e3, 'e2_preserved_payload_files': preserved_e2,
              'four_materialized_afterimages': materialized, 'source_reference_occurrences': source_occurrences,
              'source_proof_reference_occurrences': proofs, 'destination_reference_occurrences': references,
              'destination_distinct_paths': len({row['path'] for row in references}),
              'destination_basis_counts': dict(Counter(row['basis'] for row in references)),
              'copied_receipts': copied_receipts, 'custody_evidence_index': custody_index_result,
              'rehash_verified_tree_objects': len(trees), 'ordered_records': 46, 'other_records_unchanged': 44,
              'gates_unchanged': 20, 'historical_source_objects_preserved': 21, 'historical_source_commit_counts': dict(Counter(row['commit'] for row in history)),
              'six_receiver_target_objects_preserved': True, 'receiver_fixture_and_target_lineage_unchanged': True,
              'descriptor_record_and_reconciliation_unchanged': True, 'current_input_context_files_checked': 527,
              'current_input_receipt_copies_checked': 10, 'root_objects_checked': 343, 'paired_maven_declarations_checked': 201,
              'frozen_inputs_rechecked': stable, 'invalid_utf8_local_fixture_freeze_checked': 2,
              'publication_custody': copy.deepcopy(custody), 'acceptance_flags_false': True, 'capability_tests_empty': True,
              'receiver_behavior_rerun': False, 'source_export_admitted': False, 'full_module_or_jdk_acceptance': False,
              'unverified_active_hash_references_in_requested_two_rows': [],
              'limits': ['This bounded reference audit grants no repository-wide semantic closure, source export, full module coverage or JDK/JNI acceptance.',
                         'Current be92 execution is retained separately from historical 0b/d1 execution: current independent native19 passes coexist with upstream66/67 and one error.',
                         'Six mandatory direct body reads and21 observed direct reads coexist with exactly two malformed-UTF8 Git-identity-only fixtures; remote raw bytes and remote SHA256 for those two were not recovered.',
                         'The four original receiving behavior tests remain historical E2 evidence and are not rerun.',
                         'Historical source objects, receiver targets, prior qualification and unchanged Descriptor evidence are preserved without promotion.']}
    # Evidence is emitted only after every assertion passes.
    out.mkdir()
    path = out / 'REFERENCE_AUDIT.json'
    path.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'result': result['result'], 'source_refs': len(source_occurrences), 'source_proofs': len(proofs),
                      'preserved_e3_files': len(preserved_e3), 'preserved_e2_files': len(preserved_e2),
                      'destination_occurrences': len(references), 'frozen_inputs': len(stable), 'sha256': ident(path)['sha256']}))


def check_custody_indexes(final, receipt_refs):
    package = HERE / 'custody-final-evidence'
    index_path = package / 'CUSTODY_EVIDENCE_INDEX.json'
    additional_path = package / 'ADDITIONAL_RECEIPTS.json'
    check(index_path, {'sha256': '8e478debeac87c46d01d57cf1038b2195005f9f6af249b25d97f1074ef26b38b'})
    check(additional_path, {'sha256': '30cc1c07b3aac8f1a6662c494c0cbbc76c22bc6a4fdbcbb04770236fd2384907'})
    index, additional = load(index_path), load(additional_path)
    assert index['schema'] == 'm3-jcc-e04-publication-custody-evidence-carry/1'
    assert (index['source_commit'], index['source_root']) == (SOURCE, SOURCE_ROOT)
    assert index['publication_receipt_sha256'] == PUBLICATION_HASH
    assert len(additional) == 123 and len(index['items']) == 121
    for key in ('original_absolute_references_rewritten', 'complete_original_raw_custody_envelope_reproduction_claim',
                'complete_source_payload_checkout_claim', 'source_export_admitted', 'destination_gates_passed'):
        assert index[key] is False
    assert index['structured_subobject_extractions'] == []
    for name, ref in additional.items():
        relative(name)
        assert final['additional_receipts'][name] == ref
        assert receipt_refs[name]['path'] == str(TASK / 'source-recovery' / name)
        check(ROOT / receipt_refs[name]['path'], ref)
        assert (ROOT / receipt_refs[name]['path']).read_bytes() == local_ref(ref).read_bytes()
    names = set()
    for row in index['items']:
        name = row['planned_receiving_copy_name']
        assert name not in names
        names.add(name)
        assert row['planned_receiving_path'] == receipt_refs[name]['path']
        assert row['representation'] == 'EXACT_ORIGINAL_FILE_BYTES'
        assert row['carry_status'] == 'DIRECTLY_CARRIED_EXACT_ORIGINAL_FILE'
        origin, staged = Path(row['source_local_path']), Path(row['staged_local_path'])
        check(origin, row)
        check(staged, row)
        check(ROOT / row['planned_receiving_path'], row)
        assert origin.read_bytes() == staged.read_bytes() == (ROOT / row['planned_receiving_path']).read_bytes()
        if 'verified_prior_draft_copy' in row:
            assert origin.read_bytes() == Path(row['verified_prior_draft_copy']).read_bytes()
    assert sum(row['bytes'] for row in index['items']) == index['counts']['exact_original_bytes'] == 15440298
    omitted = index['omitted_original_local_files']
    assert len(omitted) == 132 and len({row['source_local_path'] for row in omitted}) == 132
    omitted_counts = Counter()
    for row in omitted:
        assert row['staged_local_path'] is None and row['planned_receiving_copy_name'] is None
        origin = Path(row['source_local_path'])
        check(origin, row)
        bind = row['binding']
        document = load(local_ref(additional[bind['carried_copy_name']]))
        pointer = bind['json_pointer']
        assert pointer.startswith('/')
        value = document
        for part in pointer.split('/')[1:]:
            token = part.replace('~1', '/').replace('~0', '~')
            value = value[int(token)] if isinstance(value, list) else value[token]
        if isinstance(value, str):
            assert pointer == '/readback_path' and value == str(origin)
            assert document['readback_sha256'] == row['sha256']
        else:
            assert value.get('local_path', value.get('path')) == str(origin)
            assert value['sha256'] == row['sha256']
        assert row['reason'].strip()
        omitted_counts[row['carry_status']] += 1
    assert dict(omitted_counts) == {'RECEIPT_BOUND_ORIGINAL_LOCAL_ENVELOPE_NOT_CARRIED': 42,
                                    'RECEIPT_BOUND_REDUNDANT_LOCAL_READBACK_NOT_CARRIED': 1,
                                    'INDEX_BOUND_TRANSPORT_BATCH_NOT_CARRIED': 89}
    return {'index': {'path': str(index_path), **ident(index_path)}, 'exact_original_copies': 121,
            'additional_receipts': 123, 'omitted_local_originals_hash_rechecked': 132,
            'omitted_status_counts': dict(omitted_counts), 'complete_raw_envelope_reproduction_claim': False}


if __name__ == '__main__':
    main()
