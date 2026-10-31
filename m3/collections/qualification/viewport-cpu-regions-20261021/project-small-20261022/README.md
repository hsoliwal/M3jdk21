# Received native small full-selection refinement

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

M3JDK21 owns the existing runtime implementation; no wrapper, model store or runtime layer is added. This pass changes only the original callable native project function in m3/collections/src/main/c/m3maskviewport.c. All Java sources, shared native helpers, the full projectPage body and existing signatures/exports are unchanged.

Canonical recipe and exact source: Synexia PR12855, commit 3d8ba758c204201e3145504fe8b825b009ef1908, dataset viewport-project-small-20261022. The unchanged M3HashPinnedTextSnapshotRecipe emitted and received the source after seven actual Maven/OpenRewrite tests. One Result and zero-change replay passed. No Java source was routed through the text recipe.

For up to 128 matches, the existing complete validation pass captures selected IDs in a fixed 128-jint local buffer. Only after full validation and destination-capacity admission does one writeback publish the result. Larger results retain the preceding allocated-scratch path. Ordering, duplicate IDs, all 32 state bits, aliases and validation/failure behavior remain covered by the recorded finite tests. Inputs still require caller synchronization.

For 64 selected IDs from one million rows, the separate instrumented JNI-table fixture observes 256 to 0 native heap-scratch bytes and 31,252 to 15,626 input-region reads. At 129 matches, both versions retain 516 heap bytes and 31,252 reads. The GCC14.2 -O2 x86_64 project stack frame increases from 1,168 to 1,664 bytes. This is not a total-memory or throughput improvement claim; validation remains linear.

Original product Java and JNI contracts passed 121,831 and 163,640 assertions per execution. Retained viewport and stable-sort regression observations matched baseline/candidate in mixed and interpreted modes. Independent ASan/UBSan/leak-checked fault instrumentation passed 5,666 candidate cases and 7,829 baseline cases; fewer reads mean fewer candidate fault sites. Strict Java21/GCC/Clang diagnostics, ABI checks, real JVM -Xcheck:jni and fatal UBSan passed. Native runtime stderr was empty; the SDK SLF4J warning is retained.

The outer qualifier reached its 180-second command limit during candidate interpreted Java testing. Partial logs remain; only the incomplete command and remaining ordered stages were explicitly resumed. This is not an uninterrupted qualifier-success claim. RECEIPT.json binds exact observations and the full 91-entry RAW_LOGS.json bundle hash; SELECTED_LOGS.json is only a raw-log subset.

Full Java-LST receiving is still blocked by unavailable Java21 parser/Lombok dependencies; no parser stub or Java-text substitute was introduced. Product Java still declares only project; native page wiring remains unreceived. Complete collection/provider, installed JDK/HotSpot/jtreg, SWT/Nebula/GEF/RWT and rendering/theme/language/IME/accessibility, platform/concurrency and comparative process-resource gates remain open. Prior datasets and qualification history are preserved. No merge, rebase, force-push or global receiving-stage promotion is claimed.
