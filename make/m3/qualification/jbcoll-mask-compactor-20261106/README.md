# jbcoll-mask-compactor-20261106: census leaf L003, `jdk/internal/mindex/M3CollectionMaskCompactor.java` (classification only)

M3JDK convergence census leaf L003 (java.base collections, lane owner com-synexia-45).
- Master: `fa8215c8a2df98ccaf717c584688171954a88956`.
- Donor: hsoliwal/com.synexia develop `ca1aafc4d94f9dd1ee288226dcca070cc60991c9`, crate
  `m3jdk21-java-base-arraylist-inline-20261025`.
- Census row: PARTIAL, added_present=48/52, 85 changed lines, `LOWCONF_NOPRE` (low confidence, no preimage).

**Verdict: no edit.** The census postimage is an ancestor of the live owner. Applying it verbatim would revert three
later commits. The only clean 3-way merge is the no-op (ours = live, base = theirs = postimage, result = ours). The
four DIVERGENT rows in `JDK_TREE_3WAY_20261106.tsv` resolve as follows: pr1873 is already in live history
(SUPERSEDED), and pr1754/pr1802 and pr1908 are NEC rows (CLASSIFICATION.tsv). All of them behave identically to live
over 1,000,000 seeded cases.

## Lineage (`lineage.py` -> LINEAGE.tsv, MERGES.tsv)

Owner history on master, newest first (owner blob in parentheses):
- 684b79f4cd27 (2026-10-28, `9f8b4c965b52`): live
- b361ddbf0552 (2026-10-28)
- 4476604f7d95 (2026-10-27, `e6ca59d8ae7f`)
- ff0b94fc2a3b (2026-10-21, `51ea3a72bb19`)
- 584cf8a6d0b5 (2026-10-18)
- 336d70ac1287 (2026-10-18)

| candidate | blob | relation | lines vs postimage / live |
|---|---|---|---|
| census postimage | `51ea3a72bb19` (sha256 `1c3f3d2cf129`) | **the owner blob of ff0b94fc2a3b**. That commit is the 3-way base of all four retained rows (base=ff0b94fc2a3b) and an ancestor of live | 0 / 88 |
| pr1873 | `e6ca59d8ae7f` | **the owner blob of 4476604f7d95**, an ancestor of live | 11 / 91 |
| pr1754 = pr1802 | `f85e7359010b` | base + prefix skip in the scalar path | 8 / 92 |
| pr1908 | `3f88d207e1f6` | base + survivor-word compaction for fragmented masks >= 1024 | 25 / 87 |
| live | `9f8b4c965b52` | base -> pr1873 content -> #1363 superset -> landing fix | 86 / 0 |

The crate manifest lists this file with a single sha256, which equals the postimage. So the crate pins it as the
state its ArrayList postimage expects; it is not an edit. L002 applies that ArrayList hunk against the live
compactor, and the dispatch contract is the same: `compact(es, beg, end, deathRow)` for spans of 512 or more.

| merge (ours / base / theirs) | result |
|---|---|
| live / postimage / postimage | CLEAN = ours (the no-op) |
| live / postimage / pr1754 | CONFLICT=1 |
| live / postimage / pr1873 | CONFLICT=1 |
| live / postimage / pr1908 | CONFLICT=2 |
| live / pr1873 / pr1754 | CONFLICT=1 |
| live / pr1873 / pr1908 | CONFLICT=2 |

These match 45's ledger (CONFLICT=1 for pr1754/1802/1873, CONFLICT=2 for pr1908).

## Differential (`CompactorDifferential.java`; seed 0x4d334a424d534b31; 1,000,000 cases; about 35 s)

The live class runs from the patched java.base. The other versions are renamed copies compiled outside java.base:
live itself (as a control), the postimage, pr1754, pr1873, pr1908, and a one-token mutant of the scalar path.

Cases cover:
- lengths 0..4100 around the 512 and 1024 thresholds, plus offsets;
- mask shapes: random density, runs, sparse, alternating, word blocks, boundary bits, all-zero, all-one;
- garbage bits past the length, and extra mask words;
- the refusal paths: mask too short, bad range.

The outcome is the returned end plus a hash of the whole array, or the exception class.

Results: 889,339,623 elements and 14,868 refusals. Every version (live, control, postimage, pr1754, pr1873, pr1908)
gave **0 mismatches**, with the same outcome sha256 `c5f342c12d3d6ae1...`. The mutant gave 306,431 mismatches.

## Finding for the lane owner (INFO, behaviour-identical)

The bug is in live `maskStatistics`. On its early exit (`if (count > threshold) return count;`) it returns the bare
run count instead of `count << 32 | removed`. `compact()` therefore reads `runs = 0` and dispatches `compactRuns` for
every mask of length 512 or more with any removal. As a result, `compactWords` and the post-statistics
`compactScalar` are unreachable.

Probe on 4096-element masks (median ns per call):

| mask | maskStatistics | dispatch | live | postimage |
|---|---|---|---|---|
| alternating (2,048 runs) | `0x40`: runs_field 0 | compactRuns | 58,871 / 61,638 | 13,189 / 10,901 |
| every 8th bit (512 runs) | `0x28`: runs_field 0 | compactRuns | 16,688 / 13,350 | 12,578 / 12,954 |
| two bits | `0x200000002` | compactRuns | 3,845 / 3,263 | 4,098 / 4,314 |

Fragmented masks run about 4.5x slower than the postimage's scalar path. The owner's 8 jtreg runs pass with either
compactor, so the tests cannot see this. The fix belongs in a separate java.base leaf that decides the dispatch
together with the pr1908 and pr1754 intents. It is out of scope here: no clean 3-way exists for it.

## Gates (GATES.tsv; `verify.sh`)

| gate | result |
|---|---|
| lineage | PASS. Tables are a fixed point; 1 clean merge (the no-op) |
| patch_compile | PASS. `javac -g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module java.base=<ArrayList + Vector + compactor>`: live and postimage, 13 classes each, **0 warnings** |
| differential | PASS. 0 mismatches for every version; the mutant is caught |
| tests | PASS. The owner's jtreg tests (`test/jdk/java/util/ArrayList/M3*`, 6 files, 8 runs including G1 and Serial) run as plain mains under `--patch-module`: 8/8 on live, and 8/8 with the postimage compactor (INFO) |
| naming | PASS. `jdk_naming.py` apply, then plan, on the leaf dir: 0/0/0 |
| scope | PASS. Only this directory |

Not run: jtreg itself (no jtreg on the host).

Rerun: `JAVA_HOME=<JDK 21> bash verify.sh <scratch> 1000000 <base>`
