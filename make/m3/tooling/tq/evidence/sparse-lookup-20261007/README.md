# Sparse prepared TQ lookup receiving candidate

Canonical recipe and reproducible evidence:
[Synexia PR #9726](https://github.com/hsoliwal/com.synexia/pull/9726), commit
`f1e9ff1f2426b20d55f82e50033c0aa8ed66bf72`, packet
`synexia-openrewrite-recipes/recipes/m3jdk21-tq-sparse-lookup`.
This target receives byte-exact OpenRewrite-generated runtime source and its
semantic test. Canonical templates, fixtures, probe tooling and raw timing logs
remain in the existing Synexia recipe module.

`M3TQ.Facts.containsAll` now estimates sparse binary-search cost against source
cardinality and retains merge for dense requirements. Singleton first-key,
identity, empty and impossible-cardinality cases have bounded fast paths. No
representation, retained field, source preparation, String dispatch or public
API changes. This guard proves set inclusion only; exact matching remains
required, including when all requested trigrams exist at different positions.

`receipt.json` binds parent, canonical recipe commit, pre/post hashes and evidence.
The sparse work probe falls from 32,766 source-key reads to 16. This is a
test-only instrumented copy. Exact uninstrumented candidate passes 31,011 new
set-oracle/UTF-16/payload-fence checks and 308,649 existing JNI-backed checks in
each host Java21 mixed/interpreter/C1/C2 run. This is a patched `java.base` kernel
proof, not a custom M3 JVM image or `UseM3StringStorage` proof.

Three-fork host smoke timings on synthetic prepared sets are included in the
receipt. Late sparse probes improve substantially; first-key hits retain a small
overhead (median 12.39 ns before, 17.96 ns after). No overall String speed claim
or dense-algorithm speed claim follows from this microbenchmark. Cold preparation
is unchanged and excluded from timing. Retained-memory measurement remains open.

The shared String invariant checker reports the same pre-existing
`HOLD_MATCHER_STATE_GAP` failure before and after the change; both outputs are
retained here. Full custom JDK build/jtreg, end-to-end String/JMH, cross-platform
qualification and receiving snapshot reconciliation remain pending. Existing
historical TQ seals are preserved; do not reinstall old templates or weaken their
hash gates. Keep the runtime PR draft until receiving admission is resolved.

The mapping change appends one refinement record and preserves all 55 prior
records and the rest of the mapping byte-equivalent as JSON values. Its status
remains `implemented-unverified` for complete-image admission.
