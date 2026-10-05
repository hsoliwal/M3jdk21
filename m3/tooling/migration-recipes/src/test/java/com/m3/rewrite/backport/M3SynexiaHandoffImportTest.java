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
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3SynexiaHandoffImportTest {
  @TempDir Path temp;

  @Test
  void importsExternalCrateThenReplaysAtFixedPoint() throws Exception {
    Path export = fixture("good");
    Path target = temp.resolve("target");
    Files.createDirectories(target);

    SynexiaHandoffPacket.Verified verified =
        SynexiaHandoffPacket.verify(export, "synexia-sample-v1");
    assertEquals(2, verified.rows());

    SynexiaHandoffImport.Receipt first =
        SynexiaHandoffImport.apply(export, target, "synexia-sample-v1");
    assertEquals(7, first.resourceFiles());
    assertEquals(7, first.changedFiles());
    assertEquals(verified.packetRoot(), first.packetRoot());

    SynexiaHandoffImport.Receipt second =
        SynexiaHandoffImport.apply(export, target, "synexia-sample-v1");
    assertEquals(7, second.resourceFiles());
    assertEquals(0, second.changedFiles());
    assertEquals(first.packetRoot(), second.packetRoot());

    var stdout = new ByteArrayOutputStream();
    var stderr = new ByteArrayOutputStream();
    int status =
        SynexiaHandoffImportCli.run(
            new String[] {
              "--check", export.toString(), target.toString(), "synexia-sample-v1"
            },
            new PrintStream(stdout, true, StandardCharsets.UTF_8),
            new PrintStream(stderr, true, StandardCharsets.UTF_8));
    assertEquals(0, status, stderr.toString(StandardCharsets.UTF_8));
    String receipt = stdout.toString(StandardCharsets.UTF_8);
    assertTrue(receipt.contains("SYNEXIA_HANDOFF_IMPORT_V1"));
    assertTrue(receipt.contains("mode=check"));
    assertTrue(receipt.contains("changed=0"));
  }

  @Test
  void refusesUnexpectedFilesAliasDriftAndDestinationDriftBeforeWrites() throws Exception {
    Path export = fixture("refusal-extra");
    Files.writeString(export.resolve("unexpected.txt"), "extra\n");
    Path target = temp.resolve("target-extra");
    Files.createDirectories(target);
    assertThrows(
        IllegalStateException.class,
        () -> SynexiaHandoffImport.apply(export, target, "synexia-sample-v1"));

    export = fixture("refusal-alias");
    Path alias = resources(export).resolve("META-INF/rewrite/m3-synexia-sample-v1.yml");
    Files.writeString(alias, Files.readString(alias) + "# drift\n");
    Path aliasTarget = temp.resolve("target-alias");
    Files.createDirectories(aliasTarget);
    assertThrows(
        IllegalStateException.class,
        () -> SynexiaHandoffImport.apply(export, aliasTarget, "synexia-sample-v1"));

    export = fixture("refusal-target");
    Path driftTarget = temp.resolve("target-drift");
    Path destinationPacket =
        driftTarget.resolve(
            "m3/tooling/migration-recipes/src/main/resources/"
                + "com/m3/rewrite/backport/synexia-bridge/synexia-sample-v1/packet.tsv");
    Files.createDirectories(destinationPacket.getParent());
    Files.writeString(destinationPacket, "drift\n");
    Path javaManifest =
        driftTarget.resolve(
            "m3/tooling/migration-recipes/src/main/resources/"
                + "com/m3/rewrite/backport/jdk21-hash-pinned/synexia-sample-v1/manifest.tsv");
    assertThrows(
        IllegalStateException.class,
        () -> SynexiaHandoffImport.apply(export, driftTarget, "synexia-sample-v1"));
    assertTrue(Files.notExists(javaManifest), "preflight must precede every write");
  }

  @Test
  void refusesTextPayloadTargetingJavaEvenWithAValidPacketRoot() throws Exception {
    Path export = fixture("text-as-java");
    Path bridge =
        resources(export)
            .resolve("com/m3/rewrite/backport/synexia-bridge/synexia-sample-v1");
    Path packet = bridge.resolve("packet.tsv");
    String changed =
        Files.readString(packet)
            .replace("m3/ports/sample/README.txt", "m3/ports/sample/README.java");
    Files.writeString(packet, changed);
    Path properties = bridge.resolve("bridge.properties");
    String propertyText =
        Files.readString(properties)
            .replaceFirst("packetRoot=[0-9a-f]{64}", "packetRoot=" + sha256(changed));
    Files.writeString(properties, propertyText);

    Path target = temp.resolve("target-text-as-java");
    Files.createDirectories(target);
    assertThrows(
        IllegalStateException.class,
        () -> SynexiaHandoffImport.inspect(export, target, "synexia-sample-v1"));
  }

  @Test
  void refusesSymlinkedExportResourceWhenSupported() throws Exception {
    Path export = fixture("refusal-symlink");
    Path packet =
        resources(export)
            .resolve(
                "com/m3/rewrite/backport/synexia-bridge/synexia-sample-v1/packet.tsv");
    Path real = packet.resolveSibling("packet.real");
    Files.move(packet, real);
    try {
      Files.createSymbolicLink(packet, real.getFileName());
    } catch (UnsupportedOperationException | java.io.IOException unavailable) {
      return;
    }
    Path target = temp.resolve("target-symlink");
    Files.createDirectories(target);
    assertThrows(
        IllegalStateException.class,
        () -> SynexiaHandoffImport.apply(export, target, "synexia-sample-v1"));
  }

  @Test
  void cliRefusesInvalidArguments() {
    int status =
        SynexiaHandoffImportCli.run(
            new String[] {"--check"},
            new PrintStream(new ByteArrayOutputStream()),
            new PrintStream(new ByteArrayOutputStream()));
    assertEquals(2, status);
  }

  private Path fixture(String name) throws Exception {
    Path export = temp.resolve(name);
    Path resources = resources(export);
    String crate = "synexia-sample-v1";
    String revision = "0123456789abcdef0123456789abcdef01234567";
    String javaPayload = "package sample; final class A {}\n";
    String textPayload = "bridge fixture\n";
    String javaSha = sha256(javaPayload);
    String textSha = sha256(textPayload);
    String contract = "1".repeat(64);
    String gate = "2".repeat(64);

    String javaOwner =
        "com/m3/rewrite/backport/jdk21-hash-pinned/" + crate + "/";
    String textOwner =
        "com/m3/rewrite/backport/jdk21-hash-pinned-text/" + crate + "/";
    String bridgeOwner =
        "com/m3/rewrite/backport/synexia-bridge/" + crate + "/";

    Files.createDirectories(resources.resolve(javaOwner));
    Files.createDirectories(resources.resolve(textOwner));
    Files.createDirectories(resources.resolve(bridgeOwner));
    Files.createDirectories(resources.resolve("META-INF/rewrite"));

    Files.writeString(resources.resolve(javaOwner + "0000-A.java.txt"), javaPayload);
    Files.writeString(resources.resolve(textOwner + "0001-README.txt.txt"), textPayload);
    Files.writeString(
        resources.resolve(javaOwner + "manifest.tsv"),
        "m3/ports/sample/src/main/java/sample/A.java\tABSENT\t"
            + javaSha
            + "\t0000-A.java.txt\n");
    Files.writeString(
        resources.resolve(textOwner + "manifest.tsv"),
        "m3/ports/sample/README.txt\tABSENT\t"
            + textSha
            + "\t0001-README.txt.txt\n");

    String packet =
        "# SYNEXIA_M3_HANDOFF_V1\n"
            + "@crate\t"
            + crate
            + "\n"
            + "@source-revision\t"
            + revision
            + "\n"
            + "ROW\tJAVA\tcap.java\tsrc/A.java\t"
            + javaSha
            + "\tm3/ports/sample/src/main/java/sample/A.java\tABSENT\t"
            + javaSha
            + "\t0000-A.java.txt\tApache-2.0\trecipe.java\t"
            + contract
            + "\t"
            + gate
            + "\n"
            + "ROW\tTEXT\tcap.docs\tREADME.txt\t"
            + textSha
            + "\tm3/ports/sample/README.txt\tABSENT\t"
            + textSha
            + "\t0001-README.txt.txt\tApache-2.0\trecipe.docs\t"
            + contract
            + "\t"
            + gate
            + "\n";
    Files.writeString(resources.resolve(bridgeOwner + "packet.tsv"), packet);
    Files.writeString(
        resources.resolve(bridgeOwner + "bridge.properties"),
        "version=SYNEXIA_M3_HANDOFF_V1\n"
            + "crate="
            + crate
            + "\n"
            + "sourceRevision="
            + revision
            + "\n"
            + "packetRoot="
            + sha256(packet)
            + "\n"
            + "rows=2\n"
            + "payloadBytes="
            + (javaPayload.getBytes(StandardCharsets.UTF_8).length
                + textPayload.getBytes(StandardCharsets.UTF_8).length)
            + "\n");
    Files.writeString(
        resources.resolve("META-INF/rewrite/m3-" + crate + ".yml"),
        "# SPDX-License-Identifier: Apache-2.0\n"
            + "---\n"
            + "type: specs.openrewrite.org/v1beta/recipe\n"
            + "name: com.m3.rewrite.backport.SynexiaBridgeSampleV1\n"
            + "displayName: Receive reviewed Synexia handoff synexia-sample-v1\n"
            + "description: Replays exact target-ready Synexia bytes through existing M3JDK21 hash-pinned owners.\n"
            + "tags:\n"
            + "  - m3\n"
            + "  - synexia\n"
            + "  - jdk21\n"
            + "  - hash-pinned\n"
            + "  - candidate-only\n"
            + "recipeList:\n"
            + "  - com.m3.rewrite.backport.M3SynexiaHandoffGuardRecipe:\n"
            + "      crateName: synexia-sample-v1\n"
            + "  - com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe:\n"
            + "      crateName: synexia-sample-v1\n"
            + "  - com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe:\n"
            + "      crateName: synexia-sample-v1\n");
    return export;
  }

  private static Path resources(Path export) {
    return export.resolve("m3/tooling/migration-recipes/src/main/resources");
  }

  private static String sha256(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
