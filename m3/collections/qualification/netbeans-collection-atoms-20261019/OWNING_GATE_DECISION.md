<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Owning collection contract corrections

The changes are outputs of the canonical Synexia Maven/OpenRewrite recipe against M3JDK21 baseline `677437be295b23f7ccf7409be287008944d0e8b0`. Three existing production owners change: `M3FilteredViews`, `M3SlotMap`, and `M3KeyedParallelismPlan`. The full current Java/JNI verifier remains the qualification authority. Original failed runs remain in the canonical task evidence and history.

## Filtered sorted views

The baseline declares four methods twice and cannot compile. Reconcile the predecessor contracts, preserve strict read-only reversed views and predicate callback order, and reuse the existing `M3SlotSpliterators.sorted` atom on reversed sorted subrange splits. A duplicate-removal-only reference compiles but fails the sorted/comparator regression. `WRAPPER_DECISION.md` and `CONTRACT_EVIDENCE.json` contain the differential evidence, including identical callback tapes and public descriptors.

## Caller-owned entry projections

The established array-storage invariant and older `M3NoRetainedStructuralEntryTest` and lazy-map tests require fresh API entries and forbid map-owned entry retention. A newer owner-held LFU cache violates those contracts. Reuse the existing `LiveEntry` projection per explicit API request and remove the two private cache fields. Preserve slot/AVL algorithms, weak-key reachability fences, live lookup and write-through behavior, immutable navigation snapshots, iteration, and revisions.

The protected `cacheEntryFacades` hook retains its descriptor, default return, invocation count and exception effects. Its return cannot authorize prohibited owner retention. Existing internal invalidation methods keep their signatures and become no-ops. The unrelated generic `M3LFUCache` and its two generic tests are unchanged. The conflicting map tests now exercise fresh projections, snapshots, canonical stored-key identity, nulls, lifetime, subclass hook behavior and inherited-owner graph retention. Existing no-retention tests remain unchanged.

The reuse source is the existing M3 owner and canonical Synexia `SlotMap.java`, blob `3ceabb2f939096e16562940009495484c0cf581f`, with its Apache-2.0 notice retained. All 505 compiled public/protected owning-module signature outputs are identical, with SHA-256 `fe05ffa6026142dd2bf9973f402df0d04a7375b00c7b6de774fcda6ca46554c2`. One caller-owned allocation per live Entry request is a contract requirement; no throughput gain is claimed.

## Key-affinity failure progress

The full owning suite exposed a coordinator blocked in a bounded failed-lane queue after the worker had published a mapper exception and exited. Keep the coordinator, source/key demand, global admission accounting, normal mapping, and ordering unchanged. On mapper failure, publish the original exception and drain cancelled work using the existing queue/end marker until shutdown interrupts the worker. No subsequent mapper callback runs on that failed lane. `KEYED_ADMISSION_DECISION.md` and `KEYED_ADMISSION_EVIDENCE.json` document the selected atom, rejected design alternatives and bounded proof.

## Fixture and gate corrections

The reserved LIVE-bit test supplied a String key to an Integer-key map, preventing test compilation. Integer zero preserves the exact reserved-bit refusal assertion. The required/excluded mask boundary expected only rows zero and two for states `[0,1,2,3,128,255]`, required mask zero and excluded mask one; row four also matches. Correct that expectation while retaining all 65,536 mask-pair and randomized-array oracle cases. No selector production code changes.

The required-native gate includes the current long-path-mask owner and checks every current native library, generated headers, native test skip counts, explicit contracts and packaged resources. The first complete receiver attempt found a test that assumed native unavailability in both lanes. Its successor preserves missing-library refusal and tests actual native selection when loaded. Production range-map and native selector code require no correction.

Two Python historical-receipt tests now check immutable receipt postimages rather than demanding equality with subsequently evolved source. They preserve the historical hashes and every verifier; all 15 owning metadata unit tests pass. The separate stale installed-source inventory remains an explicit limitation, detailed in `METADATA_DECISION.md`.

## Evidence boundary

Prepared candidates, executed recipe outputs, complete target gate results, and hosted checks are distinct evidence. The first actual combined target attempt is preserved in `INITIAL_OWNING_GATE.json`; the successful successor owning gate is recorded in `FINAL_OWNING_GATE.json` (Java 453/0/0/2, required JNI 453/0/0/0, full native and package tail passed). Donor production candidates were refused where cost admission failed. This finite module refinement does not promote the global String-first receiving phase or claim comprehensive acceptance of every deferred catalogue category.
