/* Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0 */
package com.m3.openrewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Maven/OpenRewrite facade; the separately tested Java21 kernel owns eligibility and source edits. */
public final class M3LiteralAdmissionLoweringRecipe extends Recipe {
    @Option(displayName = "Verified owner classes directory",
            description = "Directory produced by the pinned M3 core build. Its complete text class closure must match the tested root.",
            example = "/work/m3/build/safety/owner-classes")
    private final String ownerClassesDirectory;
    private final transient Outcomes outcomes = new Outcomes(this);

    @JsonCreator public M3LiteralAdmissionLoweringRecipe(
            @JsonProperty("ownerClassesDirectory") String ownerClassesDirectory) {
        this.ownerClassesDirectory = Objects.requireNonNull(ownerClassesDirectory, "verified owner classes required");
        if (ownerClassesDirectory.isBlank()) throw new IllegalArgumentException("empty owner classes directory");
    }
    public String getOwnerClassesDirectory() { return ownerClassesDirectory; }
    @Override public String getDisplayName() { return "Lower verified constant M3 text admissions"; }
    @Override public String getDescription() {
        return "Lower only typed explicit M3Text.fromString constant concatenations. Preserve ordinary String sites "
                + "and refuse source/type/owner drift. Reports are not authority to promote a changed source.";
    }
    @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree visit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof J.CompilationUnit unit)) return tree;
                String source = unit.printAll();
                if (!source.contains("fromString")) return tree;
                var result = TypedLiteralAdmissionLowering.lower(unit.getSourcePath().getFileName().toString(),
                        source, Path.of(ownerClassesDirectory));
                outcomes.insertRow(context, new Outcome(unit.getSourcePath().toString(), result.status().name(),
                        result.reason(), result.beforeSha256(), result.afterSha256(), result.changes()));
                if (result.status() != TypedLiteralAdmissionLowering.Status.CHANGED) return tree;
                try (var parsed = JavaParser.fromJavaVersion().classpath(List.of(Path.of(ownerClassesDirectory))).build()
                        .parseInputs(List.of(Parser.Input.fromString(unit.getSourcePath(), result.after())), null, context)) {
                    List<SourceFile> files = parsed.toList();
                    if (files.size() != 1 || !(files.getFirst() instanceof J.CompilationUnit)
                            || !result.after().equals(files.getFirst().printAll())) {
                        throw new IllegalStateException("lowered source failed lossless Java parsing");
                    }
                    return files.getFirst().withSourcePath(unit.getSourcePath()).withMarkers(unit.getMarkers());
                }
            }
        };
    }
    public static final class Outcomes extends DataTable<Outcome> {
        public Outcomes(Recipe recipe) { super(recipe, "M3 constant admission outcomes", "Typed decisions and exact source hashes; refused sites remain unchanged."); }
    }
    public static final class Outcome {
        @Column(displayName = "Path", description = "Source path.") private final String path;
        @Column(displayName = "Status", description = "CHANGED, UNCHANGED or REFUSED.") private final String status;
        @Column(displayName = "Reason", description = "Eligibility or refusal reason.") private final String reason;
        @Column(displayName = "Before", description = "Original source SHA-256.") private final String before;
        @Column(displayName = "After", description = "Result source SHA-256.") private final String after;
        @Column(displayName = "Sites", description = "Number of lowered sites.") private final int sites;
        public Outcome(String path, String status, String reason, String before, String after, int sites) {
            this.path = path; this.status = status; this.reason = reason; this.before = before; this.after = after; this.sites = sites;
        }
        public String getPath() { return path; }
        public String getStatus() { return status; }
        public String getReason() { return reason; }
        public String getBefore() { return before; }
        public String getAfter() { return after; }
        public int getSites() { return sites; }
    }
}
