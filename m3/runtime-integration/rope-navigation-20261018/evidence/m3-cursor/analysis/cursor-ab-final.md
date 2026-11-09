<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Final M3 full-visible-scalar-window measurements

The final source retains the original five cursor fields and resolves the complete visible scalar interval. These are actual-owner observations from the frozen 42-case fixture, with 8 warmup batches, 11 measured batches, and 64 passes per batch. All checksums pass. One final candidate JVM is compared with the retained earlier baseline JVM on the same host/settings; no process-level statistical non-regression claim is made. Full sample arrays and within-JVM spread appear in `cursor-ab-final.json`.

Ratios above 1 mean less candidate elapsed time. Allocated bytes cover each measured batch and can include or eliminate cursor allocations according to HotSpot escape analysis. Fresh-first-read counts 2,048 cursor/read pairs; other fresh batches use 64 cursors and reused batches use one. The benchmark JVM is warmed even for fresh cursors.

| Shape | Access | Cursor use | Baseline ns/unit | Final ns/unit | Before/after wall | Before/after CPU | Allocated B/batch before → after |
|---|---|---|---:|---:|---:|---:|---:|
| atom-16384 | forward | fresh | 2.555 | 3.641 | 0.702 | 0.704 | 2048 → 2048 |
| atom-16384 | forward | reused | 2.235 | 3.581 | 0.624 | 0.623 | 32 → 32 |
| atom-16384 | reverse | fresh | 6.275 | 2.639 | 2.378 | 2.374 | 2048 → 2048 |
| atom-16384 | reverse | reused | 2.401 | 3.209 | 0.748 | 0.747 | 32 → 32 |
| atom-16384 | random | fresh | 2.399 | 3.612 | 0.664 | 0.663 | 2048 → 2048 |
| atom-16384 | random | reused | 2.159 | 3.745 | 0.577 | 0.576 | 32 → 32 |
| atom-16384 | fresh-first-read | fresh | 41.745 | 53.860 | 0.775 | 0.776 | 65536 → 65536 |
| balanced-unit-16384 | forward | fresh | 120.602 | 132.714 | 0.909 | 0.910 | 2048 → 2048 |
| balanced-unit-16384 | forward | reused | 121.005 | 125.278 | 0.966 | 0.968 | 32 → 32 |
| balanced-unit-16384 | reverse | fresh | 146.180 | 139.831 | 1.045 | 1.046 | 2048 → 2048 |
| balanced-unit-16384 | reverse | reused | 125.898 | 154.380 | 0.816 | 0.823 | 32 → 32 |
| balanced-unit-16384 | random | fresh | 276.927 | 305.723 | 0.906 | 0.906 | 2048 → 2048 |
| balanced-unit-16384 | random | reused | 267.911 | 304.107 | 0.881 | 0.881 | 32 → 32 |
| balanced-unit-16384 | fresh-first-read | fresh | 250.610 | 447.063 | 0.561 | 0.558 | 65536 → 65536 |
| balanced-8-16384 | forward | fresh | 12.533 | 16.022 | 0.782 | 0.782 | 2048 → 2048 |
| balanced-8-16384 | forward | reused | 12.324 | 15.208 | 0.810 | 0.810 | 32 → 32 |
| balanced-8-16384 | reverse | fresh | 55.721 | 16.069 | 3.468 | 3.466 | 2048 → 2048 |
| balanced-8-16384 | reverse | reused | 55.568 | 12.234 | 4.542 | 4.541 | 32 → 32 |
| balanced-8-16384 | random | fresh | 147.508 | 152.203 | 0.969 | 0.971 | 2048 → 2048 |
| balanced-8-16384 | random | reused | 151.080 | 147.169 | 1.027 | 1.027 | 32 → 32 |
| balanced-8-16384 | fresh-first-read | fresh | 143.886 | 110.809 | 1.299 | 1.300 | 65536 → 65536 |
| balanced-64-16384 | forward | fresh | 3.663 | 4.445 | 0.824 | 0.822 | 2048 → 2048 |
| balanced-64-16384 | forward | reused | 3.896 | 3.887 | 1.002 | 1.002 | 32 → 32 |
| balanced-64-16384 | reverse | fresh | 23.367 | 3.701 | 6.314 | 6.311 | 2048 → 2048 |
| balanced-64-16384 | reverse | reused | 24.520 | 4.074 | 6.018 | 6.015 | 32 → 32 |
| balanced-64-16384 | random | fresh | 80.759 | 83.692 | 0.965 | 0.969 | 2048 → 2048 |
| balanced-64-16384 | random | reused | 79.499 | 81.065 | 0.981 | 0.981 | 32 → 32 |
| balanced-64-16384 | fresh-first-read | fresh | 107.044 | 96.664 | 1.107 | 1.103 | 65536 → 65536 |
| shared-16384 | forward | fresh | 24.312 | 25.972 | 0.936 | 0.936 | 2048 → 2048 |
| shared-16384 | forward | reused | 24.893 | 27.182 | 0.916 | 0.916 | 32 → 32 |
| shared-16384 | reverse | fresh | 81.425 | 27.601 | 2.950 | 2.950 | 2048 → 2048 |
| shared-16384 | reverse | reused | 78.432 | 45.583 | 1.721 | 1.721 | 32 → 32 |
| shared-16384 | random | fresh | 126.659 | 145.097 | 0.873 | 0.873 | 2048 → 2048 |
| shared-16384 | random | reused | 131.307 | 147.649 | 0.889 | 0.889 | 32 → 32 |
| shared-16384 | fresh-first-read | fresh | 127.757 | 125.610 | 1.017 | 1.025 | 0 → 0 |
| shared-slice-16382 | forward | fresh | 23.403 | 30.999 | 0.755 | 0.765 | 2048 → 2048 |
| shared-slice-16382 | forward | reused | 26.064 | 29.916 | 0.871 | 0.872 | 32 → 32 |
| shared-slice-16382 | reverse | fresh | 82.138 | 31.040 | 2.646 | 2.640 | 2048 → 2048 |
| shared-slice-16382 | reverse | reused | 83.189 | 30.434 | 2.733 | 2.725 | 32 → 32 |
| shared-slice-16382 | random | fresh | 129.227 | 163.279 | 0.791 | 0.791 | 2048 → 2048 |
| shared-slice-16382 | random | reused | 131.046 | 147.861 | 0.886 | 0.886 | 32 → 32 |
| shared-slice-16382 | fresh-first-read | fresh | 85.152 | 116.237 | 0.733 | 0.731 | 0 → 0 |

Each fixture has one actual 16,384-byte Latin1 atom; compound leaves are actual slices/views over that atom. This does not measure a many-allocation scattered-payload workload, modified public String admission, or matched HotSpot/JNI integration.
