<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Bounded GINQ-inspired equality joins (candidate)

Product owner: `src/jdk.m3.collections/share/classes/jdk/m3/collections/GinqHashJoin.java`.
API: `GinqHashJoin.execute` for `INNER`, `LEFT`, `RIGHT`, `FULL`, and `inner` for the common inner case.

The implementation uses existing `ObjectStream` inputs, `M3LinkedHashMap` right-key buckets, `IntArrayList` primitive right-row ordinals, `M3ArrayList` payload and output buffers, and a `BitSet` mask for unmatched-right output. There are **no retained object-pair records**. The right side is indexed and closed before left-side traversal. A source can be replayable or one-shot. Every input and output has an explicit caller-supplied bound. Preserves null-key matching and distinguishes unmatched sides from present null payloads using two boolean presence flags.

* **Not full Groovy GINQ:** only Java equals/hashCode equijoins with caller-supplied functions, not Groovy `==` coercion, arbitrary `on` predicates, query optimizer, DSL/AST lowering, GINQ `Queryable` ABI, SQL-null semantics, or Groovy MetaClass support. Clients must keep key hashCode/equals stable during evaluation.
* Emission order is left-major (right order within each bucket); unmatched right rows follow in right encounter order for RIGHT/FULL. Original GINQ does not promise this exact result order without `orderby`.
* Source and output budgets are checked before reading excess inputs or invoking an excess output projection. No partial result is published on failure, and opened streams close by try-with-resources.
* The materialized result is shallow-read-only. Retained client objects remain mutable as usual. No JNI, default-JDK method, module-info, or existing JDK public class changes.

## Qualification

Source-only: compile the two Java product/test files against the existing package APIs using JDK 21, then run `GinqHashJoinTest`. The test supplies a 2,000-seed oracle across four join modes and checks nontrivial budget/ordering/resource behavior.

Installed image: run `m3/compatibility/groovy/run-ginq-join-installed.sh /absolute/path/to/jdk /new/out/dir` (or `.ps1` on Windows). It rejects a stock JDK missing the `jdk.m3.collections` module and runs both mixed and `-Xint` modes. Preserve exit status/output as a receipt; an unexecuted runner does not qualify the image.

Both launchers require the collections module **and** nonempty packaged `lib/modules` and `jdk.m3.collections.jmod`. They inspect installed `java.base/java.lang.String` with the selected `javap`, requiring `capitalize()`, `uncapitalize()` and `center(int,String)`, and refuse a mismatched actual `java.home`. This distinguishes obvious stock-JDK impersonations, but does **not** certify source provenance. Both runners record SHA-256 for the jtreg source, selected `java`/`javac`/`javap`, module image, collections JMOD and runner, plus the module inventory, String API, runtime settings, compiler output, normal/`-Xint` transcripts and success only after mode parity. The Linux source-only regression deliberately refuses seven forged conditions (missing executable, existing receipt directory, missing/prefix-collision module, missing JMOD, missing M3 String member, wrong `java.home`); `bash -n` and all seven negative cases passed locally. The hosted existing native workflow now runs these negatives before native building, runs the installed-image receipt after full+jlink join checks and adds the GINQ test to real jtreg. **Windows PowerShell and the actual source-built image remain NOT RUN by the local checks.**

Canonical exact-source Java/OpenRewrite recipe proposal belongs under `hsoliwal/com.synexia`; do not copy donor Groovy implementations. Compare against real Groovy pinned releases separately, and preserve pre-existing String.capitalize/uncapitalize/center semantic FAIL. Full M3 native/JNI/jtreg, platforms, allocation/throughput benchmarking and SDK replay remain separate acceptance gates.

Official donor semantics: https://groovy-lang.org/using-ginq.html
