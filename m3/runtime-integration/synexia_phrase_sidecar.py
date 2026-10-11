#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Deterministic exporter/verifier for the m3phrase-v1 receiver contract."""

from __future__ import annotations

import csv
import io
from collections.abc import Iterable, Mapping

SCHEMA_VERSION = "m3phrase-v1"
FILE_NAME = "synexia.phrases.tsv"
COLUMNS = (
    "schema_version",
    "source_id",
    "record_id",
    "source_revision",
    "vocabulary_fingerprint",
    "source_token_ids",
    "target_token_ids",
)
KEY_COLUMNS = (
    "source_id",
    "record_id",
    "source_revision",
    "vocabulary_fingerprint",
    "source_token_ids",
)


def _text(value: object, name: str) -> str:
    if not isinstance(value, str) or not value:
        raise ValueError(f"{name} must be non-empty")
    if any(ch in value for ch in "\t\r\n"):
        raise ValueError(f"{name} contains a TSV control")
    return value


def _ints(value: object, name: str, allow_empty: bool = True) -> list[int]:
    if not isinstance(value, (list, tuple)):
        raise ValueError(f"{name} must be an array")
    result = []
    for item in value:
        if isinstance(item, bool) or not isinstance(item, int) or item < 0:
            raise ValueError(f"{name} contains an invalid token ID")
        result.append(item)
    if not allow_empty and not result:
        raise ValueError(f"{name} must not be empty")
    return result


def _encode(values: list[int]) -> str:
    return "-" if not values else ",".join(str(value) for value in values)


def _decode(value: str, name: str, allow_empty: bool = True) -> list[int]:
    if value == "-":
        values = []
    else:
        values = value.split(",")
        if any(not item.isdecimal() for item in values):
            raise ValueError(f"{name} contains an invalid token ID")
        values = [int(item) for item in values]
    return _ints(values, name, allow_empty=allow_empty)


def _normalize(raw: Mapping[str, object]) -> tuple[tuple[str, ...], dict[str, str]]:
    source = _ints(raw.get("source_token_ids"), "source_token_ids", allow_empty=False)
    target = _ints(raw.get("target_token_ids"), "target_token_ids")
    row = {
        "schema_version": SCHEMA_VERSION,
        "source_id": _text(raw.get("source_id"), "source_id"),
        "record_id": _text(raw.get("record_id"), "record_id"),
        "source_revision": _text(raw.get("source_revision"), "source_revision"),
        "vocabulary_fingerprint": _text(
            raw.get("vocabulary_fingerprint"), "vocabulary_fingerprint"
        ),
        "source_token_ids": _encode(source),
        "target_token_ids": _encode(target),
    }
    return tuple(row[column] for column in KEY_COLUMNS), row


def render(rows: Iterable[Mapping[str, object]]) -> bytes:
    # IndexPhraseTable is last-write-wins for a duplicate source phrase.
    latest: dict[tuple[str, ...], dict[str, str]] = {}
    for raw in rows:
        key, row = _normalize(raw)
        latest[key] = row
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(
        stream, fieldnames=COLUMNS, delimiter="\t", lineterminator="\n"
    )
    writer.writeheader()
    for key in sorted(latest):
        writer.writerow(latest[key])
    return stream.getvalue().encode("utf-8")


def verify(content: bytes) -> int:
    if content.startswith(b"\xef\xbb\xbf") or b"\r" in content:
        raise ValueError("phrase sidecar must be UTF-8 without BOM and use LF")
    reader = csv.DictReader(io.StringIO(content.decode("utf-8")), delimiter="\t")
    if tuple(reader.fieldnames or ()) != COLUMNS:
        raise ValueError("phrase sidecar header mismatch")
    previous: tuple[str, ...] | None = None
    count = 0
    for raw in reader:
        for column in COLUMNS:
            if raw.get(column) is None:
                raise ValueError("phrase sidecar field missing")
        if raw["schema_version"] != SCHEMA_VERSION:
            raise ValueError("phrase sidecar schema mismatch")
        key, normalized = _normalize({
            **raw,
            "source_token_ids": _decode(raw["source_token_ids"], "source_token_ids", False),
            "target_token_ids": _decode(raw["target_token_ids"], "target_token_ids"),
        })
        if previous is not None and key <= previous:
            raise ValueError("phrase sidecar rows are not sorted and unique")
        previous = key
        if any(raw[column] != normalized[column] for column in COLUMNS):
            raise ValueError("phrase sidecar row is not canonical")
        count += 1
    return count


def self_test() -> int:
    scope = {
        "source_id": "translate.index-phrases",
        "record_id": "phrases-1",
        "source_revision": "synexia-r1",
        "vocabulary_fingerprint": "lexicon-sha",
    }
    content = render([
        {**scope, "source_token_ids": [1, 2], "target_token_ids": [9]},
        {**scope, "source_token_ids": [1, 2], "target_token_ids": [8]},
        {**scope, "source_token_ids": [4], "target_token_ids": []},
    ])
    checks = 1
    if verify(content) != 2:
        raise AssertionError("phrase duplicate collapse or row count changed")
    checks += 1
    if b"\t1,2\t8\n" not in content or b"\t4\t-\n" not in content:
        raise AssertionError("phrase ordering or empty replacement changed")
    checks += 1
    try:
        verify(content.replace(b"\t1,2\t8\n", b"\t1,2\t8\n\t1,2\t8\n"))
    except ValueError:
        checks += 1
    else:
        raise AssertionError("duplicate serialized phrase accepted")
    return checks


if __name__ == "__main__":
    print(f"SYNEXIA_PHRASE_SIDECAR_PASS checks={self_test()} schema={SCHEMA_VERSION}")
