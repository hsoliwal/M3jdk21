# M3JDK21 native build-state record — review candidate (2026-10-20)

**Product:** hsoliwal/M3JDK21 is an integrated SDK, *not* an M3 layer over JDK. This is an additive feature of the already merged `com.m3.indexdb.M3IndexDB` owner. It neither adds an independent database nor changes `javac`, HotSpot, Maven, or JNI symbols.

## Existing owners and overlap

- Original `M3IndexDB` persists content-addressed, SHA-256-verified artifacts; `M3BuildState.store/load` uses it unchanged.
- `M3FileDeltaLedger` source/class byte hashes are handled in pending M3JDK21 #1568, and the compiler source projections in #1573.
- Existing java.base / m3 collection filtered/sorted and JNI owners are covered in #1451, #1549 and combined #1577. No replacement wrappers are added here.

## Indexed data and decisions

The three generated Java files contain one storage type and its contract/JUnit. Snapshot rows are sorted unique names with flat content SHA-256, ABI SHA-256, ABI-known masks and kind flags. Dependency edges are canonical consumer→provider ordinal pairs. Reverse CSR is built in `O(V+E)`, then a mask/queue closure selects source rows for recompilation.

If source changes but compiler-confirmed ABI remains identical, only that source is directly marked. Changed/unknown ABI propagates to dependents. Changed external classes with identical confirmed ABI do not trigger Java dependent compilation. A changed build input, removed tracked row, or build-context hash mismatch conservatively invalidates the relevant entire source set. Cycles terminate. Exact graph-edge edits are compared by provider names rather than positional IDs; changed dependencies invalidate the owning source even when source bytes remain identical.

**Safety:** Caller-supplied ABI and context facts are not manufactured or authenticated by this class. A plan is not an authorization to skip Maven goals; outputs must exist, source/class content be current, ABI be compiler-derived, and annotation processors, plugins, resource generation and dependency/environment inputs must be accounted for. Use clean builds if that cannot be proven. Snapshot write is through the original M3IndexDB refs/blobs, and does not claim an atomic transaction across all build outputs or a new SQLite/JNI backend.

## Recipe-first origin

Canonical [Synexia #12583](https://github.com/hsoliwal/com.synexia/pull/12583), recipe `com.synexia.m3.M3BuildStateIndex20261020`, uses existing Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe`. All three receiver Java files are identical Git blobs to the recipe templates. Each source has an ABSENT preimage and SHA-256-sealed afterimage; repeat application must be fixed-point, unexpected preimages rejected.

```text
M3BuildState.java          SHA-256 5f3d60663635567345edaa4065563a2645dc31765811284ba838e3fdfd865232
M3BuildStateContract.java  SHA-256 a99152f068eceb23121c610522577026c4f867f55b8a7619666b56e979370631
M3BuildStateTest.java      SHA-256 045a438eee2574df76612cb53a82b3743d272e8e13ac5ca1cd36669714322888
```

## Executed/local versus pending gates

Local Java21 `--release 21 -Xlint:all -Werror`: passed. Standalone `M3_BUILD_STATE_PASS checks=49` **used narrow stand-ins** for the existing `M3IndexDB/M3IndexDbArtifact` API, not the actual repository store. This receiving workflow exercises the *actual* owner and retains existing Maven JUnit **99% line and branch coverage** requirements unchanged.

Canonical Maven/OpenRewrite recipe JUnit, target Maven/JUnit/JaCoCo, actual JNI SQLite, modified JDK image, jtreg/JCK, multi-platform tests, end-to-end Maven/Gradle parsing and compile avoidance, output-cache identity and performance benchmarks remain **unverified** until their exact-head run evidence is inspected. Do not merge as a universal build cache. No challenge-code donor source was copied.
