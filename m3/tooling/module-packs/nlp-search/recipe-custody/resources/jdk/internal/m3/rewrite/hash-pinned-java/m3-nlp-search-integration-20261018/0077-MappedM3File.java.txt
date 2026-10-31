// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import com.m3.file.M3FileTextBuilder;
import com.m3.file.M3Hash256;
import java.nio.charset.Charset;

import com.m3.file.M3FileCoordinates;
import com.m3.file.M3FileId;
import com.m3.file.M3FileResolver;
import com.m3.file.M3FileText;
import com.m3.file.M3FileNative;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Read-only mmap view of a durable M3File image.
 *
 * <p>All occurrence/posting/coordinate lanes are served directly from mapped memory. Canonical
 * spellings remain owned by the supplied frozen resolver and are materialized only on request.</p>
 */
public final class MappedM3File implements AutoCloseable {
  private final FileChannel channel;
  private final MappedByteBuffer buffer;
  private final M3FileResolver resolver;
  private final M3FileImage.Layout layout;
  private final int tokenCount;
  private final int uniqueCount;
  private final int tokenWidth;
  private final int lineCount;
  private final int languageCount;
  private final int resolverTokenCount;
  private final long fingerprint;
  private volatile M3FileText indexedText;

  private MappedM3File(
      FileChannel channel,
      MappedByteBuffer buffer,
      M3FileResolver resolver,
      M3FileImage.Layout layout,
      int tokenCount,
      int uniqueCount,
      int tokenWidth,
      int lineCount,
      int languageCount,
      int resolverTokenCount,
      long fingerprint) {
    this.channel = channel;
    this.buffer = buffer;
    this.resolver = resolver;
    this.layout = layout;
    this.tokenCount = tokenCount;
    this.uniqueCount = uniqueCount;
    this.tokenWidth = tokenWidth;
    this.lineCount = lineCount;
    this.languageCount = languageCount;
    this.resolverTokenCount = resolverTokenCount;
    this.fingerprint = fingerprint;
  }

  public static MappedM3File open(Path path, M3FileResolver resolver) throws IOException {
    Path source = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    M3FileResolver checkedResolver = Objects.requireNonNull(resolver, "resolver");
    FileChannel channel = FileChannel.open(source, StandardOpenOption.READ);
    try {
      long size = channel.size();
      if (size < M3FileImage.HEADER_BYTES || size > Integer.MAX_VALUE) {
        throw new IOException("invalid M3File image length");
      }
      MappedByteBuffer buffer =
          channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
      buffer.order(ByteOrder.BIG_ENDIAN);
      if (buffer.getLong(0) != M3FileImage.MAGIC) {
        throw new IOException("invalid M3File image magic");
      }
      int format = buffer.getInt(8);
      if (format != M3FileImage.FORMAT) {
        throw new IOException("unsupported M3File image format: " + format);
      }
      int tokenCount = buffer.getInt(12);
      int uniqueCount = buffer.getInt(16);
      int tokenWidth = buffer.getInt(20);
      int lineCount = buffer.getInt(24);
      int languageCount = buffer.getInt(28);
      int resolverTokenCount = buffer.getInt(32);
      int reserved = buffer.getInt(36);
      long fingerprint = buffer.getLong(40);
      if (reserved != 0) throw new IOException("non-zero M3File reserved header field");

      M3FileImage.Layout layout;
      try {
        layout =
            M3FileImage.layout(
                tokenCount,
                uniqueCount,
                tokenWidth,
                lineCount,
                languageCount,
                resolverTokenCount);
      } catch (IllegalArgumentException | ArithmeticException invalid) {
        throw new IOException("invalid M3File image shape", invalid);
      }
      if (layout.end() != size) {
        throw new IOException(
            "M3File image length mismatch: expected " + layout.end() + " actual " + size);
      }

      validateLanguages(buffer, layout, languageCount, checkedResolver);
      validateStructure(
          buffer,
          layout,
          tokenCount,
          uniqueCount,
          tokenWidth,
          lineCount,
          languageCount,
          resolverTokenCount,
          checkedResolver);

      long actualFingerprint =
          M3FileImage.fingerprint(
              buffer,
              layout,
              tokenCount,
              uniqueCount,
              tokenWidth,
              lineCount,
              languageCount,
              resolverTokenCount);
      if (actualFingerprint != fingerprint) {
        throw new IOException("M3File content fingerprint mismatch");
      }

      return new MappedM3File(
          channel,
          buffer,
          checkedResolver,
          layout,
          tokenCount,
          uniqueCount,
          tokenWidth,
          lineCount,
          languageCount,
          resolverTokenCount,
          fingerprint);
    } catch (RuntimeException | IOException failure) {
      channel.close();
      throw failure;
    }
  }

  public M3FileResolver resolver() { return resolver; }

  public int tokenCount() { return tokenCount; }

  public int uniqueTokenCount() { return uniqueCount; }

  public int tokenWidthBytes() { return tokenWidth; }

  public int lineCount() { return lineCount; }

  public int languageCount() { return languageCount; }

  public int resolverTokenCount() { return resolverTokenCount; }

  /** Returns one defensive flattened lexicon snapshot from the authenticated mapped image. */
  public M3FileLexiconSnapshot lexiconSnapshot() {
    int[] tokenStarts = new int[uniqueCount + 1];
    for (int boundary = 0; boundary <= uniqueCount; boundary++) {
      tokenStarts[boundary] = lexiconStart(boundary);
    }
    long[] tokenIds = new long[resolverTokenCount];
    for (int ordinal = 0; ordinal < resolverTokenCount; ordinal++) {
      tokenIds[ordinal] = buffer.getLong(layout.lexiconIdsStart() + ordinal * Long.BYTES);
    }
    return new M3FileLexiconSnapshot(tokenIds, tokenStarts);
  }

  /** Return one typed transfer packet over the authenticated mapped projections. */
  public M3FileSnapshot snapshot() {
    return new M3FileSnapshot(lexiconSnapshot(), primitiveSnapshot(), null);
  }

  /**
   * Returns a detached snapshot with already-authenticated physical source facts attached.
   *
   * <p>The supplied facts must have been verified against this mapped image. This method never
   * reopens the original source file.</p>
   */
  public M3FileSnapshot snapshot(M3FileSourceFacts sourceFacts) {
    M3FileSourceFacts checked = Objects.requireNonNull(sourceFacts, "sourceFacts");
    if (checked.tokenCount() != tokenCount || checked.uniqueTokenCount() != uniqueCount) {
      throw new IllegalArgumentException("source facts do not match mapped M3File shape");
    }
    return new M3FileSnapshot(lexiconSnapshot(), primitiveSnapshot(), checked.snapshot());
  }

  /** Reads and verifies a source-facts sidecar bound to this mapped image. */
  public M3FileSnapshot snapshotWithSourceFacts(Path sourceFactsImage) throws IOException {
    return snapshot(M3FileSourceFactsImage.readMappedFacts(sourceFactsImage, this));
  }

  /** Returns a detached ordered projection snapshot directly from the authenticated mapped image. */
  public M3FileProjectionSnapshot projectionSnapshot(
      M3FileTokenKind... selectedKinds) {
    return projectionSnapshot(M3FileProjection.maskOf(selectedKinds));
  }

  /** Returns the mapped image's detached semantic-token projection. */
  public M3FileProjectionSnapshot semanticProjectionSnapshot() {
    return projectionSnapshot(M3FileProjection.semanticKindMask());
  }

  /** Returns the mapped image's detached whitespace/newline/comment projection. */
  public M3FileProjectionSnapshot triviaProjectionSnapshot() {
    return projectionSnapshot(M3FileProjection.triviaKindMask());
  }

  /** Returns one defensive export of every authenticated primitive image lane. */
  public M3FilePrimitiveSnapshot primitiveSnapshot() {
    int[] localHandles = new int[tokenCount];
    byte[] tokenKinds = new byte[tokenCount];
    int[] utf16Offsets = new int[tokenCount + 1];
    int[] lineStarts = new int[lineCount];
    int[] frequencies = new int[uniqueCount];
    int[] firstOccurrences = new int[uniqueCount];
    int[] lastOccurrences = new int[uniqueCount];
    int[] postingStarts = new int[uniqueCount + 1];
    int[] postings = new int[tokenCount];
    for (int token = 0; token < tokenCount; token++) {
      localHandles[token] = localHandleAt(token);
      tokenKinds[token] = buffer.get(layout.kindsStart() + token);
    }
    for (int boundary = 0; boundary <= tokenCount; boundary++) {
      utf16Offsets[boundary] = utf16Offset(boundary);
    }
    for (int line = 0; line < lineCount; line++) {
      lineStarts[line] = lineStartUtf16(line);
    }
    for (int handle = 0; handle < uniqueCount; handle++) {
      frequencies[handle] = occurrenceCount(handle);
      firstOccurrences[handle] = firstOccurrence(handle);
      lastOccurrences[handle] = lastOccurrence(handle);
      postingStarts[handle] = postingStart(handle);
    }
    postingStarts[uniqueCount] = postingStart(uniqueCount);
    for (int handle = 0; handle < uniqueCount; handle++) {
      int start = postingStarts[handle];
      int end = postingStarts[handle + 1];
      for (int at = start; at < end; at++) {
        postings[at] = occurrenceAt(handle, at - start);
      }
    }
    return new M3FilePrimitiveSnapshot(
        localHandles,
        tokenKinds,
        utf16Offsets,
        lineStarts,
        frequencies,
        firstOccurrences,
        lastOccurrences,
        postingStarts,
        postings,
        null,
        null);
  }

  private M3FileProjectionSnapshot projectionSnapshot(int kindMask) {
    int selected = 0;
    for (int token = 0; token < tokenCount; token++) {
      if ((kindMask & (1 << tokenKindAt(token).ordinal())) != 0) selected++;
    }
    int[] positions = new int[selected];
    int at = 0;
    for (int token = 0; token < tokenCount; token++) {
      if ((kindMask & (1 << tokenKindAt(token).ordinal())) != 0) positions[at++] = token;
    }
    return new M3FileProjectionSnapshot(kindMask, positions);
  }

  public long contentFingerprint64() { return fingerprint; }

  public long mappedBytes() { return layout.end(); }

  public int localHandleAt(int tokenIndex) {
    return M3FileImage.readLocalHandle(
        buffer,
        layout.tokensStart(),
        tokenWidth,
        Objects.checkIndex(tokenIndex, tokenCount));
  }

  public M3FileTokenKind tokenKindAt(int tokenIndex) {
    return M3FileTokenKind.fromCode(
        buffer.get(layout.kindsStart() + Objects.checkIndex(tokenIndex, tokenCount)));
  }

  public int utf16Offset(int tokenBoundary) {
    if (tokenBoundary < 0 || tokenBoundary > tokenCount) {
      throw new IndexOutOfBoundsException("token boundary " + tokenBoundary);
    }
    return buffer.getInt(layout.utf16OffsetsStart() + tokenBoundary * Integer.BYTES);
  }

  public int utf16Length() { return utf16Offset(tokenCount); }

  public int lineStartUtf16(int lineIndex) {
    return buffer.getInt(
        layout.lineStartsStart() + Objects.checkIndex(lineIndex, lineCount) * Integer.BYTES);
  }

  public int resolverTokenCountForLocal(int localHandle) {
    int handle = Objects.checkIndex(localHandle, uniqueCount);
    return lexiconStart(handle + 1) - lexiconStart(handle);
  }

  public long resolverTokenIdForLocal(int localHandle, int ordinal) {
    int handle = Objects.checkIndex(localHandle, uniqueCount);
    int start = lexiconStart(handle);
    int count = lexiconStart(handle + 1) - start;
    return buffer.getLong(
        layout.lexiconIdsStart()
            + (start + Objects.checkIndex(ordinal, count)) * Long.BYTES);
  }

  public int resolverTokenCountAt(int tokenIndex) {
    return resolverTokenCountForLocal(localHandleAt(tokenIndex));
  }

  public long resolverTokenIdAt(int tokenIndex, int ordinal) {
    return resolverTokenIdForLocal(localHandleAt(tokenIndex), ordinal);
  }

  public M3FileText tokenAt(int tokenIndex) {
    int handle = localHandleAt(tokenIndex);
    int count = resolverTokenCountForLocal(handle);
    long[] ids = new long[count];
    for (int ordinal = 0; ordinal < count; ordinal++) {
      ids[ordinal] = resolverTokenIdForLocal(handle, ordinal);
    }
    return new M3FileText(resolver, ids);
  }

  /** Returns the complete mapped occurrence stream as resolver-owned indexed text. */
  public M3FileText asM3String() {
    M3FileText cached = indexedText;
    if (cached != null) return cached;
    M3FileTextBuilder builder = new M3FileTextBuilder(resolver, tokenCount);
    for (int token = 0; token < tokenCount; token++) {
      builder.append(tokenAt(token));
    }
    M3FileText built = builder.build();
    synchronized (this) {
      if (indexedText == null) indexedText = built;
      return indexedText;
    }
  }

  /** Computes the mapped indexed-text digest through the file text descriptor over ordinary String owners. */
  public M3Hash256 sha256(Charset charset) {
    return asM3String().sha256(Objects.requireNonNull(charset, "charset"));
  }

  public int occurrenceCount(int localHandle) {
    return buffer.getInt(
        layout.frequenciesStart()
            + Objects.checkIndex(localHandle, uniqueCount) * Integer.BYTES);
  }

  public int firstOccurrence(int localHandle) {
    return buffer.getInt(
        layout.firstStarts()
            + Objects.checkIndex(localHandle, uniqueCount) * Integer.BYTES);
  }

  public int lastOccurrence(int localHandle) {
    return buffer.getInt(
        layout.lastStarts()
            + Objects.checkIndex(localHandle, uniqueCount) * Integer.BYTES);
  }

  public int occurrenceAt(int localHandle, int ordinal) {
    int handle = Objects.checkIndex(localHandle, uniqueCount);
    int start = postingStart(handle);
    int count = postingStart(handle + 1) - start;
    return buffer.getInt(
        layout.postingsStart()
            + (start + Objects.checkIndex(ordinal, count)) * Integer.BYTES);
  }

  /**
   * Exact KMP over the mapped adaptive-width local-handle lane.
   *
   * <p>Java is the semantic oracle; large mapped lanes may execute through one coarse JNI call.</p>
   */
  public int indexOf(int[] localPattern, int fromToken) {
    int[] pattern = Objects.requireNonNull(localPattern, "localPattern");
    if (fromToken < 0 || fromToken > tokenCount) {
      throw new IndexOutOfBoundsException("from token " + fromToken);
    }
    for (int handle : pattern) Objects.checkIndex(handle, uniqueCount);
    return M3FileNative.mappedFileTokenIndexOf(
        buffer, layout.tokensStart(), tokenWidth, tokenCount, pattern, fromToken);
  }

  public void appendRangeTo(int startToken, int endToken, StringBuilder out) {
    Objects.requireNonNull(out, "out");
    Objects.checkFromToIndex(startToken, endToken, tokenCount);
    for (int token = startToken; token < endToken; token++) {
      int handle = localHandleAt(token);
      int count = resolverTokenCountForLocal(handle);
      for (int ordinal = 0; ordinal < count; ordinal++) {
        resolver.appendTo(resolverTokenIdForLocal(handle, ordinal), out);
      }
    }
  }

  public String materialize() {
    StringBuilder out = new StringBuilder(utf16Length());
    appendRangeTo(0, tokenCount, out);
    return out.toString();
  }

  @Override
  public String toString() { return materialize(); }

  @Override
  public void close() throws IOException { channel.close(); }

  ByteBuffer mappedBufferForReadOnlyKernel() {
    return buffer.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
  }

  int tokenLaneStartForReadOnlyKernel() { return layout.tokensStart(); }

  private int lexiconStart(int boundary) {
    if (boundary < 0 || boundary > uniqueCount) {
      throw new IndexOutOfBoundsException("lexicon boundary " + boundary);
    }
    return buffer.getInt(layout.lexiconStartsStart() + boundary * Integer.BYTES);
  }

  private int postingStart(int boundary) {
    if (boundary < 0 || boundary > uniqueCount) {
      throw new IndexOutOfBoundsException("posting boundary " + boundary);
    }
    return buffer.getInt(layout.postingStartsStart() + boundary * Integer.BYTES);
  }

  private static void validateLanguages(
      ByteBuffer buffer,
      M3FileImage.Layout layout,
      int languageCount,
      M3FileResolver resolver)
      throws IOException {
    int previous = -1;
    for (int i = 0; i < languageCount; i++) {
      int at = layout.languageStart() + i * (Integer.BYTES + Long.BYTES);
      int language = buffer.getInt(at);
      long expected = buffer.getLong(at + Integer.BYTES);
      if (language < 0 || language > M3FileId.MAX_LANGUAGE_ID || language <= previous) {
        throw new IOException("invalid M3File language table");
      }
      OptionalLong actual = M3FileCoordinates.tryOf(resolver, language);
      if (actual.isEmpty() || actual.getAsLong() != expected) {
        throw new IOException("M3File resolver generation mismatch for language " + language);
      }
      previous = language;
    }
  }

  private static void validateStructure(
      ByteBuffer buffer,
      M3FileImage.Layout layout,
      int tokenCount,
      int uniqueCount,
      int tokenWidth,
      int lineCount,
      int languageCount,
      int resolverTokenCount,
      M3FileResolver resolver)
      throws IOException {
    if (buffer.getInt(layout.lexiconStartsStart()) != 0
        || buffer.getInt(
                layout.lexiconStartsStart() + uniqueCount * Integer.BYTES)
            != resolverTokenCount) {
      throw new IOException("invalid M3File lexicon CSR sentinels");
    }
    int previous = 0;
    for (int i = 1; i <= uniqueCount; i++) {
      int next = buffer.getInt(layout.lexiconStartsStart() + i * Integer.BYTES);
      if (next < previous || next > resolverTokenCount) {
        throw new IOException("invalid M3File lexicon CSR");
      }
      previous = next;
    }

    boolean[] languageAllowed = new boolean[M3FileId.MAX_LANGUAGE_ID + 1];
    for (int i = 0; i < languageCount; i++) {
      int at = layout.languageStart() + i * (Integer.BYTES + Long.BYTES);
      languageAllowed[buffer.getInt(at)] = true;
    }
    for (int i = 0; i < resolverTokenCount; i++) {
      long id = buffer.getLong(layout.lexiconIdsStart() + i * Long.BYTES);
      if (M3FileId.isOverlay(id)) throw new IOException("overlay ID in durable M3File image");
      int language = resolver.languageId(id);
      if (language < 0
          || language > M3FileId.MAX_LANGUAGE_ID
          || !languageAllowed[language]) {
        throw new IOException("resolver ID language missing from M3File generation table");
      }
    }

    for (int token = 0; token < tokenCount; token++) {
      int handle =
          M3FileImage.readLocalHandle(buffer, layout.tokensStart(), tokenWidth, token);
      if (handle < 0 || handle >= uniqueCount) {
        throw new IOException("M3File token local handle out of range");
      }
      int kind = Byte.toUnsignedInt(buffer.get(layout.kindsStart() + token));
      if (kind >= M3FileTokenKind.values().length) {
        throw new IOException("M3File token kind out of range");
      }
    }

    int utf16Previous = buffer.getInt(layout.utf16OffsetsStart());
    if (utf16Previous != 0) throw new IOException("M3File UTF-16 offsets must start at zero");
    for (int i = 1; i <= tokenCount; i++) {
      int next = buffer.getInt(layout.utf16OffsetsStart() + i * Integer.BYTES);
      if (next < utf16Previous) throw new IOException("M3File UTF-16 offsets not monotonic");
      utf16Previous = next;
    }

    if (lineCount <= 0 || buffer.getInt(layout.lineStartsStart()) != 0) {
      throw new IOException("M3File line starts must begin with zero");
    }
    int linePrevious = 0;
    for (int line = 1; line < lineCount; line++) {
      int next = buffer.getInt(layout.lineStartsStart() + line * Integer.BYTES);
      if (next < linePrevious || next > utf16Previous) {
        throw new IOException("invalid M3File line start");
      }
      linePrevious = next;
    }

    if (buffer.getInt(layout.postingStartsStart()) != 0
        || buffer.getInt(
                layout.postingStartsStart() + uniqueCount * Integer.BYTES)
            != tokenCount) {
      throw new IOException("invalid M3File posting CSR sentinels");
    }
    int postingPrevious = 0;
    for (int handle = 0; handle < uniqueCount; handle++) {
      int start =
          buffer.getInt(layout.postingStartsStart() + handle * Integer.BYTES);
      int end =
          buffer.getInt(layout.postingStartsStart() + (handle + 1) * Integer.BYTES);
      if (start != postingPrevious || end < start || end > tokenCount) {
        throw new IOException("invalid M3File posting CSR");
      }
      int frequency =
          buffer.getInt(layout.frequenciesStart() + handle * Integer.BYTES);
      if (frequency != end - start) {
        throw new IOException("M3File frequency/posting mismatch");
      }
      int first = buffer.getInt(layout.firstStarts() + handle * Integer.BYTES);
      int last = buffer.getInt(layout.lastStarts() + handle * Integer.BYTES);
      if (frequency == 0) {
        if (first != -1 || last != -1) {
          throw new IOException("empty M3File posting row has first/last");
        }
      } else {
        int rowPrevious = -1;
        for (int at = start; at < end; at++) {
          int position = buffer.getInt(layout.postingsStart() + at * Integer.BYTES);
          if (position <= rowPrevious || position < 0 || position >= tokenCount) {
            throw new IOException("M3File postings are not strictly ordered");
          }
          int actualHandle =
              M3FileImage.readLocalHandle(
                  buffer, layout.tokensStart(), tokenWidth, position);
          if (actualHandle != handle) {
            throw new IOException("M3File posting does not reference owning local handle");
          }
          rowPrevious = position;
        }
        int rowFirst = buffer.getInt(layout.postingsStart() + start * Integer.BYTES);
        int rowLast = buffer.getInt(layout.postingsStart() + (end - 1) * Integer.BYTES);
        if (first != rowFirst || last != rowLast) {
          throw new IOException("M3File first/last posting mismatch");
        }
      }
      postingPrevious = end;
    }
  }
}
