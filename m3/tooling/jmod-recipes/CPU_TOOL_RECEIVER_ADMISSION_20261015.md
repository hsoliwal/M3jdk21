# CPU tool receiver: complete catalogue admission

M3JDK21 target-owned, **Java 21 CPU-only** validation of Synexia's module manifest.
The frozen donor manifest is a **metadata snapshot**, not borrowed implementation code.
It now contains 21 source module entries: the original 18 mapped entries stay
`QUALIFICATION_PENDING`, and signed INT8 quantization is an additional nineteenth
mapped candidate. The other two entries must remain
explicitly pending:
* `com.synexia.m3.model.distill` — CPU model-math provider, separate Java/JNI qualification
* `com.synexia.m3.compute.webgpu` — optional GPU-native Dawn provider, not a CPU dependency

The verifier rejects silent omission, extra/duplicate modules, mismatched target
naming, unexpected third-party license classifications, premature promotion,
unreviewed deferrals and overlarge/noncanonical manifests. It reports a
deterministic, sorted list of deferred capabilities. It never runs modules,
loads native code, edits the JDK image, or grants donor copying rights.

Problem-category comparison: `CPU_TOOL_RECEIVER_ALGORITHM_CATALOGUE_20261015.tsv`.
The keyed-set comparison is O(D + M) expected time/space, not an O(D*M)
repeated pairwise scan. LeetCode 350, HackerRank Sparse Arrays and
GeeksforGeeks hashing serve as problem families only; no solution text is copied.

Synexia is the canonical recipe owner: `com.synexia.rewrite.M3CpuToolReceiverAdmission20261015`,
using `M3HashPinnedJavaSnapshotRecipe` with ABSENT preimages for the two
first-party Java classes. The target retains its copy and test artifacts but
has **no Synexia runtime dependency**. Modify/replay the canonical recipe
for any future generated-source correction; do not hand-patch output.

Reproduce a bounded CPU-only check:
```sh
mkdir -p /tmp/m3-cpu-tool-proof
javac --release 21 -Xlint:all -Werror -d /tmp/m3-cpu-tool-proof \
 m3/tooling/jmod-recipes/src/main/java/com/m3/rewrite/jmod/CpuToolReceiverAdmission.java \
 m3/tooling/jmod-recipes/src/test/java/com/m3/rewrite/jmod/CpuToolReceiverAdmissionChecks.java
java -cp /tmp/m3-cpu-tool-proof com.m3.rewrite.jmod.CpuToolReceiverAdmissionChecks
java -cp /tmp/m3-cpu-tool-proof com.m3.rewrite.jmod.CpuToolReceiverAdmission \
 m3/tooling/jmod-recipes/CPU_TOOL_DONOR_CATALOGUE_PIN_20261015.tsv \
 m3/tooling/jmod-recipes/CPU_TOOL_RECEIVER_MAP_20261014.tsv \
 m3/tooling/jmod-recipes/CPU_TOOL_RECEIVER_DEFERRALS_20261015.tsv
```
Maven build for scoped Java implementation:
`mvn -B -ntp -f m3/tooling/jmod-recipes/pom-cpu-tool-admission-20261015.xml verify`.

Executed locally: an equivalent Java 21 implementation and synthetic refusal
oracle passed 12 assertions. **The exact published GitHub candidate, Maven
and hosted CI have not been executed as part of this source-transfer step**;
full target-owned JMOD rebuild, jlink, real JNI, C1/C2/jtreg and tool API
runtime certification remain **NOT_RUN**. Nothing in this admission
check grants JDK release status or silently includes GPU.

## 2026-10-17 additive quantization receiver

The snapshot now admits the twenty-first Synexia CPU tool,
`com.synexia.m3.model.quantize`, as a **mapped candidate** named
`com.m3.tools.model.quantize`. The original 18 pending
receivers remain unchanged and the independent signed-INT8
candidate is the nineteenth. Two deferrals (CPU model distillation
and optional WebGPU) remain. All receiver entries still say
QUALIFICATION_PENDING; the detached receiving module and JNI
have their own compile/runtime proof. See
`CPU_TOOL_QUANTIZE_RECEIVER_20261017.md`.
