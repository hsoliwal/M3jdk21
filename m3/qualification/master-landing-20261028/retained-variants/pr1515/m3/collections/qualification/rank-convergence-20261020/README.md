# Current collection/JNI and adaptive rank convergence

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

This receiver refines the existing M3JDK21 `M3IndexPathRank.Window.toArray()` owner. It stacks on [#1549](https://github.com/hsoliwal/M3JDK21/pull/1549), preserving current filtered/sorted collection implementations and Java/C mask-window compaction. M3JDK21 remains the integrated product; the private Synexia workspace owns reusable authoring recipes and templates. There is no added runtime integration layer or independent payload store.

Dense windows enumerate masked words directly. Sparse wide windows retain the existing prefix-rank selection algorithm, avoiding the dense-only donor's wide empty-word scan. Ordering, duplicate/signed row IDs, immutable capture, windows and detached returned arrays are unchanged. Existing `rows`, `selected` and `wordPrefix` arrays remain the only rank authorities. No extra tree, entry object, String implementation, JNI method or public runtime class is added.

The exact output comes from [canonical PR #12563](https://github.com/hsoliwal/com.synexia/pull/12563), commit `b5d2bbb3c75ff928f3dbb0f92ed4471488ea8c05`, using `com.synexia.m3.M3IndexPathRankAdaptive20261020`. The original dense donor from #12545 is retained there byte-for-byte. The existing hash-pinned OpenRewrite engine admits exact pre/postimages and rejects drift. It materialized this owner and two proof sources; a second application changed zero files.

`receipt.json` seals the three generated outputs and records validation. `INPUTS.tsv` seals 1,318 actual collection and vendored JUnit/Univocity source/resource inputs. The canonical packet contains the Maven SDK test, Linux `verify.py`, full raw logs, reproduction commands, independent review and exact source licenses/pins:

`synexia-openrewrite-recipes/tasks/m3-index-rank-word-walk-20261020/qualification-20261020/`

From that exact canonical checkout, with Java 21 in `JAVA_HOME`, run:

```sh
mvn -f synexia-openrewrite-recipes/tasks/m3-index-rank-word-walk-20261020/qualification-20261020/pom.xml -Dm3.applyRoot="$M3_TARGET" test
python3 synexia-openrewrite-recipes/tasks/m3-index-rank-word-walk-20261020/qualification-20261020/verify.py "$M3_TARGET" /tmp/m3-rank-verification-new
```

Use exclusive checkout access during materialization. Its filesystem writes are serial, not an atomic transaction against concurrent writers. The output directory must be absent. The driver checks exact input inventory, compiles 267 production and 261 test Java sources plus 751 vendored JUnit/Univocity Java sources, builds ten native libraries from eleven C units, and requires 468 successful tests with no skips/aborts. This classpath proof excludes upstream modular, Java 9 multi-release and Kotlin source variants. Native proof uses checked JNI and fatal UBSan. `abi.json` records unchanged generated JNI headers; the two affected public surfaces were also compared by `javap -public -s`.

Observed results: 6 actual SDK tests passed; 468 collection tests passed; 180 rank JNI parity paths passed; interpreter proof passed 6,300 random windows plus 29 sparse/extreme-row windows. The inherited current mask proof also passed 236,639 Java assertions, 320,006 checked-JNI assertions, 21 missing-library refusals and 54 C function-table checks. These are Linux source-owner results, not installed JDK qualification.

The sparse regression's instrumented work drops from 16,385 to 14 loop steps; instrumentation is confined to a disposable copy. The catalogue retains a single-host timing pair showing dense improvement and an intermediate-sparsity slowdown. It makes no universal timing or retained-memory claim. LeetCode, HackerRank and GeeksforGeeks supplied problem-category references only; no third-party solutions were copied.

The original module Maven command remains blocked by the locally uninstalled source-owned `junit-jupiter:5.10.2-m3-source-1` artifact. The SDK's cached Maven success is not source-built OpenRewrite dependency closure. Full-borrow ancestry is confirmed remotely, but the local checker encounters shallow history. Installed integrated M3JDK21/HotSpot, jtreg/JCK, the full canonical reactor, other platforms, retained/native memory and the remaining tool families are still HOLD. String-first promotion remains unchanged. This PR does not claim the whole conversation's programme is complete.
