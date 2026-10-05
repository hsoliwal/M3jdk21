#!/usr/bin/env python3
"""Select a finite receiver evidence cohort for source publication and coverage."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath

R = Path(__file__).resolve().parents[1]
TASK = 'm3/migration/intake-20261005/current-752-ci'
SOURCE_PREFIX = 'synexia-openrewrite-recipes/verification/ci-current-20261005/receiver-current/'
SOURCE = '9963cc08ff13922b92a0e3db7c30f56fddacba7d'
TARGET = '752191c9291f6467110fb8a7badfbdc4c2d41af2'
HEAD = '5c1ee9490e76943dfdb3ca27fade0d093d017226'
SOURCE_PUBLICATION = 'e1cf684627a66ee784cfca0b42553531b2d8e7d4'
MANIFEST_SHA = '8c3d4e6a28d4314a1de4c3a15c3616a371e04ac2eaf6357852c6ce28c1642db4'
SELECTION_SHA = 'f6d77e30c21e26205ce0af953c7c741dab9519c07f74f3bcd0f1832e5d780d7c'
PRIOR_SHA = '83e2729fc7dae8769acce8cbdb0a1cd104088cf0d8a5e1162ce8a04e89c70ece'
PREFIXES = tuple(TASK + '/' + name + '/' for name in (
    'consumer-portable', 'receiver-authoring', 'source-admission-evidence', 'source-delivery'))
SELECTION_TARGET = TASK + '/source-delivery-coverage.json'


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def unique(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, 'duplicate JSON key: ' + key)
        result[key] = value
    return result


def parse(data):
    return json.loads(data, object_pairs_hook=unique)


def read(path):
    path = Path(path)
    require(path.is_file() and not path.is_symlink(), 'regular input required: ' + str(path))
    return path.read_bytes()


def relative(path):
    require(isinstance(path, str) and path and not PurePosixPath(path).is_absolute()
            and '\\' not in path and all(p not in ('', '.', '..') and ':' not in p for p in path.split('/'))
            and all(ord(c) >= 32 and ord(c) != 127 for c in path), 'unsafe publication path')
    return path


def check(data, row):
    require(type(row['bytes']) is int and len(data) == row['bytes']
            and sha(data) == row['sha256'], 'body identity drift: ' + row['path'])
    blob = hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()
    require(row.get('sha', row.get('gitBlob')) == blob, 'Git identity drift: ' + row['path'])
    if 'gitBlob' in row:
        require(row['gitBlob'] == blob, 'second Git identity drift')
    require(row['mode'] in ('100644', '100755') and row.get('type', 'blob') == 'blob',
            'regular Git file mode required')
    return blob


def encoded(value):
    return (json.dumps(value, indent=2, sort_keys=True) + '\n').encode()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--receiver-whitelist', type=Path, required=True)
    parser.add_argument('--sha', required=True, help='Exact reviewed receiver whitelist SHA-256')
    parser.add_argument('--result-sha', required=True, help='Actual full consumer-v5 PASS RESULT SHA-256')
    parser.add_argument('--output', type=Path, required=True, help='Fresh publication-only directory')
    args = parser.parse_args()
    out = args.output.absolute()
    require(not out.exists(), 'fresh output directory required')
    whitelist_path = args.receiver_whitelist.absolute()
    whitelist_bytes = read(whitelist_path)
    require(sha(whitelist_bytes) == args.sha, 'reviewed receiver whitelist seal')
    main_whitelist = parse(whitelist_bytes)
    require(main_whitelist['schema'] == 'm3.current-receiver-publication-whitelist/1'
            and main_whitelist['state'] == 'EXACT_QUALIFIED_CONTEXT_AND_SEPARATE_PROVENANCE',
            'actual reviewed receiver selection required')
    require(main_whitelist['consumerResultSHA256'] == args.result_sha
            and main_whitelist['consumerManifestSHA256'] == MANIFEST_SHA
            and main_whitelist['sourceDeliveryCoverageSHA256'] == SELECTION_SHA,
            'exact final result, manifest and source selection bindings')
    require(main_whitelist['runtimeAccepted'] is False
            and main_whitelist['proofGatesExecutedByPublication'] is False,
            'publication cannot promote product admission')
    rows = {relative(row['path']): row for row in main_whitelist['files']}
    require(len(rows) == len(main_whitelist['files']) == main_whitelist['fileCount'],
            'unique complete receiver whitelist')
    selected = {path: row for path, row in rows.items()
                if path.startswith(PREFIXES) or path == SELECTION_TARGET}
    require(SELECTION_TARGET in selected and all(any(p.startswith(prefix) for p in selected)
                                                for prefix in PREFIXES), 'required evidence families')

    # Reuse the reviewed main selector's archive validation; check its exact final receipt here.
    proof = R / 'evidence/consumer-v5'
    result_path = proof / 'RESULT.json'
    result_bytes = read(result_path)
    require(sha(result_bytes) == args.result_sha, 'actual full consumer-v5 receipt seal')
    result = parse(result_bytes)
    require(result['state'] == 'PASS_BOUNDED_CURRENT_CONSUMERS' and result['failed'] is None,
            'actual full consumer-v5 PASS required; scoped Python PASS is insufficient')
    require(result['runtimeAccepted'] is False and result['productWrites'] == 0
            and result['canonicalProductionApplied'] is False, 'bounded proof meaning unchanged')
    result_row = selected[TASK + '/consumer-portable/final/RESULT.json']
    require(Path(result_row['localPath']).resolve() == result_path.resolve()
            and read(result_row['localPath']) == result_bytes, 'selected final receipt is the actual proof receipt')
    manifest_bytes = read(proof / 'authority/manifest.json')
    require(sha(manifest_bytes) == MANIFEST_SHA, 'proof manifest seal')
    manifest = parse(manifest_bytes)
    require(manifest['source']['commit'] == SOURCE and manifest['receiving']['commit'] == TARGET
            and manifest['publicationHead']['commit'] == HEAD, 'distinct frozen lineage pins')

    selection_row = selected[SELECTION_TARGET]
    selection_bytes = read(selection_row['localPath'])
    require(sha(selection_bytes) == SELECTION_SHA, 'exact75 selection body')
    selection = parse(selection_bytes)
    require(selection['knownDeclaredSourcePaths'] == 387
            and selection['selectedFileCount'] == len(selection['publicationAdditions']) == 75,
            'exact prior provenance selection')
    require(len({row['path'] for row in selection['publicationAdditions']}) == 75,
            'unique prior provenance selection')
    for prior_row in selection['publicationAdditions']:
        require(prior_row['path'] in selected, 'all75 provenance rows must be retained')
        row = selected[prior_row['path']]
        require(all(row[key] == prior_row[key] for key in ('sha256', 'bytes', 'mode'))
                and row['sha'] == prior_row.get('sha', prior_row.get('gitBlob')),
                'prior provenance selection identity changed')

    prior_path = R / 'research/source-delivery-coverage/cih-copies-v1/coverage-append.json'
    prior_bytes = read(prior_path)
    require(sha(prior_bytes) == PRIOR_SHA, 'immutable387 census seal')
    prior = parse(prior_bytes)
    require(prior['allSourcePathCount'] == len(prior['allSourcePaths']) == 387, 'prior source universe')
    source_rows = []
    mappings = []
    input_bodies = {}
    for destination, row in sorted(selected.items()):
        body_path = Path(row['localPath']).absolute()
        # Aliases point at the existing sealed body; no duplicate archive bodies are created.
        require(not body_path.is_relative_to(out), 'collector outputs cannot be cohort inputs')
        body = read(body_path)
        blob = check(body, row)
        source_path = SOURCE_PREFIX + destination[len(TASK) + 1:]
        require(source_path not in prior['allSourcePaths'], 'source alias overlaps the prior census')
        input_bodies[str(body_path)] = (sha(body), len(body))
        source_rows.append({'path': source_path, 'localPath': str(body_path), 'mode': row['mode'],
                            'type': 'blob', 'sha': blob, 'gitBlob': blob, 'sha256': sha(body),
                            'bytes': len(body), 'group': 'receiver-current-provenance'})
        mappings.append({'sourcePath': source_path, 'path': destination, 'file': str(body_path),
                         'mode': row['mode'], 'type': 'blob', 'sha': blob, 'gitBlob': blob,
                         'sha256': sha(body), 'bytes': len(body)})
    require(len(source_rows) == len({row['path'] for row in source_rows}), 'unique finite source aliases')
    bindings = {'sourceCommit': SOURCE, 'targetCommit': TARGET, 'receivingHead': HEAD,
                'originalSourcePublicationCommit': SOURCE_PUBLICATION,
                'receiverWhitelist': {'file': str(whitelist_path), 'sha256': args.sha},
                'consumerResultSHA256': args.result_sha, 'consumerManifestSHA256': MANIFEST_SHA,
                'sourceDeliverySelectionSHA256': SELECTION_SHA,
                'consumerBundleSHA256': main_whitelist['consumerBundleSHA256'],
                'proofGatesExecutedByPublication': False, 'runtimeAccepted': False,
                'canonicalProductionApplied': False, 'productWrites': 0, 'remoteWrites': False}
    source_document = dict(bindings, schema='synexia.receiver-current.source-publication-whitelist/1',
                           state='EXACT_FINITE_RECEIVER_EVIDENCE_COHORT', files=source_rows,
                           fileCount=len(source_rows), bytes=sum(row['bytes'] for row in source_rows),
                           originalDeclaredSourcePathCount=387,
                           boundary='Additional receiver provenance aliases; this does not enlarge the original387 source claim. The main receiver whitelist, these output documents and their future census are outside this cohort.')
    mapping_document = dict(bindings, schema='m3.source-delivery-explicit-mapping/1', files=mappings,
                            fileCount=len(mappings), byteIdentical=True,
                            frozenConsumerContextModified=False, consumerQualified=False)
    source_bytes = encoded(source_document)
    mapping_bytes = encoded(mapping_document)
    plan = {'schema': 'm3.source-delivery-append-plan/1', 'label': 'receiver-current-evidence-cohort',
            'prior': {'file': str(prior_path), 'sha256': PRIOR_SHA}, 'segments': [{
                'id': 'receiver-current-provenance',
                'whitelist': {'file': str(out / 'source-whitelist.json'), 'sha256': sha(source_bytes)},
                'mapping': {'file': str(out / 'source-to-receiving.json'), 'sha256': sha(mapping_bytes)}}]}
    require(read(whitelist_path) == whitelist_bytes and read(result_path) == result_bytes
            and read(proof / 'authority/manifest.json') == manifest_bytes
            and read(prior_path) == prior_bytes and read(selection_row['localPath']) == selection_bytes,
            'input document changed during collection')
    for path, identity in input_bodies.items():
        body = read(path)
        require((sha(body), len(body)) == identity, 'selected body changed during collection')
    out.mkdir(parents=True, exist_ok=False)
    outputs = {'source-whitelist.json': source_bytes, 'source-to-receiving.json': mapping_bytes,
               'coverage-plan.json': encoded(plan)}
    for name, body in outputs.items():
        with (out / name).open('xb') as stream:
            stream.write(body)
    print(json.dumps({'state': 'FINITE_RECEIVER_EVIDENCE_COHORT_SELECTED', 'fileCount': len(source_rows),
                      'bytes': source_document['bytes'], 'outputs': {name: sha(body) for name, body in outputs.items()},
                      'proofGatesExecuted': False, 'remoteWrites': False}))


if __name__ == '__main__':
    main()
