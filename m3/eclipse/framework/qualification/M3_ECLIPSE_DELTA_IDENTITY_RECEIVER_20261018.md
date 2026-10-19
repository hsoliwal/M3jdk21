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
- Target modified owner: `m3/eclipse/framework/src/main/java/com/m3/eclipse/framework/M3EclipseResourceDeltaSnapshot.java`.
- Owner original Git blob: `ef4769a17ba2447902ac99dd8b289783f4d31227`.
- Owner before SHA-256: `3e499ab33152d8a79c3171ce96a20ae35968c8e573e1ac48b999ede35a7581ec`.
- Owner after SHA-256: `94fc5f6ac7d87439758e8dc6544754e0a77ebbff52d36bbb8ae18a6447aa555c`.
- New test SHA-256: `2805f91b08f95e42f3fb9e0e6390de3174e5bf71fc761bc09210bb9ca9b6d781`.
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
