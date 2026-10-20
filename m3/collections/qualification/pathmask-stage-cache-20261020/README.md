# M3JDK21 native stage-cache refinement — 20 October 2026

This is an **in-place M3JDK21 JNI implementation** in `m3/collections/src/main/native/collections/m3_index_path_masks.c`. The existing M3 owner stays authoritative: `int[]` signed row IDs, `int[]` logical-to-physical offsets, `int[][]` state planes and required/forbidden bitmasks. `M3IndexPathMasks.select` remains the Java contract; filtered and sorted wrappers retain their distinct existing owners and active convergence PRs (#1503, #1522, #1508).

## Behavioral atom and Android motivation

A local, single-run JNI microbenchmark found that eager metadata caching regressed empty selections (8-stage path: roughly 83 ns/call incumbent versus 618 ns/call cached) but improved an 8-row, 8-stage example (roughly 2,224 ns/call incumbent versus 1,218 ns/call cached). Accordingly, the final candidate keeps the original path for fewer than 8 rows. These noisy local microbench figures are selection rationale, not production or device performance proof.\n\nThe incumbent C kernel reads `required`, `forbidden` and the current plane JNI local reference repeatedly for **every** logical row and stage. The selected atom uses a bounded 8-stage, call-local C metadata struct. It validates and loads only primitive mask words, lengths and local refs once; every state word is still fetched with `GetIntArrayRegion`. JNI references are explicitly released at both successful and failing exits, no `GetPrimitiveArrayCritical`, no retained heap pointer, native allocation, callback, persistent JNI reference or Java object-valued result.

- 1–8 stages and at least 8 logical rows: cached mask/plane metadata, two-pass stable count and exact-sized output.
- 9+ stages, or fewer than 8 rows: unchanged original JNI matcher, with per-stage local-reference cleanup.
- Signed row IDs, duplicate occurrences, stride directions, empty selections, geometry/refusal, full-width 32-bit masks and encounter order are preserved.
- Caller arrays are borrowed during the call only; source mutation while traversing remains outside the existing contract.
- Android DEX indexes, ART memory constraints and official JNI recommendations are *inputs to M3JDK21 architecture*, not a separately installed compatibility layer.

## Serial qualified source recipes

All product changes have source-pinned recipe images. Apply **in this order**, under their respective original source guards:
1. `com.synexia.rewrite.M3NativeRecipePath20261020`: extends the existing OpenRewrite source-sealed engine by allowing native C/H/CC/CPP paths only with `lst:false` in established M3JDK21 JNI folders, and creates Java path-admission tests. It preserves the original Java LST-only mode.
2. Rebuild the recipe engine from the exact new source using the existing source-pinned Maven control reactor.
3. `com.synexia.rewrite.M3NativePathMaskStageCache20261020`: actual existing C owner, exact before SHA-256 `3f5a760e633762469fad5a9dcd531cb39c33d11752d79ad241e506800b26e7cb`, after SHA-256 `559f4cfe5a1fa1eb6f77d8c7cd805915b7bc09ae91abbc87914a9e1543b63058`; PlainText-only source custody, not AST proof for C.
4. `com.synexia.rewrite.M3NativePathMaskTest20261020`: Java LST fixture pin and parity suite creation.

Each recipe refuses unknown source drift, preserves already-qualified targets as a fixed point, and touches its own semantic owner. The verifier checks source/template SHA-256 after the recipe. Do not hand-edit generated targets to correct an implementation: fix template and its hash, rerun, qualify and compare contracts.

## Local Java/JNI test

```sh
bash m3/collections/qualification/pathmask-stage-cache-20261020/verify.sh
```

Requires a JDK 21, C17 compiler, Python 3, JNI headers and UBSan on Linux. It compiles the **actual repo M3IndexPathMasks.java owner** and new test, compiles the actual C JNI owner with warnings-as-errors and UBSan, runs Java CheckJNI and verifies all three source seals.

Candidate C17 `-Werror` build passed locally; a local Java reference harness (not the complete repository class) ran 2,120 Java/JNI parity scenarios: 1,143 cached and 977 fallback cases. That is **not** proof of real Android/ART, full Maven recipe replay, installed JDK21 image, complete collection module tests or speed/energy superiority. The new workflow executes the actual production Java owner, which is a separate gate.

## Remaining acceptance

- Maven/OpenRewrite recipe admission, fixed point, refusal tests, broader existing `m3/collections` module/Java/JNI tests and parallel integration.
- Java 21 product build, jtreg/JCK, JNI ABI/symbol parity, full Windows/macOS/Linux native matrix, proper ARM64 ART/NDK device execution.
- Cold and warmed CPU, PSS/RSS/retained heap, JNI crossings, battery and contention across real workloads. A cache is a candidate, not a performance claim.
- Future algorithm category passes: filtered writable/read-only wrappers, sorted and navigable views, physical index projections and state-mask scans must converge in their authoritative M3JDK21 owners while preserving public behavior.

No release claim is promoted by this candidate.
