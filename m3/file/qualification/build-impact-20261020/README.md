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
