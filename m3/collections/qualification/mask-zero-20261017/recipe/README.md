# Existing mask-family zero-mask refinement

This successor refines the existing `M3LongArrayList`, `M3LongArrayDeque`, and
`M3IndexMaskCompactor` JNI implementation. It adds no collection type, arena,
interner, public ABI, or recipe engine. The receiver is pinned to M3jdk21
`4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`.

The current Java selector already contains the upstream count/defensive-clone
optimization from `b598d98202aa38aafe45308ac8f7dbdc242bb36e`. Its exact source is
admitted and remains unchanged. The original Synexia donor, its original
receiving recipe, the current receiver, and this refinement are separate sealed
stages; historical manifests and their six original postimages remain intact.

## Behavior and ownership

| Owner | Selected refinement | Preserved contract |
| --- | --- | --- |
| `M3LongArrayList.retainByStateMask` | Return zero after validation when the required mask is zero. | Same backing array, all resident payload, size, iterator epoch, live iterators, and state input. |
| `M3LongArrayDeque.retainByStateMask` | Return zero after validation when the required mask is zero. | Same backing array, wrapped head, all resident payload, size, iterator epoch, and state input. |
| Existing native `selectNative` | Allocate the detached result and copy row IDs in the existing 256-element JNI region chunks when the mask is zero. | Validation order, JNI exceptions, explicit native routing, order, duplicates, integer extrema, and detached output. |
| Current `M3IndexMaskCompactor` Java owner | Retain exact current bytes, including upstream optimizations. | No alternate wrapper and no change to current nonzero behavior. |

The Java retain operations become O(1) after their existing validation. Detached
selection remains O(n). Nonzero native count/emit and drift checks are unchanged.
The native branch performs no state payload reads for zero mask, uses no
critical JNI section, retains no caller reference, and adds no native heap
allocation. Concurrent mutation of caller arrays remains unsupported.

## Recipe and source custody

`com.synexia.m3.MaskZeroRefinement20261017` composes the existing class-backed
`M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`. The four
exact provider sources are custody copies from canonical revision
`3aa8e45d93f5fed721fdb64579b655f4a432a308`; no shared provider is modified.

`SOURCE_PINS.json` binds the current receiver's six admitted paths, Git blobs,
before/after SHA-256 values, and manifest hashes. Four Java rows and two text rows
produce five changes: two Java owners, one Java contract test, the existing C
kernel, and its existing workflow. The sixth row seals the unchanged selector.
`DONOR_RECEIPT.json` binds the original donor and eleven original recipe
resources to their exact Git blobs. `CATALOGUE.json` records selected and retained
algorithms; `TARGET_POSTIMAGES.tsv` lists the five receiver changes.

The real OpenRewrite SDK tests parse Java to `J.CompilationUnit`, reject parse
failures, verify exact inputs and outputs, reject missing/duplicate/drifted
owners and tampered templates, preserve unrelated paths, reverse input order,
and check a full fixed point before materialization. A separate test replays
the original receiving recipe from its original list/deque preimages and
checks all six historical postimages. Runtime qualification compiles emitted
before/after sources, rather than independently handwritten candidate copies.

## Evidence

`evidence/RESULTS.json` is the runtime receipt; `evidence/sdk-final.log` and the
two JUnit XML files are the real SDK receipt. The SDK passed 4 tests with no
failures, errors, or skips. Each matched runtime variant compiled 99 current
core Java files with Java 21, all lint checks, and warnings as errors. Both
variants ran the same expanded contract test:

| Runtime mode | Checks per variant | Result |
| --- | ---: | --- |
| Java | 19,919 | Passed |
| Actual JNI with `-Xcheck:jni` | 24,300 | Passed |
| Explicit native backend with absent library | 28 | Required refusal passed |
| Library loads but selector symbol is absent | 28 | Required refusal passed |

The missing-symbol case demonstrates that zero-mask `NATIVE_REQUIRED` still
enters the native path. The C build used C17, optimization, strict conversion
and shadow warnings, and warnings as errors. Generated JNI headers and the
single native export were checked. The candidate also passed the actual JNI
test with undefined-behavior sanitization. Stack usage reported by GCC was
2,432 bytes for the JNI entry before and after.

The finite resource probe records 360 samples across lengths 0, 257, and 65,536,
six operations, five rounds, and matched JIT/interpreter processes. It records
CPU time, wall time, allocated bytes, exact owner/backing-array retained bytes,
RSS, and JVM native-memory summaries. All raw samples, including allocation
outliers, are retained. The median allocation for count/retain was zero. At
65,536 entries the detached native output allocated 262,160 Java bytes in both
variants, and the measured list/deque owners plus backing arrays retained
1,572,952 bytes in both variants.

For the 65,536-entry interpreter case, median thread CPU nanoseconds per
operation changed from 4,580,327.25 to 1,634 for list retain, from 13,470,057.25
to 1,531.5 for deque retain, and from 317,335 to 107,070.25 for JNI selection.
The source removes the linear retain work, while the measurements provide
finite corroboration. They are not a universal speed ratio or a JMH result:
the unchanged selected-count control also varied, and RSS/NMT describe whole
processes rather than assigning every native byte to this kernel.

The separate owning collections Maven verification is **unavailable in this
environment** because its pinned
`org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1` artifact is absent from the
offline Maven cache. The exact failure is retained in
`evidence/owning-module-maven.log` and `evidence/OWNING_MODULE.json`. No substitute
artifact or POM change was used. Full JDK images, jtreg/JCK, other operating
systems, and hosted CI are separate gates.

## Reproduction and publication

Use stock Java 21 and a GCC toolchain on Linux. With `JAVA_HOME`, `MAVEN_OPTS`,
and Maven's settings/cache set for the environment:

```sh
mvn -B -ntp -f pom.xml -Dm3.mask.output=/absolute/work/sdk test
python3 verify.py --jdk /absolute/jdk21 --target /absolute/M3jdk21 \
  --sdk /absolute/work/sdk --work /absolute/work/runtime
```

For an authorized exact-preimage receiver, add
`-Dm3.mask.receiver=/absolute/M3jdk21` to the SDK invocation to materialize
the admitted successor results after fixed-point verification. The receiver
used for this qualification already contains those actual SDK emissions.

`PUBLICATION_FILES.tsv` lists the exact canonical files and SHA-256 values.
After that canonical content has an immutable commit,
`materialize_mirror.py --target /absolute/M3jdk21 --canonical-commit <40-hex>`
creates a byte-verified receiver proof mirror and handoff. It requires all six
current target postimages first and writes no production source. The existing
mask receiver workflow validates this custody and reruns the historical and
current SDK proofs, Java contracts, checked JNI, and finite resource probe.
No branch or pull request is created by these scripts.
