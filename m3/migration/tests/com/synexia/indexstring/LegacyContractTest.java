// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** Runs unchanged against the pinned source closure and the port; no fake owners. */
public final class LegacyContractTest {
  private static long checks;
  private static long fingerprint = 0xcbf29ce484222325L;
  private LegacyContractTest() {}

  public static void main(String[] args) throws Exception {
    mutableBoundaries();
    legacyIdentity();
    allUtf16Units();
    randomRangesAndEncoding();
    byteViews();
    streamCursors();
    directViews();
    concurrency();
    System.out.println("LEGACY checks=" + checks + " fingerprint=" + Long.toUnsignedString(fingerprint, 16));
  }

  private static void mutableBoundaries() {
    char[] input = {'a', '\0', '\ud800', 'z'};
    FrozenChars value = FrozenChars.copyOf(input);
    input[0] = '!';
    equal(value.toString(), "a\0\ud800z");
    char[] output = value.copy();
    output[0] = '?';
    equal(value.charAt(0), 'a');
    equal(value.hash32(), value.toString().hashCode());
    expect(ReadOnlyBufferException.class, () -> value.asReadOnlyBuffer().put(0, 'x'));
    expect(NullPointerException.class, () -> FrozenChars.copyOf((char[]) null));
    expect(IndexOutOfBoundsException.class, () -> value.charAt(-1));
    expect(IndexOutOfBoundsException.class, () -> value.subSequence(3, 2));
    expect(IllegalStateException.class, value::directUtf16Bytes);
    char[] target = {'?', '?', '?', '?'};
    value.copyTo(1, target, 1, 2);
    equal(new String(target), "?\0\ud800?");
    expect(IndexOutOfBoundsException.class, () -> value.copyTo(0, target, 3, 2));
    equal(new String(target), "?\0\ud800?");
  }

  private static void legacyIdentity() {
    FrozenChars a = chars("a"), b = chars("b");
    MIndexJoinedChars first = MIndexJoinedChars.of(a, b);
    truth(first == MIndexJoinedChars.of(a, b));
    // This is the source's content-and-geometry policy, NOT owner identity.
    truth(first.sharesBackingWith(MIndexJoinedChars.of(chars("a"), chars("b"))));
    truth(!first.sharesBackingWith(MIndexJoinedChars.of(chars("ab"))));
    truth(first.subSequence(0, 2) == first);
    truth(first.sharesBackingWith(first.subSequence(1, 2)));
    FrozenChars[] supplied = {a, b};
    MIndexJoinedChars joined = MIndexJoinedChars.of(supplied);
    supplied[0] = chars("changed");
    equal(joined.toString(), "ab");
    equal(MIndexJoinedChars.of(chars(""), a, chars("")).toString(), "a");
    equal(MIndexJoinedChars.of().length(), 0);
    expect(NullPointerException.class, () -> MIndexJoinedChars.of((FrozenChars[]) null));
    expect(NullPointerException.class, () -> MIndexJoinedChars.of(a, null));
  }

  private static void allUtf16Units() {
    FrozenChars left = chars("L");
    FrozenChars right = chars("R");
    for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
      String text = "L" + (char) unit + "R";
      MIndexJoinedChars view = MIndexJoinedChars.of(left, FrozenChars.copyOf(new char[] {(char) unit}), right);
      equal(view.length(), text.length());
      equal(view.charAt(1), unit);
      equal(view.codePointAt(1), text.codePointAt(1));
      equal(view.codePointCount(), text.codePointCount(0, text.length()));
      equal(view.subSequence(1, 2).toString(), text.substring(1, 2));
      equal(view.toString(), text);
    }
    MIndexJoinedChars pair = MIndexJoinedChars.of(chars("\ud83d"), chars("\ude00"));
    equal(pair.codePointAt(0), 0x1f600);
    equal(pair.codePointCount(), 1);
    equal(pair.subSequence(0, 1).toString(), "\ud83d");
    equal(pair.subSequence(1, 2).toString(), "\ude00");
  }

  private static void randomRangesAndEncoding() {
    Random random = new Random(0x4d334a444b21L);
    Charset[] charsets = {StandardCharsets.UTF_8, StandardCharsets.UTF_16,
        StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE, StandardCharsets.US_ASCII,
        Charset.forName("ISO-2022-JP")};
    for (int sample = 0; sample < 1200; sample++) {
      int length = random.nextInt(25);
      char[] data = new char[length];
      for (int i = 0; i < length; i++) data[i] = (char) random.nextInt(65536);
      String oracle = new String(data);
      int seam = random.nextInt(length + 1);
      MIndexJoinedChars view = MIndexJoinedChars.of(chars(oracle.substring(0, seam)), chars(oracle.substring(seam)));
      equal(view.toString(), oracle);
      for (int round = 0; round < 4; round++) {
        int from = random.nextInt(length + 1);
        int to = from + random.nextInt(length - from + 1);
        MIndexJoinedChars range = view.subSequence(from, to);
        equal(range.toString(), oracle.substring(from, to));
        truth(range.sharesBackingWith(view));
        equal(range.detach().hash32(), oracle.substring(from, to).hashCode());
        equal(range.subSequence(0, range.length()).toString(), oracle.substring(from, to));
        for (Charset charset : charsets) {
          truth(Arrays.equals(range.encode(charset).copy(), oracle.substring(from, to).getBytes(charset)));
        }
      }
    }
  }

  private static void byteViews() {
    byte[] input = {0, -1, 42, 80};
    FrozenBytes bytes = FrozenBytes.copyOf(input);
    input[0] = 99;
    equal(bytes.byteAt(0), 0);
    equal(bytes.retainedBytes(), 4);
    FrozenBytes range = bytes.slice(1, 3);
    equal(range.retainedBytes(), 4);
    equal(range.compact().retainedBytes(), 2);
    equal(range.hash32(), fnv(new byte[] {-1, 42}));
    byte[] copied = range.copy(); copied[0] = 0;
    equal(range.byteAt(0), -1);
    expect(ReadOnlyBufferException.class, () -> bytes.asReadOnlyBuffer().put(0, (byte) 1));
    MIndexJoinedBytes joined = MIndexJoinedBytes.of(bytes, range);
    truth(joined == MIndexJoinedBytes.of(bytes, range));
    truth(joined.sharesBackingWith(joined.slice(1, 4)));
    truth(Arrays.equals(joined.copy(), new byte[] {0, -1, 42, 80, -1, 42}));
    equal(joined.slice(4, 6).byteAt(1), 42);
    equal(MIndexJoinedBytes.of().length(), 0);
    expect(IndexOutOfBoundsException.class, () -> joined.byteAt(joined.length()));
  }

  private static void streamCursors() throws IOException {
    MIndexJoinedChars chars = MIndexJoinedChars.of(chars("ab"), chars("cd"), chars("ef"));
    try (Reader reader = chars.asReader()) {
      equal(reader.read(), 'a'); reader.mark(0); equal(reader.skip(3), 3L);
      equal(reader.read(), 'e'); reader.reset(); equal(reader.read(), 'b');
      equal(reader.skip(Long.MAX_VALUE), 4L); equal(reader.read(), -1);
      equal(reader.read(new char[0], 0, 0), 0);
      expect(IllegalArgumentException.class, () -> reader.skip(-1));
    }
    Reader closed = chars.asReader(); closed.close();
    expect(IOException.class, closed::read); equal(chars.toString(), "abcdef");
    MIndexJoinedBytes bytes = MIndexJoinedBytes.of(FrozenBytes.copyOf(new byte[] {0, -1}), FrozenBytes.copyOf(new byte[] {3}));
    try (InputStream input = bytes.asInputStream()) {
      equal(input.read(), 0); input.mark(0); equal(input.read(), 255);
      input.reset(); equal(input.read(), 255); equal(input.skip(-1), 0L);
      equal(input.available(), 1); equal(input.read(), 3); equal(input.read(), -1);
      equal(input.read(new byte[0]), 0);
    }
    InputStream closedBytes = bytes.asInputStream(); closedBytes.close();
    expect(IOException.class, closedBytes::read);
  }

  private static void directViews() {
    ByteBuffer source = ByteBuffer.allocateDirect(8);
    source.putChar('A').putChar('\ud83d').putChar('\ude00').putChar('Z').flip();
    ByteBuffer immutable = source.asReadOnlyBuffer();
    FrozenBytes bytes = FrozenBytes.mapped(immutable, 8, FrozenBytes.hash(immutable));
    FrozenChars chars = FrozenChars.mapped(bytes, FrozenChars.hashUtf16(immutable));
    truth(chars.isDirect()); equal(chars.toString(), "A\ud83d\ude00Z");
    FrozenChars range = chars.subSequence(1, 3);
    equal(range.hash32(), "\ud83d\ude00".hashCode());
    equal(range.directUtf16Bytes().capacity(), 4);
    truth(range.directUtf16Bytes().isReadOnly());
    MIndexJoinedChars view = MIndexJoinedChars.of(chars.subSequence(0, 2), chars.subSequence(2, 4));
    equal(view.codePointAt(1), 0x1f600);
    truth(view.directUtf16Buffers()[0].isDirect());
    equal(view.directUtf16Buffers()[0].capacity(), 4);
    expect(IllegalArgumentException.class, () -> FrozenBytes.mapped(ByteBuffer.allocate(2).asReadOnlyBuffer(), 2, 0));
    expect(IllegalArgumentException.class, () -> FrozenBytes.mapped(source, 8, 0));
  }

  private static void concurrency() throws Exception {
    FrozenChars a = chars("concurrent-left"), b = chars("right");
    MIndexJoinedChars expected = MIndexJoinedChars.of(a, b);
    try (var pool = Executors.newFixedThreadPool(4)) {
      Callable<Boolean> task = () -> {
        for (int i = 0; i < 2000; i++) if (MIndexJoinedChars.of(a, b) != expected) return false;
        return true;
      };
      for (var result : pool.invokeAll(Arrays.asList(task, task, task, task))) truth(result.get());
    }
  }

  private static FrozenChars chars(String text) { return FrozenChars.copyOf(text.toCharArray()); }
  private static int fnv(byte[] bytes) { int h = 0x811c9dc5; for (byte b : bytes) h = (h ^ (b & 255)) * 0x01000193; return h; }
  private static void truth(boolean value) { if (!value) throw new AssertionError("check " + checks); checks++; mix(1); }
  private static void equal(long actual, long expected) {
    if (actual != expected) throw new AssertionError(actual + " != " + expected + " at " + checks);
    checks++; mix(actual);
  }
  private static void equal(String actual, String expected) {
    if (!actual.equals(expected)) throw new AssertionError("text differs at " + checks);
    checks++; mix(actual.length()); for (int i = 0; i < actual.length(); i++) mix(actual.charAt(i));
  }
  private static void mix(long value) { fingerprint = (fingerprint ^ value) * 0x100000001b3L; }
  private static void expect(Class<? extends Throwable> type, Throwing action) {
    try { action.run(); } catch (Throwable error) {
      if (type.isInstance(error)) { checks++; mix(type.getName().hashCode()); return; }
      throw new AssertionError("expected " + type.getName(), error);
    }
    throw new AssertionError("missing " + type.getName());
  }
  @FunctionalInterface private interface Throwing { void run() throws Exception; }
}
