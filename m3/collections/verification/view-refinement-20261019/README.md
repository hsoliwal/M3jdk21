<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3 filtered and sorted views: received refinement

Runtime owner: **M3JDK21**. These are private refinements in the existing
`M3FilteredViews` and `M3LongIndexProjection`, not an M3-plus-JDK layer.
Live generic views stay live; primitive indexed snapshots keep one shared long[]
payload, int[] physical indexes, half-open offsets and unsigned-byte filter masks.
No retained entry store, replacement collection framework or runtime dependency is added.

The first correction handles a comparator placing null BETWEEN non-null members,
including filtered descending/range and map-key views. The second reverses the
order of equal-value runs of an already sorted immutable index without reversing
ties or allocating merge scratch. Unordered stable merge sort and existing scalar,
filtering, JNI and same-direction paths remain in their original owners.

## Canonical recipe and exact source custody

[Synexia PR #12461](https://github.com/hsoliwal/com.synexia/pull/12461)
owns `com.synexia.m3.ViewOrder20261019`, its class-backed Maven/OpenRewrite
composition, original fixtures, templates, hashes, category decisions and replay proof.
The pinned source commit is `db329cda4434f40c282285c5879984b515660403`.
It was published before this receiver. See
[the canonical proof instructions](https://github.com/hsoliwal/com.synexia/blob/db329cda4434f40c282285c5879984b515660403/synexia-openrewrite-recipes/verification/view-order-20261019/README.md).

All four received Java files are byte-identical to the actually emitted, locally
qualified recipe outputs. `RECEIPT.json` pins before/after hashes and bounded proof.
Source generation used the existing canonical manifest kernel, labelled
**KERNEL_REPLAY**, not unexecuted OpenRewrite SDK execution. No receiver production
output was hand-patched. Original license notices are retained.

## Reproduce focused Java checks from this checkout (JDK 21, Bash)

```sh
mkdir -p target/m3-view-order/classes
javac --release 21 -Xlint:all -Werror -d target/m3-view-order/classes \
  m3/collections/src/main/java/com/m3/collections/{M3FilteredViews,M3LongIndexProjection,M3IndexMaskCompactor,M3LongAddressKernel}.java \
  m3/collections/verification/view-refinement-20261019/*Probe.java
java -ea -cp target/m3-view-order/classes com.m3.collections.M3FilteredNullNavigationProbe
java -ea -cp target/m3-view-order/classes com.m3.collections.M3ProjectionRefinementProbe java
```

The canonical `verify.py` builds BOTH actual C libraries with C17 warnings-as-errors,
runs required JNI under `-Xcheck:jni` and UBSan, checks eight exports and generated
headers, tests absent libraries and exceptional partial writes, and compares three
fresh JVM allocation/CPU/heap/RSS observations. Native binaries are temporary build
outputs, never committed. The existing native C sources and JNI descriptors are unchanged.

Local results: 3,555,352 navigation checks; 282,479 Java projection checks;
287,107 required-JNI checks, repeated under UBSan; four absent-library refusals.
The unchanged original navigation fails the five frozen witnesses. Forty known
sort-direction conversions of 262,144 indexes allocate 83,888,952 versus 41,945,112
Java bytes in the retained benchmark. This is temporary-allocation reduction;
retained payload and post-GC-request heap are effectively unchanged. These are
scoped measurements, not an application or widget speedup guarantee.

## Explicit scope and remaining gates

This receiver improves two collection owners. It does not claim implementation or
qualification of the whole conversation. SDK/JUnit (mvn unavailable locally), full
collections Maven tests, complete M3JDK21/HotSpot/jtreg, other native OS backends,
SWT/Nebula/Skija/OpenCV visible-screen acceptance and React/RWT/GWT integration are
NOT_RUN. Existing mutable/primitive sorted-view owners are preserved, not replaced.
The original full product build and runtime gates remain authoritative. Keep draft
until the applicable remaining gates run; a source receipt or zero-job CI is not promotion.
