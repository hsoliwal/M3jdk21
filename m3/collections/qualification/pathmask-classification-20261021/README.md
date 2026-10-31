# M3JDK21 full-width index-path mask terminal classification (2026-10-21)

**Product:** M3JDK21 (OpenJDK 21 + the M3-native Java/JNI implementation as one SDK).
**Owning source:** `m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java`.
No external collection wrapper, relocated storage class, per-entry node, or extra runtime integration layer was introduced.

## The specific behavioral atom

The existing API borrows `int[]` logical row IDs, `int[]` physical offsets, aligned `int[][]` state planes, and two complete 32-bit mask vectors. The public `select` **first** validates every logical path coordinate and physical offset for its selected window; even an impossible mask cannot bypass these exceptions. Backend dispatch remains `JAVA` vs explicit `NATIVE_REQUIRED`, with no silent fallback.

Only the existing `selectJava` terminal atom changes. For eight or more logical rows:

- When all required/forbidden masks are zero, emit the original encounter-order row IDs without reading individual state words, using one new exact-sized detached `int[]`; the contiguous +1 path may use `Arrays.copyOfRange`.
- When the first nontrivial stage has overlapping required and forbidden bits, no row can match, so return a new empty array after complete geometry validation. All signed 32-bit mask bits, including the sign bit, remain meaningful.
- For the **first nontrivial but compatible stage**, immediately delegate to the exact pre-existing two-pass count/emit implementation. Later-stage contradictions are not separately optimized in this pass. Small scans (fewer than eight rows) retain the unchanged algorithm to avoid metadata prepass overhead.

This preserves signed and duplicate row IDs, negative and strided logical paths, detached output ownership, error ordering for invalid geometry, JNI backend requirement, and unchanged source arrays. JNI C code and symbol signatures are unchanged. The method's contract still excludes concurrent mutation of borrowed arrays.

## M3Scale serial recipe custody

Use the existing Maven/OpenRewrite `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` (Java LST mode), with the new recipe `com.synexia.rewrite.M3IndexPathMaskClassification20261021`. The manifest under `m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-index-pathmask-classification-20261021/` seals two Java files and records exact before/post SHA-256 values. Required original product SHA-256 is `33b894d2b4c9f89a0244023737d49645d54cbcb1b388a0956e475567fdd4a577`, proposed new source SHA-256 is `a7055268950266c3ed19bc38bcecb002d81ca72e9d58b4d996d2d268cb1a2f62`, and the new proof source is guarded as absent.

Refine **one source file/one replaceable atom per pass**. Never hand-edit generated postimages to iterate: change the recipe template, update its hash, rerun on a copy of the expected source epoch, prove a fixed-point second replay and drift refusal, then publish the recipe, tests, and generated source together. The repository's pinned Maven/OpenRewrite dependency closure has outstanding source-build gates; this PR does **not** claim actual Maven recipe replay until those are resolved.

## Executable product-owner Java/JNI proof

```bash
bash m3/collections/qualification/pathmask-classification-20261021/verify.sh
```

Requires Java 21, Python 3, GCC/C17/JNI headers, and UBSan on Linux. It verifies **both** source/template postimages against their recorded SHA-256 digests, compiles the actual M3JDK21 `M3IndexPathMasks.java` (not a surrogate) with all Java warnings treated as errors, and performs 3,092 deterministic seeded reference differentials on the actual production Java implementation. It then builds the **existing** JNI C17 selector with UBSan and reruns the proof under `-Xcheck:jni` with explicit native availability. The native subset includes zero/conflict/general masks and stride directions.

A local Java 21 fixture implementing the same selected algorithm passed 3,092 comparisons; that fixture is **not** a substitute for the product-owner CI. Source/source-template exact digests and CI product builds remain the acceptance gate. The previous local screening of the candidate algorithm showed that a full classification pass can regress normal filters and short scans, motivating the early bailout and eight-row threshold; its timings are not a validated M3JDK21 or Android speedup.

## Donor catalogue and superset preservation

See `m3/collections/algorithm-catalog/pathmask-terminal-classification-20261021.tsv`. Entries identify category, original owner, complexity, source, donor rights and precise accept/keep/reject decisions. LeetCode #27's in-place deletion is **rejected** for this detached selection; HackerRank BitSet and GeeksforGeeks bit-mask examples supply concepts only. No third-party code was copied.

Filtered/sorted live views, the existing `java.util` M3JDK21 implementation, indexed snapshots and native mask windows have separate authoritative contracts. Do **not** overwrite active convergence in PRs #1503, #1522, #1508 or #1545. The old and new Java paths maintain a superset of those existing operations, rather than introducing another `M3 + JDK` layer.

## Still to qualify

Full Maven/OpenRewrite replay and source-closure install, jtreg/JCK and matched HotSpot/JDK image, all collection/API tests, JNI native platform matrix, Android ART/NDK/ARM64 device startup, PSS/RSS, battery/thermal impact, and reproducible head-to-head performance measurements. No complete-product readiness or universal performance claim is made.
