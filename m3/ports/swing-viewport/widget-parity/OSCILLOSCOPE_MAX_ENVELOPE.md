<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native M3Oscilloscope: all-MAX_VALUE envelope correction

The canonical widget lives in `src/java.desktop/share/classes/jdk/m3/swing/widgets`,
following the merged #3313 receiving PR. This changes an intrinsic paint
atom of that owner, not a copied helper, new renderer or new widget.

## Contract and defect

`copyEnvelope` gives every empty pixel column the pair
`(Integer.MAX_VALUE, Integer.MIN_VALUE)`. A legitimately populated
column whose samples all equal `Integer.MAX_VALUE` has the pair
`(Integer.MAX_VALUE, Integer.MAX_VALUE)`.

The historical paint loop treated **minimum == Integer.MAX_VALUE** alone
as an empty bin, so a legitimate peak disappeared. This also affected
the new canonical runtime from #3313. Its source SHA before this change
was `ac244200338d5ffde09dcd1004f7ebdf81bba743`.

The corrected predicate skips the pair only when **both** sentinel
components indicate emptiness. This preserves the original public
API, the primitive sample ring, memory/caching, and paint control flow.
No sample object, native peer, new dependency or source copying is added.

## Executed proof

OpenJDK 21.0.12.1, Linux headless:

| Test | Result |
|---|---|
| New M3OscilloscopeMaxEnvelopeSmoke on exact old source | RED: all-MAX_VALUE waveform skipped |
| Exact one-atom change, strict javac --release 21 with -Xlint:all,-serial,-this-escape -Werror | GREEN |
| Same test after change: actual rendered red sample present and empty bin remains blank | GREEN |
| Original M3OscilloscopeSmoke | GREEN |
| M3OscilloscopeScale: 10M incoming samples, 8 channels × 1M retained, -Xmx128m | GREEN |
| Java 21 --patch-module java.desktop compile and run all three smoke/scale classes as named-module entrypoints | GREEN |
| Full Maven reactor, source-built M3JDK/jtreg, cross-platform/LAF and accessibility | NOT_RUN |
| Paired Synexia OpenRewrite recipe fixed point | NOT_RUN |

Exact committed Git blobs match the locally compiled/tested code:
`M3Oscilloscope.java` = `56cfeafbd5e585ed3d5d993b82357acca78981c3`
and `M3OscilloscopeMaxEnvelopeSmoke.java` =
`c93d457559be8a1e1179a904dd946f519ff8462c`.

The regression joins the existing `M3NativeWidgetParityTest` JUnit class.
The surrounding widget catalogue and Grid PR #3316 remain untouched.
