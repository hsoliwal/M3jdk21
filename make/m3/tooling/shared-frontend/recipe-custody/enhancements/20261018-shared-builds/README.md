<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3AST: one original javac task per compilation

The qualified changes extend the existing compiler-owned M3AST session and supply
the missing provider for the custom Gradle fork. Maven and Gradle consumers observe
their build's original javac task, context and trees. No receiving implementation
creates a second parser, attribution pass, AST or process-wide cache.

## Changes and receiving order

| Leaf | Receiving behavior | Exact source owner |
| --- | --- | --- |
| `m3jdk21-seals` | Abort the existing session on source-capture failure; hash exact UTF-16 units in bounded blocks; reject changed length before hashing. | Existing `M3ASTPC` and a focused source-seal test. |
| `m3jdk21-plugin-selection` | Add static `M3AST.isPluginSelected(JavacTask)` and observe the task's existing parsed `Arguments` owner. | Existing public API and compiler owner; a separate real-task test. |
| `gradle-provider-20261018` | Attach the original Gradle task; choose one consumer-discovery owner using the compiler query; forward task setters, call and cleanup. | New optional `org.gradle.internal.m3.ast.M3GradleASTProvider` in M3JDK's documented build-support location. |
| `m3-maven-session-recovery-20261018` | Recover default Maven session-service admission that was absent from the merge tree, preserving lazy suppliers and current M3 compiler hooks. | Two Java outputs in the Maven fork; a separate canonical recipe crate. |

Apply source seals before plugin selection: the second dataset deliberately seals
the first leaf's compiler-owner postimage. The Gradle provider is an independent
nine-file addition. Both original compiler API declarations and instance interface
methods remain intact; the new static method adds no abstract implementation duty.

The custom Gradle SPI/hooks already survive master
`419a77cf61af3924f60678510ed2440230047671`. The missing implementation belongs in
the provider location named by that fork, so no duplicate Gradle fork patch is
needed. Maven's receiving base is
`bd11c44cd98aef16a3d049db0ebd8477608ccf4d`; M3JDK's is
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

## Sharing semantics

Each actual javac context retains one M3AST session. Consumers use the original
PARSE/ANALYZE events and share a projection by reusing the same projection-key
object during the same event. Callback lifetime, thread confinement, null/error
memoization, recursion refusal and cleanup remain with the existing owner.
Source seals never authorize reuse of attributed facts across different tasks,
classpaths, processors or phases.

The Gradle provider queries effective plugin selection before opening the session.
If javac selected the builtin M3AST plugin, javac's original plugin/processor
loader discovers consumers. Otherwise the provider's explicit asset loader owns
discovery. A classpath operand that looks like a plugin and an internal option
override therefore follow javac's actual decision. Parent-loaded provider-only
consumers are refused before construction while the host SPI identities remain
shared.

In the standalone Gradle provider proof, the original source and a generated source
produce two PARSE events. Two independent consumers request the same projection:
there are two computations and two hits, zero extra M3AST source reads, equal class
bytes to ordinary compilation, and the emitted code returns 42. The comparison
baseline is its own ordinary compilation; the one-task assertion applies to each
selected compilation.

Separate Maven and Gradle JVMs cannot share a live tree object. Reuse across those
boundaries would require qualified detached data and matching compilation-context
identity. This change establishes the shared frontend inside each original
compilation and introduces no new cross-process cache.

## Executed qualification

| Gate | Observed result |
| --- | --- |
| Existing snapshot SDK, per compiler/provider dataset | 5 tests pass; unchanged four recipe owners. |
| Input sealing / isolated receiver, per compiler/provider dataset | 8 / 9 cases pass; exact receive and zero-write repeat. |
| Compiler sources plus actual module descriptor | Strict `-Xlint:all -Werror -implicit:none` compile passes. |
| Existing compiler sharing suite | 70 checks pass on the final image. |
| Source seals | 117 checks pass; 23 digest cases include all 65,536 UTF-16 unit values. |
| Plugin-selection API | 462 checks across 24 real javac tasks pass on the final image. |
| Existing OpenRewrite tree adapter | 5 tests pass on the final image. |
| Gradle provider | 20 tests pass without skips; packaged provider and emitted-code runtime pass. |
| Maven selected native reactor | 2,273 reported tests, zero failures/errors, 5 existing skips. |
| Maven integration on final compiler image | Six-case in-process/fork suite and actual compiler-plugin 4.0.0-beta-5 native-V4 stock/M3 pair pass. |
| Final full-borrow receiver guard | 32 families pass; state remains `SOURCE_PIN_ONLY`, completion false. |

The first source-seal image and its receipts are retained as that leaf's historical
qualification. Final consumer receipts bind `descriptor-qualification-02` and its
module payload. The source-capture baseline witness reproduces publication of a
partial image on the old owner; the received owner passes the same regression.
This cleanup correction is scoped to capture failures, not a universal success
signal for unrelated compiler failures.

The runtime driver uses supported `jmod`/`jlink` operations, the actual compiler
descriptor, unchanged stock payloads outside the named compiler owners, and
preserved coverage of all 65 original module hashes. It does not disable module
hash checks. It produces a hybrid Java 21 compiler-source qualification image;
a complete native JDK/HotSpot source build was not performed. The Gradle proof
uses the exact published SPI source; a complete Gradle distribution, worker,
daemon, incremental task or build-cache run was not performed. No benchmark or
whole-platform compatibility claim follows from these finite gates.

## Reproduction and custody

The sibling enhancement directories retain contracts written before source edits,
exact pre/postimages, actual SDK inputs/results, patches, stage logs, runtime
receipts and source/class/module hashes. Large immutable inventories are stored
with deterministic gzip and their original hashes. Failed environment and command
setup attempts are retained separately from successful product gates.

In the canonical recipe crate, use Java 21 and the existing POM with each dataset
in the receiving order above. Run `test-compile` with `-DskipTests`, then
`surefire:test`, with `-Dm3.dataset=<dataset>` and the actual recipe-owner root.
The unchanged `contract/test_prepare.py` and `contract/verify_receiver.py` record
the 8 and 9 contract cases. The unchanged `receive.py --check`, receive, and repeat
commands then admit only exact SDK outputs. Full argument arrays are in each
leaf's event log. The public custody checkout uses its existing `sdk-owner`
directory; canonical qualification uses `synexia-openrewrite-recipes` as owner.

The unchanged runtime driver lives in the source-seal enhancement's `runtime/`
directory. Its `build` command accepts `--stock-jdk`, `--source-root`, and
`--output`; `validate` accepts `--output`. The separate plugin-query test's strict
compile and execution flags, including declared module exports/opens, are retained
in the plugin-selection event log.

The historical #1192 compiler-owner variant was inventoried and correctly refused
as an unqualified preimage without writes. Its separate missing AST hierarchy is
not restored or claimed by these enhancements. Preserving a merge parent alone
does not prove that every historical source change survives the current tree.

## Existing Maven configuration edge

Maven's unchanged pre-task `m3.ast` helper inspects individual `compilerArgs`
values before the compiler task exists. A classpath operand literally equal to
`-Xplugin:M3AST`, or an advanced internal override, can confuse that shortcut.
For these configurations, complete all operands and supply a real final
`<arg>-Xplugin:M3AST</arg>` switch after the overrides, retaining intended plugin
arguments. The two-file Maven restoration stays exact. No temporary compiler or
second option parser is introduced into Maven's earlier configuration hook.
