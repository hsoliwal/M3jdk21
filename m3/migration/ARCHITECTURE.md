# MIndex → M3 consolidated architecture and requirements

Status: implementation specification plus current evidence map. This document is subordinate to exact source contracts, the migration manifest, and route-specific tests. It does not turn an unexecuted gate into acceptance.

## 1. Layering

The migration has four layers with one-way bootstrap dependencies:

1. **M3 text kernel** — immutable UTF-16 values, ranges, joins, exact value equality/hash, explicit materialization.
2. **M3 structural kernel** — atom/AST/DAG/interaction owners using the text kernel for textual payloads. This layer stays outside `java.base`.
3. **Compiler/tooling adapters** — OpenRewrite/compiler lowering, inventories, recipes and donor catalogues. These may depend on Maven/tooling but the text and JDK runtime kernels may not.
4. **Modified JDK runtime** — the complete matched `java.base` + HotSpot integration. Runtime code may specialize the common contracts but must not depend on Maven/OpenRewrite or application modules.

JNI, FFM, GPU and other accelerators are optional backends. They do not define logical String identity.

## 2. The three String routes

### Route A — explicit stock-JVM value

`M3Text` is the additive stock-JVM contract. It preserves exact UTF-16 `CharSequence` semantics while concat/slice/repeat retain immutable M3 payload references. `String`, `char[]`, encoded bytes and incompatible external APIs are explicit projection/materialization boundaries.

### Route B — compiler lowering

`M3TextCompilerRuntime` is the target membrane. A rewrite is eligible only when type attribution and effect analysis prove preservation of:
- left-to-right evaluation and side effects;
- null behavior and exception timing;
- overload resolution and constant semantics;
- public/ABI boundaries;
- escaping values, identity-sensitive code, synchronization and reflection.

Unresolved or ambiguous code falls back to ordinary Java. The initial OpenRewrite recipe is intentionally inventory-only.

### Route C — complete modified JDK

The existing runtime candidate uses JDK-private MIndex atom/range storage below ordinary `java.lang.String`. It requires a complete matched JDK image and independent gates for HotSpot, GC, StringTable/intern, JNI/JVMTI, intrinsics/JIT, CDS, deduplication, serialization/reflection and serviceability. Interpreter success is not JIT acceptance.

## 3. Canonical immutable storage

Logical text is an ordered sequence of exact UTF-16 code units over immutable owners and ranges.

- Known immutable dictionary/lexicon rows may be retained by reference.
- Unknown exact text belongs to a VM/resolver-local immutable interner unless a separately reviewed publication path promotes it.
- Joining already admitted text must not allocate a duplicate result-sized character/byte payload; bounded descriptor metadata is allowed.
- Slicing retains owners and adjusts ranges. Tiny slices may retain large owners; compaction is a deliberate copy/retention trade-off.
- Mutable arrays are copied unless exclusive immutable ownership transfer is provable.
- Java arrays remain contiguous fixed-size objects. JNI cannot turn disjoint arrays into one Java array.

Keep these identities distinct:
1. exact UTF-16 text value;
2. owner/namespace/generation atom identity;
3. normalized composition geometry;
4. Java wrapper object identity and `String.intern` identity;
5. process-local native handles.

## 4. Structural M3 ownership

The canonical structural direction is:

```
M3Text
  └─ M3AtomStore (primitive immutable lanes)
       ├─ M3Ast (parser-neutral projection)
       └─ M3Dag (deterministic graph projection)
            └─ M3Interaction (precomputed role × role × mode classification)
```

Large application AST/parser/compiler dependencies must not leak into the storage kernel. Adapters convert donor objects during admission and then release them; frozen structural values retain primitive coordinates and `M3Text` payloads.

## 5. String/Unicode and conversion contract

Java String value semantics are the oracle for String-shaped operations:
- indexes and lengths are UTF-16 code units;
- supplementary pairs may cross storage seams;
- unpaired surrogates remain representable;
- ordinary substring may split a surrogate pair;
- mutable outputs are independent writable storage;
- Java `hashCode` is the exact 31-polynomial with 32-bit overflow;
- cryptographic digests and approximate similarity are separate identities;
- stateful encoders operate over the complete logical character stream, not independently encoded fragments.

Regex optimization may add only sound prefilters unless the selected engine itself implements the complete requested contract. Stock `java.util.regex` remains the semantic authority for ordinary Java Pattern/Matcher compatibility.

## 6. Shared backing and generations

OS sharing means independent processes map the same immutable published pages. Persist offsets, namespaces, generations and format versions—not process pointers.

A generation protocol must define:
- create-new/atomic publication;
- dimension, checksum and corruption validation;
- overflow/bounds checks;
- reader lifetime and retirement;
- immutable-file expectations and platform replacement behavior;
- stale-handle protection;
- mapped/native/heap accounting;
- lazy bootstrap-safe loading and local fallback.

A read-only mapping does not stop another process from truncating or modifying the file.

## 7. Derived facts and precomputation

Facts are owned by the narrowest immutable authority that makes them valid:
- atom facts — length, Java hash, selected Unicode boundary facts;
- composition facts — combined hash/length/segment directory;
- pattern facts — prefix/failure tables, safe masks, compiled programs;
- input-context facts — regions, flags, charset/error policies.

Cache eviction changes performance only, never answers. Java hashes compose; finalized SHA-256 digests do not compose using the Java-hash formula.

## 8. Permanent migration discipline

`manifest.json` is the source of truth for source→target lineage. Future work uses three-way comparison:
1. last synchronized source → current source;
2. last synchronized target → current target;
3. the mapping contract and target-only adaptations.

Every semantic mutation needs a reusable recipe or another source-pinned deterministic mechanism for files OpenRewrite cannot safely transform. Recipes refuse drift, ambiguous ownership and partial incompatible states; replay is idempotent; rollback is explicit.

No mapping is complete merely because a similarly named target file exists.
