// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Fail-closed OpenJDK source atomization, real JVM proof and post-pattern recheck. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-maven-plugin/src/test/java/com/synexia/ai/m3scale/M3OpenJdkOrderedPassesTest.java blob f809b7cd6baa; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
final class M3OpenJdkOrderedPassesTest {
    private static final String PATH = "src/java.base/share/classes/java/lang/M3Synthetic.java";
    private static final Path RULES = Path.of(System.getProperty("m3.atomjit.patternRules",
            "../atom-census/PATTERN_RULES.tsv"));
    private static final byte[] SOURCE = ("package java.lang; public final class M3Synthetic {"
            + " public static int mixed(int x) { return (x << 2) ^ (x >>> 1); }"
            + " public static long bits(long x) { return x * 31L; } }")
            .getBytes(StandardCharsets.UTF_8);

    @Test
    void jdkSourceUsesCanonicalAtomizerAndSourceIdentity() throws IOException {
        String input = System.getProperty("m3.openjdk.integer.source");
        if (Boolean.getBoolean("m3.openjdk.source.required") && (input == null || input.isBlank())) {
            throw new AssertionError("real OpenJDK Integer.java source is mandatory");
        }
        byte[] bytes = input == null || input.isBlank() ? SOURCE : Files.readAllBytes(Path.of(input));
        String logical = input == null || input.isBlank() ? PATH
                : "src/java.base/share/classes/java/lang/Integer.java";
        var atomized = M3OpenJdkOrderedPasses.atomize(logical, bytes, RULES);
        assertEquals(logical, atomized.path());
        assertEquals(M3OpenJdkOrderedPasses.sha256(bytes), atomized.sourceSha256());
        assertTrue(atomized.atomCount() > 0);
    }

    @Test
    void mustJitCheckAfterAtomizeBeforePatternReviewAndThenRetest()
            throws IOException, InterruptedException, URISyntaxException {
        var atomized = M3OpenJdkOrderedPasses.atomize(PATH, SOURCE, RULES);
        Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        String classes = Path.of(M3OpenJdkOrderedPasses.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toString();
        var jit = M3OpenJdkOrderedPasses.testJit(atomized, SOURCE, java, classes);
        assertTrue(jit.digest().matches("[0-9a-f]{1,16}"));
        var reviewed = M3OpenJdkOrderedPasses.patternize(jit);
        assertEquals(atomized.atomCount(), reviewed.patternEntries());
        assertFalse(reviewed.mutationAuthority());
        assertFalse(reviewed.promotionAuthority());
        var retested = M3OpenJdkOrderedPasses.retest(reviewed, SOURCE);
        assertEquals(atomized.atomCount(), retested.checkedAtoms());
        assertEquals(atomized.sourceSha256(), retested.sourceSha256());
        assertTrue(retested.patternRoot().matches("[0-9a-f]{64}"));
        assertFalse(retested.promotionAuthority());
    }

    @Test
    void inputDriftAndPrematurePatternReviewAreRefused() {
        var atomized = M3OpenJdkOrderedPasses.atomize(PATH, SOURCE, RULES);
        assertThrows(NullPointerException.class,
                () -> M3OpenJdkOrderedPasses.patternize(null));
        assertThrows(IllegalStateException.class,
                () -> M3OpenJdkOrderedPasses.testJit(atomized,
                        "modified".getBytes(StandardCharsets.UTF_8),
                        Path.of(System.getProperty("java.home"), "bin", "java"), "ignored"));
    }

    @Test
    void unsafeOpenJdkCoordinatesAndBudgetsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkOrderedPasses.atomize("src/java.base/share/classes/../Leak.java", SOURCE, RULES));
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkOrderedPasses.atomize("test/jdk/NotOpenJdk.java", SOURCE, RULES));
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkOrderedPasses.atomize(PATH, new byte[0], RULES));
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkOrderedPasses.atomize(PATH, new byte[16 * 1024 * 1024 + 1], RULES));
        assertNotEquals(M3OpenJdkOrderedPasses.sha256(SOURCE), M3OpenJdkOrderedPasses.sha256("changed".getBytes(StandardCharsets.UTF_8)));
    }
}
