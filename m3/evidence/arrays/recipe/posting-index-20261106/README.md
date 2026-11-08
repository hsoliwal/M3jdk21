<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# posting-index-20261106: relocate the galloping posting index and its JNI kernel into `jdk.m3.arrays.M3IntPostingIndex` (recipe RELOCATE_POSTING_INDEX v2)

Second leaf of the collections/arrays convergence plan (`C:/m3-superset/conv/plans/collections-arrays.tsv`, row 1
`M3IntPostingIndex`, decision ABSORB, the largest in-lane gap): M3JDK master had no posting owner (`git grep -i posting`
over `m3/collections/src` and `m3/arrays/src` = 0 files; the galloping bounds of `M3LongCoordinateIndex` are a
different owner and are left alone).

## What it is

`M3IntPostingIndex` is an immutable CSR inverted index: sorted distinct `int` keys -> offsets -> sorted row ordinals,
three `int[]` lanes and no object per key. `ofKeys(keyByRow)` / `ofPairs(keys, rows, rowUniverse)` canonicalize
duplicate `(key,row)` pairs; queries are `rows()`, `keyCount()`, `postingCount()`, `primitivePayloadBytes()`,
`keyAt`, `keys()`, `postingSize(key)`, `rows(key)` (an `M3IntPostingList` view), `contains(key,row)`,
`equalSelection(key)`, `any(keys...)` (union), `all(keys...)` (shortest-first intersection: in-place two-pointer
merge for dense peers, cursor-local galloping lower bound for peers at least 16x longer), `countAny` / `countAll`
(cardinalities without a result lane: primitive heap, monotone cursors) and `countAnyUpTo(limit, ...)` /
`countAllUpTo(limit, ...)` (stop at the limit-th distinct match). Every query takes a `Backend`: `JAVA` (default,
never loads a library) or `NATIVE_REQUIRED` (after `loadNative(Path)`, ABI 20261029; the Java result is computed and
the JNI result must equal it, `IllegalStateException` otherwise; no fallback, no retained native pointer). Results are
`M3IntSelection` values (immutable row bitmaps with `rows`, `size`, `contains`, `rowsSelected`, `and/or/xor/minus/not`)
over the `M3SelectionBits` word lane.

`m3_posting_merge.c` is the C17 JNI kernel: one fully validated immutable CSR batch per call (`GetIntArrayElements`
released with `JNI_ABORT`, invalid offsets/positions/rows refused with `IllegalArgumentException`), bitmap union or
primitive-heap sparse union, shortest-first intersection with the same 16:1 galloping threshold, union/intersection
counts and limit-bounded counts; exported as `Java_jdk_m3_arrays_M3IntPostingIndex_{nativeAbi, unionNative,
intersectionNative, countUnionNative, countIntersectionNative, countUnionUpToNative, countIntersectionUpToNative}`
and compiled into the module's `libm3_arrays` by the `native-linux` profile (one `target_sources` block appended to
`src/main/native/CMakeLists.txt` by the recipe).

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `64652588f0d4c23b1f6e0411090b2a796325ae0e`; Java owner commits `1c0f0843b21` (2026-11-03, galloping Java + C17), `94d22fee2e5` and `7a309c659e7` (2026-11-04, counts and bounded counts), `94486a51a24` (2026-11-06, superset align); C kernel commits `b2f0199da3d` / `35fb2ea3e2e` (2026-11-02), `1c0f0843b21`, `94d22fee2e5`, `7a309c659e7` |
| donor files | `MatIntPostingIndex.java` (blob `da841daf91`, 565 lines), `MatSelection.java` (`4dfbee90f2`), `synexia-mat-value/.../MatBits.java` (`961054762a`), `MatIntList.java` (`821f776b5e`), the kernel `mat_posting_merge.c` taken from the develop tree's retained variant `m3/qualification/master-landing-20261028/retained-variants/pr15528/` (blob `9f301002dc`, 572 lines), and seven oracle mains (`MatPostingGallopingContract`, `MatPostingSkewSearchContract`, `MatPostingIndexConvergenceContract`, `MatPostingCardinalityContract`, `MatPostingBoundedCountContract`, `MatPostingIntervalIntersectionContract`, `MatPostingSparseUnionContract`) |
| recipe | `relocate.py` v2: `python -I m3/arrays/recipe/posting-index-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--module m3/arrays] [--check]` |
| map | `RELOCATION_MAP.tsv`: 12 `FILE` rows, 2 `PACKAGE` rows (`com.synexia.matcollections`, `com.synexia.matvalue` -> `jdk.m3.arrays`), 11 `TYPE` rows, 1 `JNI` row (symbol prefix), 1 `INCLUDE` row (javac -h header -> `<jni.h>`), 1 `DROP` row, no `EXTERNAL` row |
| provenance | `PROVENANCE.tsv`: donor commit, blob sha, donor sha256, target sha256, `body_match`, `body_match_renamed`, `declared_drops` per file |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/arrays/`: 0 moves, 0 rewrites, 0 refusals (fixed point) |

The recipe rewrites only the license header (M3JDK style, inside the C block comment too), the `package` line, mapped
`import` lines (including nested members such as `MatIntPostingIndex.Backend.NATIVE_REQUIRED`), the mapped type
names (whole-word tokens), the JNI symbol prefix, the `#include` of the donor's javac -h header, one declared DROP
line, and inserts one `//FLLDCIM: RELOCATE` anchor per file naming the donor blob URL at the donor commit. Everything
else is kept byte for byte: `body_match_renamed=TRUE` for all 12 files. `javap -public` of donor and relocated
classes agree modulo names, except the dropped `plan()` of `M3IntPostingList`. The recipe is idempotent (second run
`changed=0`), appends the CMake registration once, and refuses an unmapped package, import, header or JNI symbol.

Why the closure is relocated rather than bridged: the plan foresaw a `java.util.BitSet` bridge for `MatSelection`,
which would have been a hand-written production class (held by the Chrome single-file generation rule while Chrome
is down); `MatSelection` + `MatBits` + `MatIntList` are `java.util`-only donor bodies, so they are relocated as
`M3IntSelection`, `M3SelectionBits` (the selection's word lane, not a general bitmap owner: plan row 2 brings the
Roaring / rank-select owners) and `M3IntPostingList` (jdk.m3.collections already owns a different `M3IntList`).
The single `plan()` line of `MatIntList` would pull the MAT capability-plan family (`MatCollectionPlan`,
`MatStorageKind`); it is removed by a declared, hash-pinned `DROP` row (nothing in the index or its oracles calls it).

Why the kernel comes from the retained variant: the develop main path
`synexia-mat-collections/native/mat_posting_merge.c` (blob `781288b369`, 415 lines) exports only three natives and has
no galloping, because the superset align commit `94486a51a24` resolved the two-sided file to its 2026-11-02 version;
`git log -- <path>` hides that merge. The develop Java declares seven natives; the only kernel in the tree defining all
seven with the galloping lower bound is the retained variant (blob `9f301002dc`, commit `7a309c659e7`). It lacks the
2026-11-02 singleton/disjoint early exits of `bf1c9ce2ef8` (performance only: the galloping oracle digest is identical
for both kernels, see GATES.tsv). Re-merging the two C lineages is a `com.synexia` fix, outside this leaf.

Derived from `m3/arrays/recipe/csr-index-20261106/relocate.py` v1 (PR #2499): C/JNI/INCLUDE/DROP rows,
nested-member imports, CMake registration and `declared_drops` added.

## Build and gates (2026-11-06, see GATES.tsv)

| Step | Command | Result |
|---|---|---|
| 1 compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/arrays/pom.xml package` (module flags `-Xlint:all,-module -Werror`) | BUILD SUCCESS; 92 tests, 0 failures, 16 skipped (baseline 85 / 0 / 14; the two new skips are the native-required interval and sparse-union oracles without `libm3_arrays`) |
| 2 oracles in the test phase | seven `M3IntPostingIndex*ContractTest` wrappers run the relocated mains | `MAT_GALLOP_SCENARIOS=36260` digest `63be72acd48b3ccb...` (JAVA_ONLY), `MAT_POSTING_SKEW_SCENARIOS=1034` (4,142 assertions), convergence 7,178 scenarios / 2,028,523 assertions, `POSTING_CARDINALITY_PASS` 35,768 scenarios, `BOUNDED_COUNT_PASS` 33,368 scenarios |
| 3 differential (Java) | `bash m3/arrays/recipe/posting-index-20261106/differential.sh <donor clone> <rev> <scratch>` after `package` | five oracle outputs identical; `M3IntPostingIndexDump` identical over 101,028 inputs / 101,044 queries / 43,581,224 rows (the oracle fixtures + 100,000 seeded posting lists; 33,264,594 bytes, sha256 `e6c3b9d28efe1de7...` on both sides) |
| 4 native (WSL: gcc 13.3, cmake 3.28, Temurin 21.0.13) | the donor C and the relocated C compiled with `-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared` (relocated also with `-include` of its javac -h header and with the module's `-Wconversion -Wshadow` flags); seven oracles under `-Xcheck:jni` against each library | all seven outputs identical, 0 JNI warnings: `MAT_GALLOP_SCENARIOS=38360` digest `1c7fb0a807479e6c7a142eb831b6057702f1a75bfe14ff21041f7a38cf48cf53` (JAVA_AND_JNI), skew 1,034 / 5,178, convergence 4,054,930 assertions (JNI_PARITY), cardinality 250,437 checks, bounded count 1,302,030 checks, interval 2,065 scenarios, sparse union 3,089 scenarios; `libm3_arrays.so` built by the module's CMake with the registration (GATES.tsv) |
| recipe | `relocate.py --check` after apply and after `jdk_naming.py` | `files=12 changed=0 provenance=UNCHANGED` |
| all | `JAVA_HOME=<JDK 21> bash m3/arrays/recipe/posting-index-20261106/verify.sh <donor clone> <rev> <scratch> [m2]` (`M3_POSTING_NATIVE_GATE=1` on a Linux host adds gate 4 through `native-gate.sh <scratch>/differential <scratch>/native`) | one `GATE` line per gate, `VERIFY PASS` |

Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Next leaves (not in this PR)

- `com.synexia`: re-merge the two `mat_posting_merge.c` lineages (galloping + counts of `7a309c659e7` with the
  singleton/disjoint early exits of `bf1c9ce2ef8`) on the develop main path; this leaf's kernel then re-seals.
- plan row 2 (Roaring / rank-select activation) may re-point `M3IntSelection` to the activated bitmap owner;
  `M3SelectionBits` is scoped to the selection until then.
- the module's `pom.xml` / `module-info.java` are unchanged; the kernel is built only by the `native-linux` profile.

## Files

| File | Purpose |
|---|---|
| `m3/arrays/src/main/java/jdk/m3/arrays/M3IntPostingIndex.java` | the relocated owner (donor body, names mapped) |
| `m3/arrays/src/main/java/jdk/m3/arrays/M3IntSelection.java`, `M3SelectionBits.java`, `M3IntPostingList.java` | the relocated closure (selection, its word lane, the posting list view; one declared DROP) |
| `m3/arrays/src/main/native/m3_posting_merge.c` | the relocated C17 JNI kernel (seven natives) |
| `m3/arrays/src/main/native/CMakeLists.txt` | `+ target_sources(m3_arrays PRIVATE m3_posting_merge.c)` block, appended by the recipe |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3IntPostingIndex{,Skew,Convergence,Cardinality,BoundedCount,Interval,SparseUnion}Contract.java` | the relocated oracle mains |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3IntPostingIndex*ContractTest.java`, `M3IntPostingNativePath.java` | JUnit 5 wrappers in the module's template and the native-path bridge |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3IntPostingIndexDump.java` | reflective dump driver of the differential gate (not a test) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `differential.sh`, `native-gate.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates (Java differential, Linux native gate, orchestration) and their record |
