// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * M3JDK21's executable Machine Oriented Programming recipe-memory custody.
 * This does not execute or replace an OpenRewrite recipe or any source parser.
 * A correction is append-only, source/guard sealed and requires an external
 * compile/test/fixed-point proof receipt before admission.
 *
 * <p>SELF@patch#sha256 is a same-commit reference: a Git commit cannot embed
 * its own 40-hex SHA without changing that SHA. CI resolves SELF to the
 * containing commit and checks the patch, guard and ledger were committed
 * together. It is not a Git commit ID on disk before commit creation.</p>
 */
public final class M3RecipeMemory {
    public static final String HEADER =
            "utc\trecipe\ttrigger\tobserved\tcorrection\tcorrected_by\tguard\tlesson\n";
    private static final long MAX_BYTES = 4L << 20;
    private static final Pattern RECIPE =
            Pattern.compile("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)+");
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    private M3RecipeMemory() { }

    public record Entry(Instant utc, String recipe, String trigger, String observed,
                        String correction, String correctedBy, String guard, String lesson) {
        public Entry {
            Objects.requireNonNull(utc, "utc");
            field(recipe, "recipe"); field(trigger, "trigger");
            field(observed, "observed"); field(correction, "correction");
            field(correctedBy, "correctedBy"); field(guard, "guard");
            field(lesson, "lesson");
            if (!RECIPE.matcher(recipe).matches()) {
                throw new IllegalArgumentException("invalid canonical recipe identity");
            }
            if (lesson.length() > 1024 || observed.length() > 2048) {
                throw new IllegalArgumentException("unbounded recipe lesson/observation");
            }
        }
        public String line() {
            return utc + "\t" + recipe + "\t" + trigger + "\t" + observed + "\t"
                    + correction + "\t" + correctedBy + "\t" + guard + "\t" + lesson + "\n";
        }
    }

    private static void field(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0 || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("invalid recipe memory field: " + name);
        }
    }

    /** Read a complete immutable recipe memory before attempting any recipe edit. */
    public static List<Entry> read(Path ledger) throws IOException {
        Objects.requireNonNull(ledger, "ledger");
        if (!Files.exists(ledger)) return List.of();
        if (!Files.isRegularFile(ledger) || Files.isSymbolicLink(ledger)
                || Files.size(ledger) > MAX_BYTES) {
            throw new IOException("unsafe or oversized recipe memory");
        }
        String content = Files.readString(ledger, StandardCharsets.UTF_8);
        if (!content.startsWith(HEADER) || !content.endsWith("\n")
                || content.contains("\n\n")) {
            throw new IOException("invalid recipe memory header or final newline");
        }
        List<Entry> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Instant last = Instant.EPOCH;
        String payload = content.substring(HEADER.length(), content.length() - 1);
        if (payload.isEmpty()) return List.of();
        for (String line : payload.split("\n", -1)) {
            if (line.isEmpty()) throw new IOException("blank line in recipe memory");
            String[] values = line.split("\t", -1);
            if (values.length != 8) throw new IOException("invalid recipe memory TSV schema");
            final Entry row;
            try {
                row = new Entry(Instant.parse(values[0]), values[1], values[2], values[3],
                        values[4], values[5], values[6], values[7]);
            } catch (RuntimeException invalid) {
                throw new IOException("invalid recipe memory row", invalid);
            }
            if (row.utc().isBefore(last) || !seen.add(row.recipe() + '\0' + row.correction())) {
                throw new IOException("nonappendable duplicate or reordered memory");
            }
            last = row.utc();
            result.add(row);
        }
        return List.copyOf(result);
    }

    /** Byte-prefix proof: existing corrections cannot be rewritten or deleted. */
    public static void verifyAppendOnly(byte[] before, byte[] after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if (after.length < before.length) throw new IllegalArgumentException("memory truncated");
        for (int i = 0; i < before.length; i++) {
            if (before[i] != after[i]) throw new IllegalArgumentException("old recipe memory changed");
        }
    }

    /** SHA-256 of first-party generated patch or regression fixture. */
    public static String sha256(Path source) throws IOException {
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        try (var input = Files.newInputStream(source)) {
            byte[] scratch = new byte[8192];
            for (int n; (n = input.read(scratch)) >= 0; ) digest.update(scratch, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Reusable, machine-readable correction marker, without a circular Git self-reference. */
    public static String correction(Path root, String patch) throws IOException {
        return "SELF@" + patch + "#" + sha256(owned(root, patch));
    }

    /**
     * Append an independently qualified improvement. Receipt binds the exact
     * canonical recipe, the patch SHA, guard SHA and three original gate results.
     * A synthetic receipt alone is NOT proof the external commands actually ran;
     * generate it only from Maven/OpenRewrite/JUnit process exit evidence.
     */
    public static synchronized void append(Path root, String ledgerRelative, Entry improvement,
            Path actualReceipt) throws IOException {
        Objects.requireNonNull(improvement, "improvement");
        Path ledger = destination(root, ledgerRelative);
        String correction = improvement.correction();
        if (!correction.startsWith("SELF@") || correction.indexOf('#') < 6) {
            throw new IOException("same-commit patch/hash reference required");
        }
        int separator = correction.lastIndexOf('#');
        String patch = correction.substring(5, separator);
        String expectedHash = correction.substring(separator + 1);
        if (!HASH.matcher(expectedHash).matches()
                || !expectedHash.equals(sha256(owned(root, patch)))) {
            throw new IOException("correction patch hash changed");
        }
        String guardHash = sha256(owned(root, improvement.guard()));
        String proof = Files.readString(Objects.requireNonNull(actualReceipt, "receipt"),
                StandardCharsets.UTF_8);
        String header = "M3_RECIPE_PROOF_V1\t" + improvement.recipe() + "\t"
                + expectedHash + "\t" + guardHash + "\n";
        if (!proof.equals(header + "compile\tPASS\ntest\tPASS\nfixed_point\tPASS\n")) {
            throw new IOException("recipe correction proof absent, stale or incomplete");
        }
        Files.createDirectories(ledger.getParent());
        Path lockPath = ledger.resolveSibling(ledger.getFileName() + ".lock");
        try (FileChannel lockChannel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = lockChannel.lock()) {
            if (!lock.isValid()) throw new IOException("invalid recipe memory lock");
            byte[] before = Files.exists(ledger) ? Files.readAllBytes(ledger) : HEADER.getBytes(StandardCharsets.UTF_8);
            List<Entry> previous = Files.exists(ledger) ? read(ledger) : List.of();
            if (!previous.isEmpty() && improvement.utc().isBefore(previous.getLast().utc())) {
                throw new IOException("correction timestamp predates prior memory");
            }
            if (previous.stream().anyMatch(row -> row.recipe().equals(improvement.recipe())
                    && row.correction().equals(improvement.correction()))) {
                throw new IOException("duplicate proven recipe correction");
            }
            byte[] appended = improvement.line().getBytes(StandardCharsets.UTF_8);
            if (before.length + (long) appended.length > MAX_BYTES) {
                throw new IOException("recipe memory byte budget exceeded");
            }
            Path temp = Files.createTempFile(ledger.getParent(), ".m3-memory-", ".tsv");
            boolean installed = false;
            try {
                try (FileChannel output = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                    ByteBuffer prefix = ByteBuffer.wrap(before);
                    ByteBuffer row = ByteBuffer.wrap(appended);
                    while (prefix.hasRemaining()) output.write(prefix);
                    while (row.hasRemaining()) output.write(row);
                    output.force(true);
                }
                Files.move(temp, ledger, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                installed = true;
            } finally {
                if (!installed) Files.deleteIfExists(temp);
            }
            verifyAppendOnly(before, Files.readAllBytes(ledger));
            read(ledger);
        }
    }

    private static Path destination(Path root, String relative) throws IOException {
        Objects.requireNonNull(root, "root");
        field(relative, "path");
        Path base = root.toRealPath();
        Path route = Path.of(relative);
        if (route.isAbsolute() || relative.indexOf('\\') >= 0) {
            throw new IOException("recipe path must be relative and provider-local");
        }
        for (Path element : route) {
            if (element.toString().equals("..") || element.toString().equals(".")) {
                throw new IOException("path traversal in recipe memory");
            }
        }
        Path target = base.resolve(route).normalize();
        if (!target.startsWith(base)) throw new IOException("recipe path escapes workspace");
        for (Path path = target; path != null && !path.equals(base); path = path.getParent()) {
            if (Files.isSymbolicLink(path)) throw new IOException("symbolic link recipe source refused");
        }
        return target;
    }

    private static Path owned(Path root, String relative) throws IOException {
        Path target = destination(root, relative);
        if (!Files.isRegularFile(target)) throw new IOException("missing guard or correction patch");
        return target;
    }

    /** Command-line entrypoint for coding agents and the M3JDK21 Maven recipe gate. */
    public static void main(String[] args) throws Exception {
        if (args.length == 3 && "show".equals(args[0])) {
            for (Entry row : read(destination(Path.of(args[1]), args[2]))) {
                System.out.print(row.line());
            }
        } else if (args.length == 12 && "append".equals(args[0])) {
            append(Path.of(args[1]), args[2],
                    new Entry(Instant.parse(args[3]), args[4], args[5], args[6],
                            args[7], args[8], args[9], args[10]), Path.of(args[11]));
            System.out.println("M3_MOP_RECIPE_MEMORY_APPENDED");
        } else if (args.length == 3 && "verify-append".equals(args[0])) {
            verifyAppendOnly(Files.readAllBytes(Path.of(args[1])), Files.readAllBytes(Path.of(args[2])));
            read(Path.of(args[2]));
            System.out.println("M3_MOP_APPEND_ONLY_PASS");
        } else {
            throw new IllegalArgumentException("usage: show <workspace> <relative-ledger> "
                    + "OR append <workspace> <relative-ledger> <utc> <recipe> <trigger> "
                    + "<observed> <SELF@patch#sha256> <corrected_by> <relative_guard> <lesson> <proof_receipt> "
                    + "OR verify-append <old-ledger> <new-ledger>");
        }
    }
}
