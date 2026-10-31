// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * One-pass structural reader and validator for the existing MIDXEVT2 event artifact.
 *
 * <p>The default reader is observational: it validates bounded frames, reconstructs immutable
 * event records and replays them to the supplied sink. The additive {@link #readM3File}
 * overload composes the same events through the canonical builder to rehydrate an
 * {@link M3File} without rereading the source. In both modes, malformed ordering,
 * coordinates, metadata shape or final content digest fails before successful completion.</p>
 */
public final class M3FileEventReader {
  private static final byte[] MAGIC = "MIDXEVT2".getBytes(StandardCharsets.US_ASCII);
  private static final int STARTED = 1;
  private static final int BYTES = 2;
  private static final int LEXICON = 3;
  private static final int TOKEN = 4;
  private static final int FINISHED = 5;
  private static final int TOKEN_FRAME_BYTES = 73;
  private static final int SHA256_BYTES = 32;

  /** A writer chunk is bounded by this default reader allocation limit. */
  public static final int DEFAULT_MAX_FRAME_BYTES = 64 * 1024 * 1024;

  private M3FileEventReader() {
    throw new AssertionError("No instances");
  }

  /** Read and validate one complete MIDXEVT2 stream without closing the caller-owned input. */
  public static Summary read(InputStream input, M3FileStreamEvents.Sink sink)
      throws IOException {
    return read(input, sink, DEFAULT_MAX_FRAME_BYTES);
  }

  /**
   * Validate one MIDXEVT2 stream and rehydrate the canonical M3File without source reread.
   *
   * <p>The event artifact remains observational and its wire format is unchanged. Resolver-native
   * lexicon IDs are reattached to the supplied resolver, while the existing builder derives the
   * immutable occurrence, line, posting, byte-fact and source-fact lanes from the event sequence.</p>
   */
  public static M3File readM3File(
      InputStream input, M3FileResolver resolver) throws IOException {
    return readM3File(input, resolver, false, DEFAULT_MAX_FRAME_BYTES);
  }

  /** Rehydrate with optional admission-time rolling range lanes. */
  public static M3File readM3File(
      InputStream input, M3FileResolver resolver, boolean precomputeRollingIndex) throws IOException {
    return readM3File(input, resolver, precomputeRollingIndex, DEFAULT_MAX_FRAME_BYTES);
  }

  /** Rehydrate with explicit frame and optional rolling-lane bounds. */
  public static M3File readM3File(
      InputStream input,
      M3FileResolver resolver,
      boolean precomputeRollingIndex,
      int maxFrameBytes)
      throws IOException {
    M3FileEventSnapshotCollector collector =
        new M3FileEventSnapshotCollector(resolver, precomputeRollingIndex);
    read(input, collector, maxFrameBytes);
    return collector.file();
  }

  /** Read with an explicit maximum encoded frame payload to keep allocation bounded. */
  public static Summary read(
      InputStream input, M3FileStreamEvents.Sink sink, int maxFrameBytes) throws IOException {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(sink, "sink");
    if (maxFrameBytes < TOKEN_FRAME_BYTES) {
      throw new IllegalArgumentException("maxFrameBytes is too small for a token frame");
    }

    DataInputStream data = new DataInputStream(input);
    byte[] magic = new byte[MAGIC.length];
    try {
      data.readFully(magic);
    } catch (EOFException truncated) {
      throw invalid("truncated MIDXEVT2 magic", truncated);
    }
    if (!Arrays.equals(MAGIC, magic)) throw invalid("invalid MIDXEVT2 magic");

    return readFrames(data, sink, maxFrameBytes, false);
  }

  /**
   * Read one embedded MIDXEVT2 sequence and stop after its validated Finished frame.
   *
   * <p>No standalone magic is consumed; the caller-owned input remains positioned at the
   * enclosing stream's next boundary.</p>
   */
  public static Summary readEmbedded(
      InputStream input, M3FileStreamEvents.Sink sink, int maxFrameBytes) throws IOException {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(sink, "sink");
    if (maxFrameBytes < TOKEN_FRAME_BYTES) {
      throw new IllegalArgumentException("maxFrameBytes is too small for a token frame");
    }
    return readFrames(new DataInputStream(input), sink, maxFrameBytes, true);
  }

  /** Read one embedded MIDXEVT2 sequence with the default frame bound. */
  public static Summary readEmbedded(InputStream input, M3FileStreamEvents.Sink sink)
      throws IOException {
    return readEmbedded(input, sink, DEFAULT_MAX_FRAME_BYTES);
  }

  /** Rehydrate one embedded event sequence through the current canonical file builder. */
  public static M3File readEmbeddedM3File(
      InputStream input, M3FileResolver resolver, int maxFrameBytes) throws IOException {
    M3FileEventSnapshotCollector collector =
        new M3FileEventSnapshotCollector(Objects.requireNonNull(resolver, "resolver"), false);
    readEmbedded(input, collector, maxFrameBytes);
    return collector.file();
  }

  /** Rehydrate one embedded event sequence with the default frame bound. */
  public static M3File readEmbeddedM3File(InputStream input, M3FileResolver resolver)
      throws IOException {
    return readEmbeddedM3File(input, resolver, DEFAULT_MAX_FRAME_BYTES);
  }

  private static Summary readFrames(
      DataInputStream data,
      M3FileStreamEvents.Sink sink,
      int maxFrameBytes,
      boolean stopAfterFinish)
      throws IOException {
    MessageDigest contentDigest = digest();
    Set<Integer> lexiconHandles = new HashSet<>();
    M3FileAttributes attributesBefore = null;
    M3FileAttributes attributesAfter = null;
    int tokenCount = 0;
    long byteCount = 0L;
    long utf8End = 0L;
    int utf16End = 0;
    int codePointEnd = 0;
    boolean started = false;
    boolean finished = false;
    Summary summary = null;

    while (true) {
      int type;
      try {
        type = data.readUnsignedByte();
      } catch (EOFException end) {
        break;
      }
      if (finished) throw invalid("MIDXEVT2 frame follows finish");

      int length;
      try {
        length = data.readInt();
      } catch (EOFException truncated) {
        throw invalid("truncated MIDXEVT2 frame header", truncated);
      }
      if (length < 0 || length > maxFrameBytes) {
        throw invalid("MIDXEVT2 frame exceeds configured bound: " + length);
      }
      byte[] encoded = new byte[length];
      try {
        data.readFully(encoded);
      } catch (EOFException truncated) {
        throw invalid("truncated MIDXEVT2 frame payload", truncated);
      }

      DataInputStream frame =
          new DataInputStream(new ByteArrayInputStream(encoded));
      switch (type) {
        case STARTED -> {
          if (started) throw invalid("duplicate MIDXEVT2 start");
          M3FileAttributes attributes = readMetadata(frame);
          requireExhausted(frame, "started frame");
          sink.accept(new M3FileStreamEvents.Started(attributes));
          attributesBefore = attributes;
          started = true;
        }
        case BYTES -> {
          requireStarted(started, "bytes");
          long offset = readLong(frame, "byte offset");
          int payloadLength = readInt(frame, "byte payload length");
          if (payloadLength <= 0 || payloadLength != frame.available()) {
            throw invalid("invalid MIDXEVT2 byte payload length");
          }
          byte[] payload = new byte[payloadLength];
          frame.readFully(payload);
          requireExhausted(frame, "bytes frame");
          if (offset != byteCount) throw invalid("non-contiguous MIDXEVT2 bytes");
          M3FileStreamEvents.Bytes event = new M3FileStreamEvents.Bytes(offset, payload);
          sink.accept(event);
          contentDigest.update(payload);
          byteCount = Math.addExact(byteCount, payload.length);
        }
        case LEXICON -> {
          requireStarted(started, "lexicon");
          int localHandle = readInt(frame, "lexicon local handle");
          int idCount = readInt(frame, "lexicon token count");
          if (localHandle < 0 || idCount <= 0 || idCount > frame.available() / Long.BYTES) {
            throw invalid("invalid MIDXEVT2 lexicon shape");
          }
          long[] ids = new long[idCount];
          for (int i = 0; i < ids.length; i++) ids[i] = frame.readLong();
          requireExhausted(frame, "lexicon frame");
          if (localHandle != lexiconHandles.size() || !lexiconHandles.add(localHandle)) {
            throw invalid("non-contiguous or duplicate MIDXEVT2 lexicon handle");
          }
          sink.accept(new M3FileStreamEvents.Lexicon(localHandle, ids));
        }
        case TOKEN -> {
          requireStarted(started, "token");
          if (encoded.length != TOKEN_FRAME_BYTES) {
            throw invalid("invalid MIDXEVT2 token frame length");
          }
          int ordinal = frame.readInt();
          int localHandle = frame.readInt();
          M3FileTokenKind kind;
          try {
            kind = M3FileTokenKind.fromCode(frame.readByte());
          } catch (IllegalArgumentException malformedKind) {
            throw invalid("invalid MIDXEVT2 token kind", malformedKind);
          }
          int utf16Start = frame.readInt();
          int utf16TokenEnd = frame.readInt();
          long utf8Start = frame.readLong();
          long utf8TokenEnd = frame.readLong();
          int codePointStart = frame.readInt();
          int codePointTokenEnd = frame.readInt();
          byte[] sha256 = new byte[SHA256_BYTES];
          frame.readFully(sha256);
          requireExhausted(frame, "token frame");
          if (!lexiconHandles.contains(localHandle)) {
            throw invalid("MIDXEVT2 token has no lexicon entry");
          }
          if (ordinal != tokenCount
              || utf8Start != utf8End
              || utf16Start != utf16End
              || codePointStart != codePointEnd) {
            throw invalid("non-contiguous MIDXEVT2 token coordinates");
          }
          M3FileStreamEvents.Token event =
              new M3FileStreamEvents.Token(
                  ordinal,
                  localHandle,
                  kind,
                  utf16Start,
                  utf16TokenEnd,
                  utf8Start,
                  utf8TokenEnd,
                  codePointStart,
                  codePointTokenEnd,
                  sha256);
          sink.accept(event);
          tokenCount = Math.incrementExact(tokenCount);
          utf8End = utf8TokenEnd;
          utf16End = utf16TokenEnd;
          codePointEnd = codePointTokenEnd;
        }
        case FINISHED -> {
          requireStarted(started, "finish");
          M3FileAttributes attributes = readMetadata(frame);
          int finishedTokenCount = readInt(frame, "finished token count");
          int finishedUniqueTokenCount = readInt(frame, "finished unique token count");
          long finishedByteCount = readLong(frame, "finished byte count");
          byte[] expectedSha256 = readBytes(frame, "finished content SHA-256");
          requireExhausted(frame, "finished frame");
          if (expectedSha256.length != SHA256_BYTES
              || finishedTokenCount != tokenCount
              || finishedUniqueTokenCount != lexiconHandles.size()
              || finishedByteCount != byteCount
              || attributes.size() != byteCount
              || !MessageDigest.isEqual(expectedSha256, contentDigest.digest())) {
            throw invalid("MIDXEVT2 stream summary mismatch");
          }
          M3FileStreamEvents.Finished event =
              new M3FileStreamEvents.Finished(
                  attributes,
                  finishedTokenCount,
                  finishedUniqueTokenCount,
                  finishedByteCount,
                  expectedSha256);
          sink.accept(event);
          attributesAfter = attributes;
          finished = true;
          summary =
              new Summary(
                  attributesBefore,
                  attributesAfter,
                  finishedTokenCount,
                  finishedUniqueTokenCount,
                  finishedByteCount,
                  expectedSha256);
          if (stopAfterFinish) return summary;
        }
        default -> throw invalid("unknown MIDXEVT2 frame type: " + type);
      }
    }

    if (!started) throw invalid("MIDXEVT2 stream has no start");
    if (!finished || summary == null) throw invalid("MIDXEVT2 stream has no finish");
    return summary;
  }

  /** Detached summary returned after all frames and the final content digest have been verified. */
  public record Summary(
      M3FileAttributes attributesBefore,
      M3FileAttributes attributesAfter,
      int tokenCount,
      int uniqueTokenCount,
      long byteCount,
      byte[] contentSha256) {
    public Summary {
      attributesBefore = Objects.requireNonNull(attributesBefore, "attributesBefore");
      attributesAfter = Objects.requireNonNull(attributesAfter, "attributesAfter");
      if (tokenCount < 0 || uniqueTokenCount < 0 || uniqueTokenCount > tokenCount) {
        throw new IllegalArgumentException("invalid event summary token counts");
      }
      if (byteCount < 0L) throw new IllegalArgumentException("negative event summary bytes");
      contentSha256 = Objects.requireNonNull(contentSha256, "contentSha256").clone();
      if (contentSha256.length != SHA256_BYTES) {
        throw new IllegalArgumentException("event summary SHA-256 must be 32 bytes");
      }
    }

    @Override
    public byte[] contentSha256() {
      return contentSha256.clone();
    }
  }

  private static M3FileAttributes readMetadata(DataInputStream frame) throws IOException {
    String normalizedPath = readString(frame, "normalized path");
    long size = readLong(frame, "metadata size");
    if (normalizedPath == null || size < 0L) throw invalid("invalid MIDXEVT2 metadata identity");
    FileTime creationTime = readTime(frame, "creation time");
    FileTime lastModifiedTime = readTime(frame, "last modified time");
    FileTime lastAccessTime = readTime(frame, "last access time");
    boolean regularFile = readBoolean(frame, "regular-file flag");
    boolean directory = readBoolean(frame, "directory flag");
    boolean symbolicLinkPath = readBoolean(frame, "symbolic-link flag");
    boolean other = readBoolean(frame, "other-file flag");
    String fileKey = readString(frame, "file key");
    String owner = readString(frame, "owner");
    String group = readString(frame, "group");
    boolean posixAvailable = readBoolean(frame, "POSIX availability");
    int permissionCount = readCount(frame, "POSIX permission count");
    EnumSet<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
    for (int i = 0; i < permissionCount; i++) {
      String name = readString(frame, "POSIX permission");
      try {
        permissions.add(PosixFilePermission.valueOf(Objects.requireNonNull(name, "permission")));
      } catch (IllegalArgumentException | NullPointerException malformed) {
        throw invalid("invalid POSIX permission", malformed);
      }
    }
    boolean dosAvailable = readBoolean(frame, "DOS availability");
    boolean dosArchive = readBoolean(frame, "DOS archive flag");
    boolean dosHidden = readBoolean(frame, "DOS hidden flag");
    boolean dosReadOnly = readBoolean(frame, "DOS read-only flag");
    boolean dosSystem = readBoolean(frame, "DOS system flag");
    List<String> aclEntries = readStrings(frame, "ACL entries");
    Map<String, String> userAttributes = readMap(frame, "user attributes");
    Map<String, String> providerAttributes = readMap(frame, "provider attributes");
    try {
      return M3FileAttributes.restore(
          normalizedPath,
          size,
          creationTime,
          lastModifiedTime,
          lastAccessTime,
          regularFile,
          directory,
          symbolicLinkPath,
          other,
          fileKey,
          owner,
          group,
          posixAvailable,
          permissions,
          dosAvailable,
          dosArchive,
          dosHidden,
          dosReadOnly,
          dosSystem,
          aclEntries,
          userAttributes,
          providerAttributes);
    } catch (IllegalArgumentException | NullPointerException malformed) {
      throw invalid("invalid MIDXEVT2 metadata", malformed);
    }
  }

  private static List<String> readStrings(DataInputStream frame, String label) throws IOException {
    int count = readCount(frame, label + " count");
    List<String> values = new ArrayList<>(count);
    for (int i = 0; i < count; i++) values.add(readString(frame, label));
    return values;
  }

  private static Map<String, String> readMap(DataInputStream frame, String label) throws IOException {
    int count = readCount(frame, label + " count");
    Map<String, String> values = new TreeMap<>();
    for (int i = 0; i < count; i++) {
      String key = readString(frame, label + " key");
      String value = readString(frame, label + " value");
      if (key == null || value == null) throw invalid("null MIDXEVT2 map entry");
      if (values.put(key, value) != null) throw invalid("duplicate MIDXEVT2 map key: " + key);
    }
    return values;
  }

  private static FileTime readTime(DataInputStream frame, String label) throws IOException {
    String value = readString(frame, label);
    if (value == null) throw invalid("missing MIDXEVT2 " + label);
    try {
      return FileTime.from(Instant.parse(value));
    } catch (DateTimeParseException malformed) {
      throw invalid("invalid MIDXEVT2 " + label, malformed);
    }
  }

  private static byte[] readBytes(DataInputStream frame, String label) throws IOException {
    int length = readInt(frame, label + " length");
    if (length < 0 || length > frame.available()) throw invalid("invalid " + label + " length");
    byte[] bytes = new byte[length];
    frame.readFully(bytes);
    return bytes;
  }

  private static String readString(DataInputStream frame, String label) throws IOException {
    int length = readInt(frame, label + " length");
    if (length == -1) return null;
    if (length < -1 || length > frame.available()) {
      throw invalid("invalid MIDXEVT2 " + label + " length");
    }
    byte[] bytes = new byte[length];
    frame.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static int readCount(DataInputStream frame, String label) throws IOException {
    int count = readInt(frame, label);
    if (count < 0 || count > frame.available()) throw invalid("invalid " + label);
    return count;
  }

  private static int readInt(DataInputStream frame, String label) throws IOException {
    try {
      return frame.readInt();
    } catch (EOFException truncated) {
      throw invalid("truncated MIDXEVT2 " + label, truncated);
    }
  }

  private static long readLong(DataInputStream frame, String label) throws IOException {
    try {
      return frame.readLong();
    } catch (EOFException truncated) {
      throw invalid("truncated MIDXEVT2 " + label, truncated);
    }
  }

  private static boolean readBoolean(DataInputStream frame, String label) throws IOException {
    try {
      return frame.readBoolean();
    } catch (EOFException truncated) {
      throw invalid("truncated MIDXEVT2 " + label, truncated);
    }
  }

  private static void requireStarted(boolean started, String event) throws IOException {
    if (!started) throw invalid("MIDXEVT2 " + event + " precedes start");
  }

  private static void requireExhausted(DataInputStream frame, String label) throws IOException {
    if (frame.available() != 0) throw invalid("trailing bytes in MIDXEVT2 " + label);
  }

  private static IOException invalid(String message) {
    return new IOException(message);
  }

  private static IOException invalid(String message, Throwable cause) {
    return new IOException(message, cause);
  }

  private static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }
}
