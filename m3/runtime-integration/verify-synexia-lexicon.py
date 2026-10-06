#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Source-blind verifier for an exported Synexia M3JDK lexicon.

This intentionally does not import the exporter.  It validates the materialized
artifacts after they have crossed the recipe boundary: hashes, shard geometry,
UTF-16 ordering, Java hashes, source-coordinate mappings, and precompute facts.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import pathlib
import re
import struct

MAGIC = 0x4D334C4558303031
VERSION = 2
EXPORT_SCHEMA = "synexia-m3jdk-lexicon-export-2"
HEADER = 64
SHARD_COLUMNS = ("shard_id", "file", "first_lexeme", "last_lexeme", "image_records", "utf16_units", "sha256")
MAPPING_COLUMNS = ("source_id", "source_path", "source_kind", "language_tag", "record_id", "lexeme",
                   "shard_id", "image_row", "mapping_id", "mapping_name", "translation_profile",
                   "precompute_profile", "precompute_payload")
FACT_COLUMNS = ("shard_id", "image_row", "utf16_units", "java_hash", "code_points",
                "unpaired_surrogates", "non_bmp_code_points", "ascii", "latin1",
                "contains_whitespace", "precompute_profile")
PROFILE_COLUMNS = ("precompute_profile", "source_records", "image_records", "sha256")
MAX_PRECOMPUTE_PAYLOAD_BYTES = 1 * 1024 * 1024
ALLOWED_DONOR_TYPES = frozenset(("boolean", "double", "int", "long", "int[]", "long[]"))


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


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


def _fits_donor_type(value: object, donor_type: str) -> bool:
    if donor_type == "boolean":
        return isinstance(value, bool)
    if donor_type == "double":
        if isinstance(value, bool) or not isinstance(value, (int, float)):
            return False
        try:
            return math.isfinite(float(value))
        except (OverflowError, ValueError):
            return False
    if donor_type == "int":
        return isinstance(value, int) and not isinstance(value, bool) and -2**31 <= value < 2**31
    if donor_type == "long":
        return isinstance(value, int) and not isinstance(value, bool) and -2**63 <= value < 2**63
    if donor_type == "int[]":
        return (isinstance(value, list)
                and all(_fits_donor_type(item, "int") for item in value))
    if donor_type == "long[]":
        return (isinstance(value, list)
                and all(_fits_donor_type(item, "long") for item in value))
    return False


def unescape_sidecar_text(value: str) -> str:
    """Decode the exporter’s UTF-16-preserving sidecar escaping."""
    encoded = bytearray()
    at = 0
    while at < len(value):
        if value[at] != "\\":
            encoded.extend(value[at].encode("utf-16-le", "surrogatepass"))
            at += 1
            continue
        if at + 1 < len(value) and value[at + 1] == "\\":
            encoded.extend(b"\\\x00")
            at += 2
            continue
        if at + 5 >= len(value) or value[at + 1] != "u":
            raise ValueError("invalid sidecar escape")
        try:
            unit = int(value[at + 2:at + 6], 16)
        except ValueError as error:
            raise ValueError("invalid sidecar UTF-16 escape") from error
        encoded.extend(struct.pack("<H", unit))
        at += 6
    return bytes(encoded).decode("utf-16-le", "surrogatepass")


def read_tsv(path: pathlib.Path, columns: tuple[str, ...]) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", errors="surrogatepass", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if tuple(reader.fieldnames or ()) != columns:
            raise ValueError(f"{path.name}: unexpected columns")
        rows = list(reader)
    if any(None in row or any(value is None for value in row.values()) for row in rows):
        raise ValueError(f"{path.name}: malformed field count")
    return rows


def java_hash(text: str) -> int:
    value = 0
    encoded = text.encode("utf-16-le", "surrogatepass")
    for at in range(0, len(encoded), 2):
        value = (value * 31 + encoded[at] + (encoded[at + 1] << 8)) & 0xFFFFFFFF
    return value


def facts(text: str) -> tuple[int, int, int, int, int, bool, bool, bool]:
    encoded = text.encode("utf-16-be", "surrogatepass")
    units = struct.unpack(">" + "H" * (len(encoded) // 2), encoded)
    code_points = unpaired = non_bmp = 0
    at = 0
    while at < len(units):
        unit = units[at]
        if 0xD800 <= unit <= 0xDBFF and at + 1 < len(units) and 0xDC00 <= units[at + 1] <= 0xDFFF:
            code_points += 1
            non_bmp += 1
            at += 2
        else:
            if 0xD800 <= unit <= 0xDFFF:
                unpaired += 1
            code_points += 1
            at += 1
    return (len(units), java_hash(text), code_points, unpaired, non_bmp,
            all(unit < 0x80 for unit in units), all(unit < 0x100 for unit in units),
            any(character.isspace() for character in text))


def safe_filename(value: str) -> pathlib.Path:
    path = pathlib.Path(value)
    if not value or path.is_absolute() or "/" in value or "\\" in value or value in {".", ".."}:
        raise ValueError("unsafe export filename")
    return path


def read_image(path: pathlib.Path) -> tuple[list[str], list[int], int]:
    data = path.read_bytes()
    if len(data) < HEADER:
        raise ValueError(f"{path.name}: image too small")
    magic, version, count, payload, units = struct.unpack_from(">QIIQQ", data)
    if magic != MAGIC or version != VERSION:
        raise ValueError(f"{path.name}: unsupported M3LEX image")
    if count < 1 or payload != HEADER + 12 * count or payload + 2 * units != len(data):
        raise ValueError(f"{path.name}: invalid image dimensions")
    if data[32:64] != hashlib.sha256(data[:32] + data[64:]).digest():
        raise ValueError(f"{path.name}: image checksum mismatch")
    values: list[str] = []
    hashes: list[int] = []
    previous: str | None = None
    for row in range(count):
        offset, length, stored_hash = struct.unpack_from(">III", data, HEADER + 12 * row)
        if offset + length > units:
            raise ValueError(f"{path.name}: record outside payload")
        raw = data[payload + offset * 2:payload + (offset + length) * 2]
        text = raw.decode("utf-16-le", "surrogatepass")
        if java_hash(text) != stored_hash:
            raise ValueError(f"{path.name}: Java hash mismatch at row {row}")
        if previous is not None and previous >= text:
            raise ValueError(f"{path.name}: records are not strictly UTF-16 sorted")
        previous = text
        values.append(text)
        hashes.append(stored_hash)
    if sum(len(value.encode("utf-16-le", "surrogatepass")) // 2 for value in values) != units:
        raise ValueError(f"{path.name}: unit total mismatch")
    return values, hashes, units


def verify(output: pathlib.Path) -> dict[str, int]:
    manifest_path = output / "synexia.export.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("schema") != EXPORT_SCHEMA:
        raise ValueError("unsupported export schema")
    target = manifest.get("target", {})
    if target.get("image_format") != "M3LEX001" or target.get("image_version") != VERSION:
        raise ValueError("unsupported target image contract")
    outputs = manifest.get("outputs", {})
    required = {"synexia.shards.tsv", "synexia.records.tsv", "synexia.precompute-index.tsv",
                "synexia.precompute.tsv"}
    if not required.issubset(outputs):
        raise ValueError("export output hashes are incomplete")
    for name, expected in outputs.items():
        path = output / safe_filename(name)
        if not path.is_file() or digest(path.read_bytes()) != expected:
            raise ValueError(f"output hash mismatch: {name}")

    source_payload_fields = manifest.get("source", {}).get("precompute_fields")
    if not isinstance(source_payload_fields, dict):
        raise ValueError("source precompute field requirements are missing")
    for source_id, fields in source_payload_fields.items():
        if (not isinstance(source_id, str) or not isinstance(fields, list)
                or any(not isinstance(field, str)
                       or re.fullmatch(r"[a-z][a-z0-9_]*", field) is None for field in fields)
                or fields != sorted(set(fields))):
            raise ValueError("invalid source precompute field requirements")
    source_optional_fields = manifest.get("source", {}).get("optional_precompute_fields")
    if source_optional_fields is None:
        source_optional_fields = {source_id: [] for source_id in source_payload_fields}
    if (not isinstance(source_optional_fields, dict)
            or any(source_id not in source_payload_fields for source_id in source_optional_fields)):
        raise ValueError("invalid source optional precompute fields")
    for source_id, fields in source_optional_fields.items():
        if (not isinstance(source_id, str) or not isinstance(fields, list)
                or any(not isinstance(field, str)
                       or re.fullmatch(r"[a-z][a-z0-9_]*", field) is None for field in fields)
                or fields != sorted(set(fields))
                or set(fields) & set(source_payload_fields[source_id])):
            raise ValueError("invalid source optional precompute fields")
    source_field_types = manifest.get("source", {}).get("precompute_field_types", {})
    if not isinstance(source_field_types, dict) or any(
            not isinstance(field, str) or not isinstance(donor_type, str)
            or donor_type not in ALLOWED_DONOR_TYPES
            for field, donor_type in source_field_types.items()):
        raise ValueError("invalid source precompute field types")
    field_map_digest = manifest.get("source", {}).get("precompute_field_map_sha256")
    if (field_map_digest is not None
            and (not isinstance(field_map_digest, str)
                 or re.fullmatch(r"[0-9a-f]{64}", field_map_digest) is None)):
        raise ValueError("invalid source precompute field-map hash")

    shard_rows = read_tsv(output / "synexia.shards.tsv", SHARD_COLUMNS)
    images: dict[tuple[int, int], str] = {}
    image_hashes: dict[tuple[int, int], int] = {}
    total_units = 0
    for row in shard_rows:
        shard = int(row["shard_id"])
        filename = safe_filename(row["file"])
        values, hashes, units = read_image(output / filename)
        if int(row["image_records"]) != len(values) or int(row["utf16_units"]) != units:
            raise ValueError(f"shard metadata mismatch: {filename}")
        if unescape_sidecar_text(row["first_lexeme"]) != values[0] or unescape_sidecar_text(row["last_lexeme"]) != values[-1]:
            raise ValueError(f"shard lexical bounds mismatch: {filename}")
        if filename.name not in outputs:
            raise ValueError(f"shard missing from output manifest: {filename}")
        total_units += units
        for image_row, value in enumerate(values):
            key = (shard, image_row)
            if key in images:
                raise ValueError("duplicate physical image coordinate")
            images[key] = value
            image_hashes[key] = hashes[image_row]
    if len(shard_rows) != int(manifest["counts"]["shards"]):
        raise ValueError("shard count mismatch")
    if len(images) != int(manifest["counts"]["image_records"]):
        raise ValueError("image record count mismatch")

    mapping_rows = read_tsv(output / "synexia.records.tsv", MAPPING_COLUMNS)
    identities: set[tuple[str, str]] = set()
    profile_sources: dict[str, set[tuple[str, str]]] = {}
    profile_coordinates: dict[str, set[tuple[int, int]]] = {}
    mapping_profiles: dict[tuple[int, int], set[str]] = {}
    for row in mapping_rows:
        identity = (row["source_id"], row["record_id"])
        if identity in identities:
            raise ValueError("duplicate source identity")
        identities.add(identity)
        key = (int(row["shard_id"]), int(row["image_row"]))
        if key not in images or unescape_sidecar_text(row["lexeme"]) != images[key]:
            raise ValueError("source mapping does not match image coordinate")
        if not row["mapping_name"] or not row["precompute_profile"]:
            raise ValueError("mapping metadata is incomplete")
        if canonical_precompute_payload(row["precompute_payload"]) != row["precompute_payload"]:
            raise ValueError("precompute payload is not canonical")
        if row["source_id"] not in source_payload_fields:
            raise ValueError("source precompute field requirements do not cover mapping")
        payload = json.loads(row["precompute_payload"])
        payload_keys = set(payload)
        missing = [field for field in source_payload_fields[row["source_id"]]
                   if field not in payload_keys]
        if missing:
            raise ValueError("precompute payload field coverage mismatch")
        if source_field_types:
            fields = source_payload_fields[row["source_id"]] + [
                field for field in source_optional_fields.get(row["source_id"], [])
                if field in payload_keys]
            for field in fields:
                donor_type = source_field_types.get(field)
                if donor_type is None or not _fits_donor_type(payload[field], donor_type):
                    raise ValueError("precompute payload field type mismatch")
        profile = row["precompute_profile"]
        profile_sources.setdefault(profile, set()).add(identity)
        profile_coordinates.setdefault(profile, set()).add(key)
        mapping_profiles.setdefault(key, set()).add(profile)
    if len(mapping_rows) != int(manifest["counts"]["source_records"]):
        raise ValueError("source record count mismatch")

    fact_rows = read_tsv(output / "synexia.precompute.tsv", FACT_COLUMNS)
    fact_keys: set[tuple[int, int]] = set()
    fact_profiles: dict[tuple[int, int], str] = {}
    for row in fact_rows:
        key = (int(row["shard_id"]), int(row["image_row"]))
        if key in fact_keys or key not in images:
            raise ValueError("precompute coordinate mismatch")
        fact_keys.add(key)
        text = images[key]
        expected = facts(text)
        actual = (int(row["utf16_units"]), int(row["java_hash"]), int(row["code_points"]),
                  int(row["unpaired_surrogates"]), int(row["non_bmp_code_points"]),
                  row["ascii"] == "True", row["latin1"] == "True",
                  row["contains_whitespace"] == "True")
        if actual != expected or not row["precompute_profile"]:
            raise ValueError("precompute fact mismatch")
        fact_profiles[key] = row["precompute_profile"]
    if fact_keys != set(images):
        raise ValueError("precompute coverage mismatch")
    for key, profiles in mapping_profiles.items():
        if any(profile not in fact_profiles[key] for profile in profiles):
            raise ValueError("precompute owner profile coverage mismatch")

    profile_rows = read_tsv(output / "synexia.precompute-index.tsv", PROFILE_COLUMNS)
    seen_profiles: set[str] = set()
    for row in profile_rows:
        profile = row["precompute_profile"]
        if profile in seen_profiles or profile not in profile_sources:
            raise ValueError("precompute profile catalog mismatch")
        seen_profiles.add(profile)
        source_count = int(row["source_records"])
        image_count = int(row["image_records"])
        if source_count != len(profile_sources[profile]) or image_count != len(profile_coordinates[profile]):
            raise ValueError("precompute profile cardinality mismatch")
        expected_fingerprint = digest(
            f"{profile}\t{source_count}\t{image_count}\n".encode("utf-8"))
        if row["sha256"] != expected_fingerprint:
            raise ValueError("precompute profile fingerprint mismatch")
    if seen_profiles != set(profile_sources):
        raise ValueError("precompute profile coverage mismatch")
    if len(profile_rows) != int(manifest["counts"]["precompute_profiles"]):
        raise ValueError("precompute profile count mismatch")
    return {"source_records": len(mapping_rows), "image_records": len(images),
            "shards": len(shard_rows), "precompute_profiles": len(profile_rows),
            "utf16_units": total_units}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=pathlib.Path)
    args = parser.parse_args(argv)
    counts = verify(args.output)
    print("SYNEXIA_M3JDK_LEXICON_VERIFY_PASS " + json.dumps(counts, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
