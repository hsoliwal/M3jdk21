// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

class M3SynexiaHandoffGuardRecipeTest {
  @Test
  void verifiesPacketAndReplaysExistingTypedReceivers() {
    var guard = new M3SynexiaHandoffGuardRecipe("synexia-fixture-v1");
    assertEquals(
        "7964c80982e4ab4d62b1c9f906b6da1060d769fa4aea805dc1f15bea077773fa",
        guard.packetRoot());

    var errors = new ArrayList<Throwable>();
    var context = new InMemoryExecutionContext(errors::add);

    var javaRecipe = new M3Jdk21HashPinnedSnapshotRecipe("synexia-fixture-v1");
    var javaRun =
        javaRecipe.run(new InMemoryLargeSourceSet(List.of()), context).getChangeset().getAllResults();
    assertEquals(1, javaRun.size());
    SourceFile javaAfter = javaRun.getFirst().getAfter();
    assertEquals("m3/bridge-fixture/src/sample/BridgeJava.java", javaAfter.getSourcePath().toString().replace('\\', '/'));
    assertTrue(javaAfter.printAll().contains("return 7"));
    assertTrue(
        javaRecipe
            .run(new InMemoryLargeSourceSet(List.of(javaAfter)), context)
            .getChangeset()
            .getAllResults()
            .isEmpty());

    var textRecipe = new M3Jdk21HashPinnedTextSnapshotRecipe("synexia-fixture-v1");
    var textRun =
        textRecipe.run(new InMemoryLargeSourceSet(List.of()), context).getChangeset().getAllResults();
    assertEquals(1, textRun.size());
    SourceFile textAfter = textRun.getFirst().getAfter();
    assertEquals("m3/bridge-fixture/README.txt", textAfter.getSourcePath().toString().replace('\\', '/'));
    assertEquals("bridge fixture\n", textAfter.printAll());
    assertTrue(
        textRecipe
            .run(new InMemoryLargeSourceSet(List.of(textAfter)), context)
            .getChangeset()
            .getAllResults()
            .isEmpty());

    assertTrue(errors.isEmpty(), errors.toString());
  }
}
