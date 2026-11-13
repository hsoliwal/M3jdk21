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

Custody is make/m3/tooling/jdk-superset/retarget/tools/donor_overlay.py (DONOR_OVERLAY v2), the donors' overlay mechanism:
each upstream file stays byte-identical under upstream-excluded/ (kind unmodified-production-superseded-by-m3-overlay),
the overlay under src/main/java is pinned with its base and this recipe (kind m3-overlay-source), OVERLAY_SOURCES.json
lists the overlays, and verify_source.py re-derives every overlay through overlay() below. The relocated commons-codec
(module jdk.m3.codec, lib-intake) gets the same overlay through the lib-intake name mapping.

  python checksums_to_jdk.py            apply (writes only what differs; re-running changes nothing)
  python checksums_to_jdk.py --check    write nothing; exit 1 when any target differs
"""
from __future__ import annotations

from pathlib import Path
import re
import sys

TOOL_REL = "make/m3/tooling/jdk-superset/hashing/tools/checksums_to_jdk.py"
RECIPE_ID = "CHECKSUMS_TO_JDK v1"
DONORS = ("commons-codec-1.17.1", "commons-codec-1.22.1")
RELOCATED = {"commons-codec-1.22.1": ("m3/vendor/third_party-m3/relocated/commons-codec-1.22.1",
                                      ("org.apache.commons.codec", "jdk.m3.lib.codec"),
                                      ("org/apache/commons/codec", "jdk/m3/lib/codec"))}
PACKAGE_PATH = "org/apache/commons/codec/digest"
JDK_CLASS = {"PureJavaCrc32": "CRC32", "PureJavaCrc32C": "CRC32C"}
TARGETS = tuple(f"{PACKAGE_PATH}/{name}.java" for name in JDK_CLASS)
ENCODING = "utf-8"

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
              "The overlay's proof is `make/m3/evidence/java-base/CHECKSUMS_CODEC.md`. Outside it no speed, allocation or\n"
              "behaviour change is claimed. The published sources jar carries no test suite; the compile from source and\n"
              "the consumers' own tests are the proof lane. Upstream tests remain a follow-up with their own recipe.")
README_TEXT = tuple(zip(README_OLD, README_NEW))
MODIFICATIONS = ("M3 overlays " + RECIPE_ID + ": digest.PureJavaCrc32 and PureJavaCrc32C keep their public members and "
                 "delegate to java.base (java.util.zip.CRC32, CRC32C; PureJavaCrc32C through one MethodHandle because "
                 "the donor compiles with --release 8), so their lookup tables are gone (recipe " + TOOL_REL + "; the "
                 "unchanged upstream files are retained under upstream-excluded/). No other upstream source changes; "
                 "Maven descriptor and provenance files are M3jdk21 composition (Apache-2.0) and do not relicense the "
                 "donor.")


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


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "retarget" / "tools"))
    import donor_overlay
    raise SystemExit(donor_overlay.run(sys.modules[__name__]))
