// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3SynexiaHandoffMaterializerTest {
  @TempDir Path temp;

  @Test
  void materializesFixtureThroughExistingReceiversThenReplaysAtFixedPoint() throws Exception {
    Path repo = temp.resolve("repo");
    Files.createDirectories(repo);

    SynexiaHandoffMaterializer.Receipt check =
        SynexiaHandoffMaterializer.inspect(repo, "synexia-fixture-v1");
    assertEquals(2, check.targets());
    assertEquals(2, check.changedTargets());
    assertTrue(Files.notExists(repo.resolve("m3/ports/bridge-fixture/src/sample/BridgeJava.java")));

    SynexiaHandoffMaterializer.Receipt first =
        SynexiaHandoffMaterializer.apply(repo, "synexia-fixture-v1");
    assertEquals(2, first.changedTargets());
    assertTrue(
        Files.readString(
                repo.resolve("m3/ports/bridge-fixture/src/sample/BridgeJava.java"),
                StandardCharsets.UTF_8)
            .contains("return 7"));
    assertEquals(
        "bridge fixture\n",
        Files.readString(repo.resolve("m3/bridge-fixture/README.txt"), StandardCharsets.UTF_8));

    SynexiaHandoffMaterializer.Receipt second =
        SynexiaHandoffMaterializer.apply(repo, "synexia-fixture-v1");
    assertEquals(0, second.changedTargets());
    assertEquals(first.packetRoot(), second.packetRoot());
  }

  @Test
  void materializeCliChecksWithoutWriting() throws Exception {
    Path repo = temp.resolve("cli-repo");
    Files.createDirectories(repo);
    var stdout = new ByteArrayOutputStream();
    var stderr = new ByteArrayOutputStream();
    int status =
        SynexiaHandoffMaterializeCli.run(
            new String[] {"--check", repo.toString(), "synexia-fixture-v1"},
            new PrintStream(stdout, true, StandardCharsets.UTF_8),
            new PrintStream(stderr, true, StandardCharsets.UTF_8));
    assertEquals(0, status, stderr.toString(StandardCharsets.UTF_8));
    assertTrue(
        stdout.toString(StandardCharsets.UTF_8).contains("SYNEXIA_HANDOFF_MATERIALIZE_V1"));
    assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("changed=2"));
    assertTrue(Files.notExists(repo.resolve("m3/ports/bridge-fixture/src/sample/BridgeJava.java")));
  }

  @Test
  void sourceDriftFailsBeforeAnyReceiverWrite() throws Exception {
    Path repo = temp.resolve("drift");
    Files.createDirectories(repo);
    SynexiaHandoffMaterializer.apply(repo, "synexia-fixture-v1");

    Path java = repo.resolve("m3/ports/bridge-fixture/src/sample/BridgeJava.java");
    Path text = repo.resolve("m3/bridge-fixture/README.txt");
    Files.writeString(java, Files.readString(java) + "// drift\n");
    Files.delete(text);

    assertThrows(
        IllegalStateException.class,
        () -> SynexiaHandoffMaterializer.apply(repo, "synexia-fixture-v1"));
    assertTrue(Files.notExists(text), "complete preflight must precede writes");
  }

  @Test
  void realJoinedStreamsCrateIsAlreadyAtReceiverFixedPoint() throws Exception {
    Path repo = temp.resolve("real");
    Path target =
        repo.resolve(
            "m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedStreams.java");
    Files.createDirectories(target.getParent());
    String source =
        resource(
            "/com/m3/rewrite/backport/jdk21-hash-pinned/"
                + "synexia-mindex-joined-streams-v1/0000-MIndexJoinedStreams.java.txt");
    Files.writeString(target, source, StandardCharsets.UTF_8);

    SynexiaHandoffMaterializer.Receipt receipt =
        SynexiaHandoffMaterializer.inspect(repo, "synexia-mindex-joined-streams-v1");
    assertEquals(
        "6f65a22ef31dc83bc1946564effea1a938b29301de8390dcaec2c7bd446b14ad",
        receipt.packetRoot());
    assertEquals(1, receipt.targets());
    assertEquals(0, receipt.changedTargets());
  }

  private static String resource(String name) throws Exception {
    try (var input = M3SynexiaHandoffMaterializerTest.class.getResourceAsStream(name)) {
      if (input == null) throw new IllegalStateException("missing resource: " + name);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
