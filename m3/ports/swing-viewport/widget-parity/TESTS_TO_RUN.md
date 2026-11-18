<!-- SPDX-License-Identifier: Apache-2.0 -->
# SWT/Nebula widget -> Swing first leaf: tests to run

| Gate | Status as of 2026-11-18 |
|---|---|
| Java 21 compile of the three production classes plus standalone test, -Xlint:all,-serial,-this-escape -Werror | LOCAL_PASS |
| Headless EDT smoke: sorted/filtered JTable selection; model/view columns; model and selection-model replacement; model delta; tree paths and changed nodes; lazy popup; million-row model | LOCAL_PASS (M3_NEBULA_COMBO_PARITY_PASS) |
| Existing m3-swing-viewport full JUnit suite at exact PR head | NOT_RUN |
| SwingX JXTable/JXTree actual runtime linkage on fork | NOT_RUN |
| Headful popup: keyboard, mouse, selection, editing, focus, scrolling, accessibility, RTL and LAF | NOT_RUN |
| SWT/Nebula-to-Swing native screenshot/event differential on GTK/Win32/Cocoa | NOT_RUN |
| Canonical Synexia OpenRewrite recipe source seals, fixed-point second run | NOT_RUN |

Local_pass is from stock JDK 21.0.12.1 on a Linux headless JVM. It is not
a claim of native GUI parity or a successful hosted CI build.

Additional local gate: M3PasswordRevealerSmoke on the EDT passed
(M3_PASSWORD_REVEAL_PARITY_PASS) for HOLD/TOGGLE, disarm/disable hiding,
mask restoration and stable JPasswordField password contents. Native input
and accessibility comparisons remain NOT_RUN.

## Native widget wave 2: Oscilloscope and PrintPreview

| Gate | Result |
|---|---|
| JDK 21.0.12.1, headless, javac --release 21 -Xlint:all,-serial,-this-escape -Werror | LOCAL_PASS |
| M3OscilloscopeSmoke: ring, two-channel painted pixels, 4:1 decimation, pause/reset/input bounds | LOCAL_PASS |
| M3PrintPreviewSmoke: actual Pageable Book, red/blue page pixels, caching, page switch, zoom, invalidation | LOCAL_PASS |
| M3OscilloscopeScale: 10M sample stream (8 channels, 1M retained each), -Xmx128m | LOCAL_PASS |
| Committed Java Git blob SHAs match the locally compiled/tested source | VERIFIED |
| Full Maven/JUnit reactor at exact branch head | NOT_RUN |
| Headful SWT/Nebula vs Swing screenshots and behavior on GTK/Win32/Cocoa | NOT_RUN |
| Screen-reader, bidirectional input, alternative LookAndFeel, print device WYSIWYG | NOT_RUN |
| Synexia source-sealed OpenRewrite recipe fixed-point qualification | NOT_RUN |

Standalone scripts, run from the m3/ports/swing-viewport module root:

```shell
javac --release 21 -Xlint:all,-serial,-this-escape -Werror -d /tmp/m3-native \
  src/main/java/jdk/m3/swing/widgets/M3Oscilloscope.java \
  src/main/java/jdk/m3/swing/widgets/M3PrintPreview.java \
  src/test/java/jdk/m3/swing/widgets/M3OscilloscopeSmoke.java \
  src/test/java/jdk/m3/swing/widgets/M3PrintPreviewSmoke.java \
  src/test/java/jdk/m3/swing/widgets/M3OscilloscopeScale.java
java -Djava.awt.headless=true -Xmx128m -cp /tmp/m3-native jdk.m3.swing.widgets.M3OscilloscopeSmoke
java -Djava.awt.headless=true -Xmx128m -cp /tmp/m3-native jdk.m3.swing.widgets.M3PrintPreviewSmoke
java -Djava.awt.headless=true -Xmx128m -cp /tmp/m3-native jdk.m3.swing.widgets.M3OscilloscopeScale
```

## Native CDateTime text/spinner wave

| Gate | Result |
|---|---|
| javac --release 21 -Xlint:all,-serial,-this-escape -Werror | LOCAL_PASS |
| M3DateTimeSmoke: IST/UTC, DST gaps/overlaps, inclusive bounds, defensive Date, text commit and reject | LOCAL_PASS |
| Exact committed source Git blobs versus tested local files | VERIFIED |
| Full Maven/JUnit reactor at exact PR head | NOT_RUN |
| GUI focus/keyboard/accessibility/locale/LookAndFeel tests | NOT_RUN |
| Nebula/SwingX graphical dropdown and date picker differential | NOT_RUN |
| Synexia sealed OpenRewrite recipe fixed-point and release qualification | NOT_RUN |

## Native Grid frozen-column first leaf

| Gate | Result |
|---|---|
| Original Java 21 source compiled with -Xlint:all,-serial,-this-escape -Werror | LOCAL_PASS |
| M3GridContractSmoke: stock/off parity, scrolled body/header pixels, view hit mapping, editing, reordering, sorter/filter, clip/damage isolation | LOCAL_PASS |
| 1M logical rows, far viewport paint, fewer than 140 getValueAt calls under -Xmx128m | LOCAL_PASS |
| Real Swing AWT Robot mouse click under Linux/Xvfb -Xmx128m | LOCAL_PASS |
| Git blobs of M3Grid/ContractSmoke/HeadfulSmoke equal exact local tested source | VERIFIED |
| Existing m3-swing-viewport Maven/JUnit full reactor on exact PR head | NOT_RUN |
| Native Xvfb other LAF/drag/resizing/RTL/HiDPI/accessible GTK/Win32/Cocoa suite | NOT_RUN |
| Full Nebula Grid behavior (nested groups/summary rows, cross-plane spans, long indices) | OPEN |
| Canonical M3DateTime moved from pre-#3313 port with identical source blob | VERIFIED |
| Combined Grid/DateTime Java 21 headless tests | LOCAL_PASS |
| #3313 merged; source blob received in canonical java.desktop | VERIFIED |

To reproduce the executed standalone gates, use the exact commands
in [GRID_PARITY.md](GRID_PARITY.md). The full Maven, jtreg,
source-built M3JDK and cross-toolkit gates remain separate.

## M3Grid pinned top/bottom view rows

| Gate | Result |
|---|---|
| Strict Java 21 Werror compile, source/standalone tests | LOCAL_PASS |
| Existing M3GridContractSmoke frozen-column/headless/1M | LOCAL_PASS |
| New M3GridPinnedRowsSmoke pixels, hit geometry, 2+2 rows, sorter, filter, scroll-mode restore, 1M virtual model | LOCAL_PASS |
| Existing M3GridHeadfulSmoke native frozen-column Robot click on Linux/Xvfb | LOCAL_PASS |
| New M3GridPinnedRowsHeadfulSmoke native top and bottom row Robot click, no body scroll jump | LOCAL_PASS |
| java.desktop --patch-module Werror compile and named-module headless/headful execution | LOCAL_PASS |
| Git blob seals of new production and both standalone pinned tests | VERIFIED |
| Full reactor, source-built JDK/jtreg, GTK4/Cocoa/Win32 and accessibility | NOT_RUN |
| Canonical Synexia recipe fixed point, full Nebula Grid parity | NOT_RUN |

See [GRID_PINNED_ROWS.md](GRID_PINNED_ROWS.md) for design,
exact source seals, reproduction and limits.

## Native Grid merged body cells

| Gate | Result |
|---|---|
| Strict Java 21 javac Werror with canonical Grid source and old/new smoke sources | LOCAL_PASS |
| Old M3GridContractSmoke and M3GridPinnedRowsSmoke | LOCAL_PASS |
| New M3GridSpansSmoke: pixels, anchor selection, model/view contiguous mapping, filtering and structural reset | LOCAL_PASS |
| Two active spans at one-million model rows, including a 300,000-row unsorted span, -Xmx128m | LOCAL_PASS |
| Old Xvfb native frozen-column/pinned-row Robot smokes | LOCAL_PASS |
| New M3GridSpansHeadfulSmoke: actual covered-cell click maps to anchor without viewport scroll | LOCAL_PASS |
| java.desktop patch module strict compile and named-module execution | LOCAL_PASS |
| Exact source Git blob seals for Grid and both new tests | VERIFIED |
| Full jtreg/Maven/JUnit, Win32/Cocoa/GTK4/HiDPI, accessibility, canonical OpenRewrite recipe | NOT_RUN |

See [GRID_SPANS.md](GRID_SPANS.md) for model-coordinate policy,
primitive storage, proof bounds, donor hashes and remaining gates.

## Sparse native Grid row grouping

| Gate | Status |
|---|---|
| Java 21 -Xlint:all,-serial,-this-escape -Werror from canonical Grid source | LOCAL_PASS |
| Patched java.desktop compile and named-module runtime smoke suite | LOCAL_PASS |
| M3GridGroupsSmoke with 1M rows, 2 large collapsed ranges, then 3000 groups at -Xmx128m | LOCAL_PASS |
| M3GridGroupsMappingSmoke: 24K rows, 600 groups, 45 seeded collapse toggles against independent dense oracle | LOCAL_PASS |
| M3GridGroupsSerializationSmoke: collapsed sparse state round-trip | LOCAL_PASS |
| Serialized pre-group M3Grid source read into new M3Grid, then newly collapsed | LOCAL_PASS |
| Existing frozen columns, pinned rows and merged spans headless smoke tests | LOCAL_PASS |
| Four native Swing Robot tests with Linux/Xvfb (frozen, pinned, spans, group disclosure) | LOCAL_PASS |
| Original stock JTable header toggle while sparse sorter installed | LOCAL_PASS |
| Public javap comparison: old methods preserved, group methods additive | LOCAL_PASS |
| Exact Git blob identity for source and four new standalone tests | VERIFIED |
| Full M3JDK jtreg and Maven reactor, GTK/Win32/Cocoa/HiDPI/assistive technology | NOT_RUN |
| Synexia source-sealed OpenRewrite fixed point; nested group/summary feature parity | NOT_RUN |

See [GRID_ROW_GROUPS.md](GRID_ROW_GROUPS.md) for the sparse rank/select
design, source SHA pins, commands, known restrictions and test evidence.

## Native graphical month calendar / CDateTime popup (2026-11-18)

| Gate | Status |
|---|---|
| New M3MonthView and additive M3DateTime code in both java.desktop and swing-viewport | SOURCE_BLOBS_IDENTICAL |
| javac --release 21 -Xlint:all,-serial,-this-escape -Werror | LOCAL_PASS |
| Existing M3DateTimeSmoke: bounds, immutable instant, zone and DST parse | LOCAL_PASS |
| M3MonthViewCalendarSmoke: 42 cells, locale, bounds, 2000 month moves, action and date-time DST atomicity | LOCAL_PASS |
| M3MonthViewHeadfulSmoke: JFrame/JPopupMenu mouse event, date commit, popup close/detach on Xvfb | XVFB_PASS |
| Xvfb repeatability: 3 consecutive X11 headful runs after pre-initializing MouseEvent before EDT | 3_OF_3_PASS |
| Public API javap: 19 baseline signatures retained; seven additive calendar methods | SOURCE_BINARY_SIGNATURE_PASS |
| Direct JSpinner model value changes update popup without reentrant range clamping | HEADLESS_PASS |
| Full Maven/JUnit & OpenJDK java.desktop reactor at exact branch head | NOT_RUN |
| GUI pixel/event differentials on Win32, GTK, Cocoa and SwingX | NOT_RUN |
| Accessibility screen-reader, RTL, and advanced Nebula CDateTime styles | NOT_RUN |
| Canonical Synexia OpenRewrite sealed source recipe fixed-point | NOT_RUN |

From m3/ports/swing-viewport root with JDK 21:

```sh
mkdir -p /tmp/m3-monthview
javac --release 21 -Xlint:all,-serial,-this-escape -Werror -d /tmp/m3-monthview \
  src/main/java/jdk/m3/swing/widgets/M3DateTime.java \
  src/main/java/jdk/m3/swing/widgets/M3MonthView.java \
  src/test/java/jdk/m3/swing/widgets/M3DateTimeSmoke.java \
  src/test/java/jdk/m3/swing/widgets/M3MonthViewCalendarSmoke.java \
  src/test/java/jdk/m3/swing/widgets/M3MonthViewHeadfulSmoke.java
java -Djava.awt.headless=true -Xmx128m -cp /tmp/m3-monthview jdk.m3.swing.widgets.M3DateTimeSmoke
java -Djava.awt.headless=true -Xmx128m -cp /tmp/m3-monthview jdk.m3.swing.widgets.M3MonthViewCalendarSmoke
xvfb-run -a java -Djava.awt.headless=false -Xmx128m -cp /tmp/m3-monthview jdk.m3.swing.widgets.M3MonthViewHeadfulSmoke
```
