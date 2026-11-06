<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 CPU Wave22 — original JNI fused monotone-prefix interval batch

## Recipe-first source ownership and source custody

Canonical original [Synexia #15301](https://github.com/hsoliwal/com.synexia/pull/15301)
`b09b32f8a42205618aa0fadc60cbf40b59e15e3f` owns the existing `M3HashPinnedTextSnapshotRecipe`
and `M3HashPinnedJavaSnapshotRecipe`, named Maven/OpenRewrite
`m3-cpu-structured-wave22-20261102` crate, source manifests,
fixed-point and refusal tests. Do not clone or rewrite these owners.

Downstream parent [M3JDK21 #2303](https://github.com/hsoliwal/M3jdk21/pull/2303)
`853d3f26ac640b5d32d65ee0819f3330585eb256` already holds the exact Wave21 signed-half-open interval
Java owner, optional JNI wrapper, C17 source and independent tests.
The original C bytes at `native/m3_interval_set.c` have
Git blob `37a7371cfac52f2f57072c8f00632bfcd1e19007`,
SHA-256 `ef71160197b39b8a0e58aa3453c9c6ba2c1db945c94a67db15c00ce0c2fe63b9`.
That is **identical** to the original Maven/OpenRewrite preimage.
The original Wave22 complete C source after-image is SHA-256
`8e256da0d370c3ea3a29af132ed5bb8e0b399e88b7d77aa6260d214f0542228f`,
Git blob `8e8a36d707bff57ec5a08e2b239e7af49cc3a7e0`.
One existing private C17 function body is refined mechanically;
its public Java classes, method signatures and the exported symbol
`Java_com_m3_tools_cpu_M3IntIntervalSetNative_contains0` remain fixed.

## Algorithm and JNI behavior

The original Wave21 algorithm pre-scans a whole query batch
for monotonicity before processing. Wave22 preserves its threshold
`length >= 64 && 2*length >= intervalCount` but fuses sorted-prefix
proof with the monotone interval cursor. Upon the first inversion
at query q, it preserves the completed prefix and delegates the
remaining suffix q..end to the original signed binary search.
Original query order, duplicate and negative values, signed half-open
`[start,end)` semantics, fresh byte flags and `JNI_ABORT` ownership
remain unchanged. No JNI pointer is retained across calls.

Receive canonical create-only C17 streaming oracle, Java21 JNI
streaming oracle, and 91-category Wave22 catalogue (14 columns).
The preceding eleven Wave21 columns must be byte-identical for
every row. Source identity, donor rights and proof stage are kept
in SOURCE_RECEIPT.tsv, DONOR_REVIEW.tsv and EVENT_LOG.tsv.

## Strict algorithm donor comparison

LeetCode #56/#57 closed/inclusive interval tasks, HackerRank
Gridland Metro inclusive tracks, and GeeksforGeeks sweep methods
are problem-shape references only; none supplies equivalent
JNI signed half-open query order and buffer-lifetime semantics.
GitHub RoaringBitmap is Apache-2.0 but represents bitmap membership
rather than this normalized interval-pair owner. **No external
challenge, editorial or library source was copied**.

## Hard stage order

Inventory/pinned donor preimage → documentation → original exact
Maven/OpenRewrite recipe and SHA manifest → one C17 atom receipt
→ new oracle and catalogue receipts → lint/compile → executable
C17 scalar and Java21/JNI differential in JIT/-Xint, -Xcheck:jni
→ original Maven/OpenRewrite JUnit fixed-point/refusal → full
M3JDK21 JMOD/jtreg and platform/ABI/runtime → measured performance.

**HOLD:** The JNI shared library is compiled as an explicit
sidecar during isolated qualification, NOT packaged into the
standard CPU-tools JMOD by its existing build.py. This task
does not authorize edits to build.py, module-info.java, develop
or existing Java/JNI public APIs. Real CI must execute before
any gate is called green; history ancestry alone is insufficient.
