<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Bounded indexed path-mask output window

The canonical Maven/Java exact-source recipe is held in `hsoliwal/com.synexia`
under `synexia-m3-recipe/recipes/index-path-mask-window-20261017`.
This target only receives the compiler-checked generated Java owner, test, and CI.

The existing `M3IndexPathMasks.select` (detached `int[]`) and
`firstMatchingIndex` APIs/behavior are held. New `scanWindow` borrows the same
ordered `int[]` row IDs, logical-to-physical `int[]` offsets, `int[][]` state
planes, and signed `(first,count,stride)` path. It writes into a caller-supplied
`int[]` output segment, at most 4,096 values per call. The high 32 bits of
its result encode **next path ordinal**, not a signed array index; the low 32
bits encode written count. This supports reverse/sparse paths without inventing
a sentinel and maintains duplicate occurrence order.

Validation of stage shape, complete logical path geometry, capacity, output
aliasing, and all *inspected* physical offsets precedes any write. Uninspected
later slots are validated upon next window. Masks are unsigned 32-bit bitwise
required/forbidden tests and may overlap, selecting nothing. Source arrays
must stay immutable during each operation; no race or ownership epoch guarantee
is introduced. The method does not replace live Java filtered/sorted wrappers.

Java backend needs O(1) auxiliary memory and O(rows inspected × planes) time.
`NATIVE_REQUIRED` uses the **existing unchanged** C17/JNI selector over only
the inspected segment: it allocates at most one returned `int[4096]`, never
retains Java array pins, calls a user callback or introduces another JNI symbol.
No automatic native fallback, throughput or memory-speedup claim.

Corrective behavior is ADDITIVE M3 API only. All existing public JDK interfaces,
M3 public method descriptors, sorted comparators, indexing contracts, observable
old behavior and JNI native exported symbols are preserved. The new method has
new validation semantics explicitly stated above. No global canonical string
identity, HotSpot lowering, universal virtual-address promise or JDK collection
replacement is asserted.

HOLD: full Maven/OpenRewrite SDK/root reactor, full JDK-jtreg/JCK, native
cross-platform/parity and JMH remained unexecuted in local qualification.
