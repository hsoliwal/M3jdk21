# M3 collection port wave 1

This directory is an **isolated portable receiver**, not a `java.base` replacement.

The implementation bodies are materialized from exact Apache-2.0 Synexia owners pinned in
`PROVENANCE.tsv`. Synexia remains the canonical implementation/recipe/donor-convergence
workspace. M3JDK21 owns only this product-side materialization, verification and later JDK
integration evidence.

## Included M3 owners

- `com.m3.util.M3PrimitiveKind`
- package-private `com.m3.util.M3DenseHash` implementation atom
- `com.m3.util.M3PrimitiveArrayList`
- `com.m3.util.M3PrimitiveArrayDeque`
- `com.m3.util.M3IntArrayDeque`
- `com.m3.util.M3PrimitiveMinMaxQueue`
- `com.m3.ds.M3LongFenwickTree`

The primitive collection names remain explicit because their source contracts are not
`java.util.List`, `Deque` or `Queue` contracts. This receiver does not claim they are drop-in
JDK collection replacements.

The Fenwick implementation is the packed exact-arithmetic owner selected in Synexia's fast-search
primary-owner work. That does not imply it is universally fastest; target promotion still requires
a concrete consumer and reproducible workload evidence.

## Gates

`verify_collection_port_wave1.py` checks exact provenance rows, source-blob headers, target
packages and absence of Synexia runtime imports. Maven compiles with Java 21, `-Xlint:all`,
`-Werror`, and runs JUnit contract tests.

No public JDK API, HotSpot/JIT/GC/JNI code, `java.util` class, or module boundary is changed by
this port.
