#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed path and byte custody for JDK backport FILE patch atoms.

This helper guards candidate evidence, not JDK product source. Normal exact replays
are idempotent; divergent output is never overwritten, and no symlink/hardlink
alias is accepted as an output or target source path.
"""

from __future__ import annotations

import os
import stat
from pathlib import Path


def relative_git_path(value: str) -> str:
    if not isinstance(value, str) or not value or len(value) > 4096:
        raise ValueError("invalid patch atom path")
    if (value.startswith("/") or "\\" in value or ":" in value
            or any(ord(c) < 32 or ord(c) == 127 for c in value)):
        raise ValueError("nonportable or absolute patch atom path")
    parts = value.split("/")
    if any(part in ("", ".", "..", ".git") for part in parts):
        raise ValueError("traversal or noncanonical patch atom path")
    return value


def _check_chain(path: Path) -> None:
    """No ancestor or final component may be a symlink."""
    for candidate in (*reversed(path.parents), path):
        try:
            mode = candidate.lstat().st_mode
        except FileNotFoundError:
            continue
        if stat.S_ISLNK(mode):
            raise ValueError(f"symlink in backport path: {candidate}")


def _is_within(path: Path, root: Path) -> bool:
    return path == root or root in path.parents


def safe_output_root(output: Path, donor: Path, target: Path) -> Path:
    """Never put generated patch material inside or above a source tree."""
    result = Path(output).absolute()
    _check_chain(result)
    for source in (donor, target):
        source = Path(source).absolute()
        _check_chain(source)
        if _is_within(result, source) or _is_within(source, result):
            raise ValueError("backport output overlaps donor or target source")
    result.mkdir(parents=True, exist_ok=True)
    if not result.is_dir():
        raise ValueError("backport output root is not a directory")
    _check_chain(result)
    return result


def target_blob(root: Path, relative: str) -> tuple[bool, bytes]:
    """Only regular, single-link target preimages may become patch candidates."""
    relative = relative_git_path(relative)
    base = Path(root).absolute()
    path = base / relative
    _check_chain(path)
    if not path.exists():
        return False, b""
    info = path.lstat()
    if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1:
        raise ValueError(f"target path is not a singly owned regular file: {relative}")
    return True, path.read_bytes()


def _make_dirs(root: Path, path: Path) -> None:
    if not _is_within(path, root) or path == root:
        raise ValueError("output escapes receipt root")
    _check_chain(path.parent)
    path.parent.mkdir(parents=True, exist_ok=True)
    _check_chain(path.parent)


def write_exact(root: Path, relative: str, payload: bytes) -> None:
    """Write once or accept exactly identical bytes; never mutate another hardlink."""
    name = relative_git_path(relative)
    path = Path(root).absolute() / name
    _make_dirs(Path(root).absolute(), path)
    _check_chain(path)
    try:
        info = path.lstat()
    except FileNotFoundError:
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
        if hasattr(os, "O_NOFOLLOW"):
            flags |= os.O_NOFOLLOW
        fd = os.open(path, flags, 0o600)
        with os.fdopen(fd, "wb") as stream:
            stream.write(payload)
        return
    if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1:
        raise ValueError(f"output aliases another file: {relative}")
    if path.read_bytes() != payload:
        raise ValueError(f"existing backport output has drifted: {relative}")
