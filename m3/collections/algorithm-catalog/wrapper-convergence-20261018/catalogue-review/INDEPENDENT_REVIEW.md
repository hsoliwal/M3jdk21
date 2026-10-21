<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Independent review of the admitted index, native, and iterator atoms

Reviewed on 2026-10-18 by the independent wrapper-audit agent. This is a supplement to the sealed catalogue, not a replacement for its original baseline census. No production file, frozen oracle, or previously sealed catalogue file was changed during this review. No compiler, test, lint, or project runtime was executed by this reviewer.

## Decision and scope

**No production correctness blocker found for these three narrow atoms.** The Java group reversal preserves stable physical occurrence order; the native suffix copy preserves values and window geometry; the iterator guard repairs the documented unconditional mutation refusal while retaining the inherited read path. Final admission still requires the parent workflow's candidate compile, test, runtime, and evidence checks against the exact integration revision. The execution results below are imported baseline results, not candidate qualification claims.

The review read the complete two production templates, their exact baseline differences, the three frozen index/native oracle and resource templates, the crate contract and candidate plan, and the complete unchanged address-kernel/window/compactor dependencies. The strict-iterator owner and wrapper oracle had already been read completely in this task; their proposed difference and read-effect assertions were checked again. This scoped review does not upgrade the other owners in the sealed 139-owner inventory to fully reviewed.

## Exact production identities

All production paths below are relative to `hsoliwal/M3jdk21`. The baseline is `0d73745d7203123533dce8f8fb264ba2746f6d2d`.

| Owner | Baseline SHA-256 | Candidate SHA-256 |
| --- | --- | --- |
| `m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java` | `77ef7d6197ef936f5ae9f62c35accf2f5f3ff690dcc55ca3fd4d8f545a007545` | `7bf939ea374ce6c0b13c403f5e154c0f8d376f9cddb8e623cf00ef299183a77e` |
| `m3/collections/src/main/native/collections/m3_index_mask_window.c` | `a5cefed03d9db9429048a218945a8af71b5b7acc862e089f6cc8a451a172e212` | `d731f12916ee7f6e935624e7dd9f108c8de99bba02e1a81f6cc96c8cd21a5b84` |
| `m3/collections/src/main/java/com/m3/collections/M3FilteredLists.java` | `b2dc49d3a9ec43b6e6bd34cf5c6dac53057cf90f42cc9ed506d54822a760fc2d` | `032bffd456e67c7568b028489ceec85f24510469b3a0e00571ab36cd17ae3b75` |

### Stable opposite-order group reversal

`M3LongIndexProjection.reverseValueGroups` partitions exactly the current `[from,to)` index window into nonempty equal-value groups. It visits the groups in reverse order and copies each group forward with `System.arraycopy`. Equal values therefore retain their physical occurrence order, including a slice beginning or ending within a group. The address-kernel calls use absolute positions in the index array with offset zero; they do not add the slice offset twice.

The shortcut is reached only when the stored order is already sorted and the requested direction is opposite. Snapshot payloads are privately owned, filtering retains the order of selected indices, and slicing retains the order descriptor. These properties justify the shortcut across nested sort, slice, and filter compositions. Encounter-order stable merge sorting, same-direction descriptor creation and sharing, and the size-below-two path retain their original implementations. Reversing the whole index array would violate duplicate stability; this candidate deliberately reverses groups instead.

The existing `get`, `physicalIndex`, `toArray`, and Java/native filter routing are unchanged. In particular, native projection filtering continues to pass the current `indices`, `from`, and `size()` to the existing range compactor. Its older-ABI fallback still copies the index window and uses native compaction; this candidate does not reroute it to Java or replace the canonical payload/address kernel. The admitted group atom reuses the historical Synexia/M3 implementation identified in `index-candidates/HISTORY.tsv`, rather than restoring an older entire owner.

The source supports linear work and one result index array for opposite-order sorting, replacing the previous copied index array plus merge scratch array. It does not support a claim that retained result memory is halved: both implementations retain one final index array.

### Native rejected-prefix rebasing

The native change copies rows starting at `next + first_match` for `take - first_match` elements, then emits `values[i - first_match]` instead of `values[i]`. For every emitted position, `0 <= first_match <= i < take`; the candidate element is exactly the old source element `rows[next + i]`. `take` is bounded by the remaining valid window, so the adjusted region remains in bounds even at nonzero source offsets and in partial final chunks.

The rejected prefix cannot produce output. Validation, state-region reads, first-match detection, masks, contradiction/all-pass handling, output batching across chunks, limits, examined-row accounting, exclusive cursor behavior, and both JNI exports retain their prior code. Neither prefix rebasing nor the subtraction alters the logical scan position. The Java owner documents stable input arrays during a call; this review does not assert equivalence under concurrent array mutation.

The copied suffix can still contain rows after an early output limit. It is accurate to claim removal of rejected prefixes from matching chunks, not that every transferred row is selected. The two 128-element `jint` buffers and one 128-element `jbyte` buffer are unchanged: their explicit C array payload remains 1,152 bytes. That source calculation is not a measurement of the compiler's complete stack frame.

### Strict iterator exception repair

The original `M3FilteredLists.readOnlyFiltered` factory contract at lines 54–55 promises a live view that explicitly refuses all mutation methods. The original strict-owner comment at lines 128–130 extends that refusal to empty/no-op writes, direct mutators at lines 144–161 throw `UnsupportedOperationException`, and `listIterator` already uses an unmodifiable delegate. The inherited ordinary iterator's `IllegalStateException` before `next()` therefore contradicts the owner's declared unconditional refusal policy.

The candidate wraps `super.iterator()` and forwards `hasNext`, `next`, and `forEachRemaining` directly. Only `remove` changes to unconditional `UnsupportedOperationException`. This is explicitly an **exception defect repair**, including `IllegalStateException` to `UnsupportedOperationException` before traversal; it is not baseline equivalence for mutation attempts. It adds one small cursor wrapper and makes no iterator performance claim.

The separate read-equivalence oracle compares the candidate against an `AbstractList` facade that reproduces the old `size/get` path, including callback order, repeated demand, null-consumer failure, and the identity and retry behavior of a predicate-thrown exception. Directly routing to `listIterator` was refused because it would change those read effects. The existing repeated scans and missing backing-owner fail-fast detection remain pending findings, not repairs delivered by this guard.

## Frozen qualification boundaries

The index oracle compares physical indices against a stable boxed-JDK-sort model. It covers all 1,093 arrays of length zero through six over `{-1,0,1}`, two additional signed-extreme/tie examples, both sort directions, every valid slice of those sorted inputs, opposite-order round trips, selected nested slice/filter compositions, descriptor and detached-array properties, and deterministic native-filter subsets. This is exhaustive for the stated finite domain, not for all long values or all arbitrary compositions.

Window checks cover all 65,536 required/excluded byte-mask pairs, the complete unsigned-byte state domain, boundary lengths around 64/128 and 4,096, nonzero offsets, selected limit/budget combinations, repeated-row resumptions, output canaries, unchanged inputs, validation precedence, and raw JNI geometry refusals. The selected boundary combinations are not a full Cartesian product of all lengths, budgets, and limits. Missing-library and older-window-symbol modes exercise their explicit cases; they do not qualify every historical projection-compactor ABI variant. That projection fallback is retained unchanged in source.

The C JNI-table fixture validates region bounds, row/state/output region calls and element counts, output canaries, values, and scan cursors for 16 cases against a scalar model. Its fake `ExceptionCheck` always returns false, and it does not populate every JNI error callback. It therefore does not prove JNI exception-path behavior; those checks belong to the real Java/JNI oracle. It measures region transfer work, not native stack-frame bytes, native heap allocation, or JVM pinning behavior.

The resource probe reports sampled wall/thread/process CPU time, thread allocation, heap observations around requested GC, RSS/high-water memory, and direct/mapped buffer-pool observations. Instrumentation is included, process CPU may be coarse, requested GC does not guarantee an exact retained-heap measurement, and there is no fork-based statistical analysis or empty-harness subtraction. These measurements cannot establish a universal speedup, a zero-allocation native call, or a complete native-memory bound.

The exact frozen oracle hashes are recorded in `index-candidates/POSTIMAGES.tsv`; the wrapper oracle is sealed in `candidates/CANDIDATE_NOTES.md` and the original catalogue evidence tables. A shared observations digest is useful corroboration, but assertions, exact source pins, and execution receipts remain necessary: the digest is not a complete trace of every refusal and JNI action.

## Baseline evidence inspected

The parent-generated receipts in `work-m3-wrappers-20261018` were read, not rerun. `baseline-index-java.json` reports 2,990,400 checks; `baseline-index-native.json` reports 3,267,362 checks under `-Xcheck:jni`. Both report 65,536 mask pairs, 67,914 windows, and observations digest `e15c399b7be3d8c0fbd38bc6e2cd4c63b93bb0b95d1272bce9acdcc7bec5445b`. Missing-library and old-window-ABI modes report 13 and 5 checks, respectively. All four receipts exit zero. The baseline C-region fixture passes its 16 cases. Baseline Java sort and JNI mask resource receipts also exit zero; candidate resource comparisons were not yet available to this reviewer.

The wrapper baseline receipt reproduces the strict iterator exception defect and all five frozen pending witnesses; the separate eight read-effect checks and six comparator-failure checks pass. The linear-traversal witness observes 787,456 predicate calls for 1,024 source rows. Each pending mode stops at its first failed assertion, so its later assertions must not be described as executed. Newly observed upstream wrapper drift requires a separate reconciliation at the new exact revision; it does not retroactively change these original baseline findings.

Candidate acceptance should attach the parent workflow's exact integration-source identities, successful ordered qualification receipts, and bounded resource comparisons. This review supplies the source argument and disclosure boundaries, not a substitute for that execution evidence.
