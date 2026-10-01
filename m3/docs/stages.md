# Integration stages and gates

## P0: independent safe substrate (this draft)

Build immutable local ownership/ranges and a flat joined-leaf directory, preserve exact UTF16 code units, and validate a read-only image format with explicit copy-to-local fallback. This code is independently authored under Apache-2.0 and built as a separate module. It does not participate in String bootstrap or change JVM String layout.

## P1: source-grounded local interning and derived formats

Obtain the actual pinned MatIndex/MIndex contracts before introducing compatibility facades. The provisional name LocalMIndexStringPiece maps to LocalM3StringPiece, but the current class is not asserted to implement an unseen old API. Preserve old names additively until source-bound equivalence tests exist. Do not rename unrelated families by text substitution.

A local interner should compare caller input against immutable canonical storage before allocating new backing on hits. Cache retention must have explicit payload and metadata budgets. Prefer existing suitable primitive-array owners once their source/provenance is verified; no donor imports are present here. Use bounded occupancy/hash/generation/range/recency arrays, not boxed per-entry maps or linked-list nodes. Compare LRU, MRU, direct-mapped and scan-resistant admission policies with deterministic reuse/scan/pressure traces before choosing one. A cache-slot eviction must never cause a live identity/generation to resolve to different content. Immutable owned handles can survive cache eviction; derived facts and encoded forms can be evicted separately.

Optional encoded forms require keys including charset and malformed/unmappable-input actions, independently charged retained bytes, and bounded admission tracking. Do not eagerly retain both char and every encoded representation for every word. Prepared-operation benchmarks must exclude one-time preparation from their warm timing, while separately reporting preparation CPU/time, retained memory, cold totals and justified break-even reuse counts.

## P2: shared lexicon ownership

Use versioned content-addressed images published once, never rewritten in place. Prebuild an English shard from explicitly licensed data; the included original 13-word fixture only exercises the format. Open/warm after bootstrap in the first JVM. Later JVMs can map the same inode for OS page-cache sharing. Language/domain shards remain explicit lazy first-touch operations with local fallback, never optional class initialization or network access in String constructors.

Before publishing canonical handles directly backed by a mapping, define and test immutable-file enforcement or platform sealing, image identity/generation, range validation, reclamation, corruption/truncation behavior and cross-process version transitions. File read-only mode alone is insufficient. This draft copies records before publishing immutable local pieces, so it makes no zero-copy mapped-String claim.

## P3: coordinated java.lang.String / HotSpot representation

OpenJDK String currently holds VM-trusted `@Stable byte[] value` and a coder. `java_lang_String::value/length` casts the backing to typeArrayOop and derives length from the flat array. C1/C2 and architecture string intrinsics operate on flat byte addresses. Joined storage therefore requires one coherent representation/layout and intrinsic design, GC traversal and dedup rules, StringTable/intern semantics, CDS handling, and JNI/JVMTI contiguous views with exact lifetimes. Merely copying a String.class into another JDK is not an integration strategy.

Preserve public code-unit semantics, malformed UTF16, bounds/errors, hash/equals/compare, regex/charset behavior, identity/monitor behavior and serialization. Define flattening, allocation/OOM, atomic publication and lifetime rules before implementing a new discriminator or fields. Build fastdebug/release baseline and modified images, validate compiler/intrinsic and GC/native paths, then run relevant jtreg and tier suites. A separate module's successful smoke tests are not evidence that this VM work is complete.

## Constructor-pool decision

Retire both experimental constructor pools from the product path. V1 can poison lazy initialization and stalls on a discoverable class monitor. V2 moved activation off constructors, published default-null volatile state and used one-attempt atomic slots; its focused fault checks improved, but constructor copying/hashing remained and measured warm construction was slower. Stock G1 deduplication achieved nearly the same post-GC duplicate-backing retention on the synthetic test. No custom String pool is applied in this repository branch. Preserve the evidence, prefer stock VM dedup as an optional measured baseline, and investigate allocation avoidance at the explicit local-owner layer.
