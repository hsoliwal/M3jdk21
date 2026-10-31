# M3JDK21 build-impact propagation from native M3File source/class deltas

This is a product-owned native Java21 source in `com.m3.file`; **M3JDK21 is the SDK**, not an M3 addon to OpenJDK. It intentionally builds on and does not duplicate the original `M3FileDeltaLedger` in M3JDK21 PR #1568.

A single stable physical row index identifies a source, class or build-input file across M3File delta snapshots. M3BuildImpactPlan consumes that snapshot and *compiler-supplied* reverse dependency CSR arrays; it produces a byte-mask reason per slot and a physical-index selection in ascending order. It does not build an independent filesystem, source parser, Maven POM parser or JVM runtime layer.

## Conservative proof boundary
- Unverified or incomplete dependency graph: fail closed by selecting every live source/class physical slot.
- Any changed POM, build or processor input: fail closed until Maven/Gradle's authoritative model and options are proven unaffected.
- Verified complete graph with trustworthy javac/M3AST/JDT semantic/ABI facts: traverse only affected reverse edges in O(V+E) with primitive queue/flags. A changed source always recompiles itself; verified `propagationSafe=1` prevents dependent rebuilding, not the source's compilation.
- Unknown annotation processors, generated sources, reflective ABI dependencies, non-compilation tasks or input changes cannot be treated as safe without a recorded generation-bound authority. This class does not falsely claim to discover these facts.
- Removed slots remain M3File tombstones; no new object handles or JNI references.

## Verified locally
Java 21 `javac --release 21 -Xlint:all -Werror` and M3BuildImpactPlanContract **25 checks passed with an API-compatible M3FileDeltaLedger test shim**. Source/code templates have SHA-256 seals:
- Source 711bf6befa4b91514538ba009c7405af32ee0e0bda10e79c0cfc68e660cc088c
- Contract eb00b149e08e7fc2acf774d7a034a3b74cca7796015526026e5904ce23579ef8

Canonical reusable OpenRewrite recipe: `hsoliwal/com.synexia#12584`, `com.synexia.rewrite.M3FileBuildImpact20261020`. It remains recipe-first and exact-source admittance. Full Maven, real ledger, custom matched JDK and integration builds are not locally executed.

## Product qualification
1. The updated workflow `.github/workflows/m3-file-delta-20261020.yml` compiles the **actual** original M3File/M3FileDeltaLedger plus the new M3BuildImpactPlan and both deterministic contracts under Java21 warnings-as-errors.
2. The workflow runs the M3HashPinnedJavaSnapshotRecipe's LST generation, fixed-point, SHA seals and drift refusal through the dedicated recipe POM.
3. Before integrating `hsoliwal/maven`, `hsoliwal/eclipse.jdt.core`, m2e, Gradle or Eclipse Resources, the respective compiler/resolver/toolchain fact producers must supply generation-matched complete CSR edges, plugin/annotation-processor metadata and build-model identities. No unverified cache reuse.
4. No JNI implementation is invented here; the Java primitive BFS already operates directly on parent-owned arrays. Existing JNI file/mask kernels retain their exact ABI/parity requirements.

This stacked PR targets the M3FileDeltaLedger feature branch, not master until that underlying SHA/delta owner is qualified and admitted. No Maven reactor/JDT incremental engine integration is claimed from this planning atom alone.

## Native Maven/Gradle reactor and original physical file coordinates

The same canonical recipe now receives two additional Java21 atoms: `M3ReactorImpactPlan` and its dependency-free `M3ReactorImpactPlanContract`. Physical file slots remain owned by the original `M3FileDeltaLedger`; no new file hashes, Maven model store, JDT parser, build plugin wrapper or JNI callback engine.

- The reactor kernel receives a complete physical-slot-to-project index plane from the authoritative build model. Downstream project edges are CSR int[] arrays. Its Kahn topological traversal and BFS produce a dependency-ordered impacted project int[] without boxed nodes.
- Missing graph, unknown physical file owner, changed POM/plugin/build input or incomplete compiler ABI proof conservatively invalidates all reactor projects. Cyclic or invalid dependency graphs are refused instead of returning a partial build. No result is cached unless source and dependency generations agree.
- Real compiler, annotation processor, Maven 4 Resolver, m2e/Tycho and Gradle project metadata producers are **not yet wired**. They must supply complete generation-matched models; no plugin behavior is silently skipped.
- Verified locally: **18 additional** Java21 deterministic checks on top of 25 file-plan checks, with a test-only M3FileDeltaLedger API shim and `-Xlint:all -Werror`. CI now compiles and exercises both classes against the original M3File ledger on the exact PR head.
- Canonical recipe has four hash-sealed `ABSENT` targets (two production and two contracts); added source SHA256 `ebb3c7f8d56d578f311a176b06df4aec5af1572f2235b9903e817a32d0e7a328`, contract SHA256 `7a16c118fc4bca14d7bd1994b166fd99f83e333d3176bfed89e0b72661075a51`.
