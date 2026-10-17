<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Versioned recipe receiver: filtered sorted wrappers and strict compiler fix

## Source chain, without rewriting history

- Canonical upstream v2 catalogue recipe: Synexia merged PR [#12113](https://github.com/hsoliwal/com.synexia/pull/12113), exact successor `6c701554f2152f51e32d5b54a5ab5245d24bf146`.
- Canonical next version: Synexia draft PR [#12138](https://github.com/hsoliwal/com.synexia/pull/12138), branch `m3/sdk-sorted-views-contract-20261017`; mirrored Maven recipe and category review here. Generator Git blob `7b9f7d5a9bfeecc3cf7f4b1b2e328b84e3a2d23e`.
- Admitted predecessor: M3JDK21 PR #1127 commit `838866062b4a63622c8a80e33291414543de3009`, with 107 API names and 12 optional-adapter names.
- Three new Git-blob-sealed postimages and preimages are enumerated by the canonical recipe and README. No production changes are hand-patched, rebased or force-written.

## New receiver behavior

`M3CollectionSdk.readOnlyFilteredSortedSet` and
`M3CollectionSdk.readOnlyFilteredSortedMap` delegate to the existing
`M3FilteredViews` overloads. These are **live, read-only filtered views** of
the caller's JDK-owned `SortedSet`/`SortedMap` storage, retaining comparator,
range bounds and ordering; they do **not** create a new payload owner.
The generated oracle checks visibility, subrange behavior, reflected source
changes, rejected mutations and immutable `Map.Entry` values.

`M3ObjectStateSlots28` receives an explicitly spelled-out public zero-arg
constructor. This is source/binary/behavior equivalent to its previous implicit
public constructor. Its slot generation, masks, 28-bit lane structure, callback
behavior and JNI boundary are unchanged.

## Ordered executable gate

The exact-head workflow `m3-sdk-families-20261017.yml` must:
1. Maven-verify the old catalogue generator and new recipe selftest.
2. Recreate the five relevant v2 files from the exact predecessor Git commit.
3. Verify v2 `--check` on those predecessors; execute v3 `--apply` then
   `--check` on the same scratch receiver and on exact head.
4. Byte-compare all five scratch outputs to committed receiver files.
5. Compile and execute strict Java 21 SDK tests, existing C17 JNI parity
   under `-Xcheck:jni`, and the owning full Maven collections module.

This is incremental SDK wrapper exposure, *not* full Guava/Eclipse/Spring/
Apache/JDK substitutability, complete provider adapters, native platform
coverage, or benchmark certification. Keep PR draft until the hosted gates are
actually green; classify unavailable gates as not proven.
