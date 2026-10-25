// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.CpuTools;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import jdk.m3.io.M3File;

/** Actual named-module M3File paired-source receipt and minimal patch oracle. */
public final class M3BoundedDiffTest {
    private static int assertions;
    private M3BoundedDiffTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-bounded-diff-");
        Path original = directory.resolve("original.txt");
        Path target = directory.resolve("target.txt");
        Path moved = directory.resolve("same-bytes.txt");
        Path independentPath = directory.resolve("independent.txt");
        Path conflictingPath = directory.resolve("conflicting.txt");
        try {
            Files.writeString(original, "left\r\nright\ud83d\ude42\n", StandardCharsets.UTF_8);
            Files.writeString(target, "left\r\nright!\ud83d\ude42\n", StandardCharsets.UTF_8);
            Files.copy(original, moved);
            M3File before = M3File.load(original);
            M3File after = M3File.load(target);
            Files.writeString(independentPath, "#left\r\nright\ud83d\ude42\n", StandardCharsets.UTF_8);
            Files.writeString(conflictingPath, "left\r\nright?\ud83d\ude42\n", StandardCharsets.UTF_8);
            M3File independent = M3File.load(independentPath);
            M3File conflicting = M3File.load(conflictingPath);
            CpuTools.FileDiff result = CpuTools.diff(before, after, 1);
            check(result.complete() && result.distance() == 1, "shortest insert/delete script");
            check(result.sha256().matches("[0-9a-f]{64}"), "two-source receipt");
            check(result.patch().sourceSha256().matches("[0-9a-f]{64}")
                    && result.patch().targetSha256().matches("[0-9a-f]{64}"), "exact UTF-16 hashes");
            check(result.patch().changes().size() == 1, "one insertion projection");
            check(result.patch().changes().getFirst().inserted().equals("!"), "inserted character");
            check(result.apply(before).equals(after.text()), "replay reconstructs target");
            check(CpuTools.verifyDiff(before, after, 1, result, null), "exact receipt replay");
            var merged = CpuTools.merge(before, after, independent, 1);
            check(merged.mergeable() && merged.conflictCount() == 0,
                    "bounded three-source M3File merge composes independent edits");
            check(merged.apply(before).equals("#left\r\nright!\ud83d\ude42\n"),
                    "exact merged file materialization preserves original UTF-16 and line endings");
            check(CpuTools.verifyMerge(before, after, independent, 1, merged, null),
                    "three ordered file snapshots source-sealed in replay receipt");
            check(!CpuTools.verifyMerge(before, independent, after, 1, merged, null),
                    "left/right source order is part of the receipt");
            check(!CpuTools.verifyMerge(M3File.load(moved), after, independent, 1, merged, null),
                    "base URI is part of the merge receipt");
            try {
                merged.apply(M3File.load(moved));
                throw new AssertionError("location-substituted base accepted");
            } catch (IllegalArgumentException expected) { check(true, "file merge rejects substituted base"); }
            var conflict = CpuTools.merge(before, after, conflicting, 1);
            check(!conflict.mergeable() && conflict.status() == com.m3.tools.text.diff.ThreeWayMerge.Status.CONFLICT
                    && conflict.conflictCount() > 0, "overlapping inserts remain explicit conflicts");
            try {
                conflict.apply(before);
                throw new AssertionError("conflicting patch applied");
            } catch (IllegalStateException expected) { check(true, "conflict cannot produce a partial file"); }
            var unqualified = CpuTools.merge(before, after, independent, 0);
            check(!unqualified.mergeable() && !unqualified.result().complete(),
                    "insufficient budget is not silently applied as a merge");
            check(!CpuTools.verifyDiff(before, after, 2, result, null), "budget bound to receipt");
            check(!CpuTools.verifyDiff(M3File.load(moved), after, 1, result, null), "source URI bound");
            check(!CpuTools.verifyDiff(after, before, 1, result, null), "ordered sides bound");
            CpuTools.FileDiff incomplete = CpuTools.diff(before, after, 0);
            check(!incomplete.complete() && incomplete.patch().changes().isEmpty(),
                    "distance-budget miss is not a patch");
            try {
                incomplete.apply(before);
                throw new AssertionError("incomplete patch applied");
            } catch (IllegalStateException expected) { check(true, "incomplete apply refused"); }
            Files.writeString(original, "changed on disk", StandardCharsets.UTF_8);
            check(CpuTools.verifyDiff(before, after, 1, result, null), "admitted snapshot remains immutable");
            check(CpuTools.verifyMerge(before, after, independent, 1, merged, null),
                    "three-way receipt retains admitted snapshot after underlying disk change");
            check(!CpuTools.verifyMerge(M3File.load(original), after, independent, 1, merged, null),
                    "new disk version cannot inherit original three-way receipt");
            check(!CpuTools.verifyDiff(M3File.load(original), after, 1, result, null),
                    "new file bytes do not inherit old receipt");
            Counter monitor = new Counter();
            check(CpuTools.diff(before, after, 1, monitor).sha256().equals(result.sha256()),
                    "repeated deterministic result");
            check(monitor.worked == 1 && monitor.finished == 0, "borrowed monitor not completed");
            monitor.cancelled = true;
            try {
                CpuTools.diff(before, after, 1, monitor);
                throw new AssertionError("cancelled work executed");
            } catch (CancellationException expected) { check(true, "cancellation observed"); }
            try {
                CpuTools.merge(before, after, independent, 1, monitor);
                throw new AssertionError("cancelled merge executed");
            } catch (CancellationException expected) { check(true, "merge cancellation observed"); }
            System.out.println("M3_BOUNDED_DIFF_PASS assertions=" + assertions);
        } finally {
            Files.deleteIfExists(conflictingPath);
            Files.deleteIfExists(independentPath);
            Files.deleteIfExists(moved);
            Files.deleteIfExists(target);
            Files.deleteIfExists(original);
            Files.delete(directory);
        }
    }

    private static void check(boolean condition, String why) {
        assertions++;
        if (!condition) throw new AssertionError(why);
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
