<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/collections at the JDK naming fixed point (recipe JDK_NAMING_RENAME, 2026-11-06)

Base: `master@351ed627035c13dea148b409809e16196c2c3e8a` (merge of #2498). The absorb-primitives waves #2494 and #2496
landed ten sources under `m3/collections/src/main/java/jdk/m3/collections/` whose lineage Javadoc still names the
donor path and recipe in Synexia spelling. `python -I m3/tooling/jdk-naming/jdk_naming.py plan --tree HEAD` on master
(workflow "M3 JDK naming standard", run 42198122643) stops at `rewrites=10 CONST=10 KEBAB=10 PKG_PATH=10`; this leaf
brings the module to the fixed point with the recipe as the only editor. No hand edit.

## Method (executed)

```text
jdk_naming.py plan  --only m3/collections/ --tsv PLAN_RECIPE_BEFORE.tsv   moves=0 rewrites=10 refusals=0 absent=0
plan_lines (rewrite_line per QUICK line)   -> PLAN.tsv                   20 rows: file, line, rules, before, after
jdk_naming.py apply --only m3/collections/ --tsv APPLY.tsv               FIXED_POINT moves=0 rewrites=0
jdk_naming.py plan  --only m3/collections/ --tsv PLAN_RECIPE_AFTER.tsv    moves=0 rewrites=0 refusals=0 absent=0
```

`PLAN.tsv` was written before `apply`; after `apply` the 40 changed lines of `git diff --cached -U0` equal the
before/after columns of `PLAN.tsv` (sorted `diff`: no difference). `git diff --cached --check` is clean and the CR
count of every file is unchanged (bytes kept).

## Rewrites (10 files, 2 lines each, all inside the class Javadoc)

| rule | before | after | files |
|---|---|---|---|
| KEBAB | `synexia-primitives/` | `m3-primitives/` | 10 |
| PKG_PATH | `com/synexia/primitives/` | `jdk/internal/m3/primitives/` | 10 |
| CONST | `RELOCATE_SYNEXIA_OWNER` | `RELOCATE_M3_OWNER` | 10 |

Files: `M3DeltaEncodedLongList`, `M3FixedDecimal64`, `M3IntDisjointSet`, `M3IntKmpPattern`, `M3LongLiChaoTree`,
`M3LongLongRangeMap`, `M3LongTimingWheel`, `M3Unsigned128`, `M3UnsignedMath`, `M3WideIntegerArray` (`.java`).

No Java identifier, package, file name, string literal or public name changes: the three tokens occur only in the
`<p>Lineage:` Javadoc. The donor blob sha and the `com.synexia develop {@code ded4a4ac...}` commit on the same lines
are kept by the rules, so the lineage still resolves (`M3OrderStatisticForest.java` already carried the renamed form
from an earlier run). The word `Synexia` in attribution prose is not a rule and stays.

## Gates

| gate | command | result |
|---|---|---|
| 1 compile | `mvn -q -B -ntp -Dmaven.repo.local=S:/m3-scratch/eb-conv/m2 -f m3/collections/pom.xml -DskipTests package` (JDK 21.0.9, Maven 3.9.14; `m3-arrays` installed first, see RECIPE_MEMORY) | BUILD SUCCESS, 14.6 s |
| 1b tests | `mvn -B -ntp -Dmaven.repo.local=... -f m3/collections/pom.xml test` (1.5 min, no JNI library built) | head: 1624 run, 5 failures, 49 errors, 2 skipped; base 351ed627035c: 1624 run, 6 failures, 49 errors, 2 skipped; the 54 head failures are all in the base set (53 "M3 array JNI library is not loaded" / "real m3-arrays JNI library path required", 1 `M3ObjectResidencyTest.residencyAnnotationIsClassRetainedAndExplicit`); the one base-only failure asserts `..\arrays\pom.xml` exists and the base worktree had no `m3/arrays` checkout (`TEST_BASELINE_DIFF.txt`) |
| 2 fixed point | `jdk_naming.py plan --only m3/collections/` after apply | moves=0 rewrites=0 refusals=0 absent=0 |
| 3 scope | `git diff --stat` | 10 files, +20/-20, all under `m3/collections/src/main/java/jdk/m3/collections/`; this recipe directory is the only addition |

Logs: `GATES_20261106.log` (summary; full logs on `S:/m3-scratch/eb-conv/naming/`, not committed).

## Files

| file | content |
|---|---|
| `PLAN.tsv` | one row per rewritten line: `file line rules before after` (written before apply) |
| `PLAN_RECIPE_BEFORE.tsv`, `APPLY.tsv`, `PLAN_RECIPE_AFTER.tsv` | the recipe's own `--tsv` output of plan, apply and the fixed-point plan |
| `GATES_20261106.log` | timestamps and results of the recipe run and the Maven gate |
| `TEST_BASELINE_DIFF.txt` | failing-test sets of the base commit and of this head, and their `diff` |
| `RECIPE_MEMORY.tsv` | one row per lesson |

## Open

- The whole-tree fixed point is CI's `--tree HEAD` plan (a blobless clone cannot read every blob locally); master
  moved to `48537c8e7416` (#2499, #2500) while this leaf was prepared, its naming run was in progress.
- The module's test phase needs the m3-arrays JNI library; without it the same 54 tests fail on base and head (53 of
  them for the missing library). The owner's green test command is the module's CI job (`Installed Java 21
  collections and JNI`), not this local run.
