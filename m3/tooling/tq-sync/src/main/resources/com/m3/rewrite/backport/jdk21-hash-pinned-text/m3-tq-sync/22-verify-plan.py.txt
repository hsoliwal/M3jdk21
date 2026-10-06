#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exercise existing sealed-install custody; require actual recipe postimages."""
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
sealed.observe_guards(ROOT, plan)
# These three documents have shared ownership across serial port packets. Their
# historical postimages remain byte-exact in the replay fixture below. The live
# tree may append other ports, but must retain all this packet's authority.
shared = {'m3/docs/name-mapping.json', 'm3/docs/counterpart-naming.md',
          'src/java.base/share/legal/synexia.md'}
for row in rows:
    current = (ROOT / row['path']).read_bytes()
    if row['path'] not in shared:
        assert current == row['after'], row['path']
    elif row['path'] == 'm3/docs/name-mapping.json':
        current_map = json.loads(current)
        expected_map = json.loads(row['after'])
        ids = {r['id'] for r in expected_map['migration']['records']}
        projected = copy.deepcopy(current_map)
        projected['migration']['records'] = [r for r in projected['migration']['records'] if r['id'] in ids]
        assert projected == expected_map, 'Existing mapping records/gates/authority changed'
        assert len({r['id'] for r in current_map['migration']['records']}) == len(current_map['migration']['records'])
    elif row['path'] == 'm3/docs/counterpart-naming.md':
        lines = iter(current.decode().splitlines())
        assert all(any(candidate == line for candidate in lines)
                   for line in row['after'].decode().splitlines()), 'Naming lineage removed'
    else:
        assert current.endswith(row['after']), 'Prior distribution notices changed' 
for row in rows:
    assert (CRATE / 'target/generated' / row['path']).read_bytes() == row['after'], row['path']

before_path = CRATE / 'src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq/name-mapping.json.txt.before'
before = json.loads(before_path.read_text())
after = json.loads(next(row['after'] for row in rows if row['path'] == 'm3/docs/name-mapping.json'))
retained = copy.deepcopy(after)
record = retained['migration']['records'].pop()
assert record['id'] == 'synexia.counterpart.MIndexRegexTrigramQuery'
assert retained == before, 'Existing mapping authority must be retained in full'

events = []
with tempfile.TemporaryDirectory(prefix='m3-tq-custody-') as temporary:
    root = Path(temporary)
    for guard in plan['guards']:
        path = root / guard['path']
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes((ROOT / guard['path']).read_bytes())
    for row in rows:
        path = root / row['path']
        if row['before'] is not None:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(row['before'])

    for mode, state, writes in [('check', 'before', 0), ('apply', 'after', len(rows)),
                                ('apply', 'after', 0), ('check', 'after', 0),
                                ('rollback', 'before', len(rows)), ('rollback', 'before', 0)]:
        result = sealed.execute(plan_path, root, mode)
        assert (result['state'], result['writes']) == (state, writes), result
        events.append({'mode': mode, **result})

    def refused(label):
        snapshot = {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()}
        try:
            sealed.execute(plan_path, root, 'apply')
        except sealed.Refusal:
            events.append({'refusal': label})
        else:
            raise AssertionError('Unexpected admission: ' + label)
        assert snapshot == {str(p.relative_to(root)): p.read_bytes() for p in root.rglob('*') if p.is_file()}

    added = next(row for row in rows if row['before'] is None)
    occupied = root / added['path']
    occupied.parent.mkdir(parents=True, exist_ok=True)
    occupied.write_text('foreign bytes\n')
    refused('occupied target, no writes')
    occupied.write_bytes(added['after'])
    refused('mixed pre/postimages, no writes')
    occupied.unlink()
    guard = root / plan['guards'][0]['path']
    original = guard.read_bytes()
    guard.write_bytes(original + b'\ndrift\n')
    refused('dependency drift, no writes')
    guard.write_bytes(original)
    assert sealed.execute(plan_path, root)['state'] == 'before'

result = {'schema': 'm3.tq-custody/1', 'plan_sha256': plan['plan_sha256'],
          'generated_outputs': len(rows), 'retained_records': len(before['migration']['records']),
          'retained_gates': len(before['migration']['gates']), 'events': events}
(CRATE / 'target/custody.json').write_text(json.dumps(result, indent=2) + '\n')
print('PASS: exact recipe postimages, retained mapping, replay/rollback and three no-write refusals')
