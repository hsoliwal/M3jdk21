# jbcoll-mask-compactor-perf-20261106: `M3CollectionMaskCompactor` dispatch regression (java.base)

java.base lane leaf (owner com-synexia-45), follow-up of census leaf L003 (`../jbcoll-mask-compactor-20261106/`, PR #2578).
- Master base: written on `dbce93c3abf02256f2ecf656720c339b909713b6`, rebased onto
  `c46cfa6639aa63bfa01b2f8e9ef00266c13759c2` (owner, ArrayList, Vector and the tests unchanged in between).
- Owner: `src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java`, blob `9f8b4c965b52` ->
  sha256 `ad30c982c60b` (recipe `apply.py`, `EDITS.tsv`).

## Root cause: b361ddbf0552, a merge accident

`git log -- <owner>` with diffs:
- ff0b94fc2a3b / 4476604f7d95: `int removalRuns(...)` with an early exit `if (count > threshold) return count;`. The
  caller only compared the count with the threshold, so the early exit was correct.
- 584cf8a6d0b5 (head of PR #1363, 2026-10-18): `long maskStatistics(...)`, a **complete** scan returning
  `runs << 32 | removed`. compact() dispatches compactRuns, compactWords or compactScalar from both fields.
- **b361ddbf0552** (2026-10-28, "Superset: merge PR #1363 (source conflicts resolved to the candidate the compiler
  accepts ...)"): the resolution kept the long maskStatistics and also kept the early exit from removalRuns. The int
  `count` widens to long, so it compiles. Its message shows a mechanical conflict resolution, not a design change.
  On the early exit the bare count lands in the low half, so compact() reads `runs = 0` and `removed = count`. Every
  mask of 512+ entries with a removal went to compactRuns, and compactWords and the post-statistics compactScalar
  could not run.
- 684b79f4cd27 added the `removed == 0` no-op and the store check in compactScalar; it did not touch the statistic.

Because this was an accident, the fix restores the statistic 584cf8a6d0b5 introduced, rather than adding a new one.

## Fix (`apply.py`: one EDIT rule, six anchored substitutions, hash-pinned)

1. **STAT_EDITS:** maskStatistics drops the early exit and its threshold. The scan completes and returns the complete
   `runs << 32 | removed`. The Javadoc states this contract, and `//FLLDCIM: HPCPOP` marks the line where the early
   exit was.
2. **DISPATCH_EDITS:** the compactRuns threshold moves from `length >>> 7` to `length >>> 4`. With the complete
   statistic alone (rule STAT), sparse masks (Bernoulli 1/32, about n/33 runs) left compactRuns, the path master took
   by accident, for compactWords. They became **15-32% slower** on both CPU sets: E 1.257/1.286/1.320 and
   P 1.230/1.180/1.140 at 512/1024/4096. Up to one run per 16 entries now stays on compactRuns, so sparse and
   bern16 masks take the master path again (ratios 0.99-1.01).
3. **SURVIVOR_EDITS:** the NEC idea pr1908 is adopted with its 1024 bound dropped. The post-statistics fallback (masks
   of 512+ that are too fragmented for compactRuns and neither sparse nor nearly all deleted) runs pr1908's
   survivor-word method instead of compactScalar. The method comes verbatim from retained blob `3f88d207e1f6`, renamed
   compactSurvivors because the owner already has a compactWords. `apply.py` re-derives the method from the blob on
   every run. The compact() header comment now names the paths. compactScalar still serves masks below 512.

The pr1908 conflict with live is CONFLICT=2, so it could not come in as a clean 3-way merge. It is a declared EDIT row
(base = master blob `9f8b4c965b52`, theirs = pr1908 blob `3f88d207e1f6`, its method only).

## NEC ideas (rules in `timing_table.py`, numbers from `TIMING.tsv`)

Rule: adopt only when the idea is behaviour-identical (differential: 0 mismatches) **and** the geometric mean of its
target cells is <= 0.85 x base on every CPU set **and** no affected cell is slower by more than 5% on any CPU set.

| idea | base | target gmean E / P | affected cells > 5% slower | verdict |
|---|---|---|---|---|
| pr1908 survivor words, verbatim (1024+) | threshold | 0.532 / 0.480 (n=10) | 0 | passes the idea rule, but compactScalar stays at 512-1023, where E-core cluster12/512 is 1.111 x master (rule THRESHOLD fails) |
| **pr1908 survivor words, bound dropped (512+)** | threshold | **0.536 / 0.493** (n=15) | **0** | **ADOPTED** (the recipe postimage) |
| pr1754 = pr1802 prefix skip | threshold | 0.552 / 0.641 (n=16) | 43, up to 1.826 (P bern16/256), E dense/512 1.214 | **REJECTED** |
| pr1754 = pr1802 prefix skip | fixed | 0.526 / 0.592 (n=13) | 19, up to 1.821 (P every8th/64), E dense/64 1.100 | **REJECTED** |

The prefix skip wins large when the first deletion is late: tail/64 drops from 112 to 6 ns. But its loop drops the
live store check `if (write != from + read)`, and it is slower whenever the first deletion is early, which is the
common removeIf case. A prefix skip that keeps the live loop body was not measured; it would be a new idea, not the
retained one.

## Timing (`CompactorTiming.java`, `timing.sh`, `timing_table.py`; not JMH)

Method:
- One JVM per run, 3 runs per CPU set.
- Each JVM is pinned from the start (`cmd start /affinity`): P = CPUs 8,10,12,14 (one thread on each of four P-cores
  of the i9-14900HX); E = CPUs 16-19 (four E-cores).
- JVM flags: `-XX:-TieredCompilation -Xbatch -Xms1g -Xmx1g`.
- Seed 0x4d334a424d534b31; 21 samples of max(32, 131072/n) repetitions.
- All variants are interleaved in one JVM. **net** = median over runs of (variant - restore), where restore is the
  `System.arraycopy` refill that every repetition does.

Two traps the method avoids:
- Unpinned or tiered runs were **bimodal (up to 2.5x)** between forks, because the main thread landed on P- or E-cores.
- The two core types rank the paths differently. Small overlapping `System.arraycopy` calls cost about 3x more on
  the loaded P-cores, so compactScalar beats compactRuns on sparse masks there, but loses on E-cores.

A dispatch change must therefore hold on both sets.

Net ns/op, master -> fixed (fixed/master), required classes:

| class | n | path master -> fixed | E master | E fixed | E ratio | P master | P fixed | P ratio |
|---|---|---|---|---|---|---|---|---|
| dense | 64 | scalar -> scalar | 116 | 113 | 0.974 | 61 | 59 | 0.972 |
| dense | 512 | runs -> survivors | 2916 | 349 | 0.120 | 1708 | 168 | 0.099 |
| dense | 1024 | runs -> survivors | 6238 | 738 | 0.118 | 3548 | 350 | 0.099 |
| dense | 4096 | runs -> survivors | 25909 | 3144 | 0.121 | 15091 | 1388 | 0.092 |
| sparse | 64 | scalar -> scalar | 197 | 191 | 0.972 | 106 | 112 | 1.058 |
| sparse | 512 | runs -> runs | 702 | 706 | 1.006 | 2383 | 2384 | 1.000 |
| sparse | 1024 | runs -> runs | 1097 | 1105 | 1.007 | 3701 | 3652 | 0.987 |
| sparse | 4096 | runs -> runs | 4406 | 4459 | 1.012 | 15875 | 15653 | 0.986 |
| fragmented | 64 | scalar -> scalar | 158 | 157 | 0.992 | 92 | 86 | 0.929 |
| fragmented | 512 | runs -> survivors | 8159 | 724 | 0.089 | 4693 | 354 | 0.075 |
| fragmented | 1024 | runs -> survivors | 15719 | 1423 | 0.091 | 9784 | 733 | 0.075 |
| fragmented | 4096 | runs -> survivors | 61909 | 5647 | 0.091 | 39706 | 2750 | 0.069 |

- **Fragmented 4096 vs the postimage:** the postimage runs 8575 ns (E) and 5822 ns (P), so fixed/postimage = 0.659 (E)
  and 0.472 (P). Fixed is back past the postimage, not just near it. L003's unpinned probe measured 52.8 us vs
  11.6 us.
- **Extra classes, affected cells (fixed/master, E / P):**
  - every8th: 0.61 / 0.51
  - prefix: 0.10 / 0.08
  - random50: 0.18 / 0.14
  - cluster12 (3 of every 12): 0.80 / 0.08-0.14
  - block, tail, bern16: unchanged path
- **Noise:** the cells of length 64 run identical compactScalar code in master and fixed. Across variants they spread
  up to 1.53x net (P, small cells), from JIT profile and code layout. So the 5% rules judge only **affected** cells,
  meaning cells where the compared variants run different paths.

## Gates (`GATES.tsv`, `verify.sh`)

`verify.sh <scratch>` prints one GATE line per gate. Run on the rebased leaf: **VERIFY PASS**.

| gate | result |
|---|---|
| recipe | `changed=0 plan=UNCHANGED`; one appended byte -> `REFUSED drifted preimage`, exit 2; pr1908 method equals its blob slice |
| patch_compile | `-Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline`, ArrayList + Vector + compactor: base and leaf rc=0, 0 warnings, 13 classes |
| differential | L003 driver, seed 0x4d334a424d534b31, 1,000,000 cases (889,339,623 elements, 14,868 refusals): leaf (patched java.base), fixed, master, postimage, statonly, threshold, threshold_prefix, threshold_words1024, fixed_prefix all **0 mismatches**, sha256 `c5f342c12d3d...` = L003's; L003 mutant **306,431** mismatches |
| tests_owner | `test/jdk/java/util/ArrayList/M3*` as plain mains: leaf 9/9 (G1 + Serial runs), base 9/9 |
| tests_arraylist | `test/jdk/java/util/ArrayList/*`: leaf 14 pass, 2 skip (ArrayManagement testng, Bug8146568 main/manual), base the same |
| timing | fresh 3 x P + 3 x E runs: RULE FIX PASS (affected gmean E 0.212 / P 0.122, 0 regressions; fragmented 4096 0.670 / 0.473 x postimage); verdicts as above |
| naming | `jdk_naming.py` apply, then plan: moves=0 rewrites=0 refusals=0 |
| scope | 11 paths, 0 outside the owner + this dir (base = merge-base) |
| jtreg | NOT_RUN: no jtreg on the host |

Callers of the compactor in java.base (`git grep`): `ArrayList.removeIf` and `Vector.removeIf` only.

## Files

| file | role |
|---|---|
| `apply.py` | recipe JBCOLL_MASK_COMPACTOR_PERF v1; writes the owner and `EDITS.tsv` |
| `EDITS.tsv` | file, kind, before blob, before/after sha256, rule (fixed point) |
| `variants.py` | renamed variants for the differential and timing; pr1754 provenance check |
| `CompactorTiming.java`, `timing.sh`, `timing_table.py` | timing probe, pinned runner, table + decision rules |
| `TIMING.tsv` | per CPU set x class x length: paths, net ns per variant, gross values per run |
| `verify.sh`, `GATES.tsv` | gates |
| `RECIPE_MEMORY.tsv` | lessons |
