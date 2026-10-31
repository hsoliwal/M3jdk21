<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 filtered SortedMap/NavigableMap strict default-method receiver

Canonical recipe authority: [Synexia #12147](https://github.com/hsoliwal/com.synexia/pull/12147)
at exact recipe/test commit
`31487226f363c25617014572ff0cbb19a8522e51`.
Product receiver: hsoliwal/M3JDK21 `m3/collections`, separately
built. **Draft, not STRING -> arrays -> collections phase promotion.**

## Problem and existing JDK owner

`M3FilteredViews.SortedMapView` is correctly a live filtered,
backed read-only view for ordinary direct methods. However inherited
`AbstractMap` Map defaults can no-op without throwing
`UnsupportedOperationException` (e.g. computeIfAbsent absent key
returning null; conditional remove absent key). Derived views can also
silently no-op in `removeIf`, `removeAll` and `retainAll`.
That contradicts M3's own explicit read-only policy and the JDK
`Collections.unmodifiableSortedMap`/NavigableMap contract.

Reuse those **JDK** wrappers at the two existing factory return
boundaries. Preserve the exact existing M3 SortedMapView/NavigableMapView
delegate as filtering, stored-representative, comparator, range,
backing-liveness and iteration authority. No new collection data
storage, per-row Entry, object state flags, native backend or JNI
signature. No programmatic element copying; one thin return descriptor
per factory is admissible.

The returned JDK read-only wrappers guard inherited/default no-op
methods and the transitive `entrySet`, `keySet`, `values`,
`headMap`, `tailMap`, `subMap`, `descendingMap` and
Java21 `reversed` views, preserving the existing public factory
method signatures. Java predicates/comparators continue to execute
only in Java.

## Source and exact replay

- `M3FilteredViews.java`: original Git blob
  `10bc8328c0aef2cab69ba0d55f7f566fc4e60df3`,
  SHA-256 `d67a632f63647f033e71360858f8f1be1f93418880178a96437bb0605998ad5b`.
  Expected generated SHA-256
  `3657695ea56c3c01e53d332e37aede2a126efbf718378d217b72d844a4192f86`.
- Two new Java sources: dependency-free main oracle and Maven/JUnit
  adapter with source hashes in `SOURCE_RECEIPT.tsv`.
- The `recipe-mirror/` source payloads are byte-identical to the
  privately developed Synexia canonical recipe; they are read-only
  proof/custody artifacts, **not a second recipe implementation**.
- `verify.py` compares the exact source and template bytes with
  manifest, SHA-256 and original preimage.

## Tests and remaining gates

Focused red/green workflow compiles the exact original owner against
the new oracle, demands the expected absence-of-UOE failure, then
compiles the generated target owner against both the new and the
existing `M3FilteredViewsContract` Java21 oracles. Maven/JUnit
and Synexia real OpenRewrite SDK jobs are independent admission gates.
The JDK unmodifiable wrapper local mini-oracle **passed 84 checks**;
this is not full source or hosted test proof.

**Still pending:** actual selected owner red/green CI, real recipe
SDK/JUnit execution, full owning module and JDK/jtreg/JCK, all
compatible platforms, performance and JNI integration gates.
No speedup or global five-library contract superset is asserted.
Original Java/JNI primitive mask selection and sorted/filtered views
elsewhere remain unchanged, including Apache/Guava/Spring/Eclipse
facades awaiting separate qualification.

Original first-party expression: Copyright 2026 Hitesh Soliwal
and contributors, Apache-2.0; JDK/third-party code is invoked
through standard APIs, not copied. Challenge sites provide algorithm
taxonomy only, no borrowed solution bodies.
