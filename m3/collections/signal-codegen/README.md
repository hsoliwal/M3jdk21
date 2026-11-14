<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 source-time `@Sortable` signals (opt-in)

`@Sortable(signal = "absoluteWeight")` on a top-level Java class or record generates a separate
`<Type>M3Sortable` companion during JDK 21 annotation processing. It does **not** rewrite the
annotated class, bytecode, or `java.base`, and does not require Lombok or runtime reflection.

The compiler precomputes six independently hashed, unsigned-sorted, duplicate-free primitive
`long[]` metadata lanes: package name, fully qualified class name, declared field names/types,
method signatures, referenced types (supertype, interfaces, fields, method parameters/returns/
throws), and *resolved* source method calls through `jdk.compiler` Trees when available.
Calls are intentionally empty when the compiler does not expose Trees. Tokens are FNV-1a 64-bit
hints and may collide; metadata similarity is **not** a proof of API equivalence, call-graph
completeness, or Java `equals`.

The optional `signal` names exactly one accessible instance numeric field or zero-argument
method. The generated `absoluteSignal(T)` and `absoluteSignals(T[])` evaluate the domain value
once per object and can feed the existing `M3SignalOrder.ascending(...)`,
`M3SignalOrder.filtered(...)`, or `M3ObjectIndexSnapshot.sortedBySignal(...)`, including their
already-qualified opt-in JNI paths. Numeric conversions (notably large `long` to `double`) can
lose ordering resolution; exact domain order still requires a valid comparator or richer keys.

The independent `StructuralSignals` helper returns Jaccard distances over source-generated hashed
token sets, with a caller-selected nonnegative weight vector for the six dimensions. It retains
no per-object metadata, global pair cache, or Java object pointer. Comparison work is linear in
the number of distinct tokens examined. Hash collisions make the distance approximate relative
to the original source identities.

Example:

```java
@com.m3.collections.sortable.Sortable(signal = "absoluteWeight")
public final class Person {
    public double absoluteWeight() { return 0.5; }
}

// Generated at compile time, with the annotation processor enabled:
double[] keys = PersonM3Sortable.absoluteSignals(people);
int[] order = com.m3.collections.M3SignalOrder.ascending(keys).rows();
```

Compile this processor first, then explicitly enable it on caller sources via
`-processorpath <built-processor-jar> -processor com.m3.collections.sortable.SortableProcessor`.
The processor service file also supports ordinary processor discovery when explicitly admitted.
Running `verify.sh` exercises actual Java 21 compiler rounds, deterministic permutation,
invalid signal declarations, numeric extraction, call/dependency metadata, defensive-copy
exports, and distance bounds without requiring a Maven repository mirror.

`mvn -f m3/collections/signal-codegen/pom.xml test` is the Maven lane; its compiler plugin
uses `-proc:none` while building the processor itself to avoid self-discovery. No automatic
`m3/collections` reactor inclusion or JDK migration-phase advancement is implied.

This is a product source-time extension. The canonical recipe, byte-sealed templates, donor
catalogue, and fixed-point/refusal test belong to `hsoliwal/com.synexia`. Respect original
Synexia Apache-2.0 notices; no challenge website submission is copied.
