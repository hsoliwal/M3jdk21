#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""FDP_DELEGATES_TO_JDK v1: FastDoubleParser 1.0.90 double/float facades delegate to java.base.

Deterministic recipe over the pinned upstream bytes (the donor's SOURCES_MANIFEST.tsv). It is re-runnable
to a fixed point and records everything through the donor's existing overlay/custody files:

- each original stays byte-identical under upstream-excluded/ (kind unmodified-production-superseded-by-m3-overlay),
- the overlay is written under src/main/java (kind m3-overlay-source; base, recipe and result pinned),
- OVERLAY_SOURCES.json lists the overlays under m3_overlays, PROVENANCE.json pins every byte,
- the jackson module pack SOURCE_LOCK.json is relocked, keeping the previous lock as its predecessor.

Only the offset/length parse methods change (the convenience overloads already call them). The
BigDecimal/BigInteger parsers and the JSON parsers stay upstream bytes.

  python fdp_delegate_overlay.py            apply (writes only what differs)
  python fdp_delegate_overlay.py --check    write nothing; exit 1 when any target differs
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True

REPO = Path(__file__).resolve().parents[4]
TOOL_REL = "m3/tooling/fdp-delegate/tools/fdp_delegate_overlay.py"
DONOR_REL = "m3/vendor/third_party/fastdoubleparser-1.0.90"
PACKAGE_PATH = "ch/randelshofer/fastdoubleparser"
SOURCE_ROOT = "src/main/java"
RETAINED_ROOT = "upstream-excluded"
PACK_REL = "m3/tooling/module-packs/jackson"
PACK_DONOR_KEY = "fastdoubleparser-1.0.90"
PREDECESSOR_REL = "qualification/fdp-delegate/SOURCE_LOCK_PREDECESSOR.json"
BASE_REVISION = "52fb81c483c74448a12eecb63aa9465e42cdb82c"
OVERLAY_ID = "FDP_DELEGATES_TO_JDK v1"
OVERLAY_KIND = "m3-overlay-source"
RETAINED_KIND = "unmodified-production-superseded-by-m3-overlay"
ORIGINAL_KIND = "unmodified-production"
OVERLAY_RECORD_KEY = "m3_overlays"
OVERLAY_RECORD_FILE = "OVERLAY_SOURCES.json"
OVERLAY_RECORD_UPSTREAM = "fetch_overlays.py; m3_overlays: " + TOOL_REL
MODIFICATIONS = ("M3 overlays " + OVERLAY_ID + ": JavaDoubleParser and JavaFloatParser delegate their parse "
                 "methods to java.base Double.parseDouble/Float.parseFloat (recipe " + TOOL_REL + "; the "
                 "unchanged upstream files are retained under upstream-excluded/). No other upstream source "
                 "changes; Maven descriptor and provenance files are M3jdk21 composition (Apache-2.0) and do "
                 "not relicense the donor.")
ENCODING = "utf-8"
EXIT_DIFFERS = 1

PACKAGE_LINE = "package ch.randelshofer.fastdoubleparser;\n"
HEADER_END = " */\n" + PACKAGE_LINE
IMPORT_BLOCK = "\nimport java.nio.charset.StandardCharsets;\n"
NOTICE = ("// M3JDK overlay " + OVERLAY_ID + " (" + TOOL_REL + "):\n"
          "// the parse methods delegate to java.base, whose M3JDK {jdk} runs this library's algorithm\n"
          "// (absorbed as jdk.internal.math.M3*). The unchanged upstream file is retained under upstream-excluded/.\n"
          "//FLLDCIM: M3 - one number parser for Jackson and the JDK\n")
FIELD = "    private static final {kind}{source} {name} = new {kind}{source}();\n\n"
FIELD_SOURCES = (("ByteArray", "BYTE_ARRAY_PARSER"), ("CharArray", "CHAR_ARRAY_PARSER"),
                 ("CharSequence", "CHAR_SEQUENCE_PARSER"))
BODY = ("        long bitPattern = {name}.parseFloatingPointLiteral(str, offset, length);\n"
        "        return {bits};\n")
DELEGATE = "        AbstractNumberParser.checkBounds({size}, offset, length);\n        return {jdk}({text});\n"
DELEGATIONS = (("CHAR_SEQUENCE_PARSER", "str.length()", "str.subSequence(offset, offset + length).toString()"),
               ("BYTE_ARRAY_PARSER", "str.length", "new String(str, offset, length, StandardCharsets.ISO_8859_1)"),
               ("CHAR_ARRAY_PARSER", "str.length", "new String(str, offset, length)"))
TARGETS = {
    "JavaDoubleParser": {"bits_kind": "JavaDoubleBitsFrom", "bits": "Double.longBitsToDouble(bitPattern)",
                         "jdk": "Double.parseDouble"},
    "JavaFloatParser": {"bits_kind": "JavaFloatBitsFrom", "bits": "Float.intBitsToFloat((int) bitPattern)",
                        "jdk": "Float.parseFloat"},
}


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise ValueError(f"{label}: anchor matched {count} times, expected 1")
    return text.replace(old, new)


def overlay(original: bytes, upstream: str) -> bytes:
    """The recipe: pinned upstream bytes of one target facade -> its M3 overlay bytes."""
    name = upstream.rsplit("/", 1)[-1].removesuffix(".java")
    spec = TARGETS[name]
    text = original.decode(ENCODING)
    text = replace_once(text, HEADER_END, " */\n" + NOTICE.format(jdk=spec["jdk"]) + PACKAGE_LINE + IMPORT_BLOCK,
                        name + " header")
    for source, field in FIELD_SOURCES:
        text = replace_once(text, FIELD.format(kind=spec["bits_kind"], source=source, name=field), "",
                            name + " field " + field)
    for field, size, argument in DELEGATIONS:
        text = replace_once(text, BODY.format(name=field, bits=spec["bits"]),
                            DELEGATE.format(size=size, jdk=spec["jdk"], text=argument), name + " body " + field)
    return text.encode(ENCODING)


def dump_json(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode(ENCODING)


def manifest_pins(donor: Path) -> dict[str, str]:
    rows = csv.DictReader(io.StringIO((donor / "SOURCES_MANIFEST.tsv").read_text(encoding=ENCODING)), delimiter="\t")
    return {row["path"]: row["sha256"] for row in rows}


def plan_overlays(donor: Path) -> list[dict]:
    pins = manifest_pins(donor)
    tool_sha = sha256((REPO / TOOL_REL).read_bytes())
    plans = []
    for name, spec in TARGETS.items():
        upstream = f"{PACKAGE_PATH}/{name}.java"
        source_rel = f"{DONOR_REL}/{SOURCE_ROOT}/{upstream}"
        retained_rel = f"{DONOR_REL}/{RETAINED_ROOT}/{upstream}"
        holder = REPO / (retained_rel if (REPO / retained_rel).is_file() else source_rel)
        original = holder.read_bytes()
        if sha256(original) != pins[upstream]:
            raise ValueError("Original is not the pinned upstream byte sequence: " + str(holder))
        result = overlay(original, upstream)
        plans.append({"class": f"{PACKAGE_PATH}/{name}", "upstream": upstream, "original": original,
                      "result": result, "source_rel": source_rel, "retained_rel": retained_rel,
                      "jdk": "java.lang." + spec["jdk"], "tool_sha": tool_sha})
    return plans


def overlay_entry(plan: dict) -> dict:
    return {"base_repository": plan["retained_rel"], "base_sha256": sha256(plan["original"]),
            "kind": OVERLAY_KIND, "overlay": OVERLAY_ID, "recipe": TOOL_REL, "recipe_sha256": plan["tool_sha"],
            "repository": plan["source_rel"], "sha256": sha256(plan["result"]), "upstream": plan["upstream"]}


def retained_entry(plan: dict) -> dict:
    return {"kind": RETAINED_KIND, "repository": plan["retained_rel"], "sha256": sha256(plan["original"]),
            "upstream": plan["upstream"]}


def overlay_record(plan: dict) -> dict:
    record = overlay_entry(plan)
    record.pop("kind")
    record.update({"class": plan["class"], "delegates_to": plan["jdk"], "release": "base"})
    return record


def desired_records(donor: Path, plans: list[dict]) -> tuple[bytes, bytes]:
    record = json.loads((donor / OVERLAY_RECORD_FILE).read_text(encoding=ENCODING))
    record[OVERLAY_RECORD_KEY] = [overlay_record(plan) for plan in plans]
    record_bytes = dump_json(record)
    provenance = json.loads((donor / "PROVENANCE.json").read_text(encoding=ENCODING))
    by_source = {plan["source_rel"]: plan for plan in plans}
    retained = {plan["retained_rel"] for plan in plans}
    files = []
    for item in provenance["files"]:
        if item["repository"] in by_source:
            files.append(overlay_entry(by_source[item["repository"]]))
        elif item["repository"] in retained:
            continue
        else:
            if item["repository"] == f"{DONOR_REL}/{OVERLAY_RECORD_FILE}":
                item = dict(item, sha256=sha256(record_bytes), upstream=OVERLAY_RECORD_UPSTREAM)
            files.append(item)
    files.extend(retained_entry(plan) for plan in plans)
    provenance["files"] = files
    provenance["modifications"] = MODIFICATIONS
    return record_bytes, dump_json(provenance)


def tree_digest(root: Path) -> tuple[str, int]:
    """The jackson pack's own digest (build.py tree_digest), so the lock matches its check_donors."""
    spec = importlib.util.spec_from_file_location("m3_jackson_pack_build", REPO / PACK_REL / "build.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.tree_digest(root)


def desired_lock(donor: Path, pack: Path) -> tuple[bytes, bytes]:
    predecessor = pack / PREDECESSOR_REL
    previous = predecessor.read_bytes() if predecessor.is_file() else (pack / "SOURCE_LOCK.json").read_bytes()
    lock = json.loads((pack / "SOURCE_LOCK.json").read_text(encoding=ENCODING))
    tree, count = tree_digest(donor)
    lock["donors"][PACK_DONOR_KEY].update({"file_count": count, "tree_sha256": tree,
                                           "provenance_sha256": sha256((donor / "PROVENANCE.json").read_bytes())})
    lock.update({"predecessor_path": PREDECESSOR_REL, "predecessor_sha256": sha256(previous),
                 "target_base_revision": BASE_REVISION})
    return previous, dump_json(lock)


def reconcile(path: Path, data: bytes, check: bool, changed: list[str]) -> None:
    if path.is_file() and path.read_bytes() == data:
        return
    changed.append(path.relative_to(REPO).as_posix())
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when any target differs")
    args = parser.parse_args(argv)
    donor, pack, changed = REPO / DONOR_REL, REPO / PACK_REL, []
    plans = plan_overlays(donor)
    for plan in plans:
        reconcile(REPO / plan["retained_rel"], plan["original"], args.check, changed)
        reconcile(REPO / plan["source_rel"], plan["result"], args.check, changed)
    record_bytes, provenance_bytes = desired_records(donor, plans)
    reconcile(donor / OVERLAY_RECORD_FILE, record_bytes, args.check, changed)
    reconcile(donor / "PROVENANCE.json", provenance_bytes, args.check, changed)
    previous, lock_bytes = desired_lock(donor, pack)
    reconcile(pack / PREDECESSOR_REL, previous, args.check, changed)
    reconcile(pack / "SOURCE_LOCK.json", lock_bytes, args.check, changed)
    print(json.dumps({"overlay": OVERLAY_ID, "mode": "check" if args.check else "apply",
                      "changed": changed, "fixed_point": not changed}, sort_keys=True))
    return EXIT_DIFFERS if args.check and changed else 0


if __name__ == "__main__":
    raise SystemExit(main())
