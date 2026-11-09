<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# recipe null-flow-20261107: the Kotlin-inspired null-flow atoms relocated into jdk.internal.m3.ast

Census pass-1 leaf **L048** (`C:/m3-superset/conv/record/census/LEAVES_PROPOSED.tsv`; ledger row 110, lane
com-synexia-0e; qualification record `m3/qualification/census1-remainder-20261107a/`): the three method-local null-flow
atoms of `hsoliwal/com.synexia` crate `m3-kotlin-null-flow-20261031` and their oracles become part of `m3/ast` by recipe,
in the internal package `jdk.internal.m3.ast`.

## What was relocated

| Donor (develop `56cbdc631259`) | M3JDK (`m3/ast/`) | Role |
|---|---|---|
| `.../hash-pinned-java/m3-kotlin-null-flow-20261031/M3NullFlowFacts.java` (sha256 `13d7c4e827c9`) | `src/main/java/jdk/internal/m3/ast/M3NullFlowFacts.java` | packed two-bit nullness facts (UNKNOWN/NULL/NONNULL, 32 per `long`), frame reachability, `joinFrom` (lane clear in O(ceil(V/32))), `mergeFrom` (monotone, change detection), `assume` (a contradiction makes the frame unreachable) |
| `.../M3NullFlowVariableIds.java` (`fcae3588f9be`) | `src/main/java/jdk/internal/m3/ast/M3NullFlowVariableIds.java` | identity-hashed open-address table from attributed compiler symbols to method-local ordinal IDs; bound to one source SHA-256 and method identity; frozen before facts |
| `.../M3NullFlowWorklist.java` (`cc1442b34e32`) | `src/main/java/jdk/internal/m3/ast/M3NullFlowWorklist.java` | primitive CSR adjacency and ring-queue worklist over supplied edges and six transfers (IDENTITY, SET_NULL, SET_NONNULL, FORGET, REQUIRE_NULL, REQUIRE_NONNULL); least reachable fixed point, one frame per node, the entry frame never modified |
| crate `src/test/java/.../M3NullFlow{Facts,VariableIds,Worklist}Probe.java` | `src/test/java/jdk/internal/m3/ast/` | the donor oracles (mains + `prove()`): seeded scalar lattice (16 sizes x 180 trials), object-oriented scalar CFG fixed point over 6 x 8 x 110 random cyclic graphs, `IdentityHashMap` oracle plus a real `JavacTask` attribution fixture |
| crate `.../M3NullFlow{Facts,VariableIds,Worklist}Test.java` | `src/test/java/jdk/internal/m3/ast/` | the donor JUnit entries (`prove() > 100,000 / 80,000 / 100,000`) |
| (M3JDK-authored) | `src/test/java/jdk/internal/m3/ast/M3NullFlowReferenceTableTest.java` | the hand-derived reference model: join table at word boundaries, reachability/assumption/merge tables, a four-exit loop solved by hand, identity not equality |

The three sha256 figures are the crate manifest's `ABSENT -> sha256` rows (the OpenRewrite recipe
`M3KotlinNullFlow20261031` stays in com.synexia: OMIT_RECIPE); the crate's own pom compiles exactly these resources, so no
second source copy exists in the donor either.

## Decisions (`DECISION.tsv`, `FOSS_REUSE_DECISION.tsv`)

- **Owner**: the closest owner by name, `m3/compiler/nullness-facts` `M3JavaNullnessFlow`, is a javac-plugin fact
  extractor over attributed trees; the atoms compute (lattice, symbol table, worklist) and a compiler consumer feeds them.
  Same area, other contract: **NEC**, both kept, nothing absorbed into the plugin, no existing file edited. The donor's
  `NEXT_ATOMS.tsv` rows 4-10 (javac owner integration, diagnostics, JSpecify projection, JNI candidate) are not started
  in the donor and are later leaves.
- **Package**: `jdk.internal.m3.ast`, the JDK naming standard's internal form (`jdk.internal.m3.<area>`) for
  package-private candidate code; `m3/ast` has no module-info, so the name declares the status (as `jdk.internal.m3.optane`
  does in `m3/optane`). The census spelling `NEW:jdk.m3.ast.internal` is the mechanical rename of `com.m3.ast.internal`
  and would be the exported form.

## Recipe

`python -I m3/ast/recipe/null-flow-20261107/relocate.py --donor-git <clone> --donor-rev 56cbdc631259 [--check]`
(`RELOCATE_CENSUS1_REMAINDER` v4: pmem-gram-index v3 plus DROP_METHOD rows and a module-derived recipe path; the same
text as `m3/optane/recipe/pmem-accelerator-20261107/relocate.py`). Map: 9 FILE rows, 1 PACKAGE, 9 identity TYPE rows,
3 EXTERNAL (`com.sun.source.tree`, `com.sun.source.util`, `org.junit.jupiter.api`), SKIP rows for the crate records and
the OpenRewrite recipe. `PROVENANCE.tsv`: 9 rows `body_match=TRUE` (every relocated line is a donor line; only the
header, the package line and one `//FLLDCIM: RELOCATE` anchor differ).

## Gates (2026-11-07, `GATES.tsv`)

| Gate | Result |
|---|---|
| 0 recipe fixed point | `files=9 changed=0 provenance=UNCHANGED` after the run and after `jdk_naming apply` |
| 1 `mvn -Dmaven.test.skip=true -f m3/ast/pom.xml package` | BUILD SUCCESS, 0 warnings (master 0); 30 main classes (master 27) |
| 1b `mvn -f m3/ast/pom.xml package` | red on master and here, identically: the pre-existing `M3SyntaxGrowthAgent` testCompile (9 errors, `--add-exports` refused with `--release`; #2573 recorded it); failing set = baseline |
| 2 tests (`tests.sh`: the #2573 javac + JUnit console method) | 19 found, 19 successful (master 10/10) |
| 2b untouched class files | main 27/27, test 45/45 byte-identical with the master build |
| 3 differential (`differential.sh`) | donor and relocated probes byte-identical in JIT and `-Xint`: `scalar_contract_assertions=1525513`, `1130923`, `attributed_contract_assertions=86468` (the donor README's figures) |
| 4 `javap -p` / `-p -c` parity (`javap_parity.sh`) | 13/13 class files identical after the package normalization |
| 5 mutants (`mutants.py`) | 8/8 killed by the donor probes (join, assume, slot read, merge change, FORGET, entry alias, equality-vs-identity, intern after freeze) |
| 6 reference table | `M3NullFlowReferenceTableTest` 6/6 |
| 7 naming | `jdk_naming.py apply --only m3/ast/`: 0 moves, 0 rewrites; fixed point; plan 0/0/0 |

## Files

| File | Purpose |
|---|---|
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the recipe (v4), its map and its pins |
| `tests.sh`, `differential.sh`, `javap_parity.sh`, `mutants.py` | gates 2, 3, 4, 5 |
| `DECISION.tsv`, `FOSS_REUSE_DECISION.tsv`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | owner/package decisions, donor-first table, the gate record, the lessons |
