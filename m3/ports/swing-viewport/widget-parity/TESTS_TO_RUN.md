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
