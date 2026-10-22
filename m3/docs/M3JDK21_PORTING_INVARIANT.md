<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 porting invariant

**M3JDK21 is the canonical product/runtime and target-naming authority. Synexia
is only the convergence workspace and source contributor for this target.**

This is a documentation-only recovery of the policy from #207, commit
`05daa15c75524cb2ed76cfdb0b44d62bacaa47fb`, with explicit asset-reuse clarification.
Merge `d6da66ad1795271cf17769992a62cb47e76f3639` retained that commit as ancestry
without placing this document in its resulting tree. A merged PR is not proof
that its files were materialized.

The existing [name-mapping.json](name-mapping.json) remains the only target
naming/migration authority. This recovery does not restore #207's machine-readable
`porting_policy`, runtime changes, tooling or other artifacts. Their current-tree
reconciliation and qualification remain separate work. The historical family
rows below describe intended policy scope, not a fresh census of implemented
classes or accepted behavior. No pending status or admission gate is promoted.

## JDK contracts and internal names

Public JDK APIs keep JDK names: `java.lang.String`, `java.lang.Class`,
`java.lang.ClassLoader`, `java.util` interfaces/classes and existing compiler
contracts are not renamed to mirror a donor. **M3-prefixed names identify
internal replacement or optimization owners, not new public JDK contracts.**

The naming analogy is `com.synexia.indexstring.MIndexString` →
`java.lang.M3String`, behind the retained public `java.lang.String` contract.
Keep the existing target spellings and packages, including `jdk.internal.mindex`.
Use [counterpart-naming.md](counterpart-naming.md) for concise names and acronyms.
Source compatibility names stay in Synexia. A new target name needs per-type
review in the same mapping file; a prefix swap is not compatibility proof.

## Canonical product ownership

An M3 String value is a canonical owner reference plus packed coordinate.
Shared mapped/native atoms and canonical coordinate composition own the text;
an operation must not create a second retained spelling store. Precomputed facts
belong to exact owner/generation/range/composition and semantic context. Facts
do not own spelling, a filter hit is not equality, and eviction may change only
performance. JNI-created byte/char/UTF compatibility projections are shadows,
never canonical String payload.

Collections reuse the existing primitive, packed, segmented or other appropriate
owners. N logical entries must not require N retained structural wrappers.
Required public entry/array projections preserve the JDK caller contract.
Storage changes preserve order, equality, null/exception behavior, iteration,
concurrency and lifetime obligations of the replaced operation.

The adapted Java, JNI/native, HotSpot, precompute, format and compiler owners
live in the target's appropriate layer. M3JDK21 builds and runs without Synexia
modules, packages, services, databases or processes. Maven/OpenRewrite is
authoring and proof tooling, not a runtime dependency. Classloader identity,
verification, initialization, access checks, GC/JNI lifetime and deoptimization
cannot be bypassed by a matching hash/version key. Radical class-file, linkage
or identity changes belong in a separately scoped experimental project.

## Existing family mappings

| Family | Existing target owners or reserved mappings | Boundary |
| --- | --- | --- |
| String | `java.lang.M3String`, `java.lang.M3StringOwner`, `java.lang.M3StringAtom`, `java.lang.M3StringPool`, `java.lang.M3StringTuple`, `jdk.internal.mindex.M3StringBacking`, `jdk.internal.mindex.M3MappedStringBacking` | Public java.lang.String retains its JDK name and contract; M3String and its existing owners implement internal canonical storage. |
| Precompute | `java.lang.M3StringFacts`, `java.lang.M3StringSearchPrecompute`, `java.lang.M3StringPositionPrecompute`, `jdk.internal.mindex.M3TQ` | Derived exact-owner/range/composition facts and conservative filters; no second spelling store, public donor facade or positive-match shortcut. Broader non-String facts stay in their appropriate layer. |
| Collections | `jdk.internal.mindex.M3Address28`, `jdk.internal.mindex.M3IntLane28`, `jdk.internal.mindex.M3LongLane28`, `jdk.internal.mindex.M3BitLane28`, `jdk.internal.mindex.M3Bits` | Preserve public java.util names and contracts. Reuse existing primitive/packed/segmented owners without retained structural objects per logical entry; bounded lane presence does not admit automatic collection replacement. |
| JNI and native | `java.lang.M3String.nativeCharShadow`, `java.lang.M3String.nativeByteShadow`, `src/java.base/share/native/libjava/M3BitLane28.c` | Target-owned JNI/native linkage, lifetime and ABI. String byte/char exports are compatibility shadows, never canonical payload; no Synexia runtime service or library dependency. |
| Class, version and AST | `jdk.internal.mindex.M3VI`, `jdk.internal.mindex.M3Release`, `jdk.internal.mindex.M3CB`, `jdk.internal.mindex.M3CI`, `jdk.internal.mindex.M3Class`, `jdk.internal.mindex.M3PC`, `jdk.internal.mindex.M3AST`, `jdk.internal.mindex.M3ASTPC` | Public Class/ClassLoader identity remains JDK-owned. Preserve defining-loader/module/generation boundaries and invalidation; reserved AST mappings do not imply implementations or admission. |
| Compiler | `com.sun.tools.javac.m3.M3ASTPC` | Keep compiler-specific owners in jdk.compiler and retain the reserved mapping until qualified. No javac dependency in java.base; Maven/OpenRewrite is authoring/proof tooling, not the runtime compiler or proof of JIT/AOT integration. |

The String/precompute detail map remains
[synexia-string-precompute-port-map.tsv](synexia-string-precompute-port-map.tsv).
Non-String precompute stays in its appropriate layer; this policy does not
transplant every Synexia fact or application structure into `java.lang.String`.
Swing belongs to `java.desktop`; JavaFX/scene-graph integration needs its own
target/module decision. No existing mapping, exclusion or unresolved gate is
removed by these family summaries.

## Full precompute second-pass invariant

Synexia may experiment with wide precompute surfaces; M3JDK21 does not copy that width into
`java.lang.String` or public JDK objects by default. Broad reusable text/search/fuzzy/code-text
precompute first lands in the isolated target-owned `m3/ports/precompute` adaptation layer using
M3 target names and no Synexia runtime dependency.

Promotion from that layer into `java.base`, `java.util.regex`, collections, compiler or another
JDK module requires a named concrete consumer and its own contract, retention, lifecycle,
differential, memory, CPU and native/JNI gates. Fixed-size String-semantic geometry stays with the
existing `M3StringFacts` family; operation-specific bounded plans stay with existing
`M3StringSearchPrecompute`, `M3StringPositionPrecompute` and `M3TQ` owners.

Approximate signals such as SimHash, MinHash/Jaccard, fuzzy scores, code-likeness, regex-likeness,
Bloom-like signals and candidate rankings are **never semantic authority**. They may reject only
when their mathematical contract proves a safe negative condition; otherwise they may order or
nominate candidates only. Equality, edit thresholds, regex matches, compiler behavior and public
JDK results remain exact-authority operations.

Every LLM-assisted source-changing M3JDK task remains recipe-first: develop or improve the
Maven/OpenRewrite transformation exclusively in `hsoliwal/com.synexia`, validate exact
preimages/postimages and fixed-point refusal there, and independently verify the resulting Java/JNI
product source in M3JDK21. No manual production-file patches and no second transformation
framework. The recipe bodies, templates, authoring manifests and donor experiments stay in
Synexia. M3JDK21 owns only its qualified generated product source, public contracts, names,
tests, provenance references, runtime verification and promotion receipts.

## Convergence, handoff and promotion

Recipe-module code is also intake material. In the inspected Synexia root the
relevant modules include `synexia-m3-recipe` and `synexia-openrewrite-recipes`.
They may contribute Java/JNI algorithms, recipe-generated postimages,
atomizer/patternizer compositions, fixtures, catalogues and proof receipts.
Inventory each actual source body, history, contract and dependency closure:
qualified runtime algorithms may enhance the existing JDK owner, while recipe
execution and convergence tooling remain outside the product runtime. A module
name or generated file alone establishes neither reuse rights nor admission.

1. Synexia develops and qualifies a source contribution through an existing
   Maven/OpenRewrite recipe, with exact revision/path/blob, contracts, fixtures,
   dependencies, licence/NOTICE, before/after images and refusal/fixed-point proof.
2. M3JDK21 resolves the existing target owner, reconciles both histories, adapts
   the qualified atoms and updates this map's existing lineage and receipts.
   Product bytes come from the executed recipe; no competing naming catalogue.
3. The target independently verifies contract, lifecycle, API/ABI, JNI and
   supported compiler modes, plus the matching complete image and required
   admission gates. Source-only or finite-fixture passes are not full admission.
   Preserve the tested fallback/invalidation paths and current execution limits.
4. Measure cold preparation separately from warm reuse; distinguish work counts
   from elapsed time/allocation evidence. Preserve prior proven capabilities.
5. Target receipts, missing capabilities and measured requirements flow back to
   Synexia convergence. Runtime and target-naming authority stay in M3JDK21.

The compatibility baseline is OpenJDK 21. Added internal capability may exceed
it only while preserving the required public behavior. A family mapping is a
scope/accounting statement, not an implementation or performance claim. Existing
pending/blocked/unverified statuses and full-image gates remain in force.

Eligible copied Synexia code retains its Apache-2.0 provenance. Existing OpenJDK
and third-party licences retain their own scope; a port does not relicense them.
See [m3-porting.md](m3-porting.md) for the serial execution order and
[migration-mapping-lifecycle.md](migration-mapping-lifecycle.md) for record updates.

## Reuse qualified Synexia results; never duplicate recipe ownership

**SYNEXIA_EXCLUSIVE_RECIPE_HOME.** Apache-2.0 permits reuse of eligible first-party
material but does not require all source types to be shipped in every target.
Synexia alone owns donor harvesting, recipe source/classes, Maven/OpenRewrite
authoring crates, templates, execution and candidate qualification. M3JDK21,
JDK, Eclipse/SWT and other public product forks may receive **only** the
qualified Java/JNI/native transformation results and corresponding public tests,
compatible product build logic, licensing/provenance, mapping and proof receipts.
No new reusable or target-specific recipe source is to be copied into a public
product fork. JDK-specific backports are authored as Synexia recipes against
pinned JDK preimages, then received as tested results. The product remains
independent of Synexia at runtime and controls its own API/JNI/GC/JIT gates.

Previously deposited recipe source or mirror files in M3JDK21 are legacy
migration debt; their presence is not permission for a second recipe owner.
Retire them through separately qualified, proof-preserving migrations. The
product PR #1810 and Synexia PR #12951 are an example: five exact source
results remain in the product, while three canonical recipe crates, their
manifests and replay tests live in Synexia.

Keep applicable Apache-2.0 licensing and copyright/attribution/NOTICE material,
record modifications, and carry exact donor -> Synexia -> target lineage and
dependency/recipe/postimage pins. Existing Apache-2.0 grants need no bespoke
per-recipient grant, but their conditions and exact target compatibility still
apply. M3JDK21 remains the independent product/runtime and naming owner; copied
recipe execution stays authoring/proof tooling, never a JDK runtime dependency.

This policy does not relicense inherited OpenJDK, third-party donors or other
contributors' work. The repository's [ADDITIONAL_LICENSE_INFO](../../ADDITIONAL_LICENSE_INFO)
distinguishes independent modules from commingling incompatible code in GPLv2
files. Apache-2.0-only provenance is not automatic permission for the latter.
Any necessary compatible grant must come from the relevant rights holders;
this policy and its recipes grant no additional licence or exception. Generated
output retains obligations arising from actual copied input/template material,
not simply the licence of the tool used to generate it.

**MERGED_ANCESTRY_IS_NOT_MATERIALIZED_CAPABILITY.** Verify exact receiving paths
and postimage hashes, then bind executed tests to that receiving tree before
promoting an export. This policy recovery changes no Java/JNI/runtime behavior,
does not restore all of #207, and establishes no build, performance or full-image
PASS. Preserve public contracts, require applicable memory/CPU and native gates,
and return qualified improvements to the existing Synexia convergence owners.

References: [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0),
[ASF GPL compatibility guidance](https://www.apache.org/licenses/GPL-compatibility.html),
and the existing [Synexia donor boundary](https://github.com/hsoliwal/com.synexia/blob/develop/docs/M3JDK21_DONOR_BOUNDARY.md).
