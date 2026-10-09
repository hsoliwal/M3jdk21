#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Deterministic, provenance-scoped acronym string sidecar for M3JDK21.

This protocol is intentionally separate from m3lex-family-v2: generic
precompute_payload remains numeric/boolean-only.  Acronym/expansion/domain
text is carried here with an explicit source identity and owner scope.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import re
from pathlib import Path
from typing import Iterable, Mapping

SCHEMA_VERSION = "m3lex-acronym-v1"
SOURCE_ID = "dictlang.acronyms"
DATA_FILE = "synexia.acronyms.tsv"
INDEX_FILE = "synexia.acronyms.index.tsv"
COLUMNS = (
    "source_id",
    "record_id",
    "source_manifest_revision",
    "owner_fingerprint",
    "acronym",
    "expansion",
    "domain",
)
INDEX_COLUMNS = ("schema_version", "file", "rows", "sha256")
ACRONYM = re.compile(r"^[A-Za-z][A-Za-z0-9+.-]{0,31}$")
DOMAIN = re.compile(r"^[a-z][a-z0-9._-]*$")
HEX64 = re.compile(r"^[0-9a-f]{64}$")


def _text(value: object, name: str, *, allow_empty: bool = False) -> str:
    if not isinstance(value, str) or (not allow_empty and not value):
        raise ValueError(f"{name} must be a non-empty string")
    if any(char in value for char in "\t\r\n"):
        raise ValueError(f"{name} contains TSV control")
    return value


def _digest(value: object, name: str) -> str:
    result = _text(value, name)
    if HEX64.fullmatch(result) is None:
        raise ValueError(f"{name} must be lowercase SHA-256 hex")
    return result


def normalize(row: Mapping[str, object]) -> tuple[str, ...]:
    source_id = _text(row.get("source_id"), "source_id")
    if source_id != SOURCE_ID:
        raise ValueError("acronym sidecar source_id mismatch")
    acronym = _text(row.get("acronym"), "acronym")
    if ACRONYM.fullmatch(acronym) is None:
        raise ValueError("invalid acronym")
    if row.get("record_id") != acronym:
        raise ValueError("record_id must equal acronym")
    expansion = _text(row.get("expansion"), "expansion")
    if not expansion.strip():
        raise ValueError("expansion must be non-blank")
    domain = _text(row.get("domain"), "domain")
    if DOMAIN.fullmatch(domain) is None:
        raise ValueError("invalid domain")
    return (
        source_id,
        acronym,
        _text(row.get("source_manifest_revision"), "source_manifest_revision"),
        _digest(row.get("owner_fingerprint"), "owner_fingerprint"),
        acronym,
        expansion,
        domain,
    )


def _tsv(columns: tuple[str, ...], rows: Iterable[tuple[str, ...]]) -> bytes:
    stream = io.StringIO(newline="")
    writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
    writer.writerow(columns)
    writer.writerows(rows)
    return stream.getvalue().encode("utf-8")


def render(rows: Iterable[Mapping[str, object]]) -> dict[str, bytes]:
    normalized = [normalize(row) for row in rows]
    if not normalized:
        raise ValueError("acronym sidecar requires at least one row")
    keys = [(row[0], row[1]) for row in normalized]
    if len({(row[2], row[3]) for row in normalized}) != 1:
        raise ValueError("acronym rows must share one scope")
    if keys != sorted(keys) or len(keys) != len(set(keys)):
        raise ValueError("acronym rows must be sorted and unique")
    content = _tsv(COLUMNS, normalized)
    index = _tsv(
        INDEX_COLUMNS,
        [(SCHEMA_VERSION, DATA_FILE, str(len(normalized)), hashlib.sha256(content).hexdigest())],
    )
    return {DATA_FILE: content, INDEX_FILE: index}


def _read_data(content: bytes) -> list[tuple[str, ...]]:
    try:
        text = content.decode("utf-8")
    except UnicodeDecodeError as error:
        raise ValueError("acronym sidecar is not UTF-8") from error
    reader = csv.reader(io.StringIO(text), delimiter="\t")
    if tuple(next(reader, ())) != COLUMNS:
        raise ValueError("acronym sidecar header mismatch")
    rows = [tuple(row) for row in reader]
    if not rows:
        raise ValueError("acronym sidecar requires at least one row")
    if any(len(row) != len(COLUMNS) for row in rows):
        raise ValueError("acronym sidecar field count mismatch")
    checked = [
        normalize(dict(zip(COLUMNS, row, strict=True)))
        for row in rows
    ]
    if "\r" in text or not text.endswith("\n"):
        raise ValueError("acronym sidecar must use LF and terminate with LF")
    keys = [(row[0], row[1]) for row in checked]
    if keys != sorted(keys) or len(keys) != len(set(keys)):
        raise ValueError("acronym sidecar rows must be sorted and unique")
    if len({(row[2], row[3]) for row in checked}) != 1:
        raise ValueError("acronym sidecar scope drift")
    if checked != rows:
        raise ValueError("acronym sidecar canonical row mismatch")
    return rows


def verify(files: Mapping[str, bytes]) -> dict[str, int]:
    if set(files) != {DATA_FILE, INDEX_FILE}:
        raise ValueError("acronym sidecar file coverage mismatch")
    index_reader = csv.DictReader(
        io.StringIO(files[INDEX_FILE].decode("utf-8")), delimiter="\t"
    )
    if tuple(index_reader.fieldnames or ()) != INDEX_COLUMNS:
        raise ValueError("acronym index header mismatch")
    index = list(index_reader)
    if len(index) != 1:
        raise ValueError("acronym index must contain one row")
    row = index[0]
    if row["schema_version"] != SCHEMA_VERSION or row["file"] != DATA_FILE:
        raise ValueError("acronym index schema mismatch")
    content = files[DATA_FILE]
    if _digest(row["sha256"], "sha256") != hashlib.sha256(content).hexdigest():
        raise ValueError("acronym sidecar checksum mismatch")
    data = _read_data(content)
    if int(row["rows"]) != len(data):
        raise ValueError("acronym sidecar row-count mismatch")
    return {"rows": len(data), "files": 2}


def write_bundle(output: Path, files: Mapping[str, bytes]) -> None:
    output.mkdir(parents=True, exist_ok=False)
    for name, content in files.items():
        (output / name).write_bytes(content)
    verify({name: (output / name).read_bytes() for name in files})


def _self_test() -> int:
    owner = "0123456789abcdef" * 4
    rows = [
        {
            "source_id": SOURCE_ID,
            "record_id": "API",
            "source_manifest_revision": "synexia-acronym-1",
            "owner_fingerprint": owner,
            "acronym": "API",
            "expansion": "application programming interface",
            "domain": "computing",
        },
        {
            "source_id": SOURCE_ID,
            "record_id": "HTTP",
            "source_manifest_revision": "synexia-acronym-1",
            "owner_fingerprint": owner,
            "acronym": "HTTP",
            "expansion": "hypertext transfer protocol",
            "domain": "networking",
        },
    ]
    first = render(rows)
    if first != render(rows) or verify(first) != {"rows": 2, "files": 2}:
        raise AssertionError("acronym sidecar is not deterministic")
    broken = dict(first)
    broken[DATA_FILE] += b"tamper"
    try:
        verify(broken)
    except ValueError:
        print("M3JDK_ACRONYM_SIDECAR_PASS rows=2 files=2 checks=3")
        return 0
    raise AssertionError("checksum tamper was accepted")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("render", "verify", "self-test"))
    parser.add_argument("--input", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args(argv)
    if args.command == "self-test":
        return _self_test()
    if args.output is None:
        parser.error("the command requires --output")
    if args.command == "render":
        if args.input is None:
            parser.error("render requires --input")
        document = json.loads(args.input.read_text(encoding="utf-8"))
        files = render(document["rows"])
        write_bundle(args.output, files)
        print("M3JDK_ACRONYM_SIDECAR_RENDER_PASS rows=" + str(len(document["rows"])))
        return 0
    files = {path.name: path.read_bytes() for path in args.output.iterdir() if path.is_file()}
    print("M3JDK_ACRONYM_SIDECAR_VERIFY_PASS " + json.dumps(verify(files), sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
