// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3Hash256;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Authenticated detached envelope for one already-computed {@link M3FileSnapshot}.
 *
 * <p>This is a transfer/storage projection, not a replacement for the existing semantic MIDX,
 * event, or physical source-facts images. It contains no raw source payload and performs no source
 * reread. The body is streamed after its exact byte length is computed from the immutable snapshot,
 * and the SHA-256 trailer authenticates the header and every body lane.</p>
 */
public final class M3FileSnapshotCodec {
  private static final long MAGIC = 0x4d49445853433031L; // "MIDXSC01"
  private static final int FORMAT = 1;
  private static final int FLAG_SOURCE_FACTS = 1;
  private static final int FLAG_ROLLING_LANES = 1 << 1;
  private static final int KNOWN_FLAGS = FLAG_SOURCE_FACTS | FLAG_ROLLING_LANES;
  private static final int DIGEST_BYTES = M3Hash256.BYTES;
  private static final int MAX_STRING_BYTES = 1 * 1024 * 1024;
  private static final int MAX_ARRAY_LENGTH = 16_000_000;
  private static final long MAX_BODY_BYTES = 512L * 1024L * 1024L - DIGEST_BYTES;

  private M3FileSnapshotCodec() {}

  /** Write one complete MIDXSC01 envelope without closing the caller-owned stream. */
  public static void write(M3FileSnapshot snapshot, OutputStream output) throws IOException {
    SnapshotValues values = new SnapshotValues(Objects.requireNonNull(snapshot, "snapshot"));
    long bodyBytes = bodyBytes(values);
    if (bodyBytes > MAX_BODY_BYTES) {
      throw new IOException("M3File snapshot body is too large: " + bodyBytes);
    }
    MessageDigest digest = sha256Digest();
    DigestOutputStream digestOutput = new DigestOutputStream(output, digest);
    DataOutputStream out = new DataOutputStream(digestOutput);
    out.writeLong(MAGIC);
    out.writeInt(FORMAT);
    out.writeInt(values.flags());
    out.writeLong(bodyBytes);
    out.writeInt(values.tokenCount());
    out.writeInt(values.uniqueTokenCount());
    out.writeInt(values.tokenIds.length);
    out.writeInt(values.lineStarts.length);
    out.writeInt(values.postings.length);
    writeBody(out, values);
    out.flush();
    digestOutput.enabled = false;
    out.write(digest.digest());
    out.flush();
  }

  /** Read and authenticate one complete MIDXSC01 envelope without closing the caller-owned stream. */
  public static M3FileSnapshot read(InputStream input) throws IOException {
    Objects.requireNonNull(input, "input");
    MessageDigest digest = sha256Digest();
    DigestInputStream digestInput = new DigestInputStream(input, digest);
    DataInputStream header = new DataInputStream(digestInput);
    try {
      if (header.readLong() != MAGIC) throw new IOException("invalid M3File snapshot magic");
      if (header.readInt() != FORMAT) throw new IOException("unsupported M3File snapshot format");
      int flags = header.readInt();
      if ((flags & ~KNOWN_FLAGS) != 0) throw new IOException("unknown M3File snapshot flags");
      long bodyBytes = header.readLong();
      if (bodyBytes < 0L || bodyBytes > MAX_BODY_BYTES) {
        throw new IOException("invalid M3File snapshot body length");
      }
      int tokenCount = readCount(header.readInt(), "token", MAX_ARRAY_LENGTH);
      int uniqueCount = readCount(header.readInt(), "unique token", MAX_ARRAY_LENGTH);
      int resolverCount = readCount(header.readInt(), "resolver token", MAX_ARRAY_LENGTH);
      int lineCount = readCount(header.readInt(), "line", MAX_ARRAY_LENGTH);
      int postingCount = readCount(header.readInt(), "posting", MAX_ARRAY_LENGTH);
      if (uniqueCount > tokenCount || resolverCount < uniqueCount || lineCount == 0) {
        throw new IOException("invalid M3File snapshot header shape");
      }

      LimitedInputStream limited = new LimitedInputStream(digestInput, bodyBytes);
      DataInputStream body = new DataInputStream(limited);
      long[] tokenIds = readLongs(body, resolverCount, "lexicon token IDs");
      int[] tokenStarts = readInts(body, uniqueCount + 1, "lexicon token starts");
      int[] localHandles = readInts(body, tokenCount, "local handles");
      byte[] tokenKinds = readBytes(body, tokenCount, "token kinds");
      int[] utf16Offsets = readInts(body, tokenCount + 1, "UTF-16 offsets");
      int[] lineStarts = readInts(body, lineCount, "line starts");
      int[] frequencies = readInts(body, uniqueCount, "frequencies");
      int[] firstOccurrences = readInts(body, uniqueCount, "first occurrences");
      int[] lastOccurrences = readInts(body, uniqueCount, "last occurrences");
      int[] postingStarts = readInts(body, uniqueCount + 1, "posting starts");
      int[] postings = readInts(body, postingCount, "postings");
      boolean hasRolling = body.readBoolean();
      if (hasRolling != ((flags & FLAG_ROLLING_LANES) != 0)) {
        throw new IOException("rolling-lane flag mismatch");
      }
      long[] rollingPrefix = hasRolling ? readLongs(body, tokenCount + 1, "rolling prefix") : null;
      long[] rollingPowers = hasRolling ? readLongs(body, tokenCount + 1, "rolling powers") : null;
      M3FilePrimitiveSnapshot primitive =
          new M3FilePrimitiveSnapshot(
              localHandles,
              tokenKinds,
              utf16Offsets,
              lineStarts,
              frequencies,
              firstOccurrences,
              lastOccurrences,
              postingStarts,
              postings,
              rollingPrefix,
              rollingPowers);
      M3FileLexiconSnapshot lexicon = new M3FileLexiconSnapshot(tokenIds, tokenStarts);

      M3FileSourceFactsSnapshot sourceFacts = null;
      if ((flags & FLAG_SOURCE_FACTS) != 0) {
        sourceFacts = readSourceFacts(body, tokenCount, uniqueCount);
      }
      if (limited.remaining() != 0L) throw new IOException("trailing M3File snapshot body bytes");
      digestInput.enabled = false;
      byte[] expectedDigest = new byte[DIGEST_BYTES];
      header.readFully(expectedDigest);
      if (!MessageDigest.isEqual(expectedDigest, digest.digest())) {
        throw new IOException("M3File snapshot checksum mismatch");
      }
      if (digestInput.read() != -1) throw new IOException("trailing M3File snapshot bytes");
      return new M3FileSnapshot(lexicon, primitive, sourceFacts);
    } catch (EOFException | RuntimeException malformed) {
      throw new IOException("malformed M3File snapshot", malformed);
    }
  }

  private static void writeBody(DataOutputStream out, SnapshotValues values) throws IOException {
    writeLongs(out, values.tokenIds);
    writeInts(out, values.tokenStarts);
    writeInts(out, values.localHandles);
    writeBytes(out, values.tokenKinds);
    writeInts(out, values.utf16Offsets);
    writeInts(out, values.lineStarts);
    writeInts(out, values.frequencies);
    writeInts(out, values.firstOccurrences);
    writeInts(out, values.lastOccurrences);
    writeInts(out, values.postingStarts);
    writeInts(out, values.postings);
    out.writeBoolean(values.rollingPrefix != null);
    if (values.rollingPrefix != null) {
      writeLongs(out, values.rollingPrefix);
      writeLongs(out, values.rollingPowers);
    }
    if (values.sourceFacts != null) {
      writeAttributes(out, values.attributesBefore);
      writeAttributes(out, values.attributesAfter);
      writeByteFacts(out, values);
      writeLongs(out, values.utf8Offsets);
      writeInts(out, values.codePointOffsets);
      writeBytes(out, values.tokenSha256);
      writeLongs(out, values.prefixA);
      writeLongs(out, values.prefixB);
      writeLongs(out, values.powersA);
      writeLongs(out, values.powersB);
    }
  }

  private static M3FileSourceFactsSnapshot readSourceFacts(
      DataInputStream in, int tokenCount, int uniqueCount) throws IOException {
    M3FileAttributes before = readAttributes(in);
    M3FileAttributes after = readAttributes(in);
    M3FileByteFacts byteFacts = readByteFacts(in);
    long[] utf8Offsets = readLongs(in, tokenCount + 1, "UTF-8 offsets");
    int[] codePointOffsets = readInts(in, tokenCount + 1, "code-point offsets");
    byte[] tokenSha256 = readBytes(in, Math.multiplyExact(uniqueCount, DIGEST_BYTES), "token SHA-256");
    long[] prefixA = readLongs(in, tokenCount + 1, "range prefix A");
    long[] prefixB = readLongs(in, tokenCount + 1, "range prefix B");
    long[] powersA = readLongs(in, tokenCount + 1, "range powers A");
    long[] powersB = readLongs(in, tokenCount + 1, "range powers B");
    try {
      return M3FileSourceFacts.restore(
              before,
              after,
              byteFacts,
              utf8Offsets,
              codePointOffsets,
              tokenSha256,
              prefixA,
              prefixB,
              powersA,
              powersB,
              tokenCount,
              uniqueCount)
          .snapshot();
    } catch (RuntimeException invalid) {
      throw new IOException("invalid serialized M3File source facts", invalid);
    }
  }

  private static void writeByteFacts(DataOutputStream out, SnapshotValues values)
      throws IOException {
    out.writeLong(values.byteCount);
    out.write(values.fileSha256.bytes());
    for (long frequency : values.histogram) out.writeLong(frequency);
    writeBytes(out, values.prefix);
    writeBytes(out, values.suffix);
    out.writeInt(values.uniqueByteValues);
    out.writeLong(Double.doubleToLongBits(values.entropy));
    out.writeInt(values.chunkLengths.length);
    for (int chunk = 0; chunk < values.chunkLengths.length; chunk++) {
      out.writeInt(values.chunkLengths[chunk]);
      out.write(values.chunkSha256, Math.multiplyExact(chunk, DIGEST_BYTES), DIGEST_BYTES);
    }
  }

  private static M3FileByteFacts readByteFacts(DataInputStream in) throws IOException {
    long byteCount = in.readLong();
    M3Hash256 sha256 = M3Hash256.of(readExact(in, DIGEST_BYTES, "file SHA-256"));
    long[] histogram = new long[256];
    for (int index = 0; index < histogram.length; index++) histogram[index] = in.readLong();
    byte[] prefix = readVariableBytes(in, M3FileByteFacts.SIGNATURE_BYTES, "prefix");
    byte[] suffix = readVariableBytes(in, M3FileByteFacts.SIGNATURE_BYTES, "suffix");
    int unique = readCount(in.readInt(), "unique byte", 256);
    double entropy = Double.longBitsToDouble(in.readLong());
    int chunkCount = readCount(in.readInt(), "chunk", MAX_ARRAY_LENGTH);
    int[] chunkLengths = new int[chunkCount];
    byte[] chunkSha256 = new byte[Math.multiplyExact(chunkCount, DIGEST_BYTES)];
    for (int chunk = 0; chunk < chunkCount; chunk++) {
      chunkLengths[chunk] = in.readInt();
      in.readFully(chunkSha256, Math.multiplyExact(chunk, DIGEST_BYTES), DIGEST_BYTES);
    }
    try {
      return M3FileByteFacts.restore(
          byteCount,
          sha256,
          histogram,
          prefix,
          suffix,
          unique,
          entropy,
          chunkSha256,
          chunkLengths);
    } catch (RuntimeException invalid) {
      throw new IOException("invalid serialized M3File byte facts", invalid);
    }
  }

  private static void writeAttributes(DataOutputStream out, M3FileAttributes attributes)
      throws IOException {
    writeString(out, attributes.normalizedPath());
    out.writeLong(attributes.size());
    writeFileTime(out, attributes.creationTime());
    writeFileTime(out, attributes.lastModifiedTime());
    writeFileTime(out, attributes.lastAccessTime());
    out.writeBoolean(attributes.isRegularFile());
    out.writeBoolean(attributes.isDirectory());
    out.writeBoolean(attributes.isSymbolicLinkPath());
    out.writeBoolean(attributes.isOther());
    writeString(out, attributes.fileKey());
    writeString(out, attributes.owner());
    writeString(out, attributes.group());
    out.writeBoolean(attributes.posixAvailable());
    int permissions = 0;
    for (PosixFilePermission permission : attributes.posixPermissions()) {
      permissions |= 1 << permission.ordinal();
    }
    out.writeInt(permissions);
    out.writeBoolean(attributes.dosAvailable());
    out.writeBoolean(attributes.dosArchive());
    out.writeBoolean(attributes.dosHidden());
    out.writeBoolean(attributes.dosReadOnly());
    out.writeBoolean(attributes.dosSystem());
    writeStrings(out, attributes.aclEntries());
    writeMap(out, attributes.userDefinedAttributesBase64());
    writeMap(out, attributes.providerAttributes());
  }

  private static M3FileAttributes readAttributes(DataInputStream in) throws IOException {
    String path = readString(in, "normalized path");
    long size = in.readLong();
    FileTime creation = readFileTime(in);
    FileTime modified = readFileTime(in);
    FileTime access = readFileTime(in);
    boolean regular = in.readBoolean();
    boolean directory = in.readBoolean();
    boolean symbolic = in.readBoolean();
    boolean other = in.readBoolean();
    String fileKey = readString(in, "file key");
    String owner = readString(in, "owner");
    String group = readString(in, "group");
    boolean posix = in.readBoolean();
    int permissionBits = in.readInt();
    PosixFilePermission[] values = PosixFilePermission.values();
    if ((permissionBits & ~((1 << values.length) - 1)) != 0) {
      throw new IOException("invalid serialized POSIX permission bits");
    }
    EnumSet<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
    for (int ordinal = 0; ordinal < values.length; ordinal++) {
      if ((permissionBits & (1 << ordinal)) != 0) permissions.add(values[ordinal]);
    }
    boolean dos = in.readBoolean();
    boolean archive = in.readBoolean();
    boolean hidden = in.readBoolean();
    boolean readOnly = in.readBoolean();
    boolean system = in.readBoolean();
    List<String> acl = readStrings(in, "ACL");
    Map<String, String> user = readMap(in, "user attributes");
    Map<String, String> provider = readMap(in, "provider attributes");
    try {
      return M3FileAttributes.restore(
          path,
          size,
          creation,
          modified,
          access,
          regular,
          directory,
          symbolic,
          other,
          fileKey,
          owner,
          group,
          posix,
          permissions,
          dos,
          archive,
          hidden,
          readOnly,
          system,
          acl,
          user,
          provider);
    } catch (RuntimeException invalid) {
      throw new IOException("invalid serialized filesystem attributes", invalid);
    }
  }

  private static void writeFileTime(DataOutputStream out, FileTime time) throws IOException {
    Instant instant = time.toInstant();
    out.writeLong(instant.getEpochSecond());
    out.writeInt(instant.getNano());
  }

  private static FileTime readFileTime(DataInputStream in) throws IOException {
    try {
      return FileTime.from(Instant.ofEpochSecond(in.readLong(), in.readInt()));
    } catch (DateTimeException | ArithmeticException invalid) {
      throw new IOException("invalid serialized file time", invalid);
    }
  }

  private static void writeStrings(DataOutputStream out, List<String> values) throws IOException {
    out.writeInt(values.size());
    for (String value : values) writeString(out, value);
  }

  private static List<String> readStrings(DataInputStream in, String label) throws IOException {
    int count = readCount(in.readInt(), label, MAX_ARRAY_LENGTH);
    List<String> result = new ArrayList<>(count);
    for (int index = 0; index < count; index++) result.add(readString(in, label + " value"));
    return List.copyOf(result);
  }

  private static void writeMap(DataOutputStream out, Map<String, String> values) throws IOException {
    out.writeInt(values.size());
    for (Map.Entry<String, String> entry : new TreeMap<>(values).entrySet()) {
      writeString(out, entry.getKey());
      writeString(out, entry.getValue());
    }
  }

  private static Map<String, String> readMap(DataInputStream in, String label) throws IOException {
    int count = readCount(in.readInt(), label, MAX_ARRAY_LENGTH);
    Map<String, String> result = new LinkedHashMap<>();
    String previous = null;
    for (int index = 0; index < count; index++) {
      String key = readString(in, label + " key");
      if (key == null || (previous != null && previous.compareTo(key) >= 0)) {
        throw new IOException("serialized " + label + " keys are not sorted");
      }
      String value = readString(in, label + " value");
      if (value == null) throw new IOException("serialized " + label + " value is null");
      result.put(key, value);
      previous = key;
    }
    return result;
  }

  private static void writeString(DataOutputStream out, String value) throws IOException {
    if (value == null) {
      out.writeInt(-1);
      return;
    }
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_STRING_BYTES) throw new IOException("serialized string is too large");
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  private static String readString(DataInputStream in, String label) throws IOException {
    int length = in.readInt();
    if (length == -1) return null;
    if (length < 0 || length > MAX_STRING_BYTES) {
      throw new IOException("invalid serialized " + label + " length");
    }
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(readExact(in, length, label)))
          .toString();
    } catch (CharacterCodingException invalid) {
      throw new IOException("invalid UTF-8 in serialized " + label, invalid);
    }
  }

  private static void writeBytes(DataOutputStream out, byte[] values) throws IOException {
    if (values.length > MAX_BODY_BYTES) throw new IOException("serialized byte lane is too large");
    out.writeInt(values.length);
    out.write(values);
  }

  private static byte[] readBytes(DataInputStream in, int expected, String label) throws IOException {
    int length = in.readInt();
    if (length != expected) throw new IOException("invalid serialized " + label + " length");
    return readExact(in, length, label);
  }

  private static byte[] readVariableBytes(DataInputStream in, int maximum, String label)
      throws IOException {
    int length = in.readInt();
    if (length < 0 || length > maximum) throw new IOException("invalid serialized " + label + " length");
    return readExact(in, length, label);
  }

  private static void writeLongs(DataOutputStream out, long[] values) throws IOException {
    out.writeInt(values.length);
    for (long value : values) out.writeLong(value);
  }

  private static long[] readLongs(DataInputStream in, int expected, String label) throws IOException {
    int length = readCount(in.readInt(), label, MAX_ARRAY_LENGTH);
    if (length != expected) throw new IOException("invalid serialized " + label + " length");
    long[] values = new long[length];
    for (int index = 0; index < length; index++) values[index] = in.readLong();
    return values;
  }

  private static void writeInts(DataOutputStream out, int[] values) throws IOException {
    out.writeInt(values.length);
    for (int value : values) out.writeInt(value);
  }

  private static int[] readInts(DataInputStream in, int expected, String label) throws IOException {
    int length = readCount(in.readInt(), label, MAX_ARRAY_LENGTH);
    if (length != expected) throw new IOException("invalid serialized " + label + " length");
    int[] values = new int[length];
    for (int index = 0; index < length; index++) values[index] = in.readInt();
    return values;
  }

  private static byte[] readExact(DataInputStream in, int length, String label) throws IOException {
    if (length < 0 || length > MAX_BODY_BYTES) throw new IOException("invalid " + label + " length");
    byte[] values = new byte[length];
    in.readFully(values);
    return values;
  }

  private static int readCount(int value, String label, int maximum) throws IOException {
    if (value < 0 || value > maximum) throw new IOException("invalid serialized " + label + " count");
    return value;
  }

  private static MessageDigest sha256Digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError("SHA-256 is required by the JDK", impossible);
    }
  }

  private static long bodyBytes(SnapshotValues values) throws IOException {
    long total = 0L;
    total = add(total, longsBytes(values.tokenIds));
    total = add(total, intsBytes(values.tokenStarts));
    total = add(total, intsBytes(values.localHandles));
    total = add(total, bytesBytes(values.tokenKinds));
    total = add(total, intsBytes(values.utf16Offsets));
    total = add(total, intsBytes(values.lineStarts));
    total = add(total, intsBytes(values.frequencies));
    total = add(total, intsBytes(values.firstOccurrences));
    total = add(total, intsBytes(values.lastOccurrences));
    total = add(total, intsBytes(values.postingStarts));
    total = add(total, intsBytes(values.postings));
    total = add(total, 1L);
    if (values.rollingPrefix != null) {
      total = add(total, longsBytes(values.rollingPrefix));
      total = add(total, longsBytes(values.rollingPowers));
    }
    if (values.sourceFacts != null) {
      total = add(total, attributesBytes(values.attributesBefore));
      total = add(total, attributesBytes(values.attributesAfter));
      total = add(total, 8L + DIGEST_BYTES + 256L * Long.BYTES + bytesBytes(values.prefix));
      total = add(total, bytesBytes(values.suffix) + Integer.BYTES + Long.BYTES + Integer.BYTES);
      total = add(total, (long) values.chunkLengths.length * (Integer.BYTES + DIGEST_BYTES));
      total = add(total, longsBytes(values.utf8Offsets));
      total = add(total, intsBytes(values.codePointOffsets));
      total = add(total, bytesBytes(values.tokenSha256));
      total = add(total, longsBytes(values.prefixA));
      total = add(total, longsBytes(values.prefixB));
      total = add(total, longsBytes(values.powersA));
      total = add(total, longsBytes(values.powersB));
    }
    if (total > MAX_BODY_BYTES) throw new IOException("M3File snapshot body is too large");
    return total;
  }

  private static long attributesBytes(M3FileAttributes attributes) throws IOException {
    long total = 0L;
    total = add(total, stringBytes(attributes.normalizedPath()));
    total = add(total, Long.BYTES + 3L * (Long.BYTES + Integer.BYTES));
    total = add(total, 4L); // basic-file booleans
    total = add(total, stringBytes(attributes.fileKey()));
    total = add(total, stringBytes(attributes.owner()));
    total = add(total, stringBytes(attributes.group()));
    total = add(total, 1L + Integer.BYTES); // POSIX availability and permission bits
    total = add(total, 5L); // DOS availability and four DOS flags
    total = add(total, stringsBytes(attributes.aclEntries()));
    total = add(total, mapBytes(attributes.userDefinedAttributesBase64()));
    total = add(total, mapBytes(attributes.providerAttributes()));
    return total;
  }

  private static long stringsBytes(List<String> values) throws IOException {
    long total = Integer.BYTES;
    for (String value : values) total = add(total, stringBytes(value));
    return total;
  }

  private static long mapBytes(Map<String, String> values) throws IOException {
    long total = Integer.BYTES;
    for (Map.Entry<String, String> entry : values.entrySet()) {
      total = add(total, stringBytes(entry.getKey()));
      total = add(total, stringBytes(entry.getValue()));
    }
    return total;
  }

  private static long stringBytes(String value) throws IOException {
    if (value == null) return Integer.BYTES;
    int length = value.getBytes(StandardCharsets.UTF_8).length;
    if (length > MAX_STRING_BYTES) throw new IOException("serialized string is too large");
    return Integer.BYTES + length;
  }

  private static long longsBytes(long[] values) { return Integer.BYTES + Long.BYTES * (long) values.length; }

  private static long intsBytes(int[] values) { return Integer.BYTES + Integer.BYTES * (long) values.length; }

  private static long bytesBytes(byte[] values) { return Integer.BYTES + values.length; }

  private static long add(long left, long right) throws IOException {
    try {
      return Math.addExact(left, right);
    } catch (ArithmeticException overflow) {
      throw new IOException("M3File snapshot size overflow", overflow);
    }
  }

  private static final class SnapshotValues {
    private final long[] tokenIds;
    private final int[] tokenStarts;
    private final int[] localHandles;
    private final byte[] tokenKinds;
    private final int[] utf16Offsets;
    private final int[] lineStarts;
    private final int[] frequencies;
    private final int[] firstOccurrences;
    private final int[] lastOccurrences;
    private final int[] postingStarts;
    private final int[] postings;
    private final long[] rollingPrefix;
    private final long[] rollingPowers;
    private final M3FileSourceFactsSnapshot sourceFacts;
    private final M3FileAttributes attributesBefore;
    private final M3FileAttributes attributesAfter;
    private final long byteCount;
    private final M3Hash256 fileSha256;
    private final long[] histogram;
    private final byte[] prefix;
    private final byte[] suffix;
    private final int uniqueByteValues;
    private final double entropy;
    private final byte[] chunkSha256;
    private final int[] chunkLengths;
    private final long[] utf8Offsets;
    private final int[] codePointOffsets;
    private final byte[] tokenSha256;
    private final long[] prefixA;
    private final long[] prefixB;
    private final long[] powersA;
    private final long[] powersB;

    private SnapshotValues(M3FileSnapshot snapshot) {
      M3FileLexiconSnapshot lexicon = snapshot.lexiconSnapshot();
      M3FilePrimitiveSnapshot primitive = snapshot.primitiveSnapshot();
      tokenIds = lexicon.tokenIds();
      tokenStarts = lexicon.tokenStarts();
      localHandles = primitive.localHandles();
      tokenKinds = primitive.tokenKinds();
      utf16Offsets = primitive.utf16Offsets();
      lineStarts = primitive.lineStarts();
      frequencies = primitive.frequencies();
      firstOccurrences = primitive.firstOccurrences();
      lastOccurrences = primitive.lastOccurrences();
      postingStarts = primitive.postingStarts();
      postings = primitive.postings();
      rollingPrefix = primitive.hasRollingLanes() ? primitive.rollingPrefix() : null;
      rollingPowers = primitive.hasRollingLanes() ? primitive.rollingPowers() : null;
      sourceFacts = snapshot.hasSourceFacts() ? snapshot.sourceFactsSnapshot() : null;
      if (sourceFacts == null) {
        attributesBefore = null;
        attributesAfter = null;
        byteCount = 0L;
        fileSha256 = null;
        histogram = null;
        prefix = null;
        suffix = null;
        uniqueByteValues = 0;
        entropy = 0.0d;
        chunkSha256 = null;
        chunkLengths = null;
        utf8Offsets = null;
        codePointOffsets = null;
        tokenSha256 = null;
        prefixA = null;
        prefixB = null;
        powersA = null;
        powersB = null;
      } else {
        attributesBefore = sourceFacts.attributesBeforeRead();
        attributesAfter = sourceFacts.attributesAfterRead();
        M3FileByteFactsSnapshot byteFacts = sourceFacts.byteFactsSnapshot();
        byteCount = byteFacts.byteCount();
        fileSha256 = byteFacts.sha256();
        histogram = byteFacts.byteHistogram();
        prefix = byteFacts.prefixBytes();
        suffix = byteFacts.suffixBytes();
        uniqueByteValues = byteFacts.uniqueByteValues();
        entropy = byteFacts.shannonEntropyBitsPerByte();
        chunkSha256 = byteFacts.chunkSha256Bytes();
        chunkLengths = byteFacts.chunkLengths();
        utf8Offsets = sourceFacts.utf8Offsets();
        codePointOffsets = sourceFacts.codePointOffsets();
        tokenSha256 = sourceFacts.tokenSha256Bytes();
        prefixA = sourceFacts.prefixALane();
        prefixB = sourceFacts.prefixBLane();
        powersA = sourceFacts.powerALane();
        powersB = sourceFacts.powerBLane();
      }
    }

    private int flags() {
      return (sourceFacts == null ? 0 : FLAG_SOURCE_FACTS)
          | (rollingPrefix == null ? 0 : FLAG_ROLLING_LANES);
    }

    private int tokenCount() { return localHandles.length; }

    private int uniqueTokenCount() { return frequencies.length; }
  }

  private static final class DigestOutputStream extends OutputStream {
    private final OutputStream delegate;
    private final MessageDigest digest;
    private boolean enabled = true;

    private DigestOutputStream(OutputStream delegate, MessageDigest digest) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
      this.digest = digest;
    }

    @Override
    public void write(int value) throws IOException {
      delegate.write(value);
      if (enabled) digest.update((byte) value);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
      Objects.checkFromIndexSize(offset, length, bytes.length);
      delegate.write(bytes, offset, length);
      if (enabled) digest.update(bytes, offset, length);
    }

    @Override
    public void flush() throws IOException { delegate.flush(); }
  }

  private static final class DigestInputStream extends InputStream {
    private final InputStream delegate;
    private final MessageDigest digest;
    private boolean enabled = true;

    private DigestInputStream(InputStream delegate, MessageDigest digest) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
      this.digest = digest;
    }

    @Override
    public int read() throws IOException {
      int value = delegate.read();
      if (enabled && value >= 0) digest.update((byte) value);
      return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
      Objects.checkFromIndexSize(offset, length, bytes.length);
      int read = delegate.read(bytes, offset, length);
      if (enabled && read > 0) digest.update(bytes, offset, read);
      return read;
    }
  }

  private static final class LimitedInputStream extends InputStream {
    private final InputStream delegate;
    private long remaining;

    private LimitedInputStream(InputStream delegate, long remaining) {
      this.delegate = delegate;
      this.remaining = remaining;
    }

    @Override
    public int read() throws IOException {
      if (remaining == 0L) return -1;
      int value = delegate.read();
      if (value >= 0) remaining--;
      return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
      Objects.checkFromIndexSize(offset, length, bytes.length);
      if (length == 0) return 0;
      if (remaining == 0L) return -1;
      int bounded = (int) Math.min((long) length, remaining);
      int read = delegate.read(bytes, offset, bounded);
      if (read > 0) remaining -= read;
      return read;
    }

    private long remaining() { return remaining; }
  }
}
