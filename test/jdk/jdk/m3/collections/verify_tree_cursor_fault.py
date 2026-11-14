#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed disposable source instrumentation for the canonical M3TreeList."""
from __future__ import annotations

import hashlib
import json
import subprocess
import sys
from pathlib import Path

OWNER = Path('src/jdk.m3.collections/share/classes/jdk/m3/collections/apache/M3TreeList.java')
PROOF = Path('test/jdk/jdk/m3/collections/TreeListCursorAllocationFailure.java')
EXPECTED_OWNER = 'ab861ce0c29d53080b54117414a4ba8486b1c58e'
EXPECTED_PROOF = 'ab8a43dfb0191c81b039e8e49e419de964a126de'
CLASS = 'TreeListCursorAllocationFailure'
FIELD_MARKER = '    private class TreeIterator implements Iterator<E> {'
ALLOCATION_MARKER = '                path = Arrays.copyOf(path, capacity);'
INJECT_FIELD = '    private static int cursorGrowthFailureForTest;\n\n'
INJECT_ALLOCATION = (
    '                if (cursorGrowthFailureForTest > 0 && --cursorGrowthFailureForTest == 0) {\n'
    '                    throw new OutOfMemoryError("injected cursor growth");\n'
    '                }\n'
)


def hardcheck(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError('HARD_FAIL: ' + message)


def git_hash(blob: bytes) -> str:
    payload = b'blob ' + str(len(blob)).encode('ascii') + b'\0' + blob
    return hashlib.sha1(payload).hexdigest()


def transform(source: str) -> str:
    hardcheck(source.count(FIELD_MARKER) == 1, 'TreeIterator source marker drift')
    hardcheck(source.count(ALLOCATION_MARKER) == 1, 'single path-growth source marker drift')
    hardcheck('cursorGrowthFailureForTest' not in source, 'source already instrumented')
    modified = source.replace(FIELD_MARKER, INJECT_FIELD + FIELD_MARKER)
    modified = modified.replace(ALLOCATION_MARKER, INJECT_ALLOCATION + ALLOCATION_MARKER)
    hardcheck(
        modified.replace(INJECT_FIELD, '', 1).replace(INJECT_ALLOCATION, '', 1) == source,
        'instrumentation failed byte-reversible reconstruction',
    )
    return modified


def execute(args: list[str], logfile: Path, cwd: Path, *, timeout: int = 240) -> bytes:
    completed = subprocess.run(args, cwd=cwd, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, timeout=timeout, check=False)
    logfile.write_bytes(completed.stdout)
    hardcheck(completed.returncode == 0, f'process exit {completed.returncode}: {logfile}')
    return completed.stdout


def run(repo: Path, output: Path) -> dict[str, object]:
    repo = repo.resolve()
    output = output.resolve()
    hardcheck(output.is_relative_to(repo), 'evidence output must stay inside the checkout')
    hardcheck(OWNER.is_relative_to('src/') and PROOF.is_relative_to('test/'), 'scope guard')
    output.mkdir(parents=True, exist_ok=True)
    src_path = repo / OWNER
    proof_path = repo / PROOF
    source = src_path.read_bytes()
    proof = proof_path.read_bytes()
    hardcheck(git_hash(source) == EXPECTED_OWNER, 'worktree owner source blob mismatch')
    hardcheck(git_hash(proof) == EXPECTED_PROOF, 'original upstream fault proof blob mismatch')
    tree = execute(['git', 'ls-tree', '-r', 'HEAD', '--', OWNER.as_posix()],
                   output / 'tree-entry.log', repo).decode('utf-8')
    expected_tree_line = f'100644 blob {EXPECTED_OWNER}\t{OWNER.as_posix()}\n'
    hardcheck(tree == expected_tree_line, 'HEAD tree does not own expected source blob')
    text = source.decode('utf-8', 'strict')
    disposable = transform(text).encode('utf-8')
    hardcheck(git_hash(src_path.read_bytes()) == EXPECTED_OWNER, 'input source changed during transform')
    java = output / 'M3TreeList.java'
    hardcheck(not java.exists() or java.read_bytes() == disposable, 'existing output drift')
    java.write_bytes(disposable)
    classes = output / 'classes'
    classes.mkdir(exist_ok=True)
    compile_args = [
        'javac', '--release', '21', '-proc:none', '-Xlint:all,-module', '-Werror',
        '-d', str(classes), str(java), str(proof_path),
    ]
    execute(compile_args, output / 'compile.log', repo, timeout=180)
    outputs: list[bytes] = []
    modes = (('default', []), ('interpreted', ['-Xint']), ('compiled', ['-Xcomp']))
    for name, flags in modes:
        output_bytes = execute(['java', *flags, '-cp', str(classes), CLASS],
                               output / f'{name}.log', repo, timeout=300)
        hardcheck(output_bytes.strip() == b'PASS injected=60 checks=491520',
                  f'fault-injection result mismatch: {name}')
        outputs.append(output_bytes)
    hardcheck(outputs[0] == outputs[1] == outputs[2], 'VM receipts are not byte identical')
    hardcheck(git_hash(src_path.read_bytes()) == EXPECTED_OWNER, 'original source mutated')
    result: dict[str, object] = {
        'status': 'EXACT_DISPOSABLE_SOURCE_FAULT_PROOF_PASS',
        'source_git_blob': EXPECTED_OWNER,
        'proof_git_blob': EXPECTED_PROOF,
        'disposable_sha256': hashlib.sha256(disposable).hexdigest(),
        'mode_count': 3,
        'failure_injections': 60,
        'retry_traversal_checks': 491520,
        'logs_sha256': hashlib.sha256(outputs[0]).hexdigest(),
        'scope': 'instrumented disposable copy; original Java implementation unchanged',
        'full_installed_jdk_qualified': False,
    }
    receipt = json.dumps(result, indent=2, sort_keys=True) + '\n'
    dest = output / 'EXACT_SOURCE_FAULT_RECEIPT.json'
    hardcheck(not dest.exists() or dest.read_text(encoding='utf-8') == receipt,
              'previous receipt differs; do not overwrite')
    dest.write_text(receipt, encoding='utf-8')
    return result


def main() -> None:
    hardcheck(len(sys.argv) in (1, 2), 'usage: verify_tree_cursor_fault.py [repo-root]')
    repo = Path(sys.argv[1] if len(sys.argv) == 2 else '.').resolve()
    print(json.dumps(run(repo, repo / 'target/tree-cursor/fault'), sort_keys=True))


if __name__ == '__main__':
    main()
