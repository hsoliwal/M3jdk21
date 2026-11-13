# Retained-variant twins, core lanes (2026-11-06)

Promotion-backlog leaf of the loss-guard restore (#2517, `make/m3/qualification/loss-guard-restore-20261106/TWIN_CONTENT.tsv`):
the 290 retained source twins under `m3/qualification/master-landing-20261028/retained-variants/<label>/<path>` that
declare members their live `<path>` lacks. This leaf takes the 173 rows whose live path is under `m3/` outside
`make/m3/tooling`, `m3/search` and `text-tools` (collections, arrays, file, fs, core, ast, eclipse, ports, compatibility,
m3-import, release, runtime-integration, migration, lexicon, the root `verify-gvas-literal-precompute.py`); the
OpenJDK-tree rows (`src/java.base`, `test/jdk`, `src/jdk.compiler`, including `jdk/internal/m3` hash and mindex) and the
tooling/search/text/workflow rows are the other session's. The verdict names and output shape are the lane owner's
(`retained_audit.py`, `three_way_restore.py` in `S:/m3-scratch/converge-20261106`) so the two halves join.

Audited master: `4072669632` (the ROWS.tsv blob ids). Branch rebased onto `e65f0f664fa`; none of the 34 live
restored paths nor the 7 held paths changed on master in between (`git diff --name-only`). Four audited live paths did
change, all CONFLICT rows only (13 rows): `M3LongRankBatchNative.java`, `M3LongRankIndex.java`,
`m3_long_rank_batch_jni.c` (the #2517 topKFrequent restore) and `m3_index_mask_compactor.c`; re-run `restore3.py`
against current master before resolving those.

## Counts

RETAINED_AUDIT (173 rows): DIVERGENT 154, RETAINED_SUPERSET 18, DEFERRED_TO_63 1, SAME 0, LIVE_SUPERSET 0.

RETAINED_3WAY (172 rows, 91 live paths): RETAINED_COPY 15, 3WAY_CLEAN 31, 3WAY_CLEAN=live 4 (the merge output equals the
running content: nothing to restore), COVERED 5 (every retained line already present), CONFLICT 117 (58 paths).
41 paths restored; 7 of them (8 rows) failed their gate and are held; **34 files live** (15 retained copies, 23 clean
3-way rows).

| lane | rows | RETAINED_SUPERSET | DIVERGENT | DEFERRED_TO_63 | RETAINED_COPY_LIVE | 3WAY_CLEAN_LIVE | 3WAY_CLEAN_HELD | 3WAY_CLEAN=live | COVERED | CONFLICT | live files |
|---|---|---|---|---|---|---|---|---|---|---|---|
| m3/ast | 4 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | 0 | 0 | 4 |
| m3/collections | 87 | 9 | 78 | 0 | 7 | 8 | 2 | 4 | 2 | 64 | 12 |
| m3/collections/interop | 17 | 0 | 17 | 0 | 0 | 0 | 2 | 0 | 0 | 15 | 0 |
| make/m3/collections/qualification | 2 | 0 | 2 | 0 | 0 | 1 | 0 | 0 | 0 | 1 | 1 |
| make/m3/collections/verification | 1 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 |
| make/m3/compatibility | 6 | 0 | 6 | 0 | 0 | 0 | 1 | 0 | 0 | 5 | 0 |
| m3/core | 6 | 0 | 6 | 0 | 0 | 0 | 3 | 0 | 0 | 3 | 0 |
| m3/eclipse/core-runtime | 2 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | 0 | 0 | 2 |
| m3/eclipse/framework | 4 | 1 | 3 | 0 | 1 | 2 | 0 | 0 | 0 | 1 | 3 |
| m3/eclipse/optional-jmod | 1 | 0 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 |
| m3/file | 14 | 2 | 12 | 0 | 1 | 2 | 0 | 0 | 1 | 10 | 2 |
| m3/fs | 6 | 2 | 4 | 0 | 2 | 2 | 0 | 0 | 0 | 2 | 4 |
| m3/lexicon | 1 | 0 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 |
| m3/m3-import | 5 | 0 | 5 | 0 | 0 | 0 | 0 | 0 | 0 | 5 | 0 |
| make/m3/migration | 1 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 |
| m3/ports/collections | 1 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| m3/ports/compiletime | 3 | 1 | 2 | 0 | 1 | 2 | 0 | 0 | 0 | 0 | 3 |
| m3/ports/swing-viewport | 2 | 0 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 0 |
| make/m3/qualification | 1 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 |
| make/m3/release | 4 | 0 | 4 | 0 | 0 | 0 | 0 | 0 | 0 | 4 | 0 |
| make/m3/runtime-integration | 2 | 0 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 0 |
| make/m3/verify-gvas-literal-precompute.py | 3 | 0 | 3 | 0 | 0 | 1 | 0 | 0 | 2 | 0 | 1 |
| **total** | **173** | **18** | **154** | **1** | **15** | **23** | **8** | **4** | **5** | **117** | **34** |

## Method (recipe-first, CPU only)

1. `ROWS.tsv` - the 173 rows with the live and retained blob ids on the audited master (`git ls-tree`, no blob walk).
2. `audit.py` - RETAINED_AUDIT: line-multiset containment of the two blobs (one `git cat-file --batch`, sha1
   re-verified); member delta both ways with the twin_scan.py LSEM regexes; `DEFER.tsv` maps rows another in-flight leaf
   owns to a verdict that keeps the blob ids. Output `AUDIT.tsv`.
3. `PR_SOURCES.tsv` - per label `pr<N>`: the `Superset: retain PR #N` merge on master (65 labels; #2357 was merged as a PR,
   its merge commit is used), its first parent, T = the retained head, and `git merge-base <first parent> T`. All 66 T heads equal
   `gh pr view N --json headRefOid`.
4. `restore3.py` - per live path, labels in ascending PR order against the running content: RETAINED_SUPERSET ->
   live := retained copy; DIVERGENT -> `git merge-file -p --diff3` live / base / retained, with base = the T-tree entry
   whose `jdk_naming.target_path` is the live path, read at the merge-base and put through `jdk_naming.rewrite_text`
   (the retained copies were JDK-named); CLEAN -> result, CONFLICT -> `CONFLICTS.tsv` row and
   `conflicts/<label>/<path>.diff3` (both hunks and the base), never resolved. `--apply` writes only over the sha256 pin
   of the audited blob and skips `HELD.tsv` paths; a second run prints `ALREADY` for every path (idempotent).
5. `jdk_naming.py apply --only <each restored path>`: 0 moves, 0 rewrites, FIXED_POINT (`NAMING_APPLY.tsv`).
6. Gates per module (`GATES.tsv`); a restored file that does not compile or that changes the module's test result
   against master goes through `hold.py`: the bytes move to `held/<path>`, the live file returns to master, a
   `HELD.tsv` row records the gate and the reason.
7. `held_sequence.py` - `HELD_PRS_SEQUENCE.tsv` (below).

## Held (owner decides: adapt, or drop with a cited commit)

| path | rows | gate | why |
|---|---|---|---|
| `m3/collections/src/main/java/jdk/m3/collections/M3MaskViewportNative.java` | #1766 3-way | collections mvn package | `projectFramePage(...)` is already defined: the clean merge added a second declaration of a member master holds at another position |
| `m3/collections/src/test/java/jdk/m3/collections/M3FilteredSetNullContract.java` | #2115 3-way | collections mvn test | the restored `boundedCursorExtremeProbes` fails against master's main ("forward cursor uses its stored last boundary expected=[last] actual=[isEmpty, first]"); master's contract passes with every restored main file; proven by three isolated runs |
| `m3/collections/interop/qualification/current-census-20261017/test_verify.py` | #1808 3-way | pytest | master 6 passed; restored 3 failed / 6 passed: three new successor tests refuse today's census |
| `m3/collections/interop/qualification/current-census-superset-20261022/test_compound_var_successor.py` | #1847 3-way | pytest | 1 failed / 4 passed on both sides, but a different test fails: not the baseline set |
| `make/m3/compatibility/test_public_names.py` | #1908 3-way | pytest | 10 failed / 34 passed vs master 1 / 33: the restored `test_unreviewed_evidence_dates_and_versions_remain_refused` refuses 9 of today's evidence files |
| `m3/core/src/jdk/m3/text/SharedLexiconCatalog.java` | #1769 3-way | javac | `mappingsByIdentity.values()`: master's field is `SourceMapping[]`, the hunks expect a Map (API drift) |
| `m3/core/src/jdk/m3/text/SharedLexiconPrecomputeCatalog.java` | #1538, #1637 3-way chain | javac | `constructor Builder() is already defined`: the chain added a second no-arg constructor |

## Deferred

`m3/ports/collections/src/main/java/jdk/m3/util/M3PrimitiveIndexView.java` (#1608, live c28488290b05, retained
706c3e545c7c): DEFERRED_TO_63, CONSOLIDATE_PORT_OWNERS v1 (#2532, 2a1a31a4eebf) moved the `jdk.m3.util` / `jdk.m3.ds`
owners into `m3/collections`; the twin is that session's. No other row of this leaf has a live or retained type in
`jdk.m3.util` / `jdk.m3.ds`.

## Conflicts (owner list)

117 rows on 58 paths, `CONFLICTS.tsv` (label, path, base commit, T, base path at T, conflict count, diff3 file,
retained-only and live-only line counts). 13 rows have no base (the PR added the file; no common ancestor), the rest
are true divergences of master and the held PR since the merge-base. The largest: `m3_index_mask_compactor.c` (10 PRs, 49 hunks),
`M3FilteredViews.java` (6), `M3File.java` (6), `verify_superset.py` (5), `M3MutableFilteredViews.java` (5), `check_public_names.py` and
`test_public_names.py` (3 + 2), `verify_release_admission.py` / `test_release_admission.py` (#2116, #2120).

## HELD_PRS_SEQUENCE.tsv

The 130 whole-PR retains (HELD_PRS.tsv, 14,632 held paths) ordered for one-PR promotion rounds: 4 PRs with no live
twin first (#2234, #1878, #1892, #1816: promote by copy), then 125 with live twins, fewest promotion paths first
(promote through RETAINED_AUDIT / RETAINED_3WAY), then #2147 as DEDICATED_PASS (8,297 held paths, 8,307 inherited and not
retained: the vendored public-fork-recipe-custody crate). Per PR: promotion paths (retained + inherited), held paths,
retained paths (label ledgers excluded), inherited-not-retained, live twins, TWIN_CONTENT rows, modules, title.

## Gates

| module | gate | verdict |
|---|---|---|
| m3/collections | mvn install (package) | BUILD SUCCESS after 1 hold |
| m3/collections | mvn test: 1660 testcases, class/method/status set vs master e65f0f664fa | identical (1604 PASS, 5 FAILURE, 49 JNI ERROR, 2 SKIPPED: the Windows baseline) after 1 hold |
| m3/collections/interop | pytest of the 2 restored Python tests | both change the baseline set -> held |
| m3/file | mvn test -Dexec.skip=true: 9 testcases | identical to master (1 pre-existing failure) |
| m3/fs | mvn compile red on master (M3FS.java:661, not a twin); twin javac of the 4 restored files vs master's | no new error kind |
| m3/ast | mvn package; the 2 restored main-based contracts | BUILD SUCCESS; ORIGINS checks=44 (master 15), TREE checks=21 (master 13), PASS |
| m3/eclipse/framework | mvn test | 21/21 (master 11/11 kept, +10 green) |
| m3/eclipse/core-runtime | mvn test | 37/37 (master 26/26 kept, +11 green) |
| m3/eclipse/optional-jmod, make/m3/collections/qualification, root verify-gvas | py_compile (+ the caller's pytest for verify-gvas, = baseline) | PASS |
| m3/ports/compiletime | mvn red on master (needs the M3 java.base); twin javac of the 3 restored files | identical error multiset |
| m3/core | javac of the 2 restored files | FAIL -> both held |
| m3/lexicon/native | gcc -Wall -Wextra -shared of the restored JNI file | 0 warnings |
| make/m3/compatibility | pytest test_public_names.py | FAIL -> held |
| all | jdk_naming.py apply --only; restore3.py --apply twice | fixed point; idempotent |

Rows, logs and the exact commands: `GATES.tsv`.

## Reproduce

```
B=make/m3/qualification/retained-twins-core-lanes-20261106
python -I $B/audit.py <clone> $B/ROWS.tsv $B/AUDIT.tsv --blobs <cache>
python -I $B/restore3.py <clone> $B/AUDIT.tsv $B/PR_SOURCES.tsv <out> --blobs <cache> --apply <worktree>
python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --only <each restored path> --tsv $B/NAMING_APPLY.tsv
python -I $B/hold.py <worktree> $B <path> <gate> <reason>
python -I $B/held_sequence.py HELD_PRS.tsv LOSS_CANDIDATES.tsv MASTER_PATHS.txt TWIN_CONTENT.tsv PR_TITLES.tsv $B/HELD_PRS_SEQUENCE.tsv
```
