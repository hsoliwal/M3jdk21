// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Fail-closed qualification receipt for the Nebula M3 proving pilot.
 *
 * <p>It does not perform tests or independently authenticate an external test log. It records
 * evidence provided by a trusted verifier and refuses to call missing, failed or unbound evidence
 * complete. Even a complete receipt only permits manual review, never source mutation or promotion.
 */
public final class NebulaM3ProofGate {
    public static final String SCHEMA = "NEBULA_M3_PILOT_PROOF_V1";
    public static final String UNBOUND = "UNBOUND";
    public static final String NO_EVIDENCE = "-";

    public enum Stage {
        SOURCE_INVENTORY,
        RECIPE_JUNIT,
        ATOM_BEHAVIOR,
        PATTERN_IOP,
        DOCUMENTATION,
        JAVA21_COMPILE,
        FIXED_POINT,
        SEMANTIC_COVERAGE_99,
        M3_REACTOR,
        TYCHO_REACTOR,
        SWT_PLATFORM_RUNTIME,
        DIFF_AND_PROVENANCE
    }

    public enum State { NOT_RUN, HOLD, FAILED, PASSED }

    public record Evidence(Stage stage, State state, String proofSha256) {
        public Evidence {
            stage = Objects.requireNonNull(stage, "stage");
            state = Objects.requireNonNull(state, "state");
            proofSha256 = Objects.requireNonNull(proofSha256, "proofSha256");
            if (state == State.PASSED ? !hex(proofSha256, 64) : !NO_EVIDENCE.equals(proofSha256)) {
                throw new IllegalArgumentException("invalid proof fingerprint for " + stage);
            }
        }
    }

    public record Receipt(
            String nebulaCommit,
            String canonicalSynexiaCommit,
            String dagRoot,
            List<Evidence> evidence) {
        public Receipt {
            nebulaCommit = requireCommit(nebulaCommit, "nebulaCommit");
            canonicalSynexiaCommit = requireCommit(canonicalSynexiaCommit, "canonicalSynexiaCommit");
            if (!hex(dagRoot, 64)) throw new IllegalArgumentException("dagRoot");
            evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
            if (evidence.size() != Stage.values().length) {
                throw new IllegalArgumentException("every proof stage must be represented exactly once");
            }
            for (int index = 0; index < evidence.size(); index++) {
                if (evidence.get(index).stage() != Stage.values()[index]) {
                    throw new IllegalArgumentException("proof stages must follow canonical order");
                }
            }
            if ((UNBOUND.equals(nebulaCommit) || UNBOUND.equals(canonicalSynexiaCommit))
                    && evidence.stream().anyMatch(row -> row.state() == State.PASSED)) {
                throw new IllegalArgumentException("unbound source identity cannot have PASS evidence");
            }
        }

        public boolean complete() {
            return !UNBOUND.equals(nebulaCommit)
                    && !UNBOUND.equals(canonicalSynexiaCommit)
                    && evidence.stream().allMatch(row -> row.state() == State.PASSED);
        }

        public String disposition() {
            return complete() ? "MANUAL_REVIEW_REQUIRED" : "HOLD";
        }

        /** Passing evidence does not authorize target edits, merges or release promotion. */
        public boolean mutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }

        public String root() {
            StringBuilder framed = new StringBuilder();
            appendField(framed, SCHEMA);
            appendField(framed, nebulaCommit);
            appendField(framed, canonicalSynexiaCommit);
            appendField(framed, dagRoot);
            for (Evidence row : evidence) {
                appendField(framed, row.stage().name());
                appendField(framed, row.state().name());
                appendField(framed, row.proofSha256());
            }
            appendField(framed, disposition());
            appendField(framed, "mutation=false;promotion=false");
            try {
                return HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256")
                                .digest(framed.toString().getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new ExceptionInInitializerError(impossible);
            }
        }

        public String toTsv() {
            StringBuilder out = new StringBuilder(
                    "schema\tnebula_commit\tsynexia_commit\tdag_root\tstage\tstate\tproof_sha256"
                    + "\tdisposition\tmutation_authority\tpromotion_authority\treceipt_root\n");
            String receiptRoot = root();
            for (Evidence row : evidence) {
                out.append(SCHEMA).append('\t').append(nebulaCommit).append('\t')
                        .append(canonicalSynexiaCommit).append('\t').append(dagRoot).append('\t')
                        .append(row.stage()).append('\t').append(row.state()).append('\t')
                        .append(row.proofSha256()).append('\t').append(disposition())
                        .append("\tfalse\tfalse\t").append(receiptRoot).append('\n');
            }
            return out.toString();
        }
    }

    private NebulaM3ProofGate() { }

    /** The only default when the host has not supplied independently verified receipts. */
    public static Receipt pending(String dagRoot) {
        List<Evidence> evidence = new ArrayList<>();
        for (Stage stage : Stage.values()) {
            evidence.add(new Evidence(stage, State.NOT_RUN, NO_EVIDENCE));
        }
        return new Receipt(UNBOUND, UNBOUND, dagRoot, evidence);
    }

    private static String requireCommit(String commit, String field) {
        if (!UNBOUND.equals(commit) && !hex(commit, 40)) {
            throw new IllegalArgumentException(field);
        }
        return commit;
    }

    private static boolean hex(String value, int length) {
        return value != null && value.length() == length
                && value.chars().allMatch(c -> c >= '0' && c <= '9'
                        || c >= 'a' && c <= 'f');
    }

    private static void appendField(StringBuilder target, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        target.append(bytes.length).append(':').append(value);
    }
}
