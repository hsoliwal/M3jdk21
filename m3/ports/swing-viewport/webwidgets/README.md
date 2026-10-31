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

## Gates

- Swing: `run-tests.sh compile` (javac --release 21 -Xlint:all -Werror) then the JUnit console on `com.m3.swing.web`.
- Nebula: `tools/gate_nebula.sh` (javac --release 21 -Xlint:all -Werror against an SWT jar, JUnit 4 headless model tests).
- Fixed point: run the generator twice; the second run must report `written=0`.

SwingX is LGPL-2.1: behaviour reference only, never copied. Web kits contribute shapes only. See
`FOSS_REUSE_DECISION.tsv`.
