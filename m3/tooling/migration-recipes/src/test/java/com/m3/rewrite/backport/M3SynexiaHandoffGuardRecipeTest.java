// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.internal.InMemoryLargeSourceSet;

class M3SynexiaHandoffGuardRecipeTest {
  @Test
  void verifiesPacketAndReplaysExistingTypedReceivers() {
    var guard = new M3SynexiaHandoffGuardRecipe("synexia-fixture-v1");
    assertEquals(
        "dec398e4b1d725d1233aa953e232d444cff3f90bb816ef04fe6cd13c9459306b",
        guard.packetRoot());

    var errors = new ArrayList<Throwable>();
    var context = new InMemoryExecutionContext(errors::add);

    var javaRecipe = new M3Jdk21HashPinnedSnapshotRecipe("synexia-fixture-v1");
    var javaRun =
        javaRecipe.run(new InMemoryLargeSourceSet(List.of()), context).getChangeset().getAllResults();
    assertEquals(1, javaRun.size());
    SourceFile javaAfter = javaRun.getFirst().getAfter();
    assertEquals("m3/ports/bridge-fixture/src/sample/BridgeJava.java", javaAfter.getSourcePath().toString().replace('\\', '/'));
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
  @Test
  void verifiesRealJoinedStreamsPacketAndTypedReceiverFixedPoint() throws Exception {
    String crate = "synexia-mindex-joined-streams-v1";
    String target =
        "m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedStreams.java";
    var guard = new M3SynexiaHandoffGuardRecipe(crate);
    assertEquals(
        "6f65a22ef31dc83bc1946564effea1a938b29301de8390dcaec2c7bd446b14ad",
        guard.packetRoot());
    assertTrue(M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(target));

    String source =
        resource(
            "/com/m3/rewrite/backport/jdk21-hash-pinned/"
                + crate
                + "/0000-MIndexJoinedStreams.java.txt");
    var errors = new ArrayList<Throwable>();
    var context = new InMemoryExecutionContext(errors::add);
    List<SourceFile> parsed =
        JavaParser.fromJavaVersion()
            .build()
            .parseInputs(
                List.of(Parser.Input.fromString(Path.of(target), source)),
                null,
                context)
            .toList();
    assertEquals(1, parsed.size());
    assertEquals(source, parsed.getFirst().printAll());

    var javaRecipe = new M3Jdk21HashPinnedSnapshotRecipe(crate);
    assertTrue(
        javaRecipe
            .run(new InMemoryLargeSourceSet(parsed), context)
            .getChangeset()
            .getAllResults()
            .isEmpty());
    assertTrue(errors.isEmpty(), errors.toString());
  }

  private static String resource(String name) throws IOException {
    try (var input = M3SynexiaHandoffGuardRecipeTest.class.getResourceAsStream(name)) {
      if (input == null) throw new IOException("missing bridge resource: " + name);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

}
