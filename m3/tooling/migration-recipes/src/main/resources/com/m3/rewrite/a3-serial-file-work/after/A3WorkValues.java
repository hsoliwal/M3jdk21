// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.util.Objects;
import java.util.Set;

/** Shared value/contract validation for A3 serial FILE work evidence. */
final class A3WorkValues {

    private static final Set<String> SCOPES =
            Set.of("FILE", "PACKAGE", "MODULE", "MULTI_MODULE", "LIBRARY_API");

    private A3WorkValues() {}

    static void validate(
            int featureOrder,
            int fileOrder,
            int release,
            String commit,
            String jbsIds,
            String subject,
            String domain,
            String risk,
            String joinScope,
            String proofLane,
            String recipeStrategy,
            int priority,
            String compatibilityState,
            String nextAction,
            String path,
            A3Work.TargetState targetState,
            A3Inv.Kind kind,
            String targetSha256,
            A3Work.PrepareLane prepareLane,
            String atomScope) {
        if (featureOrder < 0 || fileOrder < 0 || release < 22 || priority < 0) {
            throw new IllegalArgumentException("numeric work coordinate");
        }
        gitSha(commit);
        clean(jbsIds);
        text(subject, "subject");
        text(domain, "domain");
        text(risk, "risk");
        scope(joinScope);
        text(proofLane, "proofLane");
        text(recipeStrategy, "recipeStrategy");
        text(compatibilityState, "compatibilityState");
        text(nextAction, "nextAction");
        Objects.requireNonNull(targetState, "targetState");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(prepareLane, "prepareLane");
        if (!"FILE".equals(scope(atomScope))) {
            throw new IllegalArgumentException("A3 work atoms must remain FILE-local");
        }
        validateTarget(path, targetState, targetSha256);
    }

    static String sourcePath(String value) {
        String checked = text(value, "path").replace('\\', '/');
        if (checked.startsWith("/") || checked.contains("//")) {
            throw new IllegalArgumentException("path");
        }
        for (String part : checked.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException("path");
            }
        }
        return checked;
    }

    static String gitSha(String value) {
        String checked = text(value, "commit").toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("commit");
        }
        return checked;
    }

    static String sha256(String value) {
        String checked = text(value, "sha256").toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sha256");
        }
        return checked;
    }

    static String scope(String value) {
        String checked = text(value, "scope");
        if (!SCOPES.contains(checked)) {
            throw new IllegalArgumentException("scope");
        }
        return checked;
    }

    static int parseInt(String value, String field) {
        long parsed = parseLong(value, field);
        if (parsed > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(field);
        }
        return (int) parsed;
    }

    static long parseLong(String value, String field) {
        try {
            long parsed = Long.parseLong(text(value, field));
            if (parsed < 0) {
                throw new IllegalArgumentException(field);
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(field, failure);
        }
    }

    static String text(String value, String field) {
        String checked = clean(value);
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    static String clean(String value) {
        String checked = Objects.toString(value, "").strip();
        if (checked.indexOf('\0') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException("TSV value");
        }
        return checked;
    }

    private static void validateTarget(
            String path,
            A3Work.TargetState targetState,
            String targetSha256) {
        if (targetState == A3Work.TargetState.FEATURE) {
            if (!path.isEmpty() || !"FEATURE".equals(targetSha256)) {
                throw new IllegalArgumentException("feature residue target");
            }
            return;
        }
        sourcePath(path);
        if (targetState == A3Work.TargetState.PRESENT) {
            sha256(targetSha256);
            return;
        }
        String expected =
                targetState == A3Work.TargetState.ABSENT ? "ABSENT" : "UNSCANNED";
        if (!expected.equals(targetSha256)) {
            throw new IllegalArgumentException("targetSha256");
        }
    }
}
