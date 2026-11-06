<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 four-donor additive source merge — executed Git custody record

Date: 2026-11-02. **DRAFT/HOLD. Git merges completed into review branch only; `master` unchanged.** The word "all" here is limited to the four specifically qualified non-conflicting donors. At inventory time the repo had **66 direct-to-master open PR search results**, many requiring separate runtime/source-conflict decisions; older 45,542-file superset #2255 and the misspelled public `Object.toAdpater` alias #2320 were explicitly not merged.

## Exact current master and serial Git merge commits

Original `master` SHA `ce005d25b29fb6246e013bffb462afd5ed070362`. New integration branch `m3/additive-qualified-open-20261102`, documentation-first parent `393fa345d2e0d6c1d393c326928a487e123af80e`.

| Donor PR | Frozen original head | Actual two-parent merge commit | Original delta vs master |
|---|---|---|---|
| [#2317](https://github.com/hsoliwal/M3jdk21/pull/2317) | `4586851d6c237052851b654c46c6611715ee6bd7` | `d1d40e3f1abdfa93619e1415b937a2a43e028da3` | 6 added |
| [#2319](https://github.com/hsoliwal/M3jdk21/pull/2319) | `af8885c1229cc7916e605f3adc2715b8cbade315` | `ca339bbc5f84108c5b972d129c288a3c8b3b1f94` | 9 added, 9 modified |
| [#2321](https://github.com/hsoliwal/M3jdk21/pull/2321) | `ce7051f77ca08a0f1c8230f476a04bb143b4553e` | `33e02676f91abc96cf431f688a49ada2b02f0027` | 2 added, 10 modified |
| [#2325](https://github.com/hsoliwal/M3jdk21/pull/2325) | `1edbbda245dea9a616feb37ecea43ef4c0ef050c` | `5b507a9db7afc77cedc98004e6c421ae918f2e04` | 1 added, 2 modified |

The four Git commits are **actual two-parent merges**, not reference pointers, squash commits, rebases or `ours` pseudo-merges. Parent-one always equals the previous verified receiving head; parent-two is each original frozen donor SHA. Merge transport PRs [#2333](https://github.com/hsoliwal/M3jdk21/pull/2333), [#2334](https://github.com/hsoliwal/M3jdk21/pull/2334), [#2335](https://github.com/hsoliwal/M3jdk21/pull/2335) and [#2336](https://github.com/hsoliwal/M3jdk21/pull/2336) were merged **only into the dedicated integration branch**.

## Full changed-path delta proof

- Before merging, the four frozen donor branch diffs vs the current master had 6 + 18 + 12 + 3 = **39 distinct changed filenames**, with **zero pairwise collisions**, **zero removed or renamed files**, zero commits behind their pinned master.
- After each individual Git merge, compared `original master -> original donor` to `immediately prior receiver -> actual merge` using complete `(filename, status)` sets. All **6/6, 18/18, 12/12, 3/3 exact matched**, **0 missing, 0 extra, 0 deletions**. The four parent Git SHA identities were independently read and verified.
- Final merge head vs original master returned **40 paths** (39 donors + documentation-first README): **19 added and 21 modified**, zero removed, zero commits behind. This exact 40-file response is under GitHub's 300-file compare cap.
- No source file was renamed or deleted; original historical donor branches and PRs were retained.

## Seven exact source Git-blob witness checks

Each original source file was fetched independently at its frozen owner parent **and** at final merge `5b507a9db7afc77cedc98004e6c421ae918f2e04`. All remote content bytes and Git blob SHA were equal:

| Source | SHA |
|---|---|
| Current `src/java.base/share/classes/java/lang/Object.java`, original master | `a9899e1c2970f6d5e9172ffb12f5b7399d052b68` |
| `test/jdk/java/lang/Object/ObjectAdapterNativeTest.java`, #2317 | `19cf899b9aad2d43566d1d0b1f52dec67464a0eb` |
| `m3/collections/src/main/java/jdk/m3/collections/M3ArrayList.java`, #2319 | `60be524df82197ae9aa4214320f10ded4e3e9d4c` |
| Native `m3textops.c`, #2321 | `416200afbf97e95dfa38f3cf35e03b1739fa5066` |
| `NativeUtf16Literal.java`, #2321 | `99e5e8f6955316c8a3b163c38e5e1d43587cb21d` |
| `VInt.java`, #2325 | `346c05694f6921b8211d15fd5fc1da0cd01bae83` |
| Native `m3_packed_codec.c`, #2325 | `1813a024d4c2ce514cebe4928ab4379b7950ff94` |

Current `java.lang.Object` contains the exact correct public generic method `public <T> T toAdapter(Class<T> clazz)`. The merged branch's Object.java Git blob is **byte-identical** to master. The older draft #2320 proposes a misspelled `toAdpater` public alias and HotSpot edits; this is **NOT** an admitted source change. No Java ABI or JNI compatibility inference is made solely from the seven witnesses.

## Release gates not executed

JDK21 full javac, jtreg/JUnit, collections, concurrent exception/reentrancy, build-impact, text/JNI C compilation, packed-varint native ABI, Windows/Linux/macOS HotSpot/JIT, serialization, API/JCK and real runtime benchmarks **NOT RUN on this merged head**. Existing donor PRs have various isolated proof claims, but their results cannot be silently inherited as proof of this combined tree. Apply source diff/lint, build, tests and runtime in exact order. These unresolved blockers prohibit promotion to `master` or using the combined tree as a qualified M3JDK runtime.

This is a precise positive source-history merge across four PRs, **not** a claim that all M3JDK21 open PRs, all `hsoliwal/*` repos or the entire 45k-file old superset have been integrated.
