// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/** Executable differential oracle; does not require the external Unix4j donor jar. */
public final class Utf8SegmentsProbe {
  private static int checks;
  private static void check(boolean okay, String name) {
    checks++;
    if (!okay) throw new AssertionError(name);
  }
  private static MessageDigest digest() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (Exception failure) { throw new RuntimeException(failure); }
  }
  private static void addNumber(MessageDigest d, long x) {
    for (int shift = 56; shift >= 0; shift -= 8) d.update((byte) (x >>> shift));
  }
  private static void addTextOld(MessageDigest d, String value) {
    byte[] data = value.getBytes(StandardCharsets.UTF_8);
    addNumber(d, data.length);
    d.update(data);
  }
  private static String old(String value) {
    MessageDigest d = digest();
    addTextOld(d, "test-host");
    addTextOld(d, value);
    return HexFormat.of().formatHex(d.digest());
  }
  private static void test(String s) {
    byte[] baseline = s.getBytes(StandardCharsets.UTF_8);
    check(Utf8Segments.length(s) == baseline.length, "length " + s.length());
    MessageDigest a = digest();
    Utf8Segments.update(a, s);
    MessageDigest b = digest();
    b.update(baseline);
    check(Arrays.equals(a.digest(), b.digest()), "digest " + s.length());
    check(new TextLine(s, "\r\n").byteCount() == baseline.length + 2L, "line bytes");
    check(new Canonical("test-host").text(s).finish().equals(old(s)), "legacy receipt");
  }
  private static String randomUnicode(Random r, int n) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < n; i++) {
      int mode = r.nextInt(9);
      int cp = switch (mode) {
        case 0 -> 0;
        case 1 -> '\r';
        case 2 -> '\n';
        case 3 -> r.nextInt(128);
        case 4 -> 0x80 + r.nextInt(1920);
        case 5 -> 0x800 + r.nextInt(0xD800 - 0x800);
        case 6 -> 0xE000 + r.nextInt(0x10000 - 0xE000);
        default -> 0x10000 + r.nextInt(0x10FFFF - 0x10000 + 1);
      };
      b.appendCodePoint(cp);
    }
    return b.toString();
  }
  private static void reject(String s) {
    boolean failed = false;
    try { Utf8Segments.length(s); } catch (IllegalArgumentException expected) { failed = true; }
    check(failed, "invalid length rejected");
    failed = false;
    try { Utf8Segments.update(digest(), s); } catch (IllegalArgumentException expected) { failed = true; }
    check(failed, "invalid streaming rejected");
    failed = false;
    try { new Canonical("test-host").text(s); } catch (IllegalArgumentException expected) { failed = true; }
    check(failed, "invalid canonical rejected");
  }
  public static void main(String[] args) {
    for (String s : new String[]{"", "x", "é", "हिन्दी", "\0", "\r\n", "😀", "a😀x", "𝄞",
        "a".repeat(65537), "𝄞".repeat(17000), "猫".repeat(9000)}) test(s);
    Random r = new Random(20261014L);
    for (int i = 0; i < 20000; i++) test(randomUnicode(r, r.nextInt(50)));
    reject("\ud800"); reject("\udc00"); reject("a\ud800x");
    reject("\ud800\ud800"); reject("\udc00\udc00");
    System.out.println("PASS checks=" + checks);
  }
}
