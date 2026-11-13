<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered views owner leaf of census pass-2 P2-098 (un-holds the census2-t3 tests)

`qualification/census2-t3-20261106` (#2619) held the P2-098 tests `M3FilteredCursorReadinessSelfTest` and
`M3FilteredCursorReadinessTest` (HOLD_OWNER_EDIT, 74 failing checks in `OWNER_VERDICTS.tsv`) because the owners lacked
four behaviours. This leaf decides each behaviour (`DECISION.tsv`), applies the adopted ones by recipe
(`apply.py`, FILTERED_VIEWS_OWNER v1), and lands the two tests by recipe (`relocate.py`, RELOCATE_CENSUS v6, rows
in `RELOCATION_MAP.tsv`, pins in `PROVENANCE.tsv`).

| behaviour | decision | owner change |
|---|---|---|
| B1 cursor checks the owner revision (exhausted `hasNext`, prepared `next`, mutable `remove`) | ADOPT (never on master; donor crate `m3jdk21-20261019` = retained variants pr1463) | read-only: an owner-epoch decorator over the existing cursors when the source is an `M3TreeMap`/`M3TreeSet` (no callback added); mutable: physical-slot cursors for those owners |
| B2 predicate / bounded-comparator / before-first-slot mutation detected | ADOPT | epoch read before the cursor opens, checked after every predicate call and slot step; the mutable own `remove` erases its returned slot (no comparator lookup inside it) |
| B3 mutable values view calls the predicate once per entry; own `remove` keeps the prepared successor | ADOPT (M3 owners, as the donor) | `MapView.values()` over the same slot cursor; other owners keep `AbstractMap.values()` |
| B4a `M3TreeMap` "ordering" in the P2-098 row | NOT_A_BUG | none: `verifyInvariants` ran the test's still-armed comparator (`FilteredViewsTreeMapRepro` R1/R3); passes through B2 |
| B4b key removal whose comparator clears the map or erases the located mapping | FIX (found by the differential, base and head alike) | `M3TreeMap.eraseAt` refuses a released slot (`ConcurrentModificationException`) instead of damaging links/free list (R4) |

Also decided in `DECISION.tsv`: `M3WrapperConvergenceContract` readonly-linear / readonly-failfast (WRAP-005/006,
owner `M3FilteredLists`) KEEP_OWNER (452dd02e9e2, catalogue VIEW-002/VIEW-003 refusals; unchanged by this leaf, not
wired); merge-loss case 04 (4b24824c3aa0 / dab450b9192), leaves 22 (348d34e818d6) and 24 (4692dce84442) SUPERSEDED;
mergeloss2 clean leaves of the two views: the B1-B3 ones adopted as behaviour (not bytes), the others never on
master (RETAIN-merge PR sides, #2547 twins) and kept, so no restore PR follows.

Files: `apply.py` + `EDITS.tsv` (owner recipe, pinned preimage blobs), `relocate.py` + `RELOCATION_MAP.tsv` +
`PROVENANCE.tsv` (tests), `oracle/jdk/m3/collections/FilteredViewsTreeMapRepro.java` (B4 repro),
`oracle/jdk/m3/collections/FilteredViewsOwnerDifferential.java` + `classify.py` + `differential.sh` (seeded
before/after differential, every differing line classified; `DIFFERENTIAL_SUMMARY.tsv`), `mutants.py` +
`MUTANTS.tsv` (20/20 killed), `GATES.tsv`, `RECIPE_MEMORY.tsv`.

Re-run (after `mvn package` of m3/arrays and m3/collections with the committed poms):
`python -I apply.py --check`; `python -I relocate.py --donor-git <com.synexia> --donor-rev d5ad5090d87ef8477a56317e60ac0d29c65029f7 --check`;
`JAVA_HOME=<JDK 21> bash differential.sh <base target/classes> <head target/classes> <m3-arrays jar> <out>`;
`python -I mutants.py --java-home <JDK 21> --classes <head classes> --test-classes <head test-classes> --arrays-jar <jar> --oracle-classes <out>/head --head-diff <out>/head.out --head-repro <out>/repro-head.out --work <dir>`.
