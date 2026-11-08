// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Locale;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemSourceImage;

/**
 * The 1 GB mapped gram proof of recipe/pmem-gram-index-20261106 (not a test; replaces the donor's
 * PmemOneGigabyteMappedProof, which ran over a test-only mapping stub of an older owner): a sparse
 * 1,000,000,000-byte source.bin of 20,000 logical 50,000-byte files with the 6-byte signature M3GRAM at offset 213 of
 * every 11th file, and its manifest.bin written in the source-image format (magic, version, sorted paths, extents,
 * per-file SHA-256, trailing manifest SHA-256), is opened by the REAL PmemSourceImage (which verifies every file
 * digest through the region of the chosen backend), indexed by PmemCorpusGramIndex and queried over all 20,000 files:
 * 1,819 complete first hits at file i*11, offset 213, at least 18,000 files pruned, and a 400-file selection equal to the
 * exact PmemCorpusSelectedSearch. Run under -Xmx128m. Prints PASS with bytes, hits, pruned files, retained index bytes,
 * the maximum heap and the elapsed time; exits non-zero on a failed contract.
 * usage: PmemGramOneGigabyteProof FILE|POSIX
 */
public final class PmemGramOneGigabyteProof {
    private static final int FILES = 20_000;
    private static final int BYTES_PER_FILE = 50_000;
    private static final int MARKER_OFFSET = 213;
    private static final byte[] MARKER = "M3GRAM".getBytes(StandardCharsets.US_ASCII);
    private static final long MAGIC = 0x4d33535243494d47L;

    private PmemGramOneGigabyteProof() { }

    public static void main(String[] args) throws Exception {
        PmemBackend backend = PmemBackend.valueOf(args.length > 0 ? args[0] : "FILE");
        long started = System.nanoTime();
        Path dir = Files.createTempDirectory("m3-gram-1gb-");
        long corpusBytes = (long) FILES * BYTES_PER_FILE;
        try {
            writeSparseImage(dir, corpusBytes);
            var limits = new PmemSourceImage.Limits(FILES, corpusBytes, 1 << 20, 1 << 16);
            try (PmemSourceImage image = PmemSourceImage.open(dir, backend, limits)) {
                PmemCorpusGramIndex gram = PmemCorpusGramIndex.build(image, 8_000_000, () -> false);
                int[] selected = new int[FILES];
                for (int i = 0; i < FILES; i++) selected[i] = i;
                var found = gram.firstHits(image, selected, MARKER, FILES, () -> false);
                int expectedHits = (FILES + 10) / 11;
                check(found.complete() && found.hitCount() == expectedHits, "complete 1,819 first hits");
                check(found.prunedFiles() >= 18_000 && found.prunedFiles() + found.candidateFiles() == FILES, "gram pruning");
                for (int i = 0; i < found.hitCount(); i++)
                    check(found.fileIndexAt(i) == i * 11 && found.fileOffsetAt(i) == MARKER_OFFSET, "first-hit position " + i);
                int[] first400 = new int[400];
                for (int i = 0; i < first400.length; i++) first400[i] = i;
                var filtered = gram.firstHits(image, first400, MARKER, 400, () -> false);
                var exact = PmemCorpusSelectedSearch.firstHits(image, first400, MARKER, 400, () -> false);
                check(filtered.hitCount() == exact.hitCount(), "selected 400 hit count");
                for (int i = 0; i < filtered.hitCount(); i++)
                    check(filtered.fileIndexAt(i) == exact.fileIndexAt(i) && filtered.fileOffsetAt(i) == exact.fileOffsetAt(i),
                            "selected 400 hit " + i);
                long elapsed = (System.nanoTime() - started) / 1_000_000;
                System.out.println("PASS M3_PMEM_GRAM_1GB backend=" + backend + " bytes=" + corpusBytes + " files=" + FILES
                        + " hits=" + found.hitCount() + " pruned=" + found.prunedFiles() + " candidates=" + found.candidateFiles()
                        + " indexBytes=" + gram.retainedPrimitiveBytes() + " denseFiles=" + gram.denseFileCount()
                        + " maxHeap=" + Runtime.getRuntime().maxMemory() + " elapsedMillis=" + elapsed);
            }
        } finally {
            Files.deleteIfExists(dir.resolve("source.bin"));
            Files.deleteIfExists(dir.resolve("manifest.bin"));
            Files.deleteIfExists(dir);
        }
    }

    private static void writeSparseImage(Path dir, long corpusBytes) throws Exception {
        try (FileChannel channel = FileChannel.open(dir.resolve("source.bin"), StandardOpenOption.READ,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.SPARSE)) {
            channel.write(ByteBuffer.wrap(new byte[1]), corpusBytes - 1);
            for (int i = 0; i < FILES; i += 11)
                channel.write(ByteBuffer.wrap(MARKER), (long) i * BYTES_PER_FILE + MARKER_OFFSET);
            channel.force(true);
        }
        byte[] zeros = new byte[BYTES_PER_FILE];
        byte[] zeroDigest = MessageDigest.getInstance("SHA-256").digest(zeros);
        System.arraycopy(MARKER, 0, zeros, MARKER_OFFSET, MARKER.length);
        byte[] markerDigest = MessageDigest.getInstance("SHA-256").digest(zeros);
        try (OutputStream file = Files.newOutputStream(dir.resolve("manifest.bin"), StandardOpenOption.CREATE_NEW);
             BufferedOutputStream buffered = new BufferedOutputStream(file, 1 << 16);
             DigestOutputStream sealed = new DigestOutputStream(buffered, MessageDigest.getInstance("SHA-256"));
             DataOutputStream output = new DataOutputStream(sealed)) {
            output.writeLong(MAGIC);
            output.writeInt(1);
            output.writeInt(FILES);
            output.writeLong(corpusBytes);
            for (int i = 0; i < FILES; i++) {
                byte[] path = String.format(Locale.ROOT, "src/F%05d.java", i).getBytes(StandardCharsets.UTF_8);
                output.writeInt(path.length);
                output.write(path);
                output.writeLong((long) i * BYTES_PER_FILE);
                output.writeInt(BYTES_PER_FILE);
                output.write(i % 11 == 0 ? markerDigest : zeroDigest);
            }
            byte[] checksum = sealed.getMessageDigest().digest();
            sealed.on(false);
            output.write(checksum);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("1 GB gram proof failed: " + message);
    }
}
