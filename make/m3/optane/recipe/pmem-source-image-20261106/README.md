<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# recipe pmem-source-image-20261106: the PMem source image relocated into jdk.m3.optane

Leaf `pmem-source-image` (plan `C:/m3-superset/conv/plans/fs-pmem.tsv` rank 3, after rank 2 `pmem-region-core`, #2534;
census L026 `PmemSourcePathIndex`) of the M3JDK convergence programme: the immutable source snapshot of
`hsoliwal/com.synexia` `m3-pmem-sdk` (pack, open, sealed reads, streaming seal, path lookup, per-file search) becomes
part of `m3/optane` (module `org.m3jdk.optane`, exported package `jdk.m3.optane`) by recipe, never by hand.

## What was relocated

| Donor | M3JDK (`m3/optane/`) | Role |
|---|---|---|
| `m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemSourceImage.java` | `src/main/java/jdk/m3/optane/M3PmemSourceImage.java` | `pack` (sorted selected files to `source.bin` + `manifest.bin`, manifest installed last by atomic rename), `open` (magic, version, geometry, manifest SHA-256 and every file digest before a READ_ONLY region is admitted), `readSealed` (owning and caller buffer), `verifySealed` (streaming seal), `fileIndexOf`, `indexOf`, `warm`, `close` |
| `.../PmemSourceSlices.java` | `.../M3PmemSourceSlices.java` | primitive offset/length/SHA-256 view over one READ_ONLY region (source-slices v2) |
| `.../PmemSourcePathIndex.java` | `.../M3PmemSourcePathIndex.java` | census L026: package-private `int[]` open-addressing path slots, 16-probe bound, binary-search fallback (called by `M3PmemSourceImage` since the BLOB re-pin, see Lineage) |
| `.../PmemCorpusAtom.java` | `.../M3PmemCorpusAtom.java` | package-private bounded candidate-only atom read over one POSIX mapping per file, six-field CLI |
| `src/junit/java/test/PmemSourceImage{,Path}JUnitTest`, `PmemSourcePathIndexJUnitTest`, `PmemSourceSlices{,Lifecycle}JUnitTest`; `campaign/src/test/.../PmemStreamingSealJUnitTest`, `PmemSourceImageReusableBufferJUnitTest`; `src/corpus-test/.../PmemCorpusAtomUnitTest` | `src/test/java/jdk/m3/optane/M3Pmem*.java` | the oracles (the two source-slices classes and the opt-in POSIX parity methods run in the native lane) |
| `docs/SOURCE_SLICES_CONTRACT.md`, `SOURCE_PATH_LOOKUP_CONTRACT.md`, `PMEM_PATH_PROBE_BOUND_CONTRACT.md` | `docs/` | the contracts |
| crate `m3-pmem-streaming-seal-20261105` (CONTRACT, LOCAL_PROOF, RECIPE_MEMORY, DONOR_CATEGORY, the PRE->POST manifest) | `streaming-seal/` | the authored-but-unrun recipe of `verifySealed`; `SEAL.tsv` states its POST against the pin and where it was executed here |
| `m3-pmem-sdk/recipes/source-slices-v2/` (PLAN, PROGRESS, the donor-run evidence and preimage records), `SOURCE_SLICES_DONOR_REVIEW.tsv`, `source-slices-manifest.tsv` | `source-slices-v2/`, `evidence/` | the donor recipe of the caller-buffer reads: plan and ledger relocated, run evidence VERBATIM |

`RELOCATION_MAP.tsv` also carries the SKIP rows (multi-pattern search: not needed by the image; ranks 4 and 5; the
crate's PRE/POST resources and API stub; the default-package evidence drivers and the v1 preimages of source-slices v2),
the DROP rows (generation templates, donor scripts) and one MERGE row (`pom.xml`, below).

## Donor pin and lineage

| Item | Value |
|---|---|
| donor revision | `hsoliwal/com.synexia` develop `5ecfe052562cdd1a7a6570068654c7f813fd0f47` (2026-11-06, merge of #16754), the one pin of the three pmem follow-up leaves: after #16749 (`36827ee8a798`: `NativePmem` adb94eb2f091, `PmemSourceImage` bd2c6c1ee83e with `verifySealed`) and after #16750 (merge `061f544acfc8`: re-sealed `PmemTransferRecipe` / `PmemSourceSlicesRecipe`, recipe tables and templates only). `git diff 36827ee8a798..5ecfe052562c -- m3-pmem-sdk` touches `recipes/` only, so every row of `pmem-region-core-20261106` holds the same blob at this pin. Every FILE row but `PmemSourceImage` is develop's own blob (`PROVENANCE.tsv` `base_kind=DEVELOP`); `PmemSourceImage` is the BLOB row (`base_kind=PR_HEAD_SUPERSET`, section Re-pin) |
| `PmemSourceImage` lineage | develop `3e8244bddee` (superset alignment, blob `0db864e19fb6`) wired `PmemSourcePathIndex` into `fileIndexOf` (`7839449e7be` bounded path index, `5ff72bcc0fe` probe bound) without `verifySealed`; #16749 (`faf902e3a308`) replaced it by the pr16669 head copy `bd2c6c1ee83e` = streaming-seal POST + `readSealed(int, byte[], int)`, whose `fileIndexOf` is `Arrays.binarySearch`. No donor commit holds both: the relocated image is develop's blob; `M3PmemSourcePathIndex` is relocated with its JUnit oracle and the probe-bound proof lane of the differential, and the donor fix-forward com.synexia #16762 (crate `m3-pmem-source-path-wiring-20261106`) restores the wiring on top of `bd2c6c1ee83e` as blob `743e5bb54bc6`, pinned here by the BLOB row at `9b4728b743ab` (section Re-pin) |
| streaming seal | crate POST of `PmemStreamingSealJUnitTest` = develop (4a4dfb13); develop `PmemSourceImage` = POST (3ba80841) + 6 lines (`SEAL.tsv`); the crate's Maven/OpenRewrite `Recipe.run` stays in Synexia, its JUnit runs here on the relocated owner (FILE and POSIX) |

## Recipe

`python -I m3/optane/recipe/pmem-source-image-20261106/relocate.py --donor-git <clone> --donor-rev 5ecfe052562c [--check]`
(`RELOCATE_PMEM_SOURCE_IMAGE` v3, derived from `pmem-region-core-20261106/relocate.py` v2: every v2 rule unchanged; v3
adds VERBATIM rows, exact donor bytes under an `evidence/` directory, and MERGE rows, documented edits of existing module
files). TYPE rows `Pmem* -> M3Pmem*` repeat the region-core map and add the twelve types of this leaf; the campaign test
package joins `jdk.m3.optane` (listed before `com.synexia.m3.pmem`); TEXT rows restate the donor path in the probe-bound
contract and the corpus-atom schema id `m3.pmem-corpus-atom.v1` (the jdk_naming form). 22 FILE rows
`body_match_renamed=TRUE` (2 also `body_match=TRUE`), 24 VERBATIM rows `body_match=TRUE`.

MERGE `pom.xml`: the Windows test filter also excludes `M3PmemSourceSlicesJUnitTest` and
`M3PmemSourceSlicesLifecycleJUnitTest` (POSIX regions only); the profile `m3-optane-native` passes
`-Dpmem.streaming.native=true -Dpmem.source.reuse.native=true`; both surefire argLines add
`-XX:-OmitStackTraceInFastThrow` (reproducible dump digests). No class of #2534 changes (class files byte-identical).

## Gates (2026-11-06, GATES.tsv)

| Gate | Result |
|---|---|
| 1_package_lint_werror | BUILD SUCCESS (exit 0); maven-compiler-plugin 3.13.0, release 21, -Xlint:all -Werror, 21 main sources (17 + 4) and 16 test sources (6 + 10); 0 compiler warnings (no [WARNING] line outside the test summary); jar --describe-module: org.m3jdk.optane exports jdk.m3.optane, requires java.base |
| 2_tests_java_path | Tests run 31, Failures 0, Errors 0, Skipped 1 (twentyThousandDistinctFiftyKilobyteFilesWithOneBuffer, opt-in -Dpmem.source.reuse.scale=true): M3PmemFileBackendJUnitTest 7 and M3PmemFileReplayTest 1 (the #2534 counts; M3_PMEM_FILE_REPLAY_PASS operations=10000 lines=10376 refusals=2235 sha256_16=8a38db06cedd3cd6, the #2534 GATES digest), M3PmemSourceImageJUnitTest 9, M3PmemSourceImagePathJUnitTest 1, M3PmemSourcePathIndexJUnitTest 2, M3PmemStreamingSealJUnitTest 4, M3PmemSourceImageReusableBufferJUnitTest 5 (1 skipped), M3PmemSourceImageReplayTest 2 (M3_PMEM_SOURCE_IMAGE_REPLAY_PASS operations=10000 lines=10606 refusals=730 sha256_16=37cce079598d15dc) |
| 2b_tests_native_lanes | region-core classes (M3PmemAtomCorrectnessJUnitTest, M3PmemFileBackendJUnitTest, M3PmemNativeKmpPrefix32JUnitTest, M3PmemFileReplayTest): 32 tests found, 32 successful (the #2534 count); source-image classes (the 7 oracles + M3PmemSourceImageReplayTest): 34 found, 33 successful, 1 skipped (scale lane), 0 failed, including the 10 + 1 POSIX source-slices tests and the opt-in POSIX parity methods; PASS PMEM_CORPUS_ATOM_UNIT checks=32; M3_PMEM_SOURCE_IMAGE_REPLAY_PASS operations=10000 lines=10606 refusals=730 sha256_16=fe3561fe23cf4a35 |
| 3_differential_file | donor develop 5ecfe052562c (14 owners: the 10 region-core owners + PmemSourceImage, PmemSourceSlices, PmemSourcePathIndex, PmemCorpusAtom; javac --release 21 -Xlint:all -Werror) vs relocated target/classes over M3PmemSourceImageDump: 9 path-index lanes (0 mismatches against Arrays.binarySearch, 16/17/256-name collision corpora), image/refusal/slice lanes, 100,000 seeded operations; TOTAL operations=243398 exceptions=6296; sha256 e82a49ca379ce0462cd22b3266a1cbe91bee20afc37719f28507bdf7a020ca46 both sides (4,214,584 bytes, 100,606 lines), identical on a second run; 34 s |
| 3b_differential_native | FILE and POSIX regions, corpus-atom lane; TOTAL operations=247051 exceptions=6381; sha256 a5014542f6532130155d7cadeee756a7391299775f3806d2241f32c2e33592e6 both sides (4,279,471 bytes, 101,220 lines), under -Xcheck:jni; 9 s |
| 4_native_matrix | both NATIVE_MATRIX PASS: gcc 13.3.0 -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror, PMDK not built, 14 exported symbols each; native dumps of 20,000 operations (591 POSIX lines, 21 corpus-atom lines, TOTAL operations=76130 exceptions=1449) identical across default, -Xint and -XX:TieredStopAtLevel=1, and identical between donor and relocated: sha256 9128921d3465d0f9a1d821aab3d8108640977903f36eec389b2b46f2108877e8 (953,351 bytes, 21,220 lines) |
| 5_untouched_classes | 23 main class files (module-info included) and 7 test class files byte-identical; 6 new main classes (M3PmemSourceImage, its Limits, M3PmemSourceSlices, M3PmemSourcePathIndex, M3PmemCorpusAtom, its Atom) |
| 6_scope | 58 files, every path under m3/optane/ (one existing file edited: pom.xml, the MERGE row) |
| recipe_idempotent | files=46 changed=0 provenance=UNCHANGED, also after jdk_naming apply; 22 FILE rows body_match_renamed=TRUE (2 body_match=TRUE), 24 VERBATIM rows body_match=TRUE; region-core recipe --check at its pin 36827ee8a798: files=22 changed=0; its 22 donor blobs are identical at 5ecfe052562c |
| jdk_naming_apply | JDK_NAMING moves=0 rewrites=0 refusals=0 absent=0; FIXED_POINT moves=0 rewrites=0 (the first apply rewrote 2 files, PKG_M3=3: differential.sh and the dump Javadoc spelled the donor package; both now take it from the map) |
| seal_crate_executed | m3-pmem-streaming-seal-20261105: develop = POST for the JUnit (4a4dfb13) and the campaign admission (e508787a), develop PmemSourceImage = POST + 6 lines; M3PmemStreamingSealJUnitTest 4 of 4 on Windows (FILE) and 4 of 4 under WSL with -Dpmem.streaming.native=true (POSIX parity) |

## Re-pin 2026-11-06: the path-index wiring (BLOB row)

#16749 replaced develop's `PmemSourceImage` (`0db864e19fb6`, path index wired) by the pr16669 copy `bd2c6c1ee83e`
(`verifySealed`, no wiring), so `M3PmemSourcePathIndex` was relocated without a caller. com.synexia #16762 (crate
`m3-pmem-source-path-wiring-20261106`, recipe PMEM_SOURCE_PATH_WIRING v1, hash-pinned) re-applies the three
WIRING_LOST hunks onto `bd2c6c1ee83e`: blob `743e5bb54bc6` (sha256 `f9f8c34d70e8d204ff90bc428b43e81e2b8c4f197c2922b61502bbae1d016d67`).
This recipe takes it through one BLOB row of `RELOCATION_MAP.tsv` (`743e5bb54bc6...@9b4728b743ab`, the #16762 head);
`--donor-rev` stays `5ecfe052562c`, so the anchors and PROVENANCE rows of the other 45 files are unchanged (a full
`--donor-rev` move would rewrite all 22 FILE anchors). Recipe output: `M3PmemSourceImage.java` (the anchor and the
three hunks: `pathSlots` field, `M3PmemSourcePathIndex.build` in the constructor, `M3PmemSourcePathIndex.find` in
`fileIndexOf`) and its PROVENANCE row; nothing else. `M3PmemSourcePathWiringJUnitTest` (new, the discriminators of the
donor's `PmemSourcePathWiringContractTest`) proves the slots are built and consulted.

| Gate | Result |
|---|---|
| repin_recipe | relocate.py v3 --donor-rev 5ecfe052562c with the BLOB row: files=46 changed=1 (M3PmemSourceImage.java, base=PR_HEAD_SUPERSET, body_match_renamed=TRUE) provenance=CHANGED (1 row); second run --check: changed=0 provenance=UNCHANGED |
| repin_package_lint_werror | mvn clean package (JDK 21, Maven 3.9.14, -Xlint:all -Werror): BUILD SUCCESS, 0 compiler warnings; Tests run 57, Failures 0, Errors 0, Skipped 4 (master 8481e882d633: 55/0/0/4; +2 M3PmemSourcePathWiringJUnitTest) |
| repin_wiring_red_green | M3PmemSourcePathWiringJUnitTest against master's target/classes: 0 of 2 (NoSuchFieldException pathSlots); against the re-pinned classes: 2 of 2 |
| repin_differential_file | differential.sh vs the fixed donor 9b4728b743ab (14 owners, PmemSourceImage = 743e5bb54bc6): TOTAL operations=243398 exceptions=6296, PATHINDEX lanes=9 mismatching=0, sha256 e82a49ca379ce0462cd22b3266a1cbe91bee20afc37719f28507bdf7a020ca46 both sides (4,214,584 bytes, 100,606 lines; the #2586 digest), identical on a second run; 31 s and 32 s |
| repin_native_lanes | WSL Ubuntu 24.04, gcc 13.3.0, Temurin 21.0.13: NATIVE_MATRIX PASS donor (fixed) and relocated, 14 symbols each, TOTAL operations=76130 exceptions=1449, sha256 9128921d3465d0f9a1d821aab3d8108640977903f36eec389b2b46f2108877e8 identical; differential.sh native vs 9b4728b743ab under -Xcheck:jni: TOTAL operations=247051 exceptions=6381, sha256 a5014542f6532130155d7cadeee756a7391299775f3806d2241f32c2e33592e6 both sides, 8 s; JUnit -Xcheck:jni: region-core 32 of 32, source-image classes + M3PmemSourcePathWiringJUnitTest 36 found, 35 successful, 1 skipped (scale lane), 0 failed; M3_PMEM_SOURCE_IMAGE_REPLAY_PASS sha256_16=fe3561fe23cf4a35 |
| repin_untouched_classes | vs master 8481e882d633 target: 44 main class files byte-identical, 1 changed (M3PmemSourceImage.class; M3PmemSourceImage$Limits identical); 39 test class files byte-identical, 1 added (M3PmemSourcePathWiringJUnitTest) |
| repin_jdk_naming | JDK_NAMING moves=0 rewrites=0 refusals=0 absent=0; FIXED_POINT moves=0 rewrites=0 (plan, apply and a second apply; no file rewritten) |
| repin_scope | 7 files, every path under m3/optane/: M3PmemSourceImage.java, M3PmemSourcePathWiringJUnitTest.java (new), RELOCATION_MAP.tsv, PROVENANCE.tsv, README.md, GATES.tsv, RECIPE_MEMORY.tsv |

Land order: com.synexia #16762 first; if its merge changes the commit that holds blob `743e5bb54bc6` on develop, move
the BLOB row to that merge (`python -I relocate.py ... --donor-rev 5ecfe052562c`: only the anchor of
`M3PmemSourceImage.java` and its PROVENANCE row change).

## Files

| File | Purpose |
|---|---|
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the recipe (v3), its map (FILE, VERBATIM, PACKAGE, TYPE, EXTERNAL, TEXT, SKIP, DROP, MERGE rows) and its pins |
| `differential.sh`, `native-matrix.sh` | gate 3 (donor vs relocated over the dump, FILE) and gate 4 (one family on Linux: C17 library, native dump in default/-Xint/C1) |
| `GATES.tsv`, `RECIPE_MEMORY.tsv`, `SEAL.tsv` | the gate record, the lessons, the streaming-seal crate against the pin |
| `../../src/test/java/jdk/m3/optane/M3PmemSourceImageDump.java`, `M3PmemSourceImageReplayTest.java` | the reflective differential driver (not a test) and the gate 2 wrapper (new) |
