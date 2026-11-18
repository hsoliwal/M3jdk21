#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Refuse M3JDK21 LLVM reception on source drift; never authorize runtime by itself.

The LLVM fork remains the source owner. This checks its immutable Git blobs and
full existing SHA-256 inventory against the original M3 compiler/native owners.
No native code or Java product classes are copied into M3JDK by this script.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys

LLVM_SOURCE_COMMIT = 'a75b9f54608bf0ccc5a2f2bbf0af4814df4419c7'
M3_OWNER_BASE = '0aa6625cfdd9c5196e90c652b6ea423aa81017a2'

LLVM_BLOBS = {
    'm3/llvm-orc-jni/src/main/java/org/m3jdk/compiler/llvm/LlvmOrcJit.java': '248639223261c7991cb529f205237adbd2590b76',
    'm3/llvm-orc-jni/src/main/java/org/m3jdk/compiler/llvm/LlvmOrcBatch.java': 'b4a248446ec276510e38e846fe144f90018f9c19',
    'm3/llvm-orc-jni/src/test/java/org/m3jdk/compiler/llvm/LlvmOrcJitNoLoadTestMain.java': '6071bb60ef8629887bc13d6c5e5269fc74c5535c',
    'm3/llvm-orc-jni/src/main/c/m3_llvm_orc_jni.c': '5ca64eb2b1f51ba80edae3f47bdbb7d189d66150',
    'm3/llvm-orc-jni/src/main/c/m3_llvm_orc_batch_jni.c': '38a43e66576f1a65cfde461276265563e9c7e14a',
    'm3/llvm-orc-jni/CMakeLists.txt': '700b5ec2fb8538dffff92cdd2534052071dd54c5',
    'm3/llvm-orc-jni/SOURCE_PINS.sha256': '310999867ea097e33d072842bce5429407dca319',
    'm3/llvm-orc-jni/scripts/verify-java.sh': 'bc4b90ba906bbce45561967b5c661023a497c0fe',
    'm3/llvm-orc-jni/scripts/verify.sh': '308abfd2acab1ca623c16d0baa0091f6f9ef64e0',
    'llvm/include/llvm-c/Core.h': '05c7eeeedc62500ebc9b257ef83cdef134abc587',
    'llvm/include/llvm-c/Orc.h': 'd4cfd92e08bcc9d46e651de523cf73f55d81ba86',
    'llvm/include/llvm-c/LLJIT.h': '130b6a239ba01af25a5c1ed78df353e3d9a53e62',
    'llvm/lib/ExecutionEngine/Orc/OrcV2CBindings.cpp': '387a5353957027d52e91e7e2814afca6206f5582',
    'cmake/Modules/LLVMVersion.cmake': 'ca16353d25c9bb9532c58e865c32a8ae3d3bb730',
}
M3_BLOBS = {
    'm3/compiler/pom.xml': 'a7df778afc37a0091a83a5b6d179895104384ad7',
    'm3/native/CMakeLists.txt': '51e9cbbe4a316e185ad3b1de92a9c6669f9889eb',
    'm3/native/README.md': '8ffd2b1d0ac218967a171001a6c6f7b7d5169338',
}


def git(repo: Path, *arguments: str, check: bool = True) -> str:
    command = ['git', '-C', str(repo), *arguments]
    run = subprocess.run(command, check=False, stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE, text=True)
    if run.returncode != 0 and check:
        raise ValueError('GIT_SOURCE_REFUSAL: ' + run.stderr.strip()[:160])
    return run.stdout.strip()


def canonical_regular_file(root: Path, relative: str) -> Path:
    path = Path(relative)
    if path.is_absolute() or not path.parts or '..' in path.parts or '\\' in relative:
        raise ValueError('NONCANONICAL_SOURCE_PATH: ' + relative)
    current = root
    if current.is_symlink():
        raise ValueError('SYMLINKED_SOURCE_ROOT')
    for component in path.parts:
        current = current / component
        if current.is_symlink():
            raise ValueError('SYMLINKED_SOURCE_PATH: ' + relative)
    if not current.is_file():
        raise ValueError('MISSING_OR_NONREGULAR_SOURCE: ' + relative)
    return current


def verify_blob(repo: Path, path: str, expected: str) -> dict[str, str]:
    if len(expected) != 40 or any(ch not in '0123456789abcdef' for ch in expected):
        raise ValueError('INVALID_SOURCE_BLOB_PIN')
    source = canonical_regular_file(repo, path)
    committed = git(repo, 'rev-parse', 'HEAD:' + path)
    current = git(repo, 'hash-object', '--', path)
    if committed != expected or current != expected:
        raise ValueError('SOURCE_BLOB_DRIFT: ' + path)
    return {'path': path, 'git_blob': expected, 'sha256': hashlib.sha256(source.read_bytes()).hexdigest()}


def verify_original_repositories(m3: Path, llvm: Path) -> dict:
    m3 = m3.absolute()
    llvm = llvm.absolute()
    if not m3.is_dir() or not llvm.is_dir() or m3.is_symlink() or llvm.is_symlink():
        raise ValueError('INVALID_RECEIVER_CHECKOUT')
    if git(llvm, 'rev-parse', 'HEAD') != LLVM_SOURCE_COMMIT:
        raise ValueError('LLVM_SOURCE_COMMIT_DRIFT')
    # Allow shallow receiver checkouts: current owner Git blobs are exact;
    # requiring full historical ancestry would unnecessarily fetch the JDK.
    llvm_files = [verify_blob(llvm, path, blob) for path, blob in LLVM_BLOBS.items()]
    m3_files = [verify_blob(m3, path, blob) for path, blob in M3_BLOBS.items()]
    verified_dir = llvm / 'm3/llvm-orc-jni'
    outcome = subprocess.run(['sha256sum', '-c', 'SOURCE_PINS.sha256'], cwd=verified_dir,
                             capture_output=True, text=True, check=False)
    if outcome.returncode != 0:
        raise ValueError('LLVM_FULL_SHA256_INVENTORY_DRIFT: ' + outcome.stderr[:160])
    return {
        'status': 'SOURCE_CUSTODY_PASS_ONLY',
        'llvm_source': LLVM_SOURCE_COMMIT,
        'm3_owner_base': M3_OWNER_BASE,
        'llvm_blob_count': len(llvm_files),
        'm3_blob_count': len(m3_files),
        'llvm_full_source_sha256': 'PASS',
        'source_catalog': {'llvm': llvm_files, 'm3': m3_files},
        'native_llvm24': 'NOT_PROVEN',
        'actual_openrewrite_sdk': 'NOT_PROVEN',
        'm3jdk21_product_receiving': 'HOLD',
        'performance_default': 'JAVA_HOTSPOT_FOR_SIMPLE_LONG_ARRAY_ARITHMETIC',
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--m3-root', type=Path, required=True)
    parser.add_argument('--llvm-root', type=Path, required=True)
    args = parser.parse_args()
    receipt = verify_original_repositories(args.m3_root, args.llvm_root)
    print(json.dumps(receipt, indent=2, sort_keys=True))
    print('M3_LLVM_SOURCE_CUSTODY_PASS_NO_PRODUCT_ADMISSION')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError) as failure:
        print('M3_LLVM_SOURCE_REFUSAL: ' + str(failure), file=sys.stderr)
        raise SystemExit(1) from failure
