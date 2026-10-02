# MIndex → M3 acceptance matrix

Status labels: **PASS** = executed on the named candidate; **HISTORICAL PASS** = evidence from a pinned earlier candidate; **OPEN** = mandatory gate not yet executed or not yet passing; **N/A** = not applicable to that route.

| Gate | Route A explicit M3Text | Route B lowering | Route C modified JDK |
| --- | --- | --- | --- |
| Exact UTF-16 value/char access | PASS — local 48-check route suite | OPEN — full transformed corpus | HISTORICAL PASS on `3776d6e...` focused/API suites |
| Arbitrary UTF-16 substring incl. surrogate split | PASS — exhaustive ranges in focused fixture | OPEN — transformed source differential | HISTORICAL PASS on selected String tests |
| Concat preserves value/hash across seam | PASS | OPEN | HISTORICAL PASS |
| Reference-only concat/slice payload reuse | PASS for M3 piece payload; descriptor metadata allocates | OPEN | HISTORICAL PASS for bounded candidate paths |
| Mutable output isolation | PASS | ordinary Java boundary required | HISTORICAL PASS selected paths |
| Stock java.util.regex compatibility | PASS for tested Pattern seam case through CharSequence | OPEN transformed boundary | HISTORICAL PASS selected Regex.jtr; not full regex conformance |
| Evaluation order / side effects | N/A | PASS only for helper-order fixture; general lowering OPEN | JDK concat tests HISTORICAL PASS |
| Null/exception/overload/constants | N/A | OPEN | historical selected JDK tests only |
| Recipe drift refusal/idempotence | additive Git/source control only | OpenRewrite crate present; Maven execution OPEN | HISTORICAL PASS sealed runtime patch |
| Native/JNI ownership | N/A | N/A unless native backend selected | HISTORICAL PASS checked JNI/JVMTI; full surface OPEN |
| GC / concurrent access | weak-interner design; stress OPEN | inherits target | HISTORICAL PASS focused lanes; full runtime OPEN |
| StringTable / intern | distinct by design | must preserve boundary | HISTORICAL PASS selected tests |
| CDS | N/A | N/A | OPEN/fail-closed candidate behavior |
| JIT/C1/C2 segmented execution | stock JVM normal JIT for M3Text; no VM specialization | OPEN lowering benchmarks | OPEN — enabled candidate interpreter-only |
| Complete JDK image | N/A | N/A | HISTORICAL PASS for `3776d6e...`; must rebuild after migration deltas |
| Selected flag-off upstream tests | N/A | N/A | HISTORICAL PASS 86/86 |
| Enabled selected upstream tests | N/A | N/A | OPEN — 16 pass, 2 StringJoiner files fail |
| Full jtreg/JCK | N/A | N/A | OPEN |
| Live SA attach | N/A | N/A | OPEN |
| Cross-process same backing | N/A current local Route A | N/A | HISTORICAL PASS pinned candidate |
| Performance/retained-memory benchmark | OPEN | OPEN | HISTORICAL allocation result only; no throughput claim |
| Exhaustive source-family migration inventory | OPEN — current manifest is seeded | OPEN | OPEN |

## Promotion rule

A route may be described as implemented only for the surface actually present. “Accepted” requires every mandatory gate for that route to pass on the exact candidate revision. Historical receipts are never silently transferred to a newer tree.

The two enabled StringJoiner OOME expectation files remain failures until resolved by a justified compatibility decision with exact-head evidence. Avoid weakening or rewriting upstream expectations merely to obtain green status.
