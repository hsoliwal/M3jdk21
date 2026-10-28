<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Replay the M3 scalar-window cursor proof

The receiver keeps the existing five-field `M3String.CodeUnitCursor` and caches the
complete scalar interval visible through its current view. This permits reuse in both
directions within a leaf. A miss still descends from the root. The receiver contains
no additional cursor fields, path arrays, native storage, or copied Swift source body.

[HANDOFF.json](HANDOFF.json) binds this support package to actual canonical commit
`67404182855a3f89f6d672274835c0f64242e4e5` (C1) and receiver base
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. It separates canonical source custody from
the receiver paths that were absent at that base. The 31 evidence/API files are exact
C1 bytes; historical paths inside their sealed logs remain historical observations.

## Prepare the exact inputs

Use a complete checkout at the pinned M3 base for `M3_SYMBOLS` and a separate emitted
receiver image for `M3_INPUT`. The latter is the union of three runtime outputs, two
core outputs, and 33 handoff additions. It supplies the changed owner and frozen final
test; the complete baseline checkout supplies unchanged actual owners and compiler
symbols. Set these paths to your local directories and choose new output directories:

```sh
JAVA21=/absolute/path/to/jdk-21
M3_SYMBOLS=/absolute/path/to/M3jdk21-at-2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49
M3_INPUT=/absolute/path/to/emitted-M3-receiver-image
M3_BEFORE_OUT=/absolute/path/to/new-before-proof
M3_AFTER_OUT=/absolute/path/to/new-after-proof
M3_SUPPORT="$M3_INPUT/m3/runtime-integration/rope-navigation-20261018"
M3_TEST="$M3_INPUT/test/jdk/java/lang/String/M3StringPathCursorTest.java"
```

Check the checkout revision and byte seals before replay. These commands must report
the pinned commit and the following SHA-256 values:

```sh
git -C "$M3_SYMBOLS" rev-parse HEAD
sha256sum "$M3_SUPPORT/run_actual_owner_cursor.py" \
  "$M3_SUPPORT/M3StringCursorBenchmark.java" "$M3_TEST"
```

| File | Required SHA-256 |
| --- | --- |
| `run_actual_owner_cursor.py` | `8ea3fb522542f0430e4714294636065446710923305852dcd5a110e6e1c72b69` |
| `M3StringCursorBenchmark.java` | `bfb53263c24edc292788f8852109477714b1db1fdd4af8691a77cbb3bb631ba2` |
| `M3StringPathCursorTest.java` | `8989a34dd9295a11f9af8c14f4c47062bb1f5cd1573ec0a86c6cce82714420bc` |

The final test is explicitly used for both sides, including the baseline where its
repository path is absent. The required owner blob option refuses source drift:

```sh
python3 "$M3_SUPPORT/run_actual_owner_cursor.py" \
  --source-root "$M3_SYMBOLS" --symbol-root "$M3_SYMBOLS" \
  --test-source "$M3_TEST" --java-home "$JAVA21" --output "$M3_BEFORE_OUT" \
  --expected-m3-blob e3bffdb11669a80a8fe8e1ba127b23fb7efc3c1e

python3 "$M3_SUPPORT/run_actual_owner_cursor.py" \
  --source-root "$M3_INPUT" --symbol-root "$M3_SYMBOLS" \
  --test-source "$M3_TEST" --java-home "$JAVA21" --output "$M3_AFTER_OUT" \
  --expected-m3-blob e53f62018eaf8e5dbf9cc85969bf3828e6950712
```

Each command compiles actual owners, confirms their runtime class loading, and runs the
same corpus under default Java 21 and `-Xint`. Each retained execution passed 1,746,792
checks across 35,914 scenarios. The modified target `String.java` is used for compiler
symbols and is not loaded into the stock VM. This bounded proof does not qualify a
matched modified String/HotSpot/JNI image or product jtreg.

## Replay the retained benchmark settings

Run the following once for each output directory, with no concurrent timed workload.
The retained 42-case matrix uses **8 warmups, 11 measured rounds and 64 passes**.
The source defaults are 4/9/32; the runner's plain `--benchmark` option uses those
defaults, so the explicit properties below are required to match the retained setup.

```sh
for M3_OUT in "$M3_BEFORE_OUT" "$M3_AFTER_OUT"; do
  "$JAVA21/bin/javac" --release 21 -proc:none -implicit:none -Xlint:all -Werror \
    --patch-module "java.base=$M3_OUT/classes" \
    -d "$M3_OUT/bench-classes" "$M3_SUPPORT/M3StringCursorBenchmark.java"
  "$JAVA21/bin/java" -Xms128m -Xmx128m -ea \
    -Dm3.cursor.warmups=8 -Dm3.cursor.rounds=11 -Dm3.cursor.passes=64 \
    --patch-module "java.base=$M3_OUT/classes" \
    -cp "$M3_OUT/bench-classes" M3StringCursorBenchmark \
    > "$M3_OUT/benchmark-8-11-64.log" 2>&1
done
```

The [frozen M3 evidence](evidence/m3-cursor/README.md) retains final results, rejected
path-cursor variants, raw samples, checksums and separate compiler diagnostics.
All 42 final checksums passed and all 42 allocation medians matched the baseline.
Reverse traversal improved on measured leaf fixtures, while forward/random atom
workloads regressed. One final candidate JVM is compared with a retained earlier
baseline JVM; these results establish no general performance non-regression.
The [final API receipt](evidence/m3-api/receipt.json) records identical
`javap -protected -s` output for all 49 production classes, including descriptors.

## Stage and source custody

1. The runtime eight-target recipe consumes its original Synexia and M3 preimages.
2. The core 13-target recipe independently consumes its original pinned preimages.
   Its receiver runner and name-map remain outputs of this core stage.
3. Actual C1 records those recipes, proofs and canonical outputs. The distinct
   `--handoff` recipe consumes C1's canonical home and ledger plus 33 receiver paths
   absent at the original M3 base. Its own exact postimages form its fixed point.
4. Compose the receiver's three runtime, two core and 33 handoff outputs: 38 files.

Replay each recipe from its declared parent. The unchanged core recipe is not expected
to accept the later handoff ledger/home; the handoff rejects their older pre-C1 bytes.
C1's immutable recipe history and this separate resource root retain both admissions.
The full provider/reactor, native-backed and platform receiving gates remain open.
