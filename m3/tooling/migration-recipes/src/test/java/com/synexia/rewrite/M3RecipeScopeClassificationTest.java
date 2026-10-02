// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.InstallIndexStringCompatibility;
import com.m3.rewrite.scope.M3EditScope;
import org.junit.jupiter.api.Test;

class M3RecipeScopeClassificationTest {
  @Test
  void indexStringCompatibilityInstallerIsModuleScoped() {
    var scope = new InstallIndexStringCompatibility().editScope();
    assertEquals(M3EditScope.MODULE, scope.scope());
    assertTrue(scope.allows("m3/ports/indexstring/src/main/java/com/m3/text/compat/M3Text.java"));
    assertFalse(scope.allows("src/java.base/share/classes/java/lang/String.java"));
  }

  @Test
  void retainedJoinedCharsRecipeIsModuleScoped() {
    var scope = new M3MIndexJoinedCharsViewRecipe().editScope();
    assertEquals(M3EditScope.MODULE, scope.scope());
    assertTrue(scope.allows("synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedChars.java"));
    assertFalse(scope.allows("synexia-other/src/main/java/Other.java"));
  }

  @Test
  void hashPinnedSnapshotRecipeIsModuleScoped() {
    var scope = new M3HashPinnedJavaSnapshotRecipe("m3-collection-lanes").editScope();
    assertEquals(M3EditScope.MODULE, scope.scope());
    assertTrue(scope.allows("src/main/java/com/m3/collections/M3Collections.java"));
    assertTrue(scope.allows("src/test/java/com/m3/collections/M3CollectionsTest.java"));
    assertFalse(scope.allows("pom.xml"));
  }

  @Test
  void segmentedLaneNativeInstallerIsExactlyFileScoped() {
    var recipe = new M3SegmentedLaneNativeRecipe();
    var scope = recipe.editScope();
    assertEquals(M3EditScope.FILE, scope.scope());
    assertEquals(M3SegmentedLaneNativeRecipe.TARGET, recipe.targetPath());
    assertTrue(scope.allows(M3SegmentedLaneNativeRecipe.TARGET));
    assertFalse(scope.allows("src/main/native/collections/other.c"));
  }
}
