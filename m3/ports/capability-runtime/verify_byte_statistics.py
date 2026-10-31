#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only source custody + strict JDK21 execution for one M3 CPU leaf."""
import argparse
import csv
import hashlib
from pathlib import Path
import re
import shutil
import subprocess


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> None:
    cmd = argparse.ArgumentParser(description=__doc__)
    cmd.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[3])
    cmd.add_argument('--out', required=True, type=Path)
    args = cmd.parse_args()
    root = args.root.resolve()
    out = args.out.absolute()
    if not (root / 'm3/ports/capability-runtime').is_dir():
        raise ValueError('not an M3JDK21 receiver root')
    if out.exists() or out == root or root in out.parents:
        raise ValueError('fresh external proof directory required')
    if not out.parent.is_dir() or out.parent.is_symlink():
        raise ValueError('untrusted proof parent')
    manifest = root / 'm3/ports/capability-runtime/RECEIVING_SHA256.tsv'
    with manifest.open(encoding='utf-8', newline='') as file:
        records = list(csv.DictReader((line for line in file if not line.startswith('#')), delimiter='\t'))
    required = {'path', 'sha256', 'source_authority', 'license', 'gate'}
    if len(records) != 4 or any(set(record) != required for record in records):
        raise RuntimeError('unexpected receiver manifest geometry')
    checked = set()
    for record in records:
        rel = Path(record['path'])
        if (not rel.parts or rel.is_absolute() or '..' in rel.parts or
                record['path'] in checked or not
                re.fullmatch(r'[a-f0-9]{64}', record['sha256'])):
            raise RuntimeError('unsafe duplicate receiver path')
        checked.add(record['path'])
        file = root / rel
        if not file.is_file() or file.is_symlink() or not file.resolve().is_relative_to(root):
            raise RuntimeError('missing or untrusted receiver source')
        if sha256(file.read_bytes()) != record['sha256']:
            raise RuntimeError('receiver postimage drift: ' + record['path'])
        if record['license'] != 'Apache-2.0':
            raise RuntimeError('license changed')
    taxonomy = root / 'm3/ports/capability-runtime/CPU_TOOLS_FAMILY_CATALOGUE_20261017.tsv'
    with taxonomy.open(encoding='utf-8', newline='') as file:
        catalogue = list(csv.DictReader(file, delimiter='\t'))
    if len(catalogue) != 110 or len({item['id'] for item in catalogue}) != 110:
        raise RuntimeError('all tool categories must remain inventoried exactly once')
    if any(item['decision'] not in {
            'PARTIAL_LOCAL_JAVA21_VERIFIED', 'EXISTING_TARGET_OWNER_NOT_FULL_CATEGORY',
            'REUSE_SYNX_OWNER_NOT_RECEIVED', 'HOST_TOOL_NOT_BUNDLED', 'CATALOGUED_ONLY'
            } for item in catalogue):
        raise RuntimeError('unqualified tool-category promotion')
    out.mkdir()
    javac = shutil.which('javac')
    java = shutil.which('java')
    if not javac or not java:
        raise RuntimeError('a full JDK 21 javac/java is required')
    def execute(args_list: list[str], log_name: str) -> str:
        done = subprocess.run(args_list, text=True, capture_output=True, timeout=120,
                              check=False, errors='replace')
        (out / log_name).write_text(done.stdout + done.stderr, encoding='utf-8')
        if done.returncode:
            raise RuntimeError(log_name + ' failed: ' + done.stderr[-1000:])
        return done.stdout.strip()
    if '21' not in execute([java, '-version'], 'version.log') and '21' not in \
            (out / 'version.log').read_text():
        raise RuntimeError('JDK21 required')
    owner = root / 'm3/ports/capability-runtime/src/main/java/com/m3/runtime/capability'
    test = root / 'm3/ports/capability-runtime/src/test/java/com/m3/runtime/capability'
    classes = out / 'classes'
    classes.mkdir()
    execute([javac, '--release', '21', '-Xlint:all,-module', '-Werror',
             '-d', str(classes),
             str(root / 'm3/ports/capability-runtime/src/main/java/module-info.java'),
             str(owner / 'SourceArtifact.java'), str(owner / 'ByteStatistics.java'),
             str(test / 'ByteStatisticsChecks.java')], 'javac.log')
    result = execute([java, '-Xmx40m', '--module-path', str(classes),
                      '-m', 'com.m3.runtime.capability/com.m3.runtime.capability.ByteStatisticsChecks'],
                     'jdk21-cpu-oracle.log')
    if not result.startswith('M3_BYTE_STATISTICS_PASS assertions=') or \
            'kernel=JAVA_256_BIN_MASK_RLE' not in result:
        raise RuntimeError('test runner skipped or returned unexpected output')
    (out / 'RECEIPT.txt').write_text('M3_BYTE_STATISTICS_SOURCE_RECEIVER_PASS\n' +
                                     result + '\n' +
                                     'sha256-manifest=' + sha256(manifest.read_bytes()) + '\n',
                                     encoding='utf-8')
    print((out / 'RECEIPT.txt').read_text(), end='')


if __name__ == '__main__':
    main()
