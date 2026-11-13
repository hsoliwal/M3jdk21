<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Pass readonly-iterator

Admits only `m3/collections/src/main/java/jdk/m3/collections/M3FilteredLists.java`. The original AbstractList read cursor remains authoritative. A forwarding guard repairs unconditional UnsupportedOperationException for every iterator remove call, matching the existing read-only policy. The exact baseline defect, effect parity and pending witnesses are retained in the parent qualification. No linear-time or backing fail-fast improvement is claimed.

Preimage: `f1e3e21c37cbe084ef6b9446b86293d5039cd9b2bc6c1bf86d63a86793e3b8d5`. Postimage: `bb0ff9a8008521576530b0cea64d00b4f22e6e4b8424b8e5df7f416a6a8b6051`.

The source and frozen oracle were reviewed and linted before this resource packet was created. Actual SDK execution, generated product compilation, tests and runtime remain pending.
