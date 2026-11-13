<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Pass native-row-suffix

Admits only `m3/collections/src/main/native/collections/m3_index_mask_window.c`. Trim only the proven rejected row prefix from each admitted JNI region; preserve state reads, output batching, cursors and both current exports.

Preimage: `e57a418a183326d17cd117bcb81c2ddd49202b8195778f38a06b970c1058f16d`. Postimage: `3361cebc637103be73d7928e61b69ecc7fd6899abd4f8078019c39dfce6b3960`.

Parent-reviewed candidate and deterministic oracles were frozen and linted before packet creation. Baseline Java and actual JNI oracle observations match at SHA-256 e15c399b7be3d8c0fbd38bc6e2cd4c63b93bb0b95d1272bce9acdcc7bec5445b. SDK replay and generated candidate verification are still required.
