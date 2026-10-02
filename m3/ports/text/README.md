# Explicit retained M3 text (Route A)

`M3String` in `com.m3.indexstring` composes the established Synexia Frozen/Joined
owners. Their original `com.synexia.indexstring` names remain available. This is
an application module on stock Java 21; it is outside `java.base` and String
bootstrap. The older `m3/core` byte-format prototype remains experimental and is
not this facade's storage owner. Structural atom/AST/DAG owners are not renamed.
The seven physical owners reside only in `m3/ports/indexstring/src/main/java`.
The incoming `com.m3.text.compat.M3Text` facade uses that same closure; no duplicate
owner classes are required on the classpath.

```java
M3StringArena arena = new M3StringArena(4096, 8 * 1024 * 1024);
M3String text = arena.fromString("hello").concat(arena.fromString(" world"));
M3String range = text.substring(1, 8);
String ordinary = range.toString(); // explicit materialization boundary
```

`M3_JDK=<stock-jdk-21> python m3/ports/text/test.py` compiles with all lint checks
and warnings as errors, then runs differential tests in JIT, interpreter,
noncompact and C2-focused modes. Each recorded invocation is in ignored
`build/receipt.json`. `M3StringTest` includes a real FNV hash collision, independent
atom identity and composition-reuse tests, arbitrary UTF16 slices, cross-segment
surrogates, cache clearing, concurrent admission, regex and stateful encoding.

String admission probes exact raw UTF16BE bytes, including NUL and unpaired
surrogates, before allocating on a hit. Mutable arrays are copied. The existing
bounded interner stops retaining new entries when either limit is reached;
each immutable byte atom lazily retains one UTF16 projection and its hash fact.
This owner-local view adds descriptors without duplicating payload and dies with
the atom; it is independent of the optional lookup map.
`clear()` drops lookup retention while live handles own their payloads. No text
is persisted or published. Reported cache bytes exclude map/key/descriptor headers
and live uncached values. This release has no OS-shared lexicon admission.

Composition reuses immutable payload and the existing weak canonical segment
body when live. Empty segments are removed; only adjacent ranges within the same
actual backing coalesce. The facade adopts the canonical body's owner projection
so retention accounting describes its actual payloads. Composition identity is
process-local and segmentation-sensitive; it is neither a resolver token nor a
portable dictionary ID. Text equality and Java hashes agree across segmentations.
`equals` compares M3String values only; `contentEquals` compares other text without
breaking String's symmetric equality contract. `compact()` deliberately bypasses
canonical body substitution to release large owners held by tiny slices.

For n code units and s segments, character lookup costs O(log s); full cursor
output/comparison/streams cost O(n+s). Joining costs O(s1+s2) descriptor allocation
plus coalescing/hash work and canonical lookup/comparison work. Repeated nonempty
appends can copy growing directories; this is not an O(1) rope. Slicing retains
owners, allocates descriptors and scans partial ranges for hashes. Literal KMP
search costs O(n+m+s), with O(m) per-call pattern metadata and cursor descriptors.
Retention accounting deliberately deduplicates owner references in O(s²).
The weak composition registry cleans stale metadata on subsequent admission and
is not a total process-memory budget.

`Pattern.matcher(text)` uses the stock engine, including regions, captures,
backreferences and lookarounds. Charset encoding uses one encoder over the whole
logical input, with JDK replacement semantics; independent fragments are never
encoded separately. Those JDK consumers use `charAt` and may pay O(log s) per unit.
Mutable outputs are independent; I/O traverses segment cursors. No alternate
regex engine, GPU acceleration, JNI, compiler correctness, mapped lifetime,
transparent String integration or speedup is admitted by these tests.

Current provenance, original licenses, exact donor hashes and target adaptations
are in `provenance-consolidated.json`, `LICENSE`, `THIRD_PARTY_NOTICES.md` and the
existing M3 naming map. `provenance.json` and `donor-adaptations.patch` preserve the
historical f07 closure; current replay uses `donor-consolidation.patch`.
`python m3/recipes/text-port.py --source-root <private-source> --replay-to
<empty-or-identical-target>` replays only the declared donor closure after hash
and drift checks. General enhancement replay and rollback use the existing
`m3/recipes/apply.py` with an explicit synchronized baseline.
The v2 installer and `m3/ports/indexstring/consolidation/recipe.json` also preserve
both public facade contracts. Fresh shared-owner receipts and selected public
descriptor/original-bytecode evidence are in `m3/evidence/`. Their code binding is
`shared-owner-code-binding.json`; full-family compatibility remains a separate gate.

The generated String surface/acceptance matrix and selected migration coverage
are under `m3/docs/`. Diagnostic three-fork results are in
`m3/evidence/retained-text-benchmark.json`, bound to the historical f07 source:
that long two-atom join uses about
160 allocated bytes per operation versus 16,424 for stock concatenation, while
tiny retained slices allocate more metadata and take longer than stock slices.
These local measurements do not establish a general throughput improvement.

Fresh raw-Git source/class-bound ef33 measurements are recorded in
`m3/evidence/shared-owner-benchmark-20261002.json`. Three forks show about 160
allocated bytes per long retained join versus 16,424 for stock concat, while tiny
retained slices allocate 248 versus 96 and take longer. A one-unit retained slice
holds 8,192 owner payload bytes; explicit compaction leaves two. These preserve
raw diagnostic outputs and do not establish full heap retention or general speedup.
