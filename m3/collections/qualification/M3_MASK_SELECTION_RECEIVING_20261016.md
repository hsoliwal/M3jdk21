<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 primitive selection: indexed masks and zero-intermediate compaction

**Status: draft receiver candidate, not an installed M3JDK replacement.**

## Sources and target
Synexia's original ordered-index/byte-mask JNI compactor at
[hsoliwal/com.synexia#11804](https://github.com/hsoliwal/com.synexia/pull/11804),
pinned source commit `9e22725d1e5c440487ce8d59cd1aa15ec217f793`.
Exact target-first source-sealed OpenRewrite recipe:
[hsoliwal/com.synexia#11866](https://github.com/hsoliwal/com.synexia/pull/11866),
commit `7370b3e158e5463b76dc1639b1f943e6a597a446`.
Receiving baseline: M3JDK21 `855afe57a679aeb454052630de7beddcb25f5f8f`.
Runtime postimages: `m3/collections/qualification/mask-selection-postimages-20261016.tsv`.
The target retains its Apache-2.0 module license, first-party attribution and
current `com.m3.collections` package ownership.

## The no-intermediate-object rule
- Resident element storage stays in the existing List `long[]` and Deque
  circular `long[]`. Neither operation allocates a per-element object,
  `Map.Entry`, `Long` wrapper, second payload owner or intermediate array.
- `retainByStateMask(byte[] flags, int requiredMask)` compacts **in place**.
  Flags are position-aligned in logical encounter order and are never retained.
  Every input condition is checked before live-lane mutation. Fail-fast iterators
  invalidate only after a real structural removal.
- `M3IndexMaskCompactor.select(int[]/long[] rows, byte[] states, mask)`
  allocates precisely one final output primitive array. Both runs are stable
  subsequence selections retaining duplicates; unsigned masks 0..255.
  The Java backend uses two scans instead of donor O(n) scratch+copy.
- Target-specific JNI loads `m3_collections_mask` only when explicitly
  requested. Primitive regions and bounded C17 stack chunks cross the boundary.
  No per-row callbacks, pinned Java heap pointers or native retained references.
- These rules apply to internal data paths. Iterator state, explicit view
  facades, user-supplied callbacks and JDK `Map.Entry` boundary objects may be
  unavoidable where their public contracts demand them. No blanket promise
  of literally zero Java objects per collection API call is made.

## Scope that remains open
This is a first-party `m3/collections` module receiver; it does not yet replace
`java.util` / `java.util.concurrent` backend implementations, change java.base
or HotSpot, install any optional JNI into a produced JDK image, or convert all
Eclipse/Guava/Apache/Spring intrinsic allocation-returning APIs.
Conformance, interop, concurrency, serialization, native installed-image and
performance gates remain independent. Preserve the canonical receiving order
**String → arrays → collections → AST/compiler**; no phase promotion.

## Reproduce on a Linux Java21 native host
Run `python3 m3/collections/qualification/verify_mask_selection.py` first.
The focused workflow `.github/workflows/m3-collections-mask-receiver.yml`
compiles all existing core Java sources with Java21 warnings-as-errors,
compiles/runs the actual receiver self-test in Java mode, builds the target JNI
C17 source under strict warnings and calls the real JNI binary under
`-Xcheck:jni`.

`mvn -B -f m3/collections/pom.xml verify` and the wider installed-image gates
remain required; they are not replaced by this focused suite. Only mark
individual gates passed once their exact runner logs and exit results exist.
