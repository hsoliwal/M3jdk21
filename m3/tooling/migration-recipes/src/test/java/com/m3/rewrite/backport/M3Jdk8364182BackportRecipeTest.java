// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3Jdk8364182BackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-security-properties-8364182/pre/";

    private static final String SECURITY =
            "src/java.base/share/classes/java/security/Security.java";
    private static final String ACCESS =
            "src/java.base/share/classes/jdk/internal/access/JavaSecurityPropertiesAccess.java";
    private static final String VM_SUPPORT =
            "src/java.base/share/classes/jdk/internal/vm/VMSupport.java";
    private static final String VM_SYMBOLS =
            "src/hotspot/share/classfile/vmSymbols.hpp";
    private static final String DCMD_CPP =
            "src/hotspot/share/services/diagnosticCommand.cpp";
    private static final String DCMD_HPP =
            "src/hotspot/share/services/diagnosticCommand.hpp";
    private static final String TEST =
            "test/hotspot/jtreg/serviceability/dcmd/vm/SecurityPropertiesTest.java";

    @Test
    void compositeRecipeOwnsStructuredJavaAndHotspotTextAtoms() {
        Recipe recipe = new M3Jdk8364182BackportRecipe();

        assertEquals(
                "f2f8828188f45d16344c82adfbf951f7409b8825",
                M3Jdk8364182BackportRecipe.UPSTREAM_COMMIT);
        assertEquals(2, recipe.getRecipeList().size());

        var javaRecipe = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class,
                recipe.getRecipeList().get(0));
        var textRecipe = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                recipe.getRecipeList().get(1));

        assertEquals(
                "jdk27-security-properties-8364182-java",
                javaRecipe.getCrateName());
        assertEquals(
                "jdk27-security-properties-8364182-text",
                textRecipe.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("serviceability"));
    }

    @Test
    void exactJava21PreimagesReachReviewedPostimageAndThenFixedPoint()
            throws Exception {
        Recipe recipe = new M3Jdk8364182BackportRecipe();
        List<SourceFile> before = baseline();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(7, changes.size());
        assertChangedContains(changes, SECURITY, "getCurrentProperties()");
        assertChangedContains(changes, ACCESS, "Properties getCurrentProperties();");
        assertChangedContains(
                changes,
                VM_SUPPORT,
                "serializeSecurityPropertiesToByteArray()");
        assertChangedContains(
                changes,
                VM_SYMBOLS,
                "serializeSecurityPropertiesToByteArray_name");
        assertChangedContains(
                changes,
                DCMD_HPP,
                "class PrintSecurityPropertiesDCmd");
        assertChangedContains(
                changes,
                DCMD_CPP,
                "PrintSecurityPropertiesDCmd");
        assertChangedContains(
                changes,
                TEST,
                "executor.execute(\"VM.security_properties\")");

        List<SourceFile> after =
                changes.stream().map(Result::getAfter).toList();
        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void staleProductPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = new ArrayList<>(baseline());
        SourceFile product = sources.get(0);
        sources.set(
                0,
                java(
                        SECURITY,
                        product.printAll()
                                .replace(
                                        "return initialSecurityProperties;",
                                        "return initialSecurityProperties; // drift")));

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jdk8364182BackportRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(sources),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());
    }

    private static void assertChangedContains(
            List<Result> changes, String path, String needle) {
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        result.getAfter() != null
                                                && path.equals(
                                                        normalized(
                                                                result.getAfter()
                                                                        .getSourcePath()))
                                                && result.getAfter()
                                                        .printAll()
                                                        .contains(needle)),
                path + " should contain " + needle);
    }

    private static List<SourceFile> baseline() throws Exception {
        List<SourceFile> sources = new ArrayList<>();
        sources.add(java(SECURITY, resource("Security.java.before.txt")));
        sources.add(
                java(
                        ACCESS,
                        resource(
                                "JavaSecurityPropertiesAccess.java.before.txt")));
        sources.add(
                java(VM_SUPPORT, resource("VMSupport.java.before.txt")));
        sources.add(
                text(VM_SYMBOLS, resource("vmSymbols.hpp.before.txt")));
        sources.add(
                text(
                        DCMD_CPP,
                        resource("diagnosticCommand.cpp.before.txt")));
        sources.add(
                text(
                        DCMD_HPP,
                        resource("diagnosticCommand.hpp.before.txt")));
        return List.copyOf(sources);
    }

    private static SourceFile java(String path, String source) {
        InMemoryExecutionContext context = context();
        List<Parser.Input> inputs =
                List.of(Parser.Input.fromString(Path.of(path), source));
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .findFirst()
                .orElseThrow();
    }

    private static SourceFile text(String path, String source) {
        return PlainText.builder()
                .sourcePath(Path.of(path))
                .text(source)
                .build();
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jdk8364182BackportRecipeTest.class.getResourceAsStream(
                        PRE + name)) {
            if (input == null) {
                throw new IOException("missing preimage resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
