#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fixtures only: all candidate writes are performed by existing Java Replay."""
import hashlib
from pathlib import Path
import shutil
import subprocess
import tempfile

TASK = Path(__file__).resolve().parent
ROOT = TASK.parents[2]
CLASSES = TASK / 'target/replay-classes'
ROWS = [line.split('\t') for line in (TASK / 'templates/manifest.tsv').read_text().splitlines()]

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def snapshot(root):
    return {p.relative_to(root).as_posix(): digest(p) for p in root.rglob('*') if p.is_file()}

def run(mode, root, templates):
    return subprocess.run(['java', '-cp', str(CLASSES), 'com.synexia.rewrite.Replay',
                           mode, str(root), str(templates)], capture_output=True, text=True, check=False)

def seed(root):
    root.mkdir()
    for target, before, _, _ in ROWS:
        if before != 'ABSENT':
            path = root / target
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(TASK / 'before' / path.name, path)
            assert digest(path) == before

with tempfile.TemporaryDirectory(prefix='m3-summary-recipe-') as tmp:
    work = Path(tmp)
    good = work / 'good'
    seed(good)
    applied = run('--apply', good, TASK / 'templates')
    assert applied.returncode == 0, applied.stderr
    assert 'changes=6 applied=true' in applied.stdout, applied.stdout
    for target, _, after, _ in ROWS:
        assert digest(good / target) == after
    before_second = snapshot(good)
    second = run('--apply', good, TASK / 'templates')
    assert second.returncode == 0 and 'changes=0 applied=true' in second.stdout, second.stderr
    assert before_second == snapshot(good)
    assert run('--check', good, TASK / 'templates').returncode == 0
    for kind in ('drift', 'missing', 'occupied', 'tamper'):
        root = work / kind
        seed(root)
        templates = TASK / 'templates'
        if kind == 'drift':
            with (root / ROWS[0][0]).open('a') as out: out.write('\n# drift\n')
        elif kind == 'missing':
            (root / ROWS[0][0]).unlink()
        elif kind == 'occupied':
            path = root / next(row[0] for row in ROWS if row[1] == 'ABSENT')
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text('foreign occupant\n')
        else:
            templates = work / 'tampered-templates'
            shutil.copytree(TASK / 'templates', templates)
            with (templates / ROWS[0][3]).open('a') as out: out.write('\n# tampered\n')
        state = snapshot(root)
        result = run('--apply', root, templates)
        assert result.returncode != 0, kind
        assert snapshot(root) == state, 'partial mutation on refusal: ' + kind
    print('RECIPE_PROOF_PASS targets=6 firstWrites=6 secondWrites=0 refusalControls=4')
