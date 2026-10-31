// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/** Independent JDK charset oracle and review-discovered adapter regression contracts. */
public final class M3FileCharsetProbe {
  private static long checks;
  private M3FileCharsetProbe() {}

  public static void main(String[] args) throws Exception {
    customCharsets();
    generationGuard();
    metadataLimits();
    int cases = 0, providers = 0;
    Random random = new Random(0x43484152534554L);
    for (Charset charset : Charset.availableCharsets().values()) {
      if (!charset.canEncode()) continue;
      providers++;
      for (int test = 0; test < 40; test++) {
        StringBuilder expected = new StringBuilder();
        M3FileDictionary dictionary = new M3FileDictionary();
        M3FileTextBuilder assembled = new M3FileTextBuilder(dictionary);
        for (int atom = 0; atom < 25; atom++) {
          StringBuilder piece = new StringBuilder();
          int length = random.nextInt(12);
          for (int unit = 0; unit < length; unit++) {
            char value = switch (random.nextInt(6)) {
              case 0 -> (char) random.nextInt(65536);
              case 1 -> (char) (0xd800 + random.nextInt(2048));
              case 2 -> (char) (0x4e00 + random.nextInt(2000));
              default -> (char) (' ' + random.nextInt(95));
            };
            piece.append(value);
          }
          String text = piece.toString();
          expected.append(text);
          assembled.appendText(3, text);
        }
        compareEncoding(expected.toString(), assembled.build(), charset);
        cases++;
      }
    }
    check(providers >= 6, "all standard JDK charsets must be available");
    System.out.println("M3_FILE_CHARSET_PASS checks=" + checks + " providers=" + providers
        + " randomCases=" + cases + " customAtomicOutput=9000 customInputSlices=true"
        + " malformedFrozenGenerationRefused=true metadataOverflowRefused=true");
  }

  private static void customCharsets() throws Exception {
    Charset wide = new ReviewingCharset("X-M3-Wide", 9000, false);
    M3FileDictionary dictionary = M3FileDictionary.frozen(3, "en", "v1", "X", "AB");
    M3FileText one = M3FileText.intern(dictionary, 3, "X");
    compareEncoding("X", one, wide);
    check(one.getBytes(wide).length == 9000, "atomic output exceeds initial 8192-byte staging");

    Charset slice = new ReviewingCharset("X-M3-Slice", 1, true);
    M3FileText two = M3FileText.intern(dictionary, 3, "AB");
    int initialSize = dictionary.size();
    compareEncoding("AB", two, slice);
    check(dictionary.size() == initialSize, "encoding input inspection cannot admit dictionary atoms");

    M3FileDictionary live = new M3FileDictionary();
    M3FileTextBuilder split = new M3FileTextBuilder(live);
    split.appendText(3, "x".repeat(4095));
    split.appendText(3, "\ud83d");
    split.appendText(3, "\ude00");
    split.appendText(3, "y".repeat(4100));
    String reference = "x".repeat(4095) + "😀" + "y".repeat(4100);
    M3FileText value = split.build();
    for (Charset charset : new Charset[] {StandardCharsets.UTF_8, StandardCharsets.UTF_16,
        StandardCharsets.UTF_16LE, StandardCharsets.US_ASCII, slice}) {
      compareEncoding(reference, value, charset);
    }
    byte[] channelBytes;
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    try (var channel = java.nio.channels.Channels.newChannel(out)) {
      long count = value.writeUtf8To(channel);
      channelBytes = out.toByteArray();
      check(count == channelBytes.length, "stream byte count");
    }
    check(Arrays.equals(reference.getBytes(StandardCharsets.UTF_8), channelBytes),
        "streaming UTF-8 preserves cross-window surrogate pair");

    Charset noProgress = new Charset("X-M3-NoProgress", new String[0]) {
      @Override public boolean contains(Charset other) { return other == this; }
      @Override public CharsetDecoder newDecoder() { return StandardCharsets.ISO_8859_1.newDecoder(); }
      @Override public CharsetEncoder newEncoder() {
        return new CharsetEncoder(this, 1, 1) {
          @Override protected CoderResult encodeLoop(CharBuffer input, ByteBuffer output) {
            return CoderResult.OVERFLOW;
          }
        };
      }
    };
    expect(IllegalStateException.class, () -> one.getBytes(noProgress));
  }

  private static void generationGuard() throws Exception {
    for (String malformed : new String[] {"\ud800", "\udc00", "\ud800x", "x\udc00", "\ud800\ud800"}) {
      expect(IllegalArgumentException.class, () -> M3FileDictionary.frozen(7, "en", "v1", malformed));
      expect(IllegalArgumentException.class, () -> M3FileDictionary.frozen(7, malformed, "v1", "?"));
      expect(IllegalArgumentException.class, () -> M3FileDictionary.frozen(7, "en", malformed, "?"));
    }
    M3FileDictionary question = M3FileDictionary.frozen(7, "en", "v1", "?");
    check(M3FileCoordinates.of(question, 7) == 7863382139593480843L,
        "valid donor fingerprint bytes must remain unchanged");
    String supplementary = "😀";
    M3FileDictionary good = M3FileDictionary.frozen(65535, "language-" + supplementary,
        "version-" + supplementary, supplementary);
    long id = good.intern(65535, supplementary);
    check(id == M3FileId.base(65535, 0), "highest language coordinate preserves base ordinal");
    check(good.resolve(id).equals(supplementary), "well-formed supplementary generation admitted");

    M3FileDictionary live = new M3FileDictionary();
    for (String malformed : new String[] {"\ud800", "\udc00", "\ud800x", "x\udc00"}) {
      long liveId = live.intern(7, malformed);
      check(M3FileId.isOverlay(liveId), "arbitrary UTF-16 remains nonportable live data");
      check(live.resolve(liveId).equals(malformed), "live source UTF-16 preserved");
      check(live.coordinateFingerprint(7).isEmpty(), "live generation has no durable fingerprint");
    }
    StringBuilder mutable = new StringBuilder("source");
    long admitted = live.intern(7, mutable);
    mutable.setCharAt(0, 'X');
    check(live.resolve(admitted).equals("source"), "mutable ingress is detached after admission");

    M3File file = new M3FileBuilder(good).add(65535, supplementary, M3FileTokenKind.OTHER).build();
    M3FileDictionary changed = M3FileDictionary.frozen(65535, "language-" + supplementary,
        "version-" + supplementary, "??");
    Path image = Files.createTempFile("m3-fixed-generation-", ".midxf");
    try {
      M3FileImage.write(file, image);
      M3FileText detached;
      try (MappedM3File mapped = MappedM3File.open(image, good)) {
        detached = mapped.asM3String();
        check(detached.materialize().equals(supplementary), "valid generation reopens exact text");
      }
      check(detached.materialize().equals(supplementary), "detached IDs do not borrow mapped channel");
      expect(IOException.class, () -> MappedM3File.open(image, changed));
    } finally {
      Files.deleteIfExists(image);
    }
  }

  private static void metadataLimits() {
    M3FileDictionary dictionary = new M3FileDictionary();
    M3FileText block = M3FileText.intern(dictionary, 1, "A");
    for (int power = 0; power < 30; power++) block = block.concat(block);
    check(block.length() == 1 << 30, "large shared descriptor UTF-16 length");
    check(block.tokenCount() == 1 << 30, "large shared descriptor ID count");
    check(block.charAt(0) == 'A' && block.charAt((1 << 30) - 1) == 'A',
        "large metadata range lookup");
    M3FileText limit = block;
    expect(ArithmeticException.class, () -> limit.concat(limit));
    check(limit.length() == 1 << 30, "failed oversized concat leaves original valid");
    M3FileDictionary second = new M3FileDictionary();
    M3FileText sameStringOtherResolver = M3FileText.intern(second, 1, "A");
    expect(IllegalArgumentException.class, () -> limit.concat(sameStringOtherResolver));
    expect(IllegalArgumentException.class,
        () -> new M3FileTextBuilder(dictionary).append(sameStringOtherResolver));
  }

  private static void compareEncoding(String expected, M3FileText value, Charset charset) throws Exception {
    check(expected.equals(value.materialize()), "materialized String: " + charset);
    byte[] bytes = expected.getBytes(charset);
    check(Arrays.equals(bytes, value.getBytes(charset)), "String bytes: " + charset);
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
    check(Arrays.equals(digest, value.sha256(charset).bytes()), "streaming digest: " + charset);
  }

  private static void check(boolean condition, String description) {
    checks++;
    if (!condition) throw new AssertionError(description);
  }

  private static void expect(Class<? extends Throwable> expected, Operation operation) {
    checks++;
    try { operation.run(); }
    catch (Throwable failure) {
      if (expected.isInstance(failure)) return;
      throw new AssertionError("Expected " + expected + ", got " + failure, failure);
    }
    throw new AssertionError("Expected " + expected);
  }

  @FunctionalInterface private interface Operation { void run() throws Exception; }

  private static final class ReviewingCharset extends Charset {
    private final int width;
    private final boolean inspectSlice;
    ReviewingCharset(String name, int width, boolean inspectSlice) {
      super(name, new String[0]); this.width = width; this.inspectSlice = inspectSlice;
    }
    @Override public boolean contains(Charset charset) { return charset == this; }
    @Override public CharsetDecoder newDecoder() {
      return new CharsetDecoder(this, 1, 1) {
        @Override protected CoderResult decodeLoop(ByteBuffer input, CharBuffer output) {
          while (input.hasRemaining()) {
            if (!output.hasRemaining()) return CoderResult.OVERFLOW;
            output.put((char) Byte.toUnsignedInt(input.get()));
          }
          return CoderResult.UNDERFLOW;
        }
      };
    }
    @Override public CharsetEncoder newEncoder() {
      return new CharsetEncoder(this, width, width) {
        private int stalledCapacity = -1;
        private int stalledPosition = -1;
        private int stalls;
        @Override protected CoderResult encodeLoop(CharBuffer input, ByteBuffer output) {
          while (input.hasRemaining()) {
            if (output.remaining() < width) {
              if (input.position() == stalledPosition && output.capacity() == stalledCapacity) {
                if (++stalls > 8) throw new AssertionError("consumer repeated identical output overflow");
              } else {
                stalledPosition = input.position(); stalledCapacity = output.capacity(); stalls = 0;
              }
              return CoderResult.OVERFLOW;
            }
            char value;
            if (inspectSlice) {
              value = input.subSequence(0, 1).toString().charAt(0);
              input.position(input.position() + 1);
            } else value = input.get();
            for (int i = 0; i < width; i++) output.put((byte) value);
          }
          return CoderResult.UNDERFLOW;
        }
      };
    }
  }
}
