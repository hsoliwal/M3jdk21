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

## Executed Java21 workflow evidence — 2026-11-12

[GitHub Actions run #43140897495](https://github.com/hsoliwal/M3jdk21/actions/runs/43140897495)
completed **SUCCESS** on PR commit
`45a77402210ed3ba1664eb2dc7c63bcf40e88447`.
The job executed both the strict source-sealed `verify.sh` and the isolated
Maven `verify` lifecycle, including Java21 compilation and correctness
parity. The runner reported `21.0.13+9-LTS`; its logs contain two
independent performance-profile executions.

Median nanoseconds per query, five samples of 4,096 queries each:

| Workload | Strict Java scan / first | Ratio | Maven scan / first | Ratio |
|---|---|---:|---|---:|
| small-cold (no index) | 74.9 / 146.4 ns | 0.51× | 367.8 / 245.7 ns | 1.50× |
| normal-repeated | 750.8 / 19.1 ns | 39.26× | 1,174.0 / 30.4 ns | 38.62× |
| high-cardinality | 1,649.8 / 21.4 ns | 77.01× | 2,160.8 / 23.5 ns | 91.80× |
| equal-hash-adversarial | 1,328.7 / 1,741.7 ns | 0.76× | 1,633.7 / 1,490.0 ns | 1.10× |

**Interpretation:** The warmed optional primitive lookup shows large
improvement relative to this accessor-based public scan in the ordinary
repeated and high-cardinality workloads. Small arrays and hostile equal-hash
inputs are mixed, so the existing no-index path and 32-probe refusal/fallback
remain necessary. No universal speedup is claimed. The reference scan uses
public `Facts.key/value` APIs, so it has extra checks compared with the
old private-array implementation; this is **not an exact original-vs-new
microbenchmark**. The two executions differ materially in baseline
timing, illustrating shared-runner/JIT variability.

This experiment **does not measure** full setup/index-manufacture latency,
GC/allocation, heap/RSS, native JNI transition, cross-platform performance
or realistic multi-source AST datasets. Their admission gates remain open.
No hard speedup ratio is used as a pass/fail criterion.

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
