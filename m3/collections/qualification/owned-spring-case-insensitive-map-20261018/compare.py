# SPDX-License-Identifier: Apache-2.0
# Composes the unchanged frozen Spring partition parser and actual recorded stdout.
import hashlib
import json
from pathlib import Path
import re
import sys

HERE = Path(__file__).resolve().parent

def sha(text):
    return hashlib.sha256(text.encode('utf-8')).hexdigest()

def parse(stdout, selectors):
    required = []
    compatibility = []
    summary = None
    observer_required = []
    for line in stdout.splitlines():
        if line.startswith('{'):
            row = json.loads(line)
            if 'case' in row:
                selected = compatibility if any(pattern.search(row['case']) for pattern in selectors) else required
                selected.append(line)
            elif 'status' in row:
                if summary is not None:
                    raise RuntimeError('duplicate fixture summary')
                summary = row
            else:
                raise RuntimeError('unclassified JSON fixture row')
        elif line.startswith('ENTRY_REQUIRED\t'):
            observer_required.append(line)
        elif line.startswith(('ENTRY_IDENTITY_NO_FIT\t', 'PRIVATE_WIRE_NO_FIT\t')):
            compatibility.append(line)
        else:
            raise RuntimeError('unclassified nonempty fixture output: ' + line[:100])
    if not summary or summary['status'] != 'COMMON_CLEAN_LAWS_PASSED_OBSERVATIONS_RECORDED':
        raise RuntimeError('missing passing clean-law summary')
    assert summary['ordinaryOperations'] == 4096
    assert len(required) + sum(1 for line in compatibility if line.startswith('{')) == summary['selectedObservations']
    assert len(observer_required) == 8
    assert len([line for line in compatibility if line.startswith('ENTRY_IDENTITY_NO_FIT\t')]) == 1
    assert len([line for line in compatibility if line.startswith('PRIVATE_WIRE_NO_FIT\t')]) == 1
    return {'common': summary, 'requiredPolicyRows': len(required), 'requiredEntryRows': len(observer_required),
            'requiredPolicySHA256': sha('\n'.join(required) + '\n'),
            'requiredEntrySHA256': sha('\n'.join(observer_required) + '\n'),
            'compatibilityRows': len(compatibility), 'compatibilitySHA256': sha('\n'.join(compatibility) + '\n'),
            'requiredPolicy': required, 'requiredEntry': observer_required, 'compatibility': compatibility}


def protocol(text):
    spec = json.loads((HERE / 'archive/proof-plan/BASELINE_PARTITIONS_V2.json').read_text(encoding='utf-8'))
    result = parse(text, [re.compile(pattern) for pattern in spec['compatibilitySelectors']])
    if result['requiredPolicyRows'] != 241 or result['requiredEntryRows'] != 8 or result['compatibilityRows'] != 7:
        raise SystemExit('frozen protocol partition cardinality differs')
    if result['common']['commonChecks'] != 196782:
        raise SystemExit('frozen common law-check cardinality differs')
    return result


def constructors(text):
    raw = text.splitlines()
    rows = [json.loads(line) for line in raw]
    if len(rows) != 33 or any('case' not in row for row in rows[:-1]):
        raise SystemExit('constructor record cardinality differs')
    summary = rows[-1]
    expected = {'status': 'PROVIDER_EMPTY_CONSTRUCTORS_RECORDED', 'invocations': 32,
        'returnedEmpty': 24, 'illegalArguments': 8, 'actualOutOfMemoryErrors': 0,
        'otherFailures': 0, 'traceSHA256': 'd84d32ffd8f6f2e23fc27392b91ad6f6e69859d2812b042464c7333f548b9037',
        'maxHeapBytes': 67108864, 'activeProcessors': 2}
    for key, value in expected.items():
        if summary.get(key) != value:
            raise SystemExit('empty constructor profile differs: ' + key)
    if sha('\n'.join(raw[:-1]) + '\n') != expected['traceSHA256']:
        raise SystemExit('constructor raw case trace differs')
    if not isinstance(summary.get('javaRuntime'), str) or not summary['javaRuntime'].startswith('21.'):
        raise SystemExit('Java21 constructor runtime missing')
    return rows


def main():
    output = Path(sys.argv[1])
    sealed = json.loads((HERE / 'proof-summary.json').read_text(encoding='utf-8'))
    recorded = protocol((HERE / 'recorded/provider-protocol.jsonl').read_text(encoding='utf-8'))
    provider = protocol((output / 'provider-protocol.jsonl').read_text(encoding='utf-8'))
    owned = protocol((output / 'owned-protocol.jsonl').read_text(encoding='utf-8'))
    for key in ('requiredPolicy', 'requiredEntry'):
        if provider[key] != recorded[key] or owned[key] != recorded[key]:
            raise SystemExit('required raw protocol records differ: ' + key)
    for key in ('ordinaryOperations', 'commonChecks', 'commonSHA256'):
        if provider['common'][key] != recorded['common'][key] or owned['common'][key] != recorded['common'][key]:
            raise SystemExit('common clean protocol differs: ' + key)
    surface = json.loads((output / 'surface.json').read_text(encoding='utf-8'))
    if surface != sealed['rootExecuted']['publicSurface']:
        raise SystemExit('complete mapped public surface differs')
    reference_constructors = constructors((HERE / 'recorded/provider-constructors.jsonl').read_text(encoding='utf-8'))
    provider_constructors = constructors((output / 'provider-constructors.jsonl').read_text(encoding='utf-8'))
    owned_constructors = constructors((output / 'owned-constructors.jsonl').read_text(encoding='utf-8'))
    if provider_constructors[:-1] != reference_constructors[:-1] or owned_constructors != provider_constructors:
        raise SystemExit('same-host constructor rows or runtime metadata differ')
    for label, count in [('getter', 9), ('equality', 6)]:
        reference = (HERE / ('recorded/provider-' + label + '.tsv')).read_text(encoding='utf-8')
        provider_effects = (output / ('provider-' + label + '.tsv')).read_text(encoding='utf-8')
        owned_effects = (output / ('owned-' + label + '.tsv')).read_text(encoding='utf-8')
        if len(reference.splitlines()) != count or provider_effects != reference or owned_effects != reference:
            raise SystemExit('supplemental raw caller effects differ: ' + label)
    projection = sealed['rootExecuted']['projection']
    for name in ['provider-projection.json', 'owned-projection-java.json']:
        if json.loads((output / name).read_text(encoding='utf-8')) != projection:
            raise SystemExit('entire public primitive projection differs: ' + name)
    native = output / 'owned-projection-native.json'
    if native.is_file() and json.loads(native.read_text(encoding='utf-8')) != projection:
        raise SystemExit('entire JNI public primitive projection differs')
    print(json.dumps({'status': 'PASS_REQUIRED_PARTITIONS_SURFACE_CONSTRUCTORS_CALLER_EFFECTS_PROJECTION',
        'requiredPolicyRows': 241, 'requiredEntryRows': 8, 'ordinaryOperations': 4096, 'commonChecks': 196782,
        'compatibilityRowsEach': 7, 'providerCompatibilitySHA256': provider['compatibilitySHA256'],
        'ownedCompatibilitySHA256': owned['compatibilitySHA256'], 'compatibilityParityClaim': False,
        'constructorInvocations': 32, 'constructorRuntime': provider_constructors[-1]['javaRuntime'],
        'supplementalCallerEffects': 15, 'projection': projection, 'nativeProjectionExecuted': native.is_file(),
        'scope': 'One mapped owner; exact required raw rows; seven diagnostics retained separately. Existing primitive JNI projection only; no native map store/ABI, concrete Spring assignability, wire identity, performance or full-five claim.'}))


if __name__ == '__main__':
    main()
