# M3 Object Residency recipe packet

This packet owns the recipe-first workflow for parent-owned lazy object projections. The runtime arena lives in m3-collections and is independently testable. This packet must not directly rewrite arbitrary JDK/Swing/SWT files.

## Canonical source owners

- Runtime contract: m3/docs/m3-parent-owned-object-residency.md
- Parent-owned flat runtime: m3/collections/src/main/java/com/m3/collections/M3ObjectResidency.java
- Compile-time marker: m3/collections/src/main/java/com/m3/collections/M3Residency.java
- Runtime tests: m3/collections/src/test/java/com/m3/collections/M3ObjectResidencyTest.java
- Existing lazy-tree owner: M3TreeMap / M3LazyValueState in m3/collections
- Existing temporary-array pool: src/java.base/share/classes/jdk/internal/util/M3ObjectArrayPool.java

## Recipe stages

1. Inventory annotated class/member sites and pin source/classfile SHA-256.
2. Emit a deterministic metadata inventory containing owner, parent, flags, estimated bytes, strategy, escape evidence, and source hash.
3. Reject unknown flags, stale preimages, unannotated sites, and any unresolved callback/reflection/JNI/serialization escape.
4. Produce a candidate transformation only for explicitly admitted sites.
5. Run strict Java 21 compile, behavior differential tests, memory-pressure tests, and second-pass fixed-point verification.
6. Promotion requires a separate receipt for each transformed file. No batch rewrite of all widgets.

The first implementation PR deliberately stops at the runtime arena and CLASS-retained marker. The class-backed OpenRewrite inventory/transformer is a separate atom and remains OPEN; the marker is not proof that any call site has been rewritten.

## Safety boundary

Ordinary Java has no reliable general-purpose reference count and cannot transparently reclaim an object while a raw reference remains reachable. The recipe must prove bounded-use/escape conditions or leave the type pinned. Swap requires a codec; recreate requires canonical parent state and a side-effect-safe factory. JNI does not change Java reachability rules.
