// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Source-bound installer for the explicit M3 primitive-collections foundation.
 *
 * <p>The production constructor refuses unless the reviewed ArrayDeque and HashMap JDK guard
 * postimages are present. It never edits java.util. It only generates absent exact M3 postimages
 * and refuses conflicting existing destinations.</p>
 */
public final class M3InstallPrimitiveCollectionsRecipe
        extends ScanningRecipe<M3InstallPrimitiveCollectionsRecipe.State> {
    private static final String ROOT = "/com/m3/rewrite/collections/after/";
    private static final Map<String, String> OUTPUTS = outputs();
    private static final Map<String, String> GUARDS = Map.of(
            "src/java.base/share/classes/java/util/ArrayDeque.java",
            "9574f633c3dd2002ca2cc29740eae7ffc8c9aede9b9f1ab88578abfe2251140e",
            "src/java.base/share/classes/java/util/HashMap.java",
            "71c8d82247c2736e9fbe3769f366e9bd250bc73ecf390749998e830201aaabf2");

    private final boolean requireGuards;

    public M3InstallPrimitiveCollectionsRecipe() {
        this(true);
    }

    M3InstallPrimitiveCollectionsRecipe(boolean requireGuards) {
        this.requireGuards = requireGuards;
    }

    public static final class State {
        final Set<String> guards = new java.util.HashSet<>();
        final Set<String> outputs = new java.util.HashSet<>();
        final List<String> failures = new ArrayList<>();
    }

    @Override
    public String getDisplayName() {
        return "Install M3 primitive collections foundation";
    }

    @Override
    public String getDescription() {
        return "Generates exact reviewed primitive collection postimages without modifying java.util.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "recipe-first", "collections", "primitive", "migration");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public State getInitialValue(ExecutionContext context) {
        return new State();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile file)) {
                    return tree;
                }
                String path = normalize(file.getSourcePath());
                String expectedGuard = GUARDS.get(path);
                if (expectedGuard != null) {
                    if (!sha256(file.printAll()).equals(expectedGuard)) {
                        state.failures.add("JDK guard drift: " + path);
                    }
                    if (!state.guards.add(path)) {
                        state.failures.add("duplicate JDK guard: " + path);
                    }
                }
                String resource = OUTPUTS.get(path);
                if (resource != null) {
                    if (!file.printAll().equals(resource(resource))) {
                        state.failures.add("conflicting M3 postimage: " + path);
                    }
                    if (!state.outputs.add(path)) {
                        state.failures.add("duplicate M3 postimage: " + path);
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(State state, ExecutionContext context) {
        validate(state);
        List<SourceFile> generated = new ArrayList<>();
        for (Map.Entry<String, String> output : OUTPUTS.entrySet()) {
            if (!state.outputs.contains(output.getKey())) {
                generated.add(parse(output.getKey(), resource(output.getValue()), context));
            }
        }
        return generated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(State state) {
        validate(state);
        return new TreeVisitor<Tree, ExecutionContext>() { };
    }

    private void validate(State state) {
        if (requireGuards) {
            for (String guard : GUARDS.keySet()) {
                if (!state.guards.contains(guard)) {
                    state.failures.add("missing JDK guard: " + guard);
                }
            }
        }
        if (!state.failures.isEmpty()) {
            state.failures.sort(String::compareTo);
            throw new IllegalStateException(String.join("; ", state.failures));
        }
    }

    private static J.CompilationUnit parse(
            String path, String source, ExecutionContext context) {
        SourceFile parsed =
                JavaParser.fromJavaVersion().build().parse(context, source).findFirst().orElseThrow();
        if (!(parsed instanceof J.CompilationUnit unit)) {
            throw new IllegalStateException("not Java LST: " + path);
        }
        unit = unit.withSourcePath(Path.of(path));
        if (!unit.printAll().equals(source)) {
            throw new IllegalStateException("parser changed postimage bytes: " + path);
        }
        return unit;
    }

    private static String normalize(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static String resource(String name) {
        try (InputStream input =
                M3InstallPrimitiveCollectionsRecipe.class.getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException("missing postimage resource: " + name);
            }
            byte[] bytes = input.readNBytes(1_048_577);
            if (bytes.length > 1_048_576) {
                throw new IllegalStateException("postimage resource budget exceeded: " + name);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Map<String, String> outputs() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        String prefix = "m3/collections/src/com/m3/collections/";
        for (String name : List.of(
                "M3LongCollection",
                "M3LongDeque",
                "M3LongIterator",
                "M3LongLongConsumer",
                "M3LongLongMap",
                "M3LongLongPredicate",
                "M3LongQueue",
                "M3PackedArrays",
                "M3PackedHashAtoms",
                "M3PackedLongDeque",
                "M3PackedLongLongHashMap",
                "M3PackedRingAtoms",
                "M3PackedSupport")) {
            map.put(prefix + name + ".java", name + ".java");
        }
        map.put("m3/collections/src/module-info.java", "module-info.java");
        return java.util.Collections.unmodifiableMap(map);
    }
}
