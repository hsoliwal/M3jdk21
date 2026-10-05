// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Source-sealed Maven/OpenRewrite crate for the A3 recipe-mastery laboratory.
 *
 * <p>The crate owns only two test-harness sources. It grants no JDK product mutation or promotion
 * authority. Existing sources must match their reviewed postimages byte-for-byte; missing sources
 * are generated from sealed classpath resources.</p>
 */
public final class M3A3RecipeMasteryLabRecipe
        extends ScanningRecipe<M3A3RecipeMasteryLabRecipe.Inventory> {
    private static final String ROOT =
            "/com/m3/rewrite/a3-recipe-mastery-lab/";
    private static final Map<String, String> TARGETS =
            Map.of(
                    "src/test/java/com/m3/rewrite/M3RecipeMasteryLab.java",
                    "M3RecipeMasteryLab.java.after",
                    "src/test/java/com/m3/rewrite/M3Java21RecipeMasteryLabTest.java",
                    "M3Java21RecipeMasteryLabTest.java.after");

    static final class Inventory {
        private final Map<String, String> seen = new LinkedHashMap<>();
        private boolean refused;
    }

    @Override
    public String getDisplayName() {
        return "Install M3 A3 recipe mastery laboratory";
    }

    @Override
    public String getDescription() {
        return "Installs the in-memory Java/regex permutation laboratory that mechanically "
                + "masters the retained A3 atomize/patternize recipe through compiler, JUnit, "
                + "behavior-parity and fixed-point fan-in.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "a3",
                "recipe-first",
                "mastery-lab",
                "atomization",
                "patternization",
                "regex",
                "java21",
                "compiler-oracle",
                "fixed-point");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        TARGETS.forEach(
                (path, resource) -> {
                    String source = resource(resource);
                    parse(path, source, context);
                });
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) {
                    return tree;
                }
                String path = normalized(source.getSourcePath());
                String resourceName = TARGETS.get(path);
                if (resourceName == null) {
                    return tree;
                }
                if (inventory.seen.putIfAbsent(path, source.printAll()) != null
                        || !(source instanceof J.CompilationUnit)
                        || !resource(resourceName).equals(source.printAll())) {
                    inventory.refused = true;
                    throw new IllegalStateException("A3 mastery-lab source drift: " + path);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory, ExecutionContext context) {
        if (inventory.refused) {
            throw new IllegalStateException("A3 mastery-lab source refused");
        }
        return TARGETS.entrySet().stream()
                .filter(entry -> !inventory.seen.containsKey(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> parse(entry.getKey(), resource(entry.getValue()), context))
                .toList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {};
    }

    public boolean productMutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static String resourceText(String targetPath) {
        String resourceName = TARGETS.get(targetPath);
        if (resourceName == null) {
            throw new IllegalArgumentException("unowned A3 mastery-lab path: " + targetPath);
        }
        return resource(resourceName);
    }

    private static SourceFile parse(
            String path, String source, ExecutionContext context) {
        Path sourcePath = Path.of(path);
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(sourcePath, source)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            if (files.size() != 1
                    || !(files.getFirst() instanceof J.CompilationUnit unit)
                    || !source.equals(unit.printAll())) {
                throw new IllegalStateException(
                        "A3 mastery-lab Java resource roundtrip failed: " + path);
            }
            return unit.withSourcePath(sourcePath);
        }
    }

    private static String resource(String name) {
        try (InputStream stream =
                M3A3RecipeMasteryLabRecipe.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) {
                throw new IllegalStateException("missing A3 mastery-lab resource: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 mastery-lab resource: " + name, failure);
        }
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }
}
