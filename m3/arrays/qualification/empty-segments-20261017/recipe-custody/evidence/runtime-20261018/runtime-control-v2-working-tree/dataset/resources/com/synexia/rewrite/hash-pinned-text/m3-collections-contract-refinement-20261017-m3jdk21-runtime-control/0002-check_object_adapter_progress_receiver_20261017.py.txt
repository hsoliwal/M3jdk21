#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only current receiver custody; recipe/runtime qualification remains separate."""
from __future__ import annotations

import hashlib
from pathlib import Path, PurePosixPath
import re

ROOT = Path(__file__).resolve().parents[2]
BASE_MANIFEST = "m3/compatibility/m3-object-adapter-progress-20261017.tsv"
NEXT_MANIFEST = "m3/compatibility/m3-default-template-source-seal.tsv"
BASE_MANIFEST_SHA256 = "2f581a791adc7ecf7f217a6276f0da8af424c1e3e49385ca6ae44704d3319c82"
NEXT_MANIFEST_SHA256 = "c431bf732eb91a289b4bcd0b886db71d43da7d7bb17074cd6116db6b37e73ad6"
DOCS_MANIFEST = "m3/compatibility/m3-progress-doclint-20261018.tsv"
DOCS_MANIFEST_SHA256 = "7246c16a0e8ec742eba98dac97e24b7fc3947250ad5d2a22f72ac32dcddf0219"
FUNCTIONAL_MANIFEST = "m3/compatibility/m3-functional-progress-doclint-20261018.tsv"
FUNCTIONAL_MANIFEST_SHA256 = "3b62981be6a617b903c559cc31904eca2bffb2b200380f1be57e07a8f634d969"
FUNCTIONAL = frozenset((
    "src/java.base/share/classes/java/util/function/Consumer.java",
    "src/java.base/share/classes/java/util/function/Function.java",
    "src/java.base/share/classes/java/util/function/Supplier.java",
))
DOCUMENTED = frozenset((
    "src/java.base/share/classes/java/lang/Runnable.java",
    "src/java.base/share/classes/java/util/concurrent/Callable.java",
    "src/java.base/share/classes/java/util/concurrent/Executor.java",
    "src/java.base/share/classes/java/util/concurrent/ExecutorService.java",
    "src/java.base/share/classes/java/util/concurrent/MonitoredCallable.java",
    "src/java.base/share/classes/java/util/concurrent/MonitoredRunnable.java",
    "src/java.base/share/classes/java/util/concurrent/ProgressMonitor.java",
    "src/java.base/share/classes/java/util/concurrent/ProgressMonitors.java",
))
REPLACED = frozenset((
    "src/java.base/share/classes/java/util/concurrent/Callable.java",
    "src/java.base/share/classes/java/util/concurrent/ProgressMonitors.java",
))
ADDED = frozenset((
    "src/java.base/share/classes/java/util/concurrent/MonitoredCallable.java",
    "src/java.base/share/classes/java/util/concurrent/MonitoredRunnable.java",
    "test/jdk/java/lang/Object/M3MonitoredTemplateTest.java",
))


def contained_file(root: Path, relative: str) -> Path:
    """Admit a regular file beneath the caller's receiving root, without symlinks."""
    part = PurePosixPath(relative)
    if (not relative or str(part) != relative or part.is_absolute()
            or ".." in part.parts or ".git" in part.parts or "\\" in relative
            or any(c in relative for c in "\t\r\n\0")):
        raise RuntimeError("unsafe receiver path: " + repr(relative))
    root = root.absolute()
    if root.is_symlink() or not root.is_dir():
        raise RuntimeError("receiver root must be a real directory")
    path = root.joinpath(*part.parts)
    cursor = path
    while cursor != root:
        if cursor.is_symlink():
            raise RuntimeError("symlink receiver: " + relative)
        cursor = cursor.parent
    if not path.is_file() or not path.resolve().is_relative_to(root.resolve()):
        raise RuntimeError("missing or non-regular receiver: " + relative)
    return path


def read_manifest(root: Path, relative: str, expected: str, count: int):
    """Keep each historical manifest byte-exact before interpreting its source transitions."""
    data = contained_file(root, relative).read_bytes()
    if hashlib.sha256(data).hexdigest() != expected:
        raise RuntimeError("historical manifest custody drift: " + relative)
    rows = {}
    for line in data.decode("utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        cells = line.split("\t")
        if len(cells) != 4:
            raise RuntimeError("invalid manifest row: " + relative)
        path, before, after, template = cells
        part = PurePosixPath(path)
        if (not path.startswith(("src/java.base/share/classes/", "test/jdk/"))
                or str(part) != path or ".." in part.parts or "\\" in path
                or path in rows or not re.fullmatch(r"[0-9a-f]{64}", after)
                or (before != "ABSENT" and not re.fullmatch(r"[0-9a-f]{64}", before))
                or not template or PurePosixPath(template).name != template):
            raise RuntimeError("invalid or duplicate manifest target: " + path)
        rows[path] = (before, after, template)
    if len(rows) != count:
        raise RuntimeError("manifest coverage differs: " + relative)
    return rows


def compose_targets(original, successor):
    """Compose the one published successor; never admit arbitrary old/new mixtures."""
    if len(original) != 11 or set(successor) != REPLACED | ADDED:
        raise RuntimeError("unexpected receiver successor closure")
    if set(original) & set(successor) != REPLACED or set(original) & ADDED:
        raise RuntimeError("unexpected receiver overlap")
    current = {path: row[1] for path, row in original.items()}
    for path in sorted(successor):
        before, after, _ = successor[path]
        if path in REPLACED:
            if before != current[path]:
                raise RuntimeError("broken historical transition: " + path)
        elif before != "ABSENT":
            raise RuntimeError("successor addition was not absent: " + path)
        current[path] = after
    if len(current) != 14:
        raise RuntimeError("current receiver coverage differs")
    return current


def compose_documentation(current, documentation):
    """Apply only the eight explicitly sealed documentation postimages."""
    if set(documentation) != DOCUMENTED or not DOCUMENTED.issubset(current):
        raise RuntimeError("unexpected documentation successor closure")
    expected = dict(current)
    for path, (before, after, _) in sorted(documentation.items()):
        if before != current[path]:
            raise RuntimeError("broken documentation transition: " + path)
        expected[path] = after
    return expected


def compose_functional(current, documentation):
    """Apply only the three explicitly sealed functional Javadoc postimages."""
    if set(documentation) != FUNCTIONAL or not FUNCTIONAL.issubset(current):
        raise RuntimeError("unexpected functional successor closure")
    expected = dict(current)
    for path, (before, after, _) in sorted(documentation.items()):
        if before != current[path]:
            raise RuntimeError("broken functional transition: " + path)
        expected[path] = after
    return expected


def current_targets(root: Path):
    original = read_manifest(root, BASE_MANIFEST, BASE_MANIFEST_SHA256, 11)
    successor = read_manifest(root, NEXT_MANIFEST, NEXT_MANIFEST_SHA256, 5)
    documentation = read_manifest(root, DOCS_MANIFEST, DOCS_MANIFEST_SHA256, 8)
    functional = read_manifest(root, FUNCTIONAL_MANIFEST, FUNCTIONAL_MANIFEST_SHA256, 3)
    return compose_functional(compose_documentation(compose_targets(original, successor), documentation), functional)


def verify(root: Path | None = None) -> int:
    root = ROOT if root is None else Path(root)
    expected = current_targets(root)
    for path, after in sorted(expected.items()):
        actual = hashlib.sha256(contained_file(root, path).read_bytes()).hexdigest()
        if actual != after:
            raise RuntimeError(f"source drift: {path}: {actual} != {after}")
    print("PASS exact 14-file current receiver identity; historical manifests preserved; "
          "Object compatibility and runtime admission remain HOLD")
    return len(expected)


if __name__ == "__main__":
    verify()
