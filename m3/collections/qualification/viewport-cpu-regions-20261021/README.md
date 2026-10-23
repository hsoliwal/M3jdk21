# M3JDK21 intrinsic viewport — review proposal, runtime NOT applied

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

**M3JDK21 is the integrated SDK. This is not an M3 + JDK layer.** The existing widget owns its viewport; canonical CPU model state and projections remain independent of replaceable rendering and theme/language presentation. See CPU_RENDER_CONTRACT.md.

## Exact source custody
Canonical recipe and candidate authority: [Synexia #12855](https://github.com/hsoliwal/com.synexia/pull/12855), immutable commit `0074a3195fd53763eecf8eb558ebafd66821d3fd`.

Dataset: `synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/viewport-cpu-regions-20261021/`.

Receiving baseline: `3666a7186f297e517261855636cb10e7a236a8ee`.

`owner-candidate.patch` is an **inert review diff**, SHA-256 `bf009c1bded000b485a8bad676c62d12c47e98665d0481b76c3ca5ea612063ac`. It describes only the two existing owner changes. The complete new Java test, original preimages, candidate postimages, category/donor catalogue, recipe memory and independently executed fixture reproduction script remain canonical in that Synexia dataset. `SOURCE_MANIFEST.tsv` binds all three intended target sources.

Runtime Java/C sources are deliberately unchanged by this proposal. The exact existing `M3HashPinnedJavaSnapshotRecipe` / `M3HashPinnedTextSnapshotRecipe` Maven execution, fixed-point and refusal gates have not run here. Applying a raw patch or copying the templates would not satisfy those gates. No second recipe engine is added to this target.

## Candidate implementation
The existing Java page selector remembers the first requested matching physical ordinal during its complete preflight, then copies from there. It preserves complete validation, encounter order, duplicates, signed state bits, destination fences and alias scratch. Preflight remains O(n), not O(viewport).

The existing JNI selector uses fixed 128-element region reads instead of acquiring both whole input arrays. It retains O(selected) output scratch and one writeback; its JNI declaration/export is unchanged. Defensive count-drift refusal does not provide a concurrent snapshot; inputs still require owner synchronization. No renderer, control, public collection facade, theme system, language engine or GPU backend is implemented by these two atoms.

## Executed host checks and acceptance limits
Strict Java21, GCC/Clang C17 lint and GCC fatal-UBSan native builds passed. Exact Java descriptors, generated JNI headers and exported symbol remained unchanged. The frozen baseline and candidate each passed 72,591 Java-only checks and 108,891 Java/JNI checks in mixed and interpreted modes; JNI ran with -Xcheck:jni and 64 MiB SerialGC, with empty runtime stderr. A second independent run of the canonical verify-host.sh reproduced the same observation digests. Counts overlap across modes; no summed coverage claim.

Maven/OpenRewrite execution was blocked by absent local Maven and unavailable dependency acquisition. Full original collection-module tests, all five wrapper-provider contracts, source-built JDK/HotSpot/jtreg/JCK, native SWT/Nebula/GEF/RWT widgets, theme/language/IME/accessibility/GPU integration and cross-platform/concurrency/resource matrices remain pending. No performance generalization or whole-chat completion claim.

## Naming and promotion disposition
Existing internal owners and public APIs retain their names. No source rename, module move or public M3 facade is introduced. Name-contract and receiving-stage promotion are **PENDING**, not inferred from this proposal. Preserve String -> arrays -> collections receiving order and all current historical gates. After actual canonical recipe qualification, receive exact SDK results serially into a separately reviewed runtime-source commit and rerun the owning target gates. No automatic merge is requested.
