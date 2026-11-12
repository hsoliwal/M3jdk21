<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST Facts.first: Java 21 cost and JNI admission profile

## Scope and source identity

This is a **read-only, source-sealed Maven proof crate** running against the
real, merged `jdk.m3.ast.SyntaxIndex` product class and its existing
`SyntaxTree`. It does not add a parser, source rewrite engine, public API,
native symbol, or new per-fact object.

- Exact active owner: `m3/ast/src/main/java/jdk/m3/ast/SyntaxIndex.java`
- Required Git blob: `db14c75a439b3366517c2b344b41d8f4f7a390fc`
- First-party source chain: Synexia #17055 -> Synexia #17056 ->
  [M3JDK21 PR #2943](https://github.com/hsoliwal/M3jdk21/pull/2943)
- Original existing contract: linear source-order lookup; **first**
  matching duplicate property value wins, exact String equality
- Product compile/JUnit proof: [GitHub Actions #43137806368](https://github.com/hsoliwal/M3jdk21/actions/runs/43137806368),
  two real successful jobs, with original-owner contracts

## What gets measured (and what does not)

`FactsFirstPerformanceAdmission.java` compiles directly against the actual
product owner and checks every measured query against a public, observable
source-order scan using `Facts.key`/`Facts.value`. It verifies the
earliest duplicate value for all distinct keys and includes absent keys.

Four deterministic workloads are measured:

| Workload | Fact rows | Unique properties | Purpose |
|---|---:|---:|---|
| small-no-cache | 128 | 8 | Avoid building a cache for small inputs |
| normal-repeated | 4,096 | 64 | Many duplicate property keys |
| high-cardinality | 4,096 | 1,024 | More unique properties and collisions |
| equal-hash-adversarial | 4,096 | 256 | All keys have equal Java hash codes; should use safe fallback |

Each workload uses five alternating-order, 4,096-query measurement samples,
after correctness verification and warmup. The printed median nanoseconds
per query and ratio are **descriptive measurements**, not a hard performance
claim or automatic product promotion. The reference uses public sidecar
accessors, not private-array access; its extra bounds checks must be
considered before interpreting any speed ratio as an exact old-implementation
speedup. Running on a shared CI worker makes timings noisy.

The profile includes build/verification **outside** the timed query loop;
therefore it specifically measures warmed lookup behavior, not full index
manufacturing cost, cold latency, GC, native transition overhead, or RSS.
Those are separately catalogued admission requirements in `NEXT_ATOMS.tsv`.

## Java and Maven verification

```sh
sh m3/ast/recipe/facts-first-performance-20261112/verify.sh
mvn -B -ntp -f m3/ast/recipe/facts-first-performance-20261112/pom.xml verify
```

The shell script and Java harness independently check the exact Git blob.
The Maven crate compiles only the two current product classes and the
benchmark using Java 21 with `-Xlint:all -Werror`. No JUnit or native
dependencies are introduced. The matching path-scoped GitHub workflow
runs both commands.

The performance harness always checks semantic parity before timing;
invalid source, changed result, or failed hash-collision input
construction is a hard error. The test has **no minimum speed ratio**.
No reliance on random seeded string hashing, private reflection or
external benchmark output.

## Donor catalogue, licences, and JNI decision

`CATEGORY_REVIEW.tsv` records the EXACT_LOOKUP problem category
with LeetCode, HackerRank, GeeksforGeeks and pinned Apache-2.0 Agrona as
**classification and conceptual review only**. No challenge solution,
editorial, or donor source was copied.

A JNI lookup is **not admitted** until a separately measured *batch*
workload demonstrates a setup-inclusive end-to-end win over the actual
Java primitive implementation, and validates source/ABI compatibility,
array ownership, GC pinning policy, 32-probe/scan fallback and platform
coverage. In this crate JNI is correctly absent.

Source/runtime changes must remain recipe-first in canonical
`hsoliwal/com.synexia`; this crate only qualifies the already merged
product postimage and does not alter observable behavior.
