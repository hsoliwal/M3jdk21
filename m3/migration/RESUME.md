# MIndex → M3 migration resume record

Snapshot: 2026-10-02.

## Exact baselines

- Synexia `develop`: `6df9df8d8f42111239013ee941723ec37f97ba6e` (private).
- M3jdk21 `master`: `8bb6215372e07712f1fdf5a0cb912af495007b19` (public at inventory time).
- Built Route-C candidate: `3776d6e674d6c9b04539ca24aca2504aa88d4a57`.
- Shared-atom contract doc head: `1210d76ce80263003981c8c6ba5bb4175524358a`.

PR #6 was first merged into a feature-stack base and later recorded through master convergence PRs. Inspect target files/tree content instead of treating PR state alone as proof that runtime postimages are present on master.

## Implemented on this branch

- Route A: `M3Text`, a stock-JVM immutable `CharSequence` over existing M3 immutable pieces. Admission copies exact UTF-16 once; concat, slice and repeat retain payload references. Ordinary String, char[] and encoded bytes are explicit materialization boundaries.
- Route B target: `M3TextCompilerRuntime`. It is intentionally narrower than general compiler lowering; no blanket Java rewrite is authorized.
- Permanent mapping: schema, fail-closed validator, negative tests, generated coverage and this resume protocol.
- OpenRewrite candidate inventory: typed String concat/substring/repeat sites are marked for review; the recipe is non-mutating.
- Local JDK 21 execution before publication: `javac --release 21 -Xlint:all -Werror`; `M3_TEXT_ROUTE_PASS checks=48`; manifest validator positive case + five refusal cases passed.

## Route C evidence retained, not relabeled

The exact `3776d6e...` candidate historically records a complete fastdebug image build, 86/86 selected flag-off tests, and 16 enabled passes with two enabled StringJoiner test files still failing six OOME expectations. Enabled execution is interpreter-only. Full jtreg/JCK, compiled segmented mode and live SA attach remain unverified. Those receipts belong to that exact candidate, not automatically to this branch.

## Current source convergence to reconcile

Active Synexia lines observed during inventory include #7576 (three-route/substring contract), #7577 (absolute reference storage), #7578 (pageable word signals + route gates), #7540 (tuple DAG), #7547 (Java/JNI search superset), #7549 (mapped facts/JNI shadows), and #7558 (exact mapped catalog + VM-local misses). Treat each as candidate work until its exact head is adopted and gated.

## Explicit inventory limitation

The available connector can fetch exact known private paths and PR file lists but did not expose a complete private-repository directory/code index. The current manifest therefore gives every concrete owner discovered during this pass a disposition, but it is a **seeded inventory**, not the required exhaustive closure. Completion is forbidden until that gap is closed.

## Next dependency-ready passes

1. Complete private-source family enumeration and add every additional owner/resource/test to the manifest.
2. Port `MIndexAtomStore` into a dependency-minimal M3 structural module; then port `MIndexInteraction`, `MIndexAst`, and `MIndexDag`. Keep them outside `java.base`.
3. Close the full MatIndex and historical `com.synexia.mindex` dependency closure before compatibility consolidation.
4. Promote the typed OpenRewrite candidate inventory to a mutating lowering recipe only after tests prove evaluation order, exceptions, overloads, constants, identity-sensitive behavior, unresolved-type refusal and mixed transformed/untransformed callers.
5. Diff post-`6df9df8...` storage/search/precompute work against Route C `3776d6e...` before another JDK edit.
6. Rebuild a complete matched JDK and rerun mandatory Route-C gates on the resulting exact head.

Maven is not installed in the execution container, so the OpenRewrite crate has not been compiled here. No merge, installed-JDK replacement, account-setting change or private-source vendoring is authorized by this branch.
