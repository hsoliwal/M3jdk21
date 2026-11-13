<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census group G6: long map merge-parent loss (L081)

Census leaf L081, crate `m3-longmap-merge-loss-20261030` (com.synexia develop `ca1aafc4d94f`): repair of the #2127
`addTo(long,long)` merge-parent loss in `M3LongLongHashMap`.

| row | verdict | evidence |
|---|---|---|
| `M3LongLongHashMap` (composed manifest postimage) | ALREADY, no edit | census APPLIED_SUPERSET 8/8; the relocated crate oracles reproduce the crate figures on the unedited owner |
| `M3LongLongHashMap` (`history-pr2127/` snapshot, census PARTIAL 193/206) | NOT_A_POSTIMAGE | the historical #2127 donor blob the crate composed from (it lacks the staged rehash the owner keeps) |
| `M3LongLongHashMapAddToTest` | ALREADY | APPLIED |
| `M3LongMapDeliveryWitness` (census NO_OWNER, 178 lines) | DECLINED | a git-subprocess acceptance witness of the donor receipt (pinned #2127 merge parents, pre-rename paths); receipt custody tooling, not a `jdk.m3.collections` owner; its three atoms are checked here directly |
| `M3LongMapRecoverySmoke`, `M3LongLongMapSharedTrace` (crate `tests/`) | PORTED (tests) + 2 WRAP wrappers | donor = relocated outputs IDENTICAL: smoke checks=240012; trace checks=302311 sha256 c13579351e14... (the crate's figure for baseline and candidate) |
| `M3LongMapAddToTiming` | SKIP | diagnostic timing, not an oracle |

Recipe `relocate.py` (RELOCATE_CENSUS v4), `RELOCATION_MAP.tsv`, `PROVENANCE.tsv`; proof `differential.sh`, `GATES.tsv`,
`CENSUS_RERUN.tsv`, `RECIPE_MEMORY.tsv`.
