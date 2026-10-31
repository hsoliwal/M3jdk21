#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Capture exact public collection checkouts with the existing preparation owner; never build donors."""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import subprocess
import sys
import zipfile
import prepare

PUBLIC_SOURCES = frozenset((
    'hsoliwal/M3jdk21', 'hsoliwal/guava', 'hsoliwal/eclipse-collections',
    'hsoliwal/commons-collections', 'hsoliwal/spring-framework',
    'hsoliwal/swift-collections', 'hsoliwal/jackson-datatypes-collections',
    'hrldcpr/pcollections'))
ROLES = frozenset(('TARGET_BASELINE', 'PUBLIC_FORK_SOURCE', 'UPSTREAM_REFERENCE_ONLY'))
M3_SPARSE = ('/m3/collections/', '/m3/pcollections/',
             '/src/java.base/share/classes/java/util/', '/AGENTS.md',
             '/m3/docs/m3jdk21-porting-invariant.md', '/m3/docs/name-mapping.json',
             '/LICENSE', '/ADDITIONAL_LICENSE_INFO')


def read_inputs(path: Path) -> list[dict]:
    with path.open(encoding='utf-8', newline='') as source:
        table = csv.DictReader(source, delimiter='\t')
        if table.fieldnames != ['repository', 'commit', 'role']:
            raise ValueError('Input table schema mismatch')
        rows = list(table)
    seen = set()
    for row in rows:
        if set(row) != {'repository', 'commit', 'role'} or None in row.values():
            raise ValueError('Malformed input row')
        repo = row['repository']
        if repo not in PUBLIC_SOURCES or repo.casefold() in seen:
            raise ValueError('Unknown or duplicate public collection source')
        if not re.fullmatch(r'[0-9a-f]{40}', row['commit']):
            raise ValueError('Exact lower-case commit pin required')
        expected_role = ('UPSTREAM_REFERENCE_ONLY' if repo == 'hrldcpr/pcollections'
                         else 'TARGET_BASELINE' if repo == 'hsoliwal/M3jdk21'
                         else 'PUBLIC_FORK_SOURCE')
        if row['role'] not in ROLES or row['role'] != expected_role:
            raise ValueError('Source role mismatch')
        seen.add(repo.casefold())
    if {row['repository'] for row in rows} != PUBLIC_SOURCES:
        raise ValueError('All eight explicitly pinned source cohorts are required')
    return sorted(rows, key=lambda row: row['repository'])


def run_git(cwd: Path, args: list[str], logs: Path, ordinal: int,
            payload: bytes | None = None) -> dict:
    command = ['git', '-c', 'core.hooksPath=/dev/null', '-C', str(cwd), *args]
    environment = dict(os.environ, GIT_TERMINAL_PROMPT='0', GIT_CONFIG_NOSYSTEM='1',
                       GIT_CONFIG_GLOBAL='/dev/null')
    result = subprocess.run(command, input=payload, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, check=False, timeout=900, env=environment)
    log = logs / f'{ordinal:03d}-git.log'
    log.write_bytes(result.stdout)
    event = {'command': command, 'exit': result.returncode, 'log': log.name,
             'log_sha256': prepare.digest(result.stdout)}
    with (logs / 'COMMANDS.jsonl').open('a', encoding='utf-8') as stream:
        stream.write(json.dumps(event, sort_keys=True) + '\n')
    if result.returncode:
        raise RuntimeError(f'Public source capture command failed; see {log.name}')
    return event


def checkout_commands(row: dict) -> list[tuple[list[str], bytes | None]]:
    repo = row['repository']
    commands = [(['init', '-q'], None),
                (['remote', 'add', 'origin', f'https://github.com/{repo}.git'], None)]
    fetch = ['fetch', '--depth=1', '--no-tags', 'origin', row['commit']]
    if repo == 'hsoliwal/M3jdk21':
        fetch.insert(1, '--filter=blob:none')
        commands += [(['config', 'remote.origin.promisor', 'true'], None),
                     (['config', 'remote.origin.partialclonefilter', 'blob:none'], None),
                     (['sparse-checkout', 'init', '--no-cone'], None),
                     (['sparse-checkout', 'set', '--no-cone', '--stdin'],
                      ('\n'.join(M3_SPARSE) + '\n').encode('utf-8'))]
    commands += [(fetch, None), (['checkout', '--detach', row['commit']], None)]
    return commands


def archive_packet(packet: Path, archive: Path) -> str:
    if archive.exists():
        raise FileExistsError('Do not overwrite prior source archive')
    with zipfile.ZipFile(archive, 'x', zipfile.ZIP_DEFLATED, compresslevel=6) as bundle:
        for file in sorted(packet.rglob('*')):
            if file.is_symlink():
                raise ValueError('Do not follow packet symlinks')
            if file.is_file():
                info = zipfile.ZipInfo(file.relative_to(packet).as_posix(), (1980, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                info.external_attr = 0o100644 << 16
                bundle.writestr(info, file.read_bytes())
    with archive.open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def capture(inputs: Path, work: Path) -> dict:
    rows = read_inputs(inputs)
    if work.exists() or work.is_symlink():
        raise FileExistsError('Capture work directory must be fresh')
    work.mkdir(parents=True)
    clones = work / 'checkouts'; clones.mkdir()
    logs = work / 'logs'; logs.mkdir()
    contexts, failures = [], []
    ordinal = 0
    for row in rows:
        checkout = clones / row['repository'].replace('/', '__'); checkout.mkdir()
        try:
            for args, payload in checkout_commands(row):
                ordinal += 1
                run_git(checkout, args, logs, ordinal, payload)
            contexts.append(prepare.inspect_checkout(row['repository'], checkout, row['commit']))
        except (OSError, ValueError, RuntimeError, subprocess.TimeoutExpired) as failure:
            failures.append({'repository': row['repository'], 'error': str(failure)})
            print(json.dumps(failures[-1]), flush=True)
        else:
            print(json.dumps({'repository': row['repository'], 'tracked': len(contexts[-1]['tracked'])}), flush=True)
    packet = work / 'packet'
    manifest = prepare.prepare(contexts, packet)
    verified = prepare.verify_packet(packet)
    complete = (len(contexts) == len(rows) and not failures
                and manifest['preparation_complete_for_supplied_checkouts'])
    result = dict(verified, source_capture_complete=complete, failures=failures,
                  supplied_repositories=len(rows), captured_repositories=len(contexts),
                  unresolved=manifest['unresolved'], atomization='NOT_RUN', patternization='NOT_RUN')
    (packet / 'CAPTURE_RESULT.json').write_bytes(prepare.stable_json(result))
    (packet / 'RUN_CONTEXT.json').write_bytes(prepare.stable_json({
        'input_sha256': prepare.digest(inputs.read_bytes()), 'inputs': rows,
        'python': sys.version, 'platform': platform.platform(), 'workers': 1,
        'tool_sha256': {name: prepare.digest(Path(__file__).with_name(name).read_bytes())
                        for name in ('prepare.py', 'capture.py')},
        'donor_code_executed': False, 'remote_writes': False, 'private_source_exported': False}))
    (packet / 'INPUTS.tsv').write_bytes(inputs.read_bytes())
    archive = work / 'M3_COLLECTION_SOURCES_20261020.zip'
    sha = archive_packet(packet, archive)
    (work / 'M3_COLLECTION_SOURCES_20261020.sha256').write_text(sha + '  ' + archive.name + '\n')
    (work / 'RESULT.json').write_bytes(prepare.stable_json(result))
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inputs', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    result = capture(args.inputs, args.work)
    print(json.dumps(result, indent=2))
    if not result['source_capture_complete']:
        raise SystemExit(2)


if __name__ == '__main__':
    main()
