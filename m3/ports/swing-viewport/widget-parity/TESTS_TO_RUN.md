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

## Oscilloscope all-MAX_VALUE bin correctness fix

| Gate | Status |
|---|---|
| Exact baseline canonical source ac244200338d5ffde09dcd1004f7ebdf81bba743 vs new M3OscilloscopeMaxEnvelopeSmoke | RED: AssertionError, all MAX_VALUE envelope skipped |
| One-atom condition: both min=MAX and max=MIN are necessary for an empty bin | FIXED |
| Java 21 Werror compile and headless MAX_VALUE pixel test | GREEN: PASS |
| Original M3OscilloscopeSmoke | GREEN: PASS |
| 10M samples, 8 channels x 1M retained, -Xmx128m | GREEN: PASS |
| Patched java.desktop compile and invocation of all three checks with -m java.desktop/... | GREEN: PASS |
| Product/test Git blobs equal locally compiled source | VERIFIED |
| Full Maven/JUnit, source-built jtreg, cross-LAF/Win32/Cocoa/GTK and dedicated Synexia recipe | NOT_RUN |

Source seals: M3Oscilloscope.java 56cfeafbd5e585ed3d5d993b82357acca78981c3;
M3OscilloscopeMaxEnvelopeSmoke.java c93d457559be8a1e1179a904dd946f519ff8462c.
Public methods and persistent data lanes are unchanged. The old branch
was not mutated: the fix is in canonical java.desktop, with the new test
registered in M3NativeWidgetParityTest.
