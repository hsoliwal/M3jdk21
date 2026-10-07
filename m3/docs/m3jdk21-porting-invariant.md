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


## Apache-2.0 reuse includes recipes

M3JDK21 may copy, adapt and redistribute eligible Synexia-owned Apache-2.0 contributions,
including Java/JNI/native code, Maven/OpenRewrite recipe implementations and definitions,
atomizer/patternizer compositions, fixtures, catalogues and proof tooling. There is no additional
Synexia-specific permission gate for rights already granted by Apache License 2.0. Synexia's
[convergence/delivery model](https://github.com/hsoliwal/com.synexia/blob/develop/SYNEXIA_CONVERGENCE_MODEL.md)
extends the same reuse to SWT, Nebula and other delivery targets.

For each intake, retain the exact donor commit/path/hash, Apache-2.0 license, applicable NOTICE,
attribution and modification notices. Inventory embedded third-party sources and recipe payloads
separately: the recipe license does not relicense upstream source or generated target postimages.
OpenJDK source keeps its existing licensing; independent Apache-2.0 material keeps its scoped
license. Reuse is subject to those existing terms and the target's integration requirements.

Use the existing Maven/OpenRewrite control plane and naming/migration map. Import the needed
source and qualified recipes into the target's existing owners; do not add a Synexia runtime
service or package dependency to Java/HotSpot/JNI. Carry compiler, behavior, replay, fixed-point
and refusal receipts with each candidate. Resource-sensitive ports also carry CPU, retained-heap
and native/process-memory evidence with exact workloads and environments. Target tests and pending
platform gates remain explicit; a reusable donor recipe alone does not establish target acceptance.


# M3-SYNEXIA-RECEIVER-1: full Synexia to M3JDK lineage

Synexia is the canonical convergence and reusable-recipe owner. M3JDK21 receives the
qualified capabilities into its existing product owners. Every continuation inherits
the source ledger and executed evidence before changing an atom. This invariant covers
the previous MIndexString work, the MIndex* data structures, AST/compiler plane and
static precompute; the literal replacement atom does not delimit migration scope.

The centre is canonical MIndexString/M3String ownership -> static regex precompute ->
String operations -> JNI/native projections. In Synexia, MIndexString resolves canonical
atoms, owner/range coordinates and compositions. In M3JDK, use the existing M3String,
M3StringOwner/Atom/Tuple/Pool and mapped backing owners. Public java.lang.String and its
observable contracts stay authoritative. char[] and byte[] are ingress/export or ABI
projections through those owners. A cache, precompute image, JNI shadow or legacy field
must not quietly become a second canonical spelling store.

## Scope and accounting

Read the catalogue index and all relevant shards under
`synexia-openrewrite-recipes/verification/synexia-m3jdk-full-family-20261007`.
Its pinned source is Synexia 7d2133f1412a9e7295296c3f86baae577bb3251c.
It records 4,770 MIndex*-named paths in 13 explicitly checked module roots: 3,108
production source/resource paths (2,556 Java and 552 resources/payloads), 1,463 test/resource paths, 195 retained evidence/recipe payloads
and four support paths. They are file identities, not 4,770 independently admitted runtime
classes. All begin NOT_EVALUATED_IN_THIS_LEDGER. Historical declarations remain attached
without converting their IMPLEMENTED labels into current target acceptance.

The checked-root catalogue is not whole-repository completion. Non-prefix dependencies,
Index*/MatIndex*/M3* owners, nested types, other modules and later revisions require further
inventory. Missing promised Git bodies are unresolved inputs, not evidence of absence.
The existing representative 28-family table and String responsibility checks remain useful
indexes; neither establishes complete migration. Expand the source closure before promotion.

Every evaluated source atom records exact donor revision/path/blob/raw hash, original
licence/copyright, dependencies and effects, existing receiver owner/module/path, recipe
identity, target preimage/postimage, evidence and outstanding gates. Each atom ends with
verified target acceptance, an explicit PENDING reason, or a justified DO_NOT_PORT / higher
layer disposition. Unknown or omitted rows cannot silently count as complete. Every wider
FILE -> PACKAGE -> MODULE -> PROJECT -> REPOSITORY claim requires its narrower proofs.

## Ordered receiving work

1. Preserve the admitted canonical String/storage/JNI lineage and stronger receiving helpers.
   Qualify the current M3JDK runtime rather than substituting a historical MIndexString image.
2. Prioritize full static regex precompute. The exact admitted v7 MIndexRegexProgram
   execution image, boolean runtime, codec and dependency closure remain the next receiver.
   M3TQ already retains the admitted v7 query/Facts operations; do not re-port it or substitute
   the different M3RegexAutomaton engine. Split cold compiler facilities from bootstrap-safe
   runtime only through attributed dependency/effect evidence and target compilation.
   java.lang.management.ThreadMXBean is not readable from java.base; a wholesale class rename
   is not a qualifying port. Exact UTF-16 keys, version/options/owner identity, cold preparation,
   zero-work warm reuse, invalidation and malformed/unauthorized artifact refusal need proof.
3. Receive AST/class/compiler facts and atomizer/patternizer recipes through existing M3AST,
   M3ASTPC, M3Class/M3CI/M3CB/M3PC and javac owners after consumer/format proof. javac-only
   parsing, binding and compiler effects stay in jdk.compiler or authoring tooling. Code-looking
   strings, regex, comments and text blocks stay payload. Compile every bounded multipass
   candidate and compare contracts and an independent behavioral oracle before a fixed point.
4. Receive MIndex* collection, bit/address/lane, tree/DAG, relation, file/source and generic/
   universal precompute capabilities by concrete consumer. Preserve null/equality/identity,
   order/views/iteration, serialization, JMM/concurrency and lifetime contracts as applicable.
   Domain, distributed, corpus and hardware state uses the existing separate target plane when
   justified; every deferred or excluded family remains visible in the ledger.
5. Review Tweety, RE2/J, native/GPU and other historical donors at their exact source/license
   boundaries. Catalogue LeetCode, HackerRank and GeeksforGeeks problem categories before
   selecting licensed GitHub source; platform statements, editorials and submissions are
   reference material, not copying permission. Hardware/native absence must preserve the
   admitted Java contract; GPU caps cannot silently truncate supported inputs.

The held V8 full-query owners remain NOT_ADMITTED: a correctly keyed/checksummed program can
disagree with its restored query, and physical DAG bounds do not bound unfolded recursive work.
The bounded 16,433-check witness recipe preserves both failures, six effectful-predicate cases,
the 16,384-callback shared DAG and stack overflow in three APIs. Do not publish/admit those
owners or invent approval authority from cache association. Define the canonical artifact
admission contract before repairing it; preserve callback order, duplication, short-circuiting
and exceptions when replacing recursive evaluation.

## Recipe and proof custody

Use the existing Maven/OpenRewrite control plane and M3SerialFileAtomConvergenceRecipe.
SEARCH -> INVENTORY -> CLASSIFY -> PROVE FIT -> ATTRIBUTE -> GENERATE ONLY THE GAP remains
mandatory. Record the FOSS_REUSE_DECISION row before substantial generation. Improve the
Synexia recipe, export pinned postimages, then apply to exact target preimages. Do not fork a
compiler, parser, convergence laboratory, precompute framework or reusable recipe authority.

Keep executed source compilation, semantic oracle, code-containing in-memory multipass corpora,
ordered subset/permutation/combination coverage, fixed point, second application, refusal and
source-drift evidence distinct. Target build/jtreg, interpreter/C1/C2/GC/CDS, native/JNI lifetime,
provider/platform, full reactor and dependency-mirror gates are applicable acceptance evidence;
record each missing gate explicitly. Hashes or candidate filters never establish exact semantic
equivalence. Measure cold and warm cost separately; finite correctness, merges and operation
counts do not establish exhaustive convergence or performance gains.

## Copyright, design lineage and licence

Synexia first-party implementation, reusable recipes and design documentation covered by its
licence retain this recorded attribution:

    Copyright 2026 Hitesh Soliwal and contributors
    Licensed under the Apache License, Version 2.0.

Preserve the exact applicable LICENSE, NOTICE, per-file copyright and modification notices,
source history and design/algorithm lineage in every handoff. Apache-2.0 reuse needs no extra
Synexia permission beyond that licence's terms. The attribution documents the contributed work
and its provenance; it does not create an exclusive right over abstract ideas.

Third-party material retains its own rights. Tweety's recorded GPL/LGPL ambiguity remains
reference-only; RE2/J BSD notices and Tornado's API/runtime split remain separate. Unknown
per-file licences require review even where a repository root is Apache-2.0. A recipe's licence
does not relicense its inputs or generated output. M3JDK/OpenJDK files keep their existing
licences and rightsholder notices. Use the existing synexia-intake-policy.tsv modes and
verify-synexia-intake.py boundary; direct source reuse into GPL/OpenJDK owners requires the
recorded compatible rights or target-owned implementation mode. Independent Apache tooling
remains scoped. No Synexia runtime service/package dependency enters Java/HotSpot/JNI.

Licence text: https://www.apache.org/licenses/LICENSE-2.0
Canonical target policy: m3/docs/m3jdk21-porting-invariant.md in hsoliwal/M3jdk21.
