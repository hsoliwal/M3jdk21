// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class M3ScopeFenceTest {
  @Test
  void scopeOrderOnlyBroadensExplicitly() {
    assertTrue(M3EditScope.FILE.canContain(M3EditScope.FILE));
    assertTrue(M3EditScope.PACKAGE.canContain(M3EditScope.FILE));
    assertTrue(M3EditScope.LIBRARY_API.canContain(M3EditScope.MULTI_MODULE));
    assertFalse(M3EditScope.FILE.canContain(M3EditScope.PACKAGE));
    assertEquals(
        M3EditScope.MODULE, M3EditScope.max(M3EditScope.PACKAGE, M3EditScope.MODULE));
    assertEquals(
        M3EditScope.MULTI_MODULE,
        M3EditScope.max(M3EditScope.MULTI_MODULE, M3EditScope.FILE));
    assertThrows(NullPointerException.class, () -> M3EditScope.FILE.canContain(null));
    assertThrows(NullPointerException.class, () -> M3EditScope.max(null, M3EditScope.FILE));
  }

  @Test
  void fileFenceAllowsExactlyOneNormalizedFile() {
    M3ScopeFence fence = M3ScopeFence.file("./m3/tooling/Foo.java");
    assertEquals(M3EditScope.FILE, fence.scope());
    assertEquals(List.of("m3/tooling/Foo.java"), fence.roots());
    assertTrue(fence.allows("m3\\tooling\\Foo.java"));
    assertFalse(fence.allows("m3/tooling/Bar.java"));
    assertFalse(fence.allows("m3/tooling/Foo.java.bak"));
  }

  @Test
  void directoryFenceRespectsSegmentBoundary() {
    M3ScopeFence fence = M3ScopeFence.module("m3/tooling/");
    assertTrue(fence.allows("m3/tooling"));
    assertTrue(fence.allows("m3/tooling/migration-recipes/pom.xml"));
    assertFalse(fence.allows("m3/tooling2/pom.xml"));
    assertFalse(fence.allows("m3/collections/pom.xml"));
  }

  @Test
  void multiModuleAndLibraryFencesAreExplicit() {
    M3ScopeFence multi = M3ScopeFence.multiModule("m3/core", "m3/collections", "m3/tooling");
    assertEquals(M3EditScope.MULTI_MODULE, multi.scope());
    assertTrue(multi.allows("m3/core/src/module-info.java"));
    assertTrue(multi.allows("m3/collections/pom.xml"));
    assertFalse(multi.allows("src/java.base/share/classes/java/lang/String.java"));

    M3ScopeFence library =
        M3ScopeFence.libraryApi(List.of("src/java.base", "src/java.compiler", "m3"));
    assertEquals(M3EditScope.LIBRARY_API, library.scope());
    assertTrue(library.allows("src/java.base/share/classes/java/lang/String.java"));
    assertTrue(library.allows("m3/docs/whole-jdk-m3-architecture.md"));
  }

  @Test
  void malformedOrAmbiguousFencesFailClosed() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new M3ScopeFence(M3EditScope.FILE, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new M3ScopeFence(M3EditScope.FILE, List.of("a.java", "b.java")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new M3ScopeFence(M3EditScope.MULTI_MODULE, List.of("m3/core")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new M3ScopeFence(M3EditScope.MODULE, List.of("m3/core", "m3/core")));
    assertThrows(IllegalArgumentException.class, () -> M3ScopeFence.file(""));
    assertThrows(IllegalArgumentException.class, () -> M3ScopeFence.file("../escape.java"));
    assertThrows(IllegalArgumentException.class, () -> M3ScopeFence.file("a/./b.java"));
    assertThrows(IllegalArgumentException.class, () -> M3ScopeFence.file("/absolute.java"));
    assertThrows(IllegalArgumentException.class, () -> M3ScopeFence.file("C:/absolute.java"));
    assertThrows(IllegalArgumentException.class, () -> M3ScopeFence.file("a//b.java"));
  }

  @Test
  void scopedRecipeRejectsWritesOutsideItsFence() {
    M3ScopedRecipe recipe = () -> M3ScopeFence.file("m3/example/Foo.java");
    assertDoesNotThrow(() -> recipe.requireWritable("m3/example/Foo.java"));
    assertThrows(
        IllegalStateException.class, () -> recipe.requireWritable("m3/example/Other.java"));
  }
}
