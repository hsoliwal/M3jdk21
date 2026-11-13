<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 desktop viewport coordinate admission (2026-10-17)

This is an **additive, opt-in SDK leaf**, not a new toolkit, widget wrapper, object-residency engine, sorter, renderer or JNI ABI. It extends the existing `m3/ports/swing-viewport` owner from merged M3JDK21 PR #828 and the JDK-owned arithmetic JTree cache from PR #1001. Neither JDK `java.desktop`, `JTree`, `JTable`, `JList`, `RepaintManager` nor any existing production source is modified. Existing SWT/Nebula native viewport ownership is unchanged.

## One row-window grammar, several toolkit owners

`M3ViewportIndexWindow.project(count, first, visible, overscan, target, offset)` writes four `int` lanes into a **caller-owned** dynamic array:

1. `FIRST_VISIBLE`: inclusive first visible **view-row** index
2. `END_VISIBLE`: exclusive end visible **view-row** index
3. `FIRST_RESIDENT`: inclusive overscanned (HOT/resident candidate) index
4. `END_RESIDENT`: exclusive overscanned index

All intervals use zero-based indices and half-open bounds; invalid arguments are rejected before writing to the destination. There is no per-row Entry, row object, native call, or allocation inside `project`. Intermediate sums use `long`, so the entire int-index domain is handled without overflow. A zero-sized viewport publishes an empty resident interval, even if overscan is requested. The kernel does **not** own memory pressure, resource leases, state masks, or row selection: existing `ItemBitSet`/`ItemResidency` and M3Collections index-mask owners are authoritative. Callers compose their flags, filtered and sorted projections with this returned coordinate window.

`M3DesktopViewportAdapters` exposes the same coordinates from **existing** Swing `JList`, `JTable` and `JTree`, plus uniform-height content in AWT `ScrollPane`. These methods refuse calls off their AWT/Swing event dispatch thread. `JXList`, `JXTable`, `JXTree` and `JXTreeTable` are subclasses of those Swing controls; no LGPL SwingX source needs copying and no replacement SwingX widget API is introduced. `copyTableModelRows` uses JTable's `convertRowIndexToModel` so a `RowSorter`/`RowFilter` (including SwingX equivalents) remains the semantic owner; output is primitive `int[]` at a chosen offset. Do not infer view indices equal physical model indices after sorting or filtering. The mapping method requires non-reentrant model state on the EDT.

`M3JavaFxViewport` is an **optional** external OpenJFX 21 dependency, built by `m3/ports/jfx-viewport/pom.xml` rather than shipping JavaFX classes in an OpenJDK 21 image. A JavaFX `ListView`, `TableView` or `TreeView` skin must explicitly supply its actual first and end visible row indices: JavaFX's public control API does not guarantee an exact VirtualFlow window query. The adapter accepts those indices only on the JavaFX application thread, and does no resident work for hidden/unattached controls. `ScrollPane` with uniform-height content may be projected directly from public viewport bounds and normalized scroll position. It does not claim this formula works for variable-height or VirtualFlow-backed controls.

## Problem-category decisions, reuse, and licensing

| Category | Candidate | Decision | Reason / proof | Source/license |
|---|---|---|---|---|
| VISIBLE_INTERVAL_CLAMP | existing M3 viewport `Target(first, visible, overscan)` | **REUSE** | same view-index semantics; no second state owner | M3JDK21 `M3ViewportBinding`, Apache-2.0 |
| INDEXED_WINDOW_BOUNDS | `int[]` + `long` bounded arithmetic | **ADD LEAF** | four integer lanes, no entry objects or writes before validation | first-party Synexia recipe, Apache-2.0 |
| STATE_MASK_STORAGE | existing `ItemBitSet` and M3Collections indexed masks | **REUSE** | already handles ARRAY/BITMAP/RUN masks and JNI parity; do not clone another bitmap implementation | M3JDK21/Synexia, pinned existing owner |
| VISIBLE_ROW_GEOMETRY | Swing JList/JTable/JTree and AWT ScrollPane native/public APIs | **ADAPT** | preserve UI/native ownership and event timing; exact hierarchy/variable row height remains toolkit-specific | OpenJDK `java.desktop`, GPLv2+CPE, no code copied |
| SORT_FILTER_VIEW_TO_MODEL | `JTable.convertRowIndexToModel` | **REUSE** | maintains existing `RowSorter`/`RowFilter`/SwingX semantics | OpenJDK `java.desktop` GPLv2+CPE, no code copied |
| SWINGX_INTEGRATION | JXList/JXTable/JXTree/JXTreeTable subclass adapters | **REUSE** | no LGPL redistribution or duplicated `JX*` class | SwingX 1.6.5-1, LGPL-2.1, behavior/optional dependency only; M3JDK21 `FOSS_REUSE_DECISION.tsv` donor `3dcitydb/swingx-all@94f86a...` |
| JAVAFX_WINDOW | OpenJFX control public APIs and explicit skin-origin range | **OPTIONAL ADAPTER** | avoids unstable internal VirtualFlow classes; external dependency kept out of java.desktop | OpenJFX 21.0.8, GPLv2 with Classpath Exception, no code copied |
| JNI_BATCH | existing M3Collections indexed mask selection | **REUSE** | scalar viewport calculations are too small to justify JNI transitions; JNI stays optional for proven batch thresholds | M3JDK21 collections Java/C/JNI owning modules |
| LEETCODE_HACKERRANK_GFG | binary search, prefix and two-pointer window problem categories | **CATEGORY EVIDENCE ONLY** | reusable algorithm shapes already embodied in existing indexed owners; unlicensed challenge code is not copied | independently catalogue if a measured semantic gap emerges |

The source recipe is `M3DesktopViewportRecipe` in Synexia. It is a deterministic **Maven module** plus a runnable Java 21 source-sealed materializer for new files only; it refuses changes to any existing output. OpenRewrite is retained for transformations of **existing** classes, but is deliberately not invoked just to copy pinned new class resources; no second source-transformation framework or whole-file rollback is introduced. The recipe's positive/negative fixture tests and postimage SHA-256 manifest are reviewable together with generated product files.

## Acceptance scope and open gates

- **Executed locally:** `javac --release 21 -Xlint:all -Werror` for the new Swing/AWT sources and their pure Java headless test, with 20 passed assertions; exact-window and rank mapping under `TableRowSorter` and `RowFilter`. JavaFX adapter syntax was checked using temporary **stubs**, which is **not** an OpenJFX build.
- **Required hosted gates:** recipe two-pass application (created then unchanged), byte-identical `sha256sum`/Git blob of every generated receiver, JDK 21 Maven compile/test of the existing Swing viewport module, actual external OpenJFX 21 compile, SwingX linkage, additional UI-thread/headful/differential parity and performance and JNI batch threshold proof when applicable.
- **Not claimed:** OpenJDK full build or jtreg, SwingX runtime/headful suites, JavaFX native startup/skin/layout parity, AWT heavyweight peer pixel comparison, GTK/Cocoa/Win32/GPU/UI rendering parity, or universal accelerated collection wrappers. Existing collection wrapper source/PR chains remain separate and authoritative.
- **CI failure is not a skip:** if Maven/JFX or original GUI test environments are missing, retain DRAFT/HOLD rather than asserting completion.
