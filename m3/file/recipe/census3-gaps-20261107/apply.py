#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe CENSUS3_GAPS v1 (m3/file): the RESTORE members of census pass 3 leaves P3-012 (M3File, M3FileProjection, M3FileSpan, M3FileSnapshot, M3FileEventWriter, M3FileEventReader).

Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored "sub" edits, every anchor
exactly once, LF payloads re-joined with the preimage's line ending). Every rule names the git blob sha of its
preimage at the base commit; a target must equal the preimage (the rule is applied) or the postimage (already
applied, no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without writing (every
preimage is verified before the first write). Idempotent: a second run reports changed=0 and EDITS.tsv (file,
before/after sha256, leaf, donor blob, rule) is a fixed point. --leaf applies one leaf's rules only; --check writes
nothing and exits 1 when a run would change a file. Donor: com.synexia develop 78cf2ad4c7e5 (blobs named per rule);
per-member decisions in DECISION.tsv (only RESTORE rows have edits here).

usage: python -I apply.py [--root DIR] [--check] [--leaf LEAF]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "0581643118d7ce05bfb9e884d80eb48d958a60df"
DONOR_COMMIT = "78cf2ad4c7e56f7b0ade5aa206c491bc866178a6"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "donor_blob", "rule")
ROOT_UP = 3


class Refused(Exception):
    pass


SRC = "m3/file/src/main/java/jdk/m3/file/"
A = "  //FLLDCIM: MRL - census3 P3-012, donor "
IMPORTS = "import java.io.IOException;\nimport java.nio.channels.WritableByteChannel;\nimport java.nio.charset.Charset;\n"

# leaf, path, preimage blob, donor blob, rule note, edits [(old, new)]
RULES = (
    ("P3-012", SRC + "M3File.java", "06336f349498f4d4dfccbfbd9bd7cd8447d3daf9",
     "develop MIndexFile blob d0f9abd3e320186154ea21c117e1589b518caad7",
     "RESTORE writeUtf8To(WritableByteChannel): indexed UTF-8 channel facade (develop 2298f0638e3c, 2026-11-02) through"
     " M3FileText.writeUtf8To; asMIndexString() is PRESENT_EVOLVED as asM3String()", [
         ("import java.nio.charset.Charset;\n", IMPORTS),
         ("  public M3FileText asM3String() {\n    return indexedText;\n  }\n\n",
          "  public M3FileText asM3String() {\n    return indexedText;\n  }\n\n"
          + A + "MIndexFile blob d0f9abd3e320: UTF-8 channel facade\n"
          "  /** Streams the complete indexed file payload as UTF-8 without joining its atoms. */\n"
          "  public long writeUtf8To(WritableByteChannel target) throws IOException {\n"
          "    return asM3String().writeUtf8To(target);\n  }\n\n"),
     ]),
    ("P3-012", SRC + "M3FileProjection.java", "f493620ed34b71ad9d4eb1d165fe982c70e541ad",
     "develop MIndexFileProjection blob c36a7907a73ff0187fac6c85d3d39d89b4770306",
     "RESTORE writeUtf8To(WritableByteChannel) (develop 1a2733080a6f); asMIndexString() PRESENT_EVOLVED as asM3String()", [
         ("import java.nio.charset.Charset;\n", IMPORTS),
         ("      if (indexedText == null) indexedText = built;\n      return indexedText;\n    }\n  }\n\n",
          "      if (indexedText == null) indexedText = built;\n      return indexedText;\n    }\n  }\n\n"
          + A + "MIndexFileProjection blob c36a7907a73f: UTF-8 channel facade\n"
          "  /** Streams the selected projection as UTF-8 without joining its atoms. */\n"
          "  public long writeUtf8To(WritableByteChannel target) throws IOException {\n"
          "    return asM3String().writeUtf8To(target);\n  }\n\n"),
     ]),
    ("P3-012", SRC + "M3FileSpan.java", "fb17e60de4a5e0cc9c55e4e65a271ecb40a0ebac",
     "develop MIndexFileSpan blob b5e4545df724a5367f32bf39b969d3163eea7480",
     "RESTORE writeUtf8To(WritableByteChannel) (develop 5cb480dacfdf); asMIndexString() PRESENT_EVOLVED as asM3String()", [
         ("import java.nio.charset.Charset;\n", IMPORTS),
         ("  public M3FileText asM3String() {\n"
          "    return file.asM3String().substring(utf16Start(), utf16End());\n  }\n\n",
          "  public M3FileText asM3String() {\n"
          "    return file.asM3String().substring(utf16Start(), utf16End());\n  }\n\n"
          + A + "MIndexFileSpan blob b5e4545df724: UTF-8 channel facade\n"
          "  /** Streams this token-boundary span as UTF-8 without joining its atoms. */\n"
          "  public long writeUtf8To(WritableByteChannel target) throws IOException {\n"
          "    return asM3String().writeUtf8To(target);\n  }\n\n"),
     ]),
    ("P3-012", SRC + "M3FileSnapshot.java", "a4853f3fba987507e67702099d1343faaa73029d",
     "develop MIndexFileSnapshot blob cb5f5f518b7bf96d20575c91536afda5337843bb",
     "RESTORE writeUtf8To(M3FileResolver, WritableByteChannel) (develop 0cde6220fbc2); the indexedText/sha256/"
     "tokenRangeSha256 rows are PRESENT_EVOLVED (IndexResolver -> M3FileResolver, MIndexString -> M3FileText)", [
         ("import java.nio.charset.Charset;\n", IMPORTS),
         ("  public M3FileText indexedText(M3FileResolver resolver) {\n"
          "    Objects.requireNonNull(resolver, \"resolver\");\n"
          "    return composeIndexedText(resolver, primitive.localHandles(), null);\n  }\n\n",
          "  public M3FileText indexedText(M3FileResolver resolver) {\n"
          "    Objects.requireNonNull(resolver, \"resolver\");\n"
          "    return composeIndexedText(resolver, primitive.localHandles(), null);\n  }\n\n"
          + A + "MIndexFileSnapshot blob cb5f5f518b7b: UTF-8 channel facade\n"
          "  /** Streams the detached indexed text as UTF-8 for the supplied resolver space without joining its atoms. */\n"
          "  public long writeUtf8To(M3FileResolver resolver, WritableByteChannel target) throws IOException {\n"
          "    return indexedText(Objects.requireNonNull(resolver, \"resolver\")).writeUtf8To(target);\n  }\n\n"),
     ]),
    ("P3-012", SRC + "M3FileEventWriter.java", "a3c4c668d012d50491d5cea2cb154987202e9204",
     "develop MIndexFileEventWriter blob ebc1d230c0688b7b0185cccd2c684c74a5f9a4ac",
     "RESTORE forChannel(WritableByteChannel) (develop 396a0e0263b8): channel writer factory over Channels.newOutputStream;"
     " wire format unchanged", [
         ("import java.io.OutputStream;\nimport java.nio.charset.StandardCharsets;\n",
          "import java.io.OutputStream;\nimport java.nio.channels.Channels;\nimport java.nio.channels.WritableByteChannel;\n"
          "import java.nio.charset.StandardCharsets;\n"),
         ("  public M3FileEventWriter(OutputStream output) throws IOException {\n    this(output, true);\n  }\n\n",
          "  public M3FileEventWriter(OutputStream output) throws IOException {\n    this(output, true);\n  }\n\n"
          + A + "MIndexFileEventWriter blob ebc1d230c068: channel writer factory (wire format unchanged)\n"
          "  /**\n"
          "   * Creates a writer that encodes the standard MIDXEVT2 stream into a caller-owned channel.\n"
          "   *\n"
          "   * <p>Closing this writer closes the bridged channel. The OutputStream constructor and the embedded\n"
          "   * event envelope behaviour remain unchanged.</p>\n"
          "   */\n"
          "  public static M3FileEventWriter forChannel(WritableByteChannel channel) throws IOException {\n"
          "    return new M3FileEventWriter(Channels.newOutputStream(Objects.requireNonNull(channel, \"channel\")));\n"
          "  }\n\n"),
     ]),
    ("P3-012", SRC + "M3FileEventReader.java", "467548f1c286a64f97b54a3a78d0a83a4a1ca598",
     "develop MIndexFileEventReader blob b7fc8cdd9008a351401b90462e094554b204d6d0",
     "RESTORE read(InputStream) (no-op sink), readIndexedText, readProjectionText, readSelectedText and the private"
     " IndexedTextCollector (develop f7e15c7731ed): indexed-text / kind-selection projection of the one validated event"
     " pass over M3FileTextBuilder.appendId; the frame constants, Summary components and helpers are PRESENT_EVOLVED,"
     " Summary.lexiconCount NEC", [
         ("  /** Read and validate one complete MIDXEVT2 stream without closing the caller-owned input. */\n"
          "  public static Summary read(InputStream input, M3FileStreamEvents.Sink sink)\n"
          "      throws IOException {\n"
          "    return read(input, sink, DEFAULT_MAX_FRAME_BYTES);\n  }\n\n",
          A + "MIndexFileEventReader blob b7fc8cdd9008: validating read without a sink\n"
          "  /** Reads and validates an event stream without retaining the event sequence. */\n"
          "  public static Summary read(InputStream input) throws IOException {\n"
          "    return read(input, M3FileStreamEvents.noOp());\n  }\n\n"
          "  /** Read and validate one complete MIDXEVT2 stream without closing the caller-owned input. */\n"
          "  public static Summary read(InputStream input, M3FileStreamEvents.Sink sink)\n"
          "      throws IOException {\n"
          "    return read(input, sink, DEFAULT_MAX_FRAME_BYTES);\n  }\n\n"
          + A + "MIndexFileEventReader blob b7fc8cdd9008: indexed-text projections of the validated pass\n"
          "  /**\n"
          "   * Reads the complete occurrence stream as resolver-owned indexed text in the same validated event\n"
          "   * pass. The reader publishes no value until the terminal digest is authenticated.\n"
          "   */\n"
          "  public static M3FileText readIndexedText(InputStream input, M3FileResolver resolver)\n"
          "      throws IOException {\n"
          "    return readSelectedText(input, resolver, -1);\n  }\n\n"
          "  /** Reads a selected token-kind projection in the same validated event pass. */\n"
          "  public static M3FileText readProjectionText(\n"
          "      InputStream input, M3FileResolver resolver, M3FileTokenKind... selectedKinds)\n"
          "      throws IOException {\n"
          "    return readSelectedText(input, resolver, M3FileProjection.maskOf(selectedKinds));\n  }\n\n"
          "  private static M3FileText readSelectedText(\n"
          "      InputStream input, M3FileResolver resolver, int kindMask) throws IOException {\n"
          "    IndexedTextCollector collector =\n"
          "        new IndexedTextCollector(Objects.requireNonNull(resolver, \"resolver\"), kindMask);\n"
          "    read(input, collector);\n"
          "    return collector.finish();\n  }\n\n"),
         ("  private static M3FileAttributes readMetadata(DataInputStream frame) throws IOException {\n",
          A + "MIndexFileEventReader blob b7fc8cdd9008: ID collector of the validated pass\n"
          "  /** Retains IDs, not source spellings; the reader remains the one validation authority. */\n"
          "  private static final class IndexedTextCollector implements M3FileStreamEvents.Sink {\n"
          "    private final M3FileTextBuilder indexedText;\n"
          "    private final int kindMask;\n"
          "    private final List<long[]> lexicon = new ArrayList<>();\n\n"
          "    private IndexedTextCollector(M3FileResolver resolver, int kindMask) {\n"
          "      indexedText = new M3FileTextBuilder(resolver);\n"
          "      this.kindMask = kindMask;\n    }\n\n"
          "    @Override\n"
          "    public void accept(M3FileStreamEvents.Event event) throws IOException {\n"
          "      if (event instanceof M3FileStreamEvents.Lexicon entry) {\n"
          "        if (entry.localHandle() != lexicon.size()) {\n"
          "          throw new IOException(\"non-contiguous MIDXEVT2 collector lexicon handle\");\n"
          "        }\n"
          "        lexicon.add(entry.resolverTokenIds());\n"
          "      } else if (event instanceof M3FileStreamEvents.Token token) {\n"
          "        if (token.localHandle() < 0 || token.localHandle() >= lexicon.size()) {\n"
          "          throw new IOException(\"MIDXEVT2 collector token has no lexicon entry\");\n"
          "        }\n"
          "        if ((kindMask & (1 << token.kind().ordinal())) != 0) {\n"
          "          for (long id : lexicon.get(token.localHandle())) {\n"
          "            indexedText.appendId(id);\n"
          "          }\n"
          "        }\n"
          "      }\n    }\n\n"
          "    private M3FileText finish() {\n"
          "      return indexedText.build();\n    }\n  }\n\n"
          "  private static M3FileAttributes readMetadata(DataInputStream frame) throws IOException {\n"),
     ]),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> str:
    return "\r\n" if b"\r\n" in data else "\n"


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("preimage blob %s is not in this repository (%s)" % (blob, failure)) from failure


def postimage(pre: bytes, edits, path: str) -> bytes:
    eol = eol_of(pre)
    text = pre.decode("utf-8")
    for old, new in edits:
        old, new = old.replace("\n", eol), new.replace("\n", eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, rel, blob, donor, note, edits in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, edits, rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED" and leaf not in (None, rleaf):
            state = "PENDING"
        elif state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += on_disk == post or state == "CHANGED"
        rows.append((rel, sha256(pre), sha256(post), rleaf, donor, note))
        print("EDIT %s %s %s" % (rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("CENSUS3_GAPS recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], DONOR_COMMIT[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[ROOT_UP]), help="repository root")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--leaf", choices=sorted({rule[0] for rule in RULES}), help="apply one leaf's rules only")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.leaf)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
