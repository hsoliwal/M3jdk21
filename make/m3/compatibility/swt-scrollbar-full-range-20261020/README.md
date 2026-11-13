<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# SWT full-range scrollbar canonical qualification

This is a narrowly pinned cross-repository compatibility proof for the ongoing SWT programme. It changes no M3JDK21 runtime class, phase, naming map or release claim. M3JDK21 is one integrated SDK, not M3 + JDK. The existing SWT widget remains its geometry owner; no duplicate runtime layer, viewport or native ABI is introduced.

Linked implementation review: hsoliwal/eclipse.platform.swt PR #236. Canonical recipe: hsoliwal/com.synexia PR #12544, revision `9372a2d57f1a05394172a7bd85dda3f0d6d83983`. The public exact-source proof mirror is pinned to SWT commit `30814fe4c1662bdc699b8f0ec25681c6945701a3`. The job checks out that exact source, verifies the unchanged canonical engine and scope-owner blobs, executes seven real Maven/OpenRewrite JUnit gates, and compiles/runs only the actual generated geometry against the immutable original-source oracle.

This cross-target lane is useful while the source repository's job is queued and the SWT fork returns no workflow records. It does not reinterpret those unavailable jobs as success. No credentials for the private source repository are needed, no generated files are pushed by CI, and the canonical recipe remains in Synexia. Target receiving is held until an actual generated-output receipt is observed; subsequent SWT production commit must be byte-identical and independently sealed.

The candidate corrects overflow of page/range additions and hit-test endpoints within the original ViewportScrollBarGeometry. Independent arithmetic fixture proof covers 5,723,783 assertions per run; package-level signatures, ordinary-range behavior and existing rounding/clamping policy are preserved. The case count is finite and is not a native UI coverage claim.

The job uses stock Java21 to qualify the source recipe and arithmetic. It is NOT installed-M3JDK runtime acceptance. Native Win32/GTK/Cocoa pixels/real drag, DPI, accessibility, events/disposal, SWT/Tycho, source-built M3JDK/HotSpot and performance/retained-memory acceptance remain pending. A passing cross-target job does not advance String -> arrays -> collections -> AST/compiler receiving phases or qualify the five collection providers.

Original SWT source/generated output keep Apache-2.0 OR EPL-2.0. Synexia recipe and new first-party proof/configuration are Apache-2.0 with contributor attribution. No source copied into GPL runtime files. No merge/rebase/force-push is requested or performed.
