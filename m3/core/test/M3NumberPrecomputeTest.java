/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3LexiconPrecompute;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.m3.text.M3NumberSpace;
import com.m3.text.SharedLexiconPrecomputeCatalog;

public final class M3NumberPrecomputeTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable body) {
        try {
            body.run();
            throw new AssertionError("missing " + kind.getSimpleName());
        } catch (Throwable failure) {
            check(kind.isInstance(failure));
        }
    }

    public static void main(String[] args) throws Exception {
        List<String> manifest = Files.readAllLines(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        String[] numericManifest = manifest.stream()
                .filter(line -> line.startsWith("dictlang.numbers.0-10000\\t"))
                .findFirst().orElseThrow().split("\\t", -1);
        check(numericManifest.length == 9);
        check(numericManifest[5].equals("M3StringFacts + M3NumberSpace"));
        List<String> targetMap = Files.readAllLines(
                Path.of("lexicon/synexia-number-space-target-map.tsv"), StandardCharsets.UTF_8);
        check(targetMap.get(0).equals(
                "source_id\\tsource_path\\tsource_commit\\ttarget_type\\ttarget_api\\tstatus\\tpreservation_rule\\tevidence"));
        String[] numericTarget = targetMap.stream()
                .filter(line -> line.startsWith("dictlang.numbers.0-10000\\t"))
                .findFirst().orElseThrow().split("\\t", -1);
        check(numericTarget.length == 8);
        check(numericTarget[3].equals("com.m3.text.M3NumberSpace"));
        check(numericTarget[5].equals("ADMITTED_TYPED_RECEIVER"));
        M3NumberSpace space = M3NumberSpace.INSTANCE;
        M3LexiconPrecompute.NumberPrecompute zero = space.precompute(0, "und");
        M3LexiconPrecompute.NumberPrecompute max = space.precompute(10_000, "und");
        check(zero.value() == 0 && zero.spelling().equals("0"));
        check(max.value() == 10_000 && max.spelling().equals("10000"));
        check(zero.languageTag().equals("und"));
        var hindi = M3LexiconPrecompute.NumberPrecompute.canonical(42, "hi");
        check(hindi.value() == 42 && hindi.spelling().equals("42"));
        check(hindi.languageTag().equals("hi"));

        SharedLexiconPrecomputeCatalog.NumberIdentity zeroKey =
                new SharedLexiconPrecomputeCatalog.NumberIdentity(
                        M3NumberSpace.SOURCE_ID, "number", 0, "und");
        SharedLexiconPrecomputeCatalog catalog = SharedLexiconPrecomputeCatalog.builder()
                .number(zeroKey, zero)
                .build();
        check(catalog.numberAt(zeroKey).orElseThrow() == zero);
        expect(java.util.NoSuchElementException.class, () -> catalog.numberAt(
                new SharedLexiconPrecomputeCatalog.NumberIdentity(
                        M3NumberSpace.SOURCE_ID, "number", 1, "hi"))
                .orElseThrow());
        expect(IllegalArgumentException.class, () ->
                new M3LexiconPrecompute.NumberPrecompute(10_001, "10001", "und"));
        expect(IllegalArgumentException.class, () ->
                new M3LexiconPrecompute.NumberPrecompute(1, "01", "und"));
        expect(IllegalArgumentException.class, () ->
                new M3LexiconPrecompute.NumberPrecompute(1, "1", ""));
        expect(IllegalArgumentException.class, () ->
                SharedLexiconPrecomputeCatalog.builder()
                        .number(zeroKey, max));
        System.out.println("M3JDK_NUMBER_PRECOMPUTE_PASS checks=" + checks
                + " source=" + M3NumberSpace.SOURCE_ID + " values=10001");
    }
}
