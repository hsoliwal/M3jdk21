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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
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
import org.openrewrite.text.PlainText;

/**
 * Source-sealed installer for the A3 OpenRewrite recipe catalogue.
 *
 * <p>The recipe owns only the A3 catalogue/tooling/docs packet. Existing files must match the exact
 * preimage Git blob or the reviewed postimage. New files use an explicit ABSENT preimage. The
 * recipe is candidate-only and grants no JDK product mutation or promotion authority.</p>
 */
public final class M3A3CatalogueRecipe extends ScanningRecipe<M3A3CatalogueRecipe.Inventory> {

    private static final String ROOT = "/com/m3/rewrite/a3-catalogue/";
    private static final String MANIFEST = "manifest.tsv";

    private enum Kind {
        JAVA,
        TEXT
    }

    private record Target(
            String path,
            Kind kind,
            String before,
            String after,
            String resource) {}

    public static final class Inventory {
        private final List<Target> targets;
        private final Map<String, String> seen = new HashMap<>();
        private final List<String> conflicts = new ArrayList<>();
        private Map<String, SourceFile> postimages;

        private Inventory(List<Target> targets) {
            this.targets = targets;
        }
    }

    @Override
    public String getDisplayName() {
        return "M3 A3 OpenRewrite catalogue";
    }

    @Override
    public String getDescription() {
        return "Installs the source-sealed A3 recipe/provenance catalogue and its tests/docs.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "a3",
                "openrewrite",
                "catalogue",
                "recipe-first",
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
                if (!(tree instanceof SourceFile source)) {
                    return tree;
                }
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                for (Target target : inventory.targets) {
                    if (!target.path().equals(path)) {
                        continue;
                    }
                    String hash = gitBlob(source.printAll());
                    synchronized (inventory) {
                        if (inventory.seen.putIfAbsent(path, hash) != null) {
                            inventory.conflicts.add("duplicate A3 catalogue target: " + path);
                        }
                        if (!typeMatches(source, target.kind())) {
                            inventory.conflicts.add("A3 catalogue target type drift: " + path);
                        }
                        if (!hash.equals(target.before()) && !hash.equals(target.after())) {
                            inventory.conflicts.add("A3 catalogue source drift: " + path);
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
        ArrayList<SourceFile> generated = new ArrayList<>();
        for (Target target : inventory.targets) {
            if (!inventory.seen.containsKey(target.path())) {
                if (!"ABSENT".equals(target.before())) {
                    throw new IllegalStateException(
                            "required A3 catalogue preimage missing: " + target.path());
                }
                generated.add(inventory.postimages.get(target.path()));
            }
        }
        return List.copyOf(generated);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        requireAdmissible(inventory);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) {
                    return tree;
                }
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                Target target = find(inventory.targets, path);
                if (target == null) {
                    return tree;
                }
                prepare(inventory, context);
                String current = gitBlob(source.printAll());
                String scanned = inventory.seen.get(path);
                boolean manifest = current.equals(target.before()) || current.equals(target.after());
                boolean scan = scanned != null && current.equals(scanned);
                if (!manifest || !scan || !typeMatches(source, target.kind())) {
                    throw new IllegalStateException("A3 catalogue target changed after scan: " + path);
                }
                if (current.equals(target.after())) {
                    return tree;
                }
                SourceFile postimage = inventory.postimages.get(path);
                SourceFile replacement = (SourceFile) postimage.withId(source.getId());
                return replacement.withSourcePath(source.getSourcePath())
                        .withMarkers(source.getMarkers())
                        .withFileAttributes(source.getFileAttributes())
                        .withCharset(source.getCharset())
                        .withCharsetBomMarked(source.isCharsetBomMarked())
                        .withChecksum(null);
            }
        };
    }

    public boolean productMutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static String resource(String name) {
        try (InputStream input =
                M3A3CatalogueRecipe.class.getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException("missing A3 catalogue recipe resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read A3 catalogue recipe resource", failure);
        }
    }

    static String gitBlob(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] prefix = ("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(prefix);
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void prepare(Inventory inventory, ExecutionContext context) {
        synchronized (inventory) {
            requireAdmissible(inventory);
            if (inventory.postimages != null) {
                return;
            }
            HashMap<String, SourceFile> prepared = new HashMap<>();
            for (Target target : inventory.targets) {
                prepared.put(target.path(), parse(target, context));
            }
            inventory.postimages = Map.copyOf(prepared);
        }
    }

    private static void requireAdmissible(Inventory inventory) {
        synchronized (inventory) {
            for (Target target : inventory.targets) {
                if (!"ABSENT".equals(target.before())
                        && !inventory.seen.containsKey(target.path())) {
                    throw new IllegalStateException(
                            "required A3 catalogue preimage missing: " + target.path());
                }
            }
            if (!inventory.conflicts.isEmpty()) {
                throw new IllegalStateException(String.join("; ", inventory.conflicts));
            }
        }
    }

    private static SourceFile parse(Target target, ExecutionContext context) {
        String text = resource(target.resource());
        if (!gitBlob(text).equals(target.after())) {
            throw new IllegalStateException(
                    "A3 catalogue postimage hash drift: " + target.path());
        }
        if (target.kind() == Kind.TEXT) {
            return PlainText.builder()
                    .sourcePath(Path.of(target.path()))
                    .text(text)
                    .build();
        }

        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(target.path()), text)),
                                null,
                                context)
                        .toList();
        if (parsed.size() != 1
                || !(parsed.getFirst() instanceof J.CompilationUnit)
                || !text.equals(parsed.getFirst().printAll())) {
            throw new IllegalStateException(
                    "A3 catalogue Java postimage roundtrip drift: " + target.path());
        }
        return parsed.getFirst();
    }

    private static List<Target> targets() {
        ArrayList<Target> targets = new ArrayList<>();
        HashSet<String> paths = new HashSet<>();
        String previous = "";
        for (String line : resource(MANIFEST).lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] cells = line.split("\t", -1);
            if (cells.length != 5
                    || !path(cells[0])
                    || !paths.add(cells[0])
                    || previous.compareTo(cells[0]) >= 0
                    || !("ABSENT".equals(cells[2]) || gitSha(cells[2]))
                    || !gitSha(cells[3])
                    || !cells[4].matches("[A-Za-z0-9_.-]+")) {
                throw new IllegalStateException("invalid A3 catalogue recipe manifest row");
            }
            Kind kind;
            try {
                kind = Kind.valueOf(cells[1]);
            } catch (IllegalArgumentException invalid) {
                throw new IllegalStateException("invalid A3 catalogue target kind", invalid);
            }
            targets.add(new Target(cells[0], kind, cells[2], cells[3], cells[4]));
            previous = cells[0];
        }
        if (targets.isEmpty() || targets.size() > 32) {
            throw new IllegalStateException("A3 catalogue recipe target budget");
        }
        return List.copyOf(targets);
    }

    private static boolean typeMatches(SourceFile source, Kind kind) {
        return switch (kind) {
            case JAVA -> source instanceof J.CompilationUnit;
            case TEXT -> source instanceof PlainText;
        };
    }

    private static Target find(List<Target> targets, String path) {
        for (Target target : targets) {
            if (target.path().equals(path)) {
                return target;
            }
        }
        return null;
    }

    private static boolean path(String value) {
        if (value == null
                || !value.startsWith("m3/")
                || value.startsWith("/")
                || value.indexOf('\\') >= 0
                || value.length() > 4096
                || value.chars().anyMatch(Character::isISOControl)) {
            return false;
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean gitSha(String value) {
        return value.matches("[0-9a-f]{40}");
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }
}
