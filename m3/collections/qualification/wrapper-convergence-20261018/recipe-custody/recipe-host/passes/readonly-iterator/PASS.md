<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Pass readonly-iterator

Admits only `m3/collections/src/main/java/com/m3/collections/M3FilteredLists.java`. The original AbstractList read cursor remains authoritative. A forwarding guard repairs unconditional UnsupportedOperationException for every iterator remove call, matching the existing read-only policy. The exact baseline defect, effect parity and pending witnesses are retained in the parent qualification. No linear-time or backing fail-fast improvement is claimed.

Preimage: `b2dc49d3a9ec43b6e6bd34cf5c6dac53057cf90f42cc9ed506d54822a760fc2d`. Postimage: `032bffd456e67c7568b028489ceec85f24510469b3a0e00571ab36cd17ae3b75`.

The source and frozen oracle were reviewed and linted before this resource packet was created. Actual SDK execution, generated product compilation, tests and runtime remain pending.
