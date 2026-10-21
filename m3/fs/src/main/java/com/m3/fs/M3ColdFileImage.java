// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.IntToLongFunction;

/** Read-only admission of the existing SMFILE01 image, with no resolver or string ownership. */
final class M3ColdFileImage {
  static final long MAGIC = 0x534d46494c453031L;
  static final int FORMAT = 1;
  static final int HEADER_BYTES = 48;
  private static final long DOMAIN = 0x561aef0143ec74dfL;
  private M3ColdFileImage() {}

  /** Returns fingerprint, token count and unique count as primitive columns. */
  static long[] validate(M3MappedObject image, IntToLongFunction languages) throws IOException {
    try {
      ByteBuffer data = image.bytes(0, image.size()).order(ByteOrder.BIG_ENDIAN);
      if (data.remaining() < HEADER_BYTES || data.getLong(0) != MAGIC || data.getInt(8) != FORMAT
          || data.getInt(36) != 0) throw new IOException("invalid SMFILE01 header");
      int tokens = data.getInt(12), unique = data.getInt(16), width = data.getInt(20);
      int lines = data.getInt(24), languageCount = data.getInt(28), flat = data.getInt(32);
      if (languageCount > 65536) throw new IOException("SMFILE01 language count");
      int[] layout = layout(tokens, unique, width, lines, languageCount, flat);
      if (layout[12] != data.remaining()) throw new IOException("SMFILE01 image length mismatch");
      validateLanguages(data, layout, languageCount, languages);
      validateStructure(data, layout, tokens, unique, width, lines, languageCount, flat, languages);
      long fingerprint = fingerprint(data, layout, tokens, unique, width, lines, languageCount, flat);
      if (fingerprint != data.getLong(40)) throw new IOException("SMFILE01 fingerprint mismatch");
      return new long[] { fingerprint, tokens, unique };
    } catch (RuntimeException malformed) {
      throw new IOException("malformed SMFILE01 image", malformed);
    }
  }

  /** Full qualification is checked before the legacy generation/structure admission. */
  static long[] validateQualified(M3MappedObject image, IntToLongFunction languages,
      java.util.Map<Integer, String> senderSeals,
      java.util.function.IntFunction<String> receiverSeal) throws IOException {
    java.util.Objects.requireNonNull(languages, "languages");
    java.util.Objects.requireNonNull(receiverSeal, "receiverSeal");
    java.util.Map<Integer, String> pins = java.util.Map.copyOf(senderSeals);
    try {
      ByteBuffer data = image.bytes(0, image.size()).order(ByteOrder.BIG_ENDIAN);
      if (data.remaining() < HEADER_BYTES || data.getLong(0) != MAGIC || data.getInt(8) != FORMAT)
        throw new IOException("invalid qualified SMFILE01 header");
      int count = data.getInt(28);
      if (count < 0 || count > 65536 || count != pins.size()
          || (long) HEADER_BYTES + count * 12L > data.remaining())
        throw new IOException("UnicodeX qualification language coverage mismatch");
      int previous = -1;
      for (int i = 0; i < count; i++) {
        int language = data.getInt(HEADER_BYTES + i * 12);
        String sender = pins.get(language);
        if (language < 0 || language > 65535 || language <= previous
            || sender == null || !sender.matches("[0-9a-f]{64}")
            || !sender.equals(receiverSeal.apply(language)))
          throw new IOException("UnicodeX qualification mismatch for language " + language);
        previous = language;
      }
    } catch (RuntimeException malformed) {
      throw new IOException("malformed qualified SMFILE01 image", malformed);
    }
    return validate(image, languages);
  }


  private static long mix(long left, long right) {
    long z = left ^ Long.rotateLeft(right, 29) ^ 0x9e3779b97f4a7c15L;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

  static int[] layout(
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
      throw new IllegalArgumentException("invalid MIndexFile image shape");
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
    return new int[] {
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
        end};
  }

  static long fingerprint(
      ByteBuffer source,
      int[] layout,
      int tokenCount,
      int uniqueCount,
      int tokenWidth,
      int lineCount,
      int languageCount,
      int resolverTokenCount) {
    ByteBuffer data = source.duplicate().order(ByteOrder.BIG_ENDIAN);
    long hash = headerHash(tokenCount, uniqueCount, tokenWidth, lineCount, languageCount, resolverTokenCount);
    for (int i = 0; i < languageCount; i++) {
      int at = layout[0] + i * (Integer.BYTES + Long.BYTES);
      hash = mix(hash, data.getInt(at));
      hash = mix(hash, data.getLong(at + Integer.BYTES));
    }
    for (int i = 0; i <= uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout[1] + i * Integer.BYTES));
    }
    for (int i = 0; i < resolverTokenCount; i++) {
      hash = mix(hash, data.getLong(layout[2] + i * Long.BYTES));
    }
    for (int i = 0; i < tokenCount; i++) {
      hash = mix(hash, readLocalHandle(data, layout[3], tokenWidth, i));
    }
    for (int i = 0; i < tokenCount; i++) {
      hash = mix(hash, Byte.toUnsignedInt(data.get(layout[4] + i)));
    }
    for (int i = 0; i <= tokenCount; i++) {
      hash = mix(hash, data.getInt(layout[5] + i * Integer.BYTES));
    }
    for (int i = 0; i < lineCount; i++) {
      hash = mix(hash, data.getInt(layout[6] + i * Integer.BYTES));
    }
    for (int i = 0; i < uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout[7] + i * Integer.BYTES));
    }
    for (int i = 0; i < uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout[8] + i * Integer.BYTES));
    }
    for (int i = 0; i < uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout[9] + i * Integer.BYTES));
    }
    for (int i = 0; i <= uniqueCount; i++) {
      hash = mix(hash, data.getInt(layout[10] + i * Integer.BYTES));
    }
    for (int i = 0; i < tokenCount; i++) {
      hash = mix(hash, data.getInt(layout[11] + i * Integer.BYTES));
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

  private static void validateLanguages(
      ByteBuffer buffer,
      int[] layout,
      int languageCount,
      IntToLongFunction resolver)
      throws IOException {
    int previous = -1;
    for (int i = 0; i < languageCount; i++) {
      int at = layout[0] + i * (Integer.BYTES + Long.BYTES);
      int language = buffer.getInt(at);
      long expected = buffer.getLong(at + Integer.BYTES);
      if (language < 0 || language > 0xffff || language <= previous) {
        throw new IOException("invalid MIndexFile language table");
      }
      if (resolver != null && resolver.applyAsLong(language) != expected) {
        throw new IOException("MIndexFile resolver generation mismatch for language " + language);
      }
      previous = language;
    }
  }

  private static void validateStructure(
      ByteBuffer buffer,
      int[] layout,
      int tokenCount,
      int uniqueCount,
      int tokenWidth,
      int lineCount,
      int languageCount,
      int resolverTokenCount,
      IntToLongFunction resolver)
      throws IOException {
    if (buffer.getInt(layout[1]) != 0
        || buffer.getInt(
                layout[1] + uniqueCount * Integer.BYTES)
            != resolverTokenCount) {
      throw new IOException("invalid MIndexFile lexicon CSR sentinels");
    }
    int previous = 0;
    for (int i = 1; i <= uniqueCount; i++) {
      int next = buffer.getInt(layout[1] + i * Integer.BYTES);
      if (next < previous || next > resolverTokenCount) {
        throw new IOException("invalid MIndexFile lexicon CSR");
      }
      previous = next;
    }

    boolean[] languageAllowed = new boolean[0xffff + 1];
    for (int i = 0; i < languageCount; i++) {
      int at = layout[0] + i * (Integer.BYTES + Long.BYTES);
      languageAllowed[buffer.getInt(at)] = true;
    }
    for (int i = 0; i < resolverTokenCount; i++) {
      long id = buffer.getLong(layout[2] + i * Long.BYTES);
      if (((id & (1L << 47)) != 0)) throw new IOException("overlay ID in durable MIndexFile image");
      int language = (int) (id >>> 48);
      if (language < 0
          || language > 0xffff
          || !languageAllowed[language]) {
        throw new IOException("resolver ID language missing from MIndexFile generation table");
      }
    }

    for (int token = 0; token < tokenCount; token++) {
      int handle =
          readLocalHandle(buffer, layout[3], tokenWidth, token);
      if (handle < 0 || handle >= uniqueCount) {
        throw new IOException("MIndexFile token local handle out of range");
      }
      int kind = Byte.toUnsignedInt(buffer.get(layout[4] + token));
      if (kind >= 8) {
        throw new IOException("MIndexFile token kind out of range");
      }
    }

    int utf16Previous = buffer.getInt(layout[5]);
    if (utf16Previous != 0) throw new IOException("MIndexFile UTF-16 offsets must start at zero");
    for (int i = 1; i <= tokenCount; i++) {
      int next = buffer.getInt(layout[5] + i * Integer.BYTES);
      if (next < utf16Previous) throw new IOException("MIndexFile UTF-16 offsets not monotonic");
      utf16Previous = next;
    }

    if (lineCount <= 0 || buffer.getInt(layout[6]) != 0) {
      throw new IOException("MIndexFile line starts must begin with zero");
    }
    int linePrevious = 0;
    for (int line = 1; line < lineCount; line++) {
      int next = buffer.getInt(layout[6] + line * Integer.BYTES);
      if (next < linePrevious || next > utf16Previous) {
        throw new IOException("invalid MIndexFile line start");
      }
      linePrevious = next;
    }

    if (buffer.getInt(layout[10]) != 0
        || buffer.getInt(
                layout[10] + uniqueCount * Integer.BYTES)
            != tokenCount) {
      throw new IOException("invalid MIndexFile posting CSR sentinels");
    }
    int postingPrevious = 0;
    for (int handle = 0; handle < uniqueCount; handle++) {
      int start =
          buffer.getInt(layout[10] + handle * Integer.BYTES);
      int end =
          buffer.getInt(layout[10] + (handle + 1) * Integer.BYTES);
      if (start != postingPrevious || end < start || end > tokenCount) {
        throw new IOException("invalid MIndexFile posting CSR");
      }
      int frequency =
          buffer.getInt(layout[7] + handle * Integer.BYTES);
      if (frequency != end - start) {
        throw new IOException("MIndexFile frequency/posting mismatch");
      }
      int first = buffer.getInt(layout[8] + handle * Integer.BYTES);
      int last = buffer.getInt(layout[9] + handle * Integer.BYTES);
      if (frequency == 0) {
        if (first != -1 || last != -1) {
          throw new IOException("empty MIndexFile posting row has first/last");
        }
      } else {
        int rowPrevious = -1;
        for (int at = start; at < end; at++) {
          int position = buffer.getInt(layout[11] + at * Integer.BYTES);
          if (position <= rowPrevious || position < 0 || position >= tokenCount) {
            throw new IOException("MIndexFile postings are not strictly ordered");
          }
          int actualHandle =
              readLocalHandle(
                  buffer, layout[3], tokenWidth, position);
          if (actualHandle != handle) {
            throw new IOException("MIndexFile posting does not reference owning local handle");
          }
          rowPrevious = position;
        }
        int rowFirst = buffer.getInt(layout[11] + start * Integer.BYTES);
        int rowLast = buffer.getInt(layout[11] + (end - 1) * Integer.BYTES);
        if (first != rowFirst || last != rowLast) {
          throw new IOException("MIndexFile first/last posting mismatch");
        }
      }
      postingPrevious = end;
    }
  }
}
