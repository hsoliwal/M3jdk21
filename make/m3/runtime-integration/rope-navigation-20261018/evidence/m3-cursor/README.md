<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 cursor evidence: complete visible scalar intervals

The selected M3 change retains the original five `CodeUnitCursor` fields and caches the complete part of the current scalar owner that is visible through the source and its tuple slices. Reads in either direction inside that interval reuse the scalar owner. A read outside it still descends from the tuple root. No tuple-path array, payload copy, or native address is retained by this cursor.

The final source is Git blob `e53f62018eaf8e5dbf9cc85969bf3828e6950712`, SHA256 `7d3afcfb6565325e822acdf70532817526e95641be89ad192711570549535580`. The comparison source is from M3 commit `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`, M3String blob `e3bffdb11669a80a8fe8e1ba127b23fb7efc3c1e`. Candidate3 was emitted by the actual OpenRewrite SDK and executed. Its three M3 target files were then directly compared byte for byte with final combined candidate4; all match. See `final-sdk-binding.json` for the SDK manifest, binding and output identifiers. These blob identifiers are not invented commit references.

## Correctness result and its boundary

The pinned baseline and all three candidate implementations pass the final actual-owner test corpus under both default Java 21 execution and `-Xint`: **1,746,792 checks in 35,914 scenarios per execution**. Final test SHA256 is `8989a34dd9295a11f9af8c14f4c47062bb1f5cd1573ec0a86c6cce82714420bc`.

The corpus uses unchanged actual M3String, M3StringOwner, M3StringAtom and M3StringTuple classes, real native payload bytes, and an independent UTF-16 array or arithmetic oracle. It covers forward/reverse/random reads, direction switches, invalid-index recovery, source and child slices with nonzero starts, empty children, shared DAGs, Latin1 and both UTF-16 endian forms, isolated surrogates, split pairs, streams and recursive spliterator splits. A 57-tuple shared DAG represents `Integer.MAX_VALUE` logical code units using eight native payload bytes and verifies tiny ranges near the coordinate limit without flattening or scanning the giant sequence.

The runner compiles the unchanged 15-file M3String source closure. The target String.java supplies compiler symbols only: no String.class is emitted or loaded. The real M3 owner classes are loaded with `--patch-module java.base` on stock HotSpot. Thus these results cover actual M3 owner/cursor behavior; they do **not** qualify modified public String admission, a matched HotSpot/JNI build, or every M3/MIndex family. The raw receipts record this boundary explicitly.

## Performance result: gains and regressions both retained

The final benchmark completes all 42 workloads with exact expected checksums. It uses the same compiled fixture and benchmark bytes as the retained baseline, six fixture shapes, forward/reverse/random traversal, fresh and reused cursors, and fresh first reads. Both full matrices use 8 warmup batches, 11 measured batches and 64 passes. Each compound fixture consists of actual slices/views over **one** 16,384-byte native Latin1 atom; it does not model scattered allocations across many native atoms.

The complete 42-case table is in [analysis/cursor-ab-final.md](analysis/cursor-ab-final.md). Its adjacent JSON contains every measured wall/CPU/allocation sample, median and within-process quartile. Representative **reused-cursor** medians are:

| Shape/access | Baseline ns/unit | Final ns/unit | Observation |
|---|---:|---:|---|
| 8-unit leaves, reverse | 55.568 | 12.234 | 4.54× lower elapsed time |
| 64-unit leaves, reverse | 24.520 | 4.074 | 6.02× lower elapsed time |
| Shared DAG, reverse | 78.432 | 45.583 | 1.72× lower elapsed time |
| Sliced shared DAG, reverse | 83.189 | 30.434 | 2.73× lower elapsed time |
| Atom, forward | 2.235 | 3.581 | 60% higher elapsed time |
| Atom, random | 2.159 | 3.745 | 73% higher elapsed time |
| Unit leaves, reverse | 125.898 | 154.380 | 23% higher elapsed time |
| 8-unit leaves, forward | 12.324 | 15.208 | 23% higher elapsed time |
| 64-unit leaves, forward | 3.896 | 3.887 | Nearly equal in this run |
| Shared DAG, random | 131.307 | 147.649 | 12% higher elapsed time |

CPU medians track these elapsed-time directions. The fresh-cursor results remain part of the full table: for example, 64-unit forward is about 21% slower with one fresh cursor per pass, and fresh first reads have both gains and regressions. These are observations from one final candidate JVM and the retained earlier baseline JVM on the same host. They are not a randomized multi-process estimate, a confidence interval or a general non-regression result. Final process RSS was 73,836 KiB versus baseline 78,832 KiB, but the process includes the JVM, warmup, verification, fixture construction, GC and compilation; RSS is not a retained owner-size measurement.

**All 42 measured allocation medians match baseline.** Traversal allocates 32 bytes for a reused-cursor batch and 2,048 bytes for 64 fresh cursors. Fresh-first-read batches allocate 65,536 bytes for 2,048 cursors in the atom and balanced cases; shared/sliced first-read allocations are eliminated in the measured HotSpot samples in both versions. The cursor adds no native allocation. Fixture reference arrays, wrappers and setup work lie outside traversal samples; construction and GC-sampled heap observations remain in the raw data and are not isolated production construction latency.

The earlier eager-path and adaptive-path candidates are preserved here, including their random/forward regressions and extra allocation. They offered larger sequential gains on some shapes. The separate isolated compiler diagnostic runs each of four cases in its own JVM with 20 warmups, 15 measured batches and 128 passes. All eight checksums pass; candidate2 compound regressions did not reproduce there. Inlining records do not establish that a specific compiler decision caused the mixed-suite regressions. Those diagnostic results do not replace the 42-case matrices. The final smaller M3 design was selected to bound state and allocation while improving scalar-window reuse, **not** because every elapsed-time workload improved. Complete retained tuple-path traversal is handled separately in canonical MIndex.

## Reproduction

Use the installed support directory `m3/runtime-integration/rope-navigation-20261018`, a complete pinned M3 source tree, the SDK-emitted target tree and Java 21. The exact original commands, source hashes, class-load records and sample logs are preserved in the raw bundles. The installed runner SHA256 is `8ea3fb522542f0430e4714294636065446710923305852dcd5a110e6e1c72b69`; benchmark source SHA256 is `bfb53263c24edc292788f8852109477714b1db1fdd4af8691a77cbb3bb631ba2`.

For a baseline run, set `M3_INPUT` and `M3_SYMBOLS` to the pinned before tree and `M3_EXPECTED_BLOB` to its M3String blob. For the final run, set `M3_INPUT` to the SDK-emitted tree, `M3_SYMBOLS` to the before tree, and `M3_EXPECTED_BLOB` to the final blob. `M3_TEST` points to the final installed test in either run. `M3_OUT` must be a fresh output directory. The original proof used Temurin/OpenJDK 21.0.12.1.

```sh
python3 "$M3_SUPPORT/run_actual_owner_cursor.py" \
  --source-root "$M3_INPUT" --symbol-root "$M3_SYMBOLS" \
  --test-source "$M3_TEST" --java-home "$JAVA21" \
  --output "$M3_OUT" --expected-m3-blob "$M3_EXPECTED_BLOB"

"$JAVA21/bin/javac" --release 21 -proc:none -implicit:none -Xlint:all -Werror \
  --patch-module "java.base=$M3_OUT/classes" \
  -d "$M3_OUT/bench-classes" "$M3_SUPPORT/M3StringCursorBenchmark.java"

"$JAVA21/bin/java" -Xms128m -Xmx128m \
  -Dm3.cursor.warmups=8 -Dm3.cursor.rounds=11 -Dm3.cursor.passes=64 -ea \
  --patch-module "java.base=$M3_OUT/classes" \
  -cp "$M3_OUT/bench-classes" M3StringCursorBenchmark
```

The runner uses the final test path by default when it exists in the source root; an explicit `--test-source` is needed for the unchanged baseline. Fresh cursors are measured in a warmed JVM; this does not measure cold JVM startup. Compile and contract work should complete before timed comparisons, with other benchmark processes idle. Any new replay should be retained as a new dataset, without overwriting this one.

## Package format

`index.json` records the chosen scope, all iteration identities, outcomes, raw-bundle inventory and negative findings. `file-manifest.json` records SHA256 and length of every other package file. Each file under `raw/` is a reversible JSON wrapper: its `files` entries contain the exact UTF-8 text, original path, byte count and SHA256. Encoding `entry["text"]` as UTF-8 reproduces the original bytes; verify the stored hash before writing. Original receipts and their historical paths or PASS fields are left intact. Their PASS statuses mean that the specified execution/checks completed, not that performance non-regression was established.

Original baseline-1 and baseline-2 runs precede the final max-coordinate test addition and retain their original test/header hashes. The retained 8/11/64 baseline, all final comparisons and the isolated diagnostic use the final actual fixture class; identical helper/benchmark bytecode is recorded in their execution receipts. Old evidence has not been relabeled with final inputs.
