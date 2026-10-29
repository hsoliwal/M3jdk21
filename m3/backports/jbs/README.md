# JBS enhancements of JDK 22-27: backport scaffold for M3JDK21

Status: **SCAFFOLD** (2026-10-20). The JEP side is complete in `../JEP_CATALOGUE.tsv` and `../ALL_JEP_MATRIX.md`
(85 feature JEPs). This folder adds the rest of JBS: every fixed **Enhancement** of JDK 22 to 27, so that
M3JDK21 can take what is possible beyond the JEPs.

| File | Content |
| --- | --- |
| `JBS_ENHANCEMENTS_22_27.tsv` | 5,299 issues: release, key, component, subcomponent, priority, labels, summary, disposition |
| `JDK21U_BACKPORTED_KEYS.txt` | 3,552 main-issue keys that a fixed 21.0.x Backport issue links to |
| `ECLIPSE_JAVA22_API_DEMAND.tsv` | Java 22+ APIs the Eclipse forks call (from their `--release 21` gate residue): replace in Eclipse or backport here |
| `jbs_enhancements.sh` | pass 1: the JBS query, one row per issue (public JIRA REST API, no login) |
| `jbs_mark_21u.sh` | pass 2: `ALREADY_IN_JDK21U` when jdk21u already carries the issue |

| Release | Fixed enhancements | Already in jdk21u | To classify |
| --- | --- | --- | --- |
| 22 | 805 | 258 | 547 |
| 23 | 885 | 149 | 736 |
| 24 | 876 | 115 | 761 |
| 25 | 901 | 98 | 803 |
| 26 | 1010 | 76 | 934 |
| 27 | 822 | 37 | 785 |

## Fill (next passes, mechanical first)

1. Component lanes: map component/subcomponent to the owner lanes of `../POST21_PRIORITY_COMPATIBILITY.tsv`.
2. Dependency pass: an issue whose code builds on a JEP marked `reject-language` or `hold-*` in
   `../JEP_CATALOGUE.tsv` inherits that disposition.
3. Additive pass: issues that only add files, tools, jcmd/JFR options or API methods (the `candidate-adapted`
   pattern of `../UPSTREAM_CHANGE_SEEDS.tsv`) become seeds there, with the upstream commit pinned.
4. Everything admitted goes through the existing backport DAG (`../BACKPORT_RECIPE_DAG.md`), never around it.

Language features stay out: M3JDK21 is Java 21 and javac source acceptance is locked (`reject-language`). The
Eclipse forks are changed to Java 21 source instead (branch `m3/jdk21-compat-20261020` in each fork).

## Full backlog, classification and landing (2026-10-29)

User directive (2026-10-29): M3jdk21 should contain every RFE, bug fix, JEP and JBS issue of JDK 22 to 27, and the
features OpenJDK withdrew. Everything below is mechanical (JBS REST snapshot, git history, `git apply --check`); no row
was decided by reading.

| File | Content |
| --- | --- |
| `BACKLOG_22_27.tsv` | 15,231 rows: every issue fixed in 22..27 (14,669, all types but Backport/CSR) and every JEP in every state (562). Columns jbs_id, type, fix_version, component, subcomponent, title, upstream_commit, in_jdk21u, area, state |
| `CLASSIFY_22_27.tsv`, `CLASS_COUNTS.tsv` | one class per issue from the standalone apply check of each upstream commit on master `3569c790d5c` |
| `BATCHES.tsv`, `CSR_LANE.tsv` | cherry-pick batches of at most 50 commits, oldest first, one area per batch; CSR-linked issues held in their own lane |
| `JDK21U_MERGE_PLAN.tsv` | merge-tree dry run of jdk21u-dev tag `jdk-21.0.14+0` into master: 29 conflicting files and a rule per file |
| `VETOED_JEPS.tsv` | the 77 JEPs closed Withdrawn (75) or Rejected (2), with the JBS comment that closed them, openjdk/jdk commits naming them or their sibling JEPs, and candidate branches in the OpenJDK project repos |
| `UPSTREAM_IDS_OUTSIDE_22_27.tsv` | 2,000 JBS ids in upstream commits whose issue is not fixed in 22..27 (mostly JDK 28 work on master) |
| `FOSS_REUSE_DECISION.tsv` | donor decision: openjdk/jdk and openjdk/jdk21u-dev, same licence (GPL-2.0 WITH Classpath-exception-2.0), direct import with upstream authorship |
| `SOURCES.sha256` | hashes of every input snapshot (JBS pages, JEP 0 page, ls-remote lists, commit lists) |

Issues per fix version (first 22..27 fix version; JEP rows excluded):

| Type | 22 | 23 | 24 | 25 | 26 | 27 | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Bug | 1,290 | 1,234 | 1,349 | 1,462 | 1,418 | 1,262 | 8,015 |
| Enhancement | 805 | 885 | 876 | 901 | 1,010 | 822 | 5,299 |
| Sub-task | 207 | 172 | 292 | 214 | 148 | 191 | 1,224 |
| Task | 25 | 27 | 16 | 32 | 16 | 9 | 125 |
| New Feature / JEP Task | 0 | 3 | 3 | 0 | 0 | 0 | 6 |
| **All** | **2,327** | **2,321** | **2,536** | **2,609** | **2,592** | **2,284** | **14,669** |
| already in jdk21u-dev | 915 | 729 | 588 | 482 | 345 | 169 | 3,228 |

Classes (issues; area from the union of the upstream commits' paths):

| Class | java-only | test-only | build | native | hotspot | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| IN_JDK21U (arrives with the jdk21u merge) | 477 | 1,646 | 167 | 178 | 760 | 3,228 |
| CLEAN (every commit applies to master) | 961 | 331 | 126 | 126 | 945 | 2,489 |
| CLEAN_EXCEPT_COPYRIGHT (applies once header-year hunks are dropped) | 231 | 46 | 14 | 22 | 55 | 368 |
| CONFLICT_HEADER_HUNK (first failing hunk in lines 1-5) | 432 | 199 | 74 | 75 | 290 | 1,070 |
| CONFLICT_MISSING_FILE (needs an earlier upstream change) | 445 | 562 | 60 | 26 | 555 | 1,648 |
| CONFLICT_CONTEXT | 1,352 | 576 | 370 | 138 | 3,200 | 5,636 |
| NO_UPSTREAM_COMMIT in openjdk/jdk | | | | | | 230 |

Of the 2,489 CLEAN issues, 210 are CSR-linked (spec or behaviour change) and wait in `CSR_LANE.tsv`; the other 2,279
(2,301 commits) are planned as batches 001..048. Batch 001 (50 java-only commits) is landed as a draft PR with its
receipt under `batches/001/`.

Landing order: (1) merge jdk21u-dev tag history (`JDK21U_MERGE_PLAN.tsv`, owner: master landing); (2) batches 002..016
java-only, gate `gate_javac.sh` (javac --release 21 --patch-module, baseline-aware, sentinel errors fail); (3) test-only,
build, native, hotspot batches (HotSpot and native builds NOT_RUN on the Windows host, so those need the hosted build);
(4) CLEAN_EXCEPT_COPYRIGHT with `drop_copyright_hunks.py`; (5) CONFLICT_MISSING_FILE in dependency order, re-running
`apply_check.sh` after each landed batch; (6) the CSR lane and the vetoed JEPs by reviewed decision.

Tools (atoms): `fetch_openjdk.sh`, `jbs_snapshot.sh`, `jbs_pages_to_tsv.py`, `build_backlog.py`, `need_blobs.py`,
`fetch_blobs.sh`, `apply_check.sh`, `drop_copyright_hunks.py`, `recheck_copyright.sh`, `classify.py`,
`plan_batches.py`, `pick_batch.sh` (cherry-pick -x by plumbing), `gate_identity.sh`, `gate_javac.sh`,
`commit_files.sh`, `vetoed_jeps.py`, `jep_reasons.sh`.
