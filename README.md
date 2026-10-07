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


## Evidence-first first release

M3JDK21 uses a machine-readable release contract: public claims must be backed by reproducible
build/test/benchmark evidence and SHA-256-sealed proof artifacts. The current release deliberately
distinguishes implemented P0 foundation work from M3 String, regex, compatibility, performance and
later-family claims that still require qualification.

Start here:

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
