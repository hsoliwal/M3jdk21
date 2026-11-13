<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Pass stable-order-groups

Admits only `m3/collections/src/main/java/jdk/m3/collections/M3LongIndexProjection.java`. Reverse only equal-value groups of a known opposite sorted order; copy occurrence IDs forward inside each group; retain address-kernel and encounter-order merge paths.

Preimage: `6be07c18dbbbe9ae259ff85821bb3f4ec666fddd820b0950f4115c4256f45deb`. Postimage: `bf601d711667e1078ca30246da6fa89b43d7ed951a3179e0a161365526581959`.

Parent-reviewed candidate and deterministic oracles were frozen and linted before packet creation. Baseline Java and actual JNI oracle observations match at SHA-256 e15c399b7be3d8c0fbd38bc6e2cd4c63b93bb0b95d1272bce9acdcc7bec5445b. SDK replay and generated candidate verification are still required.
