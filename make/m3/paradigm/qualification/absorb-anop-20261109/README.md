<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# jdk.m3.paradigm: new module + first batch, AnOP (2026-11-09)

On 2026-11-09 the user decided:
- **"Paradigm modules"** are the next convergence family: synexia-iop, synexia-anop and synexia-cop.
- **"New jdk.m3.paradigm module"**: one new JDK module, with the internal packages
  `jdk.internal.m3.paradigm.{annotation,contract,context}` and nothing exported until public API is admitted.

## Module wiring

| Piece | Location |
|---|---|
| Module | `src/jdk.m3.paradigm/share/classes/module-info.java`: `requires jdk.m3.collections`, exports nothing |
| Build | `make/modules/jdk.m3.paradigm/Java.gmk` (as jdk.m3.collections: `-Xlint:all,-module -Werror`, doclint without the missing group) and a `PLATFORM_MODULES` entry in `make/conf/module-loader-map.conf` |
| Maven bridge | `m3/paradigm/pom.xml` and the bridge `module-info.java`; reactor entry in `make/m3/pom.xml` |
| jdk.m3.collections | Both its module-infos (src and the m3/collections bridge) gain `exports jdk.internal.m3.collections to jdk.m3.paradigm`, so absorbed code can use PlaceholderLogger and landed owners. The custody tip is resealed. |

## AnOP batch

- **Source.** com.synexia develop `8ab80ea57d9f`, synexia-anop (155 main classes).
- **Selection.** `select_multi` (`SLF4J_FACADE=1`) over the three paradigm modules gives 707 ELIGIBLE (SEL_paradigm_all.tsv).
  AnOP alone has 151 ELIGIBLE and 2 HUB.
- **Landed.** 137 classes in `jdk.internal.m3.paradigm.annotation`.
- **Held.** 16 classes, listed with reasons in HELD.tsv:
  - **3 annotation processors.** Each needs `java.compiler`, and two of them name donor annotation FQNs as strings
    (needs a LITERAL_FQN decision).
  - **2 HUBs.**
  - **The `@Lazy` annotation and its closure of 10 classes.** The sprawl gate refuses `Lazy` as SECOND_OWNER of
    `jdk.internal.m3.collections.Lazy` (the b29 lazy holder). That owner is a different kind of thing, but one owner
    per name is the rule, so the user decides.

## Recipes

- **RELOCATE_M3_OWNER v6.3.** A module-scoped call table is required here. Simple names repeat across the three
  paradigm modules (for example `Log` in AnOP and COP), and relocate_mat resolves by simple name.
- **SLF4J_TO_PLACEHOLDER_LOGGER v1.1.** It now also removes an unused `lombok.extern.slf4j.Slf4j` import, and adds
  the facade import only when the file uses the logger.
- **DOCLINT_REPAIR v1.1.** Inside `<pre>` examples, a line-leading `@` becomes `&#64;` and a generic `<` becomes
  `&lt;`. Comment-only.
- **jdk_naming.** It rewrote 8 files, comments only (`@since synexia-anop`, `@see com.synexia...`). Dangling
  `@see` lines were then dropped by DOCLINT_REPAIR.
- **Banner.** `/* ****`.

## Proof (PROOF.tsv)

The donor side compiles with lombok 1.18.46 and slf4j-api 2.0.17. The normalization is `norm_anop.sed`: the slf4j
map plus stripped constant-pool indices.
- **javap -c:** 0 of 181 class files differ (148 classes plus their nested classes, before the hold).
- **Mutant:** caught.
- **javap -v:** 3 classes show only the declared facade pool shape.

## Gates

- **Compile:** both modules build together under -Xlint:all,-module -Werror.
- **Doclint:** -Xdoclint:all,-missing reports 0 errors on the 137 files.
- **jdk_naming:** fixed point.
- **Sprawl gate:** new=148, refused=1. The refused class is held (see above).
- **Default suite:** the werror reactor (m3/arrays, m3/collections, m3/paradigm) passes: 2841 tests, 0 failures, 0 errors, 151 skipped; m3/paradigm has no tests yet.
