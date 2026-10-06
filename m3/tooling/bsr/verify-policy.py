#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Check canonical product policy, existing family references and retained authority."""
import copy
import hashlib
import json
from pathlib import Path
import re
import jsonschema

CRATE = Path(__file__).resolve().parent
ROOT = CRATE.parents[2]
mapping = json.loads((ROOT / 'm3/docs/name-mapping.json').read_text())
schema = json.loads((ROOT / 'm3/docs/name-mapping.schema.json').read_text())
proof = json.loads((CRATE / 'policy-proof.json').read_text())
document = (ROOT / 'm3/docs/M3JDK21_PORTING_INVARIANT.md').read_text()

def validate(value, text):
    jsonschema.Draft202012Validator(schema).validate(value)
    policy = value['porting_policy']
    retained = {k:v for k,v in value.items() if k != 'porting_policy'}
    digest = hashlib.sha256((json.dumps(retained, sort_keys=True, separators=(',', ':')) + '\n').encode()).hexdigest()
    assert digest == proof['prior_mapping_canonical_sha256'], 'Existing naming/migration authority changed'
    sources = {row['source'] for row in value['mappings']}
    records = {row['id']:row for row in value['migration']['records']}
    known_targets = [row['target'] for row in value['mappings']]
    known_targets += [v for row in records.values() for target in row['targets']
                      for v in (target['symbol'], target['path']) if v]
    for family in policy['family_mappings']:
        assert set(family['mapping_sources']) <= sources, 'Unknown source mapping'
        assert set(family['migration_record_ids']) <= records.keys(), 'Unknown migration record'
        if family['id'] == 'compiler':
            assert family['target_modules'] == ['jdk.compiler'], 'Compiler boundary crossed'
        for target in family['existing_targets']:
            pattern = r'(?<![\w.$])' + re.escape(target) + r'(?![\w.$])'
            assert any(re.search(pattern, existing) for existing in known_targets), 'Unmapped target: ' + target
            assert '`' + target + '`' in text, 'Human family mapping differs: ' + target
    assert len(records) == 52 and len(value['migration']['gates']) == 20
    for phrase in ('Public JDK APIs keep JDK names', 'internal replacement or optimization owners',
                   'convergence workspace and source contributor', 'synexia-m3-recipe',
                   'synexia-openrewrite-recipes', 'name-mapping.json'):
        assert phrase in text.replace('\n', ' '), 'Missing human invariant: ' + phrase

validate(mapping, document)
mutants = []
def reject(label, change):
    value = copy.deepcopy(mapping)
    change(value)
    try:
        validate(value, document)
    except (AssertionError, KeyError, jsonschema.ValidationError):
        mutants.append(label)
    else:
        raise AssertionError('Invalid policy accepted: ' + label)

reject('missing policy', lambda d: d.pop('porting_policy'))
reject('wrong product owner', lambda d: d['porting_policy'].__setitem__('product_repository', 'hsoliwal/com.synexia'))
reject('public API rename', lambda d: d['porting_policy']['compatibility'].__setitem__('public_api_names', 'rename-to-M3'))
reject('duplicate family', lambda d: d['porting_policy']['family_mappings'].__setitem__(1, copy.deepcopy(d['porting_policy']['family_mappings'][0])))
reject('unknown source mapping', lambda d: d['porting_policy']['family_mappings'][0]['mapping_sources'].append('invented.Source'))
reject('invented target naming', lambda d: d['porting_policy']['family_mappings'][0]['existing_targets'].append('java.lang.MIndexString'))
reject('compiler dependency in java.base', lambda d: d['porting_policy']['family_mappings'][-1].__setitem__('target_modules', ['java.base']))
reject('existing admission promotion', lambda d: d['migration']['records'][-1].__setitem__('status', 'implemented-tested'))
reject('removed existing mapping', lambda d: d['mappings'].pop())
result = {'schema':'m3.porting-policy-proof/1', 'families':6, 'retained_records':52,
          'retained_gates':20, 'rejected_mutants':mutants,
          'prior_mapping_canonical_sha256':proof['prior_mapping_canonical_sha256']}
(CRATE / 'target/policy-proof.json').write_text(json.dumps(result, indent=2) + '\n')
print('PASS: canonical product policy, six existing family mappings, unchanged authority and nine rejected policy drifts')
