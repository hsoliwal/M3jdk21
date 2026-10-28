<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 original focused signal workflow dependency closure — 2026-10-27

This is **result-only** documentation for a source-sealed Synexia
`M3SignalFocusedCiDependencyClosureRecipe`, not a product-side recipe.
Five original M3JDK21 GitHub Actions workflows have been refined in-place.
**No original Java algorithm, public API, JNI symbol, test selector or
native/runtime source changed; no existing job was removed or skipped.**

Original hosted consolidated [run 40536671585](https://github.com/hsoliwal/M3jdk21/actions/runs/40536671585)
completed successfully: all 29 source postimage Git+SHA256 verifications,
strict original Java21 normal/`-Xint` deterministic tests, source-vendored
JUnit and arrays install, and **13 real original collections JUnit tests
with zero failures, errors or skips**.

Other independent focused workflows on the same commit failed at earlier
**compiler dependency closure** rather than their executable oracles:
- original Boolean and viewport source checks omitted
  `M3SignalTopK` and `M3SignalExactIntegerDistance` after the public
  query owner began referencing these unchanged original classes;
- original ranked source check omitted `M3SignalExactIntegerDistance`;
- javac plugin integration compiled `M3SourceSignalPlugin` without its
  existing `M3MethodSignals` owner;
- the oldest typed-columns Maven/JUnit job omitted its original
  source-vendored JUnit and first-party `m3-arrays` bootstrap, so Maven
  refused missing dependency resolution before running tests.

This receiving change adds **only** the missing owner source inputs and
first-party dependency installations. All old focused source oracles and
JUnit names remain intact and independently executable. Trigger path
coverage is extended for the relevant original dependencies.

Canonical `M3HashPinnedTextSnapshotRecipe` in private Synexia owns
exact before/after SHA-256, first apply, second no-op fixed point,
missing/drift refusal, donor decision catalogue and correction memory.
The public product receives only five YAML postimages and this passive
receipt. Current-head focused GitHub workflows and official OpenRewrite SDK
must actually pass before promotion. No observed passing mainline JDK/JCK,
JNI/GC/Windows/Linux or CPU/RSS claim follows from CI changes alone.

Remaining older typed-domain, progressive/JNI and broad five-provider
SDK failures are separate admission items; this bounded closure does not
assert they are fixed.
