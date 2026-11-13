<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# recipe pmem-task-log-replay-20261106: the PMem task log and replay relocated into jdk.m3.optane

Leaf `pmem-task-log-replay` (plan `C:/m3-superset/conv/plans/fs-pmem.tsv` rank 5, after rank 2 `pmem-region-core`, #2534)
of the M3JDK convergence programme: the append-only PMem receipt log and its read-only replay verifier of
`hsoliwal/com.synexia` `m3-pmem-sdk` become part of `m3/optane` (module `org.m3jdk.optane`, exported package
`jdk.m3.optane`) by recipe, never by hand. The log stores identities and a commit marker only; the replay returns the next
ordinal and never a promotion authority (the ChromeML bridge and the campaign orchestration stay in Synexia).

## What was relocated

| Donor | M3JDK (`m3/optane/`) | Role |
|---|---|---|
| `m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemTaskLog.java` | `src/main/java/jdk/m3/optane/M3PmemTaskLog.java` | single-writer 144-byte receipts over a caller-owned READ_WRITE region: body and zero marker persisted, then the commit marker; `open` verifies every committed receipt and detects an interrupted append tail; a failed append requires a reopen |
| `.../PmemTaskReplay.java` | `.../M3PmemTaskReplay.java` | fail-closed reconciliation against the reviewed task/source sequence and the content-addressed candidate store |
| `src/test/java/com/synexia/m3/pmem/PmemTaskLogSmokeTest.java`, `PmemTaskReplaySmokeTest.java` | `src/test/java/jdk/m3/optane/M3PmemTask{Log,Replay}SmokeTest.java` | the donor's no-native oracle mains (20,000 receipts; 20,000 durable-receipt replay simulations with an interrupted append tail above 2 GiB), compiled in their package as module tests and run by `M3PmemTaskLogReplayTest` |
| `docs/TASK_LOG_CONTRACT.md`, `TASK_REPLAY_CONTRACT.md` | `docs/` | the contracts |
| crate `m3-pmem-task-replay-20261102` (README, RECIPE_MEMORY, the 2026-11-03 resident-file qualification record) | `task-replay/` | the donor recipe record (record VERBATIM) |

Not relocated (`RELOCATION_MAP.tsv` SKIP/DROP rows): `PmemFileReplayIsolationTest` (named by the plan, but its subject is
`PmemFileCampaign` with `com.synexia.ai.m3.M3LlmInvocationLedger`; it never calls the log or the replay; the campaign is
Synexia-private by the plan and `SOURCE_CAMPAIGN.md` item 10), the ChromeML bridge and its qualification smoke, the
crate's OpenRewrite recipe test and build, and the donor scripts `test-task-log.sh` / `test-task-replay.sh` (replaced by
the JUnit wrapper and the gates below).

## Donor pin and lineage

| Item | Value |
|---|---|
| donor revision | `hsoliwal/com.synexia` develop `5ecfe052562cdd1a7a6570068654c7f813fd0f47` (2026-11-06, merge of #16754), the one pin of the three pmem follow-up leaves, after #16749 (`36827ee8a798`) and #16750 (merge `061f544acfc8`, recipe tables only); every FILE row is develop's own blob (`PROVENANCE.tsv` `base_kind=DEVELOP`) |
| family commits | `f50ffc5caa1` 2026-11-02 source-sealed task replay (draft PR #15292 head `0b39214577e`), `4d7f684830f` 2026-11-03 resident 20k x 50 KiB replay qualification |
| smoke mains | they declare package `com.synexia.m3.pmem` and fail in the donor's `scripts/build-and-test.sh` unnamed-module compile on develop (pre-existing); here they compile in their package (`jdk.m3.optane`) on the test classpath |
| Java level | the donor's local proof ran a Java 17 toolchain; every gate below is Java 21 |

## Recipe

`python -I make/m3/optane/recipe/pmem-task-log-replay-20261106/relocate.py --donor-git <clone> --donor-rev 5ecfe052562c [--check]`
(`RELOCATE_PMEM_TASK_LOG_REPLAY` v3: the same v3 as `recipe/pmem-source-image-20261106`, derived from
`pmem-region-core-20261106/relocate.py` v2 with VERBATIM and MERGE rows added). TYPE rows repeat the region-core map and
add `PmemTaskLog`, `PmemTaskReplay` and the two smoke mains. 8 FILE rows `body_match_renamed=TRUE`, 1 VERBATIM row. No
existing module file is edited (no MERGE row).

## Gates (2026-11-06, GATES.tsv)

| Gate | Result |
|---|---|
| 1_package_lint_werror | BUILD SUCCESS (exit 0); maven-compiler-plugin 3.13.0, release 21, -Xlint:all -Werror, 19 main sources (17 + 2) and 10 test sources (6 + 4); 0 compiler warnings |
| 2_tests_java_path | Tests run 11, Failures 0, Errors 0, Skipped 0: M3PmemFileBackendJUnitTest 7 and M3PmemFileReplayTest 1 (the #2534 counts; M3_PMEM_FILE_REPLAY_PASS operations=10000 lines=10376 refusals=2235 sha256_16=8a38db06cedd3cd6, the #2534 digest), M3PmemTaskLogReplayTest 3: PMEM_TASK_LOG_SMOKE_PASS records=20000, PMEM_TASK_REPLAY_SMOKE_PASS records=20000 (the donor's scripts/test-task-log.sh and test-task-replay.sh oracles: 20,000 durable-receipt simulations with an interrupted append tail above 2 GiB), M3_PMEM_TASK_LOG_REPLAY_PASS operations=10000 lines=10112 refusals=2563 sha256_16=20aaa73fa9cfcc5f |
| 2b_tests_native_lanes | region-core classes: 32 tests found, 32 successful (the #2534 count); M3PmemTaskLogReplayTest: 3 found, 3 successful (both smoke mains PASS records=20000; M3_PMEM_TASK_LOG_REPLAY_PASS ... sha256_16=20aaa73fa9cfcc5f, the Windows digest) |
| 3_differential_file | donor develop 5ecfe052562c (12 owners: the 10 region-core owners + PmemTaskLog, PmemTaskReplay; javac --release 21 -Xlint:all -Werror) vs relocated target/classes over M3PmemTaskLogDump (memory region above 3 GiB with failure injection, FILE regions): open refusals, appends and capacity, 14 corruption offsets per region kind, beyond-gap and zero-marker tails, 4 interrupted-append steps, progress cancellation, 9 replay refusal lanes and 5 drift positions, 50,000 seeded operations; TOTAL operations=8391824 exceptions=24257; sha256 78bb588d0d0faae5dd0637b4e8dd5e8436312514dc415abb23e1a7bfb13a3183 both sides (8,152,475 bytes, 50,112 lines), identical on a second run; 154 s for both families (100,000 operations took about 170 s per family: RECIPE_MEMORY) |
| 3b_differential_native | memory, FILE, POSIX and sparse FILE/POSIX regions at a file offset of 2 GiB + 4096; TOTAL operations=8421011 exceptions=21244; sha256 51c738447c3b9a8245e5f5e01b1569e1acce6e3613da01933a5eb937bafab9a0 both sides (9,122,933 bytes, 50,214 lines) under -Xcheck:jni; 115 s for both families |
| 4_native_matrix | both NATIVE_MATRIX PASS: gcc 13.3.0 -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror, PMDK not built, 14 exported symbols each; native dumps (every lane over memory, FILE, POSIX and sparse >2 GiB regions, 4 interrupted-append lanes, 2,000 seeded operations; TOTAL operations=47992 exceptions=613) identical across default, -Xint and -XX:TieredStopAtLevel=1 and between donor and relocated: sha256 bd167fb654f0af9deb83f6b897d217e1121d4a92fd7ce9c7075c872023a28472 (516,055 bytes, 2,214 lines) |
| 5_untouched_classes | 23 main class files (module-info included) and 7 test class files byte-identical; 10 new main class files (M3PmemTaskLog and its Status, Receipt, ProgressMonitor; M3PmemTaskReplay and its Expected, ContentReader, ProgressMonitor, Replay, switch map) |
| 6_scope | 19 files, every path under m3/optane/ (no existing file edited, no MERGE row) |
| recipe_idempotent | files=9 changed=0 provenance=UNCHANGED, also after jdk_naming apply; 8 FILE rows body_match_renamed=TRUE, 1 VERBATIM row body_match=TRUE |
| jdk_naming_apply | JDK_NAMING moves=0 rewrites=0 refusals=0 absent=0; FIXED_POINT moves=0 rewrites=0 (the first apply rewrote the dump Javadoc, PKG_M3=1: it spelled the donor package; it now names the map) |

## Files

| File | Purpose |
|---|---|
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the recipe (v3), its map and its pins |
| `differential.sh`, `native-matrix.sh` | gate 3 (donor vs relocated over the dump) and gate 4 (one family on Linux: C17 library, native dump in default/-Xint/C1) |
| `GATES.tsv`, `RECIPE_MEMORY.tsv` | the gate record and the lessons |
| `../../src/test/java/jdk/m3/optane/M3PmemTaskLogDump.java`, `M3PmemTaskLogReplayTest.java` | the reflective differential driver (memory, FILE, POSIX and sparse >2 GiB regions; failure injection at the four append steps; not a test) and the gate 2 wrapper (new) |
