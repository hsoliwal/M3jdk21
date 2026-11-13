// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3jdk21-openjdk-atom-pattern-20261031/src/test/java/com/synexia/m3/openjdk/M3OpenJdkAtomPatternGateTest.java blob dd180cbfc945; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
final class OpenJdkAtomPatternGateTest {
    private static final String PRODUCT = "hsoliwal/M3jdk21";
    private static final String BLOB = "a".repeat(40);
    private static final String HEADER = String.join("\t", "path", "git_blob_sha1",
            "mode", "file_kind", "owner_family", "phase", "candidate_category",
            "challenge_id", "technique_hint", "source_evidence", "source_sha256",
            "api_abi_jni_gate", "method_level_recipe_gate", "target_runtime_gate", "status");

    @Test
    void openJdkJavaLangUsesRealJdkImageAndHotspotTierGates() {
        var order = OpenJdkAtomPatternGate.plan(PRODUCT,
                "src/java.base/share/classes/java/lang/String.java", BLOB, "JAVA");
        assertEquals(OpenJdkAtomPatternGate.Lane.JDK_BOOTSTRAP, order.lane());
        assertEquals("M3AtomizeRecipe", order.atomizer());
        assertEquals("M3PatternizeRecipe", order.patternizer());
        assertTrue(order.prePatternProof().contains("JTREG_XINT_C1_C2"));
        assertTrue(order.postPatternProof().contains("JDK_IMAGE_JTREG"));
    }

    @Test
    void openJdkJavaUtilAndCompilerModulesRetainJmodAndJtreg() {
        var map = OpenJdkAtomPatternGate.plan(PRODUCT,
                "src/java.base/share/classes/java/util/HashMap.java", BLOB, "JAVA");
        var compiler = OpenJdkAtomPatternGate.plan(PRODUCT,
                "src/jdk.compiler/share/classes/com/sun/tools/javac/Main.java", BLOB, "JAVA");
        assertEquals(OpenJdkAtomPatternGate.Lane.JDK_MODULE, map.lane());
        assertEquals(OpenJdkAtomPatternGate.Lane.JDK_MODULE, compiler.lane());
        assertTrue(map.prePatternProof().contains("JMOD_JTREG"));
        assertEquals(map.prePatternProof(), compiler.prePatternProof());
    }

    @Test
    void nativeHotspotCannotBeMisreportedAsJavaJitAtom() {
        var cpp = OpenJdkAtomPatternGate.plan(PRODUCT,
                "src/hotspot/share/opto/compile.cpp", BLOB, "NATIVE_CPP");
        assertEquals(OpenJdkAtomPatternGate.Lane.HOTSPOT_NATIVE, cpp.lane());
        assertEquals("M3HashPinnedTextSnapshotRecipe", cpp.atomizer());
        assertTrue(cpp.prePatternProof().contains("C17_CPP20_ABI_JNI"));
        assertTrue(cpp.prePatternProof().contains("JAVA_CALLSITE_JIT"));
    }

    @Test
    void javaBaseCAndM3JniUseRealNativeParityGate() {
        var jdk = OpenJdkAtomPatternGate.plan(PRODUCT,
                "src/java.base/share/native/libjava/Array.c", BLOB, "NATIVE_C");
        var m3 = OpenJdkAtomPatternGate.plan(PRODUCT,
                "m3/arrays/src/main/native/m3_arrays_jni.c", BLOB, "NATIVE_C");
        assertEquals(OpenJdkAtomPatternGate.Lane.JDK_NATIVE, jdk.lane());
        assertEquals(OpenJdkAtomPatternGate.Lane.M3_NATIVE, m3.lane());
        assertEquals("NATIVE_ABI_JNI_GC_CALLSITE_PARITY", jdk.postPatternProof());
    }

    @Test
    void ordinaryM3AndAnyOtherJavaRepositoryUseSameCanonicalRecipes() {
        var m3 = OpenJdkAtomPatternGate.plan(PRODUCT,
                "m3/collections/src/main/java/jdk/m3/collections/LongHashSet.java",
                BLOB, "JAVA");
        var donor = OpenJdkAtomPatternGate.plan("hsoliwal/guava",
                "guava/src/jdk/m3/collections/guava/ImmutableList.java", BLOB, "JAVA");
        assertEquals(OpenJdkAtomPatternGate.Lane.M3_JAVA, m3.lane());
        assertEquals(OpenJdkAtomPatternGate.Lane.OTHER_JAVA, donor.lane());
        assertEquals(m3.atomizer(), donor.atomizer());
        assertEquals(m3.patternizer(), donor.patternizer());
        assertTrue(donor.prePatternProof().contains("JAVA21_JUNIT"));
    }

    @Test
    void testsAndGeneratorsAreNeverSilentlyTreatedAsProductAtoms() {
        var oracle = OpenJdkAtomPatternGate.plan(PRODUCT,
                "test/jdk/java/lang/String/Test.java", BLOB, "JAVA");
        var generator = OpenJdkAtomPatternGate.plan(PRODUCT,
                "make/gensrc/generate.py", BLOB, "SCRIPT");
        assertEquals(OpenJdkAtomPatternGate.Lane.TEST_OR_ORACLE, oracle.lane());
        assertEquals(OpenJdkAtomPatternGate.Lane.GENERATOR_OR_DONOR, generator.lane());
        assertEquals("REVIEW_ONLY", oracle.atomizer());
        assertEquals("NO_PRODUCTION_ATOM", generator.prePatternProof());
    }

    @Test
    void allLanesHoldUntilActualAtomJunitJitAndPatternizerEvidence() {
        for (String path : new String[] {
                "src/java.base/share/classes/java/lang/String.java",
                "src/java.base/share/classes/java/util/HashMap.java",
                "m3/collections/src/main/java/M3HashSet.java"
        }) {
            var plan = OpenJdkAtomPatternGate.plan(PRODUCT, path, BLOB, "JAVA");
            assertTrue(plan.status().startsWith("HOLD_"));
            assertTrue(plan.tsv().indexOf("APPLY") < plan.tsv().indexOf("PATTERNIZE"));
            assertTrue(plan.tsv().indexOf("JIT_OR_NATIVE_PROOF") <
                    plan.tsv().indexOf("PATTERNIZE"));
            assertFalse(plan.tsv().contains("PATTERNIZER_PASS"));
        }
    }

    @Test
    void exactOneWorkOrderPerSourceAndDeterministicOrder(@TempDir Path temporary)
            throws IOException {
        Path file = temporary.resolve("sources.tsv");
        Files.writeString(file, HEADER + "\n" +
                row("src/hotspot/share/opto/compile.cpp", "NATIVE_CPP") +
                row("src/java.base/share/classes/java/lang/String.java", "JAVA"));
        var records = OpenJdkAtomPatternGate.read(PRODUCT, file);
        assertEquals(2, records.size());
        assertEquals("src/hotspot/share/opto/compile.cpp", records.getFirst().path());
        assertEquals("src/java.base/share/classes/java/lang/String.java",
                records.getLast().path());
        assertEquals(Set.of("M3HashPinnedTextSnapshotRecipe", "M3AtomizeRecipe"),
                Set.of(records.getFirst().atomizer(), records.getLast().atomizer()));
    }

    @Test
    void hashPrefixedGitPathIsNotSilentlyDroppedFromCompleteTree() {
        String path = "#generated-valid-src/Listed.java";
        String queue = HEADER + "\n" + row(path, "JAVA");
        var results = OpenJdkAtomPatternGate.readSnapshot(
                PRODUCT, queue.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, results.size());
        assertEquals(path, results.getFirst().path());
        assertEquals(OpenJdkAtomPatternGate.Lane.OTHER_JAVA, results.getFirst().lane());
    }

    @Test
    void duplicateOrInvalidSourceManifestRefuses(@TempDir Path temporary)
            throws IOException {
        Path file = temporary.resolve("sources.tsv");
        String a = row("src/java.base/share/classes/java/lang/String.java", "JAVA");
        Files.writeString(file, HEADER + "\n" + a + a, StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> OpenJdkAtomPatternGate.read(PRODUCT, file));
        Files.writeString(file, HEADER + "\n" +
                a.replace("\t100644\t", "\t120000\t"), StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> OpenJdkAtomPatternGate.read(PRODUCT, file));
    }

    @Test
    void realCliRefusesOverwriteAndWrongManifestHash(@TempDir Path temporary)
            throws Exception {
        Path input = temporary.resolve("SOURCE_WORK_QUEUE.tsv");
        Path output = temporary.resolve("work-orders.tsv");
        Files.writeString(input, HEADER + "\n" +
                row("src/java.base/share/classes/java/lang/String.java", "JAVA"));
        byte[] raw = Files.readAllBytes(input);
        String sha256 = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(raw));
        OpenJdkWorkOrderCli.main(new String[] {PRODUCT, input.toString(),
                sha256, output.toString()});
        String actual = Files.readString(output);
        assertTrue(actual.contains("JDK_BOOTSTRAP"));
        assertTrue(actual.contains("HOLD_UNTIL_EXECUTED_PER_ATOM_TESTS"));
        assertThrows(IllegalArgumentException.class, () ->
                OpenJdkWorkOrderCli.main(new String[] {PRODUCT,
                        input.toString(), sha256, output.toString()}));
        assertThrows(IllegalArgumentException.class, () ->
                OpenJdkWorkOrderCli.main(new String[] {PRODUCT,
                        input.toString(), "0".repeat(64),
                        temporary.resolve("unadmitted.tsv").toString()}));
    }

    @Test
    void malformedUtf8CannotEnterTheCanonicalSourceQueue() {
        byte[] invalid = {(byte) 0xc3, (byte) 0x28};
        assertThrows(IllegalArgumentException.class,
                () -> OpenJdkAtomPatternGate.readSnapshot(PRODUCT, invalid));
    }

    @Test
    void sourceBytesAreParsedOnlyOnceEvenWhenCallerMutatesTheArray() {
        byte[] original = (HEADER + "\n" +
                row("src/java.base/share/classes/java/lang/String.java", "JAVA"))
                .getBytes(StandardCharsets.UTF_8);
        var reviewed = OpenJdkAtomPatternGate.readSnapshot(PRODUCT, original);
        assertEquals(1, reviewed.size());
        original[0] = 'X';
        assertEquals("src/java.base/share/classes/java/lang/String.java",
                reviewed.getFirst().path());
        assertThrows(IllegalArgumentException.class,
                () -> OpenJdkAtomPatternGate.readSnapshot(PRODUCT, original));
    }

    @Test
    void falsePassAndUnreviewedPrecomputedFactsNeverAuthorizePatternizer() {
        String original = HEADER + "\n" +
                row("src/java.base/share/classes/java/lang/String.java", "JAVA");
        for (String altered : new String[] {
                original.replace("FILE_NAME_HINT_ONLY_NOT_AST", "COMPILER_AST_VERIFIED"),
                original.replace("SOURCE_SHA256_NOT_READ", "SHA256_REVIEWED"),
                original.replace("INSTALLED_M3JDK21_TEST_NOT_EXECUTED", "PASS"),
                original.replace("CANONICAL_M3_METHOD_RECIPE_NOT_EXECUTED", "PASS")
        }) {
            assertThrows(IllegalArgumentException.class, () ->
                    OpenJdkAtomPatternGate.readSnapshot(PRODUCT,
                            altered.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    void vendorJavaNativeAndLegacyRecipeCustodyRequireSeparateReview() {
        var vendorJava = OpenJdkAtomPatternGate.plan(PRODUCT,
                "m3/collections/implementations/guava/X.java", BLOB, "JAVA");
        var vendorNative = OpenJdkAtomPatternGate.plan(PRODUCT,
                "m3/collections/implementations/guava/engine.c", BLOB, "NATIVE_C");
        var recipe = OpenJdkAtomPatternGate.plan(PRODUCT,
                "m3/recipes/internal/Transformation.java", BLOB, "JAVA");
        assertEquals(OpenJdkAtomPatternGate.Lane.RECEIVED_PROVIDER, vendorJava.lane());
        assertEquals(OpenJdkAtomPatternGate.Lane.RECEIVED_PROVIDER, vendorNative.lane());
        assertEquals("M3HashPinnedTextSnapshotRecipe", vendorNative.atomizer());
        assertTrue(vendorNative.prePatternProof().contains("DONOR_LICENCE_NATIVE_ABI"));
        assertEquals(OpenJdkAtomPatternGate.Lane.RECIPE_CUSTODY, recipe.lane());
        assertEquals("REVIEW_ONLY", recipe.atomizer());
    }

    @Test
    void nestedVendoredTestAndThirdPartySourceRemainUnderProviderCustody() {
        var nestedTest = OpenJdkAtomPatternGate.plan(PRODUCT,
                "m3/collections/implementations/guava/src/test/java/X.java",
                BLOB, "JAVA");
        var thirdParty = OpenJdkAtomPatternGate.plan(PRODUCT,
                "src/java.base/share/third_party/native/hash.c",
                BLOB, "NATIVE_C");
        assertEquals(OpenJdkAtomPatternGate.Lane.RECEIVED_PROVIDER, nestedTest.lane());
        assertEquals(OpenJdkAtomPatternGate.Lane.RECEIVED_PROVIDER, thirdParty.lane());
        String source = HEADER + "\n" +
                row("m3/collections/implementations/guava/src/test/java/X.java",
                    "JAVA").replace("NEEDS_AST_CONTRACT_REVIEW",
                    "VENDORED_PROVIDER_CONTRACT_HOLD");
        assertEquals(1, OpenJdkAtomPatternGate.readSnapshot(
                PRODUCT, source.getBytes(StandardCharsets.UTF_8)).size());
    }

    @Test
    void sourceDispositionMustAgreeWithActualOwnerLane() {
        String source = HEADER + "\n" +
                row("src/hotspot/share/opto/compile.cpp", "NATIVE_CPP");
        String forged = source.replace("NATIVE_ABI_JNI_HOLD",
                "ORACLE_AND_TEST_OWNER_ONLY");
        assertThrows(IllegalArgumentException.class, () ->
                OpenJdkAtomPatternGate.readSnapshot(PRODUCT,
                        forged.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void contentLanguageMustAgreeWithTheSourceExtension() {
        assertThrows(IllegalArgumentException.class, () ->
                OpenJdkAtomPatternGate.plan(PRODUCT,
                        "src/java.base/share/classes/java/lang/String.java", BLOB, "NATIVE_C"));
        assertThrows(IllegalArgumentException.class, () ->
                OpenJdkAtomPatternGate.plan(PRODUCT,
                        "src/java.base/share/native/libjava/String.c", BLOB, "JAVA"));
    }

    @Test
    void staleBlobUnsafePathAndUnknownSourceKindRefuse() {
        assertThrows(IllegalArgumentException.class, () ->
                OpenJdkAtomPatternGate.plan(PRODUCT, "../String.java", BLOB, "JAVA"));
        assertThrows(IllegalArgumentException.class, () ->
                OpenJdkAtomPatternGate.plan(PRODUCT, "m3/String.java",
                        "no-valid-source-blob", "JAVA"));
        assertThrows(IllegalArgumentException.class, () ->
                OpenJdkAtomPatternGate.plan(PRODUCT, "m3/unknown.bin", BLOB, "OTHER_TRACKED"));
    }

    private static String row(String path, String kind) {
        boolean javaSource = kind.equals("JAVA");
        boolean nativeSource = kind.equals("NATIVE_CPP");
        String gate = javaSource ? "JAVAC_21_API_ABI_JTREG_REQUIRED"
                : nativeSource ? "NATIVE_HEADERS_C17_CPP20_ABI_JNI_REQUIRED"
                : "LANGUAGE_SPECIFIC_SOURCE_BUILD_REQUIRED";
        String status = nativeSource ? "NATIVE_ABI_JNI_HOLD"
                : "NEEDS_AST_CONTRACT_REVIEW";
        return String.join("\t", path, BLOB, "100644", kind, "source",
                "P4_REMAINING", "NO_CHALLENGE_FIT", "NONE", "not-analysed",
                "FILE_NAME_HINT_ONLY_NOT_AST", "SOURCE_SHA256_NOT_READ",
                gate, "CANONICAL_M3_METHOD_RECIPE_NOT_EXECUTED",
                "INSTALLED_M3JDK21_TEST_NOT_EXECUTED", status) + "\n";
    }
}
