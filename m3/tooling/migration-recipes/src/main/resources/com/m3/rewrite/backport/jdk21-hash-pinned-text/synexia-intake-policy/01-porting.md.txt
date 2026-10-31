<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 porting and naming invariant

Status: canonical migration policy for work entering `hsoliwal/M3jdk21`.

## Authority split

**Synexia is the convergence workspace. M3JDK21 is the product destination.**

Synexia may contain experiments, donor adaptations, OpenRewrite recipes, MIndex/MatIndex
implementations, JNI prototypes, precompute images, benchmarks and convergence evidence. That
does not make Synexia package/type names part of M3JDK21's runtime ABI.

A capability that graduates into M3JDK21 is **adapted into the target's existing JDK/M3 owner and
name**, with Apache-2.0 provenance for eligible Synexia code, exact source lineage and target-side
tests. M3JDK21 must not acquire a runtime dependency on Synexia.


## Synexia intake and licensing modes

M3JDK21 may consume Synexia broadly, but the intake **mode** is part of the proof.

The machine-readable authority is `m3/docs/synexia-intake-policy.tsv`. The repository's own
`ADDITIONAL_LICENSE_INFO` explicitly distinguishes independently licensed Apache-2.0 programs
from copying incompatible-license code into GPLv2 source files. Therefore:

- `EXTERNAL_RECIPE_TOOL` — Synexia Maven/OpenRewrite recipes may be executed as authoring/proof
  tooling. They never become a JDK runtime dependency.
- `INDEPENDENT_APACHE_MODULE` — separately built `m3/**` modules may remain Apache-2.0 when
  their LICENSE/NOTICE and module boundary are preserved.
- `DATA_OR_METADATA_EXPORT` — immutable images, sidecars, facts and manifests cross the boundary
  only with source-license, revision, schema and hash proof.
- `TARGET_OWNED_PORT` — a capability entering `src/**` is adapted into the canonical
  GPL/OpenJDK-owned target implementation and is not admitted as direct Apache source copy.
- `EXPLICIT_RELICENSE_PORT` — direct source reuse inside `src/**` requires recorded authority
  from all necessary rightsholders under target-compatible terms and correct target headers.
- `EXISTING_DIRECT_COPY_REVIEW` — historical Synexia records already marked
  `copied_code=true` under `src/**` are not silently grandfathered. They are enumerated in
  `m3/docs/synexia-direct-copy-review.tsv` and remain review-required until either compatible
  relicensing is proven or the target is replaced by a target-owned implementation.

`m3/runtime-integration/verify-synexia-intake.py` fails closed if the mode set, direct-copy
review inventory, FOSS provenance, or repository licensing boundary drifts.

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
7. **Maven/OpenRewrite recipes are authoring/proof tooling, not JDK runtime dependencies.**
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
