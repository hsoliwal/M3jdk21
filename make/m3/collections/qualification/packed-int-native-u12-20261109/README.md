<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# U12: PackedIntNative, the second packed lineage's JNI codec (2026-11-09)

This is the last of the four classes covered by the user's 2026-11-09 "Full union, all four" decision for the second
Synexia packed-array lineage:

| Leaf | Class | PR |
|---|---|---|
| U9 | PackedIntArray | #2879 |
| U10 | VInt | #2881 |
| b26 | AdaptiveIntArray | #2882 |
| U12 | PackedIntNative | this change |

## Why PackedIntNative is absorbed, not unioned

M3's `jdk.m3.arrays.packed.AdaptivePackedIntNative` serves the frame-of-reference `AdaptivePackedIntArray`. Synexia's
`PackedIntNative` is a different codec: chunked fixed-width encode/decode over `IntWidth`, plus VInt sizing, scanning
and encode/decode, all under a progress monitor. The ledger row `HOLD_UNION` therefore becomes a separate internal
owner, `jdk.internal.m3.collections.packed.PackedIntNative` (placed by v6_place, packed family).

## Java: RELOCATE_M3_OWNER v6.3 (proof/)

**Landed-map receipt added.** `com.synexia.common.progress.ProgressState` -> `jdk.m3.collections.ProgressState`; M3
already owns it as the return type of `ProgressMonitor.state()`.

**Tests.** `PackedIntNativeTest` comes with its sibling helper, Synexia's `PackedIntArrayTest`. The helper is
relocated as `PackedIntLineageTest` so that it does not collide with M3's own `jdk.m3.arrays.packed.PackedIntArrayTest`.

**Declared repairs on the tests.**
- **CALLSITE_PATCH.** `new PackedIntArray(...)` becomes `PackedIntArray.of(...)`, because M3's owner is abstract
  since U9. The Synexia side gets the matching `of` through ALIAS_PATCH.
- **EXCEPTION_CONTRACT** in PackedIntLineageTest, lines 128-135. An out-of-range `set`, `copyFrom` or `fill` throws
  `IllegalArgumentException` from the M3 owner (`PackedIntArray.checkValue`) but `ArithmeticException` in Synexia.
  The M3 owner's contract governs. Only these assertions change; the Synexia side keeps its own.

**Proof** (PROOF.tsv): 8 of 8 PASS.
- javap -c and javap -v are identical for PackedIntNative.
- The mutant is caught.
- JUnit runs 13 of 13 on both sides.

## C: NATIVE_SYMBOL_RELOCATE v1.1

`synexia-common/src/main/native/collections/packed_int_width.c` (6 JNI functions) is relocated as library
`m3_packed_int_width`. The owner is host-loaded. `PackedIntNativeTest` loads it from `lint.native.path`, which the
`m3-collections-native` profile now sets.

## Census

202 native methods: BOUND 198, ABSENT 4. This adds 6 methods, all bound.

## Gates

See GATES.tsv.
