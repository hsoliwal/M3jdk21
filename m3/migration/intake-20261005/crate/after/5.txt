#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Stage portable actual-output receipts; never execute or promote a product port.

The caller provides an immutable upstream proof binding file. Original complete
upstream custody must already have been checked by its existing sealed runner.
This preparer preserves original receipt bytes and makes no independent claim
about unshipped toolchains, class files, upstream checkout, or JDK acceptance.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path):
    return json.loads(path.read_text(encoding='utf-8'))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8') as stream:
        stream.write(json.dumps(value, indent=2) + '\n')


def copy(source, destination):
    if source.is_symlink() or not source.is_file():
        raise ValueError('exact regular upstream file required: ' + str(source))
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        if destination.read_bytes() != source.read_bytes():
            raise ValueError('portable proof collision')
        return
    shutil.copyfile(source, destination)


def proof(source, destination, expected, baseline):
    result = load(source / 'RESULT.json')
    if sha(source / 'RESULT.json') != expected or result['status'] != 'PASS_FOCUSED_TEST_PROJECT' or result['baseline'] != baseline:
        raise ValueError('exact successful upstream result required')
    if (source / 'STOPPED.json').exists():
        raise ValueError('stopped proof cannot be staged')
    names = ['RESULT.json', 'PATCH.json', 'inputs.json', 'commands.json', 'staging.json',
             'tooling.json', 'build-artifacts.json', 'junit-report.json', 'runtime-report.json']
    names += [str(path.relative_to(source)) for path in sorted((source / 'chain').glob('*')) if path.is_file()]
    names += [str(path.relative_to(source)) for path in sorted(source.glob('runtime-*')) if path.is_file()]
    names += [str(path.relative_to(source)) for path in sorted((source / 'target/surefire-reports').glob('TEST-*.xml'))]
    for name in names:
        copy(source / name, destination / name)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--bindings', type=Path, required=True)
    parser.add_argument('--bindings-sha256', required=True)
    parser.add_argument('--out', default='m3/migration/intake-20261005/portable')
    args = parser.parse_args()
    root = args.root.resolve()
    if sha(args.bindings) != args.bindings_sha256:
        raise ValueError('upstream binding drift')
    if args.out.startswith('/') or '\\' in args.out or any(part in ('', '.', '..') for part in args.out.split('/')):
        raise ValueError('canonical relative output required')
    destination = root / args.out
    if destination.exists() or any(p.is_symlink() for p in (destination, *destination.parents)):
        raise ValueError('fresh portable output required')
    task = root / 'm3/migration/intake-20261005'
    plan = load(task / 'output-plan.json')
    bindings = load(args.bindings)
    if set(bindings) != {'schema', 'lanes'} or bindings['schema'] != 'm3.synexia-intake-bindings/1':
        raise ValueError('unsupported binding shape')
    grouped = {}
    for row in plan['expectedOutputs']:
        grouped.setdefault(row['lane'], []).append(row)
    if set(bindings['lanes']) != set(grouped):
        raise ValueError('exact admitted lane set required')
    lanes = []
    for name, expected in sorted(grouped.items()):
        binding = bindings['lanes'][name]
        source = Path(binding['recipe']['proof']).resolve()
        parent_digest = binding['recipe']['resultSHA256']
        proof(source, destination / name / 'recipe', parent_digest, plan['source']['commit'])
        actual = source / 'generated-candidate'
        protocols = {
            'com.synexia.rewrite.Mre': ('mre', {'mre'}, {'mre-v1'}),
            'com.synexia.rewrite.Mre2': ('mre', {'mre', 'mre2'}, {'mre-v2'}),
            'com.synexia.rewrite.Pcr': ('pcr', {'pcr'}, {'pcs-reconciled-v1'}),
            'com.synexia.rewrite.Mrc': ('mrc', {'mrc'}, {'mrc-v1'}),
            'com.synexia.rewrite.Mrc2': ('mrc', {'mrc', 'mrc2'}, {'mrc-v2'}),
            'com.synexia.rewrite.Mrc3': ('mrc', {'mrc', 'mrc2', 'mrc3'}, {'mrc-v3'}),
            'com.synexia.rewrite.Mrc4': ('mrc', {'mrc', 'mrc2', 'mrc4'}, {'mrc-v4'}),
            'com.synexia.rewrite.Mrp': ('mrp', {'mrp'}, {'mrp-v1', 'mrp-v2'}),
            'com.synexia.rewrite.Mrd': ('mrd', {'mrd'}, {'mrd-v1'})}
        recipe_id = expected[0]['recipe']
        protocol = protocols.get(recipe_id)
        if protocol is None or name not in protocol[1] or any(row['recipe'] != recipe_id for row in expected):
            raise ValueError('unsupported exact materializer protocol')
        family, _, allowed_crates = protocol
        extras = {'OUTPUT.json', 'candidate.patch'} if family != 'pcr' else set()
        all_files = sorted((p for p in actual.rglob('*') if p.is_file()),
                           key=lambda p: p.relative_to(actual).as_posix())
        if not all((actual / extra).is_file() for extra in extras):
            raise ValueError('actual output metadata missing: ' + name)
        files = [p for p in all_files if p.relative_to(actual).as_posix() not in extras]
        if [p.relative_to(actual).as_posix() for p in files] != [r['path'] for r in expected]:
            raise ValueError('actual output set differs: ' + name)
        rows, mappings = [], []
        for path, admitted in zip(files, expected):
            content = path.read_bytes()
            git_blob = hashlib.sha1(b'blob ' + str(len(content)).encode() + b'\0' + content).hexdigest()
            if sha(path) != admitted['sha256'] or git_blob != admitted['gitBlob']:
                raise ValueError('actual output differs from reviewed mapping: ' + admitted['path'])
            artifact = destination / name / 'outputs' / admitted['path']
            copy(path, artifact)
            rows.append({'path': admitted['path'], 'sha256': admitted['sha256'], 'gitBlob': git_blob,
                         'artifact': artifact.relative_to(root).as_posix()})
            mappings.append({'path': admitted['path'], 'id': admitted['mappingId'],
                             'disposition': admitted['disposition'], 'requiredGates': admitted['requiredGates']})
        materialized = destination / name / 'recipe/materialization'
        output = load(actual / 'OUTPUT.json') if family != 'pcr' else None
        crate = output.get('crate') if output is not None else 'pcs-reconciled-v1'
        if crate not in allowed_crates:
            raise ValueError('bounded materializer crate identity: ' + name)
        if output is not None and (output.get('recipe') != recipe_id
                                   or output.get('baseline') != plan['source']['commit']
                                   or output.get('schema') != 'synexia.' + family + '.output/1'):
            raise ValueError('exact recipe, family and single source baseline required: ' + name)
        resource = 'com/synexia/rewrite/hash-pinned-java/' + crate + '/manifest.tsv'
        source_manifest = source / 'sources/synexia-openrewrite-recipes/src/main/resources' / resource
        copied_manifest = materialized / 'manifest.tsv'
        copy(source_manifest, copied_manifest)
        materialization = {'manifest': {'path': copied_manifest.relative_to(root).as_posix(),
                                        'sha256': sha(copied_manifest)}, 'output': None, 'patch': None}
        for filename, key in (('OUTPUT.json', 'output'), ('candidate.patch', 'patch')):
            if family != 'pcr':
                copied = materialized / filename
                copy(actual / filename, copied)
                materialization[key] = {'path': copied.relative_to(root).as_posix(), 'sha256': sha(copied)}
        manifest = destination / name / 'outputs.json'
        write(manifest, {'sourceCommit': plan['source']['commit'], 'recipe': expected[0]['recipe'],
                         'files': rows, 'materialization': materialization})
        proofs = [{'role': 'recipe', 'result': {'path': (destination / name / 'recipe/RESULT.json').relative_to(root).as_posix(),
                                               'sha256': parent_digest}}]
        consumers = binding['consumers']
        if not consumers:
            raise ValueError('actual consumer proof required')
        for index, consumer in enumerate(consumers):
            target = destination / name / ('consumer-' + str(index))
            proof(Path(consumer['proof']).resolve(), target, consumer['resultSHA256'], plan['source']['commit'])
            proofs.append({'role': 'consumer', 'result': {'path': (target / 'RESULT.json').relative_to(root).as_posix(),
                                                         'sha256': consumer['resultSHA256']}})
        if family == 'pcr':
            seal_text = ''.join(row['path'] + '\t' + row['sha256'] + '\n' for row in rows)
        else:
            patch = actual / 'candidate.patch'
            seal_text = (family.upper() + '-OUTPUT/1\nmanifest\t' + sha(source_manifest)
                         + '\npatch\t' + sha(patch) + '\t' + str(patch.stat().st_size) + '\n')
            seal_text += ''.join(row['path'] + '\t' + row['sha256'] + '\t'
                                 + str((actual / row['path']).stat().st_size) + '\n' for row in rows)
        seal = hashlib.sha256(seal_text.encode()).hexdigest()
        if output is not None and output.get('outputSealSHA256') != seal:
            raise ValueError('original materialization seal differs: ' + name)
        lanes.append({'id': name, 'recipe': expected[0]['recipe'], 'manifest': {'path': manifest.relative_to(root).as_posix(),
                     'sha256': sha(manifest)}, 'outputSeal': seal, 'proofs': proofs, 'mappings': mappings})
    root_inventory = task / 'source-root.json'
    authority = root / 'm3/docs/name-mapping.json'
    packet = {'schema': 'm3.synexia-intake/1', 'source': plan['source'], 'target': plan['target'],
              'authority': {'path': authority.relative_to(root).as_posix(), 'sha256': sha(authority)},
              'sourceRoot': {'path': root_inventory.relative_to(root).as_posix(), 'sha256': sha(root_inventory)},
              'owners': load(task / 'owners.json'), 'lanes': lanes}
    write(destination / 'packet.json', packet)
    copy(args.bindings, destination / 'upstream-bindings.json')
    print(json.dumps({'status': 'PREPARED_NOT_ADMITTED', 'packet': str(destination / 'packet.json'),
                      'packetSHA256': sha(destination / 'packet.json'), 'actualOutputFiles': sum(len(g['mappings']) for g in lanes),
                      'upstreamBindingsSHA256': args.bindings_sha256, 'productWrites': 0}))


if __name__ == '__main__':
    main()
