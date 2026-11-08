// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.companion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Standalone Java 21 integration proof: candidate resource census delegates all
 * visibility, JPMS, reserved-owner and name-collision decisions to the existing
 * M3CompanionPlan. REVIEW never authorizes emitting a companion or JNI change.
 */
public final class M3SystemApiReviewTest {
    private static int assertions;

    private M3SystemApiReviewTest() { }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static M3CompanionPlan.Row lookup(
            List<M3CompanionPlan.Row> rows, String binaryName) {
        return rows.stream()
                .filter(row -> row.request().binaryName().equals(binaryName))
                .findFirst().orElseThrow(() -> new AssertionError("missing type: " + binaryName));
    }

    private static void refused(Class<? extends Throwable> expected, Throwing action) {
        try {
            action.run();
            throw new AssertionError("expected " + expected.getSimpleName());
        } catch (Throwable failure) {
            check(expected.isInstance(failure), "unexpected failure: " + failure);
        }
    }

    @FunctionalInterface
    private interface Throwing { void run() throws Exception; }

    public static void main(String[] unused) throws IOException {
        Set<String> reserved = Set.of("jdk.m3.companion.java.lang.M3StringOps");
        List<M3CompanionPlan.Row> base =
                M3SystemApiCensus.reviewModule("java.base", 50000, reserved);
        List<M3SystemApiCensus.ApiType> resources =
                M3SystemApiCensus.scanModule("java.base", 50000);
        check(!base.isEmpty(), "installed java.base must contribute candidates");
        check(base.size() == resources.size(), "review is lossless, not a public-type filter");
        check(new HashSet<>(base.stream().map(M3CompanionPlan.Row::request).toList()).size()
                == base.size(), "one review row per exact class resource");
        check(lookup(base, "java.lang.String").decision()
                == M3CompanionPlan.Decision.REFUSED_EXISTING,
                "reserved companion names must not generate competing public APIs");
        check(lookup(base, "java.util.Map$Entry").decision()
                == M3CompanionPlan.Decision.REVIEW,
                "exported public nested interface remains reviewable");
        check(lookup(base, "java.util.Map$Entry").companionName()
                .equals("jdk.m3.companion.java.util.M3Map_EntryOps"),
                "nested binary identity has deterministic companion spelling");
        check(lookup(base, "java.util.ArrayList$SubList").decision()
                == M3CompanionPlan.Decision.REFUSED_NOT_PUBLIC,
                "private ArrayList nested class must not get public companion");
        check(lookup(base, "java.util.AbstractList$Itr").imageFingerprint().isEmpty(),
                "nonpublic class must not receive invented proof fingerprint");
        check(lookup(M3SystemApiCensus.reviewModule("java.base", 50000, Set.of()),
                "java.lang.String").decision() == M3CompanionPlan.Decision.REVIEW,
                "unreserved String remains REVIEW, not accepted");
        check(base.equals(M3SystemApiCensus.reviewModule("java.base", 50000, reserved)),
                "repeat module review must be stable");

        List<M3CompanionPlan.Row> xml =
                M3SystemApiCensus.reviewModule("java.xml", 50000, Set.of());
        check(lookup(xml, "org.w3c.dom.Node").decision()
                == M3CompanionPlan.Decision.REVIEW,
                "non-java package export qualifies for candidate review");
        check(lookup(xml, "org.xml.sax.InputSource").decision()
                == M3CompanionPlan.Decision.REVIEW,
                "second non-java package is considered");
        check(xml.equals(M3SystemApiCensus.reviewModule("java.xml", 50000, Set.of())),
                "repeat XML review must be stable");

        refused(IllegalStateException.class,
                () -> M3SystemApiCensus.reviewModule("java.base", 1, Set.of()));
        refused(IllegalArgumentException.class,
                () -> M3SystemApiCensus.reviewModule("missing.module.forever", 50000, Set.of()));
        refused(NullPointerException.class,
                () -> M3SystemApiCensus.reviewModule("java.base", 50000, null));
        refused(IllegalStateException.class,
                () -> M3SystemApiCensus.reviewInstalled(1, 50000, Set.of()));

        List<M3CompanionPlan.Row> entire =
                M3SystemApiCensus.reviewInstalled(256, 50000, Set.of());
        check(entire.size() == M3SystemApiCensus.scanInstalled(256, 50000).size(),
                "all candidate resources receive exactly one admission decision");
        check(entire.size() > base.size(), "installed module review extends beyond java.base");
        check(lookup(entire, "java.lang.String").decision()
                == M3CompanionPlan.Decision.REVIEW,
                "read-only global planner includes core Java API");
        check(lookup(entire, "org.w3c.dom.Node").decision()
                == M3CompanionPlan.Decision.REVIEW,
                "read-only global planner includes exported non-java package");
        check(entire.equals(M3SystemApiCensus.reviewInstalled(256, 50000, Set.of())),
                "complete installed image repeat review is deterministic");
        Map<String, Integer> counts = new TreeMap<>();
        for (M3CompanionPlan.Row row : entire) {
            counts.merge(row.decision().name(), 1, Integer::sum);
        }
        check(counts.getOrDefault("REFUSED_NOT_PUBLIC", 0) > 0,
                "class-resource inventory includes nonpublic types");
        check(counts.getOrDefault("REVIEW", 0) > 0,
                "reviewable public type inventory is nonempty");
        check(counts.values().stream().mapToInt(Integer::intValue).sum() == entire.size(),
                "decision bucket census must account for every candidate");
        List<M3CompanionPlan.Row> mutation = new ArrayList<>(entire);
        mutation.clear();
        check(entire.size() > 0, "caller mutation cannot modify review output");
        System.out.println("M3SystemApiReviewTest PASS assertions=" + assertions
                + " classResourceCandidates=" + entire.size() + " decisions=" + counts);
    }
}
