<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 porting and naming invariant

Status: canonical migration policy for work entering `hsoliwal/M3jdk21`.

## Authority split

**Synexia is the convergence workspace. M3JDK21 is the product destination.**

Synexia may contain experiments, donor adaptations, OpenRewrite recipes, MIndex/MatIndex
implementations, JNI prototypes, precompute images, benchmarks and convergence evidence. That
does not make Synexia package/type names part of M3JDK21's runtime ABI.

A capability that graduates into M3JDK21 is **adapted into the target's existing JDK/M3 owner and
name**, with exact source lineage, license provenance and target-side tests. M3JDK21 must not
acquire a runtime dependency on Synexia.

## Full Synexia donor universe

For this target, **the whole qualified Synexia workspace is a candidate donor universe**. Do not
artificially restrict intake to runtime algorithms. Candidate donor material includes:

- Java/JNI/native source atoms and adapters;
- Maven/OpenRewrite recipes, atomizer/patternizer rules and recipe compositions;
- generated postimages, templates, manifests and migration crates;
- tests, fixtures, hostile corpora, mutation cases and differential oracles;
- documentation, schemas, naming maps, source-invariant gates and proof receipts;
- precompute layouts, indexes, collections, search plans and immutable image formats;
- algorithms, architecture patterns and ideas that are independently reimplemented inside the
  JDK under target ownership.

Recipes are first-class donor code. They may be copied/adapted into M3JDK21 authoring/proof
tooling when their licensing and provenance permit it. They do **not** become a Synexia runtime
dependency merely because they generated or verified an admitted target postimage.

## License boundary for public M3JDK21 code

Synexia's root project is Apache-2.0, but M3JDK21/OpenJDK has its own per-file/module licensing.
Therefore:

1. Synexia-original material actually covered by Apache-2.0 is a legitimate donor candidate.
2. Do not assume that an Apache-2.0 repository label permits source text to be pasted into any
   GPLv2/OpenJDK file. The destination file/module license and OpenJDK's existing exceptions remain
   authoritative.
3. When the copyright owner of Synexia first-party code has the right to contribute the same
   material under target-required terms, record that target contribution explicitly rather than
   pretending the target file was relicensed by automation.
4. Third-party code never becomes Synexia-original or Apache-2.0 merely because Synexia wrapped,
   atomized, tested, indexed or generated from it. Preserve original license/NOTICE obligations or
   use the donor only as reference/evidence for an independent target implementation.
5. A recipe's license does not automatically relicense its inputs or generated output. Every
   generated postimage must satisfy the destination's provenance and license rules.

This is an engineering admission rule, not permission to weaken OpenJDK licensing checks.

## M3String is the naming analogy

The canonical example is:

```text
Synexia donor/reference                 M3JDK21 target
------------------------------------    ---------------------------------------------
MIndexString                         -> java.lang.M3String
MIndexStringCanonicalFacts          -> java.lang.M3StringFacts / internal fact owners
MIndexStringSearchPlan              -> java.lang.M3StringSearchPrecompute
MIndexPositionMasks                 -> java.lang.M3StringPositionPrecompute
MIndexRegexTrigramQuery             -> jdk.internal.mindex.M3TQ
MIndexStringBacking                 -> jdk.internal.mindex.M3StringBacking
MIndexMappedStringBacking           -> jdk.internal.mindex.M3MappedStringBacking
```

The public Java API remains `java.lang.String`. `M3String` is the target-owned internal value
representation. Donor `MIndex*` names are provenance and migration evidence, not names to copy
blindly into the target.

Apply that same pattern to the rest of the port.

## Naming laws

1. **Public JDK contracts keep their JDK names.** `String`, `ArrayList`, `HashMap`,
   `ConcurrentHashMap`, `Pattern`, `Matcher`, `Class`, javac APIs and other standard
   surfaces do not become public `M3*` or `MIndex*` APIs.
2. **Internal replacement owners use the established M3JDK21 M3 vocabulary.** Reuse an existing
   M3 owner before creating a new one.
3. **Do not mechanically rename `MIndexFoo -> M3Foo`.** Map capability and ownership, not prefix.
   One donor may split across several target owners; several donors may consolidate into one target
   owner.
4. **Do not create a second payload owner.** Precompute, search plans, indexes and JNI shadows attach
   to canonical target identity and may be evicted without changing semantics.
5. **Do not move application/search corpus images into public JDK runtime state.** Batch images,
   postings, fuzzy graphs, GPU search images and challenge-corpus artifacts require a concrete
   target consumer and normally stay outside `java.lang` / `java.util`.
6. **JNI/native code follows target ownership and ABI.** Synexia JNI is donor/reference code. A
   target port generates/adapts target symbols, headers, lifetime rules and tests.
7. **Maven/OpenRewrite recipes are first-class donor artifacts and authoring/proof tooling, not JDK runtime dependencies.**
8. **Every port is recipe-first.** Exact donor revision + target preimage -> reviewed postimage;
   compile/test/runtime proof -> fixed point; rollback/refusal remains available.
9. **Every port updates the naming/migration map.** No implementation lands without a mapping row or
   an explicit reviewed exclusion/defer disposition.
10. **Reverse feedback is allowed, ownership reversal is not.** M3JDK21 may propose optimizations
    back to Synexia, but target naming/ABI remains target-owned.

## String and precompute family

String precompute is internal-only. Use the existing owners:

| Donor responsibility | M3JDK21 owner |
| --- | --- |
| canonical immutable string value | `java.lang.M3String` |
| scalar/range fixed facts | `java.lang.M3StringFacts` / owner range facts |
| prepared exact search | `java.lang.M3StringSearchPrecompute` |
| character/code-point positions | `java.lang.M3StringPositionPrecompute` |
| trigram/regex candidate facts | `jdk.internal.mindex.M3TQ` + `Pattern/Matcher` |
| mapped/native backing | `jdk.internal.mindex.M3StringBacking`, `M3MappedStringBacking` |
| canonical scalar/tuple ownership | `M3StringOwner`, `M3StringAtom`, `M3StringPool`, `M3StringTuple` |

SimHash, MinHash, fuzzy-distance, n-gram, regex-shape and code-text signals may be useful internal
candidate facts where a concrete JDK consumer exists. Candidate signals never replace exact String,
regex or compiler semantics.

## Collection family

Collections follow the same naming/ownership rule, but **per concrete contract**, never by replacing
the `Collection` interface with one universal M3 container.

Existing reusable target owners include:

- `jdk.internal.mindex.M3Address28`
- `jdk.internal.mindex.M3IntLane28`
- `jdk.internal.mindex.M3LongLane28`
- `jdk.internal.mindex.M3BitLane28`
- `jdk.internal.mindex.M3Bits`
- the separately verified Apache-2.0 `com.m3.collections` lane owners.

The public JDK classes remain `ArrayList`, `HashMap`, `LinkedHashMap`, `TreeMap`,
`IdentityHashMap`, `ArrayDeque`, `PriorityQueue`, `ConcurrentHashMap`,
`CopyOnWriteArrayList`, etc. A replacement changes their **backend**, not their public name.

Collection ports must preserve, as applicable: equality/identity, null policy, order, backed views,
iterator/spliterator behavior, fail-fast/weak/snapshot semantics, serialization, subclass hooks,
JMM/linearization/progress and concurrency. No-per-entry-object storage is a target optimization,
not permission to weaken those contracts.

## Class, AST, compiler and other families

| Donor family | Target naming direction |
| --- | --- |
| `MIndexClass*` | existing `M3Class`, `M3CI`, `M3CB`, `M3PC`; public API stays `Class` |
| `MIndexAST*` | `jdk.internal.mindex.M3AST`, `M3ASTPC` after format/consumer proof |
| compiler AST precompute | `com.sun.tools.javac.m3.M3ASTPC`; never pull javac into `java.base` |
| primitive/bit/address storage | existing `M3*Lane28`, `M3Address28`, `M3Bits` |
| regex/search acceleration | existing String/regex owners or optional `jdk.internal` accelerator |
| mapped/native storage | target-owned backing/native owners with JDK lifetime and ABI proof |
| GPU/batch/search corpus images | separate optional target module or deferred; not public JDK state |

## Placement and module rule

Choose the target by the consumer:

- `java.base`: only bootstrap-safe runtime owners needed by admitted `java.base` consumers.
- `jdk.internal.mindex`: internal M3 kernels shared inside `java.base`.
- `java.util` implementation files: public collection owners retain JDK names and APIs.
- `jdk.compiler` / `com.sun.tools.javac.m3`: compiler-only M3 state.
- `m3/collections`: separately built Apache-2.0 collection/storage proving ground; promotion into
  JDK owners still needs JDK contract proof.
- other JDK modules: keep module boundaries; do not drag Synexia dependencies into the runtime.

## Port acceptance sequence

For every source atom/module:

1. pin Synexia commit/blob/license and all non-prefix dependencies;
2. resolve the canonical M3JDK21 owner/name from `name-mapping.json`;
3. add/update the Maven/OpenRewrite recipe crate before manual target edits;
4. materialize only exact admitted postimages;
5. run compiler/JUnit/jtreg/native/JNI and relevant interpreter/C1/C2/image gates;
6. prove source/API/behavior and failure-contract parity;
7. record memory/performance evidence separately from correctness;
8. update mapping, receipts and unresolved gates;
9. feed target findings back to Synexia as proposals when useful.

A successful donor test or Synexia benchmark is not target acceptance.

## Repository rule for other threads

When work begins in Synexia but its intended owner is JDK runtime, collections, compiler or another
M3JDK21 subsystem, the thread must end with one of:

- an M3JDK21 port PR using the canonical target naming;
- an explicit `PENDING` mapping with the missing target gates; or
- an explicit `DO_NOT_PORT` / higher-layer disposition.

Do not leave target-ready runtime work only in Synexia and call the migration complete.
