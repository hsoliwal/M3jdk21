#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Execute the exact task sources in a fresh build root and retain SDK custody."""
from pathlib import Path
import hashlib
import importlib.util
import json
import shutil
import subprocess
import sys

ROOT = Path('/workspace/scratch/206049dfb270')
TASK = 'm3-nlp-current-receiver-lock-20261021'
CANONICAL = ROOT / 'synexia'
RELATIVE = Path('synexia-openrewrite-recipes/recipe-crates') / TASK
CRATE = CANONICAL / RELATIVE
WORK = ROOT / 'canonical-work/nlp-owner-sdk-001'
TEMP = Path('/tmp/m3-nlp-current-owner-sdk-001/canonical')


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def put(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x') as stream:
        stream.write(json.dumps(value, indent=2) + '\n')


def main():
    WORK.mkdir(exist_ok=False)
    TEMP.mkdir(parents=True, exist_ok=False)
    sys.dont_write_bytecode = True
    sys.path.insert(0, str(CRATE))
    spec = importlib.util.spec_from_file_location('nlp_task_input_owner', CRATE / 'qualify.py')
    owner = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(owner)
    inputs = owner.input_files(CRATE)
    before = {p.relative_to(CANONICAL).as_posix(): sha(p) for p in inputs}
    for source in inputs:
        destination = TEMP / source.relative_to(CANONICAL)
        destination.parent.mkdir(parents=True, exist_ok=True)
        with destination.open('xb') as stream:
            stream.write(source.read_bytes())
    actual_classes = ROOT / 'nlp-work/source-lock-correction/builder-and-whole-rope-001/candidate-classes'
    assert actual_classes.is_dir()
    classpath = WORK / 'parser-classpath.txt'
    with classpath.open('x') as stream:
        stream.write(str(actual_classes) + '\n')
    local = TEMP / RELATIVE
    command = ['python3', '-B', str(local / 'qualify.py'),
        '--java-home', '/tmp/m3-build-tools/jdk-21.0.12.1+1',
        '--maven', '/tmp/m3-build-tools/apache-maven-3.9.9/bin/mvn',
        '--maven-repo', '/tmp/m3-build-m2', '--parser-classpath', str(classpath),
        '--attempt', 'sdk-001-offline', '--timeout-seconds', '300']
    put(WORK / 'INPUTS_AND_COMMAND.json', {'schema': 'M3_NLP_RECIPE_ISOLATED_INPUTS/1',
        'argv': command, 'cwd': str(TEMP), 'captured_before_execution': True,
        'sources': before, 'parser_classpath': str(classpath), 'parser_classpath_sha256': sha(classpath),
        'actual_owner_result': str(ROOT / 'nlp-work/source-lock-correction/builder-and-whole-rope-001/RESULT.json'),
        'actual_owner_result_sha256': sha(ROOT / 'nlp-work/source-lock-correction/builder-and-whole-rope-001/RESULT.json'),
        'purpose': 'Fresh temporary build avoids previously observed shared-workspace compiled residue; no source or test selector alteration.'})
    with (WORK / 'qualify.log').open('x') as log:
        result = subprocess.run(command, cwd=TEMP, stdout=log, stderr=subprocess.STDOUT, timeout=330)
    attempt = local / 'proof/sdk-001-offline'
    assert attempt.is_dir(), 'The owning qualifier must retain an actual result'
    shutil.copytree(attempt, CRATE / 'proof/sdk-001-offline')
    current = {p.relative_to(CANONICAL).as_posix(): sha(p) for p in inputs}
    isolated = {p.relative_to(TEMP).as_posix(): sha(p) for p in owner.input_files(local)}
    assert before == current == isolated, 'Source drift across isolated execution'
    if result.returncode == 0:
        target = CRATE / 'target'
        target.mkdir(exist_ok=False)
        for name in ('recipe-proof.tsv', 'proof-status.tsv', 'named-recipe.yml'):
            shutil.copyfile(local / 'target' / name, target / name)
        for name in ('phase-receipts', 'receiver'):
            shutil.copytree(local / 'target' / name, target / name)
    receipt = {'schema': 'M3_NLP_CANONICAL_ISOLATED_SDK_CUSTODY/1',
        'status': 'PASS' if result.returncode == 0 else 'FAIL', 'exit_code': result.returncode,
        'command': command, 'source_inputs': before, 'source_inputs_unchanged': True,
        'task_receipt': str(CRATE / 'proof/sdk-001-offline/RESULT.json'),
        'task_receipt_sha256': sha(CRATE / 'proof/sdk-001-offline/RESULT.json'),
        'compiled_outputs_retained_in_temporary_directory_only': True}
    put(WORK / 'RESULT.json', receipt)
    print(json.dumps({k: receipt[k] for k in ('status', 'exit_code', 'source_inputs_unchanged', 'task_receipt', 'task_receipt_sha256')}))
    raise SystemExit(result.returncode)


if __name__ == '__main__':
    main()
