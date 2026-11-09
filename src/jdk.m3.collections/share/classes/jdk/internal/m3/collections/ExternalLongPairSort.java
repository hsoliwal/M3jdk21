/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import jdk.m3.collections.LongLongConsumer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Stable fixed-width external sort. Input is big-endian (long key, long payload) pairs.
 * Completed output is a verified run plus a manifest; consume through visit(). All scratch
 * is retained, never deleted or overwritten. No concurrent writer or hostile-path-race isolation.
 * Resume reuses fully verified runs; an unsealed/partial file is a hard failure, not an implicit retry.
 * Checksums establish integrity, not authentication. No fsync/directory crash-durability guarantee.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/ExternalLongPairSort.java}
 * blob {@code 0637b89f28a34ad1b91a84ee011c78c76dfaf1cc} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ExternalLongPairSort {
    private ExternalLongPairSort() { }
    public record Result(Path run, long records, String sha256, long runBytes, int mergeRounds) { }

    public static Result sort(Path input, Path workspace, ExternalSortOptions options,
            BooleanSupplier cancelled) throws IOException {
        return execute(input, workspace, options, cancelled, false);
    }
    public static Result resume(Path input, Path workspace, ExternalSortOptions options,
            BooleanSupplier cancelled) throws IOException {
        return execute(input, workspace, options, cancelled, true);
    }
    private static Result execute(Path input, Path workspace, ExternalSortOptions options,
            BooleanSupplier cancelled, boolean resume) throws IOException {
        Objects.requireNonNull(options, "options"); Objects.requireNonNull(cancelled, "cancelled");
        input = checkedPath(input); workspace = checkedPath(workspace);
        LongPairRun.regular(input);
        if (workspace.getParent() == null || workspace.getParent().getParent() == null
                || !Files.isDirectory(workspace.getParent(), LinkOption.NOFOLLOW_LINKS))
            throw new IOException("workspace requires a non-root existing parent");
        long length = Files.size(input);
        if ((length & 15) != 0) throw new IOException("truncated input pair");
        long count = length / 16;
        if (count > options.maximumInputRecords()) throw new IOException("input record budget");
        long runs = Math.max(1, count == 0 ? 0 : 1 + (count - 1) / options.runRecords());
        long planned = plannedBytes(count, runs, options.fanIn());
        if (planned > options.maximumRunBytes()) throw new IOException("retained run-byte budget");
        String sourceHash = hash(input, cancelled);
        String job = "SXLPAIR_JOB_1\n" + sourceHash + "\n" + count + "\n" + options.identity() + "\n";
        Path jobFile = workspace.resolve("JOB");
        if (resume) {
            if (!Files.isDirectory(workspace, LinkOption.NOFOLLOW_LINKS)
                    || !readBounded(jobFile, 512).equals(job)) throw new IOException("resume binding mismatch");
        } else {
            check(cancelled); Files.createDirectory(workspace);
            Files.writeString(jobFile, job, StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW);
        }
        try (DataInputStream source = new DataInputStream(new BufferedInputStream(
                Files.newInputStream(input, LinkOption.NOFOLLOW_LINKS), options.bufferBytes()))) {
            long offset = 0;
            for (long run = 0; run < runs; run++) {
                check(cancelled);
                int n = (int) Math.min(options.runRecords(), count - offset);
                Path file = run(workspace, 0, run);
                if (reusable(file, n, count, options, cancelled)) {
                    source.skipNBytes(16L * n);
                } else {
                    long[] keys = new long[n], values = new long[n];
                    for (int i = 0; i < n; i++) {
                        check(cancelled); keys[i] = source.readLong(); values[i] = source.readLong();
                    }
                    int[] order = LongKeyOrder.ascending(keys, options.unsigned());
                    try (LongPairRun.Writer out = new LongPairRun.Writer(file, n,
                            options.bufferBytes(), options.unsigned())) {
                        for (int index : order) {
                            check(cancelled); out.write(keys[index], values[index], offset + index);
                        }
                        out.finish();
                    }
                }
                offset += n;
            }
            if (source.read() != -1) throw new IOException("input grew during sort");
        }
        long currentRuns = runs;
        int round = 0;
        while (currentRuns > 1) {
            long nextRuns = 1 + (currentRuns - 1) / options.fanIn();
            for (long group = 0; group < nextRuns; group++) {
                check(cancelled);
                long first = group * options.fanIn();
                int n = (int) Math.min(options.fanIn(), currentRuns - first);
                Path output = run(workspace, round + 1, group);
                // Open only F heads, including during exceptional cleanup.
                try (Readers inputs = new Readers(n)) {
                    long expected = 0;
                    for (int i = 0; i < n; i++) {
                        inputs.readers[i] = new LongPairRun.Reader(run(workspace, round, first+i),
                                options.bufferBytes(), options.unsigned(), count);
                        expected = Math.addExact(expected, inputs.readers[i].count);
                    }
                    if (reusable(output, expected, count, options, cancelled)) {
                        for (LongPairRun.Reader reader : inputs.readers)
                            while (reader.next()) check(cancelled);
                        continue;
                    }
                    RunHeadSelector selector = new RunHeadSelector(n, options.unsigned(), options.selector());
                    for (int i = 0; i < n; i++) {
                        LongPairRun.Reader in = inputs.readers[i]; boolean live = in.next();
                        selector.initial(i, in.key, in.ordinal, live);
                    }
                    selector.build();
                    try (LongPairRun.Writer out = new LongPairRun.Writer(output, expected,
                            options.bufferBytes(), options.unsigned())) {
                        for (int id; (id = selector.winner()) >= 0;) {
                            check(cancelled); LongPairRun.Reader in = inputs.readers[id];
                            out.write(in.key, in.payload, in.ordinal);
                            boolean live = in.next(); selector.advance(id, in.key, in.ordinal, live);
                        }
                        out.finish();
                    }
                }
            }
            currentRuns = nextRuns; round++;
        }
        if (!hash(input, cancelled).equals(sourceHash) || Files.size(input) != length)
            throw new IOException("input changed during sort");
        Path finalRun = run(workspace, round, 0);
        String finalHash;
        try (LongPairRun.Reader reader = new LongPairRun.Reader(finalRun, options.bufferBytes(), options.unsigned(), count)) {
            if (reader.count != count) throw new IOException("final count mismatch");
            while (reader.next()) check(cancelled);
            finalHash = reader.hash();
        }
        String complete = "SXLPAIR_COMPLETE_1\n" + sourceHash + "\n" + count + "\n"
                + options.identity() + "\n" + finalRun.getFileName() + "\n" + finalHash + "\n"
                + planned + "\n" + round + "\n";
        Path manifest = workspace.resolve("COMPLETED");
        check(cancelled);
        if (Files.exists(manifest, LinkOption.NOFOLLOW_LINKS)) {
            if (!readBounded(manifest, 1024).equals(complete)) throw new IOException("completion mismatch");
        } else Files.writeString(manifest, complete, StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW);
        return new Result(finalRun, count, finalHash, planned, round);
    }
    /**
     * Streams a completed result without materializing records. Callback effects are not rolled
     * back on failure. Validates manifest, header, ordering and digest, including a final EOF check.
     */
    public static void visit(Result result, boolean unsigned, LongLongConsumer action) throws IOException {
        Objects.requireNonNull(result, "result"); Objects.requireNonNull(action, "action");
        Path file = checkedPath(result.run());
        String[] completion = readBounded(file.getParent().resolve("COMPLETED"), 1024).split("\n", -1);
        if (completion.length != 9 || !completion[0].equals("SXLPAIR_COMPLETE_1")
                || !completion[2].equals(Long.toString(result.records()))
                || !completion[4].equals(file.getFileName().toString())
                || !completion[5].equals(result.sha256())
                || !completion[6].equals(Long.toString(result.runBytes()))
                || !completion[7].equals(Integer.toString(result.mergeRounds())) || !completion[8].isEmpty())
            throw new IOException("uncommitted result");
        String[] config = completion[3].split(",", -1);
        if (config.length != 8 || !config[6].equals(Boolean.toString(unsigned)))
            throw new IOException("ordering mismatch");
        try (LongPairRun.Reader reader = new LongPairRun.Reader(file, 8192, unsigned, result.records())) {
            if (reader.count != result.records() || !reader.hash().equals(result.sha256()))
                throw new IOException("result binding mismatch");
            while (reader.next()) action.accept(reader.key, reader.payload);
        }
    }
    private static boolean reusable(Path file, long expected, long total, ExternalSortOptions options,
            BooleanSupplier cancelled) throws IOException {
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                && !Files.exists(LongPairRun.seal(file), LinkOption.NOFOLLOW_LINKS)) return false;
        try (LongPairRun.Reader reader = new LongPairRun.Reader(file, options.bufferBytes(), options.unsigned(), total)) {
            if (reader.count != expected) throw new IOException("reused run count");
            while (reader.next()) check(cancelled);
        }
        return true;
    }
    private static long plannedBytes(long count, long runs, int fanIn) throws IOException {
        try {
            long sum = 0;
            for (;;) {
                sum = Math.addExact(sum, Math.addExact(Math.multiplyExact(count, 24), Math.multiplyExact(runs, 16)));
                if (runs <= 1) return sum;
                runs = 1 + (runs - 1) / fanIn;
            }
        } catch (ArithmeticException overflow) { throw new IOException("run byte-count overflow", overflow); }
    }
    private static Path run(Path root, int round, long index) { return root.resolve("p" + round + "-r" + index + ".bin"); }
    private static void check(BooleanSupplier cancelled) throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) throw new InterruptedIOException("sort cancelled");
    }
    private static String hash(Path file, BooleanSupplier cancelled) throws IOException {
        MessageDigest digest = LongPairRun.digest(); byte[] buffer = new byte[65536];
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            for (int n; (n = input.read(buffer)) >= 0;) { check(cancelled); digest.update(buffer, 0, n); }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private static String readBounded(Path file, int max) throws IOException {
        LongPairRun.regular(file);
        if (Files.size(file) > max) throw new IOException("metadata length");
        return Files.readString(file, StandardCharsets.US_ASCII);
    }
    private static Path checkedPath(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (!path.isAbsolute() || !path.normalize().equals(path)) throw new IOException("absolute normalized path required");
        for (Path at = path; at != null; at = at.getParent())
            if (Files.isSymbolicLink(at)) throw new IOException("symlink path refused");
        return path;
    }
    private static final class Readers implements AutoCloseable {
        final LongPairRun.Reader[] readers;
        Readers(int size) { readers = new LongPairRun.Reader[size]; }
        @Override public void close() throws IOException {
            IOException failure = null;
            for (LongPairRun.Reader reader : readers) if (reader != null) {
                try { reader.close(); } catch (IOException close) {
                    if (failure == null) failure = close; else failure.addSuppressed(close);
                }
            }
            if (failure != null) throw failure;
        }
    }
}
