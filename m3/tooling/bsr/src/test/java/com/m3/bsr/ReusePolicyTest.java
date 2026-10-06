// SPDX-License-Identifier: Apache-2.0
package com.m3.bsr;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** Candidate-only successor; never writes a canonical repository document. */
final class ReusePolicyTest {
    private static final String RECIPE = "m3-product-reuse";
    private static final Path CRATE = Path.of(System.getProperty("m3.crate"));
    private static final Path ROOT = Path.of(System.getProperty("m3.root")).normalize();
    private static final Path OWNER = Path.of("m3/docs/M3JDK21_PORTING_INVARIANT.md");
    private static final String RESOURCE = "com/m3/rewrite/backport/jdk21-hash-pinned-text/";
    private static final Path RES = CRATE.resolve("src/main/resources/" + RESOURCE + RECIPE);

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(e -> { throw new IllegalStateException(e); });
    }
    private static List<Result> changes(List<SourceFile> input) {
        return new M3Jdk21HashPinnedTextSnapshotRecipe(RECIPE)
                .run(new InMemoryLargeSourceSet(input), context(), 1)
                .getChangeset().getAllResults();
    }
    private static SourceFile before() throws Exception {
        return PlainText.builder().sourcePath(OWNER)
                .text(Files.readString(RES.resolve("policy.md.txt.before"))).build();
    }
    private static String digest(String algorithm, byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
    }
    private static String blob(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        MessageDigest hash = MessageDigest.getInstance("SHA-1");
        hash.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash.digest(bytes));
    }
    @Test void preimageManifestAndExistingToolPins() throws Exception {
        String[] row = Files.readAllLines(RES.resolve("manifest.tsv")).get(1).split("\t", -1);
        assertEquals(4, row.length);
        assertEquals(OWNER.toString().replace('\\', '/'), row[0]);
        assertEquals("policy.md.txt", row[3]);
        assertEquals(row[1], digest("SHA-256", Files.readAllBytes(RES.resolve("policy.md.txt.before"))));
        assertEquals(row[2], digest("SHA-256", Files.readAllBytes(RES.resolve("policy.md.txt"))));
        assertEquals("83b223703880997bfbec8083d6c36f187a6063c3", blob(RES.resolve("policy.md.txt.before")));
        assertEquals("a2e8c916208b563517e51b738744a05357e633aa", blob(ROOT.resolve("m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java")));
        assertEquals("fcac75cfd8c041f302217de206cf40f0f27e52da", blob(CRATE.resolve("pom.xml")));
        String current = digest("SHA-256", Files.readAllBytes(ROOT.resolve(OWNER)));
        assertTrue(current.equals(row[1]) || current.equals(row[2]), "canonical owner drift");
    }
    @Test void generationAndFixedPoint() throws Exception {
        List<Result> result = changes(List.of(before()));
        assertEquals(1, result.size());
        SourceFile after = result.getFirst().getAfter();
        assertNotNull(after);
        assertEquals(OWNER, after.getSourcePath());
        assertEquals(Files.readString(RES.resolve("policy.md.txt")), after.printAll());
        assertTrue(changes(List.of(after)).isEmpty(), "second pass must be a fixed point");
        Path candidate = CRATE.resolve("target/generated-reuse").resolve(OWNER);
        Files.createDirectories(candidate.getParent());
        Files.writeString(candidate, after.printAll());
    }
    @Test void sourceDriftRefused() throws Exception {
        SourceFile drift = PlainText.builder().sourcePath(OWNER)
                .text(before().printAll() + "drift").build();
        assertThrows(RuntimeException.class, () -> changes(List.of(drift)));
    }
    @Test void missingAndDuplicateRefused() throws Exception {
        SourceFile original = before();
        assertThrows(RuntimeException.class, () -> changes(List.of()));
        assertThrows(RuntimeException.class, () -> changes(List.of(original, original)));
    }
    @Test void unrelatedSourcePreserved() throws Exception {
        SourceFile other = PlainText.builder().sourcePath(Path.of("unrelated.md"))
                .text("do not change\n").build();
        List<Result> result = changes(List.of(before(), other));
        assertEquals(1, result.size());
        assertNotNull(result.getFirst().getAfter());
        assertEquals(OWNER, result.getFirst().getAfter().getSourcePath());
        assertEquals("do not change\n", other.printAll());
        assertTrue(changes(List.of(result.getFirst().getAfter(), other)).isEmpty());
    }
    @Test void templateTamperRefused() throws Exception {
        String badName = RECIPE + "-tamper";
        Path bad = CRATE.resolve("target/test-classes/" + RESOURCE + badName);
        Files.createDirectories(bad);
        Files.writeString(bad.resolve("manifest.tsv"), Files.readString(RES.resolve("manifest.tsv")));
        Files.writeString(bad.resolve("policy.md.txt"), Files.readString(RES.resolve("policy.md.txt")) + "drift");
        assertThrows(IllegalStateException.class, () ->
                new M3Jdk21HashPinnedTextSnapshotRecipe(badName).getInitialValue(context()));
    }
    @Test void fullContributionAndLicenceBoundariesPresent() throws Exception {
        String policy = Files.readString(RES.resolve("policy.md.txt"));
        for (String required : List.of("all product targets", "Maven/OpenRewrite recipes",
                "Recipe implementations are reusable code", "Apache-2.0",
                "does not relicense third-party or OpenJDK material", "GPLv2-only",
                "name-mapping.json", "NOTICE", "proof receipts")) {
            assertTrue(policy.contains(required), "missing policy requirement: " + required);
        }
    }
}
