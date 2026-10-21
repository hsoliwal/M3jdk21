<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse core runtime forwarding into M3

This is **one independently built, additive Eclipse bridge**, not an Eclipse platform fork
or a claim that SWT/OSGi/JDT/EMF are already M3-backed. Eclipse's public
`org.eclipse.core.runtime.IProgressMonitor` remains the contract; M3's existing
`com.m3.collections.M3ProgressMonitor` is the sole state owner. No new scheduler, item
payload, event loop, or JNI implementation is introduced.

- `M3EclipseProgressMonitor.owned(m3)`: forwards begin, integral work, names,
  cancellation and one `done()` to the M3 owner; refuses a second begin.
- `M3EclipseProgressMonitor.borrowed(m3)`: forwards progress/cancellation and
  subtask names **without** taking ownership of `beginTask` or `done`. This matches
  the current Synexia nested Eclipse/JDT mechanics adapter's lifecycle boundary.
- `internalWorked(double)` accumulates fractional units, ignores invalid values,
  and forwards whole units. It does not claim full fidelity for >53-bit floating
  precision or arbitrary owner-specific overflow semantics.
- In both modes, `owner()` exposes the exact original M3 owner by reference.

## Build and proof

The canonical declarative OpenRewrite recipe and admitted templates belong in
`hsoliwal/com.synexia/synexia-openrewrite-recipes`. This directory mirrors the exact
recipe inputs and uses the M3JDK21 **existing** `M3HashPinnedJavaSnapshotRecipe`, not a
second rewrite engine. The two proposed Java postimages are byte-identical to absent-before,
SHA-256-sealed templates. Actual OpenRewrite replay and target Maven validation
remain required before promotion.

With a prepared M3 collections artifact, Java 21 and Maven:

```sh
mvn -B -f m3/eclipse/core-runtime/recipe/pom.xml test
mvn -B -f m3/eclipse/core-runtime/pom.xml verify
```

The CI workflow compiles the unchanged M3 core directly under Java21 -Xlint/-Werror,
packages it without running its unrelated historical test fixture, and installs
that jar with the standard maven-install-plugin before executing these commands.

An isolated Java-21 stub check is **not** an Eclipse platform/runtime
integration gate. Promotion needs exact-head Maven/JUnit, recipe fixed-point/refusal,
OSGi/PDE resolution where applicable, cancellation/reentrancy differential tests,
and licensing review. Native SWT/GC/disposal and plugin activation remain unqualified.

## Source provenance and scope

Synexia donor: `cognix-eclipse-mechanics-adapters/src/main/java/com/synexia/cognix/eclipse/mechanics/adapters/SynexiaEclipseProgressMonitor.java`, Apache-2.0.
M3 owner: `m3/collections/src/main/java/com/m3/collections/M3ProgressMonitor.java`, Apache-2.0.
Eclipse public interface: `org.eclipse.core.runtime.IProgressMonitor` (upstream Eclipse license applies).
This adapter is new first-party M3 Apache-2.0 expression; no third-party Eclipse
implementation source is copied or relicensed. The Eclipse compile dependency is
`provided` because the host supplies the actual bundle/interface.

See `ECLIPSE_SUBSYSTEM_M3_RECEIVING.tsv` for the wider framework's per-family status.
