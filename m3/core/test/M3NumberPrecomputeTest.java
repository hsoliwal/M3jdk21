/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3LexiconPrecompute;
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

    public static void main(String[] args) {
        M3NumberSpace space = M3NumberSpace.INSTANCE;
        M3LexiconPrecompute.NumberPrecompute zero = space.precompute(0, "und");
        M3LexiconPrecompute.NumberPrecompute max = space.precompute(10_000, "und");
        check(zero.value() == 0 && zero.spelling().equals("0"));
        check(max.value() == 10_000 && max.spelling().equals("10000"));
        check(zero.languageTag().equals("und"));

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
                SharedLexiconPrecomputeCatalog.builder()
                        .number(zeroKey, max));
        System.out.println("M3JDK_NUMBER_PRECOMPUTE_PASS checks=" + checks
                + " source=" + M3NumberSpace.SOURCE_ID + " values=10001");
    }
}
