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
 * Read-only dependency inventory for the Java21-compatible JEP 496/497 provider lane.
 *
 * <p>This recipe grants no product mutation or public-API promotion authority. It records the
 * prerequisite chain that must be resolved before source-sealed ML-KEM/ML-DSA materialization.</p>
 */
public final class M3PqSecurityDependencyInventoryRecipe extends Recipe {
    private static final List<Row> ROWS = List.of(
            row(
                    0,
                    "JDK-8318096",
                    "PUBLIC_API_PREREQUISITE",
                    22,
                    "9123961aaa47aa58ec436640590d2cceedb8cbb1",
                    "jdk-22+36",
                    "LIBRARY_API",
                    "NO",
                    "BLOCK_DEFAULT_PUBLIC_API",
                    "",
                    0,
                    18,
                    "Decide opt-in AsymmetricKey API or adapt the internal framework away from it."),
            row(
                    1,
                    "JDK-8340327",
                    "INTERNAL_FRAMEWORK_PREREQUISITE",
                    24,
                    "3f53d571343792341481f4d15970cdc0bcd76a5e",
                    "jdk-24+36",
                    "MODULE",
                    "YES_ADAPTED",
                    "CANDIDATE_UNVERIFIED",
                    "JDK-8318096",
                    10,
                    0,
                    "Compile an internal Java21 adaptation without importing AsymmetricKey public API."),
            row(
                    2,
                    "JEP-496",
                    "FEATURE_PROVIDER",
                    24,
                    "13987b4244614d594dc8f94c288eddb6239a066f",
                    "jdk-24+36",
                    "MODULE",
                    "YES_ADAPTED",
                    "CANDIDATE_UNVERIFIED",
                    "JDK-8340327",
                    8,
                    2,
                    "Materialize provider-only GA postimages and run ML-KEM ACVP/KEM regressions."),
            row(
                    3,
                    "JEP-497",
                    "FEATURE_PROVIDER",
                    24,
                    "8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7",
                    "jdk-24+36",
                    "MODULE",
                    "YES_ADAPTED",
                    "CANDIDATE_UNVERIFIED",
                    "JDK-8340327",
                    8,
                    2,
                    "Materialize provider-only GA postimages and run ML-DSA ACVP/Signature regressions."));

    private transient DependencyTable dependencies = new DependencyTable(this);
    private transient PlanTable plan = new PlanTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory JEP 496/497 post-quantum security dependencies";
    }

    @Override
    public String getDescription() {
        return "Emits the fail-closed Java21 dependency/adaptation plan for ML-KEM and ML-DSA "
                + "without changing JDK source or granting public-API authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "security",
                "post-quantum",
                "jep-496",
                "jep-497",
                "dependency-inventory",
                "candidate-only",
                "non-mutating");
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
                    for (Row row : ROWS) {
                        dependencies.insertRow(context, row);
                    }
                    plan.insertRow(
                            context,
                            new PlanRow(
                                    ROWS.size(),
                                    planRoot(),
                                    "MODULE",
                                    false,
                                    false,
                                    "JDK-8340327 Java21 internal adaptation must reach compile/test fixed point first."));
                }
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    public static List<Row> rows() {
        return ROWS;
    }

    public static String planRoot() {
        MessageDigest digest = digest();
        frame(digest, "M3_PQ_SECURITY_DEPENDENCY_PLAN_V1");
        for (Row row : ROWS) {
            frame(digest, row.root());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Row row(
            int order,
            String identity,
            String kind,
            int release,
            String upstreamCommit,
            String upstreamRef,
            String physicalScope,
            String defaultJava21,
            String status,
            String dependsOn,
            int acceptedTargets,
            int excludedTargets,
            String nextProof) {
        String root =
                hash(
                        Integer.toString(order),
                        identity,
                        kind,
                        Integer.toString(release),
                        upstreamCommit,
                        upstreamRef,
                        physicalScope,
                        defaultJava21,
                        status,
                        dependsOn,
                        Integer.toString(acceptedTargets),
                        Integer.toString(excludedTargets),
                        nextProof);
        return new Row(
                order,
                identity,
                kind,
                release,
                upstreamCommit,
                upstreamRef,
                physicalScope,
                defaultJava21,
                status,
                dependsOn,
                acceptedTargets,
                excludedTargets,
                nextProof,
                root);
    }

    private static String hash(String... fields) {
        MessageDigest digest = digest();
        frame(digest, "M3_PQ_SECURITY_DEPENDENCY_ROW_V1");
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

    public static final class DependencyTable extends DataTable<Row> {
        DependencyTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 JEP 496/497 dependency inventory",
                    "Read-only Java21 adaptation prerequisites and exclusions.");
        }
    }

    public record Row(
            @Column(displayName = "Order", description = "Dependency topological order.")
                    int order,
            @Column(displayName = "Identity", description = "JBS/JEP identity.")
                    String identity,
            @Column(displayName = "Kind", description = "Dependency/feature role.")
                    String kind,
            @Column(displayName = "Release", description = "Upstream release.")
                    int release,
            @Column(displayName = "Upstream commit", description = "Exact upstream provenance.")
                    String upstreamCommit,
            @Column(displayName = "Upstream ref", description = "Composition donor ref.")
                    String upstreamRef,
            @Column(displayName = "Physical scope", description = "Minimum physical review scope.")
                    String physicalScope,
            @Column(displayName = "Default Java21", description = "Default-surface compatibility.")
                    String defaultJava21,
            @Column(displayName = "Status", description = "Current admission state.")
                    String status,
            @Column(displayName = "Depends on", description = "Required predecessor identity.")
                    String dependsOn,
            @Column(displayName = "Accepted targets", description = "Candidate/review target count.")
                    int acceptedTargets,
            @Column(displayName = "Excluded targets", description = "Explicit default exclusions.")
                    int excludedTargets,
            @Column(displayName = "Next proof", description = "Required next mechanical gate.")
                    String nextProof,
            @Column(displayName = "Root", description = "Deterministic row SHA-256.")
                    String root) {}

    public static final class PlanTable extends DataTable<PlanRow> {
        PlanTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 JEP 496/497 dependency plan",
                    "Single deterministic plan root; no mutation/promotion authority.");
        }
    }

    public record PlanRow(
            @Column(displayName = "Row count", description = "Dependency row count.")
                    int rowCount,
            @Column(displayName = "Plan root", description = "Deterministic SHA-256 over dependency rows.")
                    String planRoot,
            @Column(displayName = "Product scope", description = "Candidate product scope.")
                    String productScope,
            @Column(displayName = "Mutation authority", description = "Always false.")
                    boolean mutationAuthority,
            @Column(displayName = "Public API default authority", description = "Always false.")
                    boolean publicApiDefaultAuthority,
            @Column(displayName = "Next pass", description = "Required next pass.")
                    String nextPass) {}
}
