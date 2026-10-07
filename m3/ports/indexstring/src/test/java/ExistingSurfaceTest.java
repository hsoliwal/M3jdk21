// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/** Same source runs unchanged against the exact baseline and candidate closures. */
public final class ExistingSurfaceTest {
  private static int checks;
  private static final MessageDigest DIGEST;
  static {
    try { DIGEST = MessageDigest.getInstance("SHA-256"); }
    catch (Exception error) { throw new ExceptionInInitializerError(error); }
  }
  private static void check(boolean condition) {
    checks++;
    if (!condition) throw new AssertionError("check " + checks);
  }
  private static void observe(char[] value) {
    for (char unit : value) { DIGEST.update((byte) (unit >>> 8)); DIGEST.update((byte) unit); }
  }
  public static void main(String[] args) throws Exception {
    Random random = new Random(0x4d334a444bL);
    Charset[] charsets = {StandardCharsets.UTF_8, StandardCharsets.UTF_16BE,
        StandardCharsets.UTF_16LE, StandardCharsets.ISO_8859_1, Charset.forName("ISO-2022-JP")};
    for (int iteration = 0; iteration < 5000; iteration++) {
      char[] units = new char[random.nextInt(80)];
      for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
      int cut = random.nextInt(units.length + 1);
      FrozenChars left = FrozenChars.copyOf(Arrays.copyOfRange(units, 0, cut));
      FrozenChars right = FrozenChars.copyOf(Arrays.copyOfRange(units, cut, units.length));
      MIndexJoinedChars joined = MIndexJoinedChars.of(left, FrozenChars.copyOf(new char[0]), right);
      String ordinary = new String(units);
      check(joined.length() == ordinary.length());
      check(ordinary.equals(joined.toString()));
      check(Arrays.equals(units, joined.copy()));
      check(joined.codePointCount() == ordinary.codePointCount(0, ordinary.length()));
      check(joined == MIndexJoinedChars.of(left, right));
      for (int i = 0; i < units.length; i++) {
        check(joined.charAt(i) == units[i]);
        check(joined.codePointAt(i) == ordinary.codePointAt(i));
      }
      int start = random.nextInt(units.length + 1);
      int end = start + random.nextInt(units.length - start + 1);
      MIndexJoinedChars slice = joined.subSequence(start, end);
      check(slice.toString().equals(ordinary.substring(start, end)));
      check(slice.sharesBackingWith(joined));
      char[] copied = slice.copy(); observe(copied);
      if (copied.length > 0) { copied[0] ^= 1; check(slice.charAt(0) == units[start]); }
      try (Reader reader = slice.asReader()) {
        reader.mark(0);
        char[] destination = new char[slice.length() + 3];
        int count = reader.read(destination, 1, slice.length());
        check(count == slice.length());
        check(Arrays.equals(Arrays.copyOfRange(destination, 1, slice.length() + 1), slice.copy()));
        check(reader.read() == -1);
        reader.reset();
        check(reader.skip(slice.length()) == slice.length());
        check(reader.read() == -1);
      }
      for (Charset charset : charsets) {
        check(Arrays.equals(joined.encode(charset).copy(), ordinary.getBytes(charset)));
      }
      byte[] bytes = new byte[random.nextInt(40)]; random.nextBytes(bytes);
      int half = bytes.length / 2;
      MIndexJoinedBytes binary = MIndexJoinedBytes.of(
          FrozenBytes.copyOf(Arrays.copyOfRange(bytes, 0, half)),
          FrozenBytes.copyOf(Arrays.copyOfRange(bytes, half, bytes.length)));
      check(Arrays.equals(bytes, binary.copy()));
      check(Arrays.equals(bytes, binary.asInputStream().readAllBytes()));
      for (ByteBuffer buffer : binary.asReadOnlyBuffers()) check(buffer.isReadOnly());
    }
    char[] all = new char[65536];
    for (int i = 0; i < all.length; i++) all[i] = (char) i;
    FrozenChars frozen = FrozenChars.copyOf(all);
    check(frozen.hash32() == new String(all).hashCode());
    check(frozen.asReadOnlyBuffer().isReadOnly());
    char[] mutated = all.clone(); FrozenChars snapshot = FrozenChars.copyOf(mutated);
    Arrays.fill(mutated, 'x'); check(Arrays.equals(snapshot.copy(), all));
    observe(frozen.copy());
    System.out.println("EXISTING_SURFACE_PASS checks=" + checks + " digest=" + HexFormat.of().formatHex(DIGEST.digest()));
  }
}
