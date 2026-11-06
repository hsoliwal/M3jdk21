// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Exact-source OpenRewrite execution, immutable replay and drift refusal of the re-sealed
 * ChangeMap lower-bound crate (donor hsoliwal/com.synexia 4a2e6452a7a,
 * crate m3-file-changemap-lower-bound-20261029).
 */
final class M3FileChangeMapLowerBoundRecipeTest {
    private static final String CRATE = "m3-file-changemap-lower-bound-20261106";
    private static final String ROOT = "/jdk/internal/m3/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String TARGET = "m3/file/src/main/java/jdk/m3/file/M3FileEditBatch.java";
    /** Live owner on master c3834a78dba6 (blob 5ef6e08f78), the donor preimage after the JDK rename. */
    private static final String ORIGINAL_SHA =
            "97a80e6709b5c31dae837c7c4845ffb391c9a2914032df76d5720b88f161b634";
    private static final int ANCHORS = 3;

    @Test void onlyOneExactFileIsAdmitted() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        assertEquals(List.of(TARGET), recipe.targetPaths());
        var rows = rows();
        assertEquals(1, rows.size());
        String[] row = rows.getFirst();
        assertEquals(4, row.length);
        assertEquals(TARGET, row[0]);
        assertEquals(ORIGINAL_SHA, row[1]);
        assertEquals(ORIGINAL_SHA, sha(before()));
        assertEquals(row[2], sha(after()));
        assertEquals("M3FileEditBatch.java.txt", row[3]);
    }

    @Test void transformsRealJavaSourceAndSecondRunIsFixedPoint() {
        var ctx = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var first = recipe.run(new InMemoryLargeSourceSet(List.of(parse(ctx, before()))), ctx, 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        assertNotNull(changes.getFirst().getAfter());
        assertEquals(after(), changes.getFirst().getAfter().printAll());

        var replay = new M3HashPinnedJavaSnapshotRecipe(CRATE).run(
                new InMemoryLargeSourceSet(List.of(parse(ctx, after()))), context(), 1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
    }

    @Test void refusesUnreviewedPreimageAndAbsentOriginal() {
        var ctx = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(ctx);
        recipe.getScanner(inventory).visit(parse(ctx, before() + "// drift\n"), ctx);
        var drift = assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, ctx));
        assertTrue(drift.getMessage().contains("source drift"));

        var fresh = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var missing = assertThrows(IllegalStateException.class,
                () -> fresh.generate(fresh.getInitialValue(context()), context()));
        assertTrue(missing.getMessage().contains("required source missing"));
    }

    @Test void onlyThePrivateHelperAndTwoQueryBodiesDiffer() {
        String pre = before(), post = after();
        String firstOld = "        /** Returns -1 for interior deleted bytes; endpoints honor caller affinity. */";
        String firstNew = "        /**\n         * First edit boundary not less than position.";
        String end = "        /** Conservatively includes insertion exactly on a span boundary. */";
        int oldStart = pre.indexOf(firstOld), newStart = post.indexOf(firstNew);
        int oldEnd = pre.indexOf(end), newEnd = post.indexOf(end);
        assertTrue(oldStart > 0 && newStart > 0 && oldEnd > oldStart && newEnd > newStart);
        assertEquals(pre.substring(0, oldStart), post.substring(0, newStart));
        assertEquals(pre.substring(oldEnd), post.substring(newEnd));
        assertTrue(post.contains("private static int lowerBound(int[] starts, int position) {"));
        assertTrue(post.contains("int index = lowerBound(oldStart, position);"));
        assertTrue(post.contains("int index = lowerBound(newStart, position);"));
        assertTrue(post.contains("Objects.checkIndex(position, oldLength + 1);"));
        assertTrue(post.contains("Objects.requireNonNull(affinity, \"affinity\");"));
        assertEquals(ANCHORS, post.split("//FLLDCIM: RECIPE ", -1).length - 1);
        assertEquals(0, pre.split("//FLLDCIM: RECIPE ", -1).length - 1);
    }

    @Test void recipeIsDiscoverable() {
        var result = Environment.builder().scanRuntimeClasspath("jdk.internal.m3.rewrite").build()
                .activateRecipes("jdk.internal.m3.rewrite.M3FileChangeMapLowerBound20261106");
        assertTrue(contains(result));
    }

    private static boolean contains(org.openrewrite.Recipe recipe) {
        if (recipe instanceof M3HashPinnedJavaSnapshotRecipe snapshot)
            return CRATE.equals(snapshot.getCrateName());
        return recipe.getRecipeList().stream().anyMatch(M3FileChangeMapLowerBoundRecipeTest::contains);
    }

    private static List<String[]> rows() {
        return read("manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1)).toList();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static SourceFile parse(InMemoryExecutionContext context, String source) {
        var files = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(TARGET), source)), null, context).toList();
        assertEquals(1, files.size());
        assertInstanceOf(J.CompilationUnit.class, files.getFirst());
        return files.getFirst();
    }

    private static String sha(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static String before() { return read("pre/M3FileEditBatch.java.txt"); }
    private static String after() { return read("M3FileEditBatch.java.txt"); }

    private static String read(String name) {
        try (var stream = M3FileChangeMapLowerBoundRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new AssertionError("missing sealed fixture: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("cannot read sealed fixture: " + name, e);
        }
    }
}
