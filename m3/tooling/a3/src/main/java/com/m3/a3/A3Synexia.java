// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import com.m3.synexia.importer.SynexiaImportManifest;
import com.m3.synexia.importer.SynexiaImportPlan;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Converts a sealed Synexia delivery delta into deterministic A3 review work. */
public final class A3Synexia {

    public enum ReviewLane {
        RECIPE_EXECUTION_REVIEW,
        RECIPE_PROOF_REVIEW,
        RECIPE_RESOURCE_REVIEW,
        JAVA_ATOM_PATTERN_REVIEW,
        JAVA_CONTRACT_PROOF_REVIEW,
        JNI_NATIVE_PARITY_REVIEW,
        DELIVERY_TOOL_REVIEW,
        INDEX_PRECOMPUTE_REVIEW,
        RECIPE_DAG_REVIEW,
        APACHE_MANUAL_REVIEW
    }

    public record Row(
            int order,
            String sourceRevision,
            String manifestRoot,
            String planRoot,
            String targetPath,
            SynexiaImportPlan.Action action,
            SynexiaImportPlan.Lane deliveryLane,
            ReviewLane reviewLane,
            String expectedSha256,
            String currentSha256,
            boolean candidateRequired,
            String nextAction) {

        public Row {
            if (order < 0) throw new IllegalArgumentException("order");
            sourceRevision = gitObject(sourceRevision, "sourceRevision");
            manifestRoot = sha(manifestRoot, "manifestRoot");
            planRoot = sha(planRoot, "planRoot");
            targetPath = A3WorkValues.sourcePath(targetPath);
            if (!targetPath.startsWith("m3/vendor/synexia/")) {
                throw new IllegalArgumentException("targetPath");
            }
            action = Objects.requireNonNull(action, "action");
            deliveryLane = Objects.requireNonNull(deliveryLane, "deliveryLane");
            reviewLane = Objects.requireNonNull(reviewLane, "reviewLane");
            expectedSha256 = hashOrMarker(expectedSha256, "expectedSha256", "STALE");
            currentSha256 = hashOrMarker(currentSha256, "currentSha256", "ABSENT");
            requireHashRelation(action, expectedSha256, currentSha256);
            nextAction = A3WorkValues.text(nextAction, "nextAction");
            boolean expectedCandidate =
                    action == SynexiaImportPlan.Action.ADD
                            || action == SynexiaImportPlan.Action.REPLACE;
            if (candidateRequired != expectedCandidate) {
                throw new IllegalArgumentException("candidateRequired");
            }
            if (reviewLane != review(deliveryLane)
                    || !nextAction.equals(next(action))) {
                throw new IllegalArgumentException("derived A3 Synexia work drift");
            }
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    private A3Synexia() {}

    public static List<Row> load(Path root, Path manifestFile) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        Path manifestPath = A3Fs.source(checkedRoot, manifestFile);
        if (!Files.isRegularFile(manifestPath) || Files.isSymbolicLink(manifestPath)) {
            throw new IOException("missing or unsafe Synexia delivery manifest: " + manifestPath);
        }
        SynexiaImportManifest manifest =
                SynexiaImportManifest.parse(
                        Files.readString(manifestPath, StandardCharsets.UTF_8));
        SynexiaImportPlan.Plan plan =
                SynexiaImportPlan.plan(checkedRoot, manifest);

        ArrayList<Row> rows = new ArrayList<>(plan.rows().size());
        int order = 0;
        for (SynexiaImportPlan.Row row : plan.rows()) {
            boolean candidate =
                    row.action() == SynexiaImportPlan.Action.ADD
                            || row.action() == SynexiaImportPlan.Action.REPLACE;
            rows.add(
                    new Row(
                            order++,
                            plan.sourceRevision(),
                            plan.manifestRoot(),
                            plan.root(),
                            row.targetPath(),
                            row.action(),
                            row.lane(),
                            review(row.lane()),
                            row.expectedSha256(),
                            row.currentSha256(),
                            candidate,
                            next(row.action())));
        }
        return List.copyOf(rows);
    }

    public static void write(Path root, Path manifestFile, Path out) throws IOException {
        List<Row> rows = load(root, manifestFile);
        StringBuilder tsv =
                new StringBuilder(
                        "order\tsource_revision\tmanifest_root\tplan_root\ttarget_path\t"
                                + "action\tdelivery_lane\treview_lane\texpected_sha256\t"
                                + "current_sha256\tcandidate_required\tnext_action\t"
                                + "promotion_authority\n");
        for (Row row : rows) {
            tsv.append(row.order())
                    .append('\t')
                    .append(row.sourceRevision())
                    .append('\t')
                    .append(row.manifestRoot())
                    .append('\t')
                    .append(row.planRoot())
                    .append('\t')
                    .append(A3Fs.cell(row.targetPath()))
                    .append('\t')
                    .append(row.action())
                    .append('\t')
                    .append(row.deliveryLane())
                    .append('\t')
                    .append(row.reviewLane())
                    .append('\t')
                    .append(row.expectedSha256())
                    .append('\t')
                    .append(row.currentSha256())
                    .append('\t')
                    .append(row.candidateRequired())
                    .append('\t')
                    .append(row.nextAction())
                    .append('\t')
                    .append(row.promotionAuthority())
                    .append('\n');
        }
        A3Fs.write(root, out, tsv.toString());
    }

    static ReviewLane review(SynexiaImportPlan.Lane lane) {
        return switch (Objects.requireNonNull(lane, "lane")) {
            case OPENREWRITE_RECIPE -> ReviewLane.RECIPE_EXECUTION_REVIEW;
            case OPENREWRITE_TEST -> ReviewLane.RECIPE_PROOF_REVIEW;
            case OPENREWRITE_RESOURCE -> ReviewLane.RECIPE_RESOURCE_REVIEW;
            case CONVERGENCE_JAVA -> ReviewLane.JAVA_ATOM_PATTERN_REVIEW;
            case CONVERGENCE_TEST -> ReviewLane.JAVA_CONTRACT_PROOF_REVIEW;
            case CONVERGENCE_NATIVE -> ReviewLane.JNI_NATIVE_PARITY_REVIEW;
            case M3_CLONER -> ReviewLane.DELIVERY_TOOL_REVIEW;
            case M3INDEX -> ReviewLane.INDEX_PRECOMPUTE_REVIEW;
            case M3_RECIPE -> ReviewLane.RECIPE_DAG_REVIEW;
            case OTHER_APACHE -> ReviewLane.APACHE_MANUAL_REVIEW;
        };
    }

    static String next(SynexiaImportPlan.Action action) {
        return switch (Objects.requireNonNull(action, "action")) {
            case KEEP -> "REUSE_VERIFIED_VENDOR";
            case ADD, REPLACE -> "STAGE_THEN_REVIEW";
            case STALE -> "REVIEW_STALE_NO_DELETE";
        };
    }

    private static void requireHashRelation(
            SynexiaImportPlan.Action action,
            String expected,
            String current) {
        switch (action) {
            case ADD -> {
                if (!"ABSENT".equals(current) || "STALE".equals(expected)) {
                    throw new IllegalArgumentException("ADD hash relation");
                }
            }
            case KEEP -> {
                if ("ABSENT".equals(current)
                        || "STALE".equals(expected)
                        || !current.equals(expected)) {
                    throw new IllegalArgumentException("KEEP hash relation");
                }
            }
            case REPLACE -> {
                if ("ABSENT".equals(current)
                        || "STALE".equals(expected)
                        || current.equals(expected)) {
                    throw new IllegalArgumentException("REPLACE hash relation");
                }
            }
            case STALE -> {
                if (!"STALE".equals(expected) || "ABSENT".equals(current)) {
                    throw new IllegalArgumentException("STALE hash relation");
                }
            }
        }
    }

    private static String hashOrMarker(String value, String field, String marker) {
        String checked = A3WorkValues.text(value, field);
        return marker.equals(checked) ? checked : sha(checked, field);
    }

    private static String gitObject(String value, String field) {
        String checked =
                A3WorkValues.text(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{40}|[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked =
                A3WorkValues.text(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
