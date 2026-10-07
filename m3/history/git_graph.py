# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Read-only Git object adapter. No working-tree, ref or index mutation API."""
from __future__ import annotations

import hashlib
import heapq
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

OID = re.compile(r"(?:[0-9a-f]{40}|[0-9a-f]{64})\Z")


def exact_id(value: str) -> str:
    """Reject abbreviations, ref expressions and command-line options."""
    if not isinstance(value, str) or OID.fullmatch(value) is None:
        raise ValueError("a full lowercase Git object ID is required")
    return value


def path_text(value: bytes) -> str:
    """Losslessly encode arbitrary Git path bytes into a single TSV cell."""
    return json.dumps(value.decode("utf-8", "surrogateescape"), ensure_ascii=True)


def topological(parents: dict[str, list[str]]) -> list[str]:
    """Parent-first topological order with full-OID tie breaking, never dates."""
    children: dict[str, list[str]] = {s: [] for s in parents}
    degrees = {s: sum(p in parents for p in ps) for s, ps in parents.items()}
    for child, ps in parents.items():
        for parent in ps:
            if parent in children:
                children[parent].append(child)
    ready = [s for s, d in degrees.items() if d == 0]
    heapq.heapify(ready)
    ordered = []
    while ready:
        node = heapq.heappop(ready)
        ordered.append(node)
        for child in children[node]:
            degrees[child] -= 1
            if degrees[child] == 0:
                heapq.heappush(ready, child)
    if len(ordered) != len(parents):
        raise ValueError("incomplete or cyclic commit graph")
    return ordered


def raw_changes(data: bytes) -> list[tuple[str, str, str, str, str, bytes]]:
    """Decode --raw -z --no-renames output; never split paths on whitespace."""
    fields = data.split(b"\0")
    if fields[-1] != b"":
        raise ValueError("unterminated raw diff")
    fields.pop()
    if len(fields) % 2:
        raise ValueError("incomplete raw diff")
    changes = []
    for index in range(0, len(fields), 2):
        header = fields[index].decode("ascii").split()
        if len(header) != 5 or not header[0].startswith(":"):
            raise ValueError("invalid raw-diff header")
        oldmode, newmode, old, new, status = header
        oldmode = oldmode[1:]
        exact_id(old)
        exact_id(new)
        if status not in {"A", "M", "D", "T"} or not fields[index + 1]:
            raise ValueError("unsupported or invalid raw-diff record")
        changes.append((oldmode, newmode, old, new, status, fields[index + 1]))
    return changes


class GitGraph:
    """One bounded sequential reader; callers receive facts, not promotion."""

    def __init__(self, repo: Path, max_bytes: int = 64 * 1024 * 1024):
        self.repo = repo.resolve(strict=True)
        self.max_bytes = max_bytes
        self.commands: list[list[str]] = []
        self.cache: dict[str, tuple[str, int]] = {}
        self.env = dict(os.environ, GIT_OPTIONAL_LOCKS="0", GIT_NO_REPLACE_OBJECTS="1",
                        GIT_CONFIG_NOSYSTEM="1", GIT_CONFIG_GLOBAL=os.devnull,
                        GIT_ATTR_NOSYSTEM="1", LC_ALL="C", GIT_TERMINAL_PROMPT="0")
        self.root = Path(self.run("rev-parse", "--show-toplevel").decode().strip()).resolve()
        self.common = Path(self.run("rev-parse", "--path-format=absolute", "--git-common-dir")
                           .decode().strip()).resolve()
        if self.run("rev-parse", "--is-shallow-repository").strip() != b"false":
            raise ValueError("shallow history refused; use a complete clone")
        if (self.common / "info/grafts").exists():
            raise ValueError("legacy grafts refused")
        self.initial_refs = self.refs()

    def run(self, *args: str, allow_one: bool = False) -> bytes:
        """No shell; bounded time and post-execution output size; stderr not logged."""
        command = ["git", "--no-pager", "-c", "core.fsmonitor=false", "-C", str(self.repo), *args]
        self.commands.append(list(args))
        with tempfile.TemporaryFile() as out, tempfile.TemporaryFile() as err:
            result = subprocess.run(command, env=self.env, stdout=out, stderr=err,
                                    timeout=60, check=False)
            if out.tell() > self.max_bytes or err.tell() > self.max_bytes:
                raise ValueError("Git command output budget exceeded")
            if result.returncode != 0 and not (allow_one and result.returncode == 1):
                raise ValueError("Git read failed: " + args[0] + " (exit " + str(result.returncode) + ")")
            out.seek(0)
            return out.read()

    def refs(self) -> bytes:
        return self.run("for-each-ref", "--format=%(refname) %(objectname)") + self.run("rev-parse", "HEAD")

    def commit_id(self, oid: str) -> str:
        value = exact_id(oid)
        actual = self.run("rev-parse", "--verify", value + "^{commit}").decode().strip()
        if actual != value:
            raise ValueError("snapshot ID does not name the exact commit")
        return value

    def ancestor(self, older: str, newer: str) -> bool:
        # rev-list --count avoids treating an arbitrary command failure as false.
        return self.run("rev-list", "--count", older, "--not", newer).strip() == b"0"

    def graph(self, upstream: str, tips: list[str], limit: int) -> dict[str, list[str]]:
        lines = self.run("rev-list", "--parents", *sorted(set(tips)), "--not", upstream).splitlines()
        if len(lines) > limit:
            raise ValueError("commit count budget exceeded")
        return {parts[0]: parts[1:] for line in lines if (parts := line.decode("ascii").split())}

    def metadata(self, oid: str) -> tuple[str, str, str]:
        tree, stamp, subject = self.run("show", "-s", "--format=%T%x00%ct%x00%s", oid).rstrip(b"\n").split(b"\0", 2)
        return tree.decode("ascii"), stamp.decode("ascii"), path_text(subject)

    def changes(self, parent: str, child: str):
        return raw_changes(self.run("diff-tree", "--no-commit-id", "--raw", "-r", "-z",
                                    "--no-abbrev", "--no-renames", parent, child, "--"))

    def patch(self, parent: str, child: str) -> bytes:
        return self.run("diff", "--no-ext-diff", "--no-textconv", "--no-renames",
                        "--binary", "--full-index", "--no-color", "--diff-algorithm=myers",
                        "--no-indent-heuristic", "--src-prefix=a/", "--dst-prefix=b/",
                        parent, child, "--")

    def entries(self, commit: str, paths: set[bytes]) -> dict[bytes, tuple[str, str, str]]:
        entries = {}
        for record in self.run("ls-tree", "-r", "-z", "--full-tree", commit).split(b"\0"):
            if record:
                info, path = record.split(b"\t", 1)
                if path in paths:
                    mode, kind, oid = info.decode("ascii").split()
                    entries[path] = (mode, kind, oid)
        return entries

    def blob_digest(self, oid: str, mode: str) -> tuple[str, int]:
        if not oid.strip("0") or mode == "000000":
            return "ABSENT", 0
        if mode == "160000":
            return "GITLINK_NOT_TRAVERSED", 0
        if oid not in self.cache:
            content = self.run("cat-file", "blob", exact_id(oid))
            self.cache[oid] = hashlib.sha256(content).hexdigest(), len(content)
        return self.cache[oid]
