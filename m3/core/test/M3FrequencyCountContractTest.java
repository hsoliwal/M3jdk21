/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.SynexiaPrecomputePayload;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Proves that the frequency source carries corpus counts, never an implicit rank. */
public final class M3FrequencyCountContractTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        String manifest = Files.readString(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        String row = Arrays.stream(manifest.split("\n", -1))
                .filter(line -> line.startsWith("dictlang.frequency\t"))
                .findFirst().orElseThrow();
        String[] columns = row.split("\t", -1);
        check(columns.length == 9);
        check(columns[4].equals("lexeme,corpus_count"));
        check(columns[7].contains("not a rank"));
        check(columns[8].contains("corpus_count"));
        check(columns[7].contains("OPUS/OpenSubtitles"));

        String targetMap = Files.readString(
                Path.of("lexicon/synexia-frequency-count-target-map.tsv"),
                StandardCharsets.UTF_8);
        String mapRow = Arrays.stream(targetMap.split("\n", -1))
                .filter(line -> line.startsWith("M3JDK_FREQUENCY_COUNT_TARGET_MAP_V1\t"))
                .findFirst().orElseThrow();
        String[] mapColumns = mapRow.split("\t", -1);
        check(mapColumns.length == 12);
        check(mapColumns[2].endsWith("en_full_frequency.txt"));
        check(mapColumns[3].equals(
                "7fea67ab954e2c01df6c608c9826e594cf36f8823b3243554f88245fb75dc506"));
        check(mapColumns[5].contains("not a rank"));
        check(mapColumns[6].equals("corpus_count"));
        check(mapColumns[11].equals("TARGET_CONTRACT_STAGED_SOURCE_RECONCILIATION_REQUIRED"));

        SynexiaPrecomputePayload payload =
                SynexiaPrecomputePayload.parse("{\"corpus_count\":28787591}");
        check(payload.requireNonNegativeLong("corpus_count") == 28_787_591L);
        check(payload.canonicalJson().equals("{\"corpus_count\":28787591}"));

        expect(IllegalArgumentException.class, () ->
                SynexiaPrecomputePayload.parse("{\"corpus_count\":-1}").requireNonNegativeLong("corpus_count"));
        check(!payload.field("frequency_rank").isPresent());
        System.out.println("M3JDK_FREQUENCY_COUNT_CONTRACT_PASS checks=" + checks);
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        try {
            action.run();
            throw new AssertionError("missing " + kind.getSimpleName());
        } catch (Throwable failure) {
            check(kind.isInstance(failure));
        }
    }
}
