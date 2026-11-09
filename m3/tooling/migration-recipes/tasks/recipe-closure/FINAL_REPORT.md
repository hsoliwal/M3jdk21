# JDK continuation — implemented recipes, measured failures, no whole-JDK completion

## Files and exact delta

The code delta updates five retained files: the A3 algorithm-catalogue recipe, scalar-int eligibility, external recipe registry, its retained test, and the actual internal M3Descriptor owner. It adds an in-memory multi-file test laboratory, two configurations of the existing hash-pinned Java snapshot recipe, their reviewed pre/post resources, import admission tests, descriptor framework/runtime tests and this task evidence. No POM, dependency version, existing workflow, old test removal, native ABI, canonical branch or installed JDK changes.

The A3 repair is exact public PR113 donor reuse, not another transformation engine. The new scalar-domain test first reproduced incorrect admission of a post-name array parameter; eligibility now checks the declared variable type as well as syntax. Four array/varargs/boxed parameter controls refuse unchanged. The import registry adds only the already composed upstream RemoveUnusedImports; its genuine NoMissingTypes guard is retained.

Both source-repair capsules execute the actual existing OpenRewrite engine. Four repair-owner postimages and two descriptor packet outputs equal the installed source bytes. The descriptor change reuses Objects.requireNonNull(type, "type").descriptorString(). Hidden-class output is an intended bug correction, not a claim of equivalence to the defective result. Nominal results, null messages and public/protected member descriptors are preserved.

## In-memory multipass project

Six actual JUnit tests exercise immutable three-file projects, 54 arithmetic/shift/bit-expression variants, 353 edge/seeded input pairs, and both atomizer/patternizer orders. Every intermediate project compiles through JavaCompiler using --release21 -proc:none -Xlint:all -Werror; source and class output remain in memory with isolated loaders. Compare to both an immutable original and an independent numeric model. Check effects/exception/callback traces, every declared method/field/constructor, unchanged negative controls, genuine atom/IOP/documentation output, fresh fixed point and project enumeration order. A one-sweep budget refuses. A deliberately wrong compiling candidate is caught. OpenRewrite TypeValidation.all stays enabled. This is finite-domain evidence, not arbitrary Java or VM correctness.

## Exact verification executed

The complete archived PR114 source was recovered and its Git tree matched 02854e6c82fdd4f07fd1bc79a442bc237743950e. The artifact also supplied actual Maven3.9.16/OpenRewrite8.17.1/JUnit dependencies. Local execution uses Debian OpenJDK21.0.11, not the hosted proof kit's Temurin21+35. Input recovery is not proof of tests.

The final unfiltered fail-at-end Maven clean verify completed on 2026-10-05T05:16:16Z with exit1. It executed241 JUnit tests:237 passed,2 failed,2 errored,0 skipped test cases. Migration-recipes executed163, of which159 passed; all20 new tests passed. IndexDB61, module-pack-kernel9 and module-pack-recipes8 passed. A3 and backport-DAG modules were skipped as dependents of the failing recipe module, not claimed tested. Backport admission/static checks passed.

Existing independent coverage gates passed for IndexDB (685/687 lines;240/241 branches), module-pack-kernel (151/152;99/100), and module-pack-recipes (51/51;16/16). The unchanged migration scope/atom gate FAILS:312/346 lines (90.1734%) and186/263 branches (70.7224%). Its configured JaCoCo check was actually invoked and returned1. Thresholds and exclusions were not changed.

Strict compilation of nine touched/new Java recipe/test sources passed with warnings as errors after correcting a test text-block whitespace warning; the original warning log is retained. The final full reactor was rerun after that correction. The new laboratory's scalar-array red/green failure, the initial exact-source A3 compiler failure and the correctly refused first out-of-scope capsule paths are preserved as counterexamples.

The unchanged foundation runner passed70462 checks in each of four modes. The actual descriptor candidate compiled strictly and passed8077 assertions in each of four modes (normal,interpreter,noncompact,C2-focused). Original nominal corpus passed5681 and original hidden-class failure reproduced. Public/protected javap descriptors are byte-identical. These are repeated executions of the same corpus, not4x distinct inputs. The descriptor experiment patches only a layout-independent class into stock Java21; it is not a whole modified image or jtreg pass.

## Failures and next gates

Two retained M3MIndexJniNewStringAdmissionRecipeTest cases fail inside the OpenRewrite8.17.1 Javadoc parser (Expected to be able to find @exception). Two retained M3MIndexStringBulkCharBoundaryRecipeTest cases error on missing/malformed actual String/MIndexString/internal-JDK attribution. No replacement source, fake production types, disabled validation or skipped test was used to hide these failures.

A real fastdebug native configure attempt stopped at absent ALSA development headers. No make/images/matched-image jtreg or platform acceptance followed. Checkstyle/PMD/SpotBugs/Spotless were not executed in this bounded packet; strict javac/hygiene do not substitute for any separately required gate. Hosted exact-head CI remains required and must be reported from its actual run.

The existing catalogue reports82 JEP rows:43 decided-no-direct-backport and39 pending proof/implementation;13 non-JEP seeds:1 admitted and12 pending. These are retained catalogue states, not blanket approval. Community/Temurin donor assimilation, ordinary String, collections/concurrency, GC/JIT/CDS/native, all module/platform obligations and the31-row programme ledger remain open. This PR is draft and does not authorize promotion, merge, rebase or a completion claim.

## Reproduction and evidence

Run mvn -B -ntp -fae -f m3/pom.xml clean verify with a full JDK21 and the pinned dependencies. In this execution, offline Maven used the recovered cache. Run the configured migration JaCoCo report/check separately to inspect coverage after failing tests; this never makes the failed tests optional. Run bash m3/tooling/migration-recipes/tasks/recipe-closure/verify-descriptor.sh <JDK21_HOME> <repository> <new-output-directory> for the scoped runtime experiment.

JUNIT.tsv, FAILURES.tsv and COVERAGE.tsv are mechanically extracted from the final reports. SOURCE_SEALS.tsv identifies the code and templates. EVIDENCE_SHA256.tsv binds raw logs, prior counterexamples and XML reports in the companion downloadable bundle. Their existence and this prose do not independently certify acceptance.
