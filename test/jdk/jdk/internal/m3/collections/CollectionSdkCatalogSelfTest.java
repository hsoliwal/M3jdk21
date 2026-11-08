/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Standalone provider-family registry/contract oracle; needs only Java 21. */
public final class CollectionSdkCatalogSelfTest {
    private static int checks;
    private CollectionSdkCatalogSelfTest() { }
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
        var entries = CollectionSdkCatalog.families();
        check(entries.size() >= 95, "provider coverage inventory");
        check(CollectionSdkCatalog.size() == entries.size(), "family count");
        int[] vendors = new int[CollectionSdkCatalog.Vendor.values().length];
        int optional = 0;
        var selectedOptional = new HashSet<String>();
        String previous = "";
        for (var family : entries) {
            checks++;
            check(family.upstreamName().compareTo(previous) > 0, "sorted unique external name");
            previous = family.upstreamName();
            check(CollectionSdkCatalog.require(family.upstreamName()).equals(family), "lookup equality");
            if (family.hasExistingM3Owner()) {
                check(family.existingM3Owner().startsWith("jdk.m3.collections."), "owner identity");
                check(family.disposition() != CollectionSdkCatalog.Disposition.PENDING,
                        "pending cannot claim installed owner");
            }
            if (family.disposition() == CollectionSdkCatalog.Disposition.PENDING) {
                check(!family.hasExistingM3Owner(), "pending is not already installed");
                check(!family.candidateRole().isEmpty(), "future role explicit");
            }
            if (family.disposition() == CollectionSdkCatalog.Disposition.OPTIONAL_ADAPTER) {
                optional++;
                check(selectedOptional.add(family.upstreamName()),
                        "optional adapter already counted");
                check(family.hasExistingM3Owner(), "optional adapter must name a real owner");
                check(family.existingM3Owner().startsWith("jdk.m3.collections.interop."),
                        "optional compatibility never pollutes core dependency boundary");
            }
            vendors[family.vendor().ordinal()]++;
        }
        check(optional == 14 && selectedOptional.equals(Set.of(
                "com.google.common.collect.BiMap",
                "com.google.common.collect.ClassToInstanceMap",
                "com.google.common.collect.ListMultimap",
                "com.google.common.collect.Multimap",
                "com.google.common.collect.SetMultimap",
                "com.google.common.collect.SortedSetMultimap",
                "com.google.common.collect.Table",
                "org.eclipse.collections.api.list.MutableList",
                "org.eclipse.collections.api.map.MutableMap",
                "org.eclipse.collections.api.map.sorted.MutableSortedMap",
                "org.eclipse.collections.api.set.MutableSet",
                "org.eclipse.collections.api.set.sorted.MutableSortedSet",
                "org.springframework.util.AutoPopulatingList",
                "org.springframework.util.MultiValueMap"
        )), "selected optional adapters must be enumerated exactly");
        for (var vendor : CollectionSdkCatalog.Vendor.values()) {
            check(vendors[vendor.ordinal()] > 0, "vendor coverage: " + vendor);
            check(CollectionSdkCatalog.byVendor(vendor).size() == vendors[vendor.ordinal()],
                    "indexed vendor filtering");
        }
        // Canonical names are never flattened into a misleading shared Multimap interface.
        var spring = CollectionSdkCatalog.require("org.springframework.util.MultiValueMap");
        var guava = CollectionSdkCatalog.require("com.google.common.collect.Multimap");
        var apache = CollectionSdkCatalog.require("org.apache.commons.collections4.MultiValuedMap");
        var eclipse = CollectionSdkCatalog.require("org.eclipse.collections.api.multimap.MutableMultimap");
        check(!spring.semanticContract().equals(guava.semanticContract()), "Spring map-of-lists != Guava multimap");
        check(!spring.upstreamName().equals(apache.upstreamName()), "Spring != Apache FQCN");
        check(!guava.upstreamName().equals(eclipse.upstreamName()), "Guava != Eclipse FQCN");
        check(spring.disposition() == CollectionSdkCatalog.Disposition.OPTIONAL_ADAPTER,
                "optional Spring adapter is not core or pending");
        check(CollectionSdkCatalog.require("org.eclipse.collections.api.list.primitive.MutableLongList")
                .existingM3Owner().equals("jdk.m3.collections.LongArrayList"), "Eclipse long shape only");
        for (String jdk : List.of("java.util.ArrayList", "java.util.SequencedSet",
                "java.util.SequencedCollection", "java.util.SequencedMap",
                "java.util.concurrent.ConcurrentHashMap")) {
            Class.forName(jdk);
            check(CollectionSdkCatalog.find(jdk).isPresent(), "real JDK family: " + jdk);
        }
        expect(IllegalArgumentException.class, () -> CollectionSdkCatalog.require("java.util.DoesNotExist"));
        expect(UnsupportedOperationException.class, () -> entries.clear());
        expect(NullPointerException.class, () -> CollectionSdkCatalog.find(null));
        return checks;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("PASS M3CollectionSdkCatalogSelfTest checks="+verify()
                +" families="+CollectionSdkCatalog.size());
    }
}
