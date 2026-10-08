#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Render and verify the M3JDK ``m3lex-family-v2`` precompute bundle.

Synexia remains the authority for source identity and derived values.  This
tool owns only deterministic transport: it converts explicit family rows to
the five TSV sidecars consumed by ``SharedLexiconFamilySidecarCatalog``.  It
never derives an owner from an image coordinate, downloads data, or renumbers
records.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import os
import re
import shutil
import tempfile
from pathlib import Path
from typing import Iterable, Mapping

SCHEMA_VERSION = "m3lex-family-v2"
INDEX_FILE = "synexia.precompute-family-index.tsv"
COMMON = ("source_id", "record_id", "source_manifest_revision", "owner_fingerprint")
FAMILY_SPECS = {
    "prefix-counts": {
        "file": "synexia.prefix-counts.tsv",
        "columns": COMMON + ("shard_id", "image_row", "derivation_version", "value_fingerprint", "token_id", "prefix_counts"),
        "keys": COMMON + ("shard_id", "image_row", "derivation_version", "value_fingerprint", "token_id"),
    },
    "spell-index": {
        "file": "synexia.spell.tsv",
        "columns": COMMON + ("lexicon_fingerprint", "language", "max_edit_distance", "prefix_length", "source_fingerprint", "delete_key", "candidate_token_ids", "frequencies"),
        "keys": COMMON + ("lexicon_fingerprint", "language", "max_edit_distance", "prefix_length", "source_fingerprint", "delete_key"),
    },
    "token-frequency": {
        "file": "synexia.token-frequency.tsv",
        "columns": COMMON + ("shard_id", "image_row", "derivation_version", "value_fingerprint", "frequencies"),
        "keys": COMMON + ("shard_id", "image_row", "derivation_version", "value_fingerprint"),
    },
    "token-hash-precompute": {
        "file": "synexia.token-hash.tsv",
        "columns": COMMON + ("shard_id", "image_row", "value_fingerprint", "tokenizer_version", "range_start", "range_end", "range_fingerprint_first", "range_fingerprint_second", "token_sha256", "range_sha256"),
        "keys": COMMON + ("shard_id", "image_row", "value_fingerprint", "tokenizer_version", "range_start", "range_end"),
    },
    "translation-projection": {
        "file": "synexia.translation.tsv",
        "columns": COMMON + ("source_language", "target_language", "lexicon_fingerprint", "source_fingerprint", "translated_token_ids", "mapped_token_count"),
        "keys": COMMON + ("source_language", "target_language", "lexicon_fingerprint", "source_fingerprint"),
    },
}
INDEX_COLUMNS = ("schema_version", "family", "file", "rows", "sha256")
HEX64 = frozenset("0123456789abcdef")
SAFE_NAME = re.compile(r"^[a-z][a-z0-9_]*$")
DECIMAL = re.compile(r"^-?(0|[1-9][0-9]*)$")


def _text(value: object, name: str, allow_empty: bool = False) -> str:
    if not isinstance(value, str) or (not allow_empty and not value):
        raise ValueError(f"{name} must be a non-empty string")
    if any(character in value for character in "\t\r\n"):
        raise ValueError(f"{name} contains TSV control")
    return value


def _integer(value: object, name: str, minimum: int = 0, maximum: int | None = None) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value < minimum:
        raise ValueError(f"{name} must be an integer >= {minimum}")
    if maximum is not None and value > maximum:
        raise ValueError(f"{name} exceeds {maximum}")
    return value


def _ints(value: object, name: str, sorted_unique: bool = False) -> list[int]:
    if not isinstance(value, (list, tuple)):
        raise ValueError(f"{name} must be an array")
    result = [_integer(item, name) for item in value]
    if sorted_unique and result != sorted(set(result)):
        raise ValueError(f"{name} must be strictly sorted")
    return result


def _prefixes(value: object) -> list[int]:
    result = _ints(value, "prefix_counts")
    if not result or result[0] != 0 or result != sorted(result):
        raise ValueError("prefix_counts must be non-empty, start at zero, and be monotonic")
    return result


def _digest(value: object, name: str) -> str:
    result = _text(value, name)
    if len(result) != 64 or set(result) - HEX64:
        raise ValueError(f"{name} must be lowercase SHA-256 hex")
    return result


def _map(value: object, name: str) -> dict[int, int]:
    if not isinstance(value, Mapping):
        raise ValueError(f"{name} must be an object")
    result: dict[int, int] = {}
    for key, item in value.items():
        if isinstance(key, bool) or not isinstance(key, (int, str)):
            raise ValueError(f"{name} key is not decimal")
        if isinstance(key, str) and DECIMAL.fullmatch(key) is None:
            raise ValueError(f"{name} key is not canonical decimal")
        numeric = int(key)
        result[_integer(numeric, f"{name} key")] = _integer(item, f"{name} value")
    if list(result) != sorted(result):
        raise ValueError(f"{name} keys must be sorted")
    return result


def _scope(raw: Mapping[str, object]) -> dict[str, str]:
    return {column: _text(raw.get(column), column) for column in COMMON}


def _list(value: list[int]) -> str:
    return "-" if not value else ",".join(str(item) for item in value)


def _map_text(value: dict[int, int]) -> str:
    return "-" if not value else ",".join(f"{key}:{value[key]}" for key in sorted(value))


def _normalize(family: str, raw: Mapping[str, object]) -> tuple[tuple[str, ...], dict[str, str]]:
    if family not in FAMILY_SPECS:
        raise ValueError(f"unknown family: {family}")
    scope = _scope(raw)
    if family == "translation-projection":
        ids = _ints(raw.get("translated_token_ids"), "translated_token_ids")
        count = _integer(raw.get("mapped_token_count"), "mapped_token_count")
        if count > len(ids):
            raise ValueError("mapped_token_count exceeds translated_token_ids")
        row = {**scope, "source_language": _text(raw.get("source_language"), "source_language"),
               "target_language": _text(raw.get("target_language"), "target_language"),
               "lexicon_fingerprint": _text(raw.get("lexicon_fingerprint"), "lexicon_fingerprint"),
               "source_fingerprint": _text(raw.get("source_fingerprint"), "source_fingerprint"),
               "translated_token_ids": _list(ids), "mapped_token_count": str(count)}
    elif family == "spell-index":
        row = {**scope, "lexicon_fingerprint": _text(raw.get("lexicon_fingerprint"), "lexicon_fingerprint"),
               "language": _text(raw.get("language"), "language"),
               "max_edit_distance": str(_integer(raw.get("max_edit_distance"), "max_edit_distance")),
               "prefix_length": str(_integer(raw.get("prefix_length"), "prefix_length")),
               "source_fingerprint": _text(raw.get("source_fingerprint"), "source_fingerprint"),
               "delete_key": _text(raw.get("delete_key"), "delete_key", True),
               "candidate_token_ids": _list(_ints(raw.get("candidate_token_ids"), "candidate_token_ids", True)),
               "frequencies": _map_text(_map(raw.get("frequencies"), "frequencies"))}
    elif family == "token-hash-precompute":
        start = _integer(raw.get("range_start"), "range_start")
        end = _integer(raw.get("range_end"), "range_end")
        if end < start:
            raise ValueError("range_end precedes range_start")
        digests = [_digest(item, "token_sha256") for item in raw.get("token_sha256", [])]
        if len(digests) != end - start:
            raise ValueError("token_sha256 count does not match half-open range")
        row = {**scope, "shard_id": str(_integer(raw.get("shard_id"), "shard_id")),
               "image_row": str(_integer(raw.get("image_row"), "image_row")),
               "value_fingerprint": _text(raw.get("value_fingerprint"), "value_fingerprint"),
               "tokenizer_version": _text(raw.get("tokenizer_version"), "tokenizer_version"),
               "range_start": str(start), "range_end": str(end),
               "range_fingerprint_first": str(_integer(raw.get("range_fingerprint_first"), "range_fingerprint_first", -2**63, 2**63 - 1)),
               "range_fingerprint_second": str(_integer(raw.get("range_fingerprint_second"), "range_fingerprint_second", -2**63, 2**63 - 1)),
               "token_sha256": "-" if not digests else ";".join(digests),
               "range_sha256": _digest(raw.get("range_sha256"), "range_sha256")}
    elif family == "prefix-counts":
        row = {**scope, "shard_id": str(_integer(raw.get("shard_id"), "shard_id")),
               "image_row": str(_integer(raw.get("image_row"), "image_row")),
               "derivation_version": _text(raw.get("derivation_version"), "derivation_version"),
               "value_fingerprint": _text(raw.get("value_fingerprint"), "value_fingerprint"),
               "token_id": str(_integer(raw.get("token_id"), "token_id")),
               "prefix_counts": _list(_prefixes(raw.get("prefix_counts")))}
    else:
        row = {**scope, "shard_id": str(_integer(raw.get("shard_id"), "shard_id")),
               "image_row": str(_integer(raw.get("image_row"), "image_row")),
               "derivation_version": _text(raw.get("derivation_version"), "derivation_version"),
               "value_fingerprint": _text(raw.get("value_fingerprint"), "value_fingerprint"),
               "frequencies": _map_text(_map(raw.get("frequencies"), "frequencies"))}
    return tuple(row[column] for column in FAMILY_SPECS[family]["keys"]), row


def _csv_bytes(columns: tuple[str, ...], rows: Iterable[Mapping[str, str]]) -> bytes:
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=columns, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    for row in rows:
        writer.writerow({column: _text(row[column], column, True) for column in columns})
    return stream.getvalue().encode("utf-8")


def render_bundle(families: Mapping[str, Iterable[Mapping[str, object]]]) -> dict[str, bytes]:
    if set(families) != set(FAMILY_SPECS):
        raise ValueError("family coverage mismatch")
    result: dict[str, bytes] = {}
    index: list[dict[str, str]] = []
    for family in sorted(FAMILY_SPECS):
        normalized = [_normalize(family, row) for row in families[family]]
        keys = [item[0] for item in normalized]
        if keys != sorted(keys) or len(keys) != len(set(keys)):
            raise ValueError(f"{family} rows must be sorted and unique")
        spec = FAMILY_SPECS[family]
        content = _csv_bytes(spec["columns"], (item[1] for item in normalized))
        result[spec["file"]] = content
        index.append({"schema_version": SCHEMA_VERSION, "family": family,
                      "file": spec["file"], "rows": str(len(normalized)),
                      "sha256": hashlib.sha256(content).hexdigest()})
    result[INDEX_FILE] = _csv_bytes(INDEX_COLUMNS, index)
    return result


def _serialized_rows(family: str, content: bytes) -> list[tuple[tuple[str, ...], dict[str, object]]]:
    try:
        text = content.decode("utf-8")
    except UnicodeDecodeError as error:
        raise ValueError(f"{family} is not UTF-8") from error
    reader = csv.DictReader(io.StringIO(text), delimiter="\t")
    expected = FAMILY_SPECS[family]["columns"]
    if tuple(reader.fieldnames or ()) != expected:
        raise ValueError(f"{family} header mismatch")
    parsed: list[tuple[tuple[str, ...], dict[str, object]]] = []
    for row in reader:
        if None in row or any(value is None for value in row.values()):
            raise ValueError(f"{family} field count mismatch")
        value: dict[str, object] = dict(row)
        for column in COMMON:
            _text(value[column], column)
        if family == "translation-projection":
            value["translated_token_ids"] = [] if value["translated_token_ids"] == "-" else [int(item) for item in value["translated_token_ids"].split(",")]
            value["mapped_token_count"] = int(value["mapped_token_count"])
        elif family == "spell-index":
            for field in ("max_edit_distance", "prefix_length"):
                value[field] = int(value[field])
            value["candidate_token_ids"] = [] if value["candidate_token_ids"] == "-" else [int(item) for item in value["candidate_token_ids"].split(",")]
            value["frequencies"] = {} if value["frequencies"] == "-" else {int(item.split(":", 1)[0]): int(item.split(":", 1)[1]) for item in value["frequencies"].split(",")}
        elif family == "token-hash-precompute":
            for field in ("shard_id", "image_row", "range_start", "range_end", "range_fingerprint_first", "range_fingerprint_second"):
                value[field] = int(value[field])
            value["token_sha256"] = [] if value["token_sha256"] == "-" else value["token_sha256"].split(";")
        elif family == "prefix-counts":
            for field in ("shard_id", "image_row", "token_id"):
                value[field] = int(value[field])
            value["prefix_counts"] = [int(item) for item in value["prefix_counts"].split(",")]
        else:
            for field in ("shard_id", "image_row"):
                value[field] = int(value[field])
            value["frequencies"] = {} if value["frequencies"] == "-" else {int(item.split(":", 1)[0]): int(item.split(":", 1)[1]) for item in value["frequencies"].split(",")}
        parsed.append(_normalize(family, value))
    return parsed


def verify_bundle(files: Mapping[str, bytes]) -> dict[str, int]:
    expected_files = {INDEX_FILE} | {spec["file"] for spec in FAMILY_SPECS.values()}
    if set(files) != expected_files:
        raise ValueError("family sidecar file coverage mismatch")
    index_reader = csv.DictReader(io.StringIO(files[INDEX_FILE].decode("utf-8")), delimiter="\t")
    if tuple(index_reader.fieldnames or ()) != INDEX_COLUMNS:
        raise ValueError("family index header mismatch")
    indexes = list(index_reader)
    if [row.get("family") for row in indexes] != sorted(FAMILY_SPECS):
        raise ValueError("family index ordering or coverage mismatch")
    if any(row.get("schema_version") != SCHEMA_VERSION
           or row.get("file") != FAMILY_SPECS[row["family"]]["file"]
           for row in indexes):
        raise ValueError("family index schema or file mapping mismatch")
    total = 0
    for index in indexes:
        family = index["family"]
        filename = index["file"]
        content = files[filename]
        if hashlib.sha256(content).hexdigest() != index["sha256"]:
            raise ValueError(f"family checksum mismatch: {family}")
        rows = _serialized_rows(family, content)
        if len(rows) != int(index["rows"]):
            raise ValueError(f"family row count mismatch: {family}")
        keys = [item[0] for item in rows]
        if keys != sorted(keys) or len(keys) != len(set(keys)):
            raise ValueError(f"family rows are not sorted and unique: {family}")
        total += len(rows)
    return {"families": len(indexes), "rows": total}


def write_bundle(output: Path, files: Mapping[str, bytes]) -> None:
    if output.exists():
        if not output.is_dir() or any(output.iterdir()):
            raise FileExistsError(f"output directory is not empty: {output}")
    else:
        output.parent.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix=output.name + ".", dir=output.parent))
    try:
        for name, content in files.items():
            (temporary / name).write_bytes(content)
        verify_bundle({name: (temporary / name).read_bytes() for name in files})
        if output.exists():
            output.rmdir()
        os.replace(temporary, output)
    except BaseException:
        shutil.rmtree(temporary, ignore_errors=True)
        raise


def _self_test() -> int:
    digest = "0123456789abcdef" * 4
    scope = {"source_id": "dictlang.dictionary", "record_id": "r-1",
             "source_manifest_revision": "manifest-1", "owner_fingerprint": "owner-1"}
    families = {
        "prefix-counts": [{**scope, "shard_id": 0, "image_row": 0, "derivation_version": "v1", "value_fingerprint": "value-1", "token_id": 7, "prefix_counts": [0, 1, 1, 2]}],
        "spell-index": [{**scope, "lexicon_fingerprint": "lex-1", "language": "en", "max_edit_distance": 2, "prefix_length": 4, "source_fingerprint": "spell-1", "delete_key": "helo", "candidate_token_ids": [2, 7], "frequencies": {2: 4, 7: 1}}],
        "token-frequency": [{**scope, "shard_id": 0, "image_row": 0, "derivation_version": "v1", "value_fingerprint": "value-1", "frequencies": {1: 2, 2: 1}}],
        "token-hash-precompute": [{**scope, "shard_id": 0, "image_row": 0, "value_fingerprint": "value-1", "tokenizer_version": "tok-1", "range_start": 0, "range_end": 2, "range_fingerprint_first": 7, "range_fingerprint_second": 13, "token_sha256": [digest, digest], "range_sha256": digest}],
        "translation-projection": [{**scope, "source_language": "en", "target_language": "hi", "lexicon_fingerprint": "lex-1", "source_fingerprint": "src-1", "translated_token_ids": [1, 2, 3], "mapped_token_count": 2}],
    }
    first = render_bundle(families)
    if first != render_bundle(families) or verify_bundle(first) != {"families": 5, "rows": 5}:
        raise AssertionError("sidecar self-test drift")
    broken = dict(first)
    broken["synexia.spell.tsv"] += b"tampered"
    try:
        verify_bundle(broken)
    except ValueError:
        print("M3JDK_SIDECAR_PASS checks=3 families=5 rows=5")
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
    if args.command == "render":
        if args.input is None or args.output is None:
            parser.error("render requires --input and --output")
        document = json.loads(args.input.read_text(encoding="utf-8"))
        files = render_bundle(document["families"])
        write_bundle(args.output, files)
        print("M3JDK_SIDECAR_RENDER_PASS families=5")
        return 0
    if args.output is None:
        parser.error("verify requires --output as a bundle directory")
    files = {path.name: path.read_bytes() for path in args.output.iterdir() if path.is_file()}
    stats = verify_bundle(files)
    print("M3JDK_SIDECAR_VERIFY_PASS " + json.dumps(stats, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
