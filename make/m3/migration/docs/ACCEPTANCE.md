# Feature and acceptance matrix

**INCOMPLETE.** Passing focused view tests is not compiler or modified-JDK acceptance. The actual stock Java 21 String public surface captured by `javap -public -s java.lang.String` is in `../evidence/local-20261002-final/stock-string-surface.log`; it includes constructor and method descriptors beyond this grouped matrix. Capturing the API does not test it.

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
