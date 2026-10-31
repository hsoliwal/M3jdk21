// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileCoordinates;
import com.m3.file.M3FileId;
import com.m3.file.M3FileResolver;
import com.m3.file.M3FileHash;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Deterministic durable image for one immutable M3File.
 *
 * <p>The image never treats file-local handles as universal identity. Every resolver language
 * generation referenced by the local lexicon is recorded and must match when the image is opened.
 * Overlay coordinates and resolvers without a frozen coordinate fingerprint are rejected.</p>
 */
public final class M3FileImage {
  static final long MAGIC = 0x534d46494c453031L; // "SMFILE01"
  static final int FORMAT = 1;
  static final int HEADER_BYTES = 48;
  private static final long DOMAIN = M3FileHash.hashText("SYNEXIA-MINDEX-FILE-IMAGE-1");

  private M3FileImage() {}

  /** Stable semantic identity used by additive sidecar images without changing format 1. */
  static long contentFingerprint64(M3File file) {
    M3File source = Objects.requireNonNull(file, "file");
    int flatResolverTokens = resolverTokenCount(source);
    boolean[] languagesPresent = new boolean[M3FileId.MAX_LANGUAGE_ID + 1];
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      int count = source.lexicon().tokenCount(handle);
      for (int ordinal = 0; ordinal < count; ordinal++) {
        long id = source.lexicon().tokenIdAt(handle, ordinal);
        if (M3FileId.isOverlay(id)) {
          throw new IllegalArgumentException("M3File durable identity rejects overlay IDs");
        }
        int languageId = source.lexicon().resolver().languageId(id);
        if (languageId < 0 || languageId > M3FileId.MAX_LANGUAGE_ID) {
          throw new IllegalArgumentException("resolver language ID is not durable: " + languageId);
        }
        languagesPresent[languageId] = true;
      }
    }
    int languageCount = 0;
    for (boolean present : languagesPresent) if (present) languageCount++;
    int[] languageIds = new int[languageCount];
    long[] languageFingerprints = new long[languageCount];
    for (int language = 0, at = 0; language < languagesPresent.length; language++) {
      if (!languagesPresent[language]) continue;
      OptionalLong fingerprint = M3FileCoordinates.tryOf(source.lexicon().resolver(), language);
      if (fingerprint.isEmpty()) {
        throw new IllegalArgumentException(
            "durable M3File requires frozen coordinate fingerprint for language " + language);
      }
      languageIds[at] = language;
      languageFingerprints[at] = fingerprint.getAsLong();
      at++;
    }
    return fingerprint(source, languageIds, languageFingerprints);
  }

  /** Stable semantic identity for a detached snapshot under an explicit resolver generation. */
  static long contentFingerprint64(M3FileSnapshot snapshot, M3FileResolver resolver) {
    M3FileSnapshot checkedSnapshot = Objects.requireNonNull(snapshot, "snapshot");
    M3FileResolver checkedResolver = Objects.requireNonNull(resolver, "resolver");
    M3FileLexiconSnapshot lexicon = checkedSnapshot.lexiconSnapshot();
    M3FilePrimitiveSnapshot primitive = checkedSnapshot.primitiveSnapshot();
    int tokenCount = primitive.tokenCount();
    int uniqueCount = primitive.uniqueTokenCount();
    int tokenWidth = tokenWidthBytes(uniqueCount);
    int lineCount = primitive.lineCount();
    int flatResolverTokens = resolverTokenCount(lexicon);
    boolean[] languagesPresent = new boolean[M3FileId.MAX_LANGUAGE_ID + 1];
    for (long id : lexicon.tokenIds()) {
      if (M3FileId.isOverlay(id)) {
        throw new IllegalArgumentException("M3File durable identity rejects overlay IDs");
      }
      int languageId = checkedResolver.languageId(id);
      if (languageId < 0 || languageId > M3FileId.MAX_LANGUAGE_ID) {
        throw new IllegalArgumentException("resolver language ID is not durable: " + languageId);
      }
      languagesPresent[languageId] = true;
    }
    int languageCount = 0;
    for (boolean present : languagesPresent) if (present) languageCount++;
    int[] languageIds = new int[languageCount];
    long[] languageFingerprints = new long[languageCount];
    for (int language = 0, at = 0; language < languagesPresent.length; language++) {
      if (!languagesPresent[language]) continue;
      OptionalLong fingerprint = M3FileCoordinates.tryOf(checkedResolver, language);
      if (fingerprint.isEmpty()) {
        throw new IllegalArgumentException(
            "durable M3File requires frozen coordinate fingerprint for language " + language);
      }
      languageIds[at] = language;
      languageFingerprints[at] = fingerprint.getAsLong();
      at++;
    }

    long hash =
        headerHash(
            tokenCount, uniqueCount, tokenWidth, lineCount, languageCount, flatResolverTokens);
    for (int i = 0; i < languageIds.length; i++) {
      hash = mix(hash, languageIds[i]);
      hash = mix(hash, languageFingerprints[i]);
    }
    for (int boundary : lexicon.tokenStarts()) hash = mix(hash, boundary);
    long[] tokenIds = lexicon.tokenIds();
    if (lexicon.resolverTokenCount() != tokenIds.length) {
      throw new IllegalArgumentException("detached lexicon ID count is not canonical");
    }
    for (long id : tokenIds) hash = mix(hash, id);
    for (int handle : primitive.localHandles()) hash = mix(hash, handle);
    for (byte kind : primitive.tokenKinds()) hash = mix(hash, Byte.toUnsignedInt(kind));
    for (int offset : primitive.utf16Offsets()) hash = mix(hash, offset);
    for (int lineStart : primitive.lineStarts()) hash = mix(hash, lineStart);
    for (int frequency : primitive.frequencies()) hash = mix(hash, frequency);
    for (int first : primitive.firstOccurrences()) hash = mix(hash, first);
    for (int last : primitive.lastOccurrences()) hash = mix(hash, last);
    for (int postingStart : primitive.postingStarts()) hash = mix(hash, postingStart);
    for (int posting : primitive.postings()) hash = mix(hash, posting);
    return hash;
  }

  public static void write(M3File file, Path output) throws IOException {
    M3File source = Objects.requireNonNull(file, "file");
    Path target = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
    M3FileResolver resolver = source.lexicon().resolver();

    int flatResolverTokens = resolverTokenCount(source);
    boolean[] languagesPresent = new boolean[M3FileId.MAX_LANGUAGE_ID + 1];
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      int count = source.lexicon().tokenCount(handle);
      for (int ordinal = 0; ordinal < count; ordinal++) {
        long id = source.lexicon().tokenIdAt(handle, ordinal);
        if (M3FileId.isOverlay(id)) {
          throw new IllegalArgumentException("M3File durable image rejects overlay resolver IDs");
        }
        int languageId = resolver.languageId(id);
        if (languageId < 0 || languageId > M3FileId.MAX_LANGUAGE_ID) {
          throw new IllegalArgumentException("resolver language ID is not durable: " + languageId);
        }
        languagesPresent[languageId] = true;
      }
    }

    int languageCount = 0;
    for (boolean present : languagesPresent) if (present) languageCount++;
    int[] languageIds = new int[languageCount];
    long[] languageFingerprints = new long[languageCount];
    for (int language = 0, at = 0; language < languagesPresent.length; language++) {
      if (!languagesPresent[language]) continue;
      OptionalLong fingerprint = M3FileCoordinates.tryOf(resolver, language);
      if (fingerprint.isEmpty()) {
        throw new IllegalArgumentException(
            "durable M3File requires frozen coordinate fingerprint for language " + language);
      }
      languageIds[at] = language;
      languageFingerprints[at] = fingerprint.getAsLong();
      at++;
    }

    Layout layout =
        layout(
            source.tokenCount(),
            source.uniqueTokenCount(),
            source.tokenWidthBytes(),
            source.lineCount(),
            languageCount,
            flatResolverTokens);
    long fingerprint = fingerprint(source, languageIds, languageFingerprints);

    Path parent = target.getParent();
    if (parent != null) Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, "." + target.getFileName() + ".", ".tmp");
    try {
      try (DataOutputStream out =
          new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
        out.writeLong(MAGIC);
        out.writeInt(FORMAT);
        out.writeInt(source.tokenCount());
        out.writeInt(source.uniqueTokenCount());
        out.writeInt(source.tokenWidthBytes());
        out.writeInt(source.lineCount());
        out.writeInt(languageCount);
        out.writeInt(flatResolverTokens);
        out.writeInt(0);
        out.writeLong(fingerprint);
        requirePosition(out, layout.languageStart());

        for (int i = 0; i < languageIds.length; i++) {
          out.writeInt(languageIds[i]);
          out.writeLong(languageFingerprints[i]);
        }
        requirePosition(out, layout.lexiconStartsStart());
        int lexiconCursor = 0;
        for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
          out.writeInt(lexiconCursor);
          lexiconCursor =
              Math.addExact(lexiconCursor, source.lexicon().tokenCount(handle));
        }
        out.writeInt(lexiconCursor);

        padTo(out, layout.lexiconIdsStart());
        for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
          int count = source.lexicon().tokenCount(handle);
          for (int ordinal = 0; ordinal < count; ordinal++) {
            out.writeLong(source.lexicon().tokenIdAt(handle, ordinal));
          }
        }

        requirePosition(out, layout.tokensStart());
        for (int token = 0; token < source.tokenCount(); token++) {
          writeLocalHandle(out, source.localHandleAt(token), source.tokenWidthBytes());
        }

        requirePosition(out, layout.kindsStart());
        for (int token = 0; token < source.tokenCount(); token++) {
          out.writeByte(source.tokenKindAt(token).ordinal());
        }

        padTo(out, layout.utf16OffsetsStart());
        for (int boundary = 0; boundary <= source.tokenCount(); boundary++) {
          out.writeInt(source.utf16Offset(boundary));
        }

        requirePosition(out, layout.lineStartsStart());
        for (int line = 0; line < source.lineCount(); line++) {
          out.writeInt(source.lineStartUtf16(line));
        }

        requirePosition(out, layout.frequenciesStart());
        for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
          out.writeInt(source.occurrenceCount(handle));
        }

        requirePosition(out, layout.firstStarts());
        for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
          out.writeInt(source.firstOccurrence(handle));
        }

        requirePosition(out, layout.lastStarts());
        for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
          out.writeInt(source.lastOccurrence(handle));
        }

        requirePosition(out, layout.postingStartsStart());
        int postingCursor = 0;
        for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
          out.writeInt(postingCursor);
          postingCursor = Math.addExact(postingCursor, source.occurrenceCount(handle));
        }
        out.writeInt(postingCursor);

        requirePosition(out, layout.postingsStart());
        for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
          int count = source.occurrenceCount(handle);
          for (int ordinal = 0; ordinal < count; ordinal++) {
            out.writeInt(source.occurrenceAt(handle, ordinal));
          }
        }
        requirePosition(out, layout.end());
      }
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
   * Write a canonical MIDX format-1 image from detached primitive lanes.
   */
  static void writeSnapshot(M3FileSnapshot snapshot, M3FileResolver resolver, Path output)
      throws IOException {
    M3FileSnapshot checked = Objects.requireNonNull(snapshot, "snapshot");
    M3FileResolver checkedResolver = Objects.requireNonNull(resolver, "resolver");
    M3FileLexiconSnapshot lexicon = checked.lexiconSnapshot();
    M3FilePrimitiveSnapshot primitive = checked.primitiveSnapshot();
    int tokenCount = primitive.tokenCount();
    int uniqueCount = primitive.uniqueTokenCount();
    int tokenWidth = tokenWidthBytes(uniqueCount);
    int lineCount = primitive.lineCount();
    int flatResolverTokens = resolverTokenCount(lexicon);
    boolean[] languagesPresent = new boolean[M3FileId.MAX_LANGUAGE_ID + 1];
    for (long id : lexicon.tokenIds()) {
      if (M3FileId.isOverlay(id)) throw new IllegalArgumentException("overlay resolver ID");
      int language = checkedResolver.languageId(id);
      if (language < 0 || language > M3FileId.MAX_LANGUAGE_ID) {
        throw new IllegalArgumentException("resolver language ID is not durable: " + language);
      }
      languagesPresent[language] = true;
    }
    int languageCount = 0;
    for (boolean present : languagesPresent) if (present) languageCount++;
    int[] languageIds = new int[languageCount];
    long[] languageFingerprints = new long[languageCount];
    for (int language = 0, at = 0; language < languagesPresent.length; language++) {
      if (!languagesPresent[language]) continue;
      OptionalLong coordinate = M3FileCoordinates.tryOf(checkedResolver, language);
      if (coordinate.isEmpty()) throw new IllegalArgumentException("missing coordinate fingerprint");
      languageIds[at] = language;
      languageFingerprints[at++] = coordinate.getAsLong();
    }
    int[] localHandles = primitive.localHandles();
    byte[] kinds = primitive.tokenKinds();
    int[] offsets = primitive.utf16Offsets();
    int[] lines = primitive.lineStarts();
    int[] frequencies = primitive.frequencies();
    int[] first = primitive.firstOccurrences();
    int[] last = primitive.lastOccurrences();
    int[] postingStarts = primitive.postingStarts();
    int[] postings = primitive.postings();
    Layout layout = layout(tokenCount, uniqueCount, tokenWidth, lineCount, languageCount, flatResolverTokens);
    long fingerprint = contentFingerprint64(checked, checkedResolver);
    Path target = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
    Path parent = target.getParent();
    if (parent != null) Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, "." + target.getFileName() + ".", ".tmp");
    try {
      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
        out.writeLong(MAGIC); out.writeInt(FORMAT); out.writeInt(tokenCount);
        out.writeInt(uniqueCount); out.writeInt(tokenWidth); out.writeInt(lineCount);
        out.writeInt(languageCount); out.writeInt(flatResolverTokens); out.writeInt(0);
        out.writeLong(fingerprint); requirePosition(out, layout.languageStart());
        for (int i = 0; i < languageIds.length; i++) {
          out.writeInt(languageIds[i]); out.writeLong(languageFingerprints[i]);
        }
        requirePosition(out, layout.lexiconStartsStart());
        int cursor = 0;
        for (int handle = 0; handle < uniqueCount; handle++) {
          out.writeInt(cursor); cursor = Math.addExact(cursor, lexicon.tokenCount(handle));
        }
        out.writeInt(cursor); padTo(out, layout.lexiconIdsStart());
        for (long id : lexicon.tokenIds()) out.writeLong(id);
        requirePosition(out, layout.tokensStart());
        for (int handle : localHandles) writeLocalHandle(out, handle, tokenWidth);
        requirePosition(out, layout.kindsStart());
        for (byte kind : kinds) { M3FileTokenKind.fromCode(kind); out.writeByte(kind); }
        padTo(out, layout.utf16OffsetsStart());
        for (int value : offsets) out.writeInt(value);
        requirePosition(out, layout.lineStartsStart());
        for (int value : lines) out.writeInt(value);
        requirePosition(out, layout.frequenciesStart());
        for (int value : frequencies) out.writeInt(value);
        requirePosition(out, layout.firstStarts());
        for (int value : first) out.writeInt(value);
        requirePosition(out, layout.lastStarts());
        for (int value : last) out.writeInt(value);
        requirePosition(out, layout.postingStartsStart());
        for (int value : postingStarts) out.writeInt(value);
        requirePosition(out, layout.postingsStart());
        for (int value : postings) out.writeInt(value);
        requirePosition(out, layout.end());
      }
      try {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException unsupported) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  static Layout layout(
      int tokenCount,
      int uniqueCount,
      int tokenWidth,
      int lineCount,
      int languageCount,
      int resolverTokenCount) {
    if (tokenCount < 0
        || uniqueCount < 0
        || lineCount < 0
        || languageCount < 0
        || resolverTokenCount < 0
        || (tokenWidth != 1 && tokenWidth != 2 && tokenWidth != 4)) {
      throw new IllegalArgumentException("invalid M3File image shape");
    }
    int languageStart = HEADER_BYTES;
    int lexiconStartsStart =
        add(languageStart, multiply(languageCount, Integer.BYTES + Long.BYTES));
    int lexiconIdsStart =
        align8(add(lexiconStartsStart, multiply(uniqueCount + 1, Integer.BYTES)));
    int tokensStart =
        add(lexiconIdsStart, multiply(resolverTokenCount, Long.BYTES));
    int kindsStart = add(tokensStart, multiply(tokenCount, tokenWidth));
    int utf16OffsetsStart = align4(add(kindsStart, tokenCount));
    int lineStartsStart =
        add(utf16OffsetsStart, multiply(tokenCount + 1, Integer.BYTES));
    int frequenciesStart = add(lineStartsStart, multiply(lineCount, Integer.BYTES));
    int firstStarts = add(frequenciesStart, multiply(uniqueCount, Integer.BYTES));
    int lastStarts = add(firstStarts, multiply(uniqueCount, Integer.BYTES));
    int postingStartsStart = add(lastStarts, multiply(uniqueCount, Integer.BYTES));
    int postingsStart =
        add(postingStartsStart, multiply(uniqueCount + 1, Integer.BYTES));
    int end = add(postingsStart, multiply(tokenCount, Integer.BYTES));
    return new Layout(
        languageStart,
        lexiconStartsStart,
        lexiconIdsStart,
        tokensStart,
        kindsStart,
        utf16OffsetsStart,
        lineStartsStart,
        frequenciesStart,
        firstStarts,
        lastStarts,
        postingStartsStart,
        postingsStart,
        end);
  }

  static long fingerprint(
      ByteBuffer source,
      Layout layout,
      int tokenCount,
      int uniqueCount,
      int tokenWidth,
      int lineCount,
      int languageCount,
      int resolverTokenCount) {
    ByteBuffer data = source.duplicate().order(ByteOrder.BIG_ENDIAN);
    long hash = headerHash(tokenCount, uniqueCount, tokenWidth, lineCount, languageCount, resolverTokenCount);
    for (int i = 0; i < languageCount; i++) {
      int at = layout.languageStart() + i * (Integer.BYTES + Long.BYTES);
      hash = mix(hash, data.getInt(at));
      hash = mix(hash, data.getLong(at + Integer.BYTES));
    }
    for (int i = 0; i <= uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout.lexiconStartsStart() + i * Integer.BYTES));
    }
    for (int i = 0; i < resolverTokenCount; i++) {
      hash = mix(hash, data.getLong(layout.lexiconIdsStart() + i * Long.BYTES));
    }
    for (int i = 0; i < tokenCount; i++) {
      hash = mix(hash, readLocalHandle(data, layout.tokensStart(), tokenWidth, i));
    }
    for (int i = 0; i < tokenCount; i++) {
      hash = mix(hash, Byte.toUnsignedInt(data.get(layout.kindsStart() + i)));
    }
    for (int i = 0; i <= tokenCount; i++) {
      hash = mix(hash, data.getInt(layout.utf16OffsetsStart() + i * Integer.BYTES));
    }
    for (int i = 0; i < lineCount; i++) {
      hash = mix(hash, data.getInt(layout.lineStartsStart() + i * Integer.BYTES));
    }
    for (int i = 0; i < uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout.frequenciesStart() + i * Integer.BYTES));
    }
    for (int i = 0; i < uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout.firstStarts() + i * Integer.BYTES));
    }
    for (int i = 0; i < uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout.lastStarts() + i * Integer.BYTES));
    }
    for (int i = 0; i <= uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout.postingStartsStart() + i * Integer.BYTES));
    }
    for (int i = 0; i < tokenCount; i++) {
      hash = mix(hash, data.getInt(layout.postingsStart() + i * Integer.BYTES));
    }
    return hash;
  }

  static int readLocalHandle(ByteBuffer data, int tokenStart, int width, int index) {
    int at = tokenStart + Math.multiplyExact(index, width);
    return switch (width) {
      case 1 -> Byte.toUnsignedInt(data.get(at));
      case 2 -> Short.toUnsignedInt(data.getShort(at));
      case 4 -> data.getInt(at);
      default -> throw new IllegalArgumentException("token width=" + width);
    };
  }

  private static long fingerprint(
      M3File source, int[] languageIds, long[] languageFingerprints) {
    int flatResolverTokens = resolverTokenCount(source);
    long hash =
        headerHash(
            source.tokenCount(),
            source.uniqueTokenCount(),
            source.tokenWidthBytes(),
            source.lineCount(),
            languageIds.length,
            flatResolverTokens);
    for (int i = 0; i < languageIds.length; i++) {
      hash = mix(hash, languageIds[i]);
      hash = mix(hash, languageFingerprints[i]);
    }
    int cursor = 0;
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      hash = mix(hash, cursor);
      cursor = Math.addExact(cursor, source.lexicon().tokenCount(handle));
    }
    hash = mix(hash, cursor);
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      int count = source.lexicon().tokenCount(handle);
      for (int ordinal = 0; ordinal < count; ordinal++) {
        hash = mix(hash, source.lexicon().tokenIdAt(handle, ordinal));
      }
    }
    for (int token = 0; token < source.tokenCount(); token++) {
      hash = mix(hash, source.localHandleAt(token));
    }
    for (int token = 0; token < source.tokenCount(); token++) {
      hash = mix(hash, source.tokenKindAt(token).ordinal());
    }
    for (int boundary = 0; boundary <= source.tokenCount(); boundary++) {
      hash = mix(hash, source.utf16Offset(boundary));
    }
    for (int line = 0; line < source.lineCount(); line++) {
      hash = mix(hash, source.lineStartUtf16(line));
    }
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      hash = mix(hash, source.occurrenceCount(handle));
    }
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      hash = mix(hash, source.firstOccurrence(handle));
    }
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      hash = mix(hash, source.lastOccurrence(handle));
    }
    cursor = 0;
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      hash = mix(hash, cursor);
      cursor = Math.addExact(cursor, source.occurrenceCount(handle));
    }
    hash = mix(hash, cursor);
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      int count = source.occurrenceCount(handle);
      for (int ordinal = 0; ordinal < count; ordinal++) {
        hash = mix(hash, source.occurrenceAt(handle, ordinal));
      }
    }
    return hash;
  }

  private static long headerHash(
      int tokenCount,
      int uniqueCount,
      int tokenWidth,
      int lineCount,
      int languageCount,
      int resolverTokenCount) {
    long hash = DOMAIN;
    hash = mix(hash, FORMAT);
    hash = mix(hash, tokenCount);
    hash = mix(hash, uniqueCount);
    hash = mix(hash, tokenWidth);
    hash = mix(hash, lineCount);
    hash = mix(hash, languageCount);
    return mix(hash, resolverTokenCount);
  }

  private static long mix(long hash, long value) {
    return M3FileHash.mix(hash, value);
  }

  private static int resolverTokenCount(M3File source) {
    int count = 0;
    for (int handle = 0; handle < source.uniqueTokenCount(); handle++) {
      count = Math.addExact(count, source.lexicon().tokenCount(handle));
    }
    return count;
  }

  private static int resolverTokenCount(M3FileLexiconSnapshot lexicon) {
    return lexicon.resolverTokenCount();
  }

  private static int tokenWidthBytes(int uniqueCount) {
    if (uniqueCount <= 1 << Byte.SIZE) return Byte.BYTES;
    if (uniqueCount <= 1 << Short.SIZE) return Short.BYTES;
    return Integer.BYTES;
  }

  private static void writeLocalHandle(DataOutputStream out, int handle, int width)
      throws IOException {
    switch (width) {
      case 1 -> out.writeByte(handle);
      case 2 -> out.writeShort(handle);
      case 4 -> out.writeInt(handle);
      default -> throw new IllegalArgumentException("token width=" + width);
    }
  }

  private static void requirePosition(DataOutputStream out, int expected) throws IOException {
    if (out.size() != expected) {
      throw new IOException(
          "M3File image layout mismatch: expected " + expected + " actual " + out.size());
    }
  }

  private static void padTo(DataOutputStream out, int target) throws IOException {
    while (out.size() < target) out.writeByte(0);
    requirePosition(out, target);
  }

  private static int align4(int value) {
    return Math.toIntExact(((long) value + 3L) & ~3L);
  }

  private static int align8(int value) {
    return Math.toIntExact(((long) value + 7L) & ~7L);
  }

  private static int multiply(int left, int right) {
    return Math.toIntExact(Math.multiplyExact((long) left, (long) right));
  }

  private static int add(int left, int right) {
    return Math.toIntExact(Math.addExact((long) left, (long) right));
  }

  record Layout(
      int languageStart,
      int lexiconStartsStart,
      int lexiconIdsStart,
      int tokensStart,
      int kindsStart,
      int utf16OffsetsStart,
      int lineStartsStart,
      int frequenciesStart,
      int firstStarts,
      int lastStarts,
      int postingStartsStart,
      int postingsStart,
      int end) {}
}
