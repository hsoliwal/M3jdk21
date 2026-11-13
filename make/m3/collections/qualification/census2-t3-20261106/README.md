<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census pass-2 group T3: filtered cursor readiness tests (P2-098) - HOLD

Census pass 2 leaf P2-098 (crate `m3jdk21-20261019`, com.synexia develop `d5ad5090d87e`), tests part only:
`M3FilteredCursorReadinessSelfTest` + its JUnit wrapper `M3FilteredCursorReadinessTest`. Lane owner com-synexia-45
resolved both as NEW and clean through `make/m3/qualification/cpu-owner-extensions-20261105/crate_chain2.py`
(HASH_PINNED_CHAIN v2); its output equals the donor bytes with the package line mapped, as do the uncompiled retained
variants of PR #1463 under `m3/qualification/master-landing-20261028`.

| row | verdict | evidence |
|---|---|---|
| `M3FilteredCursorReadinessSelfTest`, `M3FilteredCursorReadinessTest` | HOLD_OWNER_EDIT (not landed) | relocated by RELOCATE_CENSUS v5 (donor bytes + anchor) the module suite goes 1979 / F0 -> 1980 / F1: the self test fails on master's owners in all four modes (`OWNER_VERDICTS.tsv`, 74 distinct failing checks) |
| `M3SdkSortedContractRecipe` | not T3 | NO_START (45: master's copy evolved), owner review |
| `M3GvasLookupIndexContractTest` | not T3 | m3/core, NO_START (45), owner review |
| `M3TokenMatcherExecutionTest` | not T3 (HOLD_SCOPE) | java.base jtreg test (`test/jdk/jdk/internal/mindex`), outside m3/collections |

Owner behaviour the held test expects and master lacks (no owner edited here):
1. `M3FilteredViews.filterEntries` / `M3MutableFilteredViews.filterEntries` cursors check the owner revision: an
   exhausted `hasNext`, a prepared `next` and a mutable `remove` after an owner put / clear / slot reuse throw
   `ConcurrentModificationException`;
2. re-entrant owner mutation from a predicate (before accepting or following a slot), from a bounded comparator
   (`nextSlot`) and before the first slot (epoch captured at construction) is detected instead of returning a value or
   `NoSuchElementException`;
3. the mutable values view evaluates the predicate once per entry (master evaluates some entries twice) and a
   nonstructural replacement keeps the prepared selection;
4. `M3TreeMap.verifyInvariants` stays valid after a bounded comparator that clears the owner through the mutable view
   (master: `AssertionError: ordering`).

Files: `RELOCATION_MAP.tsv` (decision rows with the donor paths for the later relocation), `owner_probe.sh` (as-is
first failure + per-mode recording probe, compared with `OWNER_VERDICTS.tsv`; it exits 1 once an owner change alters
the verdicts), `OWNER_VERDICTS.tsv`, `GATES.tsv`, `CENSUS_RERUN.tsv`, `RECIPE_MEMORY.tsv`.

Re-run: `JAVA_HOME=<JDK 21> M3_ARRAYS_JAR=<jar> bash make/m3/collections/qualification/census2-t3-20261106/owner_probe.sh <com.synexia clone> d5ad5090d87ef8477a56317e60ac0d29c65029f7 <out>` after `mvn package` of m3/collections.
