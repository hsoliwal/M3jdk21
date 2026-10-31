# Synexia Borrowing Contract for M3JDK21

## Canonical source and permission

`hsoliwal/com.synexia` is the donor-convergence workspace and canonical owner of reusable Synexia architecture, Java/JNI implementations, Maven/OpenRewrite recipes, recipe crates, templates, fixtures, counterexamples, catalogues, documentation, and proof tooling.

M3JDK21 may freely borrow, execute, modify, and redistribute Synexia first-party assets that are actually licensed Apache-2.0, subject to the Apache-2.0 terms. This is not limited to runtime Java code: eligible recipes and their tests/templates are included. Preserve license/NOTICE and provenance, and pin the exact source revision and hashes. Reusable recipe semantics remain canonical in Synexia; this repository receives qualified exports or maintains thin receiver adapters.

## Mandatory per-file license boundary

The M3JDK21 repository contains GPLv2-licensed OpenJDK material. Do not interpret the Synexia Apache-2.0 grant as blanket permission to merge every Apache-2.0 file into every GPLv2-only target component.

- Apache-2.0 recipe/build tooling may be consumed as separate tooling when the applicable terms permit it.
- Before copying generated or handwritten Apache-2.0 source into a target component, inventory that component's exact license, notices, provenance, and compatibility.
- Apache-2.0 and GPLv2-only code have a known compatibility constraint; where the exact combination is not established as permitted, fail closed and keep the export packet separate.
- Third-party donor source retains its original license. Synexia does not relicense it merely by cataloguing, wrapping, testing, or generating a recipe around it.

This is a per-file and per-target decision, not a repository-wide assumption.

## Required delivery packet

Every admitted Synexia export records:

- source repository, immutable commit, path, and source SHA-256;
- canonical Maven/OpenRewrite recipe/task-crate identity and version/revision;
- target path, exact postimage SHA-256, and deliberate target-specific adaptations;
- applicable license/NOTICE and provenance;
- ordered verification receipts: diff -> lint/static analysis -> compile -> recipe replay/refusal/fixed point -> tests -> JNI/native runtime -> target build/runtime, as applicable;
- unresolved gates and explicit `NOT_EXECUTED` statuses.

Do not infer target qualification from a successful Synexia recipe test, a metadata row, or a workflow that failed before executing steps.

## Change direction

```text
M3JDK21 defect/evidence
    -> Synexia inventory and donor comparison
    -> canonical recipe + regression fixtures
    -> exact sealed export
    -> M3JDK21 receiver/materialization
    -> target-specific compile/test/runtime/JTReg
```

Target-specific Java/OpenJDK logic and runtime qualification remain owned here. A repeatable reusable transformation defect returns to Synexia for correction before it is reapplied to eligible targets.

Synexia `develop` is append-only history and must never be rebased. This target preserves its own history and never silently replaces an earlier valid optimization.
