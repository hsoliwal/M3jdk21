<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Swing list and private tree atoms — current receiving record

This packet combines the existing JList row-offset and measurement owners with
two private `M3FixedHeightLayoutCache` selection resets. It preserves Java SE
names and descriptors, module exports, stock fallbacks and the default-off,
read-once `m3.swing.viewport` switch.

Receive against `9a164c31b38c7013c179a3ff38b0bb60c199c7fd` using the unchanged
`com.synexia.rewrite.M3SwingUiConvergence20261019` recipe in
[canonical PR #12528](https://github.com/hsoliwal/com.synexia/pull/12528), commit
`74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd`. Its twelve-target manifest remains
`8700ab0a685f1d8ac2890874d13a440461d1a496e92907759cc718a6dc7b6d16`.
The canonical PR and [M3JDK21 receiving PR #1525](https://github.com/hsoliwal/M3jdk21/pull/1525) are
open drafts. The receiving PR is stacked on [collection PR #1522](https://github.com/hsoliwal/M3jdk21/pull/1522).
No merge or further execution is implied.

## Exact source and proof boundary

The seven runtime postimages are exact M3JDK21 files from
`ad919bf89163eb537b3852f278033d6db8bda2e7`, PR #1476's inherited source chain.
That PR's own delta was documentation and benchmarks. This receive does not
import its proposed public APIs, switch defaults or a whole historical branch.

Earlier execution used parent `c2a8733f6eebaadcc206a61d08906bfd8b6d60dc`:
two existing runtime preimages and ten absent helper/proof additions. Exact
current admission confirms those same preimages and absences at `9a164c31`,
plus the identical complete module containing four borrowed proof fixtures.
See the shared `MASTER_ADMISSION.md` and mapping fragment for exact source and
rights pins. Source identity permits explicit reuse of the earlier qualified
bytes; it does not claim a rerun or qualify a changed full JDK.

The baseline module has 73 tracked files. Its earlier executed binding-only
candidate has 75 files after two binding proofs. Adding the five UI
`jdk-reproducers` files yields an 80-file combined receiving module. That is a
source-composition count; no fresh 80-file Maven execution is claimed. The
module POM does not compile the separate `jdk-reproducers` directory.

The separate current collection manifest explicitly qualifies its RowTree
inverse. Its historical KEEP record is retained and is not inverse authority
on its own. Collection SDK/native results do not broaden this Swing packet.

## Java 21 reproduction

These instructions are not new executions. Use Bash from a complete reviewed
checkout containing the seven postimages and nine proof sources named below.
Set `JAVA_HOME` and `JUNIT_CONSOLE_JAR` to real installed tools. The accepted
earlier host was Ubuntu OpenJDK `21.0.12+8-1-24.04-Ubuntu`, Linux x86_64.
The earlier test-only JUnit Platform Console Standalone 1.12.2 jar had SHA-256
`329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a`.
Its tool dependency licence is separate from the proof-source licence.

The following `--patch-module` commands are bounded source qualification. They
do not create a source-built, installed M3JDK21 image. Staging only named runtime
files prevents implicit compilation of unrelated JDK sources. The current
baseline preimages below are byte-identical to the earlier executed baseline.

```bash
set -euo pipefail
: "${JAVA_HOME:?Set JAVA_HOME to the Java 21 installation}"
: "${JUNIT_CONSOLE_JAR:?Set the path to junit-platform-console-standalone-1.12.2.jar}"
"$JAVA_HOME/bin/java" -version
printf '%s  %s\n' \
  329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a \
  "$JUNIT_CONSOLE_JAR" | sha256sum -c -
M3_UI_CHECK=build/m3-swing-java-desktop
M3_UI_BEFORE=9a164c31b38c7013c179a3ff38b0bb60c199c7fd
mkdir -p "$M3_UI_CHECK/classes" "$M3_UI_CHECK/tests" "$M3_UI_CHECK/baseline"
: > "$M3_UI_CHECK/runtime-sources.txt"
while IFS= read -r relative; do
  destination="$M3_UI_CHECK/source/$relative"
  mkdir -p "${destination%/*}"
  cp "src/java.desktop/share/classes/$relative" "$destination"
  printf '%s\n' "$destination" >> "$M3_UI_CHECK/runtime-sources.txt"
done <<'JAVA_DESKTOP_SOURCES'
javax/swing/plaf/basic/BasicListUI.java
sun/swing/list/M3ListMeasure.java
sun/swing/list/M3ListRows.java
sun/swing/list/M3PrefixRowOffsets.java
sun/swing/list/M3RowOffsets.java
sun/swing/list/M3RowPieces.java
sun/swing/tree/M3FixedHeightLayoutCache.java
JAVA_DESKTOP_SOURCES
"$JAVA_HOME/bin/javac" --patch-module "java.desktop=$M3_UI_CHECK/source" \
  -d "$M3_UI_CHECK/classes" "@$M3_UI_CHECK/runtime-sources.txt"
: > "$M3_UI_CHECK/baseline-sources.txt"
for relative in javax/swing/plaf/basic/BasicListUI.java \
                sun/swing/tree/M3FixedHeightLayoutCache.java; do
  destination="$M3_UI_CHECK/before/$relative"
  mkdir -p "${destination%/*}"
  git show "$M3_UI_BEFORE:src/java.desktop/share/classes/$relative" > "$destination"
  printf '%s\n' "$destination" >> "$M3_UI_CHECK/baseline-sources.txt"
done
"$JAVA_HOME/bin/javac" --patch-module "java.desktop=$M3_UI_CHECK/before" \
  -d "$M3_UI_CHECK/baseline" "@$M3_UI_CHECK/baseline-sources.txt"
: > "$M3_UI_CHECK/proof-sources.txt"
for proof in M3FixedHeightSelectionResetTest M3JListPublicObservation \
             M3JListViewportDifferential M3ListMeasureTest M3ListRowsTest \
             M3FixedHeightLayoutCacheTest M3TreeLayoutCacheDifferential \
             M3VariableHeightLayoutCacheTest M3JTreeViewportDifferential; do
  printf 'm3/ports/swing-viewport/jdk-reproducers/%s.java\n' "$proof" \
    >> "$M3_UI_CHECK/proof-sources.txt"
done
"$JAVA_HOME/bin/javac" --patch-module "java.desktop=$M3_UI_CHECK/classes" \
  --add-exports java.desktop/sun.swing.list=ALL-UNNAMED \
  --add-exports java.desktop/sun.swing.tree=ALL-UNNAMED \
  -cp "$JUNIT_CONSOLE_JAR" -d "$M3_UI_CHECK/tests" "@$M3_UI_CHECK/proof-sources.txt"
m3_ui_options=(-Xmx512m -Djava.awt.headless=true -XX:-OmitStackTraceInFastThrow
  --patch-module "java.desktop=$M3_UI_CHECK/classes"
  --add-exports java.desktop/sun.swing.list=ALL-UNNAMED
  --add-exports java.desktop/sun.swing.tree=ALL-UNNAMED
  --add-opens java.desktop/sun.swing.list=ALL-UNNAMED
  --add-opens java.desktop/javax.swing.plaf.basic=ALL-UNNAMED)
"$JAVA_HOME/bin/java" "${m3_ui_options[@]}" \
  -Dm3.swing.viewport=true -Ddiff.fixed=true -jar "$JUNIT_CONSOLE_JAR" execute \
  --class-path "$M3_UI_CHECK/tests" --disable-banner \
  --select-class M3ListRowsTest --select-class M3ListMeasureTest \
  --select-class M3FixedHeightLayoutCacheTest \
  --select-class M3FixedHeightSelectionResetTest \
  --reports-dir "$M3_UI_CHECK/junit-reports"
for flag in false true; do
  "$JAVA_HOME/bin/java" "${m3_ui_options[@]}" -Dm3.swing.viewport="$flag" \
    -cp "$M3_UI_CHECK/tests" M3JListViewportDifferential 40 300 1 \
    > "$M3_UI_CHECK/differential-$flag.log"
done
"$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$M3_UI_CHECK/tests" \
  M3JListPublicObservation > "$M3_UI_CHECK/public-stock.txt"
"$JAVA_HOME/bin/java" -Djava.awt.headless=true \
  --patch-module "java.desktop=$M3_UI_CHECK/baseline" -cp "$M3_UI_CHECK/tests" \
  M3JListPublicObservation > "$M3_UI_CHECK/public-baseline.txt"
"$JAVA_HOME/bin/java" "${m3_ui_options[@]}" -cp "$M3_UI_CHECK/tests" \
  M3JListPublicObservation > "$M3_UI_CHECK/public-default.txt"
for flag in false true; do
  "$JAVA_HOME/bin/java" "${m3_ui_options[@]}" -Dm3.swing.viewport="$flag" \
    -cp "$M3_UI_CHECK/tests" M3JListPublicObservation \
    > "$M3_UI_CHECK/public-$flag.txt"
done
for variant in baseline default false true; do
  cmp "$M3_UI_CHECK/public-stock.txt" "$M3_UI_CHECK/public-$variant.txt"
done
for owner in javax/swing/plaf/basic/BasicListUI sun/swing/tree/M3FixedHeightLayoutCache; do
  name="${owner##*/}"
  "$JAVA_HOME/bin/javap" -protected -s "$M3_UI_CHECK/baseline/$owner.class" \
    > "$M3_UI_CHECK/$name-before-api.txt"
  "$JAVA_HOME/bin/javap" -protected -s "$M3_UI_CHECK/classes/$owner.class" \
    > "$M3_UI_CHECK/$name-after-api.txt"
  cmp "$M3_UI_CHECK/$name-before-api.txt" "$M3_UI_CHECK/$name-after-api.txt"
done
"$JAVA_HOME/bin/javap" --module java.desktop -protected -s javax.swing.plaf.basic.BasicListUI \
  > "$M3_UI_CHECK/BasicListUI-stock-api.txt"
cmp "$M3_UI_CHECK/BasicListUI-stock-api.txt" "$M3_UI_CHECK/BasicListUI-after-api.txt"
```

The original `m3/ports/swing-viewport/pom.xml` tests the standalone model and
binding module; it does not compile `jdk-reproducers`. Keep its source-built
JUnit coordinates unchanged and use the companion binding receiving record
for that Maven lane.

## Recorded regression witnesses and limits

`M3FixedHeightSelectionResetTest` compares real stock and M3 layout-cache
owners on the EDT and invokes the real `resetRowSelection()` method while
counting calls. Earlier baseline execution failed both recorded assertions:
empty expanded non-leaf collapse must reset once; structure change replacing
selected descendants with a leaf must clear the stale row rather than retain
row 2. The candidate passed. A class-loading or compilation failure is not
that witness. For separate inspection of the expected baseline failures,
retain the JUnit exit code 1:

```bash
"$JAVA_HOME/bin/java" -Xmx512m -Djava.awt.headless=true \
  --patch-module "java.desktop=$M3_UI_CHECK/baseline" \
  --add-exports java.desktop/sun.swing.tree=ALL-UNNAMED \
  -jar "$JUNIT_CONSOLE_JAR" execute --class-path "$M3_UI_CHECK/tests" \
  --select-class M3FixedHeightSelectionResetTest --disable-banner \
  --reports-dir "$M3_UI_CHECK/baseline-reset-reports"
```

The accepted earlier packet recorded 28 passing JUnit tests, 40 seeds × 300
steps for each false/true JList differential, 1,240 pixel comparisons per run,
and identical 300-line public observations on stock, receiver baseline and
candidate absent/false/true property modes. The source-seal kernel recorded
53,640 checks over 4,096 before/post subsets, 12 CREATED and 12 UNCHANGED.
The separately bound historical SDK run passed nine OpenRewrite 8.17.1
PlainText tests; JavaLST was not run.

Preserve these boundaries:

- Opt-in unnotified value mutation can retain old row measurements until an
  event or full measure. The exact donor test preserves this divergence;
  strict compatibility is not claimed for that mode.
- The donor partial-layout counter printed 1,064 with the flag false and 990
  with it true. Its unguarded increment does not prove activation; exact
  measurement assertions provide the applicable evidence.
- Existing fixed-tree D2 exception-class/rebuild and D4 removed-path behavior
  remain. No pre-rebuild post-exception identity proof is claimed.
- Per-UI metadata, including two initial `int[8]` lanes, is eagerly allocated
  with the flag off. No resource improvement was measured in this packet.
- Broader JTree expansion, DefaultTreeSelectionModel intake, public API and
  default-on proposals remain deferred. Installed JDK, jtreg/JCK, native UI,
  Android/iOS host/device and broad resource acceptance remain separate.

## Published evidence and rights

Canonical evidence at commit `74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd`
is beneath
`synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/`.

| Receipt | SHA-256 |
| --- | --- |
| `swing-ui/QUALIFICATION.json` | `89caed5b70931312775d451f5f65225898d1a2cfa1d12152536f4636ff98bcbc` |
| `swing-ui/QUALIFICATION.md` | `133a02765dd8ccb7448fd1ced7bc3c2a0309881012857694937d435b5d7634cd` |
| `successor-sdk-final-selection/receipt.json` | `2b2e935ceb68b54d4d1a3d053533e1fec31b0e72c2ca1d481c3845e4733db82c` |

Seven runtime headers retain GPLv2 with Classpath Exception. Proof sources and
recipe code retain Apache-2.0 terms and the per-file rights crosswalk; missing
separate leaf-four ledger rows are not invented. This is recreated receiving
documentation based on published records and prior notes, not exact recovery
of an unpublished former template. Preserve STRING / QUALIFICATION_REQUIRED
and all later waiting phases.
