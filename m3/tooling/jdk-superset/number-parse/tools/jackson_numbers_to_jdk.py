#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""JACKSON_NUMBERS_TO_JDK v2: Jackson core parses and writes numbers with java.base, the JDK's one implementation.

M3JDK absorbs FastDoubleParser 1.0.90 into java.base (jdk.internal.math.M3*): Double.parseDouble and
Float.parseFloat since NUMBER_PARSE_ABSORB v1, new BigDecimal and new BigInteger since v2. Jackson core called
its own shaded FastDoubleParser copy for the same work. This recipe retargets those calls to java.base in every
vendored jackson-core, so the four vendored FastDoubleParser copies have no consumer left:

- NumberInput: JavaDoubleParser/JavaFloatParser.parseX(s) -> Double.parseDouble(s) / Float.parseFloat(s);
  parseX(array, offset, len) -> parseX(new String(array, offset, len)), the expression Jackson's own
  non-fast branch uses.
- BigDecimalParser: JavaBigDecimalParser.parseBigDecimal(...) -> new BigDecimal(...).
- BigIntegerParser: JavaBigIntegerParser.parseBigInteger(...) -> new BigInteger(...).
- The FastDoubleParser imports go; the first becomes a //FLLDCIM anchor.
v2 (writing): JDK 19+ renders doubles and floats with Raffaello Giulietti's Schubfach (jdk.internal.math.DoubleToDecimal,
FloatToDecimal); jackson-core carries a copy of the same algorithm (io.schubfach) whose output equals Double.toString and
Float.toString on JDK 21 (m3/evidence/java-base/number-parse-jackson/SCHUBFACH_VS_JDK.txt).
- NumberOutput: DoubleToDecimal.toString(v) -> Double.toString(v), FloatToDecimal.toString(v) -> Float.toString(v).
- io.schubfach.DoubleToDecimal and FloatToDecimal keep their public members and delegate to java.base; the
  package-private MathUtils tables go (retained under upstream-excluded/). Nothing else changes.

Custody is the donors' existing overlay mechanism (FDP_DELEGATES_TO_JDK v1 introduced it): each upstream file
stays byte-identical under upstream-excluded/ (kind unmodified-production-superseded-by-m3-overlay), the overlay
under src/main/java is pinned with its base and this recipe (kind m3-overlay-source), OVERLAY_SOURCES.json lists
the overlays, and verify_source.py re-derives every overlay through overlay() below. The pom drops the
FastDoubleParser dependency and its shade filter/relocations. The relocated jackson-core (lib-intake) gets the
same overlay through the lib-intake name mapping and loses `requires jdk.m3.fastdoubleparser`. The jackson
module pack's SOURCE_LOCK.json drops the FastDoubleParser donor and relocks jackson-core.

  python jackson_numbers_to_jdk.py            apply (writes only what differs; re-running changes nothing)
  python jackson_numbers_to_jdk.py --check    write nothing; exit 1 when any target differs
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import re
import sys

sys.dont_write_bytecode = True

REPO = Path(__file__).resolve().parents[5]
TOOL_REL = "m3/tooling/jdk-superset/number-parse/tools/jackson_numbers_to_jdk.py"
RECIPE_ID = "JACKSON_NUMBERS_TO_JDK v2"
VENDOR = "m3/vendor/third_party"
DONORS = ("jackson-core-2.15.3", "jackson-core-2.16.1", "jackson-core-2.19.2")
PACK_DONOR = "jackson-core-2.19.2"
RELOCATED = "m3/vendor/third_party-m3/relocated/jackson-core-2.19.2"
RELOCATIONS = (("com.fasterxml.jackson.core", "jdk.m3.lib.core"),
               ("ch.randelshofer.fastdoubleparser", "jdk.m3.lib.fastdoubleparser"))
RELOCATED_PATH = ("com/fasterxml/jackson/core", "jdk/m3/lib/core")
RELOCATED_REQUIRES = "    requires jdk.m3.fastdoubleparser;\n"
PACKAGE_PATH = "com/fasterxml/jackson/core/io"
SCHUBFACH_PATH = "com/fasterxml/jackson/core/io/schubfach"
PARSE_TARGETS = ("NumberInput", "BigDecimalParser", "BigIntegerParser")
FACADES = {"DoubleToDecimal": ("double", "Double"), "FloatToDecimal": ("float", "Float")}
REMOVED = ("MathUtils",)
TARGETS = (tuple(f"{PACKAGE_PATH}/{name}.java" for name in PARSE_TARGETS + ("NumberOutput",))
           + tuple(f"{SCHUBFACH_PATH}/{name}.java" for name in tuple(FACADES) + REMOVED))
SOURCE_ROOT = "src/main/java"
RETAINED_ROOT = "upstream-excluded"
OVERLAY_KIND = "m3-overlay-source"
RETAINED_KIND = "unmodified-production-superseded-by-m3-overlay"
RECORD_KIND = "m3-overlay-record"
RECORD_FILE = "OVERLAY_SOURCES.json"
PACK_REL = "m3/tooling/module-packs/jackson"
PACK_FDP_DONOR = "fastdoubleparser-1.0.90"
PREDECESSOR_REL = "m3/evidence/java-base/number-parse-jackson/SOURCE_LOCK_PREDECESSOR.json"
BASE_REVISION = "5c47b0edf08d7ecd12e5c2afecc5530ac6930209"
ENCODING = "utf-8"
EXIT_DIFFERS = 1

# CNPCDA: anchors are never edited, so the v1 overlays keep their v1 anchor.
ANCHOR = "//FLLDCIM: M3 - JACKSON_NUMBERS_TO_JDK v1: numbers parse in java.base, the JDK's one number parser\n"
ANCHOR_WRITE = ("//FLLDCIM: M3 - JACKSON_NUMBERS_TO_JDK v2: doubles and floats render in java.base, the JDK's one "
                "Schubfach\n")
WRITE_IMPORT_RE = re.compile(r"^import [\w.]+\.schubfach\.(?:Double|Float)ToDecimal;\n", re.MULTILINE)
WRITE_CALLS = ((re.compile(r"\bDoubleToDecimal\.toString\("), "Double.toString("),
               (re.compile(r"\bFloatToDecimal\.toString\("), "Float.toString("))
FACADE_HEADER_RE = re.compile(r"\A.*?^package [\w.]+;\n", re.MULTILINE | re.DOTALL)
FACADE_SIZES_RE = re.compile(r"static final int H = (\d+);.*?public final int MAX_CHARS = H \+ (\d+);", re.DOTALL)
FACADE = ("\n" + ANCHOR_WRITE + "\n"
          "/**\n"
          " * This class exposes a method to render a {{@code {prim}}} as a string.\n"
          " *\n"
          " * <p>M3JDK overlay JACKSON_NUMBERS_TO_JDK v2: {{@link #toString({prim})}} returns\n"
          " * {{@link {box}#toString({prim})}}, which runs the same Schubfach algorithm in java.base. The copied\n"
          " * algorithm is retained under upstream-excluded/.\n"
          " *\n"
          " * @author Raffaello Giulietti\n"
          " */\n"
          "final public class {name} {{\n"
          "\n"
          "    /** The longest string {{@link #toString({prim})}} returns (upstream: H + {extra}, H = {digits}). */\n"
          "    public final int MAX_CHARS = {max_chars};\n"
          "\n"
          "    private {name}() {{\n"
          "    }}\n"
          "\n"
          "    /**\n"
          "     * Returns the string rendering of a {{@code {prim}}}: {{@link {box}#toString({prim})}}.\n"
          "     *\n"
          "     * @param v the {{@code {prim}}} to be rendered.\n"
          "     * @return a string rendering of the argument.\n"
          "     */\n"
          "    public static String toString({prim} v) {{\n"
          "        return {box}.toString(v);\n"
          "    }}\n"
          "}}\n")
IMPORT_RE = re.compile(r"^import [\w.]+\.fastdoubleparser\.Java(?:Double|Float|BigDecimal|BigInteger)Parser;\n",
                       re.MULTILINE)
CALLS = (
    (re.compile(r"\bJavaDoubleParser\.parseDouble\((\w+), (\w+), (\w+)\)"), r"Double.parseDouble(new String(\1, \2, \3))"),
    (re.compile(r"\bJavaFloatParser\.parseFloat\((\w+), (\w+), (\w+)\)"), r"Float.parseFloat(new String(\1, \2, \3))"),
    (re.compile(r"\bJavaDoubleParser\.parseDouble\((\w+)\)"), r"Double.parseDouble(\1)"),
    (re.compile(r"\bJavaFloatParser\.parseFloat\((\w+)\)"), r"Float.parseFloat(\1)"),
    (re.compile(r"\bJavaBigDecimalParser\.parseBigDecimal\("), "new BigDecimal("),
    (re.compile(r"\bJavaBigIntegerParser\.parseBigInteger\("), "new BigInteger("),
)
LEFTOVER_RE = re.compile(r"\bJava(?:Double|Float|BigDecimal|BigInteger)Parser\.|fastdoubleparser\.")

POM_EDITS = (
    re.compile(r"  <dependencies>\n    <dependency>\n      <groupId>ch\.randelshofer</groupId>\n.*?"
               r"</dependency>\n  </dependencies>\n", re.DOTALL),
    re.compile(r"              <filters>\n                <filter>\n                  <artifact>ch\.randelshofer:"
               r"fastdoubleparser</artifact>\n.*?              </filters>\n", re.DOTALL),
    re.compile(r"                <relocation>\n                  <pattern>[^<]*ch/randelshofer/fastdoubleparser"
               r"</pattern>\n                  <shadedPattern>[^<]*</shadedPattern>\n                </relocation>\n"),
)
POM_NAME = "- upstream source build for M3jdk21, numbers parsed and written by java.base</name>"
POM_DESCRIPTION = ("; one M3 overlay, " + RECIPE_ID + ": NumberInput, BigDecimalParser, BigIntegerParser and NumberOutput "
                   "call java.base instead of the shaded FastDoubleParser and the copied Schubfach; io.schubfach "
                   "DoubleToDecimal and FloatToDecimal delegate to java.base. See PROVENANCE.json and "
                   "OVERLAY_SOURCES.json.</description>")
POM_TEXT = (("- unchanged upstream source build for M3jdk21</name>", POM_NAME),
            ("- upstream source build for M3jdk21, numbers parsed by java.base</name>", POM_NAME),
            ("; no upstream implementation changes. See PROVENANCE.json.</description>", POM_DESCRIPTION),
            ("; one M3 overlay, JACKSON_NUMBERS_TO_JDK v1: NumberInput, BigDecimalParser and BigIntegerParser call "
             "java.base instead of the shaded FastDoubleParser. See PROVENANCE.json and OVERLAY_SOURCES.json."
             "</description>", POM_DESCRIPTION))
README_OLD = "This directory compiles the unchanged {donor} sources from the published sources jar.\n"
README_V1 = ("This directory compiles the {donor} sources from the published sources jar with one M3 overlay,\n"
             "`JACKSON_NUMBERS_TO_JDK v1` (`" + TOOL_REL + "`): `NumberInput`, `BigDecimalParser` and\n"
             "`BigIntegerParser` call `Double.parseDouble`, `Float.parseFloat`, `new BigDecimal` and `new BigInteger`\n"
             "in java.base, the JDK's one number parser, instead of the shaded FastDoubleParser. The upstream files\n"
             "stay byte-identical under `upstream-excluded/`; `verify_source.py` re-derives each overlay from them.\n")
README_NEW = ("This directory compiles the {donor} sources from the published sources jar with one M3 overlay,\n"
              "`" + RECIPE_ID + "` (`" + TOOL_REL + "`): `NumberInput`, `BigDecimalParser` and\n"
              "`BigIntegerParser` call `Double.parseDouble`, `Float.parseFloat`, `new BigDecimal` and `new BigInteger`,\n"
              "and `NumberOutput` calls `Double.toString` and `Float.toString`: java.base parses and renders the numbers,\n"
              "not the shaded FastDoubleParser or the copied Schubfach. `io.schubfach.DoubleToDecimal` and\n"
              "`FloatToDecimal` keep their public members and delegate to java.base; `io.schubfach.MathUtils` is gone.\n"
              "The upstream files stay byte-identical under `upstream-excluded/`; `verify_source.py` re-derives each\n"
              "overlay from them.\n")
MODIFICATIONS = ("M3 overlays " + RECIPE_ID + ": NumberInput, BigDecimalParser and BigIntegerParser call java.base "
                 "(Double.parseDouble, Float.parseFloat, new BigDecimal, new BigInteger) instead of the shaded "
                 "FastDoubleParser, which the pom no longer depends on; NumberOutput calls Double.toString and "
                 "Float.toString, io.schubfach.DoubleToDecimal and FloatToDecimal delegate to them, and "
                 "io.schubfach.MathUtils is removed (recipe " + TOOL_REL + "; the unchanged "
                 "upstream files are retained under upstream-excluded/). No other upstream source changes; Maven "
                 "descriptor and provenance files are M3jdk21 composition (Apache-2.0) and do not relicense the donor.")

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
            if "M3jdk21-Source-Inline: no upstream Automatic-Module-Name" not in jar.read("META-INF/MANIFEST.MF").decode():
                raise SystemExit("Automatic module identity changed")
        receipt["artifact_sha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
'''


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def overlay(original: bytes, upstream: str) -> bytes:
    """The recipe: pinned upstream bytes of one jackson-core number class -> its M3 overlay bytes."""
    name = upstream.rsplit("/", 1)[-1].removesuffix(".java")
    if name == "NumberOutput":
        return overlay_writer(original.decode(ENCODING), upstream).encode(ENCODING)
    if name in FACADES:
        return facade(original.decode(ENCODING), name).encode(ENCODING)
    if name not in PARSE_TARGETS:
        raise ValueError(upstream + ": not a recipe target")
    text = original.decode(ENCODING)
    imports = IMPORT_RE.findall(text)
    if not imports:
        raise ValueError(upstream + ": no FastDoubleParser import")
    text = text.replace(imports[0], ANCHOR, 1)
    for line in imports[1:]:
        text = text.replace(line, "", 1)
    calls = 0
    for pattern, replacement in CALLS:
        text, count = pattern.subn(replacement, text)
        calls += count
    if calls == 0 or LEFTOVER_RE.search(text):
        raise ValueError(f"{upstream}: {calls} calls retargeted, FastDoubleParser use left: "
                         f"{LEFTOVER_RE.findall(text)}")
    return text.encode(ENCODING)


def overlay_writer(text: str, upstream: str) -> str:
    imports = WRITE_IMPORT_RE.findall(text)
    if len(imports) != 2:
        raise ValueError(upstream + f": {len(imports)} Schubfach imports, expected 2")
    text = text.replace(imports[0], ANCHOR_WRITE, 1).replace(imports[1], "", 1)
    for pattern, replacement in WRITE_CALLS:
        text, count = pattern.subn(replacement, text)
        if count != 1:
            raise ValueError(f"{upstream}: {pattern.pattern} matched {count} times, expected 1")
    if "schubfach." in text or "ToDecimal." in text:
        raise ValueError(upstream + ": Schubfach use left")
    return text


def facade(text: str, name: str) -> str:
    header, sizes = FACADE_HEADER_RE.match(text), FACADE_SIZES_RE.search(text)
    if header is None or sizes is None or "public static String toString(" not in text:
        raise ValueError(name + ": header, H/MAX_CHARS or toString not found")
    prim, box = FACADES[name]
    digits, extra = int(sizes.group(1)), int(sizes.group(2))
    return header.group(0) + FACADE.format(name=name, prim=prim, box=box, digits=digits, extra=extra,
                                           max_chars=digits + extra)


def relocate(text: str) -> str:
    for old, new in RELOCATIONS:
        text = text.replace(old, new)
    return text


def dump_json(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode(ENCODING)


def manifest_pins(donor: Path) -> dict[str, str]:
    rows = csv.DictReader(io.StringIO((donor / "SOURCES_MANIFEST.tsv").read_text(encoding=ENCODING)), delimiter="\t")
    return {row["path"]: row["sha256"] for row in rows}


def plan_donor(name: str) -> list[dict]:
    donor_rel = f"{VENDOR}/{name}"
    pins, tool_sha, plans = manifest_pins(REPO / donor_rel), sha256((REPO / TOOL_REL).read_bytes()), []
    for upstream in TARGETS:
        source_rel = f"{donor_rel}/{SOURCE_ROOT}/{upstream}"
        retained_rel = f"{donor_rel}/{RETAINED_ROOT}/{upstream}"
        holder = REPO / (retained_rel if (REPO / retained_rel).is_file() else source_rel)
        original = holder.read_bytes()
        if sha256(original) != pins[upstream]:
            raise ValueError("Original is not the pinned upstream byte sequence: " + str(holder))
        removed = upstream.rsplit("/", 1)[-1].removesuffix(".java") in REMOVED
        plans.append({"upstream": upstream, "original": original,
                      "result": None if removed else overlay(original, upstream),
                      "source_rel": source_rel, "retained_rel": retained_rel, "tool_sha": tool_sha})
    return plans


def overlay_entry(plan: dict) -> dict:
    return {"base_repository": plan["retained_rel"], "base_sha256": sha256(plan["original"]),
            "kind": OVERLAY_KIND, "overlay": RECIPE_ID, "recipe": TOOL_REL, "recipe_sha256": plan["tool_sha"],
            "repository": plan["source_rel"], "sha256": sha256(plan["result"]), "upstream": plan["upstream"]}


def desired_records(name: str, plans: list[dict]) -> tuple[bytes, bytes]:
    donor_rel = f"{VENDOR}/{name}"
    records = []
    for plan in (plan for plan in plans if plan["result"] is not None):
        record = overlay_entry(plan)
        record.pop("kind")
        records.append(record)
    record_bytes = dump_json({"donor": name, "m3_overlays": records, "overlay": RECIPE_ID, "recipe": TOOL_REL})
    provenance = json.loads((REPO / donor_rel / "PROVENANCE.json").read_text(encoding=ENCODING))
    by_source = {plan["source_rel"]: plan for plan in plans if plan["result"] is not None}
    ours = ({plan["retained_rel"] for plan in plans} | {f"{donor_rel}/{RECORD_FILE}"}
            | {plan["source_rel"] for plan in plans if plan["result"] is None})
    files = [overlay_entry(by_source[item["repository"]]) if item["repository"] in by_source else item
             for item in provenance["files"] if item["repository"] not in ours]
    files.extend({"kind": RETAINED_KIND, "repository": plan["retained_rel"], "sha256": sha256(plan["original"]),
                  "upstream": plan["upstream"]} for plan in plans)
    files.append({"kind": RECORD_KIND, "repository": f"{donor_rel}/{RECORD_FILE}", "sha256": sha256(record_bytes),
                  "upstream": TOOL_REL})
    provenance.update({"files": files, "modifications": MODIFICATIONS,
                       "dependencies": [item for item in provenance["dependencies"]
                                        if item.get("artifact") != "fastdoubleparser"]})
    return record_bytes, dump_json(provenance)


def edit_pom(text: str) -> str:
    for pattern in POM_EDITS:
        text = pattern.sub("", text)
    for old, new in POM_TEXT:
        text = text.replace(old, new)
    if "randelshofer" in text or "fastdoubleparser" in text or POM_TEXT[0][1] not in text:
        raise ValueError("pom still names FastDoubleParser or lacks the overlay description")
    return text


def edit_readme(text: str, name: str) -> str:
    name = " ".join(name.rsplit("-", 1))  # the README spells the donor "jackson-core 2.19.2"
    for old in (README_OLD, README_V1):
        text = text.replace(old.format(donor=name), README_NEW.format(donor=name))
    if README_NEW.format(donor=name) not in text:
        raise ValueError(name + " README: anchor sentence not found")
    return text


def tree_digest(root: Path) -> tuple[str, int]:
    """The jackson pack's digest (build.py tree_digest) in POSIX path order on every platform.

    build.py sorts Path objects, which compare case-insensitively on Windows; its JDK 21 build runs on POSIX, so
    the files are ordered here by their case-sensitive path parts, which is POSIX Path order.
    """
    spec = importlib.util.spec_from_file_location("m3_jackson_pack_build", REPO / PACK_REL / "build.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    files = sorted(module.source_files(root), key=lambda path: path.relative_to(root).parts)
    result = hashlib.sha256()
    for path in files:
        result.update(path.relative_to(root).as_posix().encode(ENCODING) + b"\0")
        result.update(module.digest(path).encode("ascii") + b"\n")
    return result.hexdigest(), len(files)


def desired_lock() -> tuple[bytes, bytes]:
    pack, predecessor = REPO / PACK_REL, REPO / PREDECESSOR_REL
    previous = predecessor.read_bytes() if predecessor.is_file() else (pack / "SOURCE_LOCK.json").read_bytes()
    lock = json.loads((pack / "SOURCE_LOCK.json").read_text(encoding=ENCODING))
    lock["donors"].pop(PACK_FDP_DONOR, None)
    donor = REPO / VENDOR / PACK_DONOR
    tree, count = tree_digest(donor)
    lock["donors"][PACK_DONOR].update({"file_count": count, "tree_sha256": tree,
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


def reconcile_absent(path: Path, check: bool, changed: list[str]) -> None:
    if not path.exists():
        return
    changed.append(path.relative_to(REPO).as_posix())
    if not check:
        path.unlink()


def text_of(path: Path) -> str:
    return path.read_bytes().decode(ENCODING)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when any target differs")
    args = parser.parse_args(argv)
    changed: list[str] = []
    pack_plans = []
    for name in DONORS:
        donor = REPO / VENDOR / name
        plans = plan_donor(name)
        for plan in plans:
            reconcile(REPO / plan["retained_rel"], plan["original"], args.check, changed)
            if plan["result"] is None:
                reconcile_absent(REPO / plan["source_rel"], args.check, changed)
            else:
                reconcile(REPO / plan["source_rel"], plan["result"], args.check, changed)
        record_bytes, provenance_bytes = desired_records(name, plans)
        reconcile(donor / RECORD_FILE, record_bytes, args.check, changed)
        reconcile(donor / "PROVENANCE.json", provenance_bytes, args.check, changed)
        reconcile(donor / "verify_source.py", VERIFY_SOURCE.replace("{donor}", name).encode(ENCODING), args.check,
                  changed)
        reconcile(donor / "pom.xml", edit_pom(text_of(donor / "pom.xml")).encode(ENCODING), args.check, changed)
        reconcile(donor / "README.md", edit_readme(text_of(donor / "README.md"), name).encode(ENCODING),
                  args.check, changed)
        if name == PACK_DONOR:
            pack_plans = plans
    relocated = REPO / RELOCATED
    for plan in pack_plans:
        target = relocated / SOURCE_ROOT / plan["upstream"].replace(*RELOCATED_PATH)
        if plan["result"] is None:
            reconcile_absent(target, args.check, changed)
        else:
            reconcile(target, relocate(plan["result"].decode(ENCODING)).encode(ENCODING), args.check, changed)
    module_info = relocated / SOURCE_ROOT / "module-info.java"
    reconcile(module_info, text_of(module_info).replace(RELOCATED_REQUIRES, "").encode(ENCODING), args.check, changed)
    for record in ("PROVENANCE.json", "THIRD_PARTY_NOTICES.md"):
        source = REPO / VENDOR / PACK_DONOR / record
        if not args.check or not changed:
            reconcile(relocated / record, source.read_bytes(), args.check, changed)
    if not args.check or not changed:
        previous, lock_bytes = desired_lock()
        reconcile(REPO / PREDECESSOR_REL, previous, args.check, changed)
        reconcile(REPO / PACK_REL / "SOURCE_LOCK.json", lock_bytes, args.check, changed)
    print(json.dumps({"recipe": RECIPE_ID, "mode": "check" if args.check else "apply",
                      "changed": changed, "fixed_point": not changed}, sort_keys=True))
    return EXIT_DIFFERS if args.check and changed else 0


if __name__ == "__main__":
    raise SystemExit(main())
