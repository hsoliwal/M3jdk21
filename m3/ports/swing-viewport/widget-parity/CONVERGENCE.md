<!-- SPDX-License-Identifier: Apache-2.0 -->
# SWT and Nebula widgets in native Swing / SwingX

Goal: offer the *same widget types and features* in Swing, not merely share
viewport algorithms. Inventory and classify all 53 top-level Nebula widgets
modules seen on 2026-11-18: see NEBULA_WIDGET_PARITY.tsv. Presence of an
M3 generated web component is not proof of Nebula behavioral parity.

## Rules

1. Reuse stock javax.swing controls, models, renderers, UI delegates, events,
   LookAndFeel and accessible contexts before adding anything.
2. Reuse SwingX JXTable/JXTree/JXTreeTable/JXDatePicker/JXMonthView/JXTaskPane/
   JXMultiSplitPane/JXMultiThumbSlider and related widgets, preserving SwingX's
   separate LGPL-2.1 licensing. No SwingX source goes into M3 Apache files.
3. Reuse M3JDK21's **existing** packed/virtual model and viewport owners
   (M3ViewportBinding, M3ViewportExtent, M3ViewportPlanes,
   M3DesktopViewportAdapters, M3WebComponent). No parallel DOOP,
   canvas, scroll, damage, residency or render framework is justified.
4. Where a Nebula widget has no real native Swing equivalent, implement an
   additive Swing JComponent. No per-row peer/widget/object graph. Preserve
   event-thread, model, column, selection, focus, editing and accessibility.
5. Do not widen int-indexed Swing APIs to long. Long logical indices need
   separately named additive M3Long* APIs backed by the same M3 core.
6. Only after differential tests should general primitives converge into
   JDK java.desktop owners. Existing JDK 21 source/binary/API/behavior remains.

## First concrete parity leaf

Nebula TableCombo supplies a text/arrow control with a multi-column popup
table; Nebula CTreeCombo supplies a text/arrow control with a popup tree.
JComboBox does not support these capabilities out of the box without replacing
its popup behavior.

This leaf implements, in the existing Swing viewport module:

- jdk.m3.swing.widgets.M3TableCombo: accepts a TableModel or caller-owned
  JTable/JXTable. Selection exposed in *model row indices*, with the existing
  sorter/filter, column model, renderers, cell editors, table UI and data
  model preserved. The display-model-column coordinate is stable under
  column reordering; hidden/filtered rows cannot be selected.
- jdk.m3.swing.widgets.M3TreeCombo: accepts a TreeModel or caller-owned
  JTree/JXTree and uses existing TreePath selection without copying the tree.
- M3PopupSelector: one native Swing composition atom for both controls:
  read-only/editable text, button, lazily created JPopupMenu/JScrollPane,
  selection ActionListener, configurable popup row count and close policy,
  basic accessible names, Alt+Down/F4/Escape, dispose cleanup and EDT checks.

Both controls detach subscriptions from replaced models/selection models.
A selected table value updates when its model data changes. The tree value
updates on tree-model change. Programmatic selection does not synthesize a
user Selection event. An unopened million-row table combo has no per-row
objects, popup or value reads.

This is a **functional first leaf**, not full SWT/Nebula UI parity. Outstanding:
popup image/color/font decoration, header columns in tree popup, RTL layout,
edit commits, focus/traverse, screen-reader checks, exact native screenshots
and keyboard/pixel differentials on Windows/GTK/Cocoa. See TESTS_TO_RUN.md.
Neither javax.swing nor SwingX nor SWT/Nebula was changed by this PR.

## Next concrete owners

- Grid: implement Nebula's grouped/fixed/pinned columns, frozen planes, row
  spans, editor and selection behavior *within existing JTable/JXTable/
  JXTreeTable owners*, using present M3 viewport index/plane APIs.
- Gallery, Gantt, charts, tiles, Carousel: enrich the existing M3 generated
  component templates and models, never add parallel widget families.
- CDateTime/CalendarCombo: check JXDatePicker/JXMonthView first, then port
  only missing time/date/selection functionality.
- Paperclips: specific attachment-control gap for a future port.
- NetBeans Outline and SwingX tree-table: preserve their independently
  optimized listener, row-height and viewport lifecycle PRs.

## Source locks and licensing

- Nebula TableCombo.java: Git blob 09cc3ae33b87e93bfdadbd0b4a1d88c7adf3a056, EPL-2.0; behavior only
- Nebula CTreeCombo.java: Git blob e6fd319e56ffefec8bf50c21cff2a2d54c10c68a, EPL-2.0; behavior only
- SwingX JXTable.java: Git blob 8c04d3637abfb0810c8db630776051a436e19a97, LGPL-2.1; optional consumer
- SwingX JXTree.java: Git blob 54f250c199554ecd77b38c87d97a9c7dfe216137, LGPL-2.1; optional consumer
- M3 DesktopViewportAdapters.java: Git blob 6d9a2491c7d1de69c5025f423f41584a10372d8a, existing M3 API

The code in this PR is original Apache-2.0 Java, implementing observed feature
requirements rather than copying EPL/LGPL implementation. The canonical
recipe-first, source-sealed OpenRewrite implementation/qualification should
be recorded in Synexia before promotion; no such qualification is asserted.

## Additional native widget: PasswordRevealer

- M3PasswordRevealer reuses one native JPasswordField and JButton rather than
  a custom renderer or copied password buffer.
- HOLD mode reveals only while the button's Swing ButtonModel is armed and
  pressed; TOGGLE mode retains visibility until the next activation.
- Disabling/removing the widget masks the password. Echo-char restoration
  does not read or copy the secret. The public password text owner remains
  JPasswordField; consumers should clear any arrays they obtain.
- Nebula behavioral source lock: PasswordRevealer.java
  1cc3b5f11806dbef1899a21011d7639dec9fb624; EyeButton.java
  79c5e8c7872f4496bffa57bcaf1e402466f41992 (EPL-2.0; no source copied).
- Local Java 21 headless EDT smoke PASS; native mouse/keyboard/contrast and
  screen-reader gate remains OPEN.
