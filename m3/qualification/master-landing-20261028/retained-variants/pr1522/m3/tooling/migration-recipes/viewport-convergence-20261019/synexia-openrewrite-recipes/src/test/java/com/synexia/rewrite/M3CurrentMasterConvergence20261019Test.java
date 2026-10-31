// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.synexia.rewrite;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adapted from the qualified collection task: actual SDK replay, not product proof. */
class M3CurrentMasterConvergence20261019Test {
    static List<M3GpuToolJoinRecipe> recipes() {
        return List.of(new M3CurrentMasterConvergence20261019());
    }
    private static M3GpuToolJoinManifest manifest(M3GpuToolJoinRecipe recipe) {
        return M3GpuToolJoinManifest.load(recipe.resourceRoot(),
                M3CurrentMasterConvergence20261019Test.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }

    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static List<SourceFile> before(M3GpuToolJoinManifest manifest, String root) throws IOException {
        var files = new ArrayList<SourceFile>();
        for (var target : manifest.targets()) {
            if (target.addition()) continue;
            String resource = root + "before/"
                    + target.path();
            try (var input = M3CurrentMasterConvergence20261019Test.class.getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                assertEquals(target.before(), M3GpuToolJoinManifest.sha256(source), target.path());
                files.add(text(target.path(), source));
            }
        }
        return files;
    }

    private static Map<Path, SourceFile> sources(List<SourceFile> files) {
        var result = new LinkedHashMap<Path, SourceFile>();
        for (var source : files) {
            assertTrue(result.putIfAbsent(source.getSourcePath(), source) == null);
        }
        return result;
    }

    private static Map<Path, SourceFile> execute(M3GpuToolJoinRecipe recipe,
            M3GpuToolJoinManifest manifest, List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var results = recipe.run(new InMemoryLargeSourceSet(input), context)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        var after = sources(input);
        long expected = manifest.targets().stream().filter(target -> {
            SourceFile source = after.get(Path.of(target.path()));
            return source == null || !target.after().equals(
                    M3GpuToolJoinManifest.sha256(source.printAll()));
        }).count();
        assertEquals(expected, results.size(), "SDK result cardinality");
        for (var result : results) {
            assertNotNull(result.getAfter(), "this task excludes removals");
            var source = result.getAfter();
            var target = manifest.target(M3GpuToolJoinManifest.normalized(source.getSourcePath()));
            assertNotNull(target, source.getSourcePath().toString());
            assertEquals(target.after(), M3GpuToolJoinManifest.sha256(source.printAll()));
            if (result.getBefore() != null) {
                assertEquals(result.getBefore().getId(), source.getId());
                assertEquals(result.getBefore().getSourcePath(), source.getSourcePath());
            }
            after.put(source.getSourcePath(), source);
        }
        for (var target : manifest.targets()) {
            SourceFile source = after.get(Path.of(target.path()));
            assertNotNull(source);
            assertEquals(target.after(), M3GpuToolJoinManifest.sha256(source.printAll()), target.path());
        }
        return after;
    }

    private static Map<Path, String> contents(Map<Path, SourceFile> files) {
        var result = new LinkedHashMap<Path, String>();
        files.forEach((path, source) -> result.put(path, source.printAll()));
        return result;
    }

    @ParameterizedTest @MethodSource("recipes")
    void exactEmissionAndFixedPoint(M3GpuToolJoinRecipe recipe) throws IOException {
        var manifest = manifest(recipe);
        var files = before(manifest, recipe.resourceRoot());
        String decoyPath = "unrelated-sdk-proof-decoy.txt";
        String decoyText = "source-shaped opaque input\n";
        assertTrue(manifest.target(decoyPath) == null);
        files.add(text(decoyPath, decoyText));
        var after = execute(recipe, manifest, files);
        assertEquals(decoyText, after.get(Path.of(decoyPath)).printAll());
        var repeated = execute(recipe, manifest, new ArrayList<>(after.values()));
        assertEquals(contents(after), contents(repeated), "SDK fixed point");
        System.out.println("SDK_EXECUTION\tPASS\tupstream=8.17.1\ttargets="
                + manifest.targets().size() + "\tJavaLST=NOT_RUN\tproduct=SEPARATE");
    }

    @ParameterizedTest @MethodSource("recipes")
    void reverseOrderAndSerialPartialResumption(M3GpuToolJoinRecipe recipe) throws IOException {
        var manifest = manifest(recipe);
        var original = before(manifest, recipe.resourceRoot());
        var after = execute(recipe, manifest, original);
        var expected = contents(after);
        var reversed = new ArrayList<>(original);
        Collections.reverse(reversed);
        assertEquals(expected, contents(execute(recipe, manifest, reversed)));
        var prefix = sources(original);
        for (var target : manifest.targets()) {
            Path path = Path.of(target.path());
            var one = sources(original);
            one.put(path, after.get(path));
            assertEquals(expected, contents(execute(recipe, manifest, new ArrayList<>(one.values()))),
                    "single-target resume: " + path);
            prefix.put(path, after.get(path));
            assertEquals(expected,
                    contents(execute(recipe, manifest, new ArrayList<>(prefix.values()))),
                    "serial-prefix resume: " + path);
        }
    }

    @ParameterizedTest @MethodSource("recipes")
    void missingDriftDuplicateAndOccupiedAdditionRefuse(M3GpuToolJoinRecipe recipe) throws IOException {
        var manifest = manifest(recipe);
        var files = before(manifest, recipe.resourceRoot());
        for (int index = 0; index < files.size(); index++) {
            var missing = new ArrayList<>(files);
            missing.remove(index);
            reject(recipe, missing);
            var changed = new ArrayList<>(files);
            var source = files.get(index);
            changed.set(index, text(source.getSourcePath().toString(), source.printAll() + "// drift\n"));
            reject(recipe, changed);
            var duplicate = new ArrayList<>(files);
            duplicate.add(source);
            reject(recipe, duplicate);
        }
        for (var target : manifest.targets()) {
            if (target.addition()) {
                var occupied = new ArrayList<>(files);
                occupied.add(text(target.path(), "unadmitted addition\n"));
                reject(recipe, occupied);
            }
        }
    }

    private static void reject(M3GpuToolJoinRecipe recipe, List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        try {
            var results = recipe.run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add)).getChangeset().getAllResults();
            assertTrue(results.isEmpty(), "refused task must emit no candidate");
            assertFalse(errors.isEmpty(), "SDK must report admission refusal");
        } catch (IllegalStateException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
