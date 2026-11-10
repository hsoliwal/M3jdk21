# Replay guards and exact-head proof-kit triggers

Continuation of A3 replay at fb0b69967733a00b160af63bba1096f9e63b6471. Use existing owners only.

## Observed path-guard failure

The real broader module JUnit run fails M3VerbatimJavaPairRecipeTest.driftAndMalformedDescriptorsFailClosed. Its first malformed descriptor, ../escape.java, is admitted: the guard detects /../ inside a path but misses a leading ../. Preserve accepted relative source paths and separator normalization; reject leading parent traversal with the same existing exception. Strengthen the original test with slash/backslash traversal variants and valid leading ./ normalization. No new path framework or source rewriter.

## Observed proof-kit trigger gap

The existing proof kit collects full source, Maven, dependencies and full M3 reactor results. Its pull_request paths filter currently admits only edits to the workflow itself, so a real A3 recipe repair cannot run that proof kit. Add the existing migration-recipes and A3 module paths to that filter. Preserve same-repository guard, read-only token, toolchain pins, failure propagation, job limits, exact-head source archive, artifact expiry and all original steps. This schedules verification, not promotion.

Represent the exact workflow edit as com.m3.rewrite.backport.A3Kit using M3Jdk21HashPinnedTextSnapshotRecipe. Keep a separate preimage and sealed postimage. Test real named recipe activation, exact materialization, input drift/missing/duplicate refusal and fixed point. No replacement engine, no dependency changes and no POM/coverage gate weakening.

## Proof and boundary

Retain all existing tests. Run the actual JUnit tests for donor-pair admission and A3Kit with the recovered real Maven/cache. A template replay pass is not a passing GitHub workflow; exact-current reactor and all product/JDK tests remain independent gates. Prefix validation is a lexical recipe-descriptor guard, not a filesystem symlink or sandbox proof.

The earlier artifact is not silently treated as current master. Obtain exact candidate source through the corrected original proof-kit workflow. The remaining String/JNI parser failures are obligations, not skips or reasons to disable type validation.
