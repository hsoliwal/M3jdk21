<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 additive three-donor wave 2 — exact Git and source-blob execution record

Date 2026-11-02. **Integration-branch Git merges executed; master release NOT executed.** This file supplements the immutable earlier four-donor `VERIFICATION.md`. It does not alter or restate that verified historical four-PR stage as though it contained the newer donors.

## Frozen source refs and real serial two-parent commits

Retained receiving original `master@ce005d25b29fb6246e013bffb462afd5ed070362`. The verified first wave comprised #2317, #2319, #2321 and #2325 at head `3094afa99da41165cf8ffa3c5668d8eb7145a23c`. Second-wave specification was committed **before** donor source integration at `95be3b9929972c6bccced61862afe6b00535d6a9`.

| Original open donor PR | Frozen donor head SHA | Real Git merge commit | Donor delta, path-exclusive |
|---|---|---|---|
| [#2308](https://github.com/hsoliwal/M3jdk21/pull/2308) compiler source-facts/candidate review | `25ae6e7e7f209f67d807ef20d53ef9499d322d62` | `a374a2adc5d7053e1efb84dd97195454ef873e22` | 10 added, 4 modified |
| [#2315](https://github.com/hsoliwal/M3jdk21/pull/2315) M3AST attributed relationship export | `4a3b9841f57c17cd34e683644b2beafd32ada938` | `fd7f9d0bf229fc36a780d71411f9d1f1a707892a` | 3 added, 2 modified |
| [#2310](https://github.com/hsoliwal/M3jdk21/pull/2310) restore merged long-map `addTo` without removing staged rehash | `15cae75c229d0fd3440131aea263bfd4bf05da53` | `0cbf65e365cee3741d15008b69ad93244f0fba6c` | 2 added, 1 modified |

Transport PRs [#2338](https://github.com/hsoliwal/M3jdk21/pull/2338), [#2339](https://github.com/hsoliwal/M3jdk21/pull/2339), and [#2340](https://github.com/hsoliwal/M3jdk21/pull/2340) were **actually merged only to `m3/additive-qualified-open-20261102`**, using GitHub `merge`, not squash/rebase. The first parent in each Git commit is the exact previous receiving head; the second parent is the original frozen donor commit. Original PRs and `master` were not modified.

## Actual path-set and source identity verification

Each original `master -> donor` file/status set was compared directly to its `receiver-before -> Git merge` set. All **14/14 + 5/5 + 3/3** `(filename,status)` pairs matched exactly; no missing, extra, renamed or deleted paths. The three new donors have **22 disjoint changed paths** (15 additions, 7 modifications) and no overlap with the earlier 41-path four-donor integration source diff. The complete seven-donor merge head vs original master has **63 changed paths**: 35 additions, 28 modifications, **zero deletions/renames**, zero commits behind.

Remote GitHub source-byte/Git-blob witness checks confirmed the exact original donor images on the merged head:

- #2308 `SortableProcessor.java` Git blob `e2a8b3f007e06370eb5a8608f48a73f6d6e8b926`.
- #2315 `M3AST.java` Git blob `155c9405fa8f837d02d8ed9481c1f1af15a73807` and `M3ASTPC.java` Git blob `ccea209bc8d9a14f100cad8cf82937231b1b3eb6`.
- #2310 `M3LongLongHashMap.java` Git blob `37e5575193fb010b03e786ad1f717fecce111fec`; this is the expected exact canonical postimage from the original source-sealed recipe and retains the newer staged-rehash body.

The original `java.lang.Object` was not changed in either wave: its previously verified Git blob `a9899e1c2970f6d5e9172ffb12f5b7399d052b68` retains **`<T> T toAdapter(Class<T> clazz)`**. Do not merge the misspelled public `toAdpater` alias #2320 without separate API permission.

## HARD release holds / no blanket "all" claim

No full combined JDK21 javac/jtreg/JCK build, collections runtime, compiler-fact/legacy-plugin integrations, HotSpot/JNI C/C++ parity or cross-platform regression suite has been executed on this seven-head source union. The original #2315 contains compiler-component proof, but that prior source-specific proof cannot be silently inherited by this combined tree. Review its compile/test logs before promoting.

The old 45,542-file #2255 superset and the other unmerged open PR branches remain separate unresolved input. LeetCode/HackerRank/GeeksforGeeks algorithms remain problem taxonomy/test oracles only, not code-copy rights. Legal source pinning, Java API/ABI compatibility, reproducible builds, runtime/benchmarks and JCK remain mandatory.

**Integration tree complete for these seven exact donor heads, but neither `M3jdk21/master` nor other `hsoliwal/*` production branches changed.** This is a candidate-only source merge, not a claim of fleet-wide PR closure.
