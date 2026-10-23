# M3JDK21 intrinsic viewport: tree-window scratch candidate custody

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

This is an **inert review/qualification receipt**, not a runtime Java source change or a second recipe owner. The original `M3TreeWindowPlan` remains the authoritative resident native-row planner inside M3JDK21. Source-changing recipes are canonical in Synexia PR #12855 at commit `a9e9f67b532ad15f93d4256489a5725cefed95f6`.

Serial proposed receiving order: product `M3TreeWindowPlan.java` SHA-256 `939c71664bda0330d30cbf88532226ec601c8908e12cf9a6a42a013bd3624368` -> failure-atomic commit candidate `ac4fc038798a5aa75480bc26a17d498300e9fcb325d93b6926d6da62e5634d8c` -> scratch reuse candidate `1aadb71fb2b39f4370ac3430864ebeb697e427b3bb72618795fa70abb493d52e`. The last stage reuses the private previous-sibling `int[]` field after resetting its active prefix. It does not change public APIs, JNI descriptors, stable row identities, or native widgets.

The reproducible local Java21/JNI bundle contains 2,242,259 direct-plan checks per execution, 133,225 smaller deterministic contract checks, 417,171 checks per 1024-frame existing tree pipeline, and strict compiler/native/runtime logs. The 128-row warmed direct-plan allocation probe measured 536 bytes/call before vs 0 bytes/call after over 25,000 calls. This is not a global heap, RSS or UI frame rate result. A deliberately removed reset, invalid source, and reused output were rejected.

**Hold**: real Java21 OpenRewrite SDK execution and canonical `receive.py` serial receiving, full source-built M3JDK21/HotSpot and SWT/Nebula/GEF/RWT widget/GPU/IME/accessibility gates. The hosted scratch workflow was queued at last inspection; no pass is inferred. Runtime `M3TreeWindowPlan.java` has NOT been modified by this receipt. No merge, rebase, force-push or default-branch write.
