// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.CpuTools;
import com.m3.tools.text.search.AffixIndex;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import jdk.m3.io.M3File;

/** Independent named-module M3File lexicon receipt/replay probe. */
public final class M3AffixIndexTest {
    private static int assertions;
    private M3AffixIndexTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-affix-index-");
        Path first = directory.resolve("first.txt");
        Path second = directory.resolve("second.txt");
        try {
            Files.writeString(first, "apple application appeal bat cap map flap apple\r\n",
                    StandardCharsets.UTF_8);
            Files.copy(first, second);
            M3File source = M3File.load(first);
            CpuTools.FileAffixIndex lexicon = CpuTools.affixIndex(source);
            check(lexicon.tokenCount() == 8 && lexicon.distinctCount() == 7, "token multiplicity and unique IDs");
            check(lexicon.sha256().matches("[0-9a-f]{64}")
                    && lexicon.lexiconSha256().matches("[0-9a-f]{64}"), "SHA receipts");
            check(lexicon.contains("apple") && !lexicon.contains("App"), "exact UTF-16 case identity");
            check(lexicon.countPrefix("app") == 3 && lexicon.countSuffix("ap") == 3,
                    "range cardinality");
            AffixIndex.Result prefixes = lexicon.prefix("app", 10);
            check(prefixes.complete() && prefixes.totalMatches() == 3, "uncapped prefix result");
            check(lexicon.wordAt(prefixes.ordinalAt(0)).equals("appeal")
                    && lexicon.wordAt(prefixes.ordinalAt(1)).equals("apple")
                    && lexicon.wordAt(prefixes.ordinalAt(2)).equals("application"),
                    "canonical prefix order");
            AffixIndex.Result suffixes = lexicon.suffix("ap", 2);
            check(!suffixes.complete() && suffixes.totalMatches() == 3, "capped suffix result");
            check(lexicon.wordAt(suffixes.ordinalAt(0)).equals("cap")
                    && lexicon.wordAt(suffixes.ordinalAt(1)).equals("flap"),
                    "reverse UTF-16 suffix order");
            int[] detached = suffixes.ordinals();
            detached[0] = -1;
            check(suffixes.ordinalAt(0) >= 0, "primitive IDs detached");
            check(lexicon.prefix("", 0).totalMatches() == 7
                    && !lexicon.prefix("", 0).complete(), "zero-cap honest completeness");
            check(CpuTools.verifyAffixIndex(source, lexicon, null), "source replay");
            check(!CpuTools.verifyAffixIndex(M3File.load(second), lexicon, null),
                    "URI-bound receipt despite byte identity");
            Files.writeString(first, "modified source", StandardCharsets.UTF_8);
            check(CpuTools.verifyAffixIndex(source, lexicon, null), "original admitted snapshot unchanged");
            check(!CpuTools.verifyAffixIndex(M3File.load(first), lexicon, null), "modified bytes refused");
            Counter borrowed = new Counter();
            check(CpuTools.affixIndex(source, borrowed).sha256().equals(lexicon.sha256()), "stable receipt");
            check(borrowed.worked == 1 && borrowed.finished == 0, "borrowed monitor not completed");
            borrowed.cancelled = true;
            try {
                CpuTools.affixIndex(source, borrowed);
                throw new AssertionError("cancellation was ignored");
            } catch (CancellationException expected) { check(true, "pre-work cancellation"); }
            try {
                lexicon.prefix("app", 2, borrowed);
                throw new AssertionError("query cancellation was ignored");
            } catch (CancellationException expected) { check(true, "prefix query cancellation"); }
            System.out.println("M3_AFFIX_INDEX_PASS assertions=" + assertions);
        } finally {
            Files.deleteIfExists(second);
            Files.deleteIfExists(first);
            Files.delete(directory);
        }
    }

    private static void check(boolean actual, String why) {
        assertions++;
        if (!actual) throw new AssertionError(why);
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
