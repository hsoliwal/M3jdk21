#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Export an operator-supplied Synexia lexicon snapshot for M3JDK.

The exporter is deliberately offline.  Synexia owns the vocabulary identity;
this tool only validates and serializes a supplied snapshot.  It emits the
existing M3LEX001 version-2 image plus sidecars which retain source names,
opaque source IDs, mappings, translation profiles, and bounded precompute
facts.  It never downloads or silently renumbers a dataset.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import pathlib
import struct
from dataclasses import dataclass

MAGIC = 0x4D334C4558303031  # M3LEX001
VERSION = 2
EXPORT_SCHEMA = "synexia-m3jdk-lexicon-export-2"
HEADER = 64
MAX_RECORDS = 65_536
MAX_IMAGE_BYTES = 64 * 1024 * 1024
MAX_SHARD_UNITS = (MAX_IMAGE_BYTES - HEADER - 12 * MAX_RECORDS) // 2
RECORD_COLUMNS = (
    "source_id", "source_path", "source_kind", "language_tag", "record_id",
    "lexeme", "mapping_id", "mapping_name", "translation_profile",
    "precompute_profile",
)
RECORD_COLUMNS_V2 = RECORD_COLUMNS + ("precompute_payload",)
MAPPING_COLUMNS = (
    "source_id", "source_path", "source_kind", "language_tag", "record_id", "lexeme",
    "shard_id", "image_row", "mapping_id", "mapping_name", "translation_profile",
    "precompute_profile", "precompute_payload",
)
MANIFEST_COLUMNS = (
    "source_id", "canonical_name", "synexia_path", "record_id_field",
    "mapping_fields", "precompute_target", "data_license", "data_policy",
)
PROFILE_COLUMNS = ("precompute_profile", "source_records", "image_records", "sha256")
MAX_PRECOMPUTE_PAYLOAD_BYTES = 1 * 1024 * 1024


@dataclass(frozen=True)
class Record:
    values: dict[str, str]

    def __getitem__(self, name: str) -> str:
        return self.values[name]


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def utf16_units(text: str) -> bytes:
    return text.encode("utf-16-be", "surrogatepass")


def escape_sidecar_text(text: str) -> str:
    """Keep sidecars valid UTF-8 while preserving exact UTF-16 code units."""
    units = struct.unpack(">" + "H" * (len(utf16_units(text)) // 2), utf16_units(text))
    output: list[str] = []
    at = 0
    while at < len(units):
        unit = units[at]
        if 0xD800 <= unit <= 0xDBFF and at + 1 < len(units) and 0xDC00 <= units[at + 1] <= 0xDFFF:
            output.append(chr(0x10000 + ((unit - 0xD800) << 10) + units[at + 1] - 0xDC00))
            at += 2
        elif unit == 0x5C or unit < 0x20 or 0xD800 <= unit <= 0xDFFF:
            output.append(f"\\u{unit:04X}")
            at += 1
        else:
            output.append(chr(unit))
            at += 1
    return "".join(output)


def java_hash(text: str) -> int:
    encoded = text.encode("utf-16-le", "surrogatepass")
    value = 0
    for at in range(0, len(encoded), 2):
        value = (value * 31 + encoded[at] + (encoded[at + 1] << 8)) & 0xFFFFFFFF
    return value


def code_point_facts(text: str) -> tuple[int, int, int]:
    """Return code points, unpaired UTF-16 surrogates, and non-BMP count."""
    code_points = unpaired = non_bmp = 0
    units = list(struct.unpack(">" + "H" * (len(utf16_units(text)) // 2), utf16_units(text)))
    at = 0
    while at < len(units):
        unit = units[at]
        if 0xD800 <= unit <= 0xDBFF:
            if at + 1 < len(units) and 0xDC00 <= units[at + 1] <= 0xDFFF:
                code_points += 1
                non_bmp += 1
                at += 2
                continue
            unpaired += 1
        elif 0xDC00 <= unit <= 0xDFFF:
            unpaired += 1
        code_points += 1
        at += 1
    return code_points, unpaired, non_bmp


def read_tsv(path: pathlib.Path, columns: tuple[str, ...]) -> tuple[list[dict[str, str]], bytes]:
    raw = path.read_bytes()
    rows: list[dict[str, str]] = []
    with path.open("r", encoding="utf-8", errors="surrogatepass", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if tuple(reader.fieldnames or ()) != columns:
            raise ValueError(f"{path}: expected columns {columns}")
        for line, row in enumerate(reader, 2):
            if None in row or any(value is None for value in row.values()):
                raise ValueError(f"{path}:{line}: wrong field count")
            if any("\t" in value or "\r" in value or "\n" in value
                   for value in row.values()):
                raise ValueError(f"{path}:{line}: embedded tab/newline")
            if any(value == "" for value in row.values()):
                raise ValueError(f"{path}:{line}: empty field")
            rows.append(row)
    return rows, raw


def read_manifest(path: pathlib.Path) -> tuple[dict[str, dict[str, str]], bytes]:
    rows, raw = read_tsv(path, MANIFEST_COLUMNS)
    result: dict[str, dict[str, str]] = {}
    for row in rows:
        source_id = row["source_id"]
        if source_id in result:
            raise ValueError(f"duplicate source_id: {source_id}")
        result[source_id] = row
    if not result:
        raise ValueError("source manifest is empty")
    return result, raw


def _reject_json_constant(value: str) -> None:
    raise ValueError(f"non-finite JSON number is not allowed: {value}")


def _object_without_duplicate_keys(pairs: list[tuple[str, object]]) -> dict[str, object]:
    result: dict[str, object] = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate precompute payload key: {key}")
        result[key] = value
    return result


def canonical_precompute_payload(value: str) -> str:
    """Normalize an owner payload without interpreting its domain-specific fields."""
    if len(value.encode("utf-8", "surrogatepass")) > MAX_PRECOMPUTE_PAYLOAD_BYTES:
        raise ValueError("precompute payload exceeds 1 MiB")
    try:
        payload = json.loads(value, object_pairs_hook=_object_without_duplicate_keys,
                             parse_constant=_reject_json_constant)
    except (TypeError, ValueError, json.JSONDecodeError) as error:
        raise ValueError("invalid precompute payload JSON") from error
    if not isinstance(payload, dict):
        raise ValueError("precompute payload must be a JSON object")
    canonical = json.dumps(payload, ensure_ascii=True, sort_keys=True,
                           separators=(",", ":"), allow_nan=False)
    if len(canonical.encode("utf-8")) > MAX_PRECOMPUTE_PAYLOAD_BYTES:
        raise ValueError("canonical precompute payload exceeds 1 MiB")
    return canonical


def load_records(path: pathlib.Path, sources: dict[str, dict[str, str]]) -> tuple[list[Record], bytes]:
    raw = path.read_bytes()
    with path.open("r", encoding="utf-8", errors="surrogatepass", newline="") as stream:
        columns = tuple(csv.DictReader(stream, delimiter="\t").fieldnames or ())
    if columns not in (RECORD_COLUMNS, RECORD_COLUMNS_V2):
        raise ValueError(f"{path}: expected columns {RECORD_COLUMNS} or {RECORD_COLUMNS_V2}")
    rows, _ = read_tsv(path, columns)
    seen: set[tuple[str, str]] = set()
    result: list[Record] = []
    for row in rows:
        key = (row["source_id"], row["record_id"])
        if row["source_id"] not in sources:
            raise ValueError(f"record references unknown source_id: {row['source_id']}")
        if key in seen:
            raise ValueError(f"duplicate source/record identity: {key[0]}:{key[1]}")
        seen.add(key)
        if row["source_path"] != sources[row["source_id"]]["synexia_path"]:
            raise ValueError(f"source path mismatch for {row['source_id']}")
        if row["precompute_profile"] != sources[row["source_id"]]["precompute_target"]:
            raise ValueError(f"precompute profile mismatch for {row['source_id']}")
        normalized = {column: row[column] for column in RECORD_COLUMNS}
        normalized["precompute_payload"] = canonical_precompute_payload(
            row.get("precompute_payload", "{}"))
        result.append(Record(normalized))
    if not result:
        raise ValueError("lexicon records are empty")
    return result, raw


def image_bytes(lexemes: list[str]) -> bytes:
    if len(lexemes) > MAX_RECORDS:
        raise ValueError("too many unique lexemes")
    encoded = [utf16_units(value) for value in lexemes]
    units = sum(len(value) // 2 for value in encoded)
    payload = HEADER + 12 * len(encoded)
    size = payload + 2 * units
    if size > MAX_IMAGE_BYTES:
        raise ValueError("M3LEX001 image exceeds size limit")
    image = bytearray(size)
    struct.pack_into(">QIIQQ", image, 0, MAGIC, VERSION, len(encoded), payload, units)
    cursor = 0
    for row, (text, encoded_text) in enumerate(zip(lexemes, encoded)):
        struct.pack_into(">III", image, HEADER + row * 12,
                         cursor, len(encoded_text) // 2, java_hash(text))
        begin = payload + cursor * 2
        image[begin:begin + len(encoded_text)] = text.encode("utf-16-le", "surrogatepass")
        cursor += len(encoded_text) // 2
    digest = hashlib.sha256(bytes(image[:32]) + bytes(image[HEADER:])).digest()
    image[32:64] = digest
    return bytes(image)


def make_shards(lexemes: list[str]) -> list[list[str]]:
    shards: list[list[str]] = []
    current: list[str] = []
    current_units = 0
    for value in lexemes:
        value_units = len(utf16_units(value)) // 2
        if current and (len(current) >= MAX_RECORDS or current_units + value_units > MAX_SHARD_UNITS):
            shards.append(current)
            current = []
            current_units = 0
        current.append(value)
        current_units += value_units
    if current:
        shards.append(current)
    return shards


def facts_for(text: str, profile: str, row: int) -> dict[str, object]:
    encoded = utf16_units(text)
    code_points, unpaired, non_bmp = code_point_facts(text)
    return {
        "image_row": row,
        "utf16_units": len(encoded) // 2,
        "java_hash": java_hash(text),
        "code_points": code_points,
        "unpaired_surrogates": unpaired,
        "non_bmp_code_points": non_bmp,
        "ascii": all(unit < 0x80 for unit in struct.unpack(">" + "H" * (len(encoded) // 2), encoded)),
        "latin1": all(unit < 0x100 for unit in struct.unpack(">" + "H" * (len(encoded) // 2), encoded)),
        "contains_whitespace": any(character.isspace() for character in text),
        "precompute_profile": profile,
    }


def write_tsv(path: pathlib.Path, columns: tuple[str, ...], rows: list[dict[str, object]]) -> bytes:
    with path.open("w", encoding="utf-8", errors="surrogatepass", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=columns, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    return path.read_bytes()


def export(source_manifest: pathlib.Path, records_path: pathlib.Path, output: pathlib.Path,
           source_repo: str, source_commit: str) -> dict[str, object]:
    sources, source_manifest_bytes = read_manifest(source_manifest)
    records, records_bytes = load_records(records_path, sources)
    if output.exists() and any(output.iterdir()):
        raise FileExistsError(f"output directory is not empty: {output}")
    output.mkdir(parents=True, exist_ok=True)

    by_lexeme = sorted({record["lexeme"] for record in records}, key=utf16_units)
    shards = make_shards(by_lexeme)
    physical_row = {
        value: (shard, row)
        for shard, values in enumerate(shards)
        for row, value in enumerate(values)
    }
    image_outputs: dict[str, str] = {}
    shard_rows: list[dict[str, object]] = []
    for shard, values in enumerate(shards):
        image = image_bytes(values)
        filename = "synexia.m3lex" if len(shards) == 1 else f"synexia-{shard:06d}.m3lex"
        (output / filename).write_bytes(image)
        image_outputs[filename] = sha256_bytes(image)
        shard_rows.append({
            "shard_id": shard,
            "file": filename,
            "first_lexeme": escape_sidecar_text(values[0]),
            "last_lexeme": escape_sidecar_text(values[-1]),
            "image_records": len(values),
            "utf16_units": sum(len(utf16_units(value)) // 2 for value in values),
            "sha256": image_outputs[filename],
        })
    shard_columns = ("shard_id", "file", "first_lexeme", "last_lexeme", "image_records",
                     "utf16_units", "sha256")
    shard_bytes = write_tsv(output / "synexia.shards.tsv", shard_columns, shard_rows)

    mapping_columns = MAPPING_COLUMNS
    mapping_rows = [{column: (physical_row[record["lexeme"]][0] if column == "shard_id"
                              else physical_row[record["lexeme"]][1] if column == "image_row"
                              else escape_sidecar_text(record[column]) if column == "lexeme"
                              else record[column])
                     for column in mapping_columns} for record in records]
    mapping_bytes = write_tsv(output / "synexia.records.tsv", mapping_columns, mapping_rows)

    profile_sources: dict[str, set[tuple[str, str]]] = {}
    profile_coordinates: dict[str, set[tuple[int, int]]] = {}
    for record in records:
        profile = record["precompute_profile"]
        profile_sources.setdefault(profile, set()).add((record["source_id"], record["record_id"]))
        profile_coordinates.setdefault(profile, set()).add(physical_row[record["lexeme"]])
    profile_rows = []
    for profile in sorted(profile_sources):
        source_count = len(profile_sources[profile])
        image_count = len(profile_coordinates[profile])
        fingerprint = sha256_bytes(f"{profile}\t{source_count}\t{image_count}\n".encode("utf-8"))
        profile_rows.append({"precompute_profile": profile, "source_records": source_count,
                             "image_records": image_count, "sha256": fingerprint})
    profile_bytes = write_tsv(output / "synexia.precompute-index.tsv", PROFILE_COLUMNS, profile_rows)

    fact_columns = ("shard_id", "image_row", "utf16_units", "java_hash", "code_points", "unpaired_surrogates",
                    "non_bmp_code_points", "ascii", "latin1", "contains_whitespace",
                    "precompute_profile")
    profiles: dict[str, set[str]] = {value: set() for value in by_lexeme}
    for record in records:
        profiles[record["lexeme"]].add(record["precompute_profile"])
    fact_rows = [{"shard_id": physical_row[value][0], **facts_for(
        value, " + ".join(sorted(profiles[value])), physical_row[value][1])}
                 for value in by_lexeme]
    fact_bytes = write_tsv(output / "synexia.precompute.tsv", fact_columns, fact_rows)

    manifest = {
        "schema": EXPORT_SCHEMA,
        "source": {"repository": source_repo, "commit": source_commit,
                   "manifest_sha256": sha256_bytes(source_manifest_bytes),
                   "records_sha256": sha256_bytes(records_bytes)},
        "target": {"repository": "https://github.com/hsoliwal/M3jdk21",
                   "image_format": "M3LEX001", "image_version": VERSION,
                   "mapping_sidecar": "synexia.records.tsv",
                   "mapping_payload_field": "precompute_payload (canonical JSON object; v1 input defaults to {})",
                   "shards_sidecar": "synexia.shards.tsv",
                   "precompute_index_sidecar": "synexia.precompute-index.tsv",
                   "precompute_sidecar": "synexia.precompute.tsv"},
        "counts": {"source_records": len(records), "image_records": len(by_lexeme),
                   "source_families": len(sources), "shards": len(shards),
                   "precompute_profiles": len(profile_rows)},
        "outputs": {**image_outputs, "synexia.shards.tsv": sha256_bytes(shard_bytes),
                    "synexia.records.tsv": sha256_bytes(mapping_bytes),
                    "synexia.precompute-index.tsv": sha256_bytes(profile_bytes),
                    "synexia.precompute.tsv": sha256_bytes(fact_bytes)},
        "identity_rule": "source_id + record_id is opaque and never renumbered; image_row is only a physical M3LEX projection",
        "data_policy": "operator-supplied snapshot only; no network download or implicit license grant",
    }
    (output / "synexia.export.json").write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return manifest


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-manifest", type=pathlib.Path, required=True)
    parser.add_argument("--records", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--source-repository", required=True)
    parser.add_argument("--source-commit", required=True)
    args = parser.parse_args(argv)
    result = export(args.source_manifest, args.records, args.output,
                    args.source_repository, args.source_commit)
    print("SYNEXIA_M3JDK_LEXICON_EXPORT_PASS " + json.dumps(result["counts"], sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
