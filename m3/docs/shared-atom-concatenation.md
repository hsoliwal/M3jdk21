# MIndexString shared atoms and reference only concatenation

Status: proposed integration contract with an evidence snapshot dated 2026-10-02.
Audience: M3JDK, Synexia, compiler and native-runtime reviewers.
This documentation change does not implement or certify the runtime.

## Decision

Represent text as an immutable composition of canonical atoms and ranges. Concatenation reuses the existing character or byte payload and creates, or reuses, composition metadata. JNI is optional. The same ownership contract must apply to the explicit Java API, compiler-lowered operations and the modified JDK.

The intended invariant is:

> An MIndexString composition refers to immutable backing already owned by a shared lexicon or a VM-local interner. Joining and slicing do not duplicate that payload. Any materialization is an explicit, documented compatibility boundary.

This is a target contract, not a claim that every current String constructor, consumer or concatenation shape satisfies it. The evidence section identifies current exceptions.

## What a reference only join means

Suppose atom A contains "Who " and atom B contains "am I". A joined value retains two ranges, A[0,4) and B[0,4), with a logical UTF-16 length of eight. Character positions below four address A; remaining positions address B after subtracting four. No third eight-character payload is required.

A descriptor logically contains:
- Stable owner references or validated handles
- Atom identity within an explicit namespace and generation
- Start and length for each range
- Checked total UTF-16 length
- Optional prefix lengths and precomputed facts

The descriptor is small metadata, not zero allocation by definition. A canonical interner can reuse an existing live descriptor for the same normalized range sequence. Intern-table lookup, collision checks, metadata and lifetime management still cost time and memory.

A Java implementation can retain ordinary immutable-owned arrays. A JNI implementation can return a Java descriptor object or a validated opaque handle wrapped by such an object. Neither approach makes an ordinary char[] reference denote two disjoint arrays.

Java char[] values remain mutable. Public inputs must be defensively copied on admission unless an enforceable exclusive-ownership transfer makes later mutation impossible. A promise by a caller is not sufficient for a general public immutable String API.

## Storage domains

### Shared lexicon

Store a versioned immutable image in a trusted file and map its payload read-only. Different JVMs can map the same file-backed pages; their virtual addresses, Java references and local identifiers need not match. This is the intended meaning of OS-shared backing. An OS does not automatically provide a dictionary-wide Java array interner.

Store offsets and format identifiers in persistent descriptors, never process virtual addresses. Validate lengths, arithmetic, alignment assumptions, encoding, record commitment and integrity before admitting an atom.

Read-only mapping prevents this reader from writing; it does not stop another process from modifying or truncating the file. Publication must prevent in-place mutation of a mapped generation. Publish new immutable generations and retain old ones until readers release them. Unlink behavior is platform-dependent and must be tested separately.

English may be selected as an eagerly mapped post-bootstrap lexicon. Other languages and subject lexicons may load lazily on first use with single-flight loading and a local fallback. Neither network downloads nor dictionary initialization should occur recursively inside early String bootstrap.

### VM local dictionary

Unknown text must remain representable exactly. Admit it to a VM-local immutable interner; retain the resulting atom in compositions. Shared hits and local misses can coexist in one joined value.

Do not put secrets or arbitrary application text into a cross-user persistent lexicon automatically. Cross-process sharing needs explicit access boundaries, provenance and a publication policy.

Weak entries or bounded lookup indexes may reduce retention, but a live descriptor must keep its backing alive. Eviction can remove optional lookup/cache entries; it must not invalidate an existing String. If an API promises stable IDs, reclaimed slots require generation checks or non-reuse. A weak interner does not promise the same object after all strong references have disappeared.

### Canonical identity

Keep these identities distinct:
1. Text equality over the exact UTF-16 sequence
2. Atom identity within one owner namespace and generation
3. Composition identity over normalized atom ranges
4. Java String object identity and String.intern semantics

Equal text split into different atoms must compare equal and have the same Java hash. Equal text does not imply an identical descriptor unless content-level canonicalization has actually established it. Pointer or ID equality is a positive fast path only inside a compatible identity domain. A hash collision never proves equality.

Application resolver IDs, language coordinates, runtime tuple IDs and native handles are not interchangeable. MatIndex/MIndex to M3 naming consolidation must preserve these distinctions in a mapping manifest.

## Composition operations

### Concatenate

Check logical-length overflow first. Retain child owners, remove empty ranges and coalesce adjacent ranges only when owner, atom and contiguous coordinates match. Publish immutable metadata only after successful construction.

Two-child rope nodes can join in constant metadata work before balancing and canonical lookup. Flattening an existing segment directory costs work proportional to the number of segments. Do not label every join O(1). Use measured thresholds to choose balanced trees, flat directories or chunked directories.

Prevent unbounded depth from repeated appends. An iterative segment cursor should traverse text without performing a tree lookup for every character.

### Slice and mask

A slice adjusts ranges and keeps the source owners alive. Preserve UTF-16 code-unit indexing, including cuts through a surrogate pair where the String API allows them. Code-point-aware operations must reconstruct pairs across range boundaries.

A mask or noncontiguous selection needs an explicit logical-to-source position map. Regex captures and replacement ranges must use a documented coordinate system. A tiny slice may retain a large atom or mapping; expose and benchmark that retention rather than silently violating the no-copy contract to hide it. A separately named compaction operation may deliberately materialize.

### Materialize

Mutable-array outputs such as toCharArray must return independent writable storage. Consumers that require one contiguous pointer or a stock String may require copying. Traverse segments directly into the requested destination when possible, avoiding an intermediate flattened cache.

Materialization caches are optional and budgeted. Cache exhaustion must change performance only, not answers. On a stock JVM, asString necessarily produces an ordinary String representation; removing that copy requires coordinated support in the modified runtime.

## Characters and bytes

UTF-16 code units are the canonical Java text semantics. Preserve NUL, supplementary characters and unpaired surrogates. Do not normalize text implicitly.

A character atom may have cached byte representations, but their identity includes charset, byte order where applicable and malformed/unmappable-input policy. Raw binary bytes are not automatically text.

Encoding separately cached fragments and concatenating their bytes is valid only when encoding state and boundaries make it equivalent to encoding the whole string. A surrogate pair split across atoms, or a stateful charset, requires boundary-aware processing. One shared physical allocation cannot generally serve simultaneously as arbitrary UTF-8 bytes and UTF-16 characters.

## JNI and native boundaries

The [JNI functions specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html) defines primitive-array access and direct-buffer operations.

- GetCharArrayElements may copy. Its pointer must not be retained after the corresponding release
- A global JNI reference preserves reachability; it does not permanently pin an array's address
- NewDirectByteBuffer describes one contiguous address range. It cannot describe two arbitrary disjoint ranges and does not own arbitrary native memory automatically
- A direct-buffer or native-arena descriptor needs a lifetime owner. Closing an arena must not free memory still reachable through a supported live view
- Never perform allocation, blocking work or arbitrary JNI calls inside a critical region contrary to the JNI restrictions
- Check exceptions, null results, length conversion and addition overflow; pair successful acquisitions with releases on every exit path

A native scatter/gather descriptor can contain multiple segments. Native consumers must explicitly understand it. If an existing ABI requires a single contiguous pointer, provide a copy boundary or change the ABI with compatibility support.

Virtual-memory aliasing at page granularity is not a general solution for arbitrary word-sized Java arrays. It introduces alignment, page ownership, address-space, protection and GC issues; it is not part of the accepted design here.

## Precomputation

Organize cached facts by what makes them valid:
- Per atom: UTF-16 length, encoding-specific bytes, Java hash, character presence and classification summaries
- Per composition: checked lengths, prefix indexes, composable hashes and seam facts
- Per pattern: parsed regex structure, automaton and reusable replacement plan
- Per pattern and input context: transition effects, matches and captures with full context in the key

For Java's 32-bit String hash, h(A concatenated with B) = h(A) * 31^length(B) + h(B), with Java integer wraparound. Cache powers and full-atom hashes. Arbitrary slices need additional indexes or a scan. SHA-256 digests do not obey this polynomial composition rule.

Presence masks and length bounds may reject impossible matches only when sound; candidate acceptance still requires exact semantics. Word weight or hash order is not a general regex decision procedure.

Regex transition caching must include relevant flags, region/anchor semantics and boundary state. Captures, backreferences, lookarounds and Unicode boundaries need separate treatment; do not transfer RE2/J guarantees to all java.util.regex features.

Use primitive collections where beneficial, with measured LRU/admission policies and explicit byte budgets. Avoid a global synchronization bottleneck. Test concurrency, cache eviction, allocation failure and reentrancy.

## Three supported integration routes

| Route | Contract | Remaining responsibility |
| --- | --- | --- |
| Explicit MIndexString or M3 view | Standard Java CharSequence over retained atoms | Materialize at incompatible stock APIs |
| Compiler lowering | Rewrite only proven compatible operations to the same owner | Preserve evaluation order, exceptions, identity-sensitive behavior and external boundaries |
| Modified JDK | Ordinary String backed internally by MIndexString | Coordinate interpreter, VM readers, GC, JIT intrinsics, JNI, JVMTI, CDS and serviceability |

These routes share a design; they are not interchangeable proofs. Java String is final and VM-sensitive. A custom implementation requires a matched complete JDK image. Do not replace a class file or an installed runtime in place.

## Mechanical implementation and review pipeline

1. Inventory existing owners, APIs, native ABIs, generated sources and tests at exact revisions
2. Map each operation to its data owner, index, precomputation and materialization boundary
3. Reuse and consolidate existing owners; preserve compatibility adapters where required
4. Express transformations in source-pinned replayable recipes with before/after hashes
5. Test drift refusal, idempotence, partial/mixed state, rollback and output preservation
6. Validate focused behavior, then full applicable build, lint and conformance gates
7. Publish reviewable draft PRs with exact source and image identities

Partial ASTs and source atoms can support local transformations. Unresolved types, unsafe escapes or incomplete semantic information must block transformations requiring that information. Compiler success alone cannot prove all behavioral equivalence.

No recipe may silence a failing gate, remove a test or change an expected exception simply to produce green status. A justified specification change requires a separately reviewed explanation and evidence.

## Acceptance checklist

- [ ] Joining two existing atoms creates no duplicate character payload
- [ ] Existing live canonical geometry reuses its composition body
- [ ] Equal text with different segmentations preserves equals, compare and hash results
- [ ] Empty, nested, repeated, partial and noncontiguous views have defined behavior
- [ ] Surrogate pairs across seams and unpaired surrogates match Java semantics
- [ ] Caller mutation cannot change interned contents
- [ ] Eviction, GC, owner close and concurrent admission cannot invalidate live views
- [ ] Mapped generations reject malformed, truncated and corrupt records safely
- [ ] Two independent JVMs demonstrate shared file backing, with memory-sharing measurements where claimed
- [ ] JNI and contiguous fallbacks preserve contents and release ownership correctly
- [ ] Compiler-lowered paths preserve evaluation, exceptions and API contracts
- [ ] Regex, split, replacement and capture behavior match the selected engine contract
- [ ] Required build, lint, unit, integration and runtime conformance gates pass on the exact candidate
- [ ] Cold admission, steady-state CPU, allocation, retained heap/native memory and cache pressure are reported separately

Benchmarks must name input distribution, segment count, atom reuse, warmup, JVM flags, GC and forks. Report precomputation cost and amortization. A reduction in allocated bytes does not prove faster execution. The requested 25-percent CPU target remains a target until measured.

## Evidence snapshot and open gaps

The documentation branch is based on master at 1342452b40a2dae1614a2c9045935feca5fe84d0. The integrated runtime below is a separate candidate, not asserted to be present on master.

[PR 6](https://github.com/hsoliwal/M3jdk21/pull/6), pinned at 3776d6e674d6c9b04539ca24aca2504aa88d4a57, combines canonical java.lang.MIndexString storage with earlier VM boundary protections. Its [README](https://github.com/hsoliwal/M3jdk21/blob/3776d6e674d6c9b04539ca24aca2504aa88d4a57/m3/runtime-integration/README.md) and [results](https://github.com/hsoliwal/M3jdk21/blob/3776d6e674d6c9b04539ca24aca2504aa88d4a57/m3/runtime-integration/evidence/results.json) report:
- A successful complete fastdebug image build
- 86 passing flag-off tests
- 16 passing and 2 failing enabled upstream tests, including six StringJoiner OOME expectations
- Two child JVMs mapping the same file inode and retaining readable contents after unlink and GC
- Interpreter-only enabled execution; compiled mode, CDS, JFR, JVMCI and dedup remain unsupported

These are inspected published execution receipts, not reruns performed for this documentation PR. Failed OOME expectations remain failed gates even if lower allocation plausibly explains them.

The candidate accepts M3LEX001 UTF-16LE images and language-0 committed UTF-16BE scalar records from SYNARR01. It does not establish general byte-record, tuple-record or multilingual String-atom sharing. Its startup mapping is a snapshot and relies on immutable trusted backing files.

Pre-bootstrap Strings retain their original arrays. Some large join/repeat and early-linked concatenation paths materialize. Weak atom/tuple indexes are not a fixed total-memory budget. Application resolver arenas and asString materialization remain separate integration work.

[Foundation CI](https://github.com/hsoliwal/M3jdk21/actions/runs/36925614422/job/110582084028) passed recipe and foundation stages, then failed downloading reference source with HTTP 404 before compiling the comparison; the subsequent mapping step was skipped. The associated workflow uses a synthetic PR merge revision. [OpenJDK sanity CI](https://github.com/hsoliwal/M3jdk21/actions/runs/36925524534/job/110581964756) separately encountered unavailable pinned compiler packages before candidate compilation. Neither constitutes a full hosted pass.

Changes in other PRs must be reconciled by exact source trees. Their tests or build receipts cannot be borrowed as proof for this candidate.

## Publication and provenance

This document records a technical design and its current evidence. It includes no private reference implementation, conversation transcript, credentials or dataset payload. Preserve existing OpenJDK and donor notices. An Apache-2.0 intent for independent M3 contributions does not relicense upstream OpenJDK code. Dataset redistribution and integration require source-specific license review before publication.
