#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Search and validate the finite M3JDK21 utility-owner catalogue.

Uses Python's standard library. A sparse checkout can use the installed git
command to prove unchanged source membership in the catalogue's pinned base.
New and refined recipe owners must exist on disk. This validates the catalogue
and owner declarations; it is not a substitute for compilation or runtime tests.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path, PurePosixPath

CATALOGUE_PATH = "m3/docs/utilities/FAMILIES.tsv"
COLUMNS = (
    "category_id", "owner", "owner_kind", "component", "source_path",
    "source_kind", "status", "operations", "contract_notes", "baseline_git_blob",
)
KINDS = frozenset(("utility", "factory", "type-owner"))
SOURCE_KINDS = frozenset(("java", "generated-template"))
STATUSES = frozenset((
    "established", "refined-collections", "added-sorts", "added-utilities",
))
REQUIRES_DISK = frozenset((
    "refined-collections", "added-sorts", "added-utilities",
))
ADDED = frozenset(("added-sorts", "added-utilities"))
IDENTIFIER = r"[A-Za-z_$][A-Za-z0-9_$]*"
FQN = re.compile(r"(?:" + IDENTIFIER + r"\.)+" + IDENTIFIER + r"\Z")
CATEGORY = re.compile(r"[a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*)+\Z")
GIT_OID = re.compile(r"[0-9a-f]{40}\Z")
CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")
COMMENT = re.compile(r"/\*.*?\*/|//[^\r\n]*", re.DOTALL)


class CatalogueError(Exception):
    """A malformed catalogue or an unproved source owner."""


def default_root() -> Path:
    script = Path(__file__).resolve()
    if tuple(script.parts[-4:]) == ("m3", "verification", "utilities", "catalogue.py"):
        return script.parents[3]
    return Path.cwd()


def source_prefix(component: str) -> str:
    if component == "m3/collections":
        return "m3/collections/src/main/java/"
    if re.fullmatch(r"(?:java|jdk)(?:\.[a-z][a-z0-9_]*)+", component):
        return "src/" + component + "/share/classes/"
    raise CatalogueError("unknown component shape: " + component)


def validate_row(row: dict[str, str], line: int) -> None:
    label = "line " + str(line) + ": "
    for key in COLUMNS:
        value = row[key]
        if not value or value != value.strip() or CONTROL.search(value):
            raise CatalogueError(label + "invalid or empty " + key)
    if not CATEGORY.fullmatch(row["category_id"]):
        raise CatalogueError(label + "invalid stable category ID")
    if not FQN.fullmatch(row["owner"]):
        raise CatalogueError(label + "invalid owner FQN")
    if row["owner_kind"] not in KINDS:
        raise CatalogueError(label + "unknown owner kind")
    if row["source_kind"] not in SOURCE_KINDS:
        raise CatalogueError(label + "unknown source kind")
    if row["status"] not in STATUSES:
        raise CatalogueError(label + "unknown status")
    path = row["source_path"]
    pure = PurePosixPath(path)
    if pure.is_absolute() or ".." in pure.parts or str(pure) != path or "\\" in path:
        raise CatalogueError(label + "source path must be a normalized repository path")
    prefix = source_prefix(row["component"])
    owner_path = row["owner"].replace(".", "/")
    if row["source_kind"] == "java":
        expected = prefix + owner_path + ".java"
        if path != expected:
            raise CatalogueError(label + "owner/package/source path mismatch")
    else:
        package, simple = row["owner"].rsplit(".", 1)
        types = ("Byte", "Char", "Short", "Int", "Long", "Float", "Double")
        expected = prefix + package.replace(".", "/") + "/X-Buffer.java.template"
        if (row["component"] != "java.base" or package != "java.nio"
                or simple not in tuple(t + "Buffer" for t in types)
                or path != expected or row["status"] != "established"):
            raise CatalogueError(label + "unrecognized generated-buffer ownership")
    blob = row["baseline_git_blob"]
    if row["status"] in ADDED:
        if blob != "-":
            raise CatalogueError(label + "new recipe owner must have absent baseline blob")
    elif not GIT_OID.fullmatch(blob):
        raise CatalogueError(label + "existing owner needs its exact baseline blob")
    operations = row["operations"].split("; ")
    if any(not re.fullmatch(IDENTIFIER, operation) for operation in operations):
        raise CatalogueError(label + "operations must be semicolon-separated Java identifiers")
    if len(set(operations)) != len(operations):
        raise CatalogueError(label + "duplicate operation example")


def load_catalogue(path: Path) -> tuple[dict[str, str], list[dict[str, str]]]:
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeError) as failure:
        raise CatalogueError("cannot read catalogue: " + str(failure)) from failure
    metadata: dict[str, str] = {}
    records: list[tuple[int, list[str]]] = []
    for line_number, line in enumerate(lines, 1):
        if line.startswith("#"):
            comment = line[1:].strip()
            if "=" in comment:
                key, value = comment.split("=", 1)
                if key in metadata:
                    raise CatalogueError("duplicate metadata key: " + key)
                metadata[key] = value
            continue
        if not line.strip():
            continue
        fields = next(csv.reader([line], delimiter="\t", quoting=csv.QUOTE_NONE))
        records.append((line_number, fields))
    if metadata.get("schema_version") != "1":
        raise CatalogueError("catalogue requires schema_version=1")
    if not GIT_OID.fullmatch(metadata.get("baseline_revision", "")):
        raise CatalogueError("catalogue requires a full pinned baseline_revision")
    if not records or tuple(records[0][1]) != COLUMNS:
        raise CatalogueError("catalogue header does not match schema version 1")
    rows: list[dict[str, str]] = []
    categories: set[str] = set()
    owners: set[str] = set()
    java_paths: set[str] = set()
    for line_number, fields in records[1:]:
        if len(fields) != len(COLUMNS):
            raise CatalogueError("line " + str(line_number) + ": wrong field count")
        row = dict(zip(COLUMNS, fields))
        validate_row(row, line_number)
        if row["category_id"] in categories:
            raise CatalogueError("duplicate category ownership: " + row["category_id"])
        if row["owner"] in owners:
            raise CatalogueError("duplicate owner row: " + row["owner"])
        if row["source_kind"] == "java" and row["source_path"] in java_paths:
            raise CatalogueError("duplicate Java source ownership: " + row["source_path"])
        categories.add(row["category_id"])
        owners.add(row["owner"])
        if row["source_kind"] == "java":
            java_paths.add(row["source_path"])
        rows.append(row)
    try:
        declared = int(metadata["registered_families"])
    except (KeyError, ValueError) as failure:
        raise CatalogueError("catalogue requires registered_families") from failure
    if declared != len(rows) or declared == 0:
        raise CatalogueError("registered_families does not match the nonempty row set")
    return metadata, rows


def inspect_source(root: Path, row: dict[str, str]) -> dict[str, str] | None:
    source = root / row["source_path"]
    if not source.exists():
        return None
    if not source.is_file():
        raise CatalogueError("owner path is not a file: " + row["source_path"])
    try:
        resolved = source.resolve()
        resolved.relative_to(root)
        data = source.read_bytes()
        text = data.decode("utf-8")
    except (OSError, UnicodeError, ValueError) as failure:
        raise CatalogueError("cannot read owner within source root: "
                             + row["source_path"]) from failure
    code = COMMENT.sub("", text)
    package = re.search(r"(?m)^\s*package\s+([\w.]+)\s*;", code)
    expected_package, simple = row["owner"].rsplit(".", 1)
    if package is None or package.group(1) != expected_package:
        raise CatalogueError("source package does not match owner: " + row["owner"])
    if row["source_kind"] == "generated-template":
        code = code.replace("$Type$", simple[:-len("Buffer")])
    if re.search(r"\b(?:class|interface|enum|record)\s+"
                 + re.escape(simple) + r"\b", code) is None:
        raise CatalogueError("source declaration does not match owner: " + row["owner"])
    for operation in row["operations"].split("; "):
        if re.search(r"\b" + re.escape(operation) + r"\b", code) is None:
            raise CatalogueError("operation example absent from source: "
                                 + row["owner"] + "." + operation)
    return {
        "category_id": row["category_id"],
        "owner": row["owner"],
        "source_path": row["source_path"],
        "proof": "on-disk-source",
        "sha256": hashlib.sha256(data).hexdigest(),
    }


def git_inventory(root: Path, revision: str,
                  rows: list[dict[str, str]]) -> dict[str, str]:
    if not rows:
        return {}
    command = [
        "git", "-C", str(root), "--literal-pathspecs", "ls-tree", "-r", "-z",
        revision, "--",
    ] + sorted({row["source_path"] for row in rows})
    try:
        result = subprocess.run(command, capture_output=True, check=False)
    except OSError as failure:
        raise CatalogueError("sparse checkout needs git for pinned source inventory") from failure
    if result.returncode != 0:
        detail = result.stderr.decode("utf-8", errors="replace").strip()
        raise CatalogueError("cannot prove pinned Git source inventory: " + detail)
    inventory: dict[str, str] = {}
    for entry in result.stdout.split(b"\0"):
        if not entry:
            continue
        try:
            header, raw_path = entry.split(b"\t", 1)
            mode, kind, blob = header.decode("ascii").split()
            path = raw_path.decode("utf-8")
        except (ValueError, UnicodeError) as failure:
            raise CatalogueError("malformed git ls-tree result") from failure
        if kind != "blob" or mode not in ("100644", "100755"):
            raise CatalogueError("pinned owner is not a regular source blob: " + path)
        inventory[path] = blob
    return inventory


def verify(root: Path, metadata: dict[str, str],
           rows: list[dict[str, str]]) -> dict[str, object]:
    if not root.is_dir():
        raise CatalogueError("source root is not a directory: " + str(root))
    proofs: list[dict[str, str]] = []
    missing: list[dict[str, str]] = []
    for row in rows:
        proof = inspect_source(root, row)
        if proof is not None:
            proofs.append(proof)
        elif row["status"] in REQUIRES_DISK:
            raise CatalogueError("new/refined recipe owner must exist on disk: "
                                 + row["source_path"])
        else:
            missing.append(row)
    inventory = git_inventory(root, metadata["baseline_revision"], missing)
    for row in missing:
        blob = inventory.get(row["source_path"])
        if blob is None:
            raise CatalogueError("owner absent from pinned Git tree: " + row["source_path"])
        if blob != row["baseline_git_blob"]:
            raise CatalogueError("pinned source blob differs from catalogue: "
                                 + row["source_path"])
        proofs.append({
            "category_id": row["category_id"],
            "owner": row["owner"],
            "source_path": row["source_path"],
            "proof": "pinned-git-tree-inventory",
            "git_blob": blob,
        })
    proofs.sort(key=lambda row: row["category_id"])
    return {
        "result": "PASS",
        "schema_version": 1,
        "baseline_revision": metadata["baseline_revision"],
        "families": len(rows),
        "on_disk_sources": len(rows) - len(missing),
        "pinned_git_inventory_sources": len(missing),
        "scope": "catalogue schema, unique ownership and source presence/declarations; not compilation or runtime proof",
        "owners": proofs,
    }


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    actions = result.add_subparsers(dest="command", required=True)
    for name, help_text in (
        ("verify", "validate schema, unique owners and repository source proof"),
        ("search", "case-insensitive search across all catalogue columns"),
    ):
        action = actions.add_parser(name, help=help_text)
        if name == "search":
            action.add_argument("query", help="substring; use an empty string to list all families")
        action.add_argument("--source-root", type=Path, default=default_root())
        action.add_argument("--catalogue", type=Path,
                            help="override the default SOURCE_ROOT/" + CATALOGUE_PATH)
        action.add_argument("--json", action="store_true", help="emit machine-readable JSON")
    return result


def main(argv: list[str] | None = None) -> int:
    arguments = parser().parse_args(argv)
    root = arguments.source_root.expanduser().resolve()
    path = arguments.catalogue if arguments.catalogue is not None else root / CATALOGUE_PATH
    try:
        metadata, rows = load_catalogue(path)
        if arguments.command == "verify":
            result = verify(root, metadata, rows)
            if arguments.json:
                print(json.dumps(result, indent=2, sort_keys=True))
            else:
                print("PASS utility catalogue: " + str(result["families"]) + " families; "
                      + str(result["on_disk_sources"]) + " on-disk sources; "
                      + str(result["pinned_git_inventory_sources"])
                      + " pinned Git inventory sources")
                print("Scope: " + str(result["scope"]))
        else:
            needle = arguments.query.casefold()
            selected = [row for row in rows
                        if needle in "\t".join(row.values()).casefold()]
            if arguments.json:
                print(json.dumps({"query": arguments.query, "matches": len(selected),
                                  "families": selected}, indent=2, sort_keys=True))
            else:
                columns = ("category_id", "owner_kind", "owner", "component", "status", "operations")
                print("\t".join(columns))
                for row in selected:
                    print("\t".join(row[column] for column in columns))
        return 0
    except CatalogueError as failure:
        print("ERROR utility catalogue: " + str(failure), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
