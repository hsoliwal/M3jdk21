# Whole-JDK feature and acceptance matrix

**INCOMPLETE.** Passing focused view tests is not compiler or modified-JDK acceptance. The actual stock Java 21 String public surface captured by `javap -public -s java.lang.String` is in `../evidence/local-20261002-final/stock-string-surface.log`; it includes constructor and method descriptors beyond this grouped matrix. Capturing the API does not test it.

## Whole-JDK universal acceptance rules

A backend replacement is accepted only for the named API surface, route, build mode, platform and exact candidate. Evidence does not automatically transfer across branches, merged ancestry, representation changes or routes.

Every applicable replacement requires:

1. complete scoped inventory and dependency/reverse-consumer closure;
2. API, binary, serialization, format/native and identity contract review;
3. differential normal/boundary/malformed/overflow/allocation-failure tests;
4. lifetime/GC/owner-close/eviction and leak/retention checks;
5. concurrency/JMM stress where shared mutable state exists;
6. recipe/lowering drift refusal, idempotence, partial-state and rollback tests where transformation is used;
7. clean exact-candidate build/image plus applicable runtime/platform modes;
8. performance/memory results with cold admission, precomputation, conversion and cleanup charged;
9. provenance/license review;
10. mapping/evidence updates bound to the same candidate.

A benchmark gain cannot waive a semantic failure. A semantic pass without measurement cannot support a speed/memory claim.

## Atom/pattern/mapping promotion gate

Before any capability can be promoted, its selected sealed contract boundary must satisfy all three coverage gates:

1. **Atom coverage** — every required semantic atom has an owner, exact source/target location, dependency disposition and test obligation. Unresolved cycles are represented as compound atoms/SCCs rather than omitted.
2. **Pattern coverage** — every repeated transformation is bound to an accepted pattern/version with semantic preconditions, refusal cases, deterministic recipe/patch identity and evidence template, or is explicitly justified as a one-off.
3. **Mapping coverage** — every required source atom has an explicit many-to-many path through pattern/adaptation to target atom(s), target symbol(s) and exact candidate evidence under the existing capability mapping ID.

These are separate from runtime acceptance. Closing atom/pattern/mapping coverage proves traceability and completeness of the selected migration boundary; it does not prove semantic compatibility until the applicable differential, JMM, VM/native, serialization, build and performance gates pass.

Independent atoms may be analyzed and tested in parallel. Promotion is serial: one unmapped required atom, unresolved pattern conflict, stale recipe precondition or failed target-atom gate blocks the containing capability.

## Subsystem gate matrix

| Subsystem | Additional mandatory gates before promotion |
| --- | --- |
| String/text | exhaustive UTF-16/content/hash/comparison/range/Unicode/encoding behavior; StringTable/intern; GC dedup; JNI/JVMTI; CDS; interpreter/JIT/intrinsics; materialization and shared-generation lifetime |
| Collections | per-family null/duplicates/equality/order; live views and Map.Entry mutation; iterator/spliterator/stream traits; serialization/clone/subclass/reflection; boxing boundaries; adversarial collisions; retained memory |
| Concurrent collections/atomics | explicit happens-before/publication and linearization arguments; progress/fairness where promised; contention/resize/callback/cancellation/timeout stress; ABA/reclamation/GC proof |
| I/O/NIO/native buffers | close/error/interruption paths; contiguous ABI boundaries; mapped-file corruption/truncation/replacement; direct/native lifetime; platform tests |
| Reflection/class loading/modules | loader/module identity; access checks; initialization order; hidden/dynamic classes; MH/VH descriptors; service loading |
| Compiler/lowering | compile original and transformed; evaluation/side-effect/exception/overload/identity/ABI parity; strict refusal on unresolved unsafe cases |
| HotSpot/GC/JIT | all affected VM readers/writers; selected GCs; interpreter/C1/C2; intrinsics on/off; deopt; serviceability; OOME and reference processing |
| Security/crypto | provider compatibility, official vectors, key-material lifetime, native provider and security/side-channel review |
| Distribution | clean build, module image/package, supported platform matrix, flag-off/rollback and exact release provenance |

The detailed collection gates are in [../../docs/whole-jdk-collections-replacement.md](../../docs/whole-jdk-collections-replacement.md) and dependency staging is in [../../docs/whole-jdk-work-packets.md](../../docs/whole-jdk-work-packets.md).

## Failed-gate promotion example

Suppose a compact HashMap candidate reduces retained bytes and passes lookup microbenchmarks but its entrySet view fails to reflect setValue into the backing map, or a collision stress case loses a distinct key. Promotion stops. The performance result remains useful evidence, but the candidate is not a compatible HashMap backend until the semantic failure is corrected and the exact new candidate is retested.

Likewise, a String candidate with lower allocation cannot reinterpret a historical StringJoiner OOME expectation failure as success. The failed oracle remains a failed gate until the specification and implementation are reconciled through an independently reviewed change.

## Retained String/MIndex candidate evidence

| Capability / gate | This candidate | Evidence / remaining work |
|---|---|---|
| Exact prefix facts, border, long similarity sum | Locally tested | 77,735 cases and 1,564,255 checks in each of normal and interpreter modes |
| Prefix metadata budget, long overflow arithmetic, cancellation, concurrent analyses | Locally tested | Existing real five-class view subset; primitive-payload budget only |
| Selected regex flags, captures, backrefs, lookbehind, boundaries, replacements, split | Locally tested, focused | 68,106 combined regex/code-point checks per mode; not exhaustive engine conformance |
| chars/codePoints at selected UTF-16 seams | Locally tested, focused | Includes supplementary and unpaired-surrogate fixtures |
| Constructors/fromString/byte decoding/all charsets | Pending | No complete admission or decoding adapter supplied |
| length/isEmpty/charAt/getChars | Partial evidence | Existing views exercised; full String overload/exception matrix open |
| codePointAt/codePointBefore/count/offsetByCodePoints | Pending | Full surface and error contracts open |
| concat/+/invokedynamic/repeat | Pending | Selected descriptor joins used in tests; no complete String lowering |
| substring/subSequence | Partial evidence | Existing view ranges exercised; cross-source surrogate-policy conflict unresolved |
| equals/contentEquals/compareTo/case-insensitive/regionMatches | Pending | No replacement of equals by identity authorized |
| hashCode/intern/StringTable/concurrency | Pending | Prefix similarity is neither Java hash nor interning |
| indexOf/lastIndexOf/startsWith/endsWith/contains | Pending | Z facts do not complete String search contracts |
| replace/matches/split/splitWithDelimiters | Partial evidence only | Selected Pattern operations tested on views; String overloads not comprehensively tested |
| case conversion/trim/strip/lines/indent/stripIndent/translateEscapes | Pending | Locale, Unicode and line-boundary matrices open |
| getBytes/toCharArray/valueOf/copyValueOf | Pending | Independent mutable outputs and encoding boundaries remain open |
| format/formatted/join/transform/describeConstable/resolveConstantDesc | Pending | Captured in the API inventory, not ported or accepted |
| Canonical atom/composition reuse | Pending | No duplicate payload introduced by prefix analysis; no end-to-end canonical interner proof |
| OS-shared lexicon and VM-local unknown words | Pending | Publication, generations, corruption, truncation and cross-process tests open |
| Cache pressure/owner closure/GC/tiny-slice retention | Pending | No retained-memory or lifetime benchmark performed |
| Route B compiler safety and mixed callers | Pending | No lowering implementation or negative compiler suite supplied |
| Route C full matched JDK image | Unexecuted locally | No complete source checkout/build in this tranche |
| Enabled StringJoiner compatibility | Historical failures remain | PR #6 reported two OOME-expectation failures; not resolved here |
| JIT/intrinsics/GC/JNI/JVMTI/CDS/dedup/serviceability | Unaccepted | Interpreter success cannot substitute for these gates |
| Exact-file recipe | Locally tested | 45 checks: replay, drift, late preflight, symlinks, output preservation, ownership rollback |
| Naming schema/validator/reconciliation | Locally tested | 26 tests; full semantic/source inventory closure still open |
| Maven descriptor/lifecycle | Implemented, unexecuted | Maven unavailable locally; direct Java recipe execution tested |
| Hosted CI / exact PR-head checkout | Not established by local receipts | Read actual GitHub checks; existing gates are not disabled |
| Full existing foundation recipe / JPMS build | Unexecuted here | The five dependency files were verified, not the entire foundation build |
| CPU/native/GPU acceleration and benchmarks | Not implemented/measured here | No throughput, latency, retained-memory or amortization claim |
| Independent audit and exhaustive donor review | Pending | Self-review found and fixed preflight defects; no independent reviewer result |

## Performance reporting

Only algorithmic bounds and primitive fact-lane accounting are stated: O(n) comparisons, O(n log(s+1)) worst-case reads through the existing segmented view, and 4*n bytes of int-lane payload. Object headers, descriptors, backing owners and process memory are outside that budget. There are no benchmark speedups, GC savings or whole-workload memory figures to report. Cold/warm, forks/warmup, segment distributions, precomputation amortization and regressions remain unmeasured.

## CI integration not yet completed

The tools expose validation, reports, three-way review output and a completion-refusal command. A reviewed workflow must still run complete authorized source inventories, resolve symbols and dependency closure, check drift and unmapped additions, pin the actual PR head rather than a synthetic merge, and retain failures. `reconcile` emits decisions and does not currently turn every review item into a failing CI exit; a workflow must enforce those decisions explicitly. No skipped gate is a pass.
