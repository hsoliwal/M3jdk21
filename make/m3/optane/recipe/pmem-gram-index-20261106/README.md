<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# recipe pmem-gram-index-20261106: the PMem gram index relocated into jdk.internal.m3.optane

Leaf `pmem-gram-index` (plan `C:/m3-superset/conv/plans/fs-pmem.tsv` rank 4, after ranks 2 and 3; census L027
`PmemCorpusGramIndex`/`PmemCorpusSearch`, L028 `PmemCorpusSelectedSearch`, L029 `PmemGramCampaignAdmission`) of the M3JDK
convergence programme: the two-byte-gram necessary-condition prefilter of `hsoliwal/com.synexia` `m3-pmem-sdk/campaign`
and its exact first-hit searches become part of `m3/optane` (module `org.m3jdk.optane`) by recipe, in the NON-EXPORTED
package `jdk.internal.m3.optane`.

## Package decision

The plan relocates the index "as internal class" (package-private in Synexia; census `NEW:jdk.internal.m3.pmem.campaign`)
and keeps the consumer-facing search APIs unexported until the search session decides them. The campaign package maps to
`jdk.internal.m3.optane`: internal packages follow `jdk.internal.m3.<area>` in the JDK naming scheme of
`m3/tooling/jdk-naming`, the area being the module's (`optane`); `module-info.java` is unchanged (`exports jdk.m3.optane`
only), so the public `M3PmemCorpusSearch` is not reachable outside the module. The classes reach the SDK through its
exported API only (`M3PmemSourceImage`, `M3PmemSearch`).

## What was relocated

| Donor | M3JDK (`m3/optane/`) | Role |
|---|---|---|
| `campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndex.java` | `src/main/java/jdk/internal/m3/optane/M3PmemCorpusGramIndex.java` | L027: adaptive per-file sparse pairs or dense 8 KiB bit map, 512 MiB retained ceiling, distinct required pairs and three-pair conjunction; no false negatives over an immutable image; every candidate rechecked by the exact search |
| `.../PmemCorpusSelectedSearch.java` | `.../M3PmemCorpusSelectedSearch.java` | L028: exact first hits over an explicitly selected ascending set (completeness relative to the selection) |
| `.../PmemCorpusSearch.java` | `.../M3PmemCorpusSearch.java` | L027: exact first hit per file over the whole image |
| `campaign/src/test/.../PmemCorpus{GramIndex,Selected,Search}JUnitTest.java` | `src/test/java/jdk/internal/m3/optane/M3Pmem...` | the oracles (opt-in POSIX parity and scale methods by system property) |
| crate `m3-pmem-gram-index-20261102` `proof/m3gramoracle.c`, `proof/stubs/.../NativeByteOracle.java` | `src/test/native/M3GramOracle.c`, `src/test/java/jdk/m3/optane/M3NativeByteOracle.java` | the independent C17 brute-force first-hit oracle (JNI), checked against every exact result of the gram dump in the native lane |
| crates `m3-pmem-gram-index-20261102`, `m3-pmem-selected-first-hit-20261102`, `m3-pmem-gram-{conjunction,campaign,preflight,source-revalidate}-20261105`, `m3-pmem-three-gram-20261105` (contracts, local proofs, memories, donor categories, frozen inputs, proof scripts, PRE->POST manifests) | `<crate>/evidence/` | the donor recipe records, VERBATIM; `CHAIN.tsv` states every manifest row against the pin |

Not relocated (`RELOCATION_MAP.tsv` SKIP rows): **`PmemGramCampaignAdmission` (L029)** and its JUnit: the admission composes
the index with the Synexia-private LLM campaign (`PmemFileCampaign` -> `com.synexia.ai.m3.M3LlmInvocationLedger`,
`PmemCatalogueWorkOrders`, `QualityIndex`, `ProblemCatalogue`: file-level model work orders); the plan ports "only the
candidate filter" and keeps the campaign admission Synexia-private (`SOURCE_CAMPAIGN.md` item 10). The crates' stub-based
proofs (`PmemGramIndexSmoke`, `GramIndexTestRunner`, `PmemOneGigabyteMappedProof`, the selected/corpus smokes, the
conjunction and three-gram proofs) pin older owners and test-only `PmemSourceImage` stubs without the
`readSealed(int, byte[], int)` the pinned index calls; their checks run here on the real relocated SDK (below).

## Donor pin and lineage

| Item | Value |
|---|---|
| donor revision | `hsoliwal/com.synexia` develop `5ecfe052562cdd1a7a6570068654c7f813fd0f47` (2026-11-06, merge of #16754), the one pin of the three pmem follow-up leaves, after #16749 and #16750; every FILE row is develop's own blob (`PROVENANCE.tsv` `base_kind=DEVELOP`) |
| #16748 / #16749 | the 3-way repair #16748 (crate `m3-pmem-sdk-retained-merge-20261106`, `c278513bd45`) left 4 campaign paths UNRESOLVED; #16749 (`faf902e3a308`) took the pr16669 head copies, which develop's live blobs now equal: `PmemCorpusGramIndex` c3a33cabb2de, `PmemCorpusGramIndexJUnitTest` 31160b305bc5, `PmemGramCampaignAdmission` 312c70fa67fa (not relocated) |
| crate chain | `CHAIN.tsv`: develop's index = the three-gram POST (86c79682) + the reusable-buffer reads (17 lines), its JUnit = that POST + 2 tests; the selected search and its JUnit equal the selected-first-hit POSTs; the admission chain ends at the streaming-seal POST (P1 `SEAL.tsv`) |
| stacking | this leaf needs `M3PmemSourceImage` of `recipe/pmem-source-image-20261106` (PR #2586); the branch carries that commit unchanged, so #2586 merges first |

## Recipe

`python -I m3/optane/recipe/pmem-gram-index-20261106/relocate.py --donor-git <clone> --donor-rev 5ecfe052562c [--check]`
(`RELOCATE_PMEM_GRAM_INDEX` v3: the same v3 as `recipe/pmem-source-image-20261106`). PACKAGE rows map the campaign package
to `jdk.internal.m3.optane` (listed before the SDK package) and the SDK package to `jdk.m3.optane`; TYPE rows repeat the
two earlier leaves' maps and add the seven types of this leaf; one JNI row renames the oracle export. 8 FILE rows
`body_match_renamed=TRUE`, 50 VERBATIM rows. MERGE `pom.xml`: the profile `m3-optane-native` also passes
`-Dpmem.gram.native=true -Dpmem.corpus.selected.native=true -Dpmem.corpus.native=true`.

## Gates (2026-11-06, GATES.tsv)

| Gate | Result |
|---|---|
| 1_package_lint_werror | BUILD SUCCESS (exit 0); maven-compiler-plugin 3.13.0, release 21, -Xlint:all -Werror, 24 main sources (21 + 3) and 23 test sources (16 + 7); 0 compiler warnings; jar --describe-module: org.m3jdk.optane exports jdk.m3.optane, contains jdk.internal.m3.optane (not exported) |
| 2_tests_java_path | Tests run 52, Failures 0, Errors 0, Skipped 4 (the opt-in scale methods: pmem.gram.scale, pmem.corpus.selected.scale, pmem.corpus.scale and the source-image pmem.source.reuse.scale); the #2534 7 + 1 (replay 8a38db06cedd3cd6) and the #2586 31 (replay 37cce079598d15dc) with identical counts and digests; this leaf: M3PmemCorpusGramIndexJUnitTest 10 (1 skipped), M3PmemCorpusSelectedJUnitTest 5 (1), M3PmemCorpusSearchJUnitTest 5 (1), M3PmemGramIndexReplayTest 1 (M3_PMEM_GRAM_INDEX_REPLAY_PASS operations=2000 lines=2193 exact_equal=2025 refusals=289 sha256_16=f9277f62bf69ff7e) |
| 2b_tests_native_lanes | region-core classes 32 found, 32 successful (#2534 count); source-image classes 34 found, 33 successful, 1 skipped (#2586 count); gram-index classes 21 found, 18 successful, 3 skipped (scale), 0 failed, including realPosixJniMatchesFileKmpWhenExplicitlyRequired, originalFileAndActualPosixJniMatchWhenExplicitlyRequired and actualPosixJniAndFileKmpGiveTheSameOutputsWhenRequired over POSIX regions |
| 3_differential_file | donor develop 5ecfe052562c (17 owners: 10 region-core, 4 source-image, PmemCorpusGramIndex, PmemCorpusSelectedSearch, PmemCorpusSearch; javac --release 21 -Xlint:all -Werror) vs relocated over M3PmemGramIndexDump: 240-file image (UTF-8, random binary, repeated-prefix adversaries, dense 50,000-byte binaries, empty and tiny files), 10 build lanes, 145 query lanes, refusal, corpus and drift lanes, 10,000 seeded queries; 9,531 lanes gram == exact selected == naive search, 10 corpus lanes == naive, 0 UNEXPECTED; TOTAL operations=20602 exceptions=1277; sha256 c9ede0039c7df06eb8d424c965c57b420fa9b49e1905880df1c526e4855adfc4 both sides (1,319,225 bytes, 10,193 lines); 40 s for both families |
| 3b_differential_native | FILE and POSIX images, every exact result also checked against the independent C17 brute-force oracle (M3GramOracle.c via M3NativeByteOracle) for every selected file; 19,062 lanes gram == exact == naive == C17, 20 corpus lanes, 0 UNEXPECTED; TOTAL operations=41199 exceptions=2554; sha256 6d18144733a90c468b6d08fabf9235d53a83a23f5e5d671fba88d0fea96a205a both sides (2,638,423 bytes, 20,381 lines) under -Xcheck:jni; 88 s for both families |
| 4_native_matrix | both NATIVE_MATRIX PASS: gcc 13.3.0 -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror, PMDK not built, 14 SDK symbols and 1 oracle symbol each; native dumps (FILE + POSIX, 145 POSIX query lines, 852 gram == exact == naive == C17 lanes, 300 seeded queries per backend; TOTAL operations=2399 exceptions=174) identical across default, -Xint and -XX:TieredStopAtLevel=1 and between donor and relocated: sha256 50328d54b40eb943259ee527921596d7c276b2e70a9c599882701fc10f1cc707 (131,983 bytes, 981 lines) |
| 4b_mmap_1gb | real sparse 1,000,000,000-byte source.bin, 20,000 x 50,000-byte files, M3GRAM at offset 213 of every 11th file, manifest in the image format; the REAL M3PmemSourceImage verified all 20,000 digests; hits=1819 at file i*11 offset 213, pruned=18181, candidates=1819, indexBytes=427614, denseFiles=0, 400-file selection == exact selected search, maxHeap=134217728; FILE: 6.29 s, PEAK_RSS_KB=362636; POSIX: 7.80 s, PEAK_RSS_KB=1316612 (mapped pages counted) |
| 5_untouched_classes | 23 main class files (module-info included) and 7 test class files byte-identical; the 6 #2586 classes compile from #2586's unchanged commit; 6 new main class files in jdk.internal.m3.optane (the three owners and their Result classes) |
| 6_scope | 71 files, every path under m3/optane/ (one existing file edited: pom.xml, the MERGE row); the branch carries the #2586 commit unchanged below this one |
| recipe_idempotent | files=58 changed=0 provenance=UNCHANGED, also after jdk_naming apply; 8 FILE rows body_match_renamed=TRUE, 50 VERBATIM rows body_match=TRUE; the pmem-source-image recipe --check also changed=0 on this tree |
| jdk_naming_apply | JDK_NAMING moves=0 rewrites=0 refusals=0 absent=0; FIXED_POINT moves=0 rewrites=0 |
| chain_against_pin | 14 manifest rows of the seven crates: the selected search and its JUnit DEVELOP_EQUALS_POST; the gram index and its JUnit DEVELOP_SUPERSET_OF_POST of the three-gram crate (reusable-buffer reads, 2 tests); the admission rows end at the streaming-seal POST (not relocated, L029) |

## Files

| File | Purpose |
|---|---|
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the recipe (v3), its map and its pins |
| `differential.sh`, `native-matrix.sh` | gate 3 (donor vs relocated over the gram dump) and gate 4 (one family on Linux: C17 SDK library and C17 oracle, native dump in default/-Xint/C1) |
| `GATES.tsv`, `RECIPE_MEMORY.tsv`, `CHAIN.tsv` | the gate record, the lessons, the crate chain against the pin |
| `../../src/test/java/jdk/internal/m3/optane/M3PmemGramIndexDump.java`, `M3PmemGramIndexReplayTest.java`, `M3PmemGramOneGigabyteProof.java` | the reflective differential driver (not a test), the gate 2 wrapper and the 1 GB sparse proof over the real SDK (new) |
