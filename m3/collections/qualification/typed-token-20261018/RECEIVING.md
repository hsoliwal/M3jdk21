<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Receiver mirror

Canonical proof files in this directory are exact copies from Synexia commit
`a17010170d89160ba79b2e785b36933e46648340`, PR #12381. The README describes paths
relative to that canonical crate. In this public receiver use the existing crate
at `m3/collections/qualification/convergence-20261018/recipe`, with `sdk-owner`
as `--recipe-owner`. The four new datasets are mirrored there unchanged.
The canonical source remains the owner; CANONICAL_SOURCE.json pins it.

Apply datasets serially in the recorded order to baseline
`872e70d20f3f3223a72eb5f67b001528b7fb2cb6`. Run actual SDK tests to regenerate
qualified results before receiving; evidence copies do not substitute for execution.
The source and test outputs were produced by that receiver and independently
qualified. No native proof helper belongs in the JDK test source tree.
