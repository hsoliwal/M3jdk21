#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""BASE64_TO_JDK v1: HdrHistogram encodes and decodes base64 by calling java.base's java.util.Base64 directly.

HdrHistogram 2.1.12's package-private Base64Helper finds java.util.Base64 by reflection. If that fails, it falls back
to javax.xml.bind.DatatypeConverter, so that one source tree runs on Java SE 6 to 9. M3JDK is Java SE 21: the
reflection always finds java.util.Base64, and the fallback is dead (java.xml.bind left the JDK in 11). This recipe
keeps Base64Helper and its two methods:

- printBase64Binary(bytes) -> Base64.getEncoder().encodeToString(bytes);
- parseBase64Binary(text) -> Base64.getDecoder().decode(text);
- the reflective state (four static fields and the static initializer) goes; java.lang.reflect.Method becomes
  java.util.Base64 (Java 8 API, within the donor's --release 8); the class Javadoc gains one paragraph.

The values are the same calls the reflection made. A failure now surfaces as java.util.Base64's own exception:
IllegalArgumentException for malformed input, NullPointerException for null. Before, any failure was wrapped in
UnsupportedOperationException("Failed to use platform's base64 ... method").

Custody: donor_overlay.py (DONOR_OVERLAY v1). The relocated module jdk.m3.hdrhistogram gets the same overlay through
the lib-intake name mapping.

  python base64_to_jdk.py            apply (writes only what differs; re-running changes nothing)
  python base64_to_jdk.py --check    write nothing; exit 1 when any target differs
"""
from __future__ import annotations

from pathlib import Path
import re
import sys

RECIPE_ID = "BASE64_TO_JDK v1"
TOOL_REL = "m3/tooling/jdk-superset/retarget/tools/base64_to_jdk.py"
DONORS = ("HdrHistogram",)
TARGETS = ("org/HdrHistogram/Base64Helper.java",)
RELOCATED = {"HdrHistogram": ("m3/vendor/third_party-m3/relocated/HdrHistogram",
                              ("org.HdrHistogram", "jdk.m3.lib.hdrhistogram"), ("org/HdrHistogram", "jdk/m3/lib/hdrhistogram"))}
ENCODING = "utf-8"

ANCHOR = "    //FLLDCIM: M3 - BASE64_TO_JDK v1: base64 is java.base's java.util.Base64, the JDK's one implementation\n"
EDITS = (
    ("import java.lang.reflect.Method;\n", "import java.util.Base64;\n"),
    (" */\nclass Base64Helper {\n",
     " * <p>\n * M3JDK (" + RECIPE_ID + "): M3JDK is Java SE 21, so this class calls {@code java.util.Base64} directly,\n"
     " * with no reflection and no javax.xml.bind fallback.\n * </p>\n */\nclass Base64Helper {\n\n" + ANCHOR),
)
BODIES = (
    (re.compile(r"(    static String printBase64Binary\(byte \[\] binaryArray\) \{\n).*?(\n    }\n)", re.DOTALL),
     r"\1        return Base64.getEncoder().encodeToString(binaryArray);\2"),
    (re.compile(r"(    static byte\[\] parseBase64Binary\(String base64input\) \{\n).*?(\n    }\n)", re.DOTALL),
     r"\1        return Base64.getDecoder().decode(base64input);\2"),
    (re.compile(r"\n    private static Method decodeMethod;\n.*?\n    static \{\n.*?\n    }\n(?=}\n\Z)", re.DOTALL), ""),
)
LEFTOVER_RE = re.compile(r"\.invoke\(|Class\.forName|private static (?:Method|Object)|encoderObj|decoderObj|catch")

POM_TEXT = (("- unchanged upstream source build for M3jdk21</name>",
             "- upstream source build for M3jdk21, base64 by java.base</name>"),
            ("; no upstream implementation changes. See PROVENANCE.json.</description>",
             "; one M3 overlay, " + RECIPE_ID + ": Base64Helper calls java.util.Base64 directly instead of finding it "
             "by reflection. See PROVENANCE.json and OVERLAY_SOURCES.json.</description>"))
README_TEXT = (
    ("This directory compiles the unchanged {donor} sources from the published sources jar.\n",
     "This directory compiles the {donor} sources from the published sources jar with one M3 overlay,\n"
     "`" + RECIPE_ID + "` (`" + TOOL_REL + "`): `Base64Helper` calls `java.util.Base64`\n"
     "directly instead of finding it by reflection (with a `javax.xml.bind` fallback for Java SE 6 and 7). The upstream\n"
     "file stays byte-identical under `upstream-excluded/`; `verify_source.py` re-derives the overlay from it.\n"),
    ("No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the\n"
     "compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with\n"
     "their own recipe.",
     "The overlay's proof is `m3/evidence/java-base/BASE64_HDRHISTOGRAM.md`. Outside it no speed, allocation or\n"
     "behaviour change is claimed. The published sources jar carries no test suite; the compile from source and\n"
     "the consumers' own tests are the proof lane. Upstream tests remain a follow-up with their own recipe."),
)
MODIFICATIONS = ("M3 overlays " + RECIPE_ID + ": Base64Helper calls java.util.Base64 directly instead of finding it by "
                 "reflection with a javax.xml.bind fallback (recipe " + TOOL_REL + "; the unchanged upstream file is "
                 "retained under upstream-excluded/). No other upstream source changes; Maven descriptor and provenance "
                 "files are M3jdk21 composition (Apache-2.0) and do not relicense the donor.")


def overlay(original: bytes, upstream: str) -> bytes:
    """The recipe: pinned upstream bytes of HdrHistogram's Base64Helper -> its M3 overlay bytes."""
    text = original.decode(ENCODING)
    for old, new in EDITS:
        if text.count(old) != 1:
            raise ValueError(upstream + ": expected once: " + old.splitlines()[0])
        text = text.replace(old, new)
    for pattern, replacement in BODIES:
        text, hits = pattern.subn(replacement, text)
        if hits != 1:
            raise ValueError(upstream + ": rule hit " + str(hits) + " times: " + pattern.pattern[:40])
    if LEFTOVER_RE.search(text) or text.count("Base64.get") != 2:
        raise ValueError(upstream + ": reflective code left or a method not retargeted")
    return text.encode(ENCODING)


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import donor_overlay
    raise SystemExit(donor_overlay.run(sys.modules[__name__]))
