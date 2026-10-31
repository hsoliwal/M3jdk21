// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Create-only source-sealed delivery of the A3 V6 mastery gate. */
public final class M3A3MasteryV6Recipe
        extends ScanningRecipe<M3A3MasteryV6Recipe.Inventory> {
    private static final String ROOT = "/com/m3/rewrite/a3-mastery-v6/";
    private static final String OWNER = "src/main/java/com/m3/a3/A3.java";
    private static final Map<String, String> TARGETS =
            Map.of(
                    "src/main/java/com/m3/a3/A3Gate.java",
                    "A3Gate.java.txt",
                    "src/test/java/com/m3/a3/A3GateTest.java",
                    "A3GateTest.java.txt");

    @Override
    public String getDisplayName() {
        return "Install A3 V6 mastery gate";
    }

    @Override
    public String getDescription() {
        return "Installs the additive A3 gate that admits a caller-pinned Synexia M3 V6 mastery "
                + "receipt before delegating to the unchanged FILE-local A3Apply.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "a3",
                "atomize",
                "patternize",
                "mastery",
                "recipe-first",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                String path = normalized(source.getSourcePath());
                if (OWNER.equals(path)) {
                    inventory.ownerSeen = true;
                    return tree;
                }
                String resource = TARGETS.get(path);
                if (resource == null) return tree;
                if (!(source instanceof J.CompilationUnit)
                        || !resource(resource).equals(source.printAll())) {
                    inventory.refused = true;
                    throw new IllegalStateException(
                            "foreign A3 V6 mastery target: " + path);
                }
                inventory.observe(path);
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory,
            Collection<SourceFile> generatedInThisCycle,
            ExecutionContext context) {
        if (inventory.refused || !inventory.ownerSeen) return List.of();
        ArrayList<SourceFile> generated = new ArrayList<>();
        TARGETS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(entry -> !inventory.seen(entry.getKey()))
                .forEach(
                        entry ->
                                generated.add(
                                        parseJava(
                                                resource(entry.getValue()),
                                                entry.getKey())));
        return List.copyOf(generated);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        if (inventory.refused) {
            throw new IllegalStateException("A3 V6 mastery source refused");
        }
        return new TreeVisitor<Tree, ExecutionContext>() {};
    }

    public Set<String> targetPaths() {
        return Set.copyOf(TARGETS.keySet());
    }

    public boolean sourceMutationAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static String candidateSource(String path) {
        String resource = TARGETS.get(normalized(Path.of(path)));
        if (resource == null) {
            throw new IllegalArgumentException("unowned A3 V6 path: " + path);
        }
        return resource(resource);
    }

    private static J.CompilationUnit parseJava(String source, String path) {
        SourceFile parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parse(source)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "A3 V6 resource did not parse: " + path));
        if (!(parsed instanceof J.CompilationUnit unit)) {
            throw new IllegalStateException("A3 V6 resource is not Java: " + path);
        }
        J.CompilationUnit withPath = unit.withSourcePath(Path.of(path));
        if (!source.equals(withPath.printAll())) {
            throw new IllegalStateException(
                    "A3 V6 resource is not print-stable: " + path);
        }
        return withPath;
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static String resource(String name) {
        try (InputStream input =
                M3A3MasteryV6Recipe.class.getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 V6 mastery resource: " + name);
            }
            byte[] bytes = input.readNBytes(1_048_577);
            if (bytes.length > 1_048_576) {
                throw new IllegalStateException(
                        "A3 V6 mastery resource too large: " + name);
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (!java.util.Arrays.equals(
                    bytes, text.getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalStateException(
                        "invalid UTF-8 A3 V6 mastery resource: " + name);
            }
            return text;
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 V6 mastery resource", failure);
        }
    }

    public static final class Inventory {
        private final Set<String> seen = ConcurrentHashMap.newKeySet();
        private boolean ownerSeen;
        private boolean refused;

        synchronized void observe(String path) {
            if (!seen.add(path)) {
                throw new IllegalStateException(
                        "duplicate A3 V6 mastery target: " + path);
            }
        }

        synchronized boolean seen(String path) {
            return seen.contains(path);
        }
    }
}
