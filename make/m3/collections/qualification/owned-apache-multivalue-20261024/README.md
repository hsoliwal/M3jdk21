<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Apache multivalue incremental counters (D1), 2026-10-24

`M3AbstractMultiValuedMap` keeps two transient ints: `totalValues` (sum of group sizes) and `nonEmptyGroups` (the
logical key count). Every owned write measures its group's size before and after and adds the change, so `size()`,
`isEmpty()`, `keySet().size()` and `keys().uniqueSet().size()` are O(1) instead of a walk over every group. An epoch
guard voids the delta when a write is re-entered from a callback, a read happens during a write, or the raw directory
leaves owned code (`getMap()`, `setMap()`); the next query then recounts once. Only the owned leaves `M3ListValuedMap`
and `M3SetValuedMap` keep exact counters between queries; a foreign subclass always recounts, as before. Sub-lists
are wrapped (`CountedListView`) so writes through them are counted. The serialized form is unchanged.

- `FOSS_REUSE_DECISION.tsv`: written before code (Guava `AbstractMapBasedMultimap.totalSize` as idea, LeetCode and
  HackerRank ids as idea references only).
- `SDK_SOURCE_BINDING_SUCCESSOR_V4.json` + `verify_source_binding_successor_v4.py`: successor of the sealed v3 receipt,
  which stays byte-exact. Ten successor rows: the three D1 bodies, and six array/native owners plus the SDK that
  master changed after the v3 seal (re-pinned as found, decision B2; v3 had been failing on master since then).
  The added test, bench and the v2-to-v3 recipe manifest are pinned too.
- `probe_source_binding_successor_v4.py`: 55 refusal cases (byte drift of every pinned input, SDK BOM/CRLF/optimized
  drift, a v2 revert of each D1 body) and 2 positive cases; local run PASS.
- Recipe: `../../recipe/owned-apache-multivalue-20261024` (hash-pinned v2 -> v3, `com.synexia.m3.ApacheMultiValueV2ToV320261024`).
- Catalogue: `../../algorithm-catalogue/challenge-patterns-20261024/MVM_INCREMENTAL_COUNTERS.tsv`.

Local evidence (Windows, JDK 21.0.9): `M3MultiValuedMapCounterDifferentialTest` 100,000 operations, 694,996 checks, 96
round trips, PASS; three injected counter faults (no logical-key delta, no held-group delta, no exposure on
`getMap()`) each fail it. `M3MultiValuedMapSelfTest` PASS (default JIT and `-Xint`); `ApacheM3MultiValueCandidateProbe`
output identical to master except the class name of `subList` results (`CountedListView`). JMH 1.37
(`-f 1 -wi 5 -i 5 -w 1s -r 1s -prof gc`), master -> D1, ns/op at 1e5 keys x 1 value: `size()` 507,410 -> 1.23,
`isEmpty()` 457,502 -> 1.29, `keySet().size()` 419,452 -> 1.52, `writeMix` (90% put / 10% removeMapping + size())
1,673,300 -> 103; at 10 keys `size()` 29.9 -> 1.2. `size()`, `isEmpty()` and `keySet().size()` allocate 0 B/op.

Not claimed: the received Commons `ArrayListValuedHashMapTest`/`HashSetValuedHashMapTest` were not run against the M3
classes; `M3CollectionSdkSelfTest` fails the same way on master classes ("selected optional adapters must be
enumerated exactly"), independent of this change.
