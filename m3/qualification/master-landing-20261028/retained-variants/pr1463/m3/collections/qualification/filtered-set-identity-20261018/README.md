<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Filtered Set identity and stored visibility

The existing mutable `M3CollectionViews.FilteredSet` used an equals scan even when its
backing owner used identity membership. An equal-but-distinct probe could appear present
and remove a different stored object. With two equals-equal references, a hidden exact
reference could borrow its visible sibling. The existing read-only `M3FilteredViews`
already establishes the owner-first, exact-reference-priority pattern.

This pass corrects only `FilteredSet.contains/remove` in the existing owner. It checks
backing membership before stored visibility, gives an exact reference priority, and
retains the existing iterator/direct-removal fallback. The ordinary equality path,
sorted/navigable comparator overrides, nulls, nested predicate composition and public
signatures remain covered by executable tests. No new per-element object, payload owner,
retained index, native callback or Synexia runtime dependency is added.

## Actual execution

| Gate | Fresh result |
| --- | --- |
| Pinned target inventory | All 1,742 collection files recovered with exact Git blobs at `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` |
| Baseline owning-module Maven suite | 232 tests, zero failures/errors/skips |
| Frozen identity regression against baseline | Fails on foreign identity alias, as expected |
| Candidate contract and independent review | 46 supplied checks plus 18 independent checks pass |
| Actual canonical Maven/OpenRewrite SDK | Five tests pass; four existing engine sources unchanged |
| Existing input-sealing / SDK-receiver contracts | Eight unit tests and nine receiver cases pass |
| Actual SDK Results | One owner correction, two absent-before Java tests |
| Serial receiver | Three changes; second application zero changes |
| Received complete owning-module Maven suite | 233 tests, zero failures/errors/skips |
| JNI build and ABI | Ten unchanged C sources, nine C17/UBSan libraries, all 17 generated declarations match exports |
| Received-class Java/JNI regression processes | 27 existing processes pass with checked JNI; new identity contract also passes |
| Public/protected target owner API | No public/protected signature changes |

The 27 processes include explicitly labelled Java-only regression oracles. Native-required
mask/path/window, gather, signal, histogram, span, residency and SDK oracles actually load
and exercise their native paths. The first optional path-window run is Java; the supplemental
run explicitly requires native. Exact counts overlap across oracles and are not summed.
The native code itself is unchanged; this pass does not claim a new C implementation.

`evidence/application.json`, `module-suite.json`, `SDK_INPUTS.tsv`, `SDK_RESULTS.tsv`,
SDK/receiver logs, red/green counterexample, replayable independent review, native source
pins/ABI and received-native command receipt bind these results. `evidence/native-baseline`
contains the prior complete compiler/native logs; `evidence/received-native` proves execution
with received owning-module classes. Archived native helper paths describe the recorded
workspace layout; adapt paths using the exact commands and source pins when replaying.
The canonical task's existing Python source/receiver owners remain unchanged.

## Recipe ownership and replay

Reusable recipe authority is `hsoliwal/com.synexia`, under the existing
`synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017`,
new dataset `filtered-set-identity-20261018`. This packet is receiver evidence, not another
recipe engine. The effective four canonical sources are pinned at Synexia
`140c263a90f9803b9c46103c63355a286e0515f0` and in `ENGINE_EFFECTIVE.tsv`.
The published Synexia commit identifies the exact dataset and target-code templates.

With the qualified canonical checkout and an offline cache containing its declared
artifacts, run the existing crate's Maven test and receiver contracts, then receive its
actual SDK Results into an isolated checkout of the target baseline:

```sh
mvn -o -B -ntp -f "$SYNEXIA/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/pom.xml" \
  -Dmaven.repo.local="$RECIPE_CACHE" \
  -Drecipe.owner.root="$SYNEXIA/synexia-openrewrite-recipes" \
  -Dm3.dataset=filtered-set-identity-20261018 test
```

Use that crate's unchanged `contract/test_prepare.py`, `contract/verify_receiver.py` and
`receive.py --check`, apply and replay commands. Run with bytecode-cache generation disabled
for unchanged qualified input inventories. The local command logs record JDK21.0.12.1,
Maven3.9.12 and the actual caches. Maven `test` plus explicit contract scripts ran; the
`-Pcontract verify` packaging lifecycle and hosted CI did not run for this packet.
An empty offline cache is not enough to reproduce the declared dependency closure.

## Catalogue, ownership and limits

`donor-catalogue.*` and `category-*.tsv` record the existing-owner comparison and ordered
LeetCode, HackerRank and GeeksforGeeks discovery trail. Only the existing stored-representative
pattern is selected in this pass. Fastutil, Guava, Roaring and CRoaring are pinned, licensed
reference candidates for later array/index/mask work; no donor code or challenge editorial
was copied. Comparator equivalence, equals and identity remain distinct contracts.

The live wrapper still computes visibility from its source and predicate. Its lookup remains
linear in a generic backing owner, plus that owner's membership cost. No universal speedup,
allocation, retained-heap or process-memory claim follows from these correctness checks.

This is the separately licensed `m3/collections` product surface. It is not blanket replacement
of public JDK collections, java.base/HotSpot promotion, full reactor/JCK/jtreg or cross-platform
JNI qualification. String -> arrays -> collections receiving status remains unchanged.
Full repository canonical admission is not claimed. The program discussed across the chat is
not complete. Pending immutable snapshot corrections already exist in M3JDK21 PR #1260;
the distinct local wrapper-current work was inventoried but is not duplicated here.

`name-mapping.json` records the reviewed existing-owner/no-renaming disposition. Original
Apache-2.0 notices and source pins are retained. Target source templates keep their exact
M3JDK21 attribution; per-file Synexia provenance records their role without rewriting them.
The new catalogue and test artifacts are first-party Synexia contributions. Metadata/API
provenance checks are bounded checks, not hosted CI or repository-wide completion evidence.
