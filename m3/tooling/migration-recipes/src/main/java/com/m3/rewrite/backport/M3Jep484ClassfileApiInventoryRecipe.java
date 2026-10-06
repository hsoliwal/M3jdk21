// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/**
 * Read-only cumulative compatibility inventory for JEP 457 -> 466 -> 484.
 *
 * <p>JDK21 already owns the internal classfile engine. This recipe records the publicization and
 * evolution lineage, tree denominator, and opt-in-only Java21 policy. It grants no product
 * mutation or public API promotion authority.</p>
 */
public final class M3Jep484ClassfileApiInventoryRecipe extends Recipe {
    private static final List<StageRow> STAGES = List.of(
            stage(0, "JDK21-INTERNAL-CLASSFILE", "INTERNAL_ENGINE_BASELINE", 21,
                    "890adb6410dab4606a4f26a942aed02fb2f55387", "jdk-21+35",
                    "MODULE", "YES", "PRESENT", "", 241,
                    "Existing internal engine; public java.lang.classfile package absent."),
            stage(1, "JEP-457", "PUBLIC_API_FIRST_PREVIEW", 22,
                    "2b00ac0d02a110326846c75ea7ea535dccbb1924", "jdk-22+36",
                    "LIBRARY_API", "NO", "OPT_IN_ONLY", "JDK21-INTERNAL-CLASSFILE", 300,
                    "Publicizes/renames the internal engine and migrates downstream consumers."),
            stage(2, "JEP-466", "PUBLIC_API_SECOND_PREVIEW", 23,
                    "19a99d023e32fa9f4d26b76bd36993719e1dfe21", "jdk-23+37",
                    "LIBRARY_API", "NO", "OPT_IN_ONLY", "JEP-457", 1,
                    "Transition commit changes preview metadata only; cumulative history is authoritative."),
            stage(3, "JEP-484", "FINAL_PUBLIC_API", 24,
                    "84ffb64cd73f8af11cf3670c6f19d282c2ac6961", "jdk-24+36",
                    "LIBRARY_API", "NO", "CANDIDATE_OPT_IN_UNVERIFIED", "JEP-466", 165,
                    "Final API remains an explicit opt-in Java21 contract change."));

    private transient StageTable stages = new StageTable(this);
    private transient SummaryTable summary = new SummaryTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory cumulative JEP 484 Class-File API lineage";
    }

    @Override
    public String getDescription() {
        return "Emits the JDK21-internal through JEP457/JEP466/JEP484 lineage, exact tree "
                + "denominator and opt-in-only Java21 policy without changing JDK source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "backport", "jep-457", "jep-466", "jep-484",
                "classfile", "dependency-inventory", "public-api", "candidate-only", "non-mutating");
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            private boolean emitted;

            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!emitted) {
                    emitted = true;
                    STAGES.forEach(row -> stages.insertRow(context, row));
                    summary.insertRow(
                            context,
                            new SummaryRow(
                                    241,
                                    0,
                                    161,
                                    84,
                                    245,
                                    238,
                                    3,
                                    7,
                                    59,
                                    "LIBRARY_API",
                                    "NO",
                                    "YES",
                                    "EXPLICIT_CONTRACT_CHANGE",
                                    "JAVA21_MAJOR_65_DEFAULT_REQUIRED",
                                    planRoot(),
                                    false,
                                    false));
                }
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    public static List<StageRow> stages() {
        return STAGES;
    }

    public static String planRoot() {
        MessageDigest digest = digest();
        frame(digest, "M3_JEP484_CLASSFILE_PLAN_V1");
        for (StageRow stage : STAGES) {
            frame(digest, stage.root());
        }
        for (String value : List.of(
                "jdk21Internal=241",
                "jdk24Public=161",
                "jdk24Internal=84",
                "normalizedDescendants=238",
                "removed=3",
                "added=7",
                "history=59",
                "scope=LIBRARY_API",
                "defaultJava21=NO",
                "optIn=YES",
                "contract=EXPLICIT_CONTRACT_CHANGE",
                "classfileDefault=JAVA21_MAJOR_65_DEFAULT_REQUIRED")) {
            frame(digest, value);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static StageRow stage(
            int order,
            String identity,
            String kind,
            int release,
            String commit,
            String upstreamRef,
            String scope,
            String defaultJava21,
            String status,
            String dependsOn,
            int headlineTouchedPaths,
            String note) {
        String root = hash(
                Integer.toString(order), identity, kind, Integer.toString(release), commit,
                upstreamRef, scope, defaultJava21, status, dependsOn,
                Integer.toString(headlineTouchedPaths), note);
        return new StageRow(
                order, identity, kind, release, commit, upstreamRef, scope, defaultJava21,
                status, dependsOn, headlineTouchedPaths, note, root);
    }

    private static String hash(String... fields) {
        MessageDigest digest = digest();
        frame(digest, "M3_JEP484_CLASSFILE_STAGE_V1");
        for (String field : fields) {
            frame(digest, field);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static final class StageTable extends DataTable<StageRow> {
        StageTable(Recipe recipe) {
            super(recipe, "M3 JEP 484 cumulative stages",
                    "JDK21 internal engine and public Class-File API milestone lineage.");
        }
    }

    public record StageRow(
            @Column(displayName = "Order", description = "Dependency order.") int order,
            @Column(displayName = "Identity", description = "Stage identity.") String identity,
            @Column(displayName = "Kind", description = "Stage role.") String kind,
            @Column(displayName = "Release", description = "Upstream release.") int release,
            @Column(displayName = "Commit", description = "Pinned upstream commit.") String commit,
            @Column(displayName = "Upstream ref", description = "GA composition ref.") String upstreamRef,
            @Column(displayName = "Scope", description = "Minimum product review scope.") String scope,
            @Column(displayName = "Default Java21", description = "Default-surface compatibility.") String defaultJava21,
            @Column(displayName = "Status", description = "Admission status.") String status,
            @Column(displayName = "Depends on", description = "Predecessor identity.") String dependsOn,
            @Column(displayName = "Headline touched paths", description = "Touched paths in the milestone transition commit.") int headlineTouchedPaths,
            @Column(displayName = "Note", description = "Compatibility note.") String note,
            @Column(displayName = "Root", description = "Deterministic stage SHA-256.") String root) {}

    public static final class SummaryTable extends DataTable<SummaryRow> {
        SummaryTable(Recipe recipe) {
            super(recipe, "M3 JEP 484 Class-File API denominator",
                    "Tree/history denominator and opt-in Java21 contract policy.");
        }
    }

    public record SummaryRow(
            @Column(displayName = "JDK21 internal files", description = "Internal classfile Java files.") int jdk21InternalFiles,
            @Column(displayName = "JDK21 public files", description = "Public java.lang.classfile Java files.") int jdk21PublicFiles,
            @Column(displayName = "JDK24 public files", description = "Final public API Java files.") int jdk24PublicFiles,
            @Column(displayName = "JDK24 internal files", description = "Retained internal Java files.") int jdk24InternalFiles,
            @Column(displayName = "JDK24 combined files", description = "Public + internal final files.") int jdk24CombinedFiles,
            @Column(displayName = "Direct descendants", description = "Normalized JDK21 paths with JDK24 descendants.") int normalizedDirectDescendants,
            @Column(displayName = "Removed paths", description = "Normalized internal paths removed.") int normalizedRemoved,
            @Column(displayName = "Added paths", description = "Normalized final paths added.") int normalizedAdded,
            @Column(displayName = "History commits", description = "Public subtree commits from first preview to final.") int historyCommits,
            @Column(displayName = "Scope", description = "Required product scope.") String scope,
            @Column(displayName = "Default Java21", description = "Default public API authority.") String defaultJava21,
            @Column(displayName = "Opt-in extension", description = "Opt-in public API lane.") String optInExtension,
            @Column(displayName = "Contract", description = "Contract mode.") String contract,
            @Column(displayName = "Classfile default", description = "Required default emitted classfile behavior.") String classfileDefault,
            @Column(displayName = "Plan root", description = "Deterministic plan SHA-256.") String planRoot,
            @Column(displayName = "Mutation authority", description = "Always false.") boolean mutationAuthority,
            @Column(displayName = "Promotion authority", description = "Always false.") boolean promotionAuthority) {}
}
