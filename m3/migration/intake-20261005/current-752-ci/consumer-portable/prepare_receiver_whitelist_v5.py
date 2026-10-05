#!/usr/bin/env python3
"""Select exact qualified receiver bytes and separately classified provenance."""
from pathlib import Path
import argparse
import hashlib
import json

R = Path(__file__).resolve().parents[1]
TASK = 'm3/migration/intake-20261005/current-752-ci'
MANIFEST_SHA = '8c3d4e6a28d4314a1de4c3a15c3616a371e04ac2eaf6357852c6ce28c1642db4'
JCC_CRATE = 'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-handoff-20261005'
ADDED_TARGETS = {'m3/migration/test/test_jcc_handoff_current_context.py', JCC_CRATE+'/plan-intake-752-ci.json', JCC_CRATE+'/pom-before-752.xml'}
PRIOR569_EXCEPTIONS = {'m3/migration/test/test_jcc_handoff_context_profiles.py', 'm3/migration/test/test_jcc_handoff_packet.py'}
PORTABLE = {
    'producer-portable-v1': '4682c5402aeb13219555a8bf2addd286f5088eb8055c8b887523a68a624f2847',
    'cir-producer-portable-v1': 'cf090e7f50ba798f7983d8a2bb0d3a0f9377c15864e7d50a258d61900f07e889',
    'cia-producer-portable-v1': 'c9c5011e73eafac8aaec5a12c60f005994e743fdf85e7df225b0b1a4c18f564b',
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def load(path):
    return json.loads(path.read_bytes())


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, sort_keys=True, indent=2) + '\n')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--result-sha', required=True)
    parser.add_argument('--package', type=Path, required=True)
    parser.add_argument('--cic-mapping', type=Path, required=True)
    parser.add_argument('--cic-mapping-sha', required=True)
    parser.add_argument('--cih-mapping', type=Path, required=True)
    parser.add_argument('--cih-mapping-sha', required=True)
    parser.add_argument('--coverage', type=Path, required=True)
    parser.add_argument('--coverage-sha', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    proof = R / 'evidence/consumer-v5'
    manifest_path = R / 'consumer-manifest-v5.json'
    require(sha(manifest_path.read_bytes()) == MANIFEST_SHA, 'exact final manifest')
    manifest = load(manifest_path)
    result_path = proof / 'RESULT.json'
    require(sha(result_path.read_bytes()) == args.result_sha, 'exact final result')
    result = load(result_path)
    require(result['state'] == 'PASS_BOUNDED_CURRENT_CONSUMERS' and result['failed'] is None,
            'actual full current consumer PASS required')
    require(result['runtimeAccepted'] is False and result['productWrites'] == 0
            and result['canonicalProductionApplied'] is False, 'bounded acceptance unchanged')
    require(sha((proof / 'authority/manifest.json').read_bytes()) == MANIFEST_SHA,
            'consumer proof binds final manifest')
    require(sha((proof / 'inputs.json').read_bytes()) == result['inputsSHA256'], 'original input seal')
    for rel, digest in result['evidence'].items():
        require(sha((proof / rel).read_bytes()) == digest, 'original evidence drift: ' + rel)
    require(not args.output.exists(), 'fresh whitelist output')
    rows = {}

    def add(path, file, group, expected=None, mode='100644'):
        file = Path(file).absolute()
        require(file.is_file() and not file.is_symlink(), 'regular publication body')
        require(not Path(path).is_absolute() and '..' not in Path(path).parts, 'canonical target path')
        data = file.read_bytes()
        blob = hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()
        row = dict(path=path, localPath=str(file), mode=mode, type='blob', sha=blob,
                   sha256=sha(data), bytes=len(data), group=group)
        if expected is not None:
            require(row['sha256'] == expected['sha256'] and row['bytes'] == expected['bytes']
                    and row['sha'] == expected.get('sha', expected.get('gitBlob')), 'published body drift: ' + path)
        if path in rows:
            require(all(rows[path][key] == row[key] for key in ('sha', 'sha256', 'bytes', 'mode')),
                    'conflicting duplicate: ' + path)
        else:
            rows[path] = row

    targets = {entry['path']: entry for entry in manifest['producerOutputs']}
    require(len(targets) == manifest['provenance']['declaredOutputCount'] == 24, 'twenty-four exact outputs')
    require(manifest['provenance']['addedTargetPaths'] == sorted(ADDED_TARGETS)
            and manifest['provenance']['outputOperations'] == {path: 'ADD' if path in ADDED_TARGETS else 'REPLACE' for path in targets},
            'exact three ABSENT additions and twenty-one replacements')
    require({entry['path'] for entry in manifest['provenance']['historical569ActiveExceptions']} == PRIOR569_EXCEPTIONS,
            'only two qualified replacements of the prior569 active paths')
    qualified = []
    for entry in manifest['files']:
        path = entry['path']
        if path in targets or path.startswith(TASK + '/'):
            add(path, proof / 'source' / path, 'qualified-repair' if path in targets else 'qualified-task', entry)
            qualified.append(path)
    require(set(targets).issubset(rows), 'all generated outputs selected')
    for folder, pin in PORTABLE.items():
        mapping_path = R / 'publication-extra' / folder / 'MAPPING.json'
        require(sha(mapping_path.read_bytes()) == pin, 'portable mapping seal')
        mapping = load(mapping_path)
        for entry in mapping['files']:
            require(entry['path'].startswith(TASK + '/'), 'portable scope')
            add(entry['path'], entry['file'], 'provenance-addition', entry, entry['mode'])
        add(TASK + '/delivery-mappings/' + folder + '.json', mapping_path, 'provenance-addition')
    require(sha(args.cic_mapping.read_bytes()) == args.cic_mapping_sha, 'Cic mapping seal')
    cic_mapping = load(args.cic_mapping)
    require(cic_mapping['producerResultSHA256'] == manifest['provenance']['cicProducerResultSHA256'],
            'actual selected Cic producer mapping')
    for entry in cic_mapping['files']:
        require(entry['path'].startswith(TASK + '/cic-producer-portable/'), 'Cic portable scope')
        add(entry['path'], entry['file'], 'provenance-addition', entry, entry['mode'])
    add(TASK + '/delivery-mappings/cic-producer-portable-v1.json', args.cic_mapping, 'provenance-addition')
    require(sha(args.cih_mapping.read_bytes()) == args.cih_mapping_sha, 'Cih mapping seal')
    cih_mapping = load(args.cih_mapping)
    require(cih_mapping['producerResultSHA256'] == manifest['provenance']['cihProducerResultSHA256'],
            'actual selected Cih producer mapping')
    for entry in cih_mapping['files']:
        require(entry['path'].startswith(TASK + '/cih-producer-portable/'), 'Cih portable scope')
        add(entry['path'], entry['file'], 'provenance-addition', entry, entry['mode'])
    add(TASK + '/delivery-mappings/cih-producer-portable-v1.json', args.cih_mapping, 'provenance-addition')
    require(sha(args.coverage.read_bytes()) == args.coverage_sha, 'source coverage mapping seal')
    coverage = load(args.coverage)
    for entry in coverage['publicationAdditions']:
        require(entry['path'].startswith(TASK + '/'), 'source provenance scope')
        add(entry['path'], entry.get('localPath', entry.get('file')), 'provenance-addition', entry,
            entry.get('mode', '100644'))
    add(TASK + '/source-delivery-coverage.json', args.coverage, 'provenance-addition')
    package = args.package.absolute()
    expected_results = {
        'consumer-v1': {'resultSHA256': '47a1efd8b72eb173d3c48124e549e7ecb28d332be09e97bc63290989ba34ba92', 'state': 'STOPPED'},
        'consumer-v2': {'resultSHA256': 'd8591a5652baf9c92902e6b1475b773fec4a1f498c1dbd6fe55dcffc002038d7', 'state': 'STOPPED'},
        'consumer-v3': {'resultSHA256': '0d50bac3af04f4c1b498ca34c3b6c57ad76bf97aa3b51fbba1dc926d4b52d2a7', 'state': 'STOPPED'},
        'receiving-python-v1': {'resultSHA256': 'be337d009319bab7c528f4ab6e61ada6ef5f407a33d070729e8d5cd68937a63e', 'state': 'PASS_BOUNDED_PYTHON_RECEIVING_COMPONENTS'},
        'consumer-v4': {'resultSHA256': '65ad6376f5e88ca2bd1e33e549c262a50bc58952ede7af8c17c9b0beed05ed73', 'state': 'STOPPED'},
        'receiving-python-v2': {'resultSHA256': '687eed40f8cfd7c93a663dbce1bb9fbf7a80626d798fa2f502e73c1a4bbab0cc', 'state': 'PASS_BOUNDED_PYTHON_RECEIVING_COMPONENTS'},
        'consumer-v5': {'resultSHA256': args.result_sha, 'state': 'PASS_BOUNDED_CURRENT_CONSUMERS'},
    }
    custody = load(package / 'staged/COPY-CUSTODY.json')
    plan_path = package / 'staged/package-plan.json'
    package_plan = load(plan_path)
    require(custody['terminalResults'] == package_plan['terminalResults'] == expected_results,
            'exact original seven terminal consumer dispositions')
    require(set(package_plan['laneSelection']['required']) == set(expected_results)
            and package_plan['laneSelection']['additional'] == []
            and package_plan['laneSelection']['allowNotExecuted'] == []
            and package_plan['laneSelection']['includeAllExisting'] is False,
            'exact package lane selection')
    require(package_plan['inputManifestPaths'] == sorted('evidence/' + lane + '/inputs.json' for lane in expected_results),
            'all seven actual lane-root input arrays retain reference auditing')
    require(sha((package / 'staged/evidence/consumer-v5/RESULT.json').read_bytes()) == args.result_sha
            and sha((package / 'staged/evidence/consumer-v5/authority/manifest.json').read_bytes()) == MANIFEST_SHA,
            'exact copied final result and manifest')
    bundle_path = package / 'evidence-bundle/BUNDLE.json'
    bundle = load(bundle_path)
    bundle_sha = sha(bundle_path.read_bytes())
    require(bundle['plan'] == package_plan and bundle['planSHA256'] == sha(plan_path.read_bytes())
            and bundle['sourceRoot'] == str(package / 'staged') and bundle['proofGatesExecuted'] is False,
            'current bundle belongs to the exact copied proof package')
    for entry in bundle['parts'] + bundle['topFiles']:
        rel = Path(entry['path'])
        require(not rel.is_absolute() and '..' not in rel.parts, 'bounded archive member path')
        data = (package / 'evidence-bundle' / rel).read_bytes()
        require(len(data) == entry['bytes'] and sha(data) == entry['sha256'], 'archive/index body drift')
    lanes = load(package / 'evidence-bundle/LANES.json')
    require(len(lanes) == len(expected_results) and {entry['lane'] for entry in lanes} == set(expected_results),
            'exact archive lane markers')
    for entry in lanes:
        lane = entry['lane']
        original_bytes = (R / 'evidence' / lane / 'RESULT.json').read_bytes()
        require(entry['marker'] == 'evidence/' + lane + '/RESULT.json'
                and entry['markerSHA256'] == expected_results[lane]['resultSHA256'] == sha(original_bytes)
                and entry['markerBytes'] == len(original_bytes)
                and entry['originalResult'] == json.loads(original_bytes)
                and entry['reportedStatus'] == expected_results[lane]['state'], 'retained actual result marker')
    package_result = load(package / 'evidence-bundle/PACKAGE.json')
    require(package_result['indexSHA256'] == bundle_sha and package_result['indexBytes'] == bundle_path.stat().st_size
            and package_result['summary'] == bundle['summary'] and package_result['proofGatesExecuted'] is False,
            'package result seals current bundle index')
    command = load(package / 'PACK-COMMAND.json')
    require(command['exitCode'] == 0 and command['proofGatesExecuted'] is False, 'actual package success')
    verification = load(package / 'VERIFY-COMMAND.json')
    require(verification['exitCode'] == 0 and verification['proofGatesExecuted'] is False, 'actual archive integrity verification')
    packager = R / 'packaging/package_evidence_state_v2.py'
    require(sha(packager.read_bytes()) == 'c7d0d838814be81b627810ec587e1d21d2841d922b0791a4c60642386c9ea39d',
            'retained reviewed archive owner')
    python = str(R.parent.parent / 'm3-python/bin/python')
    require(command['command'] == [python, str(packager), 'pack', '--root', str(package / 'staged'),
                                  '--plan', str(plan_path), '--output', str(package / 'evidence-bundle')]
            and verification['command'] == [python, str(packager), 'verify', '--bundle', str(package / 'evidence-bundle')],
            'exact package/verify commands and selected paths')
    for label, receipt in [('pack', command), ('verify', verification)]:
        for stream in ('stdout', 'stderr'):
            require(sha((package / (label + '.' + stream + '.log')).read_bytes()) == receipt[stream + 'SHA256'],
                    'actual package/verify log identity')
    pack_stdout = load(package / 'pack.stdout.log')
    verify_stdout = load(package / 'verify.stdout.log')
    require(pack_stdout['status'] == 'PACKAGED_REVIEW_EVIDENCE'
            and pack_stdout['output'] == str(package / 'evidence-bundle')
            and pack_stdout['summary'] == bundle['summary'] and pack_stdout['proofGatesExecuted'] is False,
            'actual pack log belongs to selected bundle')
    require(verify_stdout['status'] == 'ARCHIVE_INTEGRITY_VERIFIED'
            and verify_stdout['bundleIndexSHA256'] == bundle_sha and verify_stdout['reconstructed'] is False
            and verify_stdout['proofGatesExecuted'] is False, 'actual verifier seals current bundle')
    for file in sorted((package / 'evidence-bundle').rglob('*')):
        if file.is_file():
            add(TASK + '/consumer-portable/evidence-bundle/' + file.relative_to(package / 'evidence-bundle').as_posix(),
                file, 'provenance-addition')
    for name in ('PACK-COMMAND.json', 'pack.stdout.log', 'pack.stderr.log', 'VERIFY-COMMAND.json', 'verify.stdout.log', 'verify.stderr.log'):
        add(TASK + '/consumer-portable/' + name, package / name, 'provenance-addition')
    for name in ('COPY-CUSTODY.json', 'package-plan.json', 'package_evidence_state_v2.py', 'state-reader.patch', 'STATE-READER.json', 'reference-scope.patch', 'REFERENCE-SCOPE.json', 'prepare_consumer_package_v7.py'):
        add(TASK + '/consumer-portable/' + name, package / 'staged' / name, 'provenance-addition')
    for file in sorted((package / 'staged/openjdk-752-attribution').rglob('*')):
        if file.is_file():
            add(TASK + '/consumer-portable/openjdk-752-attribution/' + file.relative_to(package / 'staged/openjdk-752-attribution').as_posix(), file, 'provenance-addition')
    for name in ('RESULT.json', 'PATCH.json', 'lint-report.json', 'compile-report.json', 'test-report.json', 'runtime-report.json'):
        add(TASK + '/consumer-portable/final/' + name, proof / name, 'provenance-addition')
    add(TASK + '/consumer-portable/prepare_receiver_whitelist_v5.py', Path(__file__), 'provenance-addition')
    write(args.output, {
        'schema': 'm3.current-receiver-publication-whitelist/1',
        'state': 'EXACT_QUALIFIED_CONTEXT_AND_SEPARATE_PROVENANCE',
        'consumerResultSHA256': args.result_sha, 'consumerManifestSHA256': MANIFEST_SHA,
        'sourceDeliveryCoverageSHA256': args.coverage_sha,
        'addedTargetPaths': manifest['provenance']['addedTargetPaths'],
        'historical569ActiveExceptions': manifest['provenance']['historical569ActiveExceptions'],
        'consumerBundleSHA256': sha((package / 'evidence-bundle/BUNDLE.json').read_bytes()),
        'qualifiedPaths': sorted(qualified), 'files': [rows[path] for path in sorted(rows)],
        'qualifiedFileCount': len(qualified), 'fileCount': len(rows),
        'provenanceAdditionsGainNoFurtherQualification': True,
        'proofGatesExecutedByPublication': False, 'remoteWrites': False,
        'runtimeAccepted': False, 'runtimeAcceptanceMeaning': 'JDK product/HotSpot runtime admission remains unclaimed; the separate bounded host/Java/JNI checks retain their actual outcomes.',
    })
    print(json.dumps({'fileCount': len(rows), 'qualifiedFiles': len(qualified),
                      'whitelistSHA256': sha(args.output.read_bytes())}))


if __name__ == '__main__':
    main()
