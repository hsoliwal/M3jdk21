#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
"""Maven recipe-reuse entry: acquire immutable first-party inputs and execute their existing recipe.

This host does not transform source. Only the pinned OpenRewrite result materializer does.
The private Synexia token is never forwarded to the public receiver repository.
"""
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent


def main():
    lock = json.loads((HERE / 'lock.json').read_text())
    if lock['repository'] != 'https://github.com/hsoliwal/M3jdk21.git':
        raise ValueError('unadmitted recipe repository')
    if not re.fullmatch('[0-9a-f]{40}', lock['commit']) or not re.fullmatch('[0-9a-f]{40}', lock['recipe_tree']):
        raise ValueError('recipe commit and tree must be immutable Git identifiers')
    if lock['receiver_recipe'] != 'm3/collections/counts/recipe':
        raise ValueError('unadmitted recipe owner')
    target = HERE / 'target/receiver-replay'
    for path in [HERE / 'target', target]:
        if path.is_symlink():
            raise ValueError('symlinked proof output')
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True)
    checkout = target / 'checkout'
    checkout.mkdir()
    evidence = target / 'evidence'
    evidence.mkdir()
    env = dict(os.environ, GIT_TERMINAL_PROMPT='0')
    # Avoid transmitting ambient authentication inherited from an outer CI checkout.
    for key in list(env):
        if key.startswith('GIT_CONFIG_') or key in ('GIT_ASKPASS', 'SSH_ASKPASS', 'GITHUB_TOKEN', 'GH_TOKEN'):
            env.pop(key)
    env['GIT_CONFIG_NOSYSTEM'] = '1'
    env['GIT_CONFIG_GLOBAL'] = os.devnull

    def run(name, command, cwd=checkout):
        result = subprocess.run([str(arg) for arg in command], cwd=cwd, env=env,
                                capture_output=True, text=True, timeout=300)
        output = result.stdout + result.stderr
        (evidence / (name + '.log')).write_text('$ ' + repr(command) + '\n' + output + '\nEXIT=' + str(result.returncode) + '\n')
        if result.returncode:
            raise RuntimeError(name + ':\n' + output[-12000:])
        print(name + ': PASS', flush=True)
        return output.strip()

    run('git-init', ['git', 'init'])
    run('git-remote', ['git', 'remote', 'add', 'origin', lock['repository']])
    run('git-fetch', ['git', '-c', 'credential.helper=', 'fetch', '--depth=1', '--filter=blob:none', 'origin', lock['commit']])
    run('git-sparse', ['git', 'sparse-checkout', 'set', 'm3/collections'])
    run('git-checkout', ['git', '-c', 'credential.helper=', 'checkout', '--detach', lock['commit']])
    actual_commit = run('commit-identity', ['git', 'rev-parse', 'HEAD'])
    tree = run('recipe-identity', ['git', 'rev-parse', 'HEAD:' + lock['receiver_recipe']])
    if actual_commit != lock['commit'] or tree != lock['recipe_tree']:
        raise ValueError('recipe identity mismatch')
    version = run('java-identity', ['java', '-version'])
    if 'version "21' not in version:
        raise ValueError('Java 21 proof host required')
    maven = shutil.which('mvn')
    if not maven:
        raise RuntimeError('Maven is required; SDK-only execution is not this proof')
    command = [maven, '-B', '-ntp', '-f', str(checkout / lock['receiver_recipe'] / 'pom.xml')]
    run('maven-recipe-tests', command + ['test'])
    run('maven-exact-delivery', command + ['-Dm3.counted.target=' + str(checkout), 'test'])
    run('fixed-point-diff', ['git', 'diff', '--exit-code', '--', 'm3/collections'])
    reports = list((checkout / lock['receiver_recipe'] / 'target/surefire-reports').glob('TEST-*.xml'))
    totals = {key: sum(int(ET.parse(path).getroot().attrib.get(key, 0)) for path in reports)
              for key in ('tests', 'failures', 'errors', 'skipped')}
    if not reports or totals != {'tests': 8, 'failures': 0, 'errors': 0, 'skipped': 0}:
        raise ValueError('missing or unsuccessful actual recipe tests: ' + repr(totals))
    for path in reports:
        shutil.copyfile(path, evidence / path.name)
    for name in ['catalogue.tsv', 'provenance.json', 'recipe-receipt.tsv', 'M3_RECIPE_FIRST_INVARIANT.md']:
        shutil.copyfile(checkout / 'm3/collections/counts' / name, evidence / name)
    qualification = {'schema': 'M3_RECIPE_REUSE_EXECUTED_V1', 'receiver_commit': actual_commit,
                     'recipe_tree': tree, 'junit': totals, 'fixed_point': True,
                     'runtime_suite': 'SEPARATE_RECEIVER_GATE', 'phase_advance': False}
    (evidence / 'qualification.json').write_text(json.dumps(qualification, indent=2) + '\n')
    print(json.dumps(qualification, indent=2))


if __name__ == '__main__':
    main()
