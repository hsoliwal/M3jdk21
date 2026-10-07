# Synexia M3 family naming receiver

Status: target-only receiving policy. This file does not fork the reusable Synexia recipe and does
not promote any collection, AST, precompute, graph or native family into the JDK runtime.

## Exact source authority

M3JDK21 pins Synexia PR #9773 head
`0928e61bff989c7df16761301491bac4e7ba41bc` through
`synexia-m3-family-naming-pin.tsv`. The pin records the exact policy, machine policy, seed
semantic map, canonical invariant table, AGENTS continuation rule, reusable OpenRewrite recipe and
recipe-test Git blobs.

The reusable recipe remains
`com.synexia.rewrite.M3FamilyNamingCollections` in `hsoliwal/com.synexia`. M3JDK21 does not
copy or re-author that implementation. Repeatable defects discovered during target receiving are
fixed in the Synexia owner first and replayed against a pinned target preimage.

## License boundary

Eligible Synexia-authored source expression, recipes, tests, documentation, schemas and manifests
are available under Apache-2.0 with their recorded copyright/provenance. This does not mean that
abstract algorithms or ideas are exclusively owned by Synexia, and it does not relicense OpenJDK
or third-party material.

OpenJDK-derived files in this repository keep their existing GPLv2+Classpath Exception or other
applicable upstream headers and notices. Third-party donor material keeps its own copyright,
license, NOTICE and modification obligations. A Synexia recipe never changes the license of its
inputs merely by transforming them.

## Target naming membrane

The receiver accepts the following direction only after per-type proof:

- canonical first-party successor prefix: `M3*`;
- no blind `MIndex*` / `MatIndex*` / historical `Index*` prefix rewrite;
- JDK-like collection names only when the corresponding JDK-like observable contract is proved;
- meaningful primitive/algorithm specialization remains in concise names where there is no honest
  JDK analogue;
- one semantic owner per collection/data-structure contract, with alternative implementations as
  internal strategies;
- no universal "fastest collection" claim: selection is per exact contract and workload evidence.

The source package membrane is intentionally small. M3JDK adapts it to existing target owners or
the smallest target-owned internal package. A source Synexia package is never a public JDK ABI.

## Ordered receiving

Continue the existing target order:

    STRING -> ARRAYS -> COLLECTIONS -> AST_COMPILER -> PRECOMPUTE -> DAG/GRAPH -> REMAINING

The companion `m3-family-naming-target-disposition.tsv` is a receiver map only. It records
preferred target naming/package direction and the proof gate that still blocks admission. No
`RECEIVER_MAP_ONLY` row is runtime acceptance.

For collections, compare candidate strategies only after exact semantic compatibility. Then measure
cold/warm CPU, allocations, heap, native memory, cache/GC effects, setup cost and expected
cardinality. JNI/native strategies additionally need Java parity, lifetime, `-Xcheck:jni`,
invalid-input behavior, crossover and Java fallback evidence.

## Donor convergence

Inventory existing Synexia owners first. Review OpenJDK/JDK, Eclipse Collections, Guava, Apache
projects and other licensed specialized collection/algorithm implementations at exact
revision/path/license boundaries. LeetCode, HackerRank and GeeksForGeeks remain category and
adversarial-fixture evidence unless a separately licensed implementation source is identified.

The target consumes only an explicit `COPY`, `ADAPT`, `CLEAN_ROOM`, `EVIDENCE_ONLY` or
`REJECT` disposition with provenance. Similarity, a benchmark result or a familiar class name is
not permission to copy or promote code.

## Current scope

This receiver changes no public JDK API, no `java.util` implementation, no `java.lang.String`
contract and no HotSpot/JIT/GC/JNI runtime behavior. It records the exact Synexia source authority
and target-specific receiving gates so later materialization can proceed one concrete owner at a
time without losing the M3 naming, recipe and licensing invariants.
