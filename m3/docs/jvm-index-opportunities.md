# MIndex opportunities across the JVM

Status: proposed experiments, not delivered runtime optimizations or measured speedups.
This extends `counterpart-naming.md`; it introduces no second registry or payload owner.
The existing VI, CB, CI and PC counterparts are the association boundary. All new names
must first pass the existing mapping and owner review.

The main opportunity is to share immutable facts and reuse completed work at a precisely
validated version. Class loading, linking, reflection and compilation can consume these
facts at runtime. Java-visible identity and execution semantics remain unchanged.

## Candidate order

| Priority / area | Proposed use of existing MIndex capabilities | Preserve / invalidate | Main measurement |
| --- | --- | --- | --- |
| 1. Symbols and descriptors | Canonical name atoms; compact symbol-ID lanes; reuse parsed field/method descriptor facts | Exact spelling, modified UTF-8 versus Java UTF-16 boundaries, bootstrap and unloading lifetimes | Symbol bytes, allocations, lookup CPU, contention |
| 1. Class-file parsing | Index constant-pool entries, attributes, member offsets and bytecode ranges over one immutable definition | Class-file bounds and validation; agent-transformed bytes; class-file version; malformed input errors | Parse CPU, startup time, retained metadata |
| 1. JAR/module/resource discovery | Compact ordered resource indexes and prepared lookup plans over exact artifacts | Delegation order, multi-release JAR selection, signature/sealing checks, custom-loader behavior, resource/provider order | Opens, reads, decompression, discovery latency |
| 1. Reflection metadata | Prepared annotation, generic-signature, member and hierarchy facts; caller-owned API projections | Access checks, module context, defensive-copy and ordering contracts, definition/access epochs | Reflection startup, allocation, retained metadata |
| 1. CDS / AppCDS | Admit shareable immutable MIndex metadata into the existing archive lifecycle | Archive/VM compatibility; no live cross-process Class, loader or raw pointer identities | Cold start, proportional/shared memory, page faults |
| 2. Resolution and type relations | Dense loader-local member indexes and budgeted subtype/interface facts over shared structural data | Defining loader, access checks, initialization, loading constraints, new subtypes, hidden classes, unloading | Link CPU, cache misses, invalidation cost |
| 2. Verification | Reuse structural bytecode/control-flow facts, then validate the current linkage context | Verifier rules, exact definition, dependency/access assumptions; untrusted metadata is never proof | Verification CPU and memory, rejection parity |
| 2. Method handles / dynamic linkage | Reuse signature adaptation plans and dependency facts within existing owners | Lookup privileges, bootstrap execution semantics, mutable call sites, SwitchPoint invalidation | Linking/allocation cost and steady invocation cost |
| 2. JIT analysis | Version method/bytecode facts, dependency graphs and suitable compiler analyses; nominate reusable work | Speculative assumptions, profiles, dependency invalidation, deoptimization, GC maps, compiler revision | Compile CPU, warmup, code size, deopts, throughput |
| 2. Diagnostics / JFR / stack metadata | Shared symbol IDs and prepared method/line indexes; materialize strings at required boundaries | Correct definition epoch, stack shape, line numbers and output contracts | Event bytes, allocation and symbolization CPU |
| 2. JNI | Prepared signature/conversion plans and bounded primitive-lane bulk operations in existing JNI owners | Modified UTF-8, local/global reference lifetimes, unloading, exceptions, copying/pinning costs | End-to-end cost including conversion and transfer |
| 3. VM worklists and GC metadata | Evaluate packed lanes, sparse/dense bitmaps and segmented collections against existing native owners | Barriers, safepoints, relocation, concurrent publication, allocation restrictions, collector-specific semantics | Pause tails, mutator cost, memory, contention |
| Separate JVM project: persistent executable code | Investigate context-validated AOT/JIT artifacts and installation through VM code-cache owners | CPU/OS ABI, VM build, compiler/GC configuration, relocation, dependencies, deopt and stack maps | Startup plus steady-state cost and invalidation behavior |
| Separate JVM project: representations and replacement | Explore new object/class-file layouts or broader live class evolution only with explicit semantics | Object identity, monitors, GC/JNI/JVMTI contracts, active frames, serialization and tooling | Full compatibility and VM correctness before performance |

These are hypotheses about where Synexia mechanisms may help. HotSpot already has indexed
constant pools, symbol interning, resolution caches, tiered compilation and optimized native
structures. An additional index must eliminate measured work or storage; it must not merely
duplicate a cache. Collections inside HotSpot require native representations and lifecycle
review, not a Java collection call from a GC or bootstrap path.

## Versioning is layered

1. **Shareable definition facts:** exact final definition content, fact/schema version,
   analysis/recipe revision, and the inputs needed for that particular fact. A library release
   label is provenance, not content identity or an automatic compatibility decision.
2. **Runtime interpretation:** actual defining Class/loader identity within one VM, definition
   epoch, relevant module/access context, and exact dependency assumptions. Same names or bytes
   in different loaders do not make their Class objects interchangeable.
3. **Executable artifacts:** the above plus relevant compiler build/options, CPU features,
   OS/native ABI, GC/barrier conventions and code-installation metadata. A content hash alone
   cannot authorize native-code reuse.

Separate immutable structure from resolved references and mutable runtime state. The loader may
attach an admitted counterpart through CB/CI; a global content catalogue shares only immutable
payload/facts. It must not root live classes/loaders, substitute for delegation, or merge type
identities across VMs. Invalidate affected runtime interpretations before publishing a changed
definition or access/dependency context. Class initialization state and arbitrary bootstrap
results are not portable pure facts.

MIndexAST is useful where source/AST provenance is available, particularly `jdk.compiler`.
Normal class files need not carry an AST and may come from other JVM languages. Runtime work
must accept bytecode/IR-derived facts. Optional namespaced class-file attributes or sidecars
can carry validated hints while retaining ordinary class semantics. Arbitrary AST payloads
must not become a new trust boundary or a requirement for loading ordinary classes.

## Admission through mechanical convergence tests

Start with independent Java/JNI prototypes in Synexia, then recipe-controlled ports into the
existing JDK owner. Use OFF / OBSERVE / ENABLED modes and the original operation as fallback.
Keep compatible internal experiments in M3JDK21; maintain a separate experimental project for
representation, identity or linkage semantics that require radical VM changes.

The companion Synexia `synexia-openrewrite-recipes/crates/m3-rct` dummy-project proof explores
bounded source combinations and recipe orders. Its compiler/runtime checks are a useful
recipe gate, not a JVM proof or evidence that every composite recipe has converged.
Add per-candidate fixtures for:

- Same name/different defining loaders; same release/different bytes; dependency and module
  changes; retransformation/redefinition; unloading; hidden, array and primitive classes.
- Reflection and exception parity; malformed classes; cold misses; cache eviction/rebuild;
  concurrent lookup/invalidation and sharing boundaries.
- Interpreter, C1 and C2 behavior; JNI platforms and collectors touched by the change;
  jtreg, actual image builds and workloads appropriate to each owner.
- Exact candidate/seed/pass receipts, compiler diagnostics and minimized counterexamples.
  Distinguish fixed point, cycle, regression and unexhausted search budget.

Retain all distinct candidates and exact evidence. Similar shapes are nominations, not
equivalence. Bound search and residency; avoid unconditional global Cartesian precompute.
Measure cold preparation, warm reuse and invalidation separately. Report CPU, allocations,
metaspace/heap/shared memory, latency tails and output size. Preserving behavior does not itself
prove less work; reaching a fixed point does not prove correctness for untested programs.

## Primary references

- [JVMS 21, class-file format and attributes](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html)
- [JVMS 21, loading, linking, resolution and initialization](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html)
- [JDK 21 CDS guide](https://docs.oracle.com/en/java/javase/21/vm/class-data-sharing.html)
- [JDK 21 HotSpot performance mechanisms](https://docs.oracle.com/en/java/javase/21/vm/java-hotspot-virtual-machine-performance-enhancements.html)
- [JNI 21 functions and lifetime rules](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html)
- [JVMTI 21, transformation, redefinition and obsolete methods](https://docs.oracle.com/en/java/javase/21/docs/specs/jvmti.html)

References establish platform constraints, not evidence of a Synexia speedup. The table is a
design proposal derived from those constraints and the repository's existing counterpart contract.
