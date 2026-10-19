# M3JDK21 parser, filtered collection and JNI refinement

The existing product owners were emitted by canonical Synexia recipe
[`com.synexia.m3.ParserFilterRefinement`](https://github.com/hsoliwal/com.synexia/pull/12511).
This is integrated M3JDK21 source refinement using existing arrays, offsets,
slot indexes and state masks. Existing collection interfaces and all nine JNI
exports remain. Storage contains no retained per-entry objects; explicit entry
APIs still return fresh caller-owned projections.

## Behavior and evidence

- TreeMap: **640/640 exact frozen JDK21 observations**, all 144 empty-map cases
  preserved, no remaining or introduced differences. Values, exceptions,
  messages, callback sequences and map state are compared without normalization.
- Current master at the pinned base matched 128/640; it lacks the earlier
  PR1442/1488 lookup atoms. PR1442's separate qualified lineage matched 630/640.
  This pass restores those atoms into current owners, including key admission
  and slot-aware API entries, then restores the null-result second search.
- JDT: **3,115 actual JDT assertions**, including bindings, diagnostics, recovery,
  source positions and rename/reparse. Optional metadata remains detached in
  reference and primitive lanes; no JDT AST/binding objects are retained.
- Existing parser tests: 297 framework and 12,071 cross-frontend assertions.
  Cross-frontend fixture shapes do not establish full semantic parser coverage.
- Filtered views: **18,720 exact baseline/candidate terminal traces**. Existing
  strict endpoint/reversal/navigation contracts pass with direct JDK21 compilation
  (2,517 + 126 + 154,239 checks). Guards and split metadata reuse existing owners.
- JNI: **61,440 Java/native assertions** under `-Xcheck:jni`; C17 warnings as
  errors, boundary/failure injection and unchanged export names. Instrumented
  one-match input fetches 1 row value instead of 1,025. No latency gain claimed.
- Recipe: five actual OpenRewrite 8.17.1 tests pass, covering exact output,
  reverse input order, fixed point and fail-closed drift/missing/duplicate cases.

The full collection baseline has 464 tests, three failures and two skips. An
incremental candidate build produced nested classes missing methods present in
sealed source, and retained those three failures. Direct javac emits the correct
methods and passes their contracts. The cause is not established; the fresh,
forked full Maven build is running. See QUALIFICATION.json for the final state.
No test has been removed or weakened to make these gates pass.

## Reproduction and source custody

Product base: `5cb893e1ab224236b33d41132a31f604d89a4e12`.
Canonical engine base: `56842e83d741df4eb368997b903b40ca09e1cce4`.
`FILE_PASSES.tsv` records all 14 exact pre/postimages. `ALGORITHMS.tsv` contains
15 category-indexed candidate evaluations and donor/license decisions.
`observations/sdk-receipt.json` and source/toolchain manifests bind the receipts
to actual files. See the canonical crate's README and `qualify.py` for portable
commands, frozen oracle sources and SHA-pinned JDT dependency inputs.

Use a fresh checkout without build output and the existing collection POM with
`-Dmaven.compiler.fork=true`. Large filtered trace streams are reproducible from
M3FilteredTerminalTrace and sealed by their complete SHA-256 and case count;
full frozen TreeMap streams are retained in observations/final. Failure receipts
remain alongside successful runs, including the actual Equinox resolution failure.

## Open qualification

This remains draft qualification. Full Eclipse/PDE workspace editing, semantic
Groovy/Kotlin models, effective Maven models, real Jackson module integration,
source-built JDK/jtreg, other native platforms and end-to-end performance are open.
The hydrated checkout cannot satisfy the existing Git-ancestry receiver guard.
There is no phase promotion and no claim that every parser is fully enhanced.
