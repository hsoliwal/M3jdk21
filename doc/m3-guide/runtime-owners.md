<!-- SPDX-License-Identifier: Apache-2.0 -->

# Runtime owner boundaries

[Guide](README.md) · [JDK modules](jdk-modules.md) · [M3 packages](m3-packages.md)

These are five source-sampled package boundaries inside java.base. They supplement the separate library/tooling package guide. They are partial current-source explanations, not matched-runtime verification.

## java.lang: canonical String value and ownership

`M3String` stores an owner and a packed coordinate. The sampled source describes native/mapped atom payloads and persistent tuple ownership rather than retaining byte[], char[] or segment arrays in the value itself. `M3StringOwner` is the canonical owner node; owner/range facts and explicit JNI compatibility shadows have separate responsibilities. Public `java.lang.String` remains the API authority.

Source: [M3String](../../src/java.base/share/classes/java/lang/M3String.java), [M3StringOwner](../../src/java.base/share/classes/java/lang/M3StringOwner.java). Start with [backing and current-source contracts](../mindex-string-backing.md) and [JNI ingress/egress](../../m3/docs/mindex-jni-string-ingress.md). The source-directory catalogue links all sibling owners.

Constraints: preserve Java UTF-16 behavior, equality/hash/order, owner lifetime, bootstrap, native acquire/release and all public String contracts. Array projections do not become a second canonical spelling store. Tests under `test/jdk/java/lang/String` and `m3/runtime-integration/tests` are located; no test was run in this pass. Full method-by-method error, GC and VM-layout review remains open.

For member-level detail at the reviewed public snapshot, continue with [bootstrap String ownership and admission](bootstrap-string-ownership.md) and [String operations and facts](bootstrap-string-contracts.md). These bounded references cover six foundational source bodies and selected String/native boundaries; the remaining sibling bodies, whole-package review and runtime/GC qualification stay open.

## java.util.regex: candidate pruning and exact matching

`M3PatternQuery` derives conservative trigram requirements from the existing compiled Pattern node tree. Negative necessary-condition facts may prune; positive results still require the authoritative regex implementation. The source explicitly refuses unsupported or observably unsafe cases rather than inventing another parser.

Source: [M3PatternQuery](../../src/java.base/share/classes/java/util/regex/M3PatternQuery.java), [Pattern](../../src/java.base/share/classes/java/util/regex/Pattern.java), [Matcher](../../src/java.base/share/classes/java/util/regex/Matcher.java). The pinned snapshot includes the current source changes; historical recipe READMEs describing an earlier literal-only state must not be treated as a current whole-package description.

Constraints include input/region identity, UTF-16 keys, callback/effect behavior and hitEnd/requireEnd semantics. Tests under `test/jdk/java/util/regex` remain NOT_RUN here. This page does not infer complete v7/v8 admission from the presence of a query class or a merged change.

## jdk.internal.mindex: bounded facts and internal target counterparts

`M3TQ` implements conservative boolean queries over UTF-16 trigram facts. It is a target-owned internal adaptation with a candidate-only contract: false can establish absence under its preconditions; true does not prove a regex match. The package also contains primitive address/bit/number lanes, String backing, class/member and version metadata owners. Their names do not imply every donor capability is received.

Source: [M3TQ](../../src/java.base/share/classes/jdk/internal/mindex/M3TQ.java) and [package directory](../../src/java.base/share/classes/jdk/internal/mindex). Its module owner is java.base; compiler-only dependencies cannot be imported into it. Read the [canonical naming map](../../m3/docs/name-mapping.json) for each concrete owner.

Open gaps: independent contract/error/lifecycle documentation for each sibling owner, actual consumers, retained/native-memory budgets and exact target tests. Source samples and historical receipts are not full-family acceptance.

## jdk.internal.util: opt-in indexed storage support

The sampled `M3IndexedStorage` describes insertion-ordered primitive-index storage whose physical slots remain stable across sorting. Mutations are single-thread-confined, the caller owns comparator consistency, and live arrays are not returned to a pool. It is an internal building block rather than a replacement for List.

Source: [M3IndexedStorage](../../src/java.base/share/classes/jdk/internal/util/M3IndexedStorage.java). `M3ObjectArrayPool` and `M3PipelinePlan` are sibling source entries requiring their own full contract review. The broader package contains unrelated inherited JDK utilities; this sample does not document them all.

Open gaps: concrete caller/test mapping, pool lifetime/error handling, pipeline semantics, resource measurements and whole-package inline documentation review.

## jdk.internal.vm.parallel: reviewed bulk execution

`BulkTask`, `BulkExecution`, `BulkExecutor`, `BulkExecutorProvider` and `BulkScheduler` are the internal contract surface. Optional bridges under `m3/ports/parallel-bulk` and `m3/ports/tornadovm-bulk` implement concrete bounded operations outside java.base. The internal scheduler is not an authorization to accelerate arbitrary Runnable objects or add a provider runtime dependency to java.base.

Source: [BulkTask](../../src/java.base/share/classes/jdk/internal/vm/parallel/BulkTask.java), [package directory](../../src/java.base/share/classes/jdk/internal/vm/parallel), [parallel bulk port](../../m3/ports/parallel-bulk/README.md), [Tornado adapter](../../m3/ports/tornadovm-bulk/README.md).

Open gaps: full provider-order/refusal contract reconciliation, every operation/schema, concurrency and cancellation, native/device lifecycle and exact Java/provider parity results. Linked test sources were not executed.

## Compiler placement is a policy boundary, not a fabricated package

The naming policy reserves compiler-specific counterparts such as com.sun.tools.javac.m3. The pinned source tree does not gain such a package merely because it appears in a design mapping. Use the actual `jdk.compiler` descriptor and package inventory; any proposed compiler owner requires an actual source path and target evidence before it is documented as implemented.
