"""Seal the additive E4 payload after actual execution and independent reviews.

This work-only packager performs no recipe, build, Git or network operation.
"""
from __future__ import annotations
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

B = Path(__file__).resolve().parent
R = B / 'overlay'
BASE = B.parent
TASK = 'm3/tooling/migration-recipes/tasks/jcc-source-recovery-handoff-20261005'
CRATE = 'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-source-recovery-handoff-20261005'
PARENT = '0994ecd65e86600f417f4d8702838d8c2663af61'
TREE = '7e8f39ac999174f8d5b6028d638a68d8c317b070'
SOURCE = '0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8'
SOURCE_TREE = '0361e4a07b77a01df170523033fef01c4052ddc5'


def load(path):
    return json.loads(path.read_bytes())


def identity(path):
    assert path.is_file() and not path.is_symlink() and path.resolve(strict=True) == path
    body = path.read_bytes()
    assert not path.stat().st_mode & 0o111, path
    assert body.decode('utf-8').encode('utf-8') == body and b'\0' not in body, path
    return {'bytes': len(body), 'sha256': hashlib.sha256(body).hexdigest(),
            'git_blob_sha1': hashlib.sha1(f'blob {len(body)}\0'.encode() + body).hexdigest(),
            'mode': '100644'}


def main():
    if not __debug__:
        raise RuntimeError('Optimized Python disables required publication assertions')
    assert not (B / 'PUBLICATION_MANIFEST.json').exists()
    required = ['REVIEW.md', 'independent-bound-review/REVIEW.md',
                'independent-resource-review/REVIEW.md', 'final-gate-review/REVIEW.md',
                'verification/receipt.json', 'reference-audit/REFERENCE_AUDIT.json']
    for name in required:
        assert (R / TASK / name).is_file(), name
    verification = load(B / 'verification-v3/receipt.json')
    assert verification['source_publication_commit'] == SOURCE
    assert verification['source_publication_root'] == SOURCE_TREE
    assert verification['destination_preimage_commit'] == PARENT
    assert verification['java_suite']['tests'] == '12' and verification['python_tests'] == 6
    assert all(verification['java_suite'][name] == '0' for name in ('failures', 'errors', 'skipped'))
    assert verification['all_fixed_inputs_unchanged'] is True
    assert verification['four_outputs_equal_sealed_afterimages'] is True
    assert verification['source_export_admitted'] is False and verification['receiver_behavior_rerun'] is False
    assert all(run['exit_code'] == run['expected_exit_code'] for run in verification['runs'])
    audit = load(B / 'reference-audit-v3/REFERENCE_AUDIT.json')
    assert audit['result'] == 'PASS' and audit['source_publication_commit'] == SOURCE
    assert audit['unverified_active_hash_references_in_requested_two_rows'] == []
    assert (R / TASK / 'verification/receipt.json').read_bytes() == (B / 'verification-v3/receipt.json').read_bytes()
    assert (R / TASK / 'reference-audit/REFERENCE_AUDIT.json').read_bytes() == (B / 'reference-audit-v3/REFERENCE_AUDIT.json').read_bytes()
    base_path = BASE.parent / 'publication-current/m3-e4-publish/BASE_TREE_READBACK.json'
    assert identity(base_path)['sha256'] == '370979753c32e8d89b7346ab418dffc0bd3ca66290d107f370cbc78685c223bd'
    base = load(base_path)
    assert base['commit'] == PARENT and base['root']['sha'] == TREE
    assert base['root']['truncated'] is False and base['m3_recursive']['truncated'] is False
    entries = {'m3/' + row['path']: row for row in base['m3_recursive']['tree']}
    plan = load(R / CRATE / 'plan.json')
    assert plan['source_commit'] == SOURCE and plan['target_commit'] == PARENT
    assert plan['plan_sha256'] == verification['plan_sha256'] == audit['plan_sha256']
    outputs = {row['path']: row for row in plan['outputs']}
    assert len(outputs) == 4
    paths = set(outputs)
    for prefix in (TASK, CRATE):
        paths.update(str(path.relative_to(R)) for path in (R / prefix).rglob('*') if path.is_file())
    paths.update([
        'm3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jcc-source-recovery-handoff.yml',
        'm3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/JccSourceRecoveryHandoffTest.java',
        'm3/migration/test/test_jcc_source_recovery_handoff_packet.py',
        'm3/tooling/migration-recipes/verification/jcc-source-recovery-handoff/pom.xml',
    ])
    rows = []
    for path in sorted(paths):
        assert not any(part in Path(path).parts for part in ('target', '__pycache__', '.git'))
        local = R / path
        actual = identity(local)
        if path in outputs:
            output = outputs[path]
            before = identity(R / CRATE / output['before']['resource'])
            remote = entries[path]
            assert remote['type'] == 'blob'
            assert (before['git_blob_sha1'], before['mode'], before['bytes']) == (remote['sha'], remote['mode'], remote['size'])
            assert before['sha256'] == output['before']['sha256'] and actual['sha256'] == output['after']['sha256']
            assert local.read_bytes() == (R / CRATE / output['after']['resource']).read_bytes()
            operation, old_sha, old_git = 'update', before['sha256'], before['git_blob_sha1']
        else:
            assert path not in entries, ('Addition already exists in E3', path)
            for ancestor in Path(path).parents:
                if str(ancestor) in entries:
                    assert entries[str(ancestor)]['type'] == 'tree'
            operation, old_sha, old_git = 'add', 'ABSENT', None
        rows.append({'path': path, 'operation': operation, 'before_sha256': old_sha,
                     'before_git_blob_sha1': old_git, 'preimage_tree': TREE,
                     **actual, 'local_path': str(local)})
    preserved = []
    for previous in load(BASE / 'successor-e03/PUBLICATION_MANIFEST.json')['files']:
        path = previous['path']
        if path in outputs:
            continue
        actual = identity(R / path)
        assert all(actual[key] == previous[key] for key in actual)
        assert path not in paths
        preserved.append({'path': path, **actual})
    assert len(preserved) == 89
    manifest = {
        'repository': 'hsoliwal/M3jdk21', 'branch': 'aix/jcc-source-final-handoff-20261005',
        'expected_branch_head': PARENT, 'base_root_tree': TREE,
        'base_tree_receipt_sha256': identity(base_path)['sha256'],
        'source_publication_commit': SOURCE, 'source_publication_tree': SOURCE_TREE,
        'source_pull_request': 'https://github.com/hsoliwal/com.synexia/pull/9362',
        'source_execution_input_commit': 'be92c62ece9023b5c33676716a1076d00e26120a',
        'source_publication_status': 'PUBLISHED_CUSTODY_VERIFIED',
        'source_publication_receipt_sha256': '95f325ce57dadab60031f41514d9a9c5fa3ef6eb29a41e3c9a8c81e2dee996b8',
        'retained_destination_owner_commit': 'da958d00d24154c0db87beca0ec80a7df2b43b73',
        'receiving_pr': 'https://github.com/hsoliwal/M3jdk21/pull/147',
        'publication_route': 'Update PR147 only if the expected branch head remains open/unmerged; root rechecks live state.',
        'target_base': 'master', 'requested_pr_state': 'draft',
        'local_candidate_kind': 'EXACT_PARTIAL_OVERLAY_WITH_FOUR_RECIPE_MATERIALIZED_UPDATES',
        'plan_sha256': plan['plan_sha256'], 'file_count': len(rows),
        'total_bytes': sum(row['bytes'] for row in rows), 'updates': 4, 'additions': len(rows) - 4,
        'preserved_e3_payload_files': preserved, 'all_other_base_paths_to_preserve': True,
        'capability_export_or_jdk_promotion': False,
        'source_gate_boundary': 'Current parser43, units119, observation8/standalone19 and independent Linux native19 PASS; upstream67 includes66PASS/1error; mechanical70 stages PASS; overall source qualification BLOCKED.',
        'receiving_local_gate_boundary': '12Java+6Python PASS;132 refusal calls within44conditions; ordinary validator VALID but INCOMPLETE; complete exit2 expected; no historical receiver4 rerun, whole-module or JDK pass.',
        'custody_boundary': '21 successful direct body reads, six mandatory; exactly two malformed-UTF8 identity-only fixtures with raw_body_readback and remote_sha256_observed false. Explicit carried/omitted evidence index retained.',
        'files': rows,
    }
    assert sum(row['operation'] == 'update' for row in rows) == 4
    path = B / 'PUBLICATION_MANIFEST.json'
    path.write_text(json.dumps(manifest, indent=2) + '\n')
    columns = ('operation', 'mode', 'path', 'bytes', 'sha256', 'git_blob_sha1', 'before_sha256', 'before_git_blob_sha1', 'local_path')
    tsv = B / 'PUBLICATION_MANIFEST.tsv'
    tsv.write_text('\t'.join(columns) + '\n' + ''.join('\t'.join(str(row[key]) if row[key] is not None else '' for key in columns) + '\n' for row in rows))
    seal = {'sealed_at_utc': datetime.now(timezone.utc).isoformat(), 'manifest_sha256': identity(path)['sha256'],
            'tsv_sha256': identity(tsv)['sha256'], 'plan_sha256': plan['plan_sha256'],
            'file_count': len(rows), 'total_bytes': manifest['total_bytes'], 'updates': 4, 'additions': len(rows) - 4,
            'execution_receipt_sha256': identity(B / 'verification-v3/receipt.json')['sha256'],
            'reference_audit_sha256': identity(B / 'reference-audit-v3/REFERENCE_AUDIT.json')['sha256'],
            'independent_bound_review_sha256': identity(R / TASK / 'independent-bound-review/REVIEW.md')['sha256'],
            'independent_resource_review_sha256': identity(R / TASK / 'independent-resource-review/REVIEW.md')['sha256'],
            'executed_evidence_review_sha256': identity(R / TASK / 'final-gate-review/REVIEW.md')['sha256'],
            'remote_publication_performed': False}
    (B / 'PUBLICATION_SEAL.json').write_text(json.dumps(seal, indent=2) + '\n')
    print(json.dumps(seal))


if __name__ == '__main__':
    main()
