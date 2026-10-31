<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 source-sealed SortedSet / SortedMap facade increment

This is a bounded follow-on **after merged M3JDK21 PR #1127**. The older
receiver delivered a 107-name SDK catalogue and reused JDK/M3 filtered
owners; this increment does not convert pending Guava/Eclipse/Spring/Apache
families into installed binary-compatible types.

## Canonical sequence

1. Synexia merged PR [#12138](https://github.com/hsoliwal/com.synexia/pull/12138)
   introduced a Maven-bound source-sealed v3 recipe with three atoms. A
   different concurrent receiver already fixed the explicit-public-constructor
   warning in `M3ObjectStateSlots28` on `master`, so that stale atom must
   not be reapplied.
2. Corrective source-first Synexia PR [#12145](https://github.com/hsoliwal/com.synexia/pull/12145)
   limits the canonical recipe to **two** exact Java transformations. The
   mirrored Java recipe has Git blob
   `0ea9e8bca2a3522d037e675a1ae298f35e59a440`, identical in both repos.
3. M3JDK21 receiver adds `M3CollectionSdk.readOnlyFilteredSortedSet` and
   `M3CollectionSdk.readOnlyFilteredSortedMap` delegating to incumbent
   `M3FilteredViews`, plus strict SortedSet/SortedMap contract assertions.
   Generated Java Git blobs are:
   `0962823301c3f80dbe4956600fe572af2067439b` and
   `8d01cb9fbefb76f65a70759d93153cfc20a20008`.
   Preimages were `042069cd167191e1faa1710fb9a9e65bca14983c`
   and `a62b63a3399d926dabe6645f342381d2a28107ca`.

## Mechanical, ordered proof

The receiver workflow verifies the original v2 Maven generator against its
own hash-sealed SDK templates and the unchanged on-head catalogue, then
executes v3 `--apply` on that real preimage, `--check` twice, and byte
compares all four generated Java files to head. It also runs the Java21 SDK
contract oracle, existing C17 JNI selector tests and full collections Maven
suite. Source and output drift refuse; no force/rebase or manual production
postimage patching.

No per-element payload duplicate, retained Map.Entry owner, JNI entry point
or Java predicate native callback is added. Live JDK filtered views must
continue to use their backing comparator and range checks rather than caching
a stale mask. Primitive snapshots already use `M3LongBitmapFilteredSnapshot`
and existing `M3IndexMaskCompactor`; a speculative second mask hierarchy
would violate the single-owner policy.

This is an exact subset, **not** complete five-provider compatibility.
Head CI, optional provider contract suites, jpms, installed-image tests and
CPU/heap performance claims remain unproven until independently green.
