# Loss-guard restore 2026-11-06 (M3jdk21 master)

Every file the superset landing rounds (2026-10-28 `master-landing-20261028`, 2026-10-30/31 `Superset 2026-10-3xx`,
2026-11-01j, 2026-11-06l and the `merge(superset): retain/fold #N head` API_FOLD merges) dropped from `master`, found
mechanically, classified, and the genuine losses restored on one branch with a ledger and re-run gates.

Master at classification time: `1351c11f402` (2026-11-06); branch base `351ed627035` (8 commits later, none of the
restored paths present there). Blobless clone `C:/m3-superset/conv/m3jdk21`; every step is a `git` tree or plumbing
command, no LLM judgement in the ledgers.

## The two proven losses

| | what | dropped by | restored from |
|---|---|---|---|
| A | `src/java.base/share/classes/jdk/internal/m3/hash` (16 classes, 5,506 LOC: M3XxHash, M3XXH3, M3WyHash, M3MetroHash, M3CityAndFarmHash_1_1, M3MurmurHash_3, the access and facade classes), `test/jdk/jdk/internal/m3/hash` (4 tests), `src/java.base/share/legal/zero-allocation-hashing.md`, the lane `m3/tooling/jdk-superset/hashing` (36 files; master kept only ATOMS_PROOF.json, HASH_PATTERN_MATRIX.tsv, PATTERNS_STRUCTURAL.tsv) — PRs #2232 (head 32f8e561b29) and #2248 (head 53167e25e67) | `afbe213266c9` Superset 2026-11-06l: #2187 5e5b3ec3ddc "(conflicts: superset side live, incoming side retained)" — the files were in the merge base and the incoming side, never on the landing line; the RETAIN resolver kept the live side (absent) and did not retain them either (retained-variants/pr2187 holds none of them) | `5e5b3ec3ddc` (the #2187 head, the newest copy in history), byte for byte: 57 paths |
| B | `M3LongRankIndex.topKFrequent`, `M3LongRankBatchNative.topKFrequent/topKFrequentJava/topKFrequent0`, JNI `Java_jdk_m3_collections_M3LongRankBatchNative_topKFrequent0`, `M3LongTopKFrequencyContract`, `M3LongTopKFrequencyTest`, `.github/workflows/m3-topk-frequency-jni-java21.yml`, `m3/collections/qualification/topk-frequency-jni-20261026/README.md` — commit 73c1e821af7 (2026-10-26) in PR #1882 | `d92ed3c9121` Superset: retain PR #1882 (RETAIN_MERGE_WHOLE_PR: the live tree stayed, 143 PR files kept under retained-variants/pr1882) | tests, workflow, README: the retained copies (already JDK-named) at their live paths; the three live files: master's version + the 73c1e821af7 hunks, JDK-naming rewritten (`tools/content_merge.py`, `patch -p1 -F3`; two fuzz placements of the first hunk corrected by anchor, see RESTORE_LEDGER.tsv) |

The rest of PR #1882's held content (71 members of M3LongRankBatchNative, 33 of M3LongRankIndex, 47 JNI functions
missing from the live files: TWIN_CONTENT.tsv) is NOT restored here: the user named topKFrequent; the whole-PR hold
is an owner decision (HELD_PRS.tsv).

## How the rest was found (CPU, two methods)

1. **Loss guard of every merged head (`tools/loss_scan.py`).** 905 merge commits on master's history since
   2026-10-26 and 97 heads named in landing messages give 498 distinct branch heads (all ancestors of master). For each:
   `base = merge-base(first parent, head)`; `git diff-tree -r --no-renames --raw base head` restricted to status A/M/T;
   every such path absent from master's tree is a candidate (59,703 rows, 32,170 unique paths). This is the tree
   comparison of `api_fold_branch.py --loss-guard` done locally (tree objects only, no blob fetch): the REST guard
   consumed 1,700 REST calls on the first head alone (master's tree differs from a 2026-10-31 head in thousands of
   directories) and was stopped at core 1,278/5,000 (`gh api rate_limit`); its tree cache stays in
   `C:/m3-superset/conv/m3jdk-cache`.
2. **Retained-variants twin scan (`tools/retained_scan.py`).** The 10,628 paths under
   `m3/qualification/master-landing-20261028/retained-variants/<label>/<rel>` on master (182 labels); 7,674 have no live
   twin `<rel>` (6,869 unique).

Union: 35,996 candidate paths (`LOSS_CANDIDATES.tsv.gz`, full; `LOSS_CANDIDATES_ACTIONABLE.tsv` = the RESTORE,
UNSURE and cited-DELIBERATE rows).

### Classification (`tools/classify.py`)

Where did each path's lineage end? Two deletion harvests over master's history since 2026-10-01, one per candidate
directory group (`tools/harvest_group.sh`; a single multi-pathspec run silently skipped merges, and rename detection
had to be off: a path dropped from the live tree while its retained copy was added shows as R100, not D):

* `DROPPERS_FP`: `git log --full-history --diff-merges=first-parent --no-renames --diff-filter=D` — the path was in the
  first parent and is absent from the commit.
* `DROPPERS_RAW_ALL`: the same with `-m` — the merges that left a parent's copy out.

| class | paths | rule |
|---|---|---|
| RETAINED_COPY | 5,584 | nested copies under retained-variants (derived data) |
| RENAMED | 10,679 | the JDK-naming target path (`m3/tooling/jdk-naming/jdk_naming.py target_path`) exists on master (5d454c3be9b) |
| MOVED | 1,593 | the same blob exists elsewhere on master |
| DUPLICATE | 3,307 | a post-rename twin (retained copies were renamed) of another candidate row |
| DELIBERATE | 14,674 | 14,632: the earliest excluding merge is a `Superset: retain PR #N` of pattern RETAIN_MERGE_WHOLE_PR (130 PRs, HELD_PRS.tsv) — by the landing's own statement "the live tree stays the landing tree ... Promotion to live code is a reviewed follow-up"; 42: a first-parent deletion or a PR-side deletion, cited per row |
| RESTORE | 155 | a first-parent deletion by a merge that held the path in both parents or dropped a superset-only addition (6), or a non-whole-PR merge that left a second parent's copy out (149): the RETAIN resolver drops |
| UNSURE | 4 | `SOURCE_SHA256.tsv` (the per-PR retain ledgers; there is no live twin by design) and 3 `RECEIPT.json` whose only excluding commit holds them in no parent (e2f10f42ebf3, a custody freeze) |

Dropper merges of the 155 RESTORE paths: `afbe213266c9` (57, loss A), `0077a2b535f0` retain #2391 (27: #2219's
array-map-fill lane), `87effb078168` 2026-11-06l #2359 (22: #2390's case-liang receiver, additive-open-pr-superset
census, M3LongHashSetAddAtomTest, workflows), `5ab20eae55f9` retain #2467 (22: receiver-transitions, nlp-search
receiver-history), `da44c2ee7e9a` retain PR #1733 (12: live files dropped by a whole-PR retain, the #1733 recipe and
facades), `082b54fa4ab7` retain PR #1368 (8: live collection tests dropped), `519b4ba1df34` 2026-10-31g (5:
sanitize-20261030 reports), `c9a1d41baaf5` merge #2216 (RENAME_PLAN_SUMMARY.tsv), `f9c7ec048e4e` (1 RECEIPT.json).

### Held PRs (not restored, owner list)

`HELD_PRS.tsv`: 130 PRs whose files the 2026-10-28/30 landings kept out of the live tree on purpose, 14,632 paths.
6,328 have a retained copy under retained-variants/pr<N>; **8,304 have none** — they were inherited by the held PR
from a stacked base (the develop line) and the whole-PR retain neither landed nor retained them. The biggest:
#2147 (8,297: the vendored `synexia-openrewrite-recipes/recipe-crates/public-fork-recipe-custody-20261017` crate,
first added on develop by a98213b1487 / #1806 / #1772 / #1790), #1637 (2,261), #1616 (911), #1357 (492), #1368 (313).
Promotion of any of them is the "reviewed follow-up" the landing named.

### Content-level twins (owner list)

`TWIN_CONTENT.tsv` (`tools/twin_scan.py`): of the 1,580 source twins (java/c/h/cpp/py/sh/yml) that exist both live and
under retained-variants, 290 retained copies declare members the live file does not (LSEM declaration diff, no
verdict). The file-level ledger cannot see these; loss B is one of them (and the only one restored here).

## Restoration

* `tools/restore.py`: each RESTORE row's source blob (`<dropper parent>:<path> blob <sha>`, the newest copy in
  history) written into a sparse worktree through one `git cat-file --batch`, sha1 re-verified, additive only.
  `RESTORE_LEDGER.tsv`: 155 rows + 7 rows of loss B (path, source, blob sha, bytes); 2,331,094 bytes.
* JDK naming: 49 restored files still carried pre-rename names (`com/m3/...`, `com/synexia/...`, `m3-jdk21-...`);
  `jdk_naming.py apply --tree --only <restored paths>` moved 31 and rewrote 48 (NAMING_APPLY.tsv), fixed point
  confirmed. Nothing else was edited.
* Branch `m3/loss-guard-restore-20261106a` from `351ed627035`: commit 1 the 162 paths, commit 2 the naming, commit 3
  this directory.

## Gates (JDK 21.0.9, stock; GATES.tsv)

* Hash lane: oracle 60 classes; `--patch-module java.base` compile 42 classes, 0 warnings under java.base's flags;
  M3HashAtomsTest + M3HashJitAtomsTest 35/35 with the prebuilt oracle and 35/35 from test.src.
* topK: workflow compile PASS; `PASS M3_LONG_TOPK_FREQUENCY checks=18561 nativeRequired=false`;
  JNI library built (gcc, win32 headers), `checks=23198 nativeRequired=true`, -Xint identical; JUnit 1/1.
* case-liang receiver (#2390): compile PASS; `M3_CASE_LIANG_PARITY_PASS checks=1698185`,
  `M3_CASE_LIANG_JNI_PASS checks=133436` (JIT and -Xint); the script's own sha256 pins drift because the restored
  files were JDK-named.
* array-map-fill lane (#2219): `run.sh` BUILD SUCCESS, 48 tests, 0 failures; generation byte-identical.
* Collections: master's module compiles (623 classes) except the restored `M3ApiEntryFacades.java:28` (API drift since
  #1733); 8 of 9 restored tests compile (`M3IndexPathMasksLongSelfTest` 11 errors, API drift since #1368); JUnit 7/8
  (M3FilteredNavigationBoundaryTest.liveOwnersAndQueryValidation fails: "empty mutable natural null query failed to
  refuse"); self-tests: DeepReadOnly PASS checks=84, FilteredNavigationBoundary and ReadOnlyViewBoundary FAIL
  ("pollFirstEntry accepted a mutation"). These tests were live on the landing tree before the #1368 retain merge
  dropped them; their failures are contract drift on master, reported, not patched.
* Untested by nature: workflows, ledgers, docs, the #1733 recipe sources (no pom was ever on master).

## Open

* `M3ApiEntryFacades.java` and `M3IndexPathMasksLong{Self,}Test.java` do not compile against today's master; two
  restored self-tests and one JUnit method fail: owner decision (adapt, or drop with a cited commit).
* The restored topK workflow pins the PR's blob ids; it stays red until re-pinned to this branch's files.
* 130 held PRs (14,632 paths, 8,304 of them retained nowhere) and 290 content-level twins await promotion decisions.
* Pre-existing master-red checks (java21, naming, profile-custody, public-names; integrated-opencl and source-names on
  some PRs) are not addressed here.

## Reproduce

```
git -C C:/m3-superset/conv/m3jdk21 fetch -q origin master
python -I tools/loss_scan.py <repo> origin/master MERGES.txt HEADS_MSG.txt MASTER_PATHS.txt LOSS_RAW.tsv
python -I tools/retained_scan.py RETAINED_PATHS.txt MASTER_PATHS.txt RETAINED_ORPHANS.tsv
awk '{print NR" "$0}' GROUPS.txt | xargs -P 6 -L 1 bash tools/harvest_group.sh     # DROP_M/, DROP_FP/
python -I tools/classify.py                                                        # LOSS_CANDIDATES.tsv
python -I tools/restore.py <repo> LOSS_CANDIDATES.tsv <worktree> RESTORE_LEDGER.tsv
bash tools/gates_wt.sh; bash tools/gates2.sh                                       # gateWT/*.log
```
