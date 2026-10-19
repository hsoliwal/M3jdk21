// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.util.Arrays;

/** Test-only producer for literal/anchored/dot NFAs in the donor image contract, not a runtime compiler. */
final class M3TestRegexImages {
  private M3TestRegexImages() {}
  static M3FsRegexImage compile(String expression) {
    boolean begin = expression.startsWith("^");
    boolean end = expression.endsWith("$");
    String text = expression.substring(begin ? 1 : 0, expression.length() - (end ? 1 : 0));
    boolean dot = text.equals(".");
    if (!dot && !text.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == ' ')) {
      throw new IllegalArgumentException("test fixture supports literal, anchors and dot only");
    }
    int chars = text.length(), count = chars + 1 + (begin ? 1 : 0) + (end ? 1 : 0);
    byte[] op = new byte[count];
    int[] out1 = new int[count], out2 = new int[count], classes = new int[count];
    Arrays.fill(out1, -1); Arrays.fill(out2, -1); Arrays.fill(classes, -1);
    long[] low = new long[chars], high = new long[chars];
    int[] rangeOffsets = new int[chars], rangeCounts = new int[chars];
    char[] rangeLow = dot ? new char[] {128, 134, 8234} : new char[0];
    char[] rangeHigh = dot ? new char[] {132, 8231, 65535} : new char[0];
    int at = 0;
    if (begin) { op[at] = 4; out1[at] = at + 1; at++; }
    for (int i = 0; i < chars; i++, at++) {
      op[at] = 1; out1[at] = at + 1; classes[at] = i;
      int c = text.charAt(i);
      if (dot) {
        low[i] = -1L & ~(1L << '\n') & ~(1L << '\r');
        high[i] = -1L;
        rangeCounts[i] = 3;
      } else if (c < 64) low[i] = 1L << c;
      else if (c < 128) high[i] = 1L << (c - 64);
      else throw new IllegalArgumentException("non-ASCII literal fixture requires explicit ranges");
    }
    if (end) { op[at] = 5; out1[at] = at + 1; at++; }
    op[at] = 6;
    return M3FsRegexImage.fromImage(new M3FsRegexImage.Image(expression, M3FsRegexImage.Semantics.JDK,
        op, out1, out2, classes, low, high, rangeOffsets, rangeCounts, rangeLow, rangeHigh,
        0, at, chars, chars, 0, dot ? 0 : signal(text), 0, dot));
  }

  static long signal(CharSequence value) {
    long result = 0;
    for (int i = 0; i < value.length(); i++) {
      int c = value.charAt(i); if (c >= 'A' && c <= 'Z') c |= 32;
      result |= 1L << (mix(c) & 63);
      result |= 1L << (mix(c ^ 0x9e3779b9) & 63);
    }
    return result;
  }
  private static int mix(int x) {
    x ^= x >>> 16; x *= 0x7feb352d; x ^= x >>> 15; x *= 0x846ca68b; return x ^ (x >>> 16);
  }
}
