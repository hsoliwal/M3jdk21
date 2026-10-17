// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** Real SDK scheduling and Java21 parsing. Exact text transport is not an AST rewrite. */
final class M3FlowJdkReceiverTest {
    private static final String ROOT = "/com/synexia/rewrite/flow-jdk21/";
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT, M3FlowJdkReceiverTest.class::getResourceAsStream, () -> { });
    }
    private static String resource(String path) throws Exception {
        try (var in = M3FlowJdkReceiverTest.class.getResourceAsStream(ROOT + path)) {
            return new String(Objects.requireNonNull(in, path).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static SourceFile text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }
    private static List<SourceFile> baseline() throws Exception {
        var result = new ArrayList<SourceFile>();
        for (var t : manifest().targets()) if (!t.addition()) {
            String data = resource("preimages/" + Path.of(t.path()).getFileName());
            assertEquals(t.before(), M3GpuToolJoinManifest.sha256(data));
            result.add(text(t.path(), data));
        }
        result.add(text("unrelated/fake.java.txt", "class M3Flow { void deleteEverything() {} }\n"));
        return result;
    }
    private record Outcome(List<SourceFile> image, List<Throwable> errors, int changes) { }
    private static Outcome run(List<SourceFile> input, Runnable checkpoint) {
        var errors = new ArrayList<Throwable>(); var ctx = new InMemoryExecutionContext(errors::add);
        if (checkpoint != null) ctx.putMessage(M3GpuToolJoinRecipe.CHECKPOINT_MESSAGE, checkpoint);
        var recipe = Environment.builder().scanClassLoader(M3FlowJdkReceiverTest.class.getClassLoader())
                .build().activateRecipes("com.synexia.m3.CollectionFlowJdk21Receiver");
        List<Result> changes;
        try { changes = recipe.run(new InMemoryLargeSourceSet(input), ctx).getChangeset().getAllResults(); }
        catch (RuntimeException failure) { errors.add(failure); return new Outcome(List.of(), errors, 0); }
        if (!errors.isEmpty()) return new Outcome(List.of(), errors, changes.size());
        var image = new TreeMap<String,SourceFile>();
        for (var file : input) image.put(file.getSourcePath().toString(), file);
        for (var change : changes) {
            assertNotNull(change.getAfter(), "no deletion");
            if (change.getBefore() != null) image.remove(change.getBefore().getSourcePath().toString());
            image.put(change.getAfter().getSourcePath().toString(), change.getAfter());
        }
        return new Outcome(new ArrayList<>(image.values()), errors, changes.size());
    }
    private static Map<String,String> hashes(List<SourceFile> sources) {
        var hashes = new TreeMap<String,String>();
        for (var file : sources) hashes.put(file.getSourcePath().toString(), M3GpuToolJoinManifest.sha256(file.printAll()));
        return hashes;
    }
    private static void accepted(Outcome result) {
        assertTrue(result.errors().isEmpty(), result.errors().toString());
        var hashes = hashes(result.image());
        for (var t : manifest().targets()) assertEquals(t.after(), hashes.get(t.path()), t.path());
        assertEquals(M3GpuToolJoinManifest.sha256("class M3Flow { void deleteEverything() {} }\n"), hashes.get("unrelated/fake.java.txt"));
    }
    @Test void namedRecipeOutputAndFixedPoint() throws Exception {
        var result = run(baseline(), null); accepted(result);
        assertEquals(manifest().targets().stream().filter(t -> t.addition()).count(), result.changes());
        var again = run(result.image(), null); accepted(again); assertEquals(0, again.changes());
        Path output = Path.of(System.getProperty("receiver.sdk.image"));
        assertFalse(Files.exists(output), "fresh SDK image required"); Files.createDirectories(output);
        for (var file : result.image()) {
            Path target = output.resolve(file.getSourcePath()); Files.createDirectories(target.getParent());
            Files.writeString(target, file.printAll(), StandardOpenOption.CREATE_NEW);
        }
        Files.writeString(output.resolve("SDK.txt"), "NAMED=com.synexia.m3.CollectionFlowJdk21Receiver\nFIXED_POINT=0\n");
    }
    @Test void boundedPartialResumeAndInputOrders() throws Exception {
        var additions = manifest().targets().stream().filter(t -> t.addition()).toList();
        for (int trial = 0; trial < 64; trial++) {
            var input = baseline(); var random = new Random(0x4d33L + trial); int installed = 0;
            for (int i = 0; i < additions.size(); i++) if (trial == i || (trial >= additions.size() && random.nextBoolean())) {
                var t = additions.get(i); input.add(text(t.path(), t.text())); installed++;
            }
            Collections.shuffle(input, random);
            var result = run(input, null); accepted(result); assertEquals(additions.size() - installed, result.changes());
            var repeat = run(result.image(), null); accepted(repeat); assertEquals(0, repeat.changes());
        }
    }
    @Test void driftOccupiedMissingDuplicateAndCancellationRefuse() throws Exception {
        for (var t : manifest().targets()) {
            var input = baseline(); input.removeIf(f -> f.getSourcePath().toString().equals(t.path()));
            input.add(text(t.path(), "unreviewed source"));
            var drift = run(input, null); assertFalse(drift.errors().isEmpty()); assertTrue(drift.image().isEmpty());
            if (!t.addition()) {
                input = baseline(); input.removeIf(f -> f.getSourcePath().toString().equals(t.path()));
                assertFalse(run(input, null).errors().isEmpty());
            }
            input = baseline(); input.removeIf(f -> f.getSourcePath().toString().equals(t.path()));
            String data = t.addition() ? t.text() : resource("preimages/" + Path.of(t.path()).getFileName());
            input.add(text(t.path(), data)); input.add(text(t.path(), data));
            assertFalse(run(input, null).errors().isEmpty());
        }
        var cancelled = run(baseline(), () -> { throw new CancellationException("cancel receiver"); });
        assertFalse(cancelled.errors().isEmpty()); assertTrue(cancelled.image().isEmpty());
    }
    @Test void actualJava21ParserRoundTrip() throws Exception {
        var result = run(baseline(), null); accepted(result);
        String[] sources = result.image().stream().filter(f -> f.getSourcePath().toString().endsWith(".java"))
                .map(SourceFile::printAll).toArray(String[]::new);
        var errors = new ArrayList<Throwable>();
        var inputs = result.image().stream().filter(f -> f.getSourcePath().toString().endsWith(".java"))
                .map(f -> Parser.Input.fromString(f.getSourcePath(), f.printAll())).toList();
        var parsed = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(), errors.toString()); assertEquals(sources.length, parsed.size());
        var expected = new HashSet<>(Arrays.asList(sources));
        for (var tree : parsed) { assertInstanceOf(J.CompilationUnit.class, tree); assertTrue(expected.remove(tree.printAll())); }
        assertTrue(expected.isEmpty());
    }
    @Test void independentParallelTrialsHaveIdenticalRoots() throws Exception {
        var expected = hashes(run(baseline(), null).image());
        try (var executor = Executors.newFixedThreadPool(4)) {
            var jobs = new ArrayList<Future<Map<String,String>>>();
            for (int i = 0; i < 8; i++) { final int seed = i;
                jobs.add(executor.submit(() -> {
                    var input = baseline(); Collections.shuffle(input, new Random(seed));
                    var result = run(input, null); accepted(result); return hashes(result.image());
                }));
            }
            for (var job : jobs) assertEquals(expected, job.get(60, TimeUnit.SECONDS));
        }
    }
}
