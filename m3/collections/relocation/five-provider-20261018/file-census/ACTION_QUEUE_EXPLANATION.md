<!-- SPDX-License-Identifier: Apache-2.0 -->
# Serial M3 file-by-file action queue

The file `ACTION_QUEUE_PER_FILE.tsv` is a mechanically sorted **2001-source-file task queue** derived from the 3214 pinned Git blob rows; the other **1213 rows** are retained as donor test oracles, not production candidates. No source candidate is marked implemented.

| Stage | Count | Contract |
|---|---:|---|
| M3 atom candidate (arrays/offsets/indexes/masks, filtered/sorted) | 1146 | Full source read + donor behavioral envelope + Maven/OpenRewrite recipe and execution + tests/JNI if applicable |
| High-risk state/concurrency/immutability | 102 | JMM/serialized-view/generation contract proof first |
| Eclipse public API | 253 | Preserve released type surface and EPL/EDL rights |
| JDK java.base protected | 223 | Isolated copied source only; never rename real java.base |
| Spring upstream read-only | 29 | Require a writable fork for source handoff; can execute in an ephemeral runner only |
| Eclipse code generation | 9 | Regenerate and compare generated API/references |
| Non-collection typed exclusion | 239 | Do not rewrite |
| Donor test oracles in inventory, not queue | 1213 | Retain and run against each candidate |

The existing named OpenRewrite ChangePackage recipes are reused. This queue is **not** a generated source patch, proof of 2001 behavior-preserving adaptations, or full coverage of other un-inventoried modules/GWT variants.

## Consolidation targets

- Dynamic array/index/offset: `M3ArrayList`, `M3LinkedHashMap`, `M3LongIndexProjection`, `M3IndexPathMasks`.
- Object/slot state: `M3ObjectStateSlots28`, `M3BitLane28`, `M3StateMaskArrays`.
- Cached filtered/sorted snapshots: `M3LongBitmapFilteredSnapshot`, `M3LongBitmapFilterPlan`, `M3TreeMap`, `M3LongSortedSnapshot`.
- Relationship/bag/table: `M3IndexedMultiValueMap`, `M3CountedBag`, `M3IndexedTable`.
- Optional JNI: **reuse** `M3IndexMaskCompactor` C17, no callback/object transfer or duplicated ABI.

A candidate owner name is a *lookup hint*, not a proven replacement for a provider class. Class-level ownership, repeated occurrences, callback ordering, serialization, lock semantics and provenance are sealed until differential proof.

**M3Scale**: parallelize recognition at the safe frontier, serialize proof-gated canonical advancement. The action queue is proposal-only. No source file in any provider fork was changed by this ledger.
