<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original dynamic-array state-mask selection

The original `M3StateMaskArrays` owner now exposes one additive, full-byte,
required-and-forbidden selection overload without introducing another collection
framework or payload layer. It verifies aligned `int[]` physical indexes and
`byte[]` state flags, applies the owner's established disjoint-mask rule, and
reuses `M3IndexMaskCompactor.selectWindow` for one primitive-only selection.

Selected physical slot IDs retain encounter order, duplicates, signed values and
identity semantics. Values remain in their original dynamic Object[] or long[]
owner. The returned index array is detached and does not own backing objects or
identity generations. The Java selector remains authoritative; requesting JNI
explicitly uses the existing JNI window-mask ABI and rejects unavailable native
code, rather than degrading silently or invoking Java callbacks from C.

The new overload preserves all existing method names, existing required-only
selection semantics and native entrypoints. Invalid aligned lengths, non-byte
masks, or intersecting required/forbidden bits are refused before selection.
Existing filter/sorted wrappers can compose these precomputed slots without
copying payloads or adding runtime adapters.

The product test uses 4,400 seeded array/state combinations, full signed and
unsigned state ranges, duplicate physical rows, an independent Java oracle,
old required-only compatibility, native-unavailable refusal, unchanged inputs
and invalid preflight cases. A strict standalone Java21 harness passed 356,868
checks in each of normal and interpreted mode using a stub for the exact
existing `M3IndexMaskCompactor` selection ABI, **not** its real implementation.
The attached GitHub Actions workflow compiles the actual original source pair
and test; exact-head job execution remains separate acceptance evidence.

Canonical Maven/OpenRewrite recipes, candidate comparisons, the test corpus
and the source hash-pinned admission manifest are private Synexia-owned.
The public M3JDK21 fork receives only executed result candidates and passive
provenance. No new JNI binary, public third-party naming, service layer or donor
source is introduced. No performance or full OpenJDK-jtreg/JCK proof is claimed.
