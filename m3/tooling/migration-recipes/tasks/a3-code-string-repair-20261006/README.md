# A3 code-string recipe repair

This bounded recipe repairs an existing Java syntax error in the A3 regex/string subject catalogue. The exact subject line held unescaped nested quotes in both the production `A3RegexMatrix.java` and its hash-pinned Java recipe template. The original A3 Java delivery test correctly refused that invalid template; a separate javac invocation also reported three syntax errors. The failure evidence remains unchanged under `tasks/jcc-ci-repair-20261006/evidence/a3-behavior-v01`.

## Change and contract

The named recipe is `com.m3.rewrite.backport.A3CodeStringRepair`, using the unchanged canonical `M3Jdk21HashPinnedTextSnapshotRecipe` and crate `a3-code-string-repair-20261006`. It admits exactly three current preimages:

1. `m3/tooling/a3/src/main/java/com/m3/a3/A3RegexMatrix.java`.
2. `m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/A3RegexMatrix.java.after`.
3. The adjacent `a3-regex-memory-lab/manifest.tsv`.

The two Java copies gain exactly two backslashes, changing the source literal to `"Pattern.compile(\"a+b?\")"`. Its intended runtime subject remains the code string `Pattern.compile("a+b?")`. The manifest changes only the matrix row's after-hash, from `f000b1eea411056baaca6051b220149f25334bc70bf69b4f0536839c6c78e404` to `19f7e3fb8c5f4332ab721d1962ab9cac261c524a10d9d91272346636fbe86ada`. All interfaces, other source bytes, and original test bodies remain unchanged. The existing Java parser and exact round-trip guard remain unchanged.

The text owner is used because the admitted preimage is invalid Java. Java acceptance remains mandatory afterward: the actual materialized production class must compile with the pinned javac, and the unchanged original Java delivery test must accept the repaired template and reach a fixed point.

The reference audit inspected all 65 current migration-recipes main-resource `manifest.tsv` and `plan.json` files at revision `d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2`, verified their Git identities, and found exactly one binding to the matrix/hash: the nominated manifest row. The complete current M3 tree contains exactly the two nominated copies of the malformed matrix blob. Historical evidence is retained.

## Actual verification

The frozen execution in `evidence/execution/EXECUTION.json` records:

- **9 canonical-owner JUnit proof methods passed**, zero failures, errors, or skips.
- **3 actual OpenRewrite Results** equal the approved exact postimages. Publication copies these actual results, not the authored candidate resources.
- **Zero changes on replay**. Source identity and metadata are preserved, with OpenRewrite's provenance marker explicitly checked.
- **12 refusal cases**: each target missing, drifted, duplicated, or changed after scan. No partial outputs are accepted.
- **All 8 before/after combinations** converge to the same three bound outputs.
- Named declarative activation and serialized owner produce the same outputs.
- The exact nested-quote change, two-byte delta per Java copy, unchanged remaining bytes, and single manifest hash substitution are checked.
- **javac 21 compiles the actual produced complete matrix class**.
- **All 4 original A3 recipe test methods pass unchanged**, including the previously failing Java lab delivery and fixed-point test and the workflow delivery/fixed-point test.

Dependency/toolchain versions remain OpenRewrite 8.17.1, JUnit Jupiter 5.10.2, Maven 3.9.9, and JDK 21+35. The run uses a private copy of the exact existing offline cache. No substitute source owners, stubs, parser changes, or weakened assertions are used.

## Replay

With the pinned JDK selected and exact dependencies available locally:

```sh
mvn -o -B -ntp -f m3/tooling/migration-recipes/tasks/a3-code-string-repair-20261006/verification/pom.xml test
mvn -o -B -ntp -f m3/tooling/migration-recipes/tasks/jcc-ci-repair-20261006/a3-behavior-verification/pom.xml test
```

The separate sealed `plan.json` binds the same three outputs and unchanged owner guards for the existing repository installer. This run's materialization authority is the actual canonical OpenRewrite execution recorded in the evidence.

These finite gates qualify this lexical repair and the original A3 recipe behavior. They do not claim whole-module coverage, the foundation workflow, A3 runtime tests, native/JNI qualification, or a complete JDK build.
