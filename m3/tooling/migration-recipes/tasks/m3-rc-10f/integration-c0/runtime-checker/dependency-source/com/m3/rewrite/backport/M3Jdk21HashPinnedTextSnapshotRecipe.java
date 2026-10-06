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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/**
 * Hash-pinned whole-JDK plain-text/resource backport recipe.
 *
 * <p>Use this for non-Java portions of mixed JDK backport packets: properties, Markdown, shell
 * scripts, make metadata, configuration and other reviewed UTF-8 text. Every target must match its
 * exact JDK21 preimage or its already-applied postimage. Added targets use the explicit
 * {@code ABSENT} preimage. Automatic removals are deliberately unsupported.</p>
 */
public final class M3Jdk21HashPinnedTextSnapshotRecipe
        extends ScanningRecipe<M3Jdk21HashPinnedTextSnapshotRecipe.Inventory> {
    private static final String RESOURCE_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/";

    @Option(
            displayName = "Crate name",
            description = "Exact classpath crate directory below jdk21-hash-pinned-text.",
            example = "jdk27-javadoc-8347112")
    private final String crateName;

    private record Target(String path, String before, String after, String text) {}

    public static final class Inventory {
        private final List<Target> targets;
        private final Map<String, String> seen = new HashMap<>();
        private final List<String> conflicts = new ArrayList<>();

        private Inventory(List<Target> targets) {
            this.targets = targets;
        }
    }

    @JsonCreator
    public M3Jdk21HashPinnedTextSnapshotRecipe(
            @JsonProperty("crateName") String crateName) {
        if (crateName == null || !crateName.matches("[a-z0-9][a-z0-9-]{0,79}")) {
            throw new IllegalArgumentException("invalid M3JDK21 text backport crate");
        }
        this.crateName = crateName;
    }

    public String getCrateName() {
        return crateName;
    }

    @Override
    public String getDisplayName() {
        return "M3JDK21 hash-pinned text snapshot";
    }

    @Override
    public String getDescription() {
        return "Replays reviewed UTF-8 JDK resources only from exact JDK21 preimages or already-converged outputs.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "openrewrite",
                "hash-pinned",
                "plain-text",
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
                        if (!(file instanceof PlainText)) {
                            inventory.conflicts.add(
                                    "target is not OpenRewrite PlainText: " + path);
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
        requireAdmissible(inventory);
        List<SourceFile> generated = new ArrayList<>();
        for (Target target : inventory.targets) {
            if (!inventory.seen.containsKey(target.path())) {
                if (!"ABSENT".equals(target.before())) {
                    throw new IllegalStateException(
                            "required JDK21 text source missing: " + target.path());
                }
                generated.add(template(target));
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
                    if (!(file instanceof PlainText) || !manifest || !scan) {
                        throw new IllegalStateException(
                                "JDK21 text target changed after scan: " + path);
                    }
                    if (current.equals(target.after())) {
                        return tree;
                    }

                    SourceFile replacement = template(target)
                            .withId(file.getId())
                            .withSourcePath(file.getSourcePath())
                            .withMarkers(file.getMarkers())
                            .withFileAttributes(file.getFileAttributes())
                            .withCharset(file.getCharset())
                            .withCharsetBomMarked(file.isCharsetBomMarked())
                            .withChecksum(null);
                    return replacement;
                }
                return tree;
            }
        };
    }

    private static void requireAdmissible(Inventory inventory) {
        synchronized (inventory) {
            for (Target target : inventory.targets) {
                if (!"ABSENT".equals(target.before())
                        && !inventory.seen.containsKey(target.path())) {
                    throw new IllegalStateException(
                            "required JDK21 text source missing: " + target.path());
                }
            }
            if (!inventory.conflicts.isEmpty()) {
                throw new IllegalStateException(String.join("; ", inventory.conflicts));
            }
        }
    }

    private static PlainText template(Target target) {
        return PlainText.builder()
                .sourcePath(Path.of(target.path()))
                .text(target.text())
                .build();
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
                    || !jdkTextPath(cells[0])
                    || !paths.add(cells[0])
                    || previous.compareTo(cells[0]) >= 0
                    || !("ABSENT".equals(cells[1]) || sha(cells[1]))
                    || !sha(cells[2])
                    || !cells[3].matches("[A-Za-z0-9_.-]+")) {
                throw new IllegalStateException(
                        "invalid M3JDK21 text crate manifest row");
            }
            previous = cells[0];
            String text = resource(root + cells[3]);
            if (!sha256(text).equals(cells[2])) {
                throw new IllegalStateException(
                        "M3JDK21 text template hash drift: " + cells[0]);
            }
            targets.add(new Target(cells[0], cells[1], cells[2], text));
        }

        if (targets.isEmpty() || targets.size() > 256) {
            throw new IllegalStateException("M3JDK21 text crate target budget");
        }
        return List.copyOf(targets);
    }

    static boolean jdkTextPath(String value) {
        if (value == null
                || value.isBlank()
                || value.startsWith("/")
                || value.matches("^[A-Za-z]:/.*")
                || value.indexOf('\\') >= 0
                || value.length() > 4096
                || value.chars().anyMatch(Character::isISOControl)) {
            return false;
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty()
                    || ".".equals(part)
                    || "..".equals(part)
                    || ".git".equalsIgnoreCase(part)) {
                return false;
            }
        }
        return true;
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static boolean sha(String value) {
        return value.matches("[0-9a-f]{64}");
    }

    private static String resource(String name) {
        try (var stream =
                M3Jdk21HashPinnedTextSnapshotRecipe.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "missing M3JDK21 text recipe resource: " + name);
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(stream.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read M3JDK21 text recipe resource", failure);
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
