# M3JDK21 agent invariants

## Product/runtime ownership

M3JDK21 owns the applied JDK product: Java, HotSpot, JNI/native, runtime storage, runtime
precompute, JDK tests, serviceability and platform qualification. For String architecture the
canonical internal term is **M3 String**.

Synexia is a donor/convergence workspace. M3JDK21 must not gain a runtime Maven/package/service
dependency on Synexia.

## Recipe custody — non-negotiable

Reusable recipes that target M3JDK21 are authored, versioned and proved in
`hsoliwal/com.synexia`, under `synexia-openrewrite-recipes`. M3JDK21 is the application target,
not a second canonical recipe repository.

For a recipe-driven change:

1. update/prove the recipe in Synexia first;
2. bind exact M3JDK21 target paths and pre/post SHA-256 there;
3. apply it to the exact M3JDK21 source revision;
4. keep the applied Java/HotSpot/JNI/native code and target-native tests here;
5. run M3JDK21 compiler/jtreg/runtime gates here;
6. keep only a compact provenance/application receipt here.

Do not recreate or retain duplicate authoritative Synexia recipe manifests, sealed postimage
templates or recipe fixed-point tests in M3JDK21 after application.

Current canonical recipe owners for the M3 String work include:

- `com.synexia.rewrite.M3Jdk21StringHistoryConvergence`
- `com.synexia.rewrite.M3Jdk21TqConvergence`
- `com.synexia.rewrite.M3Jdk21StringAdaptivePreparedSearch`
- `com.synexia.rewrite.M3Jdk21StringExactPositionMasks`
- `com.synexia.m3.TranslateEscapesCanonical`
- `com.synexia.m3.ValueOfCharCanonical`
- `com.synexia.m3.DeprecatedGetBytesBulk`
- `com.synexia.m3.GenericCharsetDirect`
- `com.synexia.m3.JniShadowBulk`

Focused recipe custody is consolidated on Synexia PR #10004. A target-side copy is a receipt or
application artifact only, never a second canonical recipe owner.

See `m3/docs/synexia-recipe-application.md`.

## Runtime precompute boundary

All accepted M3 String facts, indexes, search plans, position masks, regex candidate facts and
native/runtime execution state live inside M3JDK21. Synexia owns donor algorithms and recipes, not
the running precompute service or canonical JDK payload.
