// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Plain Java 21 actual M3FS mapped-parent regression; no JNI or JUnit dependency.
 * The expected flattened text exists only in this independent test oracle.
 */
public final class M3FsSegmentedCursorSelfTest {
  private static long checks;

  private M3FsSegmentedCursorSelfTest() { }

  private static void check(boolean condition, String description) {
    checks++;
    if (!condition) throw new AssertionError(description + " at check " + checks);
  }

  private static M3FsPrecomputeBundle.SegmentedUtf16String admit(
      M3DirectoryContentStore store, M3PrecomputeCache cache, String id,
      List<? extends CharSequence> parents, int[] indexes, int[] starts, int[] lengths)
      throws Exception {
    M3PrecomputeKey key = new M3PrecomputeKey("string", "segmented-utf16", 1,
        List.of(), Map.of("case", id));
    new M3FsPrecomputeWriter(store, key, "mindex-segmented-utf16-v1")
        .addSegmentedUtf16(parents, indexes, starts, lengths).publish(cache);
    return cache.lookupBundle(key).orElseThrow()
        .openMapped(store, 1 << 20, 4L << 20).segmentedUtf16String();
  }

  private static final class CountingChars implements CharSequence {
    private final String text;
    private final int mismatch;
    int calls;

    CountingChars(String text, int mismatch) {
      this.text = text;
      this.mismatch = mismatch;
    }

    @Override public int length() { return text.length(); }

    @Override public char charAt(int index) {
      check(index == calls, "generic CharSequence visitation order");
      calls++;
      char result = text.charAt(index);
      return index == mismatch ? (char) (result ^ 1) : result;
    }

    @Override public CharSequence subSequence(int start, int end) {
      return text.subSequence(start, end);
    }
  }

  private static void verify(Path root) throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(root);
    M3PrecomputeCache cache = new M3PrecomputeCache(store);

    List<CharSequence> parents = List.of("\uD83D", "\uDE00", "a\u0000Bc", "\u03bbQ", "xy");
    int[] which = new int[307];
    int[] starts = new int[which.length];
    int[] counts = new int[which.length];
    which[0] = 0; counts[0] = 1;
    which[1] = 1; counts[1] = 1;
    StringBuilder expected = new StringBuilder("\uD83D\uDE00");
    Random random = new Random(20261020L);
    for (int segment = 2; segment < which.length; segment++) {
      int parent = random.nextInt(parents.size());
      which[segment] = parent;
      int length = (segment % 7 == 0) ? 0 : (segment % 5 == 0 ? 2 : 1);
      length = Math.min(length, parents.get(parent).length());
      int start = random.nextInt(parents.get(parent).length() - length + 1);
      starts[segment] = start;
      counts[segment] = length;
      for (int unit = 0; unit < length; unit++) {
        expected.append(parents.get(parent).charAt(start + unit));
      }
    }
    String text = expected.toString();
    var first = admit(store, cache, "first", parents, which, starts, counts);
    check(first.segmentCount() == which.length, "all segment coordinates retained");
    check(first.length() == text.length(), "logical text length");
    check(first.contentEquals(text), "compare ordinary String without flattening");
    check(first.contentEquals(new StringBuilder(text)), "compare generic mutable CharSequence");
    char[] copied = new char[text.length()];
    first.copyChars(0, copied, 0, copied.length);
    check(Arrays.equals(copied, text.toCharArray()), "streaming copy parity");
    for (int i = 0; i < text.length(); i++) {
      check(first.charAt(i) == text.charAt(i), "binary-position lookup parity");
    }

    int[] unitIndexes = new int[text.length()];
    int[] unitStarts = new int[text.length()];
    int[] unitLengths = new int[text.length()];
    Arrays.fill(unitLengths, 1);
    for (int i = 0; i < unitStarts.length; i++) unitStarts[i] = i;
    var second = admit(store, cache, "second", List.of(text),
        unitIndexes, unitStarts, unitLengths);
    check(first.contentEquals(second), "two different composite layouts");
    check(second.contentEquals(first), "reverse composite comparison");

    var padded = admit(store, cache, "padded", List.of("!", text, "?"),
        new int[] {0, 1, 2}, new int[] {0, 0, 0},
        new int[] {1, text.length(), 1});
    check(first.contentEquals(padded.subSequence(1, padded.length() - 1)),
        "two-sided comparison through a composite subrange");

    CountingChars equal = new CountingChars(text, -1);
    check(first.contentEquals(equal) && equal.calls == text.length(),
        "generic callback once in ascending order");
    CountingChars early = new CountingChars(text, 0);
    check(!first.contentEquals(early) && early.calls == 1,
        "first mismatched code unit stops without read-ahead");
    CountingChars late = new CountingChars(text, text.length() - 1);
    check(!first.contentEquals(late) && late.calls == text.length(),
        "last mismatched code unit");
    check(!first.contentEquals(text + "x"), "unequal length rejected before scan");
    check(!first.contentEquals(text.substring(0, text.length() - 1)),
        "shorter length rejected before scan");

    String replaced = text.substring(0, text.length() - 1) + "!";
    var changed = admit(store, cache, "changed", List.of(replaced),
        new int[] {0}, new int[] {0}, new int[] {replaced.length()});
    check(!first.contentEquals(changed), "composite mismatch at final segment");
    check(!first.contentEquals(changed.subSequence(0, changed.length() - 1)),
        "range with wrong length");

    var empty = admit(store, cache, "empty", List.of("a", "b"),
        new int[] {0, 1, 0, 1}, new int[] {0, 0, 1, 1},
        new int[] {0, 0, 0, 0});
    check(empty.length() == 0 && empty.contentEquals(""),
        "all-empty segment prefix lanes");
    check(empty.contentEquals(first.subSequence(0, 0)), "empty range equality");
    check(!first.contentEquals(empty), "empty versus populated");
    try {
      first.contentEquals(null);
      throw new AssertionError("null CharSequence accepted");
    } catch (NullPointerException expectedNull) {
      checks++;
    }

    for (int i = 0; i < 512; i++) {
      check(first.contentEquals(second), "repeated zero-flattening segment comparison");
      check(first.contentEquals(padded.subSequence(1, padded.length() - 1)),
          "repeated range comparison");
    }
    System.out.println("M3_FS_SEGMENTED_CURSOR_PASS checks=" + checks
        + " segments=" + first.segmentCount() + " units=" + text.length());
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 0) throw new IllegalArgumentException("no arguments");
    Path temp = Files.createTempDirectory("m3fs-segmented-cursor-");
    verify(temp.resolve("store"));
  }
}
