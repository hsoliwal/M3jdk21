#!/usr/bin/env python3
"""Prepare append-only publication envelopes; no archive operations or proof gates."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath

R = Path(__file__).resolve().parents[1]
TASK = 'm3/migration/intake-20261005/current-752-ci'
SOURCE_ROOT = 'synexia-openrewrite-recipes/verification/ci-current-20261005/receiver-current'
NEW_SUFFIX = 'delivery-envelope/archive-locator-v1/'
OLD_MAIN_SHA = 'bc12bfabf9325064b6566c3b97af92c57870a0f52f0290c6e6f52f7b5b70346b'
OLD_SOURCE_SHA = '127b98f9c4399e3079404b6a7bb72ad613b8f4a3bf06a6eaa71e4574af6190ae'
OLD_RECEIVER_SHA = '8f20f54fb9ce34db397a46de69a8125b4a1f33d2740c92da909b06e0719bf5d6'
RESULT_SHA = 'c037e9fd5b622f17d161d8efe1fbb15f2a128f803e27b70292448403dc43bd80'
MANIFEST_SHA = '8c3d4e6a28d4314a1de4c3a15c3616a371e04ac2eaf6357852c6ce28c1642db4'
SELECTOR_SHA = '0d982a856d35b7fb9f64f4d6cdf0a25ddcf118d20f07fccbdf729c5f1711d65e'
PATCH_SHA = '531cc600b15e6a79d8254734c6ae0ff3826953ef34ac66cd860af08c8b8112c9'
LOCATOR_SHA = '29e58bf8cae108b38b36fa5c370dc2db5e370edd4f2131a728456f885f1d8b97'
RECORD_SHA = '9e2836c488ced0ae07e302e99ad7c455a28fe5b85fe197588aaab57990940351'
PRIOR_CENSUS_SHA = '83e2729fc7dae8769acce8cbdb0a1cd104088cf0d8a5e1162ce8a04e89c70ece'
REMOVED = TASK + '/consumer-portable/COPY-CUSTODY.json'
LOCATOR = TASK + '/consumer-portable/COPY-CUSTODY.ARCHIVE.json'
ERRORS = {TASK + '/consumer-portable/transport-refusals/LARGEST-BLOB-TRANSPORT-FAILURE-v' + str(n) + '.json' for n in (1, 2)}


def require(value, message):
    if not value:
        raise ValueError(message)


def sha(body):
    return hashlib.sha256(body).hexdigest()


def blob(body):
    return hashlib.sha1(b'blob ' + str(len(body)).encode() + b'\0' + body).hexdigest()


def relative(path):
    require(isinstance(path, str) and path and not PurePosixPath(path).is_absolute()
            and '\\' not in path and all(x not in ('', '.', '..') for x in path.split('/')),
            'canonical relative path required')
    return path


def encoded(document):
    return (json.dumps(document, sort_keys=True, indent=2) + '\n').encode()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('receiver-whitelist', 'selector', 'selector-patch', 'custody-record'):
        parser.add_argument('--' + name, type=Path, required=True)
        parser.add_argument('--' + name + '-sha', required=True)
    parser.add_argument('--cohort', type=Path, required=True)
    for name in ('source-whitelist', 'source-to-receiving', 'coverage-plan', 'coverage-append'):
        parser.add_argument('--' + name + '-sha', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    out = args.output.absolute()
    require(not out.exists(), 'fresh output required')
    inputs = {}

    def read(path, pin=None):
        path = Path(path).absolute()
        require(path.is_file() and not path.is_symlink() and not path.is_relative_to(out), 'regular external input required')
        body = path.read_bytes()
        if pin is not None:
            require(sha(body) == pin, 'input seal drift: ' + str(path))
        identity = {'file': str(path), 'sha256': sha(body), 'bytes': len(body)}
        require(str(path) not in inputs or inputs[str(path)] == identity, 'input changed during collection')
        inputs[str(path)] = identity
        return body

    def document(path, pin):
        return json.loads(read(path, pin))

    def identity(path):
        read(path)
        return dict(inputs[str(Path(path).absolute())])

    def rows(document):
        result = {relative(row['path']): row for row in document['files']}
        require(len(result) == len(document['files']) == document['fileCount'], 'unique file inventory required')
        return result

    def body_check(row):
        body = read(row.get('localPath', row.get('file')))
        require(sha(body) == row['sha256'] and len(body) == row['bytes']
                and blob(body) == row.get('sha', row.get('gitBlob'))
                and row.get('gitBlob', blob(body)) == blob(body)
                and row['mode'] in ('100644', '100755'), 'publication body drift: ' + row['path'])
        return body

    old_dir = R / 'publication-extra/final-delivery-envelopes-v1'
    old_source = document(old_dir / 'source-supplement.json', OLD_SOURCE_SHA)
    old_receiver = document(old_dir / 'receiver-supplement.json', OLD_RECEIVER_SHA)
    old_main = document(R / 'publication-extra/receiver-whitelist-v5.json', OLD_MAIN_SHA)
    main_whitelist = document(args.receiver_whitelist, args.receiver_whitelist_sha)
    before, current = rows(old_main), rows(main_whitelist)
    require(set(current) == (set(before) - {REMOVED}) | {LOCATOR} | ERRORS,
            'only the admitted loose-custody replacement and two raw refusal additions')
    require(all(current[path] == row for path, row in before.items() if path != REMOVED),
            'all other original receiver rows must be dictionary-identical')
    require(main_whitelist['previousSelectionSHA256'] == OLD_MAIN_SHA
            and main_whitelist['qualifiedPaths'] == old_main['qualifiedPaths']
            and main_whitelist['qualifiedFileCount'] == old_main['qualifiedFileCount'] == 221,
            'all qualified paths remain unchanged')
    for key, pin in (('consumerResultSHA256', RESULT_SHA), ('consumerManifestSHA256', MANIFEST_SHA),
                     ('consumerBundleSHA256', old_main['consumerBundleSHA256'])):
        require(main_whitelist[key] == pin, 'qualification or archive binding changed')
    for row in current.values():
        body_check(row)
    result = document(R / 'evidence/consumer-v5/RESULT.json', RESULT_SHA)
    require(result['state'] == 'PASS_BOUNDED_CURRENT_CONSUMERS' and result['failed'] is None
            and result['runtimeAccepted'] is False and result['productWrites'] == 0, 'unchanged bounded actual PASS')

    require(args.selector_sha == SELECTOR_SHA and args.selector_patch_sha == PATCH_SHA
            and args.custody_record_sha == RECORD_SHA, 'reviewed selector and inspection evidence required')
    read(args.selector, args.selector_sha)
    read(args.selector_patch, args.selector_patch_sha)
    record = document(args.custody_record, args.custody_record_sha)
    locator = json.loads(read(current[LOCATOR]['localPath'], LOCATOR_SHA))
    require(locator['archiveInspection']['sha256'] == args.custody_record_sha
            and locator['archiveInspection']['bytes'] == len(read(args.custody_record))
            and locator['archiveInspection']['evidencePath'] == '../' + NEW_SUFFIX + 'COPY_CUSTODY_ARCHIVE_RECORD.json'
            and locator['archivedFileRecord'] == record['custodyRow']
            and locator['sha256'] == before[REMOVED]['sha256']
            and locator['bytes'] == before[REMOVED]['bytes']
            and locator['bundle']['sha256'] == main_whitelist['consumerBundleSHA256'], 'exact unchanged archived custody locator')
    require({TASK + '/consumer-portable/' + x['path'] for x in locator['transportRefusals']} == ERRORS,
            'exact transport refusal set')
    for row in locator['transportRefusals']:
        supplied = current[TASK + '/consumer-portable/' + row['path']]
        require(all(supplied[key] == row[key] for key in ('sha256', 'bytes')), 'raw transport refusal identity')

    cohort = args.cohort.absolute()
    cohort_docs = {}
    for name in ('source-whitelist', 'source-to-receiving', 'coverage-plan', 'coverage-append'):
        pin = getattr(args, name.replace('-', '_') + '_sha')
        cohort_docs[name] = document(cohort / (name + '.json'), pin)
    source, mapping, plan, census = (cohort_docs[name] for name in ('source-whitelist', 'source-to-receiving', 'coverage-plan', 'coverage-append'))
    source_rows = rows(source)
    mapped = {row['sourcePath']: row for row in mapping['files']}
    require(set(source_rows) == set(mapped) and len(mapped) == len(mapping['files']), 'complete finite mapping')
    for document_ in (source, mapping):
        require(document_['receiverWhitelist']['sha256'] == args.receiver_whitelist_sha
                and document_['consumerResultSHA256'] == RESULT_SHA
                and document_['consumerManifestSHA256'] == MANIFEST_SHA, 'cohort must bind the new actual main selection')
    for path, row in source_rows.items():
        body_check(row)
        destination = mapped[path]['path']
        require(destination in current and path == SOURCE_ROOT + '/' + destination[len(TASK) + 1:]
                and destination.startswith(TASK + '/'), 'exact source-to-receiver alias')
        require(all(row[key] == current[destination][key] == mapped[path][key]
                    for key in ('sha256', 'bytes', 'mode')), 'finite alias identity drift')
    require(plan['prior']['sha256'] == census['priorCoverage']['sha256'] == PRIOR_CENSUS_SHA
            and len(plan['segments']) == 1
            and plan['segments'][0]['whitelist']['sha256'] == args.source_whitelist_sha
            and plan['segments'][0]['mapping']['sha256'] == args.source_to_receiving_sha,
            'exact retained-helper plan bindings')
    require(census['priorSourcePathCount'] == 387 and census['newSourcePathCount'] == len(source_rows)
            and census['allSourcePathCount'] == 387 + len(source_rows)
            and set(x['sourcePath'] for x in census['newRows']) == set(source_rows)
            and len(set(census['allSourcePaths'])) == census['allSourcePathCount'], 'derived finite census counts')

    old_source_rows, old_receiver_rows = rows(old_source), rows(old_receiver)
    require(len(old_source_rows) == len(old_receiver_rows) == 17, 'all17 historical envelopes required')
    for row in list(old_source_rows.values()) + list(old_receiver_rows.values()):
        body_check(row)
    old_mapping = old_source['byteIdenticalSourceReceivingMapping']
    require(old_mapping == old_receiver['byteIdenticalSourceReceivingMapping'], 'historical source/receiver mapping')
    for pair in old_mapping:
        a, b = old_source_rows[pair['sourcePath']], old_receiver_rows[pair['receivingPath']]
        require(all(a[key] == b[key] == pair[key] for key in ('sha256', 'bytes', 'gitBlob')), 'historical mapping body identity')

    items = [('receiver-whitelist.json', args.receiver_whitelist)]
    items += [(name + '.json', cohort / (name + '.json')) for name in (
        'source-whitelist', 'source-to-receiving', 'coverage-plan', 'coverage-append')]
    items += [('prepare_receiver_whitelist_v6.py', args.selector),
              ('prepare_receiver_whitelist_v6.patch', args.selector_patch),
              ('COPY_CUSTODY_ARCHIVE_RECORD.json', args.custody_record),
              ('prepare_final_delivery_envelopes_v2.py', Path(__file__))]
    operative = {name: dict(identity(path), sourcePath=SOURCE_ROOT + '/' + NEW_SUFFIX + name,
                           receivingPath=TASK + '/' + NEW_SUFFIX + name, relativePath=NEW_SUFFIX + name)
                 for name, path in items}
    envelope_count = len(old_source_rows) + len(items) + 2
    current_document = {
        'schema': 'm3.current-delivery-index/1', 'state': 'OPERATIVE_PUBLICATION_METADATA',
        'operative': operative, 'consumerResultSHA256': RESULT_SHA, 'consumerManifestSHA256': MANIFEST_SHA,
        'consumerBundleSHA256': main_whitelist['consumerBundleSHA256'],
        'historicalPreUploadEnvelopes': {'count': len(old_source_rows),
            'sourceWhitelist': identity(old_dir / 'source-supplement.json'),
            'receiverWhitelist': identity(old_dir / 'receiver-supplement.json'),
            'sourcePaths': list(old_source_rows), 'receivingPaths': list(old_receiver_rows),
            'meaning': 'All17 old rows and bodies remain exact historical pre-upload metadata. Their older physical whitelist and census are superseded for this delivery by the operative references above.'},
        'counts': {'declaredProducerSourcePaths': 387, 'receiverProvenanceSourceAliases': len(source_rows),
                   'finiteMappedSourcePaths': census['allSourcePathCount'], 'mainReceiverPaths': len(current),
                   'outerEnvelopePaths': envelope_count,
                   'finiteSourceAndOuterUnionPaths': census['allSourcePathCount'] + envelope_count,
                   'mainReceiverAndOuterUnionPaths': len(current) + envelope_count,
                   'qualifiedPaths': main_whitelist['qualifiedFileCount']},
        'physicalRepresentationDelta': main_whitelist['physicalRepresentationDelta'],
        'transportRefusalAttribution': 'Both retained connector failures were source-repository upload attempts. M3 receives the exact records as provenance under the shared request-size constraint; no separate failed M3 attempt is claimed.',
        'sourceProofCommit': source['sourceCommit'], 'receivingCommit': source['targetCommit'],
        'receivingHead': source['receivingHead'], 'originalSourcePublicationCommit': source['originalSourcePublicationCommit'],
        'proofGatesExecuted': False, 'runtimeAccepted': False, 'remoteWrites': False,
        'boundary': 'The original producer-source count, finite receiver-evidence census and outer envelopes are separate sets. Archives and qualification are unchanged. Outer whitelist controls and these CURRENT entry bytes are not inputs to their own finite census.'}
    note = ('# Current delivery metadata\n\n'
            'CURRENT-DELIVERY.json identifies the operative physical receiver selection, source mapping and finite census for this delivery. '
            'Use its `operative` relative paths under `delivery-envelope/archive-locator-v1/`.\n\n'
            'All17 older envelopes remain byte-identical historical pre-upload metadata at their original paths. '
            'Their earlier physical map/census are retained as history; the operative map/census supersedes them for this delivery.\n\n'
            'Only the redundant loose COPY-CUSTODY.json publication body is replaced by COPY-CUSTODY.ARCHIVE.json, '
            'and the two original connector-refusal records are added. The complete custody body remains in the unchanged archive; '
            'the locator and COPY_CUSTODY_ARCHIVE_RECORD.json bind its exact logical identity. All other main selection rows and all221 qualified paths are unchanged.\n\n'
            'Both recorded connector failures were source-repository upload attempts. M3 receives their exact records as provenance '
            'under the shared request-size constraint; no separate failed M3 upload attempt is claimed.\n\n'
            f'The derived finite census has {census["allSourcePathCount"]} paths: 387 declared producer-source paths plus {len(source_rows)} receiver provenance aliases. '
            f'It replaces the historical pre-upload {old_source["finiteMappedSourcePaths"]}-path census for this physical delivery. '
            f'The {envelope_count} outer envelope paths are excluded from that census. The source union has {census["allSourcePathCount"] + envelope_count} paths, '
            f'and the main receiver selection plus outer envelopes has {len(current) + envelope_count}; these are publication subsets, not repository-wide coverage.\n\n'
            'The actual bounded full consumer-v5 PASS, seven historical trial dispositions, archive bytes and proof receipts remain unchanged. '
            'This metadata operation runs no proof gate, performs no remote write and grants no broader JDK product runtime admission. '
            'The outer source/receiver whitelist controls are outside their own files and the finite census.\n')

    generated = {'CURRENT-DELIVERY.json': encoded(current_document), 'CURRENT-DELIVERY.md': note.encode()}
    source_files = list(old_source['files'])
    receiver_files = list(old_receiver['files'])
    pairs = list(old_mapping)

    def append_item(suffix, file, body):
        common = {'localPath': str(Path(file).absolute()), 'mode': '100644', 'type': 'blob',
                  'sha': blob(body), 'gitBlob': blob(body), 'sha256': sha(body), 'bytes': len(body),
                  'group': 'publication-envelope'}
        source_path, destination = SOURCE_ROOT + '/' + suffix, TASK + '/' + suffix
        source_files.append(dict(common, path=source_path))
        receiver_files.append(dict(common, path=destination))
        pairs.append({'sourcePath': source_path, 'receivingPath': destination,
                      'sha256': sha(body), 'bytes': len(body), 'gitBlob': blob(body)})

    for name, path in items:
        append_item(NEW_SUFFIX + name, path, read(path))
    for name, body in generated.items():
        append_item(name, out / name, body)
    require(source_files[:17] == old_source['files'] and receiver_files[:17] == old_receiver['files'], 'preserve all17 original row dictionaries')
    require(len({row['path'] for row in source_files}) == len({row['path'] for row in receiver_files}) == envelope_count,
            'unique outer envelope sets')
    require(not {row['path'] for row in source_files} & set(census['allSourcePaths'])
            and not {row['path'] for row in receiver_files} & set(current), 'outer envelopes disjoint from operative finite/main cohorts')
    common = {'schema': 'synexia.current-delivery-envelope-whitelist/1',
              'state': 'EXACT_OUTER_ENVELOPES_OUTSIDE_FINITE_SOURCE_CENSUS',
              'consumerResultSHA256': RESULT_SHA, 'consumerManifestSHA256': MANIFEST_SHA,
              'mainReceiverWhitelistSHA256': args.receiver_whitelist_sha,
              'finiteSourceWhitelistSHA256': args.source_whitelist_sha,
              'finiteSourceCensusSHA256': args.coverage_append_sha,
              'finiteMappedSourcePaths': census['allSourcePathCount'], 'fileCount': envelope_count,
              'bytes': sum(row['bytes'] for row in source_files), 'byteIdenticalSourceReceivingMapping': pairs,
              'historicalEnvelopeCount': 17, 'historicalRowsAndBodiesPreserved': True,
              'proofGatesExecuted': False, 'remoteWrites': False, 'runtimeAccepted': False,
              'boundary': 'Old17 envelopes retain their original historical metadata. CURRENT-DELIVERY identifies the operative map/census. These outer files and whitelist controls do not enter their own finite census; controls are also outside their own files.'}
    generated['source-supplement.json'] = encoded(dict(common, role='source', files=source_files))
    generated['receiver-supplement.json'] = encoded(dict(common, role='receiver', files=receiver_files))
    for row in inputs.values():
        body = Path(row['file']).read_bytes()
        require(sha(body) == row['sha256'] and len(body) == row['bytes'], 'input drift before publication-only write')
    out.mkdir(parents=True, exist_ok=False)
    for name, body in generated.items():
        with (out / name).open('xb') as stream:
            stream.write(body)
    print(json.dumps({'state': 'APPEND_ONLY_OPERATIVE_ENVELOPES_PREPARED', 'counts': current_document['counts'],
                      'outputs': {name: sha(body) for name, body in generated.items()},
                      'proofGatesExecuted': False, 'remoteWrites': False}))


if __name__ == '__main__':
    main()
