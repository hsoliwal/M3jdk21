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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Versioned authenticated sidecar for one-pass physical M3File source facts. */
public final class M3FileSourceFactsImage {
  private static final long MAGIC = 0x534d465341303031L; // "SMFSA001"
  private static final int FORMAT = 1;
  private static final int DIGEST_BYTES = M3Hash256.BYTES;
  private static final int MAX_IMAGE_BYTES = 512 * 1024 * 1024;
  private static final int MAX_STRING_BYTES = 1 * 1024 * 1024;
  private static final int MAX_ARRAY_LENGTH = 16_000_000;

  private M3FileSourceFactsImage() {}

  public static void write(M3File file, Path output) throws IOException {
    M3File source = Objects.requireNonNull(file, "file");
    if (!source.hasSourceFacts() || !source.sourceFacts().stableDuringRead()) {
      throw new IllegalArgumentException("source-facts image requires a stable physical admission");
    }
    Path target = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
    Path parent = target.getParent();
    if (parent != null) Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, "." + target.getFileName() + ".", ".tmp");
    try {
      writeBody(temporary, source, source.sourceFacts());
      try {
        Files.move(
            temporary,
            target,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException unsupported) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  /**
   * Write source facts retained by a detached snapshot, bound to its authenticated semantic image.
   */
  static void writeSnapshot(M3FileSnapshot snapshot, long fingerprint, Path output)
      throws IOException {
    M3FileSnapshot checked = Objects.requireNonNull(snapshot, "snapshot");
    M3FileSourceFactsSnapshot facts = checked.sourceFactsSnapshot();
    if (!facts.stableDuringRead()) {
      throw new IllegalArgumentException("source-facts image requires stable physical facts");
    }
    Path target = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
    Path parent = target.getParent();
    if (parent != null) Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, "." + target.getFileName() + ".", ".tmp");
    try {
      writeSnapshotBody(temporary, checked, fingerprint);
      try {
        Files.move(
            temporary, target,
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException unsupported) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  /** Read and attach detached source facts to a validated mapped semantic image. */
  static M3FileSnapshot readMappedSnapshot(Path image, MappedM3File mapped)
      throws IOException {
    return mapped.snapshot(readMappedFacts(image, mapped));
  }

  private static void writeSnapshotBody(
      Path output, M3FileSnapshot snapshot, long fingerprint) throws IOException {
    M3FileSourceFactsSnapshot facts = snapshot.sourceFactsSnapshot();
    M3FileByteFactsSnapshot bytes = facts.byteFactsSnapshot();
    try (OutputStream raw = Files.newOutputStream(output);
        DigestingBoundedOutputStream bounded =
            new DigestingBoundedOutputStream(raw, MAX_IMAGE_BYTES - DIGEST_BYTES);
        DataOutputStream out = new DataOutputStream(bounded)) {
      out.writeLong(MAGIC);
      out.writeInt(FORMAT);
      out.writeLong(fingerprint);
      out.writeInt(facts.tokenCount());
      out.writeInt(facts.uniqueTokenCount());
      writeAttributes(out, facts.attributesBeforeRead());
      writeAttributes(out, facts.attributesAfterRead());
      writeByteFacts(out, bytes);
      writeLongs(out, facts.utf8Offsets());
      writeInts(out, facts.codePointOffsets());
      writeBytes(out, facts.tokenSha256Bytes());
      writeLongs(out, facts.prefixALane());
      writeLongs(out, facts.prefixBLane());
      writeLongs(out, facts.powerALane());
      writeLongs(out, facts.powerBLane());
      out.flush();
      raw.write(bounded.finishDigest());
    } catch (RuntimeException invalid) {
      throw new IOException("cannot encode detached M3File source facts", invalid);
    }
  }

  /** Read and verify the sidecar, then attach its facts to the supplied immutable file. */
  public static M3File read(Path image, M3File file) throws IOException {
    return Objects.requireNonNull(file, "file").withSourceFacts(readFacts(image, file));
  }

  /** Read and verify facts without changing the supplied file object. */
  public static M3FileSourceFacts readFacts(Path image, M3File file) throws IOException {
    Path source = Objects.requireNonNull(image, "image").toAbsolutePath().normalize();
    M3File checkedFile = Objects.requireNonNull(file, "file");
    long fingerprint;
    try {
      fingerprint = M3FileImage.contentFingerprint64(checkedFile);
    } catch (RuntimeException invalid) {
      throw new IOException("supplied M3File has no durable identity", invalid);
    }
    return readFacts(source, fingerprint, checkedFile.tokenCount(), checkedFile.uniqueTokenCount());
  }

  /** Read and verify physical facts against a validated cold semantic image. */
  public static M3FileSourceFacts readMappedFacts(Path image, MappedM3File mapped)
      throws IOException {
    MappedM3File checkedMapped = Objects.requireNonNull(mapped, "mapped");
    return readFacts(
        image,
        checkedMapped.contentFingerprint64(),
        checkedMapped.tokenCount(),
        checkedMapped.uniqueTokenCount());
  }

  private static M3FileSourceFacts readFacts(
      Path image, long expectedFingerprint, int expectedTokenCount, int expectedUniqueCount)
      throws IOException {
    Path source = Objects.requireNonNull(image, "image").toAbsolutePath().normalize();
    long size = Files.size(source);
    if (size < DIGEST_BYTES || size > MAX_IMAGE_BYTES) {
      throw new IOException("invalid M3File source-facts image length");
    }
    long bodyLength = size - DIGEST_BYTES;
    try (InputStream raw = Files.newInputStream(source);
        DigestingBoundedInputStream bounded =
            new DigestingBoundedInputStream(raw, bodyLength);
        DataInputStream in = new DataInputStream(bounded)) {
      if (in.readLong() != MAGIC) throw new IOException("invalid source-facts image magic");
      if (in.readInt() != FORMAT) throw new IOException("unsupported source-facts image format");
      long fingerprint = in.readLong();
      int tokenCount = readCount(in.readInt(), "token", MAX_ARRAY_LENGTH);
      int uniqueCount = readCount(in.readInt(), "unique token", MAX_ARRAY_LENGTH);
      if (tokenCount != expectedTokenCount || uniqueCount != expectedUniqueCount) {
        throw new IOException("source-facts image/file shape mismatch");
      }
      if (fingerprint != expectedFingerprint) {
        throw new IOException("source-facts image is bound to another M3File image");
      }
      M3FileSourceFacts facts = readFacts(in, tokenCount, uniqueCount);
      if (bounded.remaining() != 0L) throw new IOException("trailing source-facts image bytes");
      byte[] expected = readExact(new DataInputStream(raw), DIGEST_BYTES, "image checksum");
      if (Files.size(source) != size) {
        throw new IOException("source-facts image changed while reading");
      }
      if (!MessageDigest.isEqual(expected, bounded.finishDigest())) {
        throw new IOException("M3File source-facts image checksum mismatch");
      }
      return facts;
    } catch (EOFException | RuntimeException malformed) {
      throw new IOException("malformed M3File source-facts image", malformed);
    }
  }

  private static void writeBody(Path output, M3File file, M3FileSourceFacts facts)
      throws IOException {
    try (OutputStream raw = Files.newOutputStream(output);
        DigestingBoundedOutputStream bounded =
            new DigestingBoundedOutputStream(raw, MAX_IMAGE_BYTES - DIGEST_BYTES);
        DataOutputStream out = new DataOutputStream(bounded)) {
      out.writeLong(MAGIC);
      out.writeInt(FORMAT);
      out.writeLong(M3FileImage.contentFingerprint64(file));
      out.writeInt(facts.tokenCount());
      out.writeInt(facts.uniqueTokenCount());
      writeAttributes(out, facts.attributesBeforeRead());
      writeAttributes(out, facts.attributesAfterRead());
      writeByteFacts(out, facts.byteFacts());
      writeLongs(out, facts.utf8OffsetsSnapshot());
      writeInts(out, facts.codePointOffsetsSnapshot());
      writeBytes(out, facts.tokenSha256Snapshot());
      writeLongs(out, facts.prefixASnapshot());
      writeLongs(out, facts.prefixBSnapshot());
      writeLongs(out, facts.powersASnapshot());
      writeLongs(out, facts.powersBSnapshot());
      out.flush();
      raw.write(bounded.finishDigest());
    } catch (RuntimeException invalid) {
      throw new IOException("cannot encode M3File source facts", invalid);
    }
  }

  private static M3FileSourceFacts readFacts(
      DataInputStream in, int tokenCount, int uniqueCount) throws IOException {
    M3FileAttributes before = readAttributes(in);
    M3FileAttributes after = readAttributes(in);
    M3FileByteFacts byteFacts = readByteFacts(in);
    long[] utf8Offsets = readLongs(in, tokenCount + 1, "UTF-8 offsets");
    int[] codePointOffsets = readInts(in, tokenCount + 1, "code-point offsets");
    byte[] tokenHashes = readBytes(in, Math.multiplyExact(uniqueCount, M3Hash256.BYTES), "token hashes");
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
          tokenHashes,
          prefixA,
          prefixB,
          powersA,
          powersB,
          tokenCount,
          uniqueCount);
    } catch (RuntimeException invalid) {
      throw new IOException("invalid source-facts lanes", invalid);
    }
  }

  private static void writeByteFacts(DataOutputStream out, M3FileByteFactsSnapshot facts)
      throws IOException {
    out.writeLong(facts.byteCount());
    out.write(facts.sha256().bytes());
    for (long frequency : facts.byteHistogram()) out.writeLong(frequency);
    writeBytes(out, facts.prefixBytes());
    writeBytes(out, facts.suffixBytes());
    out.writeInt(facts.uniqueByteValues());
    out.writeLong(Double.doubleToLongBits(facts.shannonEntropyBitsPerByte()));
    out.writeInt(facts.chunkCount());
    for (int chunk = 0; chunk < facts.chunkCount(); chunk++) {
      out.writeInt(facts.chunkLength(chunk));
      out.write(facts.chunkSha256(chunk).bytes());
    }
  }

  private static void writeByteFacts(DataOutputStream out, M3FileByteFacts facts)
      throws IOException {
    out.writeLong(facts.byteCount());
    out.write(facts.sha256().bytes());
    for (long frequency : facts.byteHistogram()) out.writeLong(frequency);
    writeBytes(out, facts.prefixBytes());
    writeBytes(out, facts.suffixBytes());
    out.writeInt(facts.uniqueByteValues());
    out.writeLong(Double.doubleToLongBits(facts.shannonEntropyBitsPerByte()));
    out.writeInt(facts.chunkCount());
    for (int chunk = 0; chunk < facts.chunkCount(); chunk++) {
      out.writeInt(facts.chunkLength(chunk));
      out.write(facts.chunkSha256(chunk).bytes());
    }
  }

  private static M3FileByteFacts readByteFacts(DataInputStream in) throws IOException {
    long byteCount = in.readLong();
    M3Hash256 sha256 = M3Hash256.of(readExact(in, M3Hash256.BYTES, "file SHA-256"));
    long[] histogram = new long[256];
    for (int index = 0; index < histogram.length; index++) histogram[index] = in.readLong();
    byte[] prefix = readVariableBytes(in, M3FileByteFacts.SIGNATURE_BYTES, "prefix");
    byte[] suffix = readVariableBytes(in, M3FileByteFacts.SIGNATURE_BYTES, "suffix");
    int unique = readCount(in.readInt(), "unique byte", 256);
    double entropy = Double.longBitsToDouble(in.readLong());
    int chunkCount = readCount(in.readInt(), "chunk", MAX_ARRAY_LENGTH);
    int[] chunkLengths = new int[chunkCount];
    byte[] chunkHashes = new byte[Math.multiplyExact(chunkCount, M3Hash256.BYTES)];
    for (int chunk = 0; chunk < chunkCount; chunk++) {
      chunkLengths[chunk] = in.readInt();
      in.readFully(chunkHashes, Math.multiplyExact(chunk, M3Hash256.BYTES), M3Hash256.BYTES);
    }
    try {
      return M3FileByteFacts.restore(
          byteCount, sha256, histogram, prefix, suffix, unique, entropy, chunkHashes, chunkLengths);
    } catch (RuntimeException invalid) {
      throw new IOException("invalid serialized byte facts", invalid);
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
    EnumSet<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
    PosixFilePermission[] values = PosixFilePermission.values();
    if ((permissionBits & ~((1 << values.length) - 1)) != 0) {
      throw new IOException("invalid serialized POSIX permission bits");
    }
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

  private static void writeBytes(DataOutputStream out, byte[] bytes) throws IOException {
    if (bytes.length > MAX_IMAGE_BYTES) throw new IOException("serialized byte lane is too large");
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  private static byte[] readBytes(DataInputStream in, int expected, String label) throws IOException {
    int length = in.readInt();
    if (length != expected) throw new IOException("invalid serialized " + label + " length");
    return readExact(in, length, label);
  }

  private static byte[] readVariableBytes(DataInputStream in, int maximum, String label)
      throws IOException {
    int length = in.readInt();
    if (length < 0 || length > maximum) {
      throw new IOException("invalid serialized " + label + " length");
    }
    return readExact(in, length, label);
  }

  private static void writeLongs(DataOutputStream out, long[] values) throws IOException {
    out.writeInt(values.length);
    for (long value : values) out.writeLong(value);
  }

  private static long[] readLongs(DataInputStream in, int expected, String label) throws IOException {
    int length = in.readInt();
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
    int length = in.readInt();
    if (length != expected) throw new IOException("invalid serialized " + label + " length");
    int[] values = new int[length];
    for (int index = 0; index < length; index++) values[index] = in.readInt();
    return values;
  }

  private static byte[] readExact(DataInputStream in, int length, String label) throws IOException {
    if (length < 0 || length > MAX_IMAGE_BYTES) throw new IOException("invalid " + label + " length");
    byte[] bytes = new byte[length];
    in.readFully(bytes);
    return bytes;
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

  private static final class DigestingBoundedInputStream extends InputStream {
    private final InputStream delegate;
    private final MessageDigest digest = sha256Digest();
    private long remaining;

    private DigestingBoundedInputStream(InputStream delegate, long limit) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
      if (limit < 0L) throw new IllegalArgumentException("negative input limit");
      this.remaining = limit;
    }

    @Override
    public int read() throws IOException {
      if (remaining == 0L) return -1;
      int value = delegate.read();
      if (value >= 0) {
        digest.update((byte) value);
        remaining--;
      }
      return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
      Objects.checkFromIndexSize(offset, length, bytes.length);
      if (length == 0) return 0;
      if (remaining == 0L) return -1;
      int boundedLength = (int) Math.min((long) length, remaining);
      int read = delegate.read(bytes, offset, boundedLength);
      if (read > 0) {
        digest.update(bytes, offset, read);
        remaining -= read;
      }
      return read;
    }

    private long remaining() {
      return remaining;
    }

    private byte[] finishDigest() {
      if (remaining != 0L) throw new IllegalStateException("source-facts body was not consumed");
      return digest.digest();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }

  private static final class DigestingBoundedOutputStream extends OutputStream {
    private final OutputStream delegate;
    private final MessageDigest digest = sha256Digest();
    private final long limit;
    private long count;

    private DigestingBoundedOutputStream(OutputStream delegate, long limit) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
      if (limit < 0L) throw new IllegalArgumentException("negative output limit");
      this.limit = limit;
    }

    @Override
    public void write(int value) throws IOException {
      ensureCapacity(1L);
      delegate.write(value);
      digest.update((byte) value);
      count++;
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
      Objects.checkFromIndexSize(offset, length, bytes.length);
      ensureCapacity(length);
      delegate.write(bytes, offset, length);
      digest.update(bytes, offset, length);
      count += length;
    }

    private byte[] finishDigest() {
      return digest.digest();
    }

    @Override
    public void flush() throws IOException {
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }

    private void ensureCapacity(long additional) throws IOException {
      if (additional < 0L || count > limit - additional) {
        throw new IOException("M3File source-facts image exceeds byte limit");
      }
    }
  }
}
