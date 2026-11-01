/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.compiler.profiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Package-private canonical encoding, validation and hashing primitives. */
final class M3ExecutionProfileFormat {
    static final String FORMAT_VERSION = "M3-EXECUTION-PROFILE/1";
    static final String BUNDLE_HEADER = "bundle_id\tformat_version\truntime_commit\tworkload_sha256"
            + "\tcapture_target\tdeployment_target\tproducer\tproducer_version";
    static final String OBSERVATIONS_HEADER = "observation_id\tkind\tsubject\tcontext\tphase\tcount"
            + "\tevidence_ref\tevidence_sha256";
    static final String BUNDLE_PREFIX = "m3p1:";
    static final String OBSERVATION_PREFIX = "m3o1:";

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern GIT_ID = Pattern.compile("(?:[0-9a-f]{40}|[0-9a-f]{64})");
    private static final Pattern TARGET = Pattern.compile("[a-z0-9][a-z0-9._+\\-]{0,127}");
    private static final Pattern PRODUCER = Pattern.compile("[a-z0-9][a-z0-9._+\\-]{0,127}");
    private static final Pattern VERSION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._+\\-]{0,127}");
    private static final Pattern ASCII_DESCRIPTOR = Pattern.compile("[!-~]{1,2048}");

    private M3ExecutionProfileFormat() {
    }

    static String observationId(
            M3ExecutionProfileKind kind,
            String subject,
            String context,
            M3ExecutionProfilePhase phase,
            long count,
            String evidenceRef,
            String evidenceSha256) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(phase, "phase");
        requireDescriptor("subject", subject, false);
        requireDescriptor("context", context, true);
        requireCount(count);
        requireEvidenceRef(evidenceRef);
        requireSha256("evidence_sha256", evidenceSha256);
        String canonical = String.join("\t", kind.name(), subject, context, phase.name(), Long.toString(count),
                evidenceRef, evidenceSha256);
        return OBSERVATION_PREFIX + sha256("M3-EXECUTION-OBSERVATION/1\n" + canonical + "\n");
    }

    static String bundleId(
            String runtimeCommit,
            String workloadSha256,
            String captureTarget,
            String deploymentTarget,
            String producer,
            String producerVersion,
            List<M3ExecutionProfileObservation> observations) {
        requireRuntimeCommit(runtimeCommit);
        requireSha256("workload_sha256", workloadSha256);
        requireTarget("capture_target", captureTarget);
        requireTarget("deployment_target", deploymentTarget);
        requireProducer(producer);
        requireVersion(producerVersion);
        Objects.requireNonNull(observations, "observations");
        StringBuilder canonical = new StringBuilder(256 + observations.size() * 160);
        canonical.append("M3-EXECUTION-PROFILE/1\n");
        canonical.append(String.join("\t", FORMAT_VERSION, runtimeCommit, workloadSha256, captureTarget,
                deploymentTarget, producer, producerVersion)).append('\n');
        canonical.append("--OBSERVATIONS--\n");
        for (M3ExecutionProfileObservation observation : observations) {
            canonical.append(observation.row()).append('\n');
        }
        return BUNDLE_PREFIX + sha256(canonical.toString());
    }

    static void requireBundleId(String value) {
        requirePrefixedIdentity("bundle_id", value, BUNDLE_PREFIX);
    }

    static void requireObservationId(String value) {
        requirePrefixedIdentity("observation_id", value, OBSERVATION_PREFIX);
    }

    static void requireRuntimeCommit(String value) {
        if (value == null || !GIT_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("runtime_commit must be a lowercase 40 or 64 hexadecimal Git identity");
        }
    }

    static void requireSha256(String name, String value) {
        if (value == null || !SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
    }

    static void requireTarget(String name, String value) {
        if (value == null || !TARGET.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be a normalized lowercase target token");
        }
    }

    static void requireProducer(String value) {
        if (value == null || !PRODUCER.matcher(value).matches()) {
            throw new IllegalArgumentException("producer must be a normalized lowercase stable identity");
        }
    }

    static void requireVersion(String value) {
        if (value == null || !VERSION.matcher(value).matches()) {
            throw new IllegalArgumentException("producer_version must be a stable printable version token");
        }
    }

    static void requireDescriptor(String name, String value, boolean dashAllowed) {
        if (value == null || !ASCII_DESCRIPTOR.matcher(value).matches() || (!dashAllowed && value.equals("-"))) {
            throw new IllegalArgumentException(name + " must be a nonblank printable ASCII canonical descriptor");
        }
    }

    static void requireEvidenceRef(String value) {
        requireDescriptor("evidence_ref", value, false);
        boolean windowsAbsolute = value.length() >= 3 && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':' && value.charAt(2) == '/';
        if (value.startsWith("/") || value.startsWith("\\") || value.contains("\\") || value.contains(":")
                || value.endsWith("/") || windowsAbsolute) {
            throw new IllegalArgumentException("evidence_ref must be a normalized portable relative path");
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw new IllegalArgumentException("evidence_ref contains an empty or traversal segment");
            }
        }
    }

    static void requireCount(long count) {
        if (count <= 0L) {
            throw new IllegalArgumentException("count must be positive");
        }
    }

    static long parseCount(String value) {
        if (value == null || !value.matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException("count must be an unsigned decimal without sign or leading zero");
        }
        try {
            long parsed = Long.parseLong(value);
            requireCount(parsed);
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("count exceeds Long.MAX_VALUE", exception);
        }
    }

    static List<String> readStrictLines(Path path, String header) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            throw invalid(path, 1, "UTF-8 byte-order marks are not permitted");
        }
        if (text.indexOf('\r') >= 0) {
            throw invalid(path, 1, "carriage returns are not permitted; use LF");
        }
        if (!text.endsWith("\n") || text.endsWith("\n\n")) {
            throw invalid(path, 1, "file must end with exactly one LF");
        }
        List<String> lines = List.of(text.substring(0, text.length() - 1).split("\n", -1));
        if (lines.isEmpty() || !header.equals(lines.get(0))) {
            throw invalid(path, 1, "expected exact header: " + header);
        }
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).isEmpty()) {
                throw invalid(path, index + 1, "blank lines are not permitted");
            }
        }
        return lines;
    }

    static String[] cells(String line, int expected, Path path, int lineNumber) {
        String[] cells = line.split("\\t", -1);
        if (cells.length != expected) {
            throw invalid(path, lineNumber, "expected " + expected + " cells, found " + cells.length);
        }
        return cells;
    }

    static <E extends Enum<E>> E enumValue(Class<E> type, String value, Path path, int lineNumber) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw invalid(path, lineNumber, "invalid " + type.getSimpleName() + " value " + value);
        }
    }

    static IllegalArgumentException invalid(Path path, int lineNumber, String message) {
        return new IllegalArgumentException(path + ":" + lineNumber + ": " + message);
    }

    private static void requirePrefixedIdentity(String name, String value, String prefix) {
        if (value == null || !value.startsWith(prefix)
                || !SHA256.matcher(value.substring(prefix.length())).matches()) {
            throw new IllegalArgumentException(name + " must be " + prefix + " followed by lowercase SHA-256");
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the JDK", exception);
        }
    }
}
