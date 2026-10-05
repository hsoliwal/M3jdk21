# Current-stack JDK build closure

Task: finish the complete previously declared M3JDK21 programme, beginning with genuine failing build gates rather than another candidate-only control engine.

Parent: `ec2b8502f2758da48b3bc8aa6dc0126ea3793e1e` (PR104), retaining PR99/103/104 source and history. The full packet denominator remains `m3/docs/whole-jdk-work-packets-and-resume.md`; the 31-row `m3/tooling/migration-recipes/tasks/map-utf/TODO.tsv` is not replaced or reduced by this leaf.

## Recovered execution inputs

The GitHub Actions artifact 11296530226 from run 37183564035 contains a complete source archive at `812d9dbbee4baebaac852c7c43289abc3a755f18`, Maven 3.9.16 and a dependency cache. Its ZIP SHA-256 is `3ae2497594952ccb08444c5344b554fa9f67db846a316ea08feab17b0d2c41fd`. The archive hashes were checked; rebuilding its complete Git tree produced `b5c56034d4e3c67ba7b1e8cb8fbb3f644fc33388`, matching that historical commit. This is a complete historical source export, not a checkout of the current parent and not Git ancestry imported into the canonical repository.

The recovered Maven runs locally with installed Debian OpenJDK21.0.11. This removes the earlier missing-Maven blocker for dependencies actually present in that cache; it does not imply a complete current dependency closure. Native development packages, exact current source and full runtime gates must still be checked.

## First reproduced failure

The unchanged module-pack Maven test lifecycle reproduces `M3_REACTOR_PREIMAGE_DRIFT` in `M3ModulePackRecipesTest.reactorRegistrationReachesFixedPoint`. The current-parent recipe and test blobs are identical to the recovered owners (`aa89cde2f4cf7195f47b4622627569785ee544e3` and `d6a31147a4c4ee52ab63117c8c5aa52a5e78ad60`). The test harness trims supplied fixture whitespace unless explicitly instructed not to; the production recipe correctly requires its exact reviewed preimage. Repair the test input boundary, not the production refusal boundary. Retain every existing test and exact pre/postimage resource.

## Approved bounded delta

Enhance the existing reusable module-pack recipes and their tests only where actual failures establish the need. Add exact-byte replay, fixed-point and refusal tests; preserve metadata where required. Existing Maven/JaCoCo thresholds and all existing test cases remain. No fabricated coverage, skipped tests or production-name stubs. Reuse the existing JDK proof-kit workflow to preserve current source, actual dependency closure and failure receipts, without increasing permissions or publishing/merging canonical state. Retain existing full module-pack verification and add the full control-reactor attempt after it; a failed gate remains a failure.

## Verification and stop conditions

Diff/whitespace and static checks precede actual compiler/tests. Tests must use the real OpenRewrite parser and scheduler. Measure existing line/branch gates instead of declaring configured thresholds achieved. Historical source evidence can only certify matching source closures. Any native image must be built as one matched source/VM tree; never transplant modified String.class into the installed JDK. Native build, jtreg, JNI/JIT/GC/CDS, platform, JEP/non-JEP, parser/DB, collections and mapping/IOP obligations remain open until their own exact-source receipts exist.

Produce the six mandatory M3 reports and a persistent priority/status queue in this task directory. No master/develop writes, rebase, force-push, squash, implicit promotion, dependency-policy weakening or unrelated source edits.
