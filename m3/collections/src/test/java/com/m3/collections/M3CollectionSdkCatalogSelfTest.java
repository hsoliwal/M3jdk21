/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.List;

/** Standalone provider-family registry/contract oracle; needs only Java 21. */
public final class M3CollectionSdkCatalogSelfTest {
    private static int checks;
    private M3CollectionSdkCatalogSelfTest() { }
    private static void check(boolean value, String issue) {
        checks++;
        if (!value) throw new AssertionError(issue);
    }
    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError("expected " + kind + ", got " + problem, problem);
        }
        throw new AssertionError("expected " + kind);
    }
    public static int verify() throws Exception {
        var entries = M3CollectionSdkCatalog.families();
        check(entries.size() >= 95, "provider coverage inventory");
        check(M3CollectionSdkCatalog.size() == entries.size(), "family count");
        int[] vendors = new int[M3CollectionSdkCatalog.Vendor.values().length];
        int optional = 0;
        String previous = "";
        for (var family : entries) {
            checks++;
            check(family.upstreamName().compareTo(previous) > 0, "sorted unique external name");
            previous = family.upstreamName();
            check(M3CollectionSdkCatalog.require(family.upstreamName()).equals(family), "lookup equality");
            if (family.hasExistingM3Owner()) {
                check(family.existingM3Owner().startsWith("com.m3.collections."), "owner identity");
                check(family.disposition() != M3CollectionSdkCatalog.Disposition.PENDING,
                        "pending cannot claim installed owner");
            }
            if (family.disposition() == M3CollectionSdkCatalog.Disposition.PENDING) {
                check(!family.hasExistingM3Owner(), "pending is not already installed");
                check(!family.candidateRole().isEmpty(), "future role explicit");
            }
            if (family.disposition() == M3CollectionSdkCatalog.Disposition.OPTIONAL_ADAPTER) {
                optional++;
                check(family.hasExistingM3Owner(), "optional adapter must name a real owner");
                check(family.existingM3Owner().startsWith("com.m3.collections.interop."),
                        "optional compatibility never pollutes core dependency boundary");
            }
            vendors[family.vendor().ordinal()]++;
        }
        check(optional == 14, "selected optional adapters must be enumerated exactly");
        for (var vendor : M3CollectionSdkCatalog.Vendor.values()) {
            check(vendors[vendor.ordinal()] > 0, "vendor coverage: " + vendor);
            check(M3CollectionSdkCatalog.byVendor(vendor).size() == vendors[vendor.ordinal()],
                    "indexed vendor filtering");
        }
        // Canonical names are never flattened into a misleading shared Multimap interface.
        var spring = M3CollectionSdkCatalog.require("org.springframework.util.MultiValueMap");
        var guava = M3CollectionSdkCatalog.require("com.google.common.collect.Multimap");
        var apache = M3CollectionSdkCatalog.require("org.apache.commons.collections4.MultiValuedMap");
        var eclipse = M3CollectionSdkCatalog.require("org.eclipse.collections.api.multimap.MutableMultimap");
        check(!spring.semanticContract().equals(guava.semanticContract()), "Spring map-of-lists != Guava multimap");
        check(!spring.upstreamName().equals(apache.upstreamName()), "Spring != Apache FQCN");
        check(!guava.upstreamName().equals(eclipse.upstreamName()), "Guava != Eclipse FQCN");
        check(spring.disposition() == M3CollectionSdkCatalog.Disposition.OPTIONAL_ADAPTER,
                "optional Spring adapter is not core or pending");
        check(M3CollectionSdkCatalog.require("org.eclipse.collections.api.list.primitive.MutableLongList")
                .existingM3Owner().equals("com.m3.collections.M3LongArrayList"), "Eclipse long shape only");
        for (String jdk : List.of("java.util.ArrayList", "java.util.SequencedSet",
                "java.util.SequencedCollection", "java.util.SequencedMap",
                "java.util.concurrent.ConcurrentHashMap")) {
            Class.forName(jdk);
            check(M3CollectionSdkCatalog.find(jdk).isPresent(), "real JDK family: " + jdk);
        }
        expect(IllegalArgumentException.class, () -> M3CollectionSdkCatalog.require("java.util.DoesNotExist"));
        expect(UnsupportedOperationException.class, () -> entries.clear());
        expect(NullPointerException.class, () -> M3CollectionSdkCatalog.find(null));
        return checks;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("PASS M3CollectionSdkCatalogSelfTest checks="+verify()
                +" families="+M3CollectionSdkCatalog.size());
    }
}
