#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Qualify BSR replay, narrow mapping changes and exact predecessor succession."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile

CRATE = Path(__file__).resolve().parent
ROOT = CRATE.parents[2]
spec = importlib.util.spec_from_file_location('sealed', ROOT / 'm3/migration/recipe.py')
sealed = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sealed)
plan_path = CRATE / 'plan.json'
plan, rows = sealed.sealed_plan(plan_path)
assert sealed.execute(plan_path, ROOT, 'check')['state'] == 'after'
assert len(rows) == 17
for row in rows:
    assert (CRATE / 'target/generated' / row['path']).read_bytes() == row['after'], row['path']

mapping = next(row for row in rows if row['path'] == 'm3/docs/name-mapping.json')
before = json.loads(mapping['before'])
after = json.loads(mapping['after'])
old_records = before['migration'].pop('records')
new_records = after['migration'].pop('records')
assert before == after, 'Top-level authority/gates changed'
assert len(old_records) == len(new_records) == 52
assert len(after['migration']['gates']) == 20
changed = []
for old, new in zip(old_records, new_records):
    assert old['id'] == new['id']
    if not old['id'].startswith('synexia.collections.'):
        assert old == new, old['id']
        continue
    changed.append(old['id'])
    assert old['status'] == new['status'] == 'implemented-unverified'
    left, right = copy.deepcopy(old), copy.deepcopy(new)
    left.pop('recipe'); right.pop('recipe')
    if old['id'] == 'synexia.collections.SegmentedBitLane28':
        assert new['sync']['source_revision'] == 'a8e8b3ade47d04229aff2a280e67bad5741ed863'
        assert new['tests'][:-1] == old['tests']
        assert new['lineage']['previous_sources'] == old['lineage']['previous_sources'] + old['sources']
        assert new['lineage']['previous_targets'] == old['lineage']['previous_targets'] + old['targets']
        for key in ('sources', 'targets', 'lineage', 'tests', 'sync', 'observation', 'provenance', 'materialization'):
            left.pop(key); right.pop(key)
        assert new['provenance']['license'].startswith('Apache-2.0; exact BSR source, LICENSE and NOTICE retained under m3/tooling/bsr/donor.')
        assert {k:v for k,v in old['provenance'].items() if k != 'license'} == {k:v for k,v in new['provenance'].items() if k != 'license'}
        assert new['materialization']['cached_facts'].startswith(old['materialization']['cached_facts'])
        assert {k:v for k,v in old['materialization'].items() if k != 'cached_facts'} == {k:v for k,v in new['materialization'].items() if k != 'cached_facts'}
    assert left == right, 'Unrelated collection authority changed: ' + old['id']
assert len(changed) == 5

events = []
def snapshot(root):
    return {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()}

def put(root, path, data):
    target = root / path
    target.parent.mkdir(parents=True, exist_ok=True)
    if data is None:
        if target.exists(): target.unlink()
    else:
        target.write_bytes(data)

def refusal(root, label, operation):
    prior = snapshot(root)
    try:
        operation()
    except (sealed.Refusal, AssertionError):
        events.append({'refusal': label, 'writes': 0})
    else:
        raise AssertionError('Unexpected admission: ' + label)
    assert prior == snapshot(root), 'Refusal wrote files: ' + label

with tempfile.TemporaryDirectory(prefix='m3-bsr-custody-') as temporary:
    root = Path(temporary)
    for guard in plan['guards']:
        put(root, guard['path'], (ROOT / guard['path']).read_bytes())
    for row in rows:
        put(root, row['path'], row['before'])
    for mode, state, writes in [('check', 'before', 0), ('apply', 'after', 17),
                                ('apply', 'after', 0), ('check', 'after', 0),
                                ('rollback', 'before', 17), ('rollback', 'before', 0)]:
        result = sealed.execute(plan_path, root, mode)
        assert (result['state'], result['writes']) == (state, writes)
        events.append({'mode': mode, **result})
    added = next(row for row in rows if row['before'] is None)
    put(root, added['path'], b'foreign bytes\n')
    refusal(root, 'occupied target', lambda: sealed.execute(plan_path, root, 'apply'))
    put(root, added['path'], added['after'])
    refusal(root, 'mixed states', lambda: sealed.execute(plan_path, root, 'apply'))
    put(root, added['path'], None)
    guard = plan['guards'][0]['path']
    put(root, guard, (ROOT / guard).read_bytes() + b'\ndrift\n')
    refusal(root, 'dependency drift', lambda: sealed.execute(plan_path, root, 'apply'))
    put(root, guard, (ROOT / guard).read_bytes())
    assert sealed.execute(plan_path, root, 'check')['state'] == 'before'

# Execute the actual amended predecessor check, not a second implementation of
# its rule. Historical replay fixtures remain immutable. Only a sealed successor
# whose before-image equals the prior after-image may supersede an output.
predecessor_path = ROOT / 'm3/tooling/tq-sync/plan.json'
prior_plan, prior_rows = sealed.sealed_plan(predecessor_path)
verifier = (ROOT / 'm3/tooling/tq-sync/verify-plan.py').read_text()
start = verifier.index('sealed.observe_guards(ROOT, plan)')
end = verifier.index('for row in rows:\n    assert (CRATE /', start)
check = compile(verifier[start:end], str(ROOT / 'm3/tooling/tq-sync/verify-plan.py'), 'exec')
with tempfile.TemporaryDirectory(prefix='m3-bsr-succession-') as temporary:
    root = Path(temporary)
    for guard in prior_plan['guards'] + plan['guards']:
        put(root, guard['path'], (ROOT / guard['path']).read_bytes())
    for row in prior_rows:
        put(root, row['path'], row['after'])
    for row in rows:
        put(root, row['path'], row['after'])
    relative = str(plan_path.relative_to(ROOT))
    put(root, relative, plan_path.read_bytes())
    packet = root / plan_path.parent.relative_to(ROOT)
    for entry in plan['outputs']:
        for side in ('before', 'after'):
            if entry[side]:
                resource = entry[side]['resource']
                put(packet, resource, (CRATE / resource).read_bytes())
    def verify():
        exec(check, {'ROOT': root, 'sealed': sealed, 'plan': prior_plan, 'rows': prior_rows})
    verify()
    events.append({'succession': 'exact predecessor after -> successor before -> current after'})
    altered = copy.deepcopy(plan)
    old_paths = {row['path'] for row in prior_rows}
    overlap = next(entry for entry in altered['outputs'] if entry['path'] in old_paths)
    resource = overlap['before']['resource']
    original = (packet / resource).read_bytes()
    bad = original + b'\nunproven preimage\n'
    put(packet, resource, bad)
    overlap['before']['sha256'] = sealed.digest(bad)
    def reseal(value):
        value.pop('plan_sha256', None)
        value['plan_sha256'] = sealed.digest(sealed.canonical(value))
        put(root, relative, sealed.canonical(value))
    reseal(altered)
    refusal(root, 'unproven successor preimage', verify)
    put(packet, resource, original)
    altered = copy.deepcopy(plan)
    altered['outputs'] = [entry for entry in altered['outputs'] if entry['path'] != overlap['path']]
    reseal(altered)
    refusal(root, 'missing successor link', verify)
    altered = copy.deepcopy(plan)
    altered['recipe_id'] = 'foreign-successor'
    reseal(altered)
    refusal(root, 'foreign successor', verify)
    put(root, relative, plan_path.read_bytes())
    put(root, overlap['path'], b'foreign live bytes\n')
    refusal(root, 'successor live drift', verify)
    put(root, overlap['path'], next(row['after'] for row in rows if row['path'] == overlap['path']))
    verify()

result = {'schema': 'm3.bsr-custody/1', 'plan_sha256': plan['plan_sha256'],
          'generated_outputs': 17, 'unchanged_records': 47, 'retained_gates': 20,
          'predecessor_plan_sha256': prior_plan['plan_sha256'], 'events': events}
(CRATE / 'target/custody.json').write_text(json.dumps(result, indent=2) + '\n')
print('PASS: exact outputs, selective mapping, fixed point, rollback, predecessor chain and seven no-write refusals')
