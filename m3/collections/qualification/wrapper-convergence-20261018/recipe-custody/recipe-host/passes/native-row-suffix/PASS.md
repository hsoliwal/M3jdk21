<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Pass native-row-suffix

Admits only `m3/collections/src/main/native/collections/m3_index_mask_window.c`. Trim only the proven rejected row prefix from each admitted JNI region; preserve state reads, output batching, cursors and both current exports.

Preimage: `a5cefed03d9db9429048a218945a8af71b5b7acc862e089f6cc8a451a172e212`. Postimage: `d731f12916ee7f6e935624e7dd9f108c8de99bba02e1a81f6cc96c8cd21a5b84`.

Parent-reviewed candidate and deterministic oracles were frozen and linted before packet creation. Baseline Java and actual JNI oracle observations match at SHA-256 e15c399b7be3d8c0fbd38bc6e2cd4c63b93bb0b95d1272bce9acdcc7bec5445b. SDK replay and generated candidate verification are still required.
