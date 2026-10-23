// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3SegmentedUtf16FsTest {
  @TempDir Path temp;

  @Test
  void persistsParentReferencesAndGathersOnlyAtCompatibilityBoundary() throws Exception {
    String alphaParent = "__alpha__";
    String highSurrogate = new String(new char[] {'\uD83D'});
    String lowSurrogate = new String(new char[] {'\uDE00'});
    String betaParent = "beta!";
    List<CharSequence> parents =
        List.of(alphaParent, highSurrogate, lowSurrogate, betaParent, alphaParent);

    int[] parentIndex = {0, 1, 2, 3, 4};
    int[] parentStart = {2, 0, 0, 0, 2};
    int[] segmentLength = {5, 1, 1, 4, 5};
    String expected = "alpha\uD83D\uDE00betaalpha";

    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("store"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source =
        store.put(
            "segmented-utf16-source".getBytes(StandardCharsets.UTF_8),
            M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "string",
            "segmented-utf16",
            1,
            List.of(source.id()),
            Map.of("segments", Integer.toString(segmentLength.length)));

    new M3FsPrecomputeWriter(store, key, "mindex-segmented-utf16-v1")
        .addSegmentedUtf16(parents, parentIndex, parentStart, segmentLength)
        .publish(cache);

    M3FsPrecomputeBundle bundle = cache.lookupBundle(key).orElseThrow();
    assertEquals(
        4,
        bundle.lanes(M3FsPrecomputeBundle.Role.UTF16_CODE_UNITS_I16).size(),
        "equal parent payloads must share one content-addressed object");
    assertEquals(8, bundle.lanes().size());

    M3FsPrecomputeBundle.SegmentedUtf16String text =
        bundle.openMapped(store, 1 << 20, 4L << 20).segmentedUtf16String();
    assertEquals(4, text.parentCount());
    assertEquals(5, text.segmentCount());
    assertEquals(expected.length(), text.length());
    assertTrue(text.contentEquals(expected));
    for (int index = 0; index < expected.length(); index++) {
      assertEquals(expected.charAt(index), text.charAt(index));
    }

    CharSequence emoji = text.subSequence(5, 7);
    assertEquals(2, emoji.length());
    assertEquals('\uD83D', emoji.charAt(0));
    assertEquals('\uDE00', emoji.charAt(1));
    assertEquals("\uD83D\uDE00", emoji.toString());

    char[] middle = "................".toCharArray();
    text.copyChars(3, middle, 2, 8);
    assertEquals("..ha\uD83D\uDE00beta......", new String(middle));

    assertArrayEquals(expected.toCharArray(), text.toCharArray());
    assertEquals(expected, text.toJavaString());

    M3PrecomputeCache reopened = new M3PrecomputeCache(store);
    M3FsPrecomputeBundle.SegmentedUtf16String reopenedText =
        reopened
            .lookupBundle(key)
            .orElseThrow()
            .openMapped(store, 1 << 20, 4L << 20)
            .segmentedUtf16String();
    assertEquals(expected, reopenedText.toJavaString());
  }

  @Test
  void rejectsSegmentOutsideParentBeforePublishingParentObjects() throws Exception {
    M3DirectoryContentStore store =
        new M3DirectoryContentStore(temp.resolve("invalid-store"));
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "string",
            "segmented-utf16",
            1,
            List.of(),
            Map.of("case", "invalid"));
    M3FsPrecomputeWriter writer =
        new M3FsPrecomputeWriter(store, key, "mindex-segmented-utf16-v1");

    assertThrows(
        IndexOutOfBoundsException.class,
        () ->
            writer.addSegmentedUtf16(
                List.of("abc"),
                new int[] {0},
                new int[] {2},
                new int[] {2}));

    try (var objects = Files.walk(store.root().resolve("objects"))) {
      assertEquals(0L, objects.filter(Files::isRegularFile).count());
    }
  }

  @Test
  void comparesIndexedSegmentsSequentiallyWithExactUtf16Callbacks() throws Exception {
    List<CharSequence> parents =
        List.of("XAa", "\uD83D", "\uDE03", "b\u2003Z");
    int segments = 2048;
    int[] indexes = new int[segments];
    int[] starts = new int[segments];
    int[] sizes = new int[segments];
    StringBuilder oracle = new StringBuilder();
    for (int index = 0; index < segments; index++) {
      int parent = index % parents.size();
      int start = parent == 0 ? 1 : 0;
      int count = index % 19 == 0 ? 0 : (parent == 0 ? 2 : parent == 3 ? 3 : 1);
      indexes[index] = parent;
      starts[index] = start;
      sizes[index] = count;
      oracle.append(parents.get(parent), start, start + count);
    }
    String expected = oracle.toString();

    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("many-segments"));
    M3PrecomputeCache cache = new M3PrecomputeCache(store);
    M3ObjectRef source =
        store.put("run-index".getBytes(StandardCharsets.UTF_8), M3ObjectKind.RAW_BYTES);
    M3PrecomputeKey key =
        new M3PrecomputeKey(
            "string", "segmented-utf16", 1, List.of(source.id()),
            Map.of("case", "many-segments"));
    new M3FsPrecomputeWriter(store, key, "mindex-segmented-utf16-v1")
        .addSegmentedUtf16(parents, indexes, starts, sizes)
        .publish(cache);

    M3FsPrecomputeBundle.SegmentedUtf16String text =
        cache.lookupBundle(key).orElseThrow().openMapped(store, 1 << 20, 4L << 20)
            .segmentedUtf16String();
    assertEquals(segments, text.segmentCount());
    assertEquals(expected.length(), text.length());

    java.util.concurrent.atomic.AtomicInteger reads =
        new java.util.concurrent.atomic.AtomicInteger();
    CharSequence traced = new CharSequence() {
      @Override public int length() { return expected.length(); }
      @Override public char charAt(int index) {
        assertEquals(reads.getAndIncrement(), index, "comparison must visit indices in order");
        return expected.charAt(index);
      }
      @Override public CharSequence subSequence(int start, int end) {
        return expected.subSequence(start, end);
      }
    };
    assertTrue(text.contentEquals(traced));
    assertEquals(expected.length(), reads.get());
    assertTrue(text.contentEquals(expected));
    assertFalse(text.contentEquals(expected + "!"), "different length must refuse");

    char[] mismatch = expected.toCharArray();
    mismatch[mismatch.length - 1] ^= 1;
    assertFalse(text.contentEquals(new String(mismatch)), "last UTF-16 code unit mismatch");
    assertEquals(expected, text.toJavaString());
    assertEquals(expected.substring(7, expected.length() - 11),
        text.subSequence(7, expected.length() - 11).toString());
    java.util.Random random = new java.util.Random(20261020L);
    for (int sample = 0; sample < 2048; sample++) {
      int index = random.nextInt(expected.length());
      assertEquals(expected.charAt(index), text.charAt(index));
    }
  }
}
