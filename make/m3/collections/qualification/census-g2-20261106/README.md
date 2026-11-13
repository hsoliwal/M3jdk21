<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census group G2: copy-on-write set and long list (L082 + L088)

M3JDK convergence census leaves L082 (crate `m3jdk21-cow-set-20261029`) and L088 (crate
`m3jdk21-cow-long-list-20261029`), com.synexia develop `ca1aafc4d94f`. Both owners were five-library scaffolds whose
every atom threw `M3NotYetReceived`; the crates ship the exact preimage and the received postimage.

| leaf | verdict | owner / classes | oracle |
|---|---|---|---|
| L082 | APPLIED (owner edit) | `M3CowSet`: one volatile `Object[]` epoch, synchronized writers, snapshot iterators, `CopyOnWriteArraySet` equality order | before/after: every differing line RECEIVED; donor = relocated dumps IDENTICAL; JDK model 0 mismatches; `M3CowSetDifferential` PASS checks=348417 |
| L082 | PORTED (tests) | `M3CowSetContractTest`, `M3CowSetDifferential` + `M3CowSetDifferentialTest` (WRAP template) | 3 + 1 tests pass |
| L088 | APPLIED (owner edit) | `M3CowLongList`: one volatile `long[]` epoch, `CopyOnWriteArrayList` index/bounds contract, snapshot `PrimitiveIterator.OfLong` | same differential run (both owners) |
| L088 | PORTED (tests) | `M3CowLongListContractTest` | 4 tests pass |

Recipes: `apply.py` (CENSUS_OWNER_EDITS v1, derived from `m3/qualification/progress-monitor-unify-20261106/unify.py` v1;
rules in `EDIT_RULES.tsv` (a lineage .tsv: jdk_naming never renames its donor paths), pins in `EDITS.tsv`): DONOR rules replace an owner with T(donor postimage) only after
T(donor preimage) equals the pinned owner blob byte for byte (T = package mapping + the KEBAB rename master applied).
`relocate.py` (RELOCATE_CENSUS v4) relocates the tests (`RELOCATION_MAP.tsv`, `PROVENANCE.tsv`). Proof: `GATES.tsv`,
`differential.sh` + `differential/M3CowDump.java.tmpl`, `CENSUS_RERUN.tsv`, `RECIPE_MEMORY.tsv`.

Re-run: `python -I m3/collections/qualification/census-g2-20261106/apply.py --donor-git <com.synexia clone> --donor-rev ca1aafc4d94f9dd1ee288226dcca070cc60991c9 [--check]`, then `relocate.py` with the same arguments.
