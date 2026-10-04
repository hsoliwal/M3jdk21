// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pass;

import com.m3.indexdb.M3IndexDbSemanticFingerprint;
import java.util.List;
import java.util.Objects;

/** Immutable proof receipt for one bounded pass iteration. */
public record M3PassReceipt(
        int passOrdinal,
        int iteration,
        String passId,
        String inputStateRoot,
        String outputStateRoot,
        String proofRoot,
        String exactCommit,
        Status status,
        boolean changed,
        boolean stopConditionSatisfied,
        String receiptRoot) {

    public enum Status {
        PASSED,
        FAILED,
        BLOCKED
    }

    public M3PassReceipt {
        if (passOrdinal < 0) throw new IllegalArgumentException("passOrdinal");
        if (iteration < 0) throw new IllegalArgumentException("iteration");
        passId = token(passId, "passId");
        inputStateRoot = sha(inputStateRoot, "inputStateRoot");
        outputStateRoot = sha(outputStateRoot, "outputStateRoot");
        proofRoot = sha(proofRoot, "proofRoot");
        exactCommit = sha(exactCommit, "exactCommit");
        status = Objects.requireNonNull(status, "status");

        String expected = computeRoot(
                passOrdinal,
                iteration,
                passId,
                inputStateRoot,
                outputStateRoot,
                proofRoot,
                exactCommit,
                status,
                changed,
                stopConditionSatisfied);
        receiptRoot = receiptRoot == null || receiptRoot.isBlank()
                ? expected
                : sha(receiptRoot, "receiptRoot");
        if (!receiptRoot.equals(expected)) {
            throw new IllegalArgumentException("receiptRoot mismatch");
        }
        if (status != Status.PASSED && stopConditionSatisfied) {
            throw new IllegalArgumentException(
                    "failed/blocked pass cannot satisfy stop condition");
        }
        if (!changed && !inputStateRoot.equals(outputStateRoot)) {
            throw new IllegalArgumentException(
                    "unchanged pass must preserve state root");
        }
    }

    public static M3PassReceipt create(
            int passOrdinal,
            int iteration,
            String passId,
            String inputStateRoot,
            String outputStateRoot,
            String proofRoot,
            String exactCommit,
            Status status,
            boolean changed,
            boolean stopConditionSatisfied) {
        return new M3PassReceipt(
                passOrdinal,
                iteration,
                passId,
                inputStateRoot,
                outputStateRoot,
                proofRoot,
                exactCommit,
                status,
                changed,
                stopConditionSatisfied,
                "");
    }

    private static String computeRoot(
            int passOrdinal,
            int iteration,
            String passId,
            String inputStateRoot,
            String outputStateRoot,
            String proofRoot,
            String exactCommit,
            Status status,
            boolean changed,
            boolean stopConditionSatisfied) {
        return M3IndexDbSemanticFingerprint.leaf(
                        "M3_PASS_RECEIPT_V1",
                        List.of(
                                "PASS:" + passOrdinal,
                                "ITERATION:" + iteration,
                                "STATUS:" + status,
                                "CHANGED:" + changed,
                                "STOP:" + stopConditionSatisfied),
                        List.of(
                                passId,
                                inputStateRoot,
                                outputStateRoot,
                                proofRoot,
                                exactCommit),
                        passId + "|" + passOrdinal + "|" + iteration)
                .logicSha256();
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
