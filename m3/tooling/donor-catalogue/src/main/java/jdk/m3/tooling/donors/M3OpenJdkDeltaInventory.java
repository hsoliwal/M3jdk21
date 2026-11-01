/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.donors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Produces a deterministic raw-file delta for an OpenJDK downstream donor from a local Git object database.
 *
 * <p>The command performs no fetch, checkout, merge, cherry-pick or source modification. Both pinned commit objects
 * must already be present. The unique merge base is computed rather than inferred from a release version.</p>
 */
public final class M3OpenJdkDeltaInventory {
    /** Exact header emitted for {@code DELTA.tsv}. */
    public static final String DELTA_HEADER = "donor_id\tmerge_base\tdonor_revision\tstatus\tscore\told_mode"
            + "\tnew_mode\told_blob\tnew_blob\told_path\tnew_path";
    /** Exact header emitted for {@code RECEIPT.tsv}. */
    public static final String RECEIPT_HEADER = "key\tvalue";

    /**
     * One raw Git delta row.
     *
     * @param donorId donor identity
     * @param mergeBase computed unique merge base
     * @param donorRevision exact donor commit
     * @param status Git raw-diff status letter
     * @param score rename or copy score, or dash
     * @param oldMode old Git mode
     * @param newMode new Git mode
     * @param oldBlob old Git object identity
     * @param newBlob new Git object identity
     * @param oldPath old repository-relative path
     * @param newPath new repository-relative path
     */
    public record DeltaRow(
            String donorId,
            String mergeBase,
            String donorRevision,
            String status,
            String score,
            String oldMode,
            String newMode,
            String oldBlob,
            String newBlob,
            String oldPath,
            String newPath) {

        String toTsv() {
            return String.join("\t", donorId, mergeBase, donorRevision, status, score, oldMode, newMode,
                    oldBlob, newBlob, oldPath, newPath);
        }
    }

    /**
     * Result of one deterministic inventory.
     *
     * @param mergeBase computed unique merge base
     * @param rows immutable sorted delta rows
     * @param deltaSha256 SHA-256 of the exact DELTA.tsv bytes
     */
    public record Result(String mergeBase, List<DeltaRow> rows, String deltaSha256) {
    }

    private M3OpenJdkDeltaInventory() {
    }

    /**
     * CLI: {@code M3OpenJdkDeltaInventory <catalogue-root> <donor-id> <git-repository> <output-directory>}.
     *
     * @param args catalogue root, donor identity, local Git repository and output directory
     * @throws IOException when Git or output operations fail
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: <catalogue-root> <donor-id> <git-repository> <output-directory>");
        }
        M3DonorCatalogue.Catalogue catalogue = M3DonorCatalogue.load(Path.of(args[0]));
        M3DonorCatalogue.Donor donor = catalogue.donors().get(args[1]);
        if (donor == null) {
            throw new IllegalArgumentException("unknown donor_id " + args[1]);
        }
        Result result = generate(donor, Path.of(args[2]), Path.of(args[3]));
        System.out.printf(Locale.ROOT,
                "M3_OPENJDK_DELTA\tdonor=%s\tmerge_base=%s\tentries=%d\tdelta_sha256=%s%n",
                donor.donorId(), result.mergeBase(), result.rows().size(), result.deltaSha256());
    }

    /**
     * Generates {@code DELTA.tsv} and {@code RECEIPT.tsv} for a pinned OpenJDK downstream donor.
     *
     * @param donor validated donor row
     * @param repository local Git repository containing both pinned commits
     * @param outputDirectory output directory
     * @return deterministic result
     * @throws IOException when Git or output operations fail
     */
    public static Result generate(
            M3DonorCatalogue.Donor donor, Path repository, Path outputDirectory) throws IOException {
        Objects.requireNonNull(donor, "donor");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        if (donor.family() != M3DonorCatalogue.DonorFamily.OPENJDK_DOWNSTREAM) {
            throw new IllegalArgumentException(donor.donorId() + " is not an OPENJDK_DOWNSTREAM donor");
        }
        if (donor.revision().equals("-") || donor.upstreamRevision().equals("-")) {
            throw new IllegalArgumentException(donor.donorId() + " lacks immutable donor/upstream revisions");
        }

        Path repo = repository.toAbsolutePath().normalize();
        if (!Files.isDirectory(repo)) {
            throw new IllegalArgumentException("Git repository directory does not exist: " + repo);
        }
        runText(repo, "git", "rev-parse", "--git-dir");
        String donorCommit = resolveCommit(repo, donor.revision());
        String upstreamCommit = resolveCommit(repo, donor.upstreamRevision());
        if (!donorCommit.equals(donor.revision())) {
            throw new IllegalStateException("donor revision resolved to an unexpected commit: " + donorCommit);
        }
        if (!upstreamCommit.equals(donor.upstreamRevision())) {
            throw new IllegalStateException("upstream revision resolved to an unexpected commit: " + upstreamCommit);
        }

        List<String> mergeBases = nonblankLines(runText(
                repo, "git", "merge-base", "--all", upstreamCommit, donorCommit));
        if (mergeBases.size() != 1) {
            throw new IllegalStateException("expected one merge base, found " + mergeBases.size());
        }
        String mergeBase = mergeBases.get(0);
        if (!mergeBase.matches("[0-9a-f]{40}|[0-9a-f]{64}")) {
            throw new IllegalStateException("invalid merge-base identity " + mergeBase);
        }

        byte[] raw = runBytes(repo, "git", "diff", "--raw", "-z", "--no-abbrev", "--find-renames=90%",
                mergeBase, donorCommit, "--");
        List<DeltaRow> rows = parseRawDiff(raw, donor.donorId(), mergeBase, donorCommit);
        rows.sort(Comparator.comparing(DeltaRow::newPath)
                .thenComparing(DeltaRow::oldPath)
                .thenComparing(DeltaRow::status)
                .thenComparing(DeltaRow::oldBlob)
                .thenComparing(DeltaRow::newBlob));

        List<String> deltaLines = new ArrayList<>();
        deltaLines.add(DELTA_HEADER);
        rows.forEach(row -> deltaLines.add(row.toTsv()));
        String deltaText = String.join("\n", deltaLines) + "\n";
        String deltaSha256 = sha256(deltaText.getBytes(StandardCharsets.UTF_8));

        Path out = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(out);
        Files.writeString(out.resolve("DELTA.tsv"), deltaText, StandardCharsets.UTF_8);

        Map<String, String> receipt = new java.util.TreeMap<>();
        receipt.put("delta_sha256", deltaSha256);
        receipt.put("donor_id", donor.donorId());
        receipt.put("donor_revision", donorCommit);
        receipt.put("entries", Integer.toString(rows.size()));
        receipt.put("merge_base", mergeBase);
        receipt.put("upstream_release_revision", upstreamCommit);
        List<String> receiptLines = new ArrayList<>();
        receiptLines.add(RECEIPT_HEADER);
        receipt.forEach((key, value) -> receiptLines.add(key + "\t" + value));
        Files.writeString(out.resolve("RECEIPT.tsv"), String.join("\n", receiptLines) + "\n",
                StandardCharsets.UTF_8);
        return new Result(mergeBase, List.copyOf(rows), deltaSha256);
    }

    static List<DeltaRow> parseRawDiff(byte[] raw, String donorId, String mergeBase, String donorRevision) {
        List<String> tokens = splitNullUtf8(raw);
        List<DeltaRow> rows = new ArrayList<>();
        int index = 0;
        while (index < tokens.size()) {
            String metadata = tokens.get(index++);
            if (metadata.isEmpty()) {
                continue;
            }
            if (!metadata.startsWith(":")) {
                throw new IllegalArgumentException("invalid raw Git metadata token: " + metadata);
            }
            String[] fields = metadata.substring(1).split(" ", -1);
            if (fields.length != 5) {
                throw new IllegalArgumentException("expected five raw Git metadata fields: " + metadata);
            }
            String oldMode = requireMode(fields[0]);
            String newMode = requireMode(fields[1]);
            String oldBlob = requireGitObject(fields[2]);
            String newBlob = requireGitObject(fields[3]);
            String statusAndScore = fields[4];
            if (statusAndScore.isEmpty()) {
                throw new IllegalArgumentException("missing Git delta status");
            }
            String status = statusAndScore.substring(0, 1);
            if (!"ACDMRTUXB".contains(status)) {
                throw new IllegalArgumentException("unsupported Git delta status " + statusAndScore);
            }
            String score = statusAndScore.length() == 1 ? "-" : statusAndScore.substring(1);
            if (!score.equals("-") && !score.matches("[0-9]{1,3}")) {
                throw new IllegalArgumentException("invalid rename/copy score " + score);
            }
            if (index >= tokens.size()) {
                throw new IllegalArgumentException("raw Git delta is missing a path");
            }
            String oldPath = requirePath(tokens.get(index++));
            String newPath = oldPath;
            if (status.equals("R") || status.equals("C")) {
                if (index >= tokens.size()) {
                    throw new IllegalArgumentException("rename/copy delta is missing its destination path");
                }
                newPath = requirePath(tokens.get(index++));
            }
            rows.add(new DeltaRow(donorId, mergeBase, donorRevision, status, score, oldMode, newMode,
                    oldBlob, newBlob, oldPath, newPath));
        }
        return rows;
    }

    private static String resolveCommit(Path repository, String revision) throws IOException {
        return runText(repository, "git", "rev-parse", "--verify", revision + "^{commit}").strip();
    }

    private static String runText(Path directory, String... command) throws IOException {
        return decodeUtf8(runBytes(directory, command));
    }

    private static byte[] runBytes(Path directory, String... command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(directory.toFile());
        builder.redirectErrorStream(false);
        Map<String, String> environment = builder.environment();
        environment.put("LC_ALL", "C");
        environment.put("LANG", "C");
        environment.put("GIT_OPTIONAL_LOCKS", "0");
        Process process = builder.start();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        AtomicReference<IOException> outFailure = new AtomicReference<>();
        AtomicReference<IOException> errFailure = new AtomicReference<>();
        Thread outThread = Thread.ofVirtual().unstarted(() -> transfer(process.getInputStream(), stdout, outFailure));
        Thread errThread = Thread.ofVirtual().unstarted(() -> transfer(process.getErrorStream(), stderr, errFailure));
        outThread.start();
        errThread.start();
        int exit;
        try {
            exit = process.waitFor();
            outThread.join();
            errThread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while executing " + String.join(" ", command), exception);
        }
        if (outFailure.get() != null) {
            throw new IOException("failed to read command stdout", outFailure.get());
        }
        if (errFailure.get() != null) {
            throw new IOException("failed to read command stderr", errFailure.get());
        }
        if (exit != 0) {
            throw new IOException("command failed (" + exit + "): " + String.join(" ", command)
                    + "\n" + decodeUtf8(stderr.toByteArray()));
        }
        return stdout.toByteArray();
    }

    private static void transfer(
            java.io.InputStream input, ByteArrayOutputStream output, AtomicReference<IOException> failure) {
        try (input) {
            input.transferTo(output);
        } catch (IOException exception) {
            failure.set(exception);
        }
    }

    private static List<String> splitNullUtf8(byte[] bytes) {
        List<String> tokens = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < bytes.length; index++) {
            if (bytes[index] == 0) {
                tokens.add(decodeUtf8(java.util.Arrays.copyOfRange(bytes, start, index)));
                start = index + 1;
            }
        }
        if (start != bytes.length) {
            throw new IllegalArgumentException("raw Git -z output did not end with NUL");
        }
        return tokens;
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Git output is not valid UTF-8", exception);
        }
    }

    private static List<String> nonblankLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line.strip());
            }
        }
        return lines;
    }

    private static String requireMode(String value) {
        if (!value.matches("[0-7]{6}")) {
            throw new IllegalArgumentException("invalid Git mode " + value);
        }
        return value;
    }

    private static String requireGitObject(String value) {
        if (!value.matches("[0-9a-f]{40}|[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid Git object identity " + value);
        }
        return value;
    }

    private static String requirePath(String value) {
        if (value.isEmpty() || value.startsWith("/") || value.startsWith("\\") || value.contains("\\")
                || value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("invalid repository-relative Git path " + value);
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw new IllegalArgumentException("path contains an empty or traversal segment: " + value);
            }
        }
        return value;
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the JDK", exception);
        }
    }
}
