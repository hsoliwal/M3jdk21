// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.CpuTools;
import com.m3.tools.text.ngram.NGrams;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import jdk.m3.io.M3File;

/** Independent installed-module receipt/replay test; no alternate source reader or parser. */
public final class M3GramIndexTest {
    private static int assertions;
    private M3GramIndexTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-gram-index-");
        Path original = directory.resolve("original.txt");
        Path copy = directory.resolve("copy.txt");
        try {
            Files.writeString(original, "ababa\ud83d\ude42\r\n", StandardCharsets.UTF_8);
            Files.copy(original, copy);
            M3File source = M3File.load(original);
            CpuTools.GramIndex grams = CpuTools.gramIndex(source, 2);
            check(grams.sha256().matches("[0-9a-f]{64}"), "receipt SHA-256");
            check(grams.windowCount() == source.length() - 1, "exact physical windows");
            check(Arrays.equals(grams.positions("ab"), new int[]{0, 2}), "overlapping ab offsets");
            check(Arrays.equals(grams.positions("ba"), new int[]{1, 3}), "overlapping ba offsets");
            check(Arrays.equals(grams.positions("\ud83d\ude42"), new int[]{5}), "surrogate-pair UTF-16 offset");
            check(grams.counts().equals(NGrams.count(source.text(), 2)), "legacy count oracle");
            check(CpuTools.verifyGramIndex(source, 2, grams, null), "unchanged source re-execution");
            check(!CpuTools.verifyGramIndex(source, 3, grams, null), "different gram size");
            check(!CpuTools.verifyGramIndex(M3File.load(copy), 2, grams, null), "identical bytes, different URI");
            int[] mutable = grams.positions("ab");
            mutable[0] = 999;
            check(grams.positions("ab")[0] == 0, "defensive primitive offsets");
            Files.writeString(original, "changed source");
            check(CpuTools.verifyGramIndex(source, 2, grams, null), "admitted M3File snapshot");
            check(!CpuTools.verifyGramIndex(M3File.load(original), 2, grams, null), "modified file content");
            Counter borrowed = new Counter();
            check(CpuTools.gramIndex(source, 2, borrowed).sha256().equals(grams.sha256()),
                    "repeat canonical receipt");
            check(borrowed.worked == 1 && borrowed.finished == 0, "borrowed progress ownership");
            borrowed.cancelled = true;
            try {
                CpuTools.gramIndex(source, 2, borrowed);
                throw new AssertionError("cancel was ignored");
            } catch (CancellationException expected) { check(true, "cancelled before work"); }
            System.out.println("M3_GRAM_INDEX_PASS assertions=" + assertions);
        } finally {
            Files.deleteIfExists(copy);
            Files.deleteIfExists(original);
            Files.delete(directory);
        }
    }

    private static void check(boolean actual, String description) {
        assertions++;
        if (!actual) throw new AssertionError(description);
    }

    private static final class Counter implements ProgressMonitor {
        int worked;
        int finished;
        boolean cancelled;
        @Override public void worked(long units) { worked += Math.toIntExact(units); }
        @Override public void done() { finished++; }
        @Override public boolean isCancelled() { return cancelled; }
    }
}
