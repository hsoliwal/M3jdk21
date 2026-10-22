// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Bounded filesystem adapter for the existing package recipe, with exact candidate admission. */
public final class M3CollectionsPackageRenameCli {
    private static final int BATCH_SIZE = 32;
    private record Input(String path, String hash) { }
    private record Change(String beforePath, String afterPath, String before, String after) { }
    private M3CollectionsPackageRenameCli() { }

    public static void main(String[] args) throws IOException {
        if (args.length == 6 && args[0].equals("stage")) {
            stage(Path.of(args[1]), args[2], Path.of(args[3]), Path.of(args[4]), Path.of(args[5]));
        } else if (args.length == 5 && args[0].equals("stage-eclipse-imports")) {
            stage(Path.of(args[1]), "eclipse", new M3CollectionsPackageRenameImportOrderRecipe(),
                    Path.of(args[2]), Path.of(args[3]), Path.of(args[4]));
        } else if (args.length == 3 && args[0].equals("promote")) {
            promote(Path.of(args[1]), Path.of(args[2]));
        } else {
            throw new IllegalArgumentException("stage ROOT FAMILY INPUT_TSV CANDIDATE_DIR CLASSPATH_FILE | stage-eclipse-imports ROOT INPUT_TSV CANDIDATE_DIR CLASSPATH_FILE | promote ROOT CANDIDATE_DIR");
        }
    }

    static void stage(Path root, String family, Path inventory, Path output, Path classpathFile) throws IOException {
        stage(root, family, new M3CollectionsPackageRenameRecipe(family), inventory, output, classpathFile);
    }

    private static void stage(Path root, String family, Recipe recipe, Path inventory, Path output, Path classpathFile) throws IOException {
        root = root.toRealPath();
        List<Input> inputs = readInventory(inventory);
        if (Files.exists(output)) throw new IllegalArgumentException("candidate directory already exists: " + output);
        Files.createDirectories(output);
        String cp = Files.readString(classpathFile).strip();
        List<Path> classpath = cp.isEmpty() ? List.of() :
                java.util.Arrays.stream(cp.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))).map(Path::of).toList();
        List<Change> changes = new ArrayList<>();
        Map<String, String> destinations = new HashMap<>();
        String parserImplementationIdentity = null;
        for (int offset = 0; offset < inputs.size(); offset += BATCH_SIZE) {
            List<Input> batch = inputs.subList(offset, Math.min(offset + BATCH_SIZE, inputs.size()));
            List<Parser.Input> parserInputs = new ArrayList<>();
            Map<String, String> beforeTexts = new HashMap<>();
            for (Input input : batch) {
                String content = Files.readString(source(root, input.path()));
                if (!sha256(content).equals(input.hash())) throw new IllegalStateException("preimage drift: " + input.path());
                beforeTexts.put(input.path(), content);
                parserInputs.add(Parser.Input.fromString(Path.of(input.path()), content));
            }
            List<Throwable> failures = new ArrayList<>();
            var context = new InMemoryExecutionContext(failures::add);
            var parser = JavaParser.fromJavaVersion().classpath(classpath).build();
            String actualParserIdentity = providerIdentity(parser.getClass());
            if (parserImplementationIdentity != null && !parserImplementationIdentity.equals(actualParserIdentity)) {
                throw new IllegalStateException("Parser implementation changed between admitted batches");
            }
            parserImplementationIdentity = actualParserIdentity;
            List<SourceFile> parsed = parser.parseInputs(parserInputs, null, context).toList();
            requireNoFailures(failures, "parse", offset);
            if (parsed.size() != batch.size()) throw new IllegalStateException("parse count mismatch at " + offset);
            for (SourceFile file : parsed) {
                if (!(file instanceof J.CompilationUnit)
                        || !file.printAll().equals(beforeTexts.get(normalized(file.getSourcePath())))) {
                    throw new IllegalStateException("parse/print drift: " + file.getSourcePath());
                }
            }
            var results = recipe.run(new InMemoryLargeSourceSet(parsed), context).getChangeset().getAllResults();
            requireNoFailures(failures, "rename", offset);
            Map<String, SourceFile> afterByBefore = new HashMap<>();
            for (var result : results) {
                SourceFile before = result.getBefore();
                SourceFile after = result.getAfter();
                if (before == null || after == null) throw new IllegalStateException("unexpected source creation/deletion");
                String beforePath = normalized(before.getSourcePath());
                String afterPath = normalized(after.getSourcePath());
                requireRelative(afterPath);
                String afterText = after.printAll();
                String postHash = sha256(afterText);
                // Destination identity is a path, not just a content hash. Two distinct
                // source files must never collapse into one file even when bytes match.
                String occupied = destinations.putIfAbsent(afterPath, beforePath);
                if (occupied != null) {
                    throw new IllegalStateException("duplicate candidate destination: "
                            + afterPath + " from " + occupied + " and " + beforePath);
                }
                Path candidate = output.resolve("sources").resolve(afterPath);
                Files.createDirectories(candidate.getParent());
                Files.writeString(candidate, afterText);
                changes.add(new Change(beforePath, afterPath, sha256(before.printAll()), postHash));
                afterByBefore.put(beforePath, after);
            }
            List<SourceFile> afterSources = parsed.stream()
                    .map(file -> afterByBefore.getOrDefault(normalized(file.getSourcePath()), file)).toList();
            if (!recipe.run(new InMemoryLargeSourceSet(afterSources), context).getChangeset().getAllResults().isEmpty()) {
                throw new IllegalStateException("recipe did not reach an unchanged fixed point at " + offset);
            }
            requireNoFailures(failures, "fixed point", offset);
            System.out.println("SDK_EXECUTION parsed=" + (offset + batch.size()) + "/" + inputs.size() + " changed=" + changes.size());
        }
        changes.sort(java.util.Comparator.comparing(Change::beforePath));
        StringBuilder rows = new StringBuilder("source_path\tdestination_path\tpreimage_sha256\tpostimage_sha256\n");
        for (Change c : changes) rows.append(c.beforePath()).append('\t').append(c.afterPath()).append('\t')
                .append(c.before()).append('\t').append(c.after()).append('\n');
        Files.writeString(output.resolve("CHANGES.tsv"), rows);
        Files.copy(inventory, output.resolve("INPUT.tsv"));
        Files.writeString(output.resolve("SDK_EXECUTION.tsv"),
                "key\tvalue\nrecipe\t" + recipe.getClass().getName() + "\nfamily\t" + family
                + "\nprovider\t" + providerIdentity(JavaParser.class)
                + "\nparser_implementation\t" + parserImplementationIdentity + "\njava\t" + System.getProperty("java.version")
                + "\ninputs\t" + inputs.size() + "\nchanged\t" + changes.size()
                + "\nbatch_size\t" + BATCH_SIZE + "\nfixed_point\tPASS\nproduction_compile\tNOT_CLAIMED\n");
        System.out.println("STAGED SDK_EXECUTION files=" + inputs.size() + " changed=" + changes.size());
    }

    static void promote(Path root, Path output) throws IOException {
        root = root.toRealPath();
        List<Change> changes = readChanges(output.resolve("CHANGES.tsv"));
        List<Input> inputs = readInventory(output.resolve("INPUT.tsv"));
        Map<String, String> admitted = new HashMap<>();
        // Admission is completed for the entire batch before the first target write.
        for (Input input : inputs) {
            admitted.put(input.path(), input.hash());
            if (!sha256(Files.readString(source(root, input.path()))).equals(input.hash())) {
                throw new IllegalStateException("inventory drift: " + input.path());
            }
        }
        Map<String, Change> bySource = new HashMap<>();
        Map<String, String> sourceByDestination = new HashMap<>();
        for (Change change : changes) {
            if (!change.before().equals(admitted.get(change.beforePath()))) {
                throw new IllegalStateException("candidate source outside admitted inventory: " + change.beforePath());
            }
            bySource.put(change.beforePath(), change);
            // A duplicate destination would collapse two distinct source identities,
            // even when the generated bytes and SHA-256 happen to be identical.
            String priorSource = sourceByDestination.putIfAbsent(change.afterPath(), change.beforePath());
            if (priorSource != null) {
                throw new IllegalStateException("duplicate rename destination: " + change.afterPath()
                        + " from " + priorSource + " and " + change.beforePath());
            }
        }
        for (Change change : changes) {
            // Writing a moved file into another source path, then deleting originals,
            // destroys the result in cycles (A->B,B->A) and dependency chains.
            // Hold such plans for a separate transactional, verified move recipe.
            if (!change.beforePath().equals(change.afterPath())
                    && bySource.containsKey(change.afterPath())) {
                throw new IllegalStateException("rename destination is also an admitted source: "
                        + change.beforePath() + " -> " + change.afterPath());
            }
        }
        for (Change change : changes) {
            Path candidate = source(output.resolve("sources").toRealPath(), change.afterPath());
            if (!sha256(Files.readString(candidate)).equals(change.after())) {
                throw new IllegalStateException("candidate drift: " + change.afterPath());
            }
            Path destination = admittedPath(root, change.afterPath());
            if (!change.beforePath().equals(change.afterPath()) && Files.exists(destination)) {
                String expected = sha256(Files.readString(source(root, change.afterPath())));
                Change existingChange = bySource.get(change.afterPath());
                if (!expected.equals(change.after()) && (existingChange == null || !existingChange.after().equals(change.after()))) {
                    throw new IllegalStateException("occupied destination differs: " + change.afterPath());
                }
            }
        }
        for (Change change : changes) {
            Path candidate = source(output.resolve("sources").toRealPath(), change.afterPath());
            Path destination = admittedPath(root, change.afterPath());
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, Files.readString(candidate));
        }
        for (Change change : changes) {
            if (!change.beforePath().equals(change.afterPath())) Files.delete(root.resolve(change.beforePath()));
        }
        Files.writeString(output.resolve("PROMOTION.tsv"), "key\tvalue\nstate\tAPPLIED\nfiles\t" + changes.size()
                + "\nadmission\tALL_PREIMAGES_AND_COLLISIONS_CHECKED_BEFORE_WRITE\n");
        System.out.println("APPLIED files=" + changes.size());
    }

    private static List<Input> readInventory(Path file) throws IOException {
        List<Input> result = new ArrayList<>();
        String previous = "";
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("path\t")) continue;
            String[] fields = line.split("\t", -1);
            if (fields.length != 2 || !fields[0].endsWith(".java") || !fields[1].matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid Java inventory row");
            }
            requireRelative(fields[0]);
            if (previous.compareTo(fields[0]) >= 0) throw new IllegalArgumentException("inventory must be sorted and unique");
            previous = fields[0];
            result.add(new Input(fields[0], fields[1]));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("empty Java inventory");
        return result;
    }

    private static List<Change> readChanges(Path path) throws IOException {
        List<Change> result = new ArrayList<>();
        String previous = "";
        for (String line : Files.readAllLines(path)) {
            if (line.startsWith("source_path\t")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 4 || !cells[2].matches("[0-9a-f]{64}") || !cells[3].matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid candidate row");
            }
            requireRelative(cells[0]);
            requireRelative(cells[1]);
            if (previous.compareTo(cells[0]) >= 0) throw new IllegalArgumentException("candidate manifest must be sorted and unique");
            previous = cells[0];
            result.add(new Change(cells[0], cells[1], cells[2], cells[3]));
        }
        return result;
    }

    static Path source(Path root, String relative) throws IOException {
        Path path = admittedPath(root, relative);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !path.toRealPath().startsWith(root)) {
            throw new IllegalArgumentException("source missing or outside owner: " + relative);
        }
        return path;
    }

    /** Checks even absent destinations and dangling links before any owner write. */
    static Path admittedPath(Path root, String relative) throws IOException {
        requireRelative(relative);
        Path path = root.resolve(relative);
        for (Path current = path; current != null && current.startsWith(root); current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("symlink in admitted path: " + relative);
            }
            if (!current.equals(path) && Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("nondirectory path ancestor: " + relative);
            }
        }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("nonregular admitted file: " + relative);
        }
        return path;
    }

    private static void requireRelative(String path) {
        if (path.isBlank() || path.indexOf('\\') >= 0 || Path.of(path).isAbsolute()
                || !Path.of(path).normalize().toString().equals(path)
                || java.util.Arrays.asList(path.split("/")).contains("..")
                || path.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("unsafe relative path");
        }
    }

    private static String normalized(Path path) { return path.toString().replace('\\', '/'); }

    private static String providerIdentity(Class<?> implementation) throws IOException {
        try {
            Path artifact = Path.of(implementation.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(artifact)) throw new IOException("Provider is not an identifiable jar: " + artifact);
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact)));
            return artifact.getFileName() + ";sha256=" + digest;
        } catch (java.net.URISyntaxException | NoSuchAlgorithmException | NullPointerException | SecurityException failure) {
            throw new IOException("Cannot record exact Java LST provider", failure);
        }
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new ExceptionInInitializerError(impossible); }
    }

    private static void requireNoFailures(List<Throwable> failures, String stage, int offset) {
        if (!failures.isEmpty()) throw new IllegalStateException(stage + " failed at batch " + offset, failures.getFirst());
    }
}
