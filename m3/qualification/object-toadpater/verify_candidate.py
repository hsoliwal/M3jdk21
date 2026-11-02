#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-2.0-only
"""Strict read-only source identity and minimum structural gates for a single draft alias."""

from pathlib import Path
import re
import subprocess
import sys

# Git raw blob identities; a new candidate requires a new reviewed, source-pinned successor.
EXPECTED = {
    "src/java.base/share/classes/java/lang/Object.java":
        "89076d679429154c620b589f122b34969f09c261",
    "src/hotspot/share/classfile/classLoader.cpp":
        "81224eef99c64c02d583830ee4b3b2872779e0dd",
    "test/jdk/java/lang/Object/ObjectToAdpaterNativeTest.java":
        "be9520d8907a29a0d689b175a041757891c85e19",
    "test/jdk/java/lang/Object/libObjectToAdpaterNativeTest.c":
        "ed5c008a5755d145d95e4bf2642b19c0aa62d8a7",
    "test/jdk/java/lang/Object/ObjectToAdpaterLegacyBinaryTest.java":
        "1e08202532222e77a3ce6e69048b266871b7375e",
}


def require_count(content: str, needle: str, count: int = 1) -> None:
    found = content.count(needle)
    if found != count:
        raise RuntimeError(f"STRUCTURE_DRIFT token={needle!r} expected={count} actual={found}")


def read_sealed(root: Path, rel: str, expected: str) -> str:
    target = root / rel
    if target.is_symlink() or not target.is_file():
        raise RuntimeError(f"SOURCE_DRIFT missing or symbolic file: {rel}")
    actual = subprocess.run(
        ["git", "hash-object", "--no-filters", "--", rel],
        cwd=root, text=True, encoding="utf-8", capture_output=True, check=True,
    ).stdout.strip()
    if actual != expected:
        raise RuntimeError(f"SOURCE_DRIFT {rel}: {actual} != {expected}")
    return target.read_text(encoding="utf-8")


def verify(root: Path) -> None:
    root = root.resolve(strict=True)
    texts = {path: read_sealed(root, path, blob)
             for path, blob in sorted(EXPECTED.items())}
    obj = texts["src/java.base/share/classes/java/lang/Object.java"]
    loader = texts["src/hotspot/share/classfile/classLoader.cpp"]
    alias = texts["test/jdk/java/lang/Object/ObjectToAdpaterLegacyBinaryTest.java"]
    require_count(obj, "public <T> T toAdapter(Class<T> clazz) {")
    require_count(obj, "public <T> T toAdpater(Class<T> clazz) {")
    require_count(obj, "return toAdapter(clazz);")
    require_count(loader, "const int Object_vtable_size = 7;")
    require_count(alias, "4eff8d88c8154820bee15152e0f2cb4791b305cc67b5675b34276310aa568e36")
    if re.search(r"const int Object_vtable_size\s*=\s*6\s*;", loader):
        raise RuntimeError("STRUCTURE_DRIFT obsolete six-slot vtable")
    print(f"PASS candidate-only source identity: files={len(texts)} mode=read-only")
    print("NOT_RUN product HotSpot/java.base compilation, jtreg, JNI runtime, full CI")


if __name__ == "__main__":
    try:
        if len(sys.argv) > 2:
            raise ValueError("usage: verify_candidate.py [repository-root]")
        root_path = Path(sys.argv[1]) if len(sys.argv) == 2 else Path(__file__).resolve().parents[3]
        verify(root_path)
    except (RuntimeError, ValueError, OSError, subprocess.CalledProcessError) as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        sys.exit(1)
