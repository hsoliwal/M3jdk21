# Native v2 resource admission and experiment history

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

## Decision

**Retain the incumbent native compactor.** Root accepted this disposition after the complete v2 matrix. The pointer/end candidate passed functional qualification but fails the frozen local CPU admission rule: 25 of 196 cases flag, including six nonempty changed emissions. Both primitive widths retain changed-path flags. The source, canonical SDK emission, assembly, transfer evidence and every raw observation remain sealed as a rejected experiment. This report does not assert that final recipe installation or publication has already occurred.

The prior ordinal-helper candidate remains held on its own evidence: 18 flags, including ten changed emissions. V1 and v2 used separate counterbalanced measurement sessions, so their flag counts do not establish a direct performance ranking. Neither candidate receives an after-the-fact width, length or shape exemption. No further timing retries or source tuning followed the v2 results.

## Method and independent audit

The unchanged frozen matrix covers int and long windows; lengths 0, 1, 255, 256, 257, 4,097 and 65,536; seven shapes; and row/state offsets 0/0 and 3/5. Six fresh JVM pairs alternate baseline/candidate order. Each JVM retains three raw samples per case, producing 7,056 timing samples and 2,352 retained-output observations. All twelve processes exited 0; every command order, argument, log hash and matrix key was checked. All timing JVMs became idle before root released the next timing slot.

The candidate and incumbent use the same pinned 282-owner Java class set and the exact already compiled standalone probe. The v2 adapter verifies its old strict compilation receipt; the v2 command tape therefore contains twelve measurements, with no new probe compilation. The runtime is Temurin JDK 21.0.8+9, Linux x86-64, CPU affinity 0, a fixed 128 MiB heap, Serial GC and -Xbatch. Each process loads its qualified unsanitized C17 library. Actual checked-JNI correctness ran separately.

For each case, the median of the three raw samples is computed within each JVM, then candidate/baseline ratios are paired across the six rounds. A flag requires a median ratio strictly above 1.10 and at least four of six ratios strictly above 1.05. This is a predeclared local admission rule, not a confidence interval. The analysis recomputed all 1,176 CPU ratios and every summary CPU, elapsed, allocation, heap and RSS field from the raw logs. Semantic/count/checksum tapes match across variants and rounds. A separate agent independently recomputed the ratios, flags, command receipts and allocation comparisons and found no discrepancy.

## CPU observations

| Width | Path | Cases | Median below 1 | Median above 1, unflagged | Frozen CPU flags |
| --- | --- | ---: | ---: | ---: | ---: |
| int | changed nonempty emission | 46 | 32 | 12 | 2 |
| int | control/empty | 52 | 22 | 20 | 10 |
| long | changed nonempty emission | 46 | 27 | 15 | 4 |
| long | control/empty | 52 | 25 | 18 | 9 |

There are no medians exactly equal to one. These bins are descriptive; the frozen plan did not define separate improved or neutral admission categories.

### Every flagged case

| Width | Shape | Length | Row/state offsets | Median CPU ratio | Six-pair range | Pairs > 1.05 | Path |
| --- | --- | ---: | --- | ---: | --- | ---: | --- |
| int | contradictory | 0 | 0/0 | 1.125812 | 0.950690–2.063276 | 4/6 | control/empty |
| int | contradictory | 256 | 0/0 | 1.333598 | 0.974340–1.820037 | 4/6 | control/empty |
| int | contradictory | 65,536 | 0/0 | 1.598695 | 0.611793–2.334845 | 5/6 | control/empty |
| int | contradictory | 65,536 | 3/5 | 1.368667 | 0.557811–1.902179 | 4/6 | control/empty |
| int | dense | 256 | 3/5 | 1.235177 | 0.599043–1.546670 | 4/6 | changed emission |
| int | last-only | 255 | 3/5 | 1.203863 | 0.458290–1.621294 | 4/6 | changed emission |
| int | prefix | 0 | 0/0 | 1.127672 | 0.763800–1.434986 | 4/6 | control/empty |
| int | rejected | 0 | 3/5 | 1.110259 | 0.648954–1.222826 | 5/6 | control/empty |
| int | zero-masks | 255 | 0/0 | 1.114971 | 0.505609–1.923234 | 4/6 | control/empty |
| int | zero-masks | 256 | 0/0 | 1.361825 | 0.884681–1.695700 | 5/6 | control/empty |
| int | zero-masks | 256 | 3/5 | 1.462721 | 1.078709–3.377218 | 6/6 | control/empty |
| int | zero-masks | 65,536 | 3/5 | 1.231734 | 0.732598–1.453000 | 4/6 | control/empty |
| long | alternating-pages | 0 | 0/0 | 1.240209 | 0.718047–1.746168 | 4/6 | control/empty |
| long | alternating-pages | 257 | 3/5 | 1.320459 | 0.550959–1.701198 | 4/6 | changed emission |
| long | contradictory | 1 | 0/0 | 1.172648 | 0.547938–1.636478 | 4/6 | control/empty |
| long | dense | 0 | 0/0 | 1.189889 | 0.684791–1.497804 | 4/6 | control/empty |
| long | dense | 255 | 0/0 | 1.288458 | 0.621507–1.635242 | 4/6 | changed emission |
| long | dense | 257 | 3/5 | 1.165065 | 0.707200–1.293231 | 4/6 | changed emission |
| long | last-only | 0 | 0/0 | 1.314169 | 0.902192–1.689975 | 4/6 | control/empty |
| long | prefix | 0 | 0/0 | 1.251753 | 0.940739–1.700803 | 4/6 | control/empty |
| long | prefix | 256 | 3/5 | 1.254163 | 0.804037–1.615774 | 4/6 | changed emission |
| long | rejected | 0 | 3/5 | 1.190376 | 0.852527–1.491158 | 4/6 | control/empty |
| long | zero-masks | 1 | 0/0 | 1.117476 | 0.933367–1.363731 | 5/6 | control/empty |
| long | zero-masks | 1 | 3/5 | 1.105800 | 0.766211–1.733267 | 4/6 | control/empty |
| long | zero-masks | 256 | 0/0 | 1.152783 | 0.547054–2.142223 | 4/6 | control/empty |

Each of the six changed flags has exactly four ratios above 1.05; each paired range also crosses one. Nineteen control/empty cases flag, including the unchanged required-only zero-mask path. This substantial variability limits causal attribution. It does not justify discarding the complete matrix or silently replacing its rule. The decision is that this candidate has not satisfied local admission, without asserting that every flagged ratio is an inherent algorithmic slowdown.

### Complete matrix of median paired CPU ratios

An asterisk marks the frozen CPU flag. The following 49 rows expose all 196 combinations; full six-ratio vectors and raw nanosecond counts remain in SUMMARY.json and the round logs.

| Length | Shape | Path | Int 0/0 | Int 3/5 | Long 0/0 | Long 3/5 |
| ---: | --- | --- | ---: | ---: | ---: | ---: |
| 0 | rejected | control/empty | 1.081689 | 1.110259* | 1.083009 | 1.190376* |
| 0 | dense | control/empty | 1.076269 | 0.939699 | 1.189889* | 1.073528 |
| 0 | prefix | control/empty | 1.127672* | 1.046644 | 1.251753* | 1.050450 |
| 0 | last-only | control/empty | 1.061869 | 0.997932 | 1.314169* | 1.097253 |
| 0 | alternating-pages | control/empty | 0.977534 | 1.057306 | 1.240209* | 0.869273 |
| 0 | zero-masks | control/empty | 0.950551 | 0.980437 | 1.082619 | 0.965684 |
| 0 | contradictory | control/empty | 1.125812* | 0.959518 | 1.236302 | 1.005013 |
| 1 | rejected | control/empty | 0.980226 | 0.944327 | 0.996363 | 0.789861 |
| 1 | dense | changed | 0.922563 | 0.847915 | 0.854433 | 0.679641 |
| 1 | prefix | control/empty | 0.903691 | 0.784274 | 1.007105 | 0.851831 |
| 1 | last-only | changed | 0.863501 | 0.788649 | 0.877082 | 1.019720 |
| 1 | alternating-pages | changed | 0.861912 | 0.888814 | 0.915212 | 0.890125 |
| 1 | zero-masks | control/empty | 0.912279 | 1.067473 | 1.117476* | 1.105800* |
| 1 | contradictory | control/empty | 1.047744 | 1.007614 | 1.172648* | 0.942162 |
| 255 | rejected | control/empty | 1.035049 | 1.054676 | 0.938684 | 1.250748 |
| 255 | dense | changed | 0.950400 | 1.025786 | 1.288458* | 1.002596 |
| 255 | prefix | changed | 1.004589 | 0.968144 | 1.007411 | 1.012009 |
| 255 | last-only | changed | 0.936912 | 1.203863* | 0.811331 | 0.868620 |
| 255 | alternating-pages | changed | 1.048526 | 1.000495 | 0.739967 | 0.762035 |
| 255 | zero-masks | control/empty | 1.114971* | 1.001139 | 0.959846 | 1.074719 |
| 255 | contradictory | control/empty | 0.948503 | 1.076622 | 0.902874 | 1.011559 |
| 256 | rejected | control/empty | 0.961416 | 1.029046 | 0.935768 | 0.747985 |
| 256 | dense | changed | 0.954698 | 1.235177* | 0.753464 | 0.771774 |
| 256 | prefix | changed | 0.935096 | 0.964073 | 1.121103 | 1.254163* |
| 256 | last-only | changed | 1.207199 | 0.996922 | 0.995815 | 1.022319 |
| 256 | alternating-pages | changed | 1.035551 | 1.025631 | 1.147015 | 0.978811 |
| 256 | zero-masks | control/empty | 1.361825* | 1.462721* | 1.152783* | 1.080898 |
| 256 | contradictory | control/empty | 1.333598* | 1.255750 | 0.796310 | 0.954476 |
| 257 | rejected | control/empty | 1.107125 | 0.931359 | 0.967632 | 0.996183 |
| 257 | dense | changed | 0.983873 | 0.982355 | 1.073564 | 1.165065* |
| 257 | prefix | changed | 0.841505 | 0.966066 | 1.023138 | 1.027544 |
| 257 | last-only | changed | 0.982606 | 0.938193 | 0.964317 | 1.023592 |
| 257 | alternating-pages | changed | 0.912360 | 0.935405 | 1.033116 | 1.320459* |
| 257 | zero-masks | control/empty | 0.969375 | 1.045693 | 1.040355 | 0.922172 |
| 257 | contradictory | control/empty | 0.866919 | 1.042771 | 0.948613 | 0.794753 |
| 4,097 | rejected | control/empty | 0.915892 | 0.968341 | 1.000463 | 1.038346 |
| 4,097 | dense | changed | 0.967200 | 1.015990 | 0.862510 | 0.805125 |
| 4,097 | prefix | changed | 0.969151 | 0.845408 | 0.965870 | 0.900212 |
| 4,097 | last-only | changed | 0.911299 | 0.976492 | 1.012991 | 0.995277 |
| 4,097 | alternating-pages | changed | 0.910624 | 1.093603 | 0.935429 | 0.990208 |
| 4,097 | zero-masks | control/empty | 1.078071 | 0.967871 | 0.986267 | 1.105070 |
| 4,097 | contradictory | control/empty | 1.008066 | 0.978606 | 0.995022 | 1.095814 |
| 65,536 | rejected | control/empty | 1.054198 | 0.984021 | 0.983263 | 0.801990 |
| 65,536 | dense | changed | 0.963984 | 1.016038 | 0.812500 | 1.015186 |
| 65,536 | prefix | changed | 1.185691 | 0.891229 | 0.999476 | 0.970727 |
| 65,536 | last-only | changed | 0.950379 | 0.985045 | 0.947312 | 0.965107 |
| 65,536 | alternating-pages | changed | 1.059714 | 0.845726 | 0.998078 | 1.118175 |
| 65,536 | zero-masks | control/empty | 0.867394 | 1.231734* | 0.938056 | 0.964334 |
| 65,536 | contradictory | control/empty | 1.598695* | 1.368667* | 0.932784 | 1.007746 |

## Allocation, retention and process observations

Measured Java allocations are exactly equal in all 3,528 paired timing samples and all 1,176 paired sets of 32 retained outputs. All 196 six-JVM median allocation vectors are equal. Output element counts and retained payload bytes match. There are zero allocation-increase flags and no measured Java allocation saving.

Both required ThreadMXBean counters, optional process CPU and RSS observations were available throughout. Process CPU can legitimately report zero deltas because of its resolution; unavailable values would remain -1. Requested-GC heap deltas differ in six paired observations, all in two empty int cases. Both variants range from -852,576 to 16,778,000 bytes. These observations include runtime cleanup and do not directly measure native scratch.

Post-retention RSS ranges overlap: baseline 46,128–109,760 KiB and candidate 46,084–108,976 KiB. Process peak RSS ranges also overlap: baseline 105,892–109,992 KiB and candidate 105,820–109,272 KiB. No process-memory reduction is established. Every process elapsed/user/system CPU value is retained in COMMANDS.jsonl and the recommendation. Whole-process totals include warmup, JIT compilation, verification, printing and requested GC; they do not override individual cases.

## Functional and transfer evidence

The actual v2 canonical SDK emission and fresh-disk fixed point passed before native compilation. Native qualification then completed 53 successful commands: strict C17 compilation of twelve translation units, eleven shared-library links, unchanged generated-header/export ABI, a C++20 generated-header/link consumer, a UBSan instrumented JNI-table model, actual checked-JNI contracts and fresh unavailable/legacy capability lanes. V2 model, Java/JNI, refusal and viewport tapes are byte-identical to v1. The native adapter does not claim a new full SDK JUnit run or full integrated JDK image.

The additive actual Java/JNI contract covers 70,080 cases and 438,034 checks per backend, with digest c665315f798fc026. Protected existing required/excluded, compactor, mask-window and spans contracts also passed. The instrumented C model separately covers 1,864 full-entrypoint cases with semantic digest 2f94c174f2012771. Model checks are not described as actual JVM execution.

| Model quantity | Incumbent | V1 and V2 |
| --- | ---: | ---: |
| Count-state bytes | 582,934 | 582,934 |
| Emission-state bytes | 503,560 | 503,560 |
| Payload region calls | 2,324 | 1,954 |
| Payload bytes | 3,021,360 | 2,304,828 |

The measured model transfer reduction is preserved: 370 fewer payload calls and 716,532 fewer payload bytes over the finite fixture matrix. In the 1,024-row one-last-row representatives, int payload transfer falls from 4,096 to 4 bytes and long from 8,192 to 8 bytes, with four region calls falling to one. Count and retained second state scans, zero-kept early return, output order, drift exceptions and pending-exception handling remain unchanged. These transfer facts do not establish CPU improvement.

## Pointer hypothesis and generated code

V2 was frozen before implementation as an algorithmic refinement across both widths and every size. Its private helper traverses cursor/end pointers and returns the first accepted byte or NULL. Each caller derives the bounded scalar begin index before any JNI call; the temporary pointer scope ends before that call. Exact reversal of these three edits reconstructs sealed v1 bytes. Independent source review found no correctness blocker.

The generated code confirms removal of the extra ordinal increment in the rejected-prefix helper. The i-minus-begin suffix expression is already strength-reduced outside the emission loop. There is no supported lost-vectorization explanation: all three variants are scalar. Earlier entrypoints and count helpers retain an identical 4,928-byte text prefix.

| Generated property | Incumbent | V1 ordinal helper | V2 pointer helper |
| --- | ---: | ---: | ---: |
| Each changed export body, bytes | 751 | 862 | 937 |
| Int principal frame reservation, bytes | 1,368 | 1,368 | 1,384 |
| Long principal frame reservation, bytes | 2,392 | 2,392 | 2,408 |

V2 also reads the excluded mask from a stack slot on part of the first-match path. Thus pointer traversal trades an increment for different register/code pressure; it does not establish generally cheaper code. The full resource experiment supplies the admission result. Fixed source buffer capacities remain unchanged even though compiler frame reservations grow by 16 bytes.

## Custody and remaining scope

NATIVE_V2_EVIDENCE_MANIFEST.json includes the original 151-file native manifest after rechecking every recorded byte count and hash, both rejected source revisions and emitted native owners, v2 contract/source review, SDK receipts, functional and assembly logs, the complete resource matrix, independent review and this admission report. EVIDENCE_SHA256SUMS seals the new manifest and principal outputs. Binaries remain at their original paths and are represented by source/header/export/library seals; no duplicate binary archive is created.

The v1 HOLD, old failed runner receipts, every sample and all historical stage-local NOT_RUN or pending statuses remain immutable. The completed resource report supplements those earlier receipts. Superseded design drafts M3RequiredExcludedWindowContract.java and m3_required_excluded_window_regions.c remain excluded from publication; their distinct additive refinement replacements are retained. Original protected product fixtures stay untouched.

The available historical core libjava lacks the current core mask-window entrypoints. SDK shared-library qualification does not close a matching current-source integrated JDK image, full owning reactor or cross-platform gate. Final recipe installation, catalogue disposition and publication belong to root; the native review supports KEEP of the pinned incumbent source.

- Complete v2 summary SHA-256: `a3786285fd040abfe0c1f9cec3e2c621222763a6447342883b0fd228783f88a7`
- Resource command receipt SHA-256: `5ea47db35796af2e92e514c1eb0ab857e4592af481c2318401e1644ee163cb14`
- V2 functional receipt SHA-256: `81cf87ca0a6d27fd44df2be83f7891ccfb03f8f93fd5700634ec047b37f5260f`
- Admission recommendation SHA-256: `97c73dee7d57743d085b52f9259f2b6a4c406cfa863351e42e98604bcfa2b524`
- Independent resource review SHA-256: `145847102716d764e335c57fdec95682535f4b3628bf01c3b743bc55a703d362`
