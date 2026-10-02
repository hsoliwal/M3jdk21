# M3 whole-JDK migration architecture and compatibility decisions

## Whole-JDK authority and document topology

This document is the detailed implementation architecture for the whole-JDK M3 program. The overview and coverage denominator live in ../../docs/whole-jdk-migration-scope.md. Module-level planning dispositions live in ../../docs/whole-jdk-subsystem-matrix.md. The canonical detailed collection contract lives in ../../docs/whole-jdk-collections-replacement.md; COLLECTIONS.md is only its migration execution overlay. Dependency-ordered implementation packets live in WORK_PACKETS.md. Existing String/text architecture remains in ../../docs/shared-atom-concatenation.md, ../../docs/mindex-migration-handoff.md and ../../../doc/mindex-string-backing.md.

The machine-readable operational authority remains ../../docs/name-mapping.json plus the existing validated migration tooling. None of these Markdown files is a replacement registry. Planning rows may propose dispositions; only a separately reviewed operational update may change capability state, synchronized pins or evidence bindings.

The whole-JDK goal is selective backend replacement and reuse under preserved Java/JVM contracts, not uniform representation. Every inventoried surface receives one disposition: replace-backend, adapt, reuse, retain-pending-evidence, platform-specific, blocked or deferred. Whole-JDK coverage is not complete until every denominator entry has a reviewed disposition and every claimed replacement closes its applicable gates.

## Architecture layers

1. Canonical data and lifetime ownership: immutable atoms, mutable owners, reference lanes, primitive lanes, namespaces, generations and publication.
2. Storage and indexing: arrays, segmented ranges, compact tables, trees, bitmaps, CSR relations, dictionaries and mapped images.
3. Algorithms and precomputation: exact reusable facts, filters, sorting/searching/bulk kernels and bounded caches with validity domains.
4. Public Java compatibility: ordinary API objects, views, adapters, boxing/materialization boundaries, serialization and reflection.
5. Compiler transformation: attributed Route B lowering with fail-closed semantic preconditions and source-pinned recipes.
6. VM/native integration: object layout, GC/barriers, interpreter/JIT/intrinsics, JNI/JVMTI/CDS/JFR/serviceability and OS/CPU variants.
7. Diagnostics and evidence: exact pins, mapping lineage, build/image identity, differential/concurrency/runtime tests and performance/memory receipts.

Dependencies flow downward wherever possible. Tooling, Maven/OpenRewrite, application frameworks, UI and network services stay outside early java.base bootstrap. A higher-level owner may project onto a lower-level M3 substrate; the substrate must not recursively depend on that higher layer.

## Cross-cutting semantic laws

- Immutable sharing is permitted only inside a declared immutable identity/lifetime domain.
- Mutable public objects remain independently observable; equal mutable collections are never silently value-interned together.
- Java object identity, value equality, comparator equivalence, canonical atom identity, arena handles and persistent image coordinates remain distinct.
- Hashes, similarity scores and precomputed signals can reject candidates or accelerate lookups; they do not replace exact equality unless the canonical identity contract itself proves exact payload identity.
- GC must see every live heap reference. Off-heap/native IDs cannot replace references without reviewed rooting/barrier/lifetime integration.
- Java arrays remain fixed-size contiguous Java objects. Segmented or native layouts require explicit descriptors/adapters and materialization where a contiguous array is required.
- Precomputation has admission, invalidation, eviction, concurrency and amortization costs. A local allocation saving cannot justify an unbounded process-global cache.
- A passing Route A experiment does not certify compiler lowering or a matched custom JDK.

## First-class replacement workstreams

String/text and collections are the first deep vertical slices because they exercise immutable sharing, mutable identity, views, generic boundaries, JNI/VM integration and performance measurement in different ways.

String retains exact UTF-16 semantics, shared-lexicon plus VM-local ownership, reference-only joins/slices, explicit materialization and independent text/atom/composition/object identity. Route C must coordinate StringLatin1/StringUTF16/concat, StringTable, GC dedup, CDS, JNI/JVMTI, interpreter/JIT and serviceability.

Collections use the family-by-family contract in COLLECTIONS.md. Candidate primitive/indexed/segmented forms do not automatically eliminate boxing or node objects at public generic boundaries. IdentityHashMap reference identity, weak-reference families, live views, serialization, subclass hooks and concurrent JMM guarantees are explicit stop gates.

All remaining JDK/HotSpot work is decomposed in WORK_PACKETS.md and advances in dependency order rather than by filename similarity.

## Existing MIndex/MatIndex slice authority

This document consolidates the requirements relevant to the first implementation tranche. The broader three-route design remains `m3/docs/shared-atom-concatenation.md`; none of its proposed runtime contracts becomes accepted merely by being restated here. The machine-readable source-to-target authority is the existing `m3/docs/name-mapping.json`, extended rather than replaced. Every observed item remains visible with a disposition; the current observations are incomplete.

## Layering

| Layer | Owner and responsibility | Current disposition |
|---|---|---|
| Canonical Synexia semantics | `com.synexia.indexstring.MIndexAtomStore`, `MIndexAst`, `MIndexDag`, `MIndexInteraction` | Established owner documentation retained; full implementation closure still to inspect |
| Runtime/experimental projections | Existing MIndex runtime owners and `synexia-mindex-indexstring-bridge` | Reuse where compatible; no numeric-ID translation across owners |
| M3 P0 explicit view | Existing `com.m3.text.M3StringPiece` and its local/joined pieces | Used unchanged by this tranche; provisional, not a complete canonical-storage port |
| Application algorithms | New `com.m3.algorithm.M3PrefixZ` | Read-only prefix facts, outside java.base |
| Migration/compiler tooling | Separate Java/Maven recipe and Python manifest validator | Tooling only; never a bootstrap dependency |
| Modified String/HotSpot | Existing experiment branch | Not brought onto master or claimed accepted here |

## Three integration routes

**Route A: explicit immutable view.** Preserve exact UTF-16 content, immutable admission, retained atoms/ranges, bounded-depth traversal and explicit String/output materialization. Stock String conversion can require allocating contiguous storage. This tranche tests prefix and selected stock-regex/code-point behavior on existing views, but does not implement their full String API or canonical shared/local interner.

**Route B: compiler lowering.** Reuse the same semantic owner, but require type resolution, eligibility and safe refusal. Preserve evaluation order, side effects, exceptions, nulls, overloads, constants, identity-sensitive operations, public/escaping boundaries, invokedynamic concatenation and mixed callers. Maven/OpenRewrite transformations and negative compiler tests remain pending; an exact-file installation recipe is not compiler lowering.

**Route C: complete modified JDK.** A matched full image must coordinate String, interpreter, HotSpot layouts/GC, StringTable, compiled code/intrinsics, JNI/JVMTI, CDS, deduplication, serialization, reflection and serviceability. Keep opt-in until all required gates pass. Never replace String.class in an installed JDK. JNI is optional and cross-cutting, not a fourth route.

## Shared immutable payload invariant

Logical text consists of canonical atoms and ordered ranges. Shared lexicon hits retain immutable mapped owners; unknown text stays in a VM-local immutable interner. Existing joins must not duplicate joined character/byte payloads, though bounded descriptors and indexes may allocate. Slices retain owners; normalize empty ranges and coalesce only physically adjacent ranges within the same owner/atom. Check length overflow before allocation/publication.

Ordinary arrays are mutable and require defensive copying unless ownership transfer is enforceable. Java arrays cannot alias two disjoint arrays through a single ordinary array reference. Keep text equality, owner/namespace/generation-qualified atom identity, normalized composition identity and Java object/intern identity separate. Equal UTF-16 content across segmentations must yield equal Java polynomial hashes. Cryptographic hashes and similarity scores do not prove exact equality or compose like String hashes.

## Lifetime, storage and bootstrap requirements still open

Use validated offsets, namespaces, generations and format versions rather than process pointers. Immutable file-backed pages can be mapped by independent processes; neither addresses nor local IDs need match. Atomic generation publication, corruption/truncation checks, file replacement rules, crash recovery, reader ownership and eviction safety need implementation and tests. Read-only mappings alone do not prevent another process from modifying/truncating the backing file. Live values must outlive optional lookup caches without stale-handle aliasing.

Load dictionaries after safe bootstrap with lazy/single-flight loading and local fallback. Do not introduce early String recursion or networking. Never automatically persist arbitrary application text or secrets. Track heap, mapped and native memory separately; tiny slices can retain large owners.

## Compatibility decisions

The prefix port changes package, accepted view type and cancellation callback deliberately; it does not replace the original public class. Source/binary compatibility with `MIndexPrefixZ(MIndexString, IProgressMonitor)` APIs is not claimed. A future adapter must preserve progress callbacks separately. No serialization format or JNI ABI is introduced by these facts.

Keep arbitrary UTF-16 slices for Java String behavior, including unpaired surrogates and pairs crossing segments. The alternate runtime SubMIndexString that rejects surrogate splitting cannot be consolidated by renaming. Mutable outputs must be independent storage. Encoding and regex must carry their complete flags/policies/context: fragment encoding is not always whole-text encoding. Approximate filters, RE2/J and GPU subsets must not be labeled full java.util.regex equivalents.

## Porting and evidence

Use explicit capability IDs, pinned references, source/target hashes, lineage, contract deltas, ownership/format constraints, dependencies, materialization boundaries, recipe preconditions/rollback, evidence and provenance. Unknowns stay null/pending with reasons. Three-way comparison is against the last synchronized source and target; target divergence is not permission for blind overwrite. Deterministic text fingerprints assist review, not semantic proof.

The current validator verifies structural records and exact artifact evidence. It does not establish full source-tree completeness, resolve all symbols, prove equivalence, authenticate externally supplied receipts or enforce a continuously synchronized cross-repository CI policy. Full discovery, dependency closure, source-pin refresh and native source-pinned recipe mechanisms remain mandatory follow-up work.
