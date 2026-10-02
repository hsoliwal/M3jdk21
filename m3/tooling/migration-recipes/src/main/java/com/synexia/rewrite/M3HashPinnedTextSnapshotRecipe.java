// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

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
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/**
 * Reusable candidate-only text migration from an exact source snapshot to reviewed text.
 *
 * <p>Each crate supplies a manifest and UTF-8 postimage templates. Every existing target must
 * match its exact SHA-256 preimage or already match its exact SHA-256 postimage. Missing targets
 * are allowed only when the manifest explicitly declares an {@code ABSENT} preimage. The recipe
 * never deletes files.</p>
 */
public final class M3HashPinnedTextSnapshotRecipe
        extends ScanningRecipe<M3HashPinnedTextSnapshotRecipe.Inventory> {
    private static final String RESOURCE_ROOT = "/com/synexia/rewrite/hash-pinned-text/";

    @Option(
            displayName = "Crate name",
            description = "Exact classpath crate directory below hash-pinned-text.",
            example = "jdk-8357439-jcmd-completion")
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
    public M3HashPinnedTextSnapshotRecipe(@JsonProperty("crateName") String crateName) {
        if (crateName == null || !crateName.matches("[a-z0-9][a-z0-9-]{0,79}")) {
            throw new IllegalArgumentException("invalid hash-pinned text crate");
        }
        this.crateName = crateName;
    }

    @Override
    public String getDisplayName() {
        return "M3 hash-pinned text snapshot candidate";
    }

    @Override
    public String getDescription() {
        return "Creates or replaces reviewed text only when every existing target matches "
                + "the crate's exact SHA-256 preimage or exact SHA-256 output.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "openrewrite",
                "text",
                "hash-pinned",
                "recipe-first",
                "candidate-only");
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
                if (tree instanceof SourceFile file) {
                    stopAfterPreVisit();
                    String path = normalized(file.getSourcePath());
                    for (Target target : inventory.targets) {
                        if (!target.path().equals(path)) continue;
                        String hash = sha256(file.printAll());
                        synchronized (inventory) {
                            if (inventory.seen.putIfAbsent(path, hash) != null) {
                                inventory.conflicts.add("duplicate target: " + path);
                            }
                            if (!hash.equals(target.before()) && !hash.equals(target.after())) {
                                inventory.conflicts.add("source drift: " + path);
                            }
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory,
            ExecutionContext context) {
        prepare(inventory, context);
        List<SourceFile> generated = new ArrayList<>();
        for (Target target : inventory.targets) {
            if (!inventory.seen.containsKey(target.path())) {
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
                if (tree instanceof SourceFile file) {
                    prepare(inventory, context);
                    stopAfterPreVisit();
                    String path = normalized(file.getSourcePath());
                    for (Target target : inventory.targets) {
                        if (!target.path().equals(path)) continue;
                        String current = sha256(file.printAll());
                        String scanned = inventory.seen.get(path);
                        boolean matchesManifest =
                                current.equals(target.before()) || current.equals(target.after());
                        boolean matchesScan = scanned == null
                                ? target.before().equals("ABSENT") && current.equals(target.after())
                                : current.equals(scanned);
                        if (!matchesManifest || !matchesScan) {
                            throw new IllegalStateException(
                                    "text target changed after scan: " + path);
                        }
                        if (current.equals(target.after())) return tree;

                        PlainText replacement = PlainTextParser.convert(file)
                                .withText(target.text());
                        return replacement.withChecksum(null);
                    }
                }
                return tree;
            }
        };
    }

    public String getCrateName() {
        return crateName;
    }

    private static void prepare(Inventory inventory, ExecutionContext context) {
        synchronized (inventory) {
            requireAdmissible(inventory);
            if (inventory.candidates == null) {
                Map<String, SourceFile> prepared = new HashMap<>();
                for (Target target : inventory.targets) {
                    prepared.put(target.path(), parse(target, context));
                }
                inventory.candidates = Map.copyOf(prepared);
            }
        }
    }

    private static void requireAdmissible(Inventory inventory) {
        synchronized (inventory) {
            for (Target target : inventory.targets) {
                if (!target.before().equals("ABSENT")
                        && !inventory.seen.containsKey(target.path())) {
                    throw new IllegalStateException(
                            "required text source missing: " + target.path());
                }
            }
            if (!inventory.conflicts.isEmpty()) {
                throw new IllegalStateException(String.join("; ", inventory.conflicts));
            }
        }
    }

    private static SourceFile parse(Target target, ExecutionContext context) {
        List<SourceFile> parsed = PlainTextParser.builder()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(target.path()), target.text())),
                        null,
                        context)
                .toList();
        if (parsed.size() != 1
                || !(parsed.getFirst() instanceof PlainText)
                || !target.text().equals(parsed.getFirst().printAll())) {
            throw new IllegalStateException(
                    "text template parse/format drift: " + target.path());
        }
        return parsed.getFirst();
    }

    private List<Target> targets() {
        String root = RESOURCE_ROOT + crateName + "/";
        List<Target> targets = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        String previousPath = "";
        for (String line : resource(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 4
                    || !productPath(cells[0])
                    || !paths.add(cells[0])
                    || previousPath.compareTo(cells[0]) >= 0
                    || !("ABSENT".equals(cells[1]) || sha(cells[1]))
                    || !sha(cells[2])
                    || !cells[3].matches("[A-Za-z0-9_.-]+")) {
                throw new IllegalStateException(
                        "invalid hash-pinned text manifest row");
            }
            previousPath = cells[0];
            String text = resource(root + cells[3]);
            if (!sha256(text).equals(cells[2])) {
                throw new IllegalStateException(
                        "text template hash drift: " + cells[0]);
            }
            targets.add(new Target(cells[0], cells[1], cells[2], text));
        }
        if (targets.isEmpty() || targets.size() > 256) {
            throw new IllegalStateException("hash-pinned text target budget");
        }
        return List.copyOf(targets);
    }

    private static boolean productPath(String value) {
        boolean admittedRoot = value.startsWith("src/")
                || value.startsWith("test/")
                || value.startsWith("make/")
                || value.startsWith("doc/");
        if (!admittedRoot
                || value.indexOf('\\') >= 0
                || value.startsWith("/")
                || value.length() > 4096) {
            return false;
        }
        for (String part : value.split("/", -1)) {
            if (part.isBlank() || part.equals(".") || part.equals("..")) return false;
        }
        return value.chars().noneMatch(Character::isISOControl);
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static boolean sha(String value) {
        return value.matches("[0-9a-f]{64}");
    }

    private static String resource(String name) {
        try (var stream =
                M3HashPinnedTextSnapshotRecipe.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "missing text recipe resource: " + name);
            }
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(stream.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read text recipe resource",
                    failure);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
