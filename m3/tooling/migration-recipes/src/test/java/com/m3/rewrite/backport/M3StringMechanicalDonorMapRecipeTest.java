// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-11 through com.synexia.m3.StringDonorMap20261011.
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3StringMechanicalDonorMapRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-string-mechanical-donor-map/";
    private static final String[][] FIXTURES = {
        {"m3/docs/string-precompute-completeness.tsv", "00-completeness.tsv.txt"},
        {"m3/docs/synexia-mindex-precompute-inventory.tsv", "02-inventory.tsv.txt"},
        {"m3/docs/synexia-string-precompute-port-map.tsv", "01-port-map.tsv.txt"},
        {"m3/runtime-integration/check-m3string-invariants.py", "03-invariants.py.txt"}
    };

    @Test void postimagesAreFixedPointForEveryInputOrder() {
        var recipe = recipe();
        assertEquals("m3-string-mechanical-donor-map", recipe.getCrateName());
        assertEquals(List.of(FIXTURES).stream().map(row -> row[0]).toList(), recipe.targetPaths());
        List<List<SourceFile>> orders = new ArrayList<>();
        permutations(new ArrayList<>(after()), 0, orders);
        assertEquals(24, orders.size());
        for (List<SourceFile> order : orders) {
            var errors = new ArrayList<Throwable>();
            var results = recipe.run(new InMemoryLargeSourceSet(order),
                    new InMemoryExecutionContext(errors::add), 1).getChangeset().getAllResults();
            assertTrue(errors.isEmpty(), errors::toString);
            assertTrue(results.isEmpty(), () -> "postimage changed for " + order);
        }
    }

    @Test void everyTargetRefusesDriftMissingAndDuplicateSources() {
        for (int i = 0; i < FIXTURES.length; i++) {
            List<SourceFile> original = after();
            PlainText file = (PlainText) original.get(i);
            List<SourceFile> drift = new ArrayList<>(original);
            drift.set(i, file.withText(file.getText() + "# drift\n"));
            refused(drift, "source drift: " + FIXTURES[i][0]);
            List<SourceFile> missing = new ArrayList<>(original);
            missing.remove(i);
            refused(missing, "required JDK21 text source missing: " + FIXTURES[i][0]);
            List<SourceFile> duplicate = new ArrayList<>(original);
            duplicate.add(text(FIXTURES[i][0], file.getText()));
            refused(duplicate, "duplicate target: " + FIXTURES[i][0]);
        }
    }

    @Test void wrongParserKindRefusesForEveryTarget() {
        var errors = new ArrayList<Throwable>();
        SourceFile java = JavaParser.fromJavaVersion().build()
                .parse(new InMemoryExecutionContext(errors::add), "class Opaque {}\n")
                .findFirst().orElseThrow();
        assertTrue(errors.isEmpty(), errors::toString);
        assertInstanceOf(org.openrewrite.java.tree.J.CompilationUnit.class, java);
        for (int i = 0; i < FIXTURES.length; i++) {
            List<SourceFile> wrong = new ArrayList<>(after());
            wrong.set(i, java.withSourcePath(Path.of(FIXTURES[i][0])));
            refused(wrong, "target is not OpenRewrite PlainText: " + FIXTURES[i][0]);
        }
    }

    @Test void unrelatedCodeLikePayloadIsOpaque() {
        PlainText opaque = text("fixtures/code-and-regex.txt",
                "class C { String code = \"System.exit(0)\"; } regex=\\bM3.*\\b\n");
        List<SourceFile> inputs = new ArrayList<>(after());
        inputs.add(opaque);
        var errors = new ArrayList<Throwable>();
        var results = recipe().run(new InMemoryLargeSourceSet(inputs),
                new InMemoryExecutionContext(errors::add), 1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors::toString);
        assertTrue(results.isEmpty());
        assertSame(opaque, inputs.getLast());
    }

    private static void refused(List<SourceFile> input, String diagnostic) {
        List<String> before = input.stream().map(SourceFile::printAll).toList();
        var errors = new ArrayList<Throwable>();
        List<org.openrewrite.Result> results = List.of();
        try {
            results = recipe().run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add), 1).getChangeset().getAllResults();
        } catch (IllegalStateException refusal) {
            errors.add(refusal);
        }
        assertTrue(results.isEmpty(), "refusal must emit no changes");
        assertEquals(before, input.stream().map(SourceFile::printAll).toList());
        assertTrue(errors.stream().anyMatch(error -> hasDiagnostic(error, diagnostic)),
                () -> "missing refusal " + diagnostic + ": " + errors);
    }

    private static boolean hasDiagnostic(Throwable error, String diagnostic) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof IllegalStateException
                    && String.valueOf(cause.getMessage()).contains(diagnostic)) return true;
        }
        return false;
    }

    private static M3Jdk21HashPinnedTextSnapshotRecipe recipe() {
        return new M3Jdk21HashPinnedTextSnapshotRecipe("m3-string-mechanical-donor-map");
    }

    private static List<SourceFile> after() {
        List<SourceFile> result = new ArrayList<>();
        for (String[] fixture : FIXTURES) result.add(text(fixture[0], resource(fixture[1])));
        return result;
    }

    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static void permutations(List<SourceFile> input, int index, List<List<SourceFile>> output) {
        if (index == input.size()) {
            output.add(List.copyOf(input));
            return;
        }
        for (int i = index; i < input.size(); i++) {
            Collections.swap(input, index, i);
            permutations(input, index + 1, output);
            Collections.swap(input, index, i);
        }
    }

    private static String resource(String name) {
        try (var stream = M3StringMechanicalDonorMapRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IllegalStateException(name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
