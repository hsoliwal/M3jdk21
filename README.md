<div align="center">

# M3JDK21

### Share structure. Reuse computation. Prove the result.

**A Java 21 runtime research and engineering programme**  
M3 String · Regex and precompute · Lean collections · JNI/native boundaries · SWT / Eclipse

[![Java 21 target](https://img.shields.io/badge/target-Java%2021-007396?style=flat-square)](https://openjdk.org/projects/jdk/21/)
[![M3 project](https://img.shields.io/badge/M%C2%B3-research%20%26%20engineering-5b55b2?style=flat-square)](#project-goals-and-plan)
[![Proof first](https://img.shields.io/badge/principle-contracts%20%2B%20evidence-247a55?style=flat-square)](m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md)

**[Start here](#start-here)** · **[Architecture](#architecture-at-a-glance)** · **[Eclipse ecosystem](#the-eclipse-desktop-lab)** · **[Open invitation](#an-open-technical-invitation)** · **[Roadmap](#project-goals-and-plan)**

</div>

> [!IMPORTANT]
> **A laboratory, not a marketing benchmark.** M3JDK21 is an OpenJDK-derived experimental Java 21 product/runtime tree. Design targets, branches, and candidate optimizations are **not** proof of compatibility, release readiness, or faster execution. Only reproducible builds, regression results, and measured comparisons qualify a claim. Upstream licenses and the [M3/Synexia provenance boundary](M3-SYNEXIA-NOTICE.md) remain authoritative.

## Fork compatibility contract for Swing and AWT

M3JDK21 is an optimized, backward-compatible superset of its original OpenJDK 21
baseline. This contract includes Swing and AWT in `java.desktop`: existing
`javax.swing` and `java.awt` public APIs, class names, source and binary usage,
observable behavior, and extension hooks must remain compatible. Existing
fork-specific APIs, features and optimizations are part of the supported
superset and must also be preserved when changes are integrated.

Compatibility is directional: original Java 21 applications must keep working
on the fork, while stock OpenJDK is not required to implement fork extensions.
SwingX and SWT keep their own inherited APIs and contracts; this relationship
does not make those toolkits interchangeable. Release qualification must cover
both the original contracts and additional fork behavior on the built runtime.

## Start here

**The problem:** modern Java workloads repeatedly scan, copy, allocate, hash, parse, and reconstruct information that may already have a safe reusable representation.

**The idea:** canonical immutable payloads, ranges, atoms, compositions, indexed sidecars, and prepared execution plans can let the runtime **do work once and reuse the result**—when their construction, retention, and synchronization costs justify it.

**The rule:** optimize nothing by breaking Java semantics. Equality, ordering, Unicode, concurrency, lifecycle, native interfaces, reflection, and public contracts remain part of the proof.

| Follow the engineering | Open the source or evidence |
| --- | --- |
| **M3 String** — canonical text, views and compositions; String/JNI integration target | [M3 String migration and lineage](m3/docs/m3string-m3-lineage.md) · [String porting invariant](m3/docs/m3jdk21-porting-invariant.md) |
| **Regex and precompute** — planned reusable indexes, caches and bounded metadata | [Regex and text contracts](m3/docs/string-text-replacement-spec.md) · [Compatibility/benchmark spec](m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md) |
| **Collections and arrays** — lean storage, indexed signals and mechanical improvement | [Collections workstream](m3/collections/) · [Array workstream](m3/arrays/) |
| **Recipes and convergence** — transformations executed in Synexia; exact results received here | [Canonical Synexia recipes](https://github.com/hsoliwal/com.synexia/tree/develop/synexia-openrewrite-recipes) · [Receiving policy](AGENTS.md) |
| **The actual runtime** — OpenJDK-derived Java 21 sources and M3 integration areas | [JDK sources](src/) · [M3 runtime integration](m3/runtime-integration/) |
| **Release discipline** — what is admitted, tested, measured or still exploratory | [Release evidence](m3/release/README.md) · [Visible release queue](https://github.com/hsoliwal/com.synexia/tree/develop/synexia-openrewrite-recipes/recipe-crates/m3jdk21-release-admission-20261029/revisions/20261117/queue) · [Machine-readable contract](m3/release/release-contract.json) |

<!-- fork-homepage-guide:begin -->
## A first experiment: learn the representation before the runtime

This fork starts from [OpenJDK 21](https://github.com/openjdk/jdk21), then adds
M3JDK21-owned libraries, runtime candidates, product tests and receipt references.
Reusable recipes are authored and executed in Synexia. The text foundation now
lives in the JDK source layout under `src/java.base/share/classes/jdk/m3/text`.
You can inspect its ownership model before building the modified runtime.

### Six concepts to keep separate

| Concept | Meaning in this tree | Important boundary |
| --- | --- | --- |
| Payload | The immutable character storage owned by a piece | A view can keep the whole backing alive |
| Range/view | A start and length into that storage | Coordinates are UTF-16 code units, not code points |
| Composition | A directory of immutable leaf ranges | Sharing characters still allocates directory objects |
| Sidecar/precompute | Metadata or prepared work associated with data | Construction, retention and synchronization have costs |
| Recipe/receipt | A reproducible transformation and its exact source/target record | Custody and transformation checks do not prove runtime behavior |
| Matched image | Java classes, HotSpot and native pieces built together | A library JAR or unrelated stock VM is not this artifact |

### Quickstart: the current JDK build

Follow the host prerequisites and platform instructions in the existing
[OpenJDK build guide](doc/building.md). From the checkout root, configure a full
JDK 21 boot installation and build the runtime image:

```sh
git clone https://github.com/hsoliwal/M3jdk21.git
cd M3jdk21
git rev-parse HEAD
export M3_BOOT_JDK=/absolute/path/to/jdk-21
"$M3_BOOT_JDK/bin/java" -version
bash configure --with-boot-jdk="$M3_BOOT_JDK"
make images
```

A completed image build places the runtime under `build/<configuration>/images/jdk`.
Record the exact configure options and test results for that image. Optional
native components need their own dependencies and qualification; a component
disabled in one configuration remains outside that configuration's proof.
The current release status and platform gaps are recorded in the canonical
Synexia queue linked above.

The former `m3/build.sh` quickstart still expects the historical `m3/core/src`
layout and is not the current build route. Do not infer a working standalone
`jdk.m3.text` JAR from those older instructions. The source example below explains
ownership and UTF-16 ranges; it is not a verified JShell session or a public
module-export guarantee.

```java
import jdk.m3.text.*;
var original = new LocalM3Arena().copyUtf16("ABCDE".toCharArray());
var middle = original.subSequence(1, 4);
var joined = M3StringPiece.join(middle, original.subSequence(4, 5));
System.out.println(middle.flatten()); // expected: BCD
System.out.println(joined.flatten()); // expected: BCDE
```

The original is copied into owned immutable storage. The two ranges share that
storage; the join records leaves; each `flatten()` explicitly allocates a normal
String. Follow [LocalM3Arena](src/java.base/share/classes/jdk/m3/text/LocalM3Arena.java),
[M3StringPiece](src/java.base/share/classes/jdk/m3/text/M3StringPiece.java), and the
[worked source tour](m3/docs/first-session-guide.md) to see where each cost occurs.
The comments are source-derived expectations, not captured output from this documentation update.

### Command and evidence cheat sheet

The current [clean-output directive](AGENTS.md) keeps new release queues, logs,
receipts and intermediate output in the canonical Synexia repository. Existing
`m3/release/queue/` snapshots are historical inputs; do not create new files there.
Before using the report command below, replace this absolute path with your
Synexia checkout. Run the checker from the M3JDK21 checkout root.

```sh
export SYNEXIA_RELEASE_QUEUE="/absolute/path/to/com.synexia/synexia-openrewrite-recipes/recipe-crates/m3jdk21-release-admission-20261029/revisions/20261117/queue/current"
```

The evidence manifest and its relative artifact paths belong to that external
queue. Missing or stale evidence remains pending; creating a report does not
qualify a release. The reviewed queue snapshot is linked above, while `current/`
is the location for the next source-bound checker run.

| Question | Command or route | What to read in the result |
| --- | --- | --- |
| Which checkout is this? | `git rev-parse HEAD` | Record the exact revision with every result |
| Are package/class names mapped correctly? | `python3 m3/tooling/name-contract/check.py` | The canonical map and queued proposals; this does not approve a rename |
| What is blocking release? | `python3 m3/release/verify_release_admission.py --root . --evidence "$SYNEXIA_RELEASE_QUEUE/evidence.json" --json-out "$SYNEXIA_RELEASE_QUEUE/admission.json" --queue-out "$SYNEXIA_RELEASE_QUEUE/README.md"` | Exact inspected revision, missing receipts, naming candidates and custody blockers |
| Where are recipe proofs run? | [Synexia release-admission crate](https://github.com/hsoliwal/com.synexia/tree/develop/synexia-openrewrite-recipes/recipe-crates/m3jdk21-release-admission-20261029) | Maven/OpenRewrite execution, refusal and fixed-point proofs remain in Synexia |
| How do I build the runtime? | [OpenJDK build guide](doc/building.md) | Host prerequisites, boot JDK, configure options and matched-image output |
| What has actually been admitted? | [Release contract](m3/release/release-contract.json) and [current String phase](m3/m3-import/current-full-borrow/STRING_PHASE.md) | Exact scope and still-open gates |

At the reviewed baseline, the release contract records the immutable text
foundation as implemented with a repository test surface. It explicitly does
not claim full String replacement, full regex precompute, full jtreg compatibility,
qualified arrays/collections/Eclipse integration, or performance superiority.
Candidate source can exist without those gates being complete. Read the exact
commit-bound receipts in the [canonical Synexia release queue](https://github.com/hsoliwal/com.synexia/tree/develop/synexia-openrewrite-recipes/recipe-crates/m3jdk21-release-admission-20261029/revisions/20261117/queue); this guide grants no claim upgrade.
The historical `m3/recipes/` scripts and recipe-bearing `m3/pom.xml` are
inventoried migration residue, not supported product transformation commands.

<!-- fork-homepage-guide:end -->

## Developer reading and verification routes

For a hands-on source tour and command cheat sheet, see the [first-session guide](m3/docs/first-session-guide.md).

Start with the route you are changing; these are different artifacts and evidence scopes.
For your first checkout, use the [build prerequisites and output checklist](CONTRIBUTING.md#first-build-prerequisites-outputs-and-test-selection)
to select the shell, JDK variables, logs and matching test configuration.

| Task | Entry point | What it establishes |
| --- | --- | --- |
| Inspect the current text foundation | [Text source owners](src/java.base/share/classes/jdk/m3/text/) | Source-level ownership examples; access follows the built image's module exports |
| Build or inspect the modified runtime | [Native build guide](m3/docs/native-build.md) and [runtime integration](m3/runtime-integration/README.md) | A matched Java/HotSpot image is required; feature gates remain explicit |
| Trace String ownership or JNI | [Backing contract](doc/mindex-string-backing.md) and [JNI ingress/egress](m3/docs/mindex-jni-string-ingress.md) | Canonical owners, compatibility copies and native pointer lifetimes |
| Work on separate collections | [Collection module](m3/collections/README.md) | Explicit library APIs; this does not replace `java.util` |
| Port or document a donor enhancement | [Contributor checklist](CONTRIBUTING.md) and [canonical porting invariant](m3/docs/m3jdk21-porting-invariant.md) | Recipe authority, exact provenance and target acceptance |
| Run upstream tests | [OpenJDK testing guide](doc/testing.md) | Harness configuration and test selection; presence of a workflow is not a passing run |

The receiving order remains **String → arrays → collections → AST/compiler → remaining families**.
Documentation, copied fixtures and successful donor tests do not advance that order. Read the
[current String phase packet](m3/m3-import/current-full-borrow/STRING_PHASE.md) alongside
its machine-readable gate records. Historical guides retain their original evidence scope;
use the current source and exact-commit results to determine what is available.

## Architecture at a glance

```mermaid
flowchart TB
  A["Synexia: algorithm, data-structure and recipe convergence"] -->|"qualified, attributed transfer"| B["M3JDK21: Java 21 product / runtime owner"]
  B --> C["M3 String: shared payloads, atoms and ranges"]
  B --> D["Indexed precompute: hashes, plans and regex"]
  B --> E["Lean arrays and collections"]
  C --> F["Java compatibility: java.base / HotSpot / JNI"]
  D --> F
  E --> F
  F --> G["Eclipse desktop integration lab"]
  G --> H["SWT / native UI"]
  G --> I["Eclipse Platform / Platform UI"]
  G --> J["Nebula / GEF Classic"]
```

**Architectural boundary:** Synexia is the canonical **donor/convergence workspace** for algorithms, recipes and provenance. **M3JDK21** owns its runtime implementation, including M3 String precompute inside java.base, HotSpot or JNI as appropriate; it does **not** acquire a Synexia runtime dependency. Desktop forks are separate consumers and evaluation targets, not proof of completed integration.

## The Eclipse desktop lab

Real desktop code is a harder test than a synthetic microbenchmark. M3's planned P2 evaluation uses the following **public, upstream-derived forks** to examine CPU work, retained memory, event-loop responsiveness, UI/native boundary costs, and compatibility.

| Fork | Engineering surface | Where to look |
| --- | --- | --- |
| **[SWT](https://github.com/hsoliwal/eclipse.platform.swt)** | Native-backed widgets, Display, event dispatch, resource lifecycle and JNI-facing workloads | [Source and fork direction](https://github.com/hsoliwal/eclipse.platform.swt) |
| **[Eclipse Platform](https://github.com/hsoliwal/eclipse.platform)** | Workspace resources, jobs/scheduling, debug, team/merge and platform services | [Source and fork direction](https://github.com/hsoliwal/eclipse.platform) |
| **[Eclipse Platform UI](https://github.com/hsoliwal/eclipse.platform.ui)** | RCP/IDE workbench, UI state, selection and responsiveness | [Source and fork direction](https://github.com/hsoliwal/eclipse.platform.ui) |
| **[Nebula](https://github.com/hsoliwal/nebula)** | Rich SWT widgets, data presentation and rendering workloads | [Source and fork direction](https://github.com/hsoliwal/nebula) |
| **[GEF Classic](https://github.com/hsoliwal/gef-classic)** | Draw2D, graphical editors, diagrams and graph workloads | [Source and fork direction](https://github.com/hsoliwal/gef-classic) |

These forks retain **their own upstream project identity, authors, license and contributor processes**. Their inclusion on this map is a statement of experimental direction, **not** a claim that M3 optimizations are already verified or upstream-approved.

## An open technical invitation

> **Want to see Java and JNI, shared strings, regex, precompute, collections, SWT and real IDE code? Come to the repositories.**  
> Challenge a design decision. Trace a code path. Bring a counterexample, a profiler trace, a reproducible workload, or a smaller and better algorithm. Let's discuss **real engineering in public**, where the source and the evidence can disagree with anyone's intuition—including ours.

- **Inspect the implementation:** [OpenJDK tree](src/) and [M3 workstreams](m3/); follow actual commits and PRs, not headline claims.
- **Start with the design:** [M3 technical paper](m3/papers/M3_SHARED_STRUCTURE_AND_REUSABLE_COMPUTATION.md) and [compatibility benchmark plan](m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md).
- **Bring an experiment:** provide JDK version, OS/CPU, exact command, baseline, inputs, repetitions, CPU time, allocation, retained memory, and regressions.
- **Contribute with boundaries:** [open issues](https://github.com/hsoliwal/M3jdk21/issues) or [propose a pull request](https://github.com/hsoliwal/M3jdk21/pulls); preserve licenses, attribution, and JDK semantics.

**The invitation is simple: look at the code, question the assumptions, and help prove or disprove the result.**

---

## The engineering premise

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
