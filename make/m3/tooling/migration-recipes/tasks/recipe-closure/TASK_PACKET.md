# JDK recipe closure — execution packet

Status: active; implementation and verification outcomes are not yet accepted.

## Baseline and custody

Repository: hsoliwal/M3jdk21. Parent: 081780ea02c11e8fd41a5445ac169b6a28ac5d25 (PR114, itself stacked on PR112 and current master). Full source tree: 02854e6c82fdd4f07fd1bc79a442bc237743950e. Hosted proof kit: run37255987007, artifact11322552678, ZIP SHA256 a0662117e005bb2afce6c66678fcef7dd878d0ec142d09c2ee56bbf6f0601676. All internal checksums and the locally reconstructed complete Git tree were verified. The artifact is input custody, not passing-test evidence.

## Intent and approved boundary

Repair the actual reusable Maven/OpenRewrite recipe owners, then execute their retained tests and the broader control reactor. Use existing recipe mechanisms for source changes. Retain atomization, patternization, IOP, documentation, scope gates, in-memory multipass testing and all old counterexamples. Reuse reviewed sibling-PR fixes rather than writing another engine. Preserve the wider JDK task denominator in tasks/map-utf/TODO.tsv.

Targets: m3/tooling/migration-recipes/**; any existing exact-source runtime test/descriptor packet admitted and documented during this pass. Product, public/protected APIs, native ABI, canonical refs, root POM, dependency versions, previous workflows and coverage thresholds are locked. No skip, exclusion, stubbed production owner, type-validation disablement, force-push, rebase, squash or automatic merge. A full native build is a separate acceptance lane and is not implied by a recipe test pass.

## Observed first blocker

The exact hosted source fails migration-recipes compilation in M3A3AlgorithmCatalogueRecipe: a fluent generic setter loses SourceFile target typing. The existing PR113 repair is the first donor candidate. Its generated-source admission and refusal behavior must be tested; no silent equivalence assumption.

## Execution and stop conditions

Local execution: Debian OpenJDK21.0.11, recovered Maven3.9.16 and OpenRewrite8.17.1 artifacts, offline dependency resolution, 4-CPU/4-GiB container; one Maven worker, two active JVM processors, bounded heap. No distributed fan-out. Each candidate follows diff/static checks, compilation, tests, runtime where applicable. Freeze original sources and expectations; repair recipes, not the oracle. Retain failed diagnostics. Stop unsafe application on source/manifest drift or failed contract checks. Do not lower the 99-percent gate to manufacture completion.

## Output contract

Publish STATUS.tsv, FINAL_REPORT.md, RUN_CONTEXT.tsv, PROVENANCE.tsv, VERIFY_CONTRACT.tsv, OUTPUT_CONTRACT.tsv and persistent TODO.tsv, with exact commands and results. Distinguish complete source recovery, compile/test execution, measured coverage, real native image/jtreg acceptance, and unresolved work. The full JDK programme cannot be marked finished merely because this packet finishes.
