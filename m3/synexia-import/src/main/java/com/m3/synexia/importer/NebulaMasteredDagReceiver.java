// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.util.List;
import java.util.Objects;

/**
 * Fail-closed receiver for the Nebula-proven portable M3 recipe DAG.
 *
 * <p>Nebula is a proving target, not an authority to mutate OpenJDK. This receiver admits
 * mechanical reuse only after one exact content-bound proof records green recipe-first,
 * 99-percent semantic coverage, fixed-point and original-build gates.</p>
 */
public final class NebulaMasteredDagReceiver {
    public static final String SCHEMA = "M3_NEBULA_MASTERED_DAG_RECEIVER_V1";
    public static final String EXPECTED_PROOF_REPOSITORY = "hsoliwal/nebula";
    public static final String EXPECTED_TARGET = "hsoliwal/M3jdk21";

    public enum Gate {
        SUCCESS,
        FAILURE,
        PENDING
    }

    public record Receipt(
            String proofRepository,
            String proofBranch,
            int proofPr,
            String proofCommit,
            String proofState,
            Gate recipeFirst,
            Gate finalTransfer,
            String semanticCoverage,
            Gate fixedPoint,
            Gate originalBuild,
            String targetRepository,
            String authority) {
        public Receipt {
            proofRepository = token(proofRepository, "proofRepository");
            proofBranch = token(proofBranch, "proofBranch");
            if (proofPr <= 0) throw new IllegalArgumentException("proofPr");
            proofCommit = sha(proofCommit, "proofCommit");
            proofState = token(proofState, "proofState");
            recipeFirst = Objects.requireNonNull(recipeFirst, "recipeFirst");
            finalTransfer = Objects.requireNonNull(finalTransfer, "finalTransfer");
            semanticCoverage = token(semanticCoverage, "semanticCoverage");
            fixedPoint = Objects.requireNonNull(fixedPoint, "fixedPoint");
            originalBuild = Objects.requireNonNull(originalBuild, "originalBuild");
            targetRepository = token(targetRepository, "targetRepository");
            authority = token(authority, "authority");
            if (!EXPECTED_PROOF_REPOSITORY.equals(proofRepository)) {
                throw new IllegalArgumentException("unexpected proof repository");
            }
            if (!EXPECTED_TARGET.equals(targetRepository)) {
                throw new IllegalArgumentException("unexpected target repository");
            }
            if (!"EVIDENCE_ONLY".equals(authority)) {
                throw new IllegalArgumentException("receiver authority must remain evidence-only");
            }
        }

        /** True only for an exact fully green proving receipt. */
        public boolean admissibleForMechanicalApplication() {
            return "VERIFIED_GREEN".equals(proofState)
                    && recipeFirst == Gate.SUCCESS
                    && finalTransfer == Gate.SUCCESS
                    && "LINE>=0.99".equals(semanticCoverage)
                    && fixedPoint == Gate.SUCCESS
                    && originalBuild == Gate.SUCCESS;
        }

        public void requireAdmissibleForMechanicalApplication() {
            if (!admissibleForMechanicalApplication()) {
                throw new IllegalStateException(
                        "Nebula recipe DAG is not proven for M3JDK21 mechanical application: "
                                + proofCommit);
            }
        }
    }

    private NebulaMasteredDagReceiver() {}

    public static Receipt parse(String tsv) {
        List<String> lines = Objects.requireNonNull(tsv, "tsv").lines()
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.size() != 2) throw new IllegalArgumentException("receiver requires one receipt row");

        String header =
                "schema\tproof_repository\tproof_branch\tproof_pr\tproof_commit\tproof_state\t"
                        + "recipe_first\tfinal_transfer\tsemantic_coverage\tfixed_point\t"
                        + "original_build\ttarget_repository\tauthority";
        if (!header.equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected receiver header");
        }

        String[] cells = lines.get(1).split("\t", -1);
        if (cells.length != 13) throw new IllegalArgumentException("invalid receiver row");
        if (!SCHEMA.equals(cells[0])) throw new IllegalArgumentException("unexpected receiver schema");

        return new Receipt(
                cells[1],
                cells[2],
                parsePositiveInt(cells[3], "proofPr"),
                cells[4],
                cells[5],
                gate(cells[6], "recipeFirst"),
                gate(cells[7], "finalTransfer"),
                cells[8],
                gate(cells[9], "fixedPoint"),
                gate(cells[10], "originalBuild"),
                cells[11],
                cells[12]);
    }

    private static Gate gate(String value, String field) {
        try {
            return Gate.valueOf(token(value, field));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(field, invalid);
        }
    }

    private static int parsePositiveInt(String value, String field) {
        try {
            int parsed = Integer.parseInt(token(value, field));
            if (parsed <= 0) throw new IllegalArgumentException(field);
            return parsed;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException(field, invalid);
        }
    }

    private static String sha(String value, String field) {
        String checked = token(value, field);
        if (!checked.matches("[0-9a-f]{40}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
