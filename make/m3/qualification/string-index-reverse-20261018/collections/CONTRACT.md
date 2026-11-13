# M3JDK21 primitive array and state-mask refinement contract

<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->

## Ownership and boundary

The current M3 collections owners remain in M3JDK21. Reusable source-sealed recipe custody is Synexia. This isolated leaf preserves interfaces, JNI ABI and all contract observations; it does not advance the canonical String-first receiving sequence or silently import overlapping live-wrapper work.

## Behavioral atoms

`M3LongBitmapFilteredSnapshot.contains(long)` uses signed-long binary search over its existing immutable ascending or descending projection. Encounter-order projection retains the original scan. No retained field, array, per-call object, comparison callback or mutation is added. Duplicates, extrema, filtered membership, nonzero slices and spliterator behavior retain their existing owner contracts.

`m3_state_mask_histogram.c` batches nonunit strides within the existing 256-byte JNI region scratch. Empty/singleton and stride magnitudes at least 256 retain the original scalar path. The existing unit-stride batch path retains its region-read order; tallying a commutative histogram does not need a second within-chunk orientation branch. Geometry validation and checked indexing precede native reads. Invalid input leaves the caller output untouched; successful publication and the JNI entry point remain unchanged. No object pinning, retained array or new native allocation is introduced.

## Candidate comparison and verification

Binary search is selected only where immutable projection order already proves its precondition. Live filtered/sorted wrappers retain callback order, invocation counts, null handling and exception boundaries; overlapping existing PRs require separate contract admission. Stride batching was compared with the original region/scalar kernels. Small and unbatchable performance controls must remain visible, including unfavorable results.

Lint, compile all 134 actual production Java owners for both revisions, compare public descriptors, compile C17 with strict warnings, check native ABI, run independent Java membership/histogram oracles, run real JNI with Xcheck:jni and UBSan, then perform diagnostic measurements and repeat source generation. The frozen source hashes identify the final native revision. Earlier candidates and failed performance controls remain evidence, not promotion claims. This leaf does not establish uniform JNI nonregression, whole-library speed or RAM bandwidth.
