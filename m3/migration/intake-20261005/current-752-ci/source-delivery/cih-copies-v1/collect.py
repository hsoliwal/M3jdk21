#!/usr/bin/env python3
"""Bounded Cih publication copies and source mappings; executes no proof code."""
from pathlib import Path, PurePosixPath
import hashlib
import io
import json
import tarfile

HERE = Path(__file__).resolve().parent
R = HERE.parents[2]
D = R.parent / 'ci-repairs'
TASK = 'm3/migration/intake-20261005/current-752-ci'
OUT = R / 'publication-extra/cih-producer-portable-v1'
PREFIX = 'synexia-openrewrite-recipes/verification/ci-current-20261005/cih-current/'
RECEIVING = TASK + '/cih-producer-portable/'
SOURCE_PIN = '9a3cb490fb4f566fca4c47cdcb10e6226e8ea1251e641df15a14f0b85327426b'
PORTABLE_PIN = '69f12df98f27fcf536e4645109f9e19c75f7ebc290190188fee65adb07266b42'
PRODUCER_PIN = 'f93e1ef05b82459105b8dacd6258af0caa86f0e2c2221f946e8250397ffa9dcb'
PRIOR_PIN = '4e6b41c9f13931272dbd82f18c8a53b179b0f1293f135bb722bb8056f642d084'
SELECTION_PIN = 'a313bd02a2518628fba0c17d77244187e3fc6d073a72aaa3fc300777b85ff5d7'


def require(value, message):
    if not value:
        raise ValueError(message)


def sha(body):
    return hashlib.sha256(body).hexdigest()


def blob(body):
    return hashlib.sha1(b'blob ' + str(len(body)).encode() + b'\0' + body).hexdigest()


def rel(value):
    require(value and not PurePosixPath(value).is_absolute()
            and '\\' not in value and all(x not in ('', '.', '..') for x in value.split('/')),
            'unsafe relative path')
    return value


def read(path):
    path = Path(path)
    require(path.is_file() and not path.is_symlink(), 'regular file required: ' + str(path))
    return path.read_bytes()


def check(body, row):
    require(len(body) == row['bytes'] and sha(body) == row['sha256'], 'file identity drift')
    for key in ('gitBlob', 'sha'):
        if key in row:
            require(blob(body) == row[key], 'Git identity drift')


def reference(path):
    return {'file': str(path), 'sha256': sha(read(path))}


def sealed(path, pin):
    body = read(path)
    require(sha(body) == pin, 'document drift: ' + str(path))
    return json.loads(body)


def write(path, document):
    with path.open('x') as stream:
        stream.write(json.dumps(document, indent=2, sort_keys=True) + '\n')


def main():
    require(not OUT.exists(), 'fresh portable copy destination required')
    require(not (HERE / 'source-mapping.json').exists(), 'fresh mapping required')
    source_path = D / 'CIH-PUBLICATION-WHITELIST.json'
    portable_path = D / 'PORTABLE-CIH-WHITELIST.json'
    prior_path = HERE.parent / 'cic-copies-v1/coverage-append.json'
    selection_path = R / 'publication-extra/source-delivery-v1/SELECTION.json'
    source = sealed(source_path, SOURCE_PIN)
    portable = sealed(portable_path, PORTABLE_PIN)
    prior = sealed(prior_path, PRIOR_PIN)
    selection = sealed(selection_path, SELECTION_PIN)
    require(prior['allSourcePathCount'] == 305, 'exact prior universe')
    require(len(selection['publicationAdditions']) == 49, 'retained selection inventory')
    for row in selection['publicationAdditions']:
        check(read(row['file']), row)
    for document, count, size in ((source, 50, 1789116), (portable, 32, 2097393)):
        require(document['producerResultSHA256'] == PRODUCER_PIN, 'producer identity')
        require(document['fileCount'] == len(document['files']) == count
                and document['bytes'] == sum(x['bytes'] for x in document['files']) == size,
                'exact whitelist inventory')
        require(len({x['path'] for x in document['files']}) == count, 'duplicate source paths')
        for row in document['files']:
            rel(row['path'])
            check(read(row['localPath']), row)
    require(sha(read(D / 'evidence/cih-producer-v3/RESULT.json')) == PRODUCER_PIN,
            'actual producer result changed')
    new_paths = [x['path'] for document in (source, portable) for x in document['files']]
    require(len(set(new_paths)) == 82 and not set(new_paths) & set(prior['allSourcePaths']),
            'new source paths must be disjoint')

    bundle_path = D / 'portable-cih/evidence-bundle/BUNDLE.json'
    bundle = sealed(bundle_path, portable['bundleIndexSHA256'])
    members = {}
    for part in bundle['parts']:
        body = read(bundle_path.parent / rel(part['path']))
        check(body, part)
        with tarfile.open(fileobj=io.BytesIO(body), mode='r:gz') as archive:
            require(len(archive.getmembers()) == part['members'], 'part member count')
            for member in archive.getmembers():
                rel(member.name)
                require(member.isfile() and member.name not in members, 'duplicate or special member')
                members[member.name] = archive.extractfile(member).read()
    for row in bundle['topFiles']:
        check(read(bundle_path.parent / rel(row['path'])), row)
    logical = {}
    verified_rows = []
    for shard in bundle['metadataShards']:
        body = members[rel(shard['member'])]
        check(body, shard)
        rows = [json.loads(line) for line in body.splitlines()]
        require(len(rows) == shard['records'], 'shard record count')
        if shard['kind'] != 'files':
            continue
        for row in rows:
            name = rel(row['path'])
            require(name not in logical, 'duplicate logical file')
            pieces = []
            for chunk in row['chunks']:
                data = members['objects/' + chunk['sha256'][:2] + '/' + chunk['sha256']]
                check(data, chunk)
                pieces.append(data)
            data = b''.join(pieces)
            check(data, row)
            logical[name] = data
            verified_rows.append({key: row[key] for key in ('path', 'sha256', 'bytes')})
    require(len(logical) == bundle['summary']['retainedPaths'] == 386, 'complete logical archive')

    manifest_path = R / 'consumer-manifest-v5.json'
    manifest = json.loads(read(manifest_path))
    candidate = R / 'candidate-v5'
    receiving_matches = {}
    for row in manifest['files']:
        data = read(candidate / rel(row['path']))
        check(data, row)
        receiving_matches.setdefault((row['sha256'], row['bytes']), []).append(row['path'])
    require(len(manifest['producerOutputs']) == 24, 'current receiver scope')

    physical = []
    for row in portable['files']:
        require(row['path'].startswith(PREFIX), 'closed Cih portable prefix')
        tail = rel(row['path'][len(PREFIX):])
        physical.append({'sourcePath': row['path'], 'path': RECEIVING + tail,
                         'file': str(OUT / 'files' / tail), 'sha': row['gitBlob'],
                         'sha256': row['sha256'], 'bytes': row['bytes'], 'mode': row['mode'],
                         'type': 'blob', 'group': 'provenance-only-publication-addition'})
    by_source = {row['sourcePath']: row for row in physical}
    selected = []
    observations = []
    for row in source['files']:
        data = read(row['localPath'])
        aliases = sorted(name for name, body in logical.items() if body == data)
        copies = sorted((x for x in physical if (x['sha256'], x['bytes']) ==
                         (row['sha256'], row['bytes'])), key=lambda x: x['path'])
        common = {key: row[key] for key in ('sha256', 'bytes', 'gitBlob')}
        common['sourcePath'] = row['path']
        if row['group'] == 'actual-qualified-task-input':
            task_prefix = 'synexia-openrewrite-recipes/tasks/m3-ci-752-jcc/'
            require(row['path'].startswith(task_prefix), 'qualified task path')
            chosen = 'evidence/cih-producer-v3/project/' + row['path'][len(task_prefix):]
            require(chosen in aliases, 'actual qualified project body absent from bundle')
            mapping = dict(common, bundleFile=str(bundle_path), bundleSHA256=portable['bundleIndexSHA256'],
                           logicalPath=chosen, receivingBundle=RECEIVING + 'evidence-bundle/BUNDLE.json')
        elif copies:
            mapping = dict(common, path=copies[0]['path'], file=copies[0]['file'])
        else:
            require(aliases, 'source body missing from both loose copies and logical archive')
            chosen = Path(row['localPath']).relative_to(D).as_posix()
            require(chosen in aliases, 'exact authoring metadata logical path absent')
            mapping = dict(common, bundleFile=str(bundle_path), bundleSHA256=portable['bundleIndexSHA256'],
                           logicalPath=chosen, receivingBundle=RECEIVING + 'evidence-bundle/BUNDLE.json')
        matches = receiving_matches.get((row['sha256'], row['bytes']), [])
        selected.append(mapping)
        observations.append({'sourcePath': row['path'], 'sha256': row['sha256'], 'bytes': row['bytes'],
                             'selectedDelivery': mapping, 'portableLogicalMatches': aliases,
                             'portablePhysicalMatches': [x['path'] for x in copies],
                             'activeReceivingByteMatches': [x for x in matches if not x.startswith(TASK + '/')],
                             'receivingEvidenceByteMatches': [x for x in matches if x.startswith(TASK + '/')]})

    # All source/copy/logical/candidate identities are established before any publication copy.
    OUT.mkdir(parents=True)
    for source_row in portable['files']:
        target = Path(by_source[source_row['path']]['file'])
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open('xb') as stream:
            stream.write(read(source_row['localPath']))
        check(read(target), by_source[source_row['path']])
    mapping_path = OUT / 'MAPPING.json'
    write(mapping_path, {'schema': 'm3.producer-portable-delivery/1', 'sourcePrefix': PREFIX,
                        'sourceWhitelist': str(portable_path), 'sourceWhitelistSHA256': PORTABLE_PIN,
                        'producerResultSHA256': PRODUCER_PIN, 'bundleIndexSHA256': portable['bundleIndexSHA256'],
                        'files': physical, 'fileCount': len(physical), 'totalBytes': sum(x['bytes'] for x in physical),
                        'byteIdentical': True, 'frozenConsumerContextModified': False,
                        'proofGatesExecuted': False, 'runtimeAccepted': False, 'productWrites': 0,
                        'consumerQualificationClaim': 'None for these publication-only additions; the separate consumer proof binds only its sealed inputs.'})
    source_mapping_path = HERE / 'source-mapping.json'
    write(source_mapping_path, {'schema': 'm3.source-delivery-explicit-mapping/1', 'files': selected,
                               'consumerQualified': False, 'proofGatesExecuted': False})
    additions = []
    for path in (source_path, portable_path):
        data = read(path)
        additions.append({'path': TASK + '/source-admission-evidence/source-whitelists/' + path.name,
                          'file': str(path), 'sha': blob(data), 'gitBlob': blob(data), 'sha256': sha(data),
                          'bytes': len(data), 'mode': '100644', 'type': 'blob',
                          'group': 'provenance-only-publication-addition',
                          'sourceInput': 'ci-repairs/' + path.name, 'outsideKnown387SourcePathUniverse': True})
    write(HERE / 'delivery-audit.json', {
        'schema': 'm3.cih-delivery-audit/1', 'state': 'VERIFIED_BYTES_AND_EXPLICIT_DELIVERY_PATHS',
        'sourceWhitelist': reference(source_path), 'portableWhitelist': reference(portable_path),
        'portableMapping': reference(mapping_path), 'bundle': reference(bundle_path), 'parts': bundle['parts'],
        'producerResultSHA256': PRODUCER_PIN, 'verifiedLogicalFileCount': len(logical),
        'verifiedLogicalFiles': sorted(verified_rows, key=lambda x: x['path']), 'sourceObservations': observations,
        'candidate': str(candidate), 'receiverManifest': reference(manifest_path),
        'candidateFileCount': len(manifest['files']), 'producerOutputCount': len(manifest['producerOutputs']),
        'priorSourceSelection': reference(selection_path), 'priorSelectedPublicationAdditionCount': 49,
        'priorSelectionModified': False, 'newKnownSourcePaths': 82, 'missingWhitelistedSourceFiles': 0,
        'publicationAdditions': additions, 'publicationAdditionBytes': sum(x['bytes'] for x in additions),
        'proofGatesExecuted': False, 'consumerQualified': False,
        'boundary': 'Known387 counts declared source paths only. The two enclosing Cih whitelist documents are separate provenance-envelope inputs. All49 previously selected provenance files remain unchanged. This content census claims no consumer-v5 result or remote publication.'})
    write(HERE / 'plan.json', {'schema': 'm3.source-delivery-append-plan/1', 'label': 'cih-source50-and-outer32',
                              'prior': reference(prior_path), 'segments': [
                                  {'id': 'cih-source-50', 'whitelist': reference(source_path),
                                   'mapping': reference(source_mapping_path)},
                                  {'id': 'cih-portable-outer-32', 'whitelist': reference(portable_path),
                                   'mapping': reference(mapping_path)}]})
    sealed(prior_path, PRIOR_PIN)
    sealed(selection_path, SELECTION_PIN)
    sealed(source_path, SOURCE_PIN)
    sealed(portable_path, PORTABLE_PIN)
    for row in selection['publicationAdditions']:
        check(read(row['file']), row)
    for path in (mapping_path, source_mapping_path, HERE / 'delivery-audit.json', HERE / 'plan.json'):
        print(json.dumps(dict(reference(path), bytes=len(read(path)))))


if __name__ == '__main__':
    main()
