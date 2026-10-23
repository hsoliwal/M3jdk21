#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Bind the explicitly selected final publication paths to exact provenance rows.

This bounded publication adapter preserves the initial historical row plan.
Unknown paths are refused; it does not classify a subtree by default.
"""
from pathlib import Path
import copy
import hashlib
import json

ROOT = Path(__file__).resolve().parents[2]
CRATE = 'synexia-openrewrite-recipes/recipe-crates/m3-arrayification-20261018/integrated-wrappers-20261021'
RESOURCE = 'synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/arrayification-20261018/integrated-wrappers-20261021'
BRANCH = 'aix/integrated-collections-refinement-20261021'
APACHE = 'Apache-2.0'
MIXED = 'Apache-2.0 AND (GPL-2.0-only WITH Classpath-exception-2.0)'

def sha(data): return hashlib.sha256(data).hexdigest()
def blob(data): return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()

def main():
    original = ROOT / 'design/provenance/ROW_PLAN.json'
    assert sha(original.read_bytes()) == 'd8e2d7a938e8084abca8f51223f091c1c544de4f2ed82821cdb202467ef0f931'
    plan = json.loads(original.read_text())
    original_entries = {x['path']: x for x in plan['required_entries']}
    excluded = {RESOURCE + '/templates/m3/collections/src/main/java/com/m3/collections/M3LongBitmapFilteredSnapshot.java',
                RESOURCE + '/templates/m3/collections/src/main/native/collections/m3_index_mask_compactor.c'}
    allowed_payload_revision = RESOURCE + '/templates/test/jdk/jdk/internal/m3/M3IndexAtomResourceProbe.java'
    entries = []
    for path, old in original_entries.items():
        if path in excluded: continue
        entry = copy.deepcopy(old)
        data = (ROOT / 'canonical' / path).read_bytes()
        current = sha(data)
        if old['content_binding'] == 'EXACT_REVIEWED_CONTENT' and current != old['expected_sha256']:
            assert path == allowed_payload_revision
            assert current == 'da251a6b38ad01d89ac199ea55b0b175a88ea4307a6a4ba45b8ad9d564cf80e2'
        previous = old['observed_sha256']
        entry.update(content_binding='EXACT_REVIEWED_CONTENT', observed_sha256=current,
                     observed_bytes=len(data), expected_sha256=current)
        if not old.get('reuse_existing_row'):
            entry['row']['evidence'] = entry['row']['evidence'].replace(previous, current)
            if entry['row']['source_revision'].startswith('sha256:'):
                entry['row']['source_revision'] = 'sha256:' + current
        entries.append(entry)

    # Every new path is named here or in an exact sealed event/copy inventory.
    additions = {}
    for suffix in ['LICENSE_PROVENANCE.md', 'REPRODUCTION.md', 'REPRODUCE.md', 'memory/LESSONS.md',
                   'evidence/package_evidence.py', 'evidence/README.md', 'evidence/LICENSE_PROVENANCE.md']:
        additions[CRATE + '/' + suffix] = ('FIRST_PARTY', APACHE, 'NEW_FIRST_PARTY_RECIPE_OR_DOCUMENT', None)
    for suffix in ['QUALIFICATION.json', 'catalogue/outcomes.json', 'catalogue/source-coverage.tsv', 'memory/INDEX.json',
                   'evidence/INPUT_RULES.json', 'evidence/bundles/MANIFEST.json', 'evidence/bundles/RECEIPT.json',
                   'evidence/bundles/ROUNDTRIP_VERIFICATION.json', 'evidence/bundles/MEASUREMENT_PARTS.json',
                   'qualification/PUBLICATION_TRANSPORT.json']:
        additions[CRATE + '/' + suffix] = ('FIRST_PARTY_NO_HEADER', APACHE, 'FIRST_PARTY_METADATA', None)
    additions[CRATE + '/evidence/bundles/source-history.jsonl'] = ('MIXED', MIXED, 'EXACT_SOURCE_HISTORY_CONTAINER', None)
    additions[CRATE + '/evidence/measurement_parts.py'] = ('FIRST_PARTY', APACHE, 'NEW_FIRST_PARTY_RECIPE_OR_DOCUMENT', None)
    transport = json.loads((ROOT / 'canonical' / CRATE / 'evidence/bundles/MEASUREMENT_PARTS.json').read_text())
    assert transport['original']['sha256'] == '4eb14cb5a8bb45f1d4f21a4e9b89ca7b7c7df7f77a9809fed715ba3c8b087900'
    assert len(transport['parts']) == 5
    for number, part in enumerate(transport['parts'], 1):
        assert part['name'] == f'measurement-evidence.part-{number:03d}.jsonl'
        path = CRATE + '/evidence/bundles/' + part['name']
        assert sha((ROOT / 'canonical' / path).read_bytes()) == part['sha256']
        additions[path] = ('FIRST_PARTY_NO_HEADER', APACHE, 'FIRST_PARTY_METADATA', None)
    index = json.loads((ROOT / 'canonical' / CRATE / 'memory/INDEX.json').read_text())
    assert len(index['events']) == 8
    for event in index['events']:
        suffix = '/memory/events/' + event['id'] + '.json'
        data = (ROOT / 'canonical' / (CRATE + suffix)).read_bytes()
        assert sha(data) == event['id']
        additions[CRATE + suffix] = ('FIRST_PARTY_NO_HEADER', APACHE, 'FIRST_PARTY_METADATA', None)
    for item in json.loads((ROOT / 'publication-v2/VISIBLE_EVIDENCE_MAP.json').read_text()):
        path = item['canonical_path']
        assert path not in additions and path not in original_entries
        assert sha((ROOT / 'canonical' / path).read_bytes()) == item['sha256']
        additions[path] = (item['classification'], item['license_id'],
                           'NEW_FIRST_PARTY_RECIPE_OR_DOCUMENT' if item['classification'] == 'FIRST_PARTY' else 'EXACT_EXECUTION_EVIDENCE', item['source'])
    for path, (classification, license_id, disposition, source) in sorted(additions.items()):
        data = (ROOT / 'canonical' / path).read_bytes()
        current = sha(data)
        origin = source or path
        bundle = 'source-history.jsonl' if source and source.startswith('design/') else 'MEASUREMENT_PARTS.json'
        url_path = CRATE + '/evidence/bundles/' + bundle if source else path
        row = {'path': path, 'classification': classification,
               'source_url': 'https://github.com/hsoliwal/com.synexia/blob/' + BRANCH + '/' + url_path,
               'source_revision': 'sha256:' + current, 'source_path': origin,
               'license_id': license_id,
               'notice_path': CRATE + '/LICENSE_PROVENANCE.md' if classification in {'MIXED', 'GENERATED'} or '/evidence/bundles/' in path else 'LICENSE',
               'disposition': disposition,
               'evidence': 'sha256=' + current + '; exact final v3 publication content; original task path=' + origin + '; final manifest=ed0798ddef0c7d052cafb776a42098086d69d67466c47b97f8bb6c46fe46dae2'}
        entries.append({'path': path, 'expected_status': 'A', 'content_binding': 'EXACT_REVIEWED_CONTENT',
                        'observed_sha256': current, 'observed_bytes': len(data), 'expected_sha256': current,
                        'category': 'EXPLICIT_FINAL_V3_' + classification, 'rationale': 'Explicit final source/evidence path with exact content identity and retained source/notice custody.', 'row': row})
    wanted = {x['path'] for x in entries}
    assert len(wanted) == len(entries)
    engines = {x['path'] for x in plan['excluded_unchanged_owners']}
    actual = {p.relative_to(ROOT / 'canonical').as_posix() for p in (ROOT / 'canonical').rglob('*')
              if p.is_file() and 'target' not in p.parts}
    publication_nonselected = {CRATE + '/evidence/bundles/measurement-evidence.jsonl', 'docs/M3-SCALE/provenance/M3_FILE_PROVENANCE.tsv'}
    assert actual - wanted - engines - excluded - publication_nonselected == set(), 'Unknown canonical publication paths: ' + repr(sorted(actual - wanted - engines - excluded - publication_nonselected))
    assert wanted <= actual
    # The reference set, not local unreferenced payload residue, defines publication.
    manifest = ROOT / 'canonical' / RESOURCE / 'manifest.tsv'
    refs = {RESOURCE + '/' + x.split('\t')[3] for x in manifest.read_text().splitlines()
            if len(x.split('\t')) == 4 and x.split('\t')[3].startswith('templates/')}
    assert len(refs) == 12
    assert {p for p in wanted if p.startswith(RESOURCE + '/templates/')} == refs
    delta = []
    for entry in sorted(entries, key=lambda x: x['path']):
        path = entry['path']; src = ROOT / 'canonical' / path; data = src.read_bytes()
        before = ROOT / 'input/syn' / path
        status = entry['expected_status']
        assert (status == 'M') == before.exists()
        delta.append({'repo': 'syn', 'path': path, 'source': src.relative_to(ROOT).as_posix(),
                      'status': status, 'sha256': sha(data), 'git_blob': blob(data), 'bytes': len(data),
                      'before_sha256': sha(before.read_bytes()) if before.exists() else None})
    plan.update(status='FINAL_PUBLICATION_EXPLICIT_CONTENT_BOUND_PLAN', required_entries=sorted(entries, key=lambda x: x['path']),
                original_plan_sha256=sha(original.read_bytes()), new_packet_file_count=sum(x['expected_status'] == 'A' for x in entries),
                excluded_rejected_payloads=sorted(excluded), publication_nonselected_paths=sorted(publication_nonselected), transport_manifest_sha256=sha((ROOT / 'canonical' / CRATE / 'evidence/bundles/MEASUREMENT_PARTS.json').read_bytes()), final_manifest_sha256=sha(manifest.read_bytes()))
    plan_path = ROOT / 'design/provenance/ROW_PLAN_FINAL_PUBLICATION.json'
    delta_path = ROOT / 'publication-v2/FILE_MANIFEST_PRE_LEDGER_FINAL.json'
    assert not plan_path.exists() and not delta_path.exists()
    plan_path.write_text(json.dumps(plan, indent=2) + '\n')
    delta_path.write_text(json.dumps(delta, indent=2) + '\n')
    print(json.dumps({'status': 'EXPLICIT_FINAL_DELTA_PREPARED', 'files': len(delta), 'bytes': sum(x['bytes'] for x in delta),
                      'plan_sha256': sha(plan_path.read_bytes()), 'delta_sha256': sha(delta_path.read_bytes())}, indent=2))

if __name__ == '__main__': main()
