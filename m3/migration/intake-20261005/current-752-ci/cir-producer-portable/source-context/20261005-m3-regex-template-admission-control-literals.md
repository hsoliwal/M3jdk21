# Cir scope correction before execution: both generated Java owners

This additive record preserves the earlier three-path admission. No Cir gate has executed. Independent review of all four A3 templates found that `A3MemoryCompiler.java.after` and its canonical source are byte-identical (6,442 bytes, SHA-256 `9171b0ada1d7845b63d7a586d99ef97a6fa6eccbfd84204c47a6a01c8b3e4697`) and contain actual control bytes inside character literals. Two contain LF, one CR, one NUL and one TAB. The physical LF and CR are compile-invalid. The NUL and TAB are represented with explicit escapes as well, preserving their actual codepoints and making the generated source inspectable. No algorithm changes.

The final Cir target set has five paths: canonical and template `A3RegexMatrix`, canonical and template `A3MemoryCompiler`, and their existing shared manifest. The two added paths are:

- `m3/tooling/a3/src/main/java/com/m3/a3/A3MemoryCompiler.java`.
- `m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/A3MemoryCompiler.java.after`.

In each MemoryCompiler file, five exact character-literal replacements turn the actual control byte into its two-byte Java escape: LF to `\n` twice, CR to `\r`, NUL to `\0`, and TAB to `\t` once each. The result grows exactly five bytes. The Java source quote positions in the original are byte offsets 2765 (LF), 3339 (NUL), 3384 (LF), 3429 (CR), and 3474 (TAB). RegexMatrix still inserts only its two backslashes. The shared manifest changes exactly the two affected postimage SHA-256 cells; all target paths, preimage pins (including ABSENT), other outputs and resource names remain unchanged.

Qualification expands to all 32 before/after combinations, 22 initial refusal cases and ten post-scan refusal cases. It retains real parser roundtrips and real compiler diagnostics for the original malformed sources. The existing unchanged nested A3 recipe must produce all four declared sources from the actual repaired resources; those four sources and exact current closure must compile with Java 21. Runtime checks execute the retained matrix-determinism, deliberate compiler-failure and root-output-guard test bodies, verify the intended quoted pattern subject, and verify the explicit character literals preserve rejected NUL/newline/carriage-return/tab behavior. Full A3 and migration-recipes receiver suites remain separate required gates.

The successful prior five-output producer and stopped receiver trial are immutable. The eventual receiver union has ten distinct outputs. No parser, recipe engine, guard, assertion, test method or admission threshold is relaxed.
