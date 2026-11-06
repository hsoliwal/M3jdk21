<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# immutable-pools-20261106: relocate the canonical pools and Uuid128 into `jdk.m3.collections` (recipe RELOCATE_IMMUTABLE_POOLS v2)

Third leaf of the m3scale convergence plan (`C:/m3-superset/conv/plans/m3scale.tsv`, row 3 `m3-immutable-pool`,
decision ABSORB into `m3/collections`): the canonicalizer and 128-bit id leaf of the donor module
`m3-immutable-pool`, without its frozen/mapped image family, registry, DAG, codecs, JNI and `java.sql` boundaries.

## What it is

| Owner | Role |
|---|---|
| `M3StrongValuePool<T>` | bounded strong canonical pool: equal values receive one retained representative and one stable dense local id; never evicts; `internId`, `canonicalize`, `findId`, `resolve`, `stats`; overflow by `M3StrongPoolOverflowPolicy` (`RETURN_INPUT` returns `M3ImmutableId.NONE` and the caller's value, `REJECT` throws). The bounded interner the anchor spec (TIL) asks for; no `String.intern` |
| `M3ImmutableId` | record of one packed `long`: high 16 bits type id (1..65535), low 48 bits local id, `NONE = -1`, unsigned order, `same(long,long)` |
| `M3StrongPoolStats` | record `hits/inserts/overflows/entries/capacity`, `requests()`, `reuseRatio()` |
| `M3WeakValueCanonicalizer<T>` | weak canonicalization (`WeakHashMap<T, WeakReference<T>>`) when GC reclaimability matters more than stable ids |
| `M3Uuid128` | record of the UUID's two longs: allocation-free `parse`/`toCanonicalString`, 16-byte big-endian codec over `byte[]` (with offset) and `ByteBuffer`, `version()`, `variant()`, unsigned 128-bit order (= canonical text and byte order, unlike `java.util.UUID.compareTo`), `fromUuid`/`toUuid` |
| `M3Uuid128Vector` | object-free dense UUID column: two `long[]` lanes, 16 bytes per occurrence, `Builder` |
| `M3Uuid128HashIndex` | immutable open-addressing index UUID bits -> first vector position (long/long/int/byte lanes), built under an `M3PoolProgressMonitor` |
| `M3UuidStorageFootprint` | binary (16) vs canonical ASCII (36) payload accounting |
| `M3PoolProgressMonitor` | the donor's progress/cancellation contract (`begin`, `worked`, `cancelled`, `done`, `checkCancelled`, all default, `none()`); carried under its own name, see NEC. Since 2026-11-06 an adapter over `jdk.m3.collections.M3ProgressMonitor` (`m3/qualification/progress-monitor-unify-20261106`: `begin` -> `beginTask`, `cancelled` -> `isCanceled`); its map row is `SUPERSEDED` |

`java.base` only (`java.lang.invoke`, `java.util`, `java.util.concurrent`, `java.lang.ref`, `java.nio`). The classes
join the existing module `jdk.m3.collections` (package already exported), so `pom.xml` and `module-info.java` are
unchanged. Main 720 lines, test 813 lines (3 relocated oracles, 3 JUnit wrappers, 1 reflective dump driver).

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/synexia-m3scale-apache2` develop `3385d79a2968c770aedf1d3398b2a62f05ebc092` (2026-11-06); leaf commits 98477ed0 (2026-09-23, StrongValuePool/WeakValueCanonicalizer/ImmutableId), fc4e7850 + 79c83cd4 + 6c09697c (2026-09-23, Uuid128 + vector + hash index), 72271745 (2026-09-23, uuid variant), 99f80871 (2026-10-27, exact tree restore) |
| donor files | 10 main files of `m3-immutable-pool/src/main/java/com/synexia/m3scale/immutablepool/` (blobs in `PROVENANCE.tsv`; `StrongValuePool` f216222edf15, `Uuid128` bb77b6d5b7a8) and the oracles `ImmutablePoolSelfTest`, `Uuid128SelfTest`, `uuid/UuidBinarySelfTest` |
| recipe | `relocate.py` v2: `python -I m3/collections/recipe/immutable-pools-20261106/relocate.py --donor-git <clone> --donor-rev 3385d79a [--module m3/collections] [--check] [--donor-subset DIR]` |
| map | `RELOCATION_MAP.tsv`: 1 `REPO`, 12 `FILE`, 1 `SUPERSEDED` (the pool monitor, since 2026-11-06), 2 `PACKAGE`, 1 `SUBSET_PACKAGE`, 13 `TYPE`, 14 `DROP_METHOD`, 7 `DROP_LINE`, 6 `REWRITE`, 1 `INSERT_AFTER`, 9 `SKIP`, 3 `NEC` rows; no `EXTERNAL` row |
| provenance | `PROVENANCE.tsv`: donor commit, blob, sha256, URL, target sha256, counts of renamed/mapped/rewritten/dropped/inserted lines, `body_match` (13/13 TRUE) |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/collections/`: 0 moves, 0 rewrites, 0 refusals, fixed point |

The recipe rewrites only the license header (M3JDK style), the `package` line, mapped `import` lines and the 13 mapped
type names (whole words), applies the declared `DROP_METHOD`/`DROP_LINE`/`REWRITE`/`INSERT_AFTER` rows, and inserts one
`//FLLDCIM: RELOCATE` anchor before the top-level type naming the donor blob and commit (the URL lives in the .tsv
files: the donor repository name is a naming-rename token). `body_match` is read back from disk: every target line is a
donor line, a donor line with the declared rows and renames applied, the mapped package or import line, or an explained
insertion. The ten main files carry no drop, rewrite or import change; `M3WeakValueCanonicalizer` carries the one
`INSERT_AFTER` (an explicit, empty constructor the exported-module lint `missing-explicit-ctor` demands). The oracles
are method subsets: the methods over the frozen/mapped/codec/DAG/JDBC pools are `DROP_METHOD` rows with their `SKIP`
owner, their `main()` calls and private imports `DROP_LINE` rows, and the PASS line of the pool oracle loses its
`NativePoolOps.available()` flag by a `REWRITE` row. `--donor-subset` writes the donor side of the differential: the
main files verbatim (`SUBSET_VERBATIM=TRUE`, refused otherwise) and the oracles with the same rows applied.

Derived from `m3/algorithms/recipe/families-20261106/relocate.py` v1 (same donor repository: `REPO` row, `DROP` rows,
anchor without URL, read-back `body_match`) and `m3/arrays/recipe/csr-index-20261106/relocate.py` v1 (PR #2499: map
and provenance in the recipe directory of an existing module).

## NEC rows (suggestions, equivalence not decided)

1. Two `Uuid128` variants in the donor: `immutablepool/Uuid128.java` (blob `bb77b6d5b7a8`, 174 lines, commit 6c09697c
   2026-09-23 12:00:25 +0530) and `uuid/Uuid128.java` (blob `1aadb7055be7`, 120 lines, commit 72271745 2026-09-23
   12:02:25 +0530). The brief's rule (port the newer by commit date) points at the uuid variant by 120 s; the survivor
   is the immutablepool variant because `Uuid128HashIndex`, `Uuid128Vector`, `UuidStorageFootprint` and
   `Uuid128SelfTest` bind to its API and porting the other would need body edits, and because its surface is the larger
   one (ByteBuffer codec, `version()`, `variant()`, allocation-free parse/format). The uuid variant alone carries
   `toHexString()`, `HEX_TEXT_CHARS` and `same(long,long,long,long)`. Its round-trip oracle is replayed on the survivor
   as `M3UuidBinarySelfTest` (7 checks) through `REWRITE` rows. Suggestion: retire the uuid variant with
   `UuidColumn`/`UuidJdbc`.
2. Donor `ProgressMonitor` (blob `6328187b7bf3`) -> `M3PoolProgressMonitor`, not `jdk.m3.collections.M3ProgressMonitor`
   (blob `117b4eadd484`): same idea, different shape (`begin`/`cancelled`/`checkCancelled`, all default, `none()` vs
   `beginTask`/`isCanceled`/`checkCanceled`/`setCanceled`/`state()`/`split()`, abstract). An adapter or rename pass is a
   later leaf.
3. `ImmutableId` (blob `a6f11fc6e2c5`) carried as the record `M3ImmutableId`; the plan suggests an int slot.

`FrozenImmutablePool` (SKIP row) is ALREADY-verify against `m3/ports/file` `jdk.m3.file.M3FmtFrozenDictionary` (blob
`cef118c056e6`): the donor restores a frozen value pool from an `ImmutablePoolImage` (96-byte little-endian header,
MAGIC `0x5049334d` 'M3IP', VERSION 1, section offsets, 32-byte fingerprint); the owner is a SHA-256 manifest over a
frozen `M3FileDictionary` (domain `M3FMT_FROZEN_DICTIONARY_V1`) with no image, magic or header. Shapes differ, nothing is
byte-comparable; hosting the image behind `M3FmtFrozenDictionaryContract` is a later leaf. Not ported.

## Build and gates (2026-11-06, see GATES.tsv)

| Step | Command | Result |
|---|---|---|
| 1 compile, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/collections/pom.xml -DskipTests package` (module flags `-Xlint:all,-module -Werror`) | BUILD SUCCESS, 0 warnings |
| 2 oracles | `java -cp target/classes;target/test-classes jdk.m3.collections.M3ImmutablePoolSelfTest` / `M3Uuid128SelfTest` / `M3UuidBinarySelfTest`; wrappers `M3ImmutablePoolTest`, `M3Uuid128Test`, `M3UuidBinaryTest` under surefire | `PASS: 71 immutable-pool checks`, `PASS: 26 uuid128 checks`, `PASS: 7 UUID binary checks`; 3 x Tests run: 1, Failures: 0 |
| 3 module tests vs base | `mvn -f m3/collections/pom.xml test` on base 39b0d9e2c6e and head | base 1624 run / 5 failures / 49 errors / 2 skipped; head 1627 / 5 / 49 / 2; failing sets 54 = 54, diff empty (pre-existing JNI-library failures on Windows), `TEST_BASELINE_DIFF.txt` |
| 4 differential | `bash m3/collections/recipe/immutable-pools-20261106/differential.sh <donor clone> 3385d79a <scratch> 100000 20261106` after step 1 | oracle outputs identical (sha256 `f5c5a1768a86fd6b...`); `M3ImmutablePoolsDump` dumps identical over 100,000 seeded operations, 103,202 lines, 13,836,186 bytes (sha256 `e5124ff1f52d6bcc...` on both sides) |
| 5 naming | `jdk_naming.py plan` before, `apply` after | 0 moves, 0 rewrites, 0 refusals, fixed point |
| 6 scope | `git status --short` | only `m3/collections/**` and the `CONVERGE_COLLECTIONS.tsv` rows |
| recipe | `relocate.py --check` after apply and after `jdk_naming.py` | `files=12 changed=0 provenance=UNCHANGED body_match=12/12` (13/13 before the monitor row became `SUPERSEDED`, 2026-11-06) |
| javap | `javap -public` donor vs relocated, names mapped | 138 lines each, identical |

`verify.sh <donor clone> <rev> <scratch> [ops] [seed]` runs the gates in order (`FULL_TESTS=1` adds step 3). Lessons of
the run are in `RECIPE_MEMORY.tsv`.

## Ledgers

- `m3/qualification/converge-collections-20261030/CONVERGE_COLLECTIONS.tsv`: the m3-immutable-pool rows of
  `StrongPoolOverflowPolicy`, `StrongPoolStats`, `StrongValuePool`, `Uuid128HashIndex` move from GAP to
  `jdk.m3.collections.M3*` PRESENT_IN_M3JDK; rows for `Uuid128`, `Uuid128Vector`, `WeakValueCanonicalizer`,
  `ImmutableId`, `UuidStorageFootprint`, `ProgressMonitor` are added (the filename census had not counted them); the
  SKIP rows stay GAP.
- `m3/collections/JDK_FAMILY_MAP.tsv`: rows `UUID` -> `M3Uuid128` family, `String.intern / bounded interner` ->
  `M3StrongValuePool` family, `WeakHashMap (canonicalizer)` -> `M3WeakValueCanonicalizer`.

## Next leaves (not in this PR)

- The frozen/mapped image family (`FrozenImmutablePool`, `ImmutablePoolImage`, `MappedImmutablePool`,
  `LiveImmutablePool`, `ImmutablePoolBuilder`, `ImmutablePoolRegistry`, codecs, ~1,500 LOC) against
  `M3FmtFrozenDictionary` + `M3FmtFrozenDictionaryContract` + `M3FmtVarints` (plan row 3 notes).
- `ImmutableDag` (MIndexDagBacked) to the index lane (plan row 10).
- Retiring the uuid `Uuid128` variant with `UuidColumn`/`UuidJdbc`/`UuidFootprint` (NEC row 1); an adapter between
  `M3PoolProgressMonitor` and `M3ProgressMonitor` (NEC row 2).

## Files

| File | Purpose |
|---|---|
| `m3/collections/src/main/java/jdk/m3/collections/M3StrongValuePool.java`, `M3WeakValueCanonicalizer.java`, `M3StrongPoolStats.java`, `M3StrongPoolOverflowPolicy.java`, `M3ImmutableId.java`, `M3Uuid128.java`, `M3Uuid128HashIndex.java`, `M3Uuid128Vector.java`, `M3UuidStorageFootprint.java`, `M3PoolProgressMonitor.java` | the relocated owners (donor bodies, names mapped) |
| `m3/collections/src/test/java/jdk/m3/collections/M3ImmutablePoolSelfTest.java`, `M3Uuid128SelfTest.java`, `M3UuidBinarySelfTest.java` | the relocated oracle mains (method subsets) |
| `m3/collections/src/test/java/jdk/m3/collections/M3ImmutablePoolTest.java`, `M3Uuid128Test.java`, `M3UuidBinaryTest.java` | JUnit 5 wrappers in the module's `M3*Test -> M3*SelfTest` template |
| `m3/collections/src/test/java/jdk/m3/collections/M3ImmutablePoolsDump.java` | reflective dump driver of the differential gate (not a test) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv`, `TEST_BASELINE_DIFF.txt` | gates and their record |
