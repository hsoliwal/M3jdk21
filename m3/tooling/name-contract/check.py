#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only M3JDK21 naming, package and companion declaration gate.

This is a target-specific consumer of the canonical name-mapping.json and the
canonical Synexia recipe, not a replacement naming registry/rewrite engine.
"""
from __future__ import annotations

import hashlib
import importlib.util
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
COMPANION_FORMS = frozenset({"STATIC_OPERATIONS", "TYPED_VIEW"})
MODULE_NAME = re.compile(r"^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*$")

# Reuse the repository's public-name lexical helper; this is not Java attribution.
_PUBLIC_NAMES_SPEC = importlib.util.spec_from_file_location(
    "m3_public_names_for_name_contract", ROOT / "m3/compatibility/check_public_names.py")
_PUBLIC_NAMES = importlib.util.module_from_spec(_PUBLIC_NAMES_SPEC)
sys.modules[_PUBLIC_NAMES_SPEC.name] = _PUBLIC_NAMES
_PUBLIC_NAMES_SPEC.loader.exec_module(_PUBLIC_NAMES)
code_without_literals = _PUBLIC_NAMES.code_without_literals


def refuse(reason: str) -> None:
    raise ValueError(reason)


def git_blob(data: bytes) -> str:
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()


def local_file(root: Path, relative: str) -> Path:
    if not isinstance(relative, str) or not relative or "\\" in relative:
        refuse("invalid relative path")
    raw = Path(relative)
    if raw.is_absolute() or any(part in (".", "..") for part in raw.parts):
        refuse("outside-root path")
    path = (root / raw).resolve()
    if not path.is_relative_to(root.resolve()):
        refuse("outside-root path")
    return path


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


def public_type_chain(code: str, names: list[str]) -> None:
    """Check a top-level/nested declaration chain; Java compiler proof stays separate."""
    enclosing_kind = None
    for name in names:
        pattern = (r"(?<![\w$])(?P<modifiers>(?:(?:public|protected|private|abstract|final|"
                   r"static|sealed|non-sealed|strictfp)\s+)*)"
                   r"(?P<kind>@interface|class|interface|enum|record)\s+" + re.escape(name) + r"\b")
        declarations = [match for match in re.finditer(pattern, code)
                        if code[:match.start()].count("{") == code[:match.start()].count("}")]
        if len(declarations) != 1:
            refuse("missing or ambiguous public type declaration: " + name)
        declaration = declarations[0]
        modifiers = set(declaration.group("modifiers").split())
        if (modifiers & {"private", "protected"}
                or ("public" not in modifiers and enclosing_kind not in {"interface", "@interface"})):
            refuse("companion relation requires public type declaration: " + name)
        # Record components and annotations may contain braces inside parentheses.
        parentheses = 0
        opening = None
        for index in range(declaration.end(), len(code)):
            if code[index] == "(":
                parentheses += 1
            elif code[index] == ")":
                parentheses -= 1
            elif code[index] == "{" and parentheses == 0:
                opening = index
                break
        if opening is None:
            refuse("missing public type body: " + name)
        depth = 1
        closing = opening + 1
        while closing < len(code) and depth:
            depth += (code[closing] == "{") - (code[closing] == "}")
            closing += 1
        if depth:
            refuse("unbalanced public type body: " + name)
        code = code[opening + 1:closing - 1]
        enclosing_kind = declaration.group("kind")


def exported_artifact(root: Path, artifact: dict, symbol: str, *, companion: bool) -> None:
    """Verify source identity and module declarations without claiming runtime acceptance."""
    checked_fqn(symbol, "companion relation type")
    module = artifact.get("module")
    path = artifact.get("path")
    if not isinstance(module, str) or not MODULE_NAME.fullmatch(module):
        refuse("companion relation requires an exact named module")
    if companion and module == "java.base":
        refuse("public companion must not be owned by java.base")
    if (not isinstance(path, str) or not path.endswith(".java")
            or any(part in ("", ".", "..") for part in path.split("/"))):
        refuse("invalid companion relation source path")
    file = local_file(root, path)
    if not file.is_file():
        refuse("mapped companion relation source missing: " + path)
    raw = file.read_bytes()
    for field, actual, width in (("sha256", hashlib.sha256(raw).hexdigest(), 64),
                                 ("git_blob_sha1", git_blob(raw), 40)):
        pin = artifact.get(field)
        if (not isinstance(pin, str)
                or not re.fullmatch(r"[0-9a-f]{" + str(width) + r"}", pin)
                or pin != actual):
            refuse("companion relation source hash drift: " + path + " (" + field + ")")
    code = code_without_literals(raw.decode("utf-8"))
    packages = re.findall(r"(?m)^\s*package\s+([\w$.]+)\s*;", code)
    if len(packages) != 1 or not symbol.startswith(packages[0] + "."):
        refuse("companion relation declared package disagrees: " + path)
    package = packages[0]
    names = symbol[len(package) + 1:].split(".")
    suffix = package.replace(".", "/") + "/" + names[0] + ".java"
    if not path.endswith("/" + suffix):
        refuse("companion relation top-level source owner disagrees: " + symbol)
    source_root = path[:-len(suffix)]
    jdk_root = "src/" + module + "/share/classes/"
    if not companion or path.startswith("src/"):
        if source_root != jdk_root:
            refuse("companion relation source module/path disagrees: " + path)
    elif not (source_root.startswith("m3/")
              and source_root.endswith(("/src/main/java/", "/src/"))):
        refuse("companion source requires a mapped module source root: " + path)
    if companion and not (package == "com.m3" or package.startswith("com.m3.")):
        refuse("public companion must use the com.m3 namespace")
    if companion and ".internal." in symbol:
        refuse("public companion must not expose an internal namespace")
    public_type_chain(code, names)
    descriptor = local_file(root, source_root + "module-info.java")
    if not descriptor.is_file():
        refuse("companion relation module descriptor missing: " + source_root)
    module_code = code_without_literals(descriptor.read_text(encoding="utf-8"))
    declarations = re.findall(r"\b(?:open\s+)?module\s+([\w$.]+)\s*\{", module_code)
    if declarations != [module]:
        refuse("companion relation declared module disagrees: " + module)
    if companion and re.search(r"\bopen\s+module\s+", module_code):
        refuse("public companion module must not be open")
    if companion and re.search(r"\bopens\s+" + re.escape(package) + r"(?:\s*;|\s+to\b)", module_code):
        refuse("public companion package must not be opened: " + package)
    if not re.search(r"\bexports\s+" + re.escape(package) + r"\s*;", module_code):
        refuse("companion relation package is not publicly exported: " + package)


def validate_companion(root: Path, record: dict) -> str | None:
    if "companion_of" not in record:
        return None  # Existing internal-owner and migration records retain their contracts.
    relation = record["companion_of"]
    if not isinstance(relation, dict) or set(relation) != {"java_api", "target_symbol", "form"}:
        refuse("invalid companion_of relation")
    api = relation["java_api"]
    if not isinstance(api, dict) or set(api) != {"module", "symbol", "path"}:
        refuse("invalid companion java_api ownership")
    if not isinstance(relation["form"], str) or relation["form"] not in COMPANION_FORMS:
        refuse("unknown public companion form")
    target = relation["target_symbol"]
    checked_fqn(target, "public companion type")
    if not target.startswith("com.m3.") or target == api["symbol"]:
        refuse("public companion must use a distinct com.m3 type")
    sources, targets = record.get("sources"), record.get("targets")
    if not isinstance(sources, list) or not isinstance(targets, list):
        refuse("companion relation requires mapped source and target artifacts")
    source_matches = [item for item in sources if isinstance(item, dict)
                      and all(item.get(key) == value for key, value in api.items())]
    target_matches = [item for item in targets if isinstance(item, dict)
                      and item.get("symbol") == target]
    if len(source_matches) != 1 or len(target_matches) != 1:
        refuse("missing or ambiguous companion artifact ownership")
    exported_artifact(root, source_matches[0], api["symbol"], companion=False)
    exported_artifact(root, target_matches[0], target, companion=True)
    return target


def validate(root: Path) -> tuple[int, list[str]]:
    policy = local_file(root, DOC)
    if not policy.is_file():
        refuse("missing M3JDK21 naming and package contract")
    agents = local_file(root, "AGENTS.md").read_text(encoding="utf-8")
    if DOC not in agents:
        refuse("AGENTS.md lacks naming contract link")
    plan = json.loads(local_file(root, PLAN).read_text(encoding="utf-8"))
    if plan.get("schema") != SCHEMA or plan.get("authority") != MAP:
        refuse("plan uses wrong schema or a competing map")
    blob = plan.get("authority_blob")
    if not isinstance(blob, str) or not SHA.fullmatch(blob):
        refuse("invalid authoritative map Git blob")
    original = local_file(root, MAP).read_bytes()
    if git_blob(original) != blob:
        refuse("canonical naming map drift: re-pin and review changes")
    official = json.loads(original.decode("utf-8"))
    if official.get("schema") != 1:
        refuse("unexpected canonical naming map schema")
    records = official.get("migration", {}).get("records", [])
    if not isinstance(records, list):
        refuse("canonical migration records missing")
    ids: dict[str, dict] = {}
    companion_owners: dict[str, tuple] = {}
    companion_relations: set[tuple] = set()
    for item in records:
        if not isinstance(item, dict) or not isinstance(item.get("id"), str):
            refuse("invalid canonical capability mapping record")
        if item["id"] in ids:
            refuse("duplicate canonical mapping ID")
        ids[item["id"]] = item
        companion = validate_companion(root, item)
        if companion is not None:
            relation = item["companion_of"]
            api = relation["java_api"]
            target = next(target for target in item["targets"]
                          if isinstance(target, dict) and target.get("symbol") == companion)
            owner = tuple(target.get(field) for field in
                          ("module", "path", "sha256", "git_blob_sha1"))
            if companion in companion_owners and companion_owners[companion] != owner:
                refuse("conflicting public companion ownership: " + companion)
            companion_owners[companion] = owner
            key = (companion, api["module"], api["symbol"], api["path"], relation["form"])
            if key in companion_relations:
                refuse("duplicate public companion relation: " + companion)
            companion_relations.add(key)
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
        if "companion_of" in canonical and op["kind"] in {"JDK_BACKEND", "INTERNAL_MOVE"}:
            refuse("public companion is not an internal replacement owner")
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
