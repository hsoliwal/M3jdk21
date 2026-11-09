<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse framework indexed forwarding into M3JDK21

This is an **additive, read-only indexed forwarding layer**, not a full rewrite of the Eclipse
framework. Original Eclipse extension/OSGi/resources services retain authority, identity,
extension execution, notifications, classloaders, bundle lifecycle, and workspace semantics.

The existing `com.m3.collections` owners provide the implementation mechanics:
`M3ObjectStateSlots28` for compact state masks, `M3SlotAllocator28` for generation-safe
physical positions, `M3LongArrayList` for parent/handle offset planes,
`M3LongLongHashMap` for primitive ID-to-handle lookup, and existing dense array
factories for references. No per-element map Entry or alternative scheduler is introduced.

## Implemented adapters
- `M3EclipseBundleSnapshot`: inspect installed OSGi bundles; index identities,
  capture one observed state, select by masks. **Never** starts/stops/resolves bundles.
- `M3EclipseExtensionSnapshot`: capture Eclipse extension points with validity flags;
  `livePoint(id)` delegates unchanged to the owning `IExtensionRegistry`; `refresh()`
  re-enumerates. Eclipse forbids clients to reimplement IExtensionRegistry.
- `M3EclipseResourceDeltaSnapshot`: bounded, breadth-first view of the original
  resource delta tree with parent ordinals; Eclipse kind masks and no recursive
  traversal. Default `getAffectedChildren` member visibility semantics remain intact.
- `M3EclipseIndexedSnapshot<T>`: shared internal primitive handle/reference bridge.
  Nothing here owns a second object graph or Eclipse runtime service.

## Build & verification (Java21)
```sh
mvn -B -f m3/eclipse/framework/recipe/pom.xml test
mvn -B -f m3/eclipse/framework/pom.xml verify
```
Install `com.m3:m3-collections:0.1.0-SNAPSHOT` first, or run the exact-head workflow,
which compiles the existing unchanged core and installs a thin Maven artifact.

Source files are byte-identical to eight SHA-256-sealed ABSENT-before templates in
canonical Synexia recipe `com.synexia.rewrite.M3EclipseFrameworkIndexForward`,
`hsoliwal/com.synexia#12154`. Two JUnit test classes cover 6 families of
adversarial cases, including bundle ID zero, Long.MAX_VALUE, duplicate bundle IDs,
extension invalidation, bounded delta walks, cycles and a 2049-node deep delta.
The isolated Java21 stub-API run passed all six tests. **Real Eclipse/M3 Maven
build and OSGi runtime proof are separate, mandatory gates**; no unexecuted
GitHub Actions job establishes qualification.

## Provenance and upstream contracts
**OSGi integration:** `M3EclipseRuntimeActivator` registers an `M3EclipseRuntimeServices` implementation through BundleContext and unregisters it once at stop. The host supplies Eclipse services and controls bundle lifetimes. The generated OSGi manifest imports `com.m3.collections`, so that package must be exported by a separately installed M3 collections bundle; the exact-head workflow builds a manifest-wrapped OSGi candidate for that dependency. Source/JUnit tests do not establish actual Equinox resolution or plugin activation.\n\nOriginal Synexia core Eclipse mechanics and pluginify adapter are donor evidence.
Synexia first-party recipe/source is Apache-2.0; Eclipse supplied APIs remain
EPL-2.0 and OSGi interfaces retain their original Apache-2.0 licensing.
No Eclipse source has been copied into the generated adapter. JDK API surface
and existing M3 runtime owners remain unchanged.

**Not implemented:** replacement of SWT/Nebula intrinsic widget behavior,
JFace/Workbench event loops, real Equinox resolver/activation qualification, JDT
refactoring internals, EMF notifications/proxy semantics or PDE launch.
See `ECLIPSE_FORWARDING_COVERAGE.tsv`, and keep unqualified subsystems
explicitly marked instead of claiming total Eclipse coverage.
