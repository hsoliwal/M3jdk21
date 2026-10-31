<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# M3 web widgets: every web UI concept as a viewport widget on Nebula (SWT) and the M3JDK Swing port

Source of the concepts: the 69-kit harvest of com.synexia `d278d4c1c02`
(`docs/M3-SCALE/runs/20261013-all-widget-viewport/universe/`): 6,323 component names, 104 widget concepts.
`CONCEPTS.tsv` lists each concept (29 gaps first, by kit count), its family and pattern, what SWT/Nebula and
Swing/SwingX already have (verified against the SWT jar, the Nebula tree and the JDK 21 `src.zip`), and the decision per
side: `GENERATE`, or `SKIP_EXISTING` with the existing class. `COMPONENT_CONCEPTS.tsv` maps every harvested name to a
concept (tier T1 = the harvest's alias table, T2 = exact-word aliases added here, T3 = non-widget support types,
UNMAPPED = still open).

## Generation rule

spec (`spec/MEMBERS.tsv`, `spec/FAMILIES.tsv`) -> behaviour atoms and one versioned pattern per family
(`templates/family/<FAMILY>.java.tmpl`) -> interfaces (`M3WebApi`, `M3<Family>Api`, `M3<Concept>Api`) -> implementation by
template only. `M3WebGen.java` is deterministic; a second run writes nothing (`GENERATED.tsv` holds every file's template
and SHA-256). No body is written by hand or by a model: a correction is a template change, a regeneration and a row in
`RECIPE_MEMORY.tsv`. Anything a family template does not cover is a `TODO M3P-<FAMILY>-1` in the generated code.

```
java M3WebGen.java <gen> <CONCEPTS.tsv> <nebula widgets/m3web | -> <M3jdk21 m3/ports/swing-viewport | -> [FAMILY,...]
```

## Shape of every widget

- **Model (toolkit neutral, identical on both sides):** `M3WebLanes` (DOOP lanes: flags, value, aux, group, rects as int
  arrays; texts are the only objects) -> `M3WebView` (viewport aspects) -> `M3WebModel` (keys, pointer, RTL mirror,
  theme, display list) -> `M3<Family>Model` -> `M3<Concept>Model` (configuration only).
- **Viewport aspects** (ALL_WIDGETS_VIEWPORT_PLAN section 1), declared per family by `aspects()` and proven by
  `M3WebChecks.viewport`: R resident window (`setLogicalCount(1_000_000)`, resident count bounded by the viewport),
  G logical geometry (`long` extents and scroll offset, scroll bar projection), D damage only (an item change damages its
  rect; a short scroll is a blit plus the exposed strip), P planes (`movePlane` damages only the plane's old and new bounds).
- **Display list:** `M3WebPaint` packed int records; the SWT side (`M3WebCanvas`, one Canvas, GC only, `Control.scroll`
  for blits, `SWT.PartVisible`/`PartHidden` when the SWT build has them, else Show/Hide) and the Swing side
  (`M3WebComponent`, retained body plane, `copyArea` blits, HierarchyEvent SHOWING_CHANGED as part-visible) replay it.
- **Theme:** light/dark palettes over the corpus `--vs-*` slots; variants x sizes x states resolved in ints.
- **Accessibility:** ARIA role per concept, mapped to `ACC.ROLE_*` and `AccessibleRole`.

## Families

See `spec/FAMILIES.tsv` (pattern, aspects, behaviour atom, members, SwingX pattern reference, web kit shapes).

## Viewport adapters of existing widgets (phase A, pattern M3P-ADAPTER-1)

Every widget a toolkit already ships for a concept (`spec/ADAPTERS.tsv`: 64 SWT/Nebula, 36 Swing/AWT rows) gets an
opt-in `M3ViewportAdapter` on the same shared core as the web widgets (`M3WebViewportCore`: SWT PR #295 facades /
M3JDK Swing port core). Atoms: measure (`M3ViewportMeasure`, a closed set of kinds in `M3ViewportKind`:
TABLE TREE LIST TEXT STYLED SCROLL COMBO RANGE TABS TOOLS CHILDREN AUTO SURFACE PLANE, `spec/ADAPTER_KINDS.tsv`),
resident window (R), logical extent and thumb (G), damage-only redraw of one item (D), plane moves that damage only
old and new bounds (P). Provided aspects = the family's declared aspects that the kind can provide; the rest are
`n/a` in `spec/VIEWPORT_MATRIX.tsv`. Off until `attach()`: an unattached or detached adapter installs no listener and
never redraws (off == stock, checked against a fresh stock instance). Factories: `M3<Family>Viewports.adapt<Concept>`;
tests: `M3<Concept>Viewport_Test` (Nebula, hidden desktop) and `M3<Concept>ViewportTest` (Swing, headless EDT).
Nebula widgets of other bundles are created by name; without the bundle on the test class path their test prints
NOT_RUN and `M3ViewportFactories_Test` binds their factory on stand-in hosts. `tools/viewport_matrix.sh` writes the
matrix rows from the specs and the test logs.

## Gates

- Swing (self-contained: M3jdk21 files and the JDK only): `run-tests.sh test --select-package com.m3.swing.web` (javac
  --release 21 -Xlint:all -Werror; JUnit Platform/Jupiter compiled from `m3/vendor/third_party`, run by
  `com.m3.swing.testkit.M3JUnitLauncher`). The generator, its templates and these tools are not part of that build.
- Nebula: `tools/gate_nebula.sh` (javac --release 21 -Xlint:all -Werror against an SWT jar, JUnit 4 headless model tests).
- Coverage (target 99% line and branch on the generated code of each side): `tools/coverage_swing.sh` (headless) and
  `tools/coverage_nebula.sh` (model and SWT canvas tests in one JVM on the hidden test desktop through
  `ui_lock.sh` + `hidden_java.sh`); both write CSV + XML only and `tools/coverage_missed.sh` lists every open line.
  A branch no test can reach is a row of `COVERAGE_EXCEPTIONS.tsv` (file:line, missed, reason) on that side.
- Tests per member: model (`core` incl. the TOP sweep, family, edges), component (Swing, headless) or canvas
  (Nebula, desktop); per family: every mode id plus the members the toolkit already ships (`M3<Family>Modes*`), and on
  Nebula every mode on the generic canvas (`M3<Family>CanvasModes_Test`).
- Fixed point: run the generator twice; the second run must report `written=0`.

SwingX is LGPL-2.1: behaviour reference only, never copied. Web kits contribute shapes only. See
`FOSS_REUSE_DECISION.tsv`.
