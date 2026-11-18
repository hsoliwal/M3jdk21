<!-- SPDX-License-Identifier: Apache-2.0 -->
# SWT and Nebula widgets in native Swing / SwingX

Goal: offer the *same widget types and features* in Swing, not merely share
viewport algorithms. Inventory and classify all 53 top-level Nebula widgets
modules seen on 2026-11-18: see NEBULA_WIDGET_PARITY.tsv. These are module
folders, NOT 53 distinct widget classes: some contain printing libraries,
styles, support utilities, and viewport infrastructure. Presence of an
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

- Grid: canonical `java.desktop` now adds `M3Grid extends JTable`, with
  opt-in frozen leading columns and **top/bottom view rows** with pinned
  headers, hit mapping, editing and damage-limited painted planes. See [GRID_PARITY.md](GRID_PARITY.md).
  The existing Swing/SwingX and M3PackedTable owners remain unchanged;
  groups, RTL and long-index widget APIs remain open;
  basic body row/column spans are now source-locked and tested;
  pinned-row behavior is detailed in [GRID_PINNED_ROWS.md](GRID_PINNED_ROWS.md).
- Gallery, Gantt, charts, tiles, Carousel: enrich the existing M3 generated
  component templates and models, never add parallel widget families.
- CDateTime/CalendarCombo: check JXDatePicker/JXMonthView first, then port
  only missing time/date/selection functionality.
- PaperClips is a printing/composition library, not an attachment widget.
  M3PrintPreview reuses Java Pageable/Printable/PrinterJob; full PaperClips
  pagination, layers, text, grid and printer-device parity remain open.
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

## Native Swing wave 2: Oscilloscope and PaperClips PrintPreview

The requested convergence means real Swing controls. Two new first-party
Apache-2.0 classes are in src/main/java/jdk/m3/swing/widgets/, next to
the merged TableCombo and TreeCombo. No SWT GC, shell, native peer, or
parallel viewport engine is introduced.

### M3Oscilloscope

- A native JComponent with N channels in a fixed-capacity int[] ring, next[]
  and used[] state lanes; no per-sample objects or per-channel widget peers.
- Appending a single value is O(1); appending a frame is O(channels), with
  one repaint if showing. Acquisition pause leaves the ring unchanged.
- Caller-owned per-pixel minima/maxima preserve narrow spikes during
  downsampling; painting uses reusable arrays and ordinary Java2D.
- Configurable samples/column, value range, channel colors, grid and
  accessible name. No internal animation or always-on timer.
- Still OPEN: native Nebula dispatcher, pulse, fade/tail/steady/trigger
  controls, platform input/accessibility and exact screenshot parity.
- EPL-2.0 behavioral donor, not copied: Nebula multichannel Oscilloscope.java
  Git blob 91ea525fdd47804a1984491cf6087e813b653402.
- Related Nebula EnvelopeRing.java idea: EPL-2.0 Git blob
  871d33793d04a01e009e445f0649844599f62299; Java implementation is original.

### M3PrintPreview

- Native JComponent built from existing Java Pageable, Printable, PageFormat
  and PrinterJob contracts, not a copied SWT PrintJob or foreign print engine.
- Only the selected page is rendered. A single cached BufferedImage is
  retained only when its size stays within four million pixels.
- Page index, count, zoom, fit-to-width, explicit cache invalidation,
  accessible name, and retained preview failure are supported.
- Still OPEN: PaperClips PrintPiece/PageEnumeration layouts, printer
  color/margin fidelity, multipage layout, async render and native screenshots.
- EPL-2.0 behavioral donor, not copied: Nebula PaperClips PrintPreview.java
  Git blob 8105dda1685d150bfa280fa992c64ec85c55cbf2.

### Executed qualification

- JDK 21.0.12.1, Linux headless: javac --release 21
  -Xlint:all,-serial,-this-escape -Werror: PASS for product and standalone tests.
- M3OscilloscopeSmoke PASS: rings, decimation, two-channel painted pixels,
  pausing, invalid inputs, clearing and no full-history repaint.
- M3PrintPreviewSmoke PASS: actual two-page Book, distinct printed colors,
  cache reuse, page switch, zoom and invalidations.
- M3OscilloscopeScale PASS with -Xmx128m: eight channels, one million
  samples per channel retained, 10 million incoming readings, 1,920 viewport
  envelope bins and exact oldest/newest reading checks.
- All source blob SHA-1 values match the locally tested files.

Full Maven/JUnit reactor, headful GTK/Win32/Cocoa, accessibility and LAF
pixel differentials, and paired canonical Synexia OpenRewrite source
sealed fixed-point qualification remain NOT_RUN. This is a draft code leaf,
not complete Nebula parity or a release. java.desktop, SWT, Nebula and SwingX
production sources are unchanged.

## Native CDateTime text/spinner implementation (third native wave)

Native control: `jdk.m3.swing.widgets.M3DateTime`, one JPanel with
JSpinner, SpinnerDateModel, and Swing's original DateEditor. This is
the text/spinner portion of Nebula CDateTime, NOT the full graphical
date/time selector, calendar popup or SwingX JXDatePicker replacement.

- Exposes canonical millisecond-precision Instant and a distinct ZoneId
  for wall-clock display. setZone changes presentation only.
- setLocalDateTime rejects nonexistent DST gap times and ambiguous overlap
  times: requires exactly one valid ZoneOffset, or a caller can setInstant.
- Inclusive setRange with bounds; private final bounded SpinnerDateModel
  validates even direct model.setValue calls. Date readout is copied so
  callers cannot mutate the internal date by reference.
- commitEdit parses and validates a complete formatted string and inclusive
  bounds prior to updating the spinner model; rejected text leaves the
  canonical Instant and listener notifications untouched.
- Existing Swing editor/document, UI delegate, selection, enabled and
  accessible behavior stays in the owning Swing components. No SWT peers.
- Behavioral donors, no source copied: Nebula CDateTime.java EPL-2.0
  Git blob 6182d7e1926f3568f7cee2ded8480eb84b4a459c;
  Nebula DiscreteTimePicker.java EPL-2.0
  Git blob f794230deb509304f3cc85302e0e18faab605c09;
  SwingX JXDatePicker.java LGPL-2.1
  Git blob 40394a5730e87df65ad53a931f1ada1a7379a5ef.

Local qualification: OpenJDK 21.0.12.1, Linux headless,
javac --release 21 -Xlint:all,-serial,-this-escape -Werror PASS;
M3DateTimeSmoke on EDT PASS (Asia/Kolkata and UTC round trip,
America/New_York spring gap/fall overlap refusal, inclusive range,
SpinnerDateModel mutation defense, text commit and atomic rejects).
Git blob SHAs match locally compiled source and smoke.

OPEN: Nebula SIMPLE and DROP_DOWN graphical calendar/time control
and typed editing equivalence, SwingX JXDatePicker integration, localized
date formats, month/day spinner navigation, alternate LAF, headful
focus/keyboard/accessibility native tests, Maven reactor and Synexia
recipe source-sealed fixed-point qualification. No Java desktop, SWT,
Nebula or SwingX production source was changed.

For merged-cell bounds, model/view-index rules and cross-plane exclusions,
see [GRID_SPANS.md](GRID_SPANS.md).
