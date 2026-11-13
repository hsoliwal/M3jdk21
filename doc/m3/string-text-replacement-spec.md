# String and text replacement specification

Status: documentation-only design and acceptance handoff. This document does not modify `java.lang.String`, regex, charset code, HotSpot, JNI, an installed JDK, or any acceptance receipt.

Read with:

- `whole-jdk-m3-architecture.md`
- `jdk-subsystem-migration-matrix.md`
- the operational mapping authority `name-mapping.json`
- branch-specific String/runtime evidence only at its exact pinned commit.

## 1. Scope

The text programme covers the complete dependency closure of ordinary Java text behavior, not only the `String` class:

- `java.lang.String`;
- `AbstractStringBuilder`, `StringBuilder`, `StringBuffer`, `StringJoiner`;
- String coding/compact-string internals;
- `Character` and code-point interaction;
- charset encoders/decoders and String byte constructors/methods;
- regex `Pattern` / `Matcher`;
- formatting, splitting, replacement and case conversion;
- compiler constants and concatenation lowering;
- VM StringTable/symbol/string-dedup interactions where applicable;
- JNI/JVMTI/JVMCI/native String boundaries;
- GC/JIT/intrinsics/CDS/serviceability consumers;
- serialization/reflection/constant-desc behavior.

Route A may expose an explicit indexed text value. Route B may lower proven operations. Route C may change the internal representation behind ordinary `java.lang.String`. These are separate acceptance domains.

## 2. Existing source-side MIndexString owner

The inspected Synexia `MIndexString` on `develop` is already a rich immutable indexed value rather than a thin String wrapper.

Observed design points that should be preserved as inputs to the JDK programme:

- the logical Java value is UTF-16 text;
- physical representation is an `owner` plus a packed `long value`;
- a scalar resolver owns one ID;
- an immutable tuple owns a packed offset/count span;
- `fromString`, `of`, value factories, compiler-runtime indexing and `intern()` form an explicit JVM-scoped canonical-wrapper boundary;
- constructors are compatibility boundaries and cannot return an existing object;
- ordinary Java `==`, monitor identity and `identityHashCode` remain unsafe as content-identity operations;
- exact JDK operations may deliberately materialize/delegate at an ABI or semantic boundary.

That source design is evidence that an indexed representation can be richer than “replace chars with an int”. It is not evidence that `java.lang.String` has already been safely replaced.

## 3. Identity model

The text replacement must keep at least four identities distinct.

### 3.1 Text equality

Two values contain the same ordered UTF-16 code units.

This is the semantic basis for ordinary `String.equals`.

### 3.2 Atom identity

Two references point at the same immutable admitted text atom inside one M3 owner/generation.

Atom identity may make exact-equality confirmation cheap after admission, but it is not Java object identity.

### 3.3 Composition identity

Two joined values may have the same text but different range/tuple composition.

Example:

- atom "ab" + atom "cd";
- atom "abcd";
- slice 0..4 of a larger atom.

These can be text-equal while retaining different descriptors.

### 3.4 Java object identity

Two `String` references compare with `==` according to JVM object identity.

Route C may share immutable backing but must not silently make all equal Strings one Java object. Interning is a separately specified operation.

## 4. Logical contract: exact UTF-16

Java String semantics are defined over UTF-16 code units, including unpaired surrogates.

The M3 owner must preserve:

- every 16-bit code unit;
- length in UTF-16 code units;
- `charAt`;
- arbitrary substring endpoints, including splitting a surrogate pair;
- code-point methods as computations over the exact code units;
- lexicographic comparison over the Java-defined representation;
- String-compatible hash;
- case/locale operations through exact JDK semantics unless an optimized path is proven equivalent.

Do not normalize Unicode during admission.

Do not treat “valid Unicode scalar sequence” as a precondition for String storage.

## 5. Physical encodings

The logical UTF-16 contract does not require one physical encoding.

Candidate leaves may use:

- LATIN1-compatible bytes for values whose code units are representable;
- UTF-16 code-unit storage;
- shared immutable dictionary/image ranges;
- VM-local immutable arenas;
- native/mapped immutable backing after exact lifetime proof.

Every leaf carries enough encoding metadata for exact random access and traversal.

Encoding is an implementation property, not part of `String.equals`.

A joined descriptor may mix physical leaf encodings if every operation remains seam-correct.

## 6. Canonical data ownership

Separate three ownership domains.

### 6.1 Shared immutable image / lexicon

Purpose:

- share immutable spelling payload between processes where the OS can share physical pages;
- precompute stable facts for immutable entries.

Requirements:

- versioned format;
- immutable publication protocol;
- bounds and checksums;
- namespace/generation identity;
- read-only consumer mapping;
- corruption/truncation rejection;
- no reliance on a mutable file staying unchanged merely because a mapping is read-only.

A shared image contains data, not cross-process Java object identity.

### 6.2 VM-local canonical atom/interner

Purpose:

- map a logical text admission to one live immutable atom/body within a JVM namespace;
- retain Java-runtime-local lifetime and GC policy.

The lookup may be weak, strong, bounded or tiered according to the selected owner. Evicting an optional lookup entry must not invalidate an atom still strongly owned by a live String/view.

### 6.3 Java String facade/object

Purpose:

- preserve ordinary Java object semantics;
- project an M3 text owner through the exact String API.

Multiple String objects may refer to the same immutable body.

`intern()` applies the String intern contract, not the generic M3 atom-intern contract by assumption.

## 7. Handle shape

A runtime handle must not be a naked slot integer.

At minimum the design records:

- owner namespace;
- generation/version;
- record/row identity or range descriptor;
- encoding;
- offset;
- length.

Compact encodings are welcome, but validation must reject stale/cross-owner handles.

If generation bits can wrap, define refusal/reclamation rather than silently reusing stale coordinates.

## 8. Slices

A slice is an immutable range over a retained owner.

Required behavior:

- exact start/end bounds;
- O(1) descriptor creation is permitted when owner lifetime is retained;
- no code-point boundary restriction;
- no normalization;
- nested slices collapse arithmetic where safe;
- empty slices follow String semantics;
- exact exception class and timing at the public surface.

Retention tradeoff:

A tiny slice can keep a huge immutable owner alive. Therefore support an explicit, measured compaction/materialization policy rather than claiming every slice is a memory win.

Compaction must be semantically invisible except for identity properties that the public contract does not expose.

## 9. Joins and concatenation

A join is an ordered sequence of immutable ranges.

Candidate representation:

- leaf/range references;
- cumulative logical ends or a bounded segment index;
- total UTF-16 length;
- optional precomputed hash/length facts.

Required invariants:

- check integer/array/descriptor overflow before publication;
- preserve operand evaluation order and exceptions in compiler lowering;
- remove empty segments where safe;
- coalesce adjacent ranges only when they share compatible owner/encoding and doing so cannot change lifetime/identity semantics;
- flatten nested descriptors only within explicit segment-count/memory bounds;
- no character payload copy merely to create the descriptor.

A descriptor join can still allocate metadata. “Zero allocation concatenation” is not a blanket claim.

## 10. Bounded descriptors

Reference-only joins can become pathological if segment count grows without bound.

Every implementation must define:

- maximum descriptor depth/segments or a rebalance rule;
- metadata byte budget;
- random-access lookup complexity;
- compaction/materialization threshold;
- cancellation/overflow behavior for enormous compositions;
- retained-owner accounting.

Possible strategies:

- flat range table for small joins;
- balanced immutable tree for large joins;
- tiered descriptor with cached segment index;
- explicit compaction when metadata or retention crosses a measured threshold.

The selected rule must be benchmarked against tiny and huge joins.

## 11. Precomputed facts

Candidate facts for immutable text:

- UTF-16 length;
- String hash;
- code-point count;
- ASCII/LATIN1 classification;
- first/last code unit;
- prefix/suffix fingerprints;
- literal-search tables;
- case/shape signals;
- encoding size where safe;
- regex candidate-rejection descriptors.

Rules:

- hashes never replace exact equality on collision;
- a fact is bound to immutable owner + generation + range/tuple parameters;
- facts for a slice may be derived lazily rather than retained globally;
- cache memory is budgeted;
- no unbounded “precompute everything” global store;
- invalid or unavailable fact falls back to exact traversal.

Precompute may make repeated operations faster, but first admission still has a cost.

## 12. hashCode and equals

For an immutable whole atom, String-compatible hash can be precomputed once.

For a composition:

- combine segment facts only with an algebra proven exactly equivalent to Java's ordered hash recurrence; or
- stream code units once and cache the result.

Equal text from different compositions must produce identical String hash.

Hash equality is only a rejection/lookup signal; exact content confirmation remains mandatory unless canonical identity is already proven inside one exact owner namespace.

## 13. compareTo and ordering

Ordering is Java String lexicographic ordering over UTF-16 code units.

Optimizations may use:

- precomputed first-difference candidates;
- atom rank only when the rank is defined by exact Java ordering for the same domain;
- shared prefix lengths;
- vectorized segment comparison.

Fallback must compare exact code units.

A dictionary frequency rank, insertion ID or language rank must never substitute for String ordering.

## 14. indexOf / lastIndexOf / contains

Search must work across segment seams.

Possible strategies:

- streaming KMP/two-way/Horspool-style search over a code-unit cursor;
- atom-aware prefix skipping;
- precomputed literal tables;
- candidate filters.

Required cases:

- empty needle;
- negative/oversized fromIndex;
- supplementary characters;
- unpaired surrogates;
- match crossing leaf seam;
- overlapping candidates;
- long pattern;
- same content with different composition.

A fast filter may reject only when rejection is sound.

## 15. regex

Regex is not equivalent to literal search.

Route A safest baseline:

- expose exact `CharSequence` behavior;
- use stock `Pattern`/`Matcher` as semantic authority where possible;
- optimize literal/prefix candidate selection only when it cannot remove a valid match.

Route C may add M3-aware traversal to regex internals, but must preserve:

- flags;
- captures;
- named groups;
- backreferences;
- lookaround;
- regions;
- anchoring/transparent bounds;
- replacements;
- zero-length matches;
- Unicode classes/properties;
- syntax exceptions.

Patterns may themselves be segmented/indexed, but materialization is acceptable at an explicit unsupported-engine boundary.

Never claim regex equivalence from a literal matcher corpus.

## 16. case conversion and locale

ASCII-only range transforms are candidates for reference composition or transformed leaf views.

General case conversion must preserve:

- Locale-sensitive behavior;
- one-to-many mappings;
- Unicode version behavior;
- Turkish/Lithuanian/special casing;
- context-sensitive rules.

Fallback to the stock JDK implementation is correct when an optimized subset cannot prove equivalence.

A transformed view must not change the semantics of subsequent `String` operations.

## 17. trim / strip / lines / indent / escapes

These APIs have distinct definitions:

- `trim` is not `strip`;
- line terminators have API-specific semantics;
- indentation and escape translation have exact exceptional behavior.

Candidate optimization:

- compute resulting ranges;
- reuse unchanged immutable spans;
- materialize only transformed insertions/removals where necessary.

Each method needs its own differential oracle. Do not infer correctness from substring.

## 18. encoding and byte conversion

Encoding a joined value must be seam-aware.

For stateless single-code-unit encodings an implementation may encode segments incrementally.

For stateful encoders or error/replacement behavior, the semantic encoder state must flow across segment boundaries exactly as if one logical String were presented.

Test:

- malformed surrogate input;
- replacement policy;
- unmappable input;
- stateful charset boundaries;
- buffer overflow/underflow loops;
- segment seam in a surrogate pair;
- exact output byte order.

Constructors from bytes likewise preserve Charset decoder semantics.

## 19. mutable builders and StringBuffer

Do not globally intern mutable builder contents.

Possible M3 design:

- mutable segmented builder owner;
- append references immutable String atoms/ranges when safe;
- mutable tail buffer for new characters;
- snapshot/freeze creates immutable String descriptor;
- compaction policy prevents segment explosion.

`StringBuffer` adds synchronization requirements.

Preserve:

- capacity API;
- mutation;
- reverse, including surrogate handling;
- insert/delete/replace;
- getChars;
- append overload evaluation;
- toString snapshot semantics;
- StringBuffer monitor behavior.

A builder representation is a separate owner from immutable String.

## 20. StringJoiner

StringJoiner has prefix, delimiter, suffix, empty-value and length semantics.

Candidate representation:

- retain immutable components as ranges;
- append delimiter metadata without eagerly joining payload;
- final `toString` may create a bounded descriptor.

Must preserve:

- empty behavior;
- merge;
- length;
- huge-size overflow/OOME behavior according to accepted JDK tests.

Historical branch failures do not become accepted expectations by documentation.

## 21. intern()

Route C must define the relationship among:

- M3 atom canonicalization;
- Java String object;
- VM StringTable.

Correctness requirement:

`s.intern()` returns the canonical String object required by the Java platform.

It is insufficient that two Strings point to one atom if `intern()` returns noncanonical facade objects.

Options include:

- StringTable key = exact M3 body identity/content plus canonical Java object;
- existing StringTable retained with M3-aware hash/equality;
- canonical object created lazily over an immutable atom.

The design must be GC-safe and compatible with CDS/string-dedup policy.

## 22. String constants and class files

Compile-time constants and ldc Strings interact with:

- classfile constant pool;
- class loading;
- resolution;
- CDS;
- intern semantics.

Route B cannot lower an ordinary String constant to an explicit M3 type when the static type/identity contract is String.

Route C may alter backing after resolution only if every consumer still sees a normal String object with correct intern behavior.

## 23. invokedynamic concatenation

Modern Java concatenation commonly uses `StringConcatFactory`.

Route B candidates:

- only explicitly M3-typed results;
- statically proven nonescaping internal values;
- exact operand/side-effect preservation.

Route C candidates:

- M3-aware concat strategy returning ordinary String facade over a bounded join.

Must preserve:

- left-to-right operand evaluation;
- null conversion;
- primitive formatting;
- exception behavior;
- bootstrap/linkage;
- constants;
- result String identity semantics.

Fallback remains ordinary String concatenation.

## 24. JNI String boundary

Inventory every JNI operation that observes or constructs Strings, including:

- `NewString`;
- `GetStringLength`;
- `GetStringChars` / `ReleaseStringChars`;
- `GetStringCritical` / release;
- `GetStringRegion`;
- `NewStringUTF`;
- `GetStringUTFLength`;
- `GetStringUTFChars` / release;
- `GetStringUTFRegion`.

Requirements:

- exact UTF-16;
- exact modified UTF-8 contract;
- pin/copy semantics allowed by JNI;
- critical-region restrictions;
- lifetime through release;
- exceptions for invalid arguments;
- native callers that assume contiguous memory.

An M3 segmented backing may require a temporary contiguous buffer at JNI boundaries. That is an explicit materialization, not a correctness defect.

Any no-copy native projection needs a stable contiguous backing and GC/lifetime proof.

## 25. Foreign Function & Memory / native libraries

FFM/native libraries may receive materialized Strings, byte arrays or MemorySegments depending on API.

Do not let M3 internal handles escape as ABI unless a versioned public/native contract explicitly defines them.

Mapped/shared internal text does not imply an FFM segment is safe to expose.

## 26. GC and lifetime

The collector must be able to trace every live owner reachable from a String facade.

If a String references:

- heap atom;
- segment descriptor;
- mapped image generation;
- native allocation;

the lifetime edge must be explicit.

Requirements:

- no stale native pointer after GC/close;
- no hidden owner kept alive only by an invalid raw ID;
- weak interner eviction does not invalidate live String;
- reference processing is deterministic with respect to public semantics;
- heap accounting distinguishes mapped/native retention.

## 27. JIT and intrinsics

Inventory all String intrinsics and compiler assumptions.

Candidate M3-aware intrinsics:

- length;
- charAt;
- equals;
- compare;
- indexOf;
- hash;
- coding conversions.

Each intrinsic requires:

- interpreter oracle;
- deoptimization path;
- uncommon trap correctness;
- null/bounds behavior;
- mixed baseline/M3 object behavior during migration;
- tiered compilation tests.

Do not enable an intrinsic merely because a Java implementation exists.

## 28. object layout

Route C must decide whether:

1. `String` retains its current fields and stores an encoded M3 handle through an existing-compatible shape;
2. `String` layout changes;
3. a transitional tagged representation supports both baseline compact String and M3 backing.

Layout changes affect:

- GC oop maps;
- JIT field offsets;
- reflection/Unsafe assumptions;
- CDS archive;
- serviceability agent;
- heap dumps;
- JVMCI.

Choose layout only after the full consumer inventory.

## 29. serialization and reflection

String has special VM/platform behavior even without ordinary custom serialization logic.

Verify:

- Java serialization round trips;
- ObjectStream String records;
- reflection;
- MethodHandle constants;
- constant descriptions;
- JNI;
- class-data sharing.

For builders, regex and charset-adjacent serializable objects, preserve their own forms separately.

## 30. materialization boundaries

Materialization is acceptable and should be named.

Expected boundaries may include:

- `toCharArray`;
- `getChars`;
- byte encoding;
- JNI contiguous access;
- unsupported regex/case/format operation;
- external serialization;
- legacy native library;
- explicit compaction.

For each boundary record:

- output owner;
- allocation size;
- mutability;
- whether output is independent;
- caching policy.

Never return an internal mutable/shared array where the API promises an independent array.

## 31. shared lexicon + VM-local join worked example

Illustration only.

Assume immutable shared image records:

- 41 -> "hello"
- 77 -> " "
- 92 -> "world"

VM-local atom descriptors point at validated image generation G7.

An expression producing "hello world" can create:

```
Join[
  Range(G7, 41, 0, 5),
  Range(G7, 77, 0, 1),
  Range(G7, 92, 0, 5)
]
```

No character payload must be copied to form that logical value.

`substring(6, 11)` may become `Range(G7, 92, 0, 5)`.

But:

- the Java String facade remains a JVM object;
- JNI may materialize a contiguous buffer;
- `toCharArray` must allocate an independent array;
- if G7 cannot be proven immutable/lifetime-safe, the VM must use a private atom copy;
- if segment metadata becomes excessive, compaction may materialize one private immutable atom.

This is “reference-only join/slice where admitted”, not a universal zero-allocation promise.

## 32. Route A acceptance

Route A can be accepted independently for an explicit M3 text type when:

- API surface is documented;
- exact UTF-16 differential tests pass;
- explicit materialization is correct;
- owner/interner lifetime is proven;
- regex/encoding boundaries are tested;
- no claim is made that ordinary String changed.

## 33. Route B acceptance

Per lowering recipe:

- exact AST/types resolved;
- before/after source or bytecode pinned;
- refusal cases tested;
- side effects/evaluation order tested;
- exceptions tested;
- identity/escape boundaries tested;
- baseline fallback tested;
- generated source maps/diagnostics acceptable;
- recipe idempotence/drift/rollback tested.

No “compiler lowering complete” status from one concat pattern.

## 34. Route C acceptance

A String backend can be promoted only after an exact source/image candidate passes applicable gates:

- full public String differential surface;
- builders/StringBuffer/StringJoiner;
- regex;
- charset/encoding;
- JNI/JVMTI;
- interpreter;
- enabled JIT/intrinsics;
- GC collectors in scope;
- CDS;
- serialization;
- reflection/MethodHandles;
- serviceability/SA/JFR/JVMCI as applicable;
- jtreg/JCK scope;
- supported platforms;
- retained heap/native/mapped accounting;
- cold/warm performance.

Any failed mandatory gate keeps the candidate blocked even when allocation improves.

## 35. Performance plan

Measure separately:

- first admission;
- canonical hit;
- hash;
- equals equal/unequal;
- compare;
- charAt;
- substring tiny/large;
- concat tiny/deep;
- search;
- regex;
- encoding;
- builder append/toString;
- intern;
- JNI region/access.

Datasets:

- ASCII/LATIN1;
- BMP;
- supplementary;
- unpaired surrogates;
- repeated dictionary text;
- unique text;
- many tiny segments;
- huge retained owner + tiny slice;
- cross-process shared image.

Report:

- allocations;
- retained heap;
- mapped/native bytes;
- metadata;
- latency distribution;
- throughput;
- GC behavior.

## 36. Nonclaims

Until exact evidence exists, do not claim:

- every String operation O(1);
- every equal String becomes `==`;
- zero allocation;
- zero copy at all boundaries;
- automatic cross-process Java-object sharing;
- all regex operations precomputed;
- all text stored once globally;
- complete JDK String replacement;
- speedup from memory reduction alone.

The architectural goal is exact semantics with less avoidable duplication where the measured tradeoff is favorable.
