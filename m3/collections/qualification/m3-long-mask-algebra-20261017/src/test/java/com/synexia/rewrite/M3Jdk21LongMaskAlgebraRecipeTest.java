// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Tests actual SDK replacement over exact V1 preimages, not a simulated mutator. */
final class M3Jdk21LongMaskAlgebraRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/"
            + M3Jdk21LongMaskAlgebraRecipe.CRATE + "/";

    @Test void exactTwoFileReplacementAndFixedPoint() {
        Map<String, String> postimage = images(true);
        Map<String, String> input = images(false);
        var result = new M3Jdk21LongMaskAlgebraRecipe().run(
                new InMemoryLargeSourceSet(parse(input)), context(), 1);
        result.getChangeset().getAllResults().forEach(change ->
                input.put(normalize(change.getAfter().getSourcePath()), change.getAfter().printAll()));
        assertEquals(postimage, input);
        var repeat = new M3Jdk21LongMaskAlgebraRecipe().run(
                new InMemoryLargeSourceSet(parse(input)), context(), 1);
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
    }

    @Test void allFourIndependentPartialResumeSubsets() {
        List<Map.Entry<String, String>> before = new ArrayList<>(images(false).entrySet());
        List<Map.Entry<String, String>> after = new ArrayList<>(images(true).entrySet());
        assertEquals(2, before.size());
        for (int mask = 0; mask < 4; mask++) {
            Map<String, String> input = new TreeMap<>();
            for (int i = 0; i < 2; i++) {
                input.put(before.get(i).getKey(), (mask & (1 << i)) != 0
                        ? after.get(i).getValue() : before.get(i).getValue());
            }
            var result = new M3Jdk21LongMaskAlgebraRecipe().run(
                    new InMemoryLargeSourceSet(parse(input)), context(), 1);
            result.getChangeset().getAllResults().forEach(change ->
                    input.put(normalize(change.getAfter().getSourcePath()), change.getAfter().printAll()));
            assertEquals(images(true), input, "partial resume mask=" + mask);
        }
    }

    @Test void refuseChangedOrForeignSourceWithoutPartialAdmittance() {
        var inner = (M3HashPinnedJavaSnapshotRecipe)
                new M3Jdk21LongMaskAlgebraRecipe().getRecipeList().getFirst();
        Map.Entry<String, String> source = images(false).entrySet().iterator().next();
        var ctx = context();
        var inventory = inner.getInitialValue(ctx);
        inner.getScanner(inventory).visit(parse(Map.of(source.getKey(),
                source.getValue() + "// contract drift\n")).getFirst(), ctx);
        assertThrows(IllegalStateException.class, () -> inner.generate(inventory, ctx));
        var foreign = inner.getInitialValue(context());
        var context = context();
        inner.getScanner(foreign).visit(PlainText.builder().sourcePath(Path.of(source.getKey()))
                .text(source.getValue()).build(), context);
        assertThrows(IllegalStateException.class, () -> inner.generate(foreign, context));
    }

    @Test void refuseMissingOneRequiredPreimage() {
        var inner = (M3HashPinnedJavaSnapshotRecipe)
                new M3Jdk21LongMaskAlgebraRecipe().getRecipeList().getFirst();
        var ctx = context();
        var inventory = inner.getInitialValue(ctx);
        inner.getScanner(inventory).visit(parse(Map.of(images(false).firstKey(),
                images(false).get(images(false).firstKey()))).getFirst(), ctx);
        assertThrows(IllegalStateException.class, () -> inner.generate(inventory, ctx));
    }

    private static TreeMap<String, String> images(boolean after) {
        TreeMap<String, String> result = new TreeMap<>();
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\t", -1);
            if (fields.length != 4 || "ABSENT".equals(fields[1])) {
                throw new IllegalStateException("invalid exact-before manifest row");
            }
            String basename = fields[3].substring(0, fields[3].length() - 4);
            result.put(fields[0], resource(after ? fields[3] : basename + ".before"));
        }
        return result;
    }
    private static List<SourceFile> parse(Map<String, String> source) {
        return JavaParser.fromJavaVersion().build().parseInputs(source.entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList(),
                null, context()).toList();
    }
    private static String normalize(Path path) { return path.toString().replace('\\', '/'); }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    private static String resource(String name) {
        try (var stream = M3Jdk21LongMaskAlgebraRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IllegalStateException("missing resource: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }
}
