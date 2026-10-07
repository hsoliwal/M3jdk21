# M3JDK21

**A Java runtime built around shared structure and reusable computation.**

M3 is based on a simple premise: immutable data can carry more than its contents.
Shared identity, composition, and indexed metadata can provide a basis for
reusing work across operations. M3JDK21 explores how far that approach can go
inside a Java runtime, starting with strings and regular expressions.

The design brings together canonical shared payloads, range and composition
views, and precomputed hashes, indexes, and matching plans. Its promise lies in
reducing repeated scanning, copying, allocation, and computation while retaining
the behavior Java applications depend on. The useful balance between reuse,
construction cost, and retained memory is a central engineering question.

The roadmap begins with **M3 String in `java.lang.String`, regex, and precompute**,
then extends to **lean collections** and **SWT/Eclipse IDE integration**.
M3JDK21 is the Java 21 product/runtime target for this work, led by
Hitesh Soliwal and Contributors to the Synexia Project.

**Read the design paper:** [M3: Shared Structure and Reusable Computation in a Java Runtime](m3/papers/M3_SHARED_STRUCTURE_AND_REUSABLE_COMPUTATION.md).

**Take a closer look:** explore the roadmap below, follow the linked repositories,
and examine the [compatibility and benchmark approach](m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md).
The source, design, and measurements offer a way to assess where this idea is
useful and help shape what comes next.

**Licensing boundary:** this is an OpenJDK-derived tree. Upstream JDK/OpenJDK
licenses remain authoritative for upstream and derived JDK code. Independently
authored, separable M3/Synexia material is Apache-2.0 only where explicitly
marked. See [M3-SYNEXIA-NOTICE.md](M3-SYNEXIA-NOTICE.md),
[M3-SYNEXIA-AUTHORS.md](M3-SYNEXIA-AUTHORS.md), and
[LICENSE-M3-APACHE-2.0.txt](LICENSE-M3-APACHE-2.0.txt).

## Project goals and plan

M3, M³, MCube — the name leaves room for interpretation. The goal is to reduce
repeated CPU work and unnecessary allocation through shared immutable payloads,
composition/range identity, indexed metadata, and reusable precompute while
preserving required behavior and contracts.

The roadmap follows the dependencies of the design. Each milestone pairs an
intended capability with the tests and measurements needed to evaluate it.

| Milestone | Deliverable | Acceptance gate |
| --- | --- | --- |
| **Foundation — inventory and faithful reproduction** | Map existing M3 String, regex, precompute, and recipe implementations to their Synexia sources; record contracts, provenance, gaps, and baseline costs. | Traceable source mapping, reproducible build/baseline, and regression cases for known semantic differences. |
| **P0.1 — M3 String in `java.lang.String`** | Integrate canonical shared payloads, atoms/ranges/compositions, views, and indexed sidecars within M3JDK21; define compatibility materialization and JNI boundaries. | Required String semantics, Unicode, equality/hash/ordering, substring/view behavior, and relevant JDK/HotSpot/JNI compatibility tests pass. |
| **P0.2 — String and regex precompute** | Develop reusable hashes, prefix/suffix and substring indexes, regex plans/caches, and bounded metadata lifecycle. | Differential regression coverage, including at least 10,000 regex cases across text and code; retained-memory and precompute costs measured; concurrency and lifecycle checks pass. |
| **P0.3 — Qualify runtime benefits** | Produce reproducible compatibility and benchmark evidence for String, regex, and precompute. | Compare with stated JDK baselines on repeated and one-off workloads; publish CPU time, allocation, retained memory, startup/construction costs, benefits, regressions, and limitations. |
| **P1 — Lean, fast collections** | Reuse qualified runtime capabilities and indexed signals for collection/search operations through converged recipes. | Collection contracts and differential tests pass; representative benchmarks substantiate reduced work or allocation and disclose tradeoffs. |
| **P2 — SWT and Eclipse IDE** | Integrate qualified runtime/collection improvements into [SWT](https://github.com/hsoliwal/eclipse.platform.swt), [Eclipse Platform](https://github.com/hsoliwal/eclipse.platform), [Platform UI](https://github.com/hsoliwal/eclipse.platform.ui), and relevant [Nebula](https://github.com/hsoliwal/nebula)/[GEF](https://github.com/hsoliwal/gef-classic) targets through bounded adapters and recipes. | Target builds, integration tests, and representative UI responsiveness/resource measurements pass; upstream boundaries remain explicit. |
| **First-release freeze** | Select qualified scope, generate the final evidence pack, and consolidate each unreleased public fork delta for release. | Final tree preserved; exactly one fork-specific release commit above the documented upstream base; notices/provenance retained; branch/tag protection checked; release claims match the machine-readable contract. |

**Ownership:** Synexia owns the canonical recipe/convergence/provenance workspace.
M3JDK21 owns the product/runtime integration and all String precompute inside
`java.base`, HotSpot, or JNI as appropriate, without a Synexia runtime dependency.
SWT/Eclipse targets consume qualified work through their own integration boundaries.

**Execution discipline:** inventory before change; reproduce faithfully; preserve
behavior and contracts; develop and converge reusable recipes in Synexia; apply
changes additively; qualify each milestone with compiler, tests, and measurements.
Parallel preparation may proceed, but dependent capabilities require the earlier
acceptance gates. Each milestone needs an owner and linked PR/test/benchmark
evidence before it can be reported as verified.

## Explore the work

The release materials describe scope, compatibility expectations, benchmark
methods, and the evidence associated with individual capabilities. They provide
context for reading the implementation and comparing its tradeoffs across workloads.

Explore:

- [First-release evidence pack](m3/release/README.md)
- [Machine-readable release contract](m3/release/release-contract.json)
- [Benchmark and compatibility evidence specification](m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md)
- [M3/Synexia first-party rights and provenance](M3-SYNEXIA-RIGHTS.md)

---

# Welcome to the JDK!

For build instructions please see the
[online documentation](https://openjdk.org/groups/build/doc/building.html),
or either of these files:

- [doc/building.html](doc/building.html) (html version)
- [doc/building.md](doc/building.md) (markdown version)

See <https://openjdk.org/> for more information about the OpenJDK
Community and the JDK and see <https://bugs.openjdk.org> for JDK issue
tracking.
