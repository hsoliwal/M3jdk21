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
import java.util.function.Consumer;
import org.openrewrite.DelegatingExecutionContext;
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
 * Reusable, candidate-only Java migration from an exact source snapshot to reviewed Java LSTs.
 *
 * <p>Each crate supplies a manifest and UTF-8 templates as classpath resources. All target
 * preimages and all output templates are admitted before any output is produced. Hashes cover
 * rendered Java text encoded as UTF-8, not the original on-disk byte encoding. Replacements
 * preserve the input charset and BOM flag and discard its obsolete checksum. This recipe checks source identity and
 * syntax; the sealed M3 contract, coverage, compiler, test and runtime gates remain authoritative.
 * An optional bounded parser-dependencies resource supplies exact source context and refuses parser
 * callbacks before candidate publication; crates without that resource retain the default parser policy.
 */
public final class M3HashPinnedJavaSnapshotRecipe
        extends ScanningRecipe<M3HashPinnedJavaSnapshotRecipe.Inventory> {
    private static final String RESOURCE_ROOT = "/com/synexia/rewrite/hash-pinned-java/";
    private static final int MAX_PARSER_DEPENDENCIES = 16;
    private static final int MAX_PARSER_DEPENDENCY_MANIFEST_BYTES = 65_536;
    private static final int MAX_PARSER_DEPENDENCY_BYTES = 262_144;

    @Option(
            displayName = "Crate name",
            description = "Exact classpath crate directory below hash-pinned-java.",
            example = "sealed-readonly-requirement")
    private final String crateName;

    private record Target(String path, String before, String after, String text) { }
    private record ParserDependency(String path, String text) { }

    public static final class Inventory {
        private final List<Target> targets;
        private final List<ParserDependency> parserDependencies;
        private final Map<String, String> seen = new HashMap<>();
        private final List<String> conflicts = new ArrayList<>();
        private Map<String, SourceFile> candidates;

        private Inventory(List<Target> targets, List<ParserDependency> parserDependencies) {
            this.targets = targets;
            this.parserDependencies = parserDependencies;
        }
    }

    @JsonCreator
    public M3HashPinnedJavaSnapshotRecipe(@JsonProperty("crateName") String crateName) {
        if (crateName == null || !crateName.matches("[a-z0-9][a-z0-9-]{0,79}")) {
            throw new IllegalArgumentException("invalid hash-pinned Java crate");
        }
        this.crateName = crateName;
    }

    @Override public String getDisplayName() {
        return "M3 hash-pinned Java snapshot candidate";
    }

    @Override public String getDescription() {
        return "Reconstructs reviewed Java LST candidates only when every source matches "
                + "the crate's exact SHA-256 preimage or already matches its SHA-256 output.";
    }

    @Override public Set<String> getTags() {
        return Set.of("synexia", "m3", "openrewrite", "recipe-first", "candidate-only");
    }

    @Override public int maxCycles() { return 1; }

    @Override public Inventory getInitialValue(ExecutionContext context) {
        List<Target> targets = targets();
        return new Inventory(targets, parserDependencies(targets));
    }

    @Override public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext context) {
                if (tree instanceof SourceFile file) {
                    stopAfterPreVisit();
                    String path = normalized(file.getSourcePath());
                    for (Target target : inventory.targets) {
                        if (!target.path().equals(path)) continue;
                        String hash = sha256(file.printAll());
                        synchronized (inventory) {
                            if (!(file instanceof J.CompilationUnit)) {
                                inventory.conflicts.add("target is not a Java compilation unit: " + path);
                            }
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

    @Override public Collection<? extends SourceFile> generate(
            Inventory inventory, ExecutionContext context) {
        prepare(inventory, context);
        List<SourceFile> generated = new ArrayList<>();
        for (Target target : inventory.targets) {
            if (!inventory.seen.containsKey(target.path())) {
                generated.add(inventory.candidates.get(target.path()));
            }
        }
        return generated;
    }

    @Override public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        requireAdmissible(inventory);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext context) {
                if (tree instanceof SourceFile file) {
                    prepare(inventory, context);
                    stopAfterPreVisit();
                    String path = normalized(file.getSourcePath());
                    for (Target target : inventory.targets) {
                        if (target.path().equals(path)) {
                            String current = sha256(file.printAll());
                            String scanned = inventory.seen.get(path);
                            boolean matchesManifest = current.equals(target.before())
                                    || current.equals(target.after());
                            boolean matchesScan = scanned == null
                                    ? target.before().equals("ABSENT")
                                            && current.equals(target.after())
                                    : current.equals(scanned);
                            if (!(file instanceof J.CompilationUnit)
                                    || !matchesManifest || !matchesScan) {
                                throw new IllegalStateException("target changed after scan: " + path);
                            }
                            if (current.equals(target.after())) return tree;
                            SourceFile parsed = inventory.candidates.get(target.path());
                            SourceFile replacement = parsed.withId(file.getId());
                            replacement = replacement.withSourcePath(file.getSourcePath());
                            replacement = replacement.withMarkers(file.getMarkers());
                            replacement = replacement.withFileAttributes(file.getFileAttributes());
                            replacement = replacement.withCharset(file.getCharset());
                            replacement = replacement.withCharsetBomMarked(file.isCharsetBomMarked());
                            return replacement.withChecksum(null);
                        }
                    }
                }
                return tree;
            }
        };
    }

    public String getCrateName() { return crateName; }

    /** Exact, sorted target paths sealed by this recipe crate. */
    public List<String> targetPaths() {
        return targets().stream().map(Target::path).toList();
    }

    /** Narrowest physical edit scope implied by the crate's exact Java target set. */
    public M3EditScope requiredScope() {
        return M3ScopeInference.inferJavaTargets(targetPaths());
    }

    /** Exact path -> expected preimage SHA-256 (or ABSENT for additive targets). */
    public Map<String, String> preimageManifest() {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (Target target : targets()) {
            result.put(target.path(), target.before());
        }
        return Map.copyOf(result);
    }

    private static void prepare(Inventory inventory, ExecutionContext context) {
        synchronized (inventory) {
            requireAdmissible(inventory);
            if (inventory.candidates == null) {
                validateParserDependencies(inventory.parserDependencies, context);
                Map<String, SourceFile> prepared = new HashMap<>();
                for (Target target : inventory.targets) {
                    prepared.put(target.path(), parse(target, context, inventory.parserDependencies));
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
                    throw new IllegalStateException("required source missing: " + target.path());
                }
            }
            if (!inventory.conflicts.isEmpty()) {
                throw new IllegalStateException(String.join("; ", inventory.conflicts));
            }
        }
    }

    private static SourceFile parse(
            Target target, ExecutionContext context, List<ParserDependency> dependencies) {
        JavaParser.Builder<?, ?> builder = JavaParser.fromJavaVersion();
        if (!dependencies.isEmpty()) builder.dependsOn(dependencyInputs(dependencies));
        JavaParser parser = builder.build();
        List<Parser.Input> inputs = List.of(Parser.Input.fromString(Path.of(target.path()), target.text()));
        List<SourceFile> parsed = dependencies.isEmpty()
                ? parser.parseInputs(inputs, null, context).toList()
                : parseChecked(parser, inputs, context, target.path());
        if (parsed.size() != 1 || !(parsed.get(0) instanceof J.CompilationUnit)
                || !target.text().equals(parsed.get(0).printAll())) {
            throw new IllegalStateException("Java template parse/format drift: " + target.path());
        }
        return parsed.get(0);
    }

    private List<ParserDependency> parserDependencies(List<Target> targets) {
        String root = RESOURCE_ROOT + crateName + "/";
        String manifest = dependencyResource(root + "parser-dependencies.tsv",
                MAX_PARSER_DEPENDENCY_MANIFEST_BYTES, true);
        if (manifest == null) return List.of();
        Set<String> reservedPaths = new HashSet<>();
        Set<String> reservedTypes = new HashSet<>();
        for (Target target : targets) {
            reservedPaths.add(target.path());
            reservedTypes.add(javaIdentity(target.path()));
        }
        Set<String> resources = new HashSet<>();
        List<ParserDependency> dependencies = new ArrayList<>();
        String previous = "";
        int remaining = MAX_PARSER_DEPENDENCY_BYTES;
        for (String line : manifest.lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 3 || !javaPath(cells[0]) || previous.compareTo(cells[0]) >= 0
                    || !reservedPaths.add(cells[0]) || !reservedTypes.add(javaIdentity(cells[0]))
                    || !sha(cells[1]) || cells[2].length() > 256
                    || !cells[2].matches("[A-Za-z0-9_][A-Za-z0-9_.-]*\\.java\\.txt")
                    || !resources.add(cells[2])) {
                throw new IllegalStateException("invalid Java parser dependency manifest row");
            }
            if (dependencies.size() == MAX_PARSER_DEPENDENCIES) {
                throw new IllegalStateException("Java parser dependency count budget");
            }
            String text = dependencyResource(root + cells[2], remaining, false);
            if (text.isBlank() || !sha256(text).equals(cells[1])) {
                throw new IllegalStateException("Java parser dependency hash/content drift: " + cells[0]);
            }
            remaining -= text.getBytes(StandardCharsets.UTF_8).length;
            dependencies.add(new ParserDependency(cells[0], text));
            previous = cells[0];
        }
        if (dependencies.isEmpty()) throw new IllegalStateException("empty Java parser dependency manifest");
        return List.copyOf(dependencies);
    }

    private static String javaIdentity(String path) {
        return M3ScopeInference.describeJavaTarget(path).packageName() + "."
                + Path.of(path).getFileName();
    }

    private static List<Parser.Input> dependencyInputs(List<ParserDependency> dependencies) {
        return dependencies.stream()
                .map(dependency -> Parser.Input.fromString(Path.of(dependency.path()), dependency.text()))
                .toList();
    }

    private static void validateParserDependencies(
            List<ParserDependency> dependencies, ExecutionContext context) {
        if (dependencies.isEmpty()) return;
        List<SourceFile> parsed = parseChecked(JavaParser.fromJavaVersion().build(),
                dependencyInputs(dependencies), context, "parser dependencies");
        Map<String, String> expected = new HashMap<>();
        for (ParserDependency dependency : dependencies) expected.put(dependency.path(), dependency.text());
        Set<String> seen = new HashSet<>();
        for (SourceFile source : parsed) {
            String path = normalized(source.getSourcePath());
            if (!(source instanceof J.CompilationUnit) || !seen.add(path)
                    || !source.printAll().equals(expected.get(path))) {
                throw new IllegalStateException("Java parser dependency parse/format drift: " + path);
            }
        }
        if (!seen.equals(expected.keySet())) {
            throw new IllegalStateException("incomplete Java parser dependency parse");
        }
    }

    private static List<SourceFile> parseChecked(
            JavaParser parser, List<Parser.Input> inputs, ExecutionContext context, String description) {
        List<Throwable> errors = new ArrayList<>();
        ExecutionContext checked = new DelegatingExecutionContext(context) {
            @Override public Consumer<Throwable> getOnError() {
                return error -> {
                    errors.add(error);
                    context.getOnError().accept(error);
                };
            }
        };
        List<SourceFile> parsed = parser.parseInputs(inputs, null, checked).toList();
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Java parser failed while preparing " + description, errors.getFirst());
        }
        return parsed;
    }

    private static String dependencyResource(String name, int maxBytes, boolean optional) {
        try (var stream = M3HashPinnedJavaSnapshotRecipe.class.getResourceAsStream(name)) {
            if (stream == null) {
                if (optional) return null;
                throw new IllegalStateException("missing Java parser dependency resource: " + name);
            }
            byte[] bytes = stream.readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) throw new IllegalStateException("Java parser dependency byte budget");
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if (text.indexOf('\0') >= 0) throw new IllegalStateException("NUL in Java parser dependency resource");
            return text;
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read Java parser dependency resource", failure);
        }
    }

    private List<Target> targets() {
        String root = RESOURCE_ROOT + crateName + "/";
        List<Target> targets = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        String previousPath = "";
        for (String line : resource(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 4 || !javaPath(cells[0]) || !paths.add(cells[0])
                    || previousPath.compareTo(cells[0]) >= 0
                    || !("ABSENT".equals(cells[1]) || sha(cells[1]))
                    || !sha(cells[2]) || !cells[3].matches("[A-Za-z0-9_.-]+")) {
                throw new IllegalStateException("invalid hash-pinned Java manifest row");
            }
            previousPath = cells[0];
            String text = resource(root + cells[3]);
            if (!sha256(text).equals(cells[2])) {
                throw new IllegalStateException("Java template hash drift: " + cells[0]);
            }
            targets.add(new Target(cells[0], cells[1], cells[2], text));
        }
        if (targets.isEmpty() || targets.size() > 256) {
            throw new IllegalStateException("hash-pinned Java target budget");
        }
        return List.copyOf(targets);
    }

    private static boolean javaPath(String value) {
        int srcSegment = value.startsWith("src/") ? 0 : value.indexOf("/src/");
        int javaSegment = srcSegment < 0 ? -1 : value.indexOf("/java/", srcSegment + 1);
        boolean mavenSource = srcSegment >= 0 && javaSegment > srcSegment;
        boolean eclipsePdeSource =
                value.startsWith("bundles/") && value.contains("/src/");
        boolean eclipsePdeTestSource =
                value.startsWith("tests/") && value.contains("/src/");
        boolean eclipseNebulaWidgetSource =
                value.startsWith("widgets/") && value.contains("/src/");
        boolean eclipseSwtLegacySource = eclipseSwtPdePath(value);
        boolean eclipseSwtExampleSource = value.startsWith(
                "examples/org.eclipse.swt.opengl.examples/src/org/eclipse/swt/opengl/examples/");
        boolean eclipseSwtTestSource =
                value.startsWith("tests/org.eclipse.swt.tests/")
                        && (value.contains("/JUnit Tests/") || value.contains("/ManualTests/"));
        if ((!mavenSource && !eclipsePdeSource && !eclipsePdeTestSource && !eclipseNebulaWidgetSource
                && !eclipseSwtLegacySource && !eclipseSwtTestSource && !eclipseSwtExampleSource)
                || !value.endsWith(".java")
                || value.startsWith("/")
                || value.indexOf('\\') >= 0
                || value.length() > 4096) {
            return false;
        }
        for (String part : value.split("/", -1)) {
            if (part.isBlank() || part.equals(".") || part.equals("..")
                    || part.chars().anyMatch(Character::isISOControl)) return false;
        }
        return true;
    }

    private static boolean eclipseSwtPdePath(String value) {
        if (!value.startsWith("bundles/")) return false;
        for (String part : value.split("/", -1)) {
            if (part.startsWith("Eclipse SWT")) return true;
        }
        return false;
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static boolean sha(String value) {
        return value.matches("[0-9a-f]{64}");
    }

    private static String resource(String name) {
        try (var stream = M3HashPinnedJavaSnapshotRecipe.class.getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("missing Java recipe resource: " + name);
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(stream.readAllBytes())).toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read Java recipe resource", failure);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
