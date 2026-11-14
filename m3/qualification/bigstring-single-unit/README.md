# Chunk-summary single-unit search (R236)

M3BigString already stores immutable chunk starts, lengths and per-chunk Bloom summaries. Its indexOf previously used whole-range filtering and KMP for every needle. This scoped recipe retains that preflight, then dispatches only an immutable String needle of length1 to a private helper. The helper reads the first valid UTF16 unit, skips chunks whose existing Bloom rejects the target, and uses Java21 String.indexOf(int,int,int) for a bounded String leaf. Other owned leaves keep exact unit reads. All other needles retain the original KMP source and callback schedule.

This adds no fields, persistent index, cache, retained arrays, Entry/Node objects, normalization, native dependency or flattening. Results remain long range-relative UTF16 code-unit positions, including lone surrogates and pairs split across leaves. The existing parallel-array storage/lifetime owner remains unchanged.

## Executed recipes and product proof

The exact canonical registry owner 761980021499f9339883de544a48a274cad68371 receives one path case for m3/core/src/jdk/m3/text/M3BigString.java; no generic source root is widened. Its anchor is the existing LocalM3Arena case. Pinned R234 registry changes use a different anchor. Actual JGit6.9 three-way merge yields the expected conflict-free composite c494545b98b78dbb6c81843c4a30f23f7c2c7497; that composite is compiled and replays the product recipe. It is qualification-only: the published canonical registry afterimage is 6f12bf31c0bf4c87ea4f3764baa5d94cde990882, containing only R236's new admission. No claim that pending R234 PRs have merged.

Each of three recipe stages executes nine JUnit methods: source replay/fixed point, missing/duplicate owner, wrong AST, source drift, post-scan mutation, frozen atom, exact-owner scope, and actual JGit merge. The actual emitted product source is compiled with Java21 --release21 -proc:none -g:none -Xlint:all -Werror. Four fresh JVMs run before/after/after/before, 14 JUnit methods each: **83 JUnit executions total (27 recipe+56 product)**. All seven child stages exited0 and were reaped.

Each product JVM also runs the unchanged M3BigStringContractTest (96 internal checks) and an adapter retaining exact pinned check/Windows/ropeWindows source from M3StreamingContractTest (1083 checks). Those 1179 checks per JVM are reported separately and are not counted as JUnit methods. The full streaming filesystem/shard main was not invoked.

Coverage includes all65536 UTF16 units /851968 unit-search cases,55 exhaustive windows,1000 seeded ropes/12000 randomized searches,4294967297-unit coordinates,35 scripted mutable/throwing CharSequence callback schedules compared with the mechanically relocated original implementation,8000 concurrent queries, ownership isolation, summary/regex/streaming parity, Bloom collisions and range exclusions. Field/public API and unrelated-method fingerprints match; the new private helper is excluded from the unrelated-method fingerprint alongside indexOf.

Separate ASM instrumentation observes a late 16384-unit fixture in256 chunks: CharSequence.charAt calls32768→1, KMP failure arrays1→0, bounded JDK String calls0→1. Generic StringBuilder fallback retains32768 unit reads and one failure array. Index code invokes no materialization methods. Timings use uninstrumented classes.

## Measurements and tradeoffs

Each JVM records70 samples across ten scenarios (seven samples each):280 total. Two fresh processes per side use -Xbatch and CICompilerCount=2. Allocation is observed with ThreadMXBean. Values below are pooled per-call medians; full per-process/sample data is in qualification-proof.json. These are local Windows Java21 fixtures, not universal speed guarantees.

| Scenario | ns/call before → after | bytes/call before → after |
| --- | ---: | ---: |
| absent-whole-bloom | 65.41 → 58.87 | 64 → 64 |
| bloom-collision-miss | 3878243.75 → 54378.13 | 88 → 64 |
| bounded-range-miss | 34657.81 → 21658.20 | 208 → 184 |
| empty-needle | 15.08 → 14.17 | 0 → 0 |
| first-ascii | 289.08 → 294.63 | 88 → 64 |
| generic-needle-fallback | 358879.69 → 409007.81 | 88 → 88 |
| late-ascii | 15534631.25 → 40056.25 | 88 → 64 |
| late-owned-piece | 25256462.50 → 41525.00 | 88 → 64 |
| late-unicode | 10610656.25 → 40537.50 | 88 → 64 |
| multi-kmp-fallback | 410562.50 → 375128.13 | 88 → 88 |

Late-hit cases improve greatly by avoiding repeated directory traversal. Eligible search allocation drops24B by omitting the failure array. Early-hit pooled median is1.92% slower; generic StringBuilder fallback is13.97% slower in this run despite unchanged KMP source. These costs are explicitly retained in ADMISSION_DECISION.json. Empty/whole-Bloom-rejected paths and multi-unit KMP timing variation are not credited as implementation improvements. This is a reviewable tradeoff, not a no-regression claim.

V1's inline candidate passed correctness but regressed early-hit19% and generic fallback46%. V2's helper candidate passed correctness and brought generic fallback near parity, but early-hit regressed32%. Their full receipts, source afterimages and summaries remain under prior/. A V2 receipt was lost through a too-small tool-output budget; its truncated transport record is explicitly unadmitted. The same sealed V2 input was rerun with authenticated receipt persistence before console framing. The first callback transport failure is also retained; distinct one-use merge callback identities fixed it without relaxing duplicate refusal.

## Reuse, attribution and reproduction

Production directly uses the Java21 bounded String search API; reviewed OpenJDK jdk-21+35 commit890adb6410dab4606a4f26a942aed02fb2f55387, String.java blob5c234d4dfacac9e642578483640d4e6b41488beb. No OpenJDK GPL source is bundled. JGit release6.9.0.202403050737-r commitd764bc34fc15079379a71bb638e7c1b5836e061e is an existing qualification-only dependency; its full EDL-1.0 license and actual installed JAR about.html are retained. No JAR or JGit implementation is copied. Existing Hitesh Soliwal/Synexia Apache-2.0 headers and conceptual donor notices remain. See THIRD_PARTY_NOTICES.md, FOSS_REUSE_DECISION.tsv and ALGORITHM_CATALOGUE.json.

qualification-input.references.json reconstructs every source-bearing field from this crate. Resolve crate-resource-utf8 recursively, serialize compact ASCII JSON preserving insertion order/no finalLF, and verify sealed input Git blob fa142e70acb250ca8963f9f1d1c87b4dedf6a9b2. qualification.py is the exact executed runner; stdin-qualified.py pins the same input. bootstrap/, recipe-test/, production-before/, incumbent-tests/, reference-src/, contract-adapters/ and proof-src/ retain the tested source closure. The public M3 mirror requires no private Synexia source fetch.

Execution is host-bound to the existing Java21, Maven3.9.14,48 SHA256-pinned offline JARs, RAM compiler and installed read-only Snappy DLL. The runner overrides the legacy POM main to the RAM compiler. Verify explicit paths/native temp routing/capacity before invoking. Do not download/copy caches or fall back to another drive. Canonical plugin descriptors/resources are packaged separately; full reactor/plugin discovery was not run. Source drift refuses, replay reaches fixed point; rollback is a revert of the scoped PR.

The authoritative receipt SHA256 is e6b384fc0794c53eda77d29ab177baa51085ccd2269239f00cd5391f2a842362. S: retained0 task files; the545MiB cap and8GiB reserve were enforced. Read-only F is charged571469824B from a sealed R230 receipt, explicitly not a fresh whole-tree audit. No clones, snapshots, caches or retained build artifacts were created. Native cap approval remains unanswered/unused. This proves one existing Java search atom, not all lexicon migration, native ABI/full JDK image/reactor or ARM/Android/macOS/ESP/Pico behavior. The broader optimization goal remains active.
