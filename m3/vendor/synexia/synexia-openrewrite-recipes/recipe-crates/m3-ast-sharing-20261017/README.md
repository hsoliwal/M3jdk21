<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Shared compiler AST and leaf-first processing

This canonical Synexia recipe packet connects existing javac and MIndex owners to
an isolated, lazy, bounded AST consumer. Its receiving implementation belongs to
the independent M3 compile-time module and the existing target-owned compiler
hooks. No Synexia runtime dependency is introduced into `jdk.compiler` or
`java.base`.

The processing order is explicit: **L0 per-file facts and atoms; L1 file summary;
L2 package; L3 module; L4 project; L5 repository**. Package and project remain
distinct. Existing parser-granularity scopes keep their original meaning.

## Review order

| Boundary | Recipe and implementation | Evidence |
|---|---|---|
| Source and cache identity | [cache](cache/README.md) | [Cache/JNI qualification](cache/evidence/QUALIFICATION.json) |
| Actual compiler task and source | [frontend](frontend/README.md) | [Composed SDK](frontend/evidence/FINAL_UPSTREAM_COMPOSITION_SDK.json), [runtime contracts](frontend/runtime-tests/evidence/QUALIFICATION.json) |
| L0–L5 dependency hierarchy | [hierarchy](frontend/hierarchy/README.md) | [Hierarchy qualification](frontend/hierarchy/evidence/QUALIFICATION.json) |
| Existing upstream public API | [upstream contract](frontend/upstream-contract/README.md) | [Unmodified upstream test](frontend/upstream-contract/evidence/QUALIFICATION.json), [both plugins](frontend/upstream-contract/evidence/COEXISTENCE.json) |
| Primitive-image task reuse | [primitive proof](frontend/primitive-image/verify.py) | [Image and sidecar equality](frontend/primitive-image/evidence/RESULTS.json) |
| Optional OpenRewrite projection | [optional adapter](frontend/openrewrite/README.md) | [Visitor qualification](frontend/openrewrite/evidence/RESULTS.json) |
| Ordinary Maven modules | [module wiring](frontend/module-wiring/README.md) | [Normal module proof](frontend/module-wiring/evidence/RESULTS.json) |
| Existing naming authority | [registry](frontend/registry/README.md) | [Registry qualification](frontend/registry/evidence/QUALIFICATION.json) |
| Receiving handoff and CI | [integration recipe](integration/prepare_integration.py) | Receives the actual canonical implementation commit after publication |

## Reusable transformation invariant

Every LLM-assisted source change uses a Maven-based recipe. Java transformations
reuse the existing class-backed OpenRewrite Java snapshot recipe; native and
build metadata use the existing text snapshot recipe. The source files emitted
by those recipes, their exact preimages and postimages, tests, counterexamples,
licences and measured evidence travel together. A correction changes the recipe
and its reviewed input, followed by a fresh recipe application. Target outputs
are never repaired independently.

The [algorithm catalogue](frontend/CATALOGUE.tsv) and
[file-by-file review](frontend/FILE_REVIEW.tsv) record operation categories,
existing owners, alternatives, contracts, complexity and qualification. The
hierarchy, cache and optional-adapter packets retain their more detailed source
and failure records. These files are searchable with ordinary text tools and
are intended to accumulate decisions across later refinement passes.

## Source and licence boundaries

The original Synexia source selection is pinned to
`e244d68fdee88d4a406be48e4ff1d31b60a17988` and reconciled against
`892e3f6c51d78bc0948e8a8cf736055b4c70eafa`. The compiler composition preserves the
public API, source-retention hook, primitive-image consumer and service contract
present in M3JDK21 `4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`. Exact current recipe
providers are reused from `3aa8e45d93f5fed721fdb64579b655f4a432a308` and are
unchanged at the reconciled canonical revision. Their exact
[task-local source custody](provider-custody/README.md) lets the public mirror
compile the qualified providers without changing older global recipe owners in
the receiving repository.

The [final publication-base reconciliation](frontend/evidence/FINAL_CANONICAL_RECONCILIATION.json)
also compares the complete MIndex subtree with Synexia
`d391494684c17b1cb8930d4a1f99fe56c0882aa0`. Its two later changes to the separate
leaf-index projection and self-test remain untouched. The qualified source
closure and module preimages retain their original identities.

Synexia-original source, recipes and proofs retain their Apache-2.0 notices.
OpenJDK source snapshots, including compiler templates and the unmodified
upstream test, retain their original GPLv2 and applicable Classpath Exception
terms. The files in [frontend/licenses](frontend/licenses) preserve the reviewed
OpenJDK licence material. Recipe ownership does not relicense a payload.
OpenRewrite retains its own Apache-2.0 attribution. SQLite is the independently
verified native-test toolchain input described in the cache packet. Challenge
sites supply problem categories and adversarial shapes; their statements,
editorials and submissions are not copied as implementation code.

Qualification is scoped to the recorded inputs and commands. Existing global
receiving-phase gates, whole-JDK/JCK qualification, full canonical reactor
availability, and custom source-built dependency coordinates remain separate.
