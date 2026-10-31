// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class M3RecipeMemoryLedgerTest {
  @Test
  void validatesAndRefusesHistoryRewrite() {
    String first =
        M3RecipeMemoryLedger.HEADER
            + "\n1\tcom.m3.rewrite.backport.M3MopStreamTraversalRecipe\tuser\tmissing monitored universe traversal\tPENDING\tM3MopStreamTraversalRecipeTest\treuse one-pass events for byte progress\tstream-traversal\tM3JDK21-existing-owners\tRECORDED\n";
    List<M3RecipeMemoryLedger.Entry> oldEntries = M3RecipeMemoryLedger.parse(first);
    assertEquals(1, oldEntries.size());

    String second =
        first
            + "2\tcom.m3.rewrite.backport.M3MopStreamTraversalRecipe\ttest\tfixture passed\t0123456789012345678901234567890123456789\tM3MopStreamTraversalRecipeTest\tkeep payload reads single-pass\tstream-traversal\tM3JDK21-existing-owners\tVALIDATED\n";
    List<M3RecipeMemoryLedger.Entry> newEntries = M3RecipeMemoryLedger.parse(second);
    M3RecipeMemoryLedger.verifyAppendOnly(oldEntries, newEntries);

    String rewritten = second.replace("reuse one-pass events", "rewrite old lesson");
    assertThrows(
        IllegalArgumentException.class,
        () -> M3RecipeMemoryLedger.verifyAppendOnly(
            newEntries, M3RecipeMemoryLedger.parse(rewritten)));
  }
}
