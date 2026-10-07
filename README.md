# M3JDK21

**M3JDK21** is the Java 21 product/runtime target for M3 work led by Hitesh Soliwal and Contributors to the Synexia Project. M3 focuses on reducing redundant CPU work and retained
memory through M3Index/precomputation, mechanical transformation, reusable
recipes, and verification while preserving required JDK behavior and contracts.

**Licensing boundary:** this is an OpenJDK-derived tree. Upstream JDK/OpenJDK
licenses remain authoritative for upstream and derived JDK code. Independently
authored, separable M3/Synexia material is Apache-2.0 only where explicitly
marked. See [M3-SYNEXIA-NOTICE.md](M3-SYNEXIA-NOTICE.md),
[M3-SYNEXIA-AUTHORS.md](M3-SYNEXIA-AUTHORS.md), and
[LICENSE-M3-APACHE-2.0.txt](LICENSE-M3-APACHE-2.0.txt).

## Project goals

M3 aims to make Java workloads do less repeated work: share immutable payloads,
preserve composition and range identity, and reuse indexed metadata and
precomputed results while preserving required behavior and contracts.

M3, M³, MCube — the name leaves room for interpretation. The engineering goals
are explicit.

| Priority | Goal |
| --- | --- |
| **P0 — M3 String, regex, and precompute** | Integrate M3 String into `java.lang.String`; preserve String and regex compatibility; develop shared atoms, ranges, composition plans, hashes, prefix/suffix and substring indexes, and reusable regex plans/caches inside M3JDK21. |
| **P1 — Lean, fast collections** | Reuse proven String/precompute capabilities and indexed signals to reduce repeated searching, copying, allocation, and materialization while preserving collection contracts. |
| **P2 — SWT and Eclipse IDE** | Bring verified runtime and collection improvements into SWT and the Eclipse IDE family through clearly bounded integration. |

**Architecture and ownership:** Synexia is the canonical donor and convergence
workspace for reusable algorithms, transformation recipes, and provenance.
M3JDK21 owns its product/runtime implementation, including String precompute
inside `java.base`, HotSpot, or JNI as appropriate, without a Synexia runtime
dependency. Reusable changes should be developed through recipes, with behavior
and contract checks governing their application.

**What counts as success:** required JDK String, regex, and collection behavior
passes compatibility and regression tests; reproducible benchmarks report CPU
time, allocation, retained memory, and precompute construction/storage costs
against stated baselines. Benefits and regressions must be visible across both
repeated and one-off workloads.

These are project goals, not a declaration that every capability is complete.
Documentation and release evidence must distinguish planned, implemented, and
verified behavior. Performance claims require measured results.

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
