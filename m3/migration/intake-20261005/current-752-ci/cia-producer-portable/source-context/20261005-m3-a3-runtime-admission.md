# Cia: finite A3 catalogue and runtime closure repair

This new admission follows the immutable actual `consumer-v2` STOP at source Synexia `9963cc08ff13922b92a0e3db7c30f56fddacba7d` and target M3 `752191c9291f6467110fb8a7badfbdc4c2d41af2`. The migration-recipes module passed all 220 tests and its retained 99% line/branch coverage thresholds. A3 compiled all 13 production and six test Java files, but its 19 tests reported two failures and seven errors. The preceding ten Cij/Cit/Cir outputs and every original trial remain immutable.

Exact source and stack review identifies three distinct issues:

- `A3Alg.Row` invokes `validate(this)` in a compact record constructor before Java's implicit field assignments. Its normalized constructor parameters are valid, while the record accessors still return default null values. The repair uses an explicit canonical constructor with the same 13 parameter names, types and order; it assigns the same normalized values to the same fields in the same order, then calls the unchanged validation helper. No predicate, message, record component or public API changes.
- Four JEP rows in `A3PlanTest` end in a physical tab inside a Java text block. Incidental-whitespace stripping removes that trailing empty seventh TSV field. The repair changes only each final physical tab into its Java source escape `\t`, preserving the intended empty field. All existing test methods and assertions remain intact; the production TSV parser remains strict.
- The exported migration-recipes library uses OpenRewrite classes that require `org.slf4j.LoggerFactory` at runtime. Its own tests receive `slf4j-api:1.7.36` through the test-only rewrite-test dependency, while downstream A3 has no SLF4J API. Add only the same exact API version with runtime scope to the shared migration-recipes POM. A logging binding is not required, and no test-only workaround or A3-specific duplicate is added.

The closed six-target set is:

1. `m3/tooling/a3/src/main/java/com/m3/a3/A3Alg.java`.
2. `m3/tooling/a3/src/test/java/com/m3/a3/A3PlanTest.java`.
3. `m3/tooling/migration-recipes/pom.xml`.
4. `m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3/M3A3AlgorithmCatalogueManifest.java`.
5. `m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/a3-algorithm-catalogue/after/A3Alg.java`.
6. `m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/a3-algorithm-catalogue/after/A3PlanTest.java`.

The two AFTER resources remain byte-identical to their canonical files. The existing manifest class changes only their two postimage Git-blob constants. All BEFORE resources and historical preimage pins stay unchanged. No existing recipe engine or execution gate is altered.

An isolated `tasks/m3-ci-752-a3` task will reuse the exact current M3 text snapshot owner and the qualified proof/scheduler fixture. Named recipe `com.synexia.m3.Cia` admits all six exact source images together. Qualification requires all 64 before/after combinations, fixed points, replay, 26 initial refusals and 12 post-scan refusals. It also requires real Java 21 compilation of the generated catalogue sources with exact current closure, public/protected javap descriptor equality, direct canonical-constructor success and failure probes, and all three retained A3Alg test bodies plus the unchanged-assertion A3Plan test body. Exact source delta and POM dependency predicates must be checked independently.

Only actual generated outputs from a fresh successful proof may join the existing ten outputs in the receiver. The receiver must rerun the full unchanged migration-recipes/A3 reactor and coverage checks, backports, host and receipt checks. A focused Cia producer PASS is not a full-reactor, hosted-CI, native-runtime or canonical-promotion claim. No gate has run under this admission when authored.
