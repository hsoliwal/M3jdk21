#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify exact Maven outputs and replay/refusal behavior for CI repair."""
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
for row in rows:
    assert (CRATE / 'target/generated' / row['path']).read_bytes() == row['after']

events = []
with tempfile.TemporaryDirectory(prefix='m3-ci-repair-') as temporary:
    root = Path(temporary)
    for guard in plan['guards']:
        path = root / guard['path']
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes((ROOT / guard['path']).read_bytes())
    for row in rows:
        path = root / row['path']
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(row['before'])
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

result = {'schema': 'm3.ci-repair-custody/1', 'plan_sha256': plan['plan_sha256'],
          'generated_outputs': len(rows), 'events': events}
(CRATE / 'target/custody.json').write_text(json.dumps(result, indent=2) + '\n')
print('PASS: exact recipe outputs, fixed point, rollback and three no-write refusals')
