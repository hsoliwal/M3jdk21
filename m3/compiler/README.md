<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/compiler: jdk.m3.compiler

The compiler family of the approved layout (jdk.m3.compiler = compiler + ast). It provides read-only javac facts and
annotation processors. None of them authorizes reclamation, elision or source mutation.

| Type | Facet | Folded from (2026-11-08) |
|---|---|---|
| `DoopLifetimeSlotPlanner` | DOOP-compatible lifetime slot plans | m3/compiler/lifetime-slots |
| `MatSortableProcessor` | Mat-sortable annotation processor | m3/compiler/mat-sortable |
| `JavaNullnessCompilerPlugin` (javac `-Xplugin:M3NullnessFacts`), `JavaNullnessFlow`, `JavaNullnessJavacBridge` | nullness facts | m3/compiler/nullness-facts |
| `ParentOwnedProcessor` | parent-owned facts processor | m3/compiler/parent-owned |
| `ReferenceFlow`, `SourceLiveness`, `Liveness`, `LoopFacts`, `CompilerReferenceCensus`, `CRefPathCertainty`, `CompilerFactsAudit` | reference flow, liveness, alias census and certainty | m3/compiler/reference-flow |
| `StaticFactsProcessor`, `StaticUsePlan` | static-use facts | m3/compiler/static-use |

Six one-package modules (`jdk.m3.compiler.lifetime`, `.matsortable`, `.nullness`, `.parentowned`, `.reference`,
`.staticuse`) and their descriptors (`org.m3jdk.compiler.*`) became this one package. The M3 prefix was dropped.
Their relocation records keep their bytes in `m3/evidence/compiler/<module>/`: README, GATES, PROVENANCE,
RELOCATION_MAP, recipe/, verify.sh, pom and descriptor. The fold is described in
`m3/evidence/compiler/FOLD_compiler.md`.

Build: `mvn -f m3/compiler/pom.xml verify` (Java 21, `-Xlint:all -Werror`; tests `-Xlint:all,-lossy-conversions` for
one donor proof). The javac Plugin descriptor joins the classes after compilation.

Still separate, for the next step of the family:
- `m3/compiler/jdk-standard` (`jdk.m3.compiler.standard`, `-Xplugin:M3JdkStandard`);
- `m3/compiler/source-signals` (`jdk.m3.compiler.signals`, `-Xplugin:M3SourceSignals`);
- `m3/ast`.
Seven workflows compile the first two directly.
