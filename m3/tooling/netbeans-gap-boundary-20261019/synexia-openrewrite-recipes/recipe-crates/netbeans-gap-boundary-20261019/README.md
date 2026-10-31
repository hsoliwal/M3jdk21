<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# NetBeans gap-boundary recipe

Read CONTRACT.md first. This Maven/OpenRewrite crate reuses the existing canonical
Java/text snapshot recipe engines. The exact before/after sources and manifests
are under resources; no generated target file is hand-edited. The donor oracle
sources under third_party/netbeans are immutable and remain Apache-2.0.

Run proof/qualify.py with explicit scratch, JDK, Maven and offline cache paths.
It records ordered lint, compile, actual SDK execution, baseline/candidate runtime,
ABI and output comparisons. Measurement and publication receipts are separate.
All five outputs and every partial replay combination must converge to a fixed point;
drift, duplicate inputs and occupied additions must refuse. The custody subrecipe
only appends exact canonical provenance/recipe ownership metadata.

Source-built image, full jtreg/tier/platform/JCK/reactor acceptance remain pending.
