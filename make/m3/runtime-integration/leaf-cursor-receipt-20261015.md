# M3String canonical tuple leaf cursor — exact receiver, 2026-10-15

**Runtime owner:** `hsoliwal/M3jdk21`, source before `ed0d5435eb8796cfd4c9375edfc05b72bb16c8c6`.
**Canonical authoring:** `hsoliwal/com.synexia` recipe commit `ed7da73bc2c1fd958e9d250acf308e8bb46c7b9e`,
named recipe `com.synexia.m3.M3Jdk21LeafCursor20261015`.
Source SHA256: `f8bac45d1623d25470e1b79beffa276f271db10e297ef5b434d5335c9d4c908b`
→ `b58c82047f795807ff32229146c370c9e596eff19942a92cd0128ce4987d20ca`.

Only private `M3StringPool.LeafCursor` and its `ArrayDeque` import changed. No public JDK String method, JNI symbol, HotSpot field, intern semantics, canonical native storage, or payload layout changed. The old walker constructed a `M3String.range()` per traversed tuple child merely to transport owner/offset/length; the new walker uses a private owner array and primitive packed coordinate array, cleared on pop. Exact original leaf order and adjacent identical-owner range coalescing are preserved.

Target source result and target jtreg source are exact postimages from Synexia's hash-sealed Maven/OpenRewrite recipe, including explicit ABSENT-before admission for `M3StringLeafCursorCanonicalTest`.

## Verification and admission

Executed locally on **stock** Java21: independent model with actual historical/candidate leaf normalization shapes under `-Xmx32m`: 169,848 assertions, baseline 21,487 temporary range wrappers, candidate zero. Separate independent String contract oracle 44,789 assertions under `-Xmx32m`. Finite isolated tests are *not* proof of matched M3JDK/HotSpot production behavior, overall allocation rate, or speedup.

**NOT RUN:** actual canonical OpenRewrite SDK/JUnit reactor, full M3JDK21 Java-base + HotSpot build, jtreg test with experimental `UseM3StringStorage` enabled, JNI native acquire/release, GC/JIT/SA/CDS/JFR/serviceability, full platform matrix and pressure benchmarks. Keep this receiver PR **draft**. The existing flat String admission fallback remains possible; this patch does not claim all Java strings are native-backed.

Development archaeology: no force push, squash, or rebase of `master` or Synexia `develop`.
