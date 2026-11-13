#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""DONOR_OVERLAY v2: the custody shared by the recipes that retarget vendored donor sources to java.base.

A recipe module supplies:
  RECIPE_ID, TOOL_REL      its id and repository-relative path (each overlay record pins the path and its sha256)
  DONORS                   donor directory names under m3/vendor/third_party
  TARGETS                  upstream paths, relative to src/main/java, of the files it overlays
  overlay(original, upstream) -> bytes
                           the overlay; verify_source.py imports the recipe and calls it, so it lives in the recipe
  POM_TEXT, README_TEXT    (old, new) replacements; README_TEXT may name {donor} ("<artifact> <version>")
  MODIFICATIONS            the PROVENANCE.json "modifications" text
  RELOCATED                {donor: (relocated directory, relocations, (old path, new path))}; relocations is one
                           (old package, new package) pair or a tuple of them, applied in order
Optional (v2):
  REMOVED                  target class names whose source goes (the original is still retained and pinned)
  POM_EDITS                compiled regexes whose matches are removed from the pom, before POM_TEXT
  POM_FORBIDDEN            strings the edited pom must not contain
  provenance_update(p)     edits the donor's PROVENANCE.json dict after the overlay records are set
  RELOCATED_EDITS          {donor: ((path under the relocated source root, old, new), ...)}
  after(check, changed, custody)
                           runs once after every donor; custody holds this module's names (reconcile, sha256,
                           dump_json, text_of, REPO, ...)

run(recipe) keeps, for each donor: every upstream original byte-identical under upstream-excluded/ (kind
unmodified-production-superseded-by-m3-overlay); the overlay under src/main/java (kind m3-overlay-source, pinned with
its base and the recipe); OVERLAY_SOURCES.json; PROVENANCE.json; and verify_source.py, which re-derives every overlay
through the recipe. The verify_source.py template is the donors' own, with two fixes: the repository root, and
`revision` falling back to `version`. A donor's relocated copy (lib-intake) gets the name-mapped overlay and copies of
PROVENANCE.json and THIRD_PARTY_NOTICES.md. Re-running changes nothing; --check writes nothing and exits 1 when any
target differs. v2 adds the optional hooks above; JACKSON_NUMBERS_TO_JDK and CHECKSUMS_TO_JDK moved onto it, so
FDP_DELEGATES_TO_JDK (retired) is the only recipe with its own copy.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import re
import types

REPO = Path(__file__).resolve().parents[6]
VENDOR = "m3/vendor/third_party"
SOURCE_ROOT = "src/main/java"
RETAINED_ROOT = "upstream-excluded"
OVERLAY_KIND = "m3-overlay-source"
RETAINED_KIND = "unmodified-production-superseded-by-m3-overlay"
RECORD_KIND = "m3-overlay-record"
RECORD_FILE = "OVERLAY_SOURCES.json"
RELOCATED_RECORDS = ("PROVENANCE.json", "THIRD_PARTY_NOTICES.md")
ENCODING = "utf-8"
EXIT_DIFFERS = 1
MANIFEST_CHECK_RE = re.compile(r'if "([^"\n]+)" not in jar\.read\("META-INF/MANIFEST\.MF"\)')

VERIFY_SOURCE = '''#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Check the pinned donor, its recorded M3 overlays and its optional built legal resources."""
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import zipfile

# Kinds compiled from src/main/java: sources-jar bytes and recorded M3 overlays.
PRODUCTION_KINDS = ("unmodified-production", "m3-overlay-source")
OVERLAY_KIND = "m3-overlay-source"
RETAINED_KIND = "unmodified-production-superseded-by-m3-overlay"


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def check_overlays(root, owner, manifest):
    """Each overlay is the pinned recipe's output of a retained, pinned upstream original."""
    files = {item["repository"]: item for item in manifest["files"]}
    overlays = [item for item in manifest["files"] if item["kind"] == OVERLAY_KIND]
    recorded = json.loads((owner / "OVERLAY_SOURCES.json").read_text(encoding="utf-8")).get("m3_overlays", [])
    if sorted((item["repository"], item["sha256"]) for item in overlays) != sorted(
            (item["repository"], item["sha256"]) for item in recorded):
        raise SystemExit("M3 overlays differ between PROVENANCE.json and OVERLAY_SOURCES.json")
    sys.dont_write_bytecode = True
    for item in overlays:
        base = files.get(item["base_repository"])
        if base is None or base["kind"] != RETAINED_KIND or base["sha256"] != item["base_sha256"]:
            raise SystemExit("M3 overlay base is not a retained pinned original: " + item["repository"])
        recipe = root / item["recipe"]
        if sha256(recipe) != item["recipe_sha256"]:
            raise SystemExit("M3 overlay recipe changed: " + item["recipe"])
        spec = importlib.util.spec_from_file_location("m3_donor_overlay_recipe", recipe)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        if module.overlay((root / base["repository"]).read_bytes(), item["upstream"]) != (
                root / item["repository"]).read_bytes():
            raise SystemExit("M3 overlay is not its recipe's output: " + item["repository"])
    return len(overlays)


def main():
    owner = Path(__file__).resolve().parent
    root = owner.parents[3]  # repository root: the pins are m3/vendor/third_party/<donor>/...
    manifest = json.loads((owner / "PROVENANCE.json").read_text(encoding="utf-8"))
    for item in manifest["files"]:
        source = root / item["repository"]
        actual = hashlib.sha256(source.read_bytes()).hexdigest()
        if actual != item["sha256"]:
            raise SystemExit("Donor bytes changed: " + item["repository"])
    expected = {
        root / item["repository"]
        for item in manifest["files"]
        if item["kind"] in PRODUCTION_KINDS
    }
    if set((owner / "src/main/java").rglob("*.java")) != expected:
        raise SystemExit("Production donor inventory changed")
    overlays = check_overlays(root, owner, manifest)
    receipt = {"revision": manifest.get("revision", manifest["version"]), "exact_files": len(manifest["files"]),
               "exact_production_files": len(expected), "m3_overlays": overlays}
    if len(sys.argv) > 1:
        artifact = Path(sys.argv[1])
        with zipfile.ZipFile(artifact) as jar:
            # The pom packages LICENSE only when the donor directory has one.
            for name in [n for n in ("LICENSE", "THIRD_PARTY_NOTICES.md", "PROVENANCE.json") if (owner / n).is_file()]:
                if jar.read("META-INF/third-party/{donor}/" + name) != (owner / name).read_bytes():
                    raise SystemExit("Packaged legal/provenance bytes changed: " + name)
            if "{manifest_check}" not in jar.read("META-INF/MANIFEST.MF").decode():
                raise SystemExit("Automatic module identity changed")
        receipt["artifact_sha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
'''


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def dump_json(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode(ENCODING)


def text_of(path: Path) -> str:
    return path.read_bytes().decode(ENCODING)


def manifest_pins(donor: Path) -> dict[str, str]:
    rows = csv.DictReader(io.StringIO(text_of(donor / "SOURCES_MANIFEST.tsv")), delimiter="\t")
    return {row["path"]: row["sha256"] for row in rows}


def plan_donor(recipe, name: str) -> list[dict]:
    """One plan per target: the pinned upstream original and the recipe's overlay of it."""
    donor_rel = f"{VENDOR}/{name}"
    pins, tool_sha, plans = manifest_pins(REPO / donor_rel), sha256((REPO / recipe.TOOL_REL).read_bytes()), []
    for upstream in recipe.TARGETS:
        source_rel = f"{donor_rel}/{SOURCE_ROOT}/{upstream}"
        retained_rel = f"{donor_rel}/{RETAINED_ROOT}/{upstream}"
        holder = REPO / (retained_rel if (REPO / retained_rel).is_file() else source_rel)
        original = holder.read_bytes()
        if sha256(original) != pins[upstream]:
            raise ValueError("Original is not the pinned upstream byte sequence: " + str(holder))
        removed = upstream.rsplit("/", 1)[-1].removesuffix(".java") in getattr(recipe, "REMOVED", ())
        plans.append({"upstream": upstream, "original": original,
                      "result": None if removed else recipe.overlay(original, upstream),
                      "source_rel": source_rel, "retained_rel": retained_rel, "tool_sha": tool_sha})
    return plans


def overlay_entry(recipe, plan: dict) -> dict:
    return {"base_repository": plan["retained_rel"], "base_sha256": sha256(plan["original"]),
            "kind": OVERLAY_KIND, "overlay": recipe.RECIPE_ID, "recipe": recipe.TOOL_REL,
            "recipe_sha256": plan["tool_sha"], "repository": plan["source_rel"], "sha256": sha256(plan["result"]),
            "upstream": plan["upstream"]}


def desired_records(recipe, name: str, plans: list[dict]) -> tuple[bytes, bytes]:
    donor_rel = f"{VENDOR}/{name}"
    kept = [plan for plan in plans if plan["result"] is not None]
    records = [{key: value for key, value in overlay_entry(recipe, plan).items() if key != "kind"} for plan in kept]
    record_bytes = dump_json({"donor": name, "m3_overlays": records, "overlay": recipe.RECIPE_ID,
                              "recipe": recipe.TOOL_REL})
    provenance = json.loads(text_of(REPO / donor_rel / "PROVENANCE.json"))
    by_source = {plan["source_rel"]: plan for plan in kept}
    ours = ({plan["retained_rel"] for plan in plans} | {f"{donor_rel}/{RECORD_FILE}"}
            | {plan["source_rel"] for plan in plans if plan["result"] is None})
    files = [overlay_entry(recipe, by_source[item["repository"]]) if item["repository"] in by_source else item
             for item in provenance["files"] if item["repository"] not in ours]
    files.extend({"kind": RETAINED_KIND, "repository": plan["retained_rel"], "sha256": sha256(plan["original"]),
                  "upstream": plan["upstream"]} for plan in plans)
    files.append({"kind": RECORD_KIND, "repository": f"{donor_rel}/{RECORD_FILE}", "sha256": sha256(record_bytes),
                  "upstream": recipe.TOOL_REL})
    provenance.update({"files": files, "modifications": recipe.MODIFICATIONS})
    if hasattr(recipe, "provenance_update"):
        recipe.provenance_update(provenance)
    return record_bytes, dump_json(provenance)


def verify_source(donor: Path, name: str) -> bytes:
    """The template for this donor; the manifest identity check is the one the donor's script already makes."""
    found = MANIFEST_CHECK_RE.findall(text_of(donor / "verify_source.py"))
    if len(found) != 1:
        raise ValueError(name + " verify_source.py: one MANIFEST.MF identity check expected")
    return VERIFY_SOURCE.replace("{donor}", name).replace("{manifest_check}", found[0]).encode(ENCODING)


def replace_all(text: str, pairs: tuple, what: str) -> str:
    for old, new in pairs:
        text = text.replace(old, new)
    if any(new not in text for _, new in pairs):
        raise ValueError(what + ": anchor text not found")
    return text


def reconcile(path: Path, data: bytes | None, check: bool, changed: list[str]) -> None:
    """Make path hold data, or not exist when data is None; record the path when it differs."""
    if data is None:
        if path.exists():
            changed.append(path.relative_to(REPO).as_posix())
            if not check:
                path.unlink()
        return
    if path.is_file() and path.read_bytes() == data:
        return
    changed.append(path.relative_to(REPO).as_posix())
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def edit_pom(recipe, text: str, name: str) -> str:
    for pattern in getattr(recipe, "POM_EDITS", ()):
        text = pattern.sub("", text)
    text = replace_all(text, tuple(recipe.POM_TEXT), name + " pom")
    left = [word for word in getattr(recipe, "POM_FORBIDDEN", ()) if word in text]
    if left:
        raise ValueError(name + " pom still names " + ", ".join(left))
    return text


def relocate(text: str, relocations) -> str:
    for old, new in ((relocations,) if isinstance(relocations[0], str) else relocations):
        text = text.replace(old, new)
    return text


def apply_donor(recipe, name: str, check: bool, changed: list[str]) -> None:
    donor, plans = REPO / VENDOR / name, plan_donor(recipe, name)
    for plan in plans:
        reconcile(REPO / plan["retained_rel"], plan["original"], check, changed)
        reconcile(REPO / plan["source_rel"], plan["result"], check, changed)
    record_bytes, provenance_bytes = desired_records(recipe, name, plans)
    reconcile(donor / RECORD_FILE, record_bytes, check, changed)
    reconcile(donor / "PROVENANCE.json", provenance_bytes, check, changed)
    reconcile(donor / "verify_source.py", verify_source(donor, name), check, changed)
    reconcile(donor / "pom.xml", edit_pom(recipe, text_of(donor / "pom.xml"), name).encode(ENCODING), check, changed)
    provenance = json.loads(provenance_bytes)
    display = f"{provenance['artifact']} {provenance['version']}"
    readme = tuple((old.format(donor=display), new.format(donor=display)) for old, new in recipe.README_TEXT)
    reconcile(donor / "README.md", replace_all(text_of(donor / "README.md"), readme, name + " README")
              .encode(ENCODING), check, changed)
    if name in recipe.RELOCATED:
        relocated, relocations, path_map = recipe.RELOCATED[name]
        for plan in plans:
            target = REPO / relocated / SOURCE_ROOT / plan["upstream"].replace(*path_map)
            result = plan["result"]
            reconcile(target, None if result is None else relocate(result.decode(ENCODING), relocations)
                      .encode(ENCODING), check, changed)
        for path, old, new in getattr(recipe, "RELOCATED_EDITS", {}).get(name, ()):
            target = REPO / relocated / SOURCE_ROOT / path
            reconcile(target, text_of(target).replace(old, new).encode(ENCODING), check, changed)
        for record in RELOCATED_RECORDS:
            if not check or not changed:
                reconcile(REPO / relocated / record, (donor / record).read_bytes(), check, changed)


def run(recipe, argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=recipe.__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when any target differs")
    args = parser.parse_args(argv)
    changed: list[str] = []
    for name in recipe.DONORS:
        apply_donor(recipe, name, args.check, changed)
    if hasattr(recipe, "after"):
        recipe.after(args.check, changed, types.SimpleNamespace(**globals()))
    print(json.dumps({"recipe": recipe.RECIPE_ID, "mode": "check" if args.check else "apply",
                      "changed": changed, "fixed_point": not changed}, sort_keys=True))
    return EXIT_DIFFERS if args.check and changed else 0
