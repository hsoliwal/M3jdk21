# Cir: repair the exact A3 regex subject literal through an existing recipe

Source authority is Synexia `9963cc08ff13922b92a0e3db7c30f56fddacba7d`; target authority is M3 `752191c9291f6467110fb8a7badfbdc4c2d41af2`. This admission extends the bounded CI repair after the actual `consumer-v1` Maven run compiled 34 production and 60 test Java sources and ran 220 tests with zero assertion failures, one error and zero skips. The retained A3 delivery test rejected its regex-matrix template at the unchanged Java parse/print guard. That stopped trial and the successful five-output producer remain immutable.

The exact template at line 61 contains `"Pattern.compile("a+b?")",`: two interior double quotes lack Java escapes. Its canonical A3 source is byte-identical: 7,514 bytes, SHA-256 `f000b1eea411056baaca6051b220149f25334bc70bf69b4f0536839c6c78e404`. Static diagnosis establishes a lexical defect; it does not establish a parser dependency defect. No parser, owner, guard, existing test method or assertion will change.

The closed target set is:

1. `m3/tooling/a3/src/main/java/com/m3/a3/A3RegexMatrix.java`.
2. `m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/A3RegexMatrix.java.after`.
3. `m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/manifest.tsv`.

Each Java-bearing postimage inserts exactly two backslash bytes before the inner quotes. The manifest changes only that template's postimage SHA-256. The runtime subject must equal `Pattern.compile("a+b?")`, including its two quote characters. All patterns, subject ordering, digest framing, public/protected declarations and test assertions are preserved.

An isolated Maven task `tasks/m3-ci-752-regex` will use the unchanged, authenticated `com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe` from current M3 as a read-only task dependency. Its named recipe `com.synexia.m3.Cir` consumes the three exact PlainText preimages. It is not installed into Synexia's canonical recipe resource discovery. The task reuses the previously qualified scheduler fixture and proof runner, with an explicit new three-target inventory. There is no new transformation engine.

Required checks are ordered static validation, real Maven compilation, JUnit scheduler/semantic checks and a separate runtime export. They include all eight before/after mixtures, exact fixed points and replay patches, missing/stale/duplicate/wrong-path/empty-input refusals, post-scan drift refusal and unrelated-file preservation. Exact two-byte changes and the single manifest-cell replacement must be independently asserted. The malformed original must produce real Java compiler/parser diagnostics; corrected generated Java must round-trip through the existing parser.

The nested original A3 delivery recipe must consume the repaired actual resource and produce its exact four declared Java outputs. All four outputs plus exact current dependency closure must compile with the real Java 21 compiler. The retained regex-matrix, in-memory compiler and root-output guard runtime tests must execute from the compiled outputs. The full current migration-recipes suite and broader A3 module qualification remain receiving-workspace checks; no filtered run substitutes for the full 220-method consumer suite or existing coverage thresholds.

Only actual scheduler outputs from a successful fresh proof may be handed to the receiver, which will combine them with the prior five immutable outputs. Their union is eight distinct target paths, not a claim of transactional atomicity across independent recipe families. This admission runs no gate and makes no product, full-reactor, hosted-CI or canonical-promotion claim.
