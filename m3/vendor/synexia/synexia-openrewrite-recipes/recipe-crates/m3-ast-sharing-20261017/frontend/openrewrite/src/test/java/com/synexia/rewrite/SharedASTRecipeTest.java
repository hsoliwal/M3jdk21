// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

class SharedASTRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/m3-openrewrite-shared-ast-20261017/";
    private static final Path TARGET = Path.of("synexia-mindex/compiler-openrewrite/src/main/java/com/synexia/mindex/compiler/ast/M3OpenRewriteSharedAST.java");

    @Test void actualTypedCandidateFixedPointAndRefusal() throws Exception {
        Recipe recipe = named();
        SourceFile emitted = run(recipe, List.of()).getFirst();
        assertInstanceOf(J.CompilationUnit.class, emitted);
        assertEquals(TARGET, emitted.getSourcePath());
        String expected = new String(getClass().getResourceAsStream(ROOT + "M3OpenRewriteSharedAST.java.txt").readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(expected, emitted.printAll());
        assertTrue(run(named(), List.of(emitted)).isEmpty(), "fixed point");
        assertThrows(IllegalStateException.class, () -> run(named(), List.of(emitted, emitted)), "duplicates");
        SourceFile occupied = parse(TARGET, "class Occupied { String text = \"M3OpenRewriteSharedAST /* decoy */\"; }\n");
        assertThrows(IllegalStateException.class, () -> run(named(), List.of(occupied)), "source drift");
        SourceFile wrongKind = PlainText.builder().sourcePath(TARGET).text(expected).build();
        assertThrows(IllegalStateException.class, () -> run(named(), List.of(wrongKind)), "wrong source representation");
        Path path = Path.of("target/candidate").resolve(TARGET);
        Files.createDirectories(path.getParent());
        Files.writeString(path, emitted.printAll());
        Files.writeString(Path.of("target/SDK_RECEIPT.tsv"), "provider\tM3HashPinnedJavaSnapshotRecipe\n"
                + "mode\tJAVA_LST\nresults\t1\nfixed_point_results\t0\ntarget\t" + TARGET
                + "\nsha256\t" + sha256(emitted.printAll()) + "\n");
    }

    @Test void unrelatedSourceIdentityAndTemplateTamper() throws Exception {
        SourceFile unrelated = parse(Path.of("demo/Untouched.java"),
                "package demo; record Untouched(String text) { String note() { return \"ANALYZE // string\"; } }\n");
        List<SourceFile> changed = run(named(), List.of(unrelated));
        assertEquals(1, changed.size());
        assertEquals(TARGET, changed.getFirst().getSourcePath());
        assertTrue(run(named(), List.of(changed.getFirst(), unrelated)).isEmpty());
        assertTrue(run(named(), List.of(unrelated, changed.getFirst())).isEmpty());
        Path template = Path.of(getClass().getResource(ROOT + "M3OpenRewriteSharedAST.java.txt").toURI());
        byte[] sealed = Files.readAllBytes(template);
        try {
            Files.writeString(template, new String(sealed, StandardCharsets.UTF_8) + "\n// tampered template\n");
            assertThrows(IllegalStateException.class, () -> run(named(), List.of()));
        } finally { Files.write(template, sealed); }
    }

    private static Recipe named() throws Exception {
        var resource = SharedASTRecipeTest.class.getResource("/META-INF/rewrite/m3-openrewrite-shared-ast.yml");
        try (var input = resource.openStream()) {
            return Environment.builder().load(new YamlResourceLoader(input, resource.toURI(), new Properties()))
                    .build().activateRecipes("com.synexia.m3.OpenRewriteSharedAST20261017");
        }
    }

    private static SourceFile parse(Path path, String source) {
        return JavaParser.fromJavaVersion().build().parseInputs(List.of(Parser.Input.fromString(path, source)),
                null, new InMemoryExecutionContext(t -> { throw new IllegalStateException(t); })).findFirst().orElseThrow();
    }

    private static List<SourceFile> run(Recipe recipe, List<SourceFile> before) {
        List<Throwable> failures = new ArrayList<>();
        var context = new InMemoryExecutionContext(failures::add);
        String extra = System.getProperty("m3.sharedAst.plugin", "");
        List<Path> classpath = new ArrayList<>(JavaParser.runtimeClasspath());
        if (!extra.isBlank()) classpath.add(Path.of(extra));
        context.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath", classpath);
        var results = recipe.run(new InMemoryLargeSourceSet(before), context).getChangeset().getAllResults();
        if (!failures.isEmpty()) throw new IllegalStateException("recipe refused candidate", failures.getFirst());
        return results.stream().map(result -> result.getAfter()).toList();
    }

    private static String sha256(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
