# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0

#!/usr/bin/env python3
"""Deterministic, typed M3JDK family-sidecar contract.

The existing ``synexia.records.tsv:precompute_payload`` remains opaque.  This
module defines the separate index-shaped sidecars needed by translation,
spelling, token-range hashes, prefix counts, and token frequencies.
"""

from __future__ import annotations

import csv
import hashlib
import io
from collections.abc import Iterable, Mapping


SCHEMA_VERSION = "m3lex-family-v2"
INDEX_FILE = "synexia.precompute-family-index.tsv"
INDEX_COLUMNS = ("schema_version", "family", "file", "rows", "sha256")
COMMON_SCOPE_COLUMNS = (
    "source_id", "record_id", "source_manifest_revision", "owner_fingerprint",
)
LEGACY_FIXED_FILES = frozenset({
    "synexia.export.json", "synexia.shards.tsv", "synexia.records.tsv",
    "synexia.precompute-index.tsv", "synexia.precompute.tsv",
})
FAMILY_SPECS = {
    "prefix-counts": {
        "file": "synexia.prefix-counts.tsv",
        "columns": COMMON_SCOPE_COLUMNS + (
            "shard_id", "image_row", "derivation_version", "value_fingerprint",
            "token_id", "prefix_counts",
        ),
        "key_columns": COMMON_SCOPE_COLUMNS + (
            "shard_id", "image_row", "derivation_version", "value_fingerprint", "token_id",
        ),
    },
    "spell-index": {
        "file": "synexia.spell.tsv",
        "columns": COMMON_SCOPE_COLUMNS + (
            "lexicon_fingerprint", "language", "max_edit_distance", "prefix_length",
            "source_fingerprint", "delete_key", "candidate_token_ids", "frequencies",
        ),
        "key_columns": COMMON_SCOPE_COLUMNS + (
            "lexicon_fingerprint", "language", "max_edit_distance", "prefix_length",
            "source_fingerprint", "delete_key",
        ),
    },
    "token-frequency": {
        "file": "synexia.token-frequency.tsv",
        "columns": COMMON_SCOPE_COLUMNS + (
            "shard_id", "image_row", "derivation_version", "value_fingerprint", "frequencies",
        ),
        "key_columns": COMMON_SCOPE_COLUMNS + (
            "shard_id", "image_row", "derivation_version", "value_fingerprint",
        ),
    },
    "token-hash-precompute": {
        "file": "synexia.token-hash.tsv",
        "columns": COMMON_SCOPE_COLUMNS + (
            "shard_id", "image_row", "value_fingerprint", "tokenizer_version",
            "range_start", "range_end", "range_fingerprint_first",
            "range_fingerprint_second", "token_sha256", "range_sha256",
        ),
        "key_columns": COMMON_SCOPE_COLUMNS + (
            "shard_id", "image_row", "value_fingerprint", "tokenizer_version",
            "range_start", "range_end",
        ),
    },
    "translation-projection": {
        "file": "synexia.translation.tsv",
        "columns": COMMON_SCOPE_COLUMNS + (
            "source_language", "target_language",
            "lexicon_fingerprint", "source_fingerprint", "translated_token_ids",
            "mapped_token_count",
        ),
        "key_columns": COMMON_SCOPE_COLUMNS + (
            "source_language", "target_language", "lexicon_fingerprint", "source_fingerprint",
        ),
    },
}
HEX64 = set("0123456789abcdef")


def _text(value: object, name: str, allow_empty: bool = False) -> str:
    if not isinstance(value, str) or (not allow_empty and not value):
        raise ValueError(f"{name} must be a non-empty string")
    if "\t" in value or "\r" in value or "\n" in value:
        raise ValueError(f"{name} contains a TSV control")
    return value


def _integer(value: object, name: str, minimum: int = 0) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value < minimum:
        raise ValueError(f"{name} must be an integer >= {minimum}")
    return value


def _ints(values: object, name: str, strictly_sorted: bool = False) -> list[int]:
    if not isinstance(values, (list, tuple)):
        raise ValueError(f"{name} must be an array")
    result = [_integer(value, name) for value in values]
    if strictly_sorted and result != sorted(set(result)):
        raise ValueError(f"{name} must be strictly sorted")
    return result


def _longs(values: object, name: str) -> list[int]:
    result = _ints(values, name)
    if not result or result[0] != 0:
        raise ValueError(f"{name} must start with zero")
    if result != sorted(result):
        raise ValueError(f"{name} must be monotonic")
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
        numeric = _integer(key, f"{name} key")
        result[numeric] = _integer(item, f"{name} value")
    if list(result) != sorted(result):
        raise ValueError(f"{name} keys must be sorted")
    return result


def _csv_bytes(columns: tuple[str, ...], rows: Iterable[dict[str, str]]) -> bytes:
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=columns, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    for row in rows:
        writer.writerow({column: _text(row[column], column, allow_empty=True) for column in columns})
    return stream.getvalue().encode("utf-8")


def _encode_list(values: list[int]) -> str:
    return "-" if not values else ",".join(str(value) for value in values)


def _encode_map(values: dict[int, int]) -> str:
    return "-" if not values else ",".join(f"{key}:{values[key]}" for key in sorted(values))


def _scope(raw: Mapping[str, object]) -> dict[str, str]:
    return {column: _text(raw.get(column), column) for column in COMMON_SCOPE_COLUMNS}


def _key(family: str, row: Mapping[str, str]) -> tuple[str, ...]:
    return tuple(row[column] for column in FAMILY_SPECS[family]["key_columns"])


def _normalize_row(family: str, raw: Mapping[str, object]) -> tuple[tuple[object, ...], dict[str, str]]:
    if family == "translation-projection":
        ids = _ints(raw.get("translated_token_ids"), "translated_token_ids")
        count = _integer(raw.get("mapped_token_count"), "mapped_token_count")
        if count > len(ids):
            raise ValueError("mapped_token_count exceeds translated_token_ids")
        row = {
            **_scope(raw),
            "source_language": _text(raw.get("source_language"), "source_language"),
            "target_language": _text(raw.get("target_language"), "target_language"),
            "lexicon_fingerprint": _text(raw.get("lexicon_fingerprint"), "lexicon_fingerprint"),
            "source_fingerprint": _text(raw.get("source_fingerprint"), "source_fingerprint"),
            "translated_token_ids": _encode_list(ids),
            "mapped_token_count": str(count),
        }
        return _key(family, row), row

    if family == "spell-index":
        ids = _ints(raw.get("candidate_token_ids"), "candidate_token_ids", strictly_sorted=True)
        frequencies = _map(raw.get("frequencies"), "frequencies")
        row = {
            **_scope(raw),
            "lexicon_fingerprint": _text(raw.get("lexicon_fingerprint"), "lexicon_fingerprint"),
            "language": _text(raw.get("language"), "language"),
            "max_edit_distance": str(_integer(raw.get("max_edit_distance"), "max_edit_distance")),
            "prefix_length": str(_integer(raw.get("prefix_length"), "prefix_length")),
            "source_fingerprint": _text(raw.get("source_fingerprint"), "source_fingerprint"),
            "delete_key": _text(raw.get("delete_key"), "delete_key", allow_empty=True),
            "candidate_token_ids": _encode_list(ids),
            "frequencies": _encode_map(frequencies),
        }
        return _key(family, row), row

    if family == "token-hash-precompute":
        start = _integer(raw.get("range_start"), "range_start")
        end = _integer(raw.get("range_end"), "range_end")
        if end < start:
            raise ValueError("range_end precedes range_start")
        digests = [_digest(value, "token_sha256") for value in raw.get("token_sha256", [])]
        if len(digests) != end - start:
            raise ValueError("token_sha256 count does not match half-open range")
        row = {
            **_scope(raw),
            "shard_id": str(_integer(raw.get("shard_id"), "shard_id")),
            "image_row": str(_integer(raw.get("image_row"), "image_row")),
            "value_fingerprint": _text(raw.get("value_fingerprint"), "value_fingerprint"),
            "tokenizer_version": _text(raw.get("tokenizer_version"), "tokenizer_version"),
            "range_start": str(start),
            "range_end": str(end),
            "range_fingerprint_first": str(raw.get("range_fingerprint_first")),
            "range_fingerprint_second": str(raw.get("range_fingerprint_second")),
            "token_sha256": "-" if not digests else ";".join(digests),
            "range_sha256": _digest(raw.get("range_sha256"), "range_sha256"),
        }
        _integer(raw.get("range_fingerprint_first"), "range_fingerprint_first", minimum=-2**63)
        _integer(raw.get("range_fingerprint_second"), "range_fingerprint_second", minimum=-2**63)
        return _key(family, row), row

    if family == "prefix-counts":
        counts = _longs(raw.get("prefix_counts"), "prefix_counts")
        row = {
            **_scope(raw),
            "shard_id": str(_integer(raw.get("shard_id"), "shard_id")),
            "image_row": str(_integer(raw.get("image_row"), "image_row")),
            "derivation_version": _text(raw.get("derivation_version"), "derivation_version"),
            "value_fingerprint": _text(raw.get("value_fingerprint"), "value_fingerprint"),
            "token_id": str(_integer(raw.get("token_id"), "token_id")),
            "prefix_counts": _encode_list(counts),
        }
        return _key(family, row), row

    if family == "token-frequency":
        frequencies = _map(raw.get("frequencies"), "frequencies")
        row = {
            **_scope(raw),
            "shard_id": str(_integer(raw.get("shard_id"), "shard_id")),
            "image_row": str(_integer(raw.get("image_row"), "image_row")),
            "derivation_version": _text(raw.get("derivation_version"), "derivation_version"),
            "value_fingerprint": _text(raw.get("value_fingerprint"), "value_fingerprint"),
            "frequencies": _encode_map(frequencies),
        }
        return _key(family, row), row

    raise ValueError(f"unknown family: {family}")


def render_bundle(families: Mapping[str, Iterable[Mapping[str, object]]]) -> dict[str, bytes]:
    if set(families) != set(FAMILY_SPECS):
        raise ValueError("family coverage mismatch")
    files: dict[str, bytes] = {}
    index_rows = []
    for family in sorted(FAMILY_SPECS):
        keys_and_rows = [_normalize_row(family, row) for row in families[family]]
        keys = [item[0] for item in keys_and_rows]
        if keys != sorted(keys) or len(keys) != len(set(keys)):
            raise ValueError(f"{family} rows must be sorted and unique")
        spec = FAMILY_SPECS[family]
        content = _csv_bytes(spec["columns"], (item[1] for item in keys_and_rows))
        file_name = spec["file"]
        files[file_name] = content
        index_rows.append({
            "schema_version": SCHEMA_VERSION,
            "family": family,
            "file": file_name,
            "rows": str(len(keys_and_rows)),
            "sha256": hashlib.sha256(content).hexdigest(),
        })
    files[INDEX_FILE] = _csv_bytes(INDEX_COLUMNS, index_rows)
    return files


def verify_bundle(files: Mapping[str, bytes]) -> dict[str, int]:
    if INDEX_FILE not in files:
        raise ValueError("family index is missing")
    for name, content in files.items():
        if content.startswith(b"\xef\xbb\xbf") or b"\r" in content:
            raise ValueError(f"{name} must be UTF-8 without BOM and use LF")
    index_reader = csv.DictReader(io.StringIO(files[INDEX_FILE].decode("utf-8")), delimiter="\t")
    if tuple(index_reader.fieldnames or ()) != INDEX_COLUMNS:
        raise ValueError("family index header mismatch")
    index_rows = list(index_reader)
    if set(files) != {INDEX_FILE, *(spec["file"] for spec in FAMILY_SPECS.values())}:
        raise ValueError("family sidecar file coverage mismatch")
    if any(row.get("schema_version") != SCHEMA_VERSION for row in index_rows):
        raise ValueError("family index schema version mismatch")
    if [row.get("family") for row in index_rows] != sorted(FAMILY_SPECS):
        raise ValueError("family index ordering or coverage mismatch")
    total = 0
    for index in index_rows:
        family = index["family"]
        file_name = index["file"]
        content = files.get(file_name)
        if content is None or hashlib.sha256(content).hexdigest() != index["sha256"]:
            raise ValueError(f"family sidecar checksum mismatch: {family}")
        spec = FAMILY_SPECS[family]
        reader = csv.DictReader(io.StringIO(content.decode("utf-8")), delimiter="\t")
        if tuple(reader.fieldnames or ()) != spec["columns"]:
            raise ValueError(f"{family} header mismatch")
        rows = list(reader)
        if len(rows) != int(index["rows"]):
            raise ValueError(f"{family} row count mismatch")
        raw_rows = []
        for row in rows:
            for column in spec["columns"]:
                _text(row.get(column), column, allow_empty=True)
            raw_rows.append(row)
        # Re-normalization validates type, bounds, digest encoding, and key order.
        normalized = [_normalize_serialized_row(family, row) for row in raw_rows]
        keys = [item[0] for item in normalized]
        if keys != sorted(keys) or len(keys) != len(set(keys)):
            raise ValueError(f"{family} serialized rows are not sorted and unique")
        total += len(rows)
    return {"families": len(index_rows), "rows": total}


def verify_optional_bundle(files: Mapping[str, bytes]) -> dict[str, int | bool]:
    """Preserve old exports while rejecting partial family publication."""

    family_files = {spec["file"] for spec in FAMILY_SPECS.values()}
    unknown = {
        name for name in files
        if name not in LEGACY_FIXED_FILES
        and not name.endswith(".m3lex")
        and name not in ({INDEX_FILE} | family_files)
    }
    if unknown:
        raise ValueError(f"unknown sidecar or legacy file: {sorted(unknown)}")
    present = set(files) & ({INDEX_FILE} | family_files)
    if not present:
        return {"legacy": True, "families": 0, "rows": 0}
    if INDEX_FILE not in files or not family_files.issubset(files):
        raise ValueError("family sidecars must be published as one complete bundle")
    result = verify_bundle(files)
    result["legacy"] = False
    return result


def _parse_int_list(value: str, name: str) -> list[int]:
    if value == "-":
        return []
    return [_integer(int(item), name) for item in value.split(",")]


def _parse_map(value: str, name: str) -> dict[int, int]:
    if value == "-":
        return {}
    result = {}
    for item in value.split(","):
        key, count = item.split(":", 1)
        result[int(key)] = int(count)
    return result


def _normalize_serialized_row(family: str, row: Mapping[str, str]) -> tuple[tuple[object, ...], dict[str, str]]:
    value = dict(row)
    if family == "translation-projection":
        value["translated_token_ids"] = _parse_int_list(value["translated_token_ids"], "translated_token_ids")
        value["mapped_token_count"] = int(value["mapped_token_count"])
    elif family == "spell-index":
        value["max_edit_distance"] = int(value["max_edit_distance"])
        value["prefix_length"] = int(value["prefix_length"])
        value["candidate_token_ids"] = _parse_int_list(value["candidate_token_ids"], "candidate_token_ids")
        value["frequencies"] = _parse_map(value["frequencies"], "frequencies")
    elif family == "token-hash-precompute":
        for field in ("shard_id", "image_row", "range_start", "range_end",
                      "range_fingerprint_first", "range_fingerprint_second"):
            value[field] = int(value[field])
        value["token_sha256"] = [] if value["token_sha256"] == "-" else value["token_sha256"].split(";")
    elif family == "prefix-counts":
        for field in ("shard_id", "image_row", "token_id"):
            value[field] = int(value[field])
        value["prefix_counts"] = _parse_int_list(value["prefix_counts"], "prefix_counts")
    elif family == "token-frequency":
        value["shard_id"] = int(value["shard_id"])
        value["image_row"] = int(value["image_row"])
        value["frequencies"] = _parse_map(value["frequencies"], "frequencies")
    return _normalize_row(family, value)


def self_test() -> int:
    digest = "0123456789abcdef" * 4
    scope = {
        "source_id": "lexicon.records", "record_id": "row-1",
        "source_manifest_revision": "manifest-r1", "owner_fingerprint": "owner-f1",
    }
    families = {
        "translation-projection": [{
            **scope, "source_language": "en",
            "target_language": "hi", "lexicon_fingerprint": "lex-1", "source_fingerprint": "src-1",
            "translated_token_ids": [11, 0, 19], "mapped_token_count": 2,
        }],
        "spell-index": [{
            **scope,
            "lexicon_fingerprint": "lex-1", "language": "en", "max_edit_distance": 2,
            "prefix_length": 7, "source_fingerprint": "spell-1", "delete_key": "helo",
            "candidate_token_ids": [2, 7], "frequencies": {2: 40, 7: 3},
        }],
        "token-hash-precompute": [{
            **scope,
            "shard_id": 2, "image_row": 7, "value_fingerprint": "value-1",
            "tokenizer_version": "tok-v1", "range_start": 0, "range_end": 3,
            "range_fingerprint_first": 7, "range_fingerprint_second": 13,
            "token_sha256": [digest, digest, digest], "range_sha256": digest,
        }],
        "prefix-counts": [{
            **scope,
            "shard_id": 2, "image_row": 7, "value_fingerprint": "value-1",
            "derivation_version": "tok-v1", "token_id": 7,
            "prefix_counts": [0, 1, 1, 2, 3],
        }],
        "token-frequency": [{
            **scope,
            "shard_id": 2, "image_row": 7, "value_fingerprint": "value-1",
            "derivation_version": "tok-v1", "frequencies": {1: 2, 2: 2, 3: 1},
        }],
    }
    first = render_bundle(families)
    second = render_bundle(families)
    if first != second:
        raise AssertionError("sidecar rendering is not deterministic")
    stats = verify_optional_bundle(first)
    checks = int(stats["families"]) + int(stats["rows"])
    legacy = verify_optional_bundle({"synexia.shards.tsv": b"legacy\n"})
    if legacy != {"legacy": True, "families": 0, "rows": 0}:
        raise AssertionError("legacy export compatibility changed")
    checks += 1
    try:
        verify_optional_bundle({
            "synexia.records.tsv": b"legacy\n",
            "synexia.unknown.tsv": b"unexpected\n",
        })
    except ValueError:
        checks += 1
    else:
        raise AssertionError("unknown legacy/sidecar file was accepted")
    missing_scope = {key: list(value) for key, value in families.items()}
    missing_scope["token-frequency"] = [{
        key: value for key, value in families["token-frequency"][0].items()
        if key != "owner_fingerprint"
    }]
    try:
        render_bundle(missing_scope)
    except ValueError:
        checks += 1
    else:
        raise AssertionError("missing owner scope was accepted")
    partial = {"synexia.prefix-counts.tsv": first["synexia.prefix-counts.tsv"]}
    try:
        verify_optional_bundle(partial)
    except ValueError:
        checks += 1
    else:
        raise AssertionError("partial family publication was accepted")
    broken = dict(first)
    broken["synexia.prefix-counts.tsv"] = first["synexia.prefix-counts.tsv"].replace(b",1,", b",9,", 1)
    try:
        verify_optional_bundle(broken)
    except ValueError:
        checks += 1
    else:
        raise AssertionError("checksum mutation was accepted")
    bad = dict(families)
    bad["prefix-counts"] = [{**families["prefix-counts"][0], "prefix_counts": [1, 1]}]
    try:
        render_bundle(bad)
    except ValueError:
        checks += 1
    else:
        raise AssertionError("invalid prefix counts were accepted")
    return checks

