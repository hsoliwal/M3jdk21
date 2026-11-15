<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse framework indexed forwarding into M3JDK21

This is an **additive, read-only indexed forwarding layer**, not a full rewrite of the Eclipse
framework. Original Eclipse extension/OSGi/resources services retain authority, identity,
extension execution, notifications, classloaders, bundle lifecycle, and workspace semantics.

The existing `jdk.m3.collections` owners provide the implementation mechanics:
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

## Build from a fork (JDK 21)
From the repository root, use the existing Maven reactor:
```sh
mvn -B -ntp -f m3/pom.xml -pl :m3-eclipse-framework-forward -am -Peclipse-osgi -DforkCount=1 -DreuseForks=true clean verify
```
Maven builds arrays, collections and the Eclipse forwarder in dependency order.
The two provider builds attach an `osgi` classifier; their ordinary modular
JARs retain their original coordinates and contents. The forwarder is already
an OSGi bundle. Install the two classified bundles and the forwarder in an Eclipse
runtime that supplies Core Resources, Core Runtime, Equinox Registry and OSGi.
One reusable Surefire JVM per project bounds parallelism. There is no local
installation or nested Maven invocation. An offline build needs a prepared cache.

For Spring Dynamic Modules or Eclipse Gemini Blueprint, provision a compatible
extender and its dependencies in the framework. A Spring application bundle
imports the exported package or service API through OSGi. Keep its manifest,
`META-INF/spring/` or `OSGI-INF/blueprint/` descriptors and bundle class loader.
Neither a classifier nor an XML descriptor starts a Spring application context.
JMODs are link-time inputs for explicit JPMS modules. Keep OSGi bundle JARs as
deployment units; do not rename a bundle JAR to .jmod or flatten an extender into
the application. The optional Eclipse image has its own existing JDK build and
link verification in `m3/eclipse/optional-jmod/`.

## Provenance and upstream contracts
**OSGi integration:** `M3EclipseRuntimeActivator` registers an `M3EclipseRuntimeServices` implementation through BundleContext and unregisters it once at stop. The host supplies Eclipse services and controls bundle lifetimes. The generated OSGi manifest imports `jdk.m3.collections`, so that package must be exported by a separately installed M3 collections bundle; the Maven profile attaches the OSGi provider artifact. Source/JUnit tests do not establish actual Equinox resolution or plugin activation.

Original Synexia core Eclipse mechanics and pluginify adapter are donor evidence.
Synexia first-party recipe/source is Apache-2.0; Eclipse supplied APIs remain
EPL-2.0 and OSGi interfaces retain their original Apache-2.0 licensing.
No Eclipse source has been copied into the generated adapter. JDK API surface
and existing M3 runtime owners remain unchanged.

**Not implemented:** replacement of SWT/Nebula intrinsic widget behavior,
JFace/Workbench event loops, real Equinox resolver/activation qualification, JDT
refactoring internals, EMF notifications/proxy semantics or PDE launch.
See `ECLIPSE_FORWARDING_COVERAGE.tsv`, and keep unqualified subsystems
explicitly marked instead of claiming total Eclipse coverage.
