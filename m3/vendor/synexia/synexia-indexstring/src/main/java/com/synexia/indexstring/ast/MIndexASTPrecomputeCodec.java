// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Authenticated rebuildable cold image for document-local navigation precomputation.
 *
 * <p>The image contains only primitive navigation arrays. It is admitted only after the source,
 * language/spec identity, and every coordinate are checked against the already-loaded document.
 * Canonical pool identity is deliberately not persisted here; that boundary belongs to the future
 * pool-handle remapping proof.</p>
 */
public final class MIndexASTPrecomputeCodec {
  private static final byte[] MAGIC = "MIDXNAV1".getBytes(StandardCharsets.US_ASCII);
  private static final int VERSION = 1;
  private static final int PROJECTION_NAVIGATION = 2;
  private static final int MAX_TEXT_BYTES = 1 << 20;
  private static final int MAX_OCCURRENCES = 16_000_000;
  private static final int MAX_LEVELS = 32;
  private static final int MAX_ATOMS = 16_000_000;

  private MIndexASTPrecomputeCodec() {}

  public static void write(
      MIndexASTPartialCache.Key key,
      int languageId,
      MIndexASTPrecompute index,
      OutputStream output)
      throws IOException {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(output, "output");
    if (languageId < 0) throw new IOException("negative languageId");

    MIndexASTDocument document = index.document();
    validateIdentity(key, document);
    MIndexASTPrecompute.Snapshot payload = index.snapshot();
    MIndexASTPrecompute.restore(document, payload);

    MessageDigest digest = sha256();
    DigestOutputStream digestOutput = new DigestOutputStream(output, digest);
    DataOutputStream data = new DataOutputStream(digestOutput);
    data.write(MAGIC);
    data.writeInt(VERSION);
    data.writeInt(PROJECTION_NAVIGATION);
    data.writeInt(languageId);
    writeText(data, key.sourceRef());
    writeText(data, key.sourceUtf16Sha256());
    writeText(data, document.root().spec().identity());
    data.writeLong(key.astSpecFingerprint());
    data.writeInt(payload.occurrenceCount());
    data.writeInt(payload.levels());
    data.writeInt(payload.atomLimit());
    writeInts(data, payload.preorderToOccurrence());
    writeInts(data, payload.occurrenceToPreorder());
    writeInts(data, payload.subtreeSizes());
    writeInts(data, payload.childCounts());
    writeInts(data, payload.childOrdinals());
    writeInts(data, payload.ancestors());
    writeLongs(data, payload.powers());
    writeLongs(data, payload.kindPathHashes());
    writeLongs(data, payload.exactPathHashes());
    data.write(payload.preorderKinds());
    writeInts(data, payload.kindOffsets());
    writeInts(data, payload.occurrencesByKind());
    writeInts(data, payload.atomOffsets());
    writeInts(data, payload.occurrencesByAtom());
    data.flush();

    digestOutput.on(false);
    output.write(digest.digest());
  }

  public static MIndexASTPrecompute read(
      MIndexASTPartialCache.Key key,
      int languageId,
      MIndexASTDocument document,
      InputStream input)
      throws IOException {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(input, "input");
    if (languageId < 0) throw new IOException("negative languageId");

    MessageDigest digest = sha256();
    DigestInputStream digestInput = new DigestInputStream(input, digest);
    DataInputStream data = new DataInputStream(digestInput);
    byte[] magic = new byte[MAGIC.length];
    data.readFully(magic);
    if (!Arrays.equals(MAGIC, magic)) throw new IOException("invalid navigation magic");
    if (data.readInt() != VERSION) throw new IOException("unsupported navigation version");
    if (data.readInt() != PROJECTION_NAVIGATION) {
      throw new IOException("wrong navigation projection code");
    }
    if (data.readInt() != languageId) throw new IOException("navigation language mismatch");

    String sourceRef = readText(data);
    String sourceSha = readText(data);
    String specIdentity = readText(data);
    long fingerprint = data.readLong();
    int occurrenceCount = data.readInt();
    int levels = data.readInt();
    int atomLimit = data.readInt();

    if (!key.sourceRef().equals(sourceRef)
        || !key.sourceUtf16Sha256().equals(sourceSha)
        || !document.root().spec().identity().equals(specIdentity)
        || key.astSpecFingerprint() != fingerprint) {
      throw new IOException("navigation identity mismatch");
    }
    validateIdentity(key, document);
    validateHeader(document, occurrenceCount, levels, atomLimit);

    int[] preorderToOccurrence = readInts(data, occurrenceCount);
    int[] occurrenceToPreorder = readInts(data, occurrenceCount);
    int[] subtreeSizes = readInts(data, occurrenceCount);
    int[] childCounts = readInts(data, occurrenceCount);
    int[] childOrdinals = readInts(data, occurrenceCount);
    int[] ancestors = readInts(data, Math.multiplyExact(levels, occurrenceCount));
    long[] powers = readLongs(data, Math.addExact(occurrenceCount, 1));
    long[] kindPathHashes = readLongs(data, occurrenceCount);
    long[] exactPathHashes = readLongs(data, occurrenceCount);
    byte[] preorderKinds = new byte[occurrenceCount];
    data.readFully(preorderKinds);
    int expectedKindOffsets = com.sun.source.tree.Tree.Kind.values().length + 1;
    int[] kindOffsets = readInts(data, expectedKindOffsets);
    int[] occurrencesByKind = readInts(data, occurrenceCount);
    int[] atomOffsets = readInts(data, Math.addExact(atomLimit, 2));
    int[] occurrencesByAtom = readInts(data, occurrenceCount);

    digestInput.on(false);
    byte[] expectedDigest = new byte[32];
    data.readFully(expectedDigest);
    if (!Arrays.equals(expectedDigest, digest.digest())) {
      throw new IOException("navigation payload digest mismatch");
    }
    if (data.read() != -1) throw new IOException("trailing navigation bytes");

    MIndexASTPrecompute.Snapshot payload =
        new MIndexASTPrecompute.Snapshot(
            occurrenceCount,
            preorderToOccurrence,
            occurrenceToPreorder,
            subtreeSizes,
            childCounts,
            childOrdinals,
            levels,
            ancestors,
            powers,
            kindPathHashes,
            exactPathHashes,
            preorderKinds,
            kindOffsets,
            occurrencesByKind,
            atomLimit,
            atomOffsets,
            occurrencesByAtom);
    return MIndexASTPrecompute.restore(document, payload);
  }

  private static void validateIdentity(
      MIndexASTPartialCache.Key key,
      MIndexASTDocument document)
      throws IOException {
    if (!key.sourceRef().equals(document.path().materialize())
        || !key.sourceUtf16Sha256().equals(document.sourceUtf16Sha256Hex())
        || key.astSpecFingerprint() != document.root().astSpec().fingerprint()) {
      throw new IOException("navigation document identity mismatch");
    }
  }

  private static void validateHeader(
      MIndexASTDocument document,
      int occurrenceCount,
      int levels,
      int atomLimit)
      throws IOException {
    int expectedOccurrences = document.occurrenceCount();
    int expectedLevels =
        Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(expectedOccurrences));
    if (occurrenceCount != expectedOccurrences
        || occurrenceCount < 1
        || occurrenceCount > MAX_OCCURRENCES
        || levels != expectedLevels
        || levels < 1
        || levels > MAX_LEVELS
        || atomLimit < 0
        || atomLimit > MAX_ATOMS
        || atomLimit > document.root().pool().size()) {
      throw new IOException("navigation header is out of bounds or stale");
    }
  }

  private static void writeInts(DataOutputStream data, int[] values) throws IOException {
    data.writeInt(values.length);
    for (int value : values) data.writeInt(value);
  }

  private static void writeLongs(DataOutputStream data, long[] values) throws IOException {
    data.writeInt(values.length);
    for (long value : values) data.writeLong(value);
  }

  private static int[] readInts(DataInputStream data, int expected) throws IOException {
    if (data.readInt() != expected) throw new IOException("navigation int-array shape mismatch");
    int[] values = new int[expected];
    for (int index = 0; index < expected; index++) values[index] = data.readInt();
    return values;
  }

  private static long[] readLongs(DataInputStream data, int expected) throws IOException {
    if (data.readInt() != expected) throw new IOException("navigation long-array shape mismatch");
    long[] values = new long[expected];
    for (int index = 0; index < expected; index++) values[index] = data.readLong();
    return values;
  }

  private static void writeText(DataOutputStream data, String value) throws IOException {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_TEXT_BYTES) throw new IOException("navigation text is too large");
    data.writeInt(bytes.length);
    data.write(bytes);
  }

  private static String readText(DataInputStream data) throws IOException {
    int length = data.readInt();
    if (length < 0 || length > MAX_TEXT_BYTES) {
      throw new IOException("navigation text length is out of bounds");
    }
    byte[] bytes = new byte[length];
    data.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }
}

