<!-- SPDX-License-Identifier: Apache-2.0 -->
# Bounded viewport pages in the original M3JDK21 indexed mask owner

## Ownership
M3JDK21 is the integrated JDK21/HotSpot/Java/JNI product, never an external
"M3 + JDK" adapter. This changes the existing package-local
`com.m3.collections.M3MaskViewportKernel` and adds only its source-owned
Java21 oracle. It changes no exported JDK/provider API, JNI symbol, source
class name, original reader, or persistent store. Synexia owns only reusable
Maven/OpenRewrite recipes, not a second runtime implementation.

Canonical source-first recipe: https://github.com/hsoliwal/com.synexia/pull/12799
Canonical authoring head: `8526407570a523c673306e72e0403843ce0a9a7f`.
Dataset: `m3-viewport-page-20261021`, named existing
`com.synexia.m3jdk.AstBuildReverse20261017`.
M3 baseline: `a62eacad46b51ce757ded74579f1ad2aabc4d638`.

### Semantic-atom contract
`projectPage(offsets,states,first,length,required,forbidden,skip,max,target,targetOffset)`:
- caller owns source `int[]` and physical/logical index-space IDs, sorting and state bits;
- `skip` is a rank *among matched elements*, not a source array coordinate;
- zero/contradictory masks, signed bit31, duplicate IDs and arbitrary input permutations are admitted;
- one full-window preflight refuses invalid source offsets and insufficient target capacity **before mutation**, even for zero-page requests;
- nonalias destinations allocate no scratch or row objects; source/state alias uses only a selected-page `int[]` scratch;
- preserved encounter ordering gives filtered/sorted/viewer wrappers one primitive bounded page without copying the model;
- time: O(n) preflight plus at most O(n) page scan; retained additional storage O(1), temporary O(k) only for alias.
- concurrent writers, caller's revision/generation fencing, Java predicates/comparators, Java object identity and UI painting remain with the owning model/viewer. No JNI per row.

The original `project`/`count`/`atRank` remain byte-identical. Existing
`M3MaskViewportNative.project` remains the JNI parity oracle without a new
symbol or claimed native speedup. GPU raster and heavy batch kernels remain
separate from CPU/UI-state publication; no GPU change here.

### Executed author-side evidence
- Actual original kernel Git blob: `b4895baf2cf592c35c2281ecc3e52b082d4c290b`.
- Generated candidate Git blob: `5966bffacc730b3ef7102d615a2331a42ae4a22b`.
- New original test Git blob: `bff901c3674649d1ad6eacfb8bd5ba1ecc76816d`.
- Generated source candidate SHA-256: `5af5cb6fed85fa0803384ec4374be4b96659844bd547788be8805bbfe942635f`.
- Contract SHA-256: `3a17cb6e0cebabdd77181bf102a558a489642cfc9945b97c6a85950162cc3a25`.
- Strict `javac --release 21 -Xlint:all -Werror` with the original
  `M3MaskViewportNative.java` declaration: PASS.
- Java differential against incumbent full-window `project`:
  **121,831 assertions PASS** over 40,000 seeded cases + boundaries
  (`existingJni=false`).

### Current draft gate
The focused workflow separately runs that exact source-backed Java21 oracle
and source pins at the PR head. The full source-built M3JDK21/HotSpot/JNI,
Maven/OpenRewrite Recipe.run and receipt receiver, owning Maven/JUnit,
source-generated ABI, GPU raster pipeline, Eclipse/JFace/SWT/OSGi UI-thread,
GPU/native/multi-platform/performance and entire five-library API gates
remain NOT_RUN in this authoring environment. Maven is unavailable.
Full Eclipse/Claude-marked viewer conversion is not claimed.

Before promotion: run actual five-test named OpenRewrite SDK, invoke the
unchanged recipe receiver against a fresh target checkout, demand zero-write
second replay, run original Maven unit tests, run the optional JNI oracle under
`-Xcheck:jni` with an actual built existing library, check old JNI ABI,
run JDK image tests and Problems View/Tree/Table/Search differential tests.
If a gate fails, change the canonical recipe and rerun rather than manually
editing product-generated outputs. Preserve historical failure receipts.

### Reproduction of the standalone Java proof
```sh
mkdir -p /tmp/m3-page-classes
javac --release 21 -Xlint:all -Werror -d /tmp/m3-page-classes \
  m3/collections/src/main/java/com/m3/collections/M3MaskViewportKernel.java \
  m3/collections/src/main/java/com/m3/collections/M3MaskViewportNative.java \
  m3/collections/src/test/java/com/m3/collections/M3MaskViewportPageContract.java
java -cp /tmp/m3-page-classes com.m3.collections.M3MaskViewportPageContract
```
