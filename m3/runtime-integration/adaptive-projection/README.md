# M3String adaptive UTF-16 projection receiver (2026-10-16)

**Status: draft, runtime admission HOLD.** Paired first-party source/recipe review: [Synexia #11886](https://github.com/hsoliwal/com.synexia/pull/11886). M3JDK21 alone owns accepted java.base, JNI, HotSpot, jtreg and release builds.

## Additive received files

- `src/java.base/share/classes/java/lang/M3StringProjection.java`: original Synexia generated Git blob `59f80a443ac43281ab22427378f031c5aedbb773` (content SHA256 `2cb6695adbd08074142e196fc314d60d50e1eb0d4810c27b12d0f39dfe2a31c4`).
- `test/jdk/java/lang/String/M3StringProjectionTest.java`: original Synexia generated Git blob `8f59fc8b58f5aaf83a01edc18e7f17678192fe04` (content SHA256 `b87bc91196ec87f4654a10e0c7c5c7c7450d2e6279598222d03feaea6dd0710a`).

The source-sealed Java21 proof crate at Synexia `synexia-openrewrite-recipes/recipes/m3jdk21-adaptive-projection-20261016` uses the incumbent `M3HashPinnedJavaSnapshotRecipe`. Both target paths are manifest `ABSENT` before installation, and its test proves fixed-point replay and refuse-unknown-drift.

## Behavior and ownership

Package-private immutable `M3StringProjection` implements CharSequence over the existing `String` or `java.lang.M3String` owner, or a prior immutable projection. EMPTY, IDENTITY, RANGE, RUNS, BITMAP geometry is chosen by source selection. Nested masks flatten into the original owner; selected source runs are directly copied by the existing String/M3String bulk range APIs. It adds no text pool, Java String public API, per-value char/byte array, address registry, new JNI symbol or replacement Pattern engine.

Operations: logical UTF16 `charAt`/`subSequence`/bulk `getChars`, exact String hash, code point navigation across masked-out gaps, immutable prepared KMP literals, lexical comparison, ends/starts/content equals, explicit fresh char[]/String materialization. The `String` wrapper still owns Java object identity. The original M3String `owner + packed long` instance shape and `M3StringOwner permits Atom, Tuple` contract remain unchanged. This receiver is a new **internal execution capability**, not a claim that ordinary String method calls automatically select masked projections.

## Evidence and promotion

- Independently compiled with Java 21 `--release 21 -proc:none -Xlint:all -Werror` in isolated model with source-locked class and test templates; 39,252 checks passed.
- Both source files match their exact donor Git blobs.
- Maven/OpenRewrite plugin execution, **full source-built patched JDK**, HotSpot/JNI lifetime/intrinsics, Jtreg with `-XX:+UseM3StringStorage` and CompactStrings both modes are **not executed here**.
- Preserve repair stacks M3JDK21 #972 (lost blobs), #967 (source guard), #963 (parser gate), #968 (modified UTF8). Do not overwrite the existing M3String, String, JNI, native pool or unrelated source; reconcile source-guards separately.
- No default branch changes, force pushes, rebases, squash, or automatic merging.

## Collection and use-case coordination

Synexia #11773 holds immutable filtered/sorted MIndexRows views, and #11813/#11865 hold live collection/map decorators. String projection supplies exact UTF-16 comparison for those consumers but this receiver deliberately does not duplicate collection implementations.

## Runbooks

Run `python3 synexia-openrewrite-recipes/recipes/m3jdk21-adaptive-projection-20261016/verify.py` in the Synexia branch, or `mvn -f .../pom.xml verify` with Maven. After producing a real M3JDK21 image, run `M3StringProjectionTest.java` with the recorded Jtreg flags in both compact modes. Full JDK String and JNI differential regression suites are required for product admission.
