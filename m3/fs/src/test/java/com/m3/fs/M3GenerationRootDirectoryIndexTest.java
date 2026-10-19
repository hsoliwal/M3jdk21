// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class M3GenerationRootDirectoryIndexTest {
  @Test
  void directoryPagesPreserveImmediateChildrenAndFileDirectoryDuality() {
    M3ContentId a = id("a");
    M3ContentId ab = id("ab");
    M3ContentId abc = id("abc");
    M3ContentId adot = id("adot");
    M3ContentId a0 = id("a0");
    M3ContentId zfile = id("zfile");
    M3ContentId zeta = id("zeta");

    M3GenerationRoot root = new M3GenerationRoot(
        7L,
        null,
        Map.of(
            "/a", a,
            "/a/b", ab,
            "/a/b/c", abc,
            "/a.", adot,
            "/a0", a0,
            "/z/file", zfile,
            "/zeta", zeta));

    M3GenerationRoot.DirectoryPage first = root.directoryPage("/", null, 2);
    assertEquals(2, first.entries().size());
    assertEquals("a", first.entries().get(0).name());
    assertTrue(first.entries().get(0).isFile());
    assertTrue(first.entries().get(0).isDirectory());
    assertEquals(a, first.entries().get(0).manifestId().orElseThrow());
    assertEquals("a.", first.entries().get(1).name());
    assertEquals("a.", first.nextAfter().orElseThrow());

    M3GenerationRoot.DirectoryPage second =
        root.directoryPage("/", first.nextAfter().orElseThrow(), 2);
    assertEquals("a0", second.entries().get(0).name());
    assertEquals("z", second.entries().get(1).name());
    assertFalse(second.entries().get(1).isFile());
    assertTrue(second.entries().get(1).isDirectory());
    assertEquals("z", second.nextAfter().orElseThrow());

    M3GenerationRoot.DirectoryPage third =
        root.directoryPage("/", second.nextAfter().orElseThrow(), 2);
    assertEquals(1, third.entries().size());
    assertEquals("zeta", third.entries().get(0).name());
    assertTrue(third.nextAfter().isEmpty());

    M3GenerationRoot.DirectoryPage underA = root.directoryPage("/a", null, 8);
    assertEquals(1, underA.entries().size());
    assertEquals("b", underA.entries().get(0).name());
    assertTrue(underA.entries().get(0).isFile());
    assertTrue(underA.entries().get(0).isDirectory());
    assertEquals(ab, underA.entries().get(0).manifestId().orElseThrow());

    assertEquals(ab, root.entry("/a/./b").orElseThrow());
    assertTrue(root.entry("/missing").isEmpty());
  }

  @Test
  void sortedRuntimeIndexDoesNotChangeCanonicalBytesOrContentIdentity() throws Exception {
    HashMap<String, M3ContentId> firstOrder = new HashMap<>();
    firstOrder.put("/z", id("z"));
    firstOrder.put("/a/b", id("ab"));
    firstOrder.put("/a", id("a"));

    HashMap<String, M3ContentId> secondOrder = new HashMap<>();
    secondOrder.put("/a", id("a"));
    secondOrder.put("/z", id("z"));
    secondOrder.put("/a/b", id("ab"));

    M3GenerationRoot first = new M3GenerationRoot(3L, null, firstOrder);
    M3GenerationRoot second = new M3GenerationRoot(3L, null, secondOrder);
    assertArrayEquals(first.canonicalBytes(), second.canonicalBytes());
    assertEquals(first.id(), second.id());

    M3GenerationRoot decoded = M3GenerationRoot.decode(first.canonicalBytes());
    assertArrayEquals(first.canonicalBytes(), decoded.canonicalBytes());
    assertEquals(first.id(), decoded.id());
    assertEquals(first.entries(), decoded.entries());
  }

  @Test
  void pageInputsRemainBoundedAndNormalized() {
    M3GenerationRoot root = new M3GenerationRoot(0L, null, Map.of("/a/b", id("x")));
    assertThrows(IllegalArgumentException.class, () -> root.directoryPage("/", null, 0));
    assertThrows(IllegalArgumentException.class, () -> root.directoryPage("/", null, 65_537));
    assertThrows(IllegalArgumentException.class, () -> root.directoryPage("/", "a/b", 1));
    assertThrows(IllegalArgumentException.class, () -> root.directoryPage("../../escape", null, 1));
  }

  private static M3ContentId id(String value) {
    return M3ContentId.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
}
