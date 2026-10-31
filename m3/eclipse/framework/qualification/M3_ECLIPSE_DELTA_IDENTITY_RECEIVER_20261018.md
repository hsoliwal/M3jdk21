<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Eclipse resource-delta identity receiver

Status: additive standalone Eclipse framework adapter candidate, NOT production Eclipse acceptance.
No java.base, HotSpot, M3String, M3File, SWT, JFace or Eclipse plugin implementation is replaced.
The active Synexia -> M3JDK21 M3String receiving stage remains unchanged.

## Ownership and source handoff

- Canonical recipe home: `hsoliwal/com.synexia`, draft PR #12251.
- Canonical exact commit: `32f65f021fb8d774694c39d861c5d6e745cca4a0`.
- Recipe: `com.synexia.rewrite.M3EclipseDeltaIdentity20261018`, using the existing
  `M3HashPinnedJavaSnapshotRecipe`, not a copied rewrite engine.
- Target source baseline: `hsoliwal/M3JDK21@f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`.
- Target modified owner: `m3/eclipse/framework/src/main/java/jdk/m3/eclipse/framework/M3EclipseResourceDeltaSnapshot.java`.
- Owner original Git blob: `1523a11f6b3e08437275c4897f5ad7ca1e7555bd`.
- Owner before SHA-256: `1ce2e7fac96924f307ab150a21e93303d2d0f1ecd422c68d6ab20b35039b276c`.
- Owner after SHA-256: `21a893d7f976e5bf2d42998335d99556edfffb266016cce15d5784ebc1c19274`.
- New test SHA-256: `44192b4116f99413f62dc5bed280d065de8bab0efffdd5441af628d43ade51da`.
- Copyright: 2026 Hitesh Soliwal and contributors; authored source Apache-2.0.
  Eclipse-provided API dependencies retain their upstream licenses.

## Replaced behavior atom

Temporary visited-reference state now uses existing `M3ObjectIdentitySet<IResourceDelta>`
rather than `IdentityHashMap<IResourceDelta, Boolean>`. This is a concrete storage
substitution only. The target does not create another M3 collection owner.
`M3ObjectIdentitySet` uses Java reference identity (==), retains one flat reference
lane, and does not consult the domain's equals/hashCode.

The Eclipse `IResourceDelta` remains authoritative for child iteration, kind,
original object lifetime and changes outside the capture. BFS ordering, parent ordinals,
captured kind masks, duplicate/cycle refusal, node-count limit, null/error behavior,
and all public method descriptors remain unchanged. No JNI symbol or native object
lifetime change is introduced. No runtime dependency on Synexia is added.

## Executed evidence

Isolated Java 21 `--release 21 -Xlint:all -Werror` source/test compile: PASS,
with test-scaffold stand-ins for missing Eclipse and M3 dependencies.
Four focused tests: PASS in that explicitly stub-assisted setup.
These checks do NOT establish real M3ObjectIdentitySet or Eclipse module behavior.

## Required pending gates

- Canonical Synexia Maven/OpenRewrite execution: fixed-point, drift/occupied refusal.
- Exact-head M3JDK21 Eclipse framework Maven compile and JUnit using real provided Eclipse/OSGi APIs.
- JDK/OSGi Equinox bundle and original source closure checks.
- Original binary-compatibility/public API and alias/exception differential tests.
- Real M3ObjectIdentitySet collision/growth workloads, memory allocation and CPU baseline comparison.
- Target JDK jtreg/HotSpot and JNI/native parity only if later scope crosses those boundaries.
- Cross-platform Eclipse/PDE workbench integration where the forwarding surface is installed.

Commands after installing the existing `com.m3:m3-collections:0.1.0-SNAPSHOT` artifact:
```sh
mvn -B -f m3/eclipse/framework/pom.xml verify
```

This packet is an E1 indexed collection-backed model candidate and does not advance
the global M3String -> arrays -> collections -> AST receiving stage.
