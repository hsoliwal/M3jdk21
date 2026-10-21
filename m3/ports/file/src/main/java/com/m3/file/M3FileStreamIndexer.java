// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * One-physical-read file admission into M3File.
 *
 * <p>The raw input stream simultaneously feeds whole-file SHA-256 and byte facts. A strict UTF-8
 * decoder feeds the lossless tokenizer. Each token updates its own reusable SHA-256 state while the
 * token characters are first observed, then is admitted directly to the file descriptor
 * resolver. No source payload is reopened or reread.</p>
 */
public final class M3FileStreamIndexer {
  private M3FileStreamIndexer() {}

  /**
   * Receives the detached snapshot after all payload-derived facts have been frozen and before
   * the stream's {@link M3FileStreamEvents.Finished} event is published.
   *
   * <p>The callback is an observational publication seam for serializers and JNI adapters. It
   * does not reread the payload and it does not become the canonical M3File owner. If it
   * throws, the traversal fails without publishing {@code Finished}.</p>
   */
  @FunctionalInterface
  public interface SnapshotSink {
    void accept(M3FileSnapshot snapshot) throws IOException;
  }

  public static M3File plainText(
      Path path, M3FileResolver resolver, int languageId) throws IOException {
    return index(path, null, resolver, languageId, false, M3FileStreamEvents.noOp(), false, null);
  }

  /** Admits plain UTF-8 text and publishes raw bytes, token spans and metadata during that pass. */
  public static M3File plainText(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3FileStreamEvents.Sink events) throws IOException {
    return index(path, null, resolver, languageId, false, events, false, null);
  }

  /** Admits plain text and publishes the unified precomputed snapshot before {@code Finished}. */
  public static M3File plainText(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3FileStreamEvents.Sink events,
      SnapshotSink snapshotSink) throws IOException {
    return index(path, null, resolver, languageId, false, events, false,
        Objects.requireNonNull(snapshotSink, "snapshotSink"));
  }

  /** Admits plain text while deriving the optional rolling lanes during token admission. */
  public static M3File plainText(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3FileStreamEvents.Sink events,
      boolean precomputeRollingIndex) throws IOException {
    return index(path, null, resolver, languageId, false, events, precomputeRollingIndex, null);
  }

  /** Admits plain text with optional rolling lanes and publishes the unified snapshot. */
  public static M3File plainText(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3FileStreamEvents.Sink events,
      boolean precomputeRollingIndex,
      SnapshotSink snapshotSink) throws IOException {
    return index(path, null, resolver, languageId, false, events, precomputeRollingIndex,
        Objects.requireNonNull(snapshotSink, "snapshotSink"));
  }

  public static M3File codeLike(
      Path path, M3FileResolver resolver, int languageId) throws IOException {
    return index(path, null, resolver, languageId, true, M3FileStreamEvents.noOp(), false, null);
  }

  /** Admits code-like UTF-8 and publishes raw bytes, token spans and metadata during that pass. */
  public static M3File codeLike(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3FileStreamEvents.Sink events) throws IOException {
    return index(path, null, resolver, languageId, true, events, false, null);
  }

  /** Admits code-like text and publishes the unified precomputed snapshot before {@code Finished}. */
  public static M3File codeLike(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3FileStreamEvents.Sink events,
      SnapshotSink snapshotSink) throws IOException {
    return index(path, null, resolver, languageId, true, events, false,
        Objects.requireNonNull(snapshotSink, "snapshotSink"));
  }

  /** Admits code-like text while deriving the optional rolling lanes during token admission. */
  public static M3File codeLike(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3FileStreamEvents.Sink events,
      boolean precomputeRollingIndex) throws IOException {
    return index(path, null, resolver, languageId, true, events, precomputeRollingIndex, null);
  }

  /** Admits code-like text with optional rolling lanes and publishes the unified snapshot. */
  public static M3File codeLike(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3FileStreamEvents.Sink events,
      boolean precomputeRollingIndex,
      SnapshotSink snapshotSink) throws IOException {
    return index(path, null, resolver, languageId, true, events, precomputeRollingIndex,
        Objects.requireNonNull(snapshotSink, "snapshotSink"));
  }

  /**
   * Admit one already-planned file without recapturing its before-read metadata.
   *
   * <p>The plan remains metadata-only; the payload is still opened exactly once and the
   * independent after-read stability capture remains authoritative.</p>
   */
  static M3File planned(
      M3FileUniversePlan.Entry entry,
      M3FileResolver resolver,
      int languageId,
      boolean codeLike,
      M3FileStreamEvents.Sink events)
      throws IOException {
    M3FileUniversePlan.Entry checkedEntry = Objects.requireNonNull(entry, "entry");
    return index(
        checkedEntry.path(),
        checkedEntry.attributes(),
        Objects.requireNonNull(resolver, "resolver"),
        languageId,
        codeLike,
        Objects.requireNonNull(events, "events"),
        false,
        null);
  }

  private static M3File index(
      Path path,
      M3FileAttributes plannedBefore,
      M3FileResolver resolver,
      int languageId,
      boolean codeLike,
      M3FileStreamEvents.Sink events,
      boolean precomputeRollingIndex,
      SnapshotSink snapshotSink) throws IOException {
    Path source = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    Objects.requireNonNull(resolver, "resolver");
    Objects.requireNonNull(events, "events");

    M3FileAttributes before =
        plannedBefore == null ? M3FileAttributes.capture(source) : plannedBefore;
    if (!source.toString().equals(before.normalizedPath())) {
      throw new IOException("planned M3File metadata path does not match source: " + source);
    }
    if (!before.isRegularFile()) {
      throw new IOException("M3File stream source is not a regular file: " + source);
    }

    events.accept(new M3FileStreamEvents.Started(before));

    M3FileBuilder fileBuilder = new M3FileBuilder(resolver);
    if (precomputeRollingIndex) fileBuilder.enableRollingIndex();
    M3FileSourceFacts.Builder factsBuilder = new M3FileSourceFacts.Builder();
    M3FileByteFacts.Builder byteFactsBuilder = new M3FileByteFacts.Builder();

    var decoder =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

    try (Reader reader =
        new InputStreamReader(
            new ObservingInputStream(Files.newInputStream(source), byteFactsBuilder, events), decoder)) {
      tokenize(reader, fileBuilder, factsBuilder, languageId, codeLike, events);
    }

    M3FileByteFacts byteFacts = byteFactsBuilder.finish();
    M3FileAttributes after = M3FileAttributes.capture(source);

    if (!before.sameContentIdentity(after)) {
      throw new IOException("source file changed during one-pass M3File admission");
    }
    if (byteFacts.byteCount() != before.size() || byteFacts.byteCount() != after.size()) {
      throw new IOException(
          "source byte count changed during one-pass M3File admission: read="
              + byteFacts.byteCount()
              + " before="
              + before.size()
              + " after="
              + after.size());
    }
    if (factsBuilder.utf8Length() != byteFacts.byteCount()) {
      throw new IOException(
          "strict UTF-8 accounting mismatch: decoded="
              + factsBuilder.utf8Length()
              + " raw="
              + byteFacts.byteCount());
    }

    M3FileSourceFacts sourceFacts =
        factsBuilder.finish(
            before,
            after,
            byteFacts,
            fileBuilder.tokenCount(),
            fileBuilder.uniqueTokenCount());
    if (!sourceFacts.stableDuringRead()) {
      throw new IOException("source file stability gate failed");
    }
    M3File result = fileBuilder.build(sourceFacts);
    if (snapshotSink != null) snapshotSink.accept(result.snapshot());
    events.accept(
        new M3FileStreamEvents.Finished(
            after,
            result.tokenCount(),
            result.uniqueTokenCount(),
            byteFacts.byteCount(),
            byteFacts.sha256().bytes()));
    return result;
  }

  private static void tokenize(
      Reader reader,
      M3FileBuilder fileBuilder,
      M3FileSourceFacts.Builder factsBuilder,
      int languageId,
      boolean codeLike,
      M3FileStreamEvents.Sink events)
      throws IOException {
    CodePointCursor cursor = new CodePointCursor(reader);
    TokenAccumulator token =
        new TokenAccumulator(languageId, fileBuilder, factsBuilder, events);

    for (int codePoint; (codePoint = cursor.next()) >= 0; ) {
      if (codePoint == '\r' || codePoint == '\n') {
        token.append(codePoint);
        if (codePoint == '\r') {
          int next = cursor.next();
          if (next == '\n') token.append(next);
          else if (next >= 0) cursor.push(next);
        }
        token.emit(M3FileTokenKind.NEWLINE);
        continue;
      }

      if (Character.isWhitespace(codePoint)) {
        token.append(codePoint);
        while (true) {
          int next = cursor.next();
          if (next < 0) break;
          if (next == '\r' || next == '\n' || !Character.isWhitespace(next)) {
            cursor.push(next);
            break;
          }
          token.append(next);
        }
        token.emit(M3FileTokenKind.WHITESPACE);
        continue;
      }

      if (codeLike && codePoint == '/') {
        int next = cursor.next();
        if (next == '/') {
          token.append(codePoint);
          token.append(next);
          while (true) {
            int current = cursor.next();
            if (current < 0) break;
            if (current == '\r' || current == '\n') {
              cursor.push(current);
              break;
            }
            token.append(current);
          }
          token.emit(M3FileTokenKind.COMMENT);
          continue;
        }
        if (next == '*') {
          token.append(codePoint);
          token.append(next);
          boolean star = false;
          while (true) {
            int current = cursor.next();
            if (current < 0) break;
            token.append(current);
            if (star && current == '/') break;
            star = current == '*';
          }
          token.emit(M3FileTokenKind.COMMENT);
          continue;
        }
        token.append(codePoint);
        if (next >= 0) cursor.push(next);
        token.emit(M3FileTokenKind.SYMBOL);
        continue;
      }

      if (codeLike && (codePoint == '\'' || codePoint == '"' || codePoint == '`')) {
        token.append(codePoint);
        boolean escaped = false;
        while (true) {
          int current = cursor.next();
          if (current < 0) break;
          token.append(current);
          if (escaped) {
            escaped = false;
          } else if (current == '\\') {
            escaped = true;
          } else if (current == codePoint) {
            break;
          }
        }
        token.emit(M3FileTokenKind.QUOTED);
        continue;
      }

      if (isWordStart(codePoint)) {
        token.append(codePoint);
        while (true) {
          int next = cursor.next();
          if (next < 0) break;
          if (!isWordPart(next)) {
            cursor.push(next);
            break;
          }
          token.append(next);
        }
        token.emit(M3FileTokenKind.WORD);
        continue;
      }

      if (Character.isDigit(codePoint)) {
        token.append(codePoint);
        while (true) {
          int next = cursor.next();
          if (next < 0) break;
          if (!(Character.isLetterOrDigit(next) || next == '_' || next == '.')) {
            cursor.push(next);
            break;
          }
          token.append(next);
        }
        token.emit(M3FileTokenKind.NUMBER);
        continue;
      }

      token.append(codePoint);
      token.emit(M3FileTokenKind.SYMBOL);
    }
  }

  private static boolean isWordStart(int codePoint) {
    return Character.isLetter(codePoint) || codePoint == '_' || codePoint == '$';
  }

  private static boolean isWordPart(int codePoint) {
    int type = Character.getType(codePoint);
    return Character.isLetterOrDigit(codePoint)
        || codePoint == '_'
        || codePoint == '$'
        || type == Character.NON_SPACING_MARK
        || type == Character.COMBINING_SPACING_MARK;
  }

  private static final class ObservingInputStream extends InputStream {
    private final InputStream delegate;
    private final M3FileByteFacts.Builder facts;
    private final M3FileStreamEvents.Sink events;
    private long byteOffset;

    private ObservingInputStream(
        InputStream delegate,
        M3FileByteFacts.Builder facts,
        M3FileStreamEvents.Sink events) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
      this.facts = Objects.requireNonNull(facts, "facts");
      this.events = Objects.requireNonNull(events, "events");
    }

    @Override
    public int read() throws IOException {
      int value = delegate.read();
      if (value >= 0) {
        facts.accept(value);
        events.accept(new M3FileStreamEvents.Bytes(byteOffset, new byte[] {(byte) value}));
        byteOffset = Math.incrementExact(byteOffset);
      }
      return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
      int read = delegate.read(bytes, offset, length);
      if (read > 0) {
        facts.accept(bytes, offset, read);
        events.accept(
            new M3FileStreamEvents.Bytes(
                byteOffset, Arrays.copyOfRange(bytes, offset, offset + read)));
        byteOffset = Math.addExact(byteOffset, read);
      }
      return read;
    }

    @Override
    public long skip(long count) throws IOException {
      if (count <= 0L) return 0L;
      byte[] buffer = new byte[(int) Math.min(8192L, count)];
      long remaining = count;
      while (remaining > 0L) {
        int read = read(buffer, 0, (int) Math.min(buffer.length, remaining));
        if (read < 0) break;
        remaining -= read;
      }
      return count - remaining;
    }

    @Override
    public int available() throws IOException {
      return delegate.available();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }

  private static final class CodePointCursor {
    private static final int NONE = Integer.MIN_VALUE;

    private final Reader reader;
    private int pushed = NONE;

    private CodePointCursor(Reader reader) {
      this.reader = Objects.requireNonNull(reader, "reader");
    }

    int next() throws IOException {
      if (pushed != NONE) {
        int result = pushed;
        pushed = NONE;
        return result;
      }

      int first = reader.read();
      if (first < 0) return -1;
      char unit = (char) first;
      if (Character.isLowSurrogate(unit)) throw new MalformedInputException(1);
      if (!Character.isHighSurrogate(unit)) return first;

      int second = reader.read();
      if (second < 0 || !Character.isLowSurrogate((char) second)) {
        throw new MalformedInputException(1);
      }
      return Character.toCodePoint(unit, (char) second);
    }

    void push(int codePoint) {
      if (codePoint < 0) throw new IllegalArgumentException("cannot push EOF");
      if (pushed != NONE) throw new IllegalStateException("code-point pushback already occupied");
      pushed = codePoint;
    }
  }

  private static final class TokenAccumulator {
    private final int languageId;
    private final M3FileBuilder fileBuilder;
    private final M3FileSourceFacts.Builder factsBuilder;
    private final M3FileStreamEvents.Sink events;
    private final Set<Integer> emittedLexiconHandles = new HashSet<>();
    private final StringBuilder text = new StringBuilder(64);
    private final MessageDigest sha256 = newSha256();
    private final byte[] digest = new byte[32];
    private int utf8Bytes;
    private int codePoints;
    private long utf8Offset;
    private int codePointOffset;

    private TokenAccumulator(
        int languageId,
        M3FileBuilder fileBuilder,
        M3FileSourceFacts.Builder factsBuilder,
        M3FileStreamEvents.Sink events) {
      this.languageId = languageId;
      this.fileBuilder = Objects.requireNonNull(fileBuilder, "fileBuilder");
      this.factsBuilder = Objects.requireNonNull(factsBuilder, "factsBuilder");
      this.events = Objects.requireNonNull(events, "events");
    }

    void append(int codePoint) {
      text.appendCodePoint(codePoint);
      updateUtf8(sha256, codePoint);
      utf8Bytes = Math.addExact(utf8Bytes, utf8Length(codePoint));
      codePoints = Math.incrementExact(codePoints);
    }

    void emit(M3FileTokenKind kind) throws IOException {
      if (text.isEmpty()) throw new IllegalStateException("cannot emit an empty token");
      int produced;
      try {
        produced = sha256.digest(digest, 0, digest.length);
      } catch (DigestException impossible) {
        throw new IllegalStateException("SHA-256 digest buffer rejected", impossible);
      }
      if (produced != digest.length) {
        throw new IllegalStateException("unexpected SHA-256 length " + produced);
      }

      int ordinal = fileBuilder.tokenCount();
      int utf16Start = fileBuilder.utf16Length();
      long utf8Start = utf8Offset;
      int codePointStart = codePointOffset;
      int localHandle = fileBuilder.addAndGetLocalHandle(languageId, text, kind);
      if (emittedLexiconHandles.add(localHandle)) {
        events.accept(
            new M3FileStreamEvents.Lexicon(
                localHandle, fileBuilder.resolverTokenIds(localHandle)));
      }
      factsBuilder.acceptToken(localHandle, utf8Bytes, codePoints, digest);
      long utf8End = Math.addExact(utf8Start, utf8Bytes);
      int codePointEnd = Math.addExact(codePointStart, codePoints);
      events.accept(
          new M3FileStreamEvents.Token(
              ordinal,
              localHandle,
              kind,
              utf16Start,
              fileBuilder.utf16Length(),
              utf8Start,
              utf8End,
              codePointStart,
              codePointEnd,
              digest));
      utf8Offset = utf8End;
      codePointOffset = codePointEnd;

      text.setLength(0);
      utf8Bytes = 0;
      codePoints = 0;
    }
  }

  private static MessageDigest newSha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }

  private static int utf8Length(int codePoint) {
    if (codePoint <= 0x7f) return 1;
    if (codePoint <= 0x7ff) return 2;
    if (codePoint <= 0xffff) return 3;
    return 4;
  }

  private static void updateUtf8(MessageDigest digest, int codePoint) {
    if (codePoint <= 0x7f) {
      digest.update((byte) codePoint);
    } else if (codePoint <= 0x7ff) {
      digest.update((byte) (0xc0 | (codePoint >>> 6)));
      digest.update((byte) (0x80 | (codePoint & 0x3f)));
    } else if (codePoint <= 0xffff) {
      digest.update((byte) (0xe0 | (codePoint >>> 12)));
      digest.update((byte) (0x80 | ((codePoint >>> 6) & 0x3f)));
      digest.update((byte) (0x80 | (codePoint & 0x3f)));
    } else {
      digest.update((byte) (0xf0 | (codePoint >>> 18)));
      digest.update((byte) (0x80 | ((codePoint >>> 12) & 0x3f)));
      digest.update((byte) (0x80 | ((codePoint >>> 6) & 0x3f)));
      digest.update((byte) (0x80 | (codePoint & 0x3f)));
    }
  }
}
