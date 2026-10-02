# String and text operations within the whole-JDK programme

Status: **proposed operation-level specification; not an implemented String replacement or a new storage owner**.
This is an extension of the existing [shared-atom contract](shared-atom-concatenation.md), [worked text trace](migration-worked-text-trace.md), [MIndex/MatIndex owner handoff](mindex-migration-handoff.md), and [acceptance matrix](migration-acceptance-matrix.md). Their complete requirements remain in force. Read them rather than using this document as a shortened substitute.

## 1. What stays invariant

A text value is a composition of canonical immutable atom references, IDs and ranges. Already-admitted shared-lexicon payload stays in its existing immutable ownership domain; genuine misses use the existing VM-local canonical overlay. Joins and slices do not create an independently retained whole-text spelling store. Operation bridges use the same atom/range/composition substrate and fact owners, not an automatic `toString()` followed by an ordinary JDK operation.

Ingress from mutable or unowned storage and export to a required stock representation remain explicit compatibility boundaries. A general public `char[]` input cannot become immutable merely because the caller promises not to mutate it. A live composition retains its backing independently of optional cache/interner entries. Shared mapping generations must not be modified or truncated while readers can use them.

Keep text equality, atom identity, composition identity and Java object identity separate. Stock `String.equals` does not become an arbitrary CharSequence comparison because a view has equal characters. A distinct String construction must not be silently replaced with a canonical Java object when that changes observable identity. `String.intern`, literals and constant expressions have their own language/runtime obligations; canonical payload alone does not implement them.

Preserve exact UTF-16 code units, including NUL, unpaired surrogates and legal cuts through pairs. Code-point operations reconstruct pair boundaries correctly. Never insert spaces, normalize Unicode, infer text from arbitrary binary bytes, or equate namespace-local IDs solely by their integer value.

## 2. Operation-level mapping, not a conversion adapter

WP-00 must inventory every Java 21 String constructor, method, static operation and affected consumer at the selected source pin, including overloads, native/runtime paths and generated call sites. The following categories organize that complete method census; they are not a claim to enumerate every declaration.

| Operation family | Proposed atom/range execution | Boundary and evidence required |
|---|---|---|
| Construction and admission | Lookup existing canonical atoms; admit misses with immutable ownership and exact decoder policy. | Caller mutation, offset/range validation, charset/null/exception behavior, Java object identity and bootstrap fallback. Admission cost is included in measurement. |
| length/isEmpty/charAt | Use checked logical UTF-16 length and owner-relative range addressing. | Code-unit indexing and bounds; directory/tree lookup cost is explicit. |
| codePointAt/codePointBefore/codePointCount/offsetByCodePoints | Use range cursors and seam facts while preserving code-unit coordinates. | Split pairs, unpaired surrogates, reverse traversal, bounds and overload behavior. |
| concat, language concatenation, join, repeat | Retain child owners; normalize compatible adjacent ranges; use bounded balanced/chunked descriptors. | Total-length overflow, null/object conversion, evaluation order, identity, empty cases, large counts and allocation failures. No unconditional O(1) claim. |
| substring/subSequence/ranges | Retain adjusted ranges and bounded metadata, not a whole-text copy. | UTF-16 indices, empty/full ranges, nested slices and retained-large-backing effects. A stricter scalar-only donor needs an adapter, not silent adoption. |
| equals/contentEquals/compareTo/compareToIgnoreCase | Apply the exact selected operation to compatible owners, with identity/length/hash fast rejection and exact character confirmation. | Preserve argument types, null handling, String-only equality where required, Unicode rules and segmentation independence. |
| startsWith/endsWith/regionMatches/indexOf/lastIndexOf/contains | Reuse owner/pattern facts and sequential range cursors; avoid restarting a tree search at each character where a cursor suffices. | Offsets, empty patterns, reverse search, seams, code units versus code points and exact result indices. |
| hashCode and derived summaries | Reuse full-atom and composition facts; compute uncached partial-range facts explicitly. | Equal UTF-16 text has equal Java hash across different segmentations. Hash collision never confirms equality. |
| replace/split/matches and regex Matcher consumers | Reuse exact pattern plans and context-keyed facts through the selected Java-compatible engine. | Groups, captures, region/anchors, backreferences, lookaround, flags, replacement escaping, empty matches, split limits and boundary semantics. |
| case conversion, normalization-adjacent utilities, trim/strip/isBlank/lines | Scan or reuse facts only for the exact operation, locale and Unicode policy. Result text re-enters the existing ownership substrate where supported. | Expansions, context-sensitive case behavior, locale, whitespace distinctions, line endings and exact Java 21 behavior. No implicit normalization. |
| getBytes/constructors from bytes and charset consumers | Encode/decode with seam and state awareness; reusable byte facts include the complete charset/error-policy identity. | Standard versus modified UTF-8, malformed input, stateful charsets, byte order and explicit output ownership. |
| getChars/toCharArray/other array exports | Traverse directly into the required independent writable destination. | Bounds, overlap where applicable, runtime array type and independent mutable storage. Export copying is not a second canonical store. |
| formatting, formatted, valueOf/copyValueOf and related conversions | Reuse compatible atoms/facts after preserving conversion/formatting semantics. | Locale, callbacks, exceptions, object-toString effects, primitive formatting and identity; do not treat them as plain concatenation. |
| streams, chars/codePoints and spliterators | Emit through the same range cursor and exact encounter order. | Code units versus code points, characteristics, splitting, laziness and supported traversal cost. |
| StringBuilder/StringBuffer and mutable CharSequence consumers | Preserve mutation ownership; freeze/copy only at defined admission boundaries or use proven copy-on-write ownership. | Later builder mutation must not change an immutable String. StringBuffer synchronization and user CharSequence effects remain distinct. |

The mapping for an operation records the real source/target symbols, parameter/return contracts, canonical owner, fact dependencies, materialization boundary, route and acceptance evidence. A generated API bridge can expose a broad surface without having an optimized implementation for each operation; unresolved or materializing paths stay visible rather than being called complete.

## 3. Reusable facts and their limits

Facts are organized by their real owner: immutable atom, logical range, composition, pattern, or pattern/input context. Their keys include all inputs that affect validity: namespace/generation, range geometry, flags, locale/charset/Unicode policy, decoder mode and any mutable-owner epoch. A different split of equal text can share value-derived facts only after exact compatible identity has been established.

The existing contract gives the Java hash composition law: `h(A+B) = h(A) * 31^length(B) + h(B)`, under Java 32-bit wraparound and UTF-16 length. This can reuse committed atom/composition facts. It does not make arbitrary range hashes free, and it does not provide a composition rule for SHA-256. Prefix or block indexes trade admission time and memory for later range work and require explicit bounds and amortization.

Presence masks, fingerprints, length bounds and native candidate filters may eliminate impossible candidates when sound. They do not admit a match without the exact operation's verifier. Dictionary ID order is not automatically Java UTF-16 lexicographic order, especially for mixed dictionaries, local misses and compositions. A primitive numeric sort cannot replace String comparison solely because strings have IDs.

Avoid a whole-result cache as a substitute for an operation bridge. Cache admission, byte budgets, eviction, concurrency, recursion/reentrancy and failure behavior belong to the existing fact owner. Live backing cannot depend on cache residency. A cache miss or budget refusal selects correct slower work, not a different answer. Class-loader, user and secret boundaries must not leak through global caching.

## 4. Descriptor and retention discipline

Two-child nodes can avoid immediate payload copying, but repeated joins need a bounded-depth policy. A flat segment directory can make traversal cheap while imposing O(segment count) construction work. A balanced tree can bound depth while requiring balancing and aggregate metadata. Choose thresholds through measurement, not a blanket claim that all concatenations are constant time.

Normalize empty and adjacent compatible ranges without coalescing unrelated owners or changing identity domains. Use checked logical length and metadata-size arithmetic. Bound segment count, depth, cached indexes and optional facts. If a representation limit is reached, take an explicitly specified fallback or fail according to the admitted API contract; do not silently keep an unbounded descriptor chain.

A tiny slice can keep a large atom or mapping alive. Report this retention honestly. A separately named compaction/materialization operation may trade sharing for smaller retained backing, but a hidden second whole-spelling store violates the canonical ownership goal. Exported stock Strings and mutable arrays have their own ownership and are counted as boundary allocations.

## 5. Worked text and seam examples

These are illustrations, not executed tests or newly implemented APIs.

### Shared lexicon plus VM-local miss

Let a shared immutable atom contain `"Who "` and an admitted VM-local atom contain `"am I"`. A join retains their two ranges and has eight UTF-16 code units. Slicing positions `[4,6)` refers to the local atom's `"am"` range; no combined eight-character payload is required. A small descriptor may still be allocated, and each live result keeps its atom owners alive. If a stock consumer requires `toCharArray`, create the independent eight-character export directly from the ranges.

The example describes compatible owner/range semantics. It does not claim the current mapped image accepts every possible atom encoding or that a particular production method already implements this path. Shared image formats, scalar-record restrictions and the existing local interner implementation must be verified separately.

### Surrogate pair across a range boundary

A logical range can end with the high surrogate of a supplementary character while the next begins with its low surrogate. `charAt` still addresses the individual code units; the relevant code-point operation must see the combined pair. Encoding each range independently with replacement and concatenating the bytes can produce a different result from encoding the logical text. A seam-aware encoder must carry the needed state. If a mapped format admits only complete scalars, that restriction does not justify changing legal String substring semantics.

### Different segmentation, equal text

One composition may be `"ab" + "c"` and another `"a" + "bc"`. Their atom and composition identities may differ; their exact UTF-16 text, Java hash and appropriate text comparison results must agree. Pointer equality is a permitted positive fast path only in a compatible domain, never the only equality implementation.

### Compiler effects and fallback

For a concatenation involving `left()` and `right()`, preserve evaluation and conversion side effects and exceptions in their original order. If either call can escape a representation, observe identity or use an unknown mutable CharSequence, refuse the unsafe rewrite. Keeping the original operation is a valid safe compiler outcome. Passing a test for constant literal folding does not admit dynamic object concatenation.

## 6. Native and runtime boundaries

The complete JNI rules remain in the shared-atom contract. In particular, JNI String UTF functions use modified UTF-8; array/string access can require copies; a global reference is not permanent address pinning; direct buffers describe contiguous regions and need real lifetime ownership. Release every successful acquisition correctly, validate JNI/native lengths and error returns, and obey critical-region restrictions. Native consumers must explicitly accept a scatter/gather representation before a segmented view can cross that ABI without copying. See the [Java 21 JNI specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).

Route A retains explicit stock conversion boundaries. Route B requires effect/type/escape proof and refuses unsupported cases. Route C retains actual ordinary String objects while coordinating backing/layout consumers throughout the matched VM, compiler, GC, JNI/JVMTI, image/CDS and diagnostic tooling. A CharSequence facade is not evidence of that integration. Do not transplant String.class or install an unaccepted runtime.

The existing contract documents early/bootstrap arrays, snapshot mapping assumptions, materializing large paths and historical interpreter-only runtime evidence. Its dated receipt section is not retroactively updated by this extension. New #23/#24 candidates must attach exact candidate/tree/configuration evidence, and unresolved tests remain unresolved. A successful explicit operation bridge does not certify custom-JDK layout or compiled execution.

## 7. Text acceptance and performance matrix

For every admitted operation, compare exact results, exception classes and required effect/identity behavior against the selected Java 21 oracle. Cover empty/single-unit/large text; shared/local/mixed ownership; many short segments; deep construction histories; equal text with different segmentation; NUL, surrogate seams and malformed input; mixed charsets/locales; regex region/capture edge cases; cache admission/eviction; mapping close/republication; GC and concurrent admission. Add constructor, serialization, compiler and native consumers, not only leaf character operations.

Measure cold dictionary lookup/admission, new composition and fact creation, warm repeated operation, cache pressure, tiny-slice retention, native/mapped memory, page faults and explicit export costs separately. Include descriptor and interner/fact-table overhead. Report speed and allocation separately; neither no-copy geometry nor an operation-count argument is a measured speedup. Record regressions and keep fallback decisions per operation.

The whole String surface remains incomplete until the actual constructor/method/native-consumer inventory, route-specific mapping and exact-build gates close. This document preserves that work as part of the whole JDK rather than calling an isolated joined-text path the completed programme.
