// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Hash-pinned OpenJDK21-to-donor Java recipe crate.
 *
 * <p>Generated crates contain exact JDK21 preimage hashes and reviewed donor Java postimages. The
 * entire crate is admitted before mutation: source drift, missing required files, invalid Java 21
 * parsing or template round-trip drift fail closed. Added Java files use the explicit ABSENT
 * preimage. Deletions are intentionally not automatic in this recipe.
 */
public final class M3Jdk21HashPinnedSnapshotRecipe
        extends ScanningRecipe<M3Jdk21HashPinnedSnapshotRecipe.Inventory> {
    private static final String RESOURCE_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/";

    @Option(
            displayName = "Crate name",
            description = "Generated exact donor crate below jdk21-hash-pinned.",
            example = "jdk27-0001")
    private final String crateName;

    private record Target(String path, String before, String after, String text) {}

    public static final class Inventory {
        private final List<Target> targets;
        private final Map<String, String> seen = new HashMap<>();
        private final List<String> conflicts = new ArrayList<>();
        private Map<String, SourceFile> candidates;

        private Inventory(List<Target> targets) {
            this.targets = targets;
        }
    }

    @JsonCreator
    public M3Jdk21HashPinnedSnapshotRecipe(@JsonProperty("crateName") String crateName) {
        if (crateName == null
                || !crateName.matches(
                        "(?:jdk(?:22|23|24|25|26|27)|synexia)-[a-z0-9][a-z0-9-]{0,63}")) {
            throw new IllegalArgumentException("invalid M3JDK21 Java snapshot crate");
        }
        this.crateName = crateName;
    }

    public String getCrateName() {
        return crateName;
    }

    @Override
    public String getDisplayName() {
        return "M3JDK21 hash-pinned Java snapshot";
    }

    @Override
    public String getDescription() {
        return "Replays one reviewed OpenJDK donor or Apache-2.0 Synexia crate only from exact JDK21 preimages or already-converged outputs.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "openrewrite",
                "hash-pinned",
                "candidate-only",
                "fail-closed");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        return new Inventory(targets());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile file)) {
                    return tree;
                }
                stopAfterPreVisit();
                String path = normalized(file.getSourcePath());
                for (Target target : inventory.targets) {
                    if (!target.path().equals(path)) {
                        continue;
                    }
                    String hash = sha256(file.printAll());
                    synchronized (inventory) {
                        if (!(file instanceof J.CompilationUnit)) {
                            inventory.conflicts.add(
                                    "target is not a Java compilation unit: " + path);
                        }
                        if (inventory.seen.putIfAbsent(path, hash) != null) {
                            inventory.conflicts.add("duplicate target: " + path);
                        }
                        if (!hash.equals(target.before()) && !hash.equals(target.after())) {
                            inventory.conflicts.add("source drift: " + path);
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory, ExecutionContext context) {
        prepare(inventory, context);
        List<SourceFile> generated = new ArrayList<>();
        for (Target target : inventory.targets) {
            if (!inventory.seen.containsKey(target.path())) {
                if (!"ABSENT".equals(target.before())) {
                    throw new IllegalStateException(
                            "required JDK21 source missing: " + target.path());
                }
                generated.add(inventory.candidates.get(target.path()));
            }
        }
        return generated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        requireAdmissible(inventory);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile file)) {
                    return tree;
                }
                prepare(inventory, context);
                stopAfterPreVisit();
                String path = normalized(file.getSourcePath());
                for (Target target : inventory.targets) {
                    if (!target.path().equals(path)) {
                        continue;
                    }
                    String current = sha256(file.printAll());
                    String scanned = inventory.seen.get(path);
                    boolean manifest = current.equals(target.before())
                            || current.equals(target.after());
                    boolean scan = scanned == null
                            ? "ABSENT".equals(target.before())
                                    && current.equals(target.after())
                            : current.equals(scanned);
                    if (!(file instanceof J.CompilationUnit) || !manifest || !scan) {
                        throw new IllegalStateException(
                                "JDK21 target changed after scan: " + path);
                    }
                    if (current.equals(target.after())) {
                        return tree;
                    }
                    SourceFile parsed = inventory.candidates.get(target.path());
                    SourceFile replacement = parsed.withId(file.getId());
                    replacement = replacement.withSourcePath(file.getSourcePath());
                    replacement = replacement.withMarkers(file.getMarkers());
                    replacement = replacement.withFileAttributes(file.getFileAttributes());
                    replacement = replacement.withCharset(file.getCharset());
                    replacement = replacement.withCharsetBomMarked(file.isCharsetBomMarked());
                    return replacement.withChecksum(null);
                }
                return tree;
            }
        };
    }

    private static void prepare(Inventory inventory, ExecutionContext context) {
        synchronized (inventory) {
            requireAdmissible(inventory);
            if (inventory.candidates != null) {
                return;
            }
            Map<String, SourceFile> prepared = new HashMap<>();
            for (Target target : inventory.targets) {
                prepared.put(target.path(), parse(target, context));
            }
            inventory.candidates = Map.copyOf(prepared);
        }
    }

    private static void requireAdmissible(Inventory inventory) {
        synchronized (inventory) {
            for (Target target : inventory.targets) {
                if (!"ABSENT".equals(target.before())
                        && !inventory.seen.containsKey(target.path())) {
                    throw new IllegalStateException(
                            "required JDK21 source missing: " + target.path());
                }
            }
            if (!inventory.conflicts.isEmpty()) {
                throw new IllegalStateException(String.join("; ", inventory.conflicts));
            }
        }
    }

    private static SourceFile parse(Target target, ExecutionContext context) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(
                                Path.of(target.path()), target.text())),
                        null,
                        context)
                .toList();
        if (parsed.size() != 1
                || !(parsed.getFirst() instanceof J.CompilationUnit)
                || !target.text().equals(parsed.getFirst().printAll())) {
            throw new IllegalStateException(
                    "Java21 donor parse/roundtrip drift: " + target.path());
        }
        return parsed.getFirst();
    }

    private List<Target> targets() {
        String root = RESOURCE_ROOT + crateName + "/";
        List<Target> targets = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        String previous = "";

        for (String line : resource(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] cells = line.split("\t", -1);
            if (cells.length != 4
                    || !jdkJavaPath(cells[0])
                    || (crateName.startsWith("synexia-") && !synexiaReceiverJavaPath(cells[0]))
                    || !paths.add(cells[0])
                    || previous.compareTo(cells[0]) >= 0
                    || !("ABSENT".equals(cells[1]) || sha(cells[1]))
                    || !sha(cells[2])
                    || !cells[3].matches("[A-Za-z0-9_.-]+")) {
                throw new IllegalStateException("invalid M3JDK21 crate manifest row");
            }
            previous = cells[0];
            String text = resource(root + cells[3]);
            if (!sha256(text).equals(cells[2])) {
                throw new IllegalStateException(
                        "M3JDK21 donor template hash drift: " + cells[0]);
            }
            targets.add(new Target(cells[0], cells[1], cells[2], text));
        }

        if (targets.isEmpty() || targets.size() > 256) {
            throw new IllegalStateException("M3JDK21 crate target budget");
        }
        return List.copyOf(targets);
    }

    static boolean jdkJavaPath(String value) {
        if (value == null
                || !(value.startsWith("src/")
                        || value.startsWith("test/")
                        || value.startsWith("m3/ports/")
                        || value.startsWith(".m3/openrewrite-recipes/src/main/java/")
                        || value.startsWith(".m3/openrewrite-recipes/src/test/java/")
                        || value.startsWith("m3/tooling/migration-recipes/src/main/java/")
                        || value.startsWith("m3/tooling/migration-recipes/src/test/java/")
                        || value.startsWith("m3/tooling/a3/src/main/java/")
                        || value.startsWith("m3/tooling/a3/src/test/java/"))
                || !value.endsWith(".java")
                || value.indexOf('\\') >= 0
                || value.length() > 4096) {
            return false;
        }
        for (String part : value.split("/", -1)) {
            if (!part.matches("[A-Za-z0-9_$.-]+")
                    || ".".equals(part)
                    || "..".equals(part)) {
                return false;
            }
        }
        return value.chars().noneMatch(Character::isISOControl);
    }

    static boolean synexiaReceiverJavaPath(String value) {
        if (!jdkJavaPath(value) || value.startsWith("src/") || value.startsWith("test/")) {
            return false;
        }
        return value.startsWith(
                        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/")
                || value.startsWith(
                        "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/")
                || value.startsWith("m3/tooling/a3/src/main/java/")
                || value.startsWith("m3/tooling/a3/src/test/java/")
                || value.startsWith(
                        ".m3/openrewrite-recipes/src/main/java/com/synexia/m3/bootstrap/")
                || value.startsWith(
                        ".m3/openrewrite-recipes/src/test/java/com/synexia/m3/bootstrap/");
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static boolean sha(String value) {
        return value.matches("[0-9a-f]{64}");
    }

    private static String resource(String name) {
        try (var stream =
                M3Jdk21HashPinnedSnapshotRecipe.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "missing M3JDK21 recipe resource: " + name);
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(stream.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read M3JDK21 recipe resource", failure);
        }
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
