#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""CHECKSUMS_TO_JDK v1: commons-codec computes CRC-32 and CRC-32C with java.base, the JDK's one implementation.

java.base owns CRC-32 (java.util.zip.CRC32) and CRC-32C (java.util.zip.CRC32C); HotSpot compiles both to the
processor's CRC instructions. commons-codec carries Hadoop's table-driven copies of the same two checksums
(digest.PureJavaCrc32 and PureJavaCrc32C, about 640 lines each, written to beat Java 6's JNI CRC32). This recipe
keeps both public classes, with their constructors, methods and modifiers, and makes them delegate to java.base:

- PureJavaCrc32: a java.util.zip.CRC32 field; getValue, reset and both update methods call it.
- PureJavaCrc32C: the same over java.util.zip.CRC32C. The donor compiles with --release 8 and CRC32C is Java 9 API,
  so the class reaches the constructor through one MethodHandle, as Guava's Hashing.crc32c() and snappy-java's
  SnappyFramed do; every other donor class compiles byte-identical to the published jar.
- The lookup tables go; the class javadoc gains one paragraph naming java.base.

Custody is the donors' overlay mechanism (FDP_DELEGATES_TO_JDK v1, JACKSON_NUMBERS_TO_JDK): each upstream file stays
byte-identical under upstream-excluded/ (kind unmodified-production-superseded-by-m3-overlay), the overlay under
src/main/java is pinned with its base and this recipe (kind m3-overlay-source), OVERLAY_SOURCES.json lists the
overlays, and verify_source.py re-derives every overlay through overlay() below. The relocated commons-codec
(module jdk.m3.codec, lib-intake) gets the same overlay through the lib-intake name mapping.

  python checksums_to_jdk.py            apply (writes only what differs; re-running changes nothing)
  python checksums_to_jdk.py --check    write nothing; exit 1 when any target differs
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import re

REPO = Path(__file__).resolve().parents[5]
TOOL_REL = "m3/tooling/jdk-superset/hashing/tools/checksums_to_jdk.py"
RECIPE_ID = "CHECKSUMS_TO_JDK v1"
VENDOR = "m3/vendor/third_party"
DONORS = ("commons-codec-1.17.1", "commons-codec-1.22.1")
RELOCATED_DONOR = "commons-codec-1.22.1"
RELOCATED = "m3/vendor/third_party-m3/relocated/commons-codec-1.22.1"
RELOCATION = ("org.apache.commons.codec", "jdk.m3.lib.codec")
RELOCATED_PATH = ("org/apache/commons/codec", "jdk/m3/lib/codec")
PACKAGE_PATH = "org/apache/commons/codec/digest"
JDK_CLASS = {"PureJavaCrc32": "CRC32", "PureJavaCrc32C": "CRC32C"}
TARGETS = tuple(f"{PACKAGE_PATH}/{name}.java" for name in JDK_CLASS)
SOURCE_ROOT = "src/main/java"
RETAINED_ROOT = "upstream-excluded"
OVERLAY_KIND = "m3-overlay-source"
RETAINED_KIND = "unmodified-production-superseded-by-m3-overlay"
RECORD_KIND = "m3-overlay-record"
RECORD_FILE = "OVERLAY_SOURCES.json"
ENCODING = "utf-8"
EXIT_DIFFERS = 1

ANCHOR = "    //FLLDCIM: M3 - CHECKSUMS_TO_JDK v1: java.base computes the checksum, the JDK's one implementation\n"
CLASS_RE = re.compile(r"^public class (\w+) implements Checksum \{\n", re.MULTILINE)
JAVADOC_TAG_RE = re.compile(r"^ \* @", re.MULTILINE)
FIELD = "    /** The current CRC value, bit-flipped */\n    private int crc;\n"
BODIES = (
    (re.compile(r"(    public long getValue\(\) \{\n).*?(\n    }\n)", re.DOTALL), r"\1        return crc.getValue();\2"),
    (re.compile(r"^        crc = 0xffffffff;$", re.MULTILINE), "        crc.reset();"),
    (re.compile(r"(    public void update\(final byte\[\] b, (?:final )?int (\w+), (?:final )?int len\) \{\n).*?"
                r"(\n    }\n)", re.DOTALL), r"\1        crc.update(b, \2, len);\3"),
    (re.compile(r"(    (?:final public|public final) void update\(final int b\) \{\n).*?(\n    }\n)", re.DOTALL),
     r"\1        crc.update(b);\2"),
)
CHECKSUM_IMPORT = "import java.util.zip.Checksum;\n"
IMPORTS = {"CRC32": "import java.util.zip.CRC32;\n",
           "CRC32C": ("import java.lang.invoke.MethodHandle;\nimport java.lang.invoke.MethodHandles;\n"
                      "import java.lang.invoke.MethodType;\n")}
JAVADOC = (" * <p>\n * M3JDK ({recipe}): java.base computes the checksum; this class delegates to\n"
           " * {{@code java.util.zip.{jdk}}}, which HotSpot compiles to the processor's CRC instructions.\n * </p>\n")
DELEGATE = {
    "CRC32": "    private final CRC32 crc = new CRC32();\n",
    "CRC32C": ("    private static final MethodHandle NEW_CRC32C = crc32cConstructor();\n\n"
               "    private final Checksum crc = newCrc32c();\n"),
}
HELPERS = {"CRC32": "", "CRC32C": '''    // java.util.zip.CRC32C is Java 9 API and the donor compiles with --release 8 (Guava's Hashing does the same).
    private static MethodHandle crc32cConstructor() {
        try {
            return MethodHandles.publicLookup()
                    .findConstructor(Class.forName("java.util.zip.CRC32C"), MethodType.methodType(void.class))
                    .asType(MethodType.methodType(Checksum.class));
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Checksum newCrc32c() {
        try {
            return (Checksum) NEW_CRC32C.invokeExact();
        } catch (final RuntimeException | Error e) {
            throw e;
        } catch (final Throwable e) {
            throw new IllegalStateException(e);
        }
    }
'''}
LEFTOVER_RE = re.compile(r"\bT\[|0xffffffff|T8_|private int crc")

POM_TEXT = (("- unchanged upstream source build for M3jdk21</name>",
             "- upstream source build for M3jdk21, CRC-32 and CRC-32C computed by java.base</name>"),
            ("; no upstream implementation changes. See PROVENANCE.json.</description>",
             "; one M3 overlay, " + RECIPE_ID + ": digest.PureJavaCrc32 and PureJavaCrc32C delegate to java.base "
             "(java.util.zip.CRC32, CRC32C) instead of carrying their own lookup tables. See PROVENANCE.json and "
             "OVERLAY_SOURCES.json.</description>"))
README_OLD = ("This directory compiles the unchanged {donor} sources from the published sources jar.\n",
              "No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the\n"
              "compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with\n"
              "their own recipe.")
README_NEW = ("This directory compiles the {donor} sources from the published sources jar with one M3 overlay,\n"
              "`" + RECIPE_ID + "` (`" + TOOL_REL + "`): `digest.PureJavaCrc32` and\n"
              "`PureJavaCrc32C` keep their public members and delegate to java.base (`java.util.zip.CRC32`, `CRC32C`),\n"
              "the JDK's one CRC implementation, instead of carrying their own lookup tables. The upstream files stay\n"
              "byte-identical under `upstream-excluded/`; `verify_source.py` re-derives each overlay from them.\n",
              "The overlay's proof is `m3/evidence/java-base/CHECKSUMS_CODEC.md`. Outside it no speed, allocation or\n"
              "behaviour change is claimed. The published sources jar carries no test suite; the compile from source and\n"
              "the consumers' own tests are the proof lane. Upstream tests remain a follow-up with their own recipe.")
MODIFICATIONS = ("M3 overlays " + RECIPE_ID + ": digest.PureJavaCrc32 and PureJavaCrc32C keep their public members and "
                 "delegate to java.base (java.util.zip.CRC32, CRC32C; PureJavaCrc32C through one MethodHandle because "
                 "the donor compiles with --release 8), so their lookup tables are gone (recipe " + TOOL_REL + "; the "
                 "unchanged upstream files are retained under upstream-excluded/). No other upstream source changes; "
                 "Maven descriptor and provenance files are M3jdk21 composition (Apache-2.0) and do not relicense the "
                 "donor.")

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
            if "Automatic-Module-Name: org.apache.commons.codec" not in jar.read("META-INF/MANIFEST.MF").decode():
                raise SystemExit("Automatic module identity changed")
        receipt["artifact_sha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
'''


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def overlay(original: bytes, upstream: str) -> bytes:
    """The recipe: pinned upstream bytes of PureJavaCrc32 or PureJavaCrc32C -> its M3 overlay bytes."""
    text, jdk = original.decode(ENCODING), JDK_CLASS[upstream.rsplit("/", 1)[-1].removesuffix(".java")]
    declaration, field = CLASS_RE.search(text), text.find(FIELD)
    if declaration is None or field < declaration.end() or text.count(CHECKSUM_IMPORT) != 1:
        raise ValueError(upstream + ": class declaration, crc field or Checksum import not found")
    head, members = text[:declaration.end()], text[field + len(FIELD):]
    tag = JAVADOC_TAG_RE.search(head, head.rfind("/**"))
    head = head[:tag.start()] + JAVADOC.format(recipe=RECIPE_ID, jdk=jdk) + head[tag.start():]
    head = head.replace(CHECKSUM_IMPORT, IMPORTS[jdk] + CHECKSUM_IMPORT)
    for pattern, replacement in BODIES:
        members = pattern.sub(replacement, members)
    if not members.endswith("\n}\n"):
        raise ValueError(upstream + ": class does not end with its closing brace")
    members = members[:-len("}\n")] + HELPERS[jdk] + ("\n" if HELPERS[jdk] else "") + "}\n"
    result = head + "\n" + ANCHOR + DELEGATE[jdk] + members
    if LEFTOVER_RE.search(result) or result.count("crc.") != 4:
        raise ValueError(upstream + ": overlay left table code behind or missed a delegating method")
    return result.encode(ENCODING)


def relocate(text: str) -> str:
    return text.replace(*RELOCATION)


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
        plans.append({"upstream": upstream, "original": original, "result": overlay(original, upstream),
                      "source_rel": source_rel, "retained_rel": retained_rel, "tool_sha": tool_sha})
    return plans


def overlay_entry(plan: dict) -> dict:
    return {"base_repository": plan["retained_rel"], "base_sha256": sha256(plan["original"]),
            "kind": OVERLAY_KIND, "overlay": RECIPE_ID, "recipe": TOOL_REL, "recipe_sha256": plan["tool_sha"],
            "repository": plan["source_rel"], "sha256": sha256(plan["result"]), "upstream": plan["upstream"]}


def desired_records(name: str, plans: list[dict]) -> tuple[bytes, bytes]:
    donor_rel = f"{VENDOR}/{name}"
    records = [{key: value for key, value in overlay_entry(plan).items() if key != "kind"} for plan in plans]
    record_bytes = dump_json({"donor": name, "m3_overlays": records, "overlay": RECIPE_ID, "recipe": TOOL_REL})
    provenance = json.loads((REPO / donor_rel / "PROVENANCE.json").read_text(encoding=ENCODING))
    by_source = {plan["source_rel"]: plan for plan in plans}
    ours = {plan["retained_rel"] for plan in plans} | {f"{donor_rel}/{RECORD_FILE}"}
    files = [overlay_entry(by_source[item["repository"]]) if item["repository"] in by_source else item
             for item in provenance["files"] if item["repository"] not in ours]
    files.extend({"kind": RETAINED_KIND, "repository": plan["retained_rel"], "sha256": sha256(plan["original"]),
                  "upstream": plan["upstream"]} for plan in plans)
    files.append({"kind": RECORD_KIND, "repository": f"{donor_rel}/{RECORD_FILE}", "sha256": sha256(record_bytes),
                  "upstream": TOOL_REL})
    provenance.update({"files": files, "modifications": MODIFICATIONS})
    return record_bytes, dump_json(provenance)


def replace_all(text: str, pairs, what: str) -> str:
    for old, new in pairs:
        text = text.replace(old, new)
    if any(new not in text for _, new in pairs):
        raise ValueError(what + ": anchor text not found")
    return text


def reconcile(path: Path, data: bytes, check: bool, changed: list[str]) -> None:
    if path.is_file() and path.read_bytes() == data:
        return
    changed.append(path.relative_to(REPO).as_posix())
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def text_of(path: Path) -> str:
    return path.read_bytes().decode(ENCODING)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when any target differs")
    args = parser.parse_args(argv)
    changed: list[str] = []
    for name in DONORS:
        donor, plans = REPO / VENDOR / name, plan_donor(name)
        for plan in plans:
            reconcile(REPO / plan["retained_rel"], plan["original"], args.check, changed)
            reconcile(REPO / plan["source_rel"], plan["result"], args.check, changed)
        record_bytes, provenance_bytes = desired_records(name, plans)
        reconcile(donor / RECORD_FILE, record_bytes, args.check, changed)
        reconcile(donor / "PROVENANCE.json", provenance_bytes, args.check, changed)
        reconcile(donor / "verify_source.py", VERIFY_SOURCE.replace("{donor}", name).encode(ENCODING), args.check,
                  changed)
        reconcile(donor / "pom.xml", replace_all(text_of(donor / "pom.xml"), POM_TEXT, name + " pom").encode(ENCODING),
                  args.check, changed)
        display = " ".join(name.rsplit("-", 1))  # the README spells the donor "commons-codec 1.22.1"
        readme = replace_all(text_of(donor / "README.md"), tuple((old.format(donor=display), new.format(donor=display))
                                                                 for old, new in zip(README_OLD, README_NEW)), name)
        reconcile(donor / "README.md", readme.encode(ENCODING), args.check, changed)
        if name == RELOCATED_DONOR:
            for plan in plans:
                target = REPO / RELOCATED / SOURCE_ROOT / plan["upstream"].replace(*RELOCATED_PATH)
                reconcile(target, relocate(plan["result"].decode(ENCODING)).encode(ENCODING), args.check, changed)
            for record in ("PROVENANCE.json", "THIRD_PARTY_NOTICES.md"):
                if not args.check or not changed:
                    reconcile(REPO / RELOCATED / record, (donor / record).read_bytes(), args.check, changed)
    print(json.dumps({"recipe": RECIPE_ID, "mode": "check" if args.check else "apply",
                      "changed": changed, "fixed_point": not changed}, sort_keys=True))
    return EXIT_DIFFERS if args.check and changed else 0


if __name__ == "__main__":
    raise SystemExit(main())
