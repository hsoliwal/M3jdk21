#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only M3JDK21 naming and package receiving gate.

This is a target-specific consumer of the canonical name-mapping.json and the
canonical Synexia recipe, not a replacement naming registry/rewrite engine.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
PLAN = "m3/tooling/name-contract/name-plan.json"
MAP = "m3/docs/name-mapping.json"
DOC = "m3/docs/M3JDK21_NAME_AND_PACKAGE_CONTRACT.md"
SCHEMA = "M3JDK21_NAME_CONTRACT_PLAN_V1"
SHA = re.compile(r"^[0-9a-f]{40}$")
FQN = re.compile(r"^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+$")
KINDS = frozenset({"FORMAT_ONLY", "INTERNAL_MOVE", "JDK_BACKEND"})
STATUSES = frozenset({"QUEUED"})
INTERNAL_PREFIXES = ("jdk.internal.", "com.m3.", "com.sun.tools.javac.m3.", "java.lang.M3")


def refuse(reason: str) -> None:
    raise ValueError(reason)


def git_blob(data: bytes) -> str:
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()


def unique_keys(pairs: list[tuple[str, object]]) -> dict:
    """All mapping/plan JSON objects must have one unambiguous value per key."""
    result = {}
    for key, value in pairs:
        if key in result:
            refuse("duplicate JSON object key: " + key)
        result[key] = value
    return result


def local_file(root: Path, relative: str) -> Path:
    if (not isinstance(relative, str) or not relative or "\\" in relative
            or "\x00" in relative or re.match(r"^[A-Za-z]:", relative)):
        refuse("invalid relative path")
    components = relative.split("/")
    raw = Path(relative)
    if raw.is_absolute() or any(part in ("", ".", "..") for part in components):
        refuse("noncanonical outside-root path")
    base = root.resolve()
    candidate = base
    for part in components:
        candidate = candidate / part
        if candidate.is_symlink():
            refuse("symlink target path forbidden: " + relative)
    resolved = candidate.resolve()
    if not resolved.is_relative_to(base):
        refuse("outside-root path")
    return resolved


def checked_fqn(value: str, field: str) -> None:
    if not isinstance(value, str) or not FQN.fullmatch(value):
        refuse("invalid " + field)


def find_mapped_target(root: Path, record: dict, name: str) -> str:
    for item in record.get("targets", []):
        if not isinstance(item, dict) or item.get("symbol") != name:
            continue
        path = item.get("path")
        if not isinstance(path, str) or not path.startswith(("src/", "m3/")):
            refuse("invalid canonical target path")
        end = "/" + name.replace(".", "/") + ".java"
        if not path.endswith(end):
            refuse("canonical target path and type disagree: " + name)
        file = local_file(root, path)
        if not file.is_file():
            refuse("mapped source file missing: " + path)
        content = file.read_text(encoding="utf-8")
        package = name.rsplit(".", 1)[0]
        simple = name.rsplit(".", 1)[1]
        if not re.search(r"(?m)^\s*package\s+" + re.escape(package) + r"\s*;", content):
            refuse("declared package disagrees: " + path)
        if not re.search(r"\b(class|interface|enum|record)\s+" + re.escape(simple) + r"\b", content):
            refuse("declared type disagrees: " + path)
        return path
    refuse("candidate not recorded as a canonical M3JDK21 target: " + name)


def validate(root: Path) -> tuple[int, list[str]]:
    policy = local_file(root, DOC)
    if not policy.is_file():
        refuse("missing M3JDK21 naming and package contract")
    agents = local_file(root, "AGENTS.md").read_text(encoding="utf-8")
    if DOC not in agents:
        refuse("AGENTS.md lacks naming contract link")
    plan = json.loads(local_file(root, PLAN).read_text(encoding="utf-8"),
                      object_pairs_hook=unique_keys)
    if plan.get("schema") != SCHEMA or plan.get("authority") != MAP:
        refuse("plan uses wrong schema or a competing map")
    blob = plan.get("authority_blob")
    if not isinstance(blob, str) or not SHA.fullmatch(blob):
        refuse("invalid authoritative map Git blob")
    original = local_file(root, MAP).read_bytes()
    if git_blob(original) != blob:
        refuse("canonical naming map drift: re-pin and review changes")
    official = json.loads(original.decode("utf-8"),
                          object_pairs_hook=unique_keys)
    if official.get("schema") != 1:
        refuse("unexpected canonical naming map schema")
    records = official.get("migration", {}).get("records", [])
    if not isinstance(records, list):
        refuse("canonical migration records missing")
    ids: dict[str, dict] = {}
    for item in records:
        if not isinstance(item, dict) or not isinstance(item.get("id"), str):
            refuse("invalid canonical capability mapping record")
        if item["id"] in ids:
            refuse("duplicate canonical mapping ID")
        ids[item["id"]] = item
    operations = plan.get("operations")
    if not isinstance(operations, list) or not operations:
        refuse("empty naming plan: distinguish queued candidates from accepted changes")
    seen = set()
    states = []
    for op in operations:
        if not isinstance(op, dict) or op.get("mapping_id") not in ids:
            refuse("unknown capability mapping ID")
        if op.get("kind") not in KINDS:
            refuse("unrecognized naming operation")
        if op.get("state") not in STATUSES:
            refuse("target receiver refuses unproven active changes; run pinned Synexia recipe first")
        if "proofs" in op or "installer_crate" in op:
            refuse("queued row falsely carries promotion material")
        reason = op.get("reason")
        if not isinstance(reason, str) or len(reason.strip()) < 20:
            refuse("queued row requires a concrete open-gate explanation")
        old, new = op.get("before_type"), op.get("after_type")
        checked_fqn(old, "source type")
        checked_fqn(new, "target type")
        entry = (op["mapping_id"], old, new, op["kind"])
        if entry in seen:
            refuse("duplicate naming operation")
        seen.add(entry)
        canonical = ids[op["mapping_id"]]
        find_mapped_target(root, canonical, old)
        if not old.startswith(INTERNAL_PREFIXES):
            refuse("candidate must be an internal M3JDK21 owner")
        if op["kind"] == "JDK_BACKEND":
            if new.startswith("java.lang.M3") or not new.startswith(("java.", "javax.")):
                refuse("public JDK backend target must retain canonical JDK class name")
            if new.rsplit(".", 1)[1].startswith("M3"):
                refuse("M3 name cannot become the public JDK class")
            jdk_path = "src/java.base/share/classes/" + new.replace(".", "/") + ".java"
            # This queue only records java.base examples. Other modules require a
            # reviewed mapping/receiver update, not guessing a module location.
            if not local_file(root, jdk_path).is_file():
                refuse("public JDK target path requires explicit owner/module review")
        elif op["kind"] == "INTERNAL_MOVE":
            if old == new or not new.startswith(INTERNAL_PREFIXES):
                refuse("internal move must name a different internal M3 owner")
        else:
            if old != new:
                refuse("format-only must retain the exact class name")
        states.append(op["mapping_id"])
    return len(operations), states


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check.py", file=sys.stderr)
        return 2
    try:
        count, ids = validate(ROOT)
    except (ValueError, OSError, UnicodeError, json.JSONDecodeError) as err:
        print("M3JDK21_NAME_RECEIVER_REFUSED: " + str(err), file=sys.stderr)
        return 2
    print("M3JDK21_NAME_RECEIVER_PASS queued=" + str(count)
          + " mappingIDs=" + ",".join(ids)
          + " qualifiedJdkPromotions=0 productSourceEdits=0")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
