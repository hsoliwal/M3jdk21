#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify TQ reconciliation, preserved authority, and sealed replay/refusals."""
import importlib.util
import copy
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
def retained_mapping(current, expected):
    """Allow serial record additions while retaining the complete earlier authority."""
    records = current['migration']['records']
    previous = expected['migration']['records']
    assert len(records) >= len(previous), 'Mapping records removed'
    assert records[:len(previous)] == previous, 'Prior mapping records changed or reordered'
    ids = [record['id'] for record in records]
    assert all(isinstance(identity, str) and identity for identity in ids)
    assert len(set(ids)) == len(ids), 'Duplicate mapping id'
    projected = copy.deepcopy(current)
    projected['migration']['records'] = previous
    assert projected == expected, 'Prior mapping gates or authority changed'


# name-mapping.json is shared by serial port packets. Preserve this packet's exact
# records and all other authority, as the retained TQ custody gate already does.
# Every non-shared target and dependency guard still requires its exact postimage.
sealed.observe_guards(ROOT, plan)
for row in rows:
    current = (ROOT / row['path']).read_bytes()
    if row['path'] == 'm3/docs/name-mapping.json':
        retained_mapping(json.loads(current), json.loads(row['after']))
    else:
        assert current == row['after'], row['path']
for row in rows:
    assert (CRATE / 'target/generated' / row['path']).read_bytes() == row['after']

# Only the existing TQ record may advance; preserve the other 51 records and
# every top-level authority field, including all 20 gates. No status promotion.
mapping = next(row for row in rows if row['path'] == 'm3/docs/name-mapping.json')
before_map = json.loads(mapping['before'])
after_map = json.loads(mapping['after'])
identity = 'synexia.counterpart.MIndexRegexTrigramQuery'
for document in (before_map, after_map):
    record = next(r for r in document['migration']['records'] if r['id'] == identity)
    assert record['status'] == 'implemented-unverified'
    document['migration']['records'].remove(record)
assert before_map == after_map, 'Unrelated mapping authority changed'
assert len(after_map['migration']['records']) == 51
assert len(after_map['migration']['gates']) == 20
kernel = 'src/java.base/share/classes/jdk/internal/mindex/M3TQ.java'
assert kernel in {guard['path'] for guard in plan['guards']}
assert kernel not in {row['path'] for row in rows}, 'Existing runtime owner must survive'

events = []
with tempfile.TemporaryDirectory(prefix='m3-tq-sync-') as temporary:
    root = Path(temporary)
    for guard in plan['guards']:
        path = root / guard['path']
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes((ROOT / guard['path']).read_bytes())
    for row in rows:
        path = root / row['path']
        path.parent.mkdir(parents=True, exist_ok=True)
        if row['before'] is not None: path.write_bytes(row['before'])
    for mode, state, writes in [('check', 'before', 0), ('apply', 'after', len(rows)),
                                ('apply', 'after', 0), ('rollback', 'before', len(rows)),
                                ('rollback', 'before', 0)]:
        result = sealed.execute(plan_path, root, mode)
        assert (result['state'], result['writes']) == (state, writes)
        events.append({'mode': mode, **result})

    def refusal(label):
        snapshot = {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()}
        try:
            sealed.execute(plan_path, root, 'apply')
        except sealed.Refusal:
            events.append({'refusal': label})
        else:
            raise AssertionError('Unexpected admission: ' + label)
        assert snapshot == {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()}

    path = root / rows[0]['path']
    path.write_bytes(rows[0]['before'] + b'\ndrift\n')
    refusal('destination drift')
    path.write_bytes(rows[0]['after'])
    refusal('mixed states')
    path.write_bytes(rows[0]['before'])
    guard = root / plan['guards'][0]['path']
    guard.write_bytes(guard.read_bytes() + b'\ndrift\n')
    refusal('dependency drift')

result = {'schema': 'm3.tq-sync-custody/1', 'plan_sha256': plan['plan_sha256'],
          'generated_outputs': len(rows), 'events': events}
(CRATE / 'target/custody.json').write_text(json.dumps(result, indent=2) + '\n')
print('PASS: exact recipe outputs, fixed point, rollback and three no-write refusals')
