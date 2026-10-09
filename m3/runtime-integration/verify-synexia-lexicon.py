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
import io
import json
import math
import pathlib
import re
import struct

import synexia_family_sidecars as family_sidecars

MAGIC = 0x4D334C4558303031
VERSION = 2
EXPORT_SCHEMA = "synexia-m3jdk-lexicon-export-2"
HEADER = 64
SHARD_COLUMNS = ("shard_id", "file", "first_lexeme", "last_lexeme", "image_records", "utf16_units", "sha256")
MAPPING_COLUMNS = ("source_id", "source_path", "source_kind", "language_tag", "record_id", "lexeme",
                   "shard_id", "image_row", "mapping_id", "mapping_name", "translation_profile",
                   "precompute_profile", "precompute_payload")
RELATION_COLUMNS = ("source_id", "record_id", "lexeme", "related_lexeme", "shard_id", "image_row")
RELATION_SOURCE_COLUMNS = ("source_id",)
FACT_COLUMNS = ("shard_id", "image_row", "utf16_units", "java_hash", "code_points",
                "unpaired_surrogates", "non_bmp_code_points", "ascii", "latin1",
                "contains_whitespace", "precompute_profile")
PROFILE_COLUMNS = ("precompute_profile", "source_records", "image_records", "sha256")
MAX_PRECOMPUTE_PAYLOAD_BYTES = 1 * 1024 * 1024
ALLOWED_DONOR_TYPES = frozenset((
    "boolean", "double", "int", "long", "int[]", "long[]", "String",
    "byte[]", "byte[][]", "Map<String,int[]>", "Map<Integer,Long>",
    "Map<Integer,Integer>", "RangeFingerprint",
))


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def utf16_key(value: str) -> bytes:
    return value.encode("utf-16-be", "surrogatepass")


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
    if donor_type == "String":
        return isinstance(value, str)
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
    if donor_type == "byte[]":
        return (isinstance(value, list)
                and all(isinstance(item, int) and not isinstance(item, bool)
                        and 0 <= item <= 255 for item in value))
    if donor_type == "byte[][]":
        return (isinstance(value, list)
                and all(_fits_donor_type(item, "byte[]") for item in value))
    if donor_type == "Map<String,int[]>":
        return (isinstance(value, dict)
                and all(isinstance(key, str)
                        and _fits_donor_type(item, "int[]")
                        for key, item in value.items()))
    if donor_type in ("Map<Integer,Long>", "Map<Integer,Integer>"):
        value_type = "long" if donor_type.endswith("Long>") else "int"
        return (isinstance(value, dict)
                and all(isinstance(key, str) and _is_decimal_int(key, "int")
                        and _fits_donor_type(item, value_type)
                        for key, item in value.items()))
    if donor_type == "RangeFingerprint":
        return (isinstance(value, dict)
                and set(value) == {"first", "second", "length"}
                and _fits_donor_type(value["first"], "long")
                and _fits_donor_type(value["second"], "long")
                and _fits_donor_type(value["length"], "int")
                and value["length"] >= 0)
    return False


def _is_decimal_int(value: object, donor_type: str) -> bool:
    if not isinstance(value, str) or not re.fullmatch(r"-?(0|[1-9][0-9]*)", value):
        return False
    try:
        parsed = int(value)
    except ValueError:
        return False
    return _fits_donor_type(parsed, donor_type)


def _fits_any_donor_type(value: object, donor_type: object) -> bool:
    candidates = (donor_type,) if isinstance(donor_type, str) else donor_type
    return (isinstance(candidates, (list, tuple))
            and all(isinstance(candidate, str) for candidate in candidates)
            and any(_fits_donor_type(value, candidate) for candidate in candidates))


def unescape_sidecar_text(value: str) -> str:
    """Decode the exporterâ€™s UTF-16-preserving sidecar escaping."""
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
    previous: bytes | None = None
    for row in range(count):
        offset, length, stored_hash = struct.unpack_from(">III", data, HEADER + 12 * row)
        if offset + length > units:
            raise ValueError(f"{path.name}: record outside payload")
        raw = data[payload + offset * 2:payload + (offset + length) * 2]
        text = raw.decode("utf-16-le", "surrogatepass")
        if java_hash(text) != stored_hash:
            raise ValueError(f"{path.name}: Java hash mismatch at row {row}")
        current_key = utf16_key(text)
        if previous is not None and previous >= current_key:
            raise ValueError(f"{path.name}: records are not strictly UTF-16 sorted")
        previous = current_key
        values.append(text)
        hashes.append(stored_hash)
    if sum(len(value.encode("utf-16-le", "surrogatepass")) // 2 for value in values) != units:
        raise ValueError(f"{path.name}: unit total mismatch")
    return values, hashes, units


def verify_family_sidecar_scope(
        files: dict[str, bytes],
        mapping_by_identity: dict[tuple[str, str], tuple[str, tuple[int, int]]],
) -> dict[str, int]:
    """Verify family sidecars and bind their source/coordinate scope to records."""

    stats = family_sidecars.verify_optional_bundle(files)
    if bool(stats["legacy"]):
        return {"families": 0, "rows": 0}
    rows = 0
    for family, spec in family_sidecars.FAMILY_SPECS.items():
        content = files[spec["file"]].decode("utf-8")
        reader = csv.DictReader(io.StringIO(content), delimiter="\t")
        for row in reader:
            identity = (row["source_id"], row["record_id"])
            mapped = mapping_by_identity.get(identity)
            if mapped is None:
                raise ValueError(f"{family} sidecar references unknown source identity")
            if family in {"prefix-counts", "token-frequency", "token-hash-precompute"}:
                coordinate = (int(row["shard_id"]), int(row["image_row"]))
                if coordinate != mapped[1]:
                    raise ValueError(f"{family} sidecar coordinate mismatch")
            rows += 1
    return {"families": int(stats["families"]), "rows": rows}


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
    source_field_types = manifest.get("source", {}).get("precompute_field_types", {})
    if not isinstance(source_field_types, dict) or any(
            not isinstance(field, str)
            or not isinstance(donor_type, (str, list))
            or (isinstance(donor_type, str)
                and donor_type not in ALLOWED_DONOR_TYPES)
            or (isinstance(donor_type, list)
                and (not donor_type
                     or donor_type != sorted(set(donor_type))
                     or any(candidate not in ALLOWED_DONOR_TYPES
                            for candidate in donor_type)))
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
    mapping_by_identity: dict[tuple[str, str], tuple[str, tuple[int, int]]] = {}
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
            for field in source_payload_fields[row["source_id"]]:
                donor_type = source_field_types.get(field)
                if donor_type is None or not _fits_any_donor_type(payload[field], donor_type):
                    raise ValueError("precompute payload field type mismatch")
        profile = row["precompute_profile"]
        profile_sources.setdefault(profile, set()).add(identity)
        profile_coordinates.setdefault(profile, set()).add(key)
        mapping_profiles.setdefault(key, set()).add(profile)
        mapping_by_identity[identity] = (unescape_sidecar_text(row["lexeme"]), key)
    if len(mapping_rows) != int(manifest["counts"]["source_records"]):
        raise ValueError("source record count mismatch")

    family_names = {family_sidecars.INDEX_FILE} | {
        spec["file"] for spec in family_sidecars.FAMILY_SPECS.values()
    }
    declared_family_files = target.get("family_sidecar_files")
    declared_family_index = target.get("family_sidecar_index")
    present_family_outputs = set(outputs) & family_names
    if declared_family_files is None:
        if declared_family_index is not None or present_family_outputs:
            raise ValueError("family sidecar metadata is incomplete")
        family_stats = {"families": 0, "rows": 0}
    else:
        expected_family_files = sorted(family_names)
        if (not isinstance(declared_family_files, list)
                or declared_family_files != expected_family_files
                or declared_family_index != family_sidecars.INDEX_FILE
                or present_family_outputs != family_names):
            raise ValueError("family sidecar publication metadata is incomplete")
        family_files = {
            name: (output / safe_filename(name)).read_bytes()
            for name in expected_family_files
        }
        family_stats = verify_family_sidecar_scope(family_files, mapping_by_identity)
        if (int(manifest["counts"].get("family_sidecar_families", -1))
                != family_stats["families"]
                or int(manifest["counts"].get("family_sidecar_rows", -1))
                != family_stats["rows"]):
            raise ValueError("family sidecar count mismatch")

    source_metadata = manifest.get("source", {})
    relation_sources = source_metadata.get("relation_sources", [])
    if (not isinstance(relation_sources, list)
            or any(not isinstance(source_id, str) for source_id in relation_sources)
            or relation_sources != sorted(set(relation_sources), key=utf16_key)):
        raise ValueError("invalid related-lexeme source catalog")
    relations_sha256 = source_metadata.get("relations_sha256")
    relation_policy = source_metadata.get("relation_policy")
    relation_sidecar = target.get("relation_sidecar")
    relation_sources_sidecar = target.get("relation_sources_sidecar")
    relation_rows: list[dict[str, str]] = []
    if relation_sources:
        if not isinstance(relation_policy, str) or not relation_policy.strip():
            raise ValueError("related-lexeme extraction policy is missing")
        if (not isinstance(relations_sha256, str)
                or re.fullmatch(r"[0-9a-f]{64}", relations_sha256) is None):
            raise ValueError("related-lexeme input hash is missing")
        if not isinstance(relation_sidecar, str):
            raise ValueError("related-lexeme sidecar is missing")
        if not isinstance(relation_sources_sidecar, str):
            raise ValueError("related-lexeme source sidecar is missing")
        declared_sources = read_tsv(
            output / safe_filename(relation_sources_sidecar), RELATION_SOURCE_COLUMNS
        )
        declared_source_ids = [row["source_id"] for row in declared_sources]
        if declared_source_ids != relation_sources:
            raise ValueError("related-lexeme source sidecar mismatch")
        relation_rows = read_tsv(output / safe_filename(relation_sidecar), RELATION_COLUMNS)
        previous_relation_key: tuple[bytes, bytes, bytes, bytes] | None = None
        relation_identities: set[tuple[str, str]] = set()
        relation_pairs: set[tuple[str, str, str]] = set()
        for row in relation_rows:
            identity = (row["source_id"], row["record_id"])
            if row["source_id"] not in relation_sources:
                raise ValueError("related-lexeme source family is not admitted")
            mapped = mapping_by_identity.get(identity)
            if mapped is None:
                raise ValueError("related-lexeme source identity is not mapped")
            lexeme = unescape_sidecar_text(row["lexeme"])
            related_lexeme = unescape_sidecar_text(row["related_lexeme"])
            if not related_lexeme:
                raise ValueError("related_lexeme must not be empty")
            relation_key = tuple(
                utf16_key(value)
                for value in (row["source_id"], row["record_id"], lexeme, related_lexeme)
            )
            if previous_relation_key is not None and relation_key <= previous_relation_key:
                raise ValueError("related-lexeme rows are not strictly UTF-16 sorted")
            previous_relation_key = relation_key
            pair = (identity[0], identity[1], related_lexeme)
            if pair in relation_pairs:
                raise ValueError("duplicate related-lexeme relation")
            relation_pairs.add(pair)
            relation_identities.add(identity)
            if lexeme != mapped[0] or (int(row["shard_id"]), int(row["image_row"])) != mapped[1]:
                raise ValueError("related-lexeme mapping mismatch")
        expected_relation_identities = {
            identity for identity in mapping_by_identity if identity[0] in relation_sources
        }
        if relation_identities != expected_relation_identities:
            raise ValueError("related-lexeme coverage mismatch")
    elif (relation_sidecar is not None or relation_sources_sidecar is not None
          or relations_sha256 is not None or relation_policy is not None):
        raise ValueError("unexpected related-lexeme metadata")

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
    result = {"source_records": len(mapping_rows), "image_records": len(images),
              "shards": len(shard_rows), "precompute_profiles": len(profile_rows),
              "utf16_units": total_units,
              "family_sidecar_families": family_stats["families"],
              "family_sidecar_rows": family_stats["rows"]}
    if relation_sources:
        result["relation_records"] = len(relation_rows)
    return result


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=pathlib.Path)
    args = parser.parse_args(argv)
    counts = verify(args.output)
    print("SYNEXIA_M3JDK_LEXICON_VERIFY_PASS " + json.dumps(counts, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())


