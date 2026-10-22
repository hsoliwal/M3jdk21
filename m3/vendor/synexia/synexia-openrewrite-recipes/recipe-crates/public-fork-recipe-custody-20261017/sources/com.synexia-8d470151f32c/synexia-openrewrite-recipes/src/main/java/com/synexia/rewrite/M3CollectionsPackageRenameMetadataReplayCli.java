// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
package com.synexia.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Reconstitutes a hash-bound namespace LST before composing the incumbent literal recipe. */
public final class M3CollectionsPackageRenameMetadataReplayCli {
    private M3CollectionsPackageRenameMetadataReplayCli() { }
    private record Namespace(String beforePath, String beforeHash, String afterHash) { }
    private record Candidate(String relative, Path file, String before, String after) { }

    public static void main(String[] args) throws Exception {
        if (args.length < 7 || args.length > 8 || (args.length == 8 && !"--apply".equals(args[7]))) {
            throw new IllegalArgumentException(
                    "ROOT PREIMAGE_ROOT FAMILY NAMESPACE_STAGE MANIFEST_JSON RECEIPT_TSV PARSER_CLASSPATH_FILE [--apply]");
        }
        Path root = Path.of(args[0]).toRealPath(), originals = Path.of(args[1]).toRealPath();
        Path stage = Path.of(args[3]).toRealPath();
        if (args.length == 8 && (root.startsWith(stage) || root.startsWith(originals))) {
            throw new IllegalArgumentException("Immutable preimages and namespace stages cannot be literal application targets");
        }
        String execution = Files.readString(M3CollectionsPackageRenameCli.source(stage, "SDK_EXECUTION.tsv"));
        if (!execution.contains("\nfamily\t" + args[2] + "\n") || !execution.contains("\nfixed_point\tPASS\n")) {
            throw new IllegalArgumentException("Namespace execution is not a completed admitted family stage");
        }
        Map<String, String> admittedInputs = new LinkedHashMap<>();
        List<String> inventory = Files.readAllLines(M3CollectionsPackageRenameCli.source(stage, "INPUT.tsv"));
        if (inventory.isEmpty() || !"path\tsha256".equals(inventory.getFirst())) throw new IllegalArgumentException("Namespace input header");
        for (String line : inventory.subList(1, inventory.size())) {
            String[] cells = line.split("\t", -1);
            if (cells.length != 2 || !cells[1].matches("[0-9a-f]{64}")
                    || admittedInputs.putIfAbsent(cells[0], cells[1]) != null) throw new IllegalArgumentException("Namespace input row");
        }
        Map<String, Namespace> namespaces = new LinkedHashMap<>();
        Path changesFile = M3CollectionsPackageRenameCli.source(stage, "CHANGES.tsv");
        List<String> changes = Files.readAllLines(changesFile);
        if (changes.isEmpty() || !"source_path\tdestination_path\tpreimage_sha256\tpostimage_sha256".equals(changes.getFirst())) {
            throw new IllegalArgumentException("Namespace changes header");
        }
        for (String line : changes.subList(1, changes.size())) {
            String[] cells = line.split("\t", -1);
            if (cells.length != 4 || !cells[2].equals(admittedInputs.get(cells[0])) || !cells[3].matches("[0-9a-f]{64}")
                    || namespaces.putIfAbsent(cells[1], new Namespace(cells[0], cells[2], cells[3])) != null) {
                throw new IllegalArgumentException("Namespace change is not bound to one admitted source");
            }
        }
        JsonNode manifest = new ObjectMapper().readTree(Path.of(args[4]).toFile());
        if (!manifest.isArray() || manifest.isEmpty()) throw new IllegalArgumentException("Nonempty literal manifest array required");
        Map<String, Map<String, String>> tokens = new LinkedHashMap<>();
        for (JsonNode row : manifest) {
            String path = required(row, "path");
            Namespace admission = namespaces.get(path);
            if (admission == null || !admission.afterHash().equals(required(row, "expected_sha256"))) {
                throw new IllegalArgumentException("Literal hash is not the admitted namespace intermediate: " + path);
            }
            if (tokens.computeIfAbsent(path, ignored -> new LinkedHashMap<>())
                    .putIfAbsent(required(row, "old"), required(row, "new")) != null) throw new IllegalArgumentException("Duplicate literal rule");
        }
        // All output admission precedes parsing and every write, including dry-run receipts.
        // A receipt may live in the fork's task packet, but may never replace an admitted
        // source, parser context, original preimage or immutable namespace-stage image.
        Set<Path> protectedInputs = new HashSet<>();
        for (String path : admittedInputs.keySet()) protectedInputs.add(root.resolve(path).normalize());
        for (String path : namespaces.keySet()) protectedInputs.add(root.resolve(path).normalize());
        protectedInputs.add(Path.of(args[4]).toRealPath());
        protectedInputs.add(Path.of(args[6]).toRealPath());
        Path receiptFile = output(Path.of(args[5]), originals, stage, protectedInputs);
        Path contextFile = output(receiptFile.resolveSibling(receiptFile.getFileName() + ".CONTEXT.tsv"),
                originals, stage, protectedInputs);
        Path candidateRoot = receiptFile.getParent().resolve("metadata-candidates");
        Set<Path> outputPaths = new HashSet<>(List.of(receiptFile, contextFile));
        for (String path : tokens.keySet()) {
            Path target = M3CollectionsPackageRenameCli.source(root, path);
            if (args.length == 8 && (target.startsWith(originals) || target.startsWith(stage))) {
                throw new IllegalArgumentException("Immutable literal application target: " + path);
            }
            Path candidateFile = output(candidateRoot.resolve(path), originals, stage, protectedInputs);
            if (!outputPaths.add(candidateFile)) throw new IllegalArgumentException("Overlapping replay outputs: " + path);
        }
        String cp = Files.readString(Path.of(args[6])).strip();
        List<Path> classpath = cp.isEmpty() ? List.of() : Arrays.stream(cp.split(
                java.util.regex.Pattern.quote(java.io.File.pathSeparator))).map(Path::of).toList();
        List<Candidate> candidates = new ArrayList<>();
        StringBuilder receipt = new StringBuilder("path\toriginal_sha256\tnamespace_sha256\tliteral_sha256\tchanged\tfixedpoint\n");
        for (String path : tokens.keySet().stream().sorted().toList()) {
            Namespace admission = namespaces.get(path);
            Path target = M3CollectionsPackageRenameCli.source(root, path);
            String targetBefore = Files.readString(target);
            String intermediate = Files.readString(M3CollectionsPackageRenameCli.source(stage, "sources/" + path));
            if (!hash(intermediate).equals(admission.afterHash())) throw new IllegalArgumentException("Namespace candidate drift: " + path);
            String original = Files.readString(M3CollectionsPackageRenameCli.source(originals, admission.beforePath()));
            if (!hash(original).equals(admission.beforeHash())) throw new IllegalArgumentException("Original source drift: " + path);
            List<Throwable> failures = new ArrayList<>();
            var context = new InMemoryExecutionContext(failures::add);
            List<SourceFile> parsed = JavaParser.fromJavaVersion().classpath(classpath).build().parseInputs(List.of(
                    Parser.Input.fromString(Path.of(admission.beforePath()), original)), null, context).toList();
            if (!failures.isEmpty() || parsed.size() != 1 || !(parsed.getFirst() instanceof J.CompilationUnit)
                    || !parsed.getFirst().printAll().equals(original)) throw new IllegalArgumentException("Original lossless admission failed: " + path);
            var renamed = new M3CollectionsPackageRenameRecipe(args[2]).run(new InMemoryLargeSourceSet(parsed), context)
                    .getChangeset().getAllResults();
            SourceFile namespace = renamed.isEmpty() ? parsed.getFirst() : renamed.getFirst().getAfter();
            if (!failures.isEmpty() || renamed.size() > 1 || namespace == null
                    || !namespace.getSourcePath().toString().replace('\\', '/').equals(path)
                    || !namespace.printAll().equals(intermediate)) {
                throw new IllegalArgumentException("Namespace replay differs from admitted intermediate: " + path);
            }
            Set<String> present = new HashSet<>();
            new JavaIsoVisitor<Set<String>>() {
                @Override public J.Literal visitLiteral(J.Literal literal, Set<String> sink) {
                    if (literal.getValue() instanceof String) sink.add(literal.getValueSource());
                    return literal;
                }
            }.visit(namespace, present);
            for (var token : tokens.get(path).entrySet()) {
                if (!present.contains(token.getKey()) && !present.contains(token.getValue())) {
                    throw new IllegalArgumentException("Neither admitted literal token exists: " + path);
                }
            }
            var literalChanges = new M3CollectionsPackageRenameMetadataRecipe(path, admission.afterHash(), tokens.get(path), context)
                    .run(new InMemoryLargeSourceSet(List.of(namespace)), context).getChangeset().getAllResults();
            SourceFile after = literalChanges.isEmpty() ? namespace : literalChanges.getFirst().getAfter();
            if (!failures.isEmpty() || after == null || literalChanges.size() > 1) throw new IllegalStateException("Literal recipe failed: " + path);
            String afterHash = hash(after.printAll());
            var fixed = new M3CollectionsPackageRenameMetadataRecipe(path, afterHash, tokens.get(path), context)
                    .run(new InMemoryLargeSourceSet(List.of(after)), context).getChangeset().getAllResults();
            if (!failures.isEmpty() || !fixed.isEmpty()) throw new IllegalStateException("Literal fixedpoint failed: " + path);
            if (!targetBefore.equals(intermediate) && !targetBefore.equals(after.printAll())) {
                throw new IllegalArgumentException("Namespace target is neither admitted intermediate nor literal postimage: " + path);
            }
            candidates.add(new Candidate(path, target, targetBefore, after.printAll()));
            receipt.append(path).append('\t').append(admission.beforeHash()).append('\t').append(admission.afterHash())
                    .append('\t').append(afterHash).append('\t').append(!targetBefore.equals(after.printAll())).append("\tPASS\n");
        }
        for (Candidate candidate : candidates) {
            M3CollectionsPackageRenameCli.source(root, candidate.relative());
            if (!Files.readString(candidate.file()).equals(candidate.before())) throw new IllegalStateException("Concurrent namespace drift");
        }
        for (Path path : outputPaths) output(path, originals, stage, protectedInputs);
        Files.createDirectories(receiptFile.getParent());
        for (Candidate candidate : candidates) {
            Path file = candidateRoot.resolve(candidate.relative());
            Files.createDirectories(file.getParent());
            replace(file, candidate.after());
        }
        replace(receiptFile, receipt.toString());
        replace(contextFile,
                "key\tvalue\nnamespace_receipt_sha256\t" + hash(Files.readString(changesFile))
                        + "\nparser_classpath_sha256\t" + hash(Files.readString(Path.of(args[6])))
                        + "\nexecution\tNAMESPACE_LST_REPLAY_THEN_EXISTING_LITERAL_RECIPE\n");
        if (args.length == 8) for (Candidate candidate : candidates) {
            if (!candidate.before().equals(candidate.after())) replace(candidate.file(), candidate.after());
        }
        System.out.println("metadata_replay_files=" + candidates.size() + " fixedpoint=PASS apply=" + (args.length == 8));
    }

    /** Replace a directory entry without truncating a potentially shared input inode. */
    private static void replace(Path destination, String contents) throws IOException {
        Path temporary = Files.createTempFile(destination.getParent(), ".m3-replay-", ".tmp");
        try {
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(destination, LinkOption.NOFOLLOW_LINKS));
                } catch (UnsupportedOperationException unavailablePermissionView) {
                    // Non-POSIX filesystems retain their native new-file permission behavior.
                }
            }
            Files.writeString(temporary, contents);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unavailableAtomicMove) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path output(Path file, Path originals, Path stage, Set<Path> protectedInputs) throws IOException {
        Path absolute = file.toAbsolutePath().normalize();
        Path admitted = M3CollectionsPackageRenameCli.admittedPath(absolute.getRoot(),
                absolute.getRoot().relativize(absolute).toString());
        if (admitted.startsWith(originals) || originals.startsWith(admitted)
                || admitted.startsWith(stage) || stage.startsWith(admitted) || protectedInputs.contains(admitted)) {
            throw new IllegalArgumentException("Replay output overlaps an immutable tree or admitted input: " + file);
        }
        return admitted;
    }

    private static String required(JsonNode node, String field) {
        if (!node.hasNonNull(field) || !node.get(field).isTextual()) throw new IllegalArgumentException("Missing text field: " + field);
        return node.get(field).asText();
    }
    private static String hash(String source) { return M3CollectionsPackageRenameMetadataRecipe.sha256(source); }
}
