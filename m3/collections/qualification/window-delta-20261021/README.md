<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original indexed window: visible dirty delta projection

**This is original in-place M3JDK21 Java21 source** in the existing
`com.m3.collections.M3WindowPlan`, not `M3 + JDK` and not a new runtime
wrapper. M3 collections remain dynamic arrays, logical/physical indexes,
offsets and masks; visible facades are deferred. The public JDK and Eclipse
APIs are unchanged, and no new JNI symbol is introduced.

Canonical Synexia source-first draft:
https://github.com/hsoliwal/com.synexia/pull/12838
at `3647c2c65d0a34125b9ff55f681270ee7947eb27`.
Canonical engine: existing Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe`,
dataset `m3-window-delta-20261021`. See source-sealed
`SOURCE_MANIFEST.tsv` here and the canonical dataset's actual preimage.

## Three reviewable Java targets
- One modified original owner:
  `m3/collections/src/main/java/com/m3/collections/M3WindowPlan.java`
  original blob `de534edb6fcd76ca188b1c61a2d30b7be47c98da`
  -> candidate `d0ce93a6e0afec40def2f84aab9a522e8fcc4e3d`.
- New original-owner source differential `M3WindowPlanChangeContract.java`;
  candidate blob `ad2750877fb062d3d59a505254d3d8e8d4e95a38`.
- New JUnit 5 discovery wrapper `M3WindowPlanChangeTest.java`;
  blob `6953a9adf993c2f273ab5591321e908f433e43df`.

Only three additive methods are introduced. The original `plan`, `commit`,
`inserted`, `removed`, scroll geometry and public descriptors remain
unchanged. Count and first affected native row for a contiguous model
update use O(1) primitive math; scattered changed row IDs are projected
stably in O(number of changed rows), not full-model repaint. All inputs
are validated before output writes, duplicate source notifications
remain present, offscreen changes produce zero native row entries,
and a temporary page-sized `int[]` exists only for overlapping source
and result arrays.

## Local author evidence
- Candidate/source compilation under genuine OpenJDK 21.0.12.1
  `javac --release 21 -Xlint:all -Werror`: PASS.
- `M3_WINDOW_DELTA_PROJECTION_PASS checks=349502`: PASS, 40,000
  randomized windows + bounded/alias/refusal cases, using exactly the
  source supplied in the canonical candidate (Git blobs verified).
- Exact original preimage compiled independently, and `javap -public`
  retained all 24 baseline dump lines. Three new public methods only.
- JUnit gateway compiled with an annotation *stub* to confirm Java syntax;
  actual owning Maven/JUnit execution **NOT_RUN**.
- No elapsed-time speedup or allocation measurement is claimed.

## Mandatory holds before promotion
The existing Maven/OpenRewrite named `Recipe.run` five-test SDK,
actual generated Result receipt and source receiving/replay, current
m3/collections Maven/JUnit, full M3JDK21 source-built java.base
and HotSpot/jtreg/JCK, real SWT/JFace/Workbench/Problems View behavior,
three native SWT platforms, JNI/GC/IME/accessibility/pixel differential,
CPU/GPU raster, performance and memory-pressure gates are **NOT_RUN**.
This source candidate is reviewable but **DRAFT/HOLD** pending actual
recipe emission and owning acceptance, never a declaration that all
109 source-derived capability families are implemented.

### Reproduce the narrow real Java 21 source proof
```sh
mkdir -p /tmp/m3-window-change
javac --release 21 -Xlint:all -Werror -d /tmp/m3-window-change \
  m3/collections/src/main/java/com/m3/collections/M3WindowPlan.java \
  m3/collections/src/test/java/com/m3/collections/M3WindowPlanChangeContract.java
java -cp /tmp/m3-window-change com.m3.collections.M3WindowPlanChangeContract
```

**Ownership**: This belongs to the CPU model/index side. The UI
thread applies only accepted resident rows after owner/generation
validation. GPU can rasterize batches only when measured to be useful.
No arbitrary Java predicates or native widget handles cross JNI in
this leaf. Reuse original `M3ListDiff`, `M3Patch`, M3 state/index
owners and compatible Eclipse viewer contracts.
