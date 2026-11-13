<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 parent-owned object-state masks: exact Java/JNI receiver recipe

The user's Synexia convergence workspaces are source-algorithm/catalogue/
recipe owners; `hsoliwal/M3jdk21` is the product runtime owner.
Existing `M3ObjectResidency` owns flat `Object[]`, `long[] parentBits`,
`byte[] states`, `int[] active/borrowers`, LRU and generation-safe
cooperative leases. This task does not replace or rewrite it.

## Inventory and selected problem categories

M3JDK21 already has byte-level `M3IndexMaskCompactor`, sparse
`M3BitLane28` and open indexed-mask/path/Fenwick PRs #1073/#1074.
Those should remain the primary 8-/32-bit and occupancy owners.
They cannot directly select both the **full 64-bit user-owned parent
state** and the **six-state lifecycle enum** from `M3ObjectResidency`
without losing high bits or conflating transient state with canonical state.

Select one missing behavioral atom: detached primitive-coordinate
filtering of a signed-stride path over parent-owned logical state.
Its inputs are caller-owned `int[] orderedSlots`, logical
`offset/count/stride`, required and forbidden **64-bit** parent
flags, and lifecycle allowed-mask (one bit per `State.ordinal()`).
Duplicates and encounter order are preserved even in reversed paths.

Source alternatives and complexity/rights comparisons are in
`CATEGORY_CATALOGUE.tsv`; exact upstream Git-file references are in
`PROVENANCE.tsv`. LeetCode, HackerRank and GFG contribute only category
and adversarial testing ideas: no challenge/editorial implementation copied.

## Mandatory recipe-first

Canonical `com.synexia.m3.M3Jdk21ResidencyStateMask20261017`
reuses the existing `M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedTextSnapshotRecipe`: **three ABSENT-before Java21 files
plus one ABSENT-before C17 JNI source**, sorted SHA-256 manifests, exact
templates, provenance and real Maven/Recipe.run tests. Fix templates
and rerun on any defect; never hand-edit product postimages.

The existing text provider admits `src/main/native/**` sources. To
avoid modifying its trust boundary and historical verification tree,
qualify the C donor at `src/main/native/m3_residency_state_mask.c`
and receive the SAME exact Git blob at product path
`m3/collections/src/main/native/collections/m3_residency_state_mask.c`.
The receiver verifies sha256 and bytes equal across the source crate.

## Contract and performance boundaries

- Java source validation uses overflow-safe `long` index arithmetic
  for signed stride and checks all physical slots BEFORE reading state.
- Selection holds the existing parent monitor while observing each
  parent word/lifecycle; mutable caller coordinate arrays must remain
  stable for the duration of the call.
- Java selection is exact result `int[k]` with no per-child object,
  no retained state cache, no materialization or new GC root. Validation
  + two mask scans are O(n), result memory O(k).
- `first` and `count` do not allocate result arrays.
- JNI is ONLY `Backend.JNI_REQUIRED` and fails if the explicitly
  installed `m3_residency_state_mask` native library is absent.
  It passes 3 TEMPORARY primitive snapshots (int/long/byte), uses
  Get*ArrayRegion bounded chunk operations and an exact JNI int[] result.
  Native memory overhead is O(n), so no universal native speedup is
  claimed. The kernel retains no JNI object references or pointers.
- A state mask `0` selects nothing. Unknown state mask bits and
  required/forbidden overlap fail before any output. `Long.MIN_VALUE`
  parent flags and reversed logical paths are first-class regression
  cases. Default Java semantics are unchanged without the optional
  library.
- This is a detached snapshot, NOT a live filtered view, transparent
  projection reclamation, reference-counting implementation or GC
  barrier enhancement. Ordinary HotSpot GC remains reachability owner.

## Verification

Standalone test `M3ResidencyStateMaskProof` includes **1,600 seeded
rounds**, duplicate coordinates, reversed signed paths, exception
boundaries and parent state persisting across `use/retire`. The first
local Java21/C17/JNI proof was executed against a test double of the
original parent API (not a product build): Java-only 4,820 checks and
native enabled 6,419 checks, same deterministic fingerprint
`12197969927824375845`. This is NOT JDK product CI evidence.

Receiving M3JDK21 must compile/run with the ACTUAL `M3ObjectResidency`
and its slot allocator/lane dependencies. Its focused CI must run
strict Java21, `-Xcheck:jni` under Serial, Parallel, G1, ZGC and
Shenandoah plus interpreter, and actual Maven/OpenRewrite JUnit
generation/fixed point. The existing installed-source verifier and
JDK build gates remain fail closed. JNI packaging on Windows/ARM,
whole-project jtreg/JCK, JMH and full DOOP/GC compiler integration
are separately pending, not silently claimed here.

## Additional array-reuse behavioral atom (v2)

`selectInto(arena, orderedSlots, offset, count, stride, required, forbidden,
allowedStates, destination, destinationOffset)` performs a full parent-monitored
mask/count preflight before storing indexes into an existing caller-owned
`int[]` segment. It rejects output/input aliasing, invalid destination
boundaries and insufficient capacity without modifying the output.
The selected indexes, duplicate row IDs, mask semantics and original order
are unchanged; no new per-row Entry/Lease/object is created. Complexity is
O(n) scan with O(1) extra storage beyond caller-provided destination.

The v2 source templates and regression proof were generated from the
canonical Synexia copy first and mirrored byte-for-byte to the target
M3JDK21 receiver. Local OpenJDK21.0.12.1 strict compilation and C17 JNI:
9,627 Java-only checks, 11,226 JNI-enabled checks, identical deterministic
fingerprint under Serial, Parallel, G1, ZGC, Shenandoah and G1 interpreter.
**Those checks use a residency API test double**; actual product JDK/Maven
and hosted OpenRewrite SDK qualification remain mandatory and unverified.
