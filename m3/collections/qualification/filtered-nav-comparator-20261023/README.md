# M3JDK21 original-owner bounded filtered navigation contract

**Canonical Synexia recipe:** [#13198](https://github.com/hsoliwal/com.synexia/pull/13198), `com.synexia.m3.M3Jdk21FilteredNavComparator20261023`. Product [#1869](https://github.com/hsoliwal/M3jdk21/pull/1869) contains only the recipe output and review receipts, no Maven/OpenRewrite runtime dependency.

## Problem

The old JDK-backed `M3FilteredViews.NavigableMapView` interpreted any comparator `IllegalArgumentException` from a `tailMap/headMap` range as a bounded-query exception. A deliberately one-shot throwing comparator caused it to silently return an entry. This violates callback exception propagation.

## Change

Use the source `NavigableMap.ceilingEntry/higherEntry/floorEntry/lowerEntry` to acquire the first owned entry. Return it if predicate-admitted, otherwise scan the remaining exclusive bound with an iterator. Preserve `M3TreeMap.rangeOrNull` in full, including the original exceptional fallback and all null-value membership logic. No public/protected signature, added retained entry nodes, native symbols or new backing arrays.

The old owner preimage and sealed postimage SHA-256 appear in `SOURCE_HANDOFF.tsv`. The two regression test resources originated from the same canonical recipe; their Git blobs were compared before the product owner commit.

## Verification scope

- **Observed locally:** isolated Java 21 comparator-range atom compiled with `--release 21 -Xlint:all -Werror` and produced RED exit 2: exception swallowed.
- **Observed locally:** a separate proposed atom compiled with the same warnings gate and passed **65,009 assertions**, including seeded bounded-map differential, null values and four comparator directions.
- **Current product owning module:** focused JUnit and full existing collection workflow must be read from executed job reports. Until then, no source-integrated PASS is claimed.
- **Not qualified by this PR:** full built JDK/HotSpot, JCK/jtreg, multi-platform JNI and end-to-end benchmark/performance acceptance.

This is a surgical correctness atom, not complete conversion of every JDK Map into an M3 implementation.
