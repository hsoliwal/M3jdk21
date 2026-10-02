# Feature and acceptance matrix

| Responsibility | This slice | Open gate |
|---|---|---|
| Source family inventory | Six production files with hashes/declarations; four inspected ownership/bridge/MatIndex/word documents; 26 mapping/disposition entries | Exhaustive repository/branch/resource/non-prefix dependency coverage |
| Route A explicit view | Compiled and tested M3Text over preserved frozen/joined owner | Complete String surface, bounded interner, production shared-loader lifetime |
| Route B compiler lowering | No new implementation | Existing compiler inventory, typed refusal, evaluation/exception/identity/escape and invokedynamic tests |
| Route C complete modified JDK | No new implementation; #6 historical candidate has merged metadata, not new acceptance | Full exact-head build; String/VM/JNI/JVMTI/GC/JIT/CDS/dedup/serialization/serviceability gates |
| UTF-16 | Exhaustive code units, random segmentation, NUL, paired/unpaired surrogates, seam encoding/search in test slice | Full API/locale/charset distribution |
| Comparison/hash | Exact text comparisons, String polynomial hash, alternative segmentation | Full public API surface; cryptographic identity remains separate |
| Regex | Calls actual java.util.regex on CharSequence; region/capture/replace/Unicode seam checks | Full engine conformance suite; no RE2/J or GPU equivalence claim |
| Mutation isolation | Admission snapshot, writable independent outputs, no partial output on tested invalid ranges | External mutation/truncation of mapped files |
| Payload reuse | Retained owners/ranges checked separately from descriptor allocation | Complete heap/native/mapped retention profiling and lifetime stress |
| Recipe | Exact source/resources/output hashes, drift refusal, replay, partial install, rollback, cross-JVM determinism | General OpenRewrite/compiler transformations; hostile concurrent filesystem transaction model |
| CI | Local Java runner and separate exact-head CI workflow provided; no existing gates disabled | Hosted execution and Maven entrypoint not observed |
| Performance | Three-fork warm allocation/latency probe | Cold admission, p50/p95/p99, varied distributions, GC/cache pressure, OS/native retention and amortization |

Test counts are generated assertion/check counts, not counts of named independent
unit-test methods. Source/preimage and target suites execute in different classpaths.
A passing explicit-view test is never recorded as compiler or modified-JDK acceptance.

The supplied historical #6 receipts remain evidence tied to that separate candidate:
86 flag-off passes; 16 enabled passes and two enabled StringJoiner OOME test failures;
enabled interpreter only. They were not re-run here. No expectations were changed,
no tests were deleted, and no unavailable dependency/download gate was marked passed.

## Publication reconciliation (2026-10-02)

Source `develop` advanced to `8830a34a042d79e2d1b89b850d177c3038bf975e`.
This port stays pinned to `75fb1abaecb56969bea2914520bbe819f130632b`;
later source synchronization is pending. PR #7526 is now merged at
`61a1b72ca791a33ad64187f681daccfbf6657414`, rather than its initially observed draft state.

Target `master` advanced to `8bb6215372e07712f1fdf5a0cb912af495007b19`,
whose tree is **identical** to original baseline `3029cff...`: both are
`26554d002cf158c97dd4ffbb06b3885b647c0a17`. Runtime #6 merged into its
feature-branch base at `305dc277139b0e7cff1f4e284e7019a381480153`; that
merge is in the newer master's ancestry. This is not evidence that the runtime
implementation is present in the master's unchanged foundation tree. The two
historical enabled StringJoiner OOME failures and missing JIT acceptance remain
open. The other PR states above are initial observations, not a fresh global scan.

Production port commit: `c68411504e9bf0fbefd1adc582f075d05bbd83e4`,
parented on the tree-equivalent newer master. Permanent mapping references pin
this production commit separately from later metadata commits, avoiding a
self-referential commit hash. No default branch was updated or merged by this task.
