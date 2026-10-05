#!/usr/bin/env python3
"""Package already-finished proof evidence; never execute a proof or build tool.

The archive is review/reconstruction evidence, not a hermetic toolchain image.
Estimate accepts unfinished lanes. Pack requires explicit terminal dispositions,
stable input bytes, and complete retention of admitted local sealed input bytes.
Every write is to a fresh destination. No evidence input is rewritten or deleted.
"""

from __future__ import annotations

import argparse
import collections
import gzip
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import stat
import sys
import tarfile


SCHEMA = "synexia.evidence.bundle/1"
PLAN_SCHEMA = "synexia.evidence.package-plan/1"
CHUNK_BYTES = 512 * 1024
MANIFEST_BYTES = 240 * 1024
PART_BYTES = 1024 * 1024
GROUP_BYTES = 8 * 1024 * 1024
SHA_RE = re.compile(r"^[0-9a-f]{64}$")
SAFE_LANE = re.compile(r"^[a-z0-9][a-z0-9-]*$")
SECRET_NAMES = {"settings.xml", "settings-security.xml", "credentials.json",
                "secrets.json", ".env", ".npmrc", ".pypirc", "id_rsa", "id_ed25519"}
CACHE_PARTS = {".git", "__pycache__", ".pytest_cache", ".mypy_cache", ".ruff_cache",
               "node_modules", "m2", "m3-m2", "m3-python", "wheels", "toolchain"}
BINARY_SUFFIXES = {".class", ".so", ".o", ".a", ".dll", ".dylib", ".pyc", ".pyo"}
DEPENDENCY_SUFFIXES = {".jar", ".jmod", ".whl", ".zip", ".tgz", ".xz", ".zst"}
PRIVATE_MATERIAL = re.compile(
    rb"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----|"
    rb"\bgh[pousr]_[A-Za-z0-9]{36,}\b|\bgithub_pat_[A-Za-z0-9_]{50,}\b")
BOUNDARY = (
    "This is exact review/reconstruction evidence, not a hermetic toolchain image. "
    "Maven, JDK, Python and other dependency caches, compiled binaries, settings and "
    "credential material are omitted. Their sealed identities remain in the original "
    "manifests where recorded. OS, dynamic-library, shell and standard-library limits "
    "in the original tooling/results remain applicable. Packaging executes no proof "
    "gate and does not turn a focused or failed result into repository-wide admission."
)


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def encoded(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n").encode("utf-8")


def safe_relative(value: str) -> str:
    p = PurePosixPath(value)
    if p.is_absolute() or not value or any(x in ("", ".", "..") for x in p.parts):
        raise ValueError(f"Unsafe logical path: {value!r}")
    if "\\" in value or "\x00" in value or "\n" in value or "\t" in value:
        raise ValueError(f"Unsupported logical path: {value!r}")
    return p.as_posix()


def under(path: Path, parent: Path) -> bool:
    return path == parent or parent in path.parents


def signature(p: Path) -> tuple[int, int, int, int]:
    s = p.stat()
    return (s.st_dev, s.st_ino, s.st_size, s.st_mtime_ns)


def classify(path: Path, logical: str) -> str | None:
    low = path.name.lower()
    parts = {x.lower() for x in PurePosixPath(logical).parts}
    if low in SECRET_NAMES or low.startswith(".env."):
        return "settings-or-credential-file"
    if "settings" in low and (low.endswith(".xml") or "acquisition" in low):
        return "settings-or-settings-acquisition"
    if parts & CACHE_PARTS:
        return "dependency-or-interpreter-cache"
    if "target" in parts and "surefire" in parts:
        return "transient-surefire-booter"
    if low.endswith((".tmp", ".temp", ".lck", ".lock")):
        return "transient-file"
    if path.suffix.lower() in BINARY_SUFFIXES or re.search(r"\.so(?:\.\d+)+$", low):
        return "compiled-artifact-hash-only"
    if path.suffix.lower() in DEPENDENCY_SUFFIXES or low.endswith(".tar.gz"):
        return "dependency-or-toolchain-archive"
    return None


class Inventory:
    def __init__(self, root: Path, plan: dict, output: Path, strict: bool):
        self.root = root
        self.plan = plan
        self.output = output
        self.strict = strict
        self.files: dict[str, dict] = {}
        self.objects: dict[str, bytes] = {}
        self.omissions: list[dict] = []
        self.omitted_paths: set[str] = set()
        self.references: list[dict] = []
        self.directory_omissions: dict[str, dict] = {}
        self.signatures: dict[Path, tuple[int, int, int, int]] = {}
        self.sha_paths: dict[str, list[str]] = collections.defaultdict(list)
        self.warnings: list[str] = []
        self.reference_sources: list[tuple[Path, Path]] = []
        self.lanes = self.read_lanes()
        self.primary = self.discover()

    def read_lanes(self) -> list[dict]:
        selection = self.plan["laneSelection"]
        names = set(selection.get("required", []))
        if selection.get("includeAllExisting", False):
            names.update(p.name for p in (self.root / "evidence").iterdir() if p.is_dir())
        names.update(selection.get("additional", []))
        allowed_unexecuted = set(selection.get("allowNotExecuted", []))
        result = []
        for name in sorted(names):
            if not SAFE_LANE.fullmatch(name):
                raise ValueError(f"Unsupported lane name: {name!r}")
            lane = self.root / "evidence" / name
            markers = [lane / x for x in ("RESULT.json", "STOPPED.json", "NOT_EXECUTED.json")
                       if (lane / x).is_file()]
            if len(markers) > 1:
                raise ValueError(f"Ambiguous terminal markers: {name}")
            row = {"lane": name, "required": name in selection.get("required", []),
                   "terminal": False, "reportedStatus": "MISSING_OR_RUNNING"}
            if markers:
                marker = markers[0]
                if marker.is_symlink() or marker.resolve() != marker:
                    raise ValueError(f"Symlink terminal marker is not admitted: {name}")
                data = marker.read_bytes()
                if PRIVATE_MATERIAL.search(data):
                    raise ValueError(f"Private-material pattern in terminal marker: {name}")
                body = json.loads(data)
                status_value = body.get("status", body.get("result", body.get("state", "UNSPECIFIED")))
                is_unexecuted = marker.name == "NOT_EXECUTED.json"
                terminal_status = isinstance(status_value, str) and (
                    status_value.startswith(("PASS", "STOPPED", "FAIL"))
                    or is_unexecuted and status_value.startswith("NOT_EXECUTED"))
                row.update({"marker": marker.relative_to(self.root).as_posix(),
                            "markerSHA256": sha(data), "markerBytes": len(data),
                            "reportedStatus": status_value,
                            "reportedBaseline": body.get("baseline", body.get("commit")),
                            "terminal": terminal_status and
                            (not is_unexecuted or name in allowed_unexecuted),
                            "disposition": "explicitly-not-executed" if is_unexecuted
                            else "reported-failure" if "STOP" in str(status_value)
                            or "FAIL" in str(status_value) else "reported-result",
                            "originalResult": body})
            if self.strict and not row["terminal"]:
                raise ValueError(f"Lane lacks an admitted final disposition: {name}")
            result.append(row)
        return result

    def discover(self) -> list[Path]:
        starts = [self.root / safe_relative(x) for x in self.plan.get("includePaths", [])]
        starts.extend(self.root / "evidence" / row["lane"] for row in self.lanes)
        found: set[Path] = set()
        for start in starts:
            if under(start, self.output):
                continue
            if not start.exists():
                if self.strict and not under(start, self.root / "evidence"):
                    raise ValueError(f"Required include path does not exist: {start}")
                continue
            if start.is_file() or start.is_symlink():
                found.add(start)
                continue
            for current, dirs, files in os.walk(start, followlinks=False):
                parent = Path(current)
                retained_dirs = []
                for name in sorted(dirs):
                    directory = parent / name
                    if under(directory, self.output):
                        continue
                    if name in CACHE_PARTS or directory.is_symlink():
                        logical = directory.relative_to(self.root).as_posix()
                        self.directory_omissions[logical] = {
                            "path": logical, "reason": "symlink-directory-not-followed"
                            if directory.is_symlink() else "excluded-cache-directory",
                            "contentsEnumerated": False, "currentBytesRead": False}
                    else:
                        retained_dirs.append(name)
                dirs[:] = retained_dirs
                for name in sorted(files):
                    path = parent / name
                    if not under(path, self.output):
                        found.add(path)
        return sorted(found, key=lambda p: p.relative_to(self.root).as_posix())

    def add(self, path: Path) -> None:
        logical = safe_relative(path.relative_to(self.root).as_posix())
        if logical in self.files or logical in self.omitted_paths:
            return
        if path.is_symlink() or path.resolve() != path:
            self.omissions.append({"path": logical, "reason": "symlink-not-followed"})
            self.omitted_paths.add(logical)
            return
        before = signature(path)
        data = path.read_bytes()
        after = signature(path)
        if before != after:
            if self.strict:
                raise ValueError(f"Input changed while read: {logical}")
            self.warnings.append(f"Input changed while estimated: {logical}")
            return
        self.signatures[path] = after
        digest = sha(data)
        reason = classify(path, logical)
        if reason is None and (data[:4] == b"\x7fELF" or data[:2] == b"MZ"):
            reason = "compiled-artifact-hash-only"
        if reason is None and PRIVATE_MATERIAL.search(data):
            reason = "private-material-pattern-content-omitted"
        if reason:
            self.omissions.append({"path": logical, "bytes": len(data), "sha256": digest,
                                   "reason": reason, "currentBytesHashed": True})
            self.omitted_paths.add(logical)
            return
        chunks = []
        for offset in range(0, len(data), CHUNK_BYTES):
            part = data[offset:offset + CHUNK_BYTES]
            part_sha = sha(part)
            prior = self.objects.setdefault(part_sha, part)
            if prior != part:
                raise ValueError("SHA-256 chunk collision")
            chunks.append({"sha256": part_sha, "bytes": len(part)})
        self.files[logical] = {"path": logical, "bytes": len(data), "sha256": digest,
                               "mode": stat.S_IMODE(path.stat().st_mode) & 0o777,
                               "chunks": chunks}
        self.sha_paths[digest].append(logical)
        if path.name == "inputs.json" and under(path, self.root / "evidence"):
            self.reference_sources.append((path, path.parent))
        if path.name == "SOURCE-MANIFEST.json" and path.parent.name == "proof":
            self.reference_sources.append((path, path.parent.parent))

    def audit_references(self) -> None:
        seen_sources: set[Path] = set()
        offset = 0
        while offset < len(self.reference_sources):
            manifest, relative_base = self.reference_sources[offset]
            offset += 1
            if manifest in seen_sources:
                continue
            seen_sources.add(manifest)
            body = json.loads(manifest.read_bytes())
            if not isinstance(body, list):
                raise ValueError(f"Unsupported input manifest: {manifest}")
            for entry in body:
                if not isinstance(entry, dict) or not isinstance(entry.get("path"), str):
                    raise ValueError(f"Malformed sealed input row: {manifest}")
                expected = entry.get("sha256")
                if not isinstance(expected, str) or not SHA_RE.fullmatch(expected):
                    raise ValueError(f"Malformed sealed input identity: {manifest}")
                original = entry["path"]
                value = Path(original)
                value = value if value.is_absolute() else relative_base / value
                # Resolve lexical '..' only. Never use a symlink to escape into caches.
                value = Path(os.path.abspath(value))
                row = {"manifest": manifest.relative_to(self.root).as_posix(),
                       "declaredPath": original, "expectedSHA256": expected,
                       "expectedBytes": entry.get("bytes")}
                if not under(value, self.root):
                    row.update({"retention": "external-input-manifest-only",
                                "currentBytesRead": False})
                else:
                    logical = value.relative_to(self.root).as_posix()
                    reason = classify(value, logical)
                    if reason:
                        row.update({"retention": "excluded-input-manifest-only",
                                    "reason": reason, "currentBytesRead": False})
                    elif value.is_file() and not value.is_symlink():
                        self.add(value)
                        actual = self.files.get(logical)
                        if actual and actual["sha256"] == expected:
                            if entry.get("bytes") is not None and actual["bytes"] != entry["bytes"]:
                                raise ValueError(f"Sealed input byte-count mismatch: {logical}")
                            row.update({"retention": "exact-path-and-content", "path": logical})
                        else:
                            row.update({"retention": "needs-exact-content-alias",
                                        "observedSHA256": actual.get("sha256") if actual else None})
                    else:
                        row["retention"] = "needs-exact-content-alias"
                self.references.append(row)
        for row in self.references:
            if row["retention"] == "needs-exact-content-alias":
                aliases = sorted(self.sha_paths.get(row["expectedSHA256"], []))
                if aliases:
                    if (row["expectedBytes"] is not None
                            and self.files[aliases[0]]["bytes"] != row["expectedBytes"]):
                        raise ValueError(f"Sealed alias byte-count mismatch: {row['declaredPath']}")
                    row.update({"retention": "exact-content-preserved-at-alias",
                                "exactContentPaths": aliases})
                else:
                    row["retention"] = "unavailable-admitted-local-input"
                    message = f"Exact local sealed input unavailable: {row['manifest']} -> {row['declaredPath']}"
                    if self.strict:
                        raise ValueError(message)
                    self.warnings.append(message)

    def collect(self) -> None:
        for path in self.primary:
            self.add(path)
        self.audit_references()
        if self.strict:
            if self.primary != self.discover():
                raise ValueError("Selected file set changed during packaging")
            for path, initial in self.signatures.items():
                if not path.exists() or signature(path) != initial:
                    raise ValueError(f"Selected bytes changed during packaging: {path}")
            if self.lanes != self.read_lanes():
                raise ValueError("Lane terminal disposition changed during packaging")

    def summary(self) -> dict:
        return {"retainedPaths": len(self.files),
                "retainedPathBytes": sum(r["bytes"] for r in self.files.values()),
                "uniqueChunks": len(self.objects),
                "uniqueChunkBytes": sum(map(len, self.objects.values())),
                "omittedFileRecords": len(self.omissions),
                "omittedFileBytes": sum(r.get("bytes", 0) for r in self.omissions),
                "omittedDirectories": len(self.directory_omissions),
                "omissionsByReason": dict(collections.Counter(r["reason"] for r in self.omissions)),
                "sealedInputReferences": len(self.references),
                "referencesByRetention": dict(collections.Counter(r["retention"] for r in self.references))}


def shards(kind: str, rows: list[dict]) -> list[tuple[str, bytes, int]]:
    result = []
    data = bytearray()
    count = 0
    for row in rows:
        line = (json.dumps(row, sort_keys=True, separators=(",", ":"), ensure_ascii=False) + "\n").encode()
        if len(line) > MANIFEST_BYTES:
            raise ValueError(f"One {kind} record exceeds the fixed manifest limit")
        if data and len(data) + len(line) > MANIFEST_BYTES:
            result.append((f"manifests/{kind}-{len(result) + 1:04d}.jsonl", bytes(data), count))
            data.clear()
            count = 0
        data.extend(line)
        count += 1
    if data:
        result.append((f"manifests/{kind}-{len(result) + 1:04d}.jsonl", bytes(data), count))
    return result


def tar_gzip(members: list[tuple[str, bytes]]) -> bytes:
    raw = io.BytesIO()
    with tarfile.open(fileobj=raw, mode="w", format=tarfile.USTAR_FORMAT) as archive:
        for name, data in members:
            item = tarfile.TarInfo(name)
            item.size = len(data)
            item.mode = 0o644
            item.mtime = 0
            item.uid = item.gid = 0
            item.uname = item.gname = ""
            archive.addfile(item, io.BytesIO(data))
    return gzip.compress(raw.getvalue(), compresslevel=9, mtime=0)


def make_parts(members: list[tuple[str, bytes]]) -> list[tuple[bytes, int]]:
    groups: list[list[tuple[str, bytes]]] = []
    current: list[tuple[str, bytes]] = []
    estimate = raw_size = 0
    for member in members:
        size = len(gzip.compress(member[1], compresslevel=9, mtime=0)) + 512
        if current and (estimate + size > PART_BYTES - 16384 or raw_size + len(member[1]) > GROUP_BYTES):
            groups.append(current)
            current = []
            estimate = raw_size = 0
        current.append(member)
        estimate += size
        raw_size += len(member[1])
    if current:
        groups.append(current)
    result = []

    def append_group(group: list[tuple[str, bytes]]) -> None:
        payload = tar_gzip(group)
        if len(payload) <= PART_BYTES:
            result.append((payload, len(group)))
        elif len(group) > 1:
            middle = len(group) // 2
            append_group(group[:middle])
            append_group(group[middle:])
        else:
            raise ValueError("A bounded single member exceeded the archive part limit")

    for group in groups:
        append_group(group)
    return result


def write_new(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("xb") as stream:
        stream.write(data)


def manifest_members(inventory: Inventory) -> tuple[list[tuple[str, bytes]], list[dict]]:
    members = []
    index = []
    for kind, rows in (("files", sorted(inventory.files.values(), key=lambda r: r["path"])),
                       ("omissions", sorted(inventory.omissions, key=lambda r: r["path"])),
                       ("directory-omissions", sorted(inventory.directory_omissions.values(),
                                                       key=lambda r: r["path"])),
                       ("references", inventory.references)):
        for name, data, count in shards(kind, rows):
            members.append((name, data))
            index.append({"kind": kind, "member": name, "sha256": sha(data),
                          "bytes": len(data), "records": count})
    return members, index


def estimate(inventory: Inventory, plan_path: Path) -> dict:
    members, _ = manifest_members(inventory)
    compressed = sum(len(gzip.compress(data, compresslevel=9, mtime=0))
                     for data in inventory.objects.values())
    metadata = sum(len(gzip.compress(data, compresslevel=9, mtime=0)) for _, data in members)
    return {"schema": "synexia.evidence.package-estimate/1", "mode": "READ_ONLY_ESTIMATE",
            "proofGatesExecuted": False, "archivesCreated": False,
            "planSHA256": sha(plan_path.read_bytes()), "plan": inventory.plan,
            "summary": inventory.summary(), "lanes": inventory.lanes,
            "individualChunkGzipBytes": compressed, "manifestGzipBytes": metadata,
            "partByteLimit": PART_BYTES,
            "roughPartCount": max(1, (compressed + metadata + PART_BYTES - 1) // PART_BYTES),
            "estimateBoundary": "Individual gzip sizes omit tar/group overhead; ongoing lanes may add bytes.",
            "warnings": inventory.warnings, "boundary": BOUNDARY}


def pack(inventory: Inventory, plan_path: Path) -> dict:
    out = inventory.output
    if out.exists():
        raise ValueError("Package destination must be fresh")
    metadata_members, metadata_index = manifest_members(inventory)
    members = [(f"objects/{digest[:2]}/{digest}", data)
               for digest, data in sorted(inventory.objects.items())] + metadata_members
    parts = make_parts(members)
    out.mkdir(parents=True, exist_ok=False)
    part_index = []
    for position, (data, count) in enumerate(parts, 1):
        name = f"part-{position:04d}.tar.gz"
        write_new(out / name, data)
        part_index.append({"path": name, "sha256": sha(data), "bytes": len(data), "members": count})
    lanes_data = encoded(inventory.lanes)
    readme = ["# Synexia recipe evolution evidence", "", BOUNDARY, "",
              "The original reported status and baseline are preserved below. A result filename alone does not imply success.", "",
              "| Lane | Reported status | Reported baseline |", "| --- | --- | --- |"]
    for lane in inventory.lanes:
        readme.append(f"| {lane['lane']} | {lane['reportedStatus']} | {lane.get('reportedBaseline') or 'See original marker'} |")
    readme.extend(["", "## Contents", "",
                   "Each part is a standalone deterministic tar.gz of content objects and/or JSONL manifest shards. All parts together reconstruct the retained logical paths. Identical source bytes are stored once; large files use ordered 512 KiB chunks.", "",
                   "BUNDLE.json binds every archive part, metadata shard, lane inventory and readable path inventory. `files` shards record original paths, bytes, SHA-256, modes and ordered chunk identities. `omissions` shards record each encountered excluded file and its actual hash when read. `references` shards retain each sealed input identity and state whether its exact content is included, preserved at an alias, or intentionally manifest-only.", "",
                   "Excluded directory names: " + ", ".join(sorted(CACHE_PARTS)) + ". Dependency caches are not traversed. Maven/settings and credential files are omitted. First-party compiled classes/shared objects are hash-only; no third-party jars are bundled. Original build-artifact and tooling manifests remain included.", "",
                   "## Verify and reconstruct", "",
                   "Use the packaged acquisition/package_evidence.py, or the identical reviewed helper, with `verify --bundle DIR`. `extract --bundle DIR --output FRESH_DIR` validates every part, object, manifest and reconstructed file before writing any source. These commands check archive integrity only; they do not rerun lint, compiler, tests or runtime gates.", "",
                   "Absolute paths in original proof metadata are preserved verbatim. Replaying commands requires deliberately restoring the declared environment and dependencies; extraction does not install software or rewrite those paths.", ""])
    readme_data = "\n".join(readme).encode()
    top_files = []
    for name, data in (("LANES.json", lanes_data), ("README.md", readme_data)):
        if len(data) > PART_BYTES:
            raise ValueError(f"Top-level metadata exceeds part limit: {name}")
        write_new(out / name, data)
        top_files.append({"path": name, "sha256": sha(data), "bytes": len(data)})
    lines = ["sha256\tbytes\tpath\n"]
    size = len(lines[0])
    number = 1
    for row in sorted(inventory.files.values(), key=lambda r: r["path"]):
        line = f"{row['sha256']}\t{row['bytes']}\t{row['path']}\n"
        if size + len(line.encode()) > MANIFEST_BYTES:
            data = "".join(lines).encode()
            name = f"PATHS-{number:04d}.tsv"
            write_new(out / name, data)
            top_files.append({"path": name, "sha256": sha(data), "bytes": len(data)})
            number += 1
            lines, size = ["sha256\tbytes\tpath\n"], len("sha256\tbytes\tpath\n")
        lines.append(line)
        size += len(line.encode())
    if len(lines) > 1:
        data = "".join(lines).encode()
        name = f"PATHS-{number:04d}.tsv"
        write_new(out / name, data)
        top_files.append({"path": name, "sha256": sha(data), "bytes": len(data)})
    index = {"schema": SCHEMA, "rootLogicalMapping": "paths are relative to the original task root",
             "sourceRoot": str(inventory.root), "planSHA256": sha(plan_path.read_bytes()),
             "plan": inventory.plan, "summary": inventory.summary(), "parts": part_index,
             "metadataShards": metadata_index, "topFiles": top_files,
             "chunkByteLimit": CHUNK_BYTES, "partByteLimit": PART_BYTES,
             "proofGatesExecuted": False, "boundary": BOUNDARY}
    index_data = encoded(index)
    if len(index_data) > PART_BYTES:
        raise ValueError("Top-level bundle index exceeds the fixed byte limit")
    write_new(out / "BUNDLE.json", index_data)
    return {"schema": "synexia.evidence.package-result/1", "status": "PACKAGED_REVIEW_EVIDENCE",
            "bundle": str(out), "indexSHA256": sha(index_data), "indexBytes": len(index_data),
            "archiveParts": len(parts), "archiveBytes": sum(len(data) for data, _ in parts),
            "summary": inventory.summary(), "proofGatesExecuted": False, "boundary": BOUNDARY}


def verify_bundle(bundle: Path, destination: Path | None) -> dict:
    index_data = (bundle / "BUNDLE.json").read_bytes()
    index = json.loads(index_data)
    if index.get("schema") != SCHEMA:
        raise ValueError("Unsupported bundle schema")
    objects: dict[str, bytes] = {}
    metadata: dict[str, bytes] = {}
    seen_members: set[str] = set()
    for row in index["topFiles"]:
        data = (bundle / safe_relative(row["path"])).read_bytes()
        if sha(data) != row["sha256"] or len(data) != row["bytes"]:
            raise ValueError(f"Top-file integrity failure: {row['path']}")
    for row in index["parts"]:
        payload = (bundle / safe_relative(row["path"])).read_bytes()
        if sha(payload) != row["sha256"] or len(payload) != row["bytes"] or len(payload) > PART_BYTES:
            raise ValueError(f"Part integrity failure: {row['path']}")
        count = 0
        with tarfile.open(fileobj=io.BytesIO(payload), mode="r:gz") as archive:
            for member in archive:
                name = safe_relative(member.name)
                if not member.isfile() or name in seen_members or member.size > CHUNK_BYTES:
                    raise ValueError(f"Invalid archive member: {name}")
                seen_members.add(name)
                stream = archive.extractfile(member)
                if stream is None:
                    raise ValueError(f"Unreadable archive member: {name}")
                data = stream.read()
                if len(data) != member.size:
                    raise ValueError(f"Truncated archive member: {name}")
                if name.startswith("objects/"):
                    digest = name.rsplit("/", 1)[-1]
                    if not SHA_RE.fullmatch(digest) or name != f"objects/{digest[:2]}/{digest}" or sha(data) != digest:
                        raise ValueError(f"Object integrity failure: {name}")
                    objects[digest] = data
                elif name.startswith("manifests/"):
                    metadata[name] = data
                else:
                    raise ValueError(f"Unknown archive member: {name}")
                count += 1
        if count != row["members"]:
            raise ValueError(f"Part member-count mismatch: {row['path']}")
    records = []
    expected_metadata = set()
    for row in index["metadataShards"]:
        name = safe_relative(row["member"])
        expected_metadata.add(name)
        data = metadata[name]
        if sha(data) != row["sha256"] or len(data) != row["bytes"]:
            raise ValueError(f"Manifest integrity failure: {name}")
        decoded = [json.loads(line) for line in data.splitlines()]
        if len(decoded) != row["records"]:
            raise ValueError(f"Manifest row-count mismatch: {name}")
        if row["kind"] == "files":
            records.extend(decoded)
    if expected_metadata != set(metadata):
        raise ValueError("Undeclared metadata members")
    names: set[str] = set()
    reconstructed = []
    used_objects: set[str] = set()
    for row in records:
        name = safe_relative(row["path"])
        if name in names:
            raise ValueError(f"Duplicate reconstructed path: {name}")
        names.add(name)
        pieces = []
        for chunk in row["chunks"]:
            data = objects[chunk["sha256"]]
            if len(data) != chunk["bytes"]:
                raise ValueError(f"Chunk byte-count mismatch: {name}")
            used_objects.add(chunk["sha256"])
            pieces.append(data)
        data = b"".join(pieces)
        if sha(data) != row["sha256"] or len(data) != row["bytes"]:
            raise ValueError(f"Reconstructed source integrity failure: {name}")
        reconstructed.append((row, data))
    summary = index["summary"]
    if (len(records) != summary["retainedPaths"] or used_objects != set(objects)
            or len(objects) != summary["uniqueChunks"]
            or sum(map(len, objects.values())) != summary["uniqueChunkBytes"]
            or sum(row["bytes"] for row in records) != summary["retainedPathBytes"]):
        raise ValueError("Bundle retained-content count mismatch")
    if destination is not None:
        if destination.exists():
            raise ValueError("Reconstruction destination must be fresh")
        destination.mkdir(parents=True, exist_ok=False)
        for row, data in reconstructed:
            path = destination / row["path"]
            write_new(path, data)
            path.chmod(row.get("mode", 0o644) & 0o777)
    return {"schema": "synexia.evidence.bundle-integrity/1", "status": "ARCHIVE_INTEGRITY_VERIFIED",
            "bundleIndexSHA256": sha(index_data), "retainedPaths": len(records),
            "uniqueChunks": len(objects), "reconstructed": destination is not None,
            "proofGatesExecuted": False, "boundary": BOUNDARY}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="mode", required=True)
    for mode in ("estimate", "pack"):
        child = sub.add_parser(mode)
        child.add_argument("--root", type=Path, required=True)
        child.add_argument("--plan", type=Path, required=True)
        child.add_argument("--output", type=Path, required=True)
    for mode in ("verify", "extract"):
        child = sub.add_parser(mode)
        child.add_argument("--bundle", type=Path, required=True)
        if mode == "extract":
            child.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.mode in ("verify", "extract"):
        result = verify_bundle(args.bundle.resolve(), args.output.resolve() if args.mode == "extract" else None)
    else:
        root, plan_path, output = args.root.resolve(), args.plan.resolve(), args.output.resolve()
        plan = json.loads(plan_path.read_bytes())
        if plan.get("schema") != PLAN_SCHEMA:
            raise ValueError("Unsupported package plan schema")
        if args.mode == "pack" and plan.get("status") != "FINAL_REVIEWED_SELECTION":
            raise ValueError("Pack requires a new final reviewed lane-selection plan")
        if output.exists():
            raise ValueError("Output must be fresh")
        if output == root or any(under(root / safe_relative(p), output) for p in plan.get("includePaths", [])):
            raise ValueError("Output cannot contain selected input roots")
        inventory = Inventory(root, plan, output, strict=args.mode == "pack")
        inventory.collect()
        if args.mode == "estimate":
            result = estimate(inventory, plan_path)
            write_new(output, encoded(result))
        else:
            result = pack(inventory, plan_path)
            write_new(output / "PACKAGE.json", encoded(result))
    print(json.dumps(result, sort_keys=True) if args.mode in ("verify", "extract") else
          json.dumps({"status": result.get("status", result.get("mode")), "output": str(args.output),
                      "summary": result["summary"], "proofGatesExecuted": False}, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError) as error:
        print(f"EVIDENCE_PACKAGE_REFUSED: {type(error).__name__}: {error}", file=sys.stderr)
        raise SystemExit(2) from error
