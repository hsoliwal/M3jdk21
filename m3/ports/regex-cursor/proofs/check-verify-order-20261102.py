#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed on SHA-256, complete compiler ordering, and JNI runtime ordering.

This is a standalone source-stage gate, not Maven/OpenRewrite/JUnit execution.
"""
import argparse
import hashlib
from pathlib import Path

EXPECTED_SHA256 = "5a021a4a8e527bbf4f8b0d106719080d85df95fc8490e05f3123f91af3c68757"


def require_sequence(source: str) -> None:
    lines = [line.strip() for line in source.splitlines()]
    commands = [x for x in lines if x and not x.startswith("#")]

    def location(prefix: str) -> int:
        matches = [i for i, line in enumerate(commands) if line.startswith(prefix)]
        if len(matches) != 1:
            raise ValueError(f"expected exactly one {prefix} command; found {len(matches)}")
        return matches[0]

    if not source.startswith("#!/usr/bin/env bash\n") or "set -euo pipefail" not in lines:
        raise ValueError("missing strict Bash execution contract")
    javac = location("javac --release 21 ")
    native = location("gcc -std=c17 ")
    checks = location("java -ea -Xcheck:jni ")
    interpreted = location("java -ea -Xint -Xcheck:jni ")
    java_tests = [i for i, x in enumerate(commands) if x.startswith("java -ea ")
                  and not x.startswith("java -ea -Xint ") and not x.startswith("java -ea -Xcheck:jni ")]
    if len(java_tests) < 5:
        raise ValueError("missing independent Java regression invocations")
    if not javac < native < min(java_tests) < checks < interpreted:
        raise ValueError("verification order violation: compile Java and C before ALL tests and JNI runtime")
    for marker in ('01-javac21.log', '08-gcc-c17.log', '09-jni-checked.log', '10-jni-interpreted.log'):
        if marker not in source:
            raise ValueError("missing evidence label: " + marker)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('--order-only', action='store_true', help='test an intentionally corrupted fixture')
    args = parser.parse_args()
    raw = args.source.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    if not args.order_only and digest != EXPECTED_SHA256:
        raise ValueError('source custody drift: ' + digest)
    require_sequence(raw.decode('utf-8', errors='strict'))
    print('M3_SEEK_VERIFICATION_ORDER_PASS order=java_compile,c17_compile,java_tests,jni_checked,jni_interpreted source_sha256=' + digest)


if __name__ == '__main__':
    main()
