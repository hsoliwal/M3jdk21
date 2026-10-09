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
ALLOWED_DONOR_TYPES = frozenset(("boolean", "double", "int", "long", "int[]", "long[]", "byte[]", "String"))
SOURCE_MANIFEST_COLUMNS = (
    "source_id", "canonical_name", "synexia_path", "record_id_field",
    "mapping_fields", "precompute_target", "data_license", "data_policy",
)
SOURCE_MANIFEST_COLUMNS_V2 = SOURCE_MANIFEST_COLUMNS + ("precompute_fields",)
FIELD_MAP_COLUMNS = (
    "donor_type", "donor_field", "donor_java_type", "canonical_payload_field",
    "m3jdk_storage", "status", "preservation_rule",
)
IDENTITY_MAP_COLUMNS = (
    "target_type", "target_field", "java_type", "canonical_source_field",
    "required", "storage_scope", "preservation_rule",
)
IDENTITY_TARGET_PATTERN = re.compile(r"M3LangDexPrecompute\.[A-Za-z0-9_]+")


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


def canonical_manifest_bytes(value: object) -> bytes:
    return (
        json.dumps(value, ensure_ascii=True, sort_keys=True, separators=(",", ":")) + "\n"
    ).encode("utf-8")


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
                and all(isinstance(item, int) and not isinstance(item, bool) and 0 <= item <= 255
                        for item in value))
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





def read_source_manifest_contract(path: pathlib.Path) -> dict[str, dict[str, object]]:
    with path.open("r", encoding="utf-8", errors="surrogatepass", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        columns = tuple(reader.fieldnames or ())
        if columns not in (SOURCE_MANIFEST_COLUMNS, SOURCE_MANIFEST_COLUMNS_V2):
            raise ValueError(f"{path.name}: unexpected source manifest columns")
        rows = list(reader)
    result: dict[str, dict[str, object]] = {}
    for row in rows:
        source_id = row["source_id"]
        if not source_id or source_id in result:
            raise ValueError("source manifest identity is duplicated or empty")
        value = row.get("precompute_fields", "")
        fields = [] if value in ("", "-") else value.split(",")
        if fields != sorted(set(fields)):
            raise ValueError("source manifest precompute fields are not sorted")
        result[source_id] = {
            "precompute_target": row["precompute_target"],
            "precompute_fields": fields,
        }
    if not result:
        raise ValueError("source manifest is empty")
    return result


def read_field_map_contract(path: pathlib.Path) -> dict[str, str]:
    rows = read_tsv(path, FIELD_MAP_COLUMNS)
    result: dict[str, str] = {}
    for row in rows:
        if row["status"] != "MAPPED":
            continue
        donor_type = row["donor_java_type"]
        if donor_type not in ALLOWED_DONOR_TYPES:
            raise ValueError("unsupported donor field type: " + donor_type)
        field = row["canonical_payload_field"]
        previous = result.setdefault(field, donor_type)
        if previous != donor_type:
            raise ValueError("conflicting donor field types for " + field)
    return dict(sorted(result.items()))


def read_identity_map_contract(path: pathlib.Path) -> dict[str, dict[str, str]]:
    rows = read_tsv(path, IDENTITY_MAP_COLUMNS)
    result: dict[str, dict[str, str]] = {}
    for row in rows:
        if row["required"].lower() != "true":
            continue
        target = row["target_type"]
        field = row["canonical_source_field"]
        if IDENTITY_TARGET_PATTERN.fullmatch(target) is None:
            raise ValueError("unsupported identity target type: " + target)
        previous = result.setdefault(target, {}).setdefault(field, row["java_type"])
        if previous != row["java_type"]:
            raise ValueError("conflicting identity field types for " + field)
    return {
        target: dict(sorted(fields.items()))
        for target, fields in sorted(result.items())
    }


def identity_requirements_contract(
        sources: dict[str, dict[str, object]],
        identity_fields: dict[str, dict[str, str]]) -> dict[str, list[str]]:
    result: dict[str, list[str]] = {}
    for source_id, source in sources.items():
        targets = IDENTITY_TARGET_PATTERN.findall(str(source["precompute_target"]))
        fields = sorted({
            field for target in targets for field in identity_fields.get(target, {})
        })
        declared = set(source["precompute_fields"])
        if not set(fields).issubset(declared):
            raise ValueError("identity fields are missing from source manifest: " + source_id)
        if fields:
            result[source_id] = fields
    return result


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
    manifest_bytes = manifest_path.read_bytes()
    if manifest_bytes.startswith(b"\xef\xbb\xbf") or b"\r" in manifest_bytes:
        raise ValueError("manifest must be UTF-8 without BOM and use LF")
    manifest = json.loads(
        manifest_bytes.decode("utf-8"),
        object_pairs_hook=_object_without_duplicate_keys,
        parse_constant=_reject_json_constant,
    )
    if manifest_bytes != canonical_manifest_bytes(manifest):
        raise ValueError("manifest is not canonical")
    if manifest.get("schema") != EXPORT_SCHEMA:
        raise ValueError("unsupported export schema")
    target = manifest.get("target", {})
    if target.get("image_format") != "M3LEX001" or target.get("image_version") != VERSION:
        raise ValueError("unsupported target image contract")
    outputs = manifest.get("outputs", {})
    required = {"synexia.source-manifest.tsv", "synexia.input-records.tsv",
                "synexia.shards.tsv", "synexia.records.tsv", "synexia.precompute-index.tsv",
                "synexia.precompute.tsv"}
    if not required.issubset(outputs):
        raise ValueError("export output hashes are incomplete")
    for name, expected in outputs.items():
        path = output / safe_filename(name)
        if not path.is_file() or digest(path.read_bytes()) != expected:
            raise ValueError(f"output hash mismatch: {name}")

    source_metadata = manifest.get("source", {})
    if not isinstance(source_metadata, dict):
        raise ValueError("source provenance metadata is missing")
    source_repo = source_metadata.get("repository")
    source_commit = source_metadata.get("commit")
    if not (source_repo == "fixture"
            or isinstance(source_repo, str)
            and re.fullmatch(r"https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", source_repo)):
        raise ValueError("source repository claim is invalid")
    if not isinstance(source_commit, str) or re.fullmatch(r"[0-9a-f]{40}", source_commit) is None:
        raise ValueError("source commit claim is invalid")
    source_artifacts = {
        "manifest_sha256": "synexia.source-manifest.tsv",
        "records_sha256": "synexia.input-records.tsv",
    }
    if source_metadata.get("relations_sha256") is not None:
        source_artifacts["relations_sha256"] = "synexia.input-relations.tsv"
    for field, filename in source_artifacts.items():
        expected = source_metadata.get(field)
        if not isinstance(expected, str) or re.fullmatch(r"[0-9a-f]{64}", expected) is None:
            raise ValueError("source input hash is missing: " + field)
        if filename not in outputs or digest((output / filename).read_bytes()) != expected:
            raise ValueError("source input hash mismatch: " + field)
    copied_manifest = output / "synexia.source-manifest.tsv"
    if not copied_manifest.is_file():
        raise ValueError("copied source manifest is missing")
    contract_sources = read_source_manifest_contract(copied_manifest)
    computed_payload_fields = {
        source_id: list(source["precompute_fields"])
        for source_id, source in sorted(contract_sources.items())
    }
    if source_metadata.get("precompute_fields") != computed_payload_fields:
        raise ValueError("source precompute fields do not match copied manifest")
    copied_field_map = output / "synexia.precompute-field-map.tsv"
    computed_field_types = (
        read_field_map_contract(copied_field_map)
        if copied_field_map.is_file() else {}
    )
    if source_metadata.get("precompute_field_types", {}) != computed_field_types:
        raise ValueError("source precompute types do not match copied field map")
    copied_identity_map = output / "synexia.langdex-identity-field-map.tsv"
    computed_identity_fields = (
        read_identity_map_contract(copied_identity_map)
        if copied_identity_map.is_file() else {}
    )
    if source_metadata.get("identity_fields", {}) != computed_identity_fields:
        raise ValueError("source identity fields do not match copied identity map")
    computed_identity_requirements = identity_requirements_contract(
        contract_sources, computed_identity_fields)
    if source_metadata.get("identity_requirements", {}) != computed_identity_requirements:
        raise ValueError("source identity coverage does not match copied inputs")

    field_map_digest = source_metadata.get("precompute_field_map_sha256")
    if field_map_digest is not None:
        if (not isinstance(field_map_digest, str) or re.fullmatch(r"[0-9a-f]{64}", field_map_digest) is None
                or "synexia.precompute-field-map.tsv" not in outputs
                or digest((output / "synexia.precompute-field-map.tsv").read_bytes()) != field_map_digest):
            raise ValueError("source precompute field-map hash mismatch")
    identity_map_digest = source_metadata.get("identity_field_map_sha256")
    if identity_map_digest is not None:
        if (not isinstance(identity_map_digest, str) or re.fullmatch(r"[0-9a-f]{64}", identity_map_digest) is None
                or "synexia.langdex-identity-field-map.tsv" not in outputs
                or digest((output / "synexia.langdex-identity-field-map.tsv").read_bytes()) != identity_map_digest):
            raise ValueError("source identity field-map hash mismatch")
    identity_fields = source_metadata.get("identity_fields", {})
    if not isinstance(identity_fields, dict):
        raise ValueError("source identity field requirements are invalid")
    for target_type, fields in identity_fields.items():
        if (not isinstance(target_type, str) or not re.fullmatch(r"M3LangDexPrecompute\.[A-Za-z0-9_]+", target_type)
                or not isinstance(fields, dict)
                or list(fields) != sorted(fields)
                or any(not isinstance(field, str) or re.fullmatch(r"[a-z][a-z0-9_]*", field) is None
                       or not isinstance(value, str) for field, value in fields.items())):
            raise ValueError("source identity field map is invalid")
    source_payload_fields = source_metadata.get("precompute_fields")
    if not isinstance(source_payload_fields, dict):
        raise ValueError("source precompute field requirements are missing")
    identity_requirements = source_metadata.get("identity_requirements", {})
    if not isinstance(identity_requirements, dict):
        raise ValueError("source identity coverage is invalid")
    for source_id, fields in identity_requirements.items():
        if (source_id not in source_payload_fields if isinstance(source_payload_fields, dict) else True):
            raise ValueError("source identity coverage references unknown source")
        if not isinstance(fields, list) or fields != sorted(set(fields)):
            raise ValueError("source identity coverage is not sorted")

    for source_id, fields in source_payload_fields.items():
        if (not isinstance(source_id, str) or not isinstance(fields, list)
                or any(not isinstance(field, str)
                       or re.fullmatch(r"[a-z][a-z0-9_]*", field) is None for field in fields)
                or fields != sorted(set(fields))):
            raise ValueError("invalid source precompute field requirements")
    source_field_types = manifest.get("source", {}).get("precompute_field_types", {})
    if not isinstance(source_field_types, dict) or any(
            not isinstance(field, str) or not isinstance(donor_type, str)
            or donor_type not in ALLOWED_DONOR_TYPES
            for field, donor_type in source_field_types.items()):
        raise ValueError("invalid source precompute field types")
    for source_id, required_fields in identity_requirements.items():
        if not set(required_fields).issubset(source_payload_fields[source_id]):
            raise ValueError("source identity fields are not admitted by precompute coverage")

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
                if donor_type is None or not _fits_donor_type(payload[field], donor_type):
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
    if not declared_family_files:
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
              "utf16_units": total_units}
    if family_stats["families"]:
        result.update({
            "family_sidecar_families": family_stats["families"],
            "family_sidecar_rows": family_stats["rows"],
        })
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