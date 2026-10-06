// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Binds each unresolved JEP row to the canonical M3 backport DAG and an admitted Nebula transfer.
 *
 * <p>The result is a proof work order, not a compatibility decision. No row can grant mutation or
 * promotion authority, and existing materialized packets remain evidence rather than acceptance.</p>
 */
public final class M3JepResidueDagPlanner {
    public static final String HEADER =
            "order	release	jep	domain	disposition	priority	evidence_state"
                    + "	evidence_paths	proof_lane	next_action	canonical_dag_root"
                    + "	nebula_transfer_root	work_order_root	source_mutation_authority"
                    + "	promotion_authority";

    private M3JepResidueDagPlanner() {}

    public static Plan plan(
            M3JepResidueQueue.Queue queue,
            M3NebulaTransferAdmission.Receipt transfer) {
        M3JepResidueQueue.Queue checkedQueue = Objects.requireNonNull(queue, "queue");
        M3NebulaTransferAdmission.Receipt checkedTransfer =
                Objects.requireNonNull(transfer, "transfer");
        String dagRoot = M3DagSemanticRoot.of(M3RecipeDag.canonical());

        ArrayList<WorkOrder> rows = new ArrayList<>(checkedQueue.rows().size());
        for (M3JepResidueQueue.Row row : checkedQueue.rows()) {
            String lane = proofLane(row.nextAction());
            String root =
                    root(
                            row,
                            lane,
                            dagRoot,
                            checkedTransfer.transferRoot());
            rows.add(
                    new WorkOrder(
                            row.order(),
                            row.release(),
                            row.jep(),
                            row.domain(),
                            row.disposition(),
                            row.priority(),
                            row.evidenceState(),
                            row.evidencePaths(),
                            lane,
                            row.nextAction(),
                            dagRoot,
                            checkedTransfer.transferRoot(),
                            root,
                            false,
                            false));
        }
        return new Plan(rows, dagRoot, checkedTransfer.transferRoot());
    }

    public static String proofLane(String nextAction) {
        return switch (Objects.requireNonNull(nextAction, "nextAction")) {
            case "PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT" -> "STANDARD_COMPATIBILITY_PROOF";
            case "CLOSE_DEPENDENCY_AND_HIGH_RISK_PROOF" -> "HIGH_RISK_DEPENDENCY_PROOF";
            case "CLOSE_JAVA21_COMPATIBILITY_POLICY" -> "JAVA21_COMPATIBILITY_POLICY_PROOF";
            case "RETAIN_RESEARCH_ONLY_UNTIL_STABLE_OR_EXPLICIT_OPT_IN" -> "RESEARCH_HOLD";
            case "CLOSE_HOTSPOT_JIT_DEPENDENCY_PROOF" -> "HOTSPOT_JIT_DEPENDENCY_PROOF";
            default -> throw new IllegalArgumentException("unknown residue next action: " + nextAction);
        };
    }

    public record Plan(
            List<WorkOrder> rows,
            String canonicalDagRoot,
            String nebulaTransferRoot) {
        public Plan {
            rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
            canonicalDagRoot = hash(canonicalDagRoot, "canonicalDagRoot");
            nebulaTransferRoot = hash(nebulaTransferRoot, "nebulaTransferRoot");
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("empty residue DAG plan");
            }
        }

        public String tsv() {
            StringBuilder out = new StringBuilder(HEADER).append('
');
            for (WorkOrder row : rows) {
                out.append(row.order()).append('	')
                        .append(row.release()).append('	')
                        .append(row.jep()).append('	')
                        .append(row.domain()).append('	')
                        .append(row.disposition()).append('	')
                        .append(row.priority()).append('	')
                        .append(row.evidenceState()).append('	')
                        .append(String.join(",", row.evidencePaths())).append('	')
                        .append(row.proofLane()).append('	')
                        .append(row.nextAction()).append('	')
                        .append(row.canonicalDagRoot()).append('	')
                        .append(row.nebulaTransferRoot()).append('	')
                        .append(row.workOrderRoot()).append('	')
                        .append(row.sourceMutationAuthority()).append('	')
                        .append(row.promotionAuthority()).append('
');
            }
            return out.toString();
        }

        public String root() {
            return sha256(framed(tsv()));
        }
    }

    public record WorkOrder(
            int order,
            int release,
            int jep,
            String domain,
            String disposition,
            int priority,
            String evidenceState,
            List<String> evidencePaths,
            String proofLane,
            String nextAction,
            String canonicalDagRoot,
            String nebulaTransferRoot,
            String workOrderRoot,
            boolean sourceMutationAuthority,
            boolean promotionAuthority) {
        public WorkOrder {
            if (order < 0 || release < 22 || release > 27 || jep <= 0 || priority < 0) {
                throw new IllegalArgumentException("invalid residue work order numeric field");
            }
            domain = text(domain, "domain");
            disposition = text(disposition, "disposition");
            evidenceState = text(evidenceState, "evidenceState");
            evidencePaths = List.copyOf(Objects.requireNonNull(evidencePaths, "evidencePaths"));
            proofLane = text(proofLane, "proofLane");
            nextAction = text(nextAction, "nextAction");
            canonicalDagRoot = hash(canonicalDagRoot, "canonicalDagRoot");
            nebulaTransferRoot = hash(nebulaTransferRoot, "nebulaTransferRoot");
            workOrderRoot = hash(workOrderRoot, "workOrderRoot");
            if (sourceMutationAuthority || promotionAuthority) {
                throw new IllegalArgumentException("residue work order may not carry authority");
            }
        }
    }

    private static String root(
            M3JepResidueQueue.Row row,
            String lane,
            String dagRoot,
            String transferRoot) {
        StringBuilder value = new StringBuilder();
        append(value, "M3_JEP_RESIDUE_WORK_ORDER_V1");
        append(value, Integer.toString(row.order()));
        append(value, Integer.toString(row.release()));
        append(value, Integer.toString(row.jep()));
        append(value, row.title());
        append(value, row.domain());
        append(value, row.disposition());
        append(value, Integer.toString(row.priority()));
        append(value, row.evidenceState());
        append(value, String.join(",", row.evidencePaths()));
        append(value, lane);
        append(value, row.nextAction());
        append(value, row.defaultJava21());
        append(value, row.priorityClassification());
        append(value, row.receiptState());
        append(value, row.promotion());
        append(value, row.receiptNextAction());
        append(value, dagRoot);
        append(value, transferRoot);
        append(value, "sourceMutationAuthority=false");
        append(value, "promotionAuthority=false");
        return sha256(value.toString());
    }

    private static void append(StringBuilder value, String field) {
        value.append(framed(Objects.requireNonNullElse(field, "")));
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf(' ') >= 0 || checked.indexOf('	') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String hash(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String framed(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return bytes.length + ":" + value;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
