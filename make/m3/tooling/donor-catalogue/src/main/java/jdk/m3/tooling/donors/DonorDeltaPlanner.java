/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.donors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Read-only, source-sealed vendor-delta candidate inventory for a downstream JDK Git fork.
 *
 * <p>The exact baseline must be an ancestor of the exact donor revision in the same object
 * database. Candidates are never executable patches; every output row is HOLD_UNQUALIFIED.
 * This tool cannot authorize code-copy, a JDK contract change, or JNI/native promotion.</p>
 */
public final class DonorDeltaPlanner {
    /** Output schema version; changing it requires a reviewed recipe and fresh evidence. */
    public static final String SCHEMA = "M3-DONOR-DELTA/1";
    /** Exact candidate ledger header. */
    public static final String CANDIDATES_HEADER = "candidate_id\tdonor_id\tpath\tchange\tbaseline_mode"
            + "\tbaseline_type\tbaseline_object\tdonor_mode\tdonor_type\tdonor_object"
            + "\tsource_family\timport_decision\tqualification";
    /** Exact provenance receipt header. */
    public static final String RECEIPT_HEADER = "key\tvalue";
    private static final Pattern FULL_SHA = Pattern.compile("(?:[a-f0-9]{40}|[a-f0-9]{64})");
    private static final Pattern SAFE_ID = Pattern.compile("[a-z][a-z0-9._-]{0,127}");
    private static final int MAX_OUTPUT_BYTES = 128 * 1024 * 1024;

    private DonorDeltaPlanner() {
    }

    /**
     * CLI entry point: exact full commit IDs only, never a mutable branch or HEAD.
     *
     * @param args checkout, baseline SHA, donor SHA, donor ID, output directory
     * @throws IOException when Git inspection or immutable output fails
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            throw new IllegalArgumentException("usage: <donor-git-root> <baseline-full-sha> <donor-full-sha>"
                    + " <donor-id> <output-dir>");
        }
        Result result = generate(Path.of(args[0]), args[1], args[2], args[3], Path.of(args[4]));
        System.out.printf(Locale.ROOT, "M3_DONOR_DELTA\tdonor=%s\tcandidates=%d\tsha256=%s\tstatus=HOLD%n",
                result.donorId(), result.candidateCount(), result.candidatesSha256());
    }

    /**
     * Scans the committed tree difference, without reading or modifying the worktree.
     *
     * @param checkout existing donor repository root
     * @param baselineFullSha reviewed upstream commit within this exact Git graph
     * @param donorFullSha reviewed downstream donor commit within this exact Git graph
     * @param donorId stable provenance identifier, not a source path
     * @param outputDir write-once destination for two TSV artifacts
     * @return immutable bounded candidate and receipt identities
     * @throws IOException on Git or artifact I/O failure
     */
    public static Result generate(Path checkout, String baselineFullSha, String donorFullSha,
            String donorId, Path outputDir) throws IOException {
        Objects.requireNonNull(checkout, "checkout");
        Objects.requireNonNull(outputDir, "outputDir");
        requireSha(baselineFullSha, "baseline");
        requireSha(donorFullSha, "donor");
        if (donorId == null || !SAFE_ID.matcher(donorId).matches()) {
            throw new IllegalArgumentException("unsafe donor id");
        }
        Path root = checkout.toRealPath();
        if (!Files.isDirectory(root) || Files.isSymbolicLink(checkout)) {
            throw new IllegalArgumentException("donor checkout must be a real directory");
        }
        String actualRoot = ascii(git(root, "rev-parse", "--show-toplevel")).trim();
        if (!root.equals(Path.of(actualRoot).toRealPath())) {
            throw new IllegalArgumentException("checkout must be the exact Git root, not a subdirectory");
        }
        String format = ascii(git(root, "rev-parse", "--show-object-format")).trim();
        int oidLength = switch (format) {
            case "sha1" -> 40;
            case "sha256" -> 64;
            default -> throw new IllegalArgumentException("unknown Git object format " + format);
        };
        if (baselineFullSha.length() != oidLength || donorFullSha.length() != oidLength) {
            throw new IllegalArgumentException("commit IDs must match repository object format");
        }
        String base = ascii(git(root, "rev-parse", "--verify", "--end-of-options",
                baselineFullSha + "^{commit}")).trim();
        String head = ascii(git(root, "rev-parse", "--verify", "--end-of-options",
                donorFullSha + "^{commit}")).trim();
        if (!base.equals(baselineFullSha) || !head.equals(donorFullSha)) {
            throw new IllegalArgumentException("revision is not the exact full commit identity");
        }
        git(root, "merge-base", "--is-ancestor", base, head);
        String baseTree = ascii(git(root, "rev-parse", base + "^{tree}")).trim();
        String headTree = ascii(git(root, "rev-parse", head + "^{tree}")).trim();
        DonorTreeLanes before = tree(root, base, oidLength);
        DonorTreeLanes after = tree(root, head, oidLength);

        List<String> rows = new ArrayList<>();
        int[] changeCounts = new int[5];
        int beforeAt = 0;
        int afterAt = 0;
        while (beforeAt < before.size() || afterAt < after.size()) {
            int left = -1;
            int right = -1;
            String path;
            if (beforeAt == before.size()) {
                right = afterAt++;
                path = after.path(right);
            } else if (afterAt == after.size()) {
                left = beforeAt++;
                path = before.path(left);
            } else {
                int comparison = before.path(beforeAt).compareTo(after.path(afterAt));
                if (comparison < 0) {
                    left = beforeAt++;
                    path = before.path(left);
                } else if (comparison > 0) {
                    right = afterAt++;
                    path = after.path(right);
                } else {
                    left = beforeAt++;
                    right = afterAt++;
                    path = before.path(left);
                }
            }
            if (left >= 0 && right >= 0 && before.same(left, after, right)) {
                continue;
            }
            String change = change(before, left, after, right);
            changeCounts[changeIndex(change)]++;
            String candidateId = "m3:donor:" + sha256("M3-DONOR-ATOM/1\n" + donorId + "\n" + path + "\n");
            rows.add(String.join("\t", candidateId, donorId, path, change,
                    cell(before, left, "mode"), cell(before, left, "type"), cell(before, left, "object"),
                    cell(after, right, "mode"), cell(after, right, "type"), cell(after, right, "object"),
                    family(path), "REVIEW_ONLY", "HOLD_UNQUALIFIED"));
        }
        String candidates = table(CANDIDATES_HEADER, rows);
        String digest = sha256(candidates);
        List<String> receiptRows = new ArrayList<>(List.of(
                "schema\t" + SCHEMA,
                "donor_id\t" + donorId,
                "git_format\t" + format,
                "baseline_commit\t" + base,
                "donor_commit\t" + head,
                "baseline_tree\t" + baseTree,
                "donor_tree\t" + headTree,
                "baseline_file_count\t" + before.size(),
                "donor_file_count\t" + after.size(),
                "candidate_count\t" + rows.size(),
                "candidates_sha256\t" + digest,
                "source_sha256_gate\tNOT_RUN",
                "license_gate\tNOT_RUN",
                "contract_gate\tNOT_RUN",
                "jdk21_product_gate\tNOT_RUN",
                "jni_native_gate\tNOT_RUN",
                "performance_gate\tNOT_RUN",
                "automatic_import\tFORBIDDEN"));
        String[] changeNames = {"ADDED", "MODE_CHANGED", "MODIFIED", "REMOVED", "TYPE_CHANGED"};
        for (int i = 0; i < changeNames.length; i++) {
            if (changeCounts[i] > 0) {
                receiptRows.add("change_" + changeNames[i].toLowerCase(Locale.ROOT)
                        + "\t" + changeCounts[i]);
            }
        }
        String receipt = table(RECEIPT_HEADER, receiptRows);
        // Preflight both paths before writing either; never overwrite a different receipt.
        Path output = outputDir.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(output)) {
            throw new IOException("symlink output directory prohibited");
        }
        Path candidatesPath = output.resolve("CANDIDATES.tsv");
        Path receiptPath = output.resolve("RECEIPT.tsv");
        preflight(candidatesPath, candidates);
        preflight(receiptPath, receipt);
        Files.createDirectories(output);
        writeOnce(candidatesPath, candidates);
        writeOnce(receiptPath, receipt);
        return new Result(donorId, base, head, rows.size(), digest, candidatesPath, receiptPath);
    }

    private static String change(DonorTreeLanes before, int left,
            DonorTreeLanes after, int right) {
        if (left < 0) return "ADDED";
        if (right < 0) return "REMOVED";
        if (!before.type(left).equals(after.type(right))) return "TYPE_CHANGED";
        if (!before.mode(left).equals(after.mode(right))) return "MODE_CHANGED";
        return "MODIFIED";
    }

    private static int changeIndex(String change) {
        return switch (change) {
            case "ADDED" -> 0;
            case "MODE_CHANGED" -> 1;
            case "MODIFIED" -> 2;
            case "REMOVED" -> 3;
            case "TYPE_CHANGED" -> 4;
            default -> throw new AssertionError("unexpected change");
        };
    }

    private static String cell(DonorTreeLanes lanes, int index, String part) {
        if (index < 0) return "-";
        return switch (part) {
            case "mode" -> lanes.mode(index);
            case "type" -> lanes.type(index);
            case "object" -> lanes.objectId(index);
            default -> throw new AssertionError("unknown cell selector");
        };
    }

    private static String family(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java")) {
            return "JAVA";
        }
        if (lower.endsWith(".ad")) {
            return "HOTSPOT_ARCH_RULE";
        }
        if (lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx")) {
            return "CPP";
        }
        if (lower.endsWith(".c")) {
            return "C";
        }
        if (lower.endsWith(".h") || lower.endsWith(".hpp")) {
            return "NATIVE_HEADER";
        }
        if (lower.endsWith(".s") || lower.endsWith(".asm")) {
            return "ASSEMBLY";
        }
        if (lower.endsWith(".gmk") || lower.equals("makefile") || lower.endsWith("/makefile")
                || lower.endsWith("pom.xml")) {
            return "BUILD";
        }
        return "OTHER";
    }

    private static DonorTreeLanes tree(Path root, String commit, int oidLength) throws IOException {
        byte[] payload = git(root, "ls-tree", "-r", "-z", "--full-tree", commit);
        DonorTreeLanes result = new DonorTreeLanes();
        int start = 0;
        for (int i = 0; i < payload.length; i++) {
            if (payload[i] != 0) {
                continue;
            }
            byte[] record = Arrays.copyOfRange(payload, start, i);
            start = i + 1;
            int tab = -1;
            for (int j = 0; j < record.length; j++) {
                if (record[j] == '\t') {
                    tab = j;
                    break;
                }
            }
            if (tab <= 0 || tab == record.length - 1) {
                throw new IOException("invalid git ls-tree record");
            }
            String header = ascii(Arrays.copyOfRange(record, 0, tab));
            String[] parts = header.split(" ", -1);
            if (parts.length != 3 || !parts[0].matches("(?:100644|100755|120000|160000)")
                    || !parts[1].matches("blob|commit") || parts[2].length() != oidLength
                    || !FULL_SHA.matcher(parts[2]).matches()) {
                throw new IOException("invalid tree entry header");
            }
            String path = utf8(Arrays.copyOfRange(record, tab + 1, record.length));
            validatePath(path);
            result.add(path, parts[0], parts[1], parts[2]);
        }
        if (start != payload.length) {
            throw new IOException("unterminated git ls-tree -z record");
        }
        result.freeze();
        return result;
    }

    private static void validatePath(String path) throws IOException {
        if (path.startsWith("/") || path.indexOf('\\') >= 0 || path.indexOf('\t') >= 0
                || path.indexOf('\n') >= 0 || path.indexOf('\r') >= 0 || path.isBlank()) {
            throw new IOException("unsafe Git source path");
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("unnormalized Git source path");
            }
        }
    }

    private static void requireSha(String value, String label) {
        if (value == null || !FULL_SHA.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " must be a lowercase full Git SHA");
        }
    }

    private static String utf8(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("invalid UTF-8 Git path", exception);
        }
    }

    private static String ascii(byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private static byte[] git(Path root, String... arguments) throws IOException {
        List<String> command = new ArrayList<>(List.of("git", "--no-optional-locks", "-C", root.toString()));
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("GIT_OPTIONAL_LOCKS", "0");
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (var stream = process.getInputStream()) {
            byte[] chunk = new byte[8192];
            int n;
            while ((n = stream.read(chunk)) != -1) {
                if (output.size() > MAX_OUTPUT_BYTES - n) {
                    process.destroyForcibly();
                    throw new IOException("bounded Git output capacity exceeded");
                }
                output.write(chunk, 0, n);
            }
        } catch (IOException exception) {
            process.destroyForcibly();
            throw exception;
        }
        try {
            if (process.waitFor() != 0) {
                throw new IOException("Git inspection refused: " + String.join(" ", arguments));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Git inspection interrupted", exception);
        }
        return output.toByteArray();
    }

    private static void preflight(Path path, String output) throws IOException {
        if (Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    || !Arrays.equals(Files.readAllBytes(path), output.getBytes(StandardCharsets.UTF_8))) {
                throw new IOException("existing artifact differs, refusing overwrite: " + path);
            }
        }
    }

    private static void writeOnce(Path path, String output) throws IOException {
        if (Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        Path temporary = Files.createTempFile(path.getParent(), ".m3-donor-", ".tmp");
        boolean moved = false;
        try {
            Files.writeString(temporary, output, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static String table(String header, List<String> rows) {
        StringBuilder out = new StringBuilder(header).append('\n');
        for (String row : rows) {
            out.append(row).append('\n');
        }
        return out.toString();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("JDK SHA-256 unavailable", exception);
        }
    }

    /**
     * Bounded output identity for one immutable donor-delta inventory.
     *
     * @param donorId stable donor identity
     * @param baselineCommit exact baseline commit
     * @param donorCommit exact donor commit
     * @param candidateCount number of review-only deltas
     * @param candidatesSha256 SHA-256 of candidate-ledger bytes
     * @param candidatesPath write-once candidate ledger
     * @param receiptPath write-once receipt ledger
     */
    public record Result(String donorId, String baselineCommit, String donorCommit, int candidateCount,
            String candidatesSha256, Path candidatesPath, Path receiptPath) { }

}
