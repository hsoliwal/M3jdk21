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
Float.toString on JDK 21 (make/m3/evidence/java-base/number-parse-jackson/SCHUBFACH_VS_JDK.txt).
- NumberOutput: DoubleToDecimal.toString(v) -> Double.toString(v), FloatToDecimal.toString(v) -> Float.toString(v).
- io.schubfach.DoubleToDecimal and FloatToDecimal keep their public members and delegate to java.base; the
  package-private MathUtils tables go (retained under upstream-excluded/). Nothing else changes.

Custody is make/m3/tooling/jdk-superset/retarget/tools/donor_overlay.py (DONOR_OVERLAY v2), the donors' overlay mechanism
that FDP_DELEGATES_TO_JDK v1 introduced: each upstream file stays byte-identical under upstream-excluded/ (kind unmodified-production-superseded-by-m3-overlay), the overlay
under src/main/java is pinned with its base and this recipe (kind m3-overlay-source), OVERLAY_SOURCES.json lists
the overlays, and verify_source.py re-derives every overlay through overlay() below. The pom drops the
FastDoubleParser dependency and its shade filter/relocations. The relocated jackson-core (lib-intake) gets the
same overlay through the lib-intake name mapping and loses `requires jdk.m3.fastdoubleparser`. The jackson
module pack's SOURCE_LOCK.json drops the FastDoubleParser donor and relocks jackson-core.

  python jackson_numbers_to_jdk.py            apply (writes only what differs; re-running changes nothing)
  python jackson_numbers_to_jdk.py --check    write nothing; exit 1 when any target differs
"""
from __future__ import annotations

import hashlib
import importlib.util
import json
from pathlib import Path
import re
import sys

sys.dont_write_bytecode = True

TOOL_REL = "make/m3/tooling/jdk-superset/number-parse/tools/jackson_numbers_to_jdk.py"
RECIPE_ID = "JACKSON_NUMBERS_TO_JDK v2"
VENDOR = "m3/vendor/third_party"
DONORS = ("jackson-core-2.15.3", "jackson-core-2.16.1", "jackson-core-2.19.2")
PACK_DONOR = "jackson-core-2.19.2"
RELOCATED = {PACK_DONOR: ("m3/vendor/third_party-m3/relocated/jackson-core-2.19.2",
                          (("com.fasterxml.jackson.core", "jdk.m3.lib.core"),
                           ("ch.randelshofer.fastdoubleparser", "jdk.m3.lib.fastdoubleparser")),
                          ("com/fasterxml/jackson/core", "jdk/m3/lib/core"))}
RELOCATED_EDITS = {PACK_DONOR: (("module-info.java", "    requires jdk.m3.fastdoubleparser;\n", ""),)}
PACKAGE_PATH = "com/fasterxml/jackson/core/io"
SCHUBFACH_PATH = "com/fasterxml/jackson/core/io/schubfach"
PARSE_TARGETS = ("NumberInput", "BigDecimalParser", "BigIntegerParser")
FACADES = {"DoubleToDecimal": ("double", "Double"), "FloatToDecimal": ("float", "Float")}
REMOVED = ("MathUtils",)
TARGETS = (tuple(f"{PACKAGE_PATH}/{name}.java" for name in PARSE_TARGETS + ("NumberOutput",))
           + tuple(f"{SCHUBFACH_PATH}/{name}.java" for name in tuple(FACADES) + REMOVED))
PACK_REL = "make/m3/tooling/module-packs/jackson"
PACK_FDP_DONOR = "fastdoubleparser-1.0.90"
PREDECESSOR_REL = "make/m3/evidence/java-base/number-parse-jackson/SOURCE_LOCK_PREDECESSOR.json"
BASE_REVISION = "5c47b0edf08d7ecd12e5c2afecc5530ac6930209"
ENCODING = "utf-8"

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
POM_FORBIDDEN = ("randelshofer", "fastdoubleparser")
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
README_TEXT = ((README_OLD, README_NEW), (README_V1, README_NEW))
MODIFICATIONS = ("M3 overlays " + RECIPE_ID + ": NumberInput, BigDecimalParser and BigIntegerParser call java.base "
                 "(Double.parseDouble, Float.parseFloat, new BigDecimal, new BigInteger) instead of the shaded "
                 "FastDoubleParser, which the pom no longer depends on; NumberOutput calls Double.toString and "
                 "Float.toString, io.schubfach.DoubleToDecimal and FloatToDecimal delegate to them, and "
                 "io.schubfach.MathUtils is removed (recipe " + TOOL_REL + "; the unchanged "
                 "upstream files are retained under upstream-excluded/). No other upstream source changes; Maven "
                 "descriptor and provenance files are M3jdk21 composition (Apache-2.0) and do not relicense the donor.")


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


def tree_digest(repo: Path, root: Path) -> tuple[str, int]:
    """The jackson pack's digest (build.py tree_digest) in POSIX path order on every platform.

    build.py sorts Path objects, which compare case-insensitively on Windows; its JDK 21 build runs on POSIX, so
    the files are ordered here by their case-sensitive path parts, which is POSIX Path order.
    """
    spec = importlib.util.spec_from_file_location("m3_jackson_pack_build", repo / PACK_REL / "build.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    files = sorted(module.source_files(root), key=lambda path: path.relative_to(root).parts)
    result = hashlib.sha256()
    for path in files:
        result.update(path.relative_to(root).as_posix().encode(ENCODING) + b"\0")
        result.update(module.digest(path).encode("ascii") + b"\n")
    return result.hexdigest(), len(files)


def desired_lock(custody) -> tuple[bytes, bytes]:
    pack, predecessor = custody.REPO / PACK_REL, custody.REPO / PREDECESSOR_REL
    previous = predecessor.read_bytes() if predecessor.is_file() else (pack / "SOURCE_LOCK.json").read_bytes()
    lock = json.loads((pack / "SOURCE_LOCK.json").read_text(encoding=ENCODING))
    lock["donors"].pop(PACK_FDP_DONOR, None)
    donor = custody.REPO / VENDOR / PACK_DONOR
    tree, count = tree_digest(custody.REPO, donor)
    lock["donors"][PACK_DONOR].update({"file_count": count, "tree_sha256": tree,
                                       "provenance_sha256": custody.sha256((donor / "PROVENANCE.json").read_bytes())})
    lock.update({"predecessor_path": PREDECESSOR_REL, "predecessor_sha256": custody.sha256(previous),
                 "target_base_revision": BASE_REVISION})
    return previous, custody.dump_json(lock)


def provenance_update(provenance: dict) -> None:
    """The pom no longer depends on FastDoubleParser, so PROVENANCE.json does not list it either."""
    provenance["dependencies"] = [item for item in provenance["dependencies"]
                                  if item.get("artifact") != "fastdoubleparser"]


def after(check: bool, changed: list[str], custody) -> None:
    """The jackson module pack's SOURCE_LOCK.json: relock jackson-core once its donor files are final."""
    if not check or not changed:
        previous, lock_bytes = desired_lock(custody)
        custody.reconcile(custody.REPO / PREDECESSOR_REL, previous, check, changed)
        custody.reconcile(custody.REPO / PACK_REL / "SOURCE_LOCK.json", lock_bytes, check, changed)


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "retarget" / "tools"))
    import donor_overlay
    raise SystemExit(donor_overlay.run(sys.modules[__name__]))
