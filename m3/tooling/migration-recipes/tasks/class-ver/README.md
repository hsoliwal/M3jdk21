# ClassVer — Java 21 module class-header admission

Resume the saved class-version candidate on PR95 ancestry. Reuse the existing Java snapshot recipe; do not add a second snapshot engine, wrapper hierarchy or runtime API.

## Contract and scope

The existing M3ModuleInspector claims Java21 non-preview effective-class admission. Its old predicate admits invalid low major versions and nonzero modern minor versions, while refusing valid legacy minor 65535. This is an explicit conformance repair to that observable admission policy, not a claim that broken and repaired behavior are identical. Public signatures, descriptor/native-resource inventory, archive ownership and input-drift handling stay unchanged.

JVMS21 section4.1 is the normative basis: majors45..65 are supported; majors45..55 may use any unsigned16 minor; majors56..65 require minor0 for this non-preview pack policy. Header acceptance is not full bytecode, linkage or native ABI verification.

https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.1

The existing validateClass leaf remains cohesive. Pattern/IOP: ArchiveAdapter / ClassVersionAdmission. One production file changes; conformance approval has LIBRARY/API scope even though its physical edit is file-local.

## Existing work reused

The inspector postimage and two behavioral test sources are the exact saved ClassVersion candidate. The previous unlanded task-specific snapshot-engine proposal is not installed. Instead `com.m3.rewrite.packs.ClassVer` configures the existing M3Jdk21HashPinnedSnapshotRecipe from PR95. Run its manifest with `m3/tooling/module-packs/kernel` as source root. Three Java targets: inspector, shared checks, JUnit wrapper.

The shared corpus tests actual Java21 class-loader acceptance and real JAR/JMOD inspection, legacy/modern/preview/out-of-range headers, multi-release effective-entry selection, read-only inputs, and a real jmod-produced control archive. Standalone checks are not described as JUnit/JaCoCo execution.

## Verification

1. Exact source/template hashes, diff and syntax/lint.
2. Actual ClassVerTest parser/scheduler JUnit, then the full migration-recipes Maven lifecycle.
3. The full unchanged module-packs Maven lifecycle and its coverage gates.
4. The Java21 shared checks in JIT and interpreter modes and the diagnostics image/native-jcmd smoke.
5. Complete modified-JDK build/jtreg and wider programme obligations before product acceptance.

No skips, framework stubs, warning suppressions, POM changes or coverage-threshold changes are introduced. Local subset results and hosted framework results must remain separately labelled. Save recipe and materialization in additive commits and a draft PR; never rebase, squash, force-push or merge canonical history automatically.
