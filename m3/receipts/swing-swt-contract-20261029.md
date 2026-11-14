<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Swing and SWT contract correction receipt

Canonical producing revision: [`bccc91680a4f172cfcd9980879fc0b25b29b4c9b`](https://github.com/hsoliwal/com.synexia/tree/bccc91680a4f172cfcd9980879fc0b25b29b4c9b/synexia-openrewrite-recipes/recipe-crates/swing-swt-contract-20261029). Receiving input: `hsoliwal/M3jdk21@669a949d192345acefcfc5e9bb0b3724174d270c`.

This is a passive receipt for generated implementation/test results. Canonical recipes, fixtures, qualification drivers and correction history remain in Synexia.

## Result and executed proof

The existing `sun.swing.list.M3ListMeasure` now retains dimensions only for known Basic/Metal label delegates, supported empty borders and immutable simple model values. Custom sizing uses the existing full loop. Unknown incremental values are refused before their rendering callbacks so the demonstrated inserted/appended UI mutations preserve stock row order. The feature remains opt-in through `-Dm3.swing.viewport=true`, off by default.

The existing mutable-value regression is strengthened to stock parity. The new public API regression covers thirteen delegate, border, Synth, icon/text and callback-order cases. No public/protected signature, class/package, EDT ownership, native entry point or consumer dependency changes.

Executed against these exact generated source/test bytes on Temurin 21.0.13+9: thirteen regression cases with activation off and on, all nine existing JUnit tests (including 30 seeds x 200 operations differential and unchanged default measurement-count assertions), and identical public/protected javap output. The unchanged naming queue checker also passes for its four existing entries; this internal owner has a separately reviewed naming-maintenance exclusion because it has no map entry.

The runtime lane patches stock `java.desktop` with the exact generated owner and pinned existing BasicListUI/helper closure. It does not establish a source-built installed M3JDK21 or jtreg gate. Unknown fallback can repeat model reads and earlier probes; universal getter/callback-count equivalence is not claimed.

## Exact source/test identities

| Generated path | Before SHA-256 | Result SHA-256 |
| --- | --- | --- |
| `src/java.desktop/share/classes/sun/swing/list/M3ListMeasure.java` | `7e8c239ec674eb2d79f83904616e00e0421b82eda67c718f295e08b3fafd1577` | `d4aff8b10ff19caeeacdbf8e464fd9d6ab3899c35502758ea58ea03999dd1635` |
| `test/jdk/javax/swing/JList/M3ListMeasureCustomLabelUITest.java` | `ABSENT` | `a028f1002cd87e8fc7171488df2d710bf274a1d31585a53ea867df3b46dac226` |
| `m3/ports/swing-viewport/jdk-reproducers/M3ListMeasureTest.java` | `9f4367476a41bcfb032aea15b9eb49382c9222b5d91fabd124025ba95f38ad9c` | `901e447f8ad9785b61cd9239335808959520f6ef138bf84940272077b158b7be` |

## Recipe custody and remaining gates

The producing revision executes 45 SDK tests serially and 45 with two workers, with identical output sets, plus 14 focused admission tests. Named FILE recipes use the existing canonical Java snapshot engine. Drift/missing/duplicate/source-kind and post-scan changes refuse; repeat execution is a fixed point. The unchanged receiver validates SDK evidence and exact sources, applies emitted bytes, then reports zero further changes.

See the producing revision's `QUALIFICATION.json`, `CONTRACT.md`, `SWING_TARGETS.json`/`SWT_TARGETS.json`, and `evidence/` for source/toolchain seals and raw results. This receipt is emitted by its own canonical Text FILE recipe in the subsequent delivery commit; it deliberately references the earlier immutable producing revision to avoid a circular commit identity.

Full installed-JDK/jtreg, complete Eclipse/Tycho/Synexia reactor, native platform matrices, full historical provenance loading, performance and release gates remain unexecuted here. Hosted CI is not claimed green. Keep the review draft; these bounded corrections do not establish complete SWT/Swing parity.
