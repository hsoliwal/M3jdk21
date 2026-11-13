# Lazy shard validated-text reuse

Reuse the exact mapping lexeme already checked against the immutable image during the facts pass, retaining complete mapping validation before facts. No new retained field/cache, public API, source identity, sense/profile association or storage representation is introduced.

RECIPE.json binds the existing Maven/OpenRewrite recipe, current host pins, exact before/after source identities and canonical recipe crate at Synexia commit eed15acb805f5e421436479374d41f49b37a1041 ([PR #17309](https://github.com/hsoliwal/com.synexia/pull/17309)). RECEIPT.json retains the executed qualification, all source identities, logs, raw samples and explicit scope. The two JUnit source files here are byte-identical to the executed fixtures. The tests are part of the Maven/RAM qualification, not the core standalone main-based test runner. Two existing filesystem test sources were compiled unchanged but not run.

The fixture executes actual dependency implementations using a RAM-backed direct MappedByteBuffer subtype and private admitted-state constructor/validator; it does not execute native filesystem/OS mapping or complete openLazy first touch. Its eight product JUnit cases execute in four fresh before/after/after/before JVMs; current-host recipe replay adds two tests, for 34 qualifying JUnit executions. Every UTF-16 unit, multiple shards/senses, source ID custody, supplied payload separation, mapping-before-facts errors, repair, queries, close/lifetime, concurrency and retained-field identities are covered.

Warmed 1,024-record validation allocation median is 278,528 before and 155,648 after bytes (44% lower). Timing medians improved with wide sample ranges under host load; no latency or cold-startup guarantee is inferred. Source-derived materializations within this validator are n rather than 2n; image checksum/open work is unchanged.

**Published image files must remain immutable.** Injected external mutation between the two passes was rejected by the old second decode and can pass with the verified-text reuse. The separate mutationWindow diagnostic records both outcomes explicitly outside that existing immutable-file contract. Mutation before mapping validation and unchanged copyRecord corruption detection remain covered. This is not OS sealing or equivalence under arbitrary external mutation.

No strict LangDex raw/precompute bridge, real dataset admission, full JDK/JNI/platform proof or complete filesystem lifecycle proof is claimed by this slice.
