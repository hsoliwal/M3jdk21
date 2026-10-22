// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
package com.synexia.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExportsTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.OpensTree;
import com.sun.source.tree.ProvidesTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UsesTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Exact module-name adaptation through real JDK compiler ASTs, distinct from OpenRewrite execution. */
public final class M3CollectionsPackageRenameModulesCli {
    private M3CollectionsPackageRenameModulesCli() { }
    record Replacement(String oldName, String newName) { }
    record Result(String text, int oldOccurrences, int postimageOccurrences) { }
    private record Edit(int start, int end, String replacement) { }
    private record Candidate(String relativePath, Path path, String before, Result result) { }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4 || args.length == 4 && !"--apply".equals(args[3])) {
            throw new IllegalArgumentException("ROOT MANIFEST_JSON RECEIPT_TSV [--apply]");
        }
        Path root = Path.of(args[0]).toRealPath();
        JsonNode manifest = new ObjectMapper().readTree(Path.of(args[1]).toFile());
        if (!manifest.isArray() || manifest.isEmpty()) throw new IllegalArgumentException("Nonempty module manifest array required");
        Map<String, List<Replacement>> files = new LinkedHashMap<>();
        Map<String, String> hashes = new LinkedHashMap<>();
        for (JsonNode row : manifest) {
            String path = required(row, "path");
            Path relative = Path.of(path);
            if (!relative.getFileName().toString().equals("module-info.java") || relative.isAbsolute()
                    || path.chars().anyMatch(Character::isISOControl)
                    || !relative.normalize().toString().replace('\\', '/').equals(path)) {
                throw new IllegalArgumentException("Canonical module-info path required");
            }
            M3CollectionsPackageRenameCli.source(root, path);
            String hash = required(row, "expected_sha256");
            if (!hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid module hash");
            String prior = hashes.putIfAbsent(path, hash);
            if (prior != null && !prior.equals(hash)) throw new IllegalArgumentException("Conflicting module hashes");
            Replacement rule = new Replacement(required(row, "old"), required(row, "new"));
            requireName(rule.oldName()); requireName(rule.newName());
            if (rule.oldName().equals(rule.newName())) throw new IllegalArgumentException("No-op module rule");
            List<Replacement> rules = files.computeIfAbsent(path, ignored -> new ArrayList<>());
            if (rules.stream().anyMatch(r -> r.oldName().equals(rule.oldName()))) throw new IllegalArgumentException("Duplicate module rule");
            rules.add(rule);
        }
        List<Candidate> candidates = new ArrayList<>();
        StringBuilder receipt = new StringBuilder("path\tbefore_sha256\tafter_sha256\told_occurrences\tpostimage_occurrences\tchanged\tfixedpoint\texecution_kind\n");
        for (String path : files.keySet().stream().sorted().toList()) {
            Path target = M3CollectionsPackageRenameCli.source(root, path);
            String before = Files.readString(target);
            String beforeHash = M3CollectionsPackageRenameMetadataRecipe.sha256(before);
            if (!beforeHash.equals(hashes.get(path))) throw new IllegalArgumentException("Module source hash drift: " + path);
            Result result = rewrite(path, before, files.get(path));
            Result fixed = rewrite(path, result.text(), files.get(path));
            if (!fixed.text().equals(result.text()) || fixed.oldOccurrences() != 0) throw new IllegalStateException("Module fixedpoint refused");
            candidates.add(new Candidate(path, target, before, result));
            receipt.append(path).append('\t').append(beforeHash).append('\t')
                    .append(M3CollectionsPackageRenameMetadataRecipe.sha256(result.text())).append('\t')
                    .append(result.oldOccurrences()).append('\t').append(fixed.postimageOccurrences())
                    .append('\t').append(!before.equals(result.text())).append("\tPASS\tJDK_MODULE_AST_PARSE\n");
        }
        for (Candidate candidate : candidates) {
            if (!Files.readString(candidate.path()).equals(candidate.before())) throw new IllegalStateException("Concurrent module source drift");
        }
        Path receiptPath = Path.of(args[2]).toAbsolutePath();
        Files.createDirectories(receiptPath.getParent());
        for (Candidate candidate : candidates) {
            Path destination = receiptPath.getParent().resolve("module-candidates").resolve(candidate.relativePath());
            Files.createDirectories(destination.getParent()); Files.writeString(destination, candidate.result().text());
        }
        Files.writeString(receiptPath, receipt.toString());
        if (args.length == 4) for (Candidate candidate : candidates) {
            if (!candidate.before().equals(candidate.result().text())) Files.writeString(candidate.path(), candidate.result().text());
        }
        System.out.println("module_files=" + candidates.size() + " changed="
                + candidates.stream().filter(c -> !c.before().equals(c.result().text())).count()
                + " execution_kind=JDK_MODULE_AST_PARSE apply=" + (args.length == 4));
    }

    static Result rewrite(String path, String source, List<Replacement> rules) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Actual JDK compiler required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavaFileObject input = new SimpleJavaFileObject(URI.create("string:///module-info.java"), JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignored) { return source; }
            };
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of("-proc:none"), null, List.of(input));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            if (units.size() != 1 || diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    || units.getFirst().getModule() == null) throw new IllegalArgumentException("JDK module AST parse refused: " + path);
            CompilationUnitTree unit = units.getFirst();
            ModuleTree module = unit.getModule();
            List<Tree> names = new ArrayList<>(); names.add(module.getName());
            for (var directive : module.getDirectives()) {
                switch (directive.getKind()) {
                    case EXPORTS -> { var node = (ExportsTree) directive; names.add(node.getPackageName()); if (node.getModuleNames() != null) names.addAll(node.getModuleNames()); }
                    case OPENS -> { var node = (OpensTree) directive; names.add(node.getPackageName()); if (node.getModuleNames() != null) names.addAll(node.getModuleNames()); }
                    case REQUIRES -> names.add(((RequiresTree) directive).getModuleName());
                    case USES -> names.add(((UsesTree) directive).getServiceName());
                    case PROVIDES -> { var node = (ProvidesTree) directive; names.add(node.getServiceName()); names.addAll(node.getImplementationNames()); }
                    default -> throw new IllegalArgumentException("Unsupported module directive: " + directive.getKind());
                }
            }
            var positions = Trees.instance(task).getSourcePositions();
            int oldCount = 0, postCount = 0;
            int[] present = new int[rules.size()];
            List<Edit> edits = new ArrayList<>();
            for (Tree name : names) {
                String identity = name.toString();
                for (int i = 0; i < rules.size(); i++) {
                    Replacement rule = rules.get(i);
                    if (identity.equals(rule.newName())) { postCount++; present[i]++; }
                    if (!identity.equals(rule.oldName())) continue;
                    oldCount++; present[i]++;
                    int start = Math.toIntExact(positions.getStartPosition(unit, name));
                    int end = Math.toIntExact(positions.getEndPosition(unit, name));
                    if (start < 0 || end < start || !source.substring(start, end).equals(identity)) {
                        throw new IllegalArgumentException("Module name source is not exact (comments/escapes remain opaque): " + path);
                    }
                    edits.add(new Edit(start, end, rule.newName()));
                }
            }
            for (int count : present) if (count == 0) throw new IllegalArgumentException("Admitted module AST name absent: " + path);
            edits.sort(Comparator.comparingInt(Edit::start).reversed());
            StringBuilder after = new StringBuilder(source);
            int priorStart = source.length();
            for (Edit edit : edits) {
                if (edit.end() > priorStart) throw new IllegalArgumentException("Overlapping module name edits");
                after.replace(edit.start(), edit.end(), edit.replacement()); priorStart = edit.start();
            }
            return new Result(after.toString(), oldCount, postCount);
        }
    }

    private static void requireName(String name) {
        if (!name.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
            throw new IllegalArgumentException("Exact qualified module/package/type name required");
        }
    }
    private static String required(JsonNode row, String field) {
        if (!row.hasNonNull(field) || !row.get(field).isTextual() || row.get(field).asText().isEmpty()) throw new IllegalArgumentException("Missing module field: " + field);
        return row.get(field).asText();
    }
}
