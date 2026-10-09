/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3LexiconPrecompute;
import com.m3.text.SharedLexiconPrecomputeCatalog;

public final class M3SiUnitPrecomputeCatalogTest {
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
        var identity = new SharedLexiconPrecomputeCatalog.SiUnitIdentity(
                "dictlang.si-units", "A");
        var ampere = new M3LexiconPrecompute.SiUnitPrecompute(-6, 42L, 0.0d, true);
        var catalog = SharedLexiconPrecomputeCatalog.builder()
                .siUnit(identity, ampere)
                .build();
        check(catalog.siUnitAt(identity).orElseThrow() == ampere);
        check(catalog.siUnitAt(identity).orElseThrow().dimensionPacked() == 42L);
        check(catalog.siUnitAt(new SharedLexiconPrecomputeCatalog.SiUnitIdentity(
                "dictlang.si-units", "missing")).isEmpty());
        expect(IllegalArgumentException.class, () ->
                new SharedLexiconPrecomputeCatalog.SiUnitIdentity("dictlang.acronyms", "A"));
        expect(IllegalArgumentException.class, () ->
                SharedLexiconPrecomputeCatalog.builder()
                        .siUnit(identity, ampere)
                        .siUnit(identity, ampere));
        expect(IllegalArgumentException.class, () ->
                new M3LexiconPrecompute.SiUnitPrecompute(101, 0L, 0.0d, false));
        expect(IllegalArgumentException.class, () ->
                new M3LexiconPrecompute.SiUnitPrecompute(0, 0L, Double.NaN, false));
        System.out.println("M3JDK_SI_UNIT_CATALOG_PASS checks=" + checks
                + " source=dictlang.si-units");
    }
}
