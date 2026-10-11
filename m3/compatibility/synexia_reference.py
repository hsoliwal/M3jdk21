# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Offline content-addressed Synexia compatibility source pins for CI."""
import hashlib
from pathlib import Path

SYNEXIA_COMMIT = "64a2ea61c73b548413fed6686a9daeeb0b9b0564"
LICENSE_FILE = "LICENSE-APACHE-2.0.txt"
LICENSE_BLOB_SHA = "29f81d812f3e768fa89638d1f72920dbfd1413a8"
NOTICE_FILE = "NOTICE"
NOTICE_BLOB_SHA = "97bb5d49f8bd3bc573c19afe4ea2392a3d02704e"
SOURCE_BLOBS = {
    "FrozenChars.java": "75264c85b4da2cd7d9193d8e245364efb53aa936",
    "FrozenBytes.java": "0e249745d0e96a1d9a8754a075f33cdf42e8fc0b",
}
SOURCE_PREFIX = Path("com/synexia/indexstring")
SPDX_PREFIX = b"// SPDX-License-Identifier: Apache-2.0"


def git_blob_sha(data: bytes) -> str:
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()


def verify_source_bytes(name: str, data: bytes, expected: str) -> None:
    if git_blob_sha(data) != expected:
        raise ValueError("Synexia reference source blob pin mismatch: " + name)
    if not data.startswith(SPDX_PREFIX):
        raise ValueError("Synexia reference SPDX marker changed: " + name)


def pinned_sources(reference_root: Path) -> tuple[Path, ...]:
    root = Path(reference_root)
    license_path = root / LICENSE_FILE
    if git_blob_sha(license_path.read_bytes()) != LICENSE_BLOB_SHA:
        raise ValueError("Synexia Apache-2.0 license blob pin mismatch")
    notice_path = root / NOTICE_FILE
    if git_blob_sha(notice_path.read_bytes()) != NOTICE_BLOB_SHA:
        raise ValueError("Synexia NOTICE blob pin mismatch")
    source_root = root / SYNEXIA_COMMIT / SOURCE_PREFIX
    paths = []
    for name, expected in SOURCE_BLOBS.items():
        path = source_root / name
        verify_source_bytes(name, path.read_bytes(), expected)
        paths.append(path)
    return tuple(paths)
