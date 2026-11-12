<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current M3AST receiver — primitive FactBuilder + bounded first-fact lookup

**This product PR is a draft, hash-pinned receiver candidate, not a merged
runtime or JVM release.** It applies **two serial per-file recipes** from the
canonical Synexia convergence repository to the currently active
`jdk.m3.ast.SyntaxIndex` owner. It does **not** use or revive the
superseded `com.m3.ast.M3SyntaxIndex` path.

## Exact source chain

| Stage | Git blob | Authority |
|---|---|---|
| Current M3JDK21 master before application | `5ee16d47b2521385008f8c5ace8848792a3d76d8` | Product master `43d7df5fa868442a33fb8943189d096a00d779ba` |
| Recipe 1: aligned primitive FactBuilder columns | `08b2e7305a5b34d545464f883dab89a2393a62d7` | [Synexia #17055](https://github.com/hsoliwal/com.synexia/pull/17055) (merged) |
| Recipe 2: bounded lazy first-fact lookup with probe refusal | `db14c75a439b3366517c2b344b41d8f4f7a390fc` | [Synexia #17056](https://github.com/hsoliwal/com.synexia/pull/17056) (merged) |

The **original input Git blob** was verified against current M3JDK21
`master` before creating this branch. Both Synexia recipes are source-sealed;
recipe 2 requires the *exact* postimage of recipe 1. All three source
images are retained; no generated code was hand-edited during product
delivery. The final file was copied into the product repo as the
**identical Git blob**, without a new lossy transformation.

Target: `m3/ast/src/main/java/jdk/m3/ast/SyntaxIndex.java`.

The only changed product runtime file is the current SyntaxIndex owner;
public names, signatures, source projection hashes, prior stable scatter,
kind postings, and JNI ABI remain unchanged.

## Execution and correctness gates

The standalone Synexia Java21 differential of recipe 2 previously passed:
`PASS M3_AST_FACTS_FIRST_PROBE_GUARD maps=6004 comparisons=1132201
concurrent_queries=40000 collision_keys=256 disabled_builds=1`.
That result is **not** proof of the integrated product postimage.

The product receiver adds verbatim first-party contracts:

- `SyntaxIndexFactsFirstContract`: 100,000 facts, earliest duplicate keys,
  exact-value comparisons and 16,000 parallel queries
- `ProbeGuardContract`: adversarial 256-way equal-hash names, refusal after
  bounded probing, permanent original-scan fallback and 8 concurrent readers
- `SyntaxIndexFactsFirstTest`: Maven/JUnit wrapper that executes both;
  existing `SyntaxIndexTest`, `SyntaxIndexStableScatterTest`, and
  `SyntaxIndexKindPostingsTest` remain intact

An independent strict Java 21 test entrypoint **fails closed** if the
current file is not the exact candidate Git blob:

```sh
sh m3/ast/recipe/facts-first-primitive-20261112/verify.sh
mvn -B -ntp -f m3/ast/recipe/facts-first-primitive-20261112/pom.xml verify
```

Do not merge this draft until the actual Java21 compiler, original-owner
contract, OpenRewrite SDK recipe, full M3JDK21 module/image/jtreg, and
representative latency/allocations/RSS benchmarks succeed. In Synexia,
[Actions run #43135398802](https://github.com/hsoliwal/com.synexia/actions/runs/43135398802)
failed without any recorded steps or logs *even after a single rerun*;
this is **INFRA_UNVERIFIED**, neither a passed test nor a
Java code failure. Do not infer positive build status from that run.

No native code is added for a scalar String lookup. A future batch JNI
implementation needs independently measured benefit, GC safety and
cross-platform Java fallback parity. This candidate respects
M3Scale inventory -> catalogue -> exact source recipe -> serial atom
-> contract proof -> product handoff without replacing another owner.

## Isolated Maven dependency reconciliation

The first product workflow attempt compiled and **passed** the strict
Java 21 full-current-owner source/contract/collision test. Its Maven job
failed *before compilation* while resolving the existing module's
`junit-jupiter:5.10.2-m3-source-1` dependency, which Maven Central
does not publish. This is a dependency-admission blocker, **not** a failed
contract assertion.

To avoid changing that curated product module dependency, this receiver
adds an **isolated Maven recipe crate** at
`m3/ast/recipe/facts-first-primitive-20261112/pom.xml`.
It references the **same source and test files in the product module**,
compiles only `SyntaxTree`/`SyntaxIndex` and the exact original-owner
contract/test classes under Java 21 `-Xlint:all -Werror`, and uses
published JUnit 5.10.2. It does not substitute a copied facade or
claim the full module Maven dependency issue is repaired.

The original `m3/ast/pom.xml` remains intact. Full product Maven
tests using its curated dependency are still HOLD until that
dependency is supplied or independently qualified.
