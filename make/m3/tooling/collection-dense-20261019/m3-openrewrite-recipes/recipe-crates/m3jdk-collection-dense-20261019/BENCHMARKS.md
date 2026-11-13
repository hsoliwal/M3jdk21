<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Complete-call diagnostics

Three fresh JVMs per variant, five rounds per shape, alternating variant order. CPU and allocation include validation, allocation and native region copies. Inputs are built before timing. Results escape through volatile sinks. These are bounded diagnostics, not JMH or a universal speedup.

| Backend | Size | Selection | Before CPU ns | After CPU ns | Ratio | Before bytes/call | After bytes/call |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: |
| integrated | 65 | all | 2187.9 | 1782.9 | 0.815 | 1200 | 640 |
| integrated | 65 | dense | 1604.4 | 1953.1 | 1.217 | 1040 | 1040 |
| integrated | 65 | sparse | 318.6 | 258.1 | 0.810 | 448 | 448 |
| integrated | 1024 | all | 69488.2 | 60331.6 | 0.868 | 15800 | 7576 |
| integrated | 1024 | dense | 39828.0 | 42292.4 | 1.062 | 13240 | 13240 |
| integrated | 1024 | sparse | 8025.5 | 6129.6 | 0.764 | 5128 | 5128 |
| integrated | 65536 | all | 5510940.5 | 5779307.4 | 1.049 | 1056264 | 531944 |
| integrated | 65536 | dense | 4311673.8 | 4309795.0 | 1.000 | 892424 | 892424 |
| integrated | 65536 | sparse | 618807.2 | 658132.3 | 1.064 | 318920 | 318920 |
| java | 64 | 1 | 114.9 | 109.3 | 0.951 | 264 | 264 |
| java | 64 | 2 | 82.0 | 95.2 | 1.161 | 144 | 144 |
| java | 64 | 64 | 22.2 | 25.0 | 1.127 | 16 | 16 |
| java | 4096 | 1 | 4741.0 | 2080.3 | 0.439 | 16144 | 16144 |
| java | 4096 | 2 | 1948.5 | 2162.7 | 1.110 | 8080 | 8080 |
| java | 4096 | 64 | 264.0 | 243.5 | 0.922 | 272 | 272 |
| java | 262144 | 1 | 275249.2 | 147363.5 | 0.535 | 1048336 | 1048336 |
| java | 262144 | 2 | 157151.3 | 153547.9 | 0.977 | 524176 | 524176 |
| java | 262144 | 64 | 18079.0 | 14714.8 | 0.814 | 16400 | 16400 |
| native | 64 | 1 | 241.6 | 215.6 | 0.892 | 264 | 264 |
| native | 64 | 2 | 185.3 | 183.5 | 0.991 | 144 | 144 |
| native | 64 | 64 | 126.7 | 143.0 | 1.128 | 16 | 16 |
| native | 4096 | 1 | 6249.4 | 2734.4 | 0.438 | 16144 | 16144 |
| native | 4096 | 2 | 2902.4 | 3234.0 | 1.114 | 8080 | 8080 |
| native | 4096 | 64 | 475.6 | 587.8 | 1.236 | 272 | 272 |
| native | 262144 | 1 | 408576.3 | 187692.0 | 0.459 | 1048336 | 1048336 |
| native | 262144 | 2 | 232288.9 | 241655.8 | 1.040 | 524176 | 524176 |
| native | 262144 | 64 | 32369.9 | 30677.4 | 0.948 | 16400 | 16400 |

Fully accepted snapshots avoid one int[] and one Object[]; returned structure is unchanged: one reference array and two facade objects, with payloads excluded from instrumentation accounting. For 65,536 selected elements the measured structural total remains 262,216 bytes. Native stack buffers remain 128 jlong and 256 jint slots; no malloc or retained JNI pointer is introduced. Per-process peak RSS and user/system time are preserved in the command receipts; no native/process-memory reduction is claimed.

The original per-word density candidate was rejected after mixed sparse regressions. Final partial-selection extraction loops are byte-identical to the baseline. Some final small/partial workloads still measured slower. CPU affinity follow-up used one workload per JVM on CPU 0; it does not override the broad series:
- 65 / dense: candidate/baseline median CPU ratio 0.827; all 15 samples per variant retained.
- 65536 / all: candidate/baseline median CPU ratio 1.011; all 15 samples per variant retained.

Retained failures: missing /usr/bin/time refused the initial measurement launcher; a resume-scoping NameError was corrected in proof tooling. Neither changed production output nor invalidated the already completed contract gates. The final runner uses os.wait4 for process receipts. An inherited diagnostic benchmark constructor warning is preserved; product Java and C compilation were warning-free.
