<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 viewport Java/JNI superset — stacked original-source receiver

This is M3JDK21 itself, the integrated JDK21/JNI SDK. Not a second M3+JDK compatibility layer. Original CPU parent owns dynamic arrays, logical offsets, bit masks, physical ordinals, sorted/filter projections and publication epochs. Native/GPU/browser planes do not own another logical model.

## Reconciliation rather than overwrite
- Stack on M3JDK21 PR #1766 `m3/viewport-frame-lanes-jni-20261021-chatgpt` head `712fda47e962631eddebe5e214b820796c755016`. That branch already implements aligned CPU viewport frames, `M3IndexedListSnapshot.copyPhysicalWindow`, `M3LongSortedViews.copyPage`, frame oracle and its workflow.
- Preserve M3JDK21 PR #1765 exact original `m3maskviewport.c` full-window and page JNI kernel, then append the unchanged #1766 frame JNI function. Java original native declaration retains `project`, `projectPage`, `projectFramePage`. No external JNI wrapper or arbitrary callback crossing.
- Receive #1765's original `M3MaskViewportPageContract` with native parity, plus a three-entry coexistence test. Original #1766 `M3ViewportFrameContract` remains unchanged.
- Canonical reusable Maven/OpenRewrite recipe: [Synexia #12874](https://github.com/hsoliwal/com.synexia/pull/12874), named `com.synexia.m3jdk.ViewportSuperset20261021`, SHA-256 pre/post sealed data, OpenRewrite fixed-point/refusal JUnit, ordered source targets. Fix/refine recipe before rerunning receiver; do not hand-patch generated targets.

## Source and proof
See SOURCE_MANIFEST.tsv. Exact candidate C Git blob `b4d7346eda4df78203b56e8cb11fdab513ad0c29` and Java blob `93d0303f930a4b56765d8baeaeac63bfe03b746d` independently compared to the locally compiled combined C and Java. C17 -std=c17 -Werror and Java21 --release 21 -Xlint:all -Werror passed locally. `nm` verified three original JNI native exports. Local deterministic `-Xcheck:jni` three-export superset 35,000-trial test passed 240,315 assertions and the original page JNI proof passed 150,556 assertions. These local corpora are not mutually independent counts and are not a full JDK or SWT UI acceptance gate.

No native speedup or O(page) total JNI memory claim: `GetIntArrayElements` can copy an entire input array; only the explicit output scratch is page bounded. Defer fixed-size input chunks to a separately qualified atom. Java comparator/predicate callbacks, backing List iteration contracts, filter invalidation, UI focus, renderer themes, language shaping, GPU swap, RWT protocol remain their original owners.

**DRAFT/HOLD**: Maven/OpenRewrite `Recipe.run`, full M3JDK21 build, original module JUnit, CPU page/frame project CI, `jtreg`, Windows/macOS/Android native ABI, SWT/Nebula/GEF/RWT differential and performance not yet executed at this branch head. Preserve both source histories and do not merge until qualified.
