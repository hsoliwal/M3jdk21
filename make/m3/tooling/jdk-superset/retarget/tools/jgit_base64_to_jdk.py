#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""JGIT_BASE64_TO_JDK v1: JGit's util.Base64 encodes and decodes with java.base's java.util.Base64.

JGit 5.13 carries its own base64, org.eclipse.jgit.util.Base64 (derived from the iHarder public-domain library), for
HTTP authentication, Amazon S3 and WalkEncryption. java.base owns base64: java.util.Base64 is Java 8 API, within the
donor's --release 8, and HotSpot compiles its loops to vector instructions. This recipe keeps the public class and its
four public methods:

- encodeBytes(source, off, len) -> java.util.Base64's encoder over ByteBuffer.wrap(source, off, len), with no copy of
  the input. The private encode3to4 goes; encodeBytes(source) still calls the three-argument form.
- decode(source, off, len) -> java.util.Base64's decoder when source is not null and the range is valid, non-empty and
  a multiple of four bytes long. There java.base accepts exactly the canonical form (alphabet characters, with '='
  padding only in the last group), on which JGit's loop gives the same bytes. Any other input is decoded by the
  upstream loop, kept as the private decodeLenient, with its own results, exceptions and messages. That covers white
  space (JGit skips it), bytes with the high bit set (JGit drops the bit), an unpadded final group (JGit drops it),
  input after the padding (JGit ignores it), invalid characters, invalid ranges and null.
- decode(String) still calls decode(byte[], int, int). The class Javadoc gains one paragraph.

encodeBytes with an invalid range now throws IndexOutOfBoundsException from ByteBuffer.wrap. Before, a negative length
threw NegativeArraySizeException, a range past the array threw ArrayIndexOutOfBoundsException, and a zero length
returned "" whatever the offset.

Custody: donor_overlay.py (DONOR_OVERLAY v2). JGit has no relocated (lib-intake) copy.

  python jgit_base64_to_jdk.py            apply (writes only what differs; re-running changes nothing)
  python jgit_base64_to_jdk.py --check    write nothing; exit 1 when any target differs
"""
from __future__ import annotations

from pathlib import Path
import re
import sys

RECIPE_ID = "JGIT_BASE64_TO_JDK v1"
TOOL_REL = "m3/tooling/jdk-superset/retarget/tools/jgit_base64_to_jdk.py"
DONORS = ("org.eclipse.jgit",)
TARGETS = ("org/eclipse/jgit/util/Base64.java",)
RELOCATED: dict = {}
ENCODING = "utf-8"

ANCHOR = "\t//FLLDCIM: M3 - " + RECIPE_ID + ": base64 is java.base's java.util.Base64, the JDK's one implementation\n"
EDITS = (
    ("import java.text.MessageFormat;\n", "import java.nio.ByteBuffer;\nimport java.text.MessageFormat;\n"),
    (" * </p>\n *\n * @author Robert Harder\n",
     " * </p>\n * <p>\n * M3JDK (" + RECIPE_ID + "): java.base's {@code java.util.Base64} encodes, and\n"
     " * decodes input in the canonical form. Other input keeps JGit's lenient\n"
     " * decoding loop, with its results and exceptions.\n"
     " * </p>\n *\n * @author Robert Harder\n"),
    ("public class Base64 {\n", "public class Base64 {\n" + ANCHOR),
)
ENCODE = ("\t\tByteBuffer encoded = java.util.Base64.getEncoder()\n"
          "\t\t\t\t.encode(ByteBuffer.wrap(source, off, len));\n"
          "\t\treturn new String(encoded.array(), 0, encoded.limit(), UTF_8);")
DECODE = """\tpublic static byte[] decode(byte[] source, int off, int len) {
\t\tif (source != null && off >= 0 && len > 0 && len <= source.length - off
\t\t\t\t&& len % 4 == 0) {
\t\t\ttry {
\t\t\t\tByteBuffer decoded = java.util.Base64.getDecoder()
\t\t\t\t\t\t.decode(ByteBuffer.wrap(source, off, len));
\t\t\t\tbyte[] out = decoded.array();
\t\t\t\treturn out.length == decoded.limit() ? out
\t\t\t\t\t\t: Arrays.copyOf(out, decoded.limit());
\t\t\t} catch (IllegalArgumentException notCanonical) {
\t\t\t\t// Not the canonical form: JGit's lenient loop decodes it.
\t\t\t}
\t\t}
\t\treturn decodeLenient(source, off, len);
\t}

\t/**
\t * JGit's lenient decoding loop, the upstream body of
\t * {@link #decode(byte[], int, int)}: white space is skipped, the high bit
\t * of each byte is dropped, an unpadded final group is dropped and input
\t * after the padding is ignored.
\t */
\tprivate static byte[] decodeLenient(byte[] source, int off, int len) {
"""
BODIES = (
    (re.compile(r"\t/\*\*\n\t \* Encodes up to three bytes .*?\n\t}\n\n", re.DOTALL), ""),
    (re.compile(r"(\tpublic static String encodeBytes\(byte\[\] source, int off, int len\) \{\n).*?(\n\t}\n)",
                re.DOTALL), lambda m: m.group(1) + ENCODE + m.group(2)),
    (re.compile(r"\tpublic static byte\[\] decode\(byte\[\] source, int off, int len\) \{\n"), lambda m: DECODE),
)
LEFTOVER_RE = re.compile(r"encode3to4")

POM_TEXT = (("- unchanged upstream source build for M3jdk21</name>",
             "- upstream source build for M3jdk21, base64 by java.base</name>"),
            ("; no upstream implementation changes. See PROVENANCE.json.</description>",
             "; one M3 overlay, " + RECIPE_ID + ": util.Base64 encodes, and decodes canonical input, with "
             "java.util.Base64. See PROVENANCE.json and OVERLAY_SOURCES.json.</description>"))
README_TEXT = (
    ("This directory compiles the unchanged {donor} sources from the published sources jar.\n",
     "This directory compiles the {donor} sources from the published sources jar with one\n"
     "M3 overlay, `" + RECIPE_ID + "` (`" + TOOL_REL + "`):\n"
     "`util.Base64` encodes with `java.util.Base64`, and decodes with it when the input is in the canonical form; other\n"
     "input keeps JGit's lenient decoding loop. The upstream file stays byte-identical under `upstream-excluded/`;\n"
     "`verify_source.py` re-derives the overlay from it.\n"),
    ("No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the\n"
     "compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with\n"
     "their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.",
     "The overlay's proof is `m3/evidence/java-base/BASE64_JGIT.md`. Outside it no speed, allocation or behaviour\n"
     "change is claimed. The published sources jar carries no test suite; the compile from source and the consumers'\n"
     "own tests are the proof lane. Upstream tests remain a follow-up with their own recipe. See `PROVENANCE.json` for\n"
     "the file-level pins and the runner receipt."),
)
MODIFICATIONS = ("M3 overlays " + RECIPE_ID + ": util.Base64 encodes with java.util.Base64, and decodes with it when "
                 "the input is in the canonical form; other input keeps JGit's lenient decoding loop (recipe "
                 + TOOL_REL + "; the unchanged upstream file is retained under upstream-excluded/). No other upstream "
                 "source changes; Maven descriptor and provenance files are M3jdk21 composition (Apache-2.0) and do "
                 "not relicense the donor.")


def overlay(original: bytes, upstream: str) -> bytes:
    """The recipe: pinned upstream bytes of JGit's util.Base64 -> its M3 overlay bytes."""
    text = original.decode(ENCODING)
    for old, new in EDITS:
        if text.count(old) != 1:
            raise ValueError(upstream + ": expected once: " + old.splitlines()[0])
        text = text.replace(old, new)
    for pattern, replacement in BODIES:
        text, hits = pattern.subn(replacement, text)
        if hits != 1:
            raise ValueError(upstream + ": rule hit " + str(hits) + " times: " + pattern.pattern[:40])
    if LEFTOVER_RE.search(text) or text.count("java.util.Base64.get") != 2 or text.count("decodeLenient(") != 2:
        raise ValueError(upstream + ": encoder loop left or a method not retargeted")
    return text.encode(ENCODING)


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import donor_overlay
    raise SystemExit(donor_overlay.run(sys.modules[__name__]))
