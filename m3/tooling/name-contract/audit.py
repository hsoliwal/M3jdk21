#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only, deterministic census of *existing canonical* M3JDK21 map targets.

This does not discover unmapped source files or declare implementation parity.
"""
from __future__ import annotations

import collections
import csv
import io
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
MAP = "m3/docs/name-mapping.json"
PLAN = "m3/tooling/name-contract/name-plan.json"
FQN = re.compile(r"^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+$")
JAVA_DECL = re.compile(r"\b(?:class|interface|enum|record|@interface)\s+([\w$]+)\b")
FIELDS = ("mapping_id", "record_state", "module", "target_path", "declared_symbol", "path_refs", "result")


def safe_path(root: Path, path: str) -> Path:
    if not isinstance(path, str) or not path or "\\" in path:
        raise ValueError("invalid target path")
    value = Path(path)
    if value.is_absolute() or any(p in (".", "..") for p in value.parts):
        raise ValueError("unsafe target path: " + path)
    resolved = (root / value).resolve()
    if not resolved.is_relative_to(root.resolve()):
        raise ValueError("unsafe target path: " + path)
    return resolved


def snapshot(root: Path) -> list[dict[str, str]]:
    from hashlib import sha1

    map_bytes = safe_path(root, MAP).read_bytes()
    pinned = json.loads(safe_path(root, PLAN).read_text(encoding="utf-8"))
    if pinned.get("authority") != MAP:
        raise ValueError("not the canonical target mapping authority")
    blob = sha1(b"blob " + str(len(map_bytes)).encode("ascii") + b"\0" + map_bytes).hexdigest()
    if pinned.get("authority_blob") != blob:
        raise ValueError("canonical name mapping has drifted")
    records = json.loads(map_bytes.decode("utf-8")).get("migration", {}).get("records")
    if not isinstance(records, list):
        raise ValueError("missing canonical migration records")
    flattened = []
    ids = set()
    for record in records:
        ident = record.get("id")
        if not isinstance(ident, str) or not ident or ident in ids:
            raise ValueError("invalid/duplicate canonical mapping id")
        ids.add(ident)
        for target in record.get("targets", []):
            if not isinstance(target, dict) or not target.get("path") or not target.get("symbol"):
                continue  # a review-only mapping without a concrete path is not a target.
            flattened.append((ident, str(record.get("status", "UNKNOWN")), str(target.get("module", "")), target["path"], target["symbol"]))
    counts = collections.Counter(path for _, _, _, path, _ in flattened)
    rows = []
    for ident, state, module, path, symbol in sorted(flattened):
        file = safe_path(root, path)
        if path.endswith(".java"):
            qualified = isinstance(symbol, str) and FQN.fullmatch(symbol)
            if not qualified and module != "jtreg":
                status = "REVIEW_NON_FQCN_JAVA"
            elif qualified and not path.endswith("/" + symbol.replace(".", "/") + ".java"):
                status = "REVIEW_PATH_SYMBOL"
            elif not file.is_file():
                status = "PENDING_MISSING" if state in ("pending", "blocked") else "REVIEW_MISSING"
            else:
                text = file.read_text(encoding="utf-8")
                if qualified:
                    pkg, simple = symbol.rsplit(".", 1)
                    declared_package = re.search(r"(?m)^\s*package\s+" + re.escape(pkg) + r"\s*;", text)
                    declared_type = any(m.group(1) == simple for m in JAVA_DECL.finditer(text))
                    status = "PRESENT_NAME_MATCH" if declared_package and declared_type else "REVIEW_DECLARATION"
                else:
                    status = "PRESENT_JTREG_SIMPLE_NAME" if JAVA_DECL.search(text) else "REVIEW_DECLARATION"
        elif path.endswith((".c", ".h", ".cpp", ".cc")):
            if file.is_file():
                status = "NATIVE_PRESENT_UNVERIFIED"
            elif state in ("pending", "blocked"):
                status = "PENDING_NATIVE_MISSING"
            else:
                status = "REVIEW_NATIVE_MISSING"
        else:
            if file.is_file():
                status = "OTHER_PRESENT_UNVERIFIED"
            elif state in ("pending", "blocked"):
                status = "PENDING_OTHER_MISSING"
            else:
                status = "REVIEW_OTHER_MISSING"
        rows.append(dict(zip(FIELDS, (ident, state, module, path, symbol, str(counts[path]), status))))
    return rows


def as_tsv(rows: list[dict[str, str]]) -> str:
    output = io.StringIO(newline="")
    writer = csv.DictWriter(output, fieldnames=FIELDS, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    return output.getvalue()


def main(argv: list[str]) -> int:
    if argv not in (["audit.py"], ["audit.py", "--strict"]):
        print("usage: audit.py [--strict]", file=sys.stderr)
        return 2
    try:
        rows = snapshot(ROOT)
        print(as_tsv(rows), end="")
        bad = [r for r in rows if r["result"].startswith("REVIEW_")]
        print("M3JDK21_MAP_CENSUS rows=" + str(len(rows)) + " distinct_paths="
              + str(len({r["source_path"] for r in rows})) + " attention=" + str(len(bad)), file=sys.stderr)
        if "--strict" in argv and bad:
            return 1
        return 0
    except (ValueError, OSError, UnicodeError, json.JSONDecodeError) as error:
        print("M3JDK21_MAP_CENSUS_REFUSED: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
