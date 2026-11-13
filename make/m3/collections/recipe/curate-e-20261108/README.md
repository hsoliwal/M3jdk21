# CURATE_E: no naming or packaging sprawl in m3/collections

The user made these decisions on 2026-11-08, after "you can't have sprawl in the JDK":

1. **Drop the `M3` prefix everywhere, internal and public.** The package already says m3. The prefix stays only where the
   bare name is a java.* simple name (for example `M3HashMap`, `M3ArrayList`, `M3TreeMap`).
2. **Split the internal package into family subpackages.** The flat `jdk.internal.m3.collections` (676 classes) becomes
   non-exported families, and access flags do not change.
3. **One owner per concept.** Duplicate concepts merge into one owner, and nothing new lands until its concept has a
   single owner.

## Census (sprawl_census.py, master 0017450623ff)

| Metric | Count |
|---|---|
| m3/collections types | 974 |
| M3-prefixed | 829 |
| flat internal | 676 |
| package-in-name | 4: `M3Selection/ShapesCollectionRequest`, `M3Selection/ShapesCollectionProvider`, all from phase D |
| variant families with 4 or more types | 12, covering 97 types (LongSet 25, LongLongMap 15, IntSet 8, …) |

## Passes

| Pass | What | Tool | Proof |
|---|---|---|---|
| E1 | Internal types move to `jdk.internal.m3.collections.<family>` and drop `M3`. Tests follow their subject. | `curate_e_classify.py` → `gen_map_e1.py` → `curate_rename_e.py` (CURATE_CORE=jdk.internal.m3.collections, TESTS_AS_OWNERS=1) in the `curate_e_loop.sh` compiler fixed point | javap -v equal modulo names; all tests unchanged; -Werror gate; jdk_naming at a fixed point |
| E2 | Public `jdk.m3.collections` types and their tests drop `M3`, except names that clash with java.* | `RENAME_MAP_E2.tsv`, same recipe with CURATE_CORE=jdk.m3.collections | as E1 |
| E3 | One owner per concept, one leaf per concept. It starts with the three collection-selection subsystems (algorithms.collections request builder; selection; shapes), then the 21 hub differences and the variant families. | API union into the chosen owner, callers repointed, tests on both sides | tests and gate; bytecode changes are declared |
| E4 | Sprawl gate: a new owner is refused when it is M3-prefixed, has a package in its name, lands in the internal root, or adds a variant to an existing concept | `sprawl_census.py` in the batch gate | — |

## Families (E1)

- **Origin families:** a lineage anchor that names a coherent Synexia subsystem picks the family: `mat`
  (matcollections/matvalue), `shape` (common.collections.shapes), `select` (common.collections.selection).
- **B+C name-stem families:** `concurrent`, `bits`, `graph`, `range`, `sketch`, `text`, `packed`.
- **No container families:** the drafted `function`, `nativeaccess`, `lane`, `view`, `frozen`, `map`, `set`, `seq` and `index` packages were retired as invented names.
- **Root:** whatever the compiler fixed point keeps for package-private coupling, plus types with no family.

Absorption from com.synexia (phase D) is frozen until E1, E2 and E4 have landed.

## E1 result (master 2f9fa20d5fa8)

- **Families:** the 7 public families plus Synexia's own `mat`, `shape` and `select`. The 20-family draft was dropped
  because invented package names are sprawl too.
- **Layout:** root 315, mat 125, packed 95, shape 67, range 18, bits 17, graph 12, concurrent 9, sketch 8, select 6, text 4.
- **Placement:** `curate_e_fastloop.sh` + `curate_e_place.py` place package-private coupling components as units
  (EDGES.tsv), so no subsystem is split. Only 18 types sit outside their intended family, each to keep a component together.
- **Bytecode:** 1597 of 1600 class files are identical modulo names (BYTECODE.tsv). The 3 others differ only in
  type-name strings, under the names-as-data rule.
- **Tests:** 2419/0/0/135 on both base and curated.
- **Build and naming:** the Maven `-Werror` test-compile is clean, and jdk_naming shows no new findings.
- **Reachability:** reach_census.py shows 656 of the 676 internal types are reachable from no public type. The user chose
  to keep them (repackage only).
